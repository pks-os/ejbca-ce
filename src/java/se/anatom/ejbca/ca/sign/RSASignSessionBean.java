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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CRLException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.ObjectNotFoundException;

import org.bouncycastle.jce.X509KeyUsage;

import se.anatom.ejbca.BaseSessionBean;
import se.anatom.ejbca.SecConst;
import se.anatom.ejbca.ca.auth.IAuthenticationSessionLocal;
import se.anatom.ejbca.ca.auth.IAuthenticationSessionLocalHome;
import se.anatom.ejbca.ca.auth.UserAuthData;
import se.anatom.ejbca.ca.caadmin.CA;
import se.anatom.ejbca.ca.caadmin.CADataLocal;
import se.anatom.ejbca.ca.caadmin.CADataLocalHome;
import se.anatom.ejbca.ca.caadmin.CAToken;
import se.anatom.ejbca.ca.caadmin.extendedcaservices.ExtendedCAServiceNotActiveException;
import se.anatom.ejbca.ca.caadmin.extendedcaservices.ExtendedCAServiceRequest;
import se.anatom.ejbca.ca.caadmin.extendedcaservices.ExtendedCAServiceRequestException;
import se.anatom.ejbca.ca.caadmin.extendedcaservices.ExtendedCAServiceResponse;
import se.anatom.ejbca.ca.caadmin.extendedcaservices.IllegalExtendedCAServiceRequestException;
import se.anatom.ejbca.ca.exception.AuthLoginException;
import se.anatom.ejbca.ca.exception.AuthStatusException;
import se.anatom.ejbca.ca.exception.CADoesntExistsException;
import se.anatom.ejbca.ca.exception.CATokenOfflineException;
import se.anatom.ejbca.ca.exception.IllegalKeyException;
import se.anatom.ejbca.ca.exception.IllegalKeyStoreException;
import se.anatom.ejbca.ca.exception.SignRequestException;
import se.anatom.ejbca.ca.exception.SignRequestSignatureException;
import se.anatom.ejbca.ca.publisher.IPublisherSessionLocal;
import se.anatom.ejbca.ca.publisher.IPublisherSessionLocalHome;
import se.anatom.ejbca.ca.store.CertificateData;
import se.anatom.ejbca.ca.store.ICertificateStoreSessionLocal;
import se.anatom.ejbca.ca.store.ICertificateStoreSessionLocalHome;
import se.anatom.ejbca.ca.store.certificateprofiles.CertificateProfile;
import se.anatom.ejbca.log.Admin;
import se.anatom.ejbca.log.ILogSessionLocal;
import se.anatom.ejbca.log.ILogSessionLocalHome;
import se.anatom.ejbca.log.LogEntry;
import se.anatom.ejbca.protocol.FailInfo;
import se.anatom.ejbca.protocol.IRequestMessage;
import se.anatom.ejbca.protocol.IResponseMessage;
import se.anatom.ejbca.protocol.ResponseStatus;
import se.anatom.ejbca.util.CertTools;
import se.anatom.ejbca.util.Hex;

/**
 * Creates and isigns certificates.
 *
 * @version $Id: RSASignSessionBean.java,v 1.128 2004-05-22 12:58:52 anatom Exp $
 */
public class RSASignSessionBean extends BaseSessionBean {
    

    /** Local interfacte to ca admin store */
    private CADataLocalHome cadatahome;
    
    /** Home interface to certificate store */
    private ICertificateStoreSessionLocalHome storeHome = null;
        
    /* Home interface to Authentication session */
    private IAuthenticationSessionLocalHome authHome = null;

    /* Home interface to Publisher session */
    private IPublisherSessionLocalHome publishHome = null;
    
    /** The local interface of the log session bean */
    private ILogSessionLocal logsession;
    /**
     * Source of good random data
     */
    SecureRandom randomSource = null;
    

    /**
     * Default create for SessionBean without any creation Arguments.
     *
     * @throws CreateException if bean instance can't be created
     */
    public void ejbCreate() throws CreateException {
        debug(">ejbCreate()");

        try {
             // Install BouncyCastle provider
             CertTools.installBCProvider();

            // get home interfaces to other session beans used
            storeHome = (ICertificateStoreSessionLocalHome) lookup(
                    "java:comp/env/ejb/CertificateStoreSessionLocal");
            authHome = (IAuthenticationSessionLocalHome) lookup(
                    "java:comp/env/ejb/AuthenticationSessionLocal");

            cadatahome = (CADataLocalHome)lookup("java:comp/env/ejb/CADataLocal");
            
            publishHome = (IPublisherSessionLocalHome) lookup("java:comp/env/ejb/PublisherSessionLocal");
            
            // Get a decent source of random data
            String  randomAlgorithm = (String) lookup("java:comp/env/randomAlgorithm");
            randomSource = SecureRandom.getInstance(randomAlgorithm);
            SernoGenerator.setAlgorithm(randomAlgorithm);


        } catch( Exception e ) {
            debug("Caught exception in ejbCreate(): ", e);
            throw new EJBException(e);
        }

        debug("<ejbCreate()");
    }
    
    
    /** Gets connection to log session bean
     */
    private ILogSessionLocal getLogSession() {
        if(logsession == null){
            try{
                ILogSessionLocalHome logsessionhome = (ILogSessionLocalHome) lookup("java:comp/env/ejb/LogSessionLocal",ILogSessionLocalHome.class);
                logsession = logsessionhome.create();
            }catch(Exception e){
                throw new EJBException(e);
            }
        }
        return logsession;
    } //getLogSession   
    

