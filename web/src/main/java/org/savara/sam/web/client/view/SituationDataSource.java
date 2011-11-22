/**
 * 
 */
package org.savara.sam.web.client.view;

import java.util.List;

import org.savara.sam.web.shared.AQMonitorService;
import org.savara.sam.web.shared.AQMonitorServiceAsync;
import org.savara.sam.web.shared.dto.SituationDTO;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.fields.DataSourceDateField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.types.DateDisplayFormat;
import com.smartgwt.client.widgets.grid.ListGridRecord;

/**
 * @author Jeff Yu
 * @date Nov 19, 2011
 */

public class SituationDataSource extends GwtRpcDataSource {

    public SituationDataSource () {
        DataSourceField id = new DataSourceTextField("id", "ID", 100);
        id.setCanEdit(false);
        id.setCanFilter(false);
        id.setPrimaryKey(true);
        DataSourceField desField = new DataSourceTextField("description", "Description", 380);
        desField.setCanEdit(false);
        desField.setCanFilter(false);
        
        DataSourceField severity = new DataSourceTextField("severity", "Severity", 80);
        severity.setValueMap("Critical", "Major", "Minor");
        DataSourceField priorityField = new DataSourceTextField("priority", "Priority", 60);
        priorityField.setValueMap("Low","Medium", "High");
        DataSourceTextField statusField = new DataSourceTextField("status", "Status", 60);
        statusField.setValueMap("New", "Accepted", "Resolved", "Rejected");
        DataSourceTextField principalField = new DataSourceTextField("principal", "Principal", 60);
        principalField.setCanEdit(false);
        DataSourceTextField externalRefField = new DataSourceTextField("externalRef", "External Reference", 100);
        externalRefField.setCanFilter(false);
        externalRefField.setCanEdit(false);
        DataSourceTextField ownerField = new DataSourceTextField("owner", "Owner", 120);
        DataSourceField dateField = new DataSourceDateField("date", "Date");
        dateField.setDateFormatter(DateDisplayFormat.TOSERIALIZEABLEDATE);
        dateField.setCanFilter(false);
        dateField.setCanEdit(false);
        
        setFields(id, desField, severity, priorityField, statusField, principalField, externalRefField, ownerField, dateField);

    }

    @Override
    protected void executeFetch (final String requestId, final DSRequest request, final DSResponse response) {

        // Finding which rows were requested
        // Normaly these two indexes should be passed to server
        // but for this example I will do "paging" on client side
    	
    	 AQMonitorServiceAsync service = GWT.create(AQMonitorService.class);
    	 
    	//TODO: hack, for the hover event.
    	if (request.getStartRow() == null || request.getEndRow() == null) {
    	       service.getSituations(new AsyncCallback<List<SituationDTO>>() {

    				public void onFailure(Throwable error) {
    					response.setStatus(RPCResponse.STATUS_FAILURE);
    					processResponse(requestId, response);
    				}

    				public void onSuccess(List<SituationDTO> result) {
    					int num = request.getCriteria().getAttributeAsInt("rowNum");
    	                ListGridRecord[] list = new ListGridRecord[1];
    	                ListGridRecord record = new ListGridRecord ();
    	                copyValues (result.get(num), record);
    	                list[0] = record;
    	                response.setData (list);
    	                processResponse (requestId, response);				
    				}
    	        });
    		return;
    	}
    	
    	
        final int startIndex = (request.getStartRow() < 0)?0:request.getStartRow ();
        final int endIndex = (request.getEndRow()== null)?-1:request.getEndRow ();
        
        service.getSituations(new AsyncCallback<List<SituationDTO>>() {

			public void onFailure(Throwable error) {
				response.setStatus(RPCResponse.STATUS_FAILURE);
				processResponse(requestId, response);
			}

			public void onSuccess(List<SituationDTO> result) {
                // Calculating size of return list
                int size = result.size ();
                if (endIndex >= 0) {
                    if (endIndex < startIndex) {
                        size = 0;
                    }
                    else {
                        size = endIndex - startIndex + 1;
                    }
                }
                // Create list for return - it is just requested records
                ListGridRecord[] list = new ListGridRecord[size];
                if (size > 0) {
                    for (int i = 0; i < result.size (); i++) {
                        if (i >= startIndex && i <= endIndex) {
                            ListGridRecord record = new ListGridRecord ();
                            copyValues (result.get (i), record);
                            list[i - startIndex] = record;
                        }
                    }
                }
                response.setData (list);
                // IMPORTANT: for paging to work we have to specify size of full result set
                response.setTotalRows (result.size ());
                processResponse (requestId, response);				
			}
        	
        });
        
    }
    
    private static void copyValues (SituationDTO from, ListGridRecord to) {
        to.setAttribute ("id", from.getId());
        to.setAttribute ("severity", from.getSeverity());
        to.setAttribute ("date", from.getCreatedDate());
        to.setAttribute("priority", from.getPriority());
        to.setAttribute("status", from.getStatus());
        to.setAttribute("principal", from.getPrincipal());
        to.setAttribute("description", from.getDescription());
        to.setAttribute("externalRef", from.getExternalRef());
        to.setAttribute("owner", from.getOwner());
    }

    @Override
    protected void executeAdd (final String requestId, final DSRequest request, final DSResponse response) {
    	throw new UnsupportedOperationException("Add operation is not supported.");
    }

    @Override
    protected void executeUpdate (final String requestId, final DSRequest request, final DSResponse response) {
    	//TODO: need to implement this.
    	throw new UnsupportedOperationException("Update operation is not supported.");
    }

    @Override
    protected void executeRemove (final String requestId, final DSRequest request, final DSResponse response) {
    	throw new UnsupportedOperationException("Remove operation is not supported.");
    }


}