
package se.anatom.ejbca.ca.auth;

import java.rmi.*;

import javax.naming.*;
import javax.rmi.*;
import javax.ejb.*;

import se.anatom.ejbca.BaseSessionBean;
import se.anatom.ejbca.util.CertTools;
import se.anatom.ejbca.SecConst;
import se.anatom.ejbca.ca.exception.AuthLoginException;
import se.anatom.ejbca.ca.exception.AuthStatusException;
import se.anatom.ejbca.log.Admin;
import se.anatom.ejbca.log.ILogSessionRemote;
import se.anatom.ejbca.log.ILogSessionHome;
import se.anatom.ejbca.log.LogEntry;

/**
 * Approves all authentication requests that contain a DN as the username, password is ignored and
 * the username is returned as DN.
 * Useful for demo purposes to give out certificates to anyone.
 *
 * @version $Id: NullAuthenticationSessionBean.java,v 1.8 2002-11-17 14:01:39 herrvendil Exp $
 */
public class NullAuthenticationSessionBean extends BaseSessionBean {

    /** The remote interface of the log session bean */
    private ILogSessionRemote logsession;   
      
    /**
     * Default create for SessionBean without any creation Arguments.
     * @throws CreateException if bean instance can't be created
     */
    public void ejbCreate () throws CreateException {
        debug(">ejbCreate()");
        try{ 
          ILogSessionHome logsessionhome = (ILogSessionHome) lookup("java:comp/env/ejb/LogSession",ILogSessionHome.class);       
          logsession = logsessionhome.create();
        }catch(Exception e){
          throw new EJBException(e);
        }
        debug("<ejbCreate()");
    }

   /**
    * Implements IAuthenticationSession::authenticateUser.
    * Implements a mechanism that does no real authentication. Returns the username as DN is the username
    * contains a DN. Only returns entities of type USER_ENDUSER.
    * STATUS_NEW, STATUS_FAILED or STATUS_INPROCESS.
    */
    public UserAuthData authenticateUser(Admin admin, String username, String password) throws ObjectNotFoundException,AuthStatusException,AuthLoginException {
        debug(">authenticateUser("+username+", hiddenpwd)");
        try {
            // Does the username contain a DN?
            String dn = CertTools.stringToBCDNString(username);
            if ( (dn != null) && (dn.length()>0) ){
                String email = CertTools.getPartFromDN(dn, "EmailAddress");
                try{
                  logsession.log(admin, LogEntry.MODULE_CA, new java.util.Date(),username, null, LogEntry.EVENT_INFO_USERAUTHENTICATION,"NULL-Authenticated user");
                }catch(RemoteException re){
                  throw new EJBException(re);
                }
                // Use default certificate profile 0
                UserAuthData ret = new UserAuthData(username, dn, email, SecConst.USER_ENDUSER, 0);
                debug("<authenticateUser("+username+", hiddenpwd)");
                return ret;
            } else {
                try{
                  logsession.log(admin, LogEntry.MODULE_CA, new java.util.Date(),username, null, LogEntry.EVENT_ERROR_USERAUTHENTICATION,"User does not contain a DN.");
                }catch(RemoteException re){
                  throw new EJBException(re);
                }
                throw new AuthLoginException("User "+username+" does not contain a DN.");
            }
        } catch (AuthLoginException le) {
            throw le;
        } catch (Exception e) {
            throw new EJBException(e.toString());
        }
    } //authenticateUser

   /**
    * Implements IAuthenticationSession::finishUser.
    * Does nothing...
    */
    public void finishUser(Admin admin, String username, String password) throws ObjectNotFoundException {
        debug(">finishUser("+username+", hiddenpwd)");
        debug("<finishUser("+username+", hiddenpwd)");
    } //finishUser
}