    /**
     *  Returns the Certificate Chain of a CA. 
     *
 	 * @param admin admin performing action!
     * @param caid is the issuerdn.hashCode()
     */
    public Collection getCertificateChain(Admin admin, int caid){
      // get CA
         CADataLocal cadata = null; 
         try{
           cadata = cadatahome.findByPrimaryKey(new Integer(caid));
         }catch(javax.ejb.FinderException fe){         
            throw new EJBException(fe);                   
         }
                
         CA ca = null;
         try{
           ca = cadata.getCA();
         }catch(java.io.UnsupportedEncodingException uee){
           throw new EJBException(uee);   
         }
        
         return ca.getCertificateChain();        
    }  // getCertificateChain


    /**
     * Implements ISignSession::createPKCS7
     *
     * @param admin Information about the administrator or admin preforming the event.
     * @param cert client certificate which we want ancapsulated in a PKCS7 together with
     *        certificate chain. If null, a PKCS7 with only CA certificate chain is returned.
     *
     * @return The DER-encoded PKCS7 message.
     *
     * @throws CADoesntExistsException if the CA does not exist or is expired, or has an invalid cert
     */
    public byte[] createPKCS7(Admin admin, Certificate cert) throws CADoesntExistsException, SignRequestSignatureException {
        Integer caid = new Integer(CertTools.getIssuerDN((X509Certificate) cert).hashCode());
        return createPKCS7(admin, caid.intValue(), cert);
    } // createPKCS7

    /**
     * Implements ISignSession::createPKCS7
     *
     * @param admin Information about the administrator or admin preforming the event.
	 * @param caId CA for which we want a PKCS7 certificate chain.
     *
     * @return The DER-encoded PKCS7 message.
     *
     * @throws CADoesntExistsException if the CA does not exist or is expired, or has an invalid cert
     */
    public byte[] createPKCS7(Admin admin, int caId) throws CADoesntExistsException {
        try {
            return createPKCS7(admin, caId, null);
        } catch (SignRequestSignatureException e) {
            error("Unknown error, strange?", e); 
            throw new EJBException(e);
        }
    } // createPKCS7

    /** Internal helper method
     * @param admin Information about the administrator or admin preforming the event.
	 * @param caId CA for which we want a PKCS7 certificate chain.
     * @param cert client certificate which we want ancapsulated in a PKCS7 together with
     *        certificate chain, or null
     * @return The DER-encoded PKCS7 message.
     * @throws CADoesntExistsException if the CA does not exist or is expired, or has an invalid cert
     */
    private byte[] createPKCS7(Admin admin, int caId, Certificate cert) throws CADoesntExistsException, SignRequestSignatureException {
        debug(">createPKCS7("+caId+", "+CertTools.getIssuerDN((X509Certificate)cert)+")");
        byte[] returnval = null;
         // get CA
         CADataLocal cadata = null; 
         try{
           cadata = cadatahome.findByPrimaryKey(new Integer(caId));
         }catch(javax.ejb.FinderException fe){         
            throw new CADoesntExistsException(fe);                   
         }
                
         CA ca = null;
         try{
           ca = cadata.getCA();
         }catch(java.io.UnsupportedEncodingException uee){
           throw new CADoesntExistsException(uee);   
         }
                
         // Check that CA hasn't expired.
         X509Certificate cacert = (X509Certificate) ca.getCACertificate();                  
         try{
           cacert.checkValidity();                   
         }catch(CertificateExpiredException e){
           // Signers Certificate has expired.   
           cadata.setStatus(SecConst.CA_EXPIRED);         
           throw new CADoesntExistsException("Signing CA " + cadata.getSubjectDN() + " has expired");   
         } catch (CertificateNotYetValidException cve) {
			throw new CADoesntExistsException(cve);
		 }           
        
         returnval = ca.createPKCS7(cert);
         debug("<createPKCS7()");
         return returnval;
    } // createPKCS7

    /**
     * Implements ISignSession::createCertificate
     */
    public Certificate createCertificate(Admin admin, String username, String password, PublicKey pk) throws ObjectNotFoundException, AuthStatusException, AuthLoginException, IllegalKeyException, CADoesntExistsException {
        debug(">createCertificate(pk)");
        // Default key usage is defined in certificate profiles
        debug("<createCertificate(pk)");
        return createCertificate(admin, username, password, pk, -1);
    } // createCertificate

    /**
     * Implements ISignSession::createCertificate
     */
    public Certificate createCertificate(Admin admin, String username, String password, PublicKey pk, boolean[] keyusage) throws ObjectNotFoundException, AuthStatusException, AuthLoginException, IllegalKeyException, CADoesntExistsException {
        return createCertificate(admin, username, password, pk, sunKeyUsageToBC(keyusage));                    
    }

