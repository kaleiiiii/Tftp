import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;


/**
 * TftpServer class.
 *
 * This class extends Thread.
 * Describes the server-side behaviour of Trival File Transfer Protocol.
 *
 * The protocol is based on a simplified version of TFTP RFC 1350.
 * The rules are defined in {@link Tftp} by public constants and static methods.
 *
 * @see 	Tftp
 * @see 	TftpClient
 */
public class TftpServer extends Thread {

    private final InetAddress addr;
    private final Path path;

    /**
     * TftpServer constructor.
     * Resolves this host's local directory.
     *
     * @param 	addr  The address of this TftpServer.
     * @throws 	IOException
     */
    public TftpServer(InetAddress addr) throws IOException {

		this.addr = addr;
		this.path = Tftp.setLocalPath(Tftp.SRC_DIR);
	}


	/**
	 * Entry point main.
	 *
	 * Starts the main Thread of TftpServer. Creates a new instance, and passes
	 * it the InetAddress of TftpServer.
	 *
	 * @param   args  	The arguments should be empty.
	 */
	public static void main(String[] args){

		if (args.length == 0) {
			System.out.println("\nWelcome to local TftpServer!\n");

			try {
				InetAddress addr = InetAddress.getLocalHost();
				TftpServer server = new TftpServer(addr);

				System.out.println("For the remote client, enter something like:");
				System.out.println("$ java TftpClient " + addr.getHostAddress() + " cat.txt");

				server.start();
				server.join(); /* Waits for the process to be complete */

			} catch (InterruptedException e) {
				System.out.println("InterruptedException: " + e.getMessage());
			} catch (IOException e) {
				System.out.println("IOException: " + e.getMessage());
			} finally {
				System.out.println("Closing TftpServer...\n");
			}
		} else {
			System.out.println("Too many arguments...");
			System.out.println("Usage: $ java TftpServer");
		}
	}


	/**
	 * Runs the TftpServer process on start.
	 *
	 * Opens a DatagramSocket in the try-with-resources block, then waits,
	 * until {@link Tftp#TIMEOUT} for a TftpClient's request.
	 *
	 * Handles the request by extracting the data from the RRQ DatagramPacket,
	 * then calls the supporting methods to resolve and package the file.
	 *
	 * @see 	#transfer(DatagramSocket socket, byte[] file, InetAddress addr, int port)
	 */
	@Override
	public void run() {

		try (
			DatagramSocket client = new DatagramSocket(Tftp.PORT);
		) {
			System.out.println("\nWaiting on port " + Tftp.PORT + "...\n");
           	DatagramPacket pkt = new DatagramPacket(new byte[Tftp.BUFFER], Tftp.BUFFER);

           	client.setSoTimeout(Tftp.TIMEOUT);
           	client.receive(pkt);

           	System.out.println("\nRequest received!\n");
            byte[] rrq = pkt.getData();  	/* The data from the request */
            byte type = rrq[0];             /* The byte Op Code to check */

            InetAddress addr = pkt.getAddress();
            int port = pkt.getPort();

            try {
            	/* Checks that the first packet is an RRQ */
	            if (type == Tftp.RRQ) {
	            	String fileName = Tftp.getString(rrq);

	            	/* Checks that the file name exists in the request */
	            	if (fileName != null) {
	            		Path filePath = Tftp.getFilePath(path, fileName);

	            		/* Checks that the file exists in the path */
	            		if (filePath != null) {

	            			/* Gets the byte array of the file */
	            			byte[] file = Tftp.getFileBytes(filePath);

	            			System.out.println("\tFile:\t" + fileName);
	            			System.out.println("\tPath:\t" + filePath);
	            			System.out.println("\tSize:\t" + file.length);

	            			/* Transfer file and checks if successful */
	            			if (transfer(client, file, addr, port)) {
	            				System.out.println("\nFile transfer was successful!\n");
	            			} else {
	            				throw new IOException("Error while transferring packets.");
	            			}
	            		} else {
	            			throw new IOException("File was not found at specified path.");
	            		}
	            	} else {
	            		throw new IOException("File name was not found in request.");
	            	}
	            } else {
	            	throw new IOException("Packet received was not of type RRQ.");
	          }
            } catch (IOException e) {
            	String msg = e.getMessage(); 	/* Gets error message to send out */
            	client.send(Tftp.errorPacket(msg, addr, port)); /* To TftpClient */
            	System.out.println("Transfer failed: " + msg);	/* To TftpServer */
            }
		} catch (Exception e) {
         	System.out.println("Fatal Error: " + e.getMessage());
         } finally {
         	System.out.println("\nClosing TftpClient Socket...");
         }
	}


	/**
	 * Transfers the data to the TftpClient.
	 *
	 * Takes the file bytes from the local TftpServer directory, then calls
	 * the method to package the data into blocks with headers. Once packaged,
	 * then sends over a block at a time by waiting for the TftpClient to send
	 * an ACK back for the next.
	 *
	 * Will try to resend one packet for a maximum of attemps set by the value
	 * of {@link Tftp#ATTEMPTS} and returns false if reached.
	 *
	 * @see 	#dataPacket(byte[] file, InetAddress addr, int port)
	 *
	 * @param   socket  The DatagramSocket the TftpClient is connected to.
	 * @param   file    The byte array containing the requested file to send.
	 * @param   addr    The InetAddress of the destination.
	 * @param   port    The int port number to send the packets through.
	 * @return  True if the file transfer was successful, False otherwise.
	 * @throws  IOException
	 */
	private boolean transfer(DatagramSocket socket, byte[] file, InetAddress addr, int port) throws IOException {

        DatagramPacket ack = new DatagramPacket(new byte[Tftp.HEADER], Tftp.HEADER);
        DatagramPacket[] packets = Tftp.dataPacket(file, addr, port);

        int total = packets.length;
        int attempts = 0;	/* Failed attempts made to send a block */
        int block = 0; 		/* The amount of blocks sent, and the index */

        System.out.println("\nTransferring " + total + " packets...\n");

        /**
         * The following loop will send a packet block on each iteration.
         *
         * This loop will break when TftpServer fails to send a packet too many
         * times, or if any exception is thrown throughout the process.
         */
        while (attempts < Tftp.ATTEMPTS) {

		   	socket.send(packets[block]);	/* Sends block to TftpClient */

        	socket.setSoTimeout(Tftp.WAIT); /* Short wait to receive an ACK */
    		socket.receive(ack);

        	byte[] arr = ack.getData();
        	int toSend = arr[1] & 0xFF;   	/* Gets the int of ACK'd block */
        	int next = block + 1;    		/* Calculates the expected block */

        	/*
        	 * Checks the expected block number against actual values.
        	 *
        	 * Note: 	The packet's array is 0-indexed, so any attempt to
        	 * 			access it must be done with a value less than the block
        	 * 			number requested.
        	 *
        	 * 			For example, the value of toSend is the 'next' block
        	 * 			requested by TftpClient, but will also be the index for
        	 * 			that block in this packet array.
        	 */
        	if (next == total) {
        		System.out.print("\t\rSent total of " + total + " packets!\r");
        		socket.send(Tftp.ackPacket(0, addr, port));
       			return true;
        	} else if (next == toSend) {
        		System.out.print("\t\rSent " + next + "/" + total + " packets\r");
        		block = toSend;  /* To send the next block over */
        	} else {
        		System.out.print("\n\t\rRetrying packet " + block + "...\r");
        		attempts++;
        	}
      	}
      	return false; 	/* Return False for failed to send */
    }
}
