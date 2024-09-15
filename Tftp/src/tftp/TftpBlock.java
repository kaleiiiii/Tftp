/**
 * TftpBlock class.
 * Provides static methods to process TFTP block numbers.
 * 
 * Functions depend on protocol settings defined by package constants.
 *
 * @see {@link Tftp}
 */
public class TftpBlock {

    /**
     * Converts a block number to a header.
     * Takes the int block number and converts to a byte array.
     * The size of the array depends on the value of {@link Tftp#OFFSET}
     *
     * @param   blockNumber The block number to be converted.
     * @return  A byte array containing the header representing the block number.
     */
    public static byte[] getHeaderFrom(int blockNumber) {

        int offset = Tftp.OFFSET;
        byte[] header = new byte[offset];
        header[offset - 1] = (byte) (blockNumber & 0xFF);
        if (offset > 1) {
            header[0] = (byte) ((blockNumber >> 8) & 0xFF);
        }
        return header;
    }

    /**
     * Calculates the number of blocks needed for a given file size.
     * The size of a block depends on the value of {@link Tftp#BLOCK_SIZE}
     *
     * @param   fileSize    The size of the file in bytes.
     * @return  The total number of blocks required to store the file.
     */
    public static int getCountFrom(int fileSize) {

        int blocks = fileSize / Tftp.BLOCK_SIZE;
        if (fileSize % Tftp.BLOCK_SIZE != 0) {
            blocks++;
        }
        return blocks;
    }

    /**
     * Checks if the header of a packet matches the expected block number.
     * The size of a packet depends on the value of {@link Tftp#HEADER_SIZE}
     *
     * @param   packetBytes     The byte array of the packet to check.
     * @param   expectedBlock   The expected block number of the packet.
     * @return  True if the packet has the expected block, False otherwise.
     */
    public static boolean checkHeader(byte[] packetBytes, int expectedBlock) {

        if (packetBytes == null || packetBytes.length < Tftp.HEADER_SIZE) {
            return false;
        } else {
            int upperByte = (packetBytes[Tftp.OFFSET] & 0xFF) << 8;
            int lowerByte = (packetBytes[Tftp.OFFSET + 1] & 0xFF);
            int blockNumber = (upperByte | lowerByte);
            return blockNumber == expectedBlock;
        }
    }
}
