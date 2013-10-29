package org.jboss.datavirtualization.client.dialogs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.datavirtualization.client.DataSourceHelper;
import org.jboss.datavirtualization.client.Messages;
import org.jboss.datavirtualization.client.PropertyItem;
import org.jboss.datavirtualization.client.PropertyTable;
import org.jboss.datavirtualization.client.Resources;
import org.jboss.datavirtualization.client.events.PropertyChangedEvent;
import org.jboss.datavirtualization.client.events.PropertyChangedEventHandler;
import org.jboss.datavirtualization.client.events.SourcesChangedEvent;
import org.jboss.datavirtualization.client.rpc.TeiidMgrServiceAsync;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/*
 * This class encapsulates the Create/Edit Source Dialog
 */
public class CreateEditDataSourceDialog {

	private final Messages messages = GWT.create(Messages.class);
	
	// Source Type and Name Controls
	private Label sourceNameLabel = new Label(messages.nameLabel());
	private TextBox sourceNameTextBox = new TextBox();

	private Label driverLabel = new Label(messages.driverLabel());
	private ListBox driverListBox = new ListBox();
	private Label driverEditLabel = new Label();

	private Label sourceNameRedeployLabel = new Label();
	private Label sourcePropertyStatusLabel = new Label();

	// Dialog Controls
	private DialogBox addSourceDialogBox = new DialogBox();
	private Button addSourceDialogOKButton = new Button(messages.okButton());
	private Button addSourceDialogCloseButton = new Button(messages.cancelButton());

	// Message Dialog
	private MessageDialog messageDialog = new MessageDialog();

	// Source Properties Table
	private PropertyTable sourcePropsTable;
	
	// Flag for Add vs Edit state
	private boolean addingSource = true;
	private List<String> currentSourceNames = new ArrayList<String>();
	
	// EventBus and TeiidMgrService
	private SimpleEventBus eventBus;
	private final TeiidMgrServiceAsync teiidMgrService;
	
	// Flag allows different handling when running on OpenShift
	private boolean isRunningOnOpenShift = false;

	/*
	 * Constructor for the Dialog
	 */
	public CreateEditDataSourceDialog(SimpleEventBus eventBus,boolean isRunningOnOpenShift,TeiidMgrServiceAsync teiidMgrService) {
		this.eventBus = eventBus;
		this.isRunningOnOpenShift = isRunningOnOpenShift;
		this.teiidMgrService=teiidMgrService;
		this.sourcePropsTable = new PropertyTable(this.eventBus);
	    // Listen for PropertyChangedEvent from properties table.
		this.eventBus.addHandler(PropertyChangedEvent.TYPE, new PropertyChangedEventHandler() {
			public void onEvent(PropertyChangedEvent event) {
	        	setAddSourceDialogOKButtonEnablement();
			}
		});
	}
	
	public void showDialogForAdd(List<String> currentSourceNames, Widget relativeToWidget) {
		addingSource=true;
		this.currentSourceNames.addAll(currentSourceNames);
		
		// Creates the components / panel / handlers
		init(null,null);
				
		// Final Step - Populates the available Sources, fires selection event
		populateSourceTypeListBox();

		// Show the Dialog
		addSourceDialogBox.showRelativeTo(relativeToWidget);
		sourceNameTextBox.setFocus(true);
	}
	
	public void showDialogForEdit(String editSourceName, String editSourceType, Widget relativeToWidget) {
		addingSource=false;

		// Creates the components / panel / handlers
		init(editSourceName,editSourceType);
		
		// Populate properties using current values
		populatePropsUsingCurrent(editSourceName,editSourceType);
		
		// Show the Dialog
		addSourceDialogBox.showRelativeTo(relativeToWidget);
	}
	
