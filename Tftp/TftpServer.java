import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;


public class TftpServer extends Thread {

    private final InetAddress address;
    private final Path directory;

    public TftpServer(InetAddress address) throws IOException {

		this.address = address;
		this.directory = Tftp.setLocalPath(Tftp.SRC_DIR);
	}

	public static void main(String[] args){

		if (args.length == 0) {

			System.out.println("\nWelcome to local TftpServer!\n");

			try {
				InetAddress address = InetAddress.getLocalHost();
				TftpServer server = new TftpServer(address);

				System.out.println("For the remote client, enter something like:");
				System.out.println("$ java TftpClient " + address.getHostAddress() + " cat.txt");

				server.start();
				server.join();

			} catch (InterruptedException e) {
				System.out.println("InterruptedException: " + e.getMessage());
			} catch (IOException e) {
				System.out.println("IOException: " + e.getMessage());
			} finally {
				System.out.println("Closing TftpServer...");
			}
		} else {
			System.out.println("Too many arguments...");
			System.out.println("Usage: $ java TftpServer");
		}
	}

	@Override
	public void run() {

		try (
			DatagramSocket client = new DatagramSocket(Tftp.PORT);
		) {
			System.out.println("\nWaiting on port " + Tftp.PORT + "...\n");
           	client.setSoTimeout(Tftp.TIMEOUT);

           	DatagramPacket pkt = new DatagramPacket(new byte[Tftp.BUFFER], Tftp.BUFFER);
           	client.receive(pkt);

           	System.out.println("\nRequest received!\n");
            byte[] rrq = pkt.getData();
            byte type = rrq[0];

            InetAddress address = pkt.getAddress();
            int port = pkt.getPort();

            try {
	            if (type == Tftp.RRQ) {
	            	String fileName = Tftp.getString(rrq);

	            	if (fileName != null) {
	            		Path filePath = getFilePath(directory, fileName);

	            		if (filePath != null) {
	            			byte[] file = getFileBytes(filePath);

	            			System.out.println("\tFile: " + fileName);
	            			System.out.println("\tPath: " + filePath);
	            			System.out.println("\tSize: " + file.length);

	            			if (transfer(client, file, address, port)) {
	            				System.out.println("\nFile transfer was successful!\n");
	            			} else {
	            				throw new IOException("Could not transfer the file.");
	            			}
	            		} else {
	            			throw new IOException("Could not find the file.");
	            		}
	            	} else {
	            		throw new IOException("Could not read the file name.");
	            	}
	            } else {
	            	throw new IOException("Did not receive and RRQ packet.");
	          }
            } catch (IOException e) {
            	System.out.println("Error: " + e.getMessage());
            	client.send(Tftp.errorPacket(e.getMessage(), address, port));
            }      
		} catch (Exception e) {
         	System.out.println("Fatal Error: " + e.getMessage());
         } finally {
         	System.out.println("Closing TftpClient Socket...");
         }
	}

	private boolean transfer(DatagramSocket socket, byte[] file, InetAddress address, int port) throws IOException {

        DatagramPacket ack = new DatagramPacket(new byte[Tftp.HEADER_SIZE], Tftp.HEADER_SIZE);
        DatagramPacket[] packets = Tftp.dataPacket(file, address, port);

        int total = packets.length;
        int attempts = 0;
        int block = 0;

        System.out.println("\nTransferring " + total + " packets...\n");      		

        while (attempts < Tftp.MAX_ATTEMPTS) {

		   	socket.send(packets[block]);

        	socket.setSoTimeout(Tftp.WAIT);
    		socket.receive(ack);

        	byte[] arr = ack.getData();
        	int sent = arr[1] & 0xFF;  
        	int next = block + 1;

        	if (sent == total) {
        		socket.send(Tftp.ackPacket(0, address, port)); 
        		return true;
        	} else if (sent == next) {  
        		System.out.print("\t\rSending block " + next + "...\r");      			        			        		
        		block = sent;     		 				      		
        	} else {
        		System.out.print("\n\t\rRetrying block " + sent + "...\r");
        		attempts++;	    		
        	}
      	}
      	return false; 	
    }

    private static Path getFilePath(Path directory, String fileName) throws IOException {

        Path filePath = directory.resolve(fileName);
        if (Files.exists(filePath)) {
            return filePath;
        } else {
            throw new IOException("Could not resolve the file path.");
        }
    }

    private static byte[] getFileBytes(Path filePath) throws IOException {

        byte[] fileBytes = Files.readAllBytes(filePath);
        if (fileBytes.length < Tftp.LIMIT) {
            return fileBytes;
        } else {
            throw new IOException("Could not get file bytes from path.");
        }
    }

}