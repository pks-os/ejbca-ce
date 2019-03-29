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
package org.ejbca.core.ejb.crl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.cert.CRLException;
import java.security.cert.Certificate;
import java.security.cert.X509CRL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.bouncycastle.asn1.x509.Extension;
import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.certificates.ca.CAInfo;
import org.cesecore.certificates.ca.CaSessionRemote;
import org.cesecore.certificates.ca.X509CAInfo;
import org.cesecore.certificates.certificate.CertificateConstants;
import org.cesecore.certificates.certificate.CertificateData;
import org.cesecore.certificates.certificate.CertificateRevokeException;
import org.cesecore.certificates.certificate.InternalCertificateStoreSessionRemote;
import org.cesecore.certificates.certificate.request.SimpleRequestMessage;
import org.cesecore.certificates.certificate.request.X509ResponseMessage;
import org.cesecore.certificates.certificateprofile.CertificateProfile;
import org.cesecore.certificates.certificateprofile.CertificateProfileConstants;
import org.cesecore.certificates.certificateprofile.CertificateProfileSessionRemote;
import org.cesecore.certificates.crl.CRLInfo;
import org.cesecore.certificates.crl.CrlStoreSessionRemote;
import org.cesecore.certificates.crl.RevokedCertInfo;
import org.cesecore.certificates.endentity.EndEntityConstants;
import org.cesecore.certificates.endentity.EndEntityInformation;
import org.cesecore.certificates.endentity.EndEntityTypes;
import org.cesecore.certificates.util.AlgorithmConstants;
import org.cesecore.certificates.util.cert.CrlExtensions;
import org.cesecore.keys.util.KeyTools;
import org.cesecore.mock.authentication.tokens.TestAlwaysAllowLocalAuthenticationToken;
import org.cesecore.util.CertTools;
import org.cesecore.util.EjbRemoteHelper;
import org.ejbca.core.ejb.ca.CaTestCase;
import org.ejbca.core.ejb.ca.caadmin.CAAdminSessionRemote;
import org.ejbca.core.ejb.ca.publisher.PublisherProxySessionRemote;
import org.ejbca.core.ejb.ca.publisher.PublisherQueueProxySessionRemote;
import org.ejbca.core.ejb.ca.sign.SignSessionRemote;
import org.ejbca.core.ejb.ra.raadmin.EndEntityProfileSessionRemote;
import org.ejbca.core.model.ca.publisher.CustomPublisherContainer;
import org.ejbca.core.model.ca.publisher.DummyCustomPublisher;
import org.ejbca.core.model.ca.publisher.PublisherExistsException;
import org.ejbca.core.model.ca.publisher.PublisherQueueData;
import org.ejbca.core.model.ra.raadmin.EndEntityProfile;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests certificate generation and publishing with partitioned CRLs
 * 
 * @version $Id$
 */
public class PartitionedCrlSystemTest {

    private static final Logger log = Logger.getLogger(PartitionedCrlSystemTest.class);

    private static final String TEST_NAME = "PartitionedCrlSystemTest";
    private static final AuthenticationToken admin = new TestAlwaysAllowLocalAuthenticationToken(TEST_NAME);

    private static final CaSessionRemote caSession = EjbRemoteHelper.INSTANCE.getRemoteSession(CaSessionRemote.class);
    private static final CAAdminSessionRemote caAdminSession = EjbRemoteHelper.INSTANCE.getRemoteSession(CAAdminSessionRemote.class);
    private static final CertificateProfileSessionRemote certificateProfileSession = EjbRemoteHelper.INSTANCE.getRemoteSession(CertificateProfileSessionRemote.class);
    private static final CrlStoreSessionRemote crlStoreSession = EjbRemoteHelper.INSTANCE.getRemoteSession(CrlStoreSessionRemote.class);
    private static final EndEntityProfileSessionRemote endEntityProfileSession = EjbRemoteHelper.INSTANCE.getRemoteSession(EndEntityProfileSessionRemote.class);
    private static final InternalCertificateStoreSessionRemote internalCertificateSessionSession = EjbRemoteHelper.INSTANCE.getRemoteSession(InternalCertificateStoreSessionRemote.class, EjbRemoteHelper.MODULE_TEST);
    private static final PublishingCrlSessionRemote publishingCrlSession = EjbRemoteHelper.INSTANCE.getRemoteSession(PublishingCrlSessionRemote.class);
    private static final PublisherProxySessionRemote publisherSession = EjbRemoteHelper.INSTANCE.getRemoteSession(PublisherProxySessionRemote.class, EjbRemoteHelper.MODULE_TEST);
    private static final PublisherQueueProxySessionRemote publisherQueueSession = EjbRemoteHelper.INSTANCE.getRemoteSession(PublisherQueueProxySessionRemote.class, EjbRemoteHelper.MODULE_TEST);
    private static final SignSessionRemote signSession = EjbRemoteHelper.INSTANCE.getRemoteSession(SignSessionRemote.class);

