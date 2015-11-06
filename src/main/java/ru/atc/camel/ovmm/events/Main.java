package ru.atc.camel.ovmm.events;

import java.io.File;
import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.Exchange;
import org.apache.camel.Header;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cache.CacheConstants;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.dataformat.JsonDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.camel.processor.cache.CacheBasedMessageBodyReplacer;
import org.apache.camel.processor.cache.CacheBasedTokenReplacer;
import org.apache.camel.processor.idempotent.FileIdempotentRepository;
import ru.at_consulting.itsm.event.Event;



public class Main {
	
	public static ModelCamelContext context;
	
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
				
				context = getContext();
				
				PropertiesComponent properties = new PropertiesComponent();
				properties.setLocation("classpath:ovmm.properties");
				context.addComponent("properties", properties);

				ConnectionFactory connectionFactory = new ActiveMQConnectionFactory
						("tcp://" + activemq_ip + ":" + activemq_port);		
				context.addComponent("activemq", JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));
				
				logger.info("*****context: " + 
						context);
				
				OVMMConsumer.setContext(context);
				
				
				
				File cachefile = new File("sendedEvents.dat");
		        cachefile.createNewFile();
		        
		        /*
		        from("cache://ServerCacheTest" +
				          "?maxElementsInMemory=1500" +
				          "&eternal=true" +
				          "&overflowToDisk=true" +
				          "&diskPersistent=true")
				          */
		        //.log("*** Value added to the cache ****");
		        //.log("*** Header1 ${header.EventUniqId}" )
		       // .log("*** Header2 ${header.CamelCacheKey}" );
		        //.end();
		        
		       // from("cache://ServerCacheTest")
		        
		        //.process(new CacheBasedMessageBodyReplacer("cache://ServerCacheTest", "Tgc1-1Cp2_ping_ERROR"))
		        //.process(new CacheBasedTokenReplacer("cache://ServerCacheTest", "author", "#author#"))
		        //.log("*** Header111 ${header.EventUniqId}" )
		       // .log("*** Header111 ${header.EventUniqId}" )
		        //.log("*** Header222 ${header.CamelCacheKey}" )
		       //.log("*** Header223 ${header.TEST}" )
		       // .log("*** Header224 ${header.CamelCacheOperation}" );
		        //.to("direct:next");
		        
		            
		        from("direct:delete")
		        //.filter(method(MyBean.class, "isGoldCustomer"))
		        //.log("*** Delete old value from the cache ****")
		        //.log("*** 1 Header111 ${header.EventUniqId}" )
		        //.log("*** 2 Header222 ${header.CamelCacheKey}" )
		        //.log("*** 3 Header223 ${header.EventUniqIdWithoutStatus}" )
		        //.log("*** 4 Header224 ${header.CamelCacheOperation}" )
		        .process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						Message in = exchange.getIn();
						String key = in.getHeader("EventUniqIdWithoutStatus").toString();
						in.setHeader(CacheConstants.CACHE_OPERATION, CacheConstants.CACHE_OPERATION_DELETE);
						in.setHeader(CacheConstants.CACHE_KEY, key+"_ERROR");
					}
				})
				.to("cache://ServerCacheTest")
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						Message in = exchange.getIn();
						String key = in.getHeader("EventUniqIdWithoutStatus").toString();
						in.setHeader(CacheConstants.CACHE_OPERATION, CacheConstants.CACHE_OPERATION_DELETE);
						in.setHeader(CacheConstants.CACHE_KEY, key+"_NA");
					}
				})
				.to("cache://ServerCacheTest")
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						Message in = exchange.getIn();
						String key = in.getHeader("EventUniqIdWithoutStatus").toString();
						in.setHeader(CacheConstants.CACHE_OPERATION, CacheConstants.CACHE_OPERATION_DELETE);
						in.setHeader(CacheConstants.CACHE_KEY, key+"_OK");
					}
				})
				.to("cache://ServerCacheTest");
		        
				from("ovmm://events?"
		    			+ "delay={{delay}}&"
		    			+ "username={{username}}&"
		    			+ "password={{password}}&"
		    			+ "mysql_host={{mysql_host}}&"
		    			+ "mysql_db={{mysql_db}}&"
		    			+ "mysql_port={{mysql_port}}&"
		    			+ "table_prefix={{table_prefix}}&"
		    			+ "query={{query}}")
		    	
		    /*
					.idempotentConsumer(
			             header("EventUniqId"),
			             FileIdempotentRepository.fileIdempotentRepository(cachefile,2500)
			             )
			*/	
					
					.to("cache://ServerCacheTest")
					.choice()
						.when(header(CacheConstants.CACHE_ELEMENT_WAS_FOUND).isNull())
						//.filter()
						//.log("*** Try to delete before add ****")
						.to("direct:delete")
						.process(new Processor() {
							public void process(Exchange exchange) throws Exception {
								//String message = (String) exchange.getIn().getBody();
								Message in = exchange.getIn();
								String key = in.getHeader("EventUniqId").toString();
								String key1 = in.getHeader("EventUniqIdWithoutStatus").toString();
								in.setHeader("TEST", key1);
								in.setHeader(CacheConstants.CACHE_OPERATION, CacheConstants.CACHE_OPERATION_ADD);
								in.setHeader(CacheConstants.CACHE_KEY, key);
								in.setHeader("CacheUniqIdWithoutStatus", key1);
								in.setHeader("CacheStatus", key1);
								
								//logger.info("*-*-*-*-* 01: " + 
								//		exchange.getIn().getHeader(CacheConstants.CACHE_OPERATION).toString());
								//logger.info("*-*-*-*-* CacheUniqIdWithoutStatus 02: " + 
								//		key1);
							}
						})
							.to("direct:ShowData")
							.to("cache://ServerCacheTest") 
							.marshal(myJson)
							.to("activemq:OVMM-tgk1-Events.queue")
							.log("New event1: ${id} ${header.EventUniqId}")
						.otherwise()
						.process(new Processor() {
							public void process(Exchange exchange) throws Exception {
								//String message = (String) exchange.getIn().getBody();
								Message in = exchange.getIn();
								String key = in.getHeader("EventUniqId").toString();
								String key1 = in.getHeader("EventUniqIdWithoutStatus").toString();
								//String key2 = in.getHeader("CacheStatus").toString();
								//String key3 = in.getHeader("EventStatus").toString();
								
								
								//logger.info("*-*-*-*-* key: " + 
								//		key);
								//logger.info("*-*-*-*-* key1: " + 
								//		key1);
								
								
								//if (key1 == key2){
									in.setHeader(CacheConstants.CACHE_OPERATION, CacheConstants.CACHE_OPERATION_GET);
									in.setHeader(CacheConstants.CACHE_KEY, key);
									in.setHeader("EventUniqId", key);
									in.setHeader("EventUniqIdWithoutStatus", key1);
									
									//logger.info("*-*-*-*-* 02: " + 
									//		exchange.getIn().getHeader(CacheConstants.CACHE_OPERATION).toString());
								//}
								//else{
									
								//}
								
								
							}
						})
							
							.to("direct:ShowData");
							//.log("***OWERWISE***")
							//.log("*1 ${header.EventUniqId}")
							//.log("*2 ${header.EventUniqIdWithoutStatus}")
							//.to("cache://ServerCacheTest")
							//.log("*3 ${header.CamelCacheKey}")
							
							//.marshal(myJson)
							//.to("activemq:OVMM-tgk1-Events.queue")
							//.log("Old event2: ${id} ${header.EventUniqId}");
					//.end();
				
								
				from("direct:ShowData").process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						String operation = (String) exchange.getIn().getHeader(
								CacheConstants.CACHE_OPERATION);
						
						String key = (String) exchange.getIn().getHeader(
								CacheConstants.CACHE_KEY);
												
						Object body = exchange.getIn().getBody();
						/*
						String data = exchange.getContext().getTypeConverter()
							.convertTo(Event.class, body);
						*/
						String data = body.toString();
						if (operation.equals("ADD")){
							logger.debug("------- Cache element was not found, Add the element to the cache ---------");
							
						}else {
							logger.debug("------- Element found in the cache ---------");
						}
						logger.debug("Show Data from: ServerCacheTest");
						logger.debug("Operation = " + operation);
						logger.debug("Key = " + key);
						logger.debug("Value = " + data);
						logger.debug("------ End  ------");
					}
				});
				}
		});
		
		
		
		
		main.run();
		
		
		
		
	}
	
	public static class MyBean {
	    public boolean doTransform(@Header(CacheConstants.CACHE_KEY) String key) { 
	        return key.equals("gold"); 
	    }
	}
}