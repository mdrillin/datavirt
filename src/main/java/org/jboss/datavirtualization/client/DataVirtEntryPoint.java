package org.jboss.datavirtualization.client;


import java.util.List;

import org.jboss.datavirtualization.client.rpc.TeiidMgrService;
import org.jboss.datavirtualization.client.rpc.TeiidMgrServiceAsync;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class DataVirtEntryPoint implements EntryPoint {
	
	// 'RunningOnOpenShift' flag
    private boolean isRunningOnOpenShift = false;

	/**
	 * Create a remote service proxy to talk to the server-side Teiid service.
	 */
	private final TeiidMgrServiceAsync teiidMgrService = GWT.create(TeiidMgrService.class);

	private final Messages messages = GWT.create(Messages.class);

	private TabPanel tabPanel = new TabPanel();
	// DataSource Panel
	private DataSourcePanel dsPanel;
	// DataSources Panel
	private VDBsPanel vdbsPanel;
	// Query Panel
	private QueryPanel queryPanel;

	private VerticalPanel appPanel;
	// Login Panel
	private LoginPanel loginPanel;
	
	// DeckPanel - for swap between Login and Application
	private DeckPanel deckPanel = new DeckPanel();
	
	// Event Bus for communication between components
	private final SimpleEventBus eventBus = new SimpleEventBus();
	
	private boolean isLoggedIn = false;

	/**
	 * Entry Point method
	 */
	public void onModuleLoad() {

		Resources.INSTANCE.style().ensureInjected();
		
		// Makes server call determine if OpenShift, inits the UI
		initApp();

	}

	/**
	 * Initialize the UI
	 */
	private void initUI() {
		// ---------------------
		// Get Window Size
		// ---------------------
	    int windowHeight=Window.getClientHeight(); 
		int windowWidth=Window.getClientWidth(); 

		// Create application tabbed panel and login panel
		appPanel = createAppPanel(windowHeight-150,windowWidth-50);
		loginPanel = createLoginPanel(windowHeight-15,windowWidth-15);
		// Initial values for login
	    loginPanel.init(9999,"admin","");
		
	    // Add Panels to deck
	    deckPanel.add(loginPanel);
	    deckPanel.add(appPanel);
	    RootPanel.get("deckPanelContainer").add(deckPanel);

	    // If user is logged in, show the application
	    if(isLoggedIn) {
	    	deckPanel.showWidget(1);
	    // User is not logged in, show login panel
	    } else {
	    	deckPanel.showWidget(0);
	    }
	    		
	}
	
	/**
	 * Create the LoginPanel
	 * @param windowHeight
	 * @param windowWidth
	 * @return the LoginPanel
	 */
	private LoginPanel createLoginPanel(int windowHeight, int windowWidth) {
	    LoginPanel loginPanel = new LoginPanel(windowHeight,this);
		String widthPx = String.valueOf(windowWidth)+"px";
	    loginPanel.setWidth(widthPx);
	    return loginPanel;
	}
	
	/**
	 * Create the application panel
	 * @param windowHeight
	 * @param windowWidth
	 * @return the application panel
	 */
	private VerticalPanel createAppPanel(int windowHeight, int windowWidth) {
		VerticalPanel appPanel = new VerticalPanel();
		
	    dsPanel = new DataSourcePanel(this.eventBus,isRunningOnOpenShift,this.teiidMgrService,windowHeight);
	    tabPanel.add(dsPanel, "Data Sources");
	    vdbsPanel = new VDBsPanel(this.eventBus,isRunningOnOpenShift,this.teiidMgrService,dsPanel,windowHeight);
	    tabPanel.add(vdbsPanel, "VDBs");
	    queryPanel = new QueryPanel(this.eventBus,this.teiidMgrService,windowHeight);
	    tabPanel.add(queryPanel, "Query");	    

	    // Set Overall Panel Widths
		String widthPx = String.valueOf(windowWidth)+"px";
		tabPanel.setWidth(widthPx);
		tabPanel.getTabBar().addStyleName(Resources.INSTANCE.style().myTabBar());
		
		// Show the 'Data Sources' tab initially.
	    tabPanel.selectTab(0);
	    
	    tabPanel.addSelectionHandler(new SelectionHandler<Integer>(){
	    	  public void onSelection(SelectionEvent<Integer> event){
	    		  int tabIndex = tabPanel.getTabBar().getSelectedTab();
	    		  if(tabIndex==0) {
	    			  dsPanel.refresh();
	    		  } else if(tabIndex==1) {
	    			  vdbsPanel.refresh();
	    		  } else if(tabIndex==2) {
	    			  queryPanel.refresh();
	    		  }
	    	 }
	    	});

        // App Banner
	    VerticalPanel imagePanel = new VerticalPanel();
	    Image dvImage = new Image(Resources.INSTANCE.dataVirtProductName());
	    imagePanel.add(dvImage);
	    imagePanel.setStyleName(Resources.INSTANCE.style().paddingBannerImage());
	    appPanel.add(imagePanel);

		// Add TabPanel
		tabPanel.setStyleName(Resources.INSTANCE.style().paddingTabPanel());
	    appPanel.add(tabPanel);

	    return appPanel;
	}
			
	/**
	 * Perform login to admin api and update the application.
	 * @param serverPort the server management port number
	 * @param userName the username
	 * @param password the password
	 */
	public void doAdminLogin(int serverPort, String userName, String password) {
		// Set up the callback object.
		AsyncCallback<List<DataItem>> callback = new AsyncCallback<List<DataItem>>() {
			// On Failure - show Error Dialog
			public void onFailure(Throwable caught) {
				loginPanel.setStatusMessage(messages.initAppErrorMsg());
			}

			// On Success - Populate the ListBox with Datasource Names
			public void onSuccess(List<DataItem> vdbItems) {
				isLoggedIn = true;
				// Show TabPanel
				deckPanel.showWidget(1);
				// Populate app panels
				vdbsPanel.populateVDBListBox(vdbItems,null);
			}
		};

		// Make the Remote Server call to init the ListBox
		teiidMgrService.initApplication(serverPort, userName, password, callback);
	}
	
	/*
	 * Makes server call to determine if OpenShift and inits the UI
	 */
	private void initApp() {
		// Set up the callback object.
		AsyncCallback<Boolean> callback = new AsyncCallback<Boolean>() {
			// On Failure - show Error Dialog
			public void onFailure(Throwable caught) {
				isRunningOnOpenShift=false;
				initUI();
			}

			// On Success - Populate the ListBox with Datasource Names
			public void onSuccess(Boolean isOpenShift) {
				isRunningOnOpenShift=isOpenShift.booleanValue();
				initUI();
			}
		};

		// Make the Remote Server call to init the ListBox
		teiidMgrService.isRunningOnOpenShift(callback);
	}
  
}
