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

package org.ejbca.ui.web.pub;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.rmi.RemoteException;
import java.rmi.ServerException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;

import javax.ejb.EJBException;
import javax.persistence.PersistenceException;

import org.apache.log4j.Logger;
import org.apache.xml.security.utils.Base64;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authentication.tokens.UsernamePrincipal;
import org.cesecore.certificates.certificateprofile.CertificateProfileConstants;
import org.cesecore.certificates.endentity.EndEntityConstants;
import org.cesecore.certificates.endentity.EndEntityInformation;
import org.cesecore.certificates.endentity.EndEntityTypes;
import org.cesecore.keys.util.KeyTools;
import org.cesecore.mock.authentication.tokens.TestAlwaysAllowLocalAuthenticationToken;
import org.cesecore.util.CertTools;
import org.cesecore.util.CryptoProviderTools;
import org.cesecore.util.EjbRemoteHelper;
import org.ejbca.config.WebConfiguration;
import org.ejbca.core.ejb.ca.CaTestCase;
import org.ejbca.core.ejb.config.ConfigurationSessionRemote;
import org.ejbca.core.ejb.ra.EndEntityManagementSessionRemote;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.ra.NotFoundException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests http servlet for certificate request
 * 
 * @version $Id$
 */
public class CertRequestHttpTest extends CaTestCase {
    private static Logger log = Logger.getLogger(CertRequestHttpTest.class);

    private String httpReqPath;
    private String resourceReq;

    private int caid = getTestCAId();
    private static final AuthenticationToken admin = new TestAlwaysAllowLocalAuthenticationToken(new UsernamePrincipal("CertRequestHttpTest"));

    private ConfigurationSessionRemote configurationSession = EjbRemoteHelper.INSTANCE.getRemoteSession(ConfigurationSessionRemote.class, EjbRemoteHelper.MODULE_TEST);
    private EndEntityManagementSessionRemote userAdminSession = EjbRemoteHelper.INSTANCE.getRemoteSession(EndEntityManagementSessionRemote.class);

    @BeforeClass
    public static void beforeClass() {    
        // Install BouncyCastle provider
        CryptoProviderTools.installBCProvider();
        
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        httpReqPath = "http://127.0.0.1:" + configurationSession.getProperty(WebConfiguration.CONFIG_HTTPSERVERPUBHTTP) + "/ejbca";
        resourceReq = "certreq";
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    	try {
    		userAdminSession.deleteUser(admin, "reqtest");
    	} catch (NotFoundException e) {
    		// NOPMD:ignore if the user was not created
    	}
    }

    /**
     * Tests request for a pkcs12
     * 
     * @throws Exception error
     */
    @Test
    public void test01RequestPKCS12() throws Exception {
        log.trace(">test01RequestPKCS12()");

        // find a CA (TestCA?) create a user
        // Send certificate request for a server generated PKCS12
        setupUser(SecConst.TOKEN_SOFT_P12);

        // POST the OCSP request
        URL url = new URL(httpReqPath + '/' + resourceReq);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        // we are going to do a POST
        con.setDoOutput(true);
        con.setRequestMethod("POST");

        // POST it
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        OutputStream os = con.getOutputStream();
        os.write("user=reqtest&password=foo123&keylength=2048".getBytes("UTF-8"));
        os.close();
        assertEquals("Response code", 200, con.getResponseCode());
        // Some appserver (Weblogic) responds with
        // "application/x-pkcs12; charset=UTF-8"
        String contentType = con.getContentType();
        boolean contentTypeIsPkcs12 = contentType.startsWith("application/x-pkcs12");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // This works for small requests, and PKCS12 requests are small
        InputStream in = con.getInputStream();
        int b = in.read();
        while (b != -1) {
            baos.write(b);
            b = in.read();
        }
        baos.flush();
        in.close();
        byte[] respBytes = baos.toByteArray();
        assertTrue(respBytes.length > 0);
        if (!contentTypeIsPkcs12 && log.isDebugEnabled()) {
            // If the content-type isn't application/x-pkcs12 we like to know what we got back..
            log.debug(new String(respBytes));
        }
        assertTrue("contentType was " + contentType, contentTypeIsPkcs12);

        KeyStore store = KeyStore.getInstance("PKCS12", "BC");
        ByteArrayInputStream is = new ByteArrayInputStream(respBytes);
        store.load(is, "foo123".toCharArray());
        assertTrue(store.containsAlias("ReqTest"));
        X509Certificate cert = (X509Certificate) store.getCertificate("ReqTest");
        PublicKey pk = cert.getPublicKey();
        if (pk instanceof RSAPublicKey) {
            RSAPublicKey rsapk = (RSAPublicKey) pk;
            assertEquals(rsapk.getAlgorithm(), "RSA");
            assertEquals(2048, rsapk.getModulus().bitLength());
        } else {
            assertTrue("Public key is not RSA", false);
        }

        log.trace("<test01RequestPKCS12()");
    }

