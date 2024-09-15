import java.io.IOException;

/**
 * TftpOpCode enum.
 * Defines the type of a packet in TFTP RFC 1350.
 *
 * This enum provides static methods to both create and check packet OP Codes.
 * Functions depend on protocol settings defined by package constants.
 *
 * @see {@link Tftp}
 */
public enum TftpOpCode {

    RRQ((byte)1),
    WRQ((byte)2),
    DATA((byte)3),
    ACK((byte)4),
    ERROR((byte)5);
    private final byte code;

    /**
     * TftpOpCode Enum constructor.
     *
     * @param   code    The byte that represents this OP Code.
     */
    TftpOpCode(byte code) {
        this.code = code;
    }

    /**
     * Returns a byte representation of the object.
     * This private method supports the public static methods of this enum.
     *
     * @return  The byte that represents this TftpOpCode.
     */
    private byte toByte() {
        return this.code;
    }

    /**
     * Converts this TftpOpCode to a header.
     * The size of the array depends on the size of {@link Tftp#OFFSET}
     *
     * Note:    The first value will be 0 if the offset is set to 1 byte.
     *
     * @return  A byte array containing the header for this OP Code.
     */
    public byte[] toHeader() {

        byte[] header = new byte[Tftp.OFFSET];
        header[0] = 0;
        header[Tftp.OFFSET - 1] = this.code;
        return header;
    }

    /**
     * Gets the TftpOpCode, the type of the packet that was sent.
     * The length of the packet depends on value of {@link Tftp.OFFSET}
     *
     * Note:    A packet's length must be at least the size of OFFSET, but not
     *          the full HEADER_SIZE, since not all packets have block numbers.
     *
     * @param   packetBytes     The byte array of the packet to check.
     * @return  The TftpOpCode object that represents the type of packet.
     * @throws  IOException
     */
    public static TftpOpCode getType(byte[] packetBytes) throws IOException {

        if (packetBytes == null || packetBytes.length < Tftp.OFFSET) {
            throw new IOException("Incorrect format of packet bytes.");
        } else {
            byte code = packetBytes[Tftp.OFFSET - 1];
            for (TftpOpCode type : TftpOpCode.values()) {
                if (type.toByte() == code) {
                    return type;
                }
            }
            throw new IOException("Unrecognised OP Code: " + code);
        }
    }

    /**
     * Determines whether the specified type is this TftpOpCode.
     * Checks if the TftpOpCodes' byte values are equal, returns True if so.
     *
     * @param   expectedType    The TftpOpCode, the expected type.
     * @return  True if the specified type matches this type, False otherwise.
     */
    public boolean isType(TftpOpCode expectedType) {
        return this.code == expectedType.toByte();
    }
}
