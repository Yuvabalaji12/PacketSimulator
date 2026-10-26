package org.network.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.network.Entity.ServerData;
import org.network.packet.DataPacket;
import org.network.packet.RetransmissionRequest;
import org.network.util.CheckSumUtil;
import org.network.util.Initial_Connection;

public class Server {

	public static void main(String[] args) throws ClassNotFoundException, IOException {
	    
			ServerSocket server = new ServerSocket(8080);
			Socket soc = server.accept();

			ObjectOutputStream os = new ObjectOutputStream(soc.getOutputStream());
			os.flush();

			ObjectInputStream in = new ObjectInputStream(soc.getInputStream());
            
			soc.setSoTimeout(1200);
        
            DataPacket data = (DataPacket) in.readObject();
			final long TOTAL_PACKETS =data.getTotalPackets();
            Map<Integer, DataPacket> receivedPackets = new TreeMap<>();
            

        while (receivedPackets.size() < TOTAL_PACKETS) {
            try {
                DataPacket packet = (DataPacket) in.readObject();
                 

                // Check SHA-256 integrity[cite: 1]
                String calculatedHash = CheckSumUtil.calculateSHA256(packet.getPayloadChunk());
                if (!calculatedHash.equals(packet.getChecksum())) {
                    System.err.println("[SERVER CORRUPT] Packet #" + packet.getSequenceNumber() + " failed SHA-256 check. Discarding.");
                    continue; // Skip storing corrupted packet[cite: 1]
                }

                // Deduplicate and store valid packet[cite: 1]
                if (!receivedPackets.containsKey(packet.getSequenceNumber())) {
                    receivedPackets.put(packet.getSequenceNumber(), packet);
                    System.out.println("[SERVER VALID] Accepted packet #" + packet.getSequenceNumber());
                }

            } catch (SocketTimeoutException ste) {
                // Time window elapsed: identify missing/corrupted sequence IDs[cite: 1]
                List<Integer> missingIds = new ArrayList<>();
                for (int i = 1; i <= TOTAL_PACKETS; i++) {
                    if (!receivedPackets.containsKey(i)) {
                        missingIds.add(i);
                    }
                }

                if (!missingIds.isEmpty()) {
                    System.out.println("[SERVER TIMEOUT] Requesting retransmission for missing packets: " + missingIds);
                    os.writeObject(new RetransmissionRequest(missingIds));
                    os.flush();
                }
            }
        }

        // Notify client that all packets are collected
        os.writeObject(new RetransmissionRequest(Collections.emptyList()));
        os.flush();

        // 3. Reconstruct data and persist to Hibernate[cite: 1]
        Session s = Initial_Connection.getSession();
        Transaction t = s.beginTransaction();

        for (DataPacket packet : receivedPackets.values()) {
            String record = new String(packet.getPayloadChunk(), StandardCharsets.UTF_8);
            String[] fields = record.split(",");

            ServerData serverData = new ServerData();
            serverData.setId(Integer.parseInt(fields[0]));
            serverData.setName(fields[1]);
            serverData.setAge(Integer.parseInt(fields[2]));

            s.persist(serverData);
            System.out.println("[DB PERSIST] Successfully saved ID: " + serverData.getId());
        }

        t.commit();
        s.close();

        in.close();
        os.close();
        soc.close();
        server.close();

	}

}
