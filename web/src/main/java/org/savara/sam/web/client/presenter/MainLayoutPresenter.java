/**
 * 
 */
package org.savara.sam.web.client.presenter;

import org.savara.sam.web.client.BootstrapContext;
import org.savara.sam.web.client.NameTokens;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
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
	
	public interface MainLayoutView extends View {
		
	}
	
	@ProxyCodeSplit
	@NameToken(NameTokens.MAIN_VIEW)
	public interface MainLayoutProxy extends ProxyPlace<MainLayoutPresenter>{}
	
    @Inject
    public MainLayoutPresenter(
            EventBus eventBus,
            MainLayoutView view,
            MainLayoutProxy proxy, BootstrapContext bootstrap) {
        super(eventBus, view, proxy);
        this.bootstrap = bootstrap;
    }
	
    
    
	@Override
	protected void revealInParent() {
		RevealRootLayoutContentEvent.fire(this, this);
	}
	
}
