package se.anatom.ejbca.admin;

import java.io.*;
import java.util.ArrayList;
import java.security.cert.X509Certificate;

import se.anatom.ejbca.util.CertTools;

/**
 * Export root CA certificate.
 *
 * @version $Id: CaGetRootCertCommand.java,v 1.8 2003-11-01 14:11:05 anatom Exp $
 */
public class CaGetRootCertCommand extends BaseCaAdminCommand {
    /**
     * Creates a new instance of CaGetRootCertCommand
     *
     * @param args command line arguments
     */
    public CaGetRootCertCommand(String[] args) {
        super(args);
    }

    /**
     * Runs the command
     *
     * @throws IllegalAdminCommandException Error in command args
     * @throws ErrorAdminCommandException Error running command
     */
    public void execute() throws IllegalAdminCommandException, ErrorAdminCommandException {
        if (args.length < 3) {
            String msg = "Save root CA certificate (PEM- or DER-format) to file.\n";
            msg += "Usage: CA getrootcert <caname> <filename> <-der>";
            throw new IllegalAdminCommandException(msg);
        }

        String caname = args[1];
        String filename = args[2];
        boolean pem = true;
        if (args.length > 2) {
            if (("-der").equals(args[3])) {
                pem = false;
            }
        }
        
        try {
            ArrayList chain = new ArrayList(getCertChain(caname));
            X509Certificate rootcert = (X509Certificate)chain.get(chain.size()-1);

            FileOutputStream fos = new FileOutputStream(filename);
            if (pem) {
                fos.write(CertTools.getPEMFromCerts(chain));
            } else {
                fos.write(rootcert.getEncoded());
            }
            fos.close();
        } catch (Exception e) {
            throw new ErrorAdminCommandException(e);
        }

        System.out.println("Wrote Root CA certificate to '" + filename + "'");
    }

    // execute
}
