package org.savara.sam.web.client;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.gwtplatform.mvp.client.DelayedBindRegistry;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class ApplicationEntryPoint implements EntryPoint {

    public static final ApplicationUI MODULE = GWT.create(ApplicationUI.class);

    public void onModuleLoad() {

        Log.setUncaughtExceptionHandler();

        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand(){

            public void execute() {
                actualModuleLoad();
            }
        });
    }


    public void actualModuleLoad() {
        DelayedBindRegistry.bind(MODULE);
        MODULE.getPlaceManager().revealCurrentPlace();
    }
}
