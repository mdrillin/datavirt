package org.jboss.datavirtualization.client.rpc;

import java.util.List;
import java.util.Map;

import org.jboss.datavirtualization.client.DataItem;
import org.jboss.datavirtualization.client.PropertyItem;
import org.jboss.datavirtualization.client.dialogs.AddEditModelDialog.ModelType;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * The client side stub for the RPC service.
 */
@RemoteServiceRelativePath("teiid")
public interface TeiidMgrService extends RemoteService {

	  Boolean isRunningOnOpenShift();
	  
	  List<DataItem> initApplication(int serverPort, String userName, String password) throws TeiidServiceException;
	  
	  List<String> getDynamicVDBNames() throws TeiidServiceException;
		
	  List<DataItem> getVDBItems() throws TeiidServiceException;

	  List<String> getDataSourceTemplates() throws TeiidServiceException;
		
	  Map<String,List<PropertyItem>> getDefaultPropertyItemMap() throws TeiidServiceException;

	  List<String> getPropertyNames(String templateName) throws TeiidServiceException;
	  
	  List<PropertyItem> getDriverPropertyItems(String driverName) throws TeiidServiceException;

	  List<PropertyItem> getDataSourcePropertyItems(String dataSourceName) throws TeiidServiceException;
	   
	  List<String> getDataSourceNames() throws TeiidServiceException;
	  
	  List<String> getTranslatorNames() throws TeiidServiceException;

	  Map<String,String> getDefaultTranslatorNames() throws TeiidServiceException;
	  
	  List<DataItem> createVDB(String vdbName) throws TeiidServiceException;
	  
	  List<DataItem> createSampleVDB(String vdbName) throws TeiidServiceException;

	  List<DataItem> deleteVDB(String vdbName) throws TeiidServiceException;
	  
	  String getVdbXml(String vdbName) throws TeiidServiceException;
	  
	  List<List<DataItem>> getDataSourceInfos() throws TeiidServiceException;
 
	  List<List<DataItem>> getVDBImportInfo(String vdbName) throws TeiidServiceException;
	  
	  List<List<DataItem>> getVDBModelInfo(String vdbName) throws TeiidServiceException;
	  
	  String deploySourceVDBAddImportAndRedeploy(String vdbName, String sourceVDBName, String dataSourceName, String translator) throws TeiidServiceException;
	  
	  String addImportAndRedeploy(String vdbName, String importVdbName, int importVdbVersion) throws TeiidServiceException;

	  List<List<DataItem>> removeImportsAndRedeploy(String vdbName, List<String> removeImportNameList) throws TeiidServiceException;		

	  List<List<DataItem>> removeModelsAndRedeploy(String vdbName, List<String> removeModelNameList, List<ModelType> removeModelTypeList) throws TeiidServiceException;		

	  List<List<DataItem>> deleteDataSource(String sourceName) throws TeiidServiceException;
	  
	  List<List<DataItem>> deleteDataSources(List<String> sourceNames) throws TeiidServiceException;

	  List<String> deleteDrivers(List<String> driverNames) throws TeiidServiceException;

	  List<List<DataItem>> copyDataSource(String sourceToCopy, String newSourceName) throws TeiidServiceException;

      String deploySourceVDB(String sourceVDBName, String dataSourceName, String translator) throws TeiidServiceException;

      String addOrReplaceViewModelAndRedeploy(String vdbName, String modelName, String ddl) throws TeiidServiceException;

	  String addDataSource(String sourceName, String templateName, Map<String,String> sourcePropMap ) throws TeiidServiceException;

	  String addDataSources(List<String> sourceNames, List<String> templateNames, Map<String,Map<String,String>> sourcesPropMap ) throws TeiidServiceException;

	  String[] getAllDataSourceNames(boolean teiidOnly) throws Exception;
	  
	  Map<String,List<String>> getTableAndColMap(String dataSource);

	  List<List<DataItem>> executeSql(String dataSource, String sql) throws Exception;  
	    
}