    private static final String TEST_CA = TEST_NAME + "_CA";
    private static final String TEST_CRL_PUBLISHER = TEST_NAME + "_CRL_PUBLISHER";
    private static final String TEST_CERTIFICATE_PUBLISHER = TEST_NAME + "_CERTIFICATE_PUBLISHER";
    private static final String TEST_ENDENTITY = TEST_NAME + "_ENDENTITY";
    private static final String TEST_CERTPROFILE = TEST_NAME + "_CP";
    private static final String TEST_EEPROFILE = TEST_NAME + "_EEP";

    private static final String CA_DN = "CN=TestCA with CRL Partitioning,OU=QA,O=TEST,C=SE";
    private static final String CERT_DN = "CN=Partitioned CRL User,OU=QA,O=TEST,C=SE";

    /** CRL Number of the first generated CRL. This will always be a Base CRL */
    private static final int FIRST_CRL_NUMBER = 1;
    /** CRL Number of the second CRL. This will be a Delt CRL in all cases in this test */
    private static final int SECOND_CRL_NUMBER = 2;

    private static final String CRLDP_TEMPLATE_URI = "http://crl*.example.com/TestCRL*.crl";
    private static final String CRLDP_MAIN_URI = "http://crl.example.com/TestCRL.crl";
    private static final String CRLDP_PARTITION1_URI = "http://crl1.example.com/TestCRL1.crl";

    private static final String DELTACRLDP_TEMPLATE_URI = "http://crl*.example.com/TestDeltaCRL*.crl";
    private static final String DELTACRLDP_MAIN_URI = "http://crl.example.com/TestDeltaCRL.crl";
    private static final String DELTACRLDP_PARTITION1_URI = "http://crl1.example.com/TestDeltaCRL1.crl";

    private static KeyPair userKeyPair;
    private static int caId, certificateProfileId, endEntityProfileId;

    @BeforeClass
    public static void beforeClass() throws Exception {
        log.trace(">beforeClass");
        cleanupClass();
        CaTestCase.createTestCA(TEST_CA, 1024, CA_DN, CAInfo.SELFSIGNED, null);
        caId = caSession.getCAInfo(admin, TEST_CA).getCAId();
        // Publishers. These will never run, but will accept CRLs/certificates in the queue.
        final int crlPublisherId = addPublisher(TEST_CRL_PUBLISHER);
        final int certPublisherId = addPublisher(TEST_CERTIFICATE_PUBLISHER);
        // Key pair and profiles for user certificates
        userKeyPair = KeyTools.genKeys("1024", AlgorithmConstants.KEYALGORITHM_RSA);
        final CertificateProfile certProf = new CertificateProfile(CertificateProfileConstants.CERTPROFILE_FIXED_ENDUSER);
        certProf.setUseCRLDistributionPoint(true);
        certProf.setUseDefaultCRLDistributionPoint(true);
        certProf.setPublisherList(new ArrayList<>(Collections.singleton(certPublisherId)));
        certificateProfileId = certificateProfileSession.addCertificateProfile(admin, TEST_CERTPROFILE, certProf);
        final EndEntityProfile eeProf = new EndEntityProfile(false);
        eeProf.setAvailableCAs(new ArrayList<>(Collections.singleton(caId)));
        eeProf.setAvailableCertificateProfileIds(new ArrayList<>(Collections.singleton(certificateProfileId)));
        endEntityProfileId = endEntityProfileSession.addEndEntityProfile(admin, TEST_EEPROFILE, eeProf);
        // Set common settings to all tests CA
        final X509CAInfo caInfo = (X509CAInfo) caSession.getCAInfo(admin, TEST_CA);
        caInfo.setUseCrlDistributionPointOnCrl(true);
        caInfo.setCrlDistributionPointOnCrlCritical(true);
        caInfo.setUseCRLNumber(true);
        caInfo.setCRLIssueInterval(20);
        caInfo.setDeltaCRLPeriod(10);
        caInfo.setCRLPublishers(new ArrayList<>(Collections.singleton(crlPublisherId)));
        caInfo.setUseUserStorage(false); // avoid having to create end entities
        caAdminSession.editCA(admin, caInfo);
        internalCertificateSessionSession.removeCRLs(admin, CA_DN); // remove initial CRLs
        log.trace("<beforeClass");
    }

