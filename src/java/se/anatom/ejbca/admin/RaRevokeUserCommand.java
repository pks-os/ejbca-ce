
package se.anatom.ejbca.admin;

import java.io.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Collection;
import java.util.Iterator;

import se.anatom.ejbca.ra.UserAdminData;
import se.anatom.ejbca.ra.UserDataLocal;
import se.anatom.ejbca.ra.authorization.AuthorizationDeniedException;
import se.anatom.ejbca.ca.store.ICertificateStoreSessionHome;
import se.anatom.ejbca.ca.store.ICertificateStoreSessionRemote;
import se.anatom.ejbca.ca.store.CertificateDataPK;
import se.anatom.ejbca.ca.store.CertificateData;
import se.anatom.ejbca.ca.store.CertificateDataHome;
import se.anatom.ejbca.util.CertTools;
import se.anatom.ejbca.util.Hex;

/** Revokes a user in the database, and also revokes all the users certificates.
 *
 * @version $Id: RaRevokeUserCommand.java,v 1.9 2002-11-17 14:01:38 herrvendil Exp $
 */
public class RaRevokeUserCommand extends BaseRaAdminCommand {

    /** Creates a new instance of RaRevokeUserCommand */
    public RaRevokeUserCommand(String[] args) {
        super(args);
    }

    public void execute() throws IllegalAdminCommandException, ErrorAdminCommandException {
        try {
            if (args.length < 3) {
                System.out.println("Usage: RA revokeuser <username> <reason>");
                System.out.println("Reason: unused(0), keyCompromise(1), cACompromise(2), affiliationChanged(3), superseded(4), cessationOfOperation(5), certficateHold(6), removeFromCRL(8),privilegeWithdrawn(9),aACompromise(10)");
                System.out.println("Normal reason is 0");
                 return;
            }
            String username = args[1];
            int reason = Integer.parseInt(args[2]);
            
            if(reason == 7 || reason < 0 || reason > 10)
              System.out.println("Error : Reason must be an integer between 0 and 10 except 7.");
            else{
              UserAdminData data = getAdminSession().findUser(administrator, username);
              System.out.println("Found user:");
              System.out.println("username="+data.getUsername());
              System.out.println("dn=\""+data.getDN()+"\"");
              System.out.println("Old status="+data.getStatus());
              getAdminSession().setUserStatus(administrator, username, UserDataLocal.STATUS_REVOKED);
              System.out.println("New status="+UserDataLocal.STATUS_REVOKED);

              // Revoke users certificates
              try{
               getAdminSession().revokeUser(administrator, username, reason);
              }catch(AuthorizationDeniedException e){
                System.out.println("Error : Not authorized to revoke user.");                             
              }
            }  
        } catch (Exception e) {
            throw new ErrorAdminCommandException(e);
        }
    } // execute

}
