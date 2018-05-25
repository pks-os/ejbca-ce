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

package org.ejbca.core.ejb.rest;

import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.certificates.ca.CADoesntExistsException;
import org.cesecore.certificates.ca.CAInfo;
import org.cesecore.certificates.ca.CaSessionLocal;
import org.cesecore.certificates.ca.X509CAInfo;
import org.cesecore.certificates.ca.catoken.CAToken;
import org.cesecore.certificates.certificateprofile.CertificateProfileDoesNotExistException;
import org.cesecore.certificates.certificateprofile.CertificateProfileSessionLocal;
import org.cesecore.certificates.endentity.EndEntityInformation;
import org.cesecore.util.Base64;
import org.cesecore.util.CertTools;
import org.easymock.EasyMock;
import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.ejbca.core.EjbcaException;
import org.ejbca.core.ejb.authentication.web.WebAuthenticationProviderSessionLocal;
import org.ejbca.core.ejb.ra.raadmin.EndEntityProfileSessionLocal;
import org.ejbca.core.model.era.RaMasterApiProxyBeanLocal;
import org.ejbca.core.model.ra.raadmin.EndEntityProfile;
import org.ejbca.core.model.ra.raadmin.EndEntityProfileNotFoundException;
import org.ejbca.core.protocol.rest.EnrollPkcs10CertificateRequest;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;


/**
 *
 * @version $Id: EjbcaRestHelperUnitTest.java 29025 2018-05-25 08:45:54Z tarmo_r_helmes $
 *
 */
@RunWith(EasyMockRunner.class)
public class EjbcaRestHelperUnitTest {
    
    @TestSubject
    EjbcaRestHelperSessionBean testClass = new EjbcaRestHelperSessionBean();    
    
    @Mock
    private WebAuthenticationProviderSessionLocal authenticationSession;
    
    @Mock
    private RaMasterApiProxyBeanLocal raMasterApiProxyBean;
    
    @Mock
    private EndEntityProfileSessionLocal endEntityProfileSessionBean;

    @Mock
    private CertificateProfileSessionLocal certificateProfileSessionBean;
    @Mock
    private CaSessionLocal caSessionBean;
    
