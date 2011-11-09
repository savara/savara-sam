/**
 * 
 */
package org.savara.sam.web.shared.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Jeff Yu
 * @date Nov 6, 2011
 */
public class ResponseTime implements Serializable {
	
	private static final long serialVersionUID = 3141906237887410242L;

	private Long requestTime;
	
	private String operation;
	
	private Long responseTime;

	public Long getRequestTime() {
		return requestTime;
	}

	public void setRequestTime(Long requestTime) {
		this.requestTime = requestTime;
	}

	public String getOperation() {
		return operation;
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}

	public Long getResponseTime() {
		return responseTime;
	}

	public void setResponseTime(Long responseTime) {
		this.responseTime = responseTime;
	}

}