    private static int addPublisher(final String name) throws PublisherExistsException, AuthorizationDeniedException {
        final CustomPublisherContainer publisher = new CustomPublisherContainer();
        publisher.setClassPath(DummyCustomPublisher.class.getName());
        publisher.setDescription("CRL publisher. Used in system test");
        publisher.setName(TEST_CRL_PUBLISHER);
        publisher.setOnlyUseQueue(true);
        publisher.setUseQueueForCertificates(true);
        publisher.setUseQueueForCRLs(true);
        publisher.setKeepPublishedInQueue(false);
        return publisherSession.addPublisher(admin, name, publisher);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        log.trace(">afterClass");
        cleanupClass();
        log.trace("<afterClass");
    }

    private static void cleanupClass() throws Exception {
        log.trace(">cleanup");
        cleanupTestCase();
        CaTestCase.removeTestCA(TEST_CA);
        endEntityProfileSession.removeEndEntityProfile(admin, TEST_EEPROFILE);
        certificateProfileSession.removeCertificateProfile(admin, TEST_CERTPROFILE);
        publisherSession.removePublisherInternal(admin, TEST_CRL_PUBLISHER);
        publisherSession.removePublisherInternal(admin, TEST_CERTIFICATE_PUBLISHER);
        log.trace("<cleanup");
    }

    private static void cleanupTestCase() throws AuthorizationDeniedException {
        internalCertificateSessionSession.removeCertificatesBySubject(CERT_DN);
        internalCertificateSessionSession.removeCRLs(admin, CA_DN);
        publisherQueueSession.removePublisherQueueEntries(TEST_CRL_PUBLISHER);
        publisherQueueSession.removePublisherQueueEntries(TEST_CERTIFICATE_PUBLISHER);
    }

    @After
    public void after() throws AuthorizationDeniedException {
        cleanupTestCase();
    }

    /**
     * Test CRL generation and publisher queuing without CRL partitioning. The following steps are performed:
     * <ol>
     * <li>Issue certificate, revoke, create Base CRL.
     * <li>Issue another certificate, revoke, create Delta CRL
     * </ol>
     * For both steps, we expect a CRL with the correct contents, and that the CRLs and certificates are placed in the publisher queue.
     */
    @Test
    public void generateAndPublishPlainCrl() throws Exception {
        log.trace(">generateAndPublishPlainCrl");
        // Given
        final X509CAInfo caInfo = (X509CAInfo) caSession.getCAInfo(admin, TEST_CA);
        caInfo.setUsePartitionedCrl(false);
        caInfo.setDefaultCRLDistPoint(CRLDP_MAIN_URI);
        caInfo.setCADefinedFreshestCRL(DELTACRLDP_MAIN_URI);
        caInfo.setCrlPartitions(0);
        caInfo.setRetiredCrlPartitions(0);
        caAdminSession.editCA(admin, caInfo);
        // When
        final Certificate cert = issueCertificate(); // should appear on CRL
        final Certificate deltaCert = issueCertificate(); // should appear on Delta CRL
        revokeCertificate(cert, new Date(new Date().getTime() - 5*60*1000)); // backdate revocation by 5 minutes
        assertTrue("CRL generation failed", publishingCrlSession.forceCRL(admin, caId));
        revokeCertificate(deltaCert, new Date());
        assertTrue("Delta CRL generation failed", publishingCrlSession.forceDeltaCRL(admin, caId));
        // Then
        assertEquals("Wrong CRL DP in first certificate.", CRLDP_MAIN_URI, CertTools.getCrlDistributionPoint(cert));
        assertEquals("Wrong CRL DP in second certificate.", CRLDP_MAIN_URI, CertTools.getCrlDistributionPoint(deltaCert));
        final X509CRL crl = getLatestCrl(CertificateConstants.NO_CRL_PARTITION, false, FIRST_CRL_NUMBER);
        final X509CRL deltaCrl = getLatestCrl(CertificateConstants.NO_CRL_PARTITION, true, SECOND_CRL_NUMBER);
        assertEquals("Wrong Base CRL number on Delta CRL.", BigInteger.valueOf(FIRST_CRL_NUMBER), CrlExtensions.getDeltaCRLIndicator(deltaCrl));
        assertEquals("Wrong Issuing Distribution Point in CRL.", Collections.singletonList(CRLDP_MAIN_URI), CertTools.getCrlDistributionPoints(crl));
        assertEquals("Wrong Freshest CRL DP in CRL.", Collections.singletonList(DELTACRLDP_MAIN_URI), CrlExtensions.extractFreshestCrlDistributionPoints(crl));
        assertEquals("Wrong Issuing Distribution Point in Delta CRL.", Collections.singletonList(CRLDP_MAIN_URI), CertTools.getCrlDistributionPoints(deltaCrl));
        assertInclusionOnCrl(crl, deltaCrl, cert, deltaCert);
        assertCorrectPartitionIndexInCertData(cert, CertificateConstants.NO_CRL_PARTITION);
        assertCorrectPartitionIndexInCertData(deltaCert, CertificateConstants.NO_CRL_PARTITION);
        assertCrlPublisherQueueData(Arrays.asList(crl, deltaCrl));
        assertCertificatePublisherQueueData(Arrays.asList(cert, deltaCert));
        log.trace("<generateAndPublishPlainCrl");
    }
    
