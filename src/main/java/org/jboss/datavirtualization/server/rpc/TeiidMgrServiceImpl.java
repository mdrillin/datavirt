package org.jboss.datavirtualization.server.rpc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.sql.rowset.serial.SerialBlob;

import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.jboss.datavirtualization.client.DataItem;
import org.jboss.datavirtualization.client.PropertyItem;
import org.jboss.datavirtualization.client.SQLProcException;
import org.jboss.datavirtualization.client.dialogs.AddEditModelDialog.ModelType;
import org.jboss.datavirtualization.client.rpc.TeiidMgrService;
import org.jboss.datavirtualization.client.rpc.TeiidServiceException;
import org.teiid.adminapi.Admin;
import org.teiid.adminapi.AdminException;
import org.teiid.adminapi.AdminFactory;
import org.teiid.adminapi.Model;
import org.teiid.adminapi.PropertyDefinition;
import org.teiid.adminapi.Translator;
import org.teiid.adminapi.VDB;
import org.teiid.adminapi.VDB.Status;
import org.teiid.adminapi.impl.ModelMetaData;
import org.teiid.adminapi.impl.VDBImportMetadata;
import org.teiid.adminapi.impl.VDBMetaData;
import org.teiid.adminapi.impl.VDBMetadataParser;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class TeiidMgrServiceImpl extends RemoteServiceServlet implements
TeiidMgrService {

	private static final String LOCALHOST = "127.0.0.1";
    private static final String DRIVER_KEY = "driver-name"; 
	private static final String CLASSNAME_KEY = "class-name";
    private static final String CONN_FACTORY_CLASS_KEY = "managedconnectionfactory-class"; 

    String serverHost;
    List<String> validLogins = new ArrayList<String>();
    PropertyItemComparator propItemComparator = new PropertyItemComparator();
    
	//ConnectionFactory connectionFactory;
	Admin admin = null;
	VdbHelper vdbHelper = VdbHelper.getInstance();

	//===========================================================================================================
	private static final String WRAPPER_DS = "org.jboss.resource.adapter.jdbc.WrapperDataSource"; //$NON-NLS-1$
	private static final String WRAPPER_DS_AS7 = "org.jboss.jca.adapters.jdbc.WrapperDataSource"; //$NON-NLS-1$
    private static final String TEIID_DRIVER_PREFIX = "teiid";
    private static final String JDBC_CONTEXT1 = "java:/"; //$NON-NLS-1$
    private static final String JDBC_CONTEXT2 = "java:/datasources/"; //$NON-NLS-1$
    private static final String JDBC_CONTEXT3 = "java:jboss/datasources/"; //$NON-NLS-1$
    public static List<String> JDBC_CONTEXTS = new ArrayList<String>() { {
    	add(JDBC_CONTEXT1);
    	add(JDBC_CONTEXT2);
    	add(JDBC_CONTEXT3);
    	}};
    	
    private static final String UPDATE = "UPDATE";
    private static final String INSERT = "INSERT";
    private static final String DELETE = "DELETE";
    private static final String TABLE_NAME = "TABLE_NAME";
    private static final String TABLE_SCHEM = "TABLE_SCHEM";
    private static final String PROCEDURE_NAME = "PROCEDURE_NAME";
    private static final String PROCEDURE_SCHEM = "PROCEDURE_SCHEM";
    private static final String SYS = "SYS";
    private static final String SYSADMIN = "SYSADMIN";
    private static final String COLUMN_NAME = "COLUMN_NAME";
    private static final String COLUMN_TYPE = "COLUMN_TYPE";
    private static final String TYPE_NAME = "TYPE_NAME";
    
	private Map<String,DataSource> mDatasources = new TreeMap<String,DataSource>();
	private Map<String,String> mDatasourceSchemas = new TreeMap<String,String>();

	private InitialContext context;
	//===========================================================================================================

	@Override
	protected void service(final HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		boolean isMultiPart = ServletFileUpload.isMultipartContent(new ServletRequestContext(request));

		if(isMultiPart) {
			FileItemFactory factory = new DiskFileItemFactory();
			ServletFileUpload upload = new ServletFileUpload(factory);

		    try{
		        FileItemIterator iter = upload.getItemIterator(request);

		        while (iter.hasNext()) {
		            FileItemStream item = iter.next();

		            String fileName = item.getName();   // The Actual File name
		            String fieldName = item.getFieldName();  // The name set on client side for the deployment name
		            
		            InputStream stream = item.openStream();
		            
		            // Dynamic or Archived VDBs - Deployment name is the file name
		            if(fileName.endsWith("-vdb.xml") || fileName.endsWith(".vdb")) {
		            	// for vdb - use the actual filename for deployment
		            	this.admin.deploy(fileName,stream);

		            	// This wait method takes deploymentName
		            	waitForVDBDeploymentToLoad(this.admin, fileName, 120);

		 				// Add the VDB Source.  If it exists, it is deleted first - then re-added.
		            	String vdbName = getVDBNameForDeployment(fileName);
		 				addVDBSource(vdbName);
		 			// Drivers
		            } else {
		            	// For driver jars - deploy using supplied fieldName 
		            	this.admin.deploy(fieldName,stream);		            	
		            }
		            
		            response.setContentType("text/plain"); 
		            final PrintWriter outWriter = response.getWriter(); 
		            outWriter.print("UploadSuccess"); 
		            outWriter.close(); 
		        }
		    }
		    catch(Exception e){
		        throw new RuntimeException(e);
		    }
		}

		else {
			super.service(request, response);
			return;
		}
	}
    
    /*
	 * (non-Javadoc)
	 * @see org.teiid.webapp.client.TeiidService#isRunningOnOpenShift( )
	 */
	public Boolean isRunningOnOpenShift( ) {
		String openShiftAppName = System.getenv("OPENSHIFT_APP_NAME");
		if(openShiftAppName!=null && openShiftAppName.trim().length()>0) {
			return new Boolean(true);
		}
		return new Boolean(false);	
	}

	/*
	 * (non-Javadoc)
	 * @see org.teiid.webapp.client.TeiidService#initApplication(int, java.lang.String, java.lang.String, java.lang.String)
	 */
	public List<DataItem> initApplication(int serverPort, String userName, String password) throws TeiidServiceException {

		// Establish serverHost this is running on
		if(this.serverHost==null) establishServerHost();
		
		// Validate the user and init the adminAPI if necessary
		try {
			validateUser(serverHost,serverPort,userName,password);
		} catch (Exception e) {
			throw new TeiidServiceException(e.getMessage());
		}
		
		return getVDBItems();
	}
	
	/**
	 * Establish the serverHost this is running on.  Only need to do this once.
	 */
	private void establishServerHost() {
		serverHost = LOCALHOST;
		
		// First priority is use OpenShift - if running on OpenShift.
		// Try both the JBOSSEAP and JBOSSAS system vars.  Also try OPENSHIFT_TEIID_ID and OPENSHIFT_DV_IP (Cartridges)
		String serverIP = System.getenv("OPENSHIFT_JBOSSEAP_IP");
		if(serverIP==null || serverIP.trim().isEmpty()) {
			serverIP = System.getenv("OPENSHIFT_JBOSSAS_IP");
		}
		if(serverIP==null || serverIP.trim().isEmpty()) {
			serverIP = System.getenv("OPENSHIFT_TEIID_IP");
		}
		if(serverIP==null || serverIP.trim().isEmpty()) {
			serverIP = System.getenv("OPENSHIFT_DV_IP");
		}
				
		if(serverIP==null || serverIP.trim().isEmpty()) {
			// Lookup the server ip address for the server this is running on.
			serverIP = System.getProperty("jboss.bind.address");
		}
						
		// If the server bind address is set, override the default 'localhost'
		if(serverIP!=null && !serverIP.trim().isEmpty()) {
			serverHost = serverIP;
		}
	}

	/*
	 * Validate the supplied User Info.  
	 * @param serverHost the server host name
	 * @param serverPort the admin port
	 * @param userName the user name
	 * @param password the user password
	 */
	private void validateUser(String serverHost, int serverPort, String userName, String password) throws Exception {
		// Check for already-validated User
		String userString = userName+password+serverPort;
		
		// If user is valid, ensure connection and return
		if(validLogins.contains(userString)) {
			// admin should be non-null but check anyway
			if(admin==null) {
				admin = getAdminApi(serverHost,serverPort,userName,password);
			}
			return;
		}
		
		// User validity is unknown.  try to establish connection to determine validity (exception thrown on failure)
		Admin newConnection = getAdminApi(serverHost,serverPort,userName,password);

		// Connection successfully established.
		if(newConnection!=null) {
			// Add the user to validLogins
			validLogins.add(userName+password+serverPort);
			
			// Already have a valid connection, close the new one
			if(admin!=null) {
				newConnection.close();
			} else {
				admin = newConnection;
			}
		}
	}
	
	/**
	 * Get an admin api connection with the supplied credentials
	 * @param serverHost the server hostname
	 * @param serverPort the server port number
	 * @param userName the username
	 * @param password the password
	 * @return the admin api
	 */
	private Admin getAdminApi (String serverHost, int serverPort, String userName, String password) throws Exception {
		Admin admin = null;
		try {
			admin = AdminFactory.getInstance().createAdmin(serverHost, serverPort, userName, password.toCharArray());
		} catch (Exception e) {
			throw new Exception(e.getMessage());
		}
		if(admin==null) {
			StringBuffer sb = new StringBuffer("Unable to establish Admin API connection.  Please check the supplied credentials: \n");
			sb.append("\n [Host]: "+serverHost);
			sb.append("\n [Port]: "+serverPort);
			
			throw new Exception(sb.toString());
		}
		return admin;
	}
	
	/*
	 * Get the current list of Dynamic VDBs
	 * @return the list of Dynamic VDBs
	 */
	public List<String> getDynamicVDBNames() throws TeiidServiceException {
		List<String> vdbNames = new ArrayList<String>();
		if(this.admin==null) return vdbNames;
		
		// Get list of VDBS - get vdbName VDB (if already deployed)
		Collection<? extends VDB> vdbs = null;
		try {
			vdbs = this.admin.getVDBs();
		} catch (AdminException e) {
		    throw new TeiidServiceException(e.getMessage());
		}
		
		// Only show dynamic VDBs, dont show the Preview VDBs
		for(VDB vdb : vdbs) {
			VDBMetaData vdbMeta = (VDBMetaData)vdb;
			if(vdbMeta.isXmlDeployment() && !vdbMeta.isPreview()) {
				String vdbName = vdbMeta.getName();
				if(vdbName!=null && !vdbName.startsWith("VDBMgr-")) {
					vdbNames.add(vdbName);
				}
			}
		}

		Collections.sort(vdbNames);
		
		return vdbNames;
	}
	
	/*
	 * Get the current list of VDBs, both dynamic and other
	 * @return the list of VDBs
	 */
	public List<DataItem> getVDBItems() throws TeiidServiceException {
		List<DataItem> vdbItems = new ArrayList<DataItem>();
		if(this.admin==null) return vdbItems;
		
		// Get list of VDBS - get vdbName VDB (if already deployed)
		Collection<? extends VDB> vdbs = null;
		try {
			vdbs = this.admin.getVDBs();
		} catch (AdminException e) {
		    throw new TeiidServiceException(e.getMessage());
		}
		
		// Get all VDBs, filtering Preview and VDBMgr vdbs out
		for(VDB vdb : vdbs) {
			VDBMetaData vdbMeta = (VDBMetaData)vdb;
			String vdbName = vdbMeta.getName();
			if(!vdbMeta.isPreview() && !vdbName.startsWith("VDBMgr-")) {
				DataItem vdbItem = new DataItem();
				vdbItem.setData(vdbName);
				if(vdbMeta.isXmlDeployment()) {
					vdbItem.setType("DYNAMIC");
				} else {
					vdbItem.setType("ARCHIVE");
				}
				vdbItems.add(vdbItem);
			}
		}
		
		return vdbItems;
	}
	
	/*
	 * Get the current list of Dynamic VDBs - only 'source' VDBMgr vdbs
	 * @return the list of Dynamic Source VDBs
	 */
	private List<String> getSourceDynamicVDBNames() throws TeiidServiceException {
		List<String> vdbNames = new ArrayList<String>();
		if(this.admin==null) return vdbNames;
		
		// Get list of VDBS - get vdbName VDB (if already deployed)
		Collection<? extends VDB> vdbs = null;
		try {
			vdbs = this.admin.getVDBs();
		} catch (AdminException e) {
		    throw new TeiidServiceException(e.getMessage());
		}
		
		// Only show dynamic VDBs, dont show the Preview VDBs
		for(VDB vdb : vdbs) {
			VDBMetaData vdbMeta = (VDBMetaData)vdb;
			if(vdbMeta.isXmlDeployment() && !vdbMeta.isPreview()) {
				String vdbName = vdbMeta.getName();
				if(vdbName!=null && vdbName.startsWith("VDBMgr-")) {
					vdbNames.add(vdbName);
				}
			}
		}

		return vdbNames;
	}
	
	/*
	 * Get the current list of Templates
	 * @return the list of Templates
	 */
	public List<String> getDataSourceTemplates() throws TeiidServiceException {
		List<String> templateNames = new ArrayList<String>();
		if(this.admin==null) return templateNames;
		
		// Get list of DataSource Template Names
		Collection<String> dsTemplates = null;
		try {
			dsTemplates = (Collection<String>) this.admin.getDataSourceTemplateNames();
		} catch (AdminException e) {
			throw new TeiidServiceException(e.getMessage());
		}
		
		// Filter out un-wanted names
		for(String template : dsTemplates) {
			if(template!=null && !template.endsWith(".war")) {
				templateNames.add(template);
			}
		}

		// Sort the list
		Collections.sort(templateNames);

		return templateNames;
	}
	
	/*
	 * Get a Map of the current Templates to their PropertyItems.
	 * @return the Map of templateName to List of PropertyItems
	 */
	public Map<String,List<PropertyItem>> getDefaultPropertyItemMap() throws TeiidServiceException {
		// Define Map to hold the results
		Map<String,List<PropertyItem>> resultMap = new HashMap<String,List<PropertyItem>>();
		
		// Get all DataSource Template names
		List<String> templateNames = getDataSourceTemplates();
		
		// For each DataSource, get the properties then populate the resultMap. 
		for(String template: templateNames) {
			List<PropertyItem> propDefns = getDriverPropertyItems(template);
			resultMap.put(template, propDefns);
		}
		
		return resultMap;
	}
	
	
	/*
	 * Get the current list of DataSource names
	 * @return the list of DataSource names
	 */
	public List<String> getDataSourceNames() throws TeiidServiceException {
		if(this.admin==null) return new ArrayList<String>();
		
		// Get list of DataSource Names
		Collection<String> sourceNames = null;
		try {
			sourceNames = this.admin.getDataSourceNames();
		} catch (AdminException e) {
			throw new TeiidServiceException(e.getMessage());
		}
		
		List<String> resultList = new ArrayList<String>(sourceNames);
		
		// Sort the list
		Collections.sort(resultList);

		return resultList;
	}

	/*
	 * Get the current list of Translator names
	 * @return the list of Translator names
	 */
	public List<String> getTranslatorNames() throws TeiidServiceException {
		List<String> transNames = new ArrayList<String>();
		if(this.admin==null) return transNames;
		
		// Get list of DataSource Template Names
		Collection<? extends Translator> translators = null;
		try {
			translators = this.admin.getTranslators();
		} catch (AdminException e) {
			throw new TeiidServiceException(e.getMessage());
		}
		
		// Filter out un-wanted names
		for(Translator translator : translators) {
			if(translator!=null) {
				transNames.add(translator.getName());
			}
		}

		// Sort the list
		Collections.sort(transNames);

		return transNames;
	}

	/*
	 * Create the Dynamic VDB - if it is not already deployed
	 * @param vdbName name of the VDB to create
	 * @return the new List of VDB names
	 */
	public List<DataItem> createVDB(String vdbName) throws TeiidServiceException {
		List<DataItem> vdbItems = new ArrayList<DataItem>();
		
 		if(this.admin==null) return vdbItems;

 		VDBMetaData vdb = getVdb(vdbName,1);
 		
 		try {
 			// Only deploy the VDB if it was not found
 			if(vdb==null) {
 				// Deployment name for vdb must end in '-vdb.xml'
 				String deploymentName = vdbName+"-vdb.xml";

 				// Deploy the VDB
 				VDBMetaData newVdb = vdbHelper.createVdb(vdbName,1);
 				newVdb.addProperty("{http://teiid.org/rest}auto-generate","true");
 				newVdb.addProperty("{http://teiid.org/rest}security-type","none");
 				String deployString = vdbHelper.getVdbString(newVdb);
 				
 				//String deployString = createNewDeployment(vdbName,1);
 				this.admin.deploy(deploymentName,new ByteArrayInputStream(deployString.getBytes("UTF-8")));

 				waitForVDBLoad(this.admin, vdbName, 1, 120);

 				// Add the VDB Source.  If it exists, it is deleted first - then re-added.
 				addVDBSource(vdbName);
 			}
 		} catch (Exception e) {
 			throw new TeiidServiceException(e.getMessage());
 		}
		// Return the new list of VDB names
		return getVDBItems();
	}
	
	/*
	 * Create the Sample Dynamic VDB - includes the sample source imports and sample views
	 * @param vdbName name of the Sample VDB to create
	 * @return the new List of VDB names
	 */
	public List<DataItem> createSampleVDB(String vdbName) throws TeiidServiceException {

		// Sample Sources to Use
		List<String> sampleDataSources = new ArrayList<String>();
		sampleDataSources.add("SampleSalesforce");
		sampleDataSources.add("SampleRemoteFlatFile");
		sampleDataSources.add("SampleRemoteXmlFile");
		// Corresponding translators
		List<String> sampleTranslators = new ArrayList<String>();
		sampleTranslators.add("salesforce");
		sampleTranslators.add("ws");
		sampleTranslators.add("ws");
		
		Map<String,String> sourceErrorMap = new HashMap<String,String>();

		List<String> sourceVdbNames = new ArrayList<String>();
		
		// ------------------------------------------------
		// First, the Source VDBs need to be deployed
		// ------------------------------------------------
		int i=0;
		for(String dataSource : sampleDataSources) {
			String translator = sampleTranslators.get(i);
			String sourceVdbName = "VDBMgr-"+dataSource+"-"+translator;
			
			sourceVdbNames.add(sourceVdbName);
			
			// Deploy and wait for completion
			String statusMessage = deploySourceVDB(sourceVdbName, dataSource, translator);
			
			// Keep track of Source Status
			sourceErrorMap.put(sourceVdbName,statusMessage);
		    i++;
		}

		List<DataItem> vdbItems = new ArrayList<DataItem>();
		
 		if(this.admin==null) return vdbItems;

 		VDBMetaData vdb = getVdb(vdbName,1);
 		
 		try {
 			// Only deploy the VDB if it was not found
 			if(vdb==null) {
 				// Deployment name for vdb must end in '-vdb.xml'
 				String deploymentName = vdbName+"-vdb.xml";

 				// Create the Sample VDB
 				VDBMetaData newVdb = vdbHelper.createVdb(vdbName,1);
 				newVdb.addProperty("{http://teiid.org/rest}auto-generate","true");
 				newVdb.addProperty("{http://teiid.org/rest}security-type","none");

 				// Create Source imports - add them to VDB (Only if Error-free)
 		 		List<VDBImportMetadata> vdbImports = new ArrayList<VDBImportMetadata>();
 		 		for(String sourceVdbName : sourceVdbNames) {
 		 			String sourceStatus = sourceErrorMap.get(sourceVdbName);
 		 			if(sourceStatus.equals("success")) {
 		 				vdbImports.add(vdbHelper.createVdbImport(sourceVdbName,1));
 		 			}
 		 		}
 				newVdb.getVDBImports().addAll(vdbImports);
 				
 				// Create View models - add them to VDB
 		 		List<ModelMetaData> viewModels = new ArrayList<ModelMetaData>();
 				for(String sampleDataSource : sampleDataSources) {
 					String viewModelName = "MyView"+sampleDataSource;
 					String ddl = getSampleViewModelDDL(viewModelName);
 	 				ModelMetaData modelMetaData = vdbHelper.createViewModel(viewModelName,ddl);
 	 				viewModels.add(modelMetaData);
 				}
 				newVdb.setModels(viewModels);
 				
 				String deployString = vdbHelper.getVdbString(newVdb);
 				
 				//String deployString = createNewDeployment(vdbName,1);
 				this.admin.deploy(deploymentName,new ByteArrayInputStream(deployString.getBytes("UTF-8")));

 				waitForVDBLoad(this.admin, vdbName, 1, 120);

 				// Add the VDB Source.  If it exists, it is deleted first - then re-added.
 				addVDBSource(vdbName);
 			}
 		} catch (Exception e) {
 			throw new TeiidServiceException(e.getMessage());
 		}
		// Return the new list of VDB names
		return getVDBItems();
	}

	private String getSampleViewModelDDL(String viewModel) {
		StringBuffer ddlBuff = new StringBuffer();
		if(viewModel.equals("MyViewSampleRemoteFlatFile")) {
			ddlBuff.append("CREATE VIEW SupplierView AS ");
			ddlBuff.append("SELECT A.SUPPLIER_ID, A.SUPPLIER_NAME, A.SUPPLIER_STATUS, A.SUPPLIER_CITY, A.SUPPLIER_STATE ");
			ddlBuff.append("FROM (EXEC SampleRemoteFlatFile.invokeHttp('GET', null, 'http://download.jboss.org/teiid/designer/data/partssupplier/file/supplier_data.txt', 'TRUE')) AS f, ");
			ddlBuff.append("TEXTTABLE(TO_CHARS(f.result, 'UTF-8') COLUMNS SUPPLIER_ID string, SUPPLIER_NAME string, SUPPLIER_STATUS string, SUPPLIER_CITY string, SUPPLIER_STATE string HEADER) AS A;");
		} else if(viewModel.equals("MyViewSampleRemoteXmlFile")) {
			ddlBuff.append("CREATE VIEW EmployeeView AS ");
			ddlBuff.append("SELECT A.LastName AS LastName, A.FirstName AS FirstName, A.MiddleName AS MiddleName, A.EmpId AS EmpId, A.Department AS Department, ");
			ddlBuff.append("A.AnnualSalary AS AnnualSalary, A.Title AS Title, A.HomePhone AS HomePhone, A.Manager AS Manager, A.Street AS Street, A.City AS City, A.State AS State, A.Zip AS Zip ");
			ddlBuff.append("FROM (EXEC SampleRemoteXmlFile.invokeHttp('GET', null, 'http://download.jboss.org/teiid/designer/data/employees/xml/EMPLOYEEDATA.xml', 'TRUE')) AS f, ");
			ddlBuff.append("XMLTABLE(XMLNAMESPACES('http://org.jboss.teiid' AS EMPLOYEES_NS), '/EmployeeData/EmployeeData' PASSING XMLPARSE(DOCUMENT f.result) COLUMNS ");
			ddlBuff.append("LastName string PATH 'LastName/text()', FirstName string PATH 'FirstName/text()', MiddleName string PATH 'MiddleName/text()', EmpId string PATH 'EmpId/text()', ");
			ddlBuff.append("Department string PATH 'Department/text()', AnnualSalary string PATH 'AnnualSalary/text()', Title string PATH 'Title/text()', HomePhone string PATH 'HomePhone/text()', ");
			ddlBuff.append("Manager string PATH 'Manager/text()', Street string PATH 'Street/text()', City string PATH 'City/text()', State string PATH 'State/text()', Zip string PATH 'Zip/text()') AS A;");
		} else if(viewModel.equals("MyViewSampleSalesforce")) {
			ddlBuff.append("CREATE VIEW SFAccountBillingView AS ");
			ddlBuff.append("SELECT Name,TickerSymbol,Type,BillingStreet,BillingCity,BillingState,BillingPostalCode FROM SampleSalesforce.Account;");
		}
		return ddlBuff.toString();
	}	 
	
	/*
	 * Delete the Dynamic VDB - undeploy it, then delete the source
	 * @param vdbName name of the VDB to delete
	 * @return the new List of VDB names
	 */
	public List<DataItem> deleteVDB(String vdbName) throws TeiidServiceException {
		String deploymentName = getDeploymentNameForVDB(vdbName,1);
		if(deploymentName!=null) {			
			try {
				// Undeploy the VDB
				admin.undeploy(deploymentName);

				// Delete the VDB Source
				deleteDataSource(vdbName);
			} catch (Exception e) {
				throw new TeiidServiceException(e.getMessage());
			}
		}
		// Return the new list of VDB names
		return getVDBItems();
	}
	
	/*
	 * Helper method - waits for the VDB to finish loading
	 * @param admin the admin api instance
	 * @param vdbName the name of the VDB
	 * @param vdbVersion the VDB version
	 * @param timeoutInSecs time to wait before timeout
	 * @return 'true' if vdb found and is out of 'Loading' status, 'false' otherwise.
	 */
	private boolean waitForVDBLoad(Admin admin, String vdbName, int vdbVersion, int timeoutInSecs) {
		long waitUntil = System.currentTimeMillis() + timeoutInSecs*1000;
		if (timeoutInSecs < 0) {
			waitUntil = Long.MAX_VALUE;
		}
		
		boolean first = true;
		do {
			// Pause 5 sec before subsequent attempts
			if (!first) {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					break;
				}
			} else {
				first = false;
			}
			// Get the VDB using admin API
			VDBMetaData vdbMetaData = null;
			try {
				vdbMetaData = (VDBMetaData)admin.getVDB(vdbName, vdbVersion);
			} catch (AdminException e) {
			}
			// Determine if VDB is loading, or whether to wait
			if(vdbMetaData!=null) {
				Status vdbStatus = vdbMetaData.getStatus();
				// return if no models in VDB, or VDB has errors (done loading)
				if(vdbMetaData.getModels().isEmpty() || vdbStatus==Status.FAILED || vdbStatus==Status.REMOVED || vdbStatus==Status.ACTIVE) {
					return true;
				}
				// If the VDB Status is LOADING, but a validity error was found - return
				if(vdbStatus==Status.LOADING && !vdbMetaData.getValidityErrors().isEmpty()) {
					return true;
				}
			}
		} while (System.currentTimeMillis() < waitUntil);
		return false;
	}
	
	/*
	 * Helper method - waits for the VDB to finish loading
	 * @param admin the admin api instance
	 * @param deploymentName the deployment name for the VDB
	 * @param timeoutInSecs time to wait before timeout
	 * @return 'true' if vdb found and is out of 'Loading' status, 'false' otherwise.
	 */
	private boolean waitForVDBDeploymentToLoad(Admin admin, String deploymentName, int timeoutInSecs) {
		long waitUntil = System.currentTimeMillis() + timeoutInSecs*1000;
		if (timeoutInSecs < 0) {
			waitUntil = Long.MAX_VALUE;
		}

		String vdbName = null;
		int vdbVersion = 1;
		// Get VDB name and version for the specified deploymentName
		Collection<? extends VDB> allVdbs = null;
		try {
			allVdbs = this.admin.getVDBs();
			for(VDB vdbMeta : allVdbs) {
				String deployName = vdbMeta.getPropertyValue("deployment-name");
				if(deployName!=null && deployName.equals(deploymentName)) {
					vdbName=vdbMeta.getName();
					vdbVersion=vdbMeta.getVersion();
					break;
				}
			}
		} catch (AdminException e) {
		}
		
		if(vdbName==null) return false;
		
		boolean first = true;
		do {
			// Pause 5 sec before subsequent attempts
			if (!first) {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					break;
				}
			} else {
				first = false;
			}
			// Get the VDB using admin API
			VDBMetaData vdbMetaData = null;
			try {
				vdbMetaData = (VDBMetaData)admin.getVDB(vdbName, vdbVersion);
			} catch (AdminException e) {
			}
			// Determine if VDB is loading, or whether to wait
			if(vdbMetaData!=null) {
				Status vdbStatus = vdbMetaData.getStatus();
				// return if no models in VDB, or VDB has errors (done loading)
				if(vdbMetaData.getModels().isEmpty() || vdbStatus==Status.FAILED || vdbStatus==Status.REMOVED || vdbStatus==Status.ACTIVE) {
					return true;
				}
				// If the VDB Status is LOADING, but a validity error was found - return
				if(vdbStatus==Status.LOADING && !vdbMetaData.getValidityErrors().isEmpty()) {
					return true;
				}
			}
		} while (System.currentTimeMillis() < waitUntil);
		return false;
	}
	
	/*
	 * Find the VDB Name for the provided deployment name 
	 * @param deploymentName
	 * @return the VDB Name
	 */
	private String getVDBNameForDeployment(String deploymentName) {
		String vdbName = null;

		// Get VDB name and version for the specified deploymentName
		Collection<? extends VDB> allVdbs = null;
		try {
			allVdbs = this.admin.getVDBs();
			for(VDB vdbMeta : allVdbs) {
				String deployName = vdbMeta.getPropertyValue("deployment-name");
				if(deployName!=null && deployName.equals(deploymentName)) {
					vdbName=vdbMeta.getName();
					break;
				}
			}
		} catch (AdminException e) {
		}
		
		return vdbName;
	}
	
	/*
	 * Find the Deployment Name for the provided VdbName and Version 
	 * @param deploymentName
	 * @return the VDB Name
	 */
	private String getDeploymentNameForVDB(String vdbName, int vdbVersion) throws TeiidServiceException {
		String deploymentName = null;
		VDBMetaData vdbMeta = getVdb(vdbName,vdbVersion);
		if(vdbMeta!=null) {
			deploymentName = vdbMeta.getPropertyValue("deployment-name");
		}
		return deploymentName;
	}

	/*
	 * Get Info for all dataSources on the server
	 * @return the List of DataSource info
	 */
	public List<List<DataItem>> getDataSourceInfos() throws TeiidServiceException {
		List<List<DataItem>> rowList = new ArrayList<List<DataItem>>();
		if(this.admin==null) return rowList;
		
		// Get list of DataSource Names
		Collection<String> dsNames = null;
		try {
			dsNames = admin.getDataSourceNames();
		} catch (AdminException e) {
			throw new TeiidServiceException(e.getMessage());
		}
		
		List<String> dsNameList = new ArrayList<String>(dsNames);
		// Return DataSources in alpha order
		Collections.sort(dsNameList);
		
		// First row of result is Header Info
		List<DataItem> headerInfo = new ArrayList<DataItem>();
		headerInfo.add(new DataItem("DataSource","string"));
		headerInfo.add(new DataItem("Driver","string"));
		rowList.add(headerInfo);

		// Iterate the dataSource names, getting the properties
		for(String dsName: dsNameList) {
			Properties dsProps = null;
			try {
				dsProps = admin.getDataSource(dsName);
			} catch (AdminException e) {
				throw new TeiidServiceException(e.getMessage());
			}
			String driverName = getDataSourceDriver(dsProps);
			List<DataItem> dsInfo = new ArrayList<DataItem>();
			dsInfo.add(new DataItem(dsName,"string"));
			dsInfo.add(new DataItem(driverName,"string"));
			rowList.add(dsInfo);
		}
		
		return rowList;
	}

	/*
	 * Get Imported VDBs in the specified VDB
	 * @param vdbName name of the VDB
	 * @return the List of VdbImportInfo data
	 */
	public List<List<DataItem>> getVDBImportInfo(String vdbName) throws TeiidServiceException {
		List<List<DataItem>> rowList = new ArrayList<List<DataItem>>();
		
		VDBMetaData vdb = getVdb(vdbName,1);

		// Build the row data
		if(vdb!=null) {
			// First row of result is VDB Import Header Info
			List<DataItem> headerInfo = new ArrayList<DataItem>();
			headerInfo.add(new DataItem("VDB Name","string"));
			headerInfo.add(new DataItem("Version","string"));
			rowList.add(headerInfo);

			// Subsequent Rows - one row for each model
			List<VDBImportMetadata> importVdbs = vdb.getVDBImports();
			
			for(VDBImportMetadata importVdb: importVdbs) {
				String name = importVdb.getName();
				int version = importVdb.getVersion();

				List<DataItem> importInfo = new ArrayList<DataItem>();
				importInfo.add(new DataItem(name,"string"));
				importInfo.add(new DataItem(String.valueOf(version),"string"));
				rowList.add(importInfo);
			}
		}
		
		return rowList;
	}

	/*
	 * Get ModelInfo for the models in the specified VDB
	 * @param vdbName name of the VDB
	 * @return the List of ModelInfo data
	 */
	public List<List<DataItem>> getVDBModelInfo(String vdbName) throws TeiidServiceException {
		List<List<DataItem>> rowList = new ArrayList<List<DataItem>>();
		
		VDBMetaData vdb = getVdb(vdbName,1);

		// VDB Status Item
		DataItem vdbStatusItem = null;
		
		// Add a model to the vdb, then re-deploy it.
		if(vdb!=null) {
			// First row of result is VDB Status
			VDB.Status status = vdb.getStatus();
			// Change FAILED or REMOVED status to INACTIVE
			String vdbStatus = status.toString();
			if(vdbStatus!=null && (vdbStatus.equalsIgnoreCase("FAILED")||vdbStatus.equalsIgnoreCase("REMOVED"))) {
				vdbStatus="INACTIVE";
			}
			List<DataItem> vdbStatusInfo = new ArrayList<DataItem>();
			vdbStatusItem = new DataItem(vdbStatus,"string");
			vdbStatusInfo.add(vdbStatusItem);
			rowList.add(vdbStatusInfo);

			// Second row of result is Model Header Info
			List<DataItem> headerInfo = new ArrayList<DataItem>();
			headerInfo.add(new DataItem("Model Name","string"));
			headerInfo.add(new DataItem("Model Type","string"));
			headerInfo.add(new DataItem("Translator","string"));
			headerInfo.add(new DataItem("JNDI source","string"));
			headerInfo.add(new DataItem("Status","string"));
			headerInfo.add(new DataItem("DDL","string"));
			rowList.add(headerInfo);

			// Subsequent Rows - one row for each model
			List<Model> models = vdb.getModels();
			
			for(Model model: models) {
				ModelMetaData modelMeta = (ModelMetaData)model;
				String modelName = modelMeta.getName();
				String modelType = modelMeta.getModelType().toString();
				String jndiName = null;
				String translatorName = null;
				String modelStatus = null;
				String ddl = "";

				// Virtual Model, use placeholders for jndiName and translatorName
				if(modelType.equals(Model.Type.VIRTUAL.toString())) {
					jndiName = "-----";
					translatorName = "teiid";
					ddl = modelMeta.getSchemaText();
					// Physical Model, get source info 
				} else {
					List<String> sourceNames = modelMeta.getSourceNames();
					for(String sourceName: sourceNames) {
						jndiName = modelMeta.getSourceConnectionJndiName(sourceName);
						translatorName = modelMeta.getSourceTranslatorName(sourceName);
					}
				}
				
				// If this is not an XML Deployment, show the Status as Unknown
				if(!vdb.isXmlDeployment()) {
					modelStatus = "Unknown";
				// Is XML Deployment, look at model errors
				} else {
					List<String> errors = modelMeta.getValidityErrors();
					if(errors.size()==0) {
						modelStatus = "ACTIVE";
					} else {
						// There may be multiple errors - process the list...
						boolean connectionError = false;
						boolean validationError = false;
						boolean isLoading = false;
						// Iterate Errors and set status flags
						for(String error: errors) {
							if(error.indexOf("TEIID11009")!=-1 || error.indexOf("TEIID60000")!=-1 || error.indexOf("TEIID31097")!=-1) {
								connectionError=true;
							} else if(error.indexOf("TEIID31080")!=-1 || error.indexOf("TEIID31071")!=-1) {
								validationError=true;
							} else if(error.indexOf("TEIID50029")!=-1) {
								isLoading=true;
							}
						}
						// --------------------------------------------------
						// Set model status string according to errors found
						// --------------------------------------------------
						// Connection Error.  Reset the VDB overall status, as it may say loading
						if(connectionError) {
							modelStatus = "INACTIVE: Data Source connection failed...";
							if(vdbStatusItem!=null && "LOADING".equalsIgnoreCase(vdbStatusItem.getData())) {
								vdbStatusItem.setData("INACTIVE");
							}
							// Validation Error with View SQL
						} else if(validationError) {
							modelStatus = "INACTIVE: Validation Error with SQL";
							// Loading in progress
						} else if(isLoading) {
							modelStatus = "INACTIVE: Metadata loading in progress...";
							// Unknown - use generic message
						} else {
							modelStatus = "INACTIVE: unknown source issue";
						}
					}
				}
				List<DataItem> modelInfo = new ArrayList<DataItem>();
				modelInfo.add(new DataItem(modelName,"string"));
				modelInfo.add(new DataItem(modelType,"string"));
				modelInfo.add(new DataItem(translatorName,"string"));
				modelInfo.add(new DataItem(jndiName,"string"));
				modelInfo.add(new DataItem(modelStatus,"string"));
				modelInfo.add(new DataItem(ddl,"string"));
				rowList.add(modelInfo);
			}
		}
		
		return rowList;
	}
	
	/*
	 * Get DDL for the model in the specified VDB
	 * @param vdbName name of the VDB
	 * @param modelName name of the model
	 * @return the model DDL
	 */
