package fr.laas.fape.planning;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Server extends Thread {

    public static final int PORT = 9876;
    public static final String HOST = "localhost";

    public static final LinkedBlockingQueue<String> msgs = new LinkedBlockingQueue<>();

    public void run() {
        String argLine;

        try {
            while((argLine = msgs.poll(1, TimeUnit.HOURS)) != null) {
                System.out.println("Going for: "+argLine);

                String[] args = argLine.split(" ");
                try {
                    Planning.main(args);
                } catch (Exception e) {
                    System.err.println("Arguments were not valid: "+argLine);
                    e.printStackTrace();
                }
            }
        } catch (InterruptedException e) {
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.setDaemon(true);
        server.start();

        try {
            DatagramSocket serverSocket = new DatagramSocket(PORT);
            System.out.println("Listening on port " + PORT);

            boolean exit = false;
            while (!exit) {
                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);
                String data = new String(receivePacket.getData());
                data = data.trim();
                if (data.startsWith("exit")) {
                    exit = true;
                } else {
                    System.out.println("RECEIVED: " + data);
                    msgs.add(data);
                }
            }

        } catch (SocketException e) {
            e.printStackTrace();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        System.out.println("EXITING");
        System.exit(0);
    }
}