    /**
     * Tests request for a unknown user
     * 
     * @throws Exception error
     */
    @Test
    public void test02RequestUnknownUser() throws Exception {
        log.trace(">test02RequestUnknownUser()");

        // POST the OCSP request
        URL url = new URL(httpReqPath + '/' + resourceReq);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        // we are going to do a POST
        con.setDoOutput(true);
        con.setRequestMethod("POST");
        con.setInstanceFollowRedirects(false);
        con.setAllowUserInteraction(false);

        // POST it
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        OutputStream os = con.getOutputStream();
        os.write("user=reqtestunknown&password=foo123&keylength=2048".getBytes("UTF-8"));
        os.close();
        final int responseCode = con.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            log.info("ResponseMessage: " + con.getResponseMessage());
            assertEquals("Response code", HttpURLConnection.HTTP_OK, responseCode);
        }
        log.info("Content-Type: " + con.getContentType());
        boolean ok = false;
        // Some containers return the content type with a space and some
        // without...
        if ("text/html;charset=UTF-8".equals(con.getContentType())) {
            ok = true;
        }
        if ("text/html; charset=UTF-8".equals(con.getContentType())) {
            ok = true;
        }
        assertTrue(con.getContentType(), ok);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // This works for small requests, and PKCS12 requests are small
        InputStream in = con.getInputStream();
        int b = in.read();
        while (b != -1) {
            baos.write(b);
            b = in.read();
        }
        baos.flush();
        in.close();
        byte[] respBytes = baos.toByteArray();
        String error = new String(respBytes);
        int index = error.indexOf("<pre>");
        int index2 = error.indexOf("</pre>");
        String errormsg = error.substring(index + 5, index2);
        log.info(errormsg);
        String expectedErrormsg = "Username: reqtestunknown\nNon existent username. To generate a certificate a valid username and password must be supplied.\n";
        assertEquals(expectedErrormsg.replaceAll("\\s", ""), errormsg.replaceAll("\\s", ""));
        log.trace("<test02RequestUnknownUser()");
    }

    /**
     * Tests request for a wrong password
     * 
     * @throws Exception error
     */
    @Test
    public void test03RequestWrongPwd() throws Exception {
        log.trace(">test03RequestWrongPwd()");

        setupUser(SecConst.TOKEN_SOFT_P12);

        // POST the OCSP request
        URL url = new URL(httpReqPath + '/' + resourceReq);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        // we are going to do a POST
        con.setDoOutput(true);
        con.setRequestMethod("POST");
        con.setInstanceFollowRedirects(false);
        con.setAllowUserInteraction(false);

        // POST it
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        OutputStream os = con.getOutputStream();
        os.write("user=reqtest&password=foo456&keylength=2048".getBytes("UTF-8"));
        os.close();
        final int responseCode = con.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            log.info("ResponseMessage: " + con.getResponseMessage());
            assertEquals("Response code", HttpURLConnection.HTTP_OK, responseCode);
        }
        boolean ok = false;
        // Some containers return the content type with a space and some
        // without...
        if ("text/html;charset=UTF-8".equals(con.getContentType())) {
            ok = true;
        }
        if ("text/html; charset=UTF-8".equals(con.getContentType())) {
            ok = true;
        }
        assertTrue(con.getContentType(), ok);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // This works for small requests, and PKCS12 requests are small
        InputStream in = con.getInputStream();
        int b = in.read();
        while (b != -1) {
            baos.write(b);
            b = in.read();
        }
        baos.flush();
        in.close();
        byte[] respBytes = baos.toByteArray();
        String error = new String(respBytes);
        int index = error.indexOf("<pre>");
        int index2 = error.indexOf("</pre>");
        String errormsg = error.substring(index + 5, index2);
        String expectedErrormsg = "Username: reqtest\nWrong username or password! To generate a certificate a valid username and password must be supplied.\n";
        assertEquals(expectedErrormsg.replaceAll("\\s", ""), errormsg.replaceAll("\\s", ""));
        log.info(errormsg);
        log.trace("<test03RequestWrongPwd()");
    }

    /**
     * Tests request with wrong status
     * 
     * @throws Exception error
     */
    @Test
    public void test04RequestWrongStatus() throws Exception {
        log.trace(">test04RequestWrongStatus()");

        setupUser(SecConst.TOKEN_SOFT_P12);
        setupUserStatus(EndEntityConstants.STATUS_GENERATED);

        // POST the OCSP request
        URL url = new URL(httpReqPath + '/' + resourceReq);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        // we are going to do a POST
        con.setDoOutput(true);
        con.setRequestMethod("POST");
        con.setInstanceFollowRedirects(false);
        con.setAllowUserInteraction(false);

        // POST it
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        OutputStream os = con.getOutputStream();
        os.write("user=reqtest&password=foo456&keylength=2048".getBytes("UTF-8"));
        os.close();
        final int responseCode = con.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            log.info("ResponseMessage: " + con.getResponseMessage());
            assertEquals("Response code", HttpURLConnection.HTTP_OK, responseCode);
        }
        boolean ok = false;
        // Some containers return the content type with a space and some
        // without...
        if ("text/html;charset=UTF-8".equals(con.getContentType())) {
            ok = true;
        }
        if ("text/html; charset=UTF-8".equals(con.getContentType())) {
            ok = true;
        }
        assertTrue(con.getContentType(), ok);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // This works for small requests, and PKCS12 requests are small
        InputStream in = con.getInputStream();
        int b = in.read();
        while (b != -1) {
            baos.write(b);
            b = in.read();
        }
        baos.flush();
        in.close();
        byte[] respBytes = baos.toByteArray();
        String error = new String(respBytes);
        int index = error.indexOf("<pre>");
        int index2 = error.indexOf("</pre>");
        String errormsg = error.substring(index + 5, index2);
        String expectedErrormsg = "Username: reqtest\nWrong user status! To generate a certificate for a user the user must have status New, Failed or In process.\n";

        assertEquals(expectedErrormsg.replaceAll("\\s", ""), errormsg.replaceAll("\\s", ""));
        log.info(errormsg);
        log.trace("<test04RequestWrongStatus()");
    }

    @Test
    public void test05RequestIE() throws Exception {
        // find a CA (TestCA?) create a user
        // Send certificate request for a server generated PKCS12
        setupUser(SecConst.TOKEN_SOFT_BROWSERGEN);

        // Create a PKCS10 request
        KeyPair rsakeys = KeyTools.genKeys("512", "RSA");
        PKCS10CertificationRequest req = new PKCS10CertificationRequest("SHA1WithRSA", CertTools.stringToBcX509Name("C=SE, O=AnaTom, CN=foo"),
                rsakeys.getPublic(), new DERSet(), rsakeys.getPrivate());
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        DEROutputStream dOut = new DEROutputStream(bOut);
        dOut.writeObject(req);
        dOut.close();
        String p10 = new String(Base64.encode(bOut.toByteArray()));
        // System.out.println(p10);

        // POST the OCSP request
        URL url = new URL(httpReqPath + '/' + resourceReq);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        // we are going to do a POST
        con.setDoOutput(true);
        con.setRequestMethod("POST");

        // POST it
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        OutputStream os = con.getOutputStream();
        final StringBuilder buf = new StringBuilder("user=reqtest&password=foo123&pkcs10=");
        buf.append(URLEncoder.encode(p10, "UTF-8"));
        os.write(buf.toString().getBytes("UTF-8"));
        os.close();
        assertEquals("Response code", 200, con.getResponseCode());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // This works for small requests, and PKCS7 responses are small
        InputStream in = con.getInputStream();
        int b = in.read();
        while (b != -1) {
            baos.write(b);
            b = in.read();
        }
        baos.flush();
        in.close();
        byte[] respBytes = baos.toByteArray();
        assertTrue(respBytes.length > 0);

        String resp = new String(respBytes);
        // This is a string with VB script and all
        // System.out.println(resp);
        assertTrue("Response does not contain 'cert ='", resp.contains("cert ="));
    }


    //
    // Private helper methods
    //

    private void setupUser(int tokentype) throws Exception {
        // Make user that we know...
        boolean userExists = false;
        try {
            userAdminSession.addUser(admin, "reqtest", "foo123", "C=SE,O=PrimeKey,CN=ReqTest", null, "reqtest@primekey.se", false,
                    SecConst.EMPTY_ENDENTITYPROFILE, CertificateProfileConstants.CERTPROFILE_FIXED_ENDUSER, EndEntityTypes.ENDUSER.toEndEntityType(), tokentype, 0, caid);
            log.debug("created user: reqtest, foo123, C=SE, O=PrimeKey, CN=ReqTest");
        } catch (EJBException ejbException) {
            // On Glassfish, ejbException.getCause() returns null, getCausedByException() should be used.
            Exception e = ejbException.getCausedByException();
            log.debug("Exception cause thrown: " + e.getClass().getName() + " message: " + e.getMessage());
            if (e instanceof PersistenceException) {
                userExists = true; // This is what we want
            } else if (e instanceof ServerException) {
                // Glassfish 2 throws EJBException(java.rmi.ServerException(java.rmi.RemoteException(javax.persistence.EntityExistsException)))), can
                // you believe this?
                Throwable t = e.getCause();
                if (t != null && t instanceof RemoteException) {
                    t = t.getCause();
                    log.debug("Exception cause thrown: " + t.getClass().getName() + " message: " + t.getMessage());
                    if (t != null && t instanceof PersistenceException) {
                        userExists = true; // This is what we want
                    }
                }
            }
        }
        if (userExists) {
            log.debug("User reqtest already exists.");
            EndEntityInformation endEntityInformation = new EndEntityInformation("reqtest", "C=SE,O=PrimeKey,CN=ReqTest", caid, null, "reqtest@anatom.se", EndEntityTypes.ENDUSER.toEndEntityType(), 
                    SecConst.EMPTY_ENDENTITYPROFILE, CertificateProfileConstants.CERTPROFILE_FIXED_ENDUSER, tokentype, 0, null);
            endEntityInformation.setPassword("foo123");
            endEntityInformation.setStatus(EndEntityConstants.STATUS_NEW);
            userAdminSession.changeUser(admin, endEntityInformation, false);
            log.debug("Reset status to NEW");
        }
    }

    private void setupUserStatus(int status) throws Exception {
        EndEntityInformation endEntityInformation = new EndEntityInformation("reqtest", "C=SE,O=PrimeKey,CN=ReqTest", caid, null, "reqtest@anatom.se", EndEntityTypes.ENDUSER.toEndEntityType(), 
                SecConst.EMPTY_ENDENTITYPROFILE, CertificateProfileConstants.CERTPROFILE_FIXED_ENDUSER, SecConst.TOKEN_SOFT_P12, 0, null);
        endEntityInformation.setPassword("foo123");
        endEntityInformation.setStatus(status);
        userAdminSession.changeUser(admin, endEntityInformation, false);
        log.debug("Set status to: " + status);
    }

    public String getRoleName() {
        return this.getClass().getSimpleName(); 
    }
}
