package org.jboss.datavirtualization.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;

/**
 * Resources used by the entire application.
 */
public interface Resources extends ClientBundle {

  public static final Resources INSTANCE =  GWT.create(Resources.class);

  @Source("DataVirt.css")
  Style style();

  @Source("../images/RH-Product-Name.png")
  ImageResource dataVirtProductName();

  public interface Style extends CssResource {
	String myTabBar();
    String loginPanel();
    String loginPanelLogo();
    String loginPanelForm();
    String applicationTitle();
    String titleLarge();
    String titleMedium();
    String titleSmall();
    String titleSmallItalics();
    String labelText();
    String labelTextItalics();
    String labelTextItalicsRed();
    String labelTextBold();
    String loginLabel();
    String loginStatus();
    String topPadding20();
    String bottomPadding10();
    String rightPadding5();
    String leftPadding15();
    String dialogButton();
    String paddingBottom10Left20();
    String paddingBannerImage();
    String paddingTabPanel();
    String vdbStatusActive();
    String vdbStatusInactive();
    String queryPanelLoadStatusLabel();
    String messageDialogPanel();
    String messageDialogErrorMessage();
    String messageDialogMessage();
    String addEditModelDialogPanel();
    String createEditDataSourceDialogPanel();
    String createVdbDialogPanel();
    String deleteVdbDialogPanel();
    String copySourceDialogPanel();
    String deleteSourceDialogPanel();
    String deleteDriverDialogPanel();
    String removeModelsDialogPanel(); 
    String showVdbXmlDialogPanel(); 
    String sqlXmlDialogPanel(); 
    String appLoginDialogPanel();
    String deploySampleSourcesDialogPanel();
    String deploySampleVdbDialogPanel();
    String driversTable(); 
    String driversTableCell();
    String driversTableHeader(); 
    String driversTableEvenRow();
    String driversTableOddRow();
    String driverButton();
    String uploadPanel();
    String dataSourcesTable();
    String dataSourcesTableCell();
    String dataSourcesTableHeader();
    String dataSourcesTableEvenRow();
    String dataSourcesTableOddRow();
    String dataSourceButton();
    String dataSourcePropertiesTable();
    String dataSourcePropertiesTableCell();
    String dataSourcePropertiesTableCellItalics();
    String dataSourcePropertiesTableCellUnmodifiable();
    String dataSourcePropertiesTableHeader();
    String dataSourcePropertiesTableEvenRow();
    String dataSourcePropertiesTableOddRow();
    String vdbListBox();
    String vdbModelsTable();
    String vdbModelsTableCell();
    String vdbModelsTableHeader();
    String vdbModelsTableEvenRow();
    String vdbModelsTableOddRow();
    String vdbButton();
    String sqlResultsPanelTitle();
    String sqlResultsTable();
    String sqlResultsTableHeader();
    String sqlResultsTableEvenRow();
    String sqlResultsTableOddRow();
  }
}


