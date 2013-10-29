package org.jboss.datavirtualization.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Helper containing static methods for DataSource-related tasks. 
 */
public class DataSourceHelper {

	private static final String CLASS_DISPLAY_NAME="Class Name";
	private static final String CLASS_NAME="class-name";
	private static final String CONNECTION_FACTORY_FILE="org.teiid.resource.adapter.file.FileManagedConnectionFactory";
	private static final String CONNECTION_FACTORY_WEBSERVICE="org.teiid.resource.adapter.ws.WSManagedConnectionFactory";
	private static final String CONNECTION_FACTORY_SALESFORCE="org.teiid.resource.adapter.salesforce.SalesForceManagedConnectionFactory";
	private static final String CONNECTION_FACTORY_LDAP="org.teiid.resource.adapter.ldap.LDAPManagedConnectionFactory";
	private static final String CONNECTION_FACTORY_MONGODB="org.teiid.resource.adapter.mongodb.MongoDBManagedConnectionFactory";

	public static final String FILE_RARFILE = "file"; //$NON-NLS-1$
	public static final String WEBSERVICE_RARFILE = "webservice"; //$NON-NLS-1$
	public static final String SALESFORCE_RARFILE = "salesforce"; //$NON-NLS-1$
	public static final String LDAP_RARFILE = "ldap"; //$NON-NLS-1$
	public static final String MONGODB_RARFILE = "mongodb"; //$NON-NLS-1$

	private static final String DDL_NAME = "Views-DDL";
	private static final String DDL_DISPLAY_NAME = "View DDL";

	public static final String FILE_DISPLAYNAME = "File"; //$NON-NLS-1$
	public static final String WEBSERVICE_DISPLAYNAME = "WebService"; //$NON-NLS-1$
	public static final String SALESFORCE_DISPLAYNAME = "Salesforce"; //$NON-NLS-1$
	public static final String LDAP_DISPLAYNAME = "LDAP"; //$NON-NLS-1$
	public static final String MONGODB_DISPLAYNAME = "MongoDB"; //$NON-NLS-1$
	
	public static final String FILE_TRANSLATOR = "file"; //$NON-NLS-1$
	public static final String WEBSERVICE_TRANSLATOR = "ws"; //$NON-NLS-1$
	public static final String SALESFORCE_TRANSLATOR = "salesforce"; //$NON-NLS-1$
	public static final String LDAP_TRANSLATOR = "ldap"; //$NON-NLS-1$
	public static final String MONGODB_TRANSLATOR = "mongodb"; //$NON-NLS-1$

	public static final String TRANS_VIEWMODEL = "teiid"; //$NON-NLS-1$
	public static final String VIEW_MODEL = "ViewModel"; //$NON-NLS-1$

	public static final String OPENSHIFT_MYSQL_SOURCETYPE = "mysql"; //$NON-NLS-1$
	public static final String OPENSHIFT_POSTGRES_SOURCETYPE = "postgres"; //$NON-NLS-1$

	// Allowed Source Types - filters available sources when running on OpenShift
//    private static final List<String> ALLOWED_SOURCETYPES = new ArrayList<String>(){
//    	private static final long serialVersionUID = 1L;
//        {
//            add(WEBSERVICE_RARFILE.toUpperCase());
//            add(SALESFORCE_RARFILE.toUpperCase());
//            add(VIEW_MODEL.toUpperCase());
//            add(OPENSHIFT_MYSQL_SOURCETYPE.toUpperCase());
//            add(OPENSHIFT_POSTGRES_SOURCETYPE.toUpperCase());
//        }
//    };
	
	private static final String PROP_CONN_URL = "connection-url";
	private static final String PROP_USERNAME1 = "username";
	private static final String PROP_USERNAME2 = "user-name";
	private static final String PROP_PARENT_DIR = "ParentDirectory";

	private static final DataSourceHelper INSTANCE = new DataSourceHelper();

	private Map<String,List<PropertyItem>> defaultPropertiesMap = new HashMap<String,List<PropertyItem>>();

	private DataSourceHelper() {}

	public static DataSourceHelper getInstance() {
		return INSTANCE;
	}

	/**
	 * Set the defaults Map
	 * @param defaultPropsMap the map of default properties
	 */
	public void setDefaults(Map<String,List<PropertyItem>> defaultPropsMap) {
		this.defaultPropertiesMap.clear();
		if(defaultPropsMap!=null) {
			this.defaultPropertiesMap.putAll(defaultPropsMap);
		}
	}
	
	public Map<String,List<PropertyItem>> getDefaultsMap() {
		return this.defaultPropertiesMap;
	}

