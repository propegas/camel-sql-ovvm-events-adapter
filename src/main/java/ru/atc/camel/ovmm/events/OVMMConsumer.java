package ru.atc.camel.ovmm.events;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.cache.CacheConstants;
import org.apache.camel.impl.ScheduledPollConsumer;
import org.apache.camel.model.ModelCamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import com.apc.stdws.xsd.isxcentral._2009._10.ISXCDevice;
//import com.apc.stdws.xsd.isxcentral._2009._10.ISXCAlarmSeverity;
//import com.apc.stdws.xsd.isxcentral._2009._10.ISXCAlarm;
//import com.apc.stdws.xsd.isxcentral._2009._10.ISXCDevice;
//import com.google.gson.Gson;
//import com.google.gson.GsonBuilder;
//import com.google.gson.JsonArray;
//import com.google.gson.JsonElement;
//import com.google.gson.JsonObject;
//import com.google.gson.JsonParser;

//import ru.at_consulting.itsm.device.Device;
import ru.at_consulting.itsm.event.Event;
//import ru.atc.camel.opsm.events.OPSMConsumer.PersistentEventSeverity;
import ru.atc.camel.ovmm.events.api.OVMMDevices;
import ru.atc.camel.ovmm.events.api.OVMMEvents;

import javax.sql.DataSource;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

//import com.mysql.jdbc.Connection;
import com.mysql.jdbc.Driver;



public class OVMMConsumer extends ScheduledPollConsumer {
	
	private static Logger logger = LoggerFactory.getLogger(Main.class);
	
	public OVMMEndpoint endpoint;
	
	public String vm_ping_col =  "attribute043";
	public String vm_snmp_col =  "attribute044";
	public String vm_vmtools_col =  "attribute045";
	public String vm_backup_col =  "attribute046";
	public String vm_power_col =  "attribute004";
	
	public static ModelCamelContext context;
	
	public enum PersistentEventSeverity {
	    OK, INFO, WARNING, MINOR, MAJOR, CRITICAL;
		
	    public String value() {
	        return name();
	    }

	    public static PersistentEventSeverity fromValue(String v) {
	        return valueOf(v);
	    }
	}

	public OVMMConsumer(OVMMEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
        this.endpoint = endpoint;
        //this.afterPoll();
        this.setTimeUnit(TimeUnit.MINUTES);
        this.setInitialDelay(0);
        this.setDelay(endpoint.getConfiguration().getDelay());
	}
	
	public static ModelCamelContext getContext() {
		// TODO Auto-generated method stub
				return context;
	}
	
	public static void setContext(ModelCamelContext context1){
		context = context1;

	}

	@Override
	protected int poll() throws Exception {
		
		String operationPath = endpoint.getOperationPath();
		
		if (operationPath.equals("events")) return processSearchEvents();
		
		// only one operation implemented for now !
		throw new IllegalArgumentException("Incorrect operation: " + operationPath);
	}
	
	@Override
	public long beforePoll(long timeout) throws Exception {
		
		logger.info("*** Before Poll!!!");
		// only one operation implemented for now !
		//throw new IllegalArgumentException("Incorrect operation: ");
		
		//send HEARTBEAT
		genHeartbeatMessage(getEndpoint().createExchange());
		
		return timeout;
	}
	
