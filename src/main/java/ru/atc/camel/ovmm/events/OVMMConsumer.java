package ru.atc.camel.ovmm.events;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.ScheduledPollConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import com.apc.stdws.xsd.isxcentral._2009._10.ISXCAlarmSeverity;
//import com.apc.stdws.xsd.isxcentral._2009._10.ISXCAlarm;
//import com.apc.stdws.xsd.isxcentral._2009._10.ISXCDevice;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
//import com.mysql.jdbc.Connection;
import com.mysql.jdbc.Driver;


public class OVMMConsumer extends ScheduledPollConsumer {
	
	private static Logger logger = LoggerFactory.getLogger(Main.class);
	
	public OVMMEndpoint endpoint;
	
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
	private int processSearchEvents() throws SQLException {
		
		//Long timestamp;
		
		String url = String.format("jdbc:mysql://%s:%s/%s",
				endpoint.getConfiguration().getMysql_host(), endpoint.getConfiguration().getMysql_port(),
				endpoint.getConfiguration().getMysql_db());
        DataSource dataSource = setupDataSource(url);
        Connection con = null; 
        PreparedStatement pstmt;
        ResultSet resultset = null;
        int columns = 0;
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
            ResultSetMetaData md = resultset.getMetaData();
            columns = md.getColumnCount();
            //result.getArray(columnIndex)
            //resultset.get
            logger.info("MYSQL columns count: " + columns); 
            
            resultset.last();
            int count = resultset.getRow();
            resultset.beforeFirst();
            
            int i = 0, n = 0;
            //ArrayList<String> arrayList = new ArrayList<String>(); 
            List<HashMap<String,Object>> list = new ArrayList<HashMap<String,Object>>();
            while (resultset.next()) {              
            	HashMap<String,Object> row = new HashMap<String, Object>(columns);
                for(int i1=1; i1<=columns; ++i1) {
                    row.put(md.getColumnName(i1),resultset.getObject(i1));
                }
                list.add(row);                 
            }
            
            for(int i1=0; i1 < list.size(); i1++) {
            	
            	
            	logger.info("MYSQL row " + i1 + ": " + list.get(i1).get("title").toString() + " " + list.get(i1).get("uuid").toString());
            	
            }
            
            logger.info("MYSQL rows count: " + n); 
            
            logger.info("MYSQL rows2 count: " + count); 
            
            resultset.close();
            pstmt.close();
            
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		} finally {
            if (con != null) con.close();
        }
		
       // con.
        
       
		
        return 1;
	}
	
	private DataSource setupDataSource(String connectURI) {
        BasicDataSource ds = new BasicDataSource();
        ds.setDriverClassName("com.mysql.jdbc.Driver");
        ds.setUsername( endpoint.getConfiguration().getUsername() );
        ds.setPassword( endpoint.getConfiguration().getPassword() );
        ds.setUrl(connectURI);
        return ds;
    }

}