//	public String getViewModelDDL(String vdbName,String modelName) throws TeiidServiceException {
//		VDBMetaData vdb = getVdb(vdbName);
//		
//		// Model DDL
//		String ddl = null;
//		
//		// Add a model to the vdb, then re-deploy it.
//		if(vdb!=null) {
//			// Subsequent Rows - one row for each model
//			List<Model> models = vdb.getModels();
//			
//			for(Model model: models) {
//				ModelMetaData modelMeta = (ModelMetaData)model;
//				String vdbModelName = modelMeta.getName();
//				String modelType = modelMeta.getModelType().toString();
//
//				if(vdbModelName.equalsIgnoreCase(modelName) && modelType.equals(Model.Type.VIRTUAL.toString())) {
//					ddl = modelMeta.getSchemaText();
//					break;
//				}
//			}
//		}
//		
//		return ddl;
//	}
	
	private VDBMetaData getVdb(String vdbName, int vdbVersion) throws TeiidServiceException {
		// Get list of VDBS - get the named VDB
		Collection<? extends VDB> vdbs = null;
		try {
			vdbs = admin.getVDBs();
		} catch (AdminException e) {
			throw new TeiidServiceException(e.getMessage());
		}
		
		VDBMetaData vdb = null;
		for(VDB aVdb : vdbs) {
			VDBMetaData vdbMeta = (VDBMetaData)aVdb;
			if(vdbMeta.getName()!=null && vdbMeta.getName().equalsIgnoreCase(vdbName) && vdbMeta.getVersion()==vdbVersion) {
				vdb = vdbMeta;
				break;
			}
		}
		return vdb;
	}
	
	private List<VDBImportMetadata> getVdbImports(VDBMetaData vdb) throws TeiidServiceException {
		// Get current vdb imports
 		List<VDBImportMetadata> currentVdbImports = null;

 		if(vdb!=null) {
			currentVdbImports = new ArrayList<VDBImportMetadata>(vdb.getVDBImports());
		} else {
			currentVdbImports = new ArrayList<VDBImportMetadata>();
		}
		
		return currentVdbImports;
	}
	
	private List<ModelMetaData> getVdbViewModels(VDBMetaData vdb) throws TeiidServiceException {
		// Get current vdb ViewModels
 		List<ModelMetaData> viewModels = new ArrayList<ModelMetaData>();

		if(vdb!=null) {
			List<Model> allModels = vdb.getModels();
			for(Model theModel : allModels) {
				if(theModel.getModelType()==Model.Type.VIRTUAL) {
					viewModels.add((ModelMetaData)theModel);
				}
			}
		} 
		
		return viewModels;
	}
	
	private Properties getVdbProperties(VDBMetaData vdb) throws TeiidServiceException {
 		Properties props = null;

 		if(vdb!=null) {
			props = vdb.getProperties();
		} else {
			props = new Properties();
		}
		
		return props;
	}
	
	/*
	 * Removes the view models from the supplied VDB deployment - if they exist.  Redeploys the VDB after
	 * models are removed.
	 * @param vdbName name of the VDB
	 * @param removeModelNameList the list of model names to remove
	 * @return the List of ModelInfo data
	 */
	public List<List<DataItem>> removeModelsAndRedeploy(String vdbName, List<String> removeModelNameList, List<ModelType> removeModelTypeList) throws TeiidServiceException {		
		if(admin!=null) {
			// Determine viewModels and sourceModels(Imports) to remove
			List<String> removeViewModelNameList = new ArrayList<String>();
			List<String> removeImportNameList = new ArrayList<String>();
			for(int i=0; i<removeModelNameList.size(); i++) {
				String modelName = removeModelNameList.get(i);
				ModelType modelType = removeModelTypeList.get(i);
				if(modelType==ModelType.VIEW) {
					removeViewModelNameList.add(modelName);
				} else {
					removeImportNameList.add(modelName);
				}
			}
			
			// Get current vdb imports
			VDBMetaData vdb = getVdb(vdbName,1);
			List<VDBImportMetadata> currentVdbImports = getVdbImports(vdb);
			// Get current vdb view models
			List<ModelMetaData> currentViewModels = getVdbViewModels(vdb);
	 		Properties currentProperties = getVdbProperties(vdb);

			// Create a new vdb
			VDBMetaData newVdb = vdbHelper.createVdb(vdbName,1);

			// Determine list of view models
			List<ModelMetaData> newViewModels = new ArrayList<ModelMetaData>();
			for(Model model: currentViewModels) {
				String currentName = model.getName();
				// Keep the model - unless its in the remove list
				if(!removeViewModelNameList.contains(currentName)) {
					newViewModels.add((ModelMetaData)model);
				}
			}
			// Add the existing ViewModels
			newVdb.setModels(newViewModels);
			
			// Create import list for new model
			List<VDBImportMetadata> newImports = new ArrayList<VDBImportMetadata>();
			for(VDBImportMetadata vdbImport: currentVdbImports) {
				String currentName = vdbImport.getName();
				// Keep the import - unless its in the remove list
				if(!removeImportNameList.contains(currentName)) {
					newImports.add((VDBImportMetadata)vdbImport);
				}
			}
			newVdb.getVDBImports().addAll(newImports);

			// Transfer the existing properties
			newVdb.setProperties(currentProperties);

			// Re-Deploy the VDB
			try {
				redeployVDB(vdbName, newVdb);
			} catch (Exception e) {
				throw new TeiidServiceException(e.getMessage());
			}
		}
		// Return the Model Info
		return getVDBModelInfo(vdbName);
	}
	
	/*
	 * Creates a datasource of specified type and properties
	 * @param sourceName the name of the source to add
	 * @param templateName the name of the template for the source
	 * @param sourcePropMap the map of property values for the specified source
	 * @return the status
	 */
	public String addDataSource(String sourceName, String templateName, Map<String,String> sourcePropMap ) throws TeiidServiceException {

		try {
			// Create the dataSource
			addSource(sourceName,templateName,sourcePropMap);
		} catch (Exception e) {
			throw new TeiidServiceException(e.getMessage());
		}
		return "Success";
	}
	
	/*
	 * Creates multiple datasources of specified type and properties
	 * @param sourceNames the list of source names 
	 * @param templateName the list of corresponding template names
	 * @param sourcesPropMap the map of property values for the specified sources
	 * @return the status
	 */
	public String addDataSources(List<String> sourceNames, List<String> templateNames, Map<String,Map<String,String>> sourcesPropMap ) throws TeiidServiceException {
		int i = 0;
		// Iterate the supplied sources
		for(String sourceName : sourceNames) {
			// Get corresponding templatename and props map for the source
			String templateName = templateNames.get(i);
			Map<String,String> sourcePropMap = sourcesPropMap.get(sourceName);
			
			// Create the source
			try {
				// Create the dataSource
				addSource(sourceName,templateName,sourcePropMap);
			} catch (Exception e) {
				throw new TeiidServiceException(e.getMessage());
			}
			
			i++;
		}
		return "Success";
	}

	public String deploySourceVDBAddImportAndRedeploy(String vdbName, String sourceVDBName, String dataSourceName, String translator) throws TeiidServiceException {
		// Deploy the Source VDB
		String message = deploySourceVDB(sourceVDBName, dataSourceName, translator);
		
		// Return with message if Source deployment failed
		if(message!=null && message!="success") {
			return message;
		}
		
		message = addImportAndRedeploy(vdbName,sourceVDBName,1);
		// Return with message if Source deployment failed
		if(message!=null && message!="success") {
			return message;
		}
		return message;
	}
	
	/*
	 * Adds the Import in supplied VDB deployment.  Redeploys the VDB after the import is added.
	 * @param vdbName name of the VDB
	 * @param importVdbName the name of the VDB to import
	 * @param importVdbVersion the version of the VDB to import
	 * @return the success string
	 */
	public String addImportAndRedeploy(String vdbName, String importVdbName, int importVdbVersion) throws TeiidServiceException {
		if(admin!=null) {
			// First Check the VDB being added.  If it has errors, dont add
			String sourceStatus = getVDBStatusMessage(importVdbName);
			if(!sourceStatus.equals("success")) {
				return "<bold>Import Source has errors and was not added:</bold><br>"+sourceStatus;
			}
			
			// Get current vdb imports
			VDBMetaData vdb = getVdb(vdbName,1);
	 		List<VDBImportMetadata> currentVdbImports = getVdbImports(vdb);
	 		List<ModelMetaData> currentViewModels = getVdbViewModels(vdb);
	 		Properties currentProperties = getVdbProperties(vdb);
	 		
	 		// Clear any prior Model Messages (needed for successful redeploy)
	 		clearModelMessages(currentViewModels);

	 		// Create a new vdb
			VDBMetaData newVdb = vdbHelper.createVdb(vdbName,1);
				 		
			// Add the existing ViewModels
			newVdb.setModels(currentViewModels);
			
			// Transfer the existing properties
			newVdb.setProperties(currentProperties);

			// Add new import to current imports
			currentVdbImports.add(vdbHelper.createVdbImport(importVdbName, importVdbVersion));
			newVdb.getVDBImports().addAll(currentVdbImports);
	 		
 			// Re-Deploy the VDB
 			try {
 				redeployVDB(vdbName, newVdb);
 			} catch (Exception e) {
 				throw new TeiidServiceException(e.getMessage());
 			}
 			
 			// Get deployed VDB and return status
			String vdbStatus = getVDBStatusMessage(vdbName);
			if(!vdbStatus.equals("success")) {
				return "<bold>Error deploying VDB "+vdbName+"</bold><br>"+vdbStatus;
			}
		}
		return "success";
	}
	
	/*
	 * Get the error messages (if any) for the supplied VDB.
	 * @param vdbName the name of the VDB
	 * @isSource 'true' if this is a source VDB being added as an import, 'false' otherwise
	 * @return the Error Message string, or 'success' if none
	 */
