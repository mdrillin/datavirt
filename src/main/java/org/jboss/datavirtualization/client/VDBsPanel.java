package org.jboss.datavirtualization.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.datavirtualization.client.dialogs.AddEditModelDialog;
import org.jboss.datavirtualization.client.dialogs.AddEditModelDialog.ModelType;
import org.jboss.datavirtualization.client.dialogs.ConfirmDialog;
import org.jboss.datavirtualization.client.dialogs.CreateVdbDialog;
import org.jboss.datavirtualization.client.dialogs.MessageDialog;
import org.jboss.datavirtualization.client.dialogs.ShowVdbXmlDialog;
import org.jboss.datavirtualization.client.events.FileUploadedEvent;
import org.jboss.datavirtualization.client.events.FileUploadedEventHandler;
import org.jboss.datavirtualization.client.events.SourcesChangedEvent;
import org.jboss.datavirtualization.client.events.SourcesChangedEventHandler;
import org.jboss.datavirtualization.client.events.VDBRedeployEvent;
import org.jboss.datavirtualization.client.events.VDBRedeployEventHandler;
import org.jboss.datavirtualization.client.rpc.TeiidMgrServiceAsync;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTMLTable.Cell;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

public class VDBsPanel extends Composite {

	private static VDBsPanelUiBinder uiBinder = GWT.create(VDBsPanelUiBinder.class);

	interface VDBsPanelUiBinder extends UiBinder<Widget, VDBsPanel> {
	}

	private final Messages messages = GWT.create(Messages.class);
	// 'No VDBs' ListBox entry
	private final String NO_VDBS = messages.noVDBsMsg();

	// EventBus and TeiidMgrService
	private final TeiidMgrServiceAsync teiidMgrService;
	private SimpleEventBus eventBus;

	// Dialogs
	private MessageDialog messageDialog = new MessageDialog();
	
	private CreateVdbDialog createVdbDialog = new CreateVdbDialog(this);
	private ShowVdbXmlDialog showVdbXmlDialog = new ShowVdbXmlDialog();
	
	private List<String> currentVDBNames = new ArrayList<String>();  // VDB names
	private Map<String,String> currentVDBTypes = new HashMap<String,String>();  // Vdb Type - "DYNAMIC" or "ARCHIVE"
	private DataSourcePanel dsPanel;
	private Map<String,String> vdbModelDDLMap = new HashMap<String,String>();
	private FlexTable vdbModelsTable = new FlexTable();

	// Within the owner class for the UiBinder template
	@UiField Resources res;

	@UiField Button newVDBButton;
	@UiField Button deleteVDBButton;
	@UiField Button showVDBXmlButton;
	@UiField Button deploySampleButton;
	@UiField Button addSourceModelButton;
	@UiField Button addViewModelButton;
	@UiField Button editModelButton;
	@UiField Button removeModelButton;
	@UiField Label vdbStatusTitleLabel;
	@UiField Label vdbStatusLabel;
    @UiField ListBox vdbSelectionListBox;
    @UiField ScrollPanel vdbModelsScrollPanel;
    @UiField Label dummyLabel;
	@UiField FileUploadPanel vdbUploadPanel;

	public VDBsPanel(SimpleEventBus eventBus,boolean isRunningOnOpenShift,TeiidMgrServiceAsync teiidMgrService,DataSourcePanel dsPanel, int panelHeight) {
		res.style().ensureInjected();
		
		initWidget(uiBinder.createAndBindUi(this));

		this.eventBus = eventBus;
		//this.isRunningOnOpenShift = isRunningOnOpenShift;
		this.teiidMgrService = teiidMgrService;
		this.dsPanel = dsPanel;
		
		initModelsTable();
        initVdbUploadPanel(messages.uploadVdbPanelTitle(),currentVDBNames);
 		initButtonStates();
		
	    // Listen for SourcesChangedEvent from Add Source Dialog, so we can update the sources table.
		eventBus.addHandler(SourcesChangedEvent.TYPE, new SourcesChangedEventHandler() {
			public void onEvent(SourcesChangedEvent event) {
				refreshVDBModelsTable(getSelectedVDBName());
			}
		});
		
	    // Listen for VDBRedeployEvent from Add Source Dialog, so we can update the UI
		eventBus.addHandler(VDBRedeployEvent.TYPE, new VDBRedeployEventHandler() {
			public void onEvent(VDBRedeployEvent event) {
				setUIStatusForVDBRedeploy();
			}
		});
		
	    // Listen for FileUploadedEvent from uploader.
		eventBus.addHandler(FileUploadedEvent.TYPE, new FileUploadedEventHandler() {
			public void onEvent(FileUploadedEvent event) {
				if(event.getUploadType().equals("VDB")) {
					refresh();
				}
			}
		});
		
		// Establish panel height
		dummyLabel.setHeight(String.valueOf(panelHeight)+"px");		
	}