    /**
     * Implements ISignSession::createCertificate
     */
    public Certificate createCertificate(Admin admin, String username, String password, PublicKey pk, int keyusage) throws ObjectNotFoundException, AuthStatusException, AuthLoginException, IllegalKeyException, CADoesntExistsException {
        debug(">createCertificate(pk, ku)");        
        try {
            // Authorize user and get DN
            UserAuthData data = authUser(admin, username, password);
            debug("Authorized user " + username + " with DN='" + data.getDN()+"'." + " with CA=" + data.getCAId());
            
            debug("type="+ data.getType());
            // get CA
            CADataLocal cadata = null; 
            try{
              cadata = cadatahome.findByPrimaryKey(new Integer(data.getCAId()));
            }catch(javax.ejb.FinderException fe){
              getLogSession().log(admin, data.getCAId(), LogEntry.MODULE_CA, new java.util.Date(),data.getUsername(), null, LogEntry.EVENT_ERROR_CREATECERTIFICATE,"Invalid CA Id",fe);  
              throw new CADoesntExistsException();                   
            }
            CA ca = null;
            try{
              ca = cadata.getCA();
            }catch(java.io.UnsupportedEncodingException uee){
               throw new EJBException(uee);   
            }
            // Check that CA hasn't expired.
            X509Certificate cacert = (X509Certificate) ca.getCACertificate();         
            
            if(ca.getStatus() != SecConst.CA_ACTIVE){
		      getLogSession().log(admin, data.getCAId(), LogEntry.MODULE_CA,  new java.util.Date(), null, null, LogEntry.EVENT_ERROR_CREATECERTIFICATE,"Signing CA " + cadata.getSubjectDN() + " isn't active.");
			  throw new EJBException("Signing CA " + cadata.getSubjectDN() + " isn't active.");             
            }
                     
            try{
                cacert.checkValidity();                   
            }catch(CertificateExpiredException cee){
                 // Signers Certificate has expired.   
                cadata.setStatus(SecConst.CA_EXPIRED);  
                getLogSession().log(admin, data.getCAId(), LogEntry.MODULE_CA,  new java.util.Date(), null, null, LogEntry.EVENT_ERROR_CREATECERTIFICATE,"Signing CA " + cadata.getSubjectDN() + " has expired",cee);
                throw new EJBException("Signing CA " + cadata.getSubjectDN() + " has expired");   
            } catch (CertificateNotYetValidException cve) {
				throw new EJBException(cve);
			}                
            

            // Now finally after all these checks, get the certificate
            Certificate cert = createCertificate(admin, data, ca, pk, keyusage);
            // Call authentication session and tell that we are finished with this user
            if (ca.getFinishUser() == true) {
                finishUser(admin, username, password);
            }
            debug("<createCertificate(pk, ku)");
            return cert;
        } catch (ObjectNotFoundException oe) {
            throw oe;
        } catch (AuthStatusException se) {
            throw se;
        } catch (AuthLoginException le) {
            throw le;
        } catch (IllegalKeyException ke) {
            throw ke;
        } 
    } // createCertificate

    /**
     * Implements ISignSession::createCertificate
     */
    public Certificate createCertificate(Admin admin, String username, String password, int certType, PublicKey pk) throws ObjectNotFoundException, AuthStatusException, AuthLoginException, IllegalKeyException, CADoesntExistsException {
        debug(">createCertificate(pk, certType)");
        // Create an array for KeyUsage acoording to X509Certificate.getKeyUsage()
        boolean[] keyusage = new boolean[9];
        Arrays.fill(keyusage, false);
        switch (certType) {
            case CertificateData.CERT_TYPE_ENCRYPTION:
                // keyEncipherment
                keyusage[2] = true;
                // dataEncipherment
                keyusage[3] = true;
                break;
            case CertificateData.CERT_TYPE_SIGNATURE:
                // digitalSignature
                keyusage[0] = true;
                // non-repudiation
                keyusage[1] = true;
                break;
            default:
                // digitalSignature
                keyusage[0] = true;
                // keyEncipherment
                keyusage[2] = true;
                break;
        }

        Certificate ret = createCertificate(admin, username, password, pk, keyusage);
        debug("<createCertificate(pk, certType)");
        return ret;
    } // createCertificate

    /**
     * Implements ISignSession::createCertificate
     */
    public Certificate createCertificate(Admin admin, String username, String password, Certificate incert) throws ObjectNotFoundException, AuthStatusException, AuthLoginException, IllegalKeyException, SignRequestSignatureException, CADoesntExistsException {
        debug(">createCertificate(cert)");
        X509Certificate cert = (X509Certificate)incert;
        try {
            cert.verify(cert.getPublicKey());
        }catch (Exception e) {                       
           throw new SignRequestSignatureException("Verification of signature (popo) on certificate failed.");
        }
        Certificate ret = createCertificate(admin, username, password, cert.getPublicKey(), cert.getKeyUsage());
        debug("<createCertificate(cert)");
        return ret;
    } // createCertificate

    /**
     * Implements ISignSession::createCertificate
     */
    public IResponseMessage createCertificate(Admin admin, IRequestMessage req, Class responseClass) throws ObjectNotFoundException, AuthStatusException, AuthLoginException, IllegalKeyException, CADoesntExistsException, SignRequestException, SignRequestSignatureException{
        return createCertificate(admin, req, -1, responseClass);
    }

