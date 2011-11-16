/**
 * 
 */
package org.savara.sam.web.client.presenter;

import java.util.HashMap;
import java.util.Map;

import org.savara.sam.web.client.BootstrapContext;
import org.savara.sam.web.client.NameTokens;
import org.savara.sam.web.client.util.DefaultCallback;
import org.savara.sam.web.shared.AQMonitorService;
import org.savara.sam.web.shared.AQMonitorServiceAsync;
import org.savara.sam.web.shared.dto.AQChartModel;
import org.savara.sam.web.shared.dto.Conversation;
import org.savara.sam.web.shared.dto.ResponseTime;
import org.savara.sam.web.shared.dto.Statistic;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.NoGatekeeper;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.ProxyPlace;
import com.gwtplatform.mvp.client.proxy.RevealRootLayoutContentEvent;

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
		
		public void refreshTxnRatioChart(Statistic[] value, boolean isSmall);
		
		public void refreshTxnBarChart(Statistic[] value, boolean isSmall);
		
		public void refreshResponseTime(ResponseTime[] value, boolean isSmall);
		
		public void refreshRTWithOperation(ResponseTime[] value, boolean isSmall);
		
		public void refreshRespTimeScatterChart(ResponseTime[] value, boolean isSmall);
		
		public void refreshConversationChart(Conversation[] value, boolean isSmall);
		
		public void setStats(Statistic[] stats);
		
		public void setRespTimes(ResponseTime[] respTimes);
		
		public void setConversationDetails(Conversation[] details);
		
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
	
	
	public void refreshTxnRatio(final boolean isSmall) {
		service.getStatistics(new AsyncCallback<Statistic[]>() {
			public void onFailure(Throwable t) {
				System.out.println(t);
			}

			public void onSuccess(Statistic[] value) {
				getView().setStats(value);
				getView().refreshTxnRatioChart(value, isSmall);
			}			
		});
	}
	
	public void refreshTxnBarChart(final boolean isSmall) {
		service.getStatistics(new AsyncCallback<Statistic[]>() {
			public void onFailure(Throwable t) {
				System.out.println(t);
			}

			public void onSuccess(Statistic[] value) {
				getView().setStats(value);
				getView().refreshTxnBarChart(value, isSmall);
			}			
		});
	}
	
	public void refreshTxnResponseTime(final boolean isSmall) {
		service.getResponseTimes(new AsyncCallback<ResponseTime[]>() {

			public void onFailure(Throwable t) {
				System.out.println(t);
			}

			public void onSuccess(ResponseTime[] value) {
				getView().setRespTimes(value);
				getView().refreshResponseTime(value, isSmall);
			}
			
		});	
	}
	
	public void refreshTxnResponseTimeWithOperations(final boolean isSmall) {
		service.getResponseTimes(new AsyncCallback<ResponseTime[]>() {

			public void onFailure(Throwable t) {
				System.out.println(t);
			}

			public void onSuccess(ResponseTime[] value) {
				getView().setRespTimes(value);
				getView().refreshRTWithOperation(value, isSmall);
			}
			
		});	
	}
	
	public void refreshTxnResponseTimeScatterChart(final boolean isSmall) {
		service.getResponseTimes(new AsyncCallback<ResponseTime[]>() {

			public void onFailure(Throwable t) {
				System.out.println(t);
			}

			public void onSuccess(ResponseTime[] value) {
				getView().setRespTimes(value);
				getView().refreshRespTimeScatterChart(value, isSmall);
			}
			
		});	
	}
	
	public void refreshConversationChart(final boolean isSmall) {
		service.getConversationDetails(new DefaultCallback<Conversation[]>() {

			public void onSuccess(Conversation[] value) {
				getView().setConversationDetails(value);
				getView().refreshConversationChart(value, isSmall);
			}
			
		});	
	}
	
	public Map<?, ?> refreshChartData(AQChartModel model) {
		return new HashMap();
	}
	
}
