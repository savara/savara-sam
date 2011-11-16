/**
 * 
 */
package org.savara.sam.web.client.util;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * 
 * 
 * @author Jeff Yu
 * @date Nov 16, 2011
 */
public abstract class DefaultCallback<T> implements AsyncCallback<T> {

	public void onFailure(Throwable throwable) {
		//TODO: global page for showing the error message.
		System.out.println("===ERROR === " + throwable);
		throwable.printStackTrace();
	}


}
