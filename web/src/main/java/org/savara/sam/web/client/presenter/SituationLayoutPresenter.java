/**
 * 
 */
package org.savara.sam.web.client.presenter;

import org.savara.sam.web.client.NameTokens;
import org.savara.sam.web.shared.AQMonitorService;
import org.savara.sam.web.shared.AQMonitorServiceAsync;

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

/**
 * @author Jeff Yu
 * @date Nov 17, 2011
 */
public class SituationLayoutPresenter extends Presenter<SituationLayoutPresenter.SituationLayoutView, 
														SituationLayoutPresenter.SituationLayoutProxy> {
	private AQMonitorServiceAsync service;
	
	@Inject
	public SituationLayoutPresenter(EventBus eventBus,
			SituationLayoutView view, SituationLayoutProxy proxy) {
		super(eventBus, view, proxy);
		service = (AQMonitorServiceAsync)GWT.create(AQMonitorService.class);
	}


	public interface SituationLayoutView extends View {
		
		public void setPresenter(SituationLayoutPresenter presenter);
			
	}	

	@ProxyCodeSplit
	@NameToken(NameTokens.SITUATION_VIEW)
	@NoGatekeeper
	public interface SituationLayoutProxy extends ProxyPlace<SituationLayoutPresenter>{}


	@Override
	protected void revealInParent() {
		RevealRootLayoutContentEvent.fire(this, this);
	}
	
	@Override
	public void onBind() {
		super.onBind();
		getView().setPresenter(this);
	}
	
}
