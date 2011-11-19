/**
 * 
 */
package org.savara.sam.web.client.view;

import org.savara.sam.web.client.presenter.SituationLayoutPresenter;
import org.savara.sam.web.client.presenter.SituationLayoutPresenter.SituationLayoutView;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.ViewImpl;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.toolbar.ToolStrip;
import com.smartgwt.client.widgets.toolbar.ToolStripButton;

/**
 * @author Jeff Yu
 * @date Nov 17, 2011
 */
public class SituationLayoutViewImpl extends ViewImpl implements SituationLayoutView {

	private SituationLayoutPresenter presenter;
		
	private VLayout panel;
	
	private ListGrid notificationList;
	
	@Inject
	public SituationLayoutViewImpl() {
        
		panel  = LayoutUtil.getPagePanel();
		panel.addMember(LayoutUtil.getHeaderLayout());
		
		HLayout body = new HLayout();
		body.setWidth100();
		body.setPadding(3);
		body.setHeight(850);
		panel.addMember(body);
				
		body.addMember(LayoutUtil.getMenuStack());
		
		VLayout main = new VLayout();
		main.setMargin(5);
		body.addMember(main);
		
		main.addMember(getNotificationList());
        panel.addMember(LayoutUtil.getFooterLayout());
        
	}


	private VLayout getNotificationList() {
		
		VLayout situationList = new VLayout();
		situationList.setWidth100();
		
		ToolStrip situationTS = new ToolStrip();
		situationTS.setWidth100();
		
		ToolStripButton refresh = new ToolStripButton("Refresh", "[SKIN]/headerIcons/refresh.png");
		refresh.addClickHandler(new ClickHandler(){
			public void onClick(ClickEvent event) {
				presenter.refreshData();
			}			
		});
		situationTS.addButton(refresh);
    
        notificationList = new ListGrid();  
        notificationList.setWidth100();
        notificationList.setShowAllRecords(true);  
        notificationList.setCellHeight(22);  
  
        ListGridField idField = new ListGridField("id", "ID");
        ListGridField desField = new ListGridField("description", "Description");
        ListGridField severityField = new ListGridField("severity", "Severity");
        ListGridField priorityField = new ListGridField("priority", "Priority");
        ListGridField statusField = new ListGridField("status", "Status");
        ListGridField principalField = new ListGridField("principal", "Principal");
        ListGridField externalRefField = new ListGridField("externalRef", "External Reference");
        ListGridField ownerField = new ListGridField("owner", "Owner");
        ListGridField dateField = new ListGridField("date", "Date");
        
        notificationList.setFields(idField, desField,severityField, priorityField, statusField, principalField, externalRefField, ownerField, dateField);  
  
        notificationList.setCanEdit(true);  
        
        situationList.addMember(situationTS);
        situationList.addMember(notificationList);
                
        return situationList;
	}
	
	
	public void refreshData(ListGridRecord[] data) {
		notificationList.setData(data);
	}
	
	public void setPresenter(SituationLayoutPresenter presenter) {
		this.presenter = presenter;
	}
	
	
	public Widget asWidget() {
		return panel;
	}

}
