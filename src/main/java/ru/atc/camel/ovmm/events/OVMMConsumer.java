package ru.atc.camel.ovmm.events;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.ScheduledPollConsumer;

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
import ru.atc.camel.ovmm.events.api.OVMMDevices;
import ru.atc.camel.ovmm.events.api.OVMMEvents;

import javax.sql.DataSource;
import org.apache.commons.dbcp.BasicDataSource;

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
        this.setDelay(endpoint.getConfiguration().getDelay());
	}
	
	@Override
	protected int poll() throws Exception {
		
		String operationPath = endpoint.getOperationPath();
		
		if (operationPath.equals("events")) return processSearchEvents();
		
		// only one operation implemented for now !
		throw new IllegalArgumentException("Incorrect operation: " + operationPath);
	}
	
	// "throws Exception" 
	private int processSearchEvents() {
		
		//Long timestamp;
		DataSource dataSource = setupDataSource();
		
		List<HashMap<String, Object>> listHostsAndUuids = new ArrayList<HashMap<String,Object>>();
		List<HashMap<String, Object>> listVmStatuses = new ArrayList<HashMap<String,Object>>();
		try {
			listHostsAndUuids = getHostsAndUuids(dataSource);
			String vmtitle, vmuuid;
			for(int i=0; i < listHostsAndUuids.size(); i++) {
			  	
				vmtitle = listHostsAndUuids.get(i).get("title").toString();
				vmuuid  = listHostsAndUuids.get(i).get("uuid").toString();
				logger.info("MYSQL row " + i + ": " + vmtitle + 
						" " + vmuuid);
				
				listVmStatuses = getVmStatuses(vmuuid, dataSource);
				
				HashMap<String, Object> sss = listVmStatuses.get(0);
				
				List<Event> vmevents = genEvents(vmtitle, sss);
				
				for(int i1=0; i1 < vmevents.size(); i1++) {
					
					logger.info("*** Create Exchange ***");
					Exchange exchange = getEndpoint().createExchange();
					exchange.getIn().setBody(vmevents.get(i1), Event.class);
					exchange.getIn().setHeader("EventUniqId", vmevents.get(i1).getHost() + "_" +
							vmevents.get(i1).getObject() + "_" + vmevents.get(i1).getParametrValue());
					exchange.getIn().setHeader("Object", vmevents.get(i1).getObject());
					exchange.getIn().setHeader("Timestamp", vmevents.get(i1).getTimestamp());
					//exchange.getIn().setHeader("DeviceType", vmevents.get(i).getDeviceType());
					
					

					try {
						getProcessor().process(exchange);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
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
	  
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	
	
        return 1;
	}
	
	private static List<Event> genEvents( String vmtitle, HashMap<String, Object> vmStatuses ) {
		
		String datetime = vmStatuses.get("datetime").toString();
		String ping_colour = vmStatuses.get("ping_colour").toString();
		String snmp_colour = vmStatuses.get("snmp_colour").toString();
		String vmtools_colour = vmStatuses.get("vmtools_colour").toString();
		String backup_colour = vmStatuses.get("backup_colour").toString();
		String power_colour = vmStatuses.get("power_colour").toString();
		//String ping_colour1 = listVmStatuses.get(0).get("ping_colour").toString();
		//vmuuid  = listVmStatuses.get(0).get("uuid").toString();
		logger.info(vmtitle + ": " + datetime );
		logger.info(vmtitle + ": " + ping_colour);
		logger.info(vmtitle + ": " + snmp_colour);
		logger.info(vmtitle + ": " + vmtools_colour);
		logger.info(vmtitle + ": " + backup_colour);
		logger.info(vmtitle + ": " + power_colour);
				
		// Create Device object for further use
		Event event = genDeviceObj(vmtitle, vmStatuses);
		List<Event> eventList = new ArrayList<Event>();;
		eventList.add(event);

		
		return eventList;
	}

	public static Event genDeviceObj( String vmtitle, HashMap<String, Object> vmStatuses) {
		Event event;
		
		event = new Event();
		//Timestamp timestamp = null;
		long timeInMillisSinceEpoch = 0;
		//long timeInMinutesSinceEpoch = 0;
		//DATE FORMAT: 2015-11-02 17:55:33.0
		String eventdate = vmStatuses.get("datetime").toString();
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
			logger.info("timeInMillisSinceEpoch: " + timeInMillisSinceEpoch);
			//logger.info("timeInMinutesSinceEpoch: " + timeInMinutesSinceEpoch);
			event.setTimestamp(timeInMillisSinceEpoch);
		}
		
		event.setHost(vmtitle);
		event.setCi(vmtitle);
		//vmStatuses.get("ping_colour").toString())
		event.setObject("ping_colour");
		event.setParametr("Status");
		event.setParametrValue(vmStatuses.get("ping_colour").toString());
		event.setCategory("SYSTEM");
		event.setStatus("OPEN");
		event.setService("OVVM");
		/*

		//System.out.println(event.toString());
		
		log.info(event.toString());
		*/
		return event;
				
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
			
        	String vmtable = "b_VM" + vmuuid;

        	
            pstmt = con.prepareStatement(String.format("SELECT datetime, %s_colour as ping_colour, "
            			+ " %s_colour as snmp_colour, %s_colour as vmtools_colour, "
            			+ " %s_colour as backup_colour, %s_colour as power_colour "
            			+ "FROM `%s`" , vm_ping_col, vm_snmp_col, vm_vmtools_col, vm_backup_col, vm_power_col, vmtable ));
            //pstmt.setString(1, vm_vmtools_col);
            
            logger.info("MYSQL query: " +  pstmt.toString()); 
            resultset = pstmt.executeQuery();
            //con.commit();
            list = convertRStoList(resultset);
            
            //list.get(0).get(ping_colour);
            
            
            resultset.close();
            pstmt.close();
            
            return list;
            
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
			return null;

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
			
            pstmt = con.prepareStatement("SELECT tree.title, tree.type, tree.uuid " +
                        "FROM tree " +
                        "WHERE tree.uuid <> ?");
            pstmt.setString(1, "");
            
            logger.info("MYSQL query: " +  pstmt.toString()); 
            resultset = pstmt.executeQuery();
            //con.commit();
            
            list = convertRStoList(resultset);
            
            
            resultset.close();
            pstmt.close();
            
            return list;
            
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
			return null;

		} finally {
            if (con != null) con.close();
        }
		
	}
	
	private List<HashMap<String, Object>> convertRStoList(ResultSet resultset) throws SQLException {
		
		List<HashMap<String,Object>> list = new ArrayList<HashMap<String,Object>>();
		
		try {
			ResultSetMetaData md = resultset.getMetaData();
	        int columns = md.getColumnCount();
	        //result.getArray(columnIndex)
	        //resultset.get
	        logger.info("MYSQL columns count: " + columns); 
	        
	        resultset.last();
	        int count = resultset.getRow();
	        logger.info("MYSQL rows2 count: " + count); 
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

	private DataSource setupDataSource() {
		
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

}