    /**
     * Implements ISignSession::createCertificate
     *
     * @param admin Information about the administrator or admin preforming the event.
     * @param req a Certification Request message, containing the public key to be put in the
     *        created certificate. Currently no additional parameters in requests are considered!
     * @param keyUsage integer with bit mask describing desired keys usage. Bit mask is packed in
     *        in integer using contants from CertificateData. ex. int keyusage =
     *        CertificateData.digitalSignature | CertificateData.nonRepudiation; gives
     *        digitalSignature and nonRepudiation. ex. int keyusage = CertificateData.keyCertSign
     *        | CertificateData.cRLSign; gives keyCertSign and cRLSign. Keyusage < 0 means that default 
     *        keyUsage should be used.
     * @param responseClass the implementation class of the desired response 
     *
     * @return The newly created certificate or null.
     *
     * @throws ObjectNotFoundException if the user does not exist.
     * @throws AuthStatusException If the users status is incorrect.
     * @throws AuthLoginException If the password is incorrect.
     * @throws IllegalKeyException if the public key is of wrong type.
     * @throws CADoesntExistsException if the targeted CA does not exist
     * @throws SignRequestException if the provided request is invalid.
     * @throws SignRequestSignatureException if the provided client certificate was not signed by
     *         the CA.
     */
    public IResponseMessage createCertificate(Admin admin, IRequestMessage req, int keyUsage, Class responseClass) throws ObjectNotFoundException, AuthStatusException, AuthLoginException, IllegalKeyException, CADoesntExistsException, SignRequestException, SignRequestSignatureException {
        debug(">createCertificate(IRequestMessage)");
        IResponseMessage ret = null;

        // Get CA that will receive request
        CADataLocal cadata = null; 
        UserAuthData data = null;
        try{
            // See if we can get issuerDN directly from request
            if (req.getIssuerDN() != null) {
                cadata = cadatahome.findByPrimaryKey(new Integer(req.getIssuerDN().hashCode()));
                debug("Using CA (from issuerDN) with id: "+cadata.getCAId()+" and DN: "+cadata.getSubjectDN());
            } else if (req.getUsername() != null ){
                // See if we can get username and password directly from request
                String username = req.getUsername();
                String password = req.getPassword();
                data = authUser(admin, username, password);
                cadata = cadatahome.findByPrimaryKey(new Integer(data.getCAId()));
                debug("Using CA (from username) with id: "+cadata.getCAId()+" and DN: "+cadata.getSubjectDN());
            } else {
                throw new CADoesntExistsException();
            }
        }catch(javax.ejb.FinderException fe) {
            error("Can not find CA Id from issuerDN: "+req.getIssuerDN() + " or username: "+req.getUsername());
            getLogSession().log(admin, -1, LogEntry.MODULE_CA, new java.util.Date(),req.getUsername(), null, LogEntry.EVENT_ERROR_CREATECERTIFICATE,"Invalid CA Id",fe);  
            throw new CADoesntExistsException(fe);                   
        }
        try {
            CA ca = cadata.getCA();
            CAToken catoken = ca.getCAToken();
            
			if(ca.getStatus() != SecConst.CA_ACTIVE){
			  getLogSession().log(admin, cadata.getCAId().intValue(), LogEntry.MODULE_CA,  new java.util.Date(), null, null, LogEntry.EVENT_ERROR_CREATECERTIFICATE,"Signing CA " + cadata.getSubjectDN() + " isn't active.");
			  throw new EJBException("Signing CA " + cadata.getSubjectDN() + " isn't active.");             
			}
			
            // Check that CA hasn't expired.
            X509Certificate cacert = (X509Certificate) ca.getCACertificate();                  
            try{	
                cacert.checkValidity();                   
            }catch(CertificateExpiredException cee){
                 // Signers Certificate has expired.   
                cadata.setStatus(SecConst.CA_EXPIRED);  
                getLogSession().log(admin, cadata.getCAId().intValue(), LogEntry.MODULE_CA,  new java.util.Date(), null, null, LogEntry.EVENT_ERROR_CREATECERTIFICATE,"Signing CA " + cadata.getSubjectDN() + " has expired",cee);
                throw new CADoesntExistsException("Signing CA " + cadata.getSubjectDN() + " has expired");   
            } catch (CertificateNotYetValidException cve) {
				throw new CADoesntExistsException(cve); 
			}                

             
            if (req.requireKeyInfo()) {
                // You go figure...scep encrypts message with the public CA-cert
                req.setKeyInfo((X509Certificate)ca.getCACertificate(), catoken.getPrivateKey(SecConst.CAKEYPURPOSE_CERTSIGN));
            }
            // Create the response message and set all required fields
            try {
                ret = (IResponseMessage) responseClass.newInstance();
            } catch (InstantiationException e) {
                //TODO : do something with these exceptions
                log.error("Error creating response message",e);
                return null;
            } catch (IllegalAccessException e) {
                log.error("Error creating response message",e);
                return null;
            }
            if (ret.requireSignKeyInfo()) {
                ret.setSignKeyInfo((X509Certificate)ca.getCACertificate(), catoken.getPrivateKey(SecConst.CAKEYPURPOSE_CERTSIGN));
            }
            if (ret.requireEncKeyInfo()) {
                ret.setEncKeyInfo((X509Certificate)ca.getCACertificate(), catoken.getPrivateKey(SecConst.CAKEYPURPOSE_KEYENCRYPT));
            }
            if (req.getSenderNonce() != null) {
                ret.setRecipientNonce(req.getSenderNonce());
            }
            if (req.getTransactionId() != null) {
                ret.setTransactionId(req.getTransactionId());
            }
            // Sendernonce is a random number
            byte[] senderNonce = new byte[16];
            randomSource.nextBytes(senderNonce);
            ret.setSenderNonce(Hex.encode(senderNonce));
            // If we have a specified request key info, use it in the reply
            if (req.getRequestKeyInfo() != null) {
                ret.setRecipientKeyInfo(req.getRequestKeyInfo());
            }
            // Verify the request
            if (req.verify() == false) {
                getLogSession().log(admin, cadata.getCAId().intValue(), LogEntry.MODULE_CA,new java.util.Date(),req.getUsername(),null,LogEntry.EVENT_ERROR_CREATECERTIFICATE,"POPO verification failed.");
                throw new SignRequestSignatureException("Verification of signature (popo) on request failed.");
            }
            if ((req.getUsername() == null ) || (req.getPassword() == null)) {
                log.error("No username/password in request");
                throw new SignRequestException("No username/password in request!");
                //ret.setFailInfo(FailInfo.BAD_REQUEST);
                //ret.setStatus(ResponseStatus.FAILURE);
            } else {
                // If we haven't done so yet, authenticate user
                if (data == null) {
                    data = authUser(admin, req.getUsername(), req.getPassword());
                }    
                PublicKey reqpk = req.getRequestPublicKey();
                if (reqpk == null) {
                    throw new InvalidKeyException("Key is null!");
                }
                Certificate cert = null;
                cert = createCertificate(admin,data,ca,reqpk,keyUsage);        
                if (cert != null) {
                    ret.setCertificate(cert);
                    ret.setStatus(ResponseStatus.SUCCESS);
                } else {
                    ret.setStatus(ResponseStatus.FAILURE);
                    ret.setFailInfo(FailInfo.BAD_REQUEST);
                }
            }
            ret.create();
            // Call authentication session and tell that we are finished with this user
            if (ca.getFinishUser() == true) {
                finishUser(admin, req.getUsername(), req.getPassword());
            }
            // TODO: handle returning errors as response message,
            // javax.ejb.ObjectNotFoundException and the others thrown...
        } catch (ObjectNotFoundException oe) {
            throw oe;
        } catch (AuthStatusException se) {
            throw se;
        } catch (AuthLoginException le) {
            throw le;
        } catch (IllegalKeyException ke) {
            log.error("Key is of unknown type: ", ke);
            throw ke;
        } catch (IllegalKeyStoreException e) {
            throw new IllegalKeyException(e);
        } catch (UnsupportedEncodingException e) {
            throw new CADoesntExistsException(e);
        } catch (NoSuchProviderException e) {
            log.error("NoSuchProvider provider: ", e);
        } catch (InvalidKeyException e) {
            log.error("Invalid key in request: ", e);
        } catch (NoSuchAlgorithmException e) {
            log.error("No such algorithm: ", e);
        } catch (IOException e) {
            log.error("Cannot create response message: ", e);
        } catch (CATokenOfflineException ctoe) {
            log.error("CA Token is Offline: ", ctoe);
            cadata.setStatus(SecConst.CA_OFFLINE);  
            getLogSession().log(admin, cadata.getCAId().intValue(), LogEntry.MODULE_CA,  new java.util.Date(), null, null, LogEntry.EVENT_ERROR_CREATECERTIFICATE,"Signing CA " + cadata.getSubjectDN() + " is offline.",ctoe);
            throw new CADoesntExistsException("Signing CA " + cadata.getSubjectDN() + " is offline.");   
		} 
        debug("<createCertificate(IRequestMessage)");
        return ret;
    }

