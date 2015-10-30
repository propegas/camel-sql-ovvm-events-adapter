package ru.atc.camel.ovmm.events;

import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriParams;

@UriParams
public class OVMMConfiguration {	
    
	private String username;
	
	private String password;
	
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


}