//	private String getVDBStatusMessage(String vdbName, boolean isSource) throws TeiidServiceException {
//		// Get deployed VDB and check status
//		VDBMetaData theVDB = getVdb(vdbName);
//		if(theVDB!=null) {
//			Status vdbStatus = theVDB.getStatus();
//			if(vdbStatus==Status.FAILED) {
//				String errorDeployingVDB = null;
//				if(isSource) {
//					errorDeployingVDB = "<bold>Import Source has errors and was not added:</bold><br>";
//				} else {
//					errorDeployingVDB = "<bold>Error deploying VDB "+vdbName+"</bold><br>";
//				}
//				List<String> allErrors = theVDB.getValidityErrors();
//				if(allErrors!=null && !allErrors.isEmpty()) {
//					StringBuffer sb = new StringBuffer(errorDeployingVDB);
//					for(String errorMsg : allErrors) {
//						sb.append("ERROR: " +errorMsg+"<br>");
//					}
//					return sb.toString();
//				} else {
//					return errorDeployingVDB;
//				}
//			}
//			return "success";
//		}
//	 	return "success";
//	}
	
	/*
	 * Get the error messages (if any) for the supplied VDB.
	 * @param vdbName the name of the VDB
	 * @return the Error Message string, or 'success' if none
	 */
	private String getVDBStatusMessage(String vdbName) throws TeiidServiceException {
		// Get deployed VDB and check status
		VDBMetaData theVDB = getVdb(vdbName,1);
		if(theVDB!=null) {
			Status vdbStatus = theVDB.getStatus();
			if(vdbStatus!=Status.ACTIVE) {
				List<String> allErrors = theVDB.getValidityErrors();
				if(allErrors!=null && !allErrors.isEmpty()) {
					StringBuffer sb = new StringBuffer();
					for(String errorMsg : allErrors) {
						sb.append("ERROR: " +errorMsg+"<br>");
					}
					return sb.toString();
				} 
			}
		}
	 	return "success";
	}
	
	/*
	 * Removes the imports from the supplied VDB deployment - if they exist.  Redeploys the VDB after
	 * imports are removed.
	 * @param vdbName name of the VDB
	 * @param removeImportNameList the list of import names to remove
	 * @return the List of ImportInfo data
	 */
	public List<List<DataItem>> removeImportsAndRedeploy(String vdbName, List<String> removeImportNameList) throws TeiidServiceException {		
		if(admin!=null) {
			// Get current vdb imports
			VDBMetaData vdb = getVdb(vdbName,1);
			List<VDBImportMetadata> currentVdbImports = getVdbImports(vdb);
			List<ModelMetaData> currentViewModels = getVdbViewModels(vdb);
	 		Properties currentProperties = getVdbProperties(vdb);

	 		// Clear any prior Model Messages (needed for successful redeploy)
	 		clearModelMessages(currentViewModels);

			// Create a new vdb
			VDBMetaData newVdb = vdbHelper.createVdb(vdbName,1);

			// Add the existing ViewModels
			newVdb.setModels(currentViewModels);

			// Transfer the existing properties
			newVdb.setProperties(currentProperties);

			// Create import list for new model
			List<VDBImportMetadata> newImports = new ArrayList<VDBImportMetadata>();
			for(VDBImportMetadata vdbImport: currentVdbImports) {
				String currentName = vdbImport.getName();
				// Keep the import - unless its in the remove list
				if(!removeImportNameList.contains(currentName)) {
					newImports.add((VDBImportMetadata)vdbImport);
				}
			}
			newVdb.getVDBImports().addAll(newImports);

			// Re-Deploy the VDB
			try {
				redeployVDB(vdbName, newVdb);
			} catch (Exception e) {
				throw new TeiidServiceException(e.getMessage());
			}
		}
		// Return the Model Info
		return getVDBModelInfo(vdbName);
	}
	
	private void clearModelMessages(List<ModelMetaData> models) {
		for(ModelMetaData model: models) {
			model.clearMessages();
		}
	}
	
	public List<String> getPropertyNames(String templateName) throws TeiidServiceException {
		List<String> propNames = new ArrayList<String>();
		if(this.admin!=null && templateName!=null && !templateName.trim().isEmpty()) {
			Collection<? extends PropertyDefinition> propDefnList = null;
			try {
				propDefnList = this.admin.getTemplatePropertyDefinitions(templateName);
			} catch (AdminException e) {
				throw new TeiidServiceException(e.getMessage());
			}
			for(PropertyDefinition propDefn: propDefnList) {
				if(propDefn.isRequired()) {
					String name = propDefn.getName();
					propNames.add(name);
				}
			}
		}
		return propNames;
	}
	
	public List<PropertyItem> getDriverPropertyItems(String driverName) throws TeiidServiceException {
		List<PropertyItem> propDefns = new ArrayList<PropertyItem>();
		if(this.admin!=null && !StringUtil.isEmpty(driverName)) {
			Collection<? extends PropertyDefinition> propDefnList = null;
			try {
				propDefnList = this.admin.getTemplatePropertyDefinitions(driverName);
			} catch (AdminException e) {
				throw new TeiidServiceException("["+driverName+"] "+e.getMessage());
			}
	        // Get the Managed connection factory class for rars
	        String rarConnFactoryValue = null;
	        if(isRarDriver(driverName)) {
	            rarConnFactoryValue = getManagedConnectionFactoryClassDefault(propDefnList);
	        }
			
			for(PropertyDefinition propDefn: propDefnList) {
				PropertyItem pDefn = new PropertyItem();
                // ------------------------
				// Set PropertyObj fields
                // ------------------------
				// Name
				String name = propDefn.getName();
				pDefn.setName(name);
				// DisplayName
				String displayName = propDefn.getDisplayName();
				pDefn.setDisplayName(displayName);
				// isModifiable
				boolean isModifiable = propDefn.isModifiable();
				pDefn.setModifiable(isModifiable);
				// isRequired
				boolean isRequired = propDefn.isRequired();
				pDefn.setRequired(isRequired);
	            // isMasked
	            boolean isMasked = propDefn.isMasked();
	            pDefn.setMasked(isMasked);
	            // defaultValue
	            Object defaultValue = propDefn.getDefaultValue();
	            if(defaultValue!=null) {
	            	pDefn.setDefaultValue(defaultValue.toString());
	            }
	            // Set the value and original Value
	            if(defaultValue!=null) {
	            	pDefn.setValue(defaultValue.toString());
	            	pDefn.setOriginalValue(defaultValue.toString());
	            // Set Connection URL to template if available and value was null
	            } else if(displayName.equalsIgnoreCase(PropertyItem.CONNECTION_URL_DISPLAYNAME)) {
	                String urlTemplate = TranslatorHelper.getUrlTemplate(driverName);
	                if(!StringUtil.isEmpty(urlTemplate)) {
	                	pDefn.setValue(urlTemplate);
	                	pDefn.setOriginalValue(urlTemplate);
	                }
	            }
	                            
	            // Copy the 'managedconnectionfactory-class' default value into the 'class-name' default value
	            if(name.equals(CLASSNAME_KEY)) {
	            	pDefn.setDefaultValue(rarConnFactoryValue);
	            	pDefn.setValue(rarConnFactoryValue);
	            	pDefn.setOriginalValue(rarConnFactoryValue);
	            	pDefn.setRequired(true);
	            }
				
                // ------------------------
				// Add PropertyObj to List
                // ------------------------
				propDefns.add(pDefn);
			}
		}
		// Order the property items
		Collections.sort(propDefns,propItemComparator);
		
		return propDefns;
	}
		
	/*
	 * Comparator for ordering the PropertyItems before they are passed back to client
	 */
	class PropertyItemComparator implements Comparator<PropertyItem> {
	    public int compare(PropertyItem s1, PropertyItem s2) {
	    	boolean s1IsRequired = s1.isRequired();
	    	boolean s2IsRequired = s2.isRequired();
	    	String s1Name = s1.getName();
	    	String s2Name = s2.getName();
	    	boolean s1IsUsername = isUserNameProp(s1);
	    	boolean s2IsUsername = isUserNameProp(s2);
	    	boolean s1IsPassword = isPasswordProp(s1);
	    	boolean s2IsPassword = isPasswordProp(s2);
	    	boolean s1IsConnUrl = isConnectionUrlProp(s1);
	    	boolean s2IsConnUrl = isConnectionUrlProp(s2);
	    	boolean s1IsClassname = isClassNameProp(s1);
	    	boolean s2IsClassname = isClassNameProp(s2);

	    	// Put username, password, connUrl 1-2-3 regardless of if required
	    	if(s1IsUsername) {
	    		return -1;
	    	} else if(s1IsPassword) {
	    		if(s2IsUsername) {
	    			return 1;
	    		}
	    		return -1;
	    	} else if(s1IsConnUrl) {
	    		if(s2IsUsername || s2IsPassword) {
	    			return 1;
	    		}
	    		return -1;
	    	}
	    	
	    	if(s2IsUsername) {
	    		return 1;
	    	} else if(s2IsPassword) {
	    		if(s1IsUsername) {
	    			return -1;
	    		}
	    		return 1;
	    	} else if(s2IsConnUrl) {
	    		if(s1IsUsername || s1IsPassword) {
	    			return -1;
	    		}
	    		return 1;
	    	}

	    	// Both required
	    	if(s1IsRequired && s2IsRequired) {
	    		if(s1IsClassname) {
	    			return 1;
	    		} else if(s2IsClassname) {
	    			return -1;
	    		} else {
	    			return s1Name.compareToIgnoreCase(s2Name);
	    		}
	    	}
	    	
	    	// Neither required
	    	if( !s1IsRequired  && !s2IsRequired ) {
	    		return s1Name.compareToIgnoreCase(s2Name);
	    	// First only required
	    	} else if (s1IsRequired) {
	    		return -1;
	    	// Second only required
	    	} else {
	    		return 1;
	    	}
	    }
	}	
	
	/*
	 * Determine if the supplied propertyItem is Username
	 * @param propItem the property item
	 * @return 'true' if username property, 'false' if not.
	 */
	private boolean isUserNameProp(PropertyItem propItem) {
		boolean found = false;
		String displayName = propItem.getDisplayName();
		if(displayName!=null) {
			if(displayName.equalsIgnoreCase("user-name") || 
			   displayName.equalsIgnoreCase("User Name") ||
			   displayName.equalsIgnoreCase("Google Account username") ||
			   displayName.equalsIgnoreCase("Ldap Admin User DN") ||
			   displayName.equalsIgnoreCase("Authentication User Name")) {
				found = true;
			}
		}
		return found;
	}
	
	/*
	 * Determine if the supplied propertyItem is Password
	 * @param propItem the property item
	 * @return 'true' if password property, 'false' if not.
	 */
	private boolean isPasswordProp(PropertyItem propItem) {
		boolean found = false;
		String displayName = propItem.getDisplayName();
		if(displayName!=null) {
			if(displayName.equalsIgnoreCase("password") || 
			   displayName.equalsIgnoreCase("Google Account password") ||
			   displayName.equalsIgnoreCase("Ldap Admin Password") ||
			   displayName.equalsIgnoreCase("Authentication User Password")) {
				found = true;
			}
		}
		return found;
	}
	
	/*
	 * Determine if the supplied propertyItem is ConnectionUrl
	 * @param propItem the property item
	 * @return 'true' if ConnectionUrl property, 'false' if not.
	 */
	private boolean isConnectionUrlProp(PropertyItem propItem) {
		boolean found = false;
		String displayName = propItem.getDisplayName();
		if(displayName!=null) {
			if(displayName.equalsIgnoreCase("connection-url") || 
			   displayName.equalsIgnoreCase("Salesforce url") ||
			   displayName.equalsIgnoreCase("Ldap URL") ||
			   displayName.equalsIgnoreCase("Connection url")) {
				found = true;
			}
		}
		return found;
	}
	
	/*
	 * Determine if the supplied propertyItem is ClassName
	 * @param propItem the property item
	 * @return 'true' if classname property, 'false' if not.
	 */
	private boolean isClassNameProp(PropertyItem propItem) {
		boolean found = false;
		String displayName = propItem.getDisplayName();
		if(displayName!=null) {
			if(displayName.equalsIgnoreCase("class-name") || 
			   displayName.equalsIgnoreCase("Class Name") ) {
				found = true;
			}
		}
		return found;
	}
	
    /*
     * Get the default value for the Managed ConnectionFactory class
     * @param propDefns the collection of property definitions
     * @return default value of the ManagedConnectionFactory, null if not found.
     */
    private String getManagedConnectionFactoryClassDefault (Collection<? extends PropertyDefinition> propDefns) {
        String resultValue = null;
        for(PropertyDefinition pDefn : propDefns) {
            if(pDefn.getName().equalsIgnoreCase(CONN_FACTORY_CLASS_KEY)) {
                resultValue=(String)pDefn.getDefaultValue();
                break;
            }
        }
        return resultValue;
    }

    /*
	 * Create the specified VDB "teiid-local" source on the server.  If it already exists, delete it first.
	 * @param vdbName the name of the VDB for the connection
	 */
	private void addVDBSource(String vdbName) throws Exception {
		if(this.admin!=null) {
			// Define Datasource properties to expose VDB as a source
			Map<String,String> propMap = new HashMap<String,String>();
			propMap.put("connection-url","jdbc:teiid:"+vdbName+";useJDBC4ColumnNameAndLabelSemantics=false");
			propMap.put("user-name","user");
			propMap.put("password","user");
			
			// Create the datasource (deletes first, if it already exists)
			String vdbSourceName = vdbName;
			addSource(vdbSourceName, "teiid-local", propMap );
		}
	}

	/*
	 * Create the specified source on the server.  If it already exists, delete it first - then redeploy
	 * @param sourceName the name of the source to add
	 * @param templateName the name of the template for the source
	 * @param sourcePropMap the map of property values for the specified source
	 */
	private void addSource(String sourceName, String templateName, Map<String,String> sourcePropMap) throws Exception {
		if(this.admin!=null) {
			// If 'sourceName' already exists - delete it first...
			deleteDataSource(sourceName);

			// Get properties for the source
			Properties sourceProps = getPropsFromMap(sourcePropMap);

			// Create the specified datasource
			admin.createDataSource(sourceName,templateName,sourceProps);
		}
	}
	
	/*
	 * Delete(undeploy) the specified drivers on the server. 
	 * @param sourceNames the list of sources to delete
	 */
	public List<String> deleteDrivers(List<String> driverNames) throws TeiidServiceException {
		if(this.admin!=null) {
			for(String driverName : driverNames) {
				// Get current list of Drivers.  If 'driverName' is found, undeploy it...
				Collection<String> currentDrivers;
				try {
					currentDrivers = (Collection<String>) this.admin.getDataSourceTemplateNames();
				} catch (AdminException e1) {
					throw new TeiidServiceException(e1.getMessage());
				}
				if(currentDrivers.contains(driverName)) {
					try {
						// Undeploy the driver
						admin.undeploy(driverName);
					} catch (Exception e) {
						throw new TeiidServiceException(e.getMessage());
					}
				}
			}
		}
		return getDataSourceTemplates();
	}
	
	/*
	 * Get a Map of default translators for the dataSources 
	 */
	public Map<String,String> getDefaultTranslatorNames( ) throws TeiidServiceException {
		Map<String,String> defaultTranslatorsMap = new HashMap<String,String>();
		
		List<String> sourceNames = getDataSourceNames();
		List<String> translators = getTranslatorNames();
		
		for(String sourceName : sourceNames) {
			String dsDriver = getDataSourceDriver(sourceName);
			String defaultTranslator = TranslatorHelper.getTranslator(dsDriver,translators);
			defaultTranslatorsMap.put(sourceName,defaultTranslator);
		}
		return defaultTranslatorsMap;
	}
	
	/*
	 * Delete the specified source on the server. 
	 * @param sourceName the name of the source to delete
	 */
	public List<List<DataItem>> deleteDataSource(String sourceName) throws TeiidServiceException {
		if(this.admin!=null) {
			// -------------------------------------
			// Delete the Data Source
			// -------------------------------------
			// Get list of DataSource Names.  If 'sourceName' is found, delete it...
			Collection<String> dsNames;
			try {
				dsNames = admin.getDataSourceNames();
			} catch (AdminException e1) {
				throw new TeiidServiceException(e1.getMessage());
			}
			if(dsNames.contains(sourceName)) {
				try {
					// Undeploy the working VDB
					admin.deleteDataSource(sourceName);
				} catch (Exception e) {
					throw new TeiidServiceException(e.getMessage());
				}
			}
			// -------------------------------------
			// Un-deploy corresponding Source VDBs
			// -------------------------------------
			Collection<String> sourceVDBs = getSourceDynamicVDBNames();
			String srcVdbPrefix = "VDBMgr-"+sourceName+"-";
			for(String sourceVDBName : sourceVDBs) {
				if(sourceVDBName.startsWith(srcVdbPrefix)) {
					// Deployment name for dynamic vdb ends in '-vdb.xml'
					String vdbDeployName = sourceVDBName+"-vdb.xml";
					try {
						// Undeploy the working VDB
						admin.undeploy(vdbDeployName);
					} catch (Exception e) {
						throw new TeiidServiceException(e.getMessage());
					}
				}
			}
			
		}
		return getDataSourceInfos();
	}
	
	/*
	 * Delete the specified sources on the server. 
	 * @param sourceNames the list of sources to delete
	 */
	public List<List<DataItem>> deleteDataSources(List<String> sourceNames) throws TeiidServiceException {
		for(String sourceName : sourceNames) {
			deleteDataSource(sourceName);
		}
		return getDataSourceInfos();
	}
	
	/*
	 * Copy the specified source on the server, giving it the provided new name
	 * @param sourceToCopy the name of the source to copy
	 * @param newSourceName the name of the new source
	 */
	public List<List<DataItem>> copyDataSource(String sourceToCopy, String newSourceName) throws TeiidServiceException {
		if(this.admin!=null) {
			String driverName = null;
			// Get list of DataSource Names.  If 'sourceName' is found, delete it...
			Collection<String> dsNames;
			Properties props = null;
			try {
				dsNames = admin.getDataSourceNames();
				driverName = getDataSourceDriver(sourceToCopy);
				props = admin.getDataSource(sourceToCopy);
			} catch (AdminException e1) {
				throw new TeiidServiceException(e1.getMessage());
			}
			if(dsNames.contains(sourceToCopy)) {
				try {
					// Create the specified datasource
					admin.createDataSource(newSourceName,driverName,props);
				} catch (Exception e) {
					throw new TeiidServiceException(e.getMessage());
				}
			}
		}
		return getDataSourceInfos();
	}

	/*
	 * Convert the Map of property key-value pairs to Properties object
	 * @param propMap the Map of property key-value pairs
	 * @return the corresponding Properties object
	 */
	private Properties getPropsFromMap(Map<String,String> propMap) {
		Properties sourceProps = new Properties();
		Iterator<String> keyIter = propMap.keySet().iterator();
		while(keyIter.hasNext()) {
			String key = keyIter.next();
			String value = propMap.get(key);
			sourceProps.setProperty(key, value);
		}
		return sourceProps;
	}
	
	/*
	 * Add a Model to the VDB for the specified source and translator type.
	 * The VDB is then re-deployed 
	 * @param vdbName name of the VDB
	 * @param sourceName the name of the source to add
	 * @param translatorName the name of the translator for the source
	 */
