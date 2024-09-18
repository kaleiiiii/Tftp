import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;


public class TftpClient extends Thread {
	
	private InetAddress address;
	private String fileName;
	private Path directory;

	public TftpClient(InetAddress address, String fileName, String folderName) throws IOException {
		
		this.address = address;
		this.fileName = fileName;
		this.directory = Tftp.setLocalPath(folderName);		
	} 

	public static void main(String[] args){
		
		if (args.length > 1) {
			System.out.println("\nWelcome, TftpClient!\n");

			try {	
				InetAddress address = InetAddress.getByName(args[0]);		
				String target = args[1];
				String folder = Tftp.OUT_DIR;

				if (args.length > 2) {
					folder = args[2];
				}
				
				TftpClient client = new TftpClient(address, target, folder);
				client.start();
				client.join();

			} catch (InterruptedException e) {
				System.out.println("InterruptedException: " + e.getMessage());
			} catch (IOException e) {
				System.out.println("IOException: " + e.getMessage());
			}finally {
				System.out.println("Closing TftpClient...");
			}
		} else {
			System.out.println("Incorrect length of arguments...");
			System.out.println("To download to default directory:");
			System.out.println("$ java TftpClient <IP Address> <File Name>\n");
			System.out.println("To change the name of the directory:");
			System.out.println("$ java TftpClient <IP Address> <File Name> <Output Folder>\n");
		}
	}

	@Override
	public void run() {

		try (
			DatagramSocket socket = new DatagramSocket();
		) {					
			System.out.println("\tFile Requested: " + fileName);
			socket.send(Tftp.rrqPacket(fileName, address));	

            Path target = directory.resolve(fileName);
            System.out.println("\tDownload location: " + target);
           
            System.out.println("\nWaiting for download...\n");

            if (downloadFile(socket, target)) {
            	System.out.println("\nDownload complete!\n");
            } else {
            	System.out.println("\nDownload failed...\n");
            }         	
		} catch (IOException e) {
         	System.out.println("IOException: " + e.getMessage());
        } catch (Exception e) {
        	System.out.println("Fatal Error: " + e.getMessage());
        } finally {
        	System.out.println("Closing TftpServer socket...");
        }
	}

	public boolean downloadFile(DatagramSocket socket, Path target) throws IOException {

        try (
        	FileOutputStream fos = new FileOutputStream(target.toFile());
        	BufferedOutputStream bos = new BufferedOutputStream(fos);
        ) {
            DatagramPacket pkt = new DatagramPacket(new byte[Tftp.BUFFER], Tftp.BUFFER);
            
			int received = 0;

            while (true) {

            	socket.setSoTimeout(Tftp.WAIT);
            	socket.receive(pkt);

            	InetAddress address = pkt.getAddress();
            	int port = pkt.getPort(); 
            	byte[] data = pkt.getData();

            	byte type = data[0];
            	int block = data[1] & 0xFF; 
            	int next = received + 1;

            	if (type == Tftp.DATA) {
            		System.out.print("\t\rDownloading packet " + block + "...\r");         		
            		
            		received = processData(bos, data, next, block);

	                socket.send(Tftp.ackPacket(received, address, port));
	                Thread.sleep(Tftp.PAUSE);

            	} else if (type == Tftp.ACK) {
            		System.out.println("\n\tReceived total " + received);
            		return true;

            	} else if (type == Tftp.ERROR) {
            		System.out.println("From TftpServer: " + Tftp.getString(data));
            		return false;
            	}
            } 
        } catch (InterruptedException e) {
            throw new IOException("Thread was interrupted unexpectedly.");
       	}
    }

    private int processData(BufferedOutputStream bos, byte[] data, int next, int received) throws IOException {

    	if (received != next) {
    		return received;
    	} else {
    		int start = Tftp.HEADER_SIZE;
    		int range = data.length - Tftp.HEADER_SIZE;

    		bos.write(data, start, range);  
    		return next;
    	}  	
    }
}
