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
     * Formats the packet for a requested file, based on TFTP RFC 1350.
     *
     * A request could be of type {@link TftpOpCode#WRQ} to write the file, or
     * {@link TftpOpCode#RRQ} to download it.
     *
     * Note:    The constants defined in {@link Tftp} give the packet's MODE,
     *          initial PORT, and EOF.
     *
     * @param   type      The TftpOpCode, the type of this request.
     * @param   fileName  The String name of the file being requested.
     * @param   address   The InetAddress, the destination of this request.
     * @return  The DatagramPacket request of the specified TftpOpCode type.
     * @throws  IOException if an error occurs on ByteArrayOutputStream.
     */
    public static DatagramPacket request(TftpOpCode type, String fileName, InetAddress address) throws IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(type.toHeader());
        out.write(Tftp.getBytesFrom(fileName));
        out.write(Tftp.EOF);
        out.write(Tftp.getBytesFrom(Tftp.MODE));
        out.write(Tftp.EOF);

        byte[] bytes = out.toByteArray();
        return new DatagramPacket(bytes, bytes.length, address, Tftp.PORT);
    }

    /**
     * Creates a DatagramPacket for an acknowledgment.
     * Constructs a packet that acknowledges a specific block number.
     *
     * Used to acknowledge receipt of data packets as per TFTP RFC 1350.
     *
     * @param   blockNumber The int block number being acknowledged.
     * @param   address     The InetAddress, the destination of this ACK packet.
     * @param   port        The int port number to send the ACK packet to.
     * @return  The DatagramPacket that acknowledges the specified block number.
     * @throws  IOException if an error occurs on ByteArrayOutputStream.
     */
    public static DatagramPacket ack(int blockNumber, InetAddress address, int port) throws IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(Op.ACK.toHeader());
        out.write(TftpBlock.getHeaderFrom(blockNumber));

        byte[] bytes = out.toByteArray();
        return new DatagramPacket(bytes, bytes.length, address, port);
    }

    /**
     * Creates a DatagramPacket for an error.
     * Constructs a packet that contains an error message and block number.
     *
     * Used to notify the recipient of an error as per TFTP RFC 1350.
     *
     * @param   message     The String message describing the error.
     * @param   blockNumber The integer block number associated with the error.
     * @param   address     The InetAddress, the destination of this error packet.
     * @param   port        The integer port number to send the error packet to.
     * @return  The DatagramPacket representing the error message and block number.
     * @throws  IOException if an error occurs on ByteArrayOutputStream.
     */
    public static DatagramPacket error(String message, int blockNumber, InetAddress address, int port) throws IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(TftpOpCode.ERROR.toHeader());
        out.write(TftpBlock.getHeaderFrom(blockNumber));
        out.write(Tftp.getBytesFrom(message));
        out.write(Tftp.EOF);

        byte[] bytes = out.toByteArray();
        return new DatagramPacket(bytes, bytes.length, address, port);
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
}
