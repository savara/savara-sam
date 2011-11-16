/**
 * 
 */
package org.savara.sam.web.client.view;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.savara.sam.web.client.presenter.MainLayoutPresenter;
import org.savara.sam.web.client.presenter.MainLayoutPresenter.MainLayoutView;
import org.savara.sam.web.client.util.ColorUtil;
import org.savara.sam.web.client.view.ChartPortalLayout.ChartPortlet;
import org.savara.sam.web.shared.dto.Conversation;
import org.savara.sam.web.shared.dto.ResponseTime;
import org.savara.sam.web.shared.dto.Statistic;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.visualization.client.AbstractDataTable.ColumnType;
import com.google.gwt.visualization.client.ChartArea;
import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.LegendPosition;
import com.google.gwt.visualization.client.VisualizationUtils;
import com.google.gwt.visualization.client.visualizations.Table;
import com.google.gwt.visualization.client.visualizations.corechart.ColumnChart;
import com.google.gwt.visualization.client.visualizations.corechart.LineChart;
import com.google.gwt.visualization.client.visualizations.corechart.Options;
import com.google.gwt.visualization.client.visualizations.corechart.PieChart;
import com.google.gwt.visualization.client.visualizations.corechart.PieChart.PieOptions;
import com.google.gwt.visualization.client.visualizations.corechart.ScatterChart;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.ViewImpl;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.HeaderControls;
import com.smartgwt.client.types.MultipleAppearance;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.HeaderControl;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.events.CloseClientEvent;
import com.smartgwt.client.widgets.events.DragRepositionStopEvent;
import com.smartgwt.client.widgets.events.DragRepositionStopHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.layout.VLayout;

/**
 * 
 * @author Jeff Yu
 * @date Nov 4, 2011
 */
public class MainLayoutViewImpl extends ViewImpl implements MainLayoutView{
	
	private VLayout panel;
				
	private MainLayoutPresenter presenter;
	
	private Timer timer;
	
	private ChartPortalLayout portal;
	
	private Statistic[] stats;
	
	private ResponseTime[] respTimes;
	
	private Conversation[] cdetails;
		
	@Inject
	public MainLayoutViewImpl() {
				
        Runnable onloadCallback = new Runnable() {
			public void run() {				
				initializeWindow();
				presenter.refreshTxnRatio(true);
				presenter.refreshTxnBarChart(true);
				presenter.refreshTxnResponseTime(true);
				presenter.refreshTxnResponseTimeWithOperations(true);
				presenter.refreshConversationChart(true);
				
				timer = new Timer(){
					public void run() {
						presenter.refreshTxnRatio(true);
						presenter.refreshTxnBarChart(true);
						presenter.refreshTxnResponseTime(true);
					}};
				
				//TODO: looks like the refreshing action sometimes block the whole page.
			    //timer.scheduleRepeating(30 * 1000);
			}        	
        };
                
        VisualizationUtils.loadVisualizationApi(onloadCallback, PieChart.PACKAGE, Table.PACKAGE);
	}

