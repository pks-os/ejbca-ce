
package se.anatom.ejbca.admin;

import se.anatom.ejbca.ra.UserAdminData;
import se.anatom.ejbca.ra.authorization.AuthorizationDeniedException;

/** Find details of a user in the database.
 *
 * @version $Id: RaFindUserCommand.java,v 1.5 2003-03-19 09:36:13 anatom Exp $
 */
public class RaFindUserCommand extends BaseRaAdminCommand {

    /** Creates a new instance of RaFindUserCommand */
    public RaFindUserCommand(String[] args) {
        super(args);
    }

    public void execute() throws IllegalAdminCommandException, ErrorAdminCommandException {
        try {
            if (args.length < 2) {
                System.out.println("Usage: RA finduser <username>");
                return;
            }
            String username = args[1];
            try{
              UserAdminData data = getAdminSession().findUser(administrator, username);
              if (data != null) {
                System.out.println("Found user:");
                System.out.println("username="+data.getUsername());
                System.out.println("password="+data.getPassword());
                System.out.println("dn=\""+data.getDN()+"\"");
                System.out.println("email="+data.getEmail());
                System.out.println("status="+data.getStatus());
                System.out.println("type="+data.getType());
                System.out.println("token type="+data.getTokenType());
                System.out.println("end entity profile id="+data.getEndEntityProfileId());
                System.out.println("certificate entity profile id="+data.getCertificateProfileId());
                System.out.println("hard token issuer id="+data.getHardTokenIssuerId());
                System.out.println("created="+data.getTimeCreated());
                System.out.println("modified="+data.getTimeModified());
              } else {
                System.out.println("User '"+username+"' does not exist.");
              }
            }catch(AuthorizationDeniedException e){
               System.out.println("Error : Not authorized to view user.");
            }
        } catch (Exception e) {
            throw new ErrorAdminCommandException(e);
        }
    } // execute

}
