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
    public static final String OUT_DIR = "client";

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
     * Converts the specified String to a byte array, with Tftp's encoding.
     *
     * @see     #ENCODING
     * @param   string  The String to convert to bytes, using Tftp's encoding.
     * @return  The byte array, containing the encoded String.
     * @throws  IOException
     */
    public static byte[] getBytesFrom(String string) throws IOException {

        return string.getBytes(ENCODING);
    }

    /**
     * Gets the file, from the specified Path, as a byte array.
     * Reads the bytes of the file, and checks it doesn't exceed maximum size.
     *
     * @see     #MAX_BYTES
     * @param   path    The Path the file is located at.
     * @return  The byte array containing the file bytes.
     * @throws  IOException
     */
    public static byte[] getFileBytesFrom(Path path) throws IOException {

        byte[] fileBytes = Files.readAllBytes(path);
        if (fileBytes.length <= MAX_BYTES) {
            return fileBytes;
        } else {
            throw new IOException("File was too large.");
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

        TftpOpCode type = TftpOpCode.getType(requestBytes);
        if (type.isType(TftpOpCode.RRQ) || type.isType(TftpOpCode.WRQ)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int i = Tftp.OFFSET;
            while (i < requestBytes.length && requestBytes[i] != EOF) {
                out.write(requestBytes[i]);
                i++;
            }
            return out.toString(ENCODING);
        }
        throw new IOException("Failed to get the file name from the packet.");
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
     * Private Constructor.
     * Prevents this package class from instantiation.
     */
    private Tftp() {}
}