	private void genErrorMessage(String message) {
		// TODO Auto-generated method stub
		long timestamp = System.currentTimeMillis();
		timestamp = timestamp / 1000;
		String textError = "Возникла ошибка при работе адаптера: ";
		Event genevent = new Event();
		genevent.setMessage(textError + message);
		genevent.setEventCategory("ADAPTER");
		genevent.setSeverity(PersistentEventSeverity.CRITICAL.name());
		genevent.setTimestamp(timestamp);
		genevent.setEventsource("OVMM_EVENTS_ADAPTER");
		genevent.setStatus("OPEN");
		genevent.setHost("adapter");
		
		logger.info(" **** Create Exchange for Error Message container");
        Exchange exchange = getEndpoint().createExchange();
        exchange.getIn().setBody(genevent, Event.class);
        
        exchange.getIn().setHeader("EventIdAndStatus", "Error_" +timestamp);
        exchange.getIn().setHeader("Timestamp", timestamp);
        exchange.getIn().setHeader("queueName", "Events");
        exchange.getIn().setHeader("Type", "Error");

        try {
			getProcessor().process(exchange);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
	}

	
	public static void genHeartbeatMessage(Exchange exchange) {
		// TODO Auto-generated method stub
		long timestamp = System.currentTimeMillis();
		timestamp = timestamp / 1000;
		//String textError = "Возникла ошибка при работе адаптера: ";
		Event genevent = new Event();
		genevent.setMessage("Сигнал HEARTBEAT от адаптера");
		genevent.setEventCategory("ADAPTER");
		genevent.setObject("HEARTBEAT");
		genevent.setSeverity(PersistentEventSeverity.OK.name());
		genevent.setTimestamp(timestamp);
		genevent.setEventsource("OVMM_EVENT_ADAPTER");
		
		logger.info(" **** Create Exchange for Heartbeat Message container");
        //Exchange exchange = getEndpoint().createExchange();
        exchange.getIn().setBody(genevent, Event.class);
        
        exchange.getIn().setHeader("Timestamp", timestamp);
        exchange.getIn().setHeader("queueName", "Heartbeats");
        exchange.getIn().setHeader("Type", "Heartbeats");
        exchange.getIn().setHeader("Source", "OVMM_EVENTS_ADAPTER");

        try {
        	//Processor processor = getProcessor();
        	//.process(exchange);
        	//processor.process(exchange);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} 
	}
	
	
	// "throws Exception" 
	private int processSearchEvents()  throws Exception, Error, SQLException {
		
		//Long timestamp;
		BasicDataSource dataSource = setupDataSource();
		
		List<HashMap<String, Object>> listHostsAndUuids = new ArrayList<HashMap<String,Object>>();
		List<HashMap<String, Object>> listVmStatuses = null;
		int events = 0;
		int statuses = 0;
		try {
			
			logger.info( String.format("***Try to get VMs***"));
			
			listHostsAndUuids = getHostsAndUuids(dataSource);
			
			logger.info( String.format("***Received %d VMs from SQL***", listHostsAndUuids.size()));
			String vmtitle, vmuuid, id;
			
			logger.info( String.format("***Try to get VMs statuses***"));
			for(int i=0; i < listHostsAndUuids.size(); i++) {
			  	
				id = listHostsAndUuids.get(i).get("id").toString();
				vmtitle = listHostsAndUuids.get(i).get("title").toString();
				vmuuid  = listHostsAndUuids.get(i).get("uuid").toString();
				logger.debug("MYSQL row " + i + ": " + vmtitle + 
						" " + vmuuid);
				
				listVmStatuses = getVmStatuses(vmuuid, dataSource);
				
				if ( listVmStatuses != null ){
					HashMap<String, Object> sss = listVmStatuses.get(0);
					
					List<Event> vmevents = genEvents(vmtitle, sss, id);
					
					for(int i1=0; i1 < vmevents.size(); i1++) {
						
						statuses++;
						
						logger.debug("*** Create Exchange ***" );
						
						
						String key = vmevents.get(i1).getHost() + "_" +
								vmevents.get(i1).getObject() + "_" + vmevents.get(i1).getParametrValue();
						String key1 = vmevents.get(i1).getHost() + "_" +
								vmevents.get(i1).getObject();
						
						Exchange exchange = getEndpoint().createExchange();
						exchange.getIn().setBody(vmevents.get(i1), Event.class);
						exchange.getIn().setHeader("EventUniqId", key);
						
						exchange.getIn().setHeader("EventUniqIdWithoutStatus", key1);
						
						exchange.getIn().setHeader("EventStatus", vmevents.get(i1).getParametrValue());
						
						exchange.getIn().setHeader("Object", vmevents.get(i1).getObject());
						exchange.getIn().setHeader("Timestamp", vmevents.get(i1).getTimestamp());
						
						exchange.getIn().setHeader(CacheConstants.CACHE_OPERATION, CacheConstants.CACHE_OPERATION_CHECK);
						exchange.getIn().setHeader(CacheConstants.CACHE_KEY, key);
						
						exchange.getIn().setHeader("CamelCacheOperation1", "CamelCacheCheck");
						exchange.getIn().setHeader("CamelCacheKey1", key);
						
						logger.debug(String.format("*** CACHE HEADERS: %s %s  ***", CacheConstants.CACHE_OPERATION, CacheConstants.CACHE_OPERATION_CHECK ));
						logger.debug(String.format("*** CACHE HEADERS: %s %s  ***", CacheConstants.CACHE_KEY, key ));
						
						exchange.getIn().setHeader("TEST", key);
						//exchange.getIn().setHeader("DeviceType", vmevents.get(i).getDeviceType());
						
						
	
						try {
							getProcessor().process(exchange);
							events++;
							
							//File cachefile = new File("sendedEvents.dat");
							//removeLineFromFile("sendedEvents.dat", "Tgc1-1Cp1_ping_OK");
							
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							logger.error( String.format("Error while process Exchange message: %s ", e));
						} 
						
						/*
						logger.debug("*** Create Exchange for DELETE ***" );
						
						
						context = getContext();
						
						Endpoint endpoint = context.getEndpoint("cache://ServerCacheTest");
						logger.debug("*** endpoint ***" + endpoint );
						Exchange exchange1 = endpoint.createExchange();
						logger.debug("*** exchange1 ***" + exchange1 );
					    //exchange.getIn().setBody(vmevents.get(i1), Event.class);
					    exchange1.getIn().setHeader(CacheConstants.CACHE_OPERATION, CacheConstants.CACHE_OPERATION_DELETE);
					    exchange1.getIn().setHeader(CacheConstants.CACHE_KEY, key1+"_OK");
					    //exchange1.getIn().setHeader("EventId", event.getExternalid());

					    
					    Producer producer2 = null;
						try {
							producer2 = endpoint.createProducer();
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
					    try {
					    	producer2.process(exchange1);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						*/
					
					}
				}
				//genevent = geEventObj( device, "fcFabric" );
				
				/*
				logger.info("Create Exchange container");
				Exchange exchange = getEndpoint().createExchange();
				exchange.getIn().setBody(listFinal.get(i), Device.class);
				exchange.getIn().setHeader("DeviceId", listFinal.get(i).getId());
				exchange.getIn().setHeader("DeviceType", listFinal.get(i).getDeviceType());
				
				

				try {
					getProcessor().process(exchange);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
				*/
	  	
			}
			
			logger.info( String.format("***Received %d VMs statuses from SQL*** ", statuses));
	  
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error( String.format("Error while get Events from SQL: %s ", e));
			genErrorMessage(e.toString());
			dataSource.close();
			return 0;
		}
		catch (NullPointerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error( String.format("Error while get Events from SQL: %s ", e));
			genErrorMessage(e.toString());
			dataSource.close();
			return 0;
		}
		catch (Error e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error( String.format("Error while get Events from SQL: %s ", e));
			genErrorMessage(e.toString());
			dataSource.close();
			return 0;
		}
		catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error( String.format("Error while get Events from SQL: %s ", e));
			genErrorMessage(e.getMessage() + " " + e.toString());
			dataSource.close();
			return 0;
		}
		finally
		{
			dataSource.close();
			//return 0;
		}
		
		dataSource.close();
		
		logger.info( String.format("***Sended to Exchange messages: %d ***", events));
		
		removeLineFromFile("sendedEvents.dat", "Tgc1-1Cp1_ping_OK");
	
        return 1;
	}
	
	private static List<Event> genEvents( String vmtitle, HashMap<String, Object> vmStatuses, String id ) {
		
		String datetime = vmStatuses.get("datetime").toString();
		String ping_colour = vmStatuses.get("ping_colour").toString();
		String snmp_colour = vmStatuses.get("snmp_colour").toString();
		String vmtools_colour = vmStatuses.get("vmtools_colour").toString();
		String backup_colour = vmStatuses.get("backup_colour").toString();
		String power_colour = vmStatuses.get("power_colour").toString();
		//String ping_colour1 = listVmStatuses.get(0).get("ping_colour").toString();
		//vmuuid  = listVmStatuses.get(0).get("uuid").toString();
		logger.debug(vmtitle + ": " + datetime );
		logger.debug(vmtitle + ": " + ping_colour);
		logger.debug(vmtitle + ": " + snmp_colour);
		logger.debug(vmtitle + ": " + vmtools_colour);
		logger.debug(vmtitle + ": " + backup_colour);
		logger.debug(vmtitle + ": " + power_colour);
		
		Event event;
		List<Event> eventList = new ArrayList<Event>();
		// Create Event object for further use
		event = genDeviceObj(vmtitle, "ping", 
				vmStatuses.get("ping_colour").toString(), vmStatuses.get("datetime").toString(),id);
		eventList.add(event);
		/*
		 event = genDeviceObj(vmtitle, "ping", 
				"#FF0000", vmStatuses.get("datetime").toString());
		eventList.add(event);
		*/
		
		
		event = genDeviceObj(vmtitle, "snmp", 
				vmStatuses.get("snmp_colour").toString(), vmStatuses.get("datetime").toString(), id);
		eventList.add(event);
		event = genDeviceObj(vmtitle, "vmtools", 
				vmStatuses.get("vmtools_colour").toString(), vmStatuses.get("datetime").toString(), id);
		eventList.add(event);
		event = genDeviceObj(vmtitle, "backup", 
				vmStatuses.get("backup_colour").toString(), vmStatuses.get("datetime").toString(),id);
		eventList.add(event);
		event = genDeviceObj(vmtitle, "power", 
				vmStatuses.get("power_colour").toString(), vmStatuses.get("datetime").toString(),id);
		eventList.add(event);
		
		return eventList;
	}

	private static Event genDeviceObj( String vmtitle, String object, String parametrValue, String datetime, String id ) {
		Event event;
		
		event = new Event();
		//Timestamp timestamp = null;
		long timeInMillisSinceEpoch = 0;
		//long timeInMinutesSinceEpoch = 0;
		//DATE FORMAT: 2015-11-02 17:55:33.0
		String eventdate = datetime;
		try {
		    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
		    Date parsedDate = dateFormat.parse(eventdate);
		    timeInMillisSinceEpoch = parsedDate.getTime() / 1000; 
		    //timeInMinutesSinceEpoch = timeInMillisSinceEpoch / (60 * 1000);
		} catch(Exception e) {
			//this generic but you can control another types of exception
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (timeInMillisSinceEpoch != 0){
			logger.debug("timeInMillisSinceEpoch: " + timeInMillisSinceEpoch);
			//logger.info("timeInMinutesSinceEpoch: " + timeInMinutesSinceEpoch);
			event.setTimestamp(timeInMillisSinceEpoch);
		}
		
		event.setHost(vmtitle);
		event.setCi("OVMM:"+id);
		//vmStatuses.get("ping_colour").toString())
		event.setObject(object);
		event.setParametr("Status");
		String status = setRightValue(parametrValue);
		event.setParametrValue(status);
		event.setSeverity(setRightSeverity(parametrValue));
		event.setMessage(setRightMessage(vmtitle, object, status));
		event.setCategory("SYSTEM");
		//event.setStatus("OPEN");
		event.setStatus(setRightStatus(parametrValue));
		event.setService("OVMM");
		event.setEventsource("OVMM");
		

		//System.out.println(event.toString());
		
		logger.debug(event.toString());
		
		return event;
				
	}
	
	
	
	private static String setRightMessage(String vmtitle, String object, String status) {
		// TODO Auto-generated method stub
		
		String newmessage = String.format("ВМ: %s. Статус проверки %s: %s", vmtitle, object, status);
		return newmessage;
	}

	private List<HashMap<String, Object>> getVmStatuses(String vmuuid, DataSource dataSource) throws SQLException {
		// TODO Auto-generated method stub
		List<HashMap<String,Object>> list = new ArrayList<HashMap<String,Object>>();
		
        Connection con = null; 
        PreparedStatement pstmt;
        ResultSet resultset = null;
        try {
        	con = (Connection) dataSource.getConnection();
			//con.setAutoCommit(false);
			
        	String table_prefix =  endpoint.getConfiguration().getTable_prefix();
        	
        	String vmtable = table_prefix + vmuuid;

        	
            pstmt = con.prepareStatement(String.format("SELECT datetime, %s_colour as ping_colour, "
            			+ " %s_colour as snmp_colour, %s_colour as vmtools_colour, "
            			+ " %s_colour as backup_colour, %s_colour as power_colour "
            			+ "FROM `%s`" , vm_ping_col, vm_snmp_col, vm_vmtools_col, vm_backup_col, vm_power_col, vmtable ));
            //pstmt.setString(1, vm_vmtools_col);
            
            logger.debug("MYSQL query: " +  pstmt.toString()); 
            resultset = pstmt.executeQuery();
            //con.commit();
            list = convertRStoList(resultset);
            
            //list.get(0).get(ping_colour);
            
            
            resultset.close();
            pstmt.close();
            if (con != null) con.close();
            return list;
            
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			//logger.error( "Error1 while SQL execution: " + e.getMessage() );
			//logger.error( "Error2 while SQL execution: " + e.getCause() );
			logger.error( String.format("Error while SQL execution: %s ", e));
			//logger.error( ExceptionUtils.getFullStackTrace(e) );
			 if (con != null) con.close();
			//logger.error("Error while SQL executiom: " + e.printStackTrace());
			throw e;
			//return null;

		} finally {
            if (con != null) con.close();
        }
		
	}

	private List<HashMap<String, Object>> getHostsAndUuids(DataSource dataSource) throws SQLException {
		// TODO Auto-generated method stub
        
		List<HashMap<String,Object>> list = new ArrayList<HashMap<String,Object>>();
		
        Connection con = null; 
        PreparedStatement pstmt;
        ResultSet resultset = null;
        try {
        	con = (Connection) dataSource.getConnection();
			//con.setAutoCommit(false);
			
            pstmt = con.prepareStatement("SELECT tree.id, tree.title, tree.type, tree.uuid " +
                        "FROM tree " +
                        "WHERE tree.uuid <> ?");
                       // +" LIMIT ?;");
            pstmt.setString(1, "");
            //pstmt.setInt(2, 3);
            
            logger.debug("MYSQL query: " +  pstmt.toString()); 
            resultset = pstmt.executeQuery();
            //con.commit();
            
            list = convertRStoList(resultset);
            
            
            resultset.close();
            pstmt.close();
            
            if (con != null) con.close();
            
            return list;
            
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			logger.error( String.format("Error while SQL execution: %s ", e));
			
			if (con != null) con.close();
			
			//return null;
			throw e;

		} finally {
            if (con != null) con.close();
            
            //return list;
        }
		
	}
	
	private List<HashMap<String, Object>> convertRStoList(ResultSet resultset) throws SQLException {
		
		List<HashMap<String,Object>> list = new ArrayList<HashMap<String,Object>>();
		
		try {
			ResultSetMetaData md = resultset.getMetaData();
	        int columns = md.getColumnCount();
	        //result.getArray(columnIndex)
	        //resultset.get
	        logger.debug("MYSQL columns count: " + columns); 
	        
	        resultset.last();
	        int count = resultset.getRow();
	        logger.debug("MYSQL rows2 count: " + count); 
	        resultset.beforeFirst();
	        
	        int i = 0, n = 0;
	        //ArrayList<String> arrayList = new ArrayList<String>(); 
	
	        while (resultset.next()) {              
	        	HashMap<String,Object> row = new HashMap<String, Object>(columns);
	            for(int i1=1; i1<=columns; ++i1) {
	                row.put(md.getColumnLabel(i1),resultset.getObject(i1));
	            }
	            list.add(row);                 
	        }
	        
	        return list;
	        
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
			return null;

		} finally {

		}
	}

	private BasicDataSource setupDataSource() {
		
		String url = String.format("jdbc:mysql://%s:%s/%s",
		endpoint.getConfiguration().getMysql_host(), endpoint.getConfiguration().getMysql_port(),
		endpoint.getConfiguration().getMysql_db());
		
        BasicDataSource ds = new BasicDataSource();
        ds.setDriverClassName("com.mysql.jdbc.Driver");
        ds.setUsername( endpoint.getConfiguration().getUsername() );
        ds.setPassword( endpoint.getConfiguration().getPassword() );
        ds.setUrl(url);
              
        return ds;
    }
	
	public static String setRightValue(String colour)
	{
		String newstatus = "";
		
		switch (colour) {
        	case "#006600":  newstatus = "OK";break;
        	case "#FF0000":  newstatus = "ERROR";break;
        	default: newstatus = "NA";break;

        	
		}
		/*
		System.out.println("***************** colour: " + colour);
		System.out.println("***************** status: " + newstatus);
		*/
		return newstatus;
	}
	
	public static String setRightSeverity(String colour)
	{
		String newseverity = "";
			
		switch (colour) {
        	case "#006600":  newseverity = PersistentEventSeverity.OK.name();break;
        	case "#FF0000":  newseverity = PersistentEventSeverity.CRITICAL.name();break;
        	default: newseverity = PersistentEventSeverity.INFO.name();break;

        	
		}
		/*
		System.out.println("***************** colour: " + colour);
		System.out.println("***************** newseverity: " + newseverity);
		*/
		return newseverity;
	}
	
	public static String setRightStatus(String colour)
	{
		String newseverity = "";
			
		switch (colour) {
        	case "#006600":  newseverity = "CLOSED";break;
        	case "#FF0000":  newseverity = "OPEN";break;
        	default: newseverity = PersistentEventSeverity.INFO.name();break;

        	
		}
		/*
		System.out.println("***************** colour: " + colour);
		System.out.println("***************** newseverity: " + newseverity);
		*/
		return newseverity;
	}
	
	public void removeLineFromFile(String file, String lineToRemove) {
		BufferedReader br = null;
		PrintWriter pw = null;
	    try {

	      File inFile = new File(file);

	      if (!inFile.isFile()) {
	        System.out.println("Parameter is not an existing file");
	        return;
	      }

	      //Construct the new file that will later be renamed to the original filename.
	      File tempFile = new File(inFile.getAbsolutePath() + ".tmp");

	      br = new BufferedReader(new FileReader(file));
	      pw = new PrintWriter(new FileWriter(tempFile));

	      String line = null;

	      //Read from the original file and write to the new
	      //unless content matches data to be removed.
	      while ((line = br.readLine()) != null) {

	        if (!line.trim().equals(lineToRemove)) {

	          pw.println(line);
	          pw.flush();
	        }
	      }
	      pw.close();
	      br.close();

	      //Delete the original file
	      if (!inFile.delete()) {
	        System.out.println("Could not delete file");
	        return;
	      }

	      //Rename the new file to the filename the original file had.
	      if (!tempFile.renameTo(inFile))
	        System.out.println("Could not rename file");

	    }
	    catch (FileNotFoundException ex) {
	      ex.printStackTrace();
	    }
	    catch (IOException ex) {
	      ex.printStackTrace();
	    }
	    finally {
	    	try {
	    		pw.close();
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	  }

}