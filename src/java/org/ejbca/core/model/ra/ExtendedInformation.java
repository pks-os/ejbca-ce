/*************************************************************************
 *                                                                       *
 *  EJBCA: The OpenSource Certificate Authority                          *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
 
package org.ejbca.core.model.ra;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.ejbca.core.model.InternalResources;
import org.ejbca.core.model.UpgradeableDataHashMap;
import org.ejbca.util.StringTools;


/**
 * The model representation of Exended Information about a user. It's used for non-searchable data about a user, 
 * like a image, in an effort to minimize the need for database alterations
 *
 * @author  Philip Vendil
 * @version $Id: ExtendedInformation.java,v 1.12 2007-06-15 13:24:25 jeklund Exp $
 */
public class ExtendedInformation extends UpgradeableDataHashMap implements java.io.Serializable, Cloneable {
    private static final Logger log = Logger.getLogger(ExtendedInformation.class);
    /** Internal localization of logs and errors */
    private static final InternalResources intres = InternalResources.getInstance();

    /**
     * Determines if a de-serialized file is compatible with this class.
     *
     * Maintainers must change this value if and only if the new version
     * of this class is not compatible with old versions. See Sun docs
     * for <a href=http://java.sun.com/products/jdk/1.1/docs/guide
     * /serialization/spec/version.doc.html> details. </a>
     *
     */
    private static final long serialVersionUID = 3981761824188420319L;
    
    public static final float LATEST_VERSION = 1;    

    public static final int TYPE_BASIC = 0;
    public static final int TYPE_SCEPRA = 1;
    
    public static final String TYPE = "type";

    // protected fields.
    protected static final String SUBJECTDIRATTRIBUTES = "subjectdirattributes";

    protected static final String XKMSREVOCATIONCODEIDENTIFIER = "revocationcodeidentifier";
    protected static final String CUSTOMDATA = "customdata_";
    
    
    // Public constants

    // Wait for fields to use with this class. 
    
    // Public methods.
    /** Creates a new instance of EndEntity Profile */
    public ExtendedInformation() {
    	setType(TYPE_BASIC);
    	data.put(SUBJECTDIRATTRIBUTES, "");
    }

    public String getSubjectDirectoryAttributes(){ 
    	String ret = (String) data.get(SUBJECTDIRATTRIBUTES);
    	if (ret == null) {
    		ret = "";
    	}
    	return ret;
    }
    public void setSubjectDirectoryAttributes(String subjdirattr) {
      if(subjdirattr==null)
        data.put(SUBJECTDIRATTRIBUTES,"");
      else
        data.put(SUBJECTDIRATTRIBUTES,subjdirattr);
    }
    
    /**
     * Returns the revocation code identifier primarily used
     * in the XKMS protocol to let the end user revoke his certificate.
     *          
     *      
     * The method is autoupgradable
     * 
     * @returns The code or null if no revocationcode have been set.
     */
    public String getRevocationCodeIdentifier(){ 
    	String retval = (String) data.get(XKMSREVOCATIONCODEIDENTIFIER);
    	

    	
    	return retval;     	
    }
    
    
    /**
     * 
     * 
     * @param revocationCodeIdentifier the string saved
     */
    public void setRevocationCodeIdentifier(String revocationCodeIdentifier) {
    	String value = revocationCodeIdentifier;    
    	    	
    	data.put(XKMSREVOCATIONCODEIDENTIFIER,value);

    }
    
    /**
     * Special method used to retrieve customly set userdata
     * 
     * @returns The data or null if no such data have been set for the user
     */
    public String getCustomData(String key){ 
    	String retval = (String) data.get(CUSTOMDATA + key);
    	

    	
    	return retval;     	
    }
    
    
    /**
     * 
     * @param customly defined key to store the data with
     * @param the string representation of the data
     */
    public void setCustomData(String key, String value) {        	    	
    	data.put(CUSTOMDATA + key,value);
    }
    
    public Object clone() throws CloneNotSupportedException {
      ExtendedInformation clone = new ExtendedInformation();
      HashMap clonedata = (HashMap) clone.saveData();

      Iterator i = (data.keySet()).iterator();
      while(i.hasNext()){
        Object key = i.next();
        clonedata.put(key, data.get(key));
      }

      clone.loadData(clonedata);
      return clone;
    }

    /** Function required by XMLEncoder to do a proper serialization. */
    public void setData( Object hmData ) { loadData(hmData); }
    /** Function required by XMLEncoder to do a proper serialization. */
    public Object getData() {return saveData();}
    
    /** Implemtation of UpgradableDataHashMap function getLatestVersion */
    public float getLatestVersion(){
       return LATEST_VERSION;
    }

    /** Implemtation of UpgradableDataHashMap function upgrade. */

    public void upgrade(){
    	if(Float.compare(LATEST_VERSION, getVersion()) != 0) {
    		// New version of the class, upgrade
			String msg = intres.getLocalizedMessage("ra.extendedinfoupgrade", new Float(getVersion()));
            log.info(msg);
    		
            if(data.get(SUBJECTDIRATTRIBUTES) == null){
                data.put(SUBJECTDIRATTRIBUTES, "");   
            }            

    		data.put(VERSION, new Float(LATEST_VERSION));
    	}
    }
    
    /**
     * Method that returns the classpath to the this or inheriting classes.
     * @return String containing the classpath.
     */
    public int getType(){
    	return ((Integer) data.get(TYPE)).intValue();
    }
    
    /**
     * Method used to specify which kind of object that should be created during
     * deserialization process.
     * 
     * Inheriting class should call 'setClassPath(this) in it's constructor.
     * 
     * @param object
     */
    protected void setType(int type){
       data.put(TYPE,new Integer(type));	
    }

}
