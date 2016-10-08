package bitcoinclient;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

public class VersionPacket extends BitcoinPacket {

    /**
     * Generate a version packet
     *
     * @param remote
     */
    public VersionPacket(Socket remote) {
        super("version", null);
        ByteBuffer buf = ByteBuffer.allocate(1024);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(70002);
        buf.putLong(SERVICES); // services
        buf.putLong(System.currentTimeMillis() / 1000);

        buf.put(to_netaddr(remote.getInetAddress(), remote.getPort()));
        
        //Log all the nodes we attempt to send data to
        try (FileWriter fw = new FileWriter("sendInt.txt", true);
                BufferedWriter bw = new BufferedWriter(fw);
                PrintWriter out = new PrintWriter(bw)) {
            out.println((remote.getInetAddress()));
            out.println(remote.getPort());
        } catch (IOException e) {
        }

        buf.put(to_netaddr(remote.getLocalAddress(), remote.getLocalPort()));

        buf.putLong(new Random().nextLong());
        buf.put(to_varstr("peerenum"));
        buf.putInt(0);
        buf.put((byte) 1);
        buf.flip();
        payload = new byte[buf.limit()];
        buf.get(payload);

    }

}