    final String csr = "-----BEGIN CERTIFICATE REQUEST-----\n"
            + "MIIDWDCCAkACAQAwYTELMAkGA1UEBhMCRUUxEDAOBgNVBAgTB0FsYWJhbWExEDAO\n"
            + "BgNVBAcTB3RhbGxpbm4xFDASBgNVBAoTC25hYWJyaXZhbHZlMRgwFgYDVQQDEw9o\n"
            + "ZWxsbzEyM3NlcnZlcjYwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDe\n"
            + "lRzGyeXlCQL3lgLjzEn4qcbD0qtth8rXAwjg/eEN1u8lpQp3GtByWm6LeeB7CEyP\n"
            + "fyy+rW9C7nQmXvJ09cJaLAlETpGjjfZLy6pHzle/D192THB2MYZRuvvAPCfpjjnV\n"
            + "hP9sYn7GN7kCaYh61fvlD2fVquzqRdz9kjib3mVEmswkS6lHuAPIsmI7SG9UuvPR\n"
            + "ND1DOsmVwqOL62EOE/RlHRStxZDHQDoYMqZISAO5arpbDujn666IVqLs1QpsQ5Ih\n"
            + "Avxlw+EGNzzYMCbFEkuGs5JK/YNS7JL3JrvMor8XLngaatbteztK0o+khgT2K9x7\n"
            + "BCkqEoz9iJrmO3B8JDATAgMBAAGggbEwga4GCSqGSIb3DQEJDjGBoDCBnTBQBgNV\n"
            + "HREESTBHggtzb21lZG5zLmNvbYcEwKgBB4ISc29tZS5vdGhlci5kbnMuY29tpB4w\n"
            + "HDENMAsGA1UEAxMEVGVzdDELMAkGA1UEBxMCWFgwMQYDVR0lBCowKAYIKwYBBQUH\n"
            + "AwEGCCsGAQUFBwMCBggrBgEFBQcDAwYIKwYBBQUHAwQwCQYDVR0TBAIwADALBgNV\n"
            + "HQ8EBAMCBeAwDQYJKoZIhvcNAQELBQADggEBAM2cW62D4D4vxaKVtIYpgolbD0zv\n"
            + "WyEA6iPa4Gg2MzeLJVswQoZXCj5gDOrttHDld3QQTDyT9GG0Vg8N8Tr9i44vUr7R\n"
            + "gK5w+PMq2ExGS48YrCoMqV+AJHaeXP+gi23ET5F6bIJnpM3ru6bbZC5IUE04YjG6\n"
            + "xQux6UsxQabuaTrHpExMgYjwJsekEVe13epUq5OiEh7xTJaSnsZm+Ja+MV2pn0gF\n"
            + "3V1hMBajTMGN9emWLR6pfj5P7QpVR4hkv3LvgCPf474pWA9l/4WiKBzrI76T5yz1\n"
            + "KoobCZQ2UrqnKFGEbdoNFchb2CDgdLnFu6Tbf6MW5zO5ypOIUih61Zf9Qyo=\n"
            + "-----END CERTIFICATE REQUEST-----\n";
    
    
    final byte[] testCertificateBytes = Base64.decode((
            "MIICWzCCAcSgAwIBAgIIJND6Haa3NoAwDQYJKoZIhvcNAQEFBQAwLzEPMA0GA1UE"
            + "AxMGVGVzdENBMQ8wDQYDVQQKEwZBbmFUb20xCzAJBgNVBAYTAlNFMB4XDTAyMDEw"
            + "ODA5MTE1MloXDTA0MDEwODA5MjE1MlowLzEPMA0GA1UEAxMGMjUxMzQ3MQ8wDQYD"
            + "VQQKEwZBbmFUb20xCzAJBgNVBAYTAlNFMIGdMA0GCSqGSIb3DQEBAQUAA4GLADCB"
            + "hwKBgQCQ3UA+nIHECJ79S5VwI8WFLJbAByAnn1k/JEX2/a0nsc2/K3GYzHFItPjy"
            + "Bv5zUccPLbRmkdMlCD1rOcgcR9mmmjMQrbWbWp+iRg0WyCktWb/wUS8uNNuGQYQe"
            + "ACl11SAHFX+u9JUUfSppg7SpqFhSgMlvyU/FiGLVEHDchJEdGQIBEaOBgTB/MA8G"
            + "A1UdEwEB/wQFMAMBAQAwDwYDVR0PAQH/BAUDAwegADAdBgNVHQ4EFgQUyxKILxFM"
            + "MNujjNnbeFpnPgB76UYwHwYDVR0jBBgwFoAUy5k/bKQ6TtpTWhsPWFzafOFgLmsw"
            + "GwYDVR0RBBQwEoEQMjUxMzQ3QGFuYXRvbS5zZTANBgkqhkiG9w0BAQUFAAOBgQAS"
            + "5wSOJhoVJSaEGHMPw6t3e+CbnEL9Yh5GlgxVAJCmIqhoScTMiov3QpDRHOZlZ15c"
            + "UlqugRBtORuA9xnLkrdxYNCHmX6aJTfjdIW61+o/ovP0yz6ulBkqcKzopAZLirX+"
            + "XSWf2uI9miNtxYMVnbQ1KPdEAt7Za3OQR6zcS0lGKg==")
            .getBytes());
    
    
    @Test(expected = CADoesntExistsException.class)
    public void shouldFailOnFindingCertificateAuthorityByName() throws Exception {

        // given
        String endEntityProfileName = "eep1";
        String certificateAuthorityName = "CA1";
        String certificateProfileName = "CP1";
        int certificateProfileId = 1;
        String subjectDn = "mydn=123";
        String name = "test123";
        int status = 20;
        String encodedValidity = "";
        int signedby = 1;

        X509Certificate mockX509Cert = EasyMock.mock(X509Certificate.class);

        X509Certificate[] certs = new X509Certificate[1];
        certs[0] = mockX509Cert;

        Collection<Certificate> certificatechain = new ArrayList<>();
        CAToken caToken = EasyMock.mock(CAToken.class);

        CAInfo caInfo = new X509CAInfo(subjectDn, name, status, certificateProfileId, encodedValidity, signedby, certificatechain, caToken);

        AuthenticationToken authenticationToken = EasyMock.mock(AuthenticationToken.class);
        
        expect(caSessionBean.getCAInfo(authenticationToken, certificateAuthorityName)).andReturn(null);
        replay(caSessionBean);
        EnrollPkcs10CertificateRequest request = new EnrollPkcs10CertificateRequest.Builder()
                .certificateRequest(csr)
                .certificateProfileName(certificateProfileName)
                .endEntityProfileName(endEntityProfileName)
                .certificateAuthorityName(certificateAuthorityName)
                .build();
        
        // when
        testClass.createCertificateRest(authenticationToken, request);

        // then
        EasyMock.verify();
    }

