package org.jboss.datavirtualization.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class LoginPanel extends Composite {

	private static LoginPanelUiBinder uiBinder = GWT.create(LoginPanelUiBinder.class);

	interface LoginPanelUiBinder extends UiBinder<Widget, LoginPanel> {
	}
	
	private final Messages messages = GWT.create(Messages.class);

	// Within the owner class for the UiBinder template
	@UiField Resources res;

	@UiField Button loginButton;
	@UiField Label loginStatusLabel;
    @UiField TextBox adminPortTextBox;
    @UiField TextBox adminUsernameTextBox;
    @UiField PasswordTextBox adminPasswordTextBox;
    @UiField Label dummyLabel;

	private DataVirtEntryPoint datavirt;
	
	/*
	 * Constructor for the Panel
	 */
	public LoginPanel(int panelHeight,DataVirtEntryPoint datavirt) {
		res.style().ensureInjected();
		
		initWidget(uiBinder.createAndBindUi(this));

		this.datavirt = datavirt;
		
		// Establish panel height
		dummyLabel.setHeight(String.valueOf(panelHeight)+"px");		
		dummyLabel.setWidth("1px");
	}
	
	@UiHandler("adminUsernameTextBox")
	void onAdminUsernameTextBoxKeyUp(KeyUpEvent event) {
		validateEntries();
	}
	
	@UiHandler("adminPasswordTextBox")
	void onAdminPasswordTextBoxKeyUp(KeyUpEvent event) {
		validateEntries();
	}
	
	@UiHandler("adminPortTextBox")
	void onAdminPortTextBoxKeyUp(KeyUpEvent event) {
		validateEntries();
	}
	
	@UiHandler("loginButton")
	void onLoginButtonClick(ClickEvent e) {
		String portText = getPortText();
		String username = getUsername();
		String password = getPassword();

		datavirt.doAdminLogin(Integer.parseInt(portText), username, password);
	}
	
	/*
	 * Init the Panel
	 */
	public void init(int port, String user, String password) {
		adminPortTextBox.setText(String.valueOf(port));

		if(user==null || user.trim().isEmpty()) {
			adminUsernameTextBox.setText("");
		} else {
			adminUsernameTextBox.setText(user);
		}
		if(password==null || password.trim().isEmpty()) {
			adminPasswordTextBox.setText("");
		} else {
			adminPasswordTextBox.setText(password);
		}
		// Validate initial entries
		validateEntries();
		
		adminUsernameTextBox.setFocus(true);
	}
	
	public void setStatusMessage(String status) {
		this.loginStatusLabel.setText(status);
	}
	
	/**
	 * Get the Admin API port
	 * @return the admin api port
	 */
	public String getPortText() {
		return this.adminPortTextBox.getText();
	}
	
	/**
	 * Get the Admin API user
	 * @return the admin api user
	 */
	public String getUsername() {
		return this.adminUsernameTextBox.getText();
	}
	
	/**
	 * Get the Admin API password
	 * @return the admin api password
	 */
	public String getPassword() {
		return this.adminPasswordTextBox.getText();
	}
	
	/*
	 * Validate the entries and return status.  The status message label is also updated.
	 * @return the status.  'true' if entries are valid, 'false' otherwise.
	 */
	private void validateEntries( ) {
		boolean statusOK = true;
		String statusStr = "OK";
		
		// Validate the entered Port
		String port = getPortText();
		if(port==null || port.trim().length()==0) {
			statusStr = messages.statusEnterLoginPort();
			statusOK = false;
		}
		
		// Validate the entered username
		if(statusOK) {
			String user = getUsername();
			if(user==null || user.trim().length()==0) {
				statusStr = messages.statusEnterLoginUsername();
				statusOK = false;
			}
		}
		
		// Validate the entered password
		if(statusOK) {
			String pass = getPassword();
			if(pass==null || pass.trim().length()==0) {
				statusStr = messages.statusEnterLoginPassword();
				statusOK = false;
			}
		}
		
		// Update the status label
		if(!statusOK) {
			loginStatusLabel.setText(statusStr);
    		loginButton.setEnabled(false);
		} else {
			loginStatusLabel.setText(messages.statusClickOKToAccept());
    		loginButton.setEnabled(true);
		}
	}
}