    /**
     * Implements ISignSession::getCRL
     *
     * @param admin Information about the administrator or admin preforming the event.
     * @param req a CRL Request message
     * @param responseClass the implementation class of the desired response 
     *
     * @return The newly created certificate or null.
     *
     * @throws ObjectNotFoundException if the user does not exist.
     * @throws AuthStatusException If the users status is incorrect.
     * @throws AuthLoginException If the password is incorrect.
     * @throws IllegalKeyException if the public key is of wrong type.
     * @throws CADoesntExistsException if the targeted CA does not exist
     * @throws SignRequestException if the provided request is invalid.
     * @throws SignRequestSignatureException if the provided client certificate was not signed by
     *         the CA.
     */
    public IResponseMessage getCRL(Admin admin, IRequestMessage req, Class responseClass) throws IllegalKeyException, CADoesntExistsException, SignRequestException, SignRequestSignatureException {
        debug(">getCRL(IRequestMessage)");
        IResponseMessage ret = null;
        ICertificateStoreSessionLocal certificateStore = null;
        try {
             certificateStore = storeHome.create();
        } catch (CreateException e) {
            error("Can not create certificate store session: ", e);
            throw new EJBException(e);
        }
        // Get CA that will receive request
        CADataLocal cadata = null; 
        UserAuthData data = null;
        try{
            // See if we can get issuerDN directly from request
            if (req.getIssuerDN() != null) {
                cadata = cadatahome.findByPrimaryKey(new Integer(req.getIssuerDN().hashCode()));
                debug("Using CA (from issuerDN) with id: "+cadata.getCAId()+" and DN: "+cadata.getSubjectDN());
            } else {
                throw new CADoesntExistsException();
            }
        }catch(javax.ejb.FinderException fe) {
            error("Can not find CA Id from issuerDN: "+req.getIssuerDN() + " or username: "+req.getUsername());
            getLogSession().log(admin, -1, LogEntry.MODULE_CA, new java.util.Date(),req.getUsername(), null, LogEntry.EVENT_ERROR_CREATECERTIFICATE,"Invalid CA Id",fe);  
            throw new CADoesntExistsException(fe);                   
        }
        try {
            CA ca = cadata.getCA();
            CAToken catoken = ca.getCAToken();
            
            if(ca.getStatus() != SecConst.CA_ACTIVE){
              getLogSession().log(admin, cadata.getCAId().intValue(), LogEntry.MODULE_CA,  new java.util.Date(), null, null, LogEntry.EVENT_ERROR_CREATECERTIFICATE,"Signing CA " + cadata.getSubjectDN() + " isn't active.");
              throw new EJBException("Signing CA " + cadata.getSubjectDN() + " isn't active.");             
            }
            
            // Check that CA hasn't expired.
            X509Certificate cacert = (X509Certificate) ca.getCACertificate();                  
            try{    
                cacert.checkValidity();                   
            }catch(CertificateExpiredException cee){
                 // Signers Certificate has expired.   
                cadata.setStatus(SecConst.CA_EXPIRED);  
                getLogSession().log(admin, cadata.getCAId().intValue(), LogEntry.MODULE_CA,  new java.util.Date(), null, null, LogEntry.EVENT_ERROR_CREATECERTIFICATE,"Signing CA " + cadata.getSubjectDN() + " has expired",cee);
                throw new CADoesntExistsException("Signing CA " + cadata.getSubjectDN() + " has expired");   
            } catch (CertificateNotYetValidException cve) {
                throw new CADoesntExistsException(cve); 
            }                

             
            if (req.requireKeyInfo()) {
                // You go figure...scep encrypts message with the public CA-cert
                req.setKeyInfo((X509Certificate)ca.getCACertificate(), catoken.getPrivateKey(SecConst.CAKEYPURPOSE_CERTSIGN));
            }
            // Create the response message and set all required fields
            try {
                ret = (IResponseMessage) responseClass.newInstance();
            } catch (InstantiationException e) {
                //TODO : do something with these exceptions
                log.error("Error creating response message",e);
                return null;
            } catch (IllegalAccessException e) {
                log.error("Error creating response message",e);
                return null;
            }
            if (ret.requireSignKeyInfo()) {
                ret.setSignKeyInfo((X509Certificate)ca.getCACertificate(), catoken.getPrivateKey(SecConst.CAKEYPURPOSE_CERTSIGN));
            }
            if (ret.requireEncKeyInfo()) {
                ret.setEncKeyInfo((X509Certificate)ca.getCACertificate(), catoken.getPrivateKey(SecConst.CAKEYPURPOSE_KEYENCRYPT));
            }
            if (req.getSenderNonce() != null) {
                ret.setRecipientNonce(req.getSenderNonce());
            }
            if (req.getTransactionId() != null) {
                ret.setTransactionId(req.getTransactionId());
            }
            // Sendernonce is a random number
            byte[] senderNonce = new byte[16];
            randomSource.nextBytes(senderNonce);
            ret.setSenderNonce(Hex.encode(senderNonce));
            // If we have a specified request key info, use it in the reply
            if (req.getRequestKeyInfo() != null) {
                ret.setRecipientKeyInfo(req.getRequestKeyInfo());
            }
            // Verify the request
            if (req.verify() == false) {
                getLogSession().log(admin, cadata.getCAId().intValue(), LogEntry.MODULE_CA,new java.util.Date(),req.getUsername(),null,LogEntry.EVENT_ERROR_CREATECERTIFICATE,"POPO verification failed.");
                throw new SignRequestSignatureException("Verification of signature (popo) on request failed.");
            }
            // Get the CRL, don't even bother digging into the encrypted CRLIssuerDN...since we already 
            // know that we are the CA (SCEP is soooo stupid!)
            byte[] crl = certificateStore.getLastCRL(admin, req.getIssuerDN());
            if (crl != null) {
                ret.setCrl(CertTools.getCRLfromByteArray(crl));
                ret.setStatus(ResponseStatus.SUCCESS);
            } else {
                ret.setStatus(ResponseStatus.FAILURE);
                ret.setFailInfo(FailInfo.BAD_REQUEST);
            }
            ret.create();
            // TODO: handle returning errors as response message,
            // javax.ejb.ObjectNotFoundException and the others thrown...
        } catch (IllegalKeyStoreException e) {
            throw new IllegalKeyException(e);
        } catch (UnsupportedEncodingException e) {
            throw new CADoesntExistsException(e);
        } catch (NoSuchProviderException e) {
            log.error("NoSuchProvider provider: ", e);
        } catch (InvalidKeyException e) {
            log.error("Invalid key in request: ", e);
        } catch (NoSuchAlgorithmException e) {
            log.error("No such algorithm: ", e);
        } catch (CRLException e) {
            log.error("Cannot create response message: ", e);
        } catch (CertificateException e) {
            log.error("Cannot create response message: ", e);
        } catch (IOException e) {
            log.error("Cannot create response message: ", e);
        } catch (CATokenOfflineException ctoe) {
            log.error("CA Token is Offline: ", ctoe);
            cadata.setStatus(SecConst.CA_OFFLINE);  
            getLogSession().log(admin, cadata.getCAId().intValue(), LogEntry.MODULE_CA,  new java.util.Date(), null, null, LogEntry.EVENT_ERROR_CREATECERTIFICATE,"Signing CA " + cadata.getSubjectDN() + " is offline.",ctoe);
            throw new CADoesntExistsException("Signing CA " + cadata.getSubjectDN() + " is offline.");   
        } 
        debug("<getCRL(IRequestMessage)");
        return ret;
    }