    @Test(expected = CertificateProfileDoesNotExistException.class)
    public void shouldFailOnFindingCertificateProfileByName() throws Exception {
        // given
        String endEntityProfileName = "eep1";
        String certificateAuthorityName = "CA1";
        String certificateProfileName = "CP1";
        int certificateProfileId = 1;
        String subjectDn = "mydn=123";
        String name = "test123";
        int status = 20;
        String encodedValidity = "";
        int signedby = 1;

        X509Certificate mockX509Cert = EasyMock.mock(X509Certificate.class);

        X509Certificate[] certs = new X509Certificate[1];
        certs[0] = mockX509Cert;

        Collection<Certificate> certificatechain = new ArrayList<>();
        CAToken caToken = EasyMock.mock(CAToken.class);

        CAInfo caInfo = new X509CAInfo(subjectDn, name, status, certificateProfileId, encodedValidity, signedby, certificatechain, caToken);

        AuthenticationToken authenticationToken = EasyMock.mock(AuthenticationToken.class);

        expect(caSessionBean.getCAInfo(authenticationToken, certificateAuthorityName)).andReturn(caInfo);
        expect(certificateProfileSessionBean.getCertificateProfileId(certificateProfileName)).andReturn(0);

        replay(caSessionBean);
        replay(certificateProfileSessionBean);

        EnrollPkcs10CertificateRequest request = new EnrollPkcs10CertificateRequest.Builder()
                .certificateRequest(csr)
                .certificateProfileName(certificateProfileName)
                .endEntityProfileName(endEntityProfileName)
                .certificateAuthorityName(certificateAuthorityName)
                .build();

        // when
        testClass.createCertificateRest(authenticationToken, request);

        // then
        EasyMock.verify();
    }


    @Test(expected = EndEntityProfileNotFoundException.class)
    public void shouldFailOnFindingEndEntityProfileByName() throws Exception {
        // given
        String endEntityProfileName = "eep1";
        String certificateAuthorityName = "CA1";
        String certificateProfileName = "CP1";
        int certificateProfileId = 1;
        String subjectDn = "mydn=123";
        String name = "test123";
        int status = 20;
        String encodedValidity = "";
        int signedby = 1;

        X509Certificate mockX509Cert = EasyMock.mock(X509Certificate.class);

        X509Certificate[] certs = new X509Certificate[1];
        certs[0] = mockX509Cert;

        Collection<Certificate> certificatechain = new ArrayList<>();
        CAToken caToken = EasyMock.mock(CAToken.class);

        CAInfo caInfo = new X509CAInfo(subjectDn, name, status, certificateProfileId, encodedValidity, signedby, certificatechain, caToken);

        AuthenticationToken authenticationToken = EasyMock.mock(AuthenticationToken.class);

        expect(caSessionBean.getCAInfo(authenticationToken, certificateAuthorityName)).andReturn(caInfo);
        expect(certificateProfileSessionBean.getCertificateProfileId(certificateProfileName)).andReturn(1);
        expect(endEntityProfileSessionBean.getEndEntityProfileId(endEntityProfileName)).andThrow(new EndEntityProfileNotFoundException());

        replay(caSessionBean);
        replay(certificateProfileSessionBean);
        replay(endEntityProfileSessionBean);

        EnrollPkcs10CertificateRequest request = new EnrollPkcs10CertificateRequest.Builder()
                .certificateRequest(csr)
                .certificateProfileName(certificateProfileName)
                .endEntityProfileName(endEntityProfileName)
                .certificateAuthorityName(certificateAuthorityName)
                .build();

        // when
        testClass.createCertificateRest(authenticationToken, request);

        // then
        EasyMock.verify();
    }



    @Test
    public void shouldFailOnAddingUserBecauseInputUsernameIsNull() throws Exception {
        // given
        String endEntityProfileName = "eep1";
        int endEntityProfileId = 7;

        String certificateAuthorityName = "CA1";
        String certificateProfileName = "CP1";
        int certificateProfileId = 9;
        String subjectDn = "mydn=123";
        String name = "test123";
        int status = 20;
        String encodedValidity = "";
        int signedby = 1;

        X509Certificate mockX509Cert = EasyMock.mock(X509Certificate.class);

        X509Certificate[] certs = new X509Certificate[1];
        certs[0] = mockX509Cert;

        Collection<Certificate> certificatechain = new ArrayList<>();
        CAToken caToken = EasyMock.mock(CAToken.class);

        CAInfo caInfo = new X509CAInfo(subjectDn, name, status, certificateProfileId, encodedValidity, signedby, certificatechain, caToken);

        EndEntityProfile endEntityProfile = mock(EndEntityProfile.class);

        AuthenticationToken authenticationToken = EasyMock.mock(AuthenticationToken.class);

        expect(caSessionBean.getCAInfo(authenticationToken, certificateAuthorityName)).andReturn(caInfo);
        expect(certificateProfileSessionBean.getCertificateProfileId(certificateProfileName)).andReturn(certificateProfileId);
        expect(endEntityProfileSessionBean.getEndEntityProfileId(endEntityProfileName)).andReturn(endEntityProfileId);
        expect(endEntityProfileSessionBean.getEndEntityProfile(endEntityProfileId)).andReturn(endEntityProfile);
        expect(endEntityProfile.useAutoGeneratedPasswd()).andReturn(false);
        expect(endEntityProfile.getValue(EndEntityProfile.SENDNOTIFICATION, 0)).andReturn(EndEntityProfile.TRUE);

        replay(caSessionBean);
        replay(certificateProfileSessionBean);
        replay(endEntityProfileSessionBean);
        replay(endEntityProfile);

        EnrollPkcs10CertificateRequest request = new EnrollPkcs10CertificateRequest.Builder()
                .certificateRequest(csr)
                .certificateProfileName(certificateProfileName)
                .endEntityProfileName(endEntityProfileName)
                .certificateAuthorityName(certificateAuthorityName)
                .build();

        // when
        try {
            testClass.createCertificateRest(authenticationToken, request);
            throw new Exception("This unit test is not ecpected to reach here!");
        } catch (EjbcaException e) {
            // then
            assertEquals("Problem with adding end entity with username null", e.getMessage());
            EasyMock.verify();
        }
    }


