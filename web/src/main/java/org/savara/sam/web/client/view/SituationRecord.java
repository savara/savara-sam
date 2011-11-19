/**
 * 
 */
package org.savara.sam.web.client.view;

import java.util.Date;

import com.smartgwt.client.widgets.grid.ListGridRecord;

/**
 * @author Jeff Yu
 * @date Nov 18, 2011
 */
public class SituationRecord extends ListGridRecord {
	
	public SituationRecord() {
		
	}
	
	public String getId() {
		return getAttributeAsString("id");
	}

	public void setId(String id) {
		setAttribute("id", id);
	}

	public Date getCreatedDate() {
		return getAttributeAsDate("date");
	}

	public void setCreatedDate(Date createdDate) {
		setAttribute("date", createdDate);
	}

	public String getSeverity() {
		return getAttributeAsString("severity");
	}

	public void setSeverity(String severity) {
		setAttribute("severity", severity);
	}

	public String getPriority() {
		return getAttributeAsString("priority");
	}

	public void setPriority(String priority) {
		setAttribute("priority", priority);
	}

	public String getStatus() {
		return getAttributeAsString("status");
	}

	public void setStatus(String status) {
		setAttribute("status", status);
	}

	public String getPrincipal() {
		return getAttributeAsString("princial");
	}

	public void setPrincipal(String principal) {
		setAttribute("principal", principal);
	}

	public String getDescription() {
		return getAttributeAsString("description");
	}

	public void setDescription(String description) {
		setAttribute("description", description);
	}

	public String getExternalRef() {
		return getAttributeAsString("externalRef");
	}

	public void setExternalRef(String externalRef) {
		setAttribute("externalRef", externalRef);
	}

	public String getOwner() {
		return getAttributeAsString("owner");
	}

	public void setOwner(String owner) {
		setAttribute("owner", owner);
	}
	
}
