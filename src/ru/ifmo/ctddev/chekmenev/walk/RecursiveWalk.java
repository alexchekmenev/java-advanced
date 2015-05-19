package ru.ifmo.ctddev.chekmenev.walk;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RecursiveWalk {
    private static int getFileHash(File file) {
        if (!file.exists()) {
            System.err.println("File or directory \"" + file.getPath() + "\" from input file not found");
            return 0;
        }
        int fileHash = 0;
        try (final FileInputStream fileInputStream = new FileInputStream(file)) {
            try {
                int ch;
                fileHash = 0x811c9dc5;
                while ((ch = fileInputStream.read()) != -1) {
                    fileHash *= 0x01000193;
                    fileHash ^= ch;
                }
            } catch (IOException e) {
                System.err.println("Something wrong with reading of " + file.getAbsolutePath());
                return 0;
            }
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
        } catch (IOException e) {
            System.err.println("IO error");
        }
        return fileHash;
    }

    public static void main(String args[]) {
        if (args == null || args.length < 2 || args[0] == null || args[1] == null) {
            System.err.println("Input or output don't exist");
            return;
        }

        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(args[0]), "UTF-8"));
             final Writer writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(args[1]), "UTF-8"));
        ) {
            boolean running = true;
            while (running) {
                try {
                    String dirOrFileName = reader.readLine();
                    if (dirOrFileName == null) {
                        running = false;
                        break;
                    }
                    if (!Files.exists(Paths.get(dirOrFileName))) {
                        writer.write(String.format("%08x", 0) + " " + dirOrFileName + "\n");
                    } else {
                        Files.walk(Paths.get(dirOrFileName))
                                .filter(Files::isRegularFile)
                                .forEach(filePath -> {
                                    try {
                                        File file = filePath.toFile();
                                        writer.write(String.format("%08x", getFileHash(file)) + " " + file.getPath() + "\n");
                                    } catch (IOException e) {
                                        System.err.println("Something wrong with writing in output");
                                    }
                                });
                    }
                } catch (IOException e) {
                    System.err.println("Something wrong with reading from input");
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("Input file " + args[0] + " don't exist");
        } catch (IOException e) {
            System.err.println("IO error");
        } catch (NullPointerException e) {
            System.err.println("Type names of input and output files");
        }
    }
}
