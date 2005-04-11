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
 
package se.anatom.ejbca.hardtoken.hardtokenprofiles;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import se.anatom.ejbca.SecConst;


/**
 * EnhancedEIDProfile with three certificates and key recovery functionallity
 * 
 * @version $Id: EnhancedEIDProfile.java,v 1.5 2005-04-11 05:44:42 herrvendil Exp $
 */
public class EnhancedEIDProfile extends EIDProfile {
						
	// Public Constants
	
	public static final int TYPE_ENHANCEDEID = 2;
	
	public static final float LATEST_VERSION = 1;

    public static final int CERTUSAGE_SIGN    = 0;
	public static final int CERTUSAGE_AUTH    = 1;
	public static final int CERTUSAGE_ENC     = 2;
	
	public static final int PINTYPE_AUTH_SAME_AS_SIGN = 100;
	public static final int PINTYPE_ENC_SAME_AS_AUTH  = 101;
	
	// Protected Constants
	protected static final int NUMBEROFCERTIFICATES = 3;
	
	
	// Private Constants
	public static final int[] AVAILABLEMINIMUMKEYLENGTHS = {1024, 2048};
	
	// Protected Fields
	private String[] pinstore = new String[NUMBEROFCERTIFICATES];
	private String[] pukstore = new String[NUMBEROFCERTIFICATES];
	
	private String[][] SUPPORTEDTOKENS = {{"TODO"}};
	
	
	
    // Default Values
    public EnhancedEIDProfile() {
      super();
               
	  data.put(TYPE, new Integer(TYPE_ENHANCEDEID));
      
      ArrayList certprofileids = new ArrayList(NUMBEROFCERTIFICATES);
	  certprofileids.add(new Integer(SecConst.CERTPROFILE_FIXED_HARDTOKENSIGN)); 
	  certprofileids.add(new Integer(SecConst.CERTPROFILE_FIXED_HARDTOKENAUTH)); 
	  certprofileids.add(new Integer(SecConst.CERTPROFILE_FIXED_HARDTOKENENC)); 
	  data.put(CERTIFICATEPROFILEID, certprofileids);
	  	  
	  ArrayList caids = new ArrayList(NUMBEROFCERTIFICATES);
	  caids.add(new Integer(CAID_USEUSERDEFINED)); 
	  caids.add(new Integer(CAID_USEUSERDEFINED)); 
	  caids.add(new Integer(CAID_USEUSERDEFINED)); 
	  data.put(CAID, caids);
	  
	  ArrayList pintypes = new ArrayList(NUMBEROFCERTIFICATES);
	  pintypes.add(new Integer(PINTYPE_4DIGITS));
	  pintypes.add(new Integer(PINTYPE_4DIGITS));
	  pintypes.add(new Integer(PINTYPE_ENC_SAME_AS_AUTH));
	  data.put(PINTYPE, pintypes);
	  
	  ArrayList iskeyrecoverable = new ArrayList(NUMBEROFCERTIFICATES);
	  iskeyrecoverable.add(Boolean.FALSE);
	  iskeyrecoverable.add(Boolean.FALSE);
	  iskeyrecoverable.add(Boolean.TRUE);
	  data.put(ISKEYRECOVERABLE, iskeyrecoverable);

	  ArrayList minimumkeylength = new ArrayList(NUMBEROFCERTIFICATES);
	  minimumkeylength.add(new Integer(2048));
	  minimumkeylength.add(new Integer(2048));
	  minimumkeylength.add(new Integer(2048));
	  data.put(MINIMUMKEYLENGTH, minimumkeylength);	  

	  ArrayList keytypes = new ArrayList(NUMBEROFCERTIFICATES);
	  keytypes.add(KEYTYPE_RSA);
	  keytypes.add(KEYTYPE_RSA);
	  keytypes.add(KEYTYPE_RSA);
	  data.put(KEYTYPES, keytypes);
	  	 
    }


	
	public int[] getAvailableMinimumKeyLengths(){
		return AVAILABLEMINIMUMKEYLENGTHS;
	}
	  				        

	/** 
	 * @see se.anatom.ejbca.hardtoken.hardtokenprofiles.HardTokenProfile#isTokenSupported(java.lang.String)
	 */
	public boolean isTokenSupported(String tokenidentificationstring) {		
		return this.isTokenSupported(SUPPORTEDTOKENS, tokenidentificationstring);
	}

	/* 
	 * @see se.anatom.ejbca.hardtoken.hardtokenprofiles.HardTokenProfile#getPIN(int, boolean)
	 */
	public String getPIN(int certusage, boolean regenerate){
		if(certusage == CERTUSAGE_AUTH && getPINType(certusage) == PINTYPE_AUTH_SAME_AS_SIGN)
		  return getPIN(pinstore,CERTUSAGE_SIGN, getPINType(CERTUSAGE_SIGN), false);
		if(certusage == CERTUSAGE_ENC && getPINType(certusage) == PINTYPE_ENC_SAME_AS_AUTH)
		  return getPIN(pinstore,CERTUSAGE_AUTH, getPINType(CERTUSAGE_AUTH), false);		  
		return getPIN(pinstore,certusage, getPINType(certusage),regenerate);
	}

	/* 
	 * @see se.anatom.ejbca.hardtoken.hardtokenprofiles.HardTokenProfile#getPUK(int, boolean)
	 */
	public String getPUK(int certusage, boolean regenerate) {		
		if(certusage == CERTUSAGE_AUTH && getPINType(certusage) == PINTYPE_AUTH_SAME_AS_SIGN)
		  return getPUK(pinstore,CERTUSAGE_SIGN, getPINType(CERTUSAGE_SIGN), false);
		if(certusage == CERTUSAGE_ENC && getPINType(certusage) == PINTYPE_ENC_SAME_AS_AUTH)
		  return getPUK(pinstore,CERTUSAGE_AUTH, getPINType(CERTUSAGE_AUTH), false);
		return getPUK(pukstore,certusage, getPINType(certusage),regenerate);
	}


	/* 
	 * @see se.anatom.ejbca.hardtoken.hardtokenprofiles.HardTokenProfile#clone()
	 */
	public Object clone() throws CloneNotSupportedException {
	    EnhancedEIDProfile clone = new EnhancedEIDProfile();
	    HashMap clonedata = (HashMap) clone.saveData();
	    Iterator i = (data.keySet()).iterator();
	    while(i.hasNext()){
		  Object key = i.next();
		  clonedata.put(key, data.get(key));
	    }

	    clone.loadData(clonedata);

	    return clone;
    }

	/* 
	 * @see se.anatom.ejbca.hardtoken.hardtokenprofiles.HardTokenProfile#getLatestVersion()
	 */
	public float getLatestVersion() {
	  return LATEST_VERSION;
	}

	public void upgrade(){
	  if(LATEST_VERSION != getVersion()){
		  // New version of the class, upgrade
	    super.upgrade();
	    data.put(VERSION, new Float(LATEST_VERSION));
	  }   
	}    
}
