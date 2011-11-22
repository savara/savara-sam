/**
 * 
 */
package org.savara.sam.web.client.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.savara.sam.web.client.presenter.MainLayoutPresenter;
import org.savara.sam.web.client.presenter.MainLayoutPresenter.MainLayoutView;
import org.savara.sam.web.client.view.ChartPortalLayout.ChartPortlet;
import org.savara.sam.web.shared.dto.AQChartModel;
import org.savara.sam.web.shared.dto.AQChartModel.ChartType;
import org.savara.sam.web.shared.dto.Conversation;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.visualization.client.VisualizationUtils;
import com.google.gwt.visualization.client.visualizations.corechart.PieChart;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.ViewImpl;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.HeaderControls;
import com.smartgwt.client.types.MultipleAppearance;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.HeaderControl;
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
	
	private VLayout main;
	
	private ChartPortalLayout portal;
	
	private List<AQChartModel> aqCharts = new ArrayList<AQChartModel>();
	
	private SelectItem aqSelect;
		
	@Inject
	public MainLayoutViewImpl() {
				
        Runnable onloadCallback = new Runnable() {
			public void run() {				
				initializeWindow();
				
				for (AQChartModel model : aqCharts) {
					refreshChartsDataFromServer(model);
				}
				
				timer = new Timer(){
					public void run() {
						for (AQChartModel model : aqCharts) {
							presenter.refreshChartData(model);
						}
					}};
				
				//TODO: looks like the refreshing action sometimes block the whole page.
			    //timer.scheduleRepeating(30 * 1000);
			}        	
        };
                
        VisualizationUtils.loadVisualizationApi(onloadCallback, PieChart.PACKAGE);
	}
	
	

	private void initializeWindow() {
		panel  = LayoutUtil.getPagePanel();
		panel.addMember(LayoutUtil.getHeaderLayout());
		
		HLayout body = new HLayout();
		body.setWidth100();
		body.setPadding(3);
		body.setHeight(850);
		panel.addMember(body);
				
		body.addMember(LayoutUtil.getMenuStack());
		
		main = new VLayout();
		main.setMargin(5);
		body.addMember(main);
		
		portal = new ChartPortalLayout(3);	
		portal.setMargin(8);
		
		setPortalMenus(main);
		
        main.addMember(portal);
        
        
        AQChartModel txnRatioModel = new AQChartModel();
        txnRatioModel.setName("Txn Ratio");
        txnRatioModel.setChartType(ChartType.PIE_CHART);
        txnRatioModel.setVerticalProperty("size");
        txnRatioModel.setHorizontalProperty("name");
        List<String> aqs = new ArrayList<String>();
        aqs.add("PurchasingSuccessful");
        aqs.add("PurchasingUnsuccessful");
        txnRatioModel.setActiveQueryNames(aqs);
               
        portal.addPortlet(createPortlet(txnRatioModel));
        
        AQChartModel txnsBarModel = new AQChartModel();
        txnsBarModel.setName("Txn Bar Chart");
        txnsBarModel.setChartType(ChartType.COLUMN_CHART);
        txnsBarModel.setVerticalProperty("size");
        txnsBarModel.setHorizontalProperty("name");
        List<String> txnsBarAQs = new ArrayList<String>();
        txnsBarAQs.add("PurchasingSuccessful");
        txnsBarAQs.add("PurchasingUnsuccessful");
        txnsBarAQs.add("PurchasingStarted");
        txnsBarModel.setActiveQueryNames(txnsBarAQs);
        txnsBarModel.setLegendName("Transactions");
        
        portal.addPortlet(createPortlet(txnsBarModel));
        
        AQChartModel responseTimeModel = new AQChartModel();
        responseTimeModel.setName("Response Time");
        responseTimeModel.setChartType(ChartType.LINE_CHART);
        responseTimeModel.setVerticalProperty("responseTime");
        responseTimeModel.setHorizontalProperty("requestTimestamp");
        List<String> responseTimeAQ = new ArrayList<String>();
        responseTimeAQ.add("PurchasingResponseTime");
        responseTimeModel.setActiveQueryNames(responseTimeAQ);
        responseTimeModel.setLegendName("Response Time");
        
        portal.addPortlet(createPortlet(responseTimeModel));
        
        AQChartModel conversationModel = new AQChartModel();
        conversationModel.setName("conversations list");
        conversationModel.setChartType(ChartType.TABLE_CHART);
        conversationModel.setActiveQueryNames("PurchasingConversation");
        conversationModel.setHorizontalProperty("test");
        conversationModel.setVerticalProperty("test");
        
        portal.addPortlet(createPortlet(conversationModel));
        
        AQChartModel buyRT = new AQChartModel();
        buyRT.setName("Buy_Operation_Response_Time");
        buyRT.setChartType(ChartType.LINE_CHART);
        buyRT.setVerticalProperty("responseTime");
        buyRT.setHorizontalProperty("requestTimestamp");
        buyRT.setActiveQueryNames("PurchasingResponseTime");
        buyRT.setLegendName("Response Time");
        buyRT.setPredicate("operation='buy'");
        
        portal.addPortlet(createPortlet(buyRT));
        
        
		panel.addMember(LayoutUtil.getFooterLayout());
		panel.draw();
	}
	
	private void showWindowModalWithChart(final AQChartModel model) {
		final Window window = new Window();
		window.setWidth(850);
		window.setHeight(650);
		
		final VLayout chartLayout = new VLayout();
		chartLayout.setMargin(25);
		
		HeaderControl refreshBtn = new HeaderControl(HeaderControl.REFRESH, new ClickHandler() {
			public void onClick(ClickEvent event) {
				//TODO: this is just a hack for now
				if (ChartType.TABLE_CHART.equals(model.getChartType())) {
					presenter.refreshTableChart(model, chartLayout, ChartManager.BIG_WIDTH, ChartManager.BIG_HEIGHT);
				} else {
					presenter.refreshChartData(model, chartLayout, ChartManager.BIG_WIDTH, ChartManager.BIG_HEIGHT);
				}
				
			}			
		});
		window.setHeaderControls(HeaderControls.HEADER_LABEL, refreshBtn, HeaderControls.CLOSE_BUTTON);
		
		window.setTitle(model.getName());
		window.setIsModal(true);
		window.setShowModalMask(true);
		window.centerInPage();
		
		window.addCloseClickHandler(new CloseClickHandler(){

			public void onCloseClick(CloseClientEvent event) {
				window.destroy();
			}
			
		});
		
		//TODO: this is just a hack for now
		if (ChartType.TABLE_CHART.equals(model.getChartType())) {
			presenter.refreshTableChart(model, chartLayout, ChartManager.BIG_WIDTH, ChartManager.BIG_HEIGHT);
		} else {
			presenter.refreshChartData(model, chartLayout, ChartManager.BIG_WIDTH, ChartManager.BIG_HEIGHT);
		}
		window.addChild(chartLayout);
		window.show();
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
				window.setWidth(450);
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
				title.setTitle("name");
				
				aqSelect = new SelectItem();
				aqSelect.setTitle("Active Queries");
				aqSelect.setMultiple(true);
				aqSelect.setMultipleAppearance(MultipleAppearance.PICKLIST);
				presenter.refreshActiveQueries();
				
				final TextItem predicate = new TextItem();
				predicate.setTitle("Predicate");
				
				final SelectItem chartType = new SelectItem();
				chartType.setTitle("Chart Type");
				chartType.setValueMap(ChartType.PIE_CHART.toString(), ChartType.COLUMN_CHART.toString(), ChartType.LINE_CHART.toString(), ChartType.TABLE_CHART.toString());
				
				final SelectItem xAxis = new SelectItem();
				xAxis.setTitle("X Axis mapped property");
				xAxis.setValueMap("requestTimestamp", "name");
				
				final SelectItem yAxis = new SelectItem();
				yAxis.setTitle("Y Axis mapped property");
				yAxis.setValueMap("responseTime", "size");
				
				final ButtonItem submitBtn = new ButtonItem();
				submitBtn.setTitle("Create");
				submitBtn.setEndRow(false);
				submitBtn.setAlign(Alignment.CENTER);
				
				submitBtn.addClickHandler(new com.smartgwt.client.widgets.form.fields.events.ClickHandler(){

					public void onClick(
							com.smartgwt.client.widgets.form.fields.events.ClickEvent event) {
							final String theTitle = title.getValueAsString();
							
							if (!portal.isTitleUnique(theTitle)) {
								return;
							}
							
							AQChartModel model = new AQChartModel();
							model.setName(theTitle);
							model.setChartType(ChartType.valueOf(chartType.getValueAsString()));
							model.setHorizontalProperty(xAxis.getValueAsString());
							model.setVerticalProperty(yAxis.getValueAsString());
							model.setActiveQueryNames(aqSelect.getValues());
							
							window.destroy();
							
							portal.addPortlet(createPortlet(model));
							refreshChartsDataFromServer(model);
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
				
				form.setFields(title, aqSelect, predicate, chartType, xAxis, yAxis, submitBtn, cancelBtn);
				
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
	
	public Widget asWidget() {
		return panel;
	}

	public void setPresenter(MainLayoutPresenter presenter) {
		this.presenter = presenter;
	}
	
	private ChartPortlet createPortlet(final AQChartModel model) {
	        ChartPortlet txnRatio = portal.createPortlet(model.getName(), new ClickHandler() {
				public void onClick(ClickEvent event) {					
					refreshChartsDataFromServer(model);
				}        	
	        }, new ClickHandler() {
				public void onClick(ClickEvent event) {
					showWindowModalWithChart(model);
				}        	
	        }, new DragRepositionStopHandler() {

				public void onDragRepositionStop(DragRepositionStopEvent event) {
                    Timer theTimer = new Timer(){
                        @Override
                        public void run() {
                                refreshChartsDataFromServer(model);
                        }                                               
                };
                //Looks like the portlet wouldn't be in the portalcolumn object when do the d&d straight away, so w
                theTimer.schedule(1000);
				}
	        	
	        }); 
		
	        aqCharts.add(model);
		return txnRatio;
	}
	
	public void refreshChart(AQChartModel model, Map data, VLayout panel, int width, int height) {		
		panel.clear();
		
		if (AQChartModel.ChartType.PIE_CHART.equals(model.getChartType()) 
				|| AQChartModel.ChartType.COLUMN_CHART.equals(model.getChartType())) {
			Map<String, Integer> result = new HashMap<String, Integer>();
			for (Object key : data.keySet()) {
				result.put((String)key, (Integer)data.get(key));
			}
			if (AQChartModel.ChartType.PIE_CHART.equals(model.getChartType())) {
				panel.addChild(ChartManager.createPieChart(result, model.getName(), width, height));
			} else {
				panel.addChild(ChartManager.createColumnChart(result, model.getName(), model.getLegendName(), width, height));
			}
		} else if (AQChartModel.ChartType.LINE_CHART.equals(model.getChartType())){
			Map<Long, Long> result = new HashMap<Long, Long>();
			for (Object key : data.keySet()) {
				result.put((Long)key, (Long)data.get(key));
			}
			panel.addChild(ChartManager.createLineChart(result, model.getName(), model.getLegendName(),  width, height, 9000));
		}		
		panel.draw();		
	}
	
	public void refreshChart(AQChartModel model, Map data) {
		VLayout chartPanel = portal.getChartPortlet(model.getName()).getChartPanel();
		refreshChart(model, data, chartPanel, ChartManager.SMALL_WIDTH, ChartManager.SMALL_HEIGHT);
	}

	public void setActiveQueries(List<String> activeQueries) {
		aqSelect.setValueMap(activeQueries.toArray(new String[0]));
	}



	public void refreshConversationChart(AQChartModel model, List<Conversation> conversations, VLayout thePanel, int width,
			int height) {		
		thePanel.clear();
		thePanel.addChild(ChartManager.createConversationChart(conversations, width, height));
		thePanel.draw();
	}



	public void refreshConversationChart(AQChartModel model, List<Conversation> conversations) {
		VLayout chartPanel = portal.getChartPortlet(model.getName()).getChartPanel();
		refreshConversationChart(model, conversations, chartPanel, ChartManager.SMALL_WIDTH, ChartManager.SMALL_HEIGHT);
	}



	private void refreshChartsDataFromServer(AQChartModel model) {
		//TODO: this is a hack for now.
		if (ChartType.TABLE_CHART.equals(model.getChartType())) {
			presenter.refreshTableChart(model);
		} else {
			presenter.refreshChartData(model);
		}
	}

}
