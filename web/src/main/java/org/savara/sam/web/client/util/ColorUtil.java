/**
 * 
 */
package org.savara.sam.web.client.util;

import org.savara.sam.web.shared.dto.ResponseTime;

/**
 * @author Jeff Yu
 * @date Nov 11, 2011
 */
public final class ColorUtil {
	
	public final static String[] WARN_COLORS = new String[]{"#FFFFFF","#FAD4DE", "#F2A5B8", "#C7798D", "#B36075","#F70A45"};
	
	public static String getResponseTimeBGColor(ResponseTime[] values, long threshold) {
		int failedCount = 0;
		
		for (int i = 0; i < values.length; i++) {
			ResponseTime rt = values[i];
			if (rt.getResponseTime() > threshold) {
				failedCount ++;
			}
		}
		
		int ratio = (failedCount * 100 /values.length);

		int result = 0;
		
		if (ratio > 60) result = 1;
		if (ratio > 70) result = 2;
		if (ratio > 75) result = 3;
		if (ratio > 85) result = 4;
		if (ratio > 90) result = 5;
		
		return WARN_COLORS[result];
	}

}
