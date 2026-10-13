package org.network.util;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.network.Entity.Data;
import org.network.Entity.ServerData;

public class Initial_Connection {
		public static Session getSession() {
			Configuration configuration = new Configuration();
			configuration.configure("hibernate.cfg.xml")
			        .addAnnotatedClass(Data.class)
			        .addAnnotatedClass(ServerData.class);
			SessionFactory sessionFactory = configuration.buildSessionFactory();
			Session session = sessionFactory.openSession();
			return session;
		}
}