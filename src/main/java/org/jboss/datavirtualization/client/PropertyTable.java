package org.jboss.datavirtualization.client;

import java.util.ArrayList;
import java.util.List;

import org.jboss.datavirtualization.client.events.PropertyChangedEvent;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.TextBoxBase;

/*
 * Property Table - extends FlexTable.  Handles Display of PropertyObj, showing DisplayName and value.
 */
public class PropertyTable extends FlexTable {
	
	private static final String PROP_COL_NAME = "Property";
	private static final String VALUE_COL_NAME = "Value";
	
	private static final String DDL_KEY = "Views-DDL";
	private static final String PASSWORD_KEY = "password";
	private static final String WIDTH_TEXTAREA = "30em";
	private static final String WIDTH_TEXTBOX = "40em";
	private static final String WIDTH_COL1 = "15em";
	
	private SimpleEventBus eventBus;
	private List<PropertyItem> properties = new ArrayList<PropertyItem>();
	private String status = "OK";

	public PropertyTable(SimpleEventBus eventBus) {
		// Adding EventBus allows us to fire events out to other components.
		this.eventBus = eventBus;
	}

	public List<PropertyItem> getProperties() {
		return properties;
	}
	
	public void setProperties(List<PropertyItem> propObjs) {
		// Reset the properties List, using the provided List.
		this.properties.clear();
		for(PropertyItem propObj: propObjs) {
			properties.add(propObj);
		}
		
		// Refresh the Table
		refreshTable();
		
		// Update Status
		updateStatus();
	}
	
	/*
	 * Clear the table
	 */
	public void clear() {
		// Reset the properties List.
		this.properties.clear();
		
		// Refresh the Table
		refreshTable();
		
		// Update Status
		updateStatus();
	}
	
	private void refreshTable() {
		// Remove Current Rows
		removeAllRows();

		// Add Header Row - Data and Styles
		setText(0,0,PROP_COL_NAME);
		setText(0,1,VALUE_COL_NAME);
		getCellFormatter().addStyleName(0, 0, Resources.INSTANCE.style().dataSourcePropertiesTableCell());
		getCellFormatter().addStyleName(0, 1, Resources.INSTANCE.style().dataSourcePropertiesTableCell());
		getRowFormatter().addStyleName(0, Resources.INSTANCE.style().dataSourcePropertiesTableHeader());

		int iRow = 1;
		// Add Property Rows, using current properties list.
		for(PropertyItem propObj: this.properties) {
			// Hidden Properties are not displayed in the table
			if(!propObj.isHidden()) {
				String propDisplayName = propObj.getDisplayName();
				String propName = propObj.getName();
				String propValue = propObj.getValue();
				boolean isRequired = propObj.isRequired();
				
				// Required Properties shown with asterisk
				if(propObj.isRequired()) {
					Label nameLabel = new Label("* "+propDisplayName);
					nameLabel.setWidth(WIDTH_COL1);
					setWidget(iRow,0,nameLabel);
				// Not required - just show the name
				} else {
					Label nameLabel = new Label(propDisplayName);
					nameLabel.setWidth(WIDTH_COL1);
					setText(iRow,0,propDisplayName);
					setWidget(iRow,0,nameLabel);
				}

				// For required Properties - the style of the name is different 
				if(isRequired) {
					getCellFormatter().addStyleName(iRow, 0, Resources.INSTANCE.style().dataSourcePropertiesTableCellItalics());
				} else {
					getCellFormatter().addStyleName(iRow, 0, Resources.INSTANCE.style().dataSourcePropertiesTableCell());
				}

				// Second Column is a TextBox or TextAreaWIDTH_COL1
				if(propName!=null && propName.equalsIgnoreCase(DDL_KEY)) {
					TextArea propValueTextArea = new TextArea();
					propValueTextArea.setCharacterWidth(50);
					propValueTextArea.setHeight(WIDTH_TEXTAREA);
					propValueTextArea.setText(propValue);
					// Add handler if modifiable, disable if not
					addTextBoxHandler(propValueTextArea, propObj, iRow,0);
					setWidget(iRow, 1, propValueTextArea);				
				} else if(propName!=null && propName.equalsIgnoreCase(PASSWORD_KEY)) {
					PasswordTextBox propValueTextBox = new PasswordTextBox();
					propValueTextBox.setWidth(WIDTH_TEXTBOX);
					propValueTextBox.setText(propValue);
					// Add handler if modifiable, disable if not
					addTextBoxHandler(propValueTextBox, propObj, iRow,0);
					setWidget(iRow, 1, propValueTextBox);				
				} else {
					PropertyValueTextBox propValueTextBox = new PropertyValueTextBox();
					propValueTextBox.setWidth(WIDTH_TEXTBOX);
					propValueTextBox.setText(propValue);
					// Add handler if modifiable, disable if not
					addTextBoxHandler(propValueTextBox, propObj, iRow,0);
					setWidget(iRow, 1, propValueTextBox);				
				}

				// Row Style
				boolean isEven = (iRow % 2 == 0);
				if(isEven) {
					getRowFormatter().addStyleName(iRow, Resources.INSTANCE.style().dataSourcePropertiesTableEvenRow());
				} else {
					getRowFormatter().addStyleName(iRow, Resources.INSTANCE.style().dataSourcePropertiesTableOddRow());
				}
				iRow++;
			}
		}
	}
	
