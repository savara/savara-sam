/**
 * 
 */
package org.savara.sam.web.client.presenter;

import org.savara.sam.web.client.BootstrapContext;
import org.savara.sam.web.client.NameTokens;
import org.savara.sam.web.shared.AQMonitorService;
import org.savara.sam.web.shared.AQMonitorServiceAsync;
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
		
		public void setStatistics(Statistic[] value);
		
		public void setResponsetime(ResponseTime[] rts);
		
		public void setPresenter(MainLayoutPresenter presenter);
		
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
		setStatisticsData();
		setResponseTimes();

	}
	
	public void setStatisticsData() {
		service.getStatistics(new AsyncCallback<Statistic[]>() {
			public void onFailure(Throwable t) {
				System.out.println(t);
			}

			public void onSuccess(Statistic[] value) {
				getView().setStatistics(value);
			}
			
		});
	}
	
	public void setResponseTimes() {
		service.getResponseTimes(new AsyncCallback<ResponseTime[]>() {

			public void onFailure(Throwable t) {
				System.out.println(t);
			}

			public void onSuccess(ResponseTime[] value) {
				getView().setResponsetime(value);
			}
			
		});		
	}
	
}