    /**
     * Test CRL generation and publisher queuing with CRL partitioning. The following steps are performed:
     * <ol>
     * <li>Issue certificate, revoke, create Base CRL.
     * <li>Issue another certificate, revoke, create Delta CRL
     * </ol>
     * For both steps, we expect a CRL with the correct contents, and that the CRLs and certificates are placed in the publisher queue.
     */
    @Test
    public void generateAndPublishPartitionedCrl() throws Exception {
        log.trace(">generateAndPublishPartitionedCrl");
        // Given
        final int partitionIndex = 1;
        final X509CAInfo caInfo = (X509CAInfo) caSession.getCAInfo(admin, TEST_CA);
        caInfo.setUsePartitionedCrl(true);
        caInfo.setDefaultCRLDistPoint(CRLDP_TEMPLATE_URI);
        caInfo.setCADefinedFreshestCRL(DELTACRLDP_TEMPLATE_URI);
        caInfo.setCrlPartitions(1);
        caInfo.setRetiredCrlPartitions(0);
        caAdminSession.editCA(admin, caInfo);
        // When
        final Certificate cert = issueCertificate(); // should appear on CRL
        final Certificate deltaCert = issueCertificate(); // should appear on Delta CRL
        revokeCertificate(cert, new Date(new Date().getTime() - 5*60*1000)); // backdate revocation by 5 minutes
        assertTrue("CRL generation failed", publishingCrlSession.forceCRL(admin, caId));
        revokeCertificate(deltaCert, new Date());
        assertTrue("Delta CRL generation failed", publishingCrlSession.forceDeltaCRL(admin, caId));
        // Then
        assertEquals("Wrong CRL DP in first certificate.", CRLDP_PARTITION1_URI, CertTools.getCrlDistributionPoint(cert));
        assertEquals("Wrong CRL DP in second certificate.", CRLDP_PARTITION1_URI, CertTools.getCrlDistributionPoint(deltaCert));
        // Legacy CRL (partition 0), always created
        final X509CRL legacyCrl = getLatestCrl(CertificateConstants.NO_CRL_PARTITION, false, FIRST_CRL_NUMBER);
        final X509CRL legacyDeltaCrl = getLatestCrl(CertificateConstants.NO_CRL_PARTITION, true, SECOND_CRL_NUMBER);
        assertEquals("Wrong Issuing Distribution Point in CRL (partition 0).", Collections.singletonList(CRLDP_MAIN_URI), CertTools.getCrlDistributionPoints(legacyCrl));
        assertEquals("Wrong Freshest CRL DP in CRL (partition 0).", Collections.singletonList(DELTACRLDP_MAIN_URI), CrlExtensions.extractFreshestCrlDistributionPoints(legacyCrl));
        assertEquals("Wrong Issuing Distribution Point in Delta CRL (partition 0).", Collections.singletonList(CRLDP_MAIN_URI), CertTools.getCrlDistributionPoints(legacyDeltaCrl));
        // Partition 1
        final X509CRL crl = getLatestCrl(partitionIndex, false, FIRST_CRL_NUMBER);
        final X509CRL deltaCrl = getLatestCrl(partitionIndex, true, SECOND_CRL_NUMBER);
        assertEquals("Wrong Base CRL number on Delta CRL (partition 1).", BigInteger.valueOf(FIRST_CRL_NUMBER), CrlExtensions.getDeltaCRLIndicator(deltaCrl));
        assertEquals("Wrong Issuing Distribution Point in CRL (partition 1).", Collections.singletonList(CRLDP_PARTITION1_URI), CertTools.getCrlDistributionPoints(crl));
        assertEquals("Wrong Freshest CRL DP in CRL (partition 1).", Collections.singletonList(DELTACRLDP_PARTITION1_URI), CrlExtensions.extractFreshestCrlDistributionPoints(crl));
        assertEquals("Wrong Issuing Distribution Point in Delta CRL (partition 1).", Collections.singletonList(CRLDP_PARTITION1_URI), CertTools.getCrlDistributionPoints(deltaCrl));
        assertInclusionOnCrl(crl, deltaCrl, cert, deltaCert);
        assertNull("Legacy CRL (partition 0) should be empty.", legacyCrl.getRevokedCertificates());
        assertNull("Legacy Delta CRL (partition 0) should be empty.", legacyDeltaCrl.getRevokedCertificates());
        assertCorrectPartitionIndexInCertData(cert, partitionIndex);
        assertCorrectPartitionIndexInCertData(deltaCert, partitionIndex);
        assertCrlPublisherQueueData(Arrays.asList(legacyCrl, legacyDeltaCrl, crl, deltaCrl));
        assertCertificatePublisherQueueData(Arrays.asList(cert, deltaCert));
        log.trace("<generateAndPublishPartitionedCrl");
    }

