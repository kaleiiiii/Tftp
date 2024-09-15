import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

public class TftpClient extends Thread {

    private final int TIMEOUT = Tftp.TIMEOUT;
    private final String PATH = "client";

    private byte[] TID;
    private InetAddress address;
    private String fileName;
    private Path downloads;
    private boolean running = true;

    public TftpClient(InetAddress address, String fileName) throws IOException {
        
        this.address = address;
        this.fileName = fileName;
        this.downloads = Tftp.setLocalPath(PATH);
    }

    public static void main(String[] args) {

        if (args.length == 2) {
            try {
                System.out.println("Starting TftpClient...");
                TftpClient client = new TftpClient(InetAddress.getByName(args[0]), args[1]);
                client.start();
                client.join();
            } catch (IOException e) {
                System.out.println("Could not initialize TftpClient's directory.");
            } catch (InterruptedException e) {
                System.out.println("TftpClient was interrupted.");
            } finally {
                System.out.println("Closing TftpClient...");
            }
        } else {
            System.out.println("Incorrect amount of arguments were entered.");
            System.out.println("Usage:\n\t$ java TftpClient <Server IP> <File Name>\n");
        }
    }

    @Override
    public void run() {

        try (
            DatagramSocket socket = new DatagramSocket()
        ) {
            socket.setSoTimeout(TIMEOUT);
            DatagramPacket rrq = TftpPacket.request(TftpOpCode.RRQ, fileName, address);
            Path target = downloads.resolve(fileName);

            System.out.println("\tSending request...");
            socket.send(rrq);

            System.out.println("\tWaiting for download...\n");
            downloadFile(socket, target);

        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private byte[] setTid(DatagramPacket packet) throws IOException {

        int port = packet.getPort();
        byte[] portBytes = new byte[Tftp.OFFSET];
        byte[] addressBytes = packet.getAddress().getAddress();

        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            portBytes[0] = (byte) (port >> 8);
            portBytes[1] = (byte) (port);

            stream.write(portBytes);
            stream.write(":".getBytes(Tftp.ENCODING));
            stream.write(addressBytes);

            return stream.toByteArray();
        }
    }

    private boolean checkTid(DatagramPacket packet, byte[] tid) {

        byte[] packetData = packet.getData();
        if (packetData.length != tid.length) {
            return false;
        } else {
            for (int i = 0; i < packetData.length; i++) {
                if (packetData[i] != tid[i]) {
                    return false;
                }
            }
            return true;
        }
    }

    public void downloadFile(DatagramSocket socket, Path target) throws IOException {

        try (
            FileOutputStream stream = new FileOutputStream(target.toFile());
            BufferedOutputStream download = new BufferedOutputStream(stream)
        ) {
            byte[] buffer = new byte[Tftp.BUFFER];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            socket.receive(packet);
            this.TID = setTid(packet);

            socket.send(TftpPacket.ack(0, address, packet.getPort()));

            while (running) {
                socket.receive(packet); 

                if (!checkTid(packet, TID)) {
                    System.out.println("Unexpected TID of packet received.");
                    continue;
                }

                byte[] blockBytes = packet.getData();
                int length = blockBytes.length - Tftp.HEADER_SIZE;
                TftpOpCode type = TftpOpCode.getType(blockBytes);

                if (type.isType(TftpOpCode.DATA)) {
                    System.out.print("\rDownloading packet...");
                    download.write(blockBytes, Tftp.HEADER_SIZE, length);
                } else if (type.isType(TftpOpCode.ERROR)) {
                    System.out.println("\rCould not download packet.");
                    running = false;
                } else if (type.isType(TftpOpCode.ACK)) {
                    System.out.println("\rDownload complete.");
                    running = false;
                }
                socket.send(TftpPacket.ack(0, address, packet.getPort()));
            }
        }
    }
}
