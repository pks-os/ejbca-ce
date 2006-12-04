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

package se.anatom.ejbca.ca.sign;

import java.io.ByteArrayOutputStream;
import java.rmi.RemoteException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import javax.ejb.DuplicateKeyException;
import javax.naming.Context;
import javax.naming.NamingException;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.x509.qualified.ETSIQCObjectIdentifiers;
import org.bouncycastle.asn1.x509.qualified.RFC3739QCObjectIdentifiers;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.provider.JCEECPublicKey;
import org.ejbca.core.ejb.ca.caadmin.ICAAdminSessionHome;
import org.ejbca.core.ejb.ca.caadmin.ICAAdminSessionRemote;
import org.ejbca.core.ejb.ca.sign.ISignSessionHome;
import org.ejbca.core.ejb.ca.sign.ISignSessionRemote;
import org.ejbca.core.ejb.ca.store.ICertificateStoreSessionHome;
import org.ejbca.core.ejb.ca.store.ICertificateStoreSessionRemote;
import org.ejbca.core.ejb.ra.IUserAdminSessionHome;
import org.ejbca.core.ejb.ra.IUserAdminSessionRemote;
import org.ejbca.core.ejb.ra.raadmin.IRaAdminSessionHome;
import org.ejbca.core.ejb.ra.raadmin.IRaAdminSessionRemote;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.ca.IllegalKeyException;
import org.ejbca.core.model.ca.caadmin.CAInfo;
import org.ejbca.core.model.ca.catoken.CATokenConstants;
import org.ejbca.core.model.ca.certificateprofiles.CertificateProfile;
import org.ejbca.core.model.ca.certificateprofiles.EndUserCertificateProfile;
import org.ejbca.core.model.log.Admin;
import org.ejbca.core.model.ra.UserDataConstants;
import org.ejbca.core.model.ra.raadmin.EndEntityProfile;
import org.ejbca.core.protocol.IResponseMessage;
import org.ejbca.core.protocol.PKCS10RequestMessage;
import org.ejbca.util.Base64;
import org.ejbca.util.CertTools;
import org.ejbca.util.KeyTools;
import org.ejbca.util.cert.QCStatementExtension;
import org.ejbca.util.dn.DnComponents;


/**
 * Tests signing session.
 *
 * @version $Id: TestSignSession.java,v 1.25 2006-12-04 09:07:19 anatom Exp $
 */
