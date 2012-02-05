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

package org.ejbca.core.ejb.hardtoken;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Date;

import org.apache.log4j.Logger;
import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authentication.tokens.UsernamePrincipal;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.certificates.certificate.CertificateConstants;
import org.cesecore.certificates.certificate.CertificateStoreSessionRemote;
import org.cesecore.certificates.certificateprofile.CertificateProfileConstants;
import org.cesecore.jndi.JndiHelper;
import org.cesecore.mock.authentication.tokens.TestAlwaysAllowLocalAuthenticationToken;
import org.cesecore.util.Base64;
import org.cesecore.util.CertTools;
import org.cesecore.util.CryptoProviderTools;
import org.cesecore.util.EjbRemoteHelper;
import org.ejbca.config.GlobalConfiguration;
import org.ejbca.core.ejb.ca.CaTestCase;
import org.ejbca.core.ejb.config.GlobalConfigurationProxySessionRemote;
import org.ejbca.core.ejb.config.GlobalConfigurationSessionRemote;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.hardtoken.HardTokenData;
import org.ejbca.core.model.hardtoken.HardTokenDoesntExistsException;
import org.ejbca.core.model.hardtoken.types.SwedishEIDHardToken;
import org.ejbca.core.model.hardtoken.types.TurkishEIDHardToken;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the hard token related entity beans.
 * 
 * @version $Id$
 */
public class HardTokenTest extends CaTestCase {
    private static final Logger log = Logger.getLogger(HardTokenTest.class);
    private static final AuthenticationToken internalAdmin = new TestAlwaysAllowLocalAuthenticationToken(new UsernamePrincipal("HardTokenTest"));

    private static int orgEncryptCAId;

    static byte[] testcert = Base64.decode(("MIICWzCCAcSgAwIBAgIIJND6Haa3NoAwDQYJKoZIhvcNAQEFBQAwLzEPMA0GA1UE"
            + "AxMGVGVzdENBMQ8wDQYDVQQKEwZBbmFUb20xCzAJBgNVBAYTAlNFMB4XDTAyMDEw" + "ODA5MTE1MloXDTA0MDEwODA5MjE1MlowLzEPMA0GA1UEAxMGMjUxMzQ3MQ8wDQYD"
            + "VQQKEwZBbmFUb20xCzAJBgNVBAYTAlNFMIGdMA0GCSqGSIb3DQEBAQUAA4GLADCB" + "hwKBgQCQ3UA+nIHECJ79S5VwI8WFLJbAByAnn1k/JEX2/a0nsc2/K3GYzHFItPjy"
            + "Bv5zUccPLbRmkdMlCD1rOcgcR9mmmjMQrbWbWp+iRg0WyCktWb/wUS8uNNuGQYQe" + "ACl11SAHFX+u9JUUfSppg7SpqFhSgMlvyU/FiGLVEHDchJEdGQIBEaOBgTB/MA8G"
            + "A1UdEwEB/wQFMAMBAQAwDwYDVR0PAQH/BAUDAwegADAdBgNVHQ4EFgQUyxKILxFM" + "MNujjNnbeFpnPgB76UYwHwYDVR0jBBgwFoAUy5k/bKQ6TtpTWhsPWFzafOFgLmsw"
            + "GwYDVR0RBBQwEoEQMjUxMzQ3QGFuYXRvbS5zZTANBgkqhkiG9w0BAQUFAAOBgQAS" + "5wSOJhoVJSaEGHMPw6t3e+CbnEL9Yh5GlgxVAJCmIqhoScTMiov3QpDRHOZlZ15c"
            + "UlqugRBtORuA9xnLkrdxYNCHmX6aJTfjdIW61+o/ovP0yz6ulBkqcKzopAZLirX+" + "XSWf2uI9miNtxYMVnbQ1KPdEAt7Za3OQR6zcS0lGKg==").getBytes());

    private CertificateStoreSessionRemote certificateStoreSession = EjbRemoteHelper.INSTANCE.getRemoteSession(CertificateStoreSessionRemote.class);
    private HardTokenSessionRemote hardTokenSessionRemote = EjbRemoteHelper.INSTANCE.getRemoteSession(HardTokenSessionRemote.class);
    private GlobalConfigurationSessionRemote globalConfigurationSession = EjbRemoteHelper.INSTANCE.getRemoteSession(GlobalConfigurationSessionRemote.class);
    private GlobalConfigurationProxySessionRemote globalConfigurationProxySession = JndiHelper.getRemoteSession(GlobalConfigurationProxySessionRemote.class);


    @BeforeClass
    public static void beforeClass() {       
        CryptoProviderTools.installBCProvider();
        
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();

    }
    
    public String getRoleName() {
        return this.getClass().getSimpleName(); 
    }

