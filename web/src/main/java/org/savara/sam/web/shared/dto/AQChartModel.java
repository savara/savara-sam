/**
 * 
 */
package org.savara.sam.web.shared.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 
 * 
 * @author Jeff Yu
 * @date Nov 16, 2011
 */
public class AQChartModel implements Serializable{
	
	private static final long serialVersionUID = -5472614165685429537L;

	public enum ChartType {
		PIE_CHART, COLUMN_CHART, LINE_CHART
	}
	
	private String name;
	
	private List<String> activeQueryNames;
	
	private ChartType chartType;
	
	private String verticalProperty;
	
	private String horizontalProperty;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getActiveQueryNames() {
		return activeQueryNames;
	}

	public void setActiveQueryNames(List<String> activeQueryNames) {
		this.activeQueryNames = activeQueryNames;
	}

	public ChartType getChartType() {
		return chartType;
	}

	public void setChartType(ChartType chartType) {
		this.chartType = chartType;
	}

	public String getVerticalProperty() {
		return verticalProperty;
	}

	public void setVerticalProperty(String verticalProperty) {
		this.verticalProperty = verticalProperty;
	}

	public String getHorizontalProperty() {
		return horizontalProperty;
	}

	public void setHorizontalProperty(String horizontalProperty) {
		this.horizontalProperty = horizontalProperty;
	}
	
}
