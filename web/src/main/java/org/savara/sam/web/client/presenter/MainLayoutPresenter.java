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
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.NoGatekeeper;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.ProxyPlace;
import com.gwtplatform.mvp.client.proxy.RevealRootLayoutContentEvent;
import com.smartgwt.client.widgets.layout.VLayout;

/**
 * @author Jeff Yu
 *
 */
public class MainLayoutPresenter extends Presenter<MainLayoutPresenter.MainLayoutView, 
												   MainLayoutPresenter.MainLayoutProxy> {
	
	private BootstrapContext bootstrap;
	
	private AQMonitorServiceAsync service;
		
	public interface MainLayoutView extends View {
				
		public void setPresenter(MainLayoutPresenter presenter);
				
		public void refreshConversationChart(Conversation[] value, boolean isSmall);
				
		public void setConversationDetails(Conversation[] details);
				
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
            MainLayoutProxy proxy, BootstrapContext bootstrap) {
        super(eventBus, view, proxy);
        this.bootstrap = bootstrap;
        service = (AQMonitorServiceAsync)GWT.create(AQMonitorService.class);
    }
	
    
    
	@Override
	protected void revealInParent() {
		RevealRootLayoutContentEvent.fire(this, this);
	}
	
	public void onBind() {
		super.onBind();
		getView().setPresenter(this);
	}
	
	public void refreshConversationChart(final boolean isSmall) {
		service.getConversationDetails(new DefaultCallback<Conversation[]>() {

			public void onSuccess(Conversation[] value) {
				getView().setConversationDetails(value);
				getView().refreshConversationChart(value, isSmall);
			}
			
		});	
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
