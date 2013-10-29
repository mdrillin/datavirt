package org.jboss.datavirtualization.client.dialogs;

import org.jboss.datavirtualization.client.Messages;
import org.jboss.datavirtualization.client.Resources;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/*
 * Message Dialog for use in VdbManager
 */
public class MessageDialog {

	private final Messages messages = GWT.create(Messages.class);

	// Message Dialog Controls
	private DialogBox dialogBox = new DialogBox();
	private Button dialogCloseButton = new Button(messages.dismissButton());
	private HTML message = new HTML();

	/*
	 * Constructor for the MessageDialog
	 */
	public MessageDialog( ) {
		init();
	}
	
	/*
	 * Init the Dialog
	 */
	private void init() {
		// Create the popup DialogBox
		dialogBox.setAnimationEnabled(true);
		
		// We can set the id of a widget by accessing its Element
		dialogCloseButton.getElement().setId("closeButton");
		VerticalPanel dialogVPanel = new VerticalPanel();
		dialogVPanel.addStyleName(Resources.INSTANCE.style().messageDialogPanel());
		dialogVPanel.add(message);
		dialogVPanel.setHorizontalAlignment(VerticalPanel.ALIGN_RIGHT);
		dialogVPanel.add(dialogCloseButton);
		dialogBox.setWidget(dialogVPanel);

		// Add a handler to close the DialogBox
		dialogCloseButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				dialogBox.hide();
			}
		});
	}

	public void showError(String dialogTitle, String messageHtml) {
		showError(dialogTitle,messageHtml,null);
	}
	
	/*
	 * Show the MessageDialog with the provided title and error message
	 * @param dialogTitle the dialog title
	 * @param messageHtml the dialog message (can use HTML tags to format)
	 */
	public void showError(String dialogTitle, String messageHtml, Widget relativeToWidget) {
		// Dialog Title
		dialogBox.setText(dialogTitle);
		
		// Dialog Text
		message.setHTML(messageHtml);
		message.setStyleName(Resources.INSTANCE.style().messageDialogErrorMessage());
		
		if(relativeToWidget!=null) {
			dialogBox.showRelativeTo(relativeToWidget);
		} else {
			dialogBox.center();
		}
		dialogCloseButton.setFocus(true);
	}
	
	public void showMessage(String dialogTitle, String messageHtml) {
		showMessage(dialogTitle,messageHtml,null);
	}
	
	/*
	 * Show the MessageDialog with the provided title and message
	 * @param dialogTitle the dialog title
	 * @param messageHtml the dialog message (can use HTML tags to format)
	 */
	public void showMessage(String dialogTitle, String messageHtml, Widget relativeToWidget) {
		// Dialog Title
		dialogBox.setText(dialogTitle);
		
		// Dialog Text
		message.setHTML(messageHtml);
		message.setStyleName(Resources.INSTANCE.style().messageDialogMessage());
		
		if(relativeToWidget!=null) {
			dialogBox.showRelativeTo(relativeToWidget);
		} else {
			dialogBox.center();
		}
		dialogCloseButton.setFocus(true);
	}

}
