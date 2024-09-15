import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.IOException;
import java.nio.file.Path;

public class TftpServer extends Thread {

    private InetAddress address;
    private Path directory;

    public TftpServer(InetAddress address) throws IOException {
        this.address = address;
        this.directory = Tftp.setLocalPath(Tftp.SRC_DIR);
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("\n\tWelcome to TftpServer\n");

            try {
                InetAddress address = InetAddress.getLocalHost();
                TftpServer server = new TftpServer(address);

                System.out.println("Starting TftpServer's main thread...");
                server.start();
                server.join();

            } catch (IOException e) {
                System.out.println("Could not initialize TftpServer's directory.");
            } catch (InterruptedException e) {
                System.out.println("Could not complete process without interruption.");
            } finally {
                System.out.println("TftpServer has closed...\n");
            }
        } else {
            System.out.println("Too many arguments were provided.");
            System.out.println("Usage:\n\t$ java TftpServer\n");
        }
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(Tftp.PORT)) {
            socket.setSoTimeout(Tftp.TIMEOUT);

            byte[] bytes = new byte[Tftp.BUFFER];
            DatagramPacket rrq = new DatagramPacket(bytes, bytes.length);
            socket.receive(rrq);
            handleRequest(rrq);

        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void handleRequest(DatagramPacket rrq) throws IOException {

        try (DatagramSocket client = new DatagramSocket()) {
            client.setSoTimeout(Tftp.TIMEOUT);
            InetAddress ip = rrq.getAddress();
            int port = rrq.getPort();
            String fileName = Tftp.getFileName(rrq.getData());
            Path filePath = Tftp.getFilePath(this.directory, fileName);

            if (filePath != null) {
                System.out.println("\tPath: " + filePath);
                byte[] fileBytes = Tftp.getFileBytesFrom(filePath);
                if (fileBytes != null) {
                    DatagramPacket[] packets = TftpPacket.getPackets(fileBytes, ip, port);
                    transfer(client, packets);
                } else {
                    client.send(TftpPacket.error("File Too Large", 0, ip, port));
                    throw new IOException("File size exceeded " + Tftp.MAX_BYTES);
                }
            } else {
                client.send(TftpPacket.error("File Not Found", 0, ip, port));
                throw new IOException("Could not locate " + fileName);
            }
        }
    }

    private void transfer(DatagramSocket socket, DatagramPacket[] packets) throws IOException {

        byte[] ackBytes = new byte[Tftp.HEADER_SIZE];
        DatagramPacket ackPacket = new DatagramPacket(ackBytes, ackBytes.length);

        int totalBlocks = packets.length;
        int attempts = 0;
        int currentBlock = 0;
        while (currentBlock < totalBlocks) {
            if (attempts > Tftp.TIMEOUT) {
                socket.send(TftpPacket.error("Too many attempts", currentBlock, socket.getInetAddress(), socket.getPort()));
                throw new IOException("Too many attempts to send packet " + currentBlock);
            }
            socket.setSoTimeout(Tftp.TIMEOUT);
            socket.receive(ackPacket);

            int nextBlock = currentBlock + 1;

            if (TftpBlock.checkHeader(ackPacket.getData(), nextBlock)) {
                socket.send(packets[nextBlock]);
                currentBlock++;
            } else {
                socket.send(packets[currentBlock]);
                attempts++;
            }
        }
    }
}