	/**
	 * Get the default property list for a source
	 * @param sourceType
	 * @return the List of default properties
	 */
	public List<PropertyItem> getDefaultProps(String sourceType) {
		return this.defaultPropertiesMap.get(sourceType);
	}

	/*
	 * Filter the supplied list, leaving only the list of allowed types
	 * @param dataSourceTypes the full list of DataSource Types
	 * @return the filtered list of DataSource Types
	 */
	public static List<String> filterAllowedTypes(Collection<String> dataSourceTypes, boolean runningOnOpenShift) {
		List<String> filteredTypes = new ArrayList<String>();
		
		// Filtering disabled currently
		
		for(String sourceType: dataSourceTypes) {
//			if(runningOnOpenShift && ALLOWED_SOURCETYPES.contains(sourceType.toUpperCase())) {
//				filteredTypes.add(sourceType);
//			} else if(!runningOnOpenShift){
				filteredTypes.add(sourceType);
//			}
 		}
		
		// Sort the typeNames before returning
		Collections.sort(filteredTypes);
		
		return filteredTypes;
	}

	/*
	 * Convert Template Name to a more user-friendly display Name.
	 * @param templateName the name of the DataSource template
	 * @return the Display Name
	 */
	public static String convertTemplateNameToDisplayName(String templateName) {
		String displayName = templateName;
		if(templateName!=null) {
			// Standard File .rar
			if(templateName.equalsIgnoreCase(FILE_RARFILE)) {
				displayName = FILE_DISPLAYNAME;
			// Standard WebService .rar
			} else if(templateName.equalsIgnoreCase(WEBSERVICE_RARFILE)) {
				displayName = WEBSERVICE_DISPLAYNAME;
			// Standard Salesforce .rar
			} else if(templateName.equalsIgnoreCase(SALESFORCE_RARFILE)) {
				displayName = SALESFORCE_DISPLAYNAME;
			// Standard ldap .rar
			} else if(templateName.equalsIgnoreCase(LDAP_RARFILE)) {
				displayName = LDAP_DISPLAYNAME;
			// Standard MongoDB .rar
			} else if(templateName.equalsIgnoreCase(MONGODB_RARFILE)) {
				displayName = MONGODB_DISPLAYNAME;
			}
		}
		return displayName;
	}
	
	/*
	 * Convert Template Display Name to the Template Name
	 * @param templateDisplayName the display name of the DataSource template
	 * @return the Template Name
	 */
	public static String convertTemplateDisplayNameToName(String displayName) {
		String templateName = displayName;
		if(displayName!=null) {
			// Standard File .rar
			if(displayName.equalsIgnoreCase(FILE_DISPLAYNAME)) {
				templateName = FILE_RARFILE;
			// Standard WebService .rar
			} else if(displayName.equalsIgnoreCase(WEBSERVICE_DISPLAYNAME)) {
				templateName = WEBSERVICE_RARFILE;
			// Standard Salesforce .rar
			} else if(displayName.equalsIgnoreCase(SALESFORCE_DISPLAYNAME)) {
				templateName = SALESFORCE_RARFILE;
			// Standard LDAP .rar
			} else if(displayName.equalsIgnoreCase(LDAP_DISPLAYNAME)) {
				templateName = LDAP_RARFILE;
			// Standard MongoDB .rar
			} else if(displayName.equalsIgnoreCase(MONGODB_DISPLAYNAME)) {
				templateName = MONGODB_RARFILE;
			}
		}
		return templateName;
	}
	
	/*
	 * Determine the best default translator, given the dataSource template name and list of available translators
	 * @param templateName the name of the DataSource template
	 * @param translators the list of available translators
	 * @return the default translator
	 */
	public static String getDefaultTranslator(String templateName, List<String> translators) {
		String translatorName = null;
		// Handle the known '*.rar' datasources
		if(templateName!=null && !templateName.trim().isEmpty()) {
			// Standard File .rar
			if(templateName.equalsIgnoreCase(FILE_RARFILE)) {
				if(translators.contains(FILE_TRANSLATOR)) {
					translatorName = FILE_TRANSLATOR;
				}
			// Standard WebService .rar
			} else if(templateName.equalsIgnoreCase(WEBSERVICE_RARFILE)) {
				if(translators.contains(WEBSERVICE_TRANSLATOR)) {
					translatorName = WEBSERVICE_TRANSLATOR;
				}
			// Standard Salesforce .rar
			} else if(templateName.equalsIgnoreCase(SALESFORCE_RARFILE)) {
				if(translators.contains(SALESFORCE_TRANSLATOR)) {
					translatorName = SALESFORCE_TRANSLATOR;
				}
			// Standard LDAP .rar
			} else if(templateName.equalsIgnoreCase(LDAP_RARFILE)) {
				if(translators.contains(LDAP_TRANSLATOR)) {
					translatorName = LDAP_TRANSLATOR;
				}
			// Standard MongoDB .rar
			} else if(templateName.equalsIgnoreCase(MONGODB_RARFILE)) {
				if(translators.contains(MONGODB_TRANSLATOR)) {
					translatorName = MONGODB_TRANSLATOR;
				}
				// ViewModel
			} else if(templateName.equalsIgnoreCase(VIEW_MODEL)) {
				translatorName = TRANS_VIEWMODEL;
		    // Exact Match between DataSource template and Translator
			} else if(translators.contains(templateName)) {
				translatorName = templateName;
			// If template name contains the translator name
			} else {
				for(String transName : translators) {
					if(templateName.indexOf(transName)!=-1) {
						translatorName = transName;
					}
				}
			}
		}
		return translatorName;
	}
	
