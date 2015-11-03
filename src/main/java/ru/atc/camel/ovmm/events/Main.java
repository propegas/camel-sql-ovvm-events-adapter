package ru.atc.camel.ovmm.events;

import java.io.File;
import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.model.dataformat.JsonDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.camel.processor.idempotent.FileIdempotentRepository;
import ru.at_consulting.itsm.event.Event;

public class Main {
	
	private static Logger logger = LoggerFactory.getLogger(Main.class);
	public static String activemq_port = null;
	public static String activemq_ip = null;
	public static String mysqldb_ip = null;
	public static String mysqldb_port = null;
	public static void main(String[] args) throws Exception {
		
		logger.info("Starting Custom Apache Camel component example");
		logger.info("Press CTRL+C to terminate the JVM");
		
		if ( args.length == 4  ) {
			activemq_port = (String)args[1];
			activemq_ip = (String)args[0];
			mysqldb_ip = (String)args[2];
			mysqldb_port = (String)args[3];
		}
		
		if (activemq_port == null || activemq_port == "" )
			activemq_port = "61616";
		if (activemq_ip == null || activemq_ip == "" )
			activemq_ip = "172.20.19.195";
		
		logger.info("activemq_ip: " + activemq_ip);
		logger.info("sdce_port: " + activemq_port);
		
		if (mysqldb_ip == null || mysqldb_ip == "" )
			mysqldb_ip = "localhost";
		if (mysqldb_port == null || mysqldb_port == "" )
			mysqldb_port = "3307";
		
		logger.info("mysqldb_ip: " + mysqldb_ip);
		logger.info("mysqldb_port: " + mysqldb_port);
		
		org.apache.camel.main.Main main = new org.apache.camel.main.Main();
		main.enableHangupSupport();
		
		main.addRouteBuilder(new RouteBuilder() {
			
			@Override
			public void configure() throws Exception {
				
				JsonDataFormat myJson = new JsonDataFormat();
				myJson.setPrettyPrint(true);
				myJson.setLibrary(JsonLibrary.Jackson);
				myJson.setJsonView(Event.class);
				
				PropertiesComponent properties = new PropertiesComponent();
				properties.setLocation("classpath:ovmm.properties");
				getContext().addComponent("properties", properties);

				ConnectionFactory connectionFactory = new ActiveMQConnectionFactory
						("tcp://" + activemq_ip + ":" + activemq_port);		
				getContext().addComponent("activemq", JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));
				
				File cachefile = new File("sendedEvents.dat");
		        cachefile.createNewFile();
		        
				from("ovmm://events?"
		    			+ "delay={{delay}}&"
		    			+ "username={{username}}&"
		    			+ "password={{password}}&"
		    			+ "mysql_host={{mysql_host}}&"
		    			+ "mysql_db={{mysql_db}}&"
		    			+ "mysql_port={{mysql_port}}&"
		    			+ "query={{query}}")
		    	
		    
				.idempotentConsumer(
			             header("EventUniqId"),
			             FileIdempotentRepository.fileIdempotentRepository(cachefile,2500)
			             )
				
					.marshal(myJson)
		    		.log("${id} ${header.EventId}")
		    		.to("activemq:OVMM-tgk1-Events.queue");
				}
		});
		
		main.run();
	}
}