public class TestSignSession extends TestCase {
    static byte[] keytoolp10 = Base64.decode(("MIIBbDCB1gIBADAtMQ0wCwYDVQQDEwRUZXN0MQ8wDQYDVQQKEwZBbmFUb20xCzAJBgNVBAYTAlNF" +
            "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDY+ATE4ZB0oKfmXStu8J+do0GhTag6rOGtoydI" +
            "eNX9DdytlsmXDyONKl8746478/3HXdx9rA0RevUizKSataMpDsb3TjprRjzBTvYPZSIfzko6s8g6" +
            "AZLO07xCFOoDmyRzb9k/KEZsMls0ujx79CQ9p5K4rg2ksjmDeW7DaPMphQIDAQABoAAwDQYJKoZI" +
            "hvcNAQEFBQADgYEAyJVobqn6wGRoEsdHxjoqPXw8fLrQyBGEwXccnVpI4kv9iIZ45Xres0LrOwtS" +
            "kFLbpn0guEzhxPBbL6mhhmDDE4hbbHJp1Kh6gZ4Bmbb5FrwpvUyrSjTIwwRC7GAT00A1kOjl9jCC" +
            "XCfJkJH2QleCy7eKANq+DDTXzpEOvL/UqN0=").getBytes());
    static byte[] oldbcp10 = Base64.decode(("MIIBbDCB1gIBADAtMQswCQYDVQQGEwJTRTEPMA0GA1UEChMGQW5hVG9tMQ0wCwYDVQQDEwRUZXN0" +
            "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCzN9nDdwmq23/RLGisvR3CRO9JSem2QZ7JC7nr" +
            "NlbxQBLVqlkypT/lxMMur+lTX1S+jBaqXjtirhZTVaV5C/+HObWZ5vrj30lmsCdgzFybSzVxBz0l" +
            "XC0UEDbgBml/hO70cSDdmyw3YE9g5eH3wdYs2FCTzexRF3kNAVHNUa8svwIDAQABoAAwDQYJKoZI" +
            "hvcNAQEFBQADgYEAm6uRSyEmyCcs652Ttg2npm6JZPFT2qwSl4dviyIKJbn6j+meCzvn2TMP10d8" +
            "7Ak5sv5NJew1XGkM4mGpF9cfcVshxLVlW+cgq0749fWbyS8KlgQP/ANh3DkLl8k5E+3Wnbi0JjCV" +
            "Xe1s44+K2solX8jOtryoR4TMJ6p9HpsuO68=").getBytes());
    static byte[] iep10 = Base64.decode(("MIICnTCCAgYCAQAwGzEZMBcGA1UEAxMQNkFFSzM0N2Z3OHZXRTQyNDCBnzANBgkq" +
            "hkiG9w0BAQEFAAOBjQAwgYkCgYEAukW70HN9bt5x2AiSZm7y8GXQuyp1jN2OIvqU" +
            "sr0dzLIOFt1H8GPJkL80wx3tLDj3xJfWJdww3TqExsxMSP+qScoYKIOeNBb/2OMW" +
            "p/k3DThCOewPebmt+M08AClq5WofXTG+YxyJgXWbMTNfXKIUyR0Ju4Spmg6Y4eJm" +
            "GXTG7ZUCAwEAAaCCAUAwGgYKKwYBBAGCNw0CAzEMFgo1LjAuMjE5NS4yMCAGCisG" +
            "AQQBgjcCAQ4xEjAQMA4GA1UdDwEB/wQEAwIE8DCB/wYKKwYBBAGCNw0CAjGB8DCB" +
            "7QIBAR5cAE0AaQBjAHIAbwBzAG8AZgB0ACAARQBuAGgAYQBuAGMAZQBkACAAQwBy" +
            "AHkAcAB0AG8AZwByAGEAcABoAGkAYwAgAFAAcgBvAHYAaQBkAGUAcgAgAHYAMQAu" +
            "ADADgYkAjuYPzZPpbLgCWYnXoNeX2gS6nuI4osrWHlQQKcS67VJclhELlnT3hBb9" +
            "Blr7I0BsJ/lguZvZFTZnC1bMeNULRg17bhExTg+nUovzPcJhMvG7G3DR17PrJ7V+" +
            "egHAsQV4dQC2hOGGhOnv88JhP9Pwpso3t2tqJROa5ZNRRSJSkw8AAAAAAAAAADAN" +
            "BgkqhkiG9w0BAQQFAAOBgQCL5k4bJt265j63qB/9GoQb1XFOPSar1BDFi+veCPA2" +
            "GJ/vRXt77Vcr4inx9M51iy87FNcGGsmyesBoDg73p06UxpIDhkL/WpPwZAfQhWGe" +
            "o/gWydmP/hl3uEfE0E4WG02UXtNwn3ziIiJM2pBCGQQIN2rFggyD+aTxwAwOU7Z2" + "fw==").getBytes());
    static byte[] openscep = Base64.decode(("MIIFSwYJKoZIhvcNAQcCoIIFPDCCBTgCAQExDjAMBggqhkiG9w0CBQUAMIICMwYJ" +
            "KoZIhvcNAQcBoIICJASCAiAwggIcBgkqhkiG9w0BBwOgggINMIICCQIBADGB1TCB" +
            "0gIBADA7MC8xDzANBgNVBAMTBlRlc3RDQTEPMA0GA1UEChMGQW5hVG9tMQswCQYD" +
            "VQQGEwJTRQIIbzEhUVZYO3gwDQYJKoZIhvcNAQEBBQAEgYDJP3tsx1KMC+Ws3gcV" +
            "gpvatMgxocUrKS2Z5BRj7z8HE/BySwa40fwzpBXq3xhakclrdK9D6Bb7I2oTqaNo" +
            "y25tk2ykow8px1HEerGg5eCIDeAwX4IGurKn+ajls4vWntybgtosAFPLuBO2sdfy" +
            "VhTv+iFxkl+lZgcRfpJhmqfOJjCCASoGCSqGSIb3DQEHATARBgUrDgMCBwQIapUt" +
            "FKgA/KmAggEIpzjb5ONkiT7gPs5VeQ6a2e3IdXMgZTRknqZZRRzRovKwp17LJPkA" +
            "AF9vQKCk6IQwM1dY4NAhu/mCvkfQwwVgML+rbsx7cYH5VuMxw6xw79CnGZbcgOoE" +
            "lhfYR9ytfZFAVjs8TF/cx1GfuxxN/3RdXzwIFmvPRX1SPh83ueMbGTHjmk0/kweE" +
            "9XcLkI85jTyG/Dsq3mUlWDS4qQg4sSbFAvkHgmCl0DQd2qW3eV9rCDbfPNjc+2dq" +
            "nG5EwjX1UVYS2TSWy7vu6MQvKtEWFP4B10+vGBcVE8fZ4IxL9TDQ4UMz3gfFIQSc" +
            "Moq4lw7YKmywbbyieGGYJuXDX/0gUBKj/MrP9s3L12bLoIIBajCCAWYwggEQoAMC" +
            "AQMCIDNGREQzNUM5NzZDODlENjcwRjNCM0IxOTgxQjhDMzA2MA0GCSqGSIb3DQEB" +
            "BAUAMCwxCzAJBgNVBAYTAlNFMQ8wDQYDVQQKEwZBbmFUb20xDDAKBgNVBAMTA2Zv" +
            "bzAeFw0wMzA2MTkwODQ3NDlaFw0wMzA3MTkwODQ3NDlaMCwxCzAJBgNVBAYTAlNF" +
            "MQ8wDQYDVQQKEwZBbmFUb20xDDAKBgNVBAMTA2ZvbzBcMA0GCSqGSIb3DQEBAQUA" +
            "A0sAMEgCQQDLfHDEOse6Mbi02egr2buI9mgWC0ur9dvGmLiIxmNg1TNhn1WHj5Zy" +
            "VsjKyLoVuVqgGRPYVA73ItANF8RNBAt9AgMBAAEwDQYJKoZIhvcNAQEEBQADQQCw" +
            "9kQsl3M0Ag1892Bu3izeZOYKpze64kJ7iGuYmN8atkdO8Rpp4Jn0W6vvUYQcat2a" +
            "Jzf6h3xfEQ7m8CzvaQ2/MYIBfDCCAXgCAQEwUDAsMQswCQYDVQQGEwJTRTEPMA0G" +
            "A1UEChMGQW5hVG9tMQwwCgYDVQQDEwNmb28CIDNGREQzNUM5NzZDODlENjcwRjNC" +
            "M0IxOTgxQjhDMzA2MAwGCCqGSIb3DQIFBQCggcEwEgYKYIZIAYb4RQEJAjEEEwIx" +
            "OTAYBgkqhkiG9w0BCQMxCwYJKoZIhvcNAQcBMBwGCSqGSIb3DQEJBTEPFw0wMzA2" +
            "MTkwODQ3NDlaMB8GCSqGSIb3DQEJBDESBBCevtHE4n3my5B7Q+MiKj04MCAGCmCG" +
            "SAGG+EUBCQUxEgQQwH1TAMlSzz1d3SNXoOARkTAwBgpghkgBhvhFAQkHMSITIDNG" +
            "REQzNUM5NzZDODlENjcwRjNCM0IxOTgxQjhDMzA2MA0GCSqGSIb3DQEBAQUABEAW" +
            "r+9YB3t1750Aj4bm5JAHv80VhzkrPmVLZqsJdC2DGn3UQFp1FhXo4od2xGpeg+pZ" +
            "b0B6kUt+uxvuq3PbagLi").getBytes());
    static byte[] keytooldsa = Base64.decode(("MIICNjCCAfQCAQAwMTERMA8GA1UEAxMIRFNBIFRlc3QxDzANBgNVBAoTBkFuYXRvbTELMAkGA1UE" +
            "BhMCU0UwggG4MIIBLAYHKoZIzjgEATCCAR8CgYEA/X9TgR11EilS30qcLuzk5/YRt1I870QAwx4/" +
            "gLZRJmlFXUAiUftZPY1Y+r/F9bow9subVWzXgTuAHTRv8mZgt2uZUKWkn5/oBHsQIsJPu6nX/rfG" +
            "G/g7V+fGqKYVDwT7g/bTxR7DAjVUE1oWkTL2dfOuK2HXKu/yIgMZndFIAccCFQCXYFCPFSMLzLKS" +
            "uYKi64QL8Fgc9QKBgQD34aCF1ps93su8q1w2uFe5eZSvu/o66oL5V0wLPQeCZ1FZV4661FlP5nEH" +
            "EIGAtEkWcSPoTCgWE7fPCTKMyKbhPBZ6i1R8jSjgo64eK7OmdZFuo38L+iE1YvH7YnoBJDvMpPG+" +
            "qFGQiaiD3+Fa5Z8GkotmXoB7VSVkAUw7/s9JKgOBhQACgYEAiVCUaC95mHaU3C9odWcuJ8j3fT6z" +
            "bSR02CVFC0F6QO5s2Tx3JYWrm5aAjWkXWJfeYOR6qBSwX0R1US3rDI0Kepsrdco2q7wGSo+235KL" +
            "Yfl7tQ9RLOKUGX/1c5+XuvN1ZbGy0yUw3Le16UViahWmmx6FM1sW6M48U7C/CZOyoxagADALBgcq" +
            "hkjOOAQDBQADLwAwLAIUQ+S2iFA1y7dfDWUCg7j1Nc8RW0oCFFhnDlU69xFRMeXXn1C/Oi+8pwrQ").getBytes());
    private static Logger log = Logger.getLogger(TestSignSession.class);
    private static Context ctx;
    private static ISignSessionHome home;
    private static ISignSessionRemote remote;
    private static IUserAdminSessionRemote usersession;
    private static IRaAdminSessionRemote rasession;
    private static ICertificateStoreSessionRemote storesession;
    private static KeyPair rsakeys=null;
    private static KeyPair ecdsakeys=null;
    private static KeyPair ecdsaimplicitlyca=null;
    private static int rsacaid = 0;
    private static int ecdsacaid = 0;
    private static int ecdsaimplicitlycacaid = 0;
    X509Certificate rsacacert = null;
    X509Certificate ecdsacacert = null;
    X509Certificate ecdsaimplicitlycacacert = null;
    private Admin admin;

