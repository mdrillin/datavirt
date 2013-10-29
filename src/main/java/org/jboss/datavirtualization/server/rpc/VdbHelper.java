package org.jboss.datavirtualization.server.rpc;

import java.io.ByteArrayOutputStream;

import org.teiid.adminapi.Model;
import org.teiid.adminapi.impl.ModelMetaData;
import org.teiid.adminapi.impl.VDBImportMetadata;
import org.teiid.adminapi.impl.VDBMetaData;
import org.teiid.adminapi.impl.VDBMetadataParser;

public class VdbHelper {

	// ============================================
    // Static Variables

    private static VdbHelper instance = new VdbHelper();

    // ============================================
    // Static Methods
    /**
     * Get the singleton instance
     * 
     * @return instance
     */
    public static VdbHelper getInstance() {
        return instance;
    }
    
    /*
     * Create a VdbManager 
     */
    private VdbHelper() {
    }

    /**
     * Create a VDB object
     * @param vdbName the name of the VDB
     * @param vdbVersion the vdb version
     * @return the VDBMetadata
     */
    public VDBMetaData createVdb(String vdbName, int vdbVersion) {
    	VDBMetaData vdb = new VDBMetaData();
    	vdb.setName(vdbName);
		vdb.setDescription("VDB for: "+vdbName+", Version: "+vdbVersion);
    	vdb.setVersion(vdbVersion);
    	return vdb;
    }
    
    /**
     * Get the bytearray version of the VDBMetaData object
     * @param vdb the VDB
     * @return the vdb in bytearray form
     */
    public byte[] getVdbByteArray(VDBMetaData vdb) throws Exception {
    	ByteArrayOutputStream out = new ByteArrayOutputStream();
    	VDBMetadataParser.marshell(vdb, out);

    	return out.toByteArray();
    }
    
    /**
     * Get the stringified version of the VDBMetaData object
     * @param vdb the VDB
     * @return the vdb in string form
     */
    public String getVdbString(VDBMetaData vdb) throws Exception {
    	return new String(getVdbByteArray(vdb));
    }

    /**
     * Create a VDB import object
     * @param vdbName the name of the VDB to import
     * @param vdbVersion the vdb version
     * @return the VDBImportMetadata
     */
    public VDBImportMetadata createVdbImport(String vdbName, int vdbVersion) {
    	VDBImportMetadata vdbImport = new VDBImportMetadata();
    	vdbImport.setName(vdbName);
    	vdbImport.setVersion(vdbVersion);
    	return vdbImport;
    }
	
    /**
     * Create a Source Model
     * @param modelName the name of the Model
     * @param sourceName the name of the jndi source
     * @param translator the translator name
     * @return the ModelMetaData
     */
	public ModelMetaData createSourceModel(String modelName, String sourceName, String translator) {
		ModelMetaData modelMetaData = new ModelMetaData();
		modelMetaData.addSourceMapping(sourceName, translator, "java:/"+sourceName);
		modelMetaData.setName(modelName);
		return modelMetaData;
	}
	
    /**
     * Create a View Model
     * @param modelName the name of the Model
     * @param ddl the DDL which defines the view
     * @return the ModelMetaData
     */
	public ModelMetaData createViewModel(String modelName, String ddl) {
		ModelMetaData modelMetaData = new ModelMetaData();
		modelMetaData.setName(modelName);
		modelMetaData.setModelType(Model.Type.VIRTUAL);
		modelMetaData.setSchemaSourceType("DDL");
		modelMetaData.setSchemaText(ddl);
		return modelMetaData;
	}
	    
}
