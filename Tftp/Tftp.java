import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * Tftp class.
 *
 * Provides constants for the TftpServer and TftpClient program.
 *
 * This class has public static methods to support operations for Trivial File
 * Transfer between a server and client.
 *
 * The protocol is based on a simplified version of TFTP RFC 1350.
 *
 * @see     Tftp
 * @see     TftpServer
 * @see     TftpClient
 */
public class Tftp {

    public static final String SRC_DIR = "server";
    public static final String OUT_DIR = "client";
    public static final String ENCODING = "ASCII";

    public static final byte RRQ = 1;           /* Request packet */
    public static final byte DATA = 2;          /* File data packet */
    public static final byte ACK = 3;           /* Acknowledge packet */
    public static final byte ERROR = 4;         /* Error message packet */

    public static final int BUFFER = 512;       /* Standard TFTP Buffer */
    public static final int LIMIT = 131072;     /* Limit for 1-byte headers */
    public static final int OFFSET = 1;         /* Offset for 1-byte headers */

    public static final int HEADER = OFFSET * 2;
    public static final int BLOCK = BUFFER - HEADER;

    public static final int PORT = 69;          /* Default listening port */
    public static final int ATTEMPTS = 5;       /* Max tries to send a block */
    public static final int TIMEOUT = 20000;    /* Timeout socket connection */
    public static final int WAIT = 1000;        /* Timeout for response wait */
    public static final int PAUSE = 50;         /* Power-nap for performance */

    /**
     * Tftp private constructor.
     * This prevents class Tftp from being instantiated.
     */
    private Tftp(){}


   	/**
     * Sets the path of the host's local directory.
     *
     * @param   directory   The directory to resolve, the folder name.
     * @return  The local path that exists, or newly created.
     * @throws  IOException
     */
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


    /**
     * Gets the Path of file that was requested.
     *
     * @param   directory   The directory the file is located in.
     * @param   fileName    The file name to find in the folder.
     * @return  The Path object that represents the file's path.
     * @throws  IOException
     */
    public static Path getFilePath(Path directory, String fileName) throws IOException {

        Path filePath = directory.resolve(fileName);
        if (Files.exists(filePath)) {
            return filePath;
        } else {
            throw new IOException("Could not resolve the file path.");
        }
    }


    /**
     * Gets the bytes of file that was requested.
     *
     * @param   filePath   The Path that describes where the file is located.
     * @return  The array of bytes containing the file, if under the LIMIT.
     * @throws  IOException
     */
    public static byte[] getFileBytes(Path filePath) throws IOException {

        byte[] fileBytes = Files.readAllBytes(filePath);
        int size = fileBytes.length;
        if (size < LIMIT) {
            return fileBytes;
        } else {
            throw new IOException("Size exceeded limit " + size + " > " + LIMIT);
        }
    }


    /**
     * Calculates total blocks for a file.
     *
     * @see     #BLOCK
     * @param   fileSize    The int size, the length of file bytes.
     * @return  The local path that exists, or newly created.
     */
    public static int getTotalBlocks(int fileSize) {

        return (fileSize + BLOCK - 1) / BLOCK;
    }


    /**
     * Creates an RRQ packet for the file name requested.
     *
     * @param   fileName    The String file name to request.
     * @param   addr        The InetAddress of the TftpServer.
     * @return  The DatagramPacket format for an RRQ.
     * @throws  IOException
     */
    public static DatagramPacket rrqPacket(String fileName, InetAddress addr) throws IOException {

        byte[] msg = fileName.getBytes();
        byte[] pkt = new byte[msg.length + 1];

        pkt[0] = RRQ;
        System.arraycopy(msg, 0, pkt, OFFSET, msg.length);

        return new DatagramPacket(pkt, pkt.length, addr, PORT);
    }


    /**
     * Creates a DATA packet array for the file to be sent as.
     *
     * For the total amount of blocks required, creates a DatagramPacket for
     * each one that contains its block number and partition of file bytes.
     *
     * @param   file    The file byte array to divide and package.
     * @param   addr    The InetAddress of the TftpServer.
     * @param   port    The int port that the packets will be sent over.
     * @return  The array of formatted DatagramPackets containing file blocks.
     * @throws  IOException
     */
    public static DatagramPacket[] dataPacket(byte[] file, InetAddress addr, int port) throws IOException {

        int sent = 0;
        int size = file.length;
        int totalBlocks = getTotalBlocks(size);

        DatagramPacket[] packets = new DatagramPacket[totalBlocks];

        for (int block = 0; block < totalBlocks; block++) {

            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            int remaining = size - sent;
            int toSend = Math.min(remaining, BLOCK);

            bos.write(DATA);                /* Writing DATA Op Code */
            bos.write((byte)(block + 1));   /* Writing block number */
            bos.write(file, sent, toSend);  /* Writing part of file bytes */

            byte[] pkt = bos.toByteArray(); /* Get the new array from stream */
            packets[block] = new DatagramPacket(pkt, pkt.length, addr, port);

            sent += toSend;
        }
        return packets;
    }


    /**
     * Creates an ACK packet with the int block number to acknowledge.
     * Note: The block number is cast as a byte.
     *
     * @param   block   The int block number to acknowledge.
     * @param   addr    The InetAddress of the destination.
     * @param   port    The port to send the packet through.
     * @return  The DatagramPacket format for an ACK packet with a block number.
     * @throws  IOException
     */
    public static DatagramPacket ackPacket(int block, InetAddress addr, int port) throws IOException {

        byte[] pkt = {ACK, (byte)block};
        return new DatagramPacket(pkt, pkt.length, addr, port);
    }


    /**
     * Creates an ERROR packet with the String message to send.
     *
     * @param   message The String error message to send with the packet.
     * @param   addr    The InetAddress of the destination.
     * @param   port    The port to send the packet through.
     * @return  The DatagramPacket format for an ERROR packet with a message.
     * @throws  IOException
     */
    public static DatagramPacket errorPacket(String message, InetAddress addr, int port) throws IOException {

        byte[] msg = message.getBytes();
        byte[] pkt = new byte[msg.length + OFFSET];

        pkt[0] = ERROR;
        System.arraycopy(msg, 0, pkt, OFFSET, msg.length);

        return new DatagramPacket(pkt, pkt.length, addr, port);
    }


    /**
     * Gets the String message of a 1-byte RRQ or ERROR packet.
     *
     * @param   data    The byte array of data from the DatagramPacket.
     * @return  The String message, extracted from the packet's array.
     * @throws  IOException
     */
    public static String getString(byte[] data) throws IOException {

    	int len = data.length - OFFSET;
    	byte[] msg = new byte[len];

    	for (int i = 0; i < len; i++) {
    		msg[i] = data[i + OFFSET];
    	}

        String message = new String(msg, ENCODING);
    	return message.trim();
    }
}
