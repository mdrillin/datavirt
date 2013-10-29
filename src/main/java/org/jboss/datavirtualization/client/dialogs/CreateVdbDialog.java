package org.jboss.datavirtualization.client.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.jboss.datavirtualization.client.DataItem;
import org.jboss.datavirtualization.client.Messages;
import org.jboss.datavirtualization.client.Resources;
import org.jboss.datavirtualization.client.VDBsPanel;
import org.jboss.datavirtualization.client.rpc.TeiidMgrService;
import org.jboss.datavirtualization.client.rpc.TeiidMgrServiceAsync;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class CreateVdbDialog {
	private final TeiidMgrServiceAsync teiidMgrService = GWT.create(TeiidMgrService.class);

	private MessageDialog messageDialog = new MessageDialog();
	
	private final Messages messages = GWT.create(Messages.class);

	private DialogBox newVDBDialogBox = new DialogBox();
	private Button newVDBDialogOKButton = new Button(messages.okButton());
	private Button newVDBDialogCloseButton = new Button(messages.cancelButton());
	private Label newVDBStatusLabel = new Label();
	private TextBox newVDBNameTextBox = new TextBox();
	private VDBsPanel vdbsPanel;
	private List<String> currentVdbNames = new ArrayList<String>();
	
	/**
	 * Constructor for the CreateVdbDialog
	 */
	public CreateVdbDialog(VDBsPanel vdbsPanel) {
		this.vdbsPanel = vdbsPanel;
		init();
	}
	
	/*
	 * Init the dialog components
	 */
	private void init( ) {
		// Create the popup Error DialogBox
		newVDBDialogBox.setText(messages.dialogCreateVDBTitle());
		newVDBDialogBox.setAnimationEnabled(true);
		
		// Dialog Box - Panel Content
		VerticalPanel vPanel = new VerticalPanel();
		vPanel.addStyleName(Resources.INSTANCE.style().createVdbDialogPanel());
		
		// Message
		Label titleLabel = new Label(messages.dialogCreateVDBEnterName());
		titleLabel.addStyleName(Resources.INSTANCE.style().labelTextBold());
		titleLabel.addStyleName(Resources.INSTANCE.style().bottomPadding10());
		vPanel.add(titleLabel);

		// Status Label
		newVDBStatusLabel.setText("");
		newVDBStatusLabel.addStyleName(Resources.INSTANCE.style().labelTextItalics());
		newVDBStatusLabel.addStyleName(Resources.INSTANCE.style().paddingBottom10Left20());
		vPanel.add(newVDBStatusLabel);
				
		// Source Name widgets
		HorizontalPanel vdbNamePanel = new HorizontalPanel();
		Label newVDBNameLabel = new Label(messages.dialogCreateVDBName());
		newVDBNameLabel.addStyleName(Resources.INSTANCE.style().labelTextBold());
		newVDBNameLabel.addStyleName(Resources.INSTANCE.style().rightPadding5());
		
		newVDBNameTextBox.setText("");
		vdbNamePanel.add(newVDBNameLabel);
		vdbNamePanel.add(newVDBNameTextBox);
		vdbNamePanel.addStyleName(Resources.INSTANCE.style().bottomPadding10());
		vPanel.add(vdbNamePanel);
		
		// Buttons Panel
		HorizontalPanel buttonPanel = new HorizontalPanel();
		buttonPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
		buttonPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
		newVDBDialogOKButton.addStyleName(Resources.INSTANCE.style().dialogButton());
		newVDBDialogCloseButton.addStyleName(Resources.INSTANCE.style().dialogButton());
		newVDBDialogCloseButton.addStyleName(Resources.INSTANCE.style().rightPadding5());
		buttonPanel.add(newVDBDialogCloseButton);
		buttonPanel.add(newVDBDialogOKButton);
		vPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
		vPanel.add(buttonPanel);
		
		// Add the Completed Panel to the Dialog
		newVDBDialogBox.setWidget(vPanel);
		
		// ---------------------------------------
		// Widgets for New VDB Dialog
		// ---------------------------------------

		// Change Listener for New VDB Name TextBox - does property validation
		newVDBNameTextBox.addKeyUpHandler(new KeyUpHandler() {
	        public void onKeyUp(KeyUpEvent event) {
	        	setNewVDBDialogOKButtonEnablement();
	        }
	    });
		
		// Click Handler for DialogBox Close Button
		newVDBDialogCloseButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				newVDBDialogBox.hide();
			}
		});
		
		// Click Handler for DialogBox OK Button
		newVDBDialogOKButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				// Get VDB name from the dialog widgets
				String vdbName = newVDBNameTextBox.getText();
				createVDB(vdbName);
				newVDBDialogBox.hide();
			}
		});
		
		setNewVDBDialogOKButtonEnablement();
	}
	
	/*
	 * Show the Dialog for Creating a New VDB
	 */
	public void show(List<String> currentVdbNames, Widget relativeToWidget) {
		this.currentVdbNames = currentVdbNames;
		newVDBNameTextBox.setText("");
		newVDBDialogBox.showRelativeTo(relativeToWidget);
		newVDBNameTextBox.setFocus(true);
	}
	/*
	 * Determine the OK Button enablement on the New VDB Dialog.  The OK button
	 * will only enable if the Name is valid and is not a duplicate.
	 */
	private void setNewVDBDialogOKButtonEnablement( ) {
		boolean okButtonEnabled = false;
		
		// Validate the entered VDB Name
		if(validateVDBName()) {
			okButtonEnabled = true;
		}
		
		// Set enabled state of OK button - disables for invalid or duplicate name
		newVDBDialogOKButton.setEnabled(okButtonEnabled);
	}
	
	/*
	 * Validate the VDB Name for a new VDB.  Checks for valid characters and checks against current list for duplicates.
	 * @return 'true' if the entered name is OK, 'false' if not OK.
	 */
	private boolean validateVDBName() {
		boolean statusOK = true;
		String statusStr = "OK";
		
		// Validate the entered VDB name
		String newVDBName = newVDBNameTextBox.getText();
		if(newVDBName==null || newVDBName.trim().length()==0) {
			statusStr = messages.statusEnterNameForVDB();
			statusOK = false;
		}
		
		// Check entered name against existing names
		if(statusOK) {
			if(this.currentVdbNames.contains(newVDBName)) {
				statusStr = messages.statusVDBNameAlreadyExists();
				statusOK = false;
			}
		}
		
		// Check for valid characters
		if(statusOK) {
			boolean allValidChars = true;
			String str = newVDBName.trim();
			for (int i = 0; i < str.length(); i++){
			    char c = str.charAt(i);
			    if( (!Character.isLetterOrDigit(c) && c!='-' && c!='_') || c==' ') {
			    	allValidChars = false;
			    	break;
			    }
			}
			if(!allValidChars) {
				statusStr = messages.statusVDBNameContainsInvalidChars();
				statusOK = false;
			}
		}
		
		// Update the status label
		
		if(!statusStr.equals("OK")) {
			newVDBStatusLabel.setText(statusStr);
		} else {
			newVDBStatusLabel.setText(messages.statusClickOKToAccept());
		}
		
		return statusOK;
	}
	
	/*
	 * Create a VDB
	 * @param vdbName the name of the VDB to create
	 */
	private void createVDB(final String vdbName) {
		// Set up the callback object.
		AsyncCallback<List<DataItem>> callback = new AsyncCallback<List<DataItem>>() {
			// On Failure - show Error Dialog
			public void onFailure(Throwable caught) {
				messageDialog.showError(messages.createVDBErrorTitle(), messages.createVDBErrorMsg()+caught.getMessage());
				vdbsPanel.setModelMgmtButtonEnablements();
			}

			// On Success - no action.  List Box updates after call.
			public void onSuccess(List<DataItem> vdbItems) {
				vdbsPanel.populateVDBListBox(vdbItems,vdbName);
			}
		};

		// Creates the VDB
		teiidMgrService.createVDB(vdbName, callback);
	}
	
}