    /**
     * Creates a new TestSignSession object.
     *
     * @param name name
     */
    public TestSignSession(String name) throws Exception {
        super(name);

        // Install BouncyCastle provider
        CertTools.installBCProvider();
        if (rsakeys == null) {
        	rsakeys = KeyTools.genKeys("1024", CATokenConstants.KEYALGORITHM_RSA);
        }
        if (ecdsakeys == null) {
        	ecdsakeys = KeyTools.genKeys("prime192v1", CATokenConstants.KEYALGORITHM_ECDSA);
        }
        if (ecdsaimplicitlyca == null) {
        	ecdsaimplicitlyca = KeyTools.genKeys("implicitlyCA", CATokenConstants.KEYALGORITHM_ECDSA);
        }

        admin = new Admin(Admin.TYPE_BATCHCOMMANDLINE_USER);

        ctx = getInitialContext();
        Object obj = ctx.lookup("CAAdminSession");
        ICAAdminSessionHome cahome = (ICAAdminSessionHome) javax.rmi.PortableRemoteObject.narrow(obj, ICAAdminSessionHome.class);
        ICAAdminSessionRemote casession = cahome.create();
        CAInfo inforsa = casession.getCAInfo(admin, "TEST");
        rsacaid = inforsa.getCAId();
        if (rsacaid == 0){
            assertTrue("No active RSA CA! Must have at least one active CA to run tests!", false);
        }
        CAInfo infoecdsa = casession.getCAInfo(admin, "TESTECDSA");
        ecdsacaid = infoecdsa.getCAId();
        if (ecdsacaid == 0){
            assertTrue("No active ECDSA CA! Must have at least one active CA to run tests!", false);
        }
        CAInfo infoecdsaimplicitlyca = casession.getCAInfo(admin, "TESTECDSAImplicitlyCA");
        ecdsaimplicitlycacaid = infoecdsaimplicitlyca.getCAId();
        if (ecdsaimplicitlycacaid == 0){
            assertTrue("No active ECDSA ImplicitlyCA CA! Must have at least one active CA to run tests!", false);
        }
        Collection coll = inforsa.getCertificateChain();
        Object[] objs = coll.toArray();
        rsacacert = (X509Certificate)objs[0]; 
        coll = infoecdsa.getCertificateChain();
        objs = coll.toArray();
        ecdsacacert = (X509Certificate)objs[0]; 
        coll = infoecdsaimplicitlyca.getCertificateChain();
        objs = coll.toArray();
        ecdsaimplicitlycacacert = (X509Certificate)objs[0]; 
        
        obj = ctx.lookup("RSASignSession");
        home = (ISignSessionHome) javax.rmi.PortableRemoteObject.narrow(obj, ISignSessionHome.class);
        remote = home.create();

        obj = ctx.lookup("UserAdminSession");
        IUserAdminSessionHome userhome = (IUserAdminSessionHome) javax.rmi.PortableRemoteObject.narrow(obj, IUserAdminSessionHome.class);
        usersession = userhome.create();
        
        obj = ctx.lookup("RaAdminSession");
        IRaAdminSessionHome rahome = (IRaAdminSessionHome) javax.rmi.PortableRemoteObject.narrow(obj, IRaAdminSessionHome.class);
        rasession = rahome.create();

        obj = ctx.lookup("CertificateStoreSession");
        ICertificateStoreSessionHome storehome = (ICertificateStoreSessionHome) javax.rmi.PortableRemoteObject.narrow(obj, ICertificateStoreSessionHome.class);
        storesession = storehome.create();

    }

    protected void setUp() throws Exception {
        log.debug(">setUp()");

        log.debug("<setUp()");
    }

    protected void tearDown() throws Exception {
    }

    private Context getInitialContext() throws NamingException {
        log.debug(">getInitialContext");

        Context ctx = new javax.naming.InitialContext();
        log.debug("<getInitialContext");

        return ctx;
    }


    /**
     * creates new user
     *
     * @throws Exception if en error occurs...
     */
    public void test01CreateNewUser() throws Exception {
        log.debug(">test01CreateNewUser()");

        // Make user that we know...
        boolean userExists = false;
        try {
            usersession.addUser(admin,"foo","foo123","C=SE,O=AnaTom,CN=foo",null,"foo@anatom.se",false,SecConst.EMPTY_ENDENTITYPROFILE,SecConst.CERTPROFILE_FIXED_ENDUSER,SecConst.USER_ENDUSER,SecConst.TOKEN_SOFT_PEM,0,rsacaid);
            log.debug("created user: foo, foo123, C=SE, O=AnaTom, CN=foo");
        } catch (RemoteException re) {
            if (re.detail instanceof DuplicateKeyException) {
                userExists = true;
            }
        } catch (DuplicateKeyException dke) {
            userExists = true;
        }
        if (userExists) {
            log.info("User foo already exists, resetting status.");
            usersession.changeUser(admin,"foo","foo123","C=SE,O=AnaTom,CN=foo",null,"foo@anatom.se",false,SecConst.EMPTY_ENDENTITYPROFILE,SecConst.CERTPROFILE_FIXED_ENDUSER,SecConst.USER_ENDUSER,SecConst.TOKEN_SOFT_PEM,0,UserDataConstants.STATUS_NEW,rsacaid);
            //usersession.setUserStatus(admin,"foo",UserDataConstants.STATUS_NEW);
            log.debug("Reset status to NEW");
        }
        userExists = false;
        try {
            usersession.addUser(admin,"fooecdsa","foo123","C=SE,O=AnaTom,CN=fooecdsa",null,"foo@anatom.se",false,SecConst.EMPTY_ENDENTITYPROFILE,SecConst.CERTPROFILE_FIXED_ENDUSER,SecConst.USER_ENDUSER,SecConst.TOKEN_SOFT_PEM,0,ecdsacaid);
            log.debug("created user: fooecdsa, foo123, C=SE, O=AnaTom, CN=fooecdsa");
        } catch (RemoteException re) {
            if (re.detail instanceof DuplicateKeyException) {
                userExists = true;
            }
        } catch (DuplicateKeyException dke) {
            userExists = true;
        }
        if (userExists) {
            log.info("User fooecdsa already exists, resetting status.");
            usersession.setUserStatus(admin,"fooecdsa",UserDataConstants.STATUS_NEW);
            log.debug("Reset status to NEW");
        }
        userExists = false;
        try {
            usersession.addUser(admin,"fooecdsaimpca","foo123","C=SE,O=AnaTom,CN=fooecdsaimpca",null,"foo@anatom.se",false,SecConst.EMPTY_ENDENTITYPROFILE,SecConst.CERTPROFILE_FIXED_ENDUSER,SecConst.USER_ENDUSER,SecConst.TOKEN_SOFT_PEM,0,ecdsaimplicitlycacaid);
            log.debug("created user: fooecdsaimpca, foo123, C=SE, O=AnaTom, CN=fooecdsaimpca");
        } catch (RemoteException re) {
            if (re.detail instanceof DuplicateKeyException) {
                userExists = true;
            }
        } catch (DuplicateKeyException dke) {
            userExists = true;
        }
        if (userExists) {
            log.info("User fooecdsaimpca already exists, resetting status.");
            usersession.setUserStatus(admin,"fooecdsaimpca",UserDataConstants.STATUS_NEW);
            log.debug("Reset status to NEW");
        }

        log.debug("<test01CreateNewUser()");
    }

