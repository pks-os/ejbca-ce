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

import java.awt.print.PrinterException;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import javax.ejb.CreateException;
import javax.ejb.DuplicateKeyException;
import javax.ejb.EJBException;
import javax.ejb.FinderException;
import javax.ejb.ObjectNotFoundException;
import javax.ejb.RemoveException;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;

import org.ejbca.core.ejb.BaseSessionBean;
import org.ejbca.core.ejb.JNDINames;
import org.ejbca.core.ejb.approval.IApprovalSessionLocal;
import org.ejbca.core.ejb.approval.IApprovalSessionLocalHome;
import org.ejbca.core.ejb.authorization.IAuthorizationSessionLocal;
import org.ejbca.core.ejb.authorization.IAuthorizationSessionLocalHome;
import org.ejbca.core.ejb.ca.caadmin.ICAAdminSessionLocal;
import org.ejbca.core.ejb.ca.caadmin.ICAAdminSessionLocalHome;
import org.ejbca.core.ejb.ca.store.ICertificateStoreSessionLocal;
import org.ejbca.core.ejb.ca.store.ICertificateStoreSessionLocalHome;
import org.ejbca.core.ejb.keyrecovery.IKeyRecoverySessionLocal;
import org.ejbca.core.ejb.keyrecovery.IKeyRecoverySessionLocalHome;
import org.ejbca.core.ejb.log.ILogSessionLocal;
import org.ejbca.core.ejb.log.ILogSessionLocalHome;
import org.ejbca.core.ejb.ra.raadmin.IRaAdminSessionLocal;
import org.ejbca.core.ejb.ra.raadmin.IRaAdminSessionLocalHome;
import org.ejbca.core.model.InternalResources;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.approval.ApprovalException;
import org.ejbca.core.model.approval.ApprovalExecutorUtil;
import org.ejbca.core.model.approval.ApprovalOveradableClassName;
import org.ejbca.core.model.approval.WaitingForApprovalException;
import org.ejbca.core.model.approval.approvalrequests.AddEndEntityApprovalRequest;
import org.ejbca.core.model.approval.approvalrequests.ChangeStatusEndEntityApprovalRequest;
import org.ejbca.core.model.approval.approvalrequests.EditEndEntityApprovalRequest;
import org.ejbca.core.model.authorization.AuthorizationDeniedException;
import org.ejbca.core.model.authorization.AvailableAccessRules;
import org.ejbca.core.model.ca.caadmin.CAInfo;
import org.ejbca.core.model.ca.certificateprofiles.CertificateProfile;
import org.ejbca.core.model.ca.crl.RevokedCertInfo;
import org.ejbca.core.model.log.Admin;
import org.ejbca.core.model.log.LogConstants;
import org.ejbca.core.model.log.LogEntry;
import org.ejbca.core.model.ra.NotFoundException;
import org.ejbca.core.model.ra.RAAuthorization;
import org.ejbca.core.model.ra.UserAdminConstants;
import org.ejbca.core.model.ra.UserDataConstants;
import org.ejbca.core.model.ra.UserDataVO;
import org.ejbca.core.model.ra.raadmin.EndEntityProfile;
import org.ejbca.core.model.ra.raadmin.GlobalConfiguration;
import org.ejbca.core.model.ra.raadmin.UserDoesntFullfillEndEntityProfile;
import org.ejbca.util.CertTools;
import org.ejbca.util.JDBCUtil;
import org.ejbca.util.NotificationParamGen;
import org.ejbca.util.PrinterManager;
import org.ejbca.util.StringTools;
import org.ejbca.util.TemplateMimeMessage;
import org.ejbca.util.query.BasicMatch;
import org.ejbca.util.query.IllegalQueryException;
import org.ejbca.util.query.Query;
import org.ejbca.util.query.UserMatch;



/**
 * Administrates users in the database using UserData Entity Bean.
 * Uses JNDI name for datasource as defined in env 'Datasource' in ejb-jar.xml.
 *
 * @version $Id: LocalUserAdminSessionBean.java,v 1.34 2007-01-03 12:27:52 anatom Exp $
 * 
 * @ejb.bean
 *   display-name="UserAdminSB"
 *   name="UserAdminSession"
 *   jndi-name="UserAdminSession"
 *   view-type="both"
 *   type="Stateless"
 *   transaction-type="Container"
 *
 * @ejb.transaction type="Required"
 *
 * @weblogic.enable-call-by-reference True
 *
 * @ejb.env-entry
 *  name="DataSource"
 *  type="java.lang.String"
 *  value="${datasource.jndi-name-prefix}${datasource.jndi-name}"
 *
 * @ejb.env-entry
 *   description="Defines the JNDI name of the mail service used"
 *   name="MailJNDIName"
 *   type="java.lang.String"
 *   value="${mail.jndi-name}"
 *
 * @ejb.env-entry
 *   description="Defines the sender of the notification message"
 *   name="sender"
 *   type="java.lang.String"
 *   value="${mail.from}"
 *
 * @ejb.env-entry
 *   description="Defines the subject used in the notification message"
 *   name="subject"
 *   type="java.lang.String"
 *   value="${mail.subject}"
 *
 * @ejb.env-entry
 *   description="Defines the actual message of the notification. Use the values $Username, $Password, $CN, $O, $OU, $C, $DATE to indicate which texts that should be replaced (Case insensitive), $NL stands for newline."
 *   name="message"
 *   type="java.lang.String"
 *   value="${mail.message}"
 *
 * @ejb.home
 *   extends="javax.ejb.EJBHome"
 *   local-extends="javax.ejb.EJBLocalHome"
 *   local-class="org.ejbca.core.ejb.ra.IUserAdminSessionLocalHome"
 *   remote-class="org.ejbca.core.ejb.ra.IUserAdminSessionHome"
 *
 * @ejb.interface
 *   extends="javax.ejb.EJBObject"
 *   local-extends="javax.ejb.EJBLocalObject"
 *   local-class="org.ejbca.core.ejb.ra.IUserAdminSessionLocal"
 *   remote-class="org.ejbca.core.ejb.ra.IUserAdminSessionRemote"
 *   
 * @ejb.ejb-external-ref
 *   description="The Certificate Store session bean"
 *   view-type="local"
 *   ref-name="ejb/CertificateStoreSessionLocal"
 *   type="Session"
 *   home="org.ejbca.core.ejb.ca.store.ICertificateStoreSessionLocalHome"
 *   business="org.ejbca.core.ejb.ca.store.ICertificateStoreSessionLocal"
 *   link="CertificateStoreSession"
 *
 * @ejb.ejb-external-ref
 *   description="The Log session bean"
 *   view-type="local"
 *   ref-name="ejb/LogSessionLocal"
 *   type="Session"
 *   home="org.ejbca.core.ejb.log.ILogSessionLocalHome"
 *   business="org.ejbca.core.ejb.log.ILogSessionLocal"
 *   link="LogSession"
 *
 * @ejb.ejb-external-ref
 *   description="The Authorization session bean"
 *   view-type="local"
 *   ref-name="ejb/AuthorizationSessionLocal"
 *   type="Session"
 *   home="org.ejbca.core.ejb.authorization.IAuthorizationSessionLocalHome"
 *   business="org.ejbca.core.ejb.authorization.IAuthorizationSessionLocal"
 *   link="AuthorizationSession"
 *
 * @ejb.ejb-external-ref
 *   description="The Ra Admin session bean"
 *   view-type="local"
 *   ref-name="ejb/RaAdminSessionLocal"
 *   type="Session"
 *   home="org.ejbca.core.ejb.ra.raadmin.IRaAdminSessionLocalHome"
 *   business="org.ejbca.core.ejb.ra.raadmin.IRaAdminSessionLocal"
 *   link="RaAdminSession"
 *
 * @ejb.ejb-external-ref
 *   description="The Key Recovery session bean"
 *   view-type="local"
 *   ref-name="ejb/KeyRecoverySessionLocal"
 *   type="Session"
 *   home="org.ejbca.core.ejb.keyrecovery.IKeyRecoverySessionLocalHome"
 *   business="org.ejbca.core.ejb.keyrecovery.IKeyRecoverySessionLocal"
 *   link="KeyRecoverySession"
 *   
 * @ejb.ejb-external-ref description="The Approval Session Bean"
 *   view-type="local"
 *   ref-name="ejb/ApprovalSessionLocal"
 *   type="Session"
 *   home="org.ejbca.core.ejb.approval.IApprovalSessionLocalHome"
 *   business="org.ejbca.core.ejb.approval.IApprovalSessionLocal"
 *   link="ApprovalSession"
 *   
 * @ejb.ejb-external-ref description="The CAAdmin Session Bean"
 *   view-type="local"
 *   ref-name="ejb/CAAdminSessionLocal"
 *   type="Session"
 *   home="org.ejbca.core.ejb.ca.caadmin.ICAAdminSessionLocalHome"
 *   business="org.ejbca.core.ejb.ca.caadmin.ICAAdminSessionLocal"
 *   link="CAAdminSession"
 *
 * @ejb.ejb-external-ref
 *   description="The User entity bean"
 *   view-type="local"
 *   ref-name="ejb/UserDataLocal"
 *   type="Entity"
 *   home="org.ejbca.core.ejb.ra.UserDataLocalHome"
 *   business="org.ejbca.core.ejb.ra.UserDataLocal"
 *   link="UserData"
 *
 * @ejb.resource-ref
 *   res-ref-name="mail/DefaultMail"
 *   res-type="javax.mail.Session"
 *   res-auth="Container"
 *
 * @weblogic.resource-description
 *   res-ref-name="mail/DefaultMail"
 *   jndi-name="EjbcaMail"
 *   
 */
public class LocalUserAdminSessionBean extends BaseSessionBean {

    /** Internal localization of logs and errors */
    private static final InternalResources intres = InternalResources.getInstance();

    /**
     * The local interface of RaAdmin Session Bean.
     */
    private IRaAdminSessionLocal raadminsession;

    /**
     * The local interface of the certificate store session bean
     */
    private ICertificateStoreSessionLocal certificatesession;

    /**
     * The local interface of the authorization session bean
     */
    private IAuthorizationSessionLocal authorizationsession;

