/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bitcoinclient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.mysql.jdbc.Connection;
import java.io.FileWriter;

public final class NetworkCrawler implements Runnable {

    public static void main(String[] args) throws UnknownHostException, InterruptedException {
        NetworkCrawler networkCrawler;
        networkCrawler = new NetworkCrawler();
    }

    // Queue off to crawl peers
    PriorityBlockingQueue<PeerAddress> crawlQueue = 
            new PriorityBlockingQueue<>(1000, (PeerAddress o1, PeerAddress o2) 
                    -> o1.time.before(o2.time)
            ? +1 : -1
    );
    // Set of known peers (ip,port) peers
    Set<PeerAddress> knownPeers = Collections.newSetFromMap(new 
        ConcurrentHashMap<PeerAddress, Boolean>());

    public void newPeersDiscovered(HashSet<PeerAddress> set) {
        set.stream().forEach((peerAddress) -> {
            newPeerDiscovered(peerAddress);
        });
    }

    public void newPeerDiscovered(PeerAddress peerAddress) {
        if (!knownPeers.contains(peerAddress)) {
            crawlQueue.add(peerAddress);
            knownPeers.add(peerAddress);
        }
    }

    public void doDnsSeeding(int count, String host, int port) throws UnknownHostException {
        InetAddress addrs[] = InetAddress.getAllByName(host);
        for (int i = 0; i < count && i < addrs.length; i++) {
            addSeed(addrs[i].getHostAddress(), port);
        }
    }

    public void addSeed(String host, int port) throws UnknownHostException {
        PeerAddress seed = new PeerAddress(InetAddress.getByName(host), port, new Date());
        newPeerDiscovered(seed);
    }

    AtomicLong successfullyConnected = new AtomicLong(0);
    Connection con = null;
    int crawlId = 0;
    AtomicLong runningThreads = new AtomicLong(0);

    public NetworkCrawler() throws UnknownHostException, InterruptedException {

        doDnsSeeding(10, "bitseed.xf2.org", 8333);
        addSeed("46.4.93.54", 8333);
        addSeed("207.226.141.129", 8333);
        addSeed("68.173.52.208", 8333);
        addSeed("209.58.130.210", 8333);
        addSeed("66.175.220.212", 8333);
        addSeed("195.62.61.25", 8333);
        addSeed("93.190.137.186", 8333);

        long startCrawl = System.currentTimeMillis();

        // launch a work thread pool to do the crawling (run() is crawl func)
        int POOLSIZE = 10000;

        ExecutorService exec = Executors.newFixedThreadPool(POOLSIZE + 5);

        for (int i = 0; i < POOLSIZE; i++) {
            exec.execute(this);
        }

        // print out status every 200ms
        while (runningThreads.get() > 0) {
            System.out.println("Time " + (System.currentTimeMillis() - startCrawl)
                    + " STATUS known " + knownPeers.size() + " left to crawl "
                    + crawlQueue.size() + " successful conns "
                    + successfullyConnected.get() + " threads "
                    + runningThreads.get());
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
            }
        }
        //exec.awaitTermination(100, TimeUnit.DAYS);
        System.out.println("FINAL: STATUS known " + knownPeers.size()
                + " successful conns " + successfullyConnected.get());
        System.exit(0);

    }

    /* do the crawling thread
     */
    @Override
    public void run() {
        runningThreads.incrementAndGet();
        while (!crawlQueue.isEmpty()) {
            PeerAddress nextPeer;
            try {
                nextPeer = crawlQueue.poll(2, TimeUnit.MINUTES);
            } catch (InterruptedException e1) {
                runningThreads.decrementAndGet();
                return;
            }

            //System.out.println("Trying "+nextPeer);
            BitcoinClient bc = new BitcoinClient();
            if (bc.connect(nextPeer)) {
                successfullyConnected.incrementAndGet();
                try {
                    String filename = "PeerAddress.txt";
                    try (FileWriter fw = new FileWriter(filename, true)) {
                        fw.write(nextPeer + "\n");
                    }
                } catch (IOException ioe) {
                    System.err.println("IOException: " + ioe.getMessage());
                }

                System.out.println("Successfully connected to " + nextPeer);

                HashSet<PeerAddress> peercache = bc.enumerate(10000);
                if (peercache != null) {

                    newPeersDiscovered(peercache);
                }

            } else {

            }
        }
        runningThreads.decrementAndGet();
    }
}
