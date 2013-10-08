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
package org.cesecore.keybind.impl;

import java.io.Serializable;
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.cesecore.config.ExtendedKeyUsageConfiguration;
import org.cesecore.keybind.CertificateImportException;
import org.cesecore.keybind.InternalKeyBindingBase;
import org.cesecore.keybind.InternalKeyBindingProperty;
import org.cesecore.util.CertTools;

/**
 * Holder of "external" (e.g. non-CA signing key) OCSP InternalKeyBinding properties.
 * 
 * @version $Id$
 */
public class OcspKeyBinding extends InternalKeyBindingBase {
  
    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(OcspKeyBinding.class);
    
    public enum ResponderIdType {
        KEYHASH, NAME;
    }

    public static final String IMPLEMENTATION_ALIAS = "OcspKeyBinding"; // This should not change, even if we rename the class in EJBCA 5.3+..
    public static final String PROPERTY_NON_EXISTING_GOOD = "nonexistingisgood";
    public static final String PROPERTY_INCLUDE_CERT_CHAIN = "includecertchain";
    public static final String PROPERTY_RESPONDER_ID_TYPE = "responderidtype";  // keyhash, name
    public static final String PROPERTY_REQUIRE_TRUSTED_SIGNATURE = "requireTrustedSignature";
    public static final String PROPERTY_UNTIL_NEXT_UPDATE = "untilNextUpdate";
    public static final String PROPERTY_MAX_AGE = "maxAge";
    
    @SuppressWarnings("serial")
    public OcspKeyBinding() {
        super(new ArrayList<InternalKeyBindingProperty<? extends Serializable>>() {{
            add(new InternalKeyBindingProperty<Boolean>(PROPERTY_NON_EXISTING_GOOD, Boolean.FALSE));
            add(new InternalKeyBindingProperty<Boolean>(PROPERTY_INCLUDE_CERT_CHAIN, Boolean.TRUE));
            add(new InternalKeyBindingProperty<String>(PROPERTY_RESPONDER_ID_TYPE, ResponderIdType.KEYHASH.name(), ResponderIdType.KEYHASH.name(), ResponderIdType.NAME.name()));
            add(new InternalKeyBindingProperty<Boolean>(PROPERTY_REQUIRE_TRUSTED_SIGNATURE, Boolean.FALSE));
            add(new InternalKeyBindingProperty<Integer>(PROPERTY_UNTIL_NEXT_UPDATE, 0));
            add(new InternalKeyBindingProperty<Integer>(PROPERTY_MAX_AGE, 0));
        }});
    }
    
    @Override
    public String getImplementationAlias() {
        return IMPLEMENTATION_ALIAS;
    }
    
    @Override
    public float getLatestVersion() {
        return serialVersionUID;
    }

    @Override
    protected void upgrade(float latestVersion, float currentVersion) {
        // Nothing to do
    }
    
    @Override
    public void assertCertificateCompatability(Certificate certificate) throws CertificateImportException {
        if (!isOcspSigningCertificate(certificate)) {
            throw new CertificateImportException();
        }
    }

    public boolean getNonExistingGood() {
        return (Boolean) getProperty(PROPERTY_NON_EXISTING_GOOD).getValue();
    }
    public void setNonExistingGood(boolean nonExistingGood) {
        setProperty(PROPERTY_NON_EXISTING_GOOD, Boolean.valueOf(nonExistingGood));
    }
    public boolean getIncludeCertChain() {
        return (Boolean) getProperty(PROPERTY_INCLUDE_CERT_CHAIN).getValue();
    }
    public void setIncludeCertChain(boolean includeCertChain) {
        setProperty(PROPERTY_INCLUDE_CERT_CHAIN, Boolean.valueOf(includeCertChain));
    }
    public ResponderIdType getResponderIdType() {
        return ResponderIdType.valueOf((String) getProperty(PROPERTY_RESPONDER_ID_TYPE).getValue());
    }
    public void setResponderIdType(ResponderIdType responderIdType) {
        setProperty(PROPERTY_RESPONDER_ID_TYPE, responderIdType.name());
    }
    public boolean getRequireTrustedSignature() {
        return (Boolean) getProperty(PROPERTY_REQUIRE_TRUSTED_SIGNATURE).getValue();
    }
    public void setRequireTrustedSignature(boolean requireTrustedSignature) {
        setProperty(PROPERTY_REQUIRE_TRUSTED_SIGNATURE, Boolean.valueOf(requireTrustedSignature));
    }
    /** @return the value in seconds (granularity defined in RFC 5019) */
    public int getUntilNextUpdate() {
        return (Integer) getProperty(PROPERTY_UNTIL_NEXT_UPDATE).getValue();
    }
    /** Set the value in seconds (granularity defined in RFC 5019) */
    public void setUntilNextUpdate(int untilNextUpdate) {
        setProperty(PROPERTY_UNTIL_NEXT_UPDATE, Integer.valueOf(untilNextUpdate));
    }
    /** @return the value in seconds (granularity defined in RFC 5019) */
    public int getMaxAge() {
        return (Integer) getProperty(PROPERTY_MAX_AGE).getValue();
    }
    /** Set the value in seconds (granularity defined in RFC 5019) */
    public void setMaxAge(int maxAge) {
        setProperty(PROPERTY_MAX_AGE, Integer.valueOf(maxAge));
    }

    public static boolean isOcspSigningCertificate(Certificate certificate) {
        if (certificate == null) {
            log.debug("No certificate provided.");
            return false;
        }
        if (!(certificate instanceof X509Certificate)) {
            log.debug("Only X509 supported.");
            return false;
        }
        try {
            final X509Certificate x509Certificate = (X509Certificate) certificate;
            if (log.isDebugEnabled()) {
                log.debug("SubjectDN: " + CertTools.getSubjectDN(x509Certificate) + " IssuerDN: " + CertTools.getIssuerDN(x509Certificate));
                final boolean[] ku = x509Certificate.getKeyUsage();
                log.debug("Key usages: " + Arrays.toString(ku));
                if (ku != null) {
                    log.debug("Key usage (digitalSignature): " + x509Certificate.getKeyUsage()[0]);
                    log.debug("Key usage (keyEncipherment): " + x509Certificate.getKeyUsage()[2]);
                }
            }
            if (x509Certificate.getExtendedKeyUsage() == null) {
                log.debug("No EKU to verify.");
                return false;
            }
            for (String extendedKeyUsage : x509Certificate.getExtendedKeyUsage()) {
                log.debug("EKU: " + extendedKeyUsage + " (" +
                        ExtendedKeyUsageConfiguration.getExtendedKeyUsageOidsAndNames().get(extendedKeyUsage) + ")");
            }
            if (!x509Certificate.getExtendedKeyUsage().contains(KeyPurposeId.id_kp_OCSPSigning.getId())) {
                log.debug("Extended Key Usage 1.3.6.1.5.5.7.3.9 (EKU_PKIX_OCSPSIGNING) is required.");
                return false;
            }
            if (!x509Certificate.getKeyUsage()[0]) {
                log.debug("Key Usage digitalSignature is required.");
                return false;
            }
        } catch (CertificateParsingException e) {
            log.debug(e.getMessage());
            return false;
        }
        return true;
    }
}