	/*
	 * Helper method - adds Handler to TextBox widgets if modifiable.  otherwise disables it.
	 */
	private void addTextBoxHandler(final TextBoxBase tBox, final PropertyItem propObj, int iRow, int iCol) {
		// TODO: isModifiable is wrong for some properties.  Set 'true' for now...
		//boolean isModifiable = propObj.isModifiable();
		boolean isModifiable = true;

		// Modifiable - add listener
		if(isModifiable) {
			tBox.addValueChangeHandler(new ValueChangeHandler<String>() {

		        public void onValueChange(ValueChangeEvent<String> event) {
					propObj.setValue(event.getValue());
					updateStatus();
					firePropertyChanged();
		        }

		    });
			tBox.addKeyUpHandler(new KeyUpHandler() {
				public void onKeyUp(KeyUpEvent event) {
					propObj.setValue(tBox.getText());
					updateStatus();
					firePropertyChanged();
				}
			});
		// Not modifiable - change background
		} else {
			tBox.setEnabled(false);
			getCellFormatter().addStyleName(iRow, 0, Resources.INSTANCE.style().dataSourcePropertiesTableCellUnmodifiable());
		}
	}
	
	/*
	 * Fire a notification that a property has changed.
	 */
	private void firePropertyChanged() {
		this.eventBus.fireEvent(new PropertyChangedEvent());
	}
	
	/*
	 * Update the current Property Error Status.  If properties pass, the status is 'OK'.  If not, a
	 * String identifying the problem is set.
	 */
	private void updateStatus() {
		// Assume 'OK' until a problem is found
		this.status = "OK";
		
		// Iterate current Property list, verify that the required properties have a value
		for(PropertyItem propObj: this.properties) {
			String propName = propObj.getName();
			String propValue = propObj.getValue();
			boolean isRequired = propObj.isRequired();
			
			// Check that required properties have a value
			if(isRequired) {
				if(propValue==null || propValue.trim().length()==0) {
					this.status = "No value is entered for property: '"+propName+"'";
					break;
				}
			}
		}
	}

	/*
	 * Get the current Property Error Status.  
	 */
	public String getStatus() {
		return this.status;
	}

	private class PropertyValueTextBox extends TextBox {

	    public PropertyValueTextBox() {
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
	                        ValueChangeEvent.fire(PropertyValueTextBox.this, getText());
	                    }

	                });
	                break;
	        }
	    }
	}

}
