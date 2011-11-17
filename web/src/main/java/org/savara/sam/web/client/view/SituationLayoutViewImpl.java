/**
 * 
 */
package org.savara.sam.web.client.view;

import org.savara.sam.web.client.presenter.SituationLayoutPresenter;
import org.savara.sam.web.client.presenter.SituationLayoutPresenter.SituationLayoutView;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.ViewImpl;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;

/**
 * @author Jeff Yu
 * @date Nov 17, 2011
 */
public class SituationLayoutViewImpl extends ViewImpl implements SituationLayoutView {

	private SituationLayoutPresenter presenter;
	
	private SectionStack grid;
	
	@Inject
	public SituationLayoutViewImpl() {
        SectionStack sectionStack = new SectionStack();  
        sectionStack.setWidth(550);  
        sectionStack.setHeight(230);  
  
        String title = "Situations";  
        SectionStackSection section = new SectionStackSection(title);  
  
        section.setCanCollapse(false);  
        section.setExpanded(true);  
  
        final ListGrid countryGrid = new ListGrid();  
        countryGrid.setWidth(550);  
        countryGrid.setHeight(224);  
        countryGrid.setShowAllRecords(true);  
        countryGrid.setCellHeight(22);  
  
        ListGridField countryCodeField = new ListGridField("countryCode", "Flag", 40);  
        countryCodeField.setAlign(Alignment.CENTER);  
        countryCodeField.setType(ListGridFieldType.IMAGE);  
        countryCodeField.setImageURLPrefix("flags/16/");  
        countryCodeField.setImageURLSuffix(".png");  
        countryCodeField.setCanEdit(false);  
  
        ListGridField nameField = new ListGridField("countryName", "Country");  
        ListGridField continentField = new ListGridField("continent", "Continent");  
        ListGridField memberG8Field = new ListGridField("member_g8", "Member G8");  
        ListGridField populationField = new ListGridField("population", "Population");  
        populationField.setType(ListGridFieldType.INTEGER); 
        
        ListGridField independenceField = new ListGridField("independence", "Independence");  
        countryGrid.setFields(countryCodeField, nameField,continentField, memberG8Field, populationField, independenceField);  
  
        countryGrid.setAutoFetchData(true);  
        countryGrid.setCanEdit(true);  
  
        section.setItems(countryGrid);  
        sectionStack.setSections(section);  
        sectionStack.draw();  
	}
	
	
	public void setPresenter(SituationLayoutPresenter presenter) {
		this.presenter = presenter;
	}
	
	
	public Widget asWidget() {
		return grid;
	}

}