    /**
     * creates cert
     *
     * @throws Exception if en error occurs...
     */
    public void test02SignSession() throws Exception {
        log.debug(">test02SignSession()");

        // user that we know exists...
        X509Certificate cert = (X509Certificate) remote.createCertificate(admin, "foo", "foo123", rsakeys.getPublic());
        assertNotNull("Misslyckades skapa cert", cert);
        log.debug("Cert=" + cert.toString());

        try {
            cert.verify(rsacacert.getPublicKey());        	
        } catch (Exception e) {
        	assertTrue("Verify failed: "+e.getMessage(), false);
        }
        //FileOutputStream fos = new FileOutputStream("testcert.crt");
        //fos.write(cert.getEncoded());
        //fos.close();
        log.debug("<test02SignSession()");
    }

    /**
     * tests bouncy PKCS10
     *
     * @throws Exception if en error occurs...
     */
    public void test03TestBCPKCS10() throws Exception {
        log.debug(">test03TestBCPKCS10()");
        usersession.setUserStatus(admin,"foo",UserDataConstants.STATUS_NEW);
        log.debug("Reset status of 'foo' to NEW");
        // Create certificate request
        PKCS10CertificationRequest req = new PKCS10CertificationRequest("SHA1WithRSA",
                CertTools.stringToBcX509Name("C=SE, O=AnaTom, CN=foo"), rsakeys.getPublic(), null,
                rsakeys.getPrivate());
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        DEROutputStream dOut = new DEROutputStream(bOut);
        dOut.writeObject(req);
        dOut.close();

        PKCS10CertificationRequest req2 = new PKCS10CertificationRequest(bOut.toByteArray());
        boolean verify = req2.verify();
        log.debug("Verify returned " + verify);
        if (verify == false) {
            log.debug("Aborting!");

            return;
        }
        log.debug("CertificationRequest generated successfully.");
        byte[] bcp10 = bOut.toByteArray();
        PKCS10RequestMessage p10 = new PKCS10RequestMessage(bcp10);
        p10.setUsername("foo");
        p10.setPassword("foo123");
        IResponseMessage resp = remote.createCertificate(admin,
                p10, Class.forName("org.ejbca.core.protocol.X509ResponseMessage"));
        X509Certificate cert = CertTools.getCertfromByteArray(resp.getResponseMessage());
        assertNotNull("Failed to create certificate", cert);
        log.debug("Cert=" + cert.toString());
        log.debug("<test03TestBCPKCS10()");
    }

    /**
     * tests keytool pkcs10
     *
     * @throws Exception if en error occurs...
     */
    public void test04TestKeytoolPKCS10() throws Exception {
        log.debug(">test04TestKeytoolPKCS10()");

        usersession.setUserStatus(admin,"foo",UserDataConstants.STATUS_NEW);
        log.debug("Reset status of 'foo' to NEW");

        PKCS10RequestMessage p10 = new PKCS10RequestMessage(keytoolp10);
        p10.setUsername("foo");
        p10.setPassword("foo123");
        IResponseMessage resp = remote.createCertificate(admin,
                p10, Class.forName("org.ejbca.core.protocol.X509ResponseMessage"));
        X509Certificate cert = CertTools.getCertfromByteArray(resp.getResponseMessage());
        assertNotNull("Failed to create certificate", cert);
        log.debug("Cert=" + cert.toString());
        log.debug("<test04TestKeytoolPKCS10()");
    }

    /**
     * tests ie pkcs10
     *
     * @throws Exception if en error occurs...
     */
    public void test05TestIEPKCS10() throws Exception {
        log.debug(">test05TestIEPKCS10()");

        usersession.setUserStatus(admin,"foo",UserDataConstants.STATUS_NEW);
        log.debug("Reset status of 'foo' to NEW");

        PKCS10RequestMessage p10 = new PKCS10RequestMessage(iep10);
        p10.setUsername("foo");
        p10.setPassword("foo123");
        IResponseMessage resp = remote.createCertificate(admin,
                p10, Class.forName("org.ejbca.core.protocol.X509ResponseMessage"));
        X509Certificate cert = CertTools.getCertfromByteArray(resp.getResponseMessage());
        assertNotNull("Failed to create certificate", cert);
        log.debug("Cert=" + cert.toString());
        log.debug("<test05TestIEPKCS10()");
    }

    /**
     * test to set specific key usage
     *
     * @throws Exception if en error occurs...
     */
    public void test06KeyUsage() throws Exception {
        log.debug(">test06KeyUsage()");

        usersession.setUserStatus(admin,"foo",UserDataConstants.STATUS_NEW);
        log.debug("Reset status of 'foo' to NEW");

        // Create an array for KeyUsage acoording to X509Certificate.getKeyUsage()
        boolean[] keyusage1 = new boolean[9];
        Arrays.fill(keyusage1, false);
        // digitalSignature
        keyusage1[0] = true;
        // keyEncipherment
        keyusage1[2] = true;

        X509Certificate cert = (X509Certificate) remote.createCertificate(admin, "foo", "foo123", rsakeys.getPublic(), keyusage1);
        assertNotNull("Misslyckades skapa cert", cert);
        log.debug("Cert=" + cert.toString());
        boolean[] retKU = cert.getKeyUsage();
        assertTrue("Fel KeyUsage, digitalSignature finns ej!", retKU[0]);
        assertTrue("Fel KeyUsage, keyEncipherment finns ej!", retKU[2]);
        assertTrue("Fel KeyUsage, cRLSign finns!", !retKU[6]);

        usersession.setUserStatus(admin,"foo",UserDataConstants.STATUS_NEW);
        log.debug("Reset status of 'foo' to NEW");

        boolean[] keyusage2 = new boolean[9];
        Arrays.fill(keyusage2, false);
        // keyCertSign
        keyusage2[5] = true;
        // cRLSign
        keyusage2[6] = true;

        X509Certificate cert1 = (X509Certificate) remote.createCertificate(admin, "foo", "foo123", rsakeys.getPublic(), keyusage2);
        assertNotNull("Misslyckades skapa cert", cert1);
        retKU = cert1.getKeyUsage();
        assertTrue("Fel KeyUsage, keyCertSign finns ej!", retKU[5]);
        assertTrue("Fel KeyUsage, cRLSign finns ej!", retKU[6]);
        assertTrue("Fel KeyUsage, digitalSignature finns!", !retKU[0]);

        log.debug("Cert=" + cert1.toString());
        log.debug("<test06KeyUsage()");
    }

    /**
     * test DSA keys instead of RSA
     *
     * @throws Exception if en error occurs...
     */
    public void test07DSAKey() throws Exception {
        log.debug(">test07DSAKey()");

        usersession.setUserStatus(admin,"foo",UserDataConstants.STATUS_NEW);
        log.debug("Reset status of 'foo' to NEW");

        try {
            PKCS10RequestMessage p10 = new PKCS10RequestMessage(keytooldsa);
            p10.setUsername("foo");
            p10.setPassword("foo123");
            IResponseMessage resp = remote.createCertificate(admin,
                    p10, Class.forName("org.ejbca.core.protocol.X509ResponseMessage"));
            X509Certificate cert = CertTools.getCertfromByteArray(resp.getResponseMessage());
            log.info("cert with DN '"+cert.getSubjectDN().getName()+"' should not be issued?");
        } catch (Exception e) {
            // RSASignSession should throw an IllegalKeyException here.
            assertTrue("Expected IllegalKeyException: " + e.toString(),
                    e instanceof IllegalKeyException);
        }

        log.debug("<test07DSAKey()");
    }

