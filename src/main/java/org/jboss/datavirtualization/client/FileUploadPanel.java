package org.jboss.datavirtualization.client;

import java.util.ArrayList;
import java.util.List;

import org.jboss.datavirtualization.client.dialogs.MessageDialog;
import org.jboss.datavirtualization.client.events.FileUploadedEvent;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class FileUploadPanel extends VerticalPanel {

	private SimpleEventBus eventBus;
	private final Messages messages = GWT.create(Messages.class);

	// Message Dialog
	private MessageDialog messageDialog = new MessageDialog();
	private List<String> currentNames = new ArrayList<String>();
	
	private Label titleLabel = new Label();
	private Label statusLabel = new Label();
	
	private FormPanel form = new FormPanel();
	private FileUpload fileUpload = new FileUpload();
	private TextBox nameTextBox = new TextBox();
	private Button uploadButton = new Button(messages.uploadButton());

	private String uploadType;
	
	/*
	 * Constructor for the Panel
	 */
	public FileUploadPanel( ) {		

	}
	
	/*
	 * Create panel
	 */
	public void init(SimpleEventBus eventBus, String title, List<String> currentNames, String uploadType ) {
		this.eventBus = eventBus;
		this.currentNames = currentNames;
		this.uploadType = uploadType;
		
		// ----------------------------------------
		// Add title and status labels
		// ----------------------------------------
		titleLabel.setText(title);
		titleLabel.addStyleName(Resources.INSTANCE.style().titleSmall());
		
		setStatusLabel(messages.statusSelectFileToUpload());
		
		add(titleLabel);
		add(statusLabel);
		
    	// ----------------------------------------
    	// Add the Uploader Widget
    	// ----------------------------------------
    	Widget uploaderWidget = getFileUploaderWidget();
    	add(uploaderWidget);
    	    	
    	// -----------------------------------------------
    	// Horizontal Panel for name entry, upload button
    	// -----------------------------------------------
    	HorizontalPanel hPanel = new HorizontalPanel();
    	Label nameLabel = new Label(messages.nameLabel());
    	nameLabel.addStyleName(Resources.INSTANCE.style().labelText());
    	hPanel.add(nameLabel);
    	
    	// TextBox for driver deployment name (defaults to driver filename)
    	nameTextBox.addKeyUpHandler(new KeyUpHandler() {
	        public void onKeyUp(KeyUpEvent event) {
	        	validateControls();
	        }
	    });
     	hPanel.add(nameTextBox);
     	if(uploadType.equals("VDB")) {
     		nameTextBox.setEnabled(false);
     	}
    	
    	// ----------------------------------------
    	// Create the Upload Button
    	// ----------------------------------------
		// Initial state of Upload Button is disabled.
		uploadButton.setEnabled(false);
		// Add a handler to close the DialogBox
		uploadButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
    	        String filename = fileUpload.getFilename();
    	        
    	        // Check for errors before uploading
    	        boolean okToUpload = true;
    	        // Check for file entry
    	        if (filename.length() == 0) {
    	        	messageDialog.showError(messages.uploadPanelNoSelectionDialogTitle(), messages.uploadPanelNoSelectionDialogMsg());
    	        	okToUpload = false;
    	        // For VDB uploads, check file type
    	        } else if(getUploadType().equals("VDB")) {
    	        	if(!filename.toUpperCase().endsWith(".VDB") && !filename.toUpperCase().endsWith("-VDB.XML")) {
    	        		messageDialog.showError(messages.uploadPanelBadVdbSelectionDialogTitle(), messages.uploadPanelBadVdbSelectionDialogMsg());
    	        		okToUpload = false;
    	        	}
    	        }
    	        
    	        if(okToUpload) {
    				setStatusLabel(messages.statusUploadInProgress());
        			form.submit();
    	        }
			}
		});
		uploadButton.addStyleName(Resources.INSTANCE.style().driverButton());
    	hPanel.add(uploadButton);

    	add(hPanel);
	}
	
	private String getUploadType() {
		return this.uploadType;
	}
	
    public Widget getFileUploaderWidget() {
    	form.setEncoding(FormPanel.ENCODING_MULTIPART);
    	form.setMethod(FormPanel.METHOD_POST);
    	form.setAction(GWT.getModuleBaseURL()+"teiid");
    	
    	VerticalPanel vPanel = new VerticalPanel();

    	// Set the deployment name for the selected file
    	//fileUpload.setName("UploadFileName");
		// Change Listener for File Selection
    	fileUpload.addChangeHandler(new ChangeHandler()
		{
			// Changing the Type selection will re-populate property table with defaults for that type
			public void onChange(ChangeEvent event)
			{
				String filename = fileUpload.getFilename();
				nameTextBox.setText(filename);
				validateControls();
			}
		});
    	vPanel.add(fileUpload);
    	
    	form.addSubmitCompleteHandler(new FormPanel.SubmitCompleteHandler() {
    		public void onSubmitComplete(SubmitCompleteEvent event) {
    			String result = event.getResults();
    			if("<pre>UploadSuccess</pre>".equalsIgnoreCase(result)) {
    				messageDialog.showMessage("Upload Complete", "Upload completed successfully",statusLabel);

        			// Reset Uploader
        			resetUploadSelection();
        			validateControls();
    				
    				// Fire Completion Event
    				fireFileUploadedEvent();
    			} else {
    				messageDialog.showError("Upload Failed", "Upload failed");
    			}
    		}
    	});

    	form.add(vPanel);

    	return form;
    }
    
	/*
	 * Fire event upon successful completion
	 */
	private void fireFileUploadedEvent() {
		FileUploadedEvent event = new FileUploadedEvent();
		event.setUploadType(this.uploadType);
		this.eventBus.fireEvent(event);
	}
   
    private void resetUploadSelection() {
		DOM.setElementProperty(fileUpload.getElement(),"value","");
		nameTextBox.setText("");
    }
    
	private void setStatusLabel(String statusText) {
		// Set the status text
	    statusLabel.setText(statusText);
	    
	    // Set the status label style based on text
		if(statusText!=null) {
			// Upload in Progress - message is red
			if(statusText.trim().equalsIgnoreCase(messages.statusUploadInProgress())) {
				statusLabel.removeStyleName(Resources.INSTANCE.style().labelTextItalics());
				statusLabel.addStyleName(Resources.INSTANCE.style().labelTextItalicsRed());
			// Anything not in Progress - black
			} else {
				statusLabel.removeStyleName(Resources.INSTANCE.style().labelTextItalicsRed());
				statusLabel.addStyleName(Resources.INSTANCE.style().labelTextItalics());
			}
		}
	}
	
	private void validateControls() {

		// ------------------------------------
		// Upload Controls
		// ------------------------------------
		boolean statusOK = true;
		String statusStr = "OK";
		
        String uploadFilename = fileUpload.getFilename();
        // Check if no selection
        if(uploadFilename==null || uploadFilename.trim().length()==0) {
        	statusStr = messages.statusSelectFileToUpload();
        	statusOK = false;
        }
        
        // Check if there is a deployment name
        String deploymentName = null;
        if(statusOK) {
        	deploymentName = this.nameTextBox.getText();
        	if(deploymentName==null || deploymentName.trim().length()==0) {
        		statusStr = messages.statusEnterDeploymentName();
        		statusOK = false;
        	}
        }
        
        // Check if entered driverName already exists
        if(statusOK) {
        	if(currentNames.contains(deploymentName)) {
        		statusStr = messages.statusNameAlreadyExists();
        		statusOK = false;
        	}
        }
        
		if(!statusStr.equals("OK")) {
			setStatusLabel(statusStr);
		} else {
			setStatusLabel(messages.statusClickUploadToAccept());
		}
		
		// if ok, set fileUpload name to textBox name
		if(statusOK) {
			fileUpload.setName(deploymentName);
			uploadButton.setEnabled(true);
		} else {
			uploadButton.setEnabled(false);
		}
				
	}

}