	@UiHandler("vdbSelectionListBox")
	void onVdbSelectionListBoxChange(ChangeEvent event) {
		// Refresh the VDB Sources Table
		String vdbName = getSelectedVDBName();
		
		refreshVDBModelsTable(vdbName);
		
		// Set the VDB Mgmt Button enabled states
		setVDBMgmtButtonEnablements();
		
		dsPanel.refresh();
	}
	
	@UiHandler("newVDBButton")
	void onNewVDBButtonClick(ClickEvent e) {
		createVdbDialog.show(currentVDBNames,deleteVDBButton);
	}
	
	@UiHandler("deleteVDBButton")
	void onDeleteVDBButtonClick(ClickEvent event) {
		showDeleteVdbDialog(getSelectedVDBName());
	}
	
	@UiHandler("showVDBXmlButton")
	void onShowVDBXmlButtonClick(ClickEvent e) {
		showVdbXmlDialog.show(getSelectedVDBName(),deleteVDBButton);
	}
	
	@UiHandler("deploySampleButton")
	void onDeploySampleButtonClick(ClickEvent e) {
		showDeploySampleVdbDialog("SampleVDB");
	}

	@UiHandler("addSourceModelButton")
	void onAddSourceModelButtonClick(ClickEvent event) {
		// Init the Add Model Dialog
		AddEditModelDialog addModelDialog = new AddEditModelDialog(eventBus,teiidMgrService);
		// Current Models, so as not to duplicate
		List<String> currentModelNames = getVDBTableModelNames(false);
		// Show Add Model Dialog
		addModelDialog.initDialogForAdd(getSelectedVDBName(),AddEditModelDialog.ModelType.SOURCE, currentModelNames, currentVDBNames);
		addModelDialog.showDialog(newVDBButton);
	}
	
	@UiHandler("addViewModelButton")
	void onAddViewModelButtonClick(ClickEvent e) {
		// Init the Add Model Dialog
		AddEditModelDialog addModelDialog = new AddEditModelDialog(eventBus,teiidMgrService);
		// Current Models, so as not to duplicate
		List<String> currentModelNames = getVDBTableModelNames(false);
		// Show Add Model Dialog
		addModelDialog.initDialogForAdd(getSelectedVDBName(),AddEditModelDialog.ModelType.VIEW, currentModelNames, currentVDBNames);
		addModelDialog.showDialog(newVDBButton);
	}
	
	@UiHandler("editModelButton")
	void onEditModelButtonClick(ClickEvent event) {
		// Init the Add Model Dialog
		AddEditModelDialog addModelDialog = new AddEditModelDialog(eventBus,teiidMgrService);
		
		String modelName = getModelNameForFirstSelectedRow();
		ModelType modelType = getModelTypeForFirstSelectedRow();
		String ddl = getViewModelDdl(modelName);

		// Show Add Model Dialog
		addModelDialog.initDialogForEdit(getSelectedVDBName(),modelType, modelName, ddl);
		addModelDialog.showDialog(newVDBButton);
	}
	
	@UiHandler("removeModelButton")
	void onRemoveModelButtonClick(ClickEvent e) {
		showRemoveModelsDialog();
	}