    /**
     * Implements ISignSession::createCRL
     */
    public X509CRL createCRL(Admin admin, int caid, Vector certs) {
        debug(">createCRL()");
        X509CRL crl = null;
        try {
          // get CA
          CADataLocal cadata = null; 
          try{
             cadata = cadatahome.findByPrimaryKey(new Integer(caid));
          }catch(javax.ejb.FinderException fe){
             getLogSession().log(admin, caid, LogEntry.MODULE_CA, new java.util.Date(),null, null, LogEntry.EVENT_ERROR_CREATECRL,"Invalid CA Id",fe);  
             throw new EJBException(fe);                   
          }
                
          CA ca = null;
          try{
            ca = cadata.getCA();
          }catch(java.io.UnsupportedEncodingException uee){
            throw new EJBException(uee);   
          }
		  if(ca.getStatus() != SecConst.CA_ACTIVE){
			getLogSession().log(admin, caid, LogEntry.MODULE_CA,  new java.util.Date(), null, null, LogEntry.EVENT_ERROR_CREATECERTIFICATE,"Signing CA " + cadata.getSubjectDN() + " isn't active.");
			throw new EJBException("Signing CA " + cadata.getSubjectDN() + " isn't active.");             
		  }
                
          // Check that CA hasn't expired.
          X509Certificate cacert = (X509Certificate) ca.getCACertificate();                  
          try{
            cacert.checkValidity();                   
          }catch(CertificateExpiredException e){
            // Signers Certificate has expired.   
            cadata.setStatus(SecConst.CA_EXPIRED);  
            getLogSession().log(admin, caid, LogEntry.MODULE_CA,  new java.util.Date(), null, null, LogEntry.EVENT_ERROR_CREATECRL,"Signing CA " + cadata.getSubjectDN() + " has expired",e);
            throw new EJBException("Signing CA " + cadata.getSubjectDN() + " has expired");   
          }catch(CertificateNotYetValidException e){
          	 throw new EJBException(e);             
          }
          
          
          ICertificateStoreSessionLocal certificateStore = storeHome.create();
           // Get number of last CRL and increase by 1
          int number = certificateStore.getLastCRLNumber(admin, ca.getSubjectDN()) + 1;
          try{
            crl = (X509CRL) ca.generateCRL(certs, number);
          } catch(CATokenOfflineException ctoe) {
            log.error("CA Token is Offline: ", ctoe);
            cadata.setStatus(SecConst.CA_OFFLINE);  
            getLogSession().log(admin, cadata.getCAId().intValue(), LogEntry.MODULE_CA,  new java.util.Date(), null, null, LogEntry.EVENT_ERROR_CREATECRL,"Signing CA " + cadata.getSubjectDN() + " is offline.",ctoe);
            throw new EJBException("Signing CA " + cadata.getSubjectDN() + " is offline.");
          }
          getLogSession().log(admin, caid, LogEntry.MODULE_CA, new java.util.Date(),null, null, LogEntry.EVENT_INFO_CREATECRL,"Number :" + number);
          
          // Store CRL in the database
          String fingerprint = CertTools.getFingerprintAsString(cacert);
          certificateStore.storeCRL(admin, crl.getEncoded(), fingerprint, number);
          // Store crl in ca CRL publishers.
          IPublisherSessionLocal pub = publishHome.create();
          pub.storeCRL(admin, ca.getCRLPublishers(), crl.getEncoded(), fingerprint, number);
          
        } catch (Exception e) {          
            getLogSession().log(admin, caid, LogEntry.MODULE_CA, new java.util.Date(),null, null, LogEntry.EVENT_ERROR_CREATECRL,"");          
            throw new EJBException(e);
        }
        debug("<createCRL()");
        return crl;
    } // createCRL
    