    /**
     * Tests international characters
     *
     * @throws Exception if en error occurs...
     */
    public void test08SwedeChars() throws Exception {
        log.debug(">test08SwedeChars()");
        // Make user that we know...
        boolean userExists = false;
        try {
            usersession.addUser(admin,"swede","foo123","C=SE, O=���, CN=���",null,"swede@anatom.se",false,SecConst.EMPTY_ENDENTITYPROFILE,SecConst.CERTPROFILE_FIXED_ENDUSER,SecConst.USER_ENDUSER,SecConst.TOKEN_SOFT_PEM,0,rsacaid);
            log.debug("created user: swede, foo123, C=SE, O=���, CN=���");
        } catch (RemoteException re) {
            if (re.detail instanceof DuplicateKeyException) {
                userExists = true;
            }
        } catch (DuplicateKeyException dke) {
            userExists = true;
        }
        if (userExists) {
            log.debug("user swede already exists.");

            usersession.setUserStatus(admin,"swede",UserDataConstants.STATUS_NEW);
            log.debug("Reset status to NEW");
        }

        // user that we know exists...
        X509Certificate cert = (X509Certificate) remote.createCertificate(admin, "swede", "foo123", rsakeys.getPublic());
        assertNotNull("Failed to create certificate", cert);
        log.debug("Cert=" + cert.toString());
        assertEquals("Wrong DN med swedechars", CertTools.getSubjectDN(cert),
                CertTools.stringToBCDNString("C=SE, O=���, CN=���"));
        //FileOutputStream fos = new FileOutputStream("swedecert.crt");
        //fos.write(cert.getEncoded());
        //fos.close();
        log.debug("<test08SwedeChars()");
    }


    /** Tests multiple instances of one altName
     * 
     */
    public void test09TestMultipleAltNames() throws Exception {
        log.debug(">test09TestMultipleAltNames()");

        // Create a good end entity profile (good enough), allowing multiple UPN names
        rasession.removeEndEntityProfile(admin, "TESTMULALTNAME");
        EndEntityProfile profile = new EndEntityProfile();
        profile.addField(DnComponents.ORGANIZATION);
        profile.addField(DnComponents.COUNTRY);
        profile.addField(DnComponents.COMMONNAME);
        profile.addField(DnComponents.UNIFORMRESOURCEID);
        profile.addField(DnComponents.DNSNAME);
        profile.addField(DnComponents.DNSNAME);
        profile.addField(DnComponents.RFC822NAME);
        profile.addField(DnComponents.IPADDRESS);
        profile.addField(DnComponents.UPN);
        profile.addField(DnComponents.UPN);
        profile.setValue(EndEntityProfile.AVAILCAS,0, Integer.toString(SecConst.ALLCAS));
        rasession.addEndEntityProfile(admin, "TESTMULALTNAME", profile);
        int eeprofile = rasession.getEndEntityProfileId(admin, "TESTMULALTNAME");
        try {
            // Change a user that we know...
            usersession.changeUser(admin, "foo", "foo123", "C=SE,O=AnaTom,CN=foo",
                    "uniformResourceId=http://www.a.se/,upn=foo@a.se,upn=foo@b.se,rfc822name=tomas@a.se,dNSName=www.a.se,dNSName=www.b.se,iPAddress=10.1.1.1",
                    "foo@anatom.se", false,
                    eeprofile,
                    SecConst.CERTPROFILE_FIXED_ENDUSER,
                    SecConst.USER_ENDUSER,
                    SecConst.TOKEN_SOFT_PEM, 0, UserDataConstants.STATUS_NEW, rsacaid);
            log.debug("created user: foo, foo123, C=SE, O=AnaTom, CN=foo");
        } catch (RemoteException re) {
            assertTrue("User foo does not exist, or error changing user", false);
        } 
        X509Certificate cert = (X509Certificate) remote.createCertificate(admin, "foo", "foo123", rsakeys.getPublic());
        assertNotNull("Failed to create certificate", cert);
//        FileOutputStream fos = new FileOutputStream("cert.crt");
//        fos.write(cert.getEncoded());
//        fos.close();
        String altNames = CertTools.getSubjectAlternativeName(cert);
        log.debug(altNames);
        ArrayList list = CertTools.getPartsFromDN(altNames,CertTools.UPN);
        assertEquals(2, list.size());
        assertTrue(list.contains("foo@a.se"));
        assertTrue(list.contains("foo@b.se"));
        String name = CertTools.getPartFromDN(altNames,CertTools.URI);
        assertEquals("http://www.a.se/", name);
        name = CertTools.getPartFromDN(altNames,CertTools.EMAIL);
        assertEquals("tomas@a.se", name);
        list = CertTools.getPartsFromDN(altNames,CertTools.DNS);
        assertEquals(2, list.size());
        assertTrue(list.contains("www.a.se"));
        assertTrue(list.contains("www.b.se"));
        name = CertTools.getPartFromDN(altNames,CertTools.IPADDR);
        assertEquals("10.1.1.1", name);

        try {
            // Change a user that we know...
            usersession.changeUser(admin, "foo", "foo123", "C=SE,O=AnaTom,CN=foo",
                    "uri=http://www.a.se/,upn=foo@a.se,upn=foo@b.se,rfc822name=tomas@a.se,dNSName=www.a.se,dNSName=www.b.se,iPAddress=10.1.1.1",
                    "foo@anatom.se", false,
                    eeprofile,
                    SecConst.CERTPROFILE_FIXED_ENDUSER,
                    SecConst.USER_ENDUSER,
                    SecConst.TOKEN_SOFT_PEM, 0, UserDataConstants.STATUS_NEW, rsacaid);
            log.debug("created user: foo, foo123, C=SE, O=AnaTom, CN=foo");
        } catch (RemoteException re) {
            assertTrue("User foo does not exist, or error changing user", false);
        } 
        cert = (X509Certificate) remote.createCertificate(admin, "foo", "foo123", rsakeys.getPublic());
        assertNotNull("Failed to create certificate", cert);
//        FileOutputStream fos = new FileOutputStream("cert.crt");
//        fos.write(cert.getEncoded());
//        fos.close();
        altNames = CertTools.getSubjectAlternativeName(cert);
        log.debug(altNames);
        list = CertTools.getPartsFromDN(altNames,CertTools.UPN);
        assertEquals(2, list.size());
        assertTrue(list.contains("foo@a.se"));
        assertTrue(list.contains("foo@b.se"));
        name = CertTools.getPartFromDN(altNames,CertTools.URI);
        assertEquals("http://www.a.se/", name);
        name = CertTools.getPartFromDN(altNames,CertTools.EMAIL);
        assertEquals("tomas@a.se", name);
        list = CertTools.getPartsFromDN(altNames,CertTools.DNS);
        assertEquals(2, list.size());
        assertTrue(list.contains("www.a.se"));
        assertTrue(list.contains("www.b.se"));
        name = CertTools.getPartFromDN(altNames,CertTools.IPADDR);
        assertEquals("10.1.1.1", name);

        // Clean up
        rasession.removeEndEntityProfile(admin, "TESTMULALTNAME");

        log.debug("<test09TestMultipleAltNames()");        
    }
    
