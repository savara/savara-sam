/**
 * 
 */
package org.savara.sam.web.client.view;

import java.util.Date;

import org.savara.sam.web.client.presenter.MainLayoutPresenter;
import org.savara.sam.web.client.presenter.MainLayoutPresenter.MainLayoutView;
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
import com.google.gwt.visualization.client.visualizations.corechart.ColumnChart;
import com.google.gwt.visualization.client.visualizations.corechart.CoreChart;
import com.google.gwt.visualization.client.visualizations.corechart.LineChart;
import com.google.gwt.visualization.client.visualizations.corechart.Options;
import com.google.gwt.visualization.client.visualizations.corechart.PieChart;
import com.google.gwt.visualization.client.visualizations.corechart.PieChart.PieOptions;
import com.google.gwt.visualization.client.visualizations.corechart.ScatterChart;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.ViewImpl;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.DragAppearance;
import com.smartgwt.client.types.HeaderControls;
import com.smartgwt.client.types.LayoutPolicy;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.VisibilityMode;
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
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.PortalLayout;
import com.smartgwt.client.widgets.layout.Portlet;
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
				
	private VLayout txnRatioPanel;
		
	private VLayout txnsBarPanel;
		
	private VLayout responseTimePanel;
	
	private VLayout conversationPanel;
	
	private VLayout rtOperationsPanel;
	
	private MainLayoutPresenter presenter;
	
	private Timer timer;
	
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
                
        VisualizationUtils.loadVisualizationApi(onloadCallback, PieChart.PACKAGE);
	}

	private void initializeWindow() {
		panel = new VLayout();
		panel.setWidth("100%");
		panel.setAlign(Alignment.CENTER);
		panel.setPadding(5);
		
		addHeaderLayout();
		
		HLayout body = new HLayout();
		body.setWidth100();
		body.setPadding(3);
		body.setHeight(700);
		panel.addMember(body);
				
		addSectionStack(body);
		
		VLayout main = new VLayout(15);
		main.setMargin(10);
		body.addMember(main);
		
		final int portalColumn = 3;
		PortalLayout portal = new PortalLayout(portalColumn);
		portal.setWidth100();
		portal.setHeight100();
		portal.setCanAcceptDrop(true);
		portal.setShowColumnMenus(false);
		portal.setBorder("0px");
		portal.setColumnBorder("0px");
			
		setPortalMenus(main);
		
        main.addMember(portal);
        
        final String txnRatioTitle = "Txn Ratio";
        Portlet txnRatio = createPortlet(txnRatioTitle, new ClickHandler() {
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
        
		txnRatioPanel = new VLayout();
		txnRatioPanel.setMargin(25);
		txnRatio.addChild(txnRatioPanel);        
        portal.addPortlet(txnRatio, 0, 0);
        
        
        Portlet txnsBar = createPortlet("Txn Bar Chart", new ClickHandler(){
			public void onClick(ClickEvent event) {
				presenter.refreshTxnBarChart(true);
			}
          }, new ClickHandler() {
				public void onClick(ClickEvent event) {
					showWindowModalWithChart("Txn Bar Chart", createTxnBarChart(stats, false));
				}         	
        }, new DragRepositionStopHandler() {
			public void onDragRepositionStop(DragRepositionStopEvent event) {
				presenter.refreshTxnBarChart(true);
			}        	
        });
        
        txnsBarPanel = new VLayout();
		txnsBarPanel.setMargin(25);
		txnsBar.addChild(txnsBarPanel);
        portal.addPortlet(txnsBar, 1, 0);
        
        Portlet responseTime = createPortlet("Response Time", new ClickHandler() {
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
        responseTimePanel = new VLayout();
		responseTimePanel.setMargin(25);
		responseTime.addChild(responseTimePanel);
        portal.addPortlet(responseTime, 2, 0);
        
        Portlet conversationPortlet = createPortlet("Conversation Chart", new ClickHandler(){
			public void onClick(ClickEvent event) {
				presenter.refreshConversationChart(true);
			}
        	
        }, new ClickHandler() {
			public void onClick(ClickEvent event) {
				showWindowModalWithChart("Conversation Chart", createConversationChart(cdetails, false));
			}        	
        }, new DragRepositionStopHandler(){
			public void onDragRepositionStop(DragRepositionStopEvent event) {
				presenter.refreshConversationChart(true);
			}
        	
        });
        conversationPanel = new VLayout();
        conversationPanel.setMargin(25);
        conversationPortlet.addChild(conversationPanel);
        portal.addPortlet(conversationPortlet,0, 1);
        

        Portlet rtOperationsPortlet = createPortlet("Buy Operation Response Time Chart", new ClickHandler(){
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
        
        rtOperationsPanel = new VLayout();
        rtOperationsPanel.setMargin(25);
        rtOperationsPortlet.addChild(rtOperationsPanel);
        portal.addPortlet(rtOperationsPortlet, 1, 1);
        
		addFooterLayout();
		panel.draw();
	}
	
	public void refreshTxnRatioChart(Statistic[] value, boolean isSmall) {
		PieChart pc = createTxnRatioChart(value, isSmall);
		txnRatioPanel.clear();
		txnRatioPanel.addChild(pc);
		txnRatioPanel.draw();
	}
	
	public void refreshTxnBarChart(Statistic[] value, boolean isSmall) {
		ColumnChart cc = createTxnBarChart(value, isSmall);
		txnsBarPanel.clear();
		txnsBarPanel.addChild(cc);
		txnsBarPanel.draw();
	}
	
	public void refreshResponseTime(ResponseTime[] value, boolean isSmall) {
		LineChart lc = createResponseTimeLineChart(value, isSmall);
		responseTimePanel.clear();
		responseTimePanel.addChild(lc);
		responseTimePanel.draw();
	}
	
	public void refreshRTWithOperation(ResponseTime[] value, boolean isSmall) {
		LineChart lc = createRTChartWithOperations(value, isSmall);
		rtOperationsPanel.clear();
		rtOperationsPanel.addChild(lc);
		rtOperationsPanel.draw();
	}
	
	public void refreshConversationChart(Conversation[] value, boolean isSmall) {
		LineChart chart = createConversationChart(value, isSmall);
		conversationPanel.clear();
		conversationPanel.addChild(chart);
		conversationPanel.draw();
	}
	
	private void showWindowModalWithChart(String title, CoreChart chart) {
		final Window window = new Window();
		window.setWidth(850);
		window.setHeight(650);
		window.setTitle(title);
		window.setShowMinimizeButton(false);
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
		chartLayout.addChild(chart);
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
		options.setWidth(300);
		options.setHeight(250);
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
		
		return new LineChart(dt, isSmall ? smallOptions() : bigOptions());
	}
	
	private LineChart createConversationChart(Conversation[] values, boolean isSmall) {
		DataTable dt = DataTable.create();
		dt.addColumn(ColumnType.STRING, "Conversation Id");
		dt.addColumn(ColumnType.NUMBER, "Conversation Status");
		dt.addRows(values == null ? 0 : values.length);
		if (values != null) {
			for (int i = 0; i < values.length; i++) {
				Conversation cd = values[i];
				dt.setValue(i, 0, cd.getConversationId());
				dt.setValue(i, 1, cd.getStatus() ? 1 : 0);
			}
		}
		
		return new LineChart(dt, isSmall ? smallOptions() : bigOptions());
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
		
		return new LineChart(dt, isSmall ? smallOptions() : bigOptions());
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
	
	private Portlet createPortlet(String title, ClickHandler refreshHandler, ClickHandler maximizeHandler,
			DragRepositionStopHandler dsHandler) {
        Portlet portlet = new Portlet();  
        portlet.setTitle(title);  
        portlet.setShowShadow(false);
        portlet.setDragAppearance(DragAppearance.OUTLINE);
        portlet.setHeaderControls(HeaderControls.MINIMIZE_BUTTON, HeaderControls.HEADER_LABEL,
        		new HeaderControl(HeaderControl.REFRESH, refreshHandler), new HeaderControl(HeaderControl.MAXIMIZE, maximizeHandler), HeaderControls.CLOSE_BUTTON);
        
        portlet.setVPolicy(LayoutPolicy.NONE);
        portlet.setOverflow(Overflow.VISIBLE);
        portlet.setAnimateMinimize(true);
        
        
        portlet.setWidth(350);
        portlet.setHeight(300);
        portlet.setCanDragResize(false);
                
        portlet.setCloseConfirmationMessage("Are you going to close the " + title + "?");
        
        portlet.addDragRepositionStopHandler(dsHandler);
        
        return portlet;
	}


	private void setPortalMenus(VLayout main) {
		final DynamicForm form = new DynamicForm();  
        form.setAutoWidth();  
        form.setNumCols(1);  
		
        ButtonItem addColumn = new ButtonItem("addAQ", "Add AQ Chart");  
        addColumn.setAutoFit(true);  
        addColumn.setStartRow(false);  
        addColumn.setEndRow(false);  
        
        form.setItems(addColumn);
        
        main.addMember(form);
	}


	private void addSectionStack(HLayout body) {
		final SectionStack  linkStack = new SectionStack();
		linkStack.setVisibilityMode(VisibilityMode.MULTIPLE);
		linkStack.setCanResizeSections(false);
		linkStack.setWidth(200);
		linkStack.setHeight(400);
		
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
		headerLabel.setContents("Savara SAM :: Header ");
		headerLabel.setSize("100%", "85");
		headerLabel.setAlign(Alignment.CENTER);
		headerLabel.setBorder("1px solid #808080");		
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
