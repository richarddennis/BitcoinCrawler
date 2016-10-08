
package bitcoinclient;

import java.io.IOException;

public class ScheduleCrawl {

    public static void main(String[] args) throws InterruptedException {
        long start;
        start = System.currentTimeMillis();

        while (true) {
            long nextCrawl = System.currentTimeMillis() + 3600 * 1000 * 3;

            try {
                NetworkCrawler networkCrawler;
                networkCrawler = new NetworkCrawler();
            } catch (InterruptedException | IOException e) {
            }

            System.out.println("Waiting for next crawl time");
            while (System.currentTimeMillis() < nextCrawl) {
                Thread.sleep(5000);
            }

        }

    }

}
