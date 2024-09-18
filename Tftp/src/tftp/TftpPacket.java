import java.net.DatagramPacket;
import java.net.InetAddress;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * This class describes a TFTP packet.
 * Provides methods to create various types of TFTP packets according to RFC 1350.
 */
public class TftpPacket {

    /**
     * Creates a DatagramPacket for a request.
     * 
     * A request could be of type {@link Op#WRQ} to write the file, or
     * {@link Op#RRQ} to download it.
     * 
     * Note:    The constants defined in {@link Tftp} give the packet's MODE,
     *          and initial PORT.
     *
     * @param   type      The Op Code, the type of this request.
     * @param   fileName  The String name of the file being requested.
     * @param   address   The InetAddress, the destination of this request.
     * @return  The DatagramPacket request of the specified TftpOpCode type.
     * @throws  IOException if an error occurs on ByteArrayOutputStream.
     */
    public static DatagramPacket request(Op type, String fileName, InetAddress ip) throws IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        out.write(type.code());
        out.write(fileName.getBytes(Tftp.ENCODING));
        out.write(0);
        out.write(Tftp.MODE.getBytes(Tftp.ENCODING));
        out.write(0);

        byte[] bytes = out.toByteArray();
        return new DatagramPacket(bytes, bytes.length, ip, Tftp.PORT);
    }

    /**
     * Creates a DatagramPacket for an acknowledgment.
     * 
     * Constructs a packet that acknowledges a specific block number.
     * Used to acknowledge receipt of data packets as per TFTP RFC 1350.
     *
     * @param   n       The int block number, the packet being acknowledged.
     * @param   ip      The InetAddress, the destination of this ACK packet.
     * @param   port    The int port number to send the ACK packet to.
     * @return  The DatagramPacket that acknowledges the specified block number.
     * @throws  IOException if an error occurs on ByteArrayOutputStream.
     */
    public static DatagramPacket ack(int n, InetAddress ip, int port) throws IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        out.write(Op.ACK.code());
        out.write(block(n));

        byte[] bytes = out.toByteArray();
        return new DatagramPacket(bytes, bytes.length, ip, port);
    }

    /**
     * Creates a DatagramPacket for an error.
     * Constructs a packet that contains an error message and block number.
     * @param   e       The String message describing the error.
     * @param   n       The integer block number associated with the error.
     * @param   ip      The InetAddress, the destination of this error packet.
     * @param   port    The integer port number to send the error packet to.
     * @return  The DatagramPacket representing the error message and block number.
     * @throws  IOException if an error occurs on ByteArrayOutputStream.
     */
    public static DatagramPacket error(String e, int n, InetAddress ip, int pn) throws IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        out.write(Op.ERROR.code());
        out.write(block(n));
        out.write(message.getBytes(Tftp.ENCODING));
        out.write(0);

        byte[] bytes = out.toByteArray();
        return new DatagramPacket(bytes, bytes.length, ip, port);
    }

    /**
     * Creates an array of DatagramPackets for file transfer.
     * Divides the file bytes into multiple packets according to TFTP block size.
     *
     * @param   fileBytes   The byte array containing the file data.
     * @param   address     The InetAddress, the destination for each data packet.
     * @param   port        The integer port number to send the data packets to.
     * @return  An array of DatagramPackets representing the file data.
     * @throws  IOException if an error occurs on ByteArrayOutputStream.
     */
    public static DatagramPacket[] getPackets(byte[] fileBytes, InetAddress address, int port) throws IOException {

        byte[] dataHeader = TftpOpCode.DATA.toHeader();
        int totalBytes = fileBytes.length;
        int totalBlocks = TftpBlock.getCountFrom(totalBytes);
        DatagramPacket[] packets = new DatagramPacket[totalBlocks];

        int blockNumber = 1;
        int sentBytes = 0;
        while (sentBytes < totalBytes) {
            byte[] blockHeader = TftpBlock.getHeaderFrom(blockNumber);
            int toSend = Math.min(totalBytes - sentBytes, Tftp.BLOCK_SIZE);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(dataHeader);
            out.write(blockHeader);
            out.write(fileBytes, sentBytes, toSend);

            byte[] packet = out.toByteArray();
            packets[blockNumber - 1] = new DatagramPacket(packet, packet.length, address, port);

            sentBytes += toSend;
            blockNumber++;
        }
        return packets;
    }

    /**
     * Converts a block number to bytes.
     * @param   number  The block number to be converted.
     * @return  A byte array containing the block number.
     */
    public static byte[] block(int number) {

        byte[] block = new byte[Tftp.OFFSET];
        block[Tftp.OFFSET - 1] = (byte)(number & 0xFF);
        if (Tftp.OFFSET > 1) {
            block[0] = (byte)((number >> 8) & 0xFF);
        }
        return block;
    }
}
