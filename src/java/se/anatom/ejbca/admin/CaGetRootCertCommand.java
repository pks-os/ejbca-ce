
package se.anatom.ejbca.admin;

import java.io.*;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

/** Export root CA certificate.
 *
 * @version $Id: CaGetRootCertCommand.java,v 1.2 2002-04-13 18:40:15 anatom Exp $
 */
public class CaGetRootCertCommand extends BaseCaAdminCommand {

    /** Creates a new instance of CaGetRootCertCommand */
    public CaGetRootCertCommand(String[] args) {
        super(args);
    }

    public void execute() throws IllegalAdminCommandException, ErrorAdminCommandException {
        if (args.length < 2) {
            String msg = "Save root CA certificate (DER-format) to file.\n";
            msg += "Usage: CA rootcert <filename>";
            throw new IllegalAdminCommandException(msg);
        }
        String filename = args[1];
        
        try {
            Certificate[] chain = getCertChain();
            X509Certificate rootcert = (X509Certificate)chain[chain.length-1];
            FileOutputStream fos = new FileOutputStream(filename);
            fos.write(rootcert.getEncoded());
            fos.close();
        } catch (Exception e) {
            throw new ErrorAdminCommandException(e);
        }
        System.out.println("Wrote Root CA certificate to '"+filename+"'");
    } // execute
    
}