    /** Method that publishes the given CA certificate chain to the list of publishers.
    * Is mainly used by CAAdminSessionBean when CA is created.
    *  @see se.anatom.ejbca.ca.sign.ISignSessionRemote
    */
   public void publishCACertificate(Admin admin, Collection certificatechain, Collection usedpublishers, int certtype){
       try{
                 
           ICertificateStoreSessionLocal certificateStore = storeHome.create();
      
           Iterator certificates = certificatechain.iterator();
           while(certificates.hasNext()){
               Certificate cacert = (Certificate) certificates.next();
          
                 //     Store CA certificate in the database
                String fingerprint = CertTools.getFingerprintAsString((X509Certificate) cacert);
           
                if(certificateStore.findCertificateByFingerprint(admin, fingerprint) == null){                                
                    certificateStore.storeCertificate(admin, cacert, "SYSTEMCA", fingerprint, CertificateData.CERT_ACTIVE, certtype);
                }  
                // Store cert in ca cert publishers.
                IPublisherSessionLocal pub = publishHome.create();
                if(usedpublishers != null)
                  pub.storeCertificate(admin, usedpublishers, cacert, fingerprint, null , fingerprint, CertificateData.CERT_ACTIVE, certtype, null);
        }
       }catch(javax.ejb.CreateException ce){
           throw new EJBException(ce);   
       }
   }

    private String getPassword(String initKey) throws Exception {
        String password;
        try {
            password = (String)lookup(initKey, java.lang.String.class);
        } catch (EJBException e) {
            password = null;
        }
        if ( password == null ) {
            debug(initKey+" password: ");
            BufferedReader in
            = new BufferedReader(new InputStreamReader(System.in));
            return (in.readLine());
        } else
            return password;
    }    

    private int sunKeyUsageToBC(boolean[] sku) {
        int bcku = 0;
        if (sku[0] == true)
            bcku = bcku | X509KeyUsage.digitalSignature;
        if (sku[1] == true)
            bcku = bcku | X509KeyUsage.nonRepudiation;
        if (sku[2] == true)
            bcku = bcku | X509KeyUsage.keyEncipherment;
        if (sku[3] == true)
            bcku = bcku | X509KeyUsage.dataEncipherment;
        if (sku[4] == true)
            bcku = bcku | X509KeyUsage.keyAgreement;
        if (sku[5] == true)
            bcku = bcku | X509KeyUsage.keyCertSign;
        if (sku[6] == true)
            bcku = bcku | X509KeyUsage.cRLSign;
        if (sku[7] == true)
            bcku = bcku | X509KeyUsage.encipherOnly;
        if (sku[8] == true)
            bcku = bcku | X509KeyUsage.decipherOnly;
        return bcku;
    }    
    
    private UserAuthData authUser(Admin admin, String username, String password) throws ObjectNotFoundException, AuthStatusException, AuthLoginException {
        // Authorize user and get DN
        try {
            IAuthenticationSessionLocal authSession = authHome.create();
            return authSession.authenticateUser(admin, username, password);
        } catch (CreateException e) {
            log.error(e);
            throw new EJBException(e);
        }
 
    } // authUser
    private void finishUser(Admin admin, String username, String password) throws ObjectNotFoundException {
        // Finnish user and set new status
        try {
            IAuthenticationSessionLocal authSession = authHome.create();
            authSession.finishUser(admin, username, password);
        } catch (CreateException e) {
            log.error(e);
            throw new EJBException(e);
        }
    } // finishUser

