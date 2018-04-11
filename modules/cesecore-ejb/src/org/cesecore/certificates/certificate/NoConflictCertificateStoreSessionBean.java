/*************************************************************************
 *                                                                       *
 *  CESeCore: CE Security Core                                           *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.cesecore.certificates.certificate;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;
import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.certificates.ca.CAInfo;
import org.cesecore.certificates.ca.CaSessionLocal;
import org.cesecore.certificates.certificateprofile.CertificateProfileConstants;
import org.cesecore.certificates.crl.RevocationReasons;
import org.cesecore.jndi.JndiConstants;
import org.cesecore.util.CertTools;

/**
 * @version $Id$
 */
@Stateless(mappedName = JndiConstants.APP_JNDI_PREFIX + "CertificateStoreSessionRemote")
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class NoConflictCertificateStoreSessionBean implements NoConflictCertificateStoreSessionRemote, NoConflictCertificateStoreSessionLocal {

    private final static Logger log = Logger.getLogger(NoConflictCertificateStoreSessionBean.class);

    @EJB
    private CaSessionLocal caSession;
    @EJB
    private CertificateStoreSessionLocal certificateStoreSession;

    @Override
    public CertificateDataWrapper getCertificateDataByIssuerAndSerno(final String issuerdn, final BigInteger certserno) {
        // TODO should it be allowed to have a certificate in both tables? (in that case we should probably take the revocation information from the most recent one in NoConflictCertificateData)
        CertificateDataWrapper cdw = certificateStoreSession.getCertificateDataByIssuerAndSerno(issuerdn, certserno);
        if (cdw != null) {
            // Full certificate is available, return it
            return cdw;
        }

        // Throw away CA or missing certificate
        final int caid = issuerdn.hashCode();
        final CAInfo cainfo = caSession.getCAInfoInternal(caid);
        if (cainfo == null || !cainfo.getSubjectDN().equals(issuerdn) || cainfo.isUseCertificateStorage()) {
            if (cainfo == null && log.isDebugEnabled()) {
                log.debug("Tried to look up certificate " + certserno.toString(16) +", but neither certificate nor CA was found. CA Id: " + caid + ". Issuer DN: '" + issuerdn + "'");
            }
            return null; // Certificate is non-existent
        }
        // FIXME this should be stored in the new table (NoConflictCertificateData)
        // FIXME should try to look up in NoConflictCertificateData also, and take the latest result
        //final NoConflictCertificateData certificateData = new NoConflictCertificateData();
        final CertificateData certificateData = new CertificateData();
        // See org.cesecore.certificates.certificate.CertificateStoreSessionBean.updateLimitedCertificateDataStatus
        certificateData.setSerialNumber(certserno.toString());
        // A fingerprint is needed by the publisher session, so we put a dummy fingerprint here
        certificateData.setFingerprint(generateDummyFingerprint(cainfo.getSubjectDN(), certserno));
        certificateData.setIssuerDN(cainfo.getSubjectDN());
        certificateData.setSubjectDN("CN=limited");
        certificateData.setUsername(null);
        certificateData.setCertificateProfileId(CertificateProfileConstants.NO_CERTIFICATE_PROFILE); // TODO Should be configurable per CA (ECA-6743)
        certificateData.setStatus(CertificateConstants.CERT_ACTIVE);
        certificateData.setRevocationReason(RevocationReasons.NOT_REVOKED.getDatabaseValue());
        certificateData.setRevocationDate(-1L);
        certificateData.setUpdateTime(Long.valueOf(System.currentTimeMillis()));
        certificateData.setCaFingerprint(CertTools.getFingerprintAsString(cainfo.getCertificateChain().get(0)));
        certificateData.setEndEntityProfileId(-1);
        return new CertificateDataWrapper(certificateData, null);
    }
    
    @Override
    public boolean setRevokeStatus(final AuthenticationToken admin, final CertificateDataWrapper cdw, final Date revokedDate, final int reason)
            throws CertificateRevokeException, AuthorizationDeniedException {
        final CertificateData certificateData = cdw.getCertificateData();
        final String issuerdn = certificateData.getIssuerDN();
        final int caid = issuerdn.hashCode();
        final CAInfo cainfo = caSession.getCAInfoInternal(caid);
        if (cainfo == null || !cainfo.getSubjectDN().equals(issuerdn) || cainfo.isUseCertificateStorage()) {
            // Not a throw away certificate. Go ahead with standard CertificateData logic.
            if (cainfo == null && log.isDebugEnabled()) {
                log.debug("Certificate " + cdw.getCertificateData().getSubjectDN() +" references a CA that does not exist. CA Id: " + caid + ". Issuer DN: '" + issuerdn + "'");
            }
            return certificateStoreSession.setRevokeStatus(admin, cdw, revokedDate, reason);
        }
        throw new UnsupportedOperationException("Throw away case is not implemented"); // TODO
//        return certificateStoreSession.setRevokeStatus(admin, cdw, revokedDate, reason);  // XXX Works with CertificateData, but remains to be tested with NoConflictCertificateData
    }
    
    private static String generateDummyFingerprint(final String issuerdn, final BigInteger certserno) {
        final byte[] fingerprintBytes = CertTools.generateSHA1Fingerprint((certserno.toString()+';'+issuerdn).getBytes(StandardCharsets.UTF_8));
        return new String(Hex.encode(fingerprintBytes));
    }

}