	private void initializeWindow() {
		panel = new VLayout();
		panel.setWidth("1180");
		panel.setPadding(0);
		panel.setBorder("1px solid black");
		panel.setAlign(Alignment.CENTER);
		panel.setAlign(VerticalAlignment.TOP);
		
		addHeaderLayout();
		
		HLayout body = new HLayout();
		body.setWidth100();
		body.setPadding(3);
		body.setHeight(850);
		panel.addMember(body);
				
		addSectionStack(body);
		
		VLayout main = new VLayout(15);
		main.setMargin(10);
		body.addMember(main);
		
		portal = new ChartPortalLayout(3);
		portal.setMargin(0);
		portal.setWidth100();
		portal.setHeight100();
		portal.setCanAcceptDrop(true);		
			
		setPortalMenus(main);
		
        main.addMember(portal);
        
        final String txnRatioTitle = "Txn Ratio";
        ChartPortlet txnRatio = portal.createPortlet(txnRatioTitle, new ClickHandler() {
			public void onClick(ClickEvent event) {
				presenter.refreshTxnRatio(true);
			}        	
        }, new ClickHandler() {
			public void onClick(ClickEvent event) {
				showWindowModalWithChart(txnRatioTitle, createTxnRatioChart(stats, false));
			}        	
        }, new DragRepositionStopHandler() {

			public void onDragRepositionStop(DragRepositionStopEvent event) {
				presenter.refreshTxnRatio(true);
			}
        	
        }); 
        portal.addPortlet(txnRatio);
        
        final String txnsBarTitle = "Txn Bar Chart";
        ChartPortlet txnsBar = portal.createPortlet(txnsBarTitle, new ClickHandler(){
			public void onClick(ClickEvent event) {
				presenter.refreshTxnBarChart(true);
			}
          }, new ClickHandler() {
				public void onClick(ClickEvent event) {
					showWindowModalWithChart(txnsBarTitle, createTxnBarChart(stats, false));
				}         	
        }, new DragRepositionStopHandler() {
			public void onDragRepositionStop(DragRepositionStopEvent event) {
				presenter.refreshTxnBarChart(true);
			}        	
        });
        portal.addPortlet(txnsBar);
        
        ChartPortlet responseTime = portal.createPortlet("Response Time", new ClickHandler() {
			public void onClick(ClickEvent event) {
				presenter.refreshTxnResponseTime(true);
			}
        }, new ClickHandler() {
			public void onClick(ClickEvent event) {
				showWindowModalWithChart("Response Time", createResponseTimeLineChart(respTimes, false));
			} 
        }, new DragRepositionStopHandler(){

			public void onDragRepositionStop(DragRepositionStopEvent event) {
				presenter.refreshTxnResponseTime(true);
			}
        	
        });
        portal.addPortlet(responseTime);
        
        ChartPortlet conversationPortlet = portal.createPortlet("Conversation Chart", new ClickHandler(){
			public void onClick(ClickEvent event) {
				presenter.refreshConversationChart(true);
			}
        	
        }, new ClickHandler() {
			public void onClick(ClickEvent event) {
				Label validConversation = new Label("<b>Valid Conversations</b>");
				validConversation.setSize("600", "10");
				showWindowModalWithChart("Conversation Chart", validConversation, createConversationTableChart(cdetails, false, true),
						createConversationTableChart(cdetails, false, false));
			}        	
        }, new DragRepositionStopHandler(){
			public void onDragRepositionStop(DragRepositionStopEvent event) {
				presenter.refreshConversationChart(true);
			}
        	
        });

        portal.addPortlet(conversationPortlet);
        

        ChartPortlet rtOperationsPortlet = portal.createPortlet("Buy Operation Response Time Chart", new ClickHandler(){
			public void onClick(ClickEvent event) {
				presenter.refreshTxnResponseTimeWithOperations(true);
			}        	
        }, new ClickHandler() {
			public void onClick(ClickEvent event) {
				showWindowModalWithChart("Buy Operation Response Time Chart", createRTChartWithOperations(respTimes, false));
			}        	
        }, new DragRepositionStopHandler(){
			public void onDragRepositionStop(DragRepositionStopEvent event) {
				presenter.refreshTxnResponseTimeWithOperations(true);
			}        	
        });
        
        portal.addPortlet(rtOperationsPortlet);
        
		addFooterLayout();
		panel.draw();
	}
	
	public void refreshTxnRatioChart(Statistic[] value, boolean isSmall) {
		PieChart pc = createTxnRatioChart(value, isSmall);
		VLayout txnRatioPanel = portal.getChartPortlet("Txn Ratio").getChartPanel();
		txnRatioPanel.clear();
		txnRatioPanel.addChild(pc);
		txnRatioPanel.draw();
	}
	
	public void refreshTxnBarChart(Statistic[] value, boolean isSmall) {
		ColumnChart cc = createTxnBarChart(value, isSmall);
		VLayout txnsBarPanel = portal.getChartPortlet("Txn Bar Chart").getChartPanel();
		txnsBarPanel.clear();
		txnsBarPanel.addChild(cc);
		txnsBarPanel.draw();
	}
	
	public void refreshResponseTime(ResponseTime[] value, boolean isSmall) {
		LineChart lc = createResponseTimeLineChart(value, isSmall);
		VLayout responseTimePanel = portal.getChartPortlet("Response Time").getChartPanel();
		responseTimePanel.clear();
		responseTimePanel.addChild(lc);
		responseTimePanel.draw();
	}
	
	public void refreshRTWithOperation(ResponseTime[] value, boolean isSmall) {
		LineChart lc = createRTChartWithOperations(value, isSmall);
		VLayout rtOperationsPanel = portal.getChartPortlet("Buy Operation Response Time Chart").getChartPanel();
		rtOperationsPanel.clear();
		rtOperationsPanel.addChild(lc);
		rtOperationsPanel.draw();
	}
	
