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

import java.io.Serializable;
import java.util.List;

import se.anatom.ejbca.util.UpgradeableDataHashMap;

/**
 * HardTokenProfile is a basic class that should be inherited by all types
 * of hardtokenprofiles in the system.
 * 
 * It used to customize the information generated on hard tokens when they are 
 * processed. This information could be PIN-type number of certificates, 
 * certificate profiles and so on. 
 *
 * @version $Id: HardTokenProfile.java,v 1.5 2005-05-06 10:34:59 herrvendil Exp $
 */
public abstract class HardTokenProfile extends UpgradeableDataHashMap implements Serializable, Cloneable {
    // Default Values
    

    public static final String TRUE  = "true";
    public static final String FALSE = "false";

    public static final int PINTYPE_ASCII_NUMERIC           = 1;   
    public static final int PINTYPE_ISO9564_1               = 4;    

    // Protected Constants.
	public static final String TYPE                           = "type";
	
    protected static final String NUMBEROFCOPIES                 = "numberofcopies";
    protected static final String EREASABLETOKEN                 = "ereasabletoken";
	protected static final String HARDTOKENPREFIX                = "hardtokenprefix";
	protected static final String PINTYPE                        = "pintype";
	protected static final String MINIMUMPINLENGTH               = "minimumpinlength";
	protected static final String GENERATEIDENTICALPINFORCOPIES  = "generateidenticalpinforcopies";	
    // Public Methods

    /**
     * Creates a new instance of CertificateProfile
     */
    public HardTokenProfile() {
      setNumberOfCopies(1);	 
      setEreasableToken(true);     
      setHardTokenSNPrefix("000000");    
      setGenerateIdenticalPINForCopies(true);  
    }

    // Public Methods mostly used by PrimeCard
    /**
     * Number of Copies indicates how many card for the samt user that should be generated.
     * Generally this means, the PIN and PUK codes and encryption keys are identical. This
     * doesn't mean that the toekns are exact copies. 
     */
    public int getNumberOfCopies(){return ((Integer)data.get(NUMBEROFCOPIES)).intValue();}

	/**
	 * Indicates if the same pin code should be used for all copies of the token or if a
	 * new one should be generated. 
	 */

	public boolean getGenerateIdenticalPINForCopies(){return ((Boolean) data.get(GENERATEIDENTICALPINFORCOPIES)).booleanValue();}

    /**
     * Indicaties if the generaded token should be ereasable or not,
     * 
     * @return true if the card should be ereasable
     */
    public boolean getEreasableToken(){ return ((Boolean)data.get(EREASABLETOKEN)).booleanValue(); }
    
    /**
     * Returns the hardtoken serialnumber prefix that is intended to identify the organization
     * issuing the cards.
     * 
     * @return the serial number prefix.
     */
    
    public String getHardTokenSNPrefix(){ return (String) data.get(HARDTOKENPREFIX); }
    


	/**
	 * Given a token identification string the method determines if the token
	 * supports the structure of this profile.
	 * 
	 * @return true if it's possible to create a token accoringly to the profile.
	 */
    public abstract boolean isTokenSupported(String tokenidentificationstring);
    // Public Methods mostly used by EJBCA  

	public void setNumberOfCopies(int numberofcopies) { data.put(NUMBEROFCOPIES,new Integer(numberofcopies));}

	public void setEreasableToken(boolean ereasabletoken) {data.put(EREASABLETOKEN, Boolean.valueOf(ereasabletoken));}
	
	public void setHardTokenSNPrefix(String hardtokensnprefix){ data.put(HARDTOKENPREFIX,hardtokensnprefix); }
	
	public void setGenerateIdenticalPINForCopies(boolean generate){ data.put(GENERATEIDENTICALPINFORCOPIES, Boolean.valueOf(generate));}
	
	/**
	 * Retrieves what type of pin code that should be generated for
	 * tokens with this profile.
	 * 
	 * @param certusage the should be one of the CERTUSAGE_ constants.
	 * @return a pintype with a value of one of the PINTYPE_ constants 
	 */
	public int getPINType(int certusage){
	  return ((Integer) ((List) data.get(PINTYPE)).get(certusage)).intValue();	
	}
	
	public  void setPINType(int certusage, int pintype){
		((List) data.get(PINTYPE)).set(certusage, new Integer(pintype));		
	}
	
	/**
	 * Retrieves the minimum pin length that should be generated for
	 * tokens with this profile.
	 * 
	 * @param certusage the should be one of the CERTUSAGE_ constants.
	 * @return a length of chars between 0 - 8. 
	 */
	public int getMinimumPINLength(int certusage){
	  return ((Integer) ((List) data.get(MINIMUMPINLENGTH)).get(certusage)).intValue();	
	}
	
	public  void setMinimumPINLength(int certusage, int length){
		((List) data.get(MINIMUMPINLENGTH)).set(certusage, new Integer(length));		
	}

    public abstract Object clone() throws CloneNotSupportedException;

    
    public abstract float getLatestVersion();

    
    public void upgrade(){
    	// Performing upgrade rutines
    }
    
	// Protected methods
	protected boolean isTokenSupported(String[] supportedtokens, String tokenidentificationstring){
	  boolean returnval = false;
	  for(int i=0; i<supportedtokens.length; i++){
	  	if(supportedtokens[i].equals(tokenidentificationstring))
	  	  returnval=true;
	  }
	  
	  return returnval;	
	}
	


}
