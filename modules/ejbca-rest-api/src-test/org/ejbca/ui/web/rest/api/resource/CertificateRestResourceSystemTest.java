/*************************************************************************
 *                                                                       *
 *  EJBCA Community: The OpenSource Certificate Authority                *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.ejbca.ui.web.rest.api.resource;

import org.apache.log4j.Logger;
import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authentication.tokens.UsernamePrincipal;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.certificates.ca.ApprovalRequestType;
import org.cesecore.certificates.ca.X509CA;
import org.cesecore.certificates.certificateprofile.CertificateProfileConstants;
import org.cesecore.certificates.endentity.EndEntityConstants;
import org.cesecore.certificates.endentity.EndEntityInformation;
import org.cesecore.certificates.endentity.EndEntityType;
import org.cesecore.certificates.endentity.EndEntityTypes;
import org.cesecore.certificates.endentity.ExtendedInformation;
import org.cesecore.certificates.util.AlgorithmConstants;
import org.cesecore.keys.token.CryptoTokenTestUtils;
import org.cesecore.keys.util.KeyTools;
import org.cesecore.mock.authentication.tokens.TestAlwaysAllowLocalAuthenticationToken;
import org.cesecore.util.CryptoProviderTools;
import org.ejbca.config.AvailableProtocolsConfiguration;
import org.ejbca.config.AvailableProtocolsConfiguration.AvailableProtocols;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.approval.Approval;
import org.ejbca.core.model.approval.ApprovalDataVO;
import org.ejbca.core.model.approval.WaitingForApprovalException;
import org.ejbca.core.model.approval.profile.AccumulativeApprovalProfile;
import org.ejbca.ui.web.rest.api.io.request.FinalizeRestResquest;
import org.ejbca.util.query.ApprovalMatch;
import org.ejbca.util.query.BasicMatch;
import org.ejbca.util.query.IllegalQueryException;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.cesecore.util.Base64;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.ejbca.ui.web.rest.api.Assert.EjbcaAssert.assertJsonContentType;
import static org.ejbca.ui.web.rest.api.Assert.EjbcaAssert.assertProperJsonStatusResponse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.security.KeyStore;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * A unit test class for CertificateRestResource to test its content.
 *
 * @version $Id: CertificateRestResourceSystemTest.java 29080 2018-05-31 11:12:13Z andrey_s_helmes $
 */
public class CertificateRestResourceSystemTest extends RestResourceSystemTestBase {

    private static final Logger log = Logger.getLogger(CertificateRestResourceSystemTest.class);
    private static final String TEST_CA_NAME = "RestCertificateResourceTestCa";
    private static final String TEST_USERNAME = "keystoreFinalizeUser";
    private static final JSONParser jsonParser = new JSONParser();
    private AvailableProtocolsConfiguration protcolConfigBackup;
    
    private static X509CA x509TestCa;
    
    @BeforeClass
    public static void beforeClass() throws AuthorizationDeniedException {
        RestResourceSystemTestBase.beforeClass();

    }

    @AfterClass
    public static void afterClass() throws Exception {
        RestResourceSystemTestBase.afterClass();
    }

    @Before
    public void setUp() throws Exception {
        CryptoProviderTools.installBCProvider();
        x509TestCa = CryptoTokenTestUtils.createTestCAWithSoftCryptoToken(INTERNAL_ADMIN_TOKEN, "C=SE,CN=" + TEST_CA_NAME);
    }

    @After
    public void tearDown() throws AuthorizationDeniedException {
        caSession.removeCA(INTERNAL_ADMIN_TOKEN, x509TestCa.getCAId());
    }

    @Test
    public void shouldReturnStatusInformation() throws Exception {
        // given
        final String expectedStatus = "OK";
        final String expectedVersion = "1.0";
        final String expectedRevision = "ALPHA";
        // when
        final ClientResponse<?> actualResponse = newRequest("/v1/certificate/status").get();
        final String actualJsonString = actualResponse.getEntity(String.class);
        // then
        assertEquals(Response.Status.OK.getStatusCode(), actualResponse.getStatus());
        assertJsonContentType(actualResponse);
        assertProperJsonStatusResponse(expectedStatus, expectedVersion, expectedRevision, actualJsonString);
    }