	/*
	 * Get the source type which corresponds to the provided translator.  
	 * This is used for re-deploy to re-populate the properties dialog.
	 * @param translatorName the translator name
	 * @return the type of DataSource
	 */
	public static String getSourceTypeForTranslator(String translatorName) {
		String sourceType = null;
		if(translatorName!=null) {
			if(translatorName.equalsIgnoreCase("mysql")) {
				sourceType="mysql";
			} else if(translatorName.equalsIgnoreCase("postgres")) {
				sourceType="postgres";
			} else if(translatorName.equalsIgnoreCase(WEBSERVICE_TRANSLATOR)) {
				sourceType=WEBSERVICE_RARFILE;
			} else if(translatorName.equalsIgnoreCase(SALESFORCE_TRANSLATOR)) {
				sourceType=SALESFORCE_RARFILE;
			} else if(translatorName.equalsIgnoreCase(FILE_TRANSLATOR)) {
				sourceType=FILE_RARFILE;
			} else if(translatorName.equalsIgnoreCase(LDAP_TRANSLATOR)) {
				sourceType=LDAP_RARFILE;
			} else if(translatorName.equalsIgnoreCase(MONGODB_TRANSLATOR)) {
				sourceType=MONGODB_RARFILE;
			} else if(translatorName.equalsIgnoreCase("teiid")) {
				sourceType=VIEW_MODEL;
			}
		}
		return sourceType;
	}
	
	/*
	 * Add any missing properties to the list of provided PropertyObjs, for the specified template name.  
	 * For example, the ConnectionFactory classname is required, but is missing from the property defns...
	 * @param dataSourceType the name of the DataSource template
	 * @param propertyItems the list of supplied PropertyObjs
	 */
	public static void addMissingProperties(String dataSourceType, List<PropertyItem> propertyItems) {
		// ------------------------------
		// Add any mising properties
		// ------------------------------
		
		// File .rar needs the class-name
		if(dataSourceType.equals(FILE_RARFILE) && !containsProperty(CLASS_NAME,propertyItems)) {
			PropertyItem propItem = new PropertyItem();
			propItem.setDisplayName(CLASS_DISPLAY_NAME);
			propItem.setName(CLASS_NAME);
			propItem.setRequired(true);
			propItem.setModifiable(false);
			propItem.setHidden(true);
			propItem.setValue(CONNECTION_FACTORY_FILE);
			propertyItems.add(propItem);
			// File .rar needs the class-name
		} else if(dataSourceType.equals(WEBSERVICE_RARFILE) && !containsProperty(CLASS_NAME,propertyItems)) {
			PropertyItem propItem = new PropertyItem();
			propItem.setDisplayName(CLASS_DISPLAY_NAME);
			propItem.setName(CLASS_NAME);
			propItem.setRequired(true);
			propItem.setModifiable(false);
			propItem.setHidden(true);
			propItem.setValue(CONNECTION_FACTORY_WEBSERVICE);
			propertyItems.add(propItem);
		// File .rar needs the class-name
		} else if(dataSourceType.equals(SALESFORCE_RARFILE) && !containsProperty(CLASS_NAME,propertyItems)) {
			PropertyItem propItem = new PropertyItem();
			propItem.setDisplayName(CLASS_DISPLAY_NAME);
			propItem.setName(CLASS_NAME);
			propItem.setRequired(true);
			propItem.setModifiable(false);
			propItem.setHidden(true);
			propItem.setValue(CONNECTION_FACTORY_SALESFORCE);
			propertyItems.add(propItem);
		// File .rar needs the class-name
		} else if(dataSourceType.equals(LDAP_RARFILE) && !containsProperty(CLASS_NAME,propertyItems)) {
			PropertyItem propItem = new PropertyItem();
			propItem.setDisplayName(CLASS_DISPLAY_NAME);
			propItem.setName(CLASS_NAME);
			propItem.setRequired(true);
			propItem.setModifiable(false);
			propItem.setHidden(true);
			propItem.setValue(CONNECTION_FACTORY_LDAP);
			propertyItems.add(propItem);
		// File .rar needs the class-name
		} else if(dataSourceType.equals(MONGODB_RARFILE) && !containsProperty(CLASS_NAME,propertyItems)) {
			PropertyItem propItem = new PropertyItem();
			propItem.setDisplayName(CLASS_DISPLAY_NAME);
			propItem.setName(CLASS_NAME);
			propItem.setRequired(true);
			propItem.setModifiable(false);
			propItem.setHidden(true);
			propItem.setValue(CONNECTION_FACTORY_MONGODB);
			propertyItems.add(propItem);
		// File .rar needs the class-name
		} else if(dataSourceType.equals(VIEW_MODEL) && !containsProperty(DDL_NAME,propertyItems)) {
			PropertyItem propItem = new PropertyItem();
			propItem.setDisplayName(DDL_DISPLAY_NAME);
			propItem.setName(DDL_NAME);
			propItem.setRequired(true);
			propItem.setModifiable(true);
			propItem.setValue("{DDL definition}");
			propertyItems.add(propItem);
		}
	}

