/**
 * 
 */
package org.savara.sam.web.client.presenter;

import java.util.List;
import java.util.Map;

import org.savara.sam.web.client.BootstrapContext;
import org.savara.sam.web.client.NameTokens;
import org.savara.sam.web.client.util.DefaultCallback;
import org.savara.sam.web.shared.AQMonitorService;
import org.savara.sam.web.shared.AQMonitorServiceAsync;
import org.savara.sam.web.shared.dto.AQChartModel;
import org.savara.sam.web.shared.dto.Conversation;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.GwtEvent;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.ContentSlot;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.NoGatekeeper;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.PlaceRequest;
import com.gwtplatform.mvp.client.proxy.ProxyPlace;
import com.gwtplatform.mvp.client.proxy.RevealContentHandler;
import com.gwtplatform.mvp.client.proxy.RevealRootLayoutContentEvent;
import com.smartgwt.client.widgets.layout.VLayout;

/**
 * 
 * @author Jeff Yu
 * @date Nov 03, 2011
 */
public class MainLayoutPresenter extends Presenter<MainLayoutPresenter.MainLayoutView, 
												   MainLayoutPresenter.MainLayoutProxy> {
	
	private BootstrapContext bootstrap;
	
	private AQMonitorServiceAsync service;
	
	private PlaceManager placeManager;
		
	public interface MainLayoutView extends View {
				
		public void setPresenter(MainLayoutPresenter presenter);
				
		public void refreshConversationChart(AQChartModel model, List<Conversation> conversations, VLayout panel, int width, int height);
		
		public void refreshConversationChart(AQChartModel model, List<Conversation> conversations);
				
		public void refreshChart(AQChartModel model, Map data, VLayout panel, int width, int height);
		
		public void refreshChart(AQChartModel model, Map data);
		
		public void setActiveQueries(List<String> activeQueries);
		
	}
		
	@ProxyCodeSplit
	@NameToken(NameTokens.MAIN_VIEW)
	@NoGatekeeper
	public interface MainLayoutProxy extends ProxyPlace<MainLayoutPresenter>{}
	
    @Inject
    public MainLayoutPresenter(
            EventBus eventBus,
            MainLayoutView view,
            MainLayoutProxy proxy, BootstrapContext bootstrap, PlaceManager placeManager) {
        super(eventBus, view, proxy);
        this.bootstrap = bootstrap;
        this.placeManager = placeManager;
        service = (AQMonitorServiceAsync)GWT.create(AQMonitorService.class);  
    }
	
    
    
	@Override
	protected void revealInParent() {
		RevealRootLayoutContentEvent.fire(this, this);
	}
	
	@Override
	public void onBind() {
		super.onBind();
		getView().setPresenter(this);
	}
	
	public void refreshChartData(final AQChartModel model) {
		service.getChartData(model, new DefaultCallback<Map>(){
			public void onSuccess(Map data) {
				getView().refreshChart(model, data);
			}
						
		});
	}
	
	public void refreshChartData(final AQChartModel model, final VLayout panel, final int width, final int height) {
		service.getChartData(model, new DefaultCallback<Map>(){
			public void onSuccess(Map data) {
				getView().refreshChart(model, data, panel, width, height);
			}
						
		});
	}
	
	public void refreshTableChart(final AQChartModel model, final VLayout panel, final int width, final int height) {
		service.getConversationDetails(new DefaultCallback<List<Conversation>>(){
			public void onSuccess(List<Conversation> data) {
				if (data != null) {
					getView().refreshConversationChart(model, data, panel, width, height);
				}
			}			
		});
	}
	
	public void refreshTableChart(final AQChartModel model) {
		service.getConversationDetails(new DefaultCallback<List<Conversation>>(){

			public void onSuccess(List<Conversation> data) {
				if (data != null) {
					getView().refreshConversationChart(model, data);
				}
			}
			
		});
	}
	
	public void refreshActiveQueries() {
		service.getSystemAQNames(new DefaultCallback<List<String>>(){
			public void onSuccess(List<String> data) {
				if (data != null) {
					getView().setActiveQueries(data);
				}
			}			
		});
	}
	
}
