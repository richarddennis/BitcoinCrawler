/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bitcoinclient;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Date;

public class BitcoinPacket {

    final static int MAINNET = 0xD9B4BEF9;
    final static int TESTNET = 0xDAB5BFFA;
    final static int TESTNET3 = 0x0709110B;

    public static int NETWORK = MAINNET;

    byte[] payload;
    String command;

    public BitcoinPacket(String command, byte[] payload) {
        super();
        this.command = command;
        this.payload = payload;
    }

    /**
     * Convert packet into bytes to be sent over socket
     *
     * @return bytes
     */
    public byte[] pack() {
        ByteBuffer buf = ByteBuffer.allocate(1024);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(NETWORK);
        buf.put(command.getBytes());
        for (int i = command.length(); i < 12; i++) {
            buf.put((byte) 0);
        }
        buf.putInt(payload.length);
        buf.put(Arrays.copyOfRange(BitcoinClient.sha256twice(payload), 0, 4));
        buf.put(payload);
        buf.flip();
        byte[] outbuf = new byte[buf.limit()];
        buf.get(outbuf);
        return outbuf;
    }

    /**
     * Read a netaddr from a bytebuffer (advancing byte buffer) and return a
     * peer address
     *
     * @param buf byte buffer
     * @return
     */
    public static PeerAddress from_netaddr(ByteBuffer buf) {
        buf.order(ByteOrder.LITTLE_ENDIAN);
        Date time = new Date((long) buf.getInt() * 1000L);
        long services = buf.getLong();
        byte[] ipdata = new byte[16];
        buf.get(ipdata);
        buf.order(ByteOrder.BIG_ENDIAN);
        int port = buf.getShort();
        InetAddress ip = null;
        try {
            ip = InetAddress.getByAddress(ipdata);
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        buf.order(ByteOrder.LITTLE_ENDIAN);

        return new PeerAddress(ip, port, time);
    }

    static int SERVICES = 1;

    /**
     * Convert ip and port into netaddr structure for incorporation into bitcoin
     * packet
     *
     * @param ip
     * @param port
     * @return bytes to go into packet
     */
    public static byte[] to_netaddr(InetAddress ip, int port) {
        ByteBuffer buf = ByteBuffer.allocate(1024);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.putLong(SERVICES); // services
        buf.putLong(0);
        buf.putInt(0xFFFF0000);
        byte ipbuf[] = new byte[4];
        try {
            InetAddress.getByAddress(ipbuf);
        } catch (UnknownHostException e) {
            throw new RuntimeException();
        }
        buf.put(ipbuf);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort((short) port);
        buf.flip();

        byte out[] = new byte[buf.limit()];
        buf.get(out);
        return out;
    }

    /**
     * Read varint from byte buffer advancing the byte buffer
     *
     * @param buf
     * @return the value
     */
    public static long from_varint(ByteBuffer buf) {
        buf.order(ByteOrder.LITTLE_ENDIAN);
        int type = buf.get() & 0xff;
        buf.order(ByteOrder.LITTLE_ENDIAN);
        if (type < 0xFD) {
            return type;
        } else if (type == 0xfd) {
            return buf.getShort();
        } else if (type == 0xfe) {
            return buf.getInt();
        } else {
            return buf.getLong();
        }
    }

    /**
     * Long to varint bytes for incorporation into a packet
     *
     * @param in
     * @return
     */
    public static byte[] to_varint(long in) {
        ByteBuffer buf = ByteBuffer.allocate(1024);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        if (in < 0xFD) {
            buf.put((byte) in);
        } else if (in < 0xFFFF) {
            buf.put((byte) 0xFD);
            buf.putShort((short) in);
        } else if (in < 0xFFFFFFFF) {
            buf.put((byte) 0xFe);
            buf.putInt((int) in);
        } else {
            buf.put((byte) 0xFF);
            buf.putLong(in);
        }

        buf.flip();

        byte out[] = new byte[buf.limit()];
        buf.get(out);
        return out;
    }

    /**
     * String to varstr bytes for incorporation into packet
     *
     * @param str
     * @return
     */
    public static byte[] to_varstr(String str) {
        ByteBuffer buf = ByteBuffer.allocate(1024);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put(to_varint(str.length()));
        buf.put(str.getBytes());
        buf.flip();

        byte out[] = new byte[buf.limit()];
        buf.get(out);
        return out;
    }
}