    /** Issues a certificate with the CRL Distribution Point URI from the CA */
    private Certificate issueCertificate() throws Exception {
        final EndEntityInformation endEntity = new EndEntityInformation();
        endEntity.setType(EndEntityTypes.ENDUSER.toEndEntityType());
        endEntity.setUsername(TEST_ENDENTITY);
        endEntity.setPassword("foo123");
        endEntity.setDN(CERT_DN);
        endEntity.setCAId(caId);
        endEntity.setCertificateProfileId(certificateProfileId);
        endEntity.setEndEntityProfileId(endEntityProfileId);
        endEntity.setTokenType(EndEntityConstants.TOKEN_USERGEN);

        final SimpleRequestMessage req = new SimpleRequestMessage(userKeyPair.getPublic(), TEST_ENDENTITY, "foo123");
        req.setIssuerDN(CA_DN);
        req.setRequestDN(CERT_DN);

        final X509ResponseMessage resp = (X509ResponseMessage) signSession.createCertificate(admin, req,
                org.cesecore.certificates.certificate.request.X509ResponseMessage.class, endEntity);
        assertNotNull("Failed to get response", resp);
        assertNotNull("No certificate was returned. " + resp.getFailText(), resp.getCertificate());
        log.debug("Issued certificate with fingerprint " + CertTools.getFingerprintAsString(resp.getCertificate()));
        return resp.getCertificate();
    }

    /** Revokes a certificate. Supports backdated revocation. */
    private void revokeCertificate(final Certificate cert, final Date revocationDate) throws CertificateRevokeException, AuthorizationDeniedException {
        internalCertificateSessionSession.setRevokeStatus(admin, cert, revocationDate, RevokedCertInfo.REVOCATION_REASON_SUPERSEDED);
        log.debug("Revoked certificate with fingerprint " + CertTools.getFingerprintAsString(cert));
    }

    /** Retrieves the latest Base or Delta CRL from database, and checks that it is the correct CRL */ 
    private X509CRL getLatestCrl(final int crlPartitionIndex, final boolean deltaCrl, final int expectedCrlNumber) throws CRLException {
        final byte[] crlBytes = crlStoreSession.getLastCRL(CA_DN, crlPartitionIndex, deltaCrl);
        final String crlDescription = (deltaCrl ? "Delta CRL" : "Base CRL") + " for partition " + crlPartitionIndex;
        assertNotNull(crlDescription + " was not created", crlBytes);
        log.debug("Fingerprint for " + crlDescription + " is: " + CertTools.getFingerprintAsString(crlBytes));
        final X509CRL crl = CertTools.getCRLfromByteArray(crlBytes);
        // Check that the CRL is correct
        assertEquals("deltaCRLIndicator extension precense is wrong.", deltaCrl, crl.getExtensionValue(Extension.deltaCRLIndicator.getId()) != null);
        assertEquals("Wrong CRL Number", BigInteger.valueOf(expectedCrlNumber), CrlExtensions.getCrlNumber(crl));
        return crl;
    }

