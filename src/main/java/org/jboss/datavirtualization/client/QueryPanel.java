package org.jboss.datavirtualization.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.datavirtualization.client.dialogs.MessageDialog;
import org.jboss.datavirtualization.client.rpc.TeiidMgrServiceAsync;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class QueryPanel extends Composite {

	private static QueryPanelUiBinder uiBinder = GWT.create(QueryPanelUiBinder.class);

	interface QueryPanelUiBinder extends UiBinder<Widget, QueryPanel> {
	}

	private final Messages messages = GWT.create(Messages.class);
	// EventBus and TeiidMgrService
	private final TeiidMgrServiceAsync teiidMgrService;

	// Within the owner class for the UiBinder template
	@UiField Resources res;

    @UiField TextArea sqlTextArea;
    @UiField ScrollPanel resultsTableScrollPanel;
    @UiField ListBox datasourceListBox;
    @UiField ListBox tablesListBox;
    @UiField CheckBox onlyVdbsCheckBox;
    @UiField Button submitButton;

	@UiField Label resultsAreaLabel;
	@UiField Label tablesLoadStatusLabel;
	
	@UiField ScrollPanel columnsTableScrollPanel;
    @UiField Label dummyLabel;

	private MessageDialog messageDialog = new MessageDialog();

	private DialogBox xmlDialogBox = new DialogBox();
	private Button xmlDialogCloseButton = new Button(messages.closeButton());
	private TextArea xmlDataTextArea = new TextArea();
	
	private Map<String,List<String>> tableColMap = new HashMap<String,List<String>>();
	private Map<String,String> tableTypeMap = new HashMap<String,String>();

	private FlexTable resultsTable = new FlexTable();
	private FlexTable columnsTable = new FlexTable();
	
	/*
	 * Constructor for the Panel
	 */
	public QueryPanel(SimpleEventBus eventBus, TeiidMgrServiceAsync teiidMgrService, int panelHeight) {
		res.style().ensureInjected();
		initWidget(uiBinder.createAndBindUi(this));

		this.teiidMgrService = teiidMgrService;
		initComponents(panelHeight);
		
		// Establish panel height
		dummyLabel.setHeight(String.valueOf(panelHeight)+"px");		
	}
	
	@UiHandler("datasourceListBox")
	void onDatasourceListBoxChange(ChangeEvent event) {
		int selectedIndex = datasourceListBox.getSelectedIndex();
		String selectedDS = datasourceListBox.getValue(selectedIndex);
		refreshTablesListBox(selectedDS);
	}
	
	@UiHandler("tablesListBox")
	void onTablesListBoxChange(ChangeEvent event) {
		int selectedIndex = tablesListBox.getSelectedIndex();
		String selectedTable = tablesListBox.getValue(selectedIndex);
		refreshColumnsTable(selectedTable);
		refreshSQLTextArea();
		setResultsDisplay_NoResults(messages.resultsLabelNoRows());
		submitButton.setEnabled(true);
	}
	
	@UiHandler("onlyVdbsCheckBox")
	void onOnlyVdbsCheckBoxClick(ClickEvent e) {
  	  refreshDatasourceListBox();
	}
	
	@UiHandler("submitButton")
	void onSubmitButtonClick(ClickEvent e) {
		doSubmit();
	}

	/*
	 * Init Components
	 */
	private void initComponents(int panelHeight) {
	    onlyVdbsCheckBox.setText(messages.onlyVDBsCheckBox());
	    onlyVdbsCheckBox.setValue(false);
	    onlyVdbsCheckBox.addStyleName(res.style().titleSmall());

		tablesLoadStatusLabel.addStyleName(res.style().queryPanelLoadStatusLabel());

		// SQL Text Area
		sqlTextArea.setCharacterWidth(80);
		sqlTextArea.setHeight("150px");
		
		// Columns Table style
		columnsTable.addStyleName(res.style().sqlResultsTable());
		columnsTableScrollPanel.setWidget(columnsTable);
		columnsTableScrollPanel.setHeight("150px");
		
		// Results Table style
		resultsTable.addStyleName(res.style().sqlResultsTable());
		resultsTableScrollPanel.setWidget(resultsTable);
		// Set results scroll area based on overall panel dimensions
		int resultsHeight=panelHeight-410; 
		int resultsWidth=Window.getClientWidth()-70; 
		resultsTableScrollPanel.setHeight(String.valueOf(resultsHeight)+"px");
		resultsTableScrollPanel.setWidth(String.valueOf(resultsWidth)+"px");

		// Init Dialogs for later use.
		initXmlDialog();

		// Refresh the DataSource listBox.  This selects first item,
		// and fires event to refresh the tables box and SQL Area
		refreshDatasourceListBox();
	}
	
	/**
	 * Refresh the panel
	 */
	public void refresh() {
		refreshDatasourceListBox();
	}
	
	/*
	 * Refresh the Datasources ListBox.  Populates the List with all
	 * available jdbc sources and selects the first item
	 */
	private void refreshDatasourceListBox() {
		// Set up the callback object.
		AsyncCallback<String[]> callback = new AsyncCallback<String[]>() {
			// On Failure - show Error Dialog
			public void onFailure(Throwable caught) {
				// Show Error Dialog
				messageDialog.showError(messages.datasourceRefreshErrorTitle(),messages.datasourceRefreshErrorMsg());
			}

			// On Success - Populate the ListBox with Datasource Names
			public void onSuccess(String[] result) {
				List<String> datasourceList = Arrays.asList(result);
				Collections.sort(datasourceList);

				// Populate the Datasource ListBox
				if(datasourceListBox.getItemCount()>0) datasourceListBox.clear();
				int i=0;
				for(String longDSName: datasourceList) {
					datasourceListBox.insertItem(longDSName, i);
					i++;
				}
				// Add Placeholder if no sources
				if(datasourceListBox.getItemCount()==0) {
					datasourceListBox.insertItem("NO SOURCES", 0);
				} 
				datasourceListBox.setSelectedIndex(0);
				DomEvent.fireNativeEvent(Document.get().createChangeEvent(),datasourceListBox);				
			}
		};

		// Make the Remote Server call to init the ListBox
		teiidMgrService.getAllDataSourceNames(onlyVdbsCheckBox.getValue().booleanValue(), callback);
	}

	/*
	 * Refresh the Tables ListBox.  Populates the List with all tables for
	 * the supplied dataSource, and sets the SQL Area Text
	 */
	private void refreshTablesListBox(String datasourceName) {
		//submitButton.setEnabled(false);

		// Set up the callback object.
		AsyncCallback<Map<String,List<String>>> callback = new AsyncCallback<Map<String,List<String>>>() {
			// On Failure - show Error Dialog
			public void onFailure(Throwable caught) {
				tablesLoadStatusLabel.setText("");
				
				tableTypeMap.clear();
				
				// Show Error Dialog
				messageDialog.showError(messages.tablesRefreshErrorTitle(),messages.tablesRefreshErrorMsg());

				submitButton.setEnabled(true);
			}

			// On Success - Populate the Table ListBox
			public void onSuccess(Map<String,List<String>> result) {				
				// Reset the Table-Column Map for later use.
				tableColMap = result;
				
				// Populate the tables ListBox
				//if(tablesListBox.getItemCount()>0) tablesListBox.clear();
				
				List<String> tableList = new ArrayList<String>();
				tableTypeMap.clear(); 
				// Remove the TABLE / PROC designators and keep track of type
				Set<String> tableProcKeys = tableColMap.keySet();
				Iterator<String> keyIter = tableProcKeys.iterator();
				while(keyIter.hasNext()) {
					String tableProcKey = keyIter.next();
					int vBarIndx = tableProcKey.indexOf('|');
					String name = tableProcKey.substring(0, vBarIndx);
					String type = tableProcKey.substring(vBarIndx+1);
					// Add Table - Proc Name to List
					tableList.add(name);
					// Keep track of whether it is a Table or Procedure
					tableTypeMap.put(name,type);
				}
				
				Collections.sort(tableList);
				
				int i=0;
				for(String tblName: tableList) {
					tablesListBox.insertItem(tblName, i);
					i++;
				}
				// Init selection to first value
				if(tablesListBox.getItemCount()>0) tablesListBox.setSelectedIndex(0);
				
				// Refresh the Columns Table
				int selectedIndex = tablesListBox.getSelectedIndex();
				String selectedTable = tablesListBox.getValue(selectedIndex);
				refreshColumnsTable(selectedTable);
				
				// Refresh SQL Area
				refreshSQLTextArea();
				setResultsDisplay_NoResults(messages.resultsLabelNoRows());
				
				submitButton.setEnabled(true);
				tablesLoadStatusLabel.setText("");
			}
		};

		// Prior to refresh, 
		//   - clear the available Tables ListBox
		//   - clear the available columns Table
		//   - Display 'Loading Tables...' message
		if(tablesListBox.getItemCount()>0) tablesListBox.clear();
		clearColumnsTable();
		tablesLoadStatusLabel.setText(messages.tablesLoadStatusMsg());

		// Make the remote server call.
		teiidMgrService.getTableAndColMap(datasourceName, callback);
	}
	
	/*
	 * Refresh the Columns Table.  Get Column info from the tableColMap
	 * and repopulate the table.
	 */
	private void refreshColumnsTable(String tableName) {
		String type = tableTypeMap.get(tableName);
		String key = null;
		if(type!=null) {
			key = tableName + "|" + type;
		} else {
			key = tableName + "|TABLE";
		}

		// Results for the selected table
		List<String> resultList = tableColMap.get(key);

		// Create List of Table Rows
		List<List<String>> rowList = new ArrayList<List<String>>();

		// Add Header Row
		List<String> headerRow = new ArrayList<String>();
		headerRow.add("Column Name");
		headerRow.add("Column Type");
		rowList.add(headerRow);
		
		// Parse out column name and type, and add row.
		// The column name may not have an associated type.  If not, set the type to "unk"
		for(String nameTypeStr: resultList) {
			List<String> dataRow = new ArrayList<String>();
			// name and type are separated by a '|' - (may not have a type)
			int indx = nameTypeStr.indexOf("|");
			String colName = null;
			String colType = null;
			// Delimiter found - set the name and type
			if(indx!=-1) {
				colName = nameTypeStr.substring(0,indx);
				colType = nameTypeStr.substring(indx+1);
		    // Delimiter not found - use entire string for name & set type to 'unk'
			} else {
				colName = nameTypeStr;
				colType = "unknown";
			}
			dataRow.add(colName);
			dataRow.add(colType);
			rowList.add(dataRow);
		}
		
		// Populate the Table with the new rows
		populateColumnsTable(rowList);
	}

	/*
	 * Refresh the SQL Text area, using the current Tables
	 * ListBox item to generate "SELECT * FROM <Table>"
	 * the supplied dataSource, and sets the SQL Area Text
	 */
	private void refreshSQLTextArea( ) {
		// Get the Table.  If there is a selected table, use it.
		// Otherwise, get the first item in the list.
		String selectedTable = null;
		int tableIndex = tablesListBox.getSelectedIndex();
		if(tablesListBox.getSelectedIndex()>=0) {
			selectedTable = tablesListBox.getItemText(tableIndex);
		} else {
			selectedTable = tablesListBox.getItemText(0);
		}
		
		// Determine if the selection is a Table or Procedure
		String type = tableTypeMap.get(selectedTable);
		StringBuffer sb = new StringBuffer();
		if(type==null || type.equalsIgnoreCase("TABLE")) {
			sb.append("SELECT * FROM ");
			if(!selectedTable.equalsIgnoreCase("NO TABLES OR PROCS")) {
				sb.append(selectedTable);
			}
		} else if(type.equalsIgnoreCase("PROC")) {
			sb.append("SELECT * FROM (EXEC ");
			sb.append(selectedTable);
			sb.append("(<params>)) AS Result");
		}

		sqlTextArea.setText(sb.toString());
	}

	/*
	 * Handler for Submit Button Pressed
	 */
	private void doSubmit() {
		// Get the selected source
		int srcIndex = datasourceListBox.getSelectedIndex();
		String selectedSource = datasourceListBox.getValue(srcIndex);

		// Get SQL
		String sql = sqlTextArea.getText();

		// Set up the callback object.
		AsyncCallback<List<List<DataItem>>> callback = new AsyncCallback<List<List<DataItem>>>() {
			// On Failure - show Error Dialog
			public void onFailure(Throwable caught) {
				String msg = messages.serverErrorMsg();
				if(caught instanceof SQLProcException) {
					msg = ((SQLProcException)caught).getSqlDetail();
				}
				// Show Error Dialog
				messageDialog.showError(messages.querySubmittalErrorTitle(),msg);
				
				setResultsDisplay_NoResults(messages.resultsLabelNoRows());
				submitButton.setEnabled(true);
			}

			// On Success - Populate the ListBox
			public void onSuccess(List<List<DataItem>> rowData) {
				populateResultsTable(rowData);
				submitButton.setEnabled(true);
			}
		};
		
		submitButton.setEnabled(false);
		setResultsDisplay_NoResults(messages.resultsLabelFetchingRows());
		
		teiidMgrService.executeSql(selectedSource, sql, callback);
	}
	
	/*
	 * Clear the Results Table and re-populate it with the supplied data
	 */
	private void populateResultsTable(List<List<DataItem>> rowData) {
		// Clear Previous Results
		clearResultsTable();

		int iRow = 0;
		for(List<DataItem> row: rowData) {
			int nCols = row.size();
			for(int i=0; i<nCols; i++) {
				DataItem data = (DataItem)row.get(i);
				if(data.getType().contains("xml")) {
					final String xmlData = data.getData();
					// Show XML Button
					Button xmlButton = new Button();
					xmlButton.setText(messages.xmlButton());
					// Click Handlers for Submit and Cancel
					xmlButton.addClickHandler(new ClickHandler() {
						public void onClick(ClickEvent event) {
							showXMLDialog(messages.xmlResultDialogTitle(),xmlData);
						}
					});
					resultsTable.setWidget(iRow, i, xmlButton);
				} else {
					resultsTable.setText(iRow,i,data.getData());
				}
			}
			
			// Header Row Style
			if(iRow==0) {
				resultsTable.getRowFormatter().addStyleName(iRow, res.style().sqlResultsTableHeader());
		    // Even Row Style
			} else {
				boolean isEven = (iRow % 2 == 0);
				if(isEven) {
					resultsTable.getRowFormatter().addStyleName(iRow, res.style().sqlResultsTableEvenRow());
				} else {
					resultsTable.getRowFormatter().addStyleName(iRow, res.style().sqlResultsTableOddRow());
				}
			}
			iRow++;
		}
		resultsAreaLabel.setText("Query Results - " + (rowData.size()-1) +" Rows");
		
	}
	
	/*
	 * Set Results Display - No Results
	 * @param labelText the text to display for the panel label
	 */
	private void setResultsDisplay_NoResults(String labelText) {
		// Clear Previous Results
		clearResultsTable();

		// Simple message with no results
		resultsTable.setText(0,0,"No Results to Display");
		resultsTable.getRowFormatter().addStyleName(0, res.style().sqlResultsTableHeader());
		
		resultsAreaLabel.setText(labelText);
	}
	
	/*
	 * Clear the Results Table and re-populate it with the supplied data
	 */
	private void populateColumnsTable(List<List<String>> rowData) {
		// Clear Previous Results
		clearColumnsTable();

		int iRow = 0;
		for(List<String> row: rowData) {
			int nCols = row.size();
			for(int i=0; i<nCols; i++) {
				columnsTable.setText(iRow,i,row.get(i));
			}
			
			// Header Row Style
			if(iRow==0) {
				columnsTable.getRowFormatter().addStyleName(iRow, res.style().sqlResultsTableHeader());
		    // Even Row Style
			} else {
				boolean isEven = (iRow % 2 == 0);
				if(isEven) {
					columnsTable.getRowFormatter().addStyleName(iRow, res.style().sqlResultsTableEvenRow());
				} else {
					columnsTable.getRowFormatter().addStyleName(iRow, res.style().sqlResultsTableOddRow());
				}
			}
			iRow++;
		}
		
	}
	
	/*
	 * Clear the Results Table 
	 */
	private void clearColumnsTable( ) {
		columnsTable.removeAllRows();
	}

	/*
	 * Clear the Results Table 
	 */
	private void clearResultsTable( ) {
		resultsTable.removeAllRows();
	}

	/*
	 * Init the Dialog for Error Display
	 */
	private void initXmlDialog() {
		// We can set the id of a widget by accessing its Element
		xmlDialogCloseButton.getElement().setId("closeButton");
		VerticalPanel dialogVPanel = new VerticalPanel();
		dialogVPanel.addStyleName(res.style().sqlXmlDialogPanel());
		xmlDataTextArea.setCharacterWidth(45);
		xmlDataTextArea.setHeight("200px");

		dialogVPanel.add(xmlDataTextArea);
		dialogVPanel.setHorizontalAlignment(VerticalPanel.ALIGN_RIGHT);
		dialogVPanel.add(xmlDialogCloseButton);
		xmlDialogBox.setWidget(dialogVPanel);
		
		// Add a handler to close the DialogBox
		xmlDialogCloseButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				xmlDialogBox.hide();
			}
		});
	}
	
	/*
	 * Show the Dialog for XML Result Display
	 */
	private void showXMLDialog(String title,String xmlData) {
		// Dialog Title
		xmlDialogBox.setText(title);
		// Dialog Message
		xmlDataTextArea.setText(xmlData);
		xmlDialogBox.center();
		xmlDialogCloseButton.setFocus(true);
	}

}
