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
import java.util.Collection;
import java.util.Date;
import java.util.UUID;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;
import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.certificates.ca.CAInfo;
import org.cesecore.certificates.ca.CaSessionLocal;
import org.cesecore.certificates.certificateprofile.CertificateProfileConstants;
import org.cesecore.certificates.crl.RevocationReasons;
import org.cesecore.config.CesecoreConfiguration;
import org.cesecore.jndi.JndiConstants;
import org.cesecore.util.CertTools;

/**
 * @version $Id$
 */
@Stateless(mappedName = JndiConstants.APP_JNDI_PREFIX + "CertificateStoreSessionRemote")
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class NoConflictCertificateStoreSessionBean implements NoConflictCertificateStoreSessionRemote, NoConflictCertificateStoreSessionLocal {

    private final static Logger log = Logger.getLogger(NoConflictCertificateStoreSessionBean.class);

    @PersistenceContext(unitName = CesecoreConfiguration.PERSISTENCE_UNIT)
    private EntityManager entityManager;
    
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
        final NoConflictCertificateData certificateData = getLimitedNoConflictCertDataRow(cainfo, certserno);
        return new CertificateDataWrapper(certificateData);
    }
    
    @Override
    public CertificateStatus getStatus(final String issuerDN, final BigInteger serno) {
        if (log.isTraceEnabled()) {
            log.trace(">getStatus(), dn:" + issuerDN + ", serno=" + serno.toString(16));
        }
        // First, try to look up in CertificateData
        final String dn = CertTools.stringToBCDNString(issuerDN);
        CertificateStatus status = certificateStoreSession.getStatus(issuerDN, serno);
        if (status != CertificateStatus.NOT_AVAILABLE) {
            log.trace("<getStatus()");
            return status;
        }
        // If not found, take most recent certificate from NoConflictCertificateData
        final NoConflictCertificateData noConflictCert = findMostRecentCertData(dn, serno); 
        if (noConflictCert == null) {
            if (log.isTraceEnabled()) {
                log.trace("<getStatus() did not find certificate with dn " + dn + " and serno " + serno.toString(16));
            }
            return CertificateStatus.NOT_AVAILABLE;
        }
        status = CertificateStatusHelper.getCertificateStatus(noConflictCert);
        if (log.isTraceEnabled()) {
            log.trace("<getStatus() returned " + status + " for cert number " + serno.toString(16));
        }
        return status;
        
    }
    
    /**
     * Locates the most recent entry in NoConflictCertificateData for a given issuerdn/serial number combination.
     * Permanent revocations always take precedence over other updates, the first one wins.
     * Otherwise, the most recent update wins.
     * @param issuerdn Issuer DN
     * @param serno Certificate serial number
     * @return NoConflictCertificateData entry, or null if not found. Entity is append-only, so do not modify it.
     */
    private NoConflictCertificateData findMostRecentCertData(final String issuerdn, final BigInteger serno) {
        final Collection<NoConflictCertificateData> certDatas = NoConflictCertificateData.findByIssuerDNSerialNumber(entityManager, issuerdn, serno.toString());
        if (CollectionUtils.isEmpty(certDatas)) {
            log.trace("<findMostRecentCertData(): no certificates found");
            return null;
        }
        NoConflictCertificateData mostRecentData = null;
        for (final NoConflictCertificateData data : certDatas) {
            if (mostRecentData == null) {
                mostRecentData = data;
                continue;
            }
            long timestampThis = data.getUpdateTime() != null ? data.getUpdateTime() : 0;
            long timestampRecent = mostRecentData.getUpdateTime() != null ? mostRecentData.getUpdateTime() : 0;
            if (data.getStatus() == CertificateConstants.CERT_REVOKED && data.getRevocationReason() != RevocationReasons.CERTIFICATEHOLD.getDatabaseValue()) {
                // Permanently revoked certificate always takes precedence over non-permanently revoked one.
                // Older permanent revocations take precedence over newer ones.
                if (mostRecentData.getStatus() != CertificateConstants.CERT_REVOKED || mostRecentData.getRevocationReason() == RevocationReasons.CERTIFICATEHOLD.getDatabaseValue() ||
                        timestampRecent > timestampThis) {
                    mostRecentData = data;
                    continue;
                }
            }
            // Otherwise, most recent status takes precedence
            if (timestampThis > timestampRecent) {
                mostRecentData = data;
            }
        }
        return mostRecentData;
    }

    @Override
    public boolean setRevokeStatus(final AuthenticationToken admin, final CertificateDataWrapper cdw, final Date revokedDate, final int reason)
            throws CertificateRevokeException, AuthorizationDeniedException {
        if (cdw.getBaseCertificateData() instanceof NoConflictCertificateData) {
            if (entityManager.contains(cdw.getBase64CertData())) {
                throw new IllegalStateException("Cannot update existing row in NoConflictCertificateData. It is append-only.");
            }
        }
        return certificateStoreSession.setRevokeStatus(admin, cdw, revokedDate, reason);
    }
    
    /**
     * Returns a row in the append-only NoConflictCertificateData table, or a new row that can be added.
     * The row is initialized with the data from the most recent entry in the table,
     * or as a new unrevoked entry if non-existent.
     * @param cainfo Issuer.
     * @param certserno Certificate serial number.
     * @return New row, or copy of an existing row. Always has a fresh UUID and timestamp, so it can be appended directly.
     */
    private NoConflictCertificateData getLimitedNoConflictCertDataRow(final CAInfo cainfo, final BigInteger certserno) {
        NoConflictCertificateData certificateData = findMostRecentCertData(cainfo.getSubjectDN(), certserno);
        if (certificateData != null) {
            // Make a copy, to prevent overwrites
            certificateData = new NoConflictCertificateData(certificateData);
        } else {
            certificateData = new NoConflictCertificateData();
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
            certificateData.setCaFingerprint(CertTools.getFingerprintAsString(cainfo.getCertificateChain().get(0)));
            certificateData.setEndEntityProfileId(-1);
        }
        // Always generate new UUID and timestamp, so updates are stored as a new row
        certificateData.setId(UUID.randomUUID().toString());
        certificateData.setUpdateTime(System.currentTimeMillis());
        return certificateData;
    }
    
    private static String generateDummyFingerprint(final String issuerdn, final BigInteger certserno) {
        final byte[] fingerprintBytes = CertTools.generateSHA1Fingerprint((certserno.toString()+';'+issuerdn).getBytes(StandardCharsets.UTF_8));
        return new String(Hex.encode(fingerprintBytes));
    }

}