	/*
	 * Refresh the panel
	 */
	public void refresh( ) {
		// Get current VDB so we can re-select it
		final String selectedVdb = getSelectedVDBName();
		
		// Set up the callback object.
		AsyncCallback<List<DataItem>> callback = new AsyncCallback<List<DataItem>>() {
			// On Failure - show Error Dialog
			public void onFailure(Throwable caught) {
				showErrorDialog(messages.refreshModelsTableErrorTitle(), messages.refreshModelsTableErrorMsg()+caught.getMessage());
			}

			// On Success - Populate the ListBox
			public void onSuccess(List<DataItem> result) {
				populateVDBListBox(result,selectedVdb);
			}
		};
		
		// Updates with current dynamic vdbs
		teiidMgrService.getVDBItems(callback);
	}
	
	private void initModelsTable( ) {
		vdbModelsTable.addStyleName(res.style().vdbModelsTable());
		// ClickHandler - Use Row selection to toggle the 'selected' state
		vdbModelsTable.addClickHandler(new ClickHandler() {
		    public void onClick(ClickEvent event) {
		        Cell cell = vdbModelsTable.getCellForEvent(event);
		        int rowIndex = cell.getRowIndex();
		        int colIndex = cell.getCellIndex();
		        if(rowIndex>0 && colIndex>0) {
		        	// Toggle the Selected checkbox
		        	Widget widget = vdbModelsTable.getWidget(rowIndex, 0);
		        	if(widget!=null && widget instanceof CheckBox) {
		                CheckBox checkBox = (CheckBox) widget;
		                checkBox.setValue(!checkBox.getValue());
		                setModelMgmtButtonEnablements();
		            }
		        }
		    }
		});
		
		vdbModelsScrollPanel.setWidget(vdbModelsTable);
		vdbModelsScrollPanel.setHeight("300px");
	}
	
	private void initButtonStates() {
		newVDBButton.setEnabled(true);
		deleteVDBButton.setEnabled(true);
		showVDBXmlButton.setEnabled(true);
		addSourceModelButton.setEnabled(true);
		addViewModelButton.setEnabled(true);
		editModelButton.setEnabled(false);
		removeModelButton.setEnabled(false);
	}
	
	/*
	 * Get the DDL for the supplied ViewModel
	 * @param modelName the Model Name
	 * @return the ViewModel DDL
	 */
	private String getViewModelDdl(String viewModelName) {
		return vdbModelDDLMap.get(viewModelName);
	}
	
	public void removeCheckedModels() {
		String vdbName = getSelectedVDBName();
		List<String> checkedModels = new ArrayList<String>();
		List<ModelType> checkedModelTypes = new ArrayList<ModelType>();
		for (int i = 1, n = vdbModelsTable.getRowCount(); i < n; i++) {
			CheckBox box = (CheckBox) vdbModelsTable.getWidget(i, 0);
			String modelName = vdbModelsTable.getText(i, 1);
			if(box.getValue()) {
				ModelType modelType = getModelTypeForVdbTable(modelName);
				String translator = getTranslatorForVdbTable(modelName);
				if(modelType==ModelType.SOURCE) {
					String importVdbName = "VDBMgr-"+modelName+"-"+translator;
					checkedModels.add(importVdbName);
				} else {
					checkedModels.add(modelName);
				}
				checkedModelTypes.add(modelType);
			}
		}
		removeModels(vdbName,checkedModels,checkedModelTypes);
	}

	/*
	 * Remove the specified models from the VDB
	 * @param vdbName the name of the VDB
	 * @param modelNames the list of model names
	 */
	private void removeModels(final String vdbName, List<String> modelNames, List<ModelType> modelTypes) {
		// Set up the callback object.
		AsyncCallback<List<List<DataItem>>> callback = new AsyncCallback<List<List<DataItem>>>() {
			// On Failure - show Error Dialog
			public void onFailure(Throwable caught) {
				showErrorDialog(messages.removeModelsErrorTitle(), messages.removeModelsErrorMsg()+caught.getMessage());
				setModelMgmtButtonEnablements();
			}

			// On Success - Populate the ListBox
			public void onSuccess(List<List<DataItem>> result) {
				populateVdbModelsTable(vdbName, result);
				setModelMgmtButtonEnablements();
			}
		};
		
		teiidMgrService.removeModelsAndRedeploy(vdbName, modelNames, modelTypes, callback);		
	}
	