    @Test
    public void test01AddHardToken() throws Exception {
        log.trace(">test01AddHardToken()");

        GlobalConfiguration gc = globalConfigurationSession.getCachedGlobalConfiguration();
        orgEncryptCAId = gc.getHardTokenEncryptCA();
        gc.setHardTokenEncryptCA(0);
        globalConfigurationProxySession.saveGlobalConfigurationRemote(internalAdmin, gc);

        SwedishEIDHardToken token = new SwedishEIDHardToken("1234", "1234", "123456", "123456", 1);

        ArrayList<Certificate> certs = new ArrayList<Certificate>();

        certs.add(CertTools.getCertfromByteArray(testcert));

        hardTokenSessionRemote.addHardToken(internalAdmin, "1234", "TESTUSER", "CN=TEST", SecConst.TOKEN_SWEDISHEID, token, certs, null);

        TurkishEIDHardToken token2 = new TurkishEIDHardToken("1234", "123456", 1);

        hardTokenSessionRemote.addHardToken(internalAdmin, "2345", "TESTUSER", "CN=TEST", SecConst.TOKEN_TURKISHEID, token2, certs, null);

        log.trace("<test01AddHardToken()");
    }

    @Test
    public void test02EditHardToken() throws Exception {
        log.trace(">test02EditHardToken()");

        boolean ret = false;

        HardTokenData token = hardTokenSessionRemote.getHardToken(internalAdmin, "1234", true);

        SwedishEIDHardToken swe = (SwedishEIDHardToken) token.getHardToken();

        assertTrue("Retrieving HardToken failed", swe.getInitialAuthEncPIN().equals("1234"));

        swe.setInitialAuthEncPIN("5678");

        hardTokenSessionRemote.changeHardToken(internalAdmin, "1234", SecConst.TOKEN_SWEDISHEID, token.getHardToken());
        ret = true;

        assertTrue("Editing HardToken failed", ret);
        log.trace("<test02EditHardToken()");
    }

    @Test
    public void test03FindHardTokenByCertificate() throws Exception {
        log.trace(">test03FindHardTokenByCertificate()");

        Certificate cert = CertTools.getCertfromByteArray(testcert);
        // Store the dummy cert for test.
        if (certificateStoreSession.findCertificateByFingerprint(CertTools.getFingerprintAsString(cert)) == null) {
            certificateStoreSession.storeCertificate(internalAdmin, cert, "DUMMYUSER",
                    CertTools.getFingerprintAsString(cert), CertificateConstants.CERT_ACTIVE,
                    CertificateConstants.CERTTYPE_ENDENTITY, CertificateProfileConstants.CERTPROFILE_FIXED_ENDUSER,
                    null, new Date().getTime());
        }
        String tokensn = hardTokenSessionRemote.findHardTokenByCertificateSNIssuerDN(internalAdmin, CertTools.getSerialNumber(cert), CertTools.getIssuerDN(cert));

        assertTrue("Couldn't find right hardtokensn", tokensn.equals("1234"));

        log.trace("<test03FindHardTokenByCertificate()");
    }

    @Test
    public void test04EncryptHardToken() throws Exception {
        log.trace(">test04EncryptHardToken()");

        GlobalConfiguration gc = globalConfigurationSession.getCachedGlobalConfiguration();
        gc.setHardTokenEncryptCA(getTestCAId());
        globalConfigurationProxySession.saveGlobalConfigurationRemote(internalAdmin, gc);
        boolean ret = false;

        // Make sure the old data can be read
        HardTokenData token = hardTokenSessionRemote.getHardToken(internalAdmin, "1234", true);

        SwedishEIDHardToken swe = (SwedishEIDHardToken) token.getHardToken();

        assertTrue("Retrieving HardToken failed : " + swe.getInitialAuthEncPIN(), swe.getInitialAuthEncPIN().equals("5678"));

        swe.setInitialAuthEncPIN("5678");

        // Store the new data as encrypted
        hardTokenSessionRemote.changeHardToken(internalAdmin, "1234", SecConst.TOKEN_SWEDISHEID, token.getHardToken());
        ret = true;

        assertTrue("Saving encrypted HardToken failed", ret);

        // Make sure the encrypted data can be read
        token = hardTokenSessionRemote.getHardToken(internalAdmin, "1234", true);

        swe = (SwedishEIDHardToken) token.getHardToken();

        assertTrue("Retrieving encrypted HardToken failed", swe.getInitialAuthEncPIN().equals("5678"));

        log.trace("<test04EncryptHardToken()");
    }

    @Test
    public void test05removeHardTokens() throws AuthorizationDeniedException {
        GlobalConfiguration gc = globalConfigurationSession.getCachedGlobalConfiguration();
        gc.setHardTokenEncryptCA(orgEncryptCAId);
        globalConfigurationProxySession.saveGlobalConfigurationRemote(internalAdmin, gc);
    
        try {
            hardTokenSessionRemote.removeHardToken(internalAdmin, "1234");
            hardTokenSessionRemote.removeHardToken(internalAdmin, "2345");
        } catch (HardTokenDoesntExistsException e) {
            e.printStackTrace();        
        }
        
        assertFalse("Removing hard token with tokensn 1234 failed.", hardTokenSessionRemote.existsHardToken(internalAdmin, "1234"));
        assertFalse("Removing hard token with tokensn 2345 failed.", hardTokenSessionRemote.existsHardToken(internalAdmin, "2345"));
    }

}