//	private void addImportVdbToVDB(String vdbName, String importVdb, String translatorName) throws Exception {
//		if(admin!=null) {
//			// Get the VDB
//			Collection<? extends VDB> vdbs = admin.getVDBs();
//			VDBMetaData workVDBMetaData = null;
//			for(VDB vdb : vdbs) {
//				if(vdb.getName()==null || vdb.getName().equalsIgnoreCase(vdbName)) {
//					workVDBMetaData = (VDBMetaData)vdb;
//					workVDBMetaData.setName(vdbName);
//				}
//			}
//			
//			VDBImportMetadata vdbImport = new VDBImportMetadata();
//			vdbImport.setName(importVdb);
//			vdbImport.setVersion(2);
//			workVDBMetaData.getVDBImports().add(vdbImport);
//
//
//			// Add a model to the vdb, then re-deploy it.
//			if(workVDBMetaData!=null) {
//				ModelMetaData modelMetaData = new ModelMetaData();
//				modelMetaData.addSourceMapping(sourceName, translatorName, "java:/"+sourceName);
//				modelMetaData.setName(sourceName+"Model");
//
//				workVDBMetaData.addModel(modelMetaData);
//
//				// Re-Deploy the VDB
//				redeployVDB(vdbName, workVDBMetaData);
//			}
//		}
//	}
		
	/*
	 * Add a Model to the VDB for the specified source and translator type.
	 * The VDB is then re-deployed 
	 * @param vdbName name of the VDB
	 * @param sourceName the name of the source to add
	 * @param translatorName the name of the translator for the source
	 */
