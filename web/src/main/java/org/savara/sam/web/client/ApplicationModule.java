/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008-11, Red Hat Middleware LLC, and others contributors as indicated
 * by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.savara.sam.web.client;

import org.savara.sam.web.client.auth.CurrentUser;
import org.savara.sam.web.client.auth.LoggedInGateKeeper;
import org.savara.sam.web.client.presenter.LoginPresenter;
import org.savara.sam.web.client.presenter.MainLayoutPresenter;
import org.savara.sam.web.client.presenter.SituationLayoutPresenter;
import org.savara.sam.web.client.view.LoginPageView;
import org.savara.sam.web.client.view.MainLayoutViewImpl;
import org.savara.sam.web.client.view.SituationLayoutViewImpl;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.inject.Singleton;
import com.gwtplatform.mvp.client.RootPresenter;
import com.gwtplatform.mvp.client.gin.AbstractPresenterModule;
import com.gwtplatform.mvp.client.proxy.Gatekeeper;
import com.gwtplatform.mvp.client.proxy.ParameterTokenFormatter;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.TokenFormatter;

/**
 * @author: Jeff Yu
 * @date: 9/05/11
 */
public class ApplicationModule extends AbstractPresenterModule {

    @Override
    protected void configure() {
        bind(EventBus.class).to(SimpleEventBus.class).in(Singleton.class);
        bind(PlaceManager.class).to(ApplicationPlaceManager.class).in(Singleton.class);
        bind(TokenFormatter.class).to(ParameterTokenFormatter.class).in(Singleton.class);
        bind(RootPresenter.class).asEagerSingleton();

        bind(Gatekeeper.class).to(LoggedInGateKeeper.class);
        bind(CurrentUser.class).in(Singleton.class);
        bind(BootstrapContext.class).in(Singleton.class);
        bind(ApplicationProperties.class).to(BootstrapContext.class).in(Singleton.class);

        //Presenters
        bindPresenter(LoginPresenter.class, LoginPresenter.LoginView.class, LoginPageView.class,
                LoginPresenter.LoginProxy.class);
        bindPresenter(MainLayoutPresenter.class, MainLayoutPresenter.MainLayoutView.class, MainLayoutViewImpl.class,
        		MainLayoutPresenter.MainLayoutProxy.class);
        bindPresenter(SituationLayoutPresenter.class, SituationLayoutPresenter.SituationLayoutView.class, SituationLayoutViewImpl.class,
        		SituationLayoutPresenter.SituationLayoutProxy.class);
    }
}
