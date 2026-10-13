package org.network.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.network.Entity.ServerData;
import org.network.packet.DataPacket;
import org.network.util.Initial_Connection;

public class Server {

	public static void main(String[] args) throws ClassNotFoundException, IOException {
	    
			ServerSocket server = new ServerSocket(8080);
			Socket soc = server.accept();
			ObjectInputStream os = new ObjectInputStream(soc.getInputStream());

			Session s = Initial_Connection.getSession();

			try {
			    while (true) {
			        // This will block until a new packet arrives
			        DataPacket dataPacket = (DataPacket) os.readObject();
			        
			        String originalString = new String(dataPacket.getPayloadChunk(), StandardCharsets.UTF_8);
			        String[] originalData = originalString.split(",");
			        
			        Transaction t = s.beginTransaction();
			        ServerData serverData = new ServerData();
			        serverData.setId(Integer.parseInt(originalData[0]));
			        serverData.setName(originalData[1]);
			        serverData.setAge(Integer.parseInt(originalData[2]));
			        
			        s.persist(serverData);
			        t.commit();
			        System.out.println("Saved packet ID: " + serverData.getId());
			    }
			} catch (Exception e) {
			    System.out.println("Client finished sending data.");
			} finally {
			    s.close();
			    os.close();
			    soc.close();
			    server.close();
			}

	}

}