	/*
	 * Populates the available VDBs ListBox
	 * @vdbList the list of VDB Items to re-populate the ListBox
	 * @vdbToSelect the name of the VDB to select initially
	 */
	public void populateVDBListBox(List<DataItem> vdbItemList, String vdbToSelect) {
		// The DataItem includes the name and type of VDB
		List<String> vdbNames = getVdbNameList(vdbItemList);
		currentVDBTypes = getVdbTypeMap(vdbItemList);
		
		// Refresh list of current VDB names
		currentVDBNames.clear();
		currentVDBNames.addAll(vdbNames);
		
		// If nothing in the list, add a placeholder NO_VDBS
		if(vdbNames.isEmpty()) {
			vdbNames.add(NO_VDBS);
		}
		
		// Clear current list
		vdbSelectionListBox.clear();
		
		int selectionIndx = 0;
		
		// Repopulate the list box
		int i = 0;
		for(String vdbName: vdbNames) {
			vdbSelectionListBox.insertItem(vdbName, i);
			if(vdbName!=null && vdbName.equalsIgnoreCase(vdbToSelect)) selectionIndx = i;
			i++;
		}
		vdbSelectionListBox.setSelectedIndex(selectionIndx);
		DomEvent.fireNativeEvent(Document.get().createChangeEvent(),vdbSelectionListBox);				
	}
	
	private List<String> getVdbNameList(List<DataItem> vdbItems) {
		List<String> vdbNames = new ArrayList<String>(vdbItems.size());
		for(DataItem vdbItem : vdbItems) {
			vdbNames.add(vdbItem.getData());
		}
		Collections.sort(vdbNames);
		return vdbNames;
	}

	private Map<String,String> getVdbTypeMap(List<DataItem> vdbItems) {
		Map<String,String> vdbTypeMap = new HashMap<String,String>(vdbItems.size());
		for(DataItem vdbItem : vdbItems) {
			vdbTypeMap.put(vdbItem.getData(),vdbItem.getType());
		}
		return vdbTypeMap;
	}

	/*
	 * Clear the VDB Models Table, then re-populate it using the supplied rows
	 * @param vdbName the VDB name
	 * @param rowData the List of Row info for re-populating the VDB Models table
	 */
	private void populateVdbModelsTable(String vdbName, List<List<DataItem>> rowData) {
		// Clear Previous Results
		clearVdbModelsTable();
		vdbModelDDLMap.clear();

		// Check for NO_VDBS
		if(NO_VDBS.equalsIgnoreCase(vdbName)) {
			setVDBStatusLine(messages.vdbPanelLabelNoVDBs(),"");
		}
		
		// First Row is VDB Status.  Use it in the Panel Title
		if(rowData.size()>0) {
			List<DataItem> vdbStatusRow = rowData.get(0);
			DataItem data = (DataItem)vdbStatusRow.get(0);
			String vdbStatusTitle = createStatusTitle(vdbName);
			String vdbStatus = data.getData();
			// If no models in the VDB, make sure INACTIVE status
			if(rowData.size()==2) {
				vdbStatus = "INACTIVE";
			}
			setVDBStatusLine(vdbStatusTitle,vdbStatus);
		} else {
			vdbModelsTable.setText(0,0,"");
			vdbModelsTable.setText(0,1,messages.vdbTableHeader_ModelName());
			vdbModelsTable.setText(0,2,messages.vdbTableHeader_ModelType());
			vdbModelsTable.setText(0,3,messages.vdbTableHeader_Translator());
			vdbModelsTable.setText(0,4,messages.vdbTableHeader_JNDISrc());
			vdbModelsTable.setText(0,5,messages.vdbTableHeader_Status());
			vdbModelsTable.getRowFormatter().addStyleName(0, res.style().vdbModelsTableHeader());
		}
		
		int iRow = 0;
		for(List<DataItem> row: rowData) {
		    // Data Row 0 is VDB Status - skip
			if(iRow==0) {
				iRow++;
				continue;
			}
			
			// Data Row 1 is header Row
			int nCols = row.size()-1;  // Don't show last column (DDL)
			if(iRow==1) {
				vdbModelsTable.setText(iRow-1,0,"");
				vdbModelsTable.getCellFormatter().addStyleName(iRow-1, 0, res.style().vdbModelsTableCell());
			} else {
				// First Column is a remove checkbox
				CheckBox rowCheckbox = new CheckBox("");
				rowCheckbox.addClickHandler(new ClickHandler() {
					public void onClick(ClickEvent event) {
						setModelMgmtButtonEnablements();
					}
				});
				vdbModelsTable.setWidget(iRow-1, 0, rowCheckbox);
			}
			
			// Columns 2+ contain the returned data
			for(int i=0; i<nCols; i++) {
				DataItem data = (DataItem)row.get(i);
				vdbModelsTable.setText(iRow-1,i+1,data.getData());
				vdbModelsTable.getCellFormatter().addStyleName(iRow-1, i+1, res.style().vdbModelsTableCell());
			}
			
			// DDL (last column) is not shown in table, but saved in Map for later use
			String modelName = ((DataItem)row.get(0)).getData();
			String ddl = ((DataItem)row.get(nCols)).getData();
			vdbModelDDLMap.put(modelName,ddl);
			
			// Header Row Style
			if(iRow==1) {
				vdbModelsTable.getRowFormatter().addStyleName(iRow-1, res.style().vdbModelsTableHeader());
		    // Even Row Style
			} else {
				boolean isEven = (iRow % 2 == 0);
				if(isEven) {
					vdbModelsTable.getRowFormatter().addStyleName(iRow-1, res.style().vdbModelsTableEvenRow());
				} else {
					vdbModelsTable.getRowFormatter().addStyleName(iRow-1, res.style().vdbModelsTableOddRow());
				}
			}
			iRow++;
		}
		
	}
	
