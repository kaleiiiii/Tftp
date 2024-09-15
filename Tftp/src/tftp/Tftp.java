import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Tftp class.
 *
 * Tftp defines the constants used throughout this package,
 * and provides static methods to support file handling and formatting.
 *
 * This class cannot be instantiated.
 *
 * @see tftp.TftpServer
 * @see tftp.TftpClient
 * @see tftp.TftpPacket
 * @see tftp.TftpOpCode
 * @see tftp.TftpBlock
 */
public final class Tftp {

    public static final int PORT = 69;
    public static final int TIMEOUT = 5000;

    /* Settings for 1-byte block number */
    // public static final int FILE_SIZE = 255;
    // public static final int OFFSET = 1;

    /* Settings for 2-byte block number */
    public static final int FILE_SIZE = 65535;
    public static final int OFFSET = 2;

    public static final int BLOCK_SIZE = 512;
    public static final int HEADER_SIZE = OFFSET * 2;
    public static final int BUFFER = HEADER_SIZE + BLOCK_SIZE;
    public static final int MAX_BYTES = FILE_SIZE * BLOCK_SIZE;

    public static final String ENCODING = "ASCII";
    public static final String MODE = "octet";
    public static final byte EOF = 0;

    public static final String SRC_DIR = "server";
    public static final String OUT_DIR = "downloads";

    /**
     * Gets the file, from the specified Path, as a byte array.
     * Reads the bytes of the file, and checks it doesn't exceed maximum size.
     *
     * @see     #MAX_BYTES
     * @param   filePath    The Path the file is located at.
     * @return  The byte array containing the file bytes.
     * @throws  IOException
     */
    public static byte[] getFileBytes(Path filePath) throws IOException {

        byte[] fileBytes = Files.readAllBytes(filePath);
        if (fileBytes.length <= MAX_BYTES) {
            return fileBytes;
        } else {
            throw new IOException("File was too large.");
        }
    }

    /**
     * Gets the Path of the directory that the specified file exists in.
     *
     * @param   directory   The Path that represents the file's parent directory.
     * @param   fileName    The String name of the file to look for.
     * @return  The Path the file is located in, or null if non-existent.
     * @throws  IOException
     */
    public static Path getFilePath(Path directory, String fileName) throws IOException {

        Path filePath = directory.resolve(fileName);
        if (Files.exists(filePath)) {
            return filePath;
        } else {
            throw new IOException("File not found in this directory.");
        }
    }

    /**
     * Gets the requested file name from the request packet.
     * Checks the type of request is valid, then extracts the file name.
     *
     * @param   requestBytes    The byte array of the request packet.
     * @return  The String name of the file that was requested.
     * @throws  IOException
     */
    public static String getFileName(byte[] requestBytes) throws IOException {

        Op code = Op.codeType(requestBytes);
        if (code.isType(Op.RRQ) || code.isType(Op.WRQ)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int index = OFFSET;
            int end = requestBytes.length;
            while (index < end && requestBytes[index] != EOF) {
                out.write(requestBytes[index]);
                i++;
            }
            return out.toString(ENCODING);
        }
        throw new IOException("Failed to get the file name from the packet.");
    }

    /**
     * Sets the Path of a local host's directory.
     * Creates a Path object with the specified directory name, then returns the
     * Path if it exists, else creates a new directory before returning the Path.
     *
     * @param   directoryName   The String name of the host's directory folder.
     * @return  The Path object that was set for the local host.
     * @throws  IOException
     */
    public static Path setLocalPath(String directoryName) throws IOException {

        Path localPath = Paths.get(directoryName);
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
     * Op enum.
     * Defines the type of a packet in TFTP RFC 1350.
     *
     * This enum provides static methods to both create and check packet OP Codes.
     * Functions depend on protocol settings defined by package constants.
     *
     * @see {@link Tftp}
     */
    enum Op {

        RRQ((byte)1),
        WRQ((byte)2),
        DATA((byte)3),
        ACK((byte)4),
        ERROR((byte)5);
        private final byte value;

        /**
         * Op Enum constructor.
         * @param   opcode    The byte that represents this OP Code.
         */
        Op(byte value) {
            this.value = value;
        }

        /**
         * @return  The byte that represents this Op Code.
         */
        private byte codeByte() {
            return this.value;
        }

        /**
         * Converts this Op Code to a byte array.
         * Note:    The first value will be 0 if OFFSET is set to 1 byte.
         * @return  A byte array containing the header for this Op Code.
         */
        public byte[] code() {

            byte[] array = new byte[OFFSET];
            array[0] = 0;
            array[OFFSET - 1] = this.value;
            return array;
        }

        /**
         * Gets the Op Code, the type of the packet that was sent.
         * Note:    A packet's length must be at least the size of OFFSET, but 
         *          not the full HEADER_SIZE, since not all packets will have
         *          a block number.
         *
         * @param   packetBytes     The byte array of the packet to check.
         * @return  The Op Code object that represents the type of packet.
         * @throws  IOException
         */
        public static Op codeType(byte[] packetBytes) throws IOException {

            if (packetBytes == null || packetBytes.length < OFFSET) {
                throw new IOException("Incorrect format of packet bytes.");
            } else {
                byte codeByte = packetBytes[OFFSET - 1];
                for (Op type : Op.values()) {
                    if (type.toByte() == codeByte) {
                        return type;
                    }
                }
                throw new IOException("Unrecognised OP Code: " + codeByte);
            }
        }

        /**
         * Determines whether the specified type is this Op Code.
         * @param   expectedType    The TftpOpCode, the expected type of packet.
         * @return  True if the specified type matches this type, False otherwise.
         */
        public boolean isType(TftpOpCode expectedType) {
            return this.value == expectedType.codeByte();
        }

        /**
         * Private Constructor.
         * Prevents this package class from instantiation.
         */
        private Tftp() {}
}
