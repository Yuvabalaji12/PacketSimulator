package org.network.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.Session;
import org.network.Entity.Data;
import org.network.Simulator.NetworkSimulator;
import org.network.packet.DataPacket;
import org.network.packet.RetransmissionRequest;
import org.network.util.CheckSumUtil;
import org.network.util.Initial_Connection;

public class Client {

	public static void main(String[] args) throws IOException, ClassNotFoundException {
		
		Session s = Initial_Connection.getSession();
		
		Socket socket = new Socket("localhost", 8080);
		
		ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
		os.flush();

		NetworkSimulator simulator = new NetworkSimulator();
        Map<Integer, DataPacket> storingPackets = new HashMap<>();
		
        final int TOTAL_PACKETS = 2;

        // 1.Initial Packet sending
		for(int i=1; i<=TOTAL_PACKETS; i++) {
			Data data = s.find(Data.class,i);
			
			StringBuilder sb = new StringBuilder();
			sb.append(data.getId()).append(",").append(data.getName()).append(",").append(data.getAge());
			System.out.println(sb.toString());

            byte[] byteData = sb.toString().getBytes();

			String checksum = CheckSumUtil.calculateSHA256(byteData);
			
			System.out.println(checksum);

			DataPacket dataPacket = new DataPacket(i, TOTAL_PACKETS, byteData, checksum);
            
			storingPackets.put(i, dataPacket);

			simulator.transmit(dataPacket, os);

			System.out.println("[CLIENT] Sent packet #" + i);
		}


		// 2. Retransmission loop until server confirms all packets received

        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

        while (true) {
            RetransmissionRequest request = (RetransmissionRequest) in.readObject();

            if (request.isComplete()) {
                System.out.println("[CLIENT] Server received all packets successfully.");
                break;
            }

            System.out.println("[CLIENT RETRANSMIT] Resending packets: " + request.getMissingSequences());
            for (int missingId : request.getMissingSequences()) {
                DataPacket packet = storingPackets.get(missingId);
                if (packet != null) {
                    simulator.transmit(packet, os);
                }
            }
        }

        s.close();
        in.close();
        os.close();
        socket.close();

	/*	Transaction t = s.beginTransaction();
		
		t.commit();
		s.close();  */
	}

}