	public void refreshConversationChart(Conversation[] value, boolean isSmall) {
		Table validTable = createConversationTableChart(value, isSmall, true);
		Table invalidTable = createConversationTableChart(value, isSmall, false);
		
		VLayout conversationPanel = portal.getChartPortlet("Conversation Chart").getChartPanel();
		
		Canvas[] canvas = conversationPanel.getMembers();
		for (int i = 0; i< canvas.length; i++) {
			conversationPanel.removeMember(canvas[i]);
		}
		conversationPanel.clear();
		conversationPanel.addMember(validTable);
		conversationPanel.addMember(invalidTable);
		conversationPanel.draw();
	}
	
	private void showWindowModalWithChart(String title, Widget... charts) {
		final Window window = new Window();
		window.setWidth(850);
		window.setHeight(650);
		
		//TODO: need to add the refresh click handler
		HeaderControl refreshBtn = new HeaderControl(HeaderControl.REFRESH);
		window.setHeaderControls(HeaderControls.HEADER_LABEL, refreshBtn, HeaderControls.CLOSE_BUTTON);
		
		window.setTitle(title);
		window.setIsModal(true);
		window.setShowModalMask(true);
		window.centerInPage();
		
		window.addCloseClickHandler(new CloseClickHandler(){

			public void onCloseClick(CloseClientEvent event) {
				window.destroy();
			}
			
		});
		VLayout chartLayout = new VLayout();
		chartLayout.setMargin(25);
		for (Widget chart : charts) {
			chartLayout.addMember(chart);
		}
		window.addChild(chartLayout);
		window.show();
	}
	
	private PieChart createTxnRatioChart(Statistic[] values, boolean isSmall) {
		DataTable dt = DataTable.create();
		dt.addColumn(ColumnType.STRING, "transaction type");
		dt.addColumn(ColumnType.NUMBER, "percentage");
		dt.addRows(values == null ? 0 : values.length);
		if (values != null) {
			for (int i = 0; i < values.length; i++) {
				Statistic statistic = values[i];
				String name = statistic.getName();
				if ("Successful".equalsIgnoreCase(name) || "Unsuccessful".equalsIgnoreCase(name)) {
					dt.setValue(i, 0, name);
					dt.setValue(i, 1, statistic.getValue());
				}
			}		
		}
		
		Options options = isSmall ? smallOptions() : bigOptions();
		PieOptions po = (PieOptions)options;
		po.setTitle("Transactions Ratio Pie Chart");
		po.set3D(true);
		
		PieChart pc = new PieChart(dt, po);
		
		return pc;
	}
	
	private ColumnChart createTxnBarChart(Statistic[] values, boolean isSmall) {
		DataTable dt = DataTable.create();
		dt.addColumn(ColumnType.STRING, "transaction type");
		dt.addColumn(ColumnType.NUMBER, "Txns");
		dt.addRows(values == null ? 0 : values.length);
		if (values != null) {
			for (int i = 0; i < values.length; i++) {
				Statistic statistic = values[i];
				String name = statistic.getName();
				if ("Successful".equalsIgnoreCase(name) || "Unsuccessful".equalsIgnoreCase(name) || "Started".equalsIgnoreCase(name)) {
					dt.setValue(i, 0, name);
					dt.setValue(i, 1, statistic.getValue());
				}
			}
		}
		
		ColumnChart cc = new ColumnChart(dt, isSmall ? smallOptions() : bigOptions());
		return cc;
	}
	
	private Options bigOptions() {
		Options options = Options.create();
		options.setWidth(800);
		options.setHeight(600);
		ChartArea area = ChartArea.create();
		area.setWidth(700);
		area.setHeight(500);
		options.setChartArea(area);
		options.setLegend(LegendPosition.BOTTOM);
		return options;
	}
	
	private Options smallOptions() {
		Options options = Options.create();
		options.setWidth(250);
		options.setHeight(200);
		options.setLegend(LegendPosition.BOTTOM);
		return options;
	}
	
	private LineChart createResponseTimeLineChart(ResponseTime[] values, boolean isSmall) {
		DataTable dt = DataTable.create();
		dt.addColumn(ColumnType.DATETIME, "Request Time");
		dt.addColumn(ColumnType.NUMBER, "Response Time");
		dt.addRows(values == null ? 0 : values.length);
		if (values != null) {
			for (int i = 0; i < values.length; i++) {
				ResponseTime rt = values[i];
				Date d = new Date();
				d.setTime(rt.getRequestTime().longValue());
				dt.setValue(i, 0, d);
				dt.setValue(i, 1, rt.getResponseTime());
			}
		}
		
		Options options = isSmall ? smallOptions() : bigOptions();
		options.set("backgroundColor", ColorUtil.getResponseTimeBGColor(respTimes, 9000));
		
		return new LineChart(dt, options);
	}
	
