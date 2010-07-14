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

package org.ejbca.core.ejb.ra;

import java.rmi.RemoteException;
import java.rmi.ServerException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

import javax.ejb.DuplicateKeyException;
import javax.transaction.TransactionRolledbackException;

import org.apache.log4j.Logger;
import org.ejbca.core.EjbcaException;
import org.ejbca.core.ErrorCode;
import org.ejbca.core.ejb.ca.CaTestCase;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.approval.WaitingForApprovalException;
import org.ejbca.core.model.authorization.AuthorizationDeniedException;
import org.ejbca.core.model.ca.caadmin.CADoesntExistsException;
import org.ejbca.core.model.ca.caadmin.CAInfo;
import org.ejbca.core.model.log.Admin;
import org.ejbca.core.model.ra.NotFoundException;
import org.ejbca.core.model.ra.UserDataConstants;
import org.ejbca.core.model.ra.UserDataVO;
import org.ejbca.core.model.ra.raadmin.EndEntityProfile;
import org.ejbca.core.model.ra.raadmin.UserDoesntFullfillEndEntityProfile;
import org.ejbca.util.TestTools;
import org.ejbca.util.dn.DnComponents;

/** Tests the UserData entity bean and some parts of UserAdminSession.
 *
 * @version $Id$
 */
public class UserAdminSessionTest extends CaTestCase {

    private static final Logger log = Logger.getLogger(UserAdminSessionTest.class);
    private static final Admin admin = new Admin(Admin.TYPE_INTERNALUSER);
    private int caid = getTestCAId();

    private static String username;
    private static String pwd;
    private static ArrayList usernames = new ArrayList();
    private static String serialnumber;

    /**
     * Creates a new TestUserData object.
     *
     * @param name DOCUMENT ME!
     */
    public UserAdminSessionTest(String name) {
        super(name);
        assertTrue("Could not create TestCA.", createTestCA());
    }

    public void setUp() throws Exception {
    }

    public void tearDown() throws Exception {
    }

    private void genRandomSerialnumber() throws Exception {
        // Gen random number
        Random rand = new Random(new Date().getTime() + 4913);
        serialnumber = "";
        for (int i = 0; i < 8; i++) {
            int randint = rand.nextInt(9);
            serialnumber += (new Integer(randint)).toString();
        }
        log.debug("Generated random serialnumber: serialnumber =" + serialnumber);

    } // genRandomSerialnumber

