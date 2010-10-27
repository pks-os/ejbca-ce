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

package org.ejbca.core.ejb.ca.sign;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;

import javax.ejb.EJBException;
import javax.persistence.PersistenceException;

import org.apache.log4j.Logger;
import org.cesecore.core.ejb.authorization.AdminGroupSessionRemote;
import org.ejbca.core.ejb.ca.CaTestCase;
import org.ejbca.core.ejb.ra.UserAdminSessionRemote;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.ca.caadmin.CAInfo;
import org.ejbca.core.model.log.Admin;
import org.ejbca.core.model.ra.UserDataConstants;
import org.ejbca.util.CryptoProviderTools;
import org.ejbca.util.InterfaceCache;
import org.ejbca.util.keystore.KeyTools;

/** This is a performance test:
 * - 10 threads generates 1000 certificates each with 1024 bit public key
 * - CA uses 2048 bit signature key
 * - total time for certificate generation is counted to get number of certificates generated per second
 * 
 * @version $Id$
 */
public class SignLotsOfCertsTest extends CaTestCase {

    public static final String DEFAULT_SUPERADMIN_CN = "SuperAdmin";
    
	private static final Logger log = Logger.getLogger(SignLotsOfCertsTest.class);

    private static final String CANAME = "TESTPERF1";
    private int caid = getTestCAId(CANAME);
    private static final Admin admin = new Admin(Admin.TYPE_BATCHCOMMANDLINE_USER);

    public static KeyPair keys;
    
    private AdminGroupSessionRemote adminGroupSession = InterfaceCache.getAdminGroupSession();
    private SignSessionRemote signSession = InterfaceCache.getSignSession();
    private UserAdminSessionRemote userAdminSession = InterfaceCache.getUserAdminSession();

    /**
     * Creates a new TestSignSession object.
     *
     * @param name name
     */
    public SignLotsOfCertsTest(String name) {
        super(name);
        CryptoProviderTools.installBCProvider();	// Install BouncyCastle provider
    }

    public void setUp() throws Exception {
        log.trace(">setUp()");
        if (keys == null) {
            keys = KeyTools.genKeys("1024", "RSA");
        }
        log.trace("<setUp()");
    }

    public void tearDown() throws Exception {
    }

    private void newUser(String post) throws Exception {
        // Make user that we know...
        boolean userExists = false;
        try {
            userAdminSession.addUser(admin, "performancefoo" + post, "foo123", "C=SE,O=AnaTom,OU=Performance Test,CN=performancefoo", null,
                    "performancefoo@foo.se", false, SecConst.EMPTY_ENDENTITYPROFILE, SecConst.CERTPROFILE_FIXED_ENDUSER, SecConst.USER_ENDUSER,
                    SecConst.TOKEN_SOFT_PEM, 0, caid);
            log.debug("created user: performancefoo" + post + ", foo123, C=SE, O=AnaTom, OU=Performance Test,CN=performancefoo");
        } catch (EJBException e) {
            if (e.getCause() instanceof PersistenceException) {
                userExists = true;
            }
        }
        if (userExists) {
            log.info("User performancefoo already exists, resetting status.");
            userAdminSession.setUserStatus(admin, "performancefoo" + post, UserDataConstants.STATUS_NEW);
            log.debug("Reset status to NEW");
        }

    }

    private void deleteUser(String post) throws Exception {
        try {
            userAdminSession.deleteUser(admin, "performancefoo" + post);
            log.debug("deleted user: performancefoo" + post);
        } catch (EJBException e) {
        	log.debug("Delete operation failed.. User probably did not exist, which is fine so do nothing." + e.getMessage());
        }
    }

    public void test00AddRSACA() throws Exception {
        adminGroupSession.init(admin, getTestCAId(CANAME), DEFAULT_SUPERADMIN_CN);
        createTestCA(CANAME, 2048);
        CAInfo info = caAdminSessionRemote.getCAInfo(admin, CANAME);
        X509Certificate cert = (X509Certificate) info.getCertificateChain().iterator().next();
        assertTrue("Error in created ca certificate", cert.getSubjectDN().toString().equals("CN=" + CANAME));
        assertTrue("Creating CA failed", info.getSubjectDN().equals("CN=" + CANAME));
        PublicKey pk = cert.getPublicKey();
        if (pk instanceof RSAPublicKey) {
            RSAPublicKey rsapk = (RSAPublicKey) pk;
            assertEquals(rsapk.getAlgorithm(), "RSA");
        } else {
            assertTrue("Public key is not EC", false);
        }
        assertTrue("CA is not valid for the specified duration.", cert.getNotAfter().after(new Date(new Date().getTime() + 10 * 364 * 24 * 60 * 60 * 1000L))
                && cert.getNotAfter().before(new Date(new Date().getTime() + 10 * 366 * 24 * 60 * 60 * 1000L)));
    }