	private String createStatusTitle(String vdbName) {
		StringBuffer sb = new StringBuffer();
		sb.append("'"+vdbName+"' ");
		String vdbType = currentVDBTypes.get(vdbName);
		if(vdbType.equals("DYNAMIC")) {
			sb.append("(Dynamic VDB) ---- Status: ");
		} else {
			sb.append("(*.vdb - edit not allowed) ---- Status: ");
		}
		return sb.toString();
	}
	
	private void setVDBStatusLine(String vdbStatusTitle, String vdbStatus) {
		String statusTitle = vdbStatusTitle!=null ? vdbStatusTitle : "";
		String status = vdbStatus!=null ? vdbStatus : "";
		
		// Set the status text
	    vdbStatusTitleLabel.setText(statusTitle);
	    vdbStatusLabel.setText(status);
	    
	    // Set the status lable style based on text
		if(vdbStatus!=null) {
			// Active is Green
			if(vdbStatus.trim().equalsIgnoreCase("active")) {
				vdbStatusLabel.removeStyleName(res.style().vdbStatusInactive());
				vdbStatusLabel.addStyleName(res.style().vdbStatusActive());
			// Anything not Active is Red
			} else {
				vdbStatusLabel.removeStyleName(res.style().vdbStatusActive());
				vdbStatusLabel.addStyleName(res.style().vdbStatusInactive());
			}
		}
	}
	
	/*
	 * Get the selected VDB from the VDB ListBox
	 * @return the currently selected VDB name
	 */
	private String getSelectedVDBName() {
		int selectedIndex = this.vdbSelectionListBox.getSelectedIndex();
		return this.vdbSelectionListBox.getValue(selectedIndex);
	}
	
	/*
	 * Return the list of ModelNames from the VDB Models table.
	 * @param onlySelected if 'true', only the selected model names are returned.  if 'false', all model names are returned.
	 * @return the list of VDB Model Names
	 */
	private List<String> getVDBTableModelNames(boolean onlySelected) {
		List<String> modelNames = new ArrayList<String>();
        for (int i = 1, n = vdbModelsTable.getRowCount(); i < n; i++) {
            CheckBox box = (CheckBox) vdbModelsTable.getWidget(i, 0);
            String modelName = vdbModelsTable.getText(i, 1);
            if(!onlySelected) {
            	modelNames.add(modelName);
            } else if(box.getValue()) {
            	modelNames.add(modelName);
            }
        }
        return modelNames;
	}
	
	/*
	 * Get the VDB Model Name for the first selected row in the VDB Models Table.
	 * @return the Model Name, null if nothing is selected
	 */
	private String getModelNameForFirstSelectedRow() {
		String modelName = null;
        for (int i = 1, n = vdbModelsTable.getRowCount(); i < n; i++) {
            CheckBox box = (CheckBox) vdbModelsTable.getWidget(i, 0);
            if(box.getValue()) {
            	modelName = vdbModelsTable.getText(i,1);
            	break;
            }
        }
        return modelName;
	}

