package org.jboss.datavirtualization.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.datavirtualization.client.dialogs.ConfirmDialog;
import org.jboss.datavirtualization.client.dialogs.MessageDialog;
import org.jboss.datavirtualization.client.events.FileUploadedEvent;
import org.jboss.datavirtualization.client.events.FileUploadedEventHandler;
import org.jboss.datavirtualization.client.events.SourcesChangedEvent;
import org.jboss.datavirtualization.client.events.SourcesChangedEventHandler;
import org.jboss.datavirtualization.client.rpc.TeiidMgrServiceAsync;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
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
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

public class DriverPanel extends Composite {

	private static DriverPanelUiBinder uiBinder = GWT.create(DriverPanelUiBinder.class);

	interface DriverPanelUiBinder extends UiBinder<Widget, DriverPanel> {
	}
	
	private final Messages messages = GWT.create(Messages.class);
	private final TeiidMgrServiceAsync teiidMgrService;
	// EventBus and TeiidMgrService
	private SimpleEventBus eventBus;

	// Message Dialog
	private MessageDialog messageDialog = new MessageDialog();

	// Data Sources Table and Table Header
	private FlexTable driversTable = new FlexTable();
	private List<String> currentDriverNames = new ArrayList<String>();

	private List<String> builtInDrivers = Arrays.asList("file","google","h2","infinispan","ldap","modeshape","mongodb","salesforce","teiid","teiid-local","webservice");
	
	// Within the owner class for the UiBinder template
	@UiField Resources res;

	@UiField Button deleteDriverButton;
    @UiField ScrollPanel driversTableScrollPanel;
	@UiField FileUploadPanel driverUploadPanel;

	public DriverPanel(SimpleEventBus eventBus, TeiidMgrServiceAsync teiidMgrService) {
		res.style().ensureInjected();
		
		initWidget(uiBinder.createAndBindUi(this));

		this.eventBus = eventBus;
		this.teiidMgrService = teiidMgrService;
		
		initComponents();
		
	    // Listen for SourcesChangedEvent from Add Source Dialog, so we can update the sources table.
		this.eventBus.addHandler(SourcesChangedEvent.TYPE, new SourcesChangedEventHandler() {
			public void onEvent(SourcesChangedEvent event) {
				refreshTable();
			}
		});
		
	    // Listen for FileUploadedEvent from uploader.
		eventBus.addHandler(FileUploadedEvent.TYPE, new FileUploadedEventHandler() {
			public void onEvent(FileUploadedEvent event) {
				if(event.getUploadType().equals("DRIVER")) {
					refreshTable();
				}
			}
		});
		
	}

	@UiHandler("deleteDriverButton")
	void onDeleteDriverButtonClick(ClickEvent event) {
		showDeleteDriverDialog();
	}
	
