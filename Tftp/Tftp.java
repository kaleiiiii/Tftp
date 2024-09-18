import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Tftp {

    public static final String SRC_DIR = "server";
    public static final String OUT_DIR = "client";

    public static final byte RRQ = 1;
    public static final byte DATA = 2;
    public static final byte ACK = 3;
    public static final byte ERROR = 4;

    public static final int LIMIT = 131072;
    public static final int BUFFER = 514;
    public static final int OFFSET = 1;
    public static final int HEADER_SIZE = OFFSET + OFFSET;
    public static final int BLOCK_SIZE = BUFFER - HEADER_SIZE;
   
    public static final int PORT = 69;
    public static final int MAX_ATTEMPTS = 5;
    public static final int TIMEOUT = 20000;
    public static final int WAIT = 1000;
    public static final int PAUSE = 50;

   	public static Path setLocalPath(String directory) throws IOException {

        Path localPath = Paths.get(directory);
        if (Files.exists(localPath)) {
            return localPath;
        } else {
            try {
                Files.createDirectories(localPath);
                return localPath;
            } catch (IOException e) {
                throw new IOException("Could not resolve a local directory.");
            }
        }
    } 

    public static int getTotalBlocks(int fileSize) {

    	return (fileSize + BLOCK_SIZE - 1) / BLOCK_SIZE;
    }

    public static DatagramPacket rrqPacket(String fileName, InetAddress addr) throws IOException {

        byte[] msg = fileName.getBytes();
        byte[] pkt = new byte[msg.length + 1];

        pkt[0] = RRQ;
        System.arraycopy(msg, 0, pkt, 1, msg.length);

        return new DatagramPacket(pkt, pkt.length, addr, PORT);
    }

    public static DatagramPacket[] dataPacket(byte[] file, InetAddress addr, int port) throws IOException {

        int sent = 0;
        int size = file.length;
        int totalBlocks = getTotalBlocks(size);

        DatagramPacket[] packets = new DatagramPacket[totalBlocks];

        for (int block = 0; block < totalBlocks; block++) {

            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            int remaining = size - sent;
            int toSend = Math.min(remaining, BLOCK_SIZE);

            bos.write(DATA);
            bos.write((byte)(block + 1));
            bos.write(file, sent, toSend);

            byte[] pkt = bos.toByteArray();
            packets[block] = new DatagramPacket(pkt, pkt.length, addr, port);

            sent += toSend;
        }
        return packets;
    }

    public static DatagramPacket ackPacket(int block, InetAddress addr, int port) throws IOException {

        byte[] pkt = {ACK, (byte)block};
        return new DatagramPacket(pkt, pkt.length, addr, port);
    }

    public static DatagramPacket errorPacket(String message, InetAddress addr, int port) throws IOException {

        byte[] msg = message.getBytes();
        byte[] pkt = new byte[msg.length + 1];

        pkt[0] = ERROR;
        System.arraycopy(msg, 0, pkt, 1, msg.length);

        return new DatagramPacket(pkt, pkt.length, addr, port);
    }

    public static String getString(byte[] data) throws IOException {

    	int len = data.length - OFFSET;
    	byte[] msg = new byte[len];

    	for (int i = 0; i < len; i++) {
    		msg[i] = data[i + OFFSET];
    	}

        String message = new String(msg);
    	return message.trim();
    }
}
