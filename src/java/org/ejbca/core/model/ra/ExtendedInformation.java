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

import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

import org.apache.commons.lang.time.FastDateFormat;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import org.ejbca.core.model.InternalResources;
import org.ejbca.core.model.UpgradeableDataHashMap;


/**
 * The model representation of Exended Information about a user. It's used for non-searchable data about a user, 
 * like a image, in an effort to minimize the need for database alterations
 *
 * @author  Philip Vendil
 * @version $Id$
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
    private static final long serialVersionUID = 3981761824188420320L;
    
    private static final float LATEST_VERSION = 3;

    /** Different types of implementations of extended information, can be used to have different implementing classes of extended information */
    static final int TYPE_BASIC = 0;
    
    static final String TYPE = "type";

    // protected fields.
    /** Used to store subject directory attributes, which are put in an extension in the certificate.
     * SubjectDirectoryAttributes are standard attributes, see rfc3280 */
    public static final String SUBJECTDIRATTRIBUTES = "subjectdirattributes";
    /**  the revocation code identifier primarily used in the XKMS protocol to let the end user revoke his certificate
     * see the XKMS specification */
    public  static final String XKMSREVOCATIONCODEIDENTIFIER = "revocationcodeidentifier";
    /** Custom data can be used by various custom work-flows and other non-standard things to store information needed  */
    public static final String CUSTOMDATA = "customdata_";
    
    /** Identifier for Custom data holding a base64 encoded PKCS10 request
     * extInfo.setCustomData("PKCS10", new String(Base64.encode(pkcs10.getEncoded())));
     */
    public static final String CUSTOM_PKCS10 = "PKCS10";
    /** Identifier for Custom data holding a end time when the users certificate should be valid
     * extInfo.setCustomData(EndEntityProfile.STARTTIME, "");
     */
    public static final String CUSTOM_STARTTIME = "STARTTIME";	//EndEntityProfile.STARTTIME;
    /** Identifier for Custom data holding a end time when the users certificate should be valid
     * extInfo.setCustomData(EndEntityProfile.ENDTIME, "");
     */
    public static final String CUSTOM_ENDTIME = "ENDTIME";	//EndEntityProfile.ENDTIME;
    /** The (optional) counter is the counter how many request have been received, will decrease for every request until 0. */
    public static final String CUSTOM_REQUESTCOUNTER = "REQUESTCOUNTER";
    /** The (optional) revocation status a certificate issued to this user will have, immediately upon issuance. */
    public static final String CUSTOM_REVOCATIONREASON = "REVOCATIONREASON";
    
    /** The counter is a counter for how many failed login attempts that can be performed before the userstatus is changed to GENERATED */
    public static final String REMAININGLOGINATTEMPTS 			= "remainingloginattempts";
    
    /** The maximum number of login attempts before the user is locked by setting its status to GENERATED */
    public static final String MAXFAILEDLOGINATTEMPTS 			= "maxfailedloginattempts";
    
    /** Default value for how many failed login attempts are allow = -1 (unlimited) */
	public static final int DEFAULT_MAXLOGINATTEMPTS 			= -1;
	
	/** Default value for how many of the allowed failed login attempts that are remaining = -1 (unlimited) */
	private static final int DEFAULT_REMAININGLOGINATTEMPTS 		= -1;

	/** Map key for certificate serial number */
	public static final Object CERTIFICATESERIALNUMBER = "CERTIFICATESERIALNUMBER";
    
    // Public constants

    // Wait for fields to use with this class. 
    
    // Public methods.
    /** Creates a new instance of EndEntity Profile */
    public ExtendedInformation() {
    	setType(TYPE_BASIC);
    	data.put(SUBJECTDIRATTRIBUTES, "");
    	setMaxLoginAttempts(DEFAULT_MAXLOGINATTEMPTS);
    	setRemainingLoginAttempts(DEFAULT_REMAININGLOGINATTEMPTS);
    }

    public String getSubjectDirectoryAttributes(){ 
    	String ret = (String) data.get(SUBJECTDIRATTRIBUTES);
    	if (ret == null) {
    		ret = "";
    	}
    	return ret;
    }
    public void setSubjectDirectoryAttributes(String subjdirattr) {
      if(subjdirattr==null) {
        data.put(SUBJECTDIRATTRIBUTES,"");
      } else {
        data.put(SUBJECTDIRATTRIBUTES,subjdirattr);
      }
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
     * @return The number of remaining allowed failed login attempts or -1 for unlimited
     */
    public int getRemainingLoginAttempts() {
    	return ((Integer) data.get(REMAININGLOGINATTEMPTS)).intValue();
    }
    
    /**
     * Set the number of remaining login attempts. -1 means unlimited.
     * @param remainingLoginAttempts The number to set
     */
    public void setRemainingLoginAttempts(int remainingLoginAttempts) {
    	data.put(REMAININGLOGINATTEMPTS, Integer.valueOf(remainingLoginAttempts));
    }
    
    /**
     * @return The maximum number of allowed failed login attempts or -1 for unlimited
     */
    public int getMaxLoginAttempts() {
    	return ((Integer) data.get(MAXFAILEDLOGINATTEMPTS)).intValue();
    }
    
    /**
     * Set the number of maximum allowed failed login attempts. -1 means unlimited.
     * @param remainingLoginAttempts The number to set
     */
    public void setMaxLoginAttempts(int maxLoginAttempts) {
    	data.put(MAXFAILEDLOGINATTEMPTS, Integer.valueOf(maxLoginAttempts));
    }

    /**
     * @return the serial number to be used for the certificate or null if no number defined.
     */
    public BigInteger certificateSerialNumber() {
        final String s = (String)this.data.get(CERTIFICATESERIALNUMBER);
        if ( s==null ) {
            return null;
        }
        return new BigInteger(Base64.decode(s));
    }

    /**
     * @param sn the serial number to be used for the certificate
     */
    public void setCertificateSerialNumber( BigInteger sn ) {
        if ( sn==null ) {
            this.data.remove(CERTIFICATESERIALNUMBER);
            return;
        }
        final String s = new String(Base64.encode(sn.toByteArray()));
        this.data.put(CERTIFICATESERIALNUMBER, s);
    }
    
    /** Gets generic string data from the ExtendedInformation map.
	 */
    public String getMapData(String key) {
    	String ret = null;
    	Object o = data.get(key);
    	if (o instanceof String) {
    		ret = (String)o;
		}
    	return ret;
    }
    
    /** Sets generic string data in the ExtendedInformation map.
	 */
    public void setMapData(String key, String value) {
    	data.put(key,value);
    }
    
    /**
     * Special method used to retrieve custom set userdata
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
    
    /** Implementation of UpgradableDataHashMap function getLatestVersion */
    public float getLatestVersion(){
       return LATEST_VERSION;
    }

    /** Implementation of UpgradableDataHashMap function upgrade. */
    public void upgrade(){
    	if(Float.compare(LATEST_VERSION, getVersion()) != 0) {
    		// New version of the class, upgrade
			String msg = intres.getLocalizedMessage("ra.extendedinfoupgrade", new Float(getVersion()));
            log.info(msg);
    		
            if(data.get(SUBJECTDIRATTRIBUTES) == null){
                data.put(SUBJECTDIRATTRIBUTES, "");   
            }
            if(data.get(MAXFAILEDLOGINATTEMPTS) == null) {
            	setMaxLoginAttempts(DEFAULT_MAXLOGINATTEMPTS);
            }
            if(data.get(REMAININGLOGINATTEMPTS) == null) {
            	setRemainingLoginAttempts(DEFAULT_REMAININGLOGINATTEMPTS);
            }
            // In EJBCA 4.0.0 we changed the date format to ISO 8601
        	if (getVersion() < 3) {
        		final DateFormat oldDateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.US);
        		final FastDateFormat newDateFormat = FastDateFormat.getInstance("yyyy-MM-dd HH:mm");
        		try {
        			final String oldCustomStartTime = getCustomData(ExtendedInformation.CUSTOM_STARTTIME);
        			if ( oldCustomStartTime != null && oldCustomStartTime.length()>0 && !oldCustomStartTime.matches("^\\d+:\\d?\\d:\\d?\\d$") ) {
        				// We use an absolute time format, so we need to upgrade
            			final String newCustomStartTime = newDateFormat.format(oldDateFormat.parse(oldCustomStartTime));
    					setCustomData(ExtendedInformation.CUSTOM_STARTTIME, newCustomStartTime);
    					if (log.isDebugEnabled()) {
    						log.debug("Upgraded " + ExtendedInformation.CUSTOM_STARTTIME + " from \"" + oldCustomStartTime + "\" to \"" + newCustomStartTime + "\" in ExtendedInformation.");
    					}
        			}
				} catch (ParseException e) {
					log.error("Unable to upgrade " + ExtendedInformation.CUSTOM_STARTTIME + " in extended user information.", e);
				}
        		try {
        			final String oldCustomEndTime = getCustomData(ExtendedInformation.CUSTOM_ENDTIME);
        			if ( oldCustomEndTime != null && oldCustomEndTime.length()>0 && !oldCustomEndTime.matches("^\\d+:\\d?\\d:\\d?\\d$") ) {
        				// We use an absolute time format, so we need to upgrade
            			final String newCustomEndTime = newDateFormat.format(oldDateFormat.parse(oldCustomEndTime));
    					setCustomData(ExtendedInformation.CUSTOM_ENDTIME, newCustomEndTime);
    					if (log.isDebugEnabled()) {
    						log.debug("Upgraded " + ExtendedInformation.CUSTOM_ENDTIME + " from \"" + oldCustomEndTime + "\" to \"" + newCustomEndTime + "\" in ExtendedInformation.");
    					}
        			}
				} catch (ParseException e) {
					log.error("Unable to upgrade " + ExtendedInformation.CUSTOM_ENDTIME + " in extended user information.", e);
				}
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
       data.put(TYPE,Integer.valueOf(type));	
    }

}