	private Table createConversationTableChart(Conversation[] values, boolean isSmall, boolean isValid) {
		DataTable dt = DataTable.create();
		dt.addColumn(ColumnType.STRING, "Id");
		dt.addColumn(ColumnType.BOOLEAN, "Status");
		
		List<Conversation> data = new ArrayList<Conversation>();
		
		for (int i = 0; i < values.length; i++) {
			Conversation cd = values[i];
			if (isValid == cd.getStatus()) {
				data.add(cd);
			}
		}
		
		dt.addRows(data.size());
		int i = 0;
		for (Conversation cd : data) {
			dt.setValue(i, 0, cd.getConversationId());
			dt.setValue(i, 1, cd.getStatus());
			i++;
		}
		
		Table.Options toption = Table.Options.create();
		toption.setShowRowNumber(true);
		if (isSmall) {
			toption.setWidth("230");
			toption.setHeight("100");
			toption.setPageSize(3);
		} else {
			toption.setWidth("800");
			toption.setHeight("300");
			toption.setPageSize(20);
		}
		
		
		if (isValid) {
			toption.set("cssClassNames", "{tableRow:'validConversationTable'}");
		} else {
			toption.set("cssClassNames", "{tableRow:'invalidConversationTable'}");
		}
		
		
		return new Table(dt, toption);
	}
	
	private LineChart createRTChartWithOperations(ResponseTime[] values, boolean isSmall) {
		DataTable dt = DataTable.create();
		dt.addColumn(ColumnType.DATETIME, "Request Time");
		dt.addColumn(ColumnType.NUMBER, "Response Time");
		
		int size = 0;		
		for (int i = 0; i < values.length; i++) {
			ResponseTime rt = values[i];
			if ("buy".equalsIgnoreCase(rt.getOperation())) {
				size ++;
			}
		}
		
		dt.addRows(size);
		
		int position = 0;
		if (values != null) {
			for (int i = 0; i < values.length; i++) {
				ResponseTime rt = values[i];
				if ("buy".equalsIgnoreCase(rt.getOperation())){
					Date d = new Date();
					d.setTime(rt.getRequestTime().longValue());
					dt.setValue(position, 0, d);
					dt.setValue(position, 1, rt.getResponseTime());
					position ++;
				} 
			}
		
		}	
		
		Options options = isSmall ? smallOptions() : bigOptions();
		options.set("backgroundColor", ColorUtil.getResponseTimeBGColor(respTimes, 9000));
		
		return new LineChart(dt, options);
	}
	
	private ScatterChart createResponseTimeScatterChart(ResponseTime[] values, boolean isSmall) {
		DataTable dt = DataTable.create();
		dt.addColumn(ColumnType.STRING, "Operation");
		dt.addColumn(ColumnType.NUMBER, "Response Time");
		dt.addRows(values == null ? 0 : values.length);
		if (values != null) {
			for (int i = 0; i < values.length; i++) {
				ResponseTime rt = values[i];
				dt.setValue(i, 0, rt.getOperation());
				dt.setValue(i, 1, rt.getResponseTime());
			}
		}
		return new ScatterChart(dt, isSmall ? smallOptions() : bigOptions());
	}