//	private void addSourceModelToVDB(String vdbName, String sourceName, String translatorName) throws Exception {
//		if(admin!=null) {
//			// Get the VDB
//	 		VDBMetaData vdb = getVdb(vdbName);
//
//			// Add a model to the vdb, then re-deploy it.
//			if(vdb!=null) {
//				// Create Model
//				ModelMetaData modelMetaData = vdbHelper.createSourceModel(sourceName+"Model", sourceName, translatorName);
//				
//				// Add Model to VDB
//				vdb.addModel(modelMetaData);
//
//				// Re-Deploy the VDB
//				redeployVDB(vdbName, vdb);
//			}
//		}
//	}
	
	/*
	 * Add a View Model to the VDB for the specified viewName.
	 * The VDB is then re-deployed 
	 * @param vdbName name of the VDB
	 * @param viewModelName the name of the viewModel to add
	 * @param ddlString the DDL string to use for the view model
	 */
	public String addOrReplaceViewModelAndRedeploy(String vdbName, String viewModelName, String ddlString) throws TeiidServiceException {
		if(admin!=null) {
			VDBMetaData vdb = getVdb(vdbName,1);
			// Get current vdb imports
			List<VDBImportMetadata> currentVdbImports = getVdbImports(vdb);
			// Get current vdb view models
			List<ModelMetaData> currentViewModels = getVdbViewModels(vdb);
	 		Properties currentProperties = getVdbProperties(vdb);

			// Create a new vdb
			VDBMetaData newVdb = vdbHelper.createVdb(vdbName,1);

			// Create View Model and add to current view models
			ModelMetaData modelMetaData = vdbHelper.createViewModel(viewModelName,ddlString);
			currentViewModels.add(modelMetaData);
			
			// Set ViewModels on new VDB
			newVdb.setModels(currentViewModels);
			
			// Transfer the existing properties
			newVdb.setProperties(currentProperties);

			// Set VDBImports on new VDB
			newVdb.getVDBImports().addAll(currentVdbImports);

			// Re-Deploy the VDB
			try {
				redeployVDB(vdbName, newVdb);
			} catch (Exception e) {
				throw new TeiidServiceException(e.getMessage());
			}
		}
		return "success";
	}
	
	/*
	 * Deploys a SourceVDB for the specified dataSource, if it doesnt already exist
	 * @param sourceVDBName the name of the source VDB
	 * @param dataSourceName the name of the datasource
	 * @param translator the name of the translator
	 */
	public String deploySourceVDB(String sourceVDBName, String dataSourceName, String translator) throws TeiidServiceException {
		if(admin==null) return "failed";

		// Get VDB with the supplied name.  
		// -- If it already exists, return its status
 		VDBMetaData sourceVdb = getVdb(sourceVDBName,1);
 		if(sourceVdb!=null) {
 			String sourceVdbStatus = getVDBStatusMessage(sourceVDBName);
 			if(!sourceVdbStatus.equals("success")) {
 				return "<bold>The Source could not be added: <br>  - Error deploying Source VDB '"+
 						dataSourceName+"', with translator '"+translator+"'</bold><br><br>"+sourceVdbStatus;
 			}
 			return sourceVdbStatus;
 		}

		// Deployment name for vdb must end in '-vdb.xml'. 
 		String deploymentName = sourceVDBName+"-vdb.xml";
 		
 		// Create a new Source VDB to deploy
 		sourceVdb = vdbHelper.createVdb(sourceVDBName,1);
 		
 		// Create source model - same name as dataSource
		ModelMetaData model = vdbHelper.createSourceModel(dataSourceName, dataSourceName, translator);
		
		// Adding the SourceModel to the VDB 
		sourceVdb.addModel(model);

		// If it exists, undeploy it
		try {
			byte[] vdbBytes = vdbHelper.getVdbByteArray(sourceVdb);

			// Deploy the VDB
			admin.deploy(deploymentName, new ByteArrayInputStream(vdbBytes));
			
			// Wait for VDB to finish loading
			waitForVDBLoad(this.admin, sourceVDBName, 1, 120);			
		} catch (Exception e) {
			throw new TeiidServiceException(e.getMessage());
		}
		
		// Get deployed VDB and return status
		String vdbStatus = getVDBStatusMessage(sourceVDBName);
		if(!vdbStatus.equals("success")) {
			return "<bold>Error deploying VDB "+sourceVDBName+"</bold><br>"+vdbStatus;
		}
 		return "success";
	}
	
	/*
	 * Undeploy the current deployed VDB, re-deploy the supplied VDBMetadata, then define
	 * the VDB as a Source
	 * @param vdbName name of the VDB
	 * @param vdb the VDBMetaData object
	 */
	private void redeployVDB(String vdbName, VDBMetaData vdb) throws Exception {
		// redeploy the VDB using the supplied deployment name
		if(admin!=null && vdb!=null) {
			// output using VDBMetadataParser
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			VDBMetadataParser.marshell(vdb, out);
			
			// Deployment name for vdb must end in '-vdb.xml'
			String vdbDeployName = vdbName+"-vdb.xml";
			// Undeploy the working VDB
			admin.undeploy(vdbDeployName);

			// Deploy the updated VDB
			admin.deploy(vdbDeployName, new ByteArrayInputStream(out.toByteArray()));
			
			// Wait for VDB to finish loading
			waitForVDBLoad(this.admin, vdbName, 1, 120);

			// Add the VDB as a source.  If it already exists, it is deleted first then recreated.
			// Re-create is required to clear the connection pool.
			addVDBSource(vdbName);
		}
	}
	
    public boolean save(String filePath, VDBMetaData vdb) throws Exception {
        File outFile = new File(filePath);
        FileOutputStream outStream = new FileOutputStream(outFile);
        VDBMetadataParser.marshell(vdb, outStream);
        return true;
    }

    /**
     * Get the list of PropertyItems for the supplied DataSource name - from the TeiidServer
     * @param dsName the data source name
     * @return the list of PropertyItem
     */
    public List<PropertyItem> getDataSourcePropertyItems(String dsName) throws TeiidServiceException  {
        
        // Get the driver template properties
        String driverName = getDataSourceDriver(dsName);
        List<PropertyItem> propertyItems = getDriverPropertyItems(driverName);
        
        if(!propertyItems.isEmpty()) {
            
            // Get the data source specific properties
            Properties props = null;
            try {
                // Get the specific property values for this data source
                props = admin.getDataSource(dsName);
            } catch (Exception ex) {
            	// If exception is thrown, no properties are set.
                props = new Properties();
            }
            
            // Set the template property values to data source specific value
            for(PropertyItem propItem: propertyItems) {
                String propName = propItem.getName();
                String propValue = props.getProperty(propName);
                if(props.containsKey(propName)) {
                    propValue = props.getProperty(propName);
                    if(propValue!=null) {
                        propItem.setValue(propValue);
                        propItem.setOriginalValue(propValue);
                    }
                }
            }
        }
        
        return propertyItems;
    }
    
    /**
     * Get the XML content for the specified VDB
     * @param vdbName the VDB name
     * @return the VDB string
     */
    public String getVdbXml(String vdbName) throws TeiidServiceException {
    	VDBMetaData vdb = getVdb(vdbName,1);
    	String vdbXml = "";
    	 try {
			vdbXml = vdbHelper.getVdbString(vdb);
		} catch (Exception e) {
			throw new TeiidServiceException(e.getMessage());
		}
    	return vdbXml;
    }
    
    /**
     * Get the Driver name for the supplied DataSource name - from the TeiidServer
     * @param dsName the data source name
     * @return the dataSource driver name
     */
    public String getDataSourceDriver(String dsName) throws TeiidServiceException {
        String driverName = null;
        Properties props = new Properties();
        try {
            props = admin.getDataSource(dsName);
        } catch (Exception e) {
			throw new TeiidServiceException("["+driverName+"] "+e.getMessage());
        }
        return getDataSourceDriver(props);
    }
    
    /**
     * Get the Driver name for the supplied DataSource name - from the TeiidServer
     * @param dsProps the data source properties
     * @return the dataSource driver name
     */
    private String getDataSourceDriver(Properties dsProps) {
    	if(dsProps==null) return "unknown";
    	
        String driverName = dsProps.getProperty(DRIVER_KEY);
        // If driver-name not found, look for class name and match up the .rar
        if(StringUtil.isEmpty(driverName)) {
            String className = dsProps.getProperty(CLASSNAME_KEY);
            if(!StringUtil.isEmpty(className)) {
            	driverName = TranslatorHelper.getDriverNameForClass(className);
            }
        }
        return driverName;
    }
    
        
    /**
     * Determine if this is a 'rar' type driver that is deployed with Teiid
     * @param driverName the name of the driver
     * @return 'true' if the driver is a rar driver, 'false' if not.
     */
    private boolean isRarDriver(String driverName) {
    	boolean isRarDriver = false;
    	if(!StringUtil.isEmpty(driverName)) {
    		if(   driverName.equals(TranslatorHelper.TEIID_FILE_DRIVER) || driverName.equals(TranslatorHelper.TEIID_GOOGLE_DRIVER)
    		   || driverName.equals(TranslatorHelper.TEIID_INFINISPAN_DRIVER) || driverName.equals(TranslatorHelper.TEIID_LDAP_DRIVER)
    		   || driverName.equals(TranslatorHelper.TEIID_MONGODB_DRIVER) || driverName.equals(TranslatorHelper.TEIID_SALESORCE_DRIVER) 
    		   || driverName.equals(TranslatorHelper.TEIID_WEBSERVICE_DRIVER)) {
    			isRarDriver = true;
    		}
    	}
    	
    	return isRarDriver;
    }
    
