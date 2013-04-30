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
package org.ejbca.core.ejb.signer.impl;

import java.io.Serializable;
import java.security.cert.Certificate;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.ejbca.core.ejb.signer.CertificateImportException;
import org.ejbca.core.ejb.signer.InternalKeyBindingBase;
import org.ejbca.core.ejb.signer.InternalKeyBindingProperty;

/**
 * Holder of "external" (e.g. non-CA signing key) OCSP InternalKeyBinding properties.
 * 
 * @version $Id$
 */
public class OcspKeyBinding extends InternalKeyBindingBase {
  
    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(OcspKeyBinding.class);

    public static final String IMPLEMENTATION_ALIAS = "OcspKeyBinding"; // This should not change, even if we rename the class in EJBCA 5.3+..
    public static final String PROPERTY_NON_EXISTING_GOOD = "nonexistingisgood";
    public static final String PROPERTY_INCLUDE_CERT_IN_CHAIN = "includecertchain";
    public static final String PROPERTY_RESPONDER_ID_TYPE = "responderidtype";  // keyhash, name
    public static final String PROPERTY_RESPONDER_ID_TYPE_VKEYHASH = "keyhash"; 
    public static final String PROPERTY_RESPONDER_ID_TYPE_VNAME = "name";
    public static final String PROPERTY_REQUIRE_TRUSTED_SIGNATURE = "requireTrustedSignature";
    public static final String PROPERTY_UNTIL_NEXT_UPDATE = "untilNextUpdate";
    public static final String PROPERTY_MAX_AGE = "maxAge";
    //signaturealgorithm -> base class
    //signaturerequired, false -> PROPERTY_REQUIRE_TRUSTED_SIGNATURE + empty trust list
    //restrictsignaturesbyissuer, false -> PROPERTY_REQUIRE_TRUSTED_SIGNATURE + trust list
    //restrictsignaturesbysigner, false -> PROPERTY_REQUIRE_TRUSTED_SIGNATURE + trust list
    //untilNextUpdate, 0, -> per cert profile still in config file, if set here it would override global but not per cert profile setting
    //maxAge, 30 -> as untilNextUpdate
    
    @SuppressWarnings("serial")
    public OcspKeyBinding() {
        super(new ArrayList<InternalKeyBindingProperty<? extends Serializable>>() {{
            add(new InternalKeyBindingProperty<Boolean>(PROPERTY_NON_EXISTING_GOOD, Boolean.FALSE));
            add(new InternalKeyBindingProperty<Boolean>(PROPERTY_INCLUDE_CERT_IN_CHAIN, Boolean.TRUE));
            add(new InternalKeyBindingProperty<String>(PROPERTY_RESPONDER_ID_TYPE, PROPERTY_RESPONDER_ID_TYPE_VKEYHASH, PROPERTY_RESPONDER_ID_TYPE_VKEYHASH, PROPERTY_RESPONDER_ID_TYPE_VNAME));
            add(new InternalKeyBindingProperty<Boolean>(PROPERTY_REQUIRE_TRUSTED_SIGNATURE, Boolean.FALSE));
            add(new InternalKeyBindingProperty<Integer>(PROPERTY_UNTIL_NEXT_UPDATE, 0));
            add(new InternalKeyBindingProperty<Integer>(PROPERTY_MAX_AGE, 30));
        }});
    }
    
    @Override
    public String getImplementationAlias() {
        return IMPLEMENTATION_ALIAS;
    }
    
    @Override
    public float getLatestVersion() {
        return Long.valueOf(serialVersionUID).floatValue();
    }

    @Override
    protected void upgrade(float latestVersion, float currentVersion) {
        // Nothing to do
    }
    
    @Override
    public void assertCertificateCompatability(Certificate certificate) throws CertificateImportException {
        log.warn("CERTIFICATE VALIDATION HAS NOT BEEN IMPLEMENTED YET!");
    }

    public boolean getNonExistingGood() {
        return (Boolean) getProperty(PROPERTY_NON_EXISTING_GOOD).getValue();
    }
    public void setNonExistingGood(boolean nonExistingGood) {
        setProperty(PROPERTY_NON_EXISTING_GOOD, Boolean.valueOf(nonExistingGood));
    }
}
