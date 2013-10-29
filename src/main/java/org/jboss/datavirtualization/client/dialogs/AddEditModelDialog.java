package org.jboss.datavirtualization.client.dialogs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.datavirtualization.client.Messages;
import org.jboss.datavirtualization.client.Resources;
import org.jboss.datavirtualization.client.events.SourcesChangedEvent;
import org.jboss.datavirtualization.client.events.VDBRedeployEvent;
import org.jboss.datavirtualization.client.rpc.TeiidMgrServiceAsync;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Dialog for Adding or Editing a Model to a Dynamic VDB
 */
public class AddEditModelDialog {

	private static final int CHAR_WIDTH_TEXTAREA = 70;
	private static final String HEIGHT_TEXTAREA = "35em";
	private final Messages messages = GWT.create(Messages.class);
	
	/** ModelTypes */
	public enum ModelType {
	  SOURCE, VIEW;
	}	
	
	// Model Name Controls
	private Label modelNameLabel = new Label(messages.nameLabel());
	private TextBox modelNameTextBox = new TextBox();
	private Label modelNameEditLabel = new Label();
	
	// Status message label
	private Label dialogStatusLabel = new Label();

	// Available DataSource Controls
	private Label availableSourcesLabel = new Label(messages.dataSourceLabel());
	private ListBox availableSourcesListBox = new ListBox();

	// Translator controls
	private Label translatorLabel = new Label(messages.translatorLabel());
	private TextBox translatorTextBox = new TextBox();
	private ListBox translatorListBox = new ListBox();
	
	// DDL text area
	private MyTextArea ddlTextArea = new MyTextArea();

	// Dialog Controls
	private DialogBox addModelDialogBox = new DialogBox();
	private Button addModelDialogOKButton = new Button(messages.okButton());
	private Button addModelDialogCloseButton = new Button(messages.cancelButton());

	// Message Dialog 
	private MessageDialog messageDialog = new MessageDialog();

	// Flag for Add vs Edit Model state
	private boolean addingModel = true;
	String vdbName;
	ModelType modelType = ModelType.SOURCE;
	
	private List<String> currentModelNames = new ArrayList<String>();
	private List<String> currentVdbNames = new ArrayList<String>();
	
	private Map<String,String> defaultTranslatorMap = new HashMap<String,String>();
	
	// EventBus and TeiidMgrService
	private SimpleEventBus eventBus;
	private final TeiidMgrServiceAsync teiidMgrService;
	
	/*
	 * Constructor for the Dialog
	 */
	public AddEditModelDialog(SimpleEventBus eventBus, TeiidMgrServiceAsync teiidMgrService) {
		this.eventBus=eventBus;
		this.teiidMgrService=teiidMgrService;
	}
	
	public void initDialogForAdd(String vdbName, ModelType modelType, List<String> currentModelNames, List<String> currentVdbNames) {
		addingModel=true;
		this.vdbName = vdbName;
		this.modelType = modelType;
		this.currentModelNames.addAll(currentModelNames);
		this.currentVdbNames.addAll(currentVdbNames);
		
		// Creates the components / panel / handlers
		init(modelType,null,null);
	}
	
	public void initDialogForEdit(String vdbName, ModelType modelType, String modelName, String ddl) {
		addingModel=false;
		this.vdbName = vdbName;
		this.modelType = modelType;

		// Creates the components / panel / handlers
		init(modelType,modelName,ddl);
	}
	
	public void showDialog(Widget relativeToWidget) {
		// Show the Dialog
		addModelDialogBox.showRelativeTo(relativeToWidget);
		translatorListBox.setFocus(true);
	}
	
