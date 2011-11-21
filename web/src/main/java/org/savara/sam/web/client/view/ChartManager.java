/**
 * 
 */
package org.savara.sam.web.client.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.savara.sam.web.shared.dto.Conversation;

import com.google.gwt.visualization.client.AbstractDataTable.ColumnType;
import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.LegendPosition;
import com.google.gwt.visualization.client.visualizations.corechart.ColumnChart;
import com.google.gwt.visualization.client.visualizations.corechart.LineChart;
import com.google.gwt.visualization.client.visualizations.corechart.Options;
import com.google.gwt.visualization.client.visualizations.corechart.PieChart;
import com.google.gwt.visualization.client.visualizations.corechart.PieChart.PieOptions;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.VLayout;

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
		po.setColors("Red", "Green", "Blue", "Coral", "Purple", "Olive", "Pink", "Salmon", "Yellow", "Tan", "Violet", "Snow", "Silver", "SlateGray", "Navy");
		
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
	
	/**
	 * Create the conversation chart.
	 * 
	 * @param data
	 * @param width
	 * @param height
	 * @return
	 */
	public static VLayout createConversationChart(List<Conversation> data, int width, int height) {
		VLayout tables = new VLayout();
		tables.setWidth(width - 10);
		ListGridField id = new ListGridField("id", "ID", width * 2 /3);
		ListGridField status = new ListGridField("status", "Status");
		ListGridField date = new ListGridField("date", "Date");
		
		boolean showDate = (width > ChartManager.SMALL_WIDTH) ? true : false;
		
		ListGrid validGrids = new ListGrid();
		validGrids.setWidth100();
		validGrids.setBaseStyle("validConversation");
		validGrids.setHeight(height/2 - 5);
		if (showDate) {
			validGrids.setFields(id, status, date);
		} else {
			validGrids.setFields(id, status);
		}
		
		validGrids.setData(getListGridData(data, true, showDate));
		
		ListGrid invalidGrids = new ListGrid();
		invalidGrids.setWidth100();
		invalidGrids.setBaseStyle("invalidConversation");
		invalidGrids.setHeight(height/2 - 5);
		if (showDate) {
			invalidGrids.setFields(id, status, date);
		} else {
			invalidGrids.setFields(id, status);
		}
		
		invalidGrids.setData(getListGridData(data, false, showDate));
		tables.addMember(validGrids);
		Label splitor = new Label();
		splitor.setHeight(1);
		tables.addMember(splitor);
		tables.addMember(invalidGrids);
		return tables;
	}
	
	private static ListGridRecord[] getListGridData(List<Conversation> data, boolean isValid, boolean showDate) {
		List<ListGridRecord> result = new ArrayList<ListGridRecord>();
		for (Conversation conversation : data) {
			if (conversation.getStatus().booleanValue() == isValid) {
				ListGridRecord record = new ListGridRecord();
				record.setAttribute("id", conversation.getConversationId());
				if (conversation.getStatus().booleanValue()) {
					record.setAttribute("status", "Valid");
				} else {
					record.setAttribute("status", "Invalid");
				}
				if (showDate) {
					Date d = new Date();
					d.setTime(conversation.getUpdatedDate());
					record.setAttribute("date", d);
				}
				result.add(record);
			}
		}
		return result.toArray(new ListGridRecord[0]);
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