//========================================================================
  //========================================================================

    /*
	 * Get List of all available Datasource Names.  This will refresh the Map of datasources, then
	 * return the datasource names.
	 * @param teiidOnly 'true' if only Teiid sources are to be returned, 'false' otherwise.
	 * @return the array of datasource names
	 */
	public String[] getAllDataSourceNames(boolean teiidOnly) {
		// Refresh the Map of Sources
		refreshDataSourceMap();
        
		// Get DataSource names
		List<String> resultList = new ArrayList<String>();
		
		Set<String> dsNames = mDatasources.keySet();
		Iterator<String> nameIter = dsNames.iterator();
		while(nameIter.hasNext()) {
			String dsName = nameIter.next();
			if(dsName!=null && !dsName.startsWith("java:/PREVIEW_")) {
				DataSource ds = mDatasources.get(dsName);
				if(!teiidOnly) {
					resultList.add(dsName);
				} else if(isTeiidSource(ds)) {
					resultList.add(dsName);
				}
			}
		}
		String[] resArray = new String[resultList.size()];
		int i=0;
		for(String name: resultList) {
			resArray[i] = name;
			i++;
		}
		return resArray;
	}
	
	/*
	 * @see org.teiid.tools.webquery.client.TeiidService#getTableAndColMap(java.lang.String)
	 */
	public Map<String,List<String>> getTableAndColMap(String dataSource) {
		
		// Get a connection for the supplied data source name
		Connection connection = getConnection(dataSource);
		
		// Result map of TableName with it's associated ColumnNames
		Map<String,List<String>> resultMap = new HashMap<String,List<String>>();

		boolean noTablesOrProcs = false;
		String[] tables = null;
		String[] procs = null;
		// Get Tables and Procedures for the Datasource
		if(connection==null || (dataSource!=null && dataSource.equalsIgnoreCase("NO SOURCES"))) {
			noTablesOrProcs = true;
		} else {
			// Get Tables and Procs for the Datasource
			tables = getTables(connection);
			procs = getProcedures(connection);
			
			// Determine if zero tables and procs
			if(tables.length==0 && procs.length==0) {
				noTablesOrProcs = true;
			}
		}

		// If there are zero tables and procedures, we can return here
		if(noTablesOrProcs) {
			// Return resultMap with "No Tables or Procs" designator
			resultMap.put("NO TABLES OR PROCS|TABLE", new ArrayList<String>());
			
			// Close Connection
			closeConnection(connection);
			
			return resultMap;
		}
		
		// Get Columns for each table.  Put entry in the Map
		for(int i=0; i<tables.length; i++) {
			String[] cols = getColumnsForTable(connection,tables[i]);
			List<String> colList = Collections.emptyList();
			if(cols!=null) {
				colList = Arrays.asList(cols);
			}
			// Add 'TABLE' designator onto Map Key
			resultMap.put(tables[i]+"|TABLE", colList);
		}
		
		// Get Columns for each procedure.  Put entry in the Map
		for(int i=0; i<procs.length; i++) {
			String[] cols = getColumnsForProcedure(connection,procs[i]);
			List<String> colList = Collections.emptyList();
			if(cols!=null) {
				colList = Arrays.asList(cols);
			}
			// Add 'PROC' designator onto Map Key
			resultMap.put(procs[i]+"|PROC", colList);
		}
		
		// Close the connection when finished
		if(connection!=null) {
			try {
				connection.close();
			} catch (SQLException e2) {

			}
		}
		
		return resultMap;
	}

	/*
	 * (non-Javadoc)
	 * @see org.teiid.tools.webquery.client.TeiidService#executeSql(java.lang.String, java.lang.String)
	 */
	public List<List<DataItem>> executeSql(String dataSource, String sql) throws SQLProcException {
		List<List<DataItem>> rowList = new ArrayList<List<DataItem>>();
		
		// Get connection for the datasource.  create a SQL Statement to issue the query.
		Connection connection = null;
		try {
			connection = getConnection(dataSource);
			if(connection!=null && sql!=null && sql.trim().length()>0) {
				sql = sql.trim();
				Statement stmt = connection.createStatement();
                String sqlUpperCase = sql.toUpperCase();
                
				// INSERT / UPDATE / DELETE - execute as an Update 
				if(sqlUpperCase.startsWith(INSERT) || sqlUpperCase.startsWith(UPDATE) || sqlUpperCase.startsWith(DELETE)) {
					int rowCount = stmt.executeUpdate(sql);
					List<DataItem> resultRow = new ArrayList<DataItem>();
					resultRow.add(new DataItem(rowCount+" Rows Updated",""));
					rowList.add(resultRow);	
				// SELECT
				} else {
					ResultSet resultSet = stmt.executeQuery(sql);

					int columnCount = resultSet.getMetaData().getColumnCount();
					List<DataItem> columnNames = new ArrayList<DataItem>();
					for (int i=1 ; i<=columnCount ; ++i) {
						columnNames.add(new DataItem(resultSet.getMetaData().getColumnName(i),"string"));
					}

					rowList.add(columnNames);

					if (!resultSet.isAfterLast()) {
						while (resultSet.next()) {
							List<DataItem> rowData = new ArrayList<DataItem>(columnCount);
							for (int i=1 ; i<=columnCount ; ++i) {
								rowData.add(createDataItem(resultSet,i));
							}
							rowList.add(rowData);
						}
					}
					resultSet.close();
				}
				stmt.close();
			}
		} catch (Exception e) {
			if(connection!=null) {
				try {
					connection.rollback();
				} catch (SQLException e2) {

				}
			}
			throw new SQLProcException(e.getMessage());
		} finally {
			if(connection!=null) {
				try {
					connection.close();
				} catch (SQLException e2) {

				}
			}
		}
		return rowList;
	}
	
	/*
	 * Determine if the data source is a Teiid source
	 * @param dataSource the data source
	 * @return 'true' if the source is a Teiid source
	 */
	private boolean isTeiidSource(DataSource dataSource) {
		boolean isVdb = false;
		Connection conn = null;
		if(dataSource!=null) {
			try {
				conn = dataSource.getConnection();
				if(conn!=null) {
					String driverName = conn.getMetaData().getDriverName();
					if(driverName!=null && driverName.trim().toLowerCase().startsWith(TEIID_DRIVER_PREFIX)) {
						isVdb = true;
					}
				}
			} catch (SQLException e) {
			} finally {
				if(conn!=null) {
					try {
						conn.close();
					} catch (SQLException e) {
					}
				}
			}
		}
		return isVdb;
	}

	/*
	 * Get List of Tables using the supplied connection
	 * @param connection the JDBC connection
	 * @return the array of table names
	 */
	private String[] getTables(Connection connection) {
		// Get the list of Tables
		List<String> tableNameList = new ArrayList<String>();
		List<String> tableSchemaList = new ArrayList<String>();
		if(connection!=null) {
			try {
				ResultSet resultSet = connection.getMetaData().getTables(null, null, "%", new String[]{"DOCUMENT", "TABLE", "VIEW"});
				int columnCount = resultSet.getMetaData().getColumnCount();
				while (resultSet.next()) {
					String tableName = null;
					String tableSchema = null;
					for (int i=1 ; i<=columnCount ; ++i) {
						String colName = resultSet.getMetaData().getColumnName(i);
						String value = resultSet.getString(i);
						if (colName.equalsIgnoreCase(TABLE_NAME)) {
							tableName = value;
						} else if(colName.equalsIgnoreCase(TABLE_SCHEM)) {
							tableSchema = value;
						}
					}
					tableNameList.add(tableName);
					tableSchemaList.add(tableSchema);
				}
				resultSet.close();
			} catch (Exception e) {
				if(connection!=null) {
					try {
						connection.rollback();
					} catch (SQLException e2) {

					}
				}
			} 
		}
		
		// Build full names if schemaName is present
		String[] tableNames = new String[tableNameList.size()];
		for(int i=0; i<tableNameList.size(); i++) {
			String schemaName = tableSchemaList.get(i);
			if(schemaName!=null && schemaName.length()>0) {
				tableNames[i]=schemaName+"."+tableNameList.get(i);
			} else {
				tableNames[i]=tableNameList.get(i);
			}
		}
		return tableNames;
	}

	/*
	 * Get List of Procedures using the supplied connection
	 * @param connection the JDBC connection
	 * @return the array of procedure names
	 */
	private String[] getProcedures(Connection connection) {
		// Get the list of Procedures
		List<String> procNameList = new ArrayList<String>();
		List<String> procSchemaList = new ArrayList<String>();
		if(connection!=null) {
			try {
				ResultSet resultSet = connection.getMetaData().getProcedures(null, null, "%");
				int columnCount = resultSet.getMetaData().getColumnCount();
				while (resultSet.next()) {
					String procName = null;
					String procSchema = null;
					for (int i=1 ; i<=columnCount ; ++i) {
						String colName = resultSet.getMetaData().getColumnName(i);
						String value = resultSet.getString(i);
						if (colName.equalsIgnoreCase(PROCEDURE_NAME)) {
							procName = value;
						} else if(colName.equalsIgnoreCase(PROCEDURE_SCHEM)) {
							procSchema = value;
						}
					}
					if(procSchema!=null && !procSchema.equalsIgnoreCase(SYS) && !procSchema.equalsIgnoreCase(SYSADMIN)) {
						procNameList.add(procName);
						procSchemaList.add(procSchema);
					}
				}
				resultSet.close();
			} catch (Exception e) {
				if(connection!=null) {
					try {
						connection.rollback();
					} catch (SQLException e2) {

					}
				}
			} 
		}
		
		// Build full names if schemaName is present
		String[] procNames = new String[procNameList.size()];
		for(int i=0; i<procNameList.size(); i++) {
			String schemaName = procSchemaList.get(i);
			if(schemaName!=null && schemaName.length()>0) {
				procNames[i]=schemaName+"."+procNameList.get(i);
			} else {
				procNames[i]=procNameList.get(i);
			}
		}
		return procNames;
	}

	/*
	 * Get List of Column names using the supplied connection and table name
	 * @param connection the JDBC connection
	 * @param fullTableName the Table name to get columns
	 * @return the array of Column names
	 */
	private String[] getColumnsForTable(Connection connection,String fullTableName) {
		if(connection==null || fullTableName==null || fullTableName.trim().isEmpty()) {
			return null;
		}
		List<String> columnNameList = new ArrayList<String>();
		List<String> columnTypeList = new ArrayList<String>();
		String schemaName = null;
		String tableName = null;
		int indx = fullTableName.lastIndexOf(".");
		if(indx!=-1) {
			schemaName = fullTableName.substring(0, indx);
			tableName = fullTableName.substring(indx+1);
		} else {
			tableName = fullTableName;
		}
		
		// Get the column name and type for the supplied schema and tableName
		try {
			ResultSet resultSet = connection.getMetaData().getColumns(null, schemaName, tableName, null);
			while(resultSet.next()) {
				String columnName = resultSet.getString(COLUMN_NAME);
				String columnType = resultSet.getString(TYPE_NAME);
				columnNameList.add(columnName);
				columnTypeList.add(columnType);
			}
			resultSet.close();
		} catch (Exception e) {
			if(connection!=null) {
				try {
					connection.rollback();
				} catch (SQLException e2) {

				}
			}
		} 
		
		// Pass back a delimited name|type result string
		String[] columns = new String[columnNameList.size()];
		for(int i=0; i<columnNameList.size(); i++) {
			String columnName = columnNameList.get(i);
			String columnType = columnTypeList.get(i);
			if(columnType!=null && columnType.length()>0) {
				columns[i]=columnName+"|"+columnType;
			} else {
				columns[i]=columnName;
			}
		}
		return columns;
	}

	/*
	 * Get List of Column names using the supplied connection and procedure name
	 * @param connection the JDBC connection
	 * @param fullProcName the Procedure name to get columns
	 * @return the array of Column names
	 */
	private String[] getColumnsForProcedure(Connection connection,String fullProcName) {
		if(connection==null || fullProcName==null || fullProcName.trim().isEmpty()) {
			return null;
		}
		List<String> columnNameList = new ArrayList<String>();
		List<String> columnDataTypeList = new ArrayList<String>();
		List<String> columnDirTypeList = new ArrayList<String>();
		String schemaName = null;
		String procName = null;
		int indx = fullProcName.lastIndexOf(".");
		if(indx!=-1) {
			schemaName = fullProcName.substring(0, indx);
			procName = fullProcName.substring(indx+1);
		} else {
			procName = fullProcName;
		}
		
		// Get the column name and type for the supplied schema and procName
		try {
			ResultSet resultSet = connection.getMetaData().getProcedureColumns(null, schemaName, procName, null);
			while(resultSet.next()) {
				String columnName = resultSet.getString(COLUMN_NAME);
				String columnType = resultSet.getString(COLUMN_TYPE);
				String columnDataType = resultSet.getString(TYPE_NAME);
				columnNameList.add(columnName);
				columnDataTypeList.add(columnDataType);
				columnDirTypeList.add(getProcColumnDirType(columnType));
			}
			resultSet.close();
		} catch (Exception e) {
			if(connection!=null) {
				try {
					connection.rollback();
				} catch (SQLException e2) {

				}
			}
		} 
		
		// Pass back a delimited name|type result string
		String[] columns = new String[columnNameList.size()];
		for(int i=0; i<columnNameList.size(); i++) {
			String columnName = columnNameList.get(i);
			String columnDataType = columnDataTypeList.get(i);
			if(columnDataType!=null && columnDataType.length()>0) {
				columns[i]=columnName+"|"+columnDataType;
			} else {
				columns[i]=columnName;
			}
		}
		return columns;
	}
	
	/*
	 * Interprets the procedure column type codes from jdbc call to strings
	 * @intStr the stringified code
	 * @return the direction type
	 */
	private String getProcColumnDirType(String intStr) {
		String result = "UNKNOWN";
		if(intStr!=null) {
			if(intStr.trim().equals("1")) {
				result = "IN";
			} else if(intStr.trim().equals("2")) {
				result = "INOUT";
			} else if(intStr.trim().equals("4")) {
				result = "OUT";
			} else if(intStr.trim().equals("3")) {
				result = "RETURN";
			} else if(intStr.trim().equals("5")) {
				result = "RESULT";
			} 		
		}
		return result;
	}

	/*
	 * Create a DataItem to pass back to client for each result
	 * @param resultSet the SQL ResultSet
	 * @param index the ResultSet index for the object
	 * @return the DataItem result
	 */
	private DataItem createDataItem(ResultSet resultSet, int index) throws SQLException {
		DataItem resultItem = null;
		String type = "string";
		Object obj = resultSet.getObject(index);
		
		String className = null;
		if(obj!=null) {
			className = obj.getClass().getName();
			if(className.equals("org.teiid.core.types.SQLXMLImpl")) {
				type = "xml";
			}
		}
		
		if(obj instanceof javax.sql.rowset.serial.SerialBlob) {
			byte[] bytes = ((SerialBlob)obj).getBytes(1, 500);
			resultItem = new DataItem(Arrays.toString(bytes),type);
		} else {
			String value = resultSet.getString(index);
			resultItem = new DataItem(value,type);
		}
		return resultItem;
	}
	
	/*
	 * Get Connection for the specified DataSource Name from the Map of DataSources
	 */
	private Connection getConnection (String datasourceName) {
		Connection connection = null;
		if(mDatasources!=null) {
			DataSource dataSource = (DataSource) mDatasources.get(datasourceName);
			if(dataSource!=null) {
				try {
					connection = dataSource.getConnection();
				} catch (SQLException e) {
				}
			}
		}
		return connection;
	}
	
	/*
	 * Close the supplied connection
	 */
	private void closeConnection(Connection conn) {
		if(conn!=null) {
			try {
				conn.close();
			} catch (SQLException e) {
			}
		}
	}
	
	/*
	 * Refresh the DataSource Maps
	 */
	private void refreshDataSourceMap( ) {
		// Clear the DataSource Maps
		mDatasources.clear();
		mDatasourceSchemas.clear();

		// New Context
		if(context==null) {
			try {
				context = new InitialContext();
			} catch (Exception e) {
			}
		}
		if(context==null) return;
		
		NamingEnumeration<javax.naming.NameClassPair> ne = null;
		// Try the list of possible context names
		for(String jdbcContext : JDBC_CONTEXTS) {
			try {
				Context theJdbcContext = (Context) context.lookup(jdbcContext);
				ne = theJdbcContext.list("");
			} catch (NamingException e1) {
				System.out.println("Error with lookup");
			}

			while (ne!=null && ne.hasMoreElements()) {
				javax.naming.NameClassPair o = (javax.naming.NameClassPair) ne.nextElement();
				Object bindingObject = null;

				try {
					if (o.getClassName().equals(WRAPPER_DS) || o.getClassName().equals(WRAPPER_DS_AS7)) {
						bindingObject = context.lookup(jdbcContext + o.getName());
					} 
				} catch (NamingException e1) {
					System.out.println("Error with lookup of "+o.getName());
				}

				if(bindingObject!=null && bindingObject instanceof DataSource && !o.getName().equalsIgnoreCase("ModeShapeDS")) {
					// Put DataSource into datasource Map
					String key = jdbcContext.concat(o.getName());
					mDatasources.put(key, (DataSource)bindingObject);

					// Put Schema into schema Map
					String schema = null;
					try {
						schema = (String) context.lookup("java:comp/env/schema/" + key);
					} catch (NamingException e) {

					}
					mDatasourceSchemas.put(key, schema);
				}
			}
		}
	}
    
	
}