	private void setPortalMenus(VLayout main) {
		final DynamicForm form = new DynamicForm();  
        form.setAutoWidth();  
        form.setNumCols(1);  
		
        ButtonItem addColumn = new ButtonItem("addAQ", "Add AQ Chart");  
        addColumn.setAutoFit(true);  
        addColumn.setStartRow(false);  
        addColumn.setEndRow(false);
        
        addColumn.addClickHandler(new com.smartgwt.client.widgets.form.fields.events.ClickHandler(){

			public void onClick(
					com.smartgwt.client.widgets.form.fields.events.ClickEvent event) {
				final Window window = new Window();
				window.setWidth(350);
				window.setHeight(300);
				window.setTitle("Add an AQ chart");
				window.setShowMinimizeButton(false);
				window.setIsModal(true);
				window.setShowModalMask(true);
				window.centerInPage();
				
				window.addCloseClickHandler(new CloseClickHandler(){
					public void onCloseClick(CloseClientEvent event) {
						window.destroy();
					}					
				});
				
				final DynamicForm form = new DynamicForm();
				form.setMargin(10);
				form.setPadding(5);
				form.setWidth100();
				form.setHeight100();
				form.setLayoutAlign(VerticalAlignment.BOTTOM);
				
				final TextItem title = new TextItem();
				title.setTitle("Chart title");
				
				final SelectItem aqSelect = new SelectItem();
				aqSelect.setTitle("AQ Select");
				aqSelect.setMultiple(true);
				aqSelect.setMultipleAppearance(MultipleAppearance.PICKLIST);
				aqSelect.setValueMap("Successful AQ", "Unsuccessful AQ", "Started Txn AQ", "Response Time AQ");
				
				final SelectItem chartType = new SelectItem();
				chartType.setTitle("Chart Type");
				chartType.setValueMap("Pie Chart", "Line Chart", "Table Chart");
				
				final SelectItem yAxis = new SelectItem();
				yAxis.setTitle("Y Axis mapped property");
				yAxis.setValueMap("Response Time");
				
				final ButtonItem submitBtn = new ButtonItem();
				submitBtn.setTitle("Create");
				submitBtn.setEndRow(false);
				submitBtn.setAlign(Alignment.CENTER);
				
				submitBtn.addClickHandler(new com.smartgwt.client.widgets.form.fields.events.ClickHandler(){

					public void onClick(
							com.smartgwt.client.widgets.form.fields.events.ClickEvent event) {
							final String theTitle = title.getValueAsString();
							
							window.destroy();
							
							ChartPortlet thePortlet = portal.createPortlet(theTitle, new ClickHandler() {
								public void onClick(ClickEvent event) {
									presenter.refreshTxnRatio(true);
								}        	
					        }, new ClickHandler() {
								public void onClick(ClickEvent event) {
									showWindowModalWithChart(theTitle, createTxnRatioChart(stats, false));
								}        	
					        }, new DragRepositionStopHandler() {

								public void onDragRepositionStop(DragRepositionStopEvent event) {
									presenter.refreshTxnRatio(true);
								}
					        	
					        });
							
							portal.addPortlet(thePortlet);
							
					}
					
				});
				
				final ButtonItem cancelBtn = new ButtonItem();
				cancelBtn.setTitle("Cancel");
				cancelBtn.setStartRow(false);
				cancelBtn.addClickHandler(new com.smartgwt.client.widgets.form.fields.events.ClickHandler(){

					public void onClick(
							com.smartgwt.client.widgets.form.fields.events.ClickEvent event) {
						window.destroy();						
					}
					
				});
				
				form.setFields(title, aqSelect, chartType, yAxis, submitBtn, cancelBtn);
				
				window.addItem(form);
				
				window.show();
			}

        	
        });
        
        form.setItems(addColumn);
        
        HLayout menus = new HLayout();
        menus.setWidth100();
        menus.setBackgroundColor("#B3BEE3");
        
        menus.addMember(form);
        
        main.addMember(menus);
	}


	private void addSectionStack(HLayout body) {
		final SectionStack  linkStack = new SectionStack();
		linkStack.setVisibilityMode(VisibilityMode.MULTIPLE);
		linkStack.setCanResizeSections(false);
		linkStack.setWidth(180);
		linkStack.setHeight(200);
		
		SectionStackSection dashboard = new SectionStackSection("Dashboard");
		dashboard.setCanCollapse(true);
		dashboard.setExpanded(true);
		HTMLFlow flow = new HTMLFlow();
		flow.setContents("Active Query");
		flow.setPadding(5);
		dashboard.addItem(flow);
		
		linkStack.addSection(dashboard);
		
		body.addMember(linkStack);
	}


	private void addHeaderLayout() {
		Label headerLabel = new Label();
		headerLabel.setMargin(0);
		headerLabel.setSize("100%", "100");
		headerLabel.setAlign(Alignment.LEFT);
		headerLabel.setStyleName("headerLabel");
		panel.addMember(headerLabel);
	}


	private void addFooterLayout() {
		Label footerLabel = new Label();
		footerLabel.setContents("Savara SAM :: Footer ");
		footerLabel.setSize("100%", "30");
		footerLabel.setAlign(Alignment.CENTER);
		footerLabel.setBorder("1px solid #808080");
		
		panel.addMember(footerLabel);
	}
	
	
	public Widget asWidget() {
		return panel;
	}

	public void setPresenter(MainLayoutPresenter presenter) {
		this.presenter = presenter;
	}

	public void setStats(Statistic[] stats) {
		this.stats = stats;
	}

	public void setRespTimes(ResponseTime[] respTimes) {
		this.respTimes = respTimes;
	}

	public void refreshRespTimeScatterChart(ResponseTime[] value,
			boolean isSmall) {
		//TODO: to be experimented.
		
	}

	public void setConversationDetails(Conversation[] cdetails) {
		this.cdetails = cdetails;
	}

}