	/*
	 * Determine if the list of supplied PropertyObj contains a property with 'propName'
	 * @param propName the property name of interest
	 * @param propertyItems the list of supplied PropertyObjs
	 * @return 'true' if the PropertyObj list contains a property of the name 'propName', 'false' if not.
	 */
	private static boolean containsProperty(String propName, List<PropertyItem> propertyItems) {
		boolean isPresent = false;
		for(PropertyItem propItem: propertyItems) {
			if(propItem.getName().equalsIgnoreCase(propName)) {
				isPresent = true;
				break;
			}
		}
		return isPresent;
	}
	
	/*
	 * Set the default values on the list of provided PropertyObjs, for the specified template name.
	 * @param templateName the name of the DataSource template
	 * @param propertyItems the list of supplied PropertyObjs
	 */
	public static void setPropertyDefaults(String templateName, List<PropertyItem> propertyItems) {
		
		// Iterate the PropertyObjs, looking for certain property names.
		for(PropertyItem propItem: propertyItems) {
			String propName = propItem.getName();
			if(PROP_CONN_URL.equalsIgnoreCase(propName)) {
				// May be a more reliable way to determine, possibly using the translator name instead of, or in addition to templateName
				if(templateName.equalsIgnoreCase("mysql")) {
					propItem.setValue("jdbc:mysql://{HOST}:3306/{DATABASE}"); 
				} else if(templateName.equalsIgnoreCase("postgres")) {
					propItem.setValue("jdbc:postgresql://{HOST}:5432/{DATABASE}"); 
				}
			} else if(PROP_USERNAME1.equalsIgnoreCase(propName) || PROP_USERNAME2.equalsIgnoreCase(propName)) {
				propItem.setValue("<USERNAME>");
			} else if(PROP_PARENT_DIR.equalsIgnoreCase(propName)) {
				propItem.setValue("{directoryPath}");
			}
		}
	}
	
	/*
	 * Convert the entire List of PropertyObj from the Table, into a Map which will be submitted to the Admin API
	 * @param propertyObjs the List of PropertyObjs
	 * @return the Map for submittal to the Admin APIAdd any mising properties
	 */
	public static Map<String,String> convertPropsForApiSubmittal(List<PropertyItem> propertyObjs) {
		Map<String,String> resultMap = new HashMap<String,String>();
		
		for(PropertyItem propertyObj: propertyObjs) {
			String propName = propertyObj.getName();
			String value = propertyObj.getValue();
			String defaultValue = propertyObj.getDefaultValue();
			// Property is included for the create if (1) it is required or (2) it is modifiable and value is different than the default
			if(propertyObj.isRequired() || (propertyObj.isModifiable() && !valuesSame(value,defaultValue)) ) {
				resultMap.put(propName, value);
			}
		}
		return resultMap;
	}
	
    private static boolean valuesSame(String value1, String value2) {
        if(isEmpty(value1) && isEmpty(value2)) {
            return true;
        }
        if(isEmpty(value1) && !isEmpty(value2)) {
            return false;
        }
        if(isEmpty(value2) && !isEmpty(value1)) {
            return false;
        }
        if(!value1.equalsIgnoreCase(value2)) {
            return false;
        }
        return true;
    }
    
    public static boolean isEmpty( final String text ) {
        return (text == null || text.length() == 0);
    }
	
}
