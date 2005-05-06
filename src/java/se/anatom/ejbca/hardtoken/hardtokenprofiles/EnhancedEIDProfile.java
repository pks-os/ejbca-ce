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
 * @version $Id: EnhancedEIDProfile.java,v 1.8 2005-05-06 10:34:59 herrvendil Exp $
 */
public class EnhancedEIDProfile extends EIDProfile {
						
	// Public Constants
	
	public static final int TYPE_ENHANCEDEID = 2;
	
	public static final float LATEST_VERSION = 2;

    public static final int CERTUSAGE_SIGN    = 0;
	public static final int CERTUSAGE_AUTH    = 1;
	public static final int CERTUSAGE_ENC     = 2;
	
	public static final int PINTYPE_AUTH_SAME_AS_SIGN = SwedishEIDProfile.PINTYPE_AUTHENC_SAME_AS_SIGN;
	public static final int PINTYPE_ENC_SAME_AS_AUTH  = 101;
	
	// Protected Constants
	protected static final int NUMBEROFCERTIFICATES = 3;
	
	
	// Private Constants
	public static final int[] AVAILABLEMINIMUMKEYLENGTHS = {1024, 2048};
	
	// Protected Fields
	
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
	  pintypes.add(new Integer(PINTYPE_ASCII_NUMERIC));
	  pintypes.add(new Integer(PINTYPE_ASCII_NUMERIC));
	  pintypes.add(new Integer(PINTYPE_ENC_SAME_AS_AUTH));
	  data.put(PINTYPE, pintypes);
	  
	  ArrayList minpinlength = new ArrayList(NUMBEROFCERTIFICATES);
	  minpinlength.add(new Integer(4));
	  minpinlength.add(new Integer(4));
	  minpinlength.add(new Integer(0));
	  data.put(MINIMUMPINLENGTH, minpinlength);
	  
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
	    
	    if(data.get(MINIMUMPINLENGTH) == null){
	  	  ArrayList minpinlength = new ArrayList(NUMBEROFCERTIFICATES);
		  minpinlength.add(new Integer(4));
		  minpinlength.add(new Integer(4));
		  minpinlength.add(new Integer(0));
		  data.put(MINIMUMPINLENGTH, minpinlength);
	    }
	    
	    data.put(VERSION, new Float(LATEST_VERSION));
	  }   
	}    
}