    /** Tests creting a certificate with QC statement
     * 
     */
    public void test10TestQcCert() throws Exception {
        log.debug(">test10TestQcCert()");

        // Create a good certificate profile (good enough), using QC statement
        storesession.removeCertificateProfile(admin,"TESTQC");
        EndUserCertificateProfile certprof = new EndUserCertificateProfile();
        certprof.setUseQCStatement(true);
        certprof.setQCStatementRAName("rfc822Name=qc@primekey.se");
        certprof.setUseQCEtsiQCCompliance(true);
        certprof.setUseQCEtsiSignatureDevice(true);
        certprof.setUseQCEtsiValueLimit(true);
        certprof.setQCEtsiValueLimit(50000);
        certprof.setQCEtsiValueLimitCurrency("SEK");
        storesession.addCertificateProfile(admin, "TESTQC", certprof);
        int cprofile = storesession.getCertificateProfileId(admin,"TESTQC");

        // Create a good end entity profile (good enough), allowing multiple UPN names
        rasession.removeEndEntityProfile(admin, "TESTQC");
        EndEntityProfile profile = new EndEntityProfile();
        profile.addField(DnComponents.COUNTRY);
        profile.addField(DnComponents.COMMONNAME);
        profile.setValue(EndEntityProfile.AVAILCAS,0, Integer.toString(SecConst.ALLCAS));
        profile.setValue(EndEntityProfile.AVAILCERTPROFILES,0,Integer.toString(cprofile));
        rasession.addEndEntityProfile(admin, "TESTQC", profile);
        int eeprofile = rasession.getEndEntityProfileId(admin, "TESTQC");
        try {
            // Change a user that we know...
            usersession.changeUser(admin, "foo", "foo123", "C=SE,CN=qc",
                    null,
                    "foo@anatom.nu", false,
                    eeprofile,
                    cprofile,
                    SecConst.USER_ENDUSER,
                    SecConst.TOKEN_SOFT_PEM, 0, UserDataConstants.STATUS_NEW, rsacaid);
            log.debug("created user: foo, foo123, C=SE, CN=qc");
        } catch (RemoteException re) {
            assertTrue("User foo does not exist, or error changing user", false);
        } 
        X509Certificate cert = (X509Certificate) remote.createCertificate(admin, "foo", "foo123", rsakeys.getPublic());
        assertNotNull("Failed to create certificate", cert);
//        FileOutputStream fos = new FileOutputStream("cert.crt");
//        fos.write(cert.getEncoded());
//        fos.close();
        String dn = cert.getSubjectDN().getName();
        assertEquals(CertTools.stringToBCDNString("cn=qc,c=SE"), CertTools.stringToBCDNString(dn));
        assertEquals("rfc822name=qc@primekey.se", QCStatementExtension.getQcStatementAuthorities(cert));
        Collection ids = QCStatementExtension.getQcStatementIds(cert);
        assertTrue(ids.contains(RFC3739QCObjectIdentifiers.id_qcs_pkixQCSyntax_v1.getId()));
        assertTrue(ids.contains(ETSIQCObjectIdentifiers.id_etsi_qcs_QcCompliance.getId()));
        assertTrue(ids.contains(ETSIQCObjectIdentifiers.id_etsi_qcs_QcSSCD.getId()));
        assertTrue(ids.contains(ETSIQCObjectIdentifiers.id_etsi_qcs_LimiteValue.getId()));
        String limit = QCStatementExtension.getQcStatementValueLimit(cert);
        assertEquals("50000 SEK", limit);

        // Clean up
        rasession.removeEndEntityProfile(admin, "TESTQC");
        storesession.removeCertificateProfile(admin,"TESTQC");

        log.debug("<test10TestQcCert()");        
    }

