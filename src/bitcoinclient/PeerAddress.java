/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bitcoinclient;

import java.net.InetAddress;
import java.util.Date;

public class PeerAddress {

    InetAddress ip;
    int port;
    Date time;

    public PeerAddress(InetAddress ip, int port, Date time) {
        super();
        this.ip = ip;
        this.port = port;
        this.time = time;
    }

    @Override
    public String toString() {
        return "PeerAddress [ip=" + ip + ", port=" + port + ", time=" + time
                + "]";
    }

    @Override
    public int hashCode() {
        // TODO Auto-generated method stub
        return ip.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        // TODO Auto-generated method stub
        if (obj instanceof PeerAddress) {
            PeerAddress other = (PeerAddress) obj;
            return ip.equals(other.ip) && port == other.port;
        }
        return false;
    }
}