    /**
     * tests creation of new user and duplicate user
     *
     * @throws Exception error
     */
    public void test01AddUser() throws Exception {
        log.trace(">test01AddUser()");

        // Make user that we know later...
        username = genRandomUserName();
        pwd = genRandomPwd();
        String email = username + "@anatom.se";
        TestTools.getUserAdminSession().addUser(admin, username, pwd, "C=SE, O=AnaTom, CN=" + username, "rfc822name=" + email, email, true, SecConst.EMPTY_ENDENTITYPROFILE, SecConst.CERTPROFILE_FIXED_ENDUSER, SecConst.USER_ENDUSER, SecConst.TOKEN_SOFT_P12, 0, caid);
		usernames.add(username);
        log.debug("created user: " + username + ", " + pwd + ", C=SE, O=AnaTom, CN=" + username);
        // Add the same user again
        boolean userexists = false;
        try {
            TestTools.getUserAdminSession().addUser(admin, username, pwd, "C=SE, O=AnaTom, CN=" + username, "rfc822name=" + email, email, true, SecConst.EMPTY_ENDENTITYPROFILE, SecConst.CERTPROFILE_FIXED_ENDUSER, SecConst.USER_ENDUSER, SecConst.TOKEN_SOFT_P12, 0, caid);
        } catch (DuplicateKeyException e) {
            // This is what we want
            userexists = true;
        } catch (TransactionRolledbackException e) {
        	// weblogic throws transactionrolledbackexception instead wrapping the duplicatekey ex
        	if (e.getCause() instanceof DuplicateKeyException) {
                userexists = true;
			}
        } catch (ServerException e) {
        	// glassfish throws serverexception, can you believe this?
        	userexists = true;
        }
        assertTrue("User already exist does not throw DuplicateKeyException", userexists);

        // try to add user with non-existing CA-id
        String username2 = genRandomUserName();
        int fakecaid = -1;
        boolean thrown = false;
        try {
        	TestTools.getUserAdminSession().addUser(admin, username2, pwd, "C=SE, O=AnaTom, CN=" + username2, null, null, true, SecConst.EMPTY_ENDENTITYPROFILE, SecConst.CERTPROFILE_FIXED_ENDUSER, SecConst.USER_ENDUSER, SecConst.TOKEN_SOFT_P12, 0, fakecaid);
        	assertTrue(false);
        } catch (CADoesntExistsException e) {
        	thrown = true;
        }
    	assertTrue(thrown);
        
        log.trace("<test01AddUser()");
    }

    
    /**
     * tests creation of new user with unique serialnumber
     *
     * @throws Exception error
     */
    public void test02AddUserWithUniqueDNSerialnumber() throws Exception {
        log.trace(">test02AddUserWithUniqueDNSerialnumber()");

        // Make user that we know later...
        String thisusername = genRandomUserName();
        String email = thisusername + "@anatom.se";
        genRandomSerialnumber();
        TestTools.getUserAdminSession().addUser(admin, thisusername, pwd, "C=SE, CN=" + thisusername + ", SN=" + serialnumber, "rfc822name=" + email, email, false, SecConst.EMPTY_ENDENTITYPROFILE, SecConst.CERTPROFILE_FIXED_ENDUSER, SecConst.USER_ENDUSER, SecConst.TOKEN_SOFT_P12, 0, caid);
        assertTrue("User " + thisusername + " was not added to the database.", TestTools.getUserAdminSession().existsUser(admin, thisusername));
        usernames.add(thisusername);
        
        //Set the CA to enforce unique subjectDN serialnumber
        CAInfo cainfo = TestTools.getCAAdminSession().getCA(admin, caid).getCAInfo();
        boolean requiredUniqueSerialnumber = cainfo.isDoEnforceUniqueSubjectDNSerialnumber();
        cainfo.setDoEnforceUniqueSubjectDNSerialnumber(true);
        TestTools.getCAAdminSession().editCA(admin, cainfo);
               
        // Add another user with the same serialnumber
        thisusername = genRandomUserName();
        try {
        	TestTools.getUserAdminSession().addUser(admin, thisusername, pwd, "C=SE, CN=" + thisusername + ", SN=" + serialnumber, "rfc822name=" + email, email, false, SecConst.EMPTY_ENDENTITYPROFILE, SecConst.CERTPROFILE_FIXED_ENDUSER, SecConst.USER_ENDUSER, SecConst.TOKEN_SOFT_P12, 0, caid);
            usernames.add(thisusername);
        } catch (EjbcaException e){
        	assertEquals(ErrorCode.SUBJECTDN_SERIALNUMBER_ALREADY_EXISTS, e.getErrorCode());
        }
        assertFalse(TestTools.getUserAdminSession().existsUser(admin, thisusername));
        
        //Set the CA to NOT enforcing unique subjectDN serialnumber
		cainfo.setDoEnforceUniqueSubjectDNSerialnumber(false);
		TestTools.getCAAdminSession().editCA(admin, cainfo);
		TestTools.getUserAdminSession().addUser(admin, thisusername, pwd, "C=SE, CN=" + thisusername + ", SN=" + serialnumber, "rfc822name=" + email, email, false, SecConst.EMPTY_ENDENTITYPROFILE, SecConst.CERTPROFILE_FIXED_ENDUSER, SecConst.USER_ENDUSER, SecConst.TOKEN_SOFT_P12, 0, caid);
		assertTrue(TestTools.getUserAdminSession().existsUser(admin, thisusername));
		usernames.add(thisusername);
		
        //Set the CA back to its original settings of enforcing unique subjectDN serialnumber.
		cainfo.setDoEnforceUniqueSubjectDNSerialnumber(requiredUniqueSerialnumber);
		TestTools.getCAAdminSession().editCA(admin, cainfo);

        log.trace("<test02AddUserWithUniqueDNSerialnumber()");
    }    
    