    /**
     * The local interface of the authorization session bean
     */
    private IKeyRecoverySessionLocal keyrecoverysession;
    
    /**
     * The local interface of the caadmin session bean
     */
    private ICAAdminSessionLocal caadminsession;
    
    /**
     * The local interface of the approval session bean
     */
    private IApprovalSessionLocal approvalsession;

    /**
     * The remote interface of the log session bean
     */
    private ILogSessionLocal logsession;

    private UserDataLocalHome home = null;
    /**
     * Columns in the database used in select
     */
    private static final String USERDATA_COL = "username, subjectDN, subjectAltName, subjectEmail, status, type, clearpassword, timeCreated, timeModified, endEntityprofileId, certificateProfileId, tokenType, hardTokenIssuerId, cAId, extendedInformationData";

    /**
     * Default create for SessionBean.
     *
     * @throws CreateException if bean instance can't be created
     * @see org.ejbca.core.model.log.Admin
     */
    public void ejbCreate() throws CreateException {
        try {
            home = (UserDataLocalHome) getLocator().getLocalHome(UserDataLocalHome.COMP_NAME);

            ILogSessionLocalHome logsessionhome = (ILogSessionLocalHome) getLocator().getLocalHome(ILogSessionLocalHome.COMP_NAME);
            logsession = logsessionhome.create();

            IAuthorizationSessionLocalHome authorizationsessionhome = (IAuthorizationSessionLocalHome) getLocator().getLocalHome(IAuthorizationSessionLocalHome.COMP_NAME);
            authorizationsession = authorizationsessionhome.create();

            IRaAdminSessionLocalHome raadminsessionhome = (IRaAdminSessionLocalHome) getLocator().getLocalHome(IRaAdminSessionLocalHome.COMP_NAME);
            raadminsession = raadminsessionhome.create();

            ICertificateStoreSessionLocalHome certificatesessionhome = (ICertificateStoreSessionLocalHome) getLocator().getLocalHome(ICertificateStoreSessionLocalHome.COMP_NAME);
            certificatesession = certificatesessionhome.create();
            
            
            ICAAdminSessionLocalHome caadminsessionhome = (ICAAdminSessionLocalHome) getLocator().getLocalHome(ICAAdminSessionLocalHome.COMP_NAME);
            caadminsession = caadminsessionhome.create();
            

        } catch (Exception e) {
            error("Error creating session bean:", e);
            throw new EJBException(e);
        }

    }
    
    private IApprovalSessionLocal getApprovalSession(){
      if(approvalsession == null){
          try {
            IApprovalSessionLocalHome approvalsessionhome = (IApprovalSessionLocalHome) getLocator().getLocalHome(IApprovalSessionLocalHome.COMP_NAME);
			approvalsession = approvalsessionhome.create();
		} catch (CreateException e) {
			throw new EJBException(e);
		}  
      }
      return approvalsession;
    }

    private IKeyRecoverySessionLocal getKeyRecoverySession(){
        if(keyrecoverysession == null){
            try {
            	IKeyRecoverySessionLocalHome keyrecoverysessionhome = (IKeyRecoverySessionLocalHome) getLocator().getLocalHome(IKeyRecoverySessionLocalHome.COMP_NAME);
                keyrecoverysession = keyrecoverysessionhome.create();
  		} catch (CreateException e) {
  			throw new EJBException(e);
  		}  
        }
        return keyrecoverysession;
      }

    
    /**
     * Gets the Global Configuration from ra admin session bean-
     */
    private GlobalConfiguration getGlobalConfiguration(Admin admin) {
        return raadminsession.loadGlobalConfiguration(admin);
    }

    private boolean authorizedToCA(Admin admin, int caid) {
        boolean returnval = false;
        try {
            returnval = authorizationsession.isAuthorizedNoLog(admin, AvailableAccessRules.CAPREFIX + caid);
        } catch (AuthorizationDeniedException e) {
        }
        return returnval;
    }

    private boolean authorizedToEndEntityProfile(Admin admin, int profileid, String rights) {
        boolean returnval = false;
        try {
            if (profileid == SecConst.EMPTY_ENDENTITYPROFILE && (rights.equals(AvailableAccessRules.CREATE_RIGHTS) || rights.equals(AvailableAccessRules.EDIT_RIGHTS)))
                returnval = authorizationsession.isAuthorizedNoLog(admin, "/super_administrator");
            else
                returnval = authorizationsession.isAuthorizedNoLog(admin, AvailableAccessRules.ENDENTITYPROFILEPREFIX + profileid + rights) &&
                            authorizationsession.isAuthorizedNoLog(admin, AvailableAccessRules.REGULAR_RAFUNCTIONALITY + rights);
        } catch (AuthorizationDeniedException e) {
        }
        return returnval;
    }


    /**
     * Implements IUserAdminSession::addUser.
     * Implements a mechanism that uses UserDataEntity Bean.
     * 
     * Important, this method is old and shouldn't be used, user addUser(..UserDataVO...) instead.
     *
     * @param admin                 the administrator pwrforming the action
     * @param username              the unique username.
     * @param password              the password used for authentication.
     * @param subjectdn             the DN the subject is given in his certificate.
     * @param subjectaltname        the Subject Alternative Name to be used.
     * @param email                 the email of the subject or null.
     * @param clearpwd              true if the password will be stored in clear form in the db, otherwise it is
     *                              hashed.
     * @param endentityprofileid    the id number of the end entity profile bound to this user.
     * @param certificateprofileid  the id number of the certificate profile that should be
     *                              generated for the user.
     * @param type                  of user i.e administrator, keyrecoverable and/or sendnotification, from SecConst.USER_XX.
     * @param tokentype             the type of token to be generated, one of SecConst.TOKEN constants
     * @param hardwaretokenissuerid , if token should be hard, the id of the hard token issuer,
     *                              else 0.
     * @param caid					the CA the user should be issued from.
     * @throws WaitingForApprovalException 
     * @throws ApprovalException 
     * @ejb.interface-method
     */
    public void addUser(Admin admin, String username, String password, String subjectdn, String subjectaltname, String email, boolean clearpwd, int endentityprofileid, int certificateprofileid,
                        int type, int tokentype, int hardwaretokenissuerid, int caid)
            throws AuthorizationDeniedException, UserDoesntFullfillEndEntityProfile, DuplicateKeyException, ApprovalException, WaitingForApprovalException {
    	
    	UserDataVO userdata = new UserDataVO(username, subjectdn, caid, subjectaltname, 
    			                             email, UserDataConstants.STATUS_NEW, type, endentityprofileid, certificateprofileid,
    			                             null,null, tokentype, hardwaretokenissuerid, null);
    	userdata.setPassword(password);
    	addUser(admin, userdata, clearpwd);
    }

	private static final ApprovalOveradableClassName[] NONAPPROVABLECLASSNAMES_ADDUSER = {
		new ApprovalOveradableClassName("org.ejbca.core.model.approval.approvalrequests.AddEndEntityApprovalRequest",null),
	};
	
