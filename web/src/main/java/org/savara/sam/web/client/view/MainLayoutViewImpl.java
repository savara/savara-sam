/**
 * 
 */
package org.savara.sam.web.client.view;

import org.savara.sam.web.client.presenter.MainLayoutPresenter.MainLayoutView;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.ViewImpl;

/**
 * @author jeffyu
 *
 */
public class MainLayoutViewImpl extends ViewImpl implements MainLayoutView{
	
	private DockLayoutPanel panel;
	
	private LayoutPanel headerPanel;
	
	private LayoutPanel mainContentPanel;
	
	private LayoutPanel footerPanel;
	
	@Inject
	public MainLayoutViewImpl() {
		mainContentPanel = new LayoutPanel();		
		headerPanel = new LayoutPanel();
		Label label = new Label();
		label.setText("Header Part.....");
		headerPanel.add(label);
		
		footerPanel = new LayoutPanel();
		Label footerLabel = new Label();
		footerLabel.setText("footer Part.....");		
		footerPanel.add(footerLabel);
		
		panel = new DockLayoutPanel(Style.Unit.PX);
		panel.addNorth(headerPanel, 64);
		panel.addSouth(footerPanel, 30);
		panel.add(mainContentPanel);
		
	}
	
	
	public Widget asWidget() {
		return panel;
	}

}