	private void init(ModelType modelType, String editModelName, String ddl) {
		// Title selection is based on add / edit flag and modelType
		String dialogTitle = null;
		if(addingModel) {
			if(modelType==ModelType.SOURCE) {
				dialogTitle = messages.addSourceModelDialogTitle();
			} else {
				dialogTitle = messages.addViewModelDialogTitle();
			}
		} else {
			if(modelType==ModelType.SOURCE) {
				dialogTitle = messages.editSourceModelDialogTitle();
			} else {
				dialogTitle = messages.editViewModelDialogTitle();
			}
		}
		
		// Create the popup AddSource DialogBox
		addModelDialogBox.setText(dialogTitle);
		addModelDialogBox.setAnimationEnabled(true);
		// We can set the id of a widget by accessing its Element
		addModelDialogOKButton.getElement().setId("okButton");
		addModelDialogCloseButton.getElement().setId("closeButton");
		addModelDialogOKButton.addStyleName(Resources.INSTANCE.style().dialogButton());
		addModelDialogCloseButton.addStyleName(Resources.INSTANCE.style().dialogButton());
		addModelDialogCloseButton.addStyleName(Resources.INSTANCE.style().rightPadding5());
		
		// Dialog Box - Panel Content
		VerticalPanel vPanel = new VerticalPanel();
		vPanel.addStyleName(Resources.INSTANCE.style().addEditModelDialogPanel());
		
		// ------------------------
		// Title Label
		// ------------------------
		Label titleLabel = new Label(messages.enterModelInfoMsg());
		titleLabel.addStyleName(Resources.INSTANCE.style().labelTextBold());
		titleLabel.addStyleName(Resources.INSTANCE.style().bottomPadding10());
		vPanel.add(titleLabel);

		// ------------------------
		// Dialog Status Label
		// ------------------------
		dialogStatusLabel.setText("");
		dialogStatusLabel.addStyleName(Resources.INSTANCE.style().labelTextItalics());
		dialogStatusLabel.addStyleName(Resources.INSTANCE.style().paddingBottom10Left20());
		vPanel.add(dialogStatusLabel);

		// ------------------------
		// Model Name controls
		// ------------------------
		if(modelType==ModelType.VIEW) {
			HorizontalPanel modelNamePanel = new HorizontalPanel();
			modelNameLabel.addStyleName(Resources.INSTANCE.style().labelTextBold());
			modelNameLabel.addStyleName(Resources.INSTANCE.style().rightPadding5());
			modelNamePanel.add(modelNameLabel);
			// New Model, the user can type in a name
			if(addingModel) {
				modelNameTextBox.setText("");
				modelNamePanel.add(modelNameTextBox);
				// Edit Model, the name cannot be changed
			} else {
				modelNameEditLabel.setText(editModelName);
				modelNamePanel.add(modelNameEditLabel);
			}
			modelNamePanel.addStyleName(Resources.INSTANCE.style().bottomPadding10());
			vPanel.add(modelNamePanel);
		}
		
		if(modelType==ModelType.SOURCE) {
			// ------------------------------
			// Overall Horizonal Panel
			// ------------------------------
			HorizontalPanel overallPanel = new HorizontalPanel();
			VerticalPanel leftPanel = new VerticalPanel();
			VerticalPanel rightPanel = new VerticalPanel();
			
			// ------------------------------
			// Available DataSource controls
			// ------------------------------
			HorizontalPanel availableSourcesPanel = new HorizontalPanel();
			availableSourcesLabel.addStyleName(Resources.INSTANCE.style().labelTextBold());
			availableSourcesLabel.addStyleName(Resources.INSTANCE.style().rightPadding5());
			availableSourcesPanel.add(availableSourcesLabel);
			
			populateAvailableSourcesListBox();
			availableSourcesPanel.add(availableSourcesListBox);
			availableSourcesPanel.addStyleName(Resources.INSTANCE.style().bottomPadding10());
			
			leftPanel.add(availableSourcesPanel);

			// --------------------------
			// Translator Name controls
			// --------------------------
			HorizontalPanel translatorNamePanel = new HorizontalPanel();
			// Add TranslatorName Label to Panel
			translatorLabel.addStyleName(Resources.INSTANCE.style().labelTextBold());
			translatorLabel.addStyleName(Resources.INSTANCE.style().rightPadding5());
			translatorNamePanel.add(translatorLabel);
			
			// Add Translator TextBox to Panel
			translatorNamePanel.add(translatorTextBox);
			
			translatorNamePanel.addStyleName(Resources.INSTANCE.style().bottomPadding10());
			leftPanel.add(translatorNamePanel);
			
			// Add Translator ListBox to RightPanel
			populateTranslatorList();
			translatorListBox.setVisibleItemCount(6);
			rightPanel.add(new Label("Available Translators"));
			rightPanel.add(translatorListBox);
			
			overallPanel.add(leftPanel);
			overallPanel.add(rightPanel);
			
			vPanel.add(overallPanel);
			
			// Populate map of source to default translator
			populateDefaultTranslatorsMap();
		}
		
		if(modelType==ModelType.VIEW) {
			Label ddlLabel = new Label();
			ddlLabel.setText(messages.ddlLabel());
			vPanel.add(ddlLabel);
			
			ddlTextArea.setCharacterWidth(CHAR_WIDTH_TEXTAREA);
			ddlTextArea.setHeight(HEIGHT_TEXTAREA);
			ddlTextArea.setText("");
			if(addingModel) {
				ddlTextArea.setText("");
			} else {
				ddlTextArea.setText(ddl);
			}
			vPanel.add(ddlTextArea);
		}
		
		// --------------------------
		// Buttons Panel
		// --------------------------
		HorizontalPanel buttonPanel = new HorizontalPanel();
		buttonPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
		buttonPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
		addModelDialogOKButton.addStyleName(Resources.INSTANCE.style().rightPadding5());
		buttonPanel.add(addModelDialogCloseButton);
		buttonPanel.add(addModelDialogOKButton);
		vPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
		vPanel.add(buttonPanel);
		
		addModelDialogBox.setHeight("200px");
		addModelDialogBox.setWidth("500px");

		// Add the Completed Panel to the Dialog
		addModelDialogBox.setWidget(vPanel);
		
		// ---------------------------------------
		// Handlers for Widgets 
		// ---------------------------------------

		// Change Listener for Source Type ListBox
		availableSourcesListBox.addChangeHandler(new ChangeHandler()
		{
			// Changing the Type selection will re-populate property table with defaults for that type
			public void onChange(ChangeEvent event)
			{
				// Set default translator in the translator TextBox
				String translator = getDefaultTranslatorForSource(getDialogDataSource());
				translatorTextBox.setText(translator);
				setAddModelDialogOKButtonEnablement();
			}
		});
		
		// Change Listener for Translator ListBox
		translatorListBox.addChangeHandler(new ChangeHandler()
		{
			// Changing the Translator selection will set the translator text in the text box
			public void onChange(ChangeEvent event)
			{
				String selectedTranslator = getTranslatorListBoxSelection();				
				translatorTextBox.setText(selectedTranslator);
				setAddModelDialogOKButtonEnablement();
			}
		});
		
		// Change Listener for Source Name TextBox - does property validation
		modelNameTextBox.addKeyUpHandler(new KeyUpHandler() {
	        public void onKeyUp(KeyUpEvent event) {
	        	setAddModelDialogOKButtonEnablement();
	        }
	    });
		
		// Change Listener for Source Name TextBox - does property validation
		ddlTextArea.addKeyUpHandler(new KeyUpHandler() {
	        public void onKeyUp(KeyUpEvent event) {
	        	setAddModelDialogOKButtonEnablement();
	        }
	    });
		ddlTextArea.addValueChangeHandler(new ValueChangeHandler<String>() {
	        public void onValueChange(ValueChangeEvent<String> event) {
	        	setAddModelDialogOKButtonEnablement();
	        }
	    });
	    
		// Change Listener for Translator Name TextBox - does property validation
		translatorTextBox.addKeyUpHandler(new KeyUpHandler() {
	        public void onKeyUp(KeyUpEvent event) {
	        	setAddModelDialogOKButtonEnablement();
	        }
	    });
		
		// Click Handler for DialogBox Close Button
		addModelDialogCloseButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				addModelDialogBox.hide();
			}
		});
		
		// Click Handler for DialogBox OK Button
		addModelDialogOKButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				// Get source name and type from the dialog widgets
				String modelName = getDialogModelName();

				// ViewModel
				if(getModelType() == ModelType.VIEW) {
					String ddl = getDialogDdl();
					// Add / Re-deploy the Model
					addOrReplaceViewModel(vdbName,modelName,ddl);
				// SourceModel
				} else {
					String dataSource = getDialogDataSource();
					String translator = getDialogTranslatorName();
					
					// Name of the SourceModel VDB
					deploySourceVDBAddImportAndRedeploy(vdbName,dataSource,translator);
				}

				addModelDialogBox.hide();
			}
		});

		setAddModelDialogOKButtonEnablement();
	}
	
	private String getDefaultTranslatorForSource(String dataSourceName) {
		return defaultTranslatorMap.get(dataSourceName);
	}
	
	private ModelType getModelType() {
		return this.modelType;
	}
			
	/*
	 * Init the List of available DataSource Names
	 */
	private void populateAvailableSourcesListBox( ) {
		// Set up the callback object.
		AsyncCallback<List<String>> callback = new AsyncCallback<List<String>>() {
			// On Failure - show Error Dialog
			public void onFailure(Throwable caught) {
				showErrorDialog(messages.initTranslatorNamesErrorTitle(), messages.initTranslatorNamesErrorMsg()+caught.getMessage());
			}

			// On Success - Populate the ListBox
			public void onSuccess(List<String> dsNames) {
				// Make sure clear first
				availableSourcesListBox.clear();
				
				// Repopulate the ListBox
				int i = 0;
				for(String dsName: dsNames) {
					if(!currentModelNames.contains(dsName) && !currentVdbNames.contains(dsName)) {
						availableSourcesListBox.insertItem(dsName, i);
						i++;
					}
				}
				
				availableSourcesListBox.setItemSelected(0,true);
			}
		};

		teiidMgrService.getDataSourceNames(callback);	
	}
	
	/*
	 * Init the List of Translator Names
	 */
	private void populateTranslatorList() {
		// Set up the callback object.
		AsyncCallback<List<String>> callback = new AsyncCallback<List<String>>() {
			// On Failure - show Error Dialog
			public void onFailure(Throwable caught) {
				showErrorDialog(messages.initTranslatorNamesErrorTitle(), messages.initTranslatorNamesErrorMsg()+caught.getMessage());
			}

			// On Success - Populate the ListBox
			public void onSuccess(List<String> translatorNames) {
				// Make sure clear first
				translatorListBox.clear();
				
				// Repopulate the ListBox
				int i = 0;
				for(String transName: translatorNames) {
					translatorListBox.insertItem(transName, i);
					i++;
				}
			}
		};

		teiidMgrService.getTranslatorNames(callback);	
	}
	
	/*
	 * Populate Map of DataSource name with Default Translators
	 */
	private void populateDefaultTranslatorsMap() {
		// Set up the callback object.
		AsyncCallback<Map<String,String>> callback = new AsyncCallback<Map<String,String>>() {
			// On Failure - show Error Dialog
			public void onFailure(Throwable caught) {
				showErrorDialog(messages.initDefaultTranslatorMapErrorTitle(), messages.initDefaultTranslatorMapErrorMsg()+caught.getMessage());
			}

			// On Success - Populate the ListBox
			public void onSuccess(Map<String,String> defaultMap) {
				// Make sure clear first
				defaultTranslatorMap.clear();
				
				defaultTranslatorMap.putAll(defaultMap);
			}
		};

		teiidMgrService.getDefaultTranslatorNames(callback);	
	}

	private void fireSourcesChanged() {
		this.eventBus.fireEvent(new SourcesChangedEvent());
	}
	
	private void fireVDBRedeploy() {
		this.eventBus.fireEvent(new VDBRedeployEvent());
	}
	
	  /*
	 * Determine the OK Button enablement on the Add Model Dialog.  The Add button
	 * will only enable if the properties are filled out.
	 */
	private void setAddModelDialogOKButtonEnablement( ) {
		boolean addModelEnabled = false;
		
		// Validate the entered properties
		if(validateSourceProperties()) {
			addModelEnabled = true;
		}
		
		// Disable if properties are invalid
		addModelDialogOKButton.setEnabled(addModelEnabled);
	}
	
	/*
	 * Validate the entered properties and return status.  The status message label is also updated.
	 * @return the property validation status.  'true' if properties are valid, 'false' otherwise.
	 */
	private boolean validateSourceProperties( ) {
		boolean statusOK = true;
		String statusStr = "OK";
		
		// Validate the entered name for view Model
		String name = getDialogModelName();
		if(modelType==ModelType.VIEW) {
			if(name==null || name.trim().length()==0) {
				statusStr = messages.statusEnterNameForModel();
				statusOK = false;
			}
			// If new source, check entered name against existing model names
			if(statusOK && addingModel) {
				if(currentModelNames.contains(name)) {
					statusStr = messages.statusModelNameAlreadyExists();
					statusOK = false;
				}
			}
			// If new source, check entered name against existing vdb names
			if(statusOK && addingModel) {
				if(currentVdbNames.contains(name)) {
					statusStr = messages.statusModelNameMatchesVdb();
					statusOK = false;
				}
			}
			// Ensure DDL box is not empty
			if(statusOK) {
				String ddlString = ddlTextArea.getText();
				if(ddlString==null || ddlString.trim().length()==0) {
					statusStr = messages.statusViewModelDDLEmptyMsg();
					statusOK = false;
				}
			}
			// Update the status label
			if(!statusStr.equals("OK")) {
				dialogStatusLabel.setText(statusStr);
			} else {
				dialogStatusLabel.setText(messages.statusClickOKToAccept());
			}
		// Generic Message for source Model
		} else {
			String translator = getDialogTranslatorName();
			if(translator==null || translator.trim().length()==0) {
				statusStr = messages.statusSelectTranslatorForSource();
				statusOK = false;
			}
			// Update the status label
			if(!statusStr.equals("OK")) {
				dialogStatusLabel.setText(statusStr);
			} else {
				dialogStatusLabel.setText(messages.statusClickOKToAccept());
			}
		}
		
		return statusOK;
	}
	
	/*
	 * Add or Replace a source Model in the specified VDB
	 * @param vdbName the name of the VDB
	 * @param modelName the model name
	 * @param dataSourceName the dataSource name
	 * @param translator the translator
	 */
	private void deploySourceVDBAddImportAndRedeploy(final String vdbName, String dataSourceName, String translator) {
		// Set up the callback object.
		AsyncCallback<String> callback = new AsyncCallback<String>() {
			// On Failure - show Error Dialog
			public void onFailure(Throwable caught) {
				showErrorDialog(messages.addOrReplaceSourceModelErrorTitle(), messages.addOrReplaceSourceModelErrorMsg()+caught.getMessage());
			}

			// On Success - Populate the ListBox
			public void onSuccess(String result) {
				if(result.equals("success")) {
					fireSourcesChanged();
				} else {
					showErrorDialog(messages.addOrReplaceSourceModelErrorTitle(), result);
					fireSourcesChanged();
				}
			}
		};
		
		fireVDBRedeploy();
		
		String sourceVDBName = "VDBMgr-"+dataSourceName+"-"+translator;
		teiidMgrService.deploySourceVDBAddImportAndRedeploy(vdbName, sourceVDBName, dataSourceName, translator, callback);		
	}
	
	/*
	 * Add or Replace a View Model in the specified VDB
	 * @param vdbName the name of the VDB
	 * @param modelName the model name
	 * @param ddl the DDL that defines the view
	 */
	private void addOrReplaceViewModel(final String vdbName, String modelName, String ddl) {
		// Set up the callback object.
		AsyncCallback<String> callback = new AsyncCallback<String>() {
			// On Failure - show Error Dialog
			public void onFailure(Throwable caught) {
				showErrorDialog(messages.addOrReplaceViewModelErrorTitle(), messages.addOrReplaceViewModelErrorMsg()+caught.getMessage());
			}

			// On Success - Populate the ListBox
			public void onSuccess(String result) {
				fireSourcesChanged();
			}
		};
		
		fireVDBRedeploy();
		
		teiidMgrService.addOrReplaceViewModelAndRedeploy(vdbName, modelName, ddl, callback);		
	}

	/*
	 * Show the Dialog for Error Display
	 */
	private void showErrorDialog(String title, String msg) {
		this.messageDialog.showError(title,msg);
	}
	
	/*
	 * Get the selected DataSource from the Dialog selection.
	 * @return the current selected DataSource Name
	 */
	private String getDialogDataSource( ) {
		int selectedIndex = availableSourcesListBox.getSelectedIndex();
		String selectedDataSource = availableSourcesListBox.getValue(selectedIndex);
		return selectedDataSource;
	}
	
	/*
	 * Get the Model Name from the Model Name TextBox
	 * @return the Model Name TextBox entry
	 */
	private String getDialogModelName( ) {
		String modelName = null;
		// For new source - use the TextBox entry
		if(addingModel) {
			modelName = modelNameTextBox.getText();
	    // For re-deploy - use the Label widget
		} else {
			modelName = modelNameEditLabel.getText();
		}
		return modelName;
	}
	
	/*
	 * Get the Translator Name from the Translator TextBox
	 * @return the Translator Name
	 */
	private String getDialogTranslatorName( ) {
		return translatorTextBox.getText();
	}
	
	/*
	 * Get the Translator Name from the Translator ListBox
	 * @return the Translator Name
	 */
	private String getTranslatorListBoxSelection( ) {
		int selectedIndex = translatorListBox.getSelectedIndex();
		String selectedTranslator = translatorListBox.getValue(selectedIndex);
		return selectedTranslator;
	}
	
	/*
	 * Get the DDL for View Models
	 * @return the DDL
	 */
	private String getDialogDdl( ) {
		if(modelType==ModelType.VIEW && ddlTextArea!=null) return ddlTextArea.getText();
		return "";
	}
	
	/** 
	 * Subclass TextArea to handle paste
	 */
	class MyTextArea extends TextArea {
		public MyTextArea() {
			super();
			sinkEvents(Event.ONPASTE);
		}

		@Override
		public void onBrowserEvent(Event event) {
			super.onBrowserEvent(event);
			switch (DOM.eventGetType(event)) {
			case Event.ONPASTE:
				Scheduler.get().scheduleDeferred(new ScheduledCommand() {

					public void execute() {
						ValueChangeEvent.fire(MyTextArea.this, getText());
					}

				});
				break;
			}
		}
	}	
	
}