	private void init(String editSourceName, String editSourceType) {
		// Title selection is based on new / redeploy flag
		String dialogTitle = messages.redeploySourceDialogTitle();
		if(addingSource) {
			dialogTitle = messages.addSourceDialogTitle();
		}
		// Create the popup AddSource DialogBox
		addSourceDialogBox.setText(dialogTitle);
		addSourceDialogBox.setAnimationEnabled(true);
		// We can set the id of a widget by accessing its Element
		addSourceDialogOKButton.getElement().setId("okButton");
		addSourceDialogCloseButton.getElement().setId("closeButton");
		addSourceDialogOKButton.addStyleName(Resources.INSTANCE.style().dialogButton());
		addSourceDialogCloseButton.addStyleName(Resources.INSTANCE.style().dialogButton());
		addSourceDialogCloseButton.addStyleName(Resources.INSTANCE.style().rightPadding5());
		
		// Dialog Box - Panel Content
		VerticalPanel vPanel = new VerticalPanel();
		vPanel.addStyleName(Resources.INSTANCE.style().createEditDataSourceDialogPanel());
		
		// ------------------------
		// Title Label
		// ------------------------
		Label titleLabel = new Label(messages.enterPropertiesMsg());
		titleLabel.addStyleName(Resources.INSTANCE.style().labelTextBold());
		titleLabel.addStyleName(Resources.INSTANCE.style().bottomPadding10());
		vPanel.add(titleLabel);

		// ------------------------
		// Status Label
		// ------------------------
		sourcePropertyStatusLabel.setText("");
		sourcePropertyStatusLabel.addStyleName(Resources.INSTANCE.style().labelTextItalics());
		sourcePropertyStatusLabel.addStyleName(Resources.INSTANCE.style().paddingBottom10Left20());
		vPanel.add(sourcePropertyStatusLabel);

		// ------------------------
		// Source Name controls
		// ------------------------
		HorizontalPanel sourceNamePanel = new HorizontalPanel();
		sourceNameLabel.addStyleName(Resources.INSTANCE.style().labelTextBold());
		sourceNameLabel.addStyleName(Resources.INSTANCE.style().rightPadding5());
		sourceNamePanel.add(sourceNameLabel);
		// New Source, the user can type in a name
		if(addingSource) {
			sourceNameTextBox.setText("");
			sourceNamePanel.add(sourceNameTextBox);
		// Re-deploy Source, the name cannot be changed
		} else {
			String sourceName = editSourceName;
			sourceNameRedeployLabel.setText(sourceName);
			sourceNamePanel.add(sourceNameRedeployLabel);
		}
		sourceNamePanel.addStyleName(Resources.INSTANCE.style().bottomPadding10());
		vPanel.add(sourceNamePanel);
		
		// ------------------------
		// Driver controls
		// ------------------------
		HorizontalPanel driverPanel = new HorizontalPanel();
		driverLabel.addStyleName(Resources.INSTANCE.style().labelTextBold());
		driverLabel.addStyleName(Resources.INSTANCE.style().rightPadding5());
		driverPanel.add(driverLabel);
		// New Source, add the Driver ListBox
		if(addingSource) {
			driverPanel.add(driverListBox);
		// Re-deploy Source, the driver cannot be changed
		} else {
			String selectedType = editSourceType;
			driverEditLabel.setText(selectedType);
			driverPanel.add(driverEditLabel);
		}
		driverPanel.addStyleName(Resources.INSTANCE.style().bottomPadding10());
		vPanel.add(driverPanel);
		
		// --------------------------
		// Source Properties Table
		// --------------------------
		sourcePropsTable.addStyleName(Resources.INSTANCE.style().dataSourcePropertiesTable());
		ScrollPanel scrollPanel = new ScrollPanel(sourcePropsTable);
		scrollPanel.setHeight("350px");
		vPanel.add(scrollPanel);
		
		// ------------------------------
		// Required Property Note Panel
		// ------------------------------
		HorizontalPanel propertyNotePanel = new HorizontalPanel();
		Label propertyNoteLabel = new Label(messages.requiredPropertyMsg());
		propertyNoteLabel.addStyleName(Resources.INSTANCE.style().labelTextItalics());
		propertyNoteLabel.addStyleName(Resources.INSTANCE.style().rightPadding5());
		propertyNotePanel.add(propertyNoteLabel);
		propertyNotePanel.addStyleName(Resources.INSTANCE.style().bottomPadding10());
		vPanel.add(propertyNotePanel);
		
		// --------------------------
		// Buttons Panel
		// --------------------------
		HorizontalPanel buttonPanel = new HorizontalPanel();
		buttonPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
		buttonPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
		addSourceDialogOKButton.addStyleName(Resources.INSTANCE.style().rightPadding5());
		buttonPanel.add(addSourceDialogCloseButton);
		buttonPanel.add(addSourceDialogOKButton);
		vPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
		vPanel.add(buttonPanel);
		
		addSourceDialogBox.setHeight("200px");
		addSourceDialogBox.setWidth("500px");

		// Add the Completed Panel to the Dialog
		addSourceDialogBox.setWidget(vPanel);
		
		// ---------------------------------------
		// Handlers for Widgets 
		// ---------------------------------------

		// Change Listener for Driver ListBox
		driverListBox.addChangeHandler(new ChangeHandler()
		{
			// Changing the Type selection will re-populate property table with defaults for that type
			public void onChange(ChangeEvent event)
			{
				String selectedType = getDialogSourceType();				
				setSourcePropertiesTableWithDefaults(selectedType);
			}
		});
		
		// Change Listener for Source Name TextBox - does property validation
		sourceNameTextBox.addKeyUpHandler(new KeyUpHandler() {
	        public void onKeyUp(KeyUpEvent event) {
	        	setAddSourceDialogOKButtonEnablement();
	        }
	    });
		
		// Click Handler for DialogBox Close Button
		addSourceDialogCloseButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				addSourceDialogBox.hide();
			}
		});
		
		// Click Handler for DialogBox OK Button
		addSourceDialogOKButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				// Get source name and type from the dialog widgets
				String selectedType = getDialogSourceType();
				String sourceName = getDialogSourceName();

				List<PropertyItem> tableProps = getSourcePropsFromTable();
				
				Map<String,String> propsForCreate = DataSourceHelper.convertPropsForApiSubmittal(tableProps);
								
				addDataSource(sourceName,selectedType,propsForCreate);

				addSourceDialogBox.hide();
			}
		});

		setAddSourceDialogOKButtonEnablement();
	}
	
	/*
	 * Populate the Properties table with defaults for the supplied dataSourceType.  The Property Definitions were
	 * retrieved previously, and saved in the dsName - PropertyObj Map.  This eliminates the need for another server call.
	 * @param dataSourceType the DataSource Type
	 */
	private void setSourcePropertiesTableWithDefaults(final String dataSourceType) {
		// Gets the defaults for the specified type
		List<PropertyItem> propObjs = DataSourceHelper.getInstance().getDefaultProps(dataSourceType);
        if(propObjs==null) {
        	propObjs = new ArrayList<PropertyItem>();
        }
		// Verify properties.  For VIEW_MODEL this adds the DDL property.
		verifyServerProperties(dataSourceType, propObjs);
		populateSourcePropertiesTable(propObjs);
	}
	
	/*
	 * Populate the Properties table using the current values from the datasource on the server
	 * @param dataSourceName the DataSource Name
	 * @param dataSourceType the DataSource Type
	 */
	private void populatePropsUsingCurrent(final String dataSourceName, final String dataSourceType) {
		// Set up the callback object.
		AsyncCallback<List<PropertyItem>> callback = new AsyncCallback<List<PropertyItem>>() {
			// On Failure - show Error Dialog
			public void onFailure(Throwable caught) {
				showErrorDialog(messages.initDSNameErrorTitle(), messages.initDSNameErrorMsg()+caught.getMessage());
			}

			// On Success - Populate the ListBox
			public void onSuccess(List<PropertyItem> propertyItems) {
				// Populate the Properties Table
				populateSourcePropertiesTable(propertyItems);
			}
		};

		teiidMgrService.getDataSourcePropertyItems(dataSourceName,callback);
	}
		
	private void verifyServerProperties(String dataSourceType, List<PropertyItem> propertyObjs) {
		// ------------------------------
		// Add any mising properties
		// ------------------------------
		DataSourceHelper.addMissingProperties(dataSourceType, propertyObjs);
		
		// --------------------------------------------
		// This sets 'default' values on properties
		//   - URL template 
		//   - username placeholder
		// --------------------------------------------
		DataSourceHelper.setPropertyDefaults(dataSourceType, propertyObjs);
	}
	
	/*
	 * Init the List of DataSource Template Names
	 * @param vdbName the name of the VDB
	 * @param sourceName the source name
	 * @param templateName the template name
	 * @param translatorName the translator name
	 * @param propsMap the property Map of name-value pairs
	 */
	private void populateSourceTypeListBox( ) {
		Map<String,List<PropertyItem>> propObjMap = DataSourceHelper.getInstance().getDefaultsMap();

		// Get the set of typeNames
		Set<String> typeNameSet = propObjMap.keySet();

		// passes in 'runningOnOpenShift' flag to filter available sources on OpenShift
		List<String> allowedTypes = DataSourceHelper.filterAllowedTypes(typeNameSet,isRunningOnOpenShift);

		// Make sure clear first
		driverListBox.clear();

		// Repopulate the ListBox.  The actual names are converted to more user-friendly display names
		int i = 0;
		for(String typeName: allowedTypes) {
			String displayName = DataSourceHelper.convertTemplateNameToDisplayName(typeName);
			driverListBox.insertItem(displayName, i);
			i++;
		}

		// Initialize by setting the selection to the first item.
		driverListBox.setSelectedIndex(0);
		DomEvent.fireNativeEvent(Document.get().createChangeEvent(),driverListBox);				
	}
		
	private void fireSourcesChanged() {
		this.eventBus.fireEvent(new SourcesChangedEvent());
	}
	
    /*
	 * Determine the OK Button enablement on the Add Source Dialog.  The Add button
	 * will only enable if the properties are filled out.
	 */
	private void setAddSourceDialogOKButtonEnablement( ) {
		boolean addSourceEnabled = false;
		
		// Validate the entered properties
		if(validateSourceProperties()) {
			addSourceEnabled = true;
		}
		
		// Disable if properties are invalid
		addSourceDialogOKButton.setEnabled(addSourceEnabled);
	}
	
	/*
	 * Validate the entered properties and return status.  The status message label is also updated.
	 * @return the property validation status.  'true' if properties are valid, 'false' otherwise.
	 */
	private boolean validateSourceProperties( ) {
		boolean statusOK = true;
		String statusStr = "OK";
		
		// Validate the entered name
		String name = getDialogSourceName();
		if(name==null || name.trim().length()==0) {
			statusStr = messages.statusEnterNameForDS();
			statusOK = false;
		}
		
		// If new source, check entered name against existing names
		if(statusOK && addingSource) {
			if(currentSourceNames.contains(name)) {
				statusStr = messages.statusDSNameAlreadyExists();
				statusOK = false;
			}
		}
		
		// Validate the Property Table
		if(statusOK) {
			statusStr = this.sourcePropsTable.getStatus();
			if(!statusStr.equals("OK")) {
				statusOK = false;
			}
		}
		
		// Update the status label
		
		if(!statusStr.equals("OK")) {
			sourcePropertyStatusLabel.setText(statusStr);
		} else {
			sourcePropertyStatusLabel.setText(messages.statusClickOKToAccept());
		}
		
		return statusOK;
	}

	/*
	 * Add a Data Source
	 * @param sourceName the source name
	 * @param templateName the template name
	 * @param propsMap the property Map of name-value pairs
	 */
	private void addDataSource(String sourceName, String templateName, Map<String,String> propsMap) {
		// Set up the callback object.
		AsyncCallback<String> callback = new AsyncCallback<String>() {
			// On Failure - show Error Dialog
			public void onFailure(Throwable caught) {
				showErrorDialog(messages.createSourceErrorTitle(), messages.createSourceErrorMsg()+caught.getMessage());
			}

			// On Success - Populate the ListBox
			public void onSuccess(String result) {
				fireSourcesChanged();
			}
		};
		
		teiidMgrService.addDataSource(sourceName, templateName, propsMap, callback);		
	}

	/*
	 * Show the Dialog for Error Display
	 */
	private void showErrorDialog(String title, String msg) {
		this.messageDialog.showError(title,msg);
	}
	
	/*
	 * Get the List of property objects from the Source Properties table.
	 * @return the List of source property objects
	 */
	private List<PropertyItem> getSourcePropsFromTable() {
		return this.sourcePropsTable.getProperties();
	}

	/*
	 * Get the selected Source Type from the Dialog selection.
	 * @return the current selected SourceType
	 */
	private String getDialogSourceType( ) {
		String selectedType = null;
		// For new source - use the ListBox selection
		if(addingSource) {
			int selectedIndex = driverListBox.getSelectedIndex();
			String selectedDisplayType = driverListBox.getValue(selectedIndex);
			// Converts the display name to actual name
			selectedType = DataSourceHelper.convertTemplateDisplayNameToName(selectedDisplayType);
	    // For re-deploy - use the Label widget
		} else {
			String selectedDisplayType = driverEditLabel.getText();
			// Converts the display name to actual name
			selectedType = DataSourceHelper.convertTemplateDisplayNameToName(selectedDisplayType);
		}
		return selectedType;
	}
	
	/*
	 * Get the Source Name from the Source Name TextBox
	 * @return the Source Name TextBox entry
	 */
	private String getDialogSourceName( ) {
		String sourceName = null;
		// For new source - use the TextBox entry
		if(addingSource) {
			sourceName = sourceNameTextBox.getText();
	    // For re-deploy - use the Label widget
		} else {
			sourceName = sourceNameRedeployLabel.getText();
		}
		return sourceName;
	}
	
	/*
	 * Populate the Source Properties Table
	 * @param propertyObjs the List of PropertyObjs
	 */
	private void populateSourcePropertiesTable(List<PropertyItem> propertyObjs) {
		this.sourcePropsTable.setProperties(propertyObjs);
	}
	
}
