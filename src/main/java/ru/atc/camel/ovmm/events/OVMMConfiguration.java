package ru.atc.camel.ovmm.events;

import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriParams;

@UriParams
public class OVMMConfiguration {	
    
	private String username;
	
	private String password;
	
	@UriParam
    private String mysql_db;
    
    @UriParam
    private String mysql_host;
    
    @UriParam
    private String mysql_port;
    
    @UriParam
    private String query;
    
    @UriParam(defaultValue = "60000")
    private int delay = 60000;

	public int getDelay() {
		return delay;
	}

	public void setDelay(int delay) {
		this.delay = delay;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getMysql_port() {
		return mysql_port;
	}

	public void setMysql_port(String mysql_port) {
		this.mysql_port = mysql_port;
	}

	public String getMysql_host() {
		return mysql_host;
	}

	public void setMysql_host(String mysql_host) {
		this.mysql_host = mysql_host;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public String getMysql_db() {
		return mysql_db;
	}

	public void setMysql_db(String mysql_db) {
		this.mysql_db = mysql_db;
	}


}