	/*
	 * Get the VDB Model Type for the first selected row in the VDB Models Table.
	 * @return the ModelType, null if nothing is selected
	 */
	private ModelType getModelTypeForFirstSelectedRow() {
		String modelType = null;
        for (int i = 1, n = vdbModelsTable.getRowCount(); i < n; i++) {
            CheckBox box = (CheckBox) vdbModelsTable.getWidget(i, 0);
            if(box.getValue()) {
            	modelType = vdbModelsTable.getText(i,2);
            	break;
            }
        }
        if(modelType!=null) {
        	if(modelType.equals("PHYSICAL")) {
        		return ModelType.SOURCE;
        	} else if(modelType.equals("VIRTUAL")) {
        		return ModelType.VIEW;
        	}
        }
        return null;
	}
	
	/*
	 * Get the VDB Model Type for the VdbModelsTable entry with the specified name
	 * @param modelName the name of the VDBModelTable entry
	 * @return the ModelType, null if nothing is selected
	 */
	private ModelType getModelTypeForVdbTable(String modelName) {
		String modelType = null;
        for (int i = 1, n = vdbModelsTable.getRowCount(); i < n; i++) {
        	String tableModelName = vdbModelsTable.getText(i,1);
        	if(tableModelName!=null && tableModelName.equalsIgnoreCase(modelName)) {
            	modelType = vdbModelsTable.getText(i,2);
            	break;
        	}
        }
        if(modelType!=null) {
        	if(modelType.equals("PHYSICAL")) {
        		return ModelType.SOURCE;
        	} else if(modelType.equals("VIRTUAL")) {
        		return ModelType.VIEW;
        	}
        }
        return ModelType.SOURCE;
	}
	
	/*
	 * Get the VDB Model Type for the VdbModelsTable entry with the specified name
	 * @param modelName the name of the VDBModelTable entry
	 * @return the ModelType, null if nothing is selected
	 */
	private String getTranslatorForVdbTable(String modelName) {
		String translator = null;
        for (int i = 1, n = vdbModelsTable.getRowCount(); i < n; i++) {
        	String tableModelName = vdbModelsTable.getText(i,1);
        	if(tableModelName!=null && tableModelName.equalsIgnoreCase(modelName)) {
            	translator = vdbModelsTable.getText(i,3);
            	break;
        	}
        }
        return translator;
	}
	/*
	 * Set the VDB Mgmt Button Enablement States.  This includes the NewVDB, DeleteVDB and
	 * RefreshVDB buttons
	 */
	public void setVDBMgmtButtonEnablements() {
		String selectedVDB = getSelectedVDBName();
		boolean isDynamicVDB = this.currentVDBTypes.get(selectedVDB).equals("DYNAMIC") ? true : false;
		boolean noVDBs = (selectedVDB==null || selectedVDB.equalsIgnoreCase(NO_VDBS)) ? true : false;
		
		// New VDB and Deploy Sample Buttons always enabled
		newVDBButton.setEnabled(true);
		deploySampleButton.setEnabled(true);
		
		// Delete disabled if NO_VDBS
		deleteVDBButton.setEnabled(true);
		if(noVDBs || selectedVDB.equalsIgnoreCase("ModeShape")) {
			deleteVDBButton.setEnabled(false);
		}
		
		// Show XML disabled if noVDBs or not a dynamic VDB
		showVDBXmlButton.setEnabled(true);
		if(noVDBs || !isDynamicVDB) {
			showVDBXmlButton.setEnabled(false);
		} 
		
		setModelMgmtButtonEnablements();
	}
	