    /** Asserts that the correct certificates are in the correct CRLs */
    private void assertInclusionOnCrl(final X509CRL crl, final X509CRL deltaCrl, final Certificate cert, final Certificate deltaCert) {
        assertTrue("First certificate should be revoked on Base CRL", crl.isRevoked(cert));
        assertFalse("Second certificate should NOT be revoked on Base CRL", crl.isRevoked(deltaCert));
        assertFalse("First certificate should NOT be revoked on Delta CRL", deltaCrl.isRevoked(cert));
        assertTrue("Second certificate should be revoked on Delta CRL", deltaCrl.isRevoked(deltaCert));
    }

    private void assertCorrectPartitionIndexInCertData(final Certificate cert, final int expectedCrlPartitionIndex) {
        final String fingerprint = CertTools.getFingerprintAsString(cert);
        final CertificateData certData = internalCertificateSessionSession.getCertificateData(fingerprint);
        assertEquals("Wrong CRL partition index in CertificateData for cert with fingerprint " + fingerprint, Integer.valueOf(expectedCrlPartitionIndex), certData.getCrlPartitionIndex());
    }

    /** Asserts that the given CRLs, and nothing else, have been queued in the CRL publisher */
    private void assertCrlPublisherQueueData(final Collection<X509CRL> crls) {
        final HashSet<String> fingerprints = new HashSet<>();
        for (final X509CRL crl : crls) {
            fingerprints.add(CertTools.getFingerprintAsString(crl));
        }
        assertPublisherQueueData(TEST_CRL_PUBLISHER, "CRL", fingerprints);
    }

    /** Asserts that the given certificates, and nothing else, have been queued in the certificate publisher */
    private void assertCertificatePublisherQueueData(final Collection<Certificate> certificates) {
        final HashSet<String> fingerprints = new HashSet<>();
        for (final Certificate cert : certificates) {
            fingerprints.add(CertTools.getFingerprintAsString(cert));
        }
        assertPublisherQueueData(TEST_CERTIFICATE_PUBLISHER, "certificate", fingerprints);
    }

    private void assertPublisherQueueData(final String publisherName, final String type, final Collection<String> fingerprints) {
        final int publisherId = publisherSession.getPublisherId(publisherName);
        final Collection<PublisherQueueData> queue = publisherQueueSession.getPendingEntriesForPublisher(publisherId);
        traceLogPublisherData(type, queue, fingerprints);
        assertEquals("Wrong number of " + type + " entries in publisher queue.", fingerprints.size(), queue.size());
        final HashSet<String> remainingFingerprints = new HashSet<>(fingerprints);
        for (final PublisherQueueData queueEntry : queue) {
            final String fingerprint = queueEntry.getFingerprint();
            assertTrue("Unexpected " + type + " publisher queue entry with fingerprint " + fingerprint, remainingFingerprints.remove(fingerprint));
        }
        assertEquals("Some " + type + " entries were not found in the publisher queue.", 0, remainingFingerprints.size()); // this should never fail at this point
    }

    private void traceLogPublisherData(final String type, final Collection<PublisherQueueData> queue, final Collection<String> fingerprints) {
        log.trace("Expected " + type + " queue entries: " + fingerprints);
        for (final PublisherQueueData queueEntry : queue) {
            final String fingerprint = queueEntry.getFingerprint();
            log.trace("Queue entry fingerprint: " + fingerprint);
            final CRLInfo crlInfo = crlStoreSession.getCRLInfo(fingerprint);
            if (crlInfo != null) {
                log.trace("CRL '" + crlInfo.getSubjectDN() + "', number " + crlInfo.getLastCRLNumber() + ", partition " + crlInfo.getCrlPartitionIndex());
            }
            final CertificateData certData = internalCertificateSessionSession.getCertificateData(fingerprint);
            if (certData != null) {
                log.trace("Certificate '" + certData.getSubjectDN() + "', partition number " + certData.getCrlPartitionIndex());
            }
        }
    }
}
