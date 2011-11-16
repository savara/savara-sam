/**
 * 
 */
package org.savara.sam.web.client.view;

import java.util.ArrayList;
import java.util.List;

import com.smartgwt.client.types.DragAppearance;
import com.smartgwt.client.types.HeaderControls;
import com.smartgwt.client.types.LayoutPolicy;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HeaderControl;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.events.CloseClientEvent;
import com.smartgwt.client.widgets.events.DragRepositionStopHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.Portlet;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.layout.VStack;

/**
 * Customized Portal Layout, to incorporate the PortalColumn feature,
 * so a new portlet will be added into the fewest portal column.
 * 
 * Portlet's title should be unique.
 * 
 * @author Jeff Yu
 * @date Nov 16, 2011
 */
public class ChartPortalLayout extends HLayout{
	
	private List<String> portletTitles = new ArrayList<String>();
	
	public ChartPortalLayout(int numColumns) {
		setMargin(0);
		setWidth100();
		setHeight100();
		setCanAcceptDrop(true);	
		
		for (int i = 0; i < numColumns; i++) {
			addMember(new ChartPortalColumn());
		}
	}
	
    public void addPortlet(ChartPortlet portlet) {    
        int fewestPortlets = Integer.MAX_VALUE;  
        ChartPortalColumn fewestPortletsColumn = null;  
        for (int i = 0; i < getMembers().length; i++) {  
            int numPortlets = ((ChartPortalColumn) getMember(i)).getMembers().length;  
            if (numPortlets < fewestPortlets) {  
                fewestPortlets = numPortlets;  
                fewestPortletsColumn = (ChartPortalColumn) getMember(i);  
            }  
        }  
        fewestPortletsColumn.addMember(portlet);
        portletTitles.add(portlet.getTitle());
    }
    
    public boolean isTitleUnique(String title) {
    	return portletTitles.contains(title);
    }
    
    public void removePortlet(String title) {
    	for (int i = 0; i< getMembers().length; i++) {
    		ChartPortalColumn portalColumn = (ChartPortalColumn)getMember(i);
    		for (int y = 0; y < portalColumn.getMembers().length; y++) {
    			ChartPortlet portlet = (ChartPortlet)portalColumn.getMember(y);
    			if (title.equals(portlet.getTitle())) {
    				portalColumn.removeMember(portlet);
    			}
    		}
    	}
    }
    
    public ChartPortlet getChartPortlet(String title) {
    	for (int i = 0; i< getMembers().length; i++) {
    		ChartPortalColumn portalColumn = (ChartPortalColumn)getMember(i);
    		for (int y = 0; y < portalColumn.getMembers().length; y++) {
    			ChartPortlet portlet = (ChartPortlet)portalColumn.getMember(y);
    			if (title.equals(portlet.getTitle())) {
    				return portlet;
    			}
    		}
    	}
    	return null;
    }
    
    public ChartPortlet createPortlet(String title, ClickHandler refreshHandler, ClickHandler maximizeHandler,
			DragRepositionStopHandler dsHandler) {
    	return new ChartPortlet(title, refreshHandler, maximizeHandler, dsHandler);       
    }
	
	
	public class ChartPortalColumn extends VStack {
		
		public ChartPortalColumn() {
            setMembersMargin(6);  
            
            // enable predefined component animation  
            setAnimateMembers(true);  
            setAnimateMemberTime(300);  
  
            // enable drop handling  
            setCanAcceptDrop(true);  
  
            // change appearance of drag placeholder and drop indicator  
            setDropLineThickness(4);  
  
            Canvas dropLineProperties = new Canvas();  
            dropLineProperties.setBackgroundColor("aqua");  
            setDropLineProperties(dropLineProperties);  
  
            setShowDragPlaceHolder(true);  
  
            Canvas placeHolderProperties = new Canvas();  
            placeHolderProperties.setBorder("2px solid #8289A6");  
            setPlaceHolderProperties(placeHolderProperties);		
		}
	
	}
	
	/**
	 *
	 * Portlet name should be unique.
	 *
	 */
	public  class ChartPortlet extends Portlet {
		
		private VLayout chartPanel;
				
		public ChartPortlet(final String title, ClickHandler refreshHandler, ClickHandler maximizeHandler,
				DragRepositionStopHandler dsHandler) {
			super();
	        setTitle(title);  
	        setShowShadow(false);
	        setDragAppearance(DragAppearance.OUTLINE);
	        setHeaderControls(HeaderControls.MINIMIZE_BUTTON, HeaderControls.HEADER_LABEL,
	        		new HeaderControl(HeaderControl.REFRESH, refreshHandler), new HeaderControl(HeaderControl.MAXIMIZE, maximizeHandler), HeaderControls.CLOSE_BUTTON);
	        
	        setVPolicy(LayoutPolicy.NONE);
	        setOverflow(Overflow.VISIBLE);
	        setAnimateMinimize(true);
	        setCanDrop(true);
	        
	        setWidth(300);
	        setHeight(250);
	        setCanDragResize(false);
	        
	        //TODO: how to add a confirmation box before closing the porlet?
	        setCloseConfirmationMessage("Are you going to close the " + title + " portlet ?");
	        setShowCloseConfirmationMessage(true);
	        
	        addCloseClickHandler(new CloseClickHandler(){
				public void onCloseClick(CloseClientEvent event) {					
					removePortlet(title);
				}	        	
	        });
	        
	        addDragRepositionStopHandler(dsHandler);
	        
	        
	        chartPanel = new VLayout();
	        chartPanel.setMargin(25);
	        addChild(chartPanel);
		}
		
		public VLayout getChartPanel() {
			return chartPanel;
		}
		
	}

}
