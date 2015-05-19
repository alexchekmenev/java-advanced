package ru.ifmo.ctddev.chekmenev.hello;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class HelloUDPClient {
    private static final int SERVER_PORT = 8031, BUFFER_SIZE = 1024;
    private static InetAddress serverAddress;
    private static int serverPort, threadCount, queriesInThread;
    private static String prefix;

    public static class Query implements Callable<String>{
        InetAddress serverAddress;
        int serverPort, queriesCount, threadNumber;
        String prefix;

        Query(int threadNumber, InetAddress serverAddress, int serverPort, String prefix, int queriesCount) {
            this.threadNumber = threadNumber;
            this.serverAddress = serverAddress;
            this.serverPort = serverPort;
            this.prefix = prefix;
            this.queriesCount = queriesCount;
        }
        @Override
        public String call() throws Exception {
            StringBuilder totalReceived = new StringBuilder();
            try(DatagramSocket clientSocket = new DatagramSocket()) {
                for(int i = 0; i < queriesCount; i++) {
                    String query = this.prefix + this.threadNumber + "_" + i;
                    System.out.println(query);
                    byte[] queryBytes = query.getBytes(), receiveData = new byte[BUFFER_SIZE];

                    DatagramPacket sendPacket = new DatagramPacket(queryBytes, query.length(), serverAddress, serverPort);
                    clientSocket.send(sendPacket);

                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    clientSocket.receive(receivePacket);

                    String received = new String(receivePacket.getData());
                    System.out.println("received["+receiveData.length+"]:" + received);
                    totalReceived.append(received+"\n");
                }
                return totalReceived.toString();
            } catch (Exception e) {
                totalReceived.append("Exception in thread[" + threadNumber + "]:" + e.getMessage());
            }
            return totalReceived.toString();
        }
    }

    public static void start(String serverIp, int _serverPort, String _prefix, int _threadCount, int _queriesInThread) {
        try {

            serverPort = _serverPort;
            prefix = _prefix;
            queriesInThread = _queriesInThread;
            threadCount = _threadCount;
            serverAddress = InetAddress.getByName(serverIp);

            ExecutorService pool = Executors.newFixedThreadPool(threadCount);
            Set<Future<String>> set = new HashSet<Future<String>>();
            for (int i = 0; i < threadCount; i++) {
                Callable<String> callable = new Query(i + 1, serverAddress, serverPort, prefix, queriesInThread);
                Future<String> future = pool.submit(callable);
                set.add(future);
            }
            for (Future<String> future : set) {
                System.out.println(future.get());
            }
        } catch (Exception e) {
            System.err.println("Exception: "+e.getMessage());
        }
    }
}