    /** Creates the certificate, does NOT check any authorization on user, profiles or CA! 
     * This must be done earlier
     * 
     * @param admin administrator performing this task
     * @param data auth data for user to get the certificate
     * @param ca the CA that will sign the certificate
     * @param pk ther users public key to be put in the certificate
     * @param keyusage requested key usage for the certificate, may be ignored by the CA
     * @throws IllegalKeyException if the public key given is invalid
     * @return Certificate that has been generated and signed by the CA
     */
    private Certificate createCertificate(Admin admin, UserAuthData data, CA ca, PublicKey pk, int keyusage) throws IllegalKeyException {
        debug(">createCertificate(pk, ku)");        
        try {
            // If the user is of type USER_INVALID, it cannot have any other type (in the mask)
            if (data.getType() == SecConst.USER_INVALID) {
                getLogSession().log(admin, data.getCAId(), LogEntry.MODULE_CA, new java.util.Date(),data.getUsername(), null, LogEntry.EVENT_ERROR_CREATECERTIFICATE,"User type is invalid, cannot create certificate for this user.");
            } else {

                ICertificateStoreSessionLocal certificateStore = storeHome.create();
                // Retrieve the certificate profile this user should have
                int certProfileId = data.getCertProfileId();
                CertificateProfile certProfile = certificateStore.getCertificateProfile(admin, certProfileId);
                // What if certProfile == null?
                if (certProfile == null) {
                    certProfileId = SecConst.CERTPROFILE_FIXED_ENDUSER;
                    certProfile = certificateStore.getCertificateProfile(admin, certProfileId);
                }
                
                // Check that CAid is among available CAs
                boolean caauthorized = false;
                Iterator iter = certProfile.getAvailableCAs().iterator();
                while(iter.hasNext()){
                  int next = ((Integer) iter.next()).intValue();
                  if(next == data.getCAId() || next == CertificateProfile.ANYCA){
                    caauthorized = true;  
                  }                    
                }
                
                // Sign Session bean is only able to issue certificates with a end entity type certificate profile.
                if(certProfile.getType() != CertificateProfile.TYPE_ENDENTITY){
                  getLogSession().log(admin, data.getCAId(), LogEntry.MODULE_CA, new java.util.Date(),data.getUsername(), null, LogEntry.EVENT_ERROR_CREATECERTIFICATE,"Wrong type of Certificate Profile for end entity. Only End Entity Certificate Profiles can be issued by signsession bean.");  
                  throw new EJBException("Wrong type of Certificate Profile for end entity. Only End Entity Certificate Profiles can be issued by signsession bean.");  
                }
                
                if(!caauthorized){
                  getLogSession().log(admin, data.getCAId(), LogEntry.MODULE_CA, new java.util.Date(),data.getUsername(), null, LogEntry.EVENT_ERROR_CREATECERTIFICATE,"End Entity data contains a CA which the Certificate Profile isn't authorized to use.");  
                  throw new EJBException("End Entity data contains a CA which the Certificate Profile isn't authorized to use.");
                }
                                                
                log.debug("Using certificate profile with id "+certProfileId);
                int keyLength;
                try {
                  keyLength = ((RSAPublicKey)pk).getModulus().bitLength();
                } catch (ClassCastException e) {
                  throw new
                    IllegalKeyException("Unsupported public key (" +
                                        pk.getClass().getName() +
                                        "), only RSA keys are supported.");
                }
                log.debug("Keylength = "+keyLength); // bitBength() will return 1 less bit if BigInt i negative
                if ( (keyLength < (certProfile.getMinimumAvailableBitLength()-1))
                    || (keyLength > (certProfile.getMaximumAvailableBitLength())) ) {
                        String msg = "Illegal key length "+keyLength;
                        log.error(msg);
                        throw new IllegalKeyException(msg);
                    }
 
                X509Certificate   cert = (X509Certificate) ca.generateCertificate(data, pk, keyusage, certProfile);
                
                getLogSession().log(admin, data.getCAId(), LogEntry.MODULE_CA, new java.util.Date(),data.getUsername(), cert, LogEntry.EVENT_INFO_CREATECERTIFICATE,"");
                debug("Generated certificate with SerialNumber '" + Hex.encode(cert.getSerialNumber().toByteArray())+"' for user '"+data.getUsername()+"'.");
                debug(cert.toString());
                
                // Store certificate in the database
                String fingerprint = CertTools.getFingerprintAsString(cert);
                certificateStore.storeCertificate(admin, cert, data.getUsername(), fingerprint, CertificateData.CERT_ACTIVE, certProfile.getType());
                // Store certificate in certificate profiles publishers.
                IPublisherSessionLocal pub = publishHome.create();
                if(certProfile.getPublisherList() != null)
                  pub.storeCertificate(admin, certProfile.getPublisherList(), cert, data.getUsername(), data.getPassword(), fingerprint, CertificateData.CERT_ACTIVE, certProfile.getType(), data.getExtendedInformation());
                                                
                debug("<createCertificate(pk, ku)");
                return cert;
            }
        } catch (IllegalKeyException ke) {
            throw ke;
        } catch (CATokenOfflineException ctoe) {        	
        	ca.setStatus(SecConst.CA_OFFLINE);
        	throw new EJBException("Error CA Token is Offline", ctoe);
        }catch (Exception e) {
            log.error(e);
            throw new EJBException(e);
        }
        debug("<createCertificate(pk, ku)");
        log.error("Invalid user type for user "+data.getUsername());
        throw new EJBException("Invalid user type for user "+data.getUsername());    
    } // createCertificate
    
	/** 
	 * Method used to perform the extended service
	 */
	public ExtendedCAServiceResponse extendedService(Admin admin, int caid, ExtendedCAServiceRequest request) 
	  throws ExtendedCAServiceRequestException, IllegalExtendedCAServiceRequestException, ExtendedCAServiceNotActiveException, CADoesntExistsException{

		// Get CA that will process request
		CADataLocal cadata = null; 		
		ExtendedCAServiceResponse returnval = null;
		try{			
		  cadata = cadatahome.findByPrimaryKey(new Integer(caid));
		  returnval = cadata.getCA().extendedService(request);			
		}catch(javax.ejb.FinderException fe){			 
			throw new CADoesntExistsException(fe);                   
		}catch(UnsupportedEncodingException ue){
			throw new EJBException(ue);
		}


		return returnval;
	  		     
	}
    

    
} //RSASignSessionBean