    public void test03ChangeUserWithUniqueDNSerialnumber() throws RemoteException, AuthorizationDeniedException, UserDoesntFullfillEndEntityProfile, WaitingForApprovalException, EjbcaException{
        log.trace(">test03ChangeUserWithUniqueDNSerialnumber()");

        // Make user that we know later...
        String thisusername;
        if (usernames.size() > 1) {
        	thisusername = (String) usernames.get(1);
        } else {
        	thisusername = username;
        }
        String email = thisusername + username + "@anatomanatom.se";
        
        CAInfo cainfo = TestTools.getCAAdminSession().getCA(admin, caid).getCAInfo();
        boolean requiredUniqueSerialnumber = cainfo.isDoEnforceUniqueSubjectDNSerialnumber();
        
        //Set the CA to enforce unique serialnumber
        cainfo.setDoEnforceUniqueSubjectDNSerialnumber(true);
        TestTools.getCAAdminSession().editCA(admin, cainfo);
        try{
        	TestTools.getUserAdminSession().changeUser(admin, thisusername, pwd, "C=SE, CN=" + thisusername + ", SN=" + serialnumber, "rfc822name=" + email, email, false, SecConst.EMPTY_ENDENTITYPROFILE, SecConst.CERTPROFILE_FIXED_ENDUSER, SecConst.USER_ENDUSER, SecConst.TOKEN_SOFT_P12, 0, UserDataConstants.STATUS_NEW, caid);
        } catch(EjbcaException e){
        	assertEquals(ErrorCode.SUBJECTDN_SERIALNUMBER_ALREADY_EXISTS, e.getErrorCode());
        }
        assertTrue("The user '" + thisusername + "' was changed eventhough the serialnumber already exists.", TestTools.getUserAdminSession().findUserByEmail(admin, email).size()==0);
        
        //Set the CA to NOT enforcing unique subjectDN serialnumber
		cainfo.setDoEnforceUniqueSubjectDNSerialnumber(false);
		TestTools.getCAAdminSession().editCA(admin, cainfo);
		TestTools.getUserAdminSession().changeUser(admin, thisusername, pwd, "C=SE, CN=" + thisusername + ", SN=" + serialnumber, "rfc822name=" + email, email, false, SecConst.EMPTY_ENDENTITYPROFILE, SecConst.CERTPROFILE_FIXED_ENDUSER, SecConst.USER_ENDUSER, SecConst.TOKEN_SOFT_P12, 0, UserDataConstants.STATUS_NEW, caid);
		assertTrue("The user '" + thisusername + "' was not changed even though unique serialnumber is not enforced", TestTools.getUserAdminSession().findUserByEmail(admin, email).size()>0);
		
        //Set the CA back to its original settings of enforcing unique subjectDN serialnumber.
		cainfo.setDoEnforceUniqueSubjectDNSerialnumber(requiredUniqueSerialnumber);
		TestTools.getCAAdminSession().editCA(admin, cainfo);

        log.trace("<test03ChangeUserWithUniqueDNSerialnumber()");
    }
    
    /**
     * tests findUser and existsUser
     *
     * @throws Exception error
     */
    public void test03FindUser() throws Exception {
    	log.trace(">test03FindUser()");
        UserDataVO data = TestTools.getUserAdminSession().findUser(admin, username);
        assertNotNull(data);
        assertEquals(username, data.getUsername());
        boolean exists = TestTools.getUserAdminSession().existsUser(admin, username);
        assertTrue(exists);
        
        String notexistusername = genRandomUserName();
        exists = TestTools.getUserAdminSession().existsUser(admin, notexistusername);
        assertFalse(exists);
        data = TestTools.getUserAdminSession().findUser(admin, notexistusername);
        assertNull(data);
        log.trace("<test03FindUser()");
    }

    /**
     * tests changeUser
     *
     * @throws Exception error
     */
    public void test04ChangeUser() throws Exception {
    	log.trace(">test04ChangeUser()");
        UserDataVO data = TestTools.getUserAdminSession().findUser(admin, username);
        assertNotNull(data);
        assertEquals(username, data.getUsername());
        assertNull(data.getCardNumber());
        assertEquals(pwd, data.getPassword()); //Note that changing the user sets the password to null!!!
        assertEquals("CN=" + username+",O=AnaTom,C=SE", data.getDN());
        String email = username + "@anatom.se";
        assertEquals("rfc822name=" + email, data.getSubjectAltName());
        data.setCardNumber("123456");
        data.setPassword("bar123");
        data.setDN("C=SE, O=AnaTom1, CN=" + username);
        data.setSubjectAltName("dnsName=a.b.se, rfc822name=" + email);

        TestTools.getUserAdminSession().changeUser(admin, data, true);
        UserDataVO data1 = TestTools.getUserAdminSession().findUser(admin, username);
        assertNotNull(data1);
        assertEquals(username, data1.getUsername());
        assertEquals("123456", data1.getCardNumber());
        assertEquals("bar123", data1.getPassword());
        assertEquals("CN=" + username+",O=AnaTom1,C=SE", data1.getDN());
        assertEquals("dnsName=a.b.se, rfc822name=" + email, data1.getSubjectAltName());
        log.trace("<test04ChangeUser()");
    }

