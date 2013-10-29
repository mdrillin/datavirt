package org.jboss.datavirtualization.client.dialogs;

import org.jboss.datavirtualization.client.Messages;
import org.jboss.datavirtualization.client.Resources;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/*
 * Confirm Dialog for use in VdbManager
 */
public class ConfirmDialog {

	private final Messages messages = GWT.create(Messages.class);

	// Message Dialog Controls
	private DialogBox dialogBox = new DialogBox();
	private Button dialogCloseButton = new Button(messages.closeButton());
	private Button dialogOKButton = new Button(messages.okButton());
	private HTML message = new HTML();

	/*
	 * Constructor for the ConfirmDialog
	 */
	public ConfirmDialog() {
	}
	
	/*
	 * Show the ConfirmDialog with the provided title and message
	 * @param dialogTitle the dialog title
	 * @param messageHtml the dialog message (can use HTML tags to format)
	 * @param okHandler the clickHandler for OK Button
	 */
	public void showConfirm(String dialogTitle, String messageHtml, ClickHandler okHandler) {
		showConfirm(dialogTitle,messageHtml,okHandler,null);
	}
	
	/*
	 * Show the ConfirmDialog with the provided title and message
	 * @param dialogTitle the dialog title
	 * @param messageHtml the dialog message (can use HTML tags to format)
	 * @param okHandler the clickHandler for OK Button
	 * @param relativeToWidget the location of the dialog relative to
	 */
	public void showConfirm(String dialogTitle, String messageHtml, ClickHandler okHandler, Widget relativeToWidget) {
		// Dialog Title
		dialogBox.setText(dialogTitle);
		dialogBox.setAnimationEnabled(true);
		dialogCloseButton.getElement().setId("closeButton");
		
		VerticalPanel dialogVPanel = new VerticalPanel();
		dialogVPanel.addStyleName(Resources.INSTANCE.style().messageDialogPanel());
		dialogVPanel.add(message);
		message.setHTML(messageHtml);
		message.setStyleName(Resources.INSTANCE.style().messageDialogMessage());
		message.addStyleName(Resources.INSTANCE.style().bottomPadding10());
		dialogVPanel.setHorizontalAlignment(VerticalPanel.ALIGN_RIGHT);
		
		
		// Buttons Panel
		HorizontalPanel buttonPanel = new HorizontalPanel();
		buttonPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
		buttonPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
		dialogOKButton.addStyleName(Resources.INSTANCE.style().dialogButton());
		dialogOKButton.addClickHandler(okHandler);
		
		dialogCloseButton.addStyleName(Resources.INSTANCE.style().dialogButton());
		dialogCloseButton.addStyleName(Resources.INSTANCE.style().rightPadding5());
		buttonPanel.add(dialogCloseButton);
		buttonPanel.add(dialogOKButton);
		
		dialogVPanel.add(buttonPanel);

		dialogBox.setWidget(dialogVPanel);

		// Add a handler to close the DialogBox
		dialogCloseButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				dialogBox.hide();
			}
		});
		
		if(relativeToWidget!=null) {
			dialogBox.showRelativeTo(relativeToWidget);
		} else {
			dialogBox.center();
		}
		dialogCloseButton.setFocus(true);
	}
	
	public void hide() {
		dialogBox.hide();
	}

}
