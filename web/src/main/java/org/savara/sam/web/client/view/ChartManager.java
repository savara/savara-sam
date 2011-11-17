/**
 * 
 */
package org.savara.sam.web.client.view;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

import com.google.gwt.visualization.client.AbstractDataTable.ColumnType;
import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.LegendPosition;
import com.google.gwt.visualization.client.visualizations.corechart.ColumnChart;
import com.google.gwt.visualization.client.visualizations.corechart.LineChart;
import com.google.gwt.visualization.client.visualizations.corechart.Options;
import com.google.gwt.visualization.client.visualizations.corechart.PieChart;
import com.google.gwt.visualization.client.visualizations.corechart.PieChart.PieOptions;

/**
 * 
 * 
 * @author Jeff Yu
 * @date Nov 16, 2011
 */
public class ChartManager {
	
	public static final int SMALL_WIDTH = 250;
	public static final int SMALL_HEIGHT = 200;
	
	public static final int BIG_WIDTH = 800;
	public static final int BIG_HEIGHT = 600;
	
	public final static String[] WARN_COLORS = new String[]{"#FFFFFF","#FAD4DE", "#F2A5B8", "#C7798D", "#B36075","#F70A45"};
	
	/**
	 * Create a Pie Chart (3D).
	 * 
	 * @param data
	 * @param title
	 * @param width
	 * @param height
	 * @return
	 */
	public static PieChart createPieChart(Map<String, Integer> data, String title, int width, int height) {
		DataTable dt = DataTable.create();
		dt.addColumn(ColumnType.STRING, "name");
		dt.addColumn(ColumnType.NUMBER, "value");
		dt.addRows(data == null ? 0 : data.size());
		
		if (data != null) {
			int i = 0;
			for (String key : data.keySet()) {
				dt.setValue(i, 0, key);
				dt.setValue(i, 1, data.get(key));
				i++;
			}
		}
		
		Options options = createOptions(width, height, title);
		PieOptions po = (PieOptions)options;
		po.set3D(true);
		
		return new PieChart(dt, po);
	}
	
	/**
	 * Create a Column Chart.
	 * 
	 * @param data
	 * @param title
	 * @param width
	 * @param height
	 * @return
	 */
	public static ColumnChart createColumnChart(Map<String, Integer> data, String title, String legendName, int width, int height) {
		DataTable dt = DataTable.create();
		dt.addColumn(ColumnType.STRING, "name");
		dt.addColumn(ColumnType.NUMBER, legendName == null ? "" : legendName);
		dt.addRows(data == null ? 0 : data.size());
		
		if (data != null) {
			int i = 0;
			for (String key : data.keySet()) {
				dt.setValue(i, 0, key);
				dt.setValue(i, 1, data.get(key));
				i++;
			}
		}
		
		Options options = createOptions(width, height, title);		
		return new ColumnChart(dt, options);
	}
	
	/**
	 * Create a Line Chart.
	 * 
	 * @param data
	 * @param title
	 * @param width
	 * @param height
	 * @param threshold
	 * @return
	 */
	public static LineChart createLineChart(Map<Long, Long> data, String title, String legendName, int width, int height, long threshold) {
		DataTable dt = DataTable.create();
		dt.addColumn(ColumnType.DATETIME, "date");
		dt.addColumn(ColumnType.NUMBER, legendName == null ? "" : legendName);
		dt.addRows(data == null ? 0 : data.size());
		
		if (data != null) {
			int i = 0;
			for (Long key : data.keySet()) {
				Date date = new Date();
				date.setTime(key.longValue());
				dt.setValue(i, 0, date);
				dt.setValue(i, 1, data.get(key));
				i++;
			}
		}
		
		Options options = createOptions(width, height, title + " (SLA " + getResponseTimeSLARatio(data.values(), 9000) + "%)");
		
		if (threshold != 0) {
			options.set("backgroundColor", getLineChartBGColor(data.values(), 9000));
		}
		
		return new LineChart(dt, options);
	}
	
	private static String getLineChartBGColor(Collection<Long> values, long threshold) {

		int ratio = getResponseTimeSLARatio(values, threshold);

		int result = 0;
		
		if (ratio > 2) result = 1;
		if (ratio > 3) result = 2;
		if (ratio > 5) result = 3;
		if (ratio > 10) result = 4;
		if (ratio > 20) result = 5;
		
		return WARN_COLORS[result];
	}
	
	private static int getResponseTimeSLARatio(Collection<Long> values, long threshold) {
		int failedCount = 0;
		
		if (values != null) {
			for (Long value : values) {
				if (value.longValue() > threshold) {
					failedCount ++;
				}
			}
		}
		
		return failedCount * 100 /values.size();
		
	}
	
	private static Options createOptions(int width, int height, String title) {
		Options options = Options.create();
		options.setWidth(width);
		options.setHeight(height);
		options.setTitle(title);
		options.setLegend(LegendPosition.BOTTOM);
		
		return options;
	}
	
}