    /**
     * creates new user
     * 
     * @throws Exception
     *             if an error occurs...
     */
    public void test01CreateNewUser() throws Exception {
        log.trace(">test01CreateNewUser()");
        newUser("no1");
        newUser("no2");
        newUser("no3");
        newUser("no4");
        newUser("no5");
        newUser("no6");
        newUser("no7");
        newUser("no8");
        newUser("no9");
        newUser("no10");
        log.trace("<test01CreateNewUser()");
    }

    /**
     * creates cert
     * 
     * @throws Exception
     *             if en error occurs...
     */
    public void test03SignLotsOfCerts() throws Exception {
        log.trace(">test03SignLotsOfCerts()");

        long before = System.currentTimeMillis();
        Thread no1 = new Thread(new SignTester(), "no1"); // NOPMD we want to use thread here, it's not a JEE app
        Thread no2 = new Thread(new SignTester(), "no2"); // NOPMD we want to use thread here, it's not a JEE app
        Thread no3 = new Thread(new SignTester(), "no3"); // NOPMD we want to use thread here, it's not a JEE app
        Thread no4 = new Thread(new SignTester(), "no4"); // NOPMD we want to use thread here, it's not a JEE app
        Thread no5 = new Thread(new SignTester(), "no5"); // NOPMD we want to use thread here, it's not a JEE app
        Thread no6 = new Thread(new SignTester(), "no6"); // NOPMD we want to use thread here, it's not a JEE app
        Thread no7 = new Thread(new SignTester(), "no7"); // NOPMD we want to use thread here, it's not a JEE app
        Thread no8 = new Thread(new SignTester(), "no8"); // NOPMD we want to use thread here, it's not a JEE app
        Thread no9 = new Thread(new SignTester(), "no9"); // NOPMD we want to use thread here, it's not a JEE app
        Thread no10 = new Thread(new SignTester(), "no10"); // NOPMD we want to use thread here, it's not a JEE app
        no1.start();
        log.info("Started no1");
        no2.start();
        log.info("Started no2");
        no3.start();
        log.info("Started no3");
        no4.start();
        log.info("Started no4");
        no5.start();
        log.info("Started no5");
        no6.start();
        log.info("Started no6");
        no7.start();
        log.info("Started no7");
        no8.start();
        log.info("Started no8");
        no9.start();
        log.info("Started no9");
        no10.start();
        log.info("Started no10");
        no1.join();
        no2.join();
        no3.join();
        no4.join();
        no5.join();
        no6.join();
        no7.join();
        no8.join();
        no9.join();
        no10.join();
        long after = System.currentTimeMillis();
        long diff = after - before;
        log.info("All threads finished. Total time: " + diff + " ms");
        int noOfGeneratedCerts = (10 * SignTester.NO_CERTS);
        log.info("Generated " + noOfGeneratedCerts + " certificates in total.");
        BigDecimal d = new BigDecimal(diff).divide(new BigDecimal(1000));
        BigDecimal noCerts = new BigDecimal(noOfGeneratedCerts).divide(d, 2, RoundingMode.UP);
        log.info("Performance is " + noCerts.intValue() + " certs/sec.");
        // FileOutputStream fos = new FileOutputStream("testcert.crt");
        // fos.write(cert.getEncoded());
        // fos.close();
        log.trace("<test03SignLotsOfCerts()");
    }

    public void testZZZCleanUp() throws Exception {
        removeTestCA(CANAME);
        deleteUser("no1");
        deleteUser("no2");
        deleteUser("no3");
        deleteUser("no4");
        deleteUser("no5");
        deleteUser("no6");
        deleteUser("no7");
        deleteUser("no8");
        deleteUser("no9");
        deleteUser("no10");
    }

    private class SignTester implements Runnable { // NOPMD we want to use thread here, it's not a JEE app

        public static final int NO_CERTS = 1000;

        public void run() {
            try {
                String user = "performancefoo" + Thread.currentThread().getName();
                long before = System.currentTimeMillis();
                for (int i = 0; i < NO_CERTS; i++) {
                    // user that we know exists...
                    X509Certificate cert = (X509Certificate) signSession.createCertificate(admin, user, "foo123", keys.getPublic());
                    assertNotNull("Failed to create certificate", cert);
                    if ((i % 100) == 0) {
                        long mellantid = System.currentTimeMillis() - before;
                        log.info(Thread.currentThread().getName() + " has generated " + i + ", time=" + mellantid);

                    }
                }
                long after = System.currentTimeMillis();
                long diff = after - before;
                log.info("Time used (" + Thread.currentThread().getName() + "): " + diff);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