	/*
	 * Init Components
	 */
	private void initComponents() {
		// --------------------
		// Drivers Table
		// --------------------
		driversTable.addStyleName(res.style().driversTable());
		// ClickHandler - Use Row selection to toggle the 'selected' state
		driversTable.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				Cell cell = driversTable.getCellForEvent(event);
				int rowIndex = cell.getRowIndex();
				int colIndex = cell.getCellIndex();
				if(rowIndex>0 && colIndex>0) {
					// Toggle the Selected checkbox
					Widget widget = driversTable.getWidget(rowIndex, 0);
					if(widget!=null && widget instanceof CheckBox) {
						CheckBox checkBox = (CheckBox) widget;
						checkBox.setValue(!checkBox.getValue());
						validateDriverMgmtControls();
					}
				}
			}
		});
		
		driversTableScrollPanel.setWidget(driversTable);
        driversTableScrollPanel.setHeight("350px");

        // ----------------------------
		// Inits the Upload Panel
		// ----------------------------
        
        initDriverUploadPanel(messages.uploadDriverPanelTitle(),currentDriverNames);
        
	}
	
	/**
	 * Refresh the panel
	 */
	public void refresh() {
		refreshTable();
	}
	
    /*
	 * Refresh the Drivers table
	 */
	public void refreshTable( ) {
		// Set up the callback object.
		AsyncCallback<List<String>> callback = new AsyncCallback<List<String>>() {
			// On Failure - show Error Dialog
			public void onFailure(Throwable caught) {
				showErrorDialog(messages.refreshDriverTableErrorTitle(), messages.refreshDriverTableErrorMsg()+caught.getMessage());
				validateDriverMgmtControls();
			}

			// On Success - Populate the ListBox
			public void onSuccess(List<String> result) {
				populateDriversTable(result);
				validateDriverMgmtControls();
			}
		};

		teiidMgrService.getDataSourceTemplates(callback);	
	}
	
	/*
	 * Return the list of all Driver names from the table.
	 * @param onlySelected if 'true', only the selected drivers are returned.  if 'false', all drivers are returned.
	 * @return the list of Driver Names
	 */
	private List<String> getDriverTableNames(boolean onlySelected) {
		List<String> driverNames = new ArrayList<String>();
        for (int i = 1, n = driversTable.getRowCount(); i < n; i++) {
            String driverName = driversTable.getText(i, 1);
            if(!onlySelected) {
            	driverNames.add(driverName);
            } else {
            	if(!isBuiltInDriver(driverName)) {
            		CheckBox box = (CheckBox) driversTable.getWidget(i, 0);
            		if(box.getValue()) {
            			driverNames.add(driverName);
            		}
            	}
            }
        }
        return driverNames;
	}
	
	/*
	 * Clear the Drivers Table, then re-populate it using the supplied driver name list
	 * @param driverNames the List of driverName info for re-populating the Drivers table
	 */
	private void populateDriversTable(List<String> driverNames) {
		// Clear Previous Results
		clearDriversTable();
		currentDriverNames.clear();
		
		// Set Header Info for Drivers Table
		driversTable.setText(0,0,"");
		driversTable.setText(0,1,"Driver");
		driversTable.getCellFormatter().addStyleName(0, 0, res.style().driversTableCell());
		driversTable.getCellFormatter().addStyleName(0, 1, res.style().driversTableCell());
		driversTable.getRowFormatter().addStyleName(0, res.style().driversTableHeader());
		
		int iRow = 1;
		for(String driverName: driverNames) {

			// First Column is a remove checkbox (only for non-built in drivers)
			if(!isBuiltInDriver(driverName)) {
				CheckBox rowCheckbox = new CheckBox("");
				rowCheckbox.addClickHandler(new ClickHandler() {
					public void onClick(ClickEvent event) {
						validateDriverMgmtControls();
					}
				});
				driversTable.setWidget(iRow, 0, rowCheckbox);
			} else {
				driversTable.setText(iRow, 0, "  ");
			}

			// Second Column is Driver Name
			driversTable.setText(iRow,1,driverName);
			driversTable.getCellFormatter().addStyleName(iRow, 1, res.style().driversTableCell());
			// Add driver name to list
			currentDriverNames.add(driverName);

			// Row Styles
			boolean isEven = (iRow % 2 == 0);
			if(isEven) {
				driversTable.getRowFormatter().addStyleName(iRow, res.style().driversTableEvenRow());
			} else {
				driversTable.getRowFormatter().addStyleName(iRow, res.style().driversTableOddRow());
			}
			iRow++;
		}
		
	}
	
	/*
	 * Determine if supplied drivername is a built-in
	 */
	private boolean isBuiltInDriver(String driverName) {
		if(builtInDrivers.contains(driverName)) {
			return true;
		}
		return false;
	}
	
	/*
	 * Clear the Drivers Table 
	 */
	private void clearDriversTable( ) {
		driversTable.removeAllRows();
	}
	
	/*
	 * Init Driver Upload panel
	 */
	private void initDriverUploadPanel(String driverUploadTitle, List<String> currentDriverNames) {
		driverUploadPanel.init(this.eventBus,driverUploadTitle,currentDriverNames,"DRIVER");
	}
	
	private void validateDriverMgmtControls() {

		// ------------------------------------
		// Delete Button enablement
		// ------------------------------------
		// Get the list of selected table rows
		List<String> selectedDrivers = getDriverTableNames(true);

		// DeleteSourceButton - If anything is checked, the button is enabled
		if(selectedDrivers.size()>0) {
			deleteDriverButton.setEnabled(true);
		} else {
			deleteDriverButton.setEnabled(false);
		}		

	}
		
	/*
	 * Show the Confirm Dialog for Deleting Source(s)
	 */
	private void showDeleteDriverDialog( ) {
		final ConfirmDialog confirmDialog = new ConfirmDialog();

		// Handler for OK Button
		ClickHandler okHandler = new ClickHandler() {
			public void onClick(ClickEvent event) {
				List<String> checkedDrivers = new ArrayList<String>();
	            for (int i = 1, n = driversTable.getRowCount(); i < n; i++) {
                    String driverName = driversTable.getText(i, 1);
	            	if(!isBuiltInDriver(driverName)) {
		                CheckBox box = (CheckBox) driversTable.getWidget(i, 0);
	                    if(box.getValue()) {
	                    	checkedDrivers.add(driverName);
	                    }
	            	}
	            }
				deleteSelectedDrivers(checkedDrivers);

				confirmDialog.hide();
			}
		};
		
		confirmDialog.showConfirm(messages.dialogDeleteDriverTitle(),"<b>"+messages.dialogDeleteDriverMsg()+"</b>",okHandler,deleteDriverButton);
	}
	
	/*
	 * Deletes the List of drivers
	 * @param deleteDrivers the list of drivers to delete
	 */
	private void deleteSelectedDrivers(List<String> deleteDrivers) {
		// Set up the callback object.
		AsyncCallback<List<String>> callback = new AsyncCallback<List<String>>() {
			// On Failure - show Error Dialog
			public void onFailure(Throwable caught) {
				showErrorDialog(messages.removeDriverErrorTitle(), messages.removeDriverErrorMsg()+caught.getMessage());
				validateDriverMgmtControls();
			}

			// On Success - Populate the ListBox
			public void onSuccess(List<String> result) {
				populateDriversTable(result);
				validateDriverMgmtControls();
			}
		};

		// service call to delete sources
		teiidMgrService.deleteDrivers(deleteDrivers, callback);		
	}

	/*
	 * Show the Dialog for Error Display
	 */
	private void showErrorDialog(String title, String msg) {
		messageDialog.showError(title,msg);
	}	

}