	/*
	 * Set the Model Mgmt Button Enablement States.  This includes the AddSourceModel, AddViewModel, EditModel, RemoveModel 
	 * Buttons.
	 */
	public void setModelMgmtButtonEnablements() {
		String selectedVDB = getSelectedVDBName();
		boolean isDynamicVDB = this.currentVDBTypes.get(selectedVDB).equals("DYNAMIC") ? true : false;
		boolean noVDBs = (selectedVDB==null || selectedVDB.equalsIgnoreCase(NO_VDBS)) ? true : false;

		// Adds Disabled if NoVDBs or Not Dynamic VDB
		addSourceModelButton.setEnabled(true);
		addViewModelButton.setEnabled(true);
		if(noVDBs || !isDynamicVDB) {
			addSourceModelButton.setEnabled(false);
			addViewModelButton.setEnabled(false);
		}
		
		// Get the list of selected table rows
		List<String> selectedModels = getVDBTableModelNames(true);

		// RemoveSourceButton - If anything is checked, the button is enabled
		if(isDynamicVDB && selectedModels.size()>0) {
			removeModelButton.setEnabled(true);
		} else {
			removeModelButton.setEnabled(false);
		}
		
		// RedeploySourceButton - If one row is checked, the button is enabled
		if(isDynamicVDB && selectedModels.size()==1) {
			ModelType type = getModelTypeForFirstSelectedRow();
			if(type==ModelType.VIEW) {
				editModelButton.setEnabled(true);
			} else {
				editModelButton.setEnabled(false);
			}
		} else {
			editModelButton.setEnabled(false);
		}
		
	}
	
	/*
	 * Changes UI status for actions that cause a VDB re-deploy.  Re-deploys
	 * may take a while while metadata is reloading...
	 */
	private void setUIStatusForVDBRedeploy() {
		vdbStatusLabel.removeStyleName(res.style().vdbStatusActive());
		vdbStatusLabel.addStyleName(res.style().vdbStatusInactive());
		vdbStatusLabel.setText(messages.statusVDBReloading());
		clearVdbModelsTable();
	}
	
	/*
	 * Show confirmation dialog for Deleting a VDB 
	 */
	private void showDeleteVdbDialog(final String vdbName) {
		final ConfirmDialog confirmDialog = new ConfirmDialog();

		// Handler for OK Button
		ClickHandler okHandler = new ClickHandler() {
			public void onClick(ClickEvent event) {
				if(vdbName!=null && !vdbName.equalsIgnoreCase(NO_VDBS)) {
					deleteVDB(vdbName);
				}
				confirmDialog.hide();
			}
		};
		
		confirmDialog.showConfirm(messages.dialogDeleteVDBTitle(),"<b> Click 'OK' to delete VDB: '"+vdbName+"'</b>",okHandler,deleteVDBButton);
	}
	
	/*
	 * Show confirmation dialog for removing models from the vdb 
	 */
	private void showRemoveModelsDialog() {
		final ConfirmDialog confirmDialog = new ConfirmDialog();

		// Handler for OK Button
		ClickHandler okHandler = new ClickHandler() {
			public void onClick(ClickEvent event) {
	            removeCheckedModels();

				confirmDialog.hide();
			}
		};
		
		confirmDialog.showConfirm(messages.dialogRemoveModelTitle(),"<b>"+messages.dialogRemoveModelMsg()+"</b>",okHandler,removeModelButton);
	}
	
	/* 
	 * Delete VDB.
	 * @param vdbName the name of the VDB to delete.
	 */
	private void deleteVDB(final String vdbName) {
		// Set up the callback object.
		AsyncCallback<List<DataItem>> callback = new AsyncCallback<List<DataItem>>() {
			// On Failure - show Error Dialog
			public void onFailure(Throwable caught) {
				messageDialog.showError(messages.deleteVDBErrorTitle(), messages.deleteVDBErrorMsg()+caught.getMessage());
				setModelMgmtButtonEnablements();
			}

			// On Success - no action.  List Box updates after call.
			public void onSuccess(List<DataItem> vdbItems) {
				populateVDBListBox(vdbItems,null);
			}
		};

		// Delete the VDB
		teiidMgrService.deleteVDB(vdbName, callback);
	}
	
