package org.network.client;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.network.Entity.Data;
import org.network.packet.DataPacket;
import org.network.util.Initial_Connection;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Client {

	public static void main(String[] args) throws IOException {
		
		Session s = Initial_Connection.getSession();
		
		Socket socket = new Socket("localhost", 8080);
		
		ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
		
		for(int i=1; i<=2; i++) {
			Data data = s.find(Data.class,i);
			
			StringBuilder sb = new StringBuilder();
			sb.append(data.getId()).append(",").append(data.getName()).append(",").append(data.getAge());
			System.out.println(sb.toString());
			DataPacket dataPacket = new DataPacket(i, sb.toString().getBytes(), "checksum");
			os.writeObject(dataPacket);
		}
		
		os.flush();

	/*	Transaction t = s.beginTransaction();
		
		t.commit();
		s.close();  */
	}

}
