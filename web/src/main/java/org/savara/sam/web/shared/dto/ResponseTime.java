/**
 * 
 */
package org.savara.sam.web.shared.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Jeff Yu
 * @date Nov 6, 2011
 */
public class ResponseTime implements Serializable {
	
	private static final long serialVersionUID = 3141906237887410242L;

	private Date requestTime;
	
	private String operation;
	
	private long responseTime;

	public Date getRequestTime() {
		return requestTime;
	}

	public void setRequestTime(Date requestTime) {
		this.requestTime = requestTime;
	}

	public String getOperation() {
		return operation;
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}

	public long getResponseTime() {
		return responseTime;
	}

	public void setResponseTime(long responseTime) {
		this.responseTime = responseTime;
	}
	
	

}
