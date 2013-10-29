package org.jboss.datavirtualization.client.dialogs;

import org.jboss.datavirtualization.client.Messages;
import org.jboss.datavirtualization.client.Resources;
import org.jboss.datavirtualization.client.rpc.TeiidMgrService;
import org.jboss.datavirtualization.client.rpc.TeiidMgrServiceAsync;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class ShowVdbXmlDialog {
	private final TeiidMgrServiceAsync teiidMgrService = GWT.create(TeiidMgrService.class);
	private static final String WIDTH_XMLAREA = "30em";

	private MessageDialog messageDialog = new MessageDialog();
	
	private final Messages messages = GWT.create(Messages.class);

	private DialogBox showVDBXmlDialogBox = new DialogBox();
	private Button showVDBXmlDialogOKButton = new Button(messages.okButton());
	private Button showVDBXmlDialogCloseButton = new Button(messages.cancelButton());
	private Label titleLabel = new Label();
 	private TextArea vdbXmlArea = new TextArea();
	
	/**
	 * Constructor for the ShowVdbXmlDialog
	 */
	public ShowVdbXmlDialog( ) {
		init();
	}
	
	/*
	 * Init the Dialog for Displaying the VDB XML
	 */
	private void init() {
		// Create the popup Error DialogBox
		showVDBXmlDialogBox.setText(messages.dialogShowVDBXmlTitle());
		showVDBXmlDialogBox.setAnimationEnabled(true);
		
		// Dialog Box - Panel Content
		VerticalPanel vPanel = new VerticalPanel();
		vPanel.addStyleName(Resources.INSTANCE.style().showVdbXmlDialogPanel());
		
		// Message
		titleLabel.addStyleName(Resources.INSTANCE.style().labelTextBold());
		titleLabel.addStyleName(Resources.INSTANCE.style().bottomPadding10());
		vPanel.add(titleLabel);
		
		Label xmlLabel = new Label();
		xmlLabel.setText("VDB XML:");
		vPanel.add(xmlLabel);
		
		vdbXmlArea.setCharacterWidth(50);
		vdbXmlArea.setHeight(WIDTH_XMLAREA);
		vdbXmlArea.setText("");

		vPanel.add(vdbXmlArea);

		// Buttons Panel
		HorizontalPanel buttonPanel = new HorizontalPanel();
		buttonPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
		buttonPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
		showVDBXmlDialogOKButton.addStyleName(Resources.INSTANCE.style().dialogButton());
		showVDBXmlDialogCloseButton.addStyleName(Resources.INSTANCE.style().dialogButton());
		showVDBXmlDialogCloseButton.addStyleName(Resources.INSTANCE.style().rightPadding5());
		buttonPanel.add(showVDBXmlDialogCloseButton);
		buttonPanel.add(showVDBXmlDialogOKButton);
		vPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
		vPanel.add(buttonPanel);
		
		// Add the Completed Panel to the Dialog
		showVDBXmlDialogBox.setWidget(vPanel);
		
		// ---------------------------------------
		// Widgets for Show VDB Xml Dialog
		// ---------------------------------------

		// Click Handler for DialogBox Close Button
		showVDBXmlDialogCloseButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				showVDBXmlDialogBox.hide();
			}
		});
		
		// Click Handler for DialogBox OK Button
		showVDBXmlDialogOKButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				showVDBXmlDialogBox.hide();
			}
		});
		
	}
	
	/*
	 * Show the XML for the specified VDB
	 * @param vdbName the name of the VDB
	 */
	public void show(final String vdbName, final Widget relativeToWidget) {
		// Set up the callback object.
		AsyncCallback<String> callback = new AsyncCallback<String>() {
			// On Failure - show Error Dialog
			public void onFailure(Throwable caught) {
				messageDialog.showError(messages.showVdbXmlErrorTitle(), messages.showVdbXmlErrorMsg()+caught.getMessage());
			}

			// On Success - Show the DDL Dialog
			public void onSuccess(String xml) {
				titleLabel.setText(" XML for VDB: '"+vdbName+"'");
				vdbXmlArea.setText(xml);
				showVDBXmlDialogBox.showRelativeTo(relativeToWidget);
				showVDBXmlDialogCloseButton.setFocus(true);
			}
		};
		
		teiidMgrService.getVdbXml(vdbName, callback);	
	}
	
}
