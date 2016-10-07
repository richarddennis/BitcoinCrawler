/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bitcoinclient;

/**
 *
 * @author mad_r
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;

public class BitcoinClient {
	Socket client;
	InputStream in;
	OutputStream out;
	
	/**
	 * Hash something twice with sha256
	 * 
	 * @param in what to hash
	 * @return the hash
	 */
	public static byte[] sha256twice(byte[] in) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("sha-256");
			byte[] hash1 = md.digest(in);
			md.reset();
			return md.digest(hash1);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException();
		}
	}
	
	/** generate a hex string of byte array with bytes reversed (bitcoin does this for tx hashes etc)
	 * 
	 * @param in what to generate hex string from
	 * @return the hex string
	 */
	public static String hexStringReversed(byte[] in) {
		String hex = Hex.encodeHexString(in);
		String nhex = "";
		for (int i = hex.length(); i>0; i-=2) {
			nhex += hex.substring(i-2, i);
		}
		return nhex;
	}
	
	public BitcoinClient() {
		
	}
	
	/**
	 * Attempt to connect to a peer
	 * 
	 * @param peer
	 * @return whether successful
	 */
	public boolean connect(PeerAddress peer) {
		// TODO Auto-generated method stub
		client = new Socket();
		try {
			client.connect(new InetSocketAddress(peer.ip, peer.port), 10000);

			in = client.getInputStream();
			out = client.getOutputStream();
		} catch (Exception e1) {
			return false;
		}
		
		return true;
	}
	
	public static BitcoinPacket decodePacket(InputStream in) throws IOException {
		// READ ENTIRE PACKET IN
		byte[] hdr = new byte[24];
		
		IOUtils.readFully(in, hdr); // hdr bytes
		ByteBuffer buf = ByteBuffer.wrap(hdr);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		
		int magic = buf.getInt();
		if(magic != BitcoinPacket.NETWORK) {
			throw new IOException("Incoming packet has wrong NETWORK MAGIC");
		}
		
		byte[] cmdbyte = new byte[12];
		buf.get(cmdbyte);
		String cmd = new String(cmdbyte).trim();
		 
		int length = buf.getInt();
		byte checksum[] = new byte[4];
		buf.get(checksum);
		
		// get payload
		byte payload[] = new byte[length];
		IOUtils.readFully(in, payload);
		
		// verify payload checksum
		byte[] calc_checksum = sha256twice(payload);
		if(!Arrays.equals(checksum, Arrays.copyOfRange(calc_checksum, 0, 4))) {
			System.out.println("CHECKSUM FAILED");
			throw new IOException("Checksum failed");
		}
		
		return new BitcoinPacket(cmd, payload);
	}
	
	long lastPingTime = 0;
	
	/**
	 * Connect to a peer and sent a getaddr message, wait for addr response and
	 * return list of peers it knows
	 * 
	 * @param timeout how long to wait for addr response (milliseconds)
	 * @return list of known peers or null on failure/timeout
	 * 
	 * @throws IOException
	 */
	public HashSet<PeerAddress> enumerate(long timeout) {
		long startConnection = System.currentTimeMillis();
			
		try {
			// send version packet
			VersionPacket vpkt = new VersionPacket(client);
			out.write(vpkt.pack());
			
			while(true) {
				if(System.currentTimeMillis() - startConnection > timeout)
					return null; // if we've been connected more than two minutes then disconnect - the node hasn't sent the peer address list
				
				BitcoinPacket inPkt = decodePacket(in);

		//		System.out.println("Received: "+inPkt.command);
				
				// handle incoming packet
				if(inPkt.command.equals("version")) { 
					// acknowledge a version packet, at which point the connection is now up
					out.write(new BitcoinPacket("verack", new byte[] {}).pack());
					
					// send a ping
					byte []nonce = new byte[8];
					new Random().nextBytes(nonce);
					out.write(new BitcoinPacket("ping", nonce).pack());
					lastPingTime = System.currentTimeMillis();
					out.flush();
					
					// request list of peers as we're now connected
					out.write(new BitcoinPacket("getaddr", new byte[] {}).pack());
					
				} else if(inPkt.command.equals("ping")) {
					out.write(new BitcoinPacket("pong", inPkt.payload).pack());
					
				} else if(inPkt.command.equals("pong")) {
					long pingTime;
					//System.out.println("PING TIME (ms): "+pingTime);   // PRINT OUT PING TIME FOR THIS NODE
                                    pingTime = System.currentTimeMillis() - lastPingTime;
					
				} else if(inPkt.command.equals("addr")) {
					ByteBuffer pl = ByteBuffer.wrap(inPkt.payload);
					int entries = (int) BitcoinPacket.from_varint(pl);
					
					// get list of peers
					HashSet<PeerAddress> peerset = new HashSet<>();
					for(int i=0; i<entries; i++) {
						PeerAddress pa = BitcoinPacket.from_netaddr(pl);
						peerset.add(pa);
					}
					
					client.close(); //close connection once we've got list of addresses (use for crawler only)
					return peerset;
					
				} /* else if(inPkt.command.equals("inv")) {
					ByteBuffer pl = ByteBuffer.wrap(inPkt.payload);
					
					// print out inv entries:
					long count = BitcoinPacket.from_varint(pl);
					for (int i = 0; i < count; i++) {
						int type = pl.getInt();
						byte hash[] = new byte[32];
						pl.get(hash);
						System.out.println("[INV] Got Type "+type+" with hash "+hexStringReversed(hash));
					}
					
					//Uncomment to request full transactions via a getdata
					//out.write(new BitcoinPacket("getdata", inPkt.payload).pack());
					
				} */ /* else if(inPkt.command.equals("tx")) { // received a transaction object
					System.out.println("TX: "+hexStringReversed(sha256twice(inPkt.payload)));
				}*/
			}
		} catch(IOException e) {
			System.out.println(e);
		}
		return null;
	}
}