	/*
	 * Refresh the VDB Models table
	 * @param vdbName the name of the VDB to refresh
	 */
	private void refreshVDBModelsTable(final String vdbName) {
		// Set up the callback object.
		AsyncCallback<List<List<DataItem>>> callback = new AsyncCallback<List<List<DataItem>>>() {
			// On Failure - show Error Dialog
			public void onFailure(Throwable caught) {
				showErrorDialog(messages.refreshModelsTableErrorTitle(), messages.refreshModelsTableErrorMsg()+caught.getMessage());
				setModelMgmtButtonEnablements();
			}

			// On Success - Populate the ListBox
			public void onSuccess(List<List<DataItem>> result) {
				populateVdbModelsTable(vdbName,result);
				setModelMgmtButtonEnablements();
			}
		};

		if(vdbName!=null && vdbName.equalsIgnoreCase(NO_VDBS)) {
			populateVdbModelsTable(vdbName,new ArrayList<List<DataItem>>());
			setModelMgmtButtonEnablements();
		} else {
			teiidMgrService.getVDBModelInfo(vdbName, callback);	
		}
	}
	
	/*
	 * Show confirm dialog for deploy sample VDB
	 */
	private void showDeploySampleVdbDialog(final String vdbName) {
		String createMessages = getSampleVDBMessages(vdbName);

		final ConfirmDialog confirmDialog = new ConfirmDialog();

		// Handler for OK Button
		ClickHandler okHandler = new ClickHandler() {
			public void onClick(ClickEvent event) {
				deploySampleVDB(vdbName);

				confirmDialog.hide();
			}
		};

		confirmDialog.showConfirm(messages.dialogCreateSampleVDBTitle(),createMessages,okHandler,showVDBXmlButton);
	}
	
	private String getSampleVDBMessages(String vdbName) {
		StringBuffer sb = new StringBuffer();
		
		sb.append("A Sample VDB will be deployed which includes the following sources, ");
		sb.append("<br>including a View definition for each source: ");
		sb.append("<br><b>- SampleRemoteFlatFile</b>");
		sb.append("<br><b>- SampleRemoteXmlFile</b>");
		sb.append("<br><b>- SampleSalesforce</b>");
		sb.append("<br><br>If you have not yet created these sources, please do so now ");
		sb.append("<br>using the 'Deploy Samples' button on the Data Sources Tab.");
		
		// Warn about source connectivity issues
		sb.append("<br><br><b>NOTE:</b> If connectivity errors are encountered with ");
		sb.append("<br>any of the Sample DataSource(s), the problem source(s) ");
		sb.append("<br>will be omitted.  You may resolve the issues and then ");
		sb.append("<br>add the sources manually, or re-deploy the entire 'SampleVDB'");

		// Check if 'SampleVDB' already exists and will be overwritten
		if(currentVDBNames.contains(vdbName)) {
			sb.append("<br><br><b>A VDB named 'SampleVDB' already exists, and will be overwritten</b>");
		}
		sb.append("<br><br><b>Deploy 'SampleVDB'?</b>");
		
		return sb.toString();
	}
	
	/*
	 * Create a Sample VDB
	 * @param vdbName the name of the VDB to create
	 */
	private void deploySampleVDB(final String vdbName) {
		// Set up the callback object.
		AsyncCallback<List<DataItem>> callback = new AsyncCallback<List<DataItem>>() {
			// On Failure - show Error Dialog
			public void onFailure(Throwable caught) {
				messageDialog.showError(messages.createVDBErrorTitle(), messages.createVDBErrorMsg()+caught.getMessage());
				setModelMgmtButtonEnablements();
			}

			// On Success - no action.  List Box updates after call.
			public void onSuccess(List<DataItem> vdbItems) {
				populateVDBListBox(vdbItems,vdbName);
			}
		};

		setVDBStatusLine("Deploying Sample VDB", "Please Wait...");
		// Creates the VDB
		teiidMgrService.createSampleVDB(vdbName, callback);
	}
	
	/*
	 * Clear the VDB Models Table 
	 */
	private void clearVdbModelsTable( ) {
		vdbModelsTable.removeAllRows();
	}

	/*
	 * Show the Dialog for Error Display
	 */
	private void showErrorDialog(String title, String msg) {
		messageDialog.showError(title,msg);
	}

	// ============================================================================
	// VDB Upload Widgets
	// ============================================================================
	
	/*
	 * Init VDB Upload panel
	 */
	private void initVdbUploadPanel(String vdbUploadTitle, List<String> currentUploadNames) {
		vdbUploadPanel.init(this.eventBus,vdbUploadTitle,currentUploadNames,"VDB");
	}

}