    /** Tests creting a certificate with QC statement
     * 
     */
    public void test11TestValidityOverride() throws Exception {
        log.debug(">test11TestValidityOverride()");

        // Create a good certificate profile (good enough), using QC statement
        storesession.removeCertificateProfile(admin,"TESTVALOVERRIDE");
        EndUserCertificateProfile certprof = new EndUserCertificateProfile();
        certprof.setAllowValidityOverride(false);
        certprof.setValidity(298);
        storesession.addCertificateProfile(admin, "TESTVALOVERRIDE", certprof);
        int cprofile = storesession.getCertificateProfileId(admin,"TESTVALOVERRIDE");

        // Create a good end entity profile (good enough), allowing multiple UPN names
        rasession.removeEndEntityProfile(admin, "TESTVALOVERRIDE");
        EndEntityProfile profile = new EndEntityProfile();
        profile.addField(DnComponents.COUNTRY);
        profile.addField(DnComponents.COMMONNAME);
        profile.setValue(EndEntityProfile.AVAILCAS,0, Integer.toString(SecConst.ALLCAS));
        profile.setValue(EndEntityProfile.AVAILCERTPROFILES,0,Integer.toString(cprofile));
        rasession.addEndEntityProfile(admin, "TESTVALOVERRIDE", profile);
        int eeprofile = rasession.getEndEntityProfileId(admin, "TESTVALOVERRIDE");
        try {
            // Change a user that we know...
            usersession.changeUser(admin, "foo", "foo123", "C=SE,CN=validityoverride",
                    null,
                    "foo@anatom.nu", false,
                    eeprofile,
                    cprofile,
                    SecConst.USER_ENDUSER,
                    SecConst.TOKEN_SOFT_PEM, 0, UserDataConstants.STATUS_NEW, rsacaid);
            log.debug("created user: foo, foo123, C=SE, CN=validityoverride");
        } catch (RemoteException re) {
            assertTrue("User foo does not exist, or error changing user", false);
        } 
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 10);
        X509Certificate cert = (X509Certificate) remote.createCertificate(admin, "foo", "foo123", rsakeys.getPublic(), -1, null, cal.getTime());
        assertNotNull("Failed to create certificate", cert);
        String dn = cert.getSubjectDN().getName();
        assertEquals(CertTools.stringToBCDNString("cn=validityoverride,c=SE"), CertTools.stringToBCDNString(dn));
        Date notAfter = cert.getNotAfter();
        cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 297);
        // Override was not enabled, the cert should have notAfter more than 3064 days in the future (3065 to be exact)
        assertTrue(notAfter.compareTo(cal.getTime()) > 0);
        cal.add(Calendar.DAY_OF_MONTH, 2);
        // Override was not enabled, the cert should have notAfter more than 3064 days in the future (3065 to be exact)
        assertTrue(notAfter.compareTo(cal.getTime()) < 0);
        
        // Change so that we allow override of validity time
        CertificateProfile prof = storesession.getCertificateProfile(admin,cprofile);
        prof.setAllowValidityOverride(true);
        prof.setValidity(3065);
        storesession.changeCertificateProfile(admin, "TESTVALOVERRIDE", prof);
        cal = Calendar.getInstance();
        Calendar notBefore = Calendar.getInstance();
        notBefore.add(Calendar.DAY_OF_MONTH, 2);
        cal.add(Calendar.DAY_OF_MONTH, 10);
        usersession.setUserStatus(admin, "foo", UserDataConstants.STATUS_NEW);
        cert = (X509Certificate) remote.createCertificate(admin, "foo", "foo123", rsakeys.getPublic(), -1, notBefore.getTime(), cal.getTime());
        assertNotNull("Failed to create certificate", cert);
        assertEquals(CertTools.stringToBCDNString("cn=validityoverride,c=SE"), CertTools.stringToBCDNString(dn));
        notAfter = cert.getNotAfter();
        cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 11);
        // Override was enabled, the cert should have notAfter less than 11 days in the future (10 to be exact)
        assertTrue(notAfter.compareTo(cal.getTime()) < 0);
        notAfter= cert.getNotBefore();
        cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        // Override was enabled, the cert should have notBefore more than 1 days in the future (2 to be exact)
        assertTrue(notAfter.compareTo(cal.getTime()) > 0);
        cal.add(Calendar.DAY_OF_MONTH, 2);
        assertTrue(notAfter.compareTo(cal.getTime()) < 0);
        
        

        // Clean up
        rasession.removeEndEntityProfile(admin, "TESTVALOVERRIDE");
        storesession.removeCertificateProfile(admin,"TESTVALOVERRIDE");

        log.debug("<test11TestValidityOverride()");        
    }

    /**
     * creates cert
     *
     * @throws Exception if en error occurs...
     */
    public void test12SignSessionECDSAWithRSACA() throws Exception {
        log.debug(">test12SignSessionECDSAWithRSACA()");

        usersession.setUserStatus(admin,"foo",UserDataConstants.STATUS_NEW);
        log.debug("Reset status of 'foo' to NEW");
        // user that we know exists...
    	X509Certificate selfcert = CertTools.genSelfCert("CN=selfsigned", 1, null, ecdsakeys.getPrivate(), ecdsakeys.getPublic(), CATokenConstants.SIGALG_SHA256_WITH_ECDSA, false);
        X509Certificate cert = (X509Certificate) remote.createCertificate(admin, "foo", "foo123", selfcert);
        assertNotNull("Misslyckades skapa cert", cert);
        log.debug("Cert=" + cert.toString());
        PublicKey pk = cert.getPublicKey();
        if (pk instanceof JCEECPublicKey) {
			JCEECPublicKey ecpk = (JCEECPublicKey) pk;
			assertEquals(ecpk.getAlgorithm(), "EC");
			org.bouncycastle.jce.spec.ECParameterSpec spec = ecpk.getParameters();
			assertNotNull("ImplicitlyCA must have null spec", spec);
		} else {
			assertTrue("Public key is not EC", false);
		}
        try {
            cert.verify(rsacacert.getPublicKey());        	
        } catch (Exception e) {
        	assertTrue("Verify failed: "+e.getMessage(), false);
        }

        //FileOutputStream fos = new FileOutputStream("testcert.crt");
        //fos.write(cert.getEncoded());
        //fos.close();
        log.debug("<test12SignSessionECDSAWithRSACA()");
    }

    /**
     * tests bouncy PKCS10
     *
     * @throws Exception if en error occurs...
     */
    public void test13TestBCPKCS10ECDSAWithRSACA() throws Exception {
        log.debug(">test13TestBCPKCS10ECDSAWithRSACA()");
        usersession.setUserStatus(admin,"foo",UserDataConstants.STATUS_NEW);
        log.debug("Reset status of 'foo' to NEW");
        // Create certificate request
        PKCS10CertificationRequest req = new PKCS10CertificationRequest("SHA256WithECDSA",
                CertTools.stringToBcX509Name("C=SE, O=AnaTom, CN=foo"), ecdsakeys.getPublic(), null,
                ecdsakeys.getPrivate());
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        DEROutputStream dOut = new DEROutputStream(bOut);
        dOut.writeObject(req);
        dOut.close();

        PKCS10CertificationRequest req2 = new PKCS10CertificationRequest(bOut.toByteArray());
        boolean verify = req2.verify();
        log.debug("Verify returned " + verify);
        if (verify == false) {
            log.debug("Aborting!");
            return;
        }
        log.debug("CertificationRequest generated successfully.");
        byte[] bcp10 = bOut.toByteArray();
        PKCS10RequestMessage p10 = new PKCS10RequestMessage(bcp10);
        p10.setUsername("foo");
        p10.setPassword("foo123");
        IResponseMessage resp = remote.createCertificate(admin,
                p10, Class.forName("org.ejbca.core.protocol.X509ResponseMessage"));
        X509Certificate cert = CertTools.getCertfromByteArray(resp.getResponseMessage());
        assertNotNull("Failed to create certificate", cert);
        log.debug("Cert=" + cert.toString());
        PublicKey pk = cert.getPublicKey();
        if (pk instanceof JCEECPublicKey) {
			JCEECPublicKey ecpk = (JCEECPublicKey) pk;
			assertEquals(ecpk.getAlgorithm(), "EC");
			org.bouncycastle.jce.spec.ECParameterSpec spec = ecpk.getParameters();
			assertNotNull("ImplicitlyCA must have null spec", spec);
		} else {
			assertTrue("Public key is not EC", false);
		}
        try {
            cert.verify(rsacacert.getPublicKey());        	
        } catch (Exception e) {
        	assertTrue("Verify failed: "+e.getMessage(), false);
        }
        log.debug("<test13TestBCPKCS10ECDSAWithRSACA()");
    }

    /**
     * creates cert
     *
     * @throws Exception if en error occurs...
     */
    public void test14SignSessionECDSAWithECDSACA() throws Exception {
        log.debug(">test14SignSessionECDSAWithECDSACA()");

        usersession.setUserStatus(admin,"fooecdsa",UserDataConstants.STATUS_NEW);
        log.debug("Reset status of 'fooecdsa' to NEW");
        // user that we know exists...
    	X509Certificate selfcert = CertTools.genSelfCert("CN=selfsigned", 1, null, ecdsakeys.getPrivate(), ecdsakeys.getPublic(), CATokenConstants.SIGALG_SHA256_WITH_ECDSA, false);
        X509Certificate cert = (X509Certificate) remote.createCertificate(admin, "fooecdsa", "foo123", selfcert);
        assertNotNull("Misslyckades skapa cert", cert);
        log.debug("Cert=" + cert.toString());
        PublicKey pk = cert.getPublicKey();
        if (pk instanceof JCEECPublicKey) {
			JCEECPublicKey ecpk = (JCEECPublicKey) pk;
			assertEquals(ecpk.getAlgorithm(), "EC");
			org.bouncycastle.jce.spec.ECParameterSpec spec = ecpk.getParameters();
			assertNotNull("ImplicitlyCA must have null spec", spec);
		} else {
			assertTrue("Public key is not EC", false);
		}
        try {
            cert.verify(ecdsacacert.getPublicKey());        	
        } catch (Exception e) {
        	assertTrue("Verify failed: "+e.getMessage(), false);
        }

        //FileOutputStream fos = new FileOutputStream("testcert.crt");
        //fos.write(cert.getEncoded());
        //fos.close();
        log.debug("<test14SignSessionECDSAWithECDSACA()");
    }

    /**
     * tests bouncy PKCS10
     *
     * @throws Exception if en error occurs...
     */
    public void test15TestBCPKCS10ECDSAWithECDSACA() throws Exception {
        log.debug(">test15TestBCPKCS10ECDSAWithECDSACA()");
        usersession.setUserStatus(admin,"fooecdsa",UserDataConstants.STATUS_NEW);
        log.debug("Reset status of 'foo' to NEW");
        // Create certificate request
        PKCS10CertificationRequest req = new PKCS10CertificationRequest("SHA256WithECDSA",
                CertTools.stringToBcX509Name("C=SE, O=AnaTom, CN=fooecdsa"), ecdsakeys.getPublic(), null,
                ecdsakeys.getPrivate());
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        DEROutputStream dOut = new DEROutputStream(bOut);
        dOut.writeObject(req);
        dOut.close();

        PKCS10CertificationRequest req2 = new PKCS10CertificationRequest(bOut.toByteArray());
        boolean verify = req2.verify();
        log.debug("Verify returned " + verify);
        if (verify == false) {
            log.debug("Aborting!");
            return;
        }
        log.debug("CertificationRequest generated successfully.");
        byte[] bcp10 = bOut.toByteArray();
        PKCS10RequestMessage p10 = new PKCS10RequestMessage(bcp10);
        p10.setUsername("fooecdsa");
        p10.setPassword("foo123");
        IResponseMessage resp = remote.createCertificate(admin,
                p10, Class.forName("org.ejbca.core.protocol.X509ResponseMessage"));
        X509Certificate cert = CertTools.getCertfromByteArray(resp.getResponseMessage());
        assertNotNull("Failed to create certificate", cert);
        log.debug("Cert=" + cert.toString());
        PublicKey pk = cert.getPublicKey();
        if (pk instanceof JCEECPublicKey) {
			JCEECPublicKey ecpk = (JCEECPublicKey) pk;
			assertEquals(ecpk.getAlgorithm(), "EC");
			org.bouncycastle.jce.spec.ECParameterSpec spec = ecpk.getParameters();
			assertNotNull("ImplicitlyCA must have null spec", spec);
		} else {
			assertTrue("Public key is not EC", false);
		}
        try {
            cert.verify(ecdsacacert.getPublicKey());        	
        } catch (Exception e) {
        	assertTrue("Verify failed: "+e.getMessage(), false);
        }
        log.debug("<test15TestBCPKCS10ECDSAWithECDSACA()");
    }
    /**
     * creates cert
     *
     * @throws Exception if en error occurs...
     */
    public void test16SignSessionECDSAWithECDSAImplicitlyCACA() throws Exception {
        log.debug(">test16SignSessionECDSAWithECDSAImplicitlyCACA()");

        usersession.setUserStatus(admin,"fooecdsaimpca",UserDataConstants.STATUS_NEW);
        log.debug("Reset status of 'fooecdsaimpca' to NEW");
        // user that we know exists...
    	X509Certificate selfcert = CertTools.genSelfCert("CN=selfsigned", 1, null, ecdsakeys.getPrivate(), ecdsakeys.getPublic(), CATokenConstants.SIGALG_SHA256_WITH_ECDSA, false);
        X509Certificate cert = (X509Certificate) remote.createCertificate(admin, "fooecdsaimpca", "foo123", selfcert);
        assertNotNull("Misslyckades skapa cert", cert);
        log.debug("Cert=" + cert.toString());
        PublicKey pk = cert.getPublicKey();
        if (pk instanceof JCEECPublicKey) {
			JCEECPublicKey ecpk = (JCEECPublicKey) pk;
			assertEquals(ecpk.getAlgorithm(), "EC");
			org.bouncycastle.jce.spec.ECParameterSpec spec = ecpk.getParameters();
			assertNotNull("ImplicitlyCA must have null spec", spec);
		} else {
			assertTrue("Public key is not EC", false);
		}
        try {
            cert.verify(ecdsaimplicitlycacacert.getPublicKey());        	
        } catch (Exception e) {
        	assertTrue("Verify failed: "+e.getMessage(), false);
        }

        //FileOutputStream fos = new FileOutputStream("testcert.crt");
        //fos.write(cert.getEncoded());
        //fos.close();
        log.debug("<test16SignSessionECDSAWithECDSAImplicitlyCACA()");
    }

    /**
     * tests bouncy PKCS10
     *
     * @throws Exception if en error occurs...
     */
    public void test17TestBCPKCS10ECDSAWithECDSAImplicitlyCACA() throws Exception {
        log.debug(">test17TestBCPKCS10ECDSAWithECDSAImplicitlyCACA()");
        usersession.setUserStatus(admin,"fooecdsaimpca",UserDataConstants.STATUS_NEW);
        log.debug("Reset status of 'foo' to NEW");
        // Create certificate request
        PKCS10CertificationRequest req = new PKCS10CertificationRequest("SHA256WithECDSA",
                CertTools.stringToBcX509Name("C=SE, O=AnaTom, CN=fooecdsaimpca"), ecdsakeys.getPublic(), null,
                ecdsakeys.getPrivate());
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        DEROutputStream dOut = new DEROutputStream(bOut);
        dOut.writeObject(req);
        dOut.close();

        PKCS10CertificationRequest req2 = new PKCS10CertificationRequest(bOut.toByteArray());
        boolean verify = req2.verify();
        log.debug("Verify returned " + verify);
        if (verify == false) {
            log.debug("Aborting!");
            return;
        }
        log.debug("CertificationRequest generated successfully.");
        byte[] bcp10 = bOut.toByteArray();
        PKCS10RequestMessage p10 = new PKCS10RequestMessage(bcp10);
        p10.setUsername("fooecdsaimpca");
        p10.setPassword("foo123");
        IResponseMessage resp = remote.createCertificate(admin,
                p10, Class.forName("org.ejbca.core.protocol.X509ResponseMessage"));
        X509Certificate cert = CertTools.getCertfromByteArray(resp.getResponseMessage());
        assertNotNull("Failed to create certificate", cert);
        log.debug("Cert=" + cert.toString());
        PublicKey pk = cert.getPublicKey();
        if (pk instanceof JCEECPublicKey) {
			JCEECPublicKey ecpk = (JCEECPublicKey) pk;
			assertEquals(ecpk.getAlgorithm(), "EC");
			org.bouncycastle.jce.spec.ECParameterSpec spec = ecpk.getParameters();
			assertNotNull("ImplicitlyCA must have null spec", spec);
		} else {
			assertTrue("Public key is not EC", false);
		}
        try {
            cert.verify(ecdsaimplicitlycacacert.getPublicKey());        	
        } catch (Exception e) {
        	assertTrue("Verify failed: "+e.getMessage(), false);
        }
        log.debug("<test17TestBCPKCS10ECDSAWithECDSAImplicitlyCACA()");
    }

    /**
     * creates new user
     *
     * @throws Exception if en error occurs...
     */
    public void test99DeleteUsers() throws Exception {
        log.debug(">test99DeleteUsers()");

        // delete users that we know...
        usersession.deleteUser(admin, "foo");
        log.debug("deleted user: foo, foo123, C=SE, O=AnaTom, CN=foo");
        usersession.deleteUser(admin, "fooecdsa");
        log.debug("deleted user: fooecdsa, foo123, C=SE, O=AnaTom, CN=foo");
        usersession.deleteUser(admin, "fooecdsaimpca");
        log.debug("deleted user: fooecdsaimpca, foo123, C=SE, O=AnaTom, CN=foo");

        log.debug("<test99DeleteUsers()");
    }
    
    /**
     * Tests scep message
     */
/*
    public void test10TestOpenScep() throws Exception {
        log.debug(">test10TestOpenScep()");
        UserDataPK pk = new UserDataPK("foo");
        UserDataRemote data = userhome.findByPrimaryKey(pk);
        data.setStatus(UserDataRemote.STATUS_NEW);
        log.debug("Reset status of 'foo' to NEW");
        IResponseMessage resp = remote.createCertificate(admin, new ScepRequestMessage(openscep), -1, Class.forName("org.ejbca.core.protocol.ScepResponseMessage"));
        assertNotNull("Failed to create certificate", resp);
        byte[] msg = resp.getResponseMessage();
        log.debug("Message: "+new String(Base64.encode(msg,true)));
        assertNotNull("Failed to get encoded response message", msg);
        log.debug("<test10TestOpenScep()");
    }
*/
}
