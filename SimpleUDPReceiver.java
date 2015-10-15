import java.io.File;
import java.io.FileOutputStream;
import java.net.*;
import java.util.*;
import java.nio.*;
import java.util.zip.*;

public class SimpleUDPReceiver {
	
	private static SocketAddress senderAddr;
	private static int port;
	private static int packetLength;
	private static byte seqNum = 0;
	private static byte receivedSeq;
	private static byte isFirstPkt;
	private static byte isEOF;
	private static byte fileStart = 15;
	private static String filePath;
	private static File file;
	private static DatagramSocket sk;
	private static DatagramPacket pkt;
	private static FileOutputStream fos;
	private static ByteBuffer b;
	private static CRC32 crc;
	private static Long chksum;

	public static void main(String[] args) throws Exception 
	{
		if (args.length != 1) {
			System.err.println("Usage: SimpleUDPReceiver <port>");
			System.exit(-1);
		}
		// Preparation for revceiving data.
		port = Integer.parseInt(args[0]);
		sk = new DatagramSocket(port);
		// This is where data stored
		byte[] data = new byte[1024];
		// This is where packet created
		pkt = new DatagramPacket(data, data.length);
		// ByteBuffer to read data from packet
		b = ByteBuffer.wrap(data);
		crc = new CRC32();
		
		while(true)
		{
			pkt.setLength(data.length);
			sk.receive(pkt);
			// Get sender address
			
			senderAddr = pkt.getSocketAddress();
			packetLength = pkt.getLength();
			
			/*if (pkt.getLength() < 8)
			{
				System.out.println("Pkt too short");
				continue;
			}*/
			b.rewind();
			// This is the first 8 bytes in header
			chksum = b.getLong();
			crc.reset();
			crc.update(data, 8, packetLength-8);
			receivedSeq = b.get();
			isFirstPkt = b.get();
			isEOF = b.get();
			
			byte[] dataPacket = new byte[1024];
			
			// ** Check this later see if it is right
			b.get(dataPacket, 0, packetLength-fileStart);
			// Debug output
			// System.out.println("Received CRC:" + crc.getValue() + " Data:" + bytesToHex(data, pkt.getLength()));
			
			// Header in packet that sender send in: 
			// 1. checksum 2. sequence number 3. is or is not first packet 4. is or is not the end of file
			// 3 is for receiving first packet that contains the save path of file and create the file in current repository
			
			// Check if data is corrupted or lost
			if (crc.getValue() != chksum || seqNum != receivedSeq)
			{
				System.out.println("Pkt corrupt or lost");
				seqNum --; // Send acknowledge previous packet
				DatagramPacket ack = createAck();
				sk.send(ack);
			} 
			
			if (isFirstPkt == 1){
				System.out.println("Receving first packet and creating the file path.");
				file = new File(filePath);
				
				// Dunno what it is used for haha
				if (file.getParent() != null){
					file.getParentFile().mkdir();
				}
				
				DatagramPacket ack = createAck();
				sk.send(ack);
				seqNum ++;
				
			} else if (isEOF == 1){
				fos.close();
			} else{
				fos.write(dataPacket);
				DatagramPacket ack = createAck();
				sk.send(ack);
				seqNum ++;
			}
			
			
			
		}
	}

	public static DatagramPacket createAck(){
		// Set up ack packet
		 byte[] ackData = new byte[1024];
		 ByteBuffer ackBuffer = ByteBuffer.wrap(ackData);
		 
		 ackBuffer.clear();
		 ackBuffer.putLong(0); // 8 bytes for checksum
		 ackBuffer.put(seqNum); // 1 byte for sequence number
		 crc.reset();
		 crc.update(ackData, 8, ackData.length-8);
		 chksum = crc.getValue();
		 ackBuffer.rewind();
		 ackBuffer.putLong(chksum);
		 
		 return new DatagramPacket(ackData, ackData.length, senderAddr);
		 
		 
	}
	
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes, int len) {
	    char[] hexChars = new char[len * 2];
	    for ( int j = 0; j < len; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
}