    @Test
    public void finalizeKeyStoreExpectPkcs12Response() throws Exception {
        // Create an add end entity approval request
        final AuthenticationToken approvalAdmin = new TestAlwaysAllowLocalAuthenticationToken(new UsernamePrincipal("EjbcaRestApiApprovalTestAdmin"));
        AccumulativeApprovalProfile approvalProfile = new AccumulativeApprovalProfile("Test Approval Profile");
        approvalProfile.setNumberOfApprovalsRequired(1);
        approvalProfile.initialize();
        int profileId = -1;
        int approvalId = -1;
        
        try {
            // Generate approval request
            profileId = approvalProfileSession.addApprovalProfile(INTERNAL_ADMIN_TOKEN, approvalProfile);
            LinkedHashMap<ApprovalRequestType, Integer> approvalsMap = new LinkedHashMap<>();
            approvalsMap.put(ApprovalRequestType.ADDEDITENDENTITY, profileId);
            x509TestCa.getCAInfo().setApprovals(approvalsMap);
            caSession.editCA(INTERNAL_ADMIN_TOKEN, x509TestCa.getCAInfo());
            EndEntityInformation userdata = new EndEntityInformation(TEST_USERNAME, "CN=" + TEST_USERNAME, x509TestCa.getCAId(), null, null, new EndEntityType(
                    EndEntityTypes.ENDUSER), EndEntityConstants.EMPTY_END_ENTITY_PROFILE, CertificateProfileConstants.CERTPROFILE_FIXED_ENDUSER,
                    SecConst.TOKEN_SOFT_P12, 0, new ExtendedInformation());
            userdata.setPassword("foo123");
            userdata.setStatus(EndEntityConstants.STATUS_NEW);
            userdata.getExtendedInformation().setKeyStoreAlgorithmType(AlgorithmConstants.KEYALGORITHM_RSA);
            userdata.getExtendedInformation().setKeyStoreAlgorithmSubType("1024");
            int requestId = -1;
            try {
                endEntityManagementSession.addUser(INTERNAL_ADMIN_TOKEN, userdata, false);
                fail("Expected WaitingForApprovalException");
            } catch (WaitingForApprovalException e) {
                requestId = e.getRequestId();
            }
            Approval approval = new Approval("REST System Test Approval", AccumulativeApprovalProfile.FIXED_STEP_ID , 
                    approvalProfile.getStep(AccumulativeApprovalProfile.FIXED_STEP_ID).getPartitions().
                    values().iterator().next().getPartitionIdentifier());
            approvalId = getApprovalDataNoAuth(requestId).getApprovalId();
            approvalExecutionSession.approve(approvalAdmin, approvalId, approval);
            
            // Attempt REST finalize
            final FinalizeRestResquest requestObject = new FinalizeRestResquest("P12", "foo123");
            final ObjectMapper objectMapper = objectMapperContextResolver.getContext(null);
            final String requestBody = objectMapper.writeValueAsString(requestObject);
            final ClientRequest request = newRequest("/v1/certificate/" + requestId + "/finalize");
            request.body(MediaType.APPLICATION_JSON, requestBody);
                    
            final ClientResponse<?> actualResponse = request.post();
            final String actualJsonString = actualResponse.getEntity(String.class);
            assertJsonContentType(actualResponse);
            final JSONObject actualJsonObject = (JSONObject) jsonParser.parse(actualJsonString);
            final String responseFormat = (String) actualJsonObject.get("response_format");
            final String base64Keystore = (String) actualJsonObject.get("certificate");
            final byte[] keystoreBytes = Base64.decode(base64Keystore.getBytes());
            KeyStore keyStore = KeyTools.createKeyStore(keystoreBytes, "foo123");
            // Verify results
            Enumeration<String> aliases = keyStore.aliases();
            assertEquals("Unexpected alias in keystore response", TEST_USERNAME, aliases.nextElement());
            assertEquals("Unexpected response format", "PKCS12", responseFormat);
            assertEquals("Unexpected keystore format", "PKCS12", keyStore.getType());
        } finally {
            // Clean up
            approvalSession.removeApprovalRequest(INTERNAL_ADMIN_TOKEN, approvalId);
            approvalProfileSession.removeApprovalProfile(INTERNAL_ADMIN_TOKEN, profileId);
            endEntityManagementSession.deleteUser(INTERNAL_ADMIN_TOKEN, TEST_USERNAME);
        }
    }

    private ApprovalDataVO getApprovalDataNoAuth(final int id) {
        final org.ejbca.util.query.Query query = new org.ejbca.util.query.Query(org.ejbca.util.query.Query.TYPE_APPROVALQUERY);
        query.add(ApprovalMatch.MATCH_WITH_UNIQUEID, BasicMatch.MATCH_TYPE_EQUALS, Integer.toString(id));
        final List<ApprovalDataVO> approvals;
        try {
            approvals = approvalProxySession.query(query, 0, 100, "", "");
        } catch (IllegalQueryException e) {
            throw new IllegalStateException("Query for approval request failed: " + e.getMessage(), e);
        }
        if (approvals.isEmpty()) {
            return null;
        }
        return approvals.iterator().next();
    }
    
    /**
     * Disables REST and then runs a simple REST access test which will expect status 403 when
     * service is disabled by configuration.
     * @throws Exception 
     */
    @Test
    public void disableRestExpectStatusForbidden() throws Exception {
        AvailableProtocolsConfiguration protcolConfig = (AvailableProtocolsConfiguration)globalConfigurationSession.
                getCachedConfiguration(AvailableProtocolsConfiguration.CONFIGURATION_ID);
        protcolConfigBackup = (AvailableProtocolsConfiguration)globalConfigurationSession.
                getCachedConfiguration(AvailableProtocolsConfiguration.CONFIGURATION_ID);
        // given
        protcolConfig.setProtocolStatus(AvailableProtocols.REST.getName(), false);
        globalConfigurationSession.saveConfiguration(INTERNAL_ADMIN_TOKEN, protcolConfig);
        // when
        final ClientResponse<?> actualResponse = newRequest("/v1/certificate/status").get();
        int status = actualResponse.getStatus();
        // then
        assertEquals("Unexpected response after disabling protocol", 403, status);
        // restore state
        globalConfigurationSession.saveConfiguration(INTERNAL_ADMIN_TOKEN, protcolConfigBackup);
    }
}