    /**
     * Implements IUserAdminSession::addUser.
     * Implements a mechanism that uses UserDataEntity Bean. 
     *
     * @param admin                 the administrator pwrforming the action
     * @param userdata 	            a UserDataVO object, the fields status, timecreated and timemodified will not be used.
     * @param clearpwd              true if the password will be stored in clear form in the db, otherwise it is
     *                              hashed.
     * @throws AuthorizationDeniedException if administrator isn't authorized to add user
     * @throws UserDoesntFullfillEndEntityProfile if data doesn't fullfil requirements of end entity profile 
     * @throws DuplicateKeyException if user already exists
     * @throws ApprovalException if an approval already is waiting for specified action 
     * @throws WaitingForApprovalException if approval is required and the action have been added in the approval queue.  
     * 
     * @ejb.interface-method
     */
    public void addUser(Admin admin, UserDataVO userdata, boolean clearpwd) throws AuthorizationDeniedException, UserDoesntFullfillEndEntityProfile, DuplicateKeyException, ApprovalException, WaitingForApprovalException {
        // String used in SQL so strip it
        String dn = CertTools.stringToBCDNString(userdata.getDN());
        dn = StringTools.strip(dn);
        int type = userdata.getType();
        String newpassword = userdata.getPassword();
        debug(">addUser(" + userdata.getUsername() + ", password, " + dn + ", "+ userdata.getDN() + ", " + userdata.getSubjectAltName()+", "+userdata.getEmail() + ")");
        int profileId = userdata.getEndEntityProfileId();
        String profileName = raadminsession.getEndEntityProfileName(admin, profileId);
        EndEntityProfile profile = raadminsession.getEndEntityProfile(admin, profileId);

        if (profile.useAutoGeneratedPasswd() && userdata.getPassword() == null) {
            // special case used to signal regeneraton of password
            newpassword = profile.getAutoGeneratedPasswd();
        }


        if (getGlobalConfiguration(admin).getEnableEndEntityProfileLimitations()) {
            // Check if user fulfills it's profile.
            try {
                profile.doesUserFullfillEndEntityProfile(userdata.getUsername(), userdata.getPassword(), dn, userdata.getSubjectAltName(), userdata.getExtendedinformation().getSubjectDirectoryAttributes(), userdata.getEmail(), userdata.getCertificateProfileId(), clearpwd,
                        (type & SecConst.USER_ADMINISTRATOR) != 0, (type & SecConst.USER_KEYRECOVERABLE) != 0, (type & SecConst.USER_SENDNOTIFICATION) != 0,
                        userdata.getTokenType(), userdata.getHardTokenIssuerId(), userdata.getCAId());
            } catch (UserDoesntFullfillEndEntityProfile udfp) {
                String msg = intres.getLocalizedMessage("ra.errorfullfillprofile", profileName, dn, udfp.getMessage());            	
                logsession.log(admin, userdata.getCAId(), LogEntry.MODULE_RA, new java.util.Date(), userdata.getUsername(), null, LogEntry.EVENT_ERROR_ADDEDENDENTITY, msg);
                throw new UserDoesntFullfillEndEntityProfile(udfp.getMessage());
            }

            // Check if administrator is authorized to add user.
            if (!authorizedToEndEntityProfile(admin, userdata.getEndEntityProfileId(), AvailableAccessRules.CREATE_RIGHTS)) {
                String msg = intres.getLocalizedMessage("ra.errorauthprofile", profileName);            	
                logsession.log(admin, userdata.getCAId(), LogEntry.MODULE_RA, new java.util.Date(), userdata.getUsername(), null, LogEntry.EVENT_ERROR_ADDEDENDENTITY, msg);
                throw new AuthorizationDeniedException(msg);
            }
        }

        // Check if administrator is authorized to add user to CA.
        if (!authorizedToCA(admin, userdata.getCAId())) {
            String msg = intres.getLocalizedMessage("ra.errorauthca", Integer.valueOf(userdata.getCAId()));            	
            logsession.log(admin, userdata.getCAId(), LogEntry.MODULE_RA, new java.util.Date(), userdata.getUsername(), null, LogEntry.EVENT_ERROR_ADDEDENDENTITY, msg);
            throw new AuthorizationDeniedException(msg);
        }

        // Check if approvals is required.
        int numOfApprovalsRequired = getNumOfApprovalRequired(admin, CAInfo.REQ_APPROVAL_ADDEDITENDENTITY, userdata.getCAId());
        AddEndEntityApprovalRequest ar = new AddEndEntityApprovalRequest(userdata,clearpwd,admin,null,numOfApprovalsRequired,userdata.getCAId(),userdata.getEndEntityProfileId());
        if (ApprovalExecutorUtil.requireApproval(ar, NONAPPROVABLECLASSNAMES_ADDUSER)) {       		    		
        	getApprovalSession().addApprovalRequest(admin, ar);
            String msg = intres.getLocalizedMessage("ra.approvalad");            	
        	throw new WaitingForApprovalException(msg);
        }
        
        try {
            UserDataLocal data1 = home.create(userdata.getUsername(), newpassword, dn, userdata.getCAId());
            if (userdata.getSubjectAltName() != null)
                data1.setSubjectAltName(userdata.getSubjectAltName());

            if (userdata.getEmail() != null)
                data1.setSubjectEmail(userdata.getEmail());

            data1.setType(type);
            data1.setEndEntityProfileId(userdata.getEndEntityProfileId());
            data1.setCertificateProfileId(userdata.getCertificateProfileId());
            data1.setTokenType(userdata.getTokenType());
            data1.setHardTokenIssuerId(userdata.getHardTokenIssuerId());
            data1.setExtendedInformation(userdata.getExtendedinformation());

            if (clearpwd) {
                try {
                    if (newpassword == null) {
                        data1.setClearPassword("");
                    } else {
                        data1.setOpenPassword(newpassword);
                    }
                } catch (java.security.NoSuchAlgorithmException nsae) {
                    debug("NoSuchAlgorithmException while setting password for user " + userdata.getUsername());
                    throw new EJBException(nsae);
                }
            }
            if ((type & SecConst.USER_SENDNOTIFICATION) != 0) {
                sendNotification(admin, profile, userdata.getUsername(), newpassword, dn, userdata.getEmail(), userdata.getCAId());
            }
            if ((type & SecConst.USER_PRINT) != 0) {
            	print(admin,profile,userdata);
            }
            String msg = intres.getLocalizedMessage("ra.addedentity", userdata.getUsername());            	
            logsession.log(admin, userdata.getCAId(), LogEntry.MODULE_RA, new java.util.Date(), userdata.getUsername(), null, LogEntry.EVENT_INFO_ADDEDENDENTITY, msg);

        } catch (DuplicateKeyException e) {
            String msg = intres.getLocalizedMessage("ra.errorentityexist", userdata.getUsername());            	
            logsession.log(admin, userdata.getCAId(), LogEntry.MODULE_RA, new java.util.Date(), userdata.getUsername(), null, LogEntry.EVENT_ERROR_ADDEDENDENTITY, msg);
            throw e;
        } catch (Exception e) {
            String msg = intres.getLocalizedMessage("ra.erroraddentity", userdata.getUsername());            	
            logsession.log(admin, userdata.getCAId(), LogEntry.MODULE_RA, new java.util.Date(), userdata.getUsername(), null, LogEntry.EVENT_ERROR_ADDEDENDENTITY, msg, e);
            error(msg, e);
            throw new EJBException(e);
        }

        debug("<addUser(" + userdata.getUsername() + ", password, " + dn + ", " + userdata.getEmail() + ")");
    } // addUser

    /**
     * Help method that checks the CA data config if specified action 
     * requires approvals and how many
     * @param action one of CAInfo.REQ_APPROVAL_ constants
     * @param caid of the ca to check
     * @return 0 of no approvals is required othervise the number of approvals
     */
    private int getNumOfApprovalRequired(Admin admin,int action, int caid) {
    	CAInfo cainfo = caadminsession.getCAInfo(admin, caid);
    	return ApprovalExecutorUtil.getNumOfApprovalRequired(action, cainfo);    	
	}

	/**
     * Changes data for a user in the database speciefied by username.
     * 
     * Important, this method is old and shouldn't be used, user changeUser(..UserDataVO...) instead.
     *
     * @param username              the unique username.
     * @param password              the password used for authentication.*
     * @param subjectdn             the DN the subject is given in his certificate.
     * @param subjectaltname        the Subject Alternative Name to be used.
     * @param email                 the email of the subject or null.
     * @param endentityprofileid    the id number of the end entity profile bound to this user.
     * @param certificateprofileid  the id number of the certificate profile that should be generated for the user.
     * @param type                  of user i.e administrator, keyrecoverable and/or sendnotification
     * @param tokentype             the type of token to be generated, one of SecConst.TOKEN constants
     * @param hardwaretokenissuerid if token should be hard, the id of the hard token issuer, else 0.
     * @param status 				the status of the user, from UserDataConstants.STATUS_X
     * @param caid                  the id of the CA that should be used to issue the users certificate
     * 
     * @throws AuthorizationDeniedException if administrator isn't authorized to add user
     * @throws UserDoesntFullfillEndEntityProfile if data doesn't fullfil requirements of end entity profile 
     * @throws ApprovalException if an approval already is waiting for specified action 
     * @throws WaitingForApprovalException if approval is required and the action have been added in the approval queue.
     * @throws EJBException if a communication or other error occurs.
     * @ejb.interface-method
     */
    public void changeUser(Admin admin, String username, String password, String subjectdn, String subjectaltname, String email, boolean clearpwd, int endentityprofileid, int certificateprofileid,
            int type, int tokentype, int hardwaretokenissuerid, int status, int caid)
throws AuthorizationDeniedException, UserDoesntFullfillEndEntityProfile, ApprovalException, WaitingForApprovalException {
    	UserDataVO userdata = new UserDataVO(username, subjectdn, caid, subjectaltname, 
                email, status, type, endentityprofileid, certificateprofileid,
                null,null, tokentype, hardwaretokenissuerid, null);
        
    	userdata.setPassword(password);
        changeUser(admin, userdata, clearpwd);    	
    }

	private static final ApprovalOveradableClassName[] NONAPPROVABLECLASSNAMES_CHANGEUSER = {
		new ApprovalOveradableClassName("org.ejbca.core.model.approval.approvalrequests.EditEndEntityApprovalRequest",null),
		new ApprovalOveradableClassName("se.primeKey.cardPersonalization.ra.connection.ejbca.EjbcaConnection",null)
	};

