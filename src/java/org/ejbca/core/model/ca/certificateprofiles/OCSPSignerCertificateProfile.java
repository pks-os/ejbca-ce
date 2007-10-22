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
 
package org.ejbca.core.model.ca.certificateprofiles;

import java.util.ArrayList;

/**
 * OCSPSignerCertificateProfile is a class defining the fixed characteristics of an enduser certificate type
 *
 * @version $Id: OCSPSignerCertificateProfile.java,v 1.4 2007-10-22 08:43:56 anatom Exp $
 */
public class OCSPSignerCertificateProfile extends CertificateProfile{

    // Public Constants

    public static final String CERTIFICATEPROFILENAME =  "OCSPSIGNER";

    // Public Methods
    /** Creates a certificate with the characteristics of an end user. 
     * General options are set in the superclass's default contructor that is called automatically.
     * You can override the general options by defining them again with different parameters here.
     */
    public OCSPSignerCertificateProfile() {

      setType(TYPE_ENDENTITY);

      // Default key usage for an OCSP signer is digitalSignature
      // Create an array for KeyUsage acoording to X509Certificate.getKeyUsage()
      setUseKeyUsage(true);
      setKeyUsage(new boolean[9]);
      setKeyUsage(DIGITALSIGNATURE,true);      
      setKeyUsageCritical(true);

      setUseExtendedKeyUsage(true);
      ArrayList eku = new ArrayList();      
	  eku.add(new Integer(OCSPSIGNING));
      setExtendedKeyUsage(eku);
      setExtendedKeyUsageCritical(false);

      setUseOcspNoCheck(true);

    }

    // Public Methods.
    public void upgrade(){
    	if(Float.compare(LATEST_VERSION, getVersion()) != 0) {
    		// New version of the class, upgrade
    		
    		super.upgrade();         
    	}
    }


    // Private fields.
}
