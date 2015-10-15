import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.nio.*;
import java.util.zip.*;

public class SimpleUDPSender {
	// set up for transfer file
	private static String source;
	private static String destination;
	private static String host;
	private static int port;
	private static int fileStart = 15;
	private static File sourceFile;
	private static DatagramSocket sk;
	private static DatagramPacket pkt;
	private static DataInputStream dis;
	private static CRC32 crc;
	
	// set up for package
	private static byte seqNum = 0;
	private static byte isEOF = 0;
	private static byte[] packet;
	private static byte isFirstPacket = 1;
	private static byte notFirstPacket = 0;
	
	// set up for receiving ack packet data from receiver
	private static byte[] ackPacket = new byte[10];
	private static ByteBuffer ackByteBuffer;
	private static DatagramPacket ackDatagramPacket;
	private static boolean receivingAck = true;
	
	

	public static void main(String[] args) throws Exception 
	{
		if (args.length != 3) {
			System.err.println("Usage: SimpleUDPSender <host> <port> <sourcefile> <destinationfile>");
			System.exit(-1);
		}
		// Preparation for sending
		host = args[0];
		port = Integer.parseInt(args[1]);
		source = args[2];
		destination = args[3];
		InetSocketAddress addr = new InetSocketAddress(host, port);
		sourceFile = new File(source);		
		sk = new DatagramSocket();
		sk.setSoTimeout(5);		
		
		crc = new CRC32();		
		dis = new DataInputStream(new FileInputStream(source));
		
		// Preparation for receiving ack
		ackByteBuffer = ByteBuffer.wrap(ackPacket);
		ackDatagramPacket = new DatagramPacket(ackPacket, 10);
		
		// Sending destination file name packet
		byte[] destinationPath = destination.getBytes();
		createFilePath(destinationPath);
		seqNum++;
		
		// Sending data
		byte[] fileData = new byte[1024];	
		ByteBuffer b = ByteBuffer.wrap(fileData);
		sendFile(dis, fileData);
	}
	
	private static void sendFile(DataInputStream readFile, byte[] fileData){
		// Set up byte array for storing file
		int bytesRead;
		try {
			while((bytesRead = readFile.read(fileData)) != -1){
				packet = new byte[1024];
				ByteBuffer packetBuffer = ByteBuffer.wrap(packet);
				// Set up packet
				packetBuffer.clear();
				packetBuffer.putLong(0);
				packetBuffer.put(seqNum);
				packetBuffer.put(notFirstPacket);
				packetBuffer.put(isEOF);
				
				// Set up checksum
				crc.reset();
				crc.update(packet, 8, packet.length - 8);
				long checksum = crc.getValue();
				packetBuffer.rewind();
				packetBuffer.putLong(checksum);
				// This is where the file starts
				packetBuffer.position(fileStart);
				packetBuffer.put(fileData);
				
				receivingAck = true;
				while(receivingAck) {
					try {
						sk.send(pkt);
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					ackByteBuffer.clear();
					// This is for checking if receiver received file name.			
					try {
						sk.receive(ackDatagramPacket);
						receivingAck = false; // Received ack
					} catch (IOException e) {
						// TODO Auto-generated catch block
						receivingAck = true; // Try to receive ack again
						e.printStackTrace();
						
					}
					
					// Received ack, not for receiving again
					if (receivingAck == false) {
						ackByteBuffer.rewind();
						long ackChecksum = ackByteBuffer.getLong();
						crc.reset();
						crc.update(ackPacket, 8, 2);
						byte ackSeqNum = ackByteBuffer.get();
						
						// Check if file is corrupted, if it is try to receive ack again
						if (crc.getValue() == ackChecksum && ackSeqNum == seqNum) {
							break;
						} else{
							receivingAck = true;
						}
					}
				}
				// Update packet number.
				seqNum++;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Set up buffer for receving information from receiveer
		
	}
	
	// This is the first packet that carries destination file name to receiver.
	
	// If there is error check the sequence of the header.
	private static void createFilePath(byte[] destinationPath){
		packet = new byte[1024];
		ByteBuffer firstPacketBuffer = ByteBuffer.wrap(packet);
		firstPacketBuffer.putLong(0); // 8 bytes
		firstPacketBuffer.put(seqNum); // 1 byte
		firstPacketBuffer.put(isFirstPacket); // 1 byte
		firstPacketBuffer.put(isEOF); // 1 byte
		// And 4 bytes left empty in header
		crc.reset();
		crc.update(packet, 8, packet.length - 8);
		long checksum = crc.getValue();
		firstPacketBuffer.rewind();
		firstPacketBuffer.putLong(checksum);
		// put file name into the packet
		firstPacketBuffer.position(fileStart);
		firstPacketBuffer.put(destinationPath);
		receivingAck = true;
		while(receivingAck) {
			try {
				sk.send(pkt);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			ackByteBuffer.clear();
			// This is for checking if receiver received file name.			
			try {
				sk.receive(ackDatagramPacket);
				receivingAck = false; // Received ack
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				receivingAck = true; // Try to receive ack again
			}
			
			// Received file name, not for receiving again
			if (receivingAck == false) {
				ackByteBuffer.rewind();
				long ackChecksum = ackByteBuffer.getLong();
				crc.reset();
				crc.update(ackPacket, 8, 2);
				byte ackSeqNum = ackByteBuffer.get();
				
				// Check if file is corrupted, if it is try to receive ack again
				if (crc.getValue() == ackChecksum && ackSeqNum == seqNum) {
					break;
				} else{
					receivingAck = true;
				}
			}
		}
		
		
		
	}

	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
}
