import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;


/**
 * TftpClient class.
 *
 * This class extends Thread.
 * Describes the client-side behaviour of Trivial File Transfer Protocol.
 *
 * The protocol is based on a simplified version of TFTP RFC 1350.
 *
 * @see 	Tftp
 * @see 	TftpServer
 */
public class TftpClient extends Thread {

	private InetAddress addr;
	private String file;
	private Path path;

	/**
	 * TftpClient constructor.
	 * Sets this Path object by the String directory name passed in.
	 *
	 * @param   addr    The InetAddress of the current TftpServer.
	 * @param   file    The name of the file to request.
	 * @param   dir 	The name of the output directory.
	 */
	public TftpClient(InetAddress addr, String file, String dir) throws IOException {

		this.addr = addr;
		this.file = file;
		this.path = Tftp.setLocalPath(dir);
	}


	/**
	 * Entry point main.
	 *
	 * Starts the main Thread of TftpClient. Creates a new instance, and passes
	 * it the InetAddress of TftpServer and the name of requested file.
	 *
	 * If an additional argument is provided, then sets it to the new output
	 * directory to override the default.
	 *
	 * @param   args  	The arguments, user input from command line.
	 */
	public static void main(String[] args){

		if (args.length > 1) {
			System.out.println("\nWelcome, TftpClient!\n");

			try {
				InetAddress addr = InetAddress.getByName(args[0]);
				String file = args[1];
				String dir = Tftp.OUT_DIR;

				/* Set a new folder name if desirded */
				if (args.length > 2) {
					dir = args[2];
				}

				TftpClient client = new TftpClient(addr, file, dir);
				client.start();
				client.join(); /* Waits for the process to finish completely */

			} catch (InterruptedException e) {
				System.out.println("InterruptedException: " + e.getMessage());
			} catch (IOException e) {
				System.out.println("IOException: " + e.getMessage());
			}finally {
				System.out.println("Closing TftpClient...\n");
			}
		} else {
			System.out.println("Incorrect length of arguments...");
			System.out.println("To download to default directory:");
			System.out.println("$ java TftpClient <IP Address> <File Name>\n");
			System.out.println("To change the name of the directory:");
			System.out.println("$ java TftpClient <IP Address> <File Name> <Output Folder>\n");
		}
	}


	/**
	 * Runs the TftpClient process on start.
	 *
	 * Opens a DatagramSocket in the try-with-resources block, then sends an RRQ
	 * DatagramPacket to TftpServer to request the file.
	 *
	 * Downloads the file to the specified target, and prints a message output
	 * was successful.
	 */
	@Override
	public void run() {

		try (
			DatagramSocket socket = new DatagramSocket();
		) {
			/* Resolve the local path for target download */
			Path target = path.resolve(file);

            System.out.println("\tYou requested:");
            System.out.println("\tFile:\t" + file);
            System.out.println("\tPath:\t" + target);
            System.out.println("\nWaiting for download...\n");

            /* Sends the request to TftpServer */
            socket.send(Tftp.rrqPacket(file, addr));

            /* Downloads the file and checks if succesful */
            if (downloadFile(socket, target)) {
            	System.out.println("\nDownload complete!\n");
            } else {
            	System.out.println("\nDownload failed...\n");
            }
		} catch (IOException e) {
         	System.out.println("File Not Found" + e.getMessage());
        } catch (Exception e) {
        	System.out.println("Fatal Error: " + e.getMessage());
        } finally {
        	System.out.println("\nClosing TftpServer socket...");
        }
	}


	/**
	 * Downloads the file through the socket and to the target.
	 *
	 * The file is downloaded through one block at a time, each on one iteration
	 * of the infinite loop, from predefined blocks that were packaged by
	 * TftpServer prior to transferring.
	 *
	 * TftpClient sends an ACK for the block received, which tells TftpServer
	 * to send the next.
	 *
	 * @param   socket  The socket
	 * @param   target  The target
	 * @return  True if the download was successful, False otherwise.
	 * @throws  IOException
	 */
	public boolean downloadFile(DatagramSocket socket, Path target) throws IOException {

        try (
        	FileOutputStream fos = new FileOutputStream(target.toFile());
        	BufferedOutputStream bos = new BufferedOutputStream(fos);
        ) {
        	/* The packet that will receive the data */
            DatagramPacket pkt = new DatagramPacket(new byte[Tftp.BUFFER], Tftp.BUFFER);

            int block = 0;

            while (true) {

            	socket.setSoTimeout(Tftp.WAIT); /* Waits until timeout limit */
            	socket.receive(pkt); 			/* Receives a packet of data */

            	if (pkt.getLength() == 0) {
            		System.out.println("\r\nTotal " + block + " packets received.\r\n");
            		return true;
            	}

            	InetAddress addr = pkt.getAddress();
            	int port = pkt.getPort();
            	byte[] data = pkt.getData();	/* The array of all data */
            	byte type = data[0]; 			/* The Op code byte */

            	/*
            	 * The following code handles the packet based on the Op Code.
            	 *
            	 * If a DATA packet, then calls the method to process the block.
            	 * If an ACK packet, then assumes the file download is complete.
            	 * If an ERROR packet, then gets the message and prints it.
            	 *
            	 * Note: 	I have decided to exclude implementation of TID
            	 * 			for the sake of simplicity. The server should
            	 * 			be responsible for making sure the download is
            	 * 			complete and isn't corrupted.
            	 */
            	if (type == Tftp.DATA) {
            		/* Calculates expected next based on the previous block */
            		int next = block + 1;
            		System.out.print("\t\rDownloaded " + next  + " packets...\r");

            		/* Get the next block number after attempting to process */
            		block = processData(bos, data, next);

            		/* Send an ACK of the block to get the next expected */
	                socket.send(Tftp.ackPacket(block, addr, port));

	                Thread.sleep(Tftp.PAUSE); /* Pause to view the output */

            	} else if (type == Tftp.ERROR) {
            		System.out.println("From TftpServer: " + Tftp.getString(data));
            		return false;
            	}
            }
        } catch (InterruptedException e) {
            throw new IOException("Thread was interrupted unexpectedly.");
       	}
    }


    /**
     * Processes the given block, if the block is the expected number.
     *
     * Checks if the received block is the expected next block, then writes the
     * bytes of the file to the stream before returning the next block number.
     * If not then skips the download and returns current expected block number.
     *
     * The start and range is based on {@link Tftp#HEADER_SIZE}
     *
     * @param   bos 		The BuffferedOutputStream to write the bytes to.
     * @param   data 		The full byte array of the packet to extract from.
     * @param   next        The expected block number, the next one to download.
     * @return  The int number of block received.
     * @throws  IOException
     */
    private int processData(BufferedOutputStream bos, byte[] data, int next) throws IOException {

    	int received = data[1] & 0xFF; 	/* Convert the block byte to an int */
    	if (received != next) {
    		return received;
    	} else {
    		int start = Tftp.HEADER;
    		int range = data.length - Tftp.HEADER;

    		bos.write(data, start, range);
    		return next;
    	}
    }
}
