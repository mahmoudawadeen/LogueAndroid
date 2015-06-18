/*
 * Copyright (c) 2014.
 * author: Ionut Damian <damian@informatik.uni-augsburg.de>
 * created: 2014/12/02
 */

package com.logue;

import android.util.Log;

import org.apache.http.conn.util.InetAddressUtils;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Johnny on 02.12.2014.
 */
public class SSIConnector extends Thread {

    final int MAX_MSG_SIZE = 4096;

    DatagramSocket _socket;
    ArrayList<String> _events = new ArrayList<String>();
    boolean _connected = false;
    boolean _terminate = false;

    public SSIConnector(String ip, int port)
    {
        if (ip == null) ip = getIPAddress(true);
        Console.print("setting up socket on " + ip + "@" + port);

        try {
            InetAddress addr = InetAddress.getByName(ip);
            _socket = new DatagramSocket(port, addr);
        } catch (Exception e) {
            e.printStackTrace();
            Console.print("ERROR: cannot create socket");
            return;
        }

        if(_socket == null || !_socket.isBound())
        {
            Console.print("ERROR: cannot bind socket");
            return;
        }

        Console.print("socket ready ("+ip + "@" + port+")");
        _connected = true;

        start();
    }

    @Override
    public void run() {

        byte[] buffer = new byte[MAX_MSG_SIZE];

        //main loop
        while(true && !_terminate)
        {
            try{
                Thread.sleep(10);

                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                _socket.receive(packet);

                if(packet.getLength() == 0)
                    continue;

                _events.add(new String(buffer));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Console.print("connection terminated");
        _connected = false;

        try {
            //close sockets
            _socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void terminate()
    {
        _terminate = true;
    }

    public synchronized String getLastEvent(boolean peek) {

        String ev;
        if(_events.size() == 0)
            return null;
        else
        {
            int last = _events.size()-1;
            ev = _events.get(last);

            if(!peek)
                _events.remove(last);
        }

        return ev;
    }

    /**
     * Get IP address from first non-localhost interface
     * @param useIPv4  true=return ipv4, false=return ipv6
     * @return  address or empty string
     */
    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {

                //for google glass: interface is named wlan0
                String name = intf.getName();
                if(!name.contains("wlan"))
                    continue;

                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress().toUpperCase();
                        boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 port suffix
                                return delim<0 ? sAddr : sAddr.substring(0, delim);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }
}
