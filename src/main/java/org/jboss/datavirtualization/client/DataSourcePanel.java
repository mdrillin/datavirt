package org.jboss.datavirtualization.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.datavirtualization.client.dialogs.ConfirmDialog;
import org.jboss.datavirtualization.client.dialogs.CreateEditDataSourceDialog;
import org.jboss.datavirtualization.client.dialogs.MessageDialog;
import org.jboss.datavirtualization.client.events.SourcesChangedEvent;
import org.jboss.datavirtualization.client.events.SourcesChangedEventHandler;
import org.jboss.datavirtualization.client.rpc.TeiidMgrServiceAsync;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTMLTable.Cell;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class DataSourcePanel extends Composite {

	private static DataSourcePanelUiBinder uiBinder = GWT.create(DataSourcePanelUiBinder.class);

	interface DataSourcePanelUiBinder extends UiBinder<Widget, DataSourcePanel> {
	}

	private final Messages messages = GWT.create(Messages.class);
	private final TeiidMgrServiceAsync teiidMgrService;
	// EventBus and TeiidMgrService
	private SimpleEventBus eventBus;
	// Flag allows different handling when running on OpenShift
	private boolean isRunningOnOpenShift = false;

	// Message Dialog
	private MessageDialog messageDialog = new MessageDialog();

	// Copy Source Dialog Controls
	private DialogBox copySourceDialogBox = new DialogBox();
	private Label copySourceDialogMessageLabel= new Label();
	// Source Type and Name Controls
	private TextBox copySourceNameTextBox = new TextBox();
	private Button copySourceDialogOKButton = new Button(messages.okButton());
	private Button copySourceDialogCloseButton = new Button(messages.cancelButton());

	// Data Sources Table and Table Header
	private FlexTable dataSourcesTable = new FlexTable();
	private List<String> currentDSNames = new ArrayList<String>();
	private DriverPanel driversPanel;
	
	// Within the owner class for the UiBinder template
	@UiField Resources res;

	@UiField Button createSourceButton;
	@UiField Button deleteSourceButton;
	@UiField Button editSourceButton;
	@UiField Button copySourceButton;
	@UiField Button createSamplesButton;
    @UiField VerticalPanel driversPanelContainer;
    @UiField ScrollPanel dataSourcesTableScrollPanel;
    @UiField Label dummyLabel;

    /*
	 * Constructor for the Panel
	 */
	public DataSourcePanel(SimpleEventBus eventBus,boolean isRunningOnOpenShift,TeiidMgrServiceAsync teiidMgrService,int panelHeight) {
		res.style().ensureInjected();
		
		initWidget(uiBinder.createAndBindUi(this));

		this.eventBus = eventBus;
		this.teiidMgrService = teiidMgrService;
		this.isRunningOnOpenShift = isRunningOnOpenShift;
		
		initComponents();

		// Establish panel height
		dummyLabel.setHeight(String.valueOf(panelHeight)+"px");		
	}
	
	@UiHandler("createSourceButton")
	void onCreateSourceButtonClick(ClickEvent e) {
		// Init the Create DataSource Dialog
		CreateEditDataSourceDialog createDialog = new CreateEditDataSourceDialog(eventBus,isRunningOnOpenShift,teiidMgrService);
		// Show Add Source Dialog
		createDialog.showDialogForAdd(currentDSNames, createSourceButton);
	}
	
	@UiHandler("deleteSourceButton")
	void onDeleteSourceButtonClick(ClickEvent event) {
		showDeleteSourceDialog();
	}
	
	@UiHandler("editSourceButton")
	void onEditSourceButtonClick(ClickEvent e) {
		// Init the Edit DataSource dialog
		CreateEditDataSourceDialog editDialog = new CreateEditDataSourceDialog(eventBus,isRunningOnOpenShift,teiidMgrService);
		// Show Edit Source Dialog
		String sourceName = getSourceNameForFirstSelectedRow();
		String sourceDriver = getSourceDriverForFirstSelectedRow();
		editDialog.showDialogForEdit(sourceName, sourceDriver, editSourceButton);
	}
	
	@UiHandler("copySourceButton")
	void onCopySourceButtonClick(ClickEvent event) {
		showCopySourceDialog();
	}
	
	@UiHandler("createSamplesButton")
	void onCreateSamplesButtonClick(ClickEvent event) {
		showDeploySampleSourcesDialog();
	}

	/*
	 * Init Components
	 */
	private void initComponents( ) {

		// -----------------------
		// Initial Button States
		// -----------------------
		// Initial state of Create Button is enabled.  It will remain enabled.
		createSourceButton.setEnabled(true);
		// Initial state of Delete Button is disabled.  It will enable if at least one source row is selected.
		deleteSourceButton.setEnabled(false);
		// Initial state of Edit Button is disabled.  It will enable if a source row is selected.
		editSourceButton.setEnabled(false);
		// Initial state of Copy Button is disabled.  It will enable if a source row is selected.
		copySourceButton.setEnabled(false);

		// --------------------
		// Data Sources Table
		// --------------------;
		dataSourcesTable.addStyleName(res.style().dataSourcesTable());
		// ClickHandler - Use Row selection to toggle the 'selected' state
		dataSourcesTable.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				Cell cell = dataSourcesTable.getCellForEvent(event);
				int rowIndex = cell.getRowIndex();
				int colIndex = cell.getCellIndex();
				if(rowIndex>0 && colIndex>0) {
					// Toggle the Selected checkbox
					Widget widget = dataSourcesTable.getWidget(rowIndex, 0);
					if(widget!=null && widget instanceof CheckBox) {
						CheckBox checkBox = (CheckBox) widget;
						checkBox.setValue(!checkBox.getValue());
						setDataSourceMgmtButtonEnablements();
					}
				}
			}
		});
		
		dataSourcesTableScrollPanel.setWidget(dataSourcesTable);

		// --------------------
		// Drivers Panel
		// --------------------
		driversPanel = new DriverPanel(this.eventBus, this.teiidMgrService);
		driversPanelContainer.add(driversPanel);
		
		// --------------------
		// Event Handler
		// --------------------
	    // Listen for SourcesChangedEvent from Add Source Dialog, so we can update the sources table.
		eventBus.addHandler(SourcesChangedEvent.TYPE, new SourcesChangedEventHandler() {
			public void onEvent(SourcesChangedEvent event) {
				refresh();
			}
		});
		
		// ---------------------------------------
		// Widgets for Copy Source Dialog
		// ---------------------------------------

		// Click Handler for DialogBox Close Button
		copySourceDialogCloseButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				copySourceDialogBox.hide();
			}
		});
		
		// Click Handler for DialogBox OK Button
		copySourceDialogOKButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				String selectedDataSourceName = getSourceNameForFirstSelectedRow();
				String newDSName = copySourceNameTextBox.getText();
				copyDataSource(selectedDataSourceName,newDSName);

				copySourceDialogBox.hide();
			}
		});

		// Change Listener for Source Name TextBox - does name validation
		copySourceNameTextBox.addKeyUpHandler(new KeyUpHandler() {
	        public void onKeyUp(KeyUpEvent event) {
	        	setCopySourceDialogOKButtonEnablement();
	        }
	    });
	}
	
	/**
	 * Refresh the panel
	 */
	public void refresh() {
		refreshTable();
		driversPanel.refresh();
		initDataSourceDefaults();
	}
	
    /*
	 * Refresh the Data Sources table
	 * @param vdbName the name of the VDB to refresh
	 */
	public void refreshTable( ) {
		// Set up the callback object.
		AsyncCallback<List<List<DataItem>>> callback = new AsyncCallback<List<List<DataItem>>>() {
			// On Failure - show Error Dialog
			public void onFailure(Throwable caught) {
				showErrorDialog(messages.refreshDataSourceTableErrorTitle(), messages.refreshDataSourceTableErrorMsg()+caught.getMessage());
				setDataSourceMgmtButtonEnablements();
			}

			// On Success - Populate the ListBox
			public void onSuccess(List<List<DataItem>> result) {
				populateDataSourcesTable(result);
				setDataSourceMgmtButtonEnablements();
			}
		};

		teiidMgrService.getDataSourceInfos(callback);	
	}
	
	/*
	 * Init the DataSources and default properties
	 */
	private void initDataSourceDefaults( ) {
		// Set up the callback object.
		AsyncCallback<Map<String,List<PropertyItem>>> callback = new AsyncCallback<Map<String,List<PropertyItem>>>() {
			// On Failure - show Error Dialog
			public void onFailure(Throwable caught) {
				showErrorDialog(messages.initDSNameErrorTitle(), messages.initDSNameErrorMsg()+caught.getMessage());
			}

			// On Success - Populate the ListBox
			public void onSuccess(Map<String,List<PropertyItem>> dsPropObjMap) {
				// Save the DataSource Default PropertyObj Map for later use
				DataSourceHelper.getInstance().setDefaults(dsPropObjMap);
			}
		};

		teiidMgrService.getDefaultPropertyItemMap(callback);	
	}

	/*
	 * Set the DataSource Management Button Enabled states
	 * @param vdbName the name of the VDB to refresh
	 */
	private void setDataSourceMgmtButtonEnablements() {
		// Get the list of selected table rows
		List<String> selectedSources = getDataSourceTableNames(true);

		// DeleteSourceButton - If anything is checked, the button is enabled
		if(selectedSources.size()>0) {
			deleteSourceButton.setEnabled(true);
		} else {
			deleteSourceButton.setEnabled(false);
		}
		
		// EditSourceButton - If one row is checked, the button is enabled
		if(selectedSources.size()==1) {
			editSourceButton.setEnabled(true);
		} else {
			editSourceButton.setEnabled(false);
		}
		
		// CopySourceButton - If one row is checked, the button is enabled
		if(selectedSources.size()==1) {
			copySourceButton.setEnabled(true);
		} else {
			copySourceButton.setEnabled(false);
		}
		
	}
	
	/*
	 * Return the list of all DataSource names from the Sources table.
	 * @param onlySelected if 'true', only the selected source names are returned.  if 'false', all source names are returned.
	 * @return the list of DataSource Names
	 */
	private List<String> getDataSourceTableNames(boolean onlySelected) {
		List<String> sourceNames = new ArrayList<String>();
        for (int i = 1, n = dataSourcesTable.getRowCount(); i < n; i++) {
            CheckBox box = (CheckBox) dataSourcesTable.getWidget(i, 0);
            String sourceName = dataSourcesTable.getText(i, 1);
            if(!onlySelected) {
            	sourceNames.add(sourceName);
            } else if(box.getValue()) {
            	sourceNames.add(sourceName);
            }
        }
        return sourceNames;
	}
	
	/*
	 * Get the DataSource Name for the first selected row in the Sources Table.
	 * @return the DataSource Name, null if nothing is selected
	 */
	private String getSourceNameForFirstSelectedRow() {
		String sourceName = null;
        for (int i = 1, n = dataSourcesTable.getRowCount(); i < n; i++) {
            CheckBox box = (CheckBox) dataSourcesTable.getWidget(i, 0);
            if(box.getValue()) {
            	sourceName = dataSourcesTable.getText(i,1);
            	break;
            }
        }
    	return sourceName;
	}
	
	/*
	 * Get the DataSource Driver for the first selected row in the Sources Table.
	 * @return the DataSource Driver, null if nothing is selected
	 */
	private String getSourceDriverForFirstSelectedRow() {
		String driverName = null;
        for (int i = 1, n = dataSourcesTable.getRowCount(); i < n; i++) {
            CheckBox box = (CheckBox) dataSourcesTable.getWidget(i, 0);
            if(box.getValue()) {
            	driverName = dataSourcesTable.getText(i,2);
            	break;
            }
        }
    	return driverName;
	}
	
	/*
	 * Clear the Data Sources Table, then re-populate it using the supplied rows
	 * @param rowData the List of Row info for re-populating the Data Sources table
	 */
	private void populateDataSourcesTable(List<List<DataItem>> rowData) {
		// Clear Previous Results
		clearDataSourcesTable();
		currentDSNames.clear();
		
		int iRow = 0;
		for(List<DataItem> row: rowData) {
			// Data Row 0 is header Row
			int nCols = row.size();
			
			// Handle first column (selection checkbox).  This is addnl column - not in rowData...
			if(iRow==0) {
				dataSourcesTable.setText(iRow,0,"");
				dataSourcesTable.getCellFormatter().addStyleName(iRow, 0, res.style().dataSourcesTableCell());
			} else {
				// First Column is a remove checkbox
				CheckBox rowCheckbox = new CheckBox("");
				rowCheckbox.addClickHandler(new ClickHandler() {
					public void onClick(ClickEvent event) {
						setDataSourceMgmtButtonEnablements();
					}
				});
				dataSourcesTable.setWidget(iRow, 0, rowCheckbox);
			}
			
			// Columns 2+ contain the returned data
			for(int iCol=0; iCol<nCols; iCol++) {
				DataItem data = (DataItem)row.get(iCol);
				dataSourcesTable.setText(iRow,iCol+1,data.getData());
				dataSourcesTable.getCellFormatter().addStyleName(iRow, iCol+1, res.style().dataSourcesTableCell());
				if(iCol==0) {
					currentDSNames.add(data.getData());
				}
			}
			
			// Header Row Style
			if(iRow==0) {
				dataSourcesTable.getRowFormatter().addStyleName(iRow, res.style().dataSourcesTableHeader());
		    // Even Row Style
			} else {
				boolean isEven = (iRow % 2 == 0);
				if(isEven) {
					dataSourcesTable.getRowFormatter().addStyleName(iRow, res.style().dataSourcesTableEvenRow());
				} else {
					dataSourcesTable.getRowFormatter().addStyleName(iRow, res.style().dataSourcesTableOddRow());
				}
			}
			iRow++;
		}
		
	}
	
	/*
	 * Clear the Data Sources Table 
	 */
	private void clearDataSourcesTable( ) {
		dataSourcesTable.removeAllRows();
	}
	
	/*
	 * Show the Copy Source Dialog
	 */
	private void showCopySourceDialog( ) {
		initCopySourceDialog();
		
		copySourceDialogBox.showRelativeTo(copySourceButton);
		copySourceDialogOKButton.setFocus(true);
	}
	
	/*
	 * Create sample sources
	 */
	private void doCreateSamples( ) {
		List<String> sourceNames = new ArrayList<String>();
		sourceNames.add("SampleSalesforce");
		sourceNames.add("SampleRemoteFlatFile");
		sourceNames.add("SampleRemoteXmlFile");
		
		List<String> sourceTypes = new ArrayList<String>();
		sourceTypes.add("salesforce");
		sourceTypes.add("webservice");
		sourceTypes.add("webservice");
		
		Map<String,Map<String,String>> sourcesPropsMap = new HashMap<String,Map<String,String>>();
		
		List<PropertyItem> sfDefaults = DataSourceHelper.getInstance().getDefaultProps("salesforce");
		List<PropertyItem> wsDefaults = DataSourceHelper.getInstance().getDefaultProps("webservice");
		
		Map<String,String> sfProps = DataSourceHelper.convertPropsForApiSubmittal(sfDefaults);
		sfProps.put("username","<user>");
		sfProps.put("password","passwd");

		Map<String,String> ws1Props = DataSourceHelper.convertPropsForApiSubmittal(wsDefaults);
		ws1Props.put("EndPoint","http://download.jboss.org/teiid/designer/data/partssupplier/file/supplier_data.txt");
		
		Map<String,String> ws2Props = DataSourceHelper.convertPropsForApiSubmittal(wsDefaults);
		ws2Props.put("EndPoint","http://download.jboss.org/teiid/designer/data/employees/xml/EMPLOYEEDATA.xml");
		
		sourcesPropsMap.put("SampleSalesforce",sfProps);
		sourcesPropsMap.put("SampleRemoteFlatFile",ws1Props);
		sourcesPropsMap.put("SampleRemoteXmlFile",ws2Props);
		
		addDataSources(sourceNames,sourceTypes,sourcesPropsMap);
	}
	
	/*
	 * Add Sample Sources
	 * @param sourceName the source name
	 * @param templateName the template name
	 * @param propsMap the property Map of name-value pairs
	 */
	private void addDataSources(List<String> sourceNames, List<String> templateNames, Map<String,Map<String,String>> sourcePropsMap) {
		// Set up the callback object.
		AsyncCallback<String> callback = new AsyncCallback<String>() {
			// On Failure - show Error Dialog
			public void onFailure(Throwable caught) {
				showErrorDialog(messages.createSourceErrorTitle(), messages.createSourceErrorMsg()+caught.getMessage());
			}

			// On Success - Populate the ListBox
			public void onSuccess(String result) {
				refresh();
			}
		};
		
		teiidMgrService.addDataSources(sourceNames, templateNames, sourcePropsMap, callback);		
	}
	
	/*
	 * Init the Dialog for Copying a Source
	 */
	private void initCopySourceDialog( ) {
		String sourceName = getSourceNameForFirstSelectedRow();

		// Create the popup DialogBox
		copySourceDialogBox.setText(messages.dialogCopySourceTitle());
		copySourceDialogBox.setAnimationEnabled(true);
		
		// Dialog Box - Panel Content
		VerticalPanel vPanel = new VerticalPanel();
		vPanel.addStyleName(res.style().copySourceDialogPanel());

		Label copySourceDialogTitle2Label = new Label(messages.dialogCopySourceTitle2()+"'"+sourceName+"'");
		copySourceDialogTitle2Label.addStyleName(res.style().labelTextBold());
		copySourceDialogTitle2Label.addStyleName(res.style().bottomPadding10());
		vPanel.add(copySourceDialogTitle2Label);
		
		// Message
		copySourceDialogMessageLabel = new Label(messages.dialogCopySourceMsg());
		copySourceDialogMessageLabel.addStyleName(res.style().labelTextItalics());
		copySourceDialogMessageLabel.addStyleName(res.style().bottomPadding10());
		vPanel.add(copySourceDialogMessageLabel);
		
		// New SourceName controls
		HorizontalPanel newSourcePanel = new HorizontalPanel();
		newSourcePanel.add(new Label(messages.nameLabel()));
		newSourcePanel.add(copySourceNameTextBox);
		vPanel.add(newSourcePanel);

		// Buttons Panel
		HorizontalPanel buttonPanel = new HorizontalPanel();
		buttonPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
		buttonPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
		copySourceDialogOKButton.addStyleName(Resources.INSTANCE.style().dialogButton());
		copySourceDialogCloseButton.addStyleName(Resources.INSTANCE.style().dialogButton());
		copySourceDialogCloseButton.addStyleName(Resources.INSTANCE.style().rightPadding5());
		buttonPanel.add(copySourceDialogCloseButton);
		buttonPanel.add(copySourceDialogOKButton);
		vPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
		vPanel.add(buttonPanel);
		
		copySourceDialogBox.setHeight("100px");
		copySourceDialogBox.setWidth("150px");

		// Add the Completed Panel to the Dialog
		copySourceDialogBox.setWidget(vPanel);
	}
	
	/*
	 * Validates the DataSource name on Copy DataSource dialog, and sets the 'OK' button enabled state.
	 */
	private void setCopySourceDialogOKButtonEnablement() {
		boolean copySourceEnabled = false;
		
		// Validate the entered SourceName
		String newSourceName = copySourceNameTextBox.getText();
		if(newSourceName==null || newSourceName.trim().length()==0) {
			copySourceDialogMessageLabel.setText(messages.dialogCopySourceMsg());
		} else if(currentDSNames.contains(newSourceName.trim())) {
			copySourceDialogMessageLabel.setText(messages.dialogCopySourceExistsMsg());
		} else {
			copySourceDialogMessageLabel.setText(messages.statusClickOKToAccept());
			copySourceEnabled = true;
		}
		
		// Disable the OK button if name is invalid
		copySourceDialogOKButton.setEnabled(copySourceEnabled);
	}
	
	/*
	 * Show the Confirm Dialog for Deleting Source(s)
	 */
	private void showDeleteSourceDialog( ) {
		final ConfirmDialog confirmDialog = new ConfirmDialog();

		// Handler for OK Button
		ClickHandler okHandler = new ClickHandler() {
			public void onClick(ClickEvent event) {
				List<String> checkedSrcs = new ArrayList<String>();
	            for (int i = 1, n = dataSourcesTable.getRowCount(); i < n; i++) {
	                CheckBox box = (CheckBox) dataSourcesTable.getWidget(i, 0);
                    String sourceName = dataSourcesTable.getText(i, 1);
                    if(box.getValue()) {
                    	checkedSrcs.add(sourceName);
                    }
	            }
				deleteSelectedSources(checkedSrcs);

				confirmDialog.hide();
			}
		};
		
		confirmDialog.showConfirm(messages.dialogDeleteSourceTitle(),"<b>"+messages.dialogDeleteSourceMsg()+"</b>",okHandler,deleteSourceButton);
	}
	
	/*
	 * Show the Confirm Dialog for Deploying Sample Sources
	 */
	private void showDeploySampleSourcesDialog( ) {
		final ConfirmDialog confirmDialog = new ConfirmDialog();

		// Handler for OK Button
		ClickHandler okHandler = new ClickHandler() {
			public void onClick(ClickEvent event) {
				doCreateSamples();
				
				confirmDialog.hide();
			}
		};
		
		confirmDialog.showConfirm(messages.dialogDeploySampleSourcesTitle(),"<b>"+messages.dialogDeploySampleSourcesMsg()+"</b>",okHandler,createSamplesButton);
	}
	
	/*
	 * Deletes the List of data sources
	 * @param deleteSrcs the list of sources to delete
	 */
	private void deleteSelectedSources(List<String> deleteSrcs) {
		// Set up the callback object.
		AsyncCallback<List<List<DataItem>>> callback = new AsyncCallback<List<List<DataItem>>>() {
			// On Failure - show Error Dialog
			public void onFailure(Throwable caught) {
				showErrorDialog(messages.removeDataSourceErrorTitle(), messages.removeDataSourceErrorMsg()+caught.getMessage());
				setDataSourceMgmtButtonEnablements();
			}

			// On Success - Populate the ListBox
			public void onSuccess(List<List<DataItem>> result) {
				populateDataSourcesTable(result);
				setDataSourceMgmtButtonEnablements();
			}
		};

		// service call to delete sources
		teiidMgrService.deleteDataSources(deleteSrcs, callback);		
	}

	/*
	 * Copy the DataSource, giving it the new name
	 * @param deleteSrcs the list of sources to delete
	 */
	private void copyDataSource(String sourceToCopy, String newSourceName) {
		// Set up the callback object.
		AsyncCallback<List<List<DataItem>>> callback = new AsyncCallback<List<List<DataItem>>>() {
			// On Failure - show Error Dialog
			public void onFailure(Throwable caught) {
				showErrorDialog(messages.copyDataSourceErrorTitle(), messages.copyDataSourceErrorMsg()+caught.getMessage());
				setDataSourceMgmtButtonEnablements();
			}

			// On Success - Populate the ListBox
			public void onSuccess(List<List<DataItem>> result) {
				populateDataSourcesTable(result);
				setDataSourceMgmtButtonEnablements();
			}
		};

		teiidMgrService.copyDataSource(sourceToCopy, newSourceName, callback);		
	}

	/*
	 * Show the Dialog for Error Display
	 */
	private void showErrorDialog(String title, String msg) {
		this.messageDialog.showError(title,msg);
	}

}