    /**
     * tests deletion of user, and user that does not exist
     *
     * @throws Exception error
     */
    public void test05DeleteUser() throws Exception {
    	log.trace(">test05DeleteUser()");
        TestTools.getUserAdminSession().deleteUser(admin, username);
        log.debug("deleted user: " + username);
        // Delete the the same user again
        boolean removed = false;
        try {
            TestTools.getUserAdminSession().deleteUser(admin, username);
        } catch (NotFoundException e) {
            removed = true;
        }
        assertTrue("User does not exist does not throw NotFoundException", removed);
        log.trace("<test05DeleteUser()");
    }

    /**
     * tests deletion of user, and user that does not exist
     *
     * @throws Exception error
     */
    public void test06MergeWithWS() throws Exception {
        EndEntityProfile profile = new EndEntityProfile();
        profile.addField(DnComponents.COMMONNAME);
        profile.addField(DnComponents.DNEMAIL);
        profile.addField(DnComponents.ORGANIZATIONUNIT);
        profile.setUse(DnComponents.ORGANIZATIONUNIT, 0, true);
        profile.setValue(DnComponents.ORGANIZATIONUNIT, 0, "FooOrgUnit");
        profile.addField(DnComponents.ORGANIZATION);
        profile.addField(DnComponents.COUNTRY);
        profile.setValue(EndEntityProfile.AVAILCAS,0, Integer.toString(SecConst.ALLCAS));
        profile.setAllowMergeDnWebServices(true);
        
        TestTools.getRaAdminSession().addEndEntityProfile(admin, "TESTMERGEWITHWS", profile);
        int profileId = TestTools.getRaAdminSession().getEndEntityProfileId(admin, "TESTMERGEWITHWS");

        UserDataVO addUser = new UserDataVO(username, "C=SE, O=AnaTom, CN=" + username, caid, null, null, UserDataConstants.STATUS_NEW, SecConst.USER_ENDUSER, profileId, SecConst.CERTPROFILE_FIXED_ENDUSER, new Date(), new Date(), SecConst.TOKEN_SOFT_P12, 0, null);
        addUser.setPassword("foo123");
        TestTools.getUserAdminSession().addUserFromWS(admin, addUser, false);
        UserDataVO data = TestTools.getUserAdminSession().findUser(admin, username);
        assertEquals("CN="+username+",OU=FooOrgUnit,O=AnaTom,C=SE", data.getDN());

        addUser.setDN("EMAIL=foo@bar.com, OU=hoho");
        TestTools.getRaAdminSession().changeEndEntityProfile(admin, "TESTMERGEWITHWS", profile);
        TestTools.getUserAdminSession().changeUser(admin, addUser, false, true);
        data = TestTools.getUserAdminSession().findUser(admin, username);
        //E=foo@bar.com,CN=430208,OU=FooOrgUnit,O=hoho,C=NO
        assertEquals("E=foo@bar.com,CN="+username+",OU=hoho,O=AnaTom,C=SE", data.getDN());
    }

	public void test99RemoveTestCA() throws Exception {
		for(int i=0; i<usernames.size(); i++){
			try {
				TestTools.getUserAdminSession().deleteUser(admin, (String) usernames.get(i));				
			} catch (Exception e) {} // NOPMD, ignore errors so we don't stop deleting users because one of them does not exist.
		}
		try {
			TestTools.getRaAdminSession().removeEndEntityProfile(admin, "TESTMERGEWITHWS");				
		} catch (Exception e) {} // NOPMD, ignore errors so we don't stop deleting users because one of them does not exist.
		
		removeTestCA();
	}
}
