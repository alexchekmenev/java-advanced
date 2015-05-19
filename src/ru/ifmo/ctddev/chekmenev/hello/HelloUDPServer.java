package ru.ifmo.ctddev.chekmenev.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.*;

public class HelloUDPServer implements HelloServer {

    private static ArrayList<Thread> threads = new ArrayList<Thread>();
    private boolean isRunning = false;

    class Handler implements Callable<String>{
        DatagramPacket packet;
        DatagramSocket serverSocket;

        Handler(DatagramSocket serverSocket, DatagramPacket packet) {
            this.packet = packet;
            this.serverSocket = serverSocket;
        }
        @Override
        public String call() throws Exception {
            InetAddress IPAddress = packet.getAddress();
            int port = packet.getPort();
            String request = new String(packet.getData());
            String response = "Hello, " + request;
            DatagramPacket sendPacket = new DatagramPacket(response.getBytes(), response.length(), IPAddress, port);
            serverSocket.send(sendPacket);
            return response;
        }
    }

    public void start(int serverPort, int threadCount) {
        //System.out.println("Start...");
        //System.out.println(threadCount + " " + threadCount);
        final int BUFFER_SIZE = 1024;
        Thread t = (new Thread(new Runnable() {
            @Override
            public void run() {
                try(DatagramSocket serverSocket = new DatagramSocket(serverPort)) {
                    ExecutorService pool = Executors.newFixedThreadPool(threadCount);
                    //HashSet<Future<String>> set = new HashSet<>();
                    byte[] receiveData = new byte[BUFFER_SIZE];
                    isRunning = true;
                    while(isRunning) {
                        //System.out.println("running...");
                        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                        serverSocket.receive(receivePacket);
                        Callable<String> callable = new Handler(serverSocket, receivePacket);
                        Future<String> future = pool.submit(callable);
                        //set.add(future);
                        try {
                            future.get();
                        } catch (InterruptedException | ExecutionException e) {
                            //System.err.println("Exception: "+e.getCause());
                            //e.printStackTrace();
                            isRunning = false;
                        }
                    }
                } catch(IOException e) {
                    System.err.println("Exception: "+e.getCause());
                    e.printStackTrace();
                }
            }
        }));
        if (t != null && !t.isInterrupted()) {
            threads.add(t);
            t.start();
        }
    }

    @Override
    public void close() {
        //System.out.println("Close...");
        isRunning = false;
        for(int i = 0; i < threads.size(); i++) {
            if (!threads.get(i).isInterrupted()) {
                //System.out.println("Thread["+i+"] interrupted");
                threads.get(i).interrupt();
            }
        }

    }
}
