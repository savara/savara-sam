/**
 * 
 */
package org.savara.sam.web.shared.dto;

import java.io.Serializable;

/**
 * @author Jeff Yu
 * @date Nov 11, 2011
 */
public class Conversation implements Serializable {
	
	private static final long serialVersionUID = 6392440811911124380L;

	private String conversationId;
	
	private Boolean status;

	public String getConversationId() {
		return conversationId;
	}

	public void setConversationId(String conversationId) {
		this.conversationId = conversationId;
	}

	public Boolean getStatus() {
		return status;
	}

	public void setStatus(Boolean status) {
		this.status = status;
	}
	
}