    @Test
    public void shouldEnrollCertificate() throws Exception {
        // given
        String endEntityProfileName = "eep1";
        int endEntityProfileId = 7;

        String certificateAuthorityName = "CA1";
        String certificateProfileName = "CP1";
        String username = "testuser";
        int certificateProfileId = 9;
        String subjectDn = "mydn=123";
        String name = "test123";
        int status = 20;
        String encodedValidity = "";
        int signedby = 1;

        X509Certificate mockX509Cert = EasyMock.mock(X509Certificate.class);

        X509Certificate[] certs = new X509Certificate[1];
        certs[0] = mockX509Cert;

        Collection<Certificate> certificatechain = new ArrayList<>();
        CAToken caToken = EasyMock.mock(CAToken.class);

        CAInfo caInfo = new X509CAInfo(subjectDn, name, status, certificateProfileId, encodedValidity, signedby, certificatechain, caToken);

        EndEntityProfile endEntityProfile = mock(EndEntityProfile.class);

        AuthenticationToken authenticationToken = EasyMock.mock(AuthenticationToken.class);

        expect(caSessionBean.getCAInfo(authenticationToken, certificateAuthorityName)).andReturn(caInfo);
        expect(certificateProfileSessionBean.getCertificateProfileId(certificateProfileName)).andReturn(certificateProfileId);
        expect(endEntityProfileSessionBean.getEndEntityProfileId(endEntityProfileName)).andReturn(endEntityProfileId);
        expect(endEntityProfileSessionBean.getEndEntityProfile(endEntityProfileId)).andReturn(endEntityProfile);
        expect(endEntityProfile.useAutoGeneratedPasswd()).andReturn(false);
        expect(endEntityProfile.getValue(EndEntityProfile.SENDNOTIFICATION, 0)).andReturn(EndEntityProfile.TRUE);
        expect(raMasterApiProxyBean.addUser((AuthenticationToken)EasyMock.anyObject(), (EndEntityInformation)EasyMock.anyObject(), EasyMock.anyBoolean()))
                .andReturn(true);

        expect(raMasterApiProxyBean.createCertificate((AuthenticationToken)EasyMock.anyObject(), (EndEntityInformation)EasyMock.anyObject()))
                .andReturn(testCertificateBytes);

        replay(caSessionBean);
        replay(certificateProfileSessionBean);
        replay(endEntityProfileSessionBean);
        replay(endEntityProfile);
        replay(raMasterApiProxyBean);

        EnrollPkcs10CertificateRequest request = new EnrollPkcs10CertificateRequest.Builder()
                .certificateRequest(csr)
                .certificateProfileName(certificateProfileName)
                .endEntityProfileName(endEntityProfileName)
                .certificateAuthorityName(certificateAuthorityName)
                .username(username)
                .build();

        // when
        byte[] result = testClass.createCertificateRest(authenticationToken, request);

        // then
        assertEquals(testCertificateBytes, result);
        EasyMock.verify();
    }

    @Test
    public void shouldParseCorrectDn() {
        PKCS10CertificationRequest pkcs10CertificateRequest = CertTools.getCertificateRequestFromPem(csr);
        String actualResult = testClass.getSubjectDn(pkcs10CertificateRequest);
        String expectedResult = "C=EE,ST=Alabama,L=tallinn,O=naabrivalve,CN=hello123server6";
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void shouldParseCorrectAn() {
        PKCS10CertificationRequest pkcs10CertificateRequest = CertTools.getCertificateRequestFromPem(csr);
        String actualResult = testClass.getSubjectAltName(pkcs10CertificateRequest);
        String expectedResult = "dNSName=somedns.com, iPAddress=192.168.1.7, dNSName=some.other.dns.com, directoryName=CN=Test\\,L=XX";
        assertEquals(expectedResult, actualResult);
    }
}
