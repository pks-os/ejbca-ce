package se.anatom.ejbca.ra.authorization;

import java.rmi.RemoteException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import javax.ejb.CreateException;
import javax.naming.*;
import javax.rmi.PortableRemoteObject;

import se.anatom.ejbca.ca.sign.ISignSessionHome;
import se.anatom.ejbca.ca.sign.ISignSessionRemote;
import se.anatom.ejbca.ca.store.ICertificateStoreSessionHome;
import se.anatom.ejbca.ca.store.ICertificateStoreSessionRemote;
import se.anatom.ejbca.log.Admin;
import se.anatom.ejbca.log.ILogSessionRemote;
import se.anatom.ejbca.log.LogEntry;
import se.anatom.ejbca.ra.GlobalConfiguration;
import se.anatom.ejbca.util.CertTools;

import org.apache.log4j.Logger;

/**
 * A java bean handling the athorization to ejbca. The main metod are isAthorized and authenticate.
 *
 * @version $Id: EjbcaAuthorization.java,v 1.14 2003-07-23 09:40:16 anatom Exp $
 */
public class EjbcaAuthorization extends Object implements java.io.Serializable {

    private static Logger log = Logger.getLogger(EjbcaAuthorization.class);
    /**
     * Creates new EjbcaAthorization
     *
     * @param admingroups DOCUMENT ME!
     * @param globalconfiguration DOCUMENT ME!
     * @param logsession DOCUMENT ME!
     * @param admin DOCUMENT ME!
     * @param module DOCUMENT ME!
     */
    public EjbcaAuthorization(AdminGroup[] admingroups, GlobalConfiguration globalconfiguration,
        ILogSessionRemote logsession, Admin admin, int module)
        throws NamingException, CreateException, RemoteException {
        accesstree = new AccessTree();
        buildAccessTree(admingroups);

        this.admin = admin;
        this.logsession = logsession;
        this.module = module;

        InitialContext jndicontext = new InitialContext();
        Object obj1 = jndicontext.lookup("CertificateStoreSession");

        try {
            ICertificateStoreSessionHome certificatesessionhome = (ICertificateStoreSessionHome) javax.rmi.PortableRemoteObject.narrow(obj1,
                    ICertificateStoreSessionHome.class);
            certificatesession = certificatesessionhome.create();

            ISignSessionHome signhome = (ISignSessionHome) PortableRemoteObject.narrow(jndicontext.lookup(
                        "RSASignSession"), ISignSessionHome.class);

            ISignSessionRemote signsession = signhome.create();

            this.cacertificatechain = signsession.getCertificateChain(admin);
        } catch (Exception e) {
            log.error("Error creating object: ",e);
            throw new CreateException(e.getMessage());
        }
    }

    // Public methods.

    /**
     * Method to check if a user is authorized to a resource
     *
     * @param admininformation information about the user to be authorized.
     * @param resource the resource to look up.
     *
     * @return true if authorizes
     *
     * @throws AuthorizationDeniedException when authorization is denied.
     */
    public boolean isAuthorized(AdminInformation admininformation, String resource)
        throws AuthorizationDeniedException {
        // Check in accesstree.
        if (accesstree.isAuthorized(admininformation, resource) == false) {
            try {
                logsession.log(admin, module, new java.util.Date(), null, null,
                    LogEntry.EVENT_ERROR_NOTAUTHORIZEDTORESOURCE, "Resource : " + resource);
            } catch (RemoteException re) {
            }

            throw new AuthorizationDeniedException();
        }

        try {
            logsession.log(admin, module, new java.util.Date(), null, null,
                LogEntry.EVENT_INFO_AUTHORIZEDTORESOURCE, "Resource : " + resource);
        } catch (RemoteException re) {
        }

        return true;
    }

    /**
     * Method to check if a user is authorized to a resource without performing any logging
     *
     * @param admininformation information about the user to be authorized.
     * @param resource the resource to look up.
     *
     * @return true if authorizes
     *
     * @throws AuthorizationDeniedException when authorization is denied.
     */
    public boolean isAuthorizedNoLog(AdminInformation admininformation, String resource)
        throws AuthorizationDeniedException {
        // Check in accesstree.
        if (accesstree.isAuthorized(admininformation, resource) == false) {
            throw new AuthorizationDeniedException();
        }

        return true;
    }

    /**
     * Method that authenticates a certificate by verifying signature, checking validity and lookup
     * if certificate is revoked.
     *
     * @param certificate the certificate to be authenticated.
     *
     * @throws AuthenticationFailedException if authentication failed.
     */
    public void authenticate(X509Certificate certificate)
        throws AuthenticationFailedException {
        // Check Validity
        try {
            certificate.checkValidity();
        } catch (Exception e) {
            throw new AuthenticationFailedException("Your certificates vality has expired.");
        }

        // Vertify Signature
        boolean verified = false;

        for (int i = 0; i < this.cacertificatechain.length; i++) {
            try {
//            System.out.println("EjbcaAuthorization: authenticate : Comparing : "  + CertTools.getIssuerDN(certificate) + " With " + CertTools.getSubjectDN((X509Certificate) cacertificatechain[i]));
//            if(LDAPDN.equals(CertTools.getIssuerDN(certificate), CertTools.getSubjectDN((X509Certificate) cacertificatechain[i]))){
                certificate.verify(cacertificatechain[i].getPublicKey());
                verified = true;

//            }
            } catch (Exception e) {
                log.error("Error authenticating: ",e);
            }
        }

        if (!verified) {
            throw new AuthenticationFailedException(
                "Your certificate cannot be verified by CA certificate chain.");
        }

        // Check if certificate is revoked.
        try {
            if (certificatesession.isRevoked(admin, CertTools.getIssuerDN(certificate),
                        certificate.getSerialNumber()) != null) {
                // Certificate revoked
                throw new AuthenticationFailedException("Your certificate have been revoked.");
            }
        } catch (RemoteException e) {
            throw new AuthenticationFailedException("Your certificate cannot be found in database.");
        }
    }

    /**
     * Metod to load the access data from database.
     *
     * @param admingroups DOCUMENT ME!
     */
    public void buildAccessTree(AdminGroup[] admingroups) {
        accesstree.buildTree(admingroups);
    }

    // Private metods
    // Private fields.
    private AccessTree accesstree;
    private Certificate[] cacertificatechain;
    private Admin admin;
    private int module;
    private ICertificateStoreSessionRemote certificatesession;
    private ILogSessionRemote logsession;
}