	/**
     * Implements IUserAdminSession::changeUser.. 
     *
     * @param admin                 the administrator performing the action
     * @param userdata 	            a UserDataVO object,  timecreated and timemodified will not be used.
     * @param clearpwd              true if the password will be stored in clear form in the db, otherwise it is
     *                              hashed.
     *                              
     * @throws AuthorizationDeniedException if administrator isn't authorized to add user
     * @throws UserDoesntFullfillEndEntityProfile if data doesn't fullfil requirements of end entity profile 
     * @throws ApprovalException if an approval already is waiting for specified action 
     * @throws WaitingForApprovalException if approval is required and the action have been added in the approval queue.

     * @ejb.interface-method
     */
    public void changeUser(Admin admin, UserDataVO userdata, boolean clearpwd)
            throws AuthorizationDeniedException, UserDoesntFullfillEndEntityProfile, ApprovalException, WaitingForApprovalException {
        // String used in SQL so strip it
        String dn = CertTools.stringToBCDNString(userdata.getDN());
        dn = StringTools.strip(dn);
        String newpassword = userdata.getPassword();
        int type = userdata.getType();
        boolean statuschanged = false;
        debug(">changeUser(" + userdata.getUsername() + ", " + dn + ", " + userdata.getEmail() + ")");
        int oldstatus;
        EndEntityProfile profile = raadminsession.getEndEntityProfile(admin, userdata.getEndEntityProfileId());

        if (profile.useAutoGeneratedPasswd() && userdata.getPassword() != null) {
            // special case used to signal regeneraton of password
            newpassword = profile.getAutoGeneratedPasswd();
        }

        // Check if user fulfills it's profile.
        if (getGlobalConfiguration(admin).getEnableEndEntityProfileLimitations()) {
            try {
                profile.doesUserFullfillEndEntityProfileWithoutPassword(userdata.getUsername(), dn, userdata.getSubjectAltName(), userdata.getExtendedinformation().getSubjectDirectoryAttributes(), userdata.getEmail(), userdata.getCertificateProfileId(),
                        (type & SecConst.USER_ADMINISTRATOR) != 0, (type & SecConst.USER_KEYRECOVERABLE) != 0, (type & SecConst.USER_SENDNOTIFICATION) != 0,
                        userdata.getTokenType(), userdata.getHardTokenIssuerId(), userdata.getCAId());
            } catch (UserDoesntFullfillEndEntityProfile udfp) {
                String msg = intres.getLocalizedMessage("ra.errorfullfillprofile", Integer.valueOf(userdata.getEndEntityProfileId()), dn, udfp.getMessage());            	
                logsession.log(admin, userdata.getCAId(), LogEntry.MODULE_RA, new java.util.Date(), userdata.getUsername(), null, LogEntry.EVENT_ERROR_CHANGEDENDENTITY, msg);
                throw udfp;
            }
            // Check if administrator is authorized to edit user.
            if (!authorizedToEndEntityProfile(admin, userdata.getEndEntityProfileId(), AvailableAccessRules.EDIT_RIGHTS)) {
                String msg = intres.getLocalizedMessage("ra.errorauthprofile", Integer.valueOf(userdata.getEndEntityProfileId()));            	
                logsession.log(admin, userdata.getCAId(), LogEntry.MODULE_RA, new java.util.Date(), userdata.getUsername(), null, LogEntry.EVENT_ERROR_CHANGEDENDENTITY, msg);
                throw new AuthorizationDeniedException(msg);
            }
        }

        // Check if administrator is authorized to edit user to CA.
        if (!authorizedToCA(admin, userdata.getCAId())) {
            String msg = intres.getLocalizedMessage("ra.errorauthca", Integer.valueOf(userdata.getCAId()));            	
            logsession.log(admin, userdata.getCAId(), LogEntry.MODULE_RA, new java.util.Date(), userdata.getUsername(), null, LogEntry.EVENT_ERROR_CHANGEDENDENTITY, msg);
            throw new AuthorizationDeniedException(msg);
        }
        // Check if approvals is required.
        int numOfApprovalsRequired = getNumOfApprovalRequired(admin, CAInfo.REQ_APPROVAL_ADDEDITENDENTITY, userdata.getCAId());
        if (numOfApprovalsRequired > 0){
        	UserDataVO orguserdata;
			try {
				orguserdata = findUser(admin, userdata.getUsername());
			} catch (FinderException e) {
	            String msg = intres.getLocalizedMessage("ra.errorentitynotexist", userdata.getUsername());            	
				throw new ApprovalException(msg);
			}        	        	
			EditEndEntityApprovalRequest ar = new EditEndEntityApprovalRequest(userdata, clearpwd, orguserdata, admin,null,numOfApprovalsRequired,userdata.getCAId(),userdata.getEndEntityProfileId());
			if (ApprovalExecutorUtil.requireApproval(ar, NONAPPROVABLECLASSNAMES_CHANGEUSER)){       		    		
				getApprovalSession().addApprovalRequest(admin, ar);
	            String msg = intres.getLocalizedMessage("ra.approvaledit");            	
				throw new WaitingForApprovalException(msg);
			}

        }   
        
        
        try {
            UserDataPK pk = new UserDataPK(userdata.getUsername());
            UserDataLocal data1 = home.findByPrimaryKey(pk);
            data1.setDN(dn);
            if (userdata.getSubjectAltName() != null)
                data1.setSubjectAltName(userdata.getSubjectAltName());
            if (userdata.getEmail() != null)
                data1.setSubjectEmail(userdata.getEmail());
            data1.setCaId(userdata.getCAId());
            data1.setType(type);
            data1.setEndEntityProfileId(userdata.getEndEntityProfileId());
            data1.setCertificateProfileId(userdata.getCertificateProfileId());
            data1.setTokenType(userdata.getTokenType());
            data1.setHardTokenIssuerId(userdata.getHardTokenIssuerId());
            data1.setExtendedInformation(userdata.getExtendedinformation());
            oldstatus = data1.getStatus();
            if(oldstatus == UserDataConstants.STATUS_KEYRECOVERY && !(userdata.getStatus() == UserDataConstants.STATUS_KEYRECOVERY || userdata.getStatus() == UserDataConstants.STATUS_INPROCESS)){
              getKeyRecoverySession().unmarkUser(admin,userdata.getUsername());	
            }
            statuschanged = userdata.getStatus() != oldstatus;
            data1.setStatus(userdata.getStatus());
            data1.setTimeModified((new java.util.Date()).getTime());

            if(newpassword != null){
                if(clearpwd) {
                    try {
                        data1.setOpenPassword(newpassword);
                    } catch (java.security.NoSuchAlgorithmException nsae) {
                        debug("NoSuchAlgorithmException while setting password for user "+userdata.getUsername());
                        throw new EJBException(nsae);
                    }
                } else {
                    data1.setPassword(newpassword);
                }
            }

            if ((type & SecConst.USER_SENDNOTIFICATION) != 0 && statuschanged && (userdata.getStatus() == UserDataConstants.STATUS_NEW || userdata.getStatus() == UserDataConstants.STATUS_KEYRECOVERY || userdata.getStatus() == UserDataConstants.STATUS_INITIALIZED)) {

                sendNotification(admin, profile, userdata.getUsername(), newpassword, dn, userdata.getEmail(), userdata.getCAId());
            }
            if ((type & SecConst.USER_PRINT) != 0 && statuschanged && (userdata.getStatus() == UserDataConstants.STATUS_NEW || userdata.getStatus() == UserDataConstants.STATUS_KEYRECOVERY || userdata.getStatus() == UserDataConstants.STATUS_INITIALIZED)) {
            	print(admin,profile,userdata);
            }
            if (statuschanged) {
                String msg = intres.getLocalizedMessage("ra.editedentitystatus", userdata.getUsername(), Integer.valueOf(userdata.getStatus()));            	
                logsession.log(admin, userdata.getCAId(), LogEntry.MODULE_RA, new java.util.Date(), userdata.getUsername(), null, LogEntry.EVENT_INFO_CHANGEDENDENTITY, msg );
            } else {
                String msg = intres.getLocalizedMessage("ra.editedentity", userdata.getUsername());            	
                logsession.log(admin, userdata.getCAId(), LogEntry.MODULE_RA, new java.util.Date(), userdata.getUsername(), null, LogEntry.EVENT_INFO_CHANGEDENDENTITY, msg);
            }
        } catch (Exception e) {
            String msg = intres.getLocalizedMessage("ra.erroreditentity", userdata.getUsername());            	
            logsession.log(admin, userdata.getCAId(), LogEntry.MODULE_RA, new java.util.Date(), userdata.getUsername(), null, LogEntry.EVENT_ERROR_CHANGEDENDENTITY, msg);
            error("ChangeUser:", e);
            throw new EJBException(e);
        }
        debug("<changeUser(" + userdata.getUsername() + ", password, " + dn + ", " + userdata.getEmail() + ")");
    } // changeUser


    /**
     * Deletes a user from the database. The users certificates must be revoked BEFORE this method is called.
     *
     * @param username the unique username.
     * @throws NotFoundException if the user does not exist
     * @throws RemoveException   if the user could not be removed
     * @ejb.interface-method
     */
    public void deleteUser(Admin admin, String username) throws AuthorizationDeniedException, NotFoundException, RemoveException {
        debug(">deleteUser(" + username + ")");
        // Check if administrator is authorized to delete user.
        int caid = LogConstants.INTERNALCAID;
        try {
            UserDataPK pk = new UserDataPK(username);
            UserDataLocal data1 = home.findByPrimaryKey(pk);
            caid = data1.getCaId();

            if (!authorizedToCA(admin, caid)) {
                String msg = intres.getLocalizedMessage("ra.errorauthca", Integer.valueOf(caid));            	
                logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_ERROR_DELETEENDENTITY, msg);
                throw new AuthorizationDeniedException(msg);
            }

            if (getGlobalConfiguration(admin).getEnableEndEntityProfileLimitations()) {
                if (!authorizedToEndEntityProfile(admin, data1.getEndEntityProfileId(), AvailableAccessRules.DELETE_RIGHTS)) {
                    String msg = intres.getLocalizedMessage("ra.errorauthprofile", Integer.valueOf(data1.getEndEntityProfileId()));            	
                    logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_ERROR_DELETEENDENTITY, msg);
                    throw new AuthorizationDeniedException(msg);
                }
            }
        } catch (FinderException e) {
            String msg = intres.getLocalizedMessage("ra.errorentitynotexist", username);            	
            logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_ERROR_DELETEENDENTITY, msg);
            throw new NotFoundException(msg);
        }
        try {
            UserDataPK pk = new UserDataPK(username);
            home.remove(pk);
            String msg = intres.getLocalizedMessage("ra.removedentity", username);            	
            logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_INFO_DELETEDENDENTITY, msg);
        } catch (EJBException e) {
            String msg = intres.getLocalizedMessage("ra.errorremoveentity", username);            	
            logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_ERROR_DELETEENDENTITY, msg);
            throw new RemoveException(msg);
        }
        debug("<deleteUser(" + username + ")");
    } // deleteUser

	private static final ApprovalOveradableClassName[] NONAPPROVABLECLASSNAMES_SETUSERSTATUS = {
		new ApprovalOveradableClassName("org.ejbca.core.model.approval.approvalrequests.ChangeStatusEndEntityApprovalRequest",null),
		new ApprovalOveradableClassName("org.ejbca.core.ejb.ra.LocalUserAdminSessionBean","revokeUser"),
		new ApprovalOveradableClassName("org.ejbca.core.ejb.ra.LocalUserAdminSessionBean","revokeCert"),
		new ApprovalOveradableClassName("org.ejbca.ui.web.admin.rainterface.RAInterfaceBean","unrevokeCert"),
		new ApprovalOveradableClassName("org.ejbca.ui.web.admin.rainterface.RAInterfaceBean","markForRecovery"),
		new ApprovalOveradableClassName("org.ejbca.extra.caservice.ExtRACAProcess","processExtRARevocationRequest"),
		new ApprovalOveradableClassName("se.primeKey.cardPersonalization.ra.connection.ejbca.EjbcaConnection",null)
	};
    
    /**
     * Changes status of a user.
     *
     * @param username the unique username.
     * @param status   the new status, from 'UserData'.
     * @param approvalflag approvalflag that indicates if approvals should be used or not
     * @throws ApprovalException if an approval already is waiting for specified action 
     * @throws WaitingForApprovalException if approval is required and the action have been added in the approval queue.
     * @ejb.interface-method
     */
    public void setUserStatus(Admin admin, String username, int status) throws AuthorizationDeniedException, FinderException, ApprovalException, WaitingForApprovalException {
        debug(">setUserStatus(" + username + ", " + status + ")");
        // Check if administrator is authorized to edit user.
        int caid = LogConstants.INTERNALCAID;
        try {
            UserDataPK pk = new UserDataPK(username);
            UserDataLocal data1 = home.findByPrimaryKey(pk);
            caid = data1.getCaId();

            if (!authorizedToCA(admin, caid)) {
                String msg = intres.getLocalizedMessage("ra.errorauthca", Integer.valueOf(caid));            	
                logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_ERROR_CHANGEDENDENTITY, msg);
                throw new AuthorizationDeniedException(msg);
            }


            if (getGlobalConfiguration(admin).getEnableEndEntityProfileLimitations()) {
                if (!authorizedToEndEntityProfile(admin, data1.getEndEntityProfileId(), AvailableAccessRules.EDIT_RIGHTS)) {
                    String msg = intres.getLocalizedMessage("ra.errorauthprofile", Integer.valueOf(data1.getEndEntityProfileId()));            	
                    logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_ERROR_CHANGEDENDENTITY, msg);
                    throw new AuthorizationDeniedException(msg);
                }
            }
            
            // Check if approvals is required.
            int numOfApprovalsRequired = getNumOfApprovalRequired(admin, CAInfo.REQ_APPROVAL_ADDEDITENDENTITY, caid);
            ChangeStatusEndEntityApprovalRequest ar = new ChangeStatusEndEntityApprovalRequest(username, data1.getStatus(), status ,  admin,null,numOfApprovalsRequired,data1.getCaId(),data1.getEndEntityProfileId());
            if (ApprovalExecutorUtil.requireApproval(ar, NONAPPROVABLECLASSNAMES_SETUSERSTATUS)){       		    		
            	getApprovalSession().addApprovalRequest(admin, ar);
	            String msg = intres.getLocalizedMessage("ra.approvaledit");            	
            	throw new WaitingForApprovalException(msg);
            }  
            
            if(data1.getStatus() == UserDataConstants.STATUS_KEYRECOVERY && !(status == UserDataConstants.STATUS_KEYRECOVERY || status == UserDataConstants.STATUS_INPROCESS || status == UserDataConstants.STATUS_INITIALIZED)){
                getKeyRecoverySession().unmarkUser(admin,username);	
            }
            
            data1.setStatus(status);
            data1.setTimeModified((new java.util.Date()).getTime());
            String msg = intres.getLocalizedMessage("ra.editedentitystatus", username, Integer.valueOf(status));            	
            logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_INFO_CHANGEDENDENTITY, msg);
        } catch (FinderException e) {
            String msg = intres.getLocalizedMessage("ra.errorentitynotexist", username);            	
            logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_ERROR_CHANGEDENDENTITY, msg);
            throw e;
        }

        debug("<setUserStatus(" + username + ", " + status + ")");
    } // setUserStatus
    

    /**
     * Sets a new password for a user.
     *
     * @param admin    the administrator pwrforming the action
     * @param username the unique username.
     * @param password the new password for the user, NOT null.
     * @ejb.interface-method
     */
    public void setPassword(Admin admin, String username, String password) throws UserDoesntFullfillEndEntityProfile, AuthorizationDeniedException, FinderException {
        setPassword(admin, username, password, false);
    } // setPassword

    /**
     * Sets a clear text password for a user.
     *
     * @param admin    the administrator pwrforming the action
     * @param username the unique username.
     * @param password the new password to be stored in clear text. Setting password to 'null'
     *                 effectively deletes any previous clear text password.
     * @ejb.interface-method
     */
    public void setClearTextPassword(Admin admin, String username, String password) throws UserDoesntFullfillEndEntityProfile, AuthorizationDeniedException, FinderException {
        setPassword(admin, username, password, true);
    } // setClearTextPassword

    /**
     * Sets a password, hashed or clear text, for a user.
     *
     * @param admin     the administrator pwrforming the action
     * @param username  the unique username.
     * @param password  the new password to be stored in clear text. Setting password to 'null'
     *                  effectively deletes any previous clear text password.
     * @param cleartext true gives cleartext password, false hashed
     */
    private void setPassword(Admin admin, String username, String password, boolean cleartext) throws UserDoesntFullfillEndEntityProfile, AuthorizationDeniedException, FinderException {
        debug(">setPassword(" + username + ", hiddenpwd), " + cleartext);
        // Find user
        String newpasswd = password;
        UserDataPK pk = new UserDataPK(username);
        UserDataLocal data = home.findByPrimaryKey(pk);
        int caid = data.getCaId();
        String dn = data.getSubjectDN();

        EndEntityProfile profile = raadminsession.getEndEntityProfile(admin, data.getEndEntityProfileId());

        if (profile.useAutoGeneratedPasswd())
            newpasswd = profile.getAutoGeneratedPasswd();

        if (getGlobalConfiguration(admin).getEnableEndEntityProfileLimitations()) {
            // Check if user fulfills it's profile.
            try {
                profile.doesPasswordFulfillEndEntityProfile(password, true);
            } catch (UserDoesntFullfillEndEntityProfile ufe) {
                String msg = intres.getLocalizedMessage("ra.errorfullfillprofile", Integer.valueOf(data.getEndEntityProfileId()), dn, ufe.getMessage());            	
                logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_ERROR_CHANGEDENDENTITY, msg);
                throw ufe;
            }

            // Check if administrator is authorized to edit user.
            if (!authorizedToEndEntityProfile(admin, data.getEndEntityProfileId(), AvailableAccessRules.EDIT_RIGHTS)) {
                String msg = intres.getLocalizedMessage("ra.errorauthprofile", Integer.valueOf(data.getEndEntityProfileId()));            	
                logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_ERROR_CHANGEDENDENTITY, msg);
                throw new AuthorizationDeniedException(msg);
            }
        }

        if (!authorizedToCA(admin, caid)) {
            String msg = intres.getLocalizedMessage("ra.errorauthca", Integer.valueOf(caid));            	
            logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_ERROR_CHANGEDENDENTITY, msg);
            throw new AuthorizationDeniedException(msg);
        }

        try {
            if ((newpasswd == null) && (cleartext)) {
                data.setClearPassword("");
                data.setTimeModified((new java.util.Date()).getTime());
            } else {
                if (cleartext) {
                    data.setOpenPassword(newpasswd);
                } else {
                    data.setPassword(newpasswd);
                }
                data.setTimeModified((new java.util.Date()).getTime());
            }
            String msg = intres.getLocalizedMessage("ra.editpwdentity", username);            	
            logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_INFO_CHANGEDENDENTITY, msg);
        } catch (java.security.NoSuchAlgorithmException nsae) {
            error("NoSuchAlgorithmException while setting password for user " + username);
            throw new EJBException(nsae);
        }
        debug("<setPassword(" + username + ", hiddenpwd), " + cleartext);
    } // setPassword

    /**
     * Verifies a password for a user.
     *
     * @param admin    the administrator pwrforming the action
     * @param username the unique username.
     * @param password the password to be verified.
     * @ejb.interface-method
     */
    public boolean verifyPassword(Admin admin, String username, String password) throws UserDoesntFullfillEndEntityProfile, AuthorizationDeniedException, FinderException {
        debug(">verifyPassword(" + username + ", hiddenpwd)");
        boolean ret = false;
        // Find user
        UserDataPK pk = new UserDataPK(username);
        UserDataLocal data = home.findByPrimaryKey(pk);
        int caid = data.getCaId();

        if (getGlobalConfiguration(admin).getEnableEndEntityProfileLimitations()) {
            // Check if administrator is authorized to edit user.
            if (!authorizedToEndEntityProfile(admin, data.getEndEntityProfileId(), AvailableAccessRules.EDIT_RIGHTS)) {
                String msg = intres.getLocalizedMessage("ra.errorauthprofile", Integer.valueOf(data.getEndEntityProfileId()));            	
                logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_ERROR_CHANGEDENDENTITY, msg);
                throw new AuthorizationDeniedException(msg);
            }
        }

        if (!authorizedToCA(admin, caid)) {
            String msg = intres.getLocalizedMessage("ra.errorauthca", Integer.valueOf(caid));            	
            logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_ERROR_CHANGEDENDENTITY, msg);
            throw new AuthorizationDeniedException(msg);
        }

        try {
            ret = data.comparePassword(password);
        } catch (java.security.NoSuchAlgorithmException nsae) {
            debug("NoSuchAlgorithmException while verifying password for user " + username);
            throw new EJBException(nsae);
        }
        debug("<verifyPassword(" + username + ", hiddenpwd)");
        return ret;
    } // verifyPassword

    /**
     * Method that revokes a user.
     *
     * @param username the username to revoke.
     * @ejb.interface-method
     */
    public void revokeUser(Admin admin, String username, int reason) throws AuthorizationDeniedException, FinderException {
        debug(">revokeUser(" + username + ")");
        UserDataPK pk = new UserDataPK(username);
        UserDataLocal data;
        try {
            data = home.findByPrimaryKey(pk);
        } catch (ObjectNotFoundException oe) {
            throw new EJBException(oe);
        }

        int caid = data.getCaId();
        if (!authorizedToCA(admin, caid)) {
            String msg = intres.getLocalizedMessage("ra.errorauthca", Integer.valueOf(caid));            	
            logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_ERROR_REVOKEDENDENTITY, msg);
            throw new AuthorizationDeniedException(msg);
        }

        if (getGlobalConfiguration(admin).getEnableEndEntityProfileLimitations()) {
            if (!authorizedToEndEntityProfile(admin, data.getEndEntityProfileId(), AvailableAccessRules.REVOKE_RIGHTS)) {
                String msg = intres.getLocalizedMessage("ra.errorauthprofile", Integer.valueOf(data.getEndEntityProfileId()));            	
                logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_ERROR_REVOKEDENDENTITY, msg);
                throw new AuthorizationDeniedException(msg);
            }
        }

        CertificateProfile prof = this.certificatesession.getCertificateProfile(admin, data.getCertificateProfileId());
        Collection publishers;
        if (prof == null) {
            publishers = new ArrayList();
        } else {
            publishers = prof.getPublisherList();
        }
        try {
			setUserStatus(admin, username, UserDataConstants.STATUS_REVOKED);
		} catch (ApprovalException e) {
			throw new EJBException("This should never happen",e);
		} catch (WaitingForApprovalException e) {
			throw new EJBException("This should never happen",e);
		}
        certificatesession.setRevokeStatus(admin, username, publishers, reason);
        String msg = intres.getLocalizedMessage("ra.revokedentity", username);            	
        logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_INFO_REVOKEDENDENTITY, msg);
        debug("<revokeUser()");
    } // revokeUser

    /**
     * Method that revokes a certificate.
     *
     * @param admin the adminsitrator performing the action
     * @param certserno the serno of certificate to revoke.
     * @param username  the username to revoke.
     * @param reason    the reason of revokation, one of the RevokedCertInfo.XX constants.
     * @ejb.interface-method
     */
    public void revokeCert(Admin admin, BigInteger certserno, String issuerdn, String username, int reason) throws AuthorizationDeniedException, FinderException {
        debug(">revokeCert(" + certserno + ", IssuerDN: " + issuerdn + ", username, " + username + ")");
        UserDataPK pk = new UserDataPK(username);
        UserDataLocal data;
        try {
            data = home.findByPrimaryKey(pk);
        } catch (ObjectNotFoundException oe) {
            throw new FinderException(oe.getMessage()+": username");
        }
        // Check that the user have revokation rigths.
        authorizationsession.isAuthorizedNoLog(admin, AvailableAccessRules.REGULAR_REVOKEENDENTITY);

        int caid = data.getCaId();
        if (!authorizedToCA(admin, caid)) {
            String msg = intres.getLocalizedMessage("ra.errorauthca", Integer.valueOf(caid));            	
            logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_ERROR_REVOKEDENDENTITY, msg);
            throw new AuthorizationDeniedException(msg);
        }

        if (getGlobalConfiguration(admin).getEnableEndEntityProfileLimitations()) {
            if (!authorizedToEndEntityProfile(admin, data.getEndEntityProfileId(), AvailableAccessRules.REVOKE_RIGHTS)) {
                String msg = intres.getLocalizedMessage("ra.errorauthprofile", Integer.valueOf(data.getEndEntityProfileId()));            	
                logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_ERROR_REVOKEDENDENTITY, msg);
                throw new AuthorizationDeniedException(msg);
            }
        }
        // Check that unrevocation is not done on anything that can not be unrevoked
        if (reason == RevokedCertInfo.NOT_REVOKED) {
            RevokedCertInfo revinfo = certificatesession.isRevoked(admin, issuerdn, certserno);        
            if ( (revinfo == null) || (revinfo != null && revinfo.getReason() != RevokedCertInfo.REVOKATION_REASON_CERTIFICATEHOLD) ) {
                String msg = intres.getLocalizedMessage("ra.errorunrevokenotonhold", issuerdn, certserno.toString(16));            	
                logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_ERROR_REVOKEDENDENTITY, msg);
                throw new AuthorizationDeniedException(msg);
            }            
        }
        CertificateProfile prof = this.certificatesession.getCertificateProfile(admin, data.getCertificateProfileId());
        Collection publishers;
        if (prof == null) {
            publishers = new ArrayList();
        } else {
            publishers = prof.getPublisherList();
        }
        // revoke certificate in database and all publishers
        certificatesession.setRevokeStatus(admin, issuerdn, certserno, publishers, reason);

        if (certificatesession.checkIfAllRevoked(admin, username)) {
            try {
    			setUserStatus(admin, username, UserDataConstants.STATUS_REVOKED);
    		} catch (ApprovalException e) {
    			throw new EJBException("This should never happen",e);
    		} catch (WaitingForApprovalException e) {
    			throw new EJBException("This should never happen",e);
    		}
            String msg = intres.getLocalizedMessage("ra.revokedentitycert", issuerdn, certserno.toString(16));            	
            logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_INFO_REVOKEDENDENTITY, msg);
        } else if (reason == RevokedCertInfo.NOT_REVOKED) {
            // Don't change status if it is already the same
            if (data.getStatus() != UserDataConstants.STATUS_GENERATED) {
                try {
                    setUserStatus(admin, username, UserDataConstants.STATUS_GENERATED);                   
                } catch (ApprovalException e) {
                    throw new EJBException("This should never happen",e);
                } catch (WaitingForApprovalException e) {
                    throw new EJBException("This should never happen",e);
                }
            }
        }
        debug("<revokeCert()");
    } // revokeCert

    /** 
     * Reactivates the certificate with certificate serno.
     *
     * @param admin the adminsitrator performing the action
     * @param certserno serial number of certificate to reactivate.
     * @param issuerdn the issuerdn of certificate to reactivate.
     * @param username the username joined to the certificate.
     * @ejb.interface-method
     */
    public void unRevokeCert(Admin admin, BigInteger certserno, String issuerdn, String username) throws AuthorizationDeniedException, FinderException {
        log.debug(">unrevokeCert()");
        revokeCert(admin, certserno, issuerdn, username, RevokedCertInfo.NOT_REVOKED);
        log.debug("<unrevokeCert()");
    }
    
    /**
     * Finds a user.
     *
     * @param admin the administrator performing the action
     * @param username username.
     * @return UserDataVO or null if the user is not found.
     * @ejb.interface-method
     * @ejb.transaction type="Supports"
     */
    public UserDataVO findUser(Admin admin, String username) throws FinderException, AuthorizationDeniedException {
        debug(">findUser(" + username + ")");
        UserDataPK pk = new UserDataPK(username);
        UserDataLocal data;
        try {
            data = home.findByPrimaryKey(pk);
        } catch (ObjectNotFoundException oe) {
            return null;
        }

        if (!authorizedToCA(admin, data.getCaId())) {
            String msg = intres.getLocalizedMessage("ra.errorauthca", Integer.valueOf(data.getCaId()));
            throw new AuthorizationDeniedException(msg);
        }

        if (getGlobalConfiguration(admin).getEnableEndEntityProfileLimitations()) {
            // Check if administrator is authorized to view user.
            if (!authorizedToEndEntityProfile(admin, data.getEndEntityProfileId(), AvailableAccessRules.VIEW_RIGHTS)){
                String msg = intres.getLocalizedMessage("ra.errorauthprofile", Integer.valueOf(data.getEndEntityProfileId()));
                throw new AuthorizationDeniedException(msg);            	
            }
        }

        UserDataVO ret = new UserDataVO(data.getUsername(), data.getSubjectDN(), data.getCaId(), data.getSubjectAltName(), data.getSubjectEmail(), data.getStatus()
                , data.getType(), data.getEndEntityProfileId(), data.getCertificateProfileId()
                , new java.util.Date(data.getTimeCreated()), new java.util.Date(data.getTimeModified())
                , data.getTokenType(), data.getHardTokenIssuerId(), data.getExtendedInformation());
        ret.setPassword(data.getClearPassword());
        debug("<findUser(" + username + ")");
        return ret;
    } // findUser

    /**
     * Finds a user by its subject and issuer DN.
     *
     * @param admin
     * @param subjectdn
     * @param issuerdn
     * @return UserDataVO or null if the user is not found.
     * @ejb.interface-method
     * @ejb.transaction type="Supports"
     */
    public UserDataVO findUserBySubjectAndIssuerDN(Admin admin, String subjectdn, String issuerdn) throws AuthorizationDeniedException {
        debug(">findUserBySubjectAndIssuerDN(" + subjectdn + ", "+issuerdn+")");
        String bcdn = CertTools.stringToBCDNString(subjectdn);
        // String used in SQL so strip it
        String dn = StringTools.strip(bcdn);
        debug("Looking for users with subjectdn: " + dn + ", issuerdn : " + issuerdn);
        UserDataVO returnval = null;

        UserDataLocal data = null;

        try {
            data = home.findBySubjectDNAndCAId(dn, issuerdn.hashCode());
        } catch (FinderException e) {
            log.debug("Cannot find user with DN='" + dn + "'");
        }
        returnval = returnUserDataVO(admin, returnval, data);
        debug("<findUserBySubjectAndIssuerDN(" + subjectdn + ", "+issuerdn+")");
        return returnval;
    } // findUserBySubjectDN

    /**
     * Finds a user by its subject DN.
     *
     * @param admin
     * @param subjectdn
     * @return UserDataVO or null if the user is not found.
     * @ejb.interface-method
     * @ejb.transaction type="Supports"
     */
    public UserDataVO findUserBySubjectDN(Admin admin, String subjectdn) throws AuthorizationDeniedException {
        debug(">findUserBySubjectDN(" + subjectdn + ")");
        String bcdn = CertTools.stringToBCDNString(subjectdn);
        // String used in SQL so strip it
        String dn = StringTools.strip(bcdn);
        debug("Looking for users with subjectdn: " + dn);
        UserDataVO returnval = null;

        UserDataLocal data = null;

        try {
            data = home.findBySubjectDN(dn);
        } catch (FinderException e) {
            log.debug("Cannot find user with DN='" + dn + "'");
        }
        returnval = returnUserDataVO(admin, returnval, data);
        debug("<findUserBySubjectDN(" + subjectdn + ")");
        return returnval;
    } // findUserBySubjectDN

	private UserDataVO returnUserDataVO(Admin admin, UserDataVO returnval, UserDataLocal data) throws AuthorizationDeniedException {
		if (data != null) {
        	if (getGlobalConfiguration(admin).getEnableEndEntityProfileLimitations()) {
        		// Check if administrator is authorized to view user.
        		if (!authorizedToEndEntityProfile(admin, data.getEndEntityProfileId(), AvailableAccessRules.VIEW_RIGHTS)) {
                    String msg = intres.getLocalizedMessage("ra.errorauthprofile", Integer.valueOf(data.getEndEntityProfileId()));
        			throw new AuthorizationDeniedException(msg);
        		}
        	}

            if (!authorizedToCA(admin, data.getCaId())) {
                String msg = intres.getLocalizedMessage("ra.errorauthca", Integer.valueOf(data.getCaId()));
                throw new AuthorizationDeniedException(msg);
            }

            returnval = new UserDataVO(data.getUsername(), data.getSubjectDN(), data.getCaId(), data.getSubjectAltName(), data.getSubjectEmail(), data.getStatus()
                    , data.getType(), data.getEndEntityProfileId(), data.getCertificateProfileId()
                    , new java.util.Date(data.getTimeCreated()), new java.util.Date(data.getTimeModified())
                    , data.getTokenType(), data.getHardTokenIssuerId(), data.getExtendedInformation());

            returnval.setPassword(data.getClearPassword());
        }
		return returnval;
	}

    /**
     * Finds a user by its Email.
     *
     * @param email
     * @return UserDataVO or null if the user is not found.
     * @ejb.interface-method
     * @ejb.transaction type="Supports"
     */
    public Collection findUserByEmail(Admin admin, String email) throws AuthorizationDeniedException {
        debug(">findUserByEmail(" + email + ")");
        debug("Looking for user with email: " + email);
        ArrayList returnval = new ArrayList();

        Collection result = null;
        try {
            result = home.findBySubjectEmail(email);
        } catch (FinderException e) {
            log.debug("Cannot find user with Email='" + email + "'");
        }

        Iterator iter = result.iterator();
        while (iter.hasNext()) {
            UserDataLocal data = (UserDataLocal) iter.next();

            if (getGlobalConfiguration(admin).getEnableEndEntityProfileLimitations()) {
                // Check if administrator is authorized to view user.
                if (!authorizedToEndEntityProfile(admin, data.getEndEntityProfileId(), AvailableAccessRules.VIEW_RIGHTS))
                    break;
            }

            if (!authorizedToCA(admin, data.getCaId())) {
                break;
            }

            UserDataVO user = new UserDataVO(data.getUsername(), data.getSubjectDN(), data.getCaId(), data.getSubjectAltName(), data.getSubjectEmail(), data.getStatus()
                    , data.getType(), data.getEndEntityProfileId(), data.getCertificateProfileId()
                    , new java.util.Date(data.getTimeCreated()), new java.util.Date(data.getTimeModified())
                    , data.getTokenType(), data.getHardTokenIssuerId(), data.getExtendedInformation());
            user.setPassword(data.getClearPassword());
            returnval.add(user);
        }
        debug("<findUserByEmail(" + email + ")");
        return returnval;
    } // findUserBySubjectDN

    /**
     * Method that checks if user with specified users certificate exists in database and is set as administrator.
     *
     * @param subjectdn
     * @throws AuthorizationDeniedException if user isn't an administrator.
     * @ejb.interface-method
     * @ejb.transaction type="Supports"
     */
    public void checkIfCertificateBelongToAdmin(Admin admin, BigInteger certificatesnr, String issuerdn) throws AuthorizationDeniedException {
        debug(">checkIfCertificateBelongToAdmin(" + certificatesnr + ")");
        String username = certificatesession.findUsernameByCertSerno(admin, certificatesnr, issuerdn);

        UserDataLocal data = null;
        if (username != null) {
            UserDataPK pk = new UserDataPK(username);
            try {
                data = home.findByPrimaryKey(pk);
            } catch (FinderException e) {
                log.debug("Cannot find user with username='" + username + "'");
            }
        }

        if (data != null) {
            int type = data.getType();
            if ((type & SecConst.USER_ADMINISTRATOR) == 0) {
                String msg = intres.getLocalizedMessage("ra.errorcertnoadmin", issuerdn, certificatesnr.toString(16));
                logsession.log(admin, data.getCaId(), LogEntry.MODULE_RA, new java.util.Date(), null, null, LogEntry.EVENT_ERROR_ADMINISTRATORLOGGEDIN, msg);
                throw new AuthorizationDeniedException(msg);
            }
        } else {
            String msg = intres.getLocalizedMessage("ra.errorcertnouser", issuerdn, certificatesnr.toString(16));
            logsession.log(admin, LogConstants.INTERNALCAID, LogEntry.MODULE_RA, new java.util.Date(), null, null, LogEntry.EVENT_ERROR_ADMINISTRATORLOGGEDIN, msg);
            throw new AuthorizationDeniedException(msg);
        }

        debug("<checkIfCertificateBelongToAdmin()");
    } // checkIfCertificateBelongToAdmin


    /**
     * Finds all users with a specified status.
     *
     * @param status the status to look for, from 'UserData'.
     * @return Collection of UserDataVO
     * @ejb.interface-method
     * @ejb.transaction type="Supports"
     */
    public Collection findAllUsersByStatus(Admin admin, int status) throws FinderException {
        debug(">findAllUsersByStatus(" + status + ")");
        debug("Looking for users with status: " + status);

        Query query = new Query(Query.TYPE_USERQUERY);
        query.add(UserMatch.MATCH_WITH_STATUS, BasicMatch.MATCH_TYPE_EQUALS, Integer.toString(status));
        Collection returnval = null;

        try {
            returnval = query(admin, query, false, null, null, false,0);
        } catch (IllegalQueryException e) {
        }
        debug("found " + returnval.size() + " user(s) with status=" + status);
        debug("<findAllUsersByStatus(" + status + ")");
        return returnval;
    }
    /**
     * Finds all users registered to a specified ca.
     *
     * @param caid the caid of the CA, from 'UserData'.
     * @return Collection of UserDataVO
     * @ejb.interface-method
     * @ejb.transaction type="Supports"
     */
     public Collection findAllUsersByCaId(Admin admin, int caid) throws FinderException {
         debug(">findAllUsersByCaId("+caid+")");
         debug("Looking for users with caid: " + caid);
         
         Query query = new Query(Query.TYPE_USERQUERY);
         query.add(UserMatch.MATCH_WITH_CA, BasicMatch.MATCH_TYPE_EQUALS, Integer.toString(caid));
         Collection returnval = null;
         
         try{
           returnval = query(admin, query, false, null, null, false,0);  
         }catch(IllegalQueryException e){}
         debug("found "+returnval.size()+" user(s) with caid="+caid);
         debug("<findAllUsersByCaId("+caid+")");
         return returnval;         
     }


    /**
     * Finds all users and returns the first MAXIMUM_QUERY_ROWCOUNT.
     *
     * @return Collection of UserDataVO
     * @ejb.interface-method
     * @ejb.transaction type="Supports"
     */
    public Collection findAllUsersWithLimit(Admin admin) throws FinderException {
        debug(">findAllUsersWithLimit()");
        Collection returnval = null;
        try {
            returnval = query(admin, null, true, null, null, false, 0);
        } catch (IllegalQueryException e) {
        }
        debug("<findAllUsersWithLimit()");
        return returnval;
    }

    /**
     * Finds all users with a specified status and returns the first MAXIMUM_QUERY_ROWCOUNT.
     *
     * @param status the new status, from 'UserData'.
     * @ejb.interface-method
     * @ejb.transaction type="Supports"
     */
    public Collection findAllUsersByStatusWithLimit(Admin admin, int status, boolean onlybatchusers) throws FinderException {
        debug(">findAllUsersByStatusWithLimit()");

        Query query = new Query(Query.TYPE_USERQUERY);
        query.add(UserMatch.MATCH_WITH_STATUS, BasicMatch.MATCH_TYPE_EQUALS, Integer.toString(status));
        Collection returnval = null;

        try {
            returnval = query(admin, query, false, null, null, onlybatchusers, 0);
        } catch (IllegalQueryException e) {
        }

        debug("<findAllUsersByStatusWithLimit()");
        return returnval;
    }


    /**
     * Method to execute a customized query on the ra user data. The parameter query should be a legal Query object.
     *
     * @param query                  a number of statments compiled by query class to a SQL 'WHERE'-clause statment.
     * @param caauthorizationstring  is a string placed in the where clause of SQL query indication which CA:s the administrator is authorized to view.
     * @param endentityprofilestring is a string placed in the where clause of SQL query indication which endentityprofiles the administrator is authorized to view.
     * @param numberofrows  the number of rows to fetch, use 0 for default UserAdminConstants.MAXIMUM_QUERY_ROWCOUNT 
     * @return a collection of UserDataVO. Maximum size of Collection is defined i IUserAdminSessionRemote.MAXIMUM_QUERY_ROWCOUNT
     * @throws IllegalQueryException when query parameters internal rules isn't fullfilled.
     * @ejb.interface-method
     * @ejb.transaction type="Supports"
     * @see se.anatom.ejbca.util.query.Query
     */
    public Collection query(Admin admin, Query query, String caauthorizationstring, String endentityprofilestring, int numberofrows) throws IllegalQueryException {
        return query(admin, query, true, caauthorizationstring, endentityprofilestring, false, numberofrows);
    }

    /**
     * Help function used to retrieve user information. A query parameter of null indicates all users.
     * If caauthorizationstring or endentityprofilestring are null then the method will retrieve the information
     * itself.
     * 
     * @param numberofrows  the number of rows to fetch, use 0 for default UserAdminConstants.MAXIMUM_QUERY_ROWCOUNT 
     */
    private Collection query(Admin admin, Query query, boolean withlimit, String caauthorizationstr, String endentityprofilestr, boolean onlybatchusers, int numberofrows) throws IllegalQueryException {
        debug(">query(): withlimit="+withlimit);
        boolean authorizedtoanyprofile = true;
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        String caauthorizationstring = StringTools.strip(caauthorizationstr);
        String endentityprofilestring = StringTools.strip(endentityprofilestr);
        ArrayList returnval = new ArrayList();
        GlobalConfiguration globalconfiguration = getGlobalConfiguration(admin);
        RAAuthorization raauthorization = null;
        String caauthstring = caauthorizationstring;
        String endentityauth = endentityprofilestring;
        String sqlquery = "select " + USERDATA_COL + " from UserData where ";
        int fetchsize = UserAdminConstants.MAXIMUM_QUERY_ROWCOUNT;
        
        if(numberofrows != 0){
        	fetchsize = numberofrows;
        }


        // Check if query is legal.
        if (query != null && !query.isLegalQuery())
            throw new IllegalQueryException();

        if (query != null)
            sqlquery = sqlquery + query.getQueryString();

        if (caauthorizationstring == null || endentityprofilestring == null) {
            raauthorization = new RAAuthorization(admin, raadminsession, authorizationsession);
            caauthstring = raauthorization.getCAAuthorizationString();
            if (globalconfiguration.getEnableEndEntityProfileLimitations())
                endentityauth = raauthorization.getEndEntityProfileAuthorizationString(true);
            else
                endentityauth = "";
        }

        if (!caauthstring.trim().equals("") && query != null)
            sqlquery = sqlquery + " AND " + caauthstring;
        else
            sqlquery = sqlquery + caauthstring;


        if (globalconfiguration.getEnableEndEntityProfileLimitations()) {
            if (caauthstring.trim().equals("") && query == null)
                sqlquery = sqlquery + endentityauth;
            else
                sqlquery = sqlquery + " AND " + endentityauth;

            if (endentityauth == null || endentityauth.trim().equals("")) {
                authorizedtoanyprofile = false;
            }
        }

        try {
            if (authorizedtoanyprofile) {
                // Construct SQL query.
                con = JDBCUtil.getDBConnection(JNDINames.DATASOURCE);
                log.debug("generated query" + sqlquery);
                ps = con.prepareStatement(sqlquery);

                // Execute query.
                rs = ps.executeQuery();

                // Assemble result.
                while (rs.next() && (!withlimit || returnval.size() <= fetchsize)) {
                    // Read the variables in order, some databases (i.e. MS-SQL) 
                    // seems to not like out-of-order read of columns (i.e. nr 15 before nr 1) 
                    String user = rs.getString(1);
                    String dn = rs.getString(2);
                    String subaltname = rs.getString(3);
                    String email = rs.getString(4);
                    int status = rs.getInt(5);
                    int type = rs.getInt(6);
                    String pwd = rs.getString(7);
                    Date timecreated = new java.util.Date(rs.getLong(8));
                    Date timemodified = new java.util.Date(rs.getLong(9));
                    int eprofileid = rs.getInt(10);
                    int cprofileid = rs.getInt(11);
                    int tokentype = rs.getInt(12);
                    int tokenissuerid = rs.getInt(13);
                    int caid = rs.getInt(14);
                    String extendedInformation = rs.getString(15);
                    UserDataVO data = new UserDataVO(user, dn, caid, subaltname, email, status, type
                            , eprofileid, cprofileid, timecreated, timemodified, tokentype, tokenissuerid,
							UserDataVO.getExtendedInformation(extendedInformation));
                    data.setPassword(pwd);

                    if (!onlybatchusers || (data.getPassword() != null && data.getPassword().length() > 0))
                        returnval.add(data);
                }
            }
            debug("<query()");
            return returnval;

        } catch (Exception e) {
            throw new EJBException(e);
        } finally {
            JDBCUtil.close(con, ps, rs);
        }

    } // query
    

    /**
     * Methods that checks if a user exists in the database having the given endentityprofileid. This function is mainly for avoiding
     * desyncronisation when a end entity profile is deleted.
     *
     * @param endentityprofileid the id of end entity profile to look for.
     * @return true if endentityprofileid exists in userdatabase.
     * @ejb.interface-method
     * @ejb.transaction type="Supports"
     */
    public boolean checkForEndEntityProfileId(Admin admin, int endentityprofileid) {
        debug(">checkForEndEntityProfileId()");
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        int count = 1; // return true as default.

        Query query = new Query(Query.TYPE_USERQUERY);
        query.add(UserMatch.MATCH_WITH_ENDENTITYPROFILE, BasicMatch.MATCH_TYPE_EQUALS, Integer.toString(endentityprofileid));

        try {
            // Construct SQL query.
            con = JDBCUtil.getDBConnection(JNDINames.DATASOURCE);
            ps = con.prepareStatement("select COUNT(*) from UserData where " + query.getQueryString());
            // Execute query.
            rs = ps.executeQuery();
            // Assemble result.
            if (rs.next()) {
                count = rs.getInt(1);
            }
            debug("<checkForEndEntityProfileId()");
            return count > 0;

        } catch (Exception e) {
            throw new EJBException(e);
        } finally {
            JDBCUtil.close(con, ps, rs);
        }


    }

    /**
     * Methods that checks if a user exists in the database having the given certificateprofileid. This function is mainly for avoiding
     * desyncronisation when a certificateprofile is deleted.
     *
     * @param certificateprofileid the id of certificateprofile to look for.
     * @return true if certificateproileid exists in userdatabase.
     * @ejb.interface-method
     * @ejb.transaction type="Supports"
     */
    public boolean checkForCertificateProfileId(Admin admin, int certificateprofileid) {
        debug(">checkForCertificateProfileId()");
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        int count = 1; // return true as default.

        Query query = new Query(Query.TYPE_USERQUERY);
        query.add(UserMatch.MATCH_WITH_CERTIFICATEPROFILE, BasicMatch.MATCH_TYPE_EQUALS, Integer.toString(certificateprofileid));

        try {
            // Construct SQL query.
            con = JDBCUtil.getDBConnection(JNDINames.DATASOURCE);
            ps = con.prepareStatement("select COUNT(*) from UserData where " + query.getQueryString());
            // Execute query.
            rs = ps.executeQuery();
            // Assemble result.
            if (rs.next()) {
                count = rs.getInt(1);
            }
            debug("<checkForCertificateProfileId()");
            return count > 0;

        } catch (Exception e) {
            throw new EJBException(e);
        } finally {
            JDBCUtil.close(con, ps, rs);
        }
    } // checkForCertificateProfileId

    /**
     * Methods that checks if a user exists in the database having the given caid. This function is mainly for avoiding
     * desyncronisation when a CAs is deleted.
     *
     * @param caid the id of CA to look for.
     * @return true if caid exists in userdatabase.
     * @ejb.interface-method
     * @ejb.transaction type="Supports"
     */
    public boolean checkForCAId(Admin admin, int caid) {
        debug(">checkForCAId()");
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        int count = 1; // return true as default.

        Query query = new Query(Query.TYPE_USERQUERY);
        query.add(UserMatch.MATCH_WITH_CA, BasicMatch.MATCH_TYPE_EQUALS, Integer.toString(caid));

        try {
            // Construct SQL query.
            con = JDBCUtil.getDBConnection(JNDINames.DATASOURCE);
            ps = con.prepareStatement("select COUNT(*) from UserData where " + query.getQueryString());
            // Execute query.
            rs = ps.executeQuery();
            // Assemble result.
            if (rs.next()) {
                count = rs.getInt(1);
            }
            debug("<checkForCAId()");
            return count > 0;

        } catch (Exception e) {
            throw new EJBException(e);
        } finally {
            JDBCUtil.close(con, ps, rs);
        }
    } // checkForCAId


    /**
     * Methods that checks if a user exists in the database having the given hard token profile id. This function is mainly for avoiding
     * desyncronisation when a hard token profile is deleted.
     *
     * @param profileid of hardtokenprofile to look for.
     * @return true if proileid exists in userdatabase.
     * @ejb.interface-method
     * @ejb.transaction type="Supports"
     */
    public boolean checkForHardTokenProfileId(Admin admin, int profileid) {
        debug(">checkForHardTokenProfileId()");
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        int count = 1; // return true as default.

        Query query = new Query(Query.TYPE_USERQUERY);
        query.add(UserMatch.MATCH_WITH_TOKEN, BasicMatch.MATCH_TYPE_EQUALS, Integer.toString(profileid));

        try {
            // Construct SQL query.
            con = JDBCUtil.getDBConnection(JNDINames.DATASOURCE);
            ps = con.prepareStatement("select COUNT(*) from UserData where " + query.getQueryString());
            // Execute query.
            rs = ps.executeQuery();
            // Assemble result.
            if (rs.next()) {
                count = rs.getInt(1);
            }
            debug("<checkForHardTokenProfileId()");
            return count > 0;

        } catch (Exception e) {
            throw new EJBException(e);
        } finally {
            JDBCUtil.close(con, ps, rs);
        }
    } // checkForHardTokenProfileId


    private void  print(Admin admin, EndEntityProfile profile, UserDataVO userdata){
    	try{
      	  if(profile.getUsePrinting()){
      	    String[] pINs = new String[1];
      	    pINs[0] = userdata.getPassword();
              PrinterManager.print(profile.getPrinterName(), profile.getPrinterSVGFileName(), profile.getPrinterSVGData(), profile.getPrintedCopies(), 0, userdata, pINs, new String[0], "", "", "");
      	  }
    	}catch(PrinterException e){
    		String msg = intres.getLocalizedMessage("ra.errorprint", userdata.getUsername(), e.getMessage());
    		error(msg, e);
    		try{
    			logsession.log(admin, userdata.getCAId(), LogEntry.MODULE_RA, new java.util.Date(),userdata.getUsername(), null, LogEntry.EVENT_ERROR_NOTIFICATION, msg);
    		}catch(Exception f){
    			throw new EJBException(f);
    		}
    	}
    }
    
    private void sendNotification(Admin admin, EndEntityProfile profile, String username, String password, String dn, String email, int caid) {
        debug(">sendNotification: user="+username+", email="+email);
        try {
            if (email == null) {
        		String msg = intres.getLocalizedMessage("ra.errornotificationnoemail", username);
                throw new Exception(msg);
            }

            String mailJndi = getLocator().getString("java:comp/env/MailJNDIName");
            Session mailSession = getLocator().getMailSession(mailJndi);
            NotificationParamGen paramGen = new NotificationParamGen(username,password,dn);
            HashMap params = paramGen.getParams();

            Message msg = new TemplateMimeMessage(params, mailSession);
            msg.setFrom(new InternetAddress(profile.getNotificationSender()));
            msg.setRecipients(javax.mail.Message.RecipientType.TO, InternetAddress.parse(email, false));
            msg.setSubject(profile.getNotificationSubject());
            msg.setContent(profile.getNotificationMessage(), "text/plain");
            msg.setHeader("X-Mailer", "JavaMailer");
            msg.setSentDate(new Date());
            Transport.send(msg);

            logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_INFO_NOTIFICATION, intres.getLocalizedMessage("ra.sentnotification", username, email));
        } catch (Exception e) {
        	String msg = intres.getLocalizedMessage("ra.errorsendnotification", username, email);
        	error(msg, e);
            try{
                logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(),username, null, LogEntry.EVENT_ERROR_NOTIFICATION, msg);
            }catch(Exception f){
                throw new EJBException(f);
            }
        }
        debug("<sendNotification: user="+username+", email="+email);
    } // sendNotification

    /**
     * Method checking if username already exists in database.
     *
     * @return true if username already exists.
     * @ejb.interface-method
     * @ejb.transaction type="Supports"
     */
    public boolean existsUser(Admin admin, String username) {
        boolean returnval = true;

        try {
            home.findByPrimaryKey(new UserDataPK(username));
        } catch (FinderException fe) {
            returnval = false;
        }

        return returnval;
    }

} // LocalUserAdminSessionBean
