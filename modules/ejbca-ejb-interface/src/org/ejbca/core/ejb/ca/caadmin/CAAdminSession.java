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
package org.ejbca.core.ejb.ca.caadmin;

import java.io.UnsupportedEncodingException;
import java.security.cert.CertPathValidatorException;
import java.util.Collection;

import javax.ejb.CreateException;
import javax.ejb.EJBException;

import org.ejbca.core.EjbcaException;
import org.ejbca.core.model.approval.ApprovalException;
import org.ejbca.core.model.approval.WaitingForApprovalException;
import org.ejbca.core.model.authorization.AuthorizationDeniedException;
import org.ejbca.core.model.ca.caadmin.CADoesntExistsException;
import org.ejbca.core.model.ca.caadmin.CAExistsException;
import org.ejbca.core.model.ca.caadmin.CAInfo;
import org.ejbca.core.model.ca.caadmin.IllegalKeyStoreException;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.ExtendedCAServiceInfo;
import org.ejbca.core.model.ca.catoken.CATokenAuthenticationFailedException;
import org.ejbca.core.model.ca.catoken.CATokenOfflineException;
import org.ejbca.core.model.log.Admin;
import org.ejbca.core.model.ra.raadmin.GlobalConfiguration;
import org.ejbca.core.protocol.IRequestMessage;
import org.ejbca.core.protocol.IResponseMessage;

public interface CAAdminSession {

	/**
     * A method designed to be called at startuptime to speed up the (next)
     * first request to a CA. This method will initialize the CA-cache with all
     * CAs, if they are not already in the cache. Can have a side-effect of
     * upgrading a CA, therefore the Required transaction setting.
     * 
     * @param admin
     *            administrator calling the method
     */
    public void initializeAndUpgradeCAs(Admin admin);
	
	/**
     * Method used to create a new CA. The cainfo parameter should at least
     * contain the following information. SubjectDN Name (if null then is
     * subjectDN used). Validity a CATokenInfo Description (optional) Status
     * (SecConst.CA_ACTIVE or SecConst.CA_WAITING_CERTIFICATE_RESPONSE) SignedBy
     * (CAInfo.SELFSIGNED, CAInfo.SIGNEDBYEXTERNALCA or CAId of internal CA) For
     * other optional values see:
     * 
     * @see org.ejbca.core.model.ca.caadmin.CAInfo
     * @see org.ejbca.core.model.ca.caadmin.X509CAInfo
     */
    public void createCA(Admin admin, CAInfo cainfo) throws CAExistsException, AuthorizationDeniedException, CATokenOfflineException,
            CATokenAuthenticationFailedException;

    /**
     * Method used to edit the data of a CA. Not all of the CAs data can be
     * edited after the creation, therefore will only the values from CAInfo
     * that is possible be uppdated.
     * 
     * @param cainfo
     *            CAInfo object containing values that will be updated For
     *            values see:
     * @see org.ejbca.core.model.ca.caadmin.CAInfo
     * @see org.ejbca.core.model.ca.caadmin.X509CAInfo
     */
    public void editCA(Admin admin, CAInfo cainfo) throws AuthorizationDeniedException;

    /**
     * Method used to remove a CA from the system. You should first check that
     * the CA isn't used by any EndEntity, Profile or AccessRule before it is
     * removed. CADataHandler for example makes this check. Should be used with
     * care. If any certificate has been created with the CA use revokeCA
     * instead and don't remove it.
     */
    public void removeCA(Admin admin, int caid) throws AuthorizationDeniedException;

    /**
     * Renames the name of CA used in administrators web interface. This name
     * doesn't have to be the same as SubjectDN and is only used for reference.
     */
    public void renameCA(Admin admin, String oldname, String newname) throws CAExistsException, AuthorizationDeniedException;

    /**
     * Returns a value object containing nonsensitive information about a CA
     * give it's name.
     * 
     * @param admin
     *            administrator calling the method
     * @param name
     *            human readable name of CA
     * @return value object
     * @throws CADoesntExistsException
     *             if no such CA exists
     */
    public org.ejbca.core.model.ca.caadmin.CAInfo getCAInfoOrThrowException(Admin admin, String name)
            throws CADoesntExistsException;

    /**
     * Returns a value object containing nonsensitive information about a CA
     * give it's name.
     * 
     * @param admin
     *            administrator calling the method
     * @param name
     *            human readable name of CA
     * @return value object or null if CA does not exist
     */
    public org.ejbca.core.model.ca.caadmin.CAInfo getCAInfo(Admin admin, String name);

    /**
     * Returns a value object containing nonsensitive information about a CA
     * give it's CAId.
     * 
     * @param admin
     *            administrator calling the method
     * @param caid
     *            numerical id of CA (subjectDN.hashCode())
     * @return value object
     * @throws CADoesntExistsException
     *             if no such CA exists
     */
    public org.ejbca.core.model.ca.caadmin.CAInfo getCAInfoOrThrowException(Admin admin, int caid)
            throws CADoesntExistsException;

    /**
     * Returns a value object containing nonsensitive information about a CA
     * give it's CAId.
     * 
     * @param admin
     *            administrator calling the method
     * @param caid
     *            numerical id of CA (subjectDN.hashCode())
     * @return value object or null if CA does not exist
     */
    public org.ejbca.core.model.ca.caadmin.CAInfo getCAInfo(Admin admin, int caid);

    /**
     * Returns a value object containing nonsensitive information about a CA
     * give it's CAId. If doSignTest is true, and the CA is active and the CA is
     * included in healthcheck (cainfo.getIncludeInHealthCheck()), a signature
     * with the test keys is performed to set the CA Token status correctly.
     * 
     * @param admin
     *            administrator calling the method
     * @param caid
     *            numerical id of CA (subjectDN.hashCode())
     * @param doSignTest
     *            true if a test signature should be performed, false if only
     *            the status from token info is checked. Should normally be set
     *            to false.
     * @return value object or null if CA does not exist
     */
    public org.ejbca.core.model.ca.caadmin.CAInfo getCAInfo(Admin admin, int caid, boolean doSignTest);

    /**
     * Get the CA object. Does not perform any authorization check. Checks if
     * the CA has expired or the certificate isn't valid yet and in that case
     * sets the correct CA status.
     * 
     * @param admin
     *            is used for logging
     * @param caid
     *            identifies the CA
     * @return the CA object
     * @throws CADoesntExistsException
     *             if no CA was found
     */
    public org.ejbca.core.model.ca.caadmin.CA getCA(Admin admin, int caid) throws CADoesntExistsException;

    /**
     * Makes sure that no CAs are cached to ensure that we read from database next time we try to access it. 
     */
    public void flushCACache();

    /**
     * Verify that a CA exists. (This method does not check admin privileges and
     * will leak the existence of a CA.)
     * 
     * @param caid
     *            is the id of the CA
     * @throws CADoesntExistsException
     *             if the CA isn't found
     */
    public void verifyExistenceOfCA(int caid) throws CADoesntExistsException;

    /**
     * Returns a HashMap containing mappings of caid (Integer) to CA name
     * (String) of all CAs in the system.
     * 
     * @return HashMap with Integer->String mappings
     */
    public java.util.HashMap getCAIdToNameMap(Admin admin);

    /**
     * Method returning id's of all CA's available to the system. i.e. not
     * having status "external" or "waiting for certificate response"
     * 
     * @return a Collection (Integer) of available CA id's
     */
    public Collection getAvailableCAs();

    /**
     * Method returning id's of all CA's available to the system that the
     * administrator is authorized to i.e. not having status "external" or
     * "waiting for certificate response"
     * 
     * @param admin
     *            The administrator
     * @return a Collection<Integer> of available CA id's
     */
    public Collection getAvailableCAs(Admin admin);

    /**
     * Creates a certificate request that should be sent to External Root CA for
     * processing. To create a normal request using the CAs currently active
     * signature keys use false for all of regenerateKeys, usenextkey and
     * activatekey. There are three paths: current key, new key or existing next
     * key. Down these paths there are first two choices activate or don't
     * activate, not applicable for current key that is always active. And
     * lastly the choice if CA status should be set to
     * waiting_for_certificate_response, can be automatically determined
     * depending on if a new key has been activated, making the CA unable to
     * continue issuing certificate until a response is received.
     * 
     * @param admin
     *            the administrator performing the action
     * @param caid
     *            id of the CA that should create the request
     * @param cachain
     *            A Collection of CA-certificates.
     * @param regenerateKeys
     *            if renewing a CA this is used to also generate a new KeyPair,
     *            if this is true and activatekey is false, the new key will not
     *            be activated immediately, but added as "next" signingkey.
     * @param usenextkey
     *            if regenerateKey is true this should be false. Otherwise it
     *            makes a request using an already existing "next" signing key,
     *            perhaps from a previous call with regenerateKeys true.
     * @param activatekey
     *            if regenerateKey is true or usenextkey is true, setting this
     *            flag to true makes the new or "next" key be activated when the
     *            request is created.
     * @param keystorepass
     *            password used when regenerating keys or activating keys, can
     *            be null if regenerateKeys and activatekey is false.
     * @return request message in binary format, can be a PKCS10 or CVC request
     */
    public byte[] makeRequest(Admin admin, int caid, Collection cachain, boolean regenerateKeys, boolean usenextkey, boolean activatekey, String keystorepass)
            throws CADoesntExistsException, AuthorizationDeniedException, CertPathValidatorException,
            CATokenOfflineException, CATokenAuthenticationFailedException;

    /**
     * If the CA can do so, this method signs a nother entitys CSR, for
     * authentication. Prime example of for EU EAC ePassports where the DVs
     * initial certificate request is signed by the CVCA. The signature
     * algorithm used to sign the request will be whatever algorithm the CA uses
     * to sign certificates.
     * 
     * @param admin
     * @param caid
     *            the CA that should sign the request
     * @param request
     *            binary certificate request, the format should be understood by
     *            the CA
     * @return binary certificate request, which is the same as passed in except
     *         also signed by the CA, or it might be the exact same if the CA
     *         does not support request signing
     * @throws AuthorizationDeniedException
     * @throws CADoesntExistsException
     * @throws CATokenOfflineException
     */
    public byte[] signRequest(Admin admin, int caid, byte[] request, boolean usepreviouskey, boolean createlinkcert) throws AuthorizationDeniedException,
            CADoesntExistsException, CATokenOfflineException;

    /**
     * Receives a certificate response from an external CA and sets the newly
     * created CAs status to active.
     * 
     * @param admin
     *            The administrator performing the action
     * @param caid
     *            The caid (DN.hashCode()) of the CA that is receiving this
     *            response
     * @param responsemessage
     *            X509ResponseMessage with the certificate issued to this CA
     * @param chain
     *            an optional collection with the CA certificate(s), or null. If
     *            given the complete chain (except this CAs own certificate must
     *            be given)
     * @param tokenAuthenticationCode
     *            the CA token authentication code, if we need to activate new
     *            CA keys. Otherwise this can be null. This is needed if we have
     *            generated a request with new CA keys, but not activated the
     *            new keys immediately. See makeRequest method
     * @throws EjbcaException
     */
    public void receiveResponse(Admin admin, int caid, IResponseMessage responsemessage, Collection cachain,
            String tokenAuthenticationCode) throws AuthorizationDeniedException, CertPathValidatorException, EjbcaException;

    /**
     * Processes a Certificate Request from an external CA.
     * 
     * @param cainfo
     *            the info for the CA that should be created, or already exists.
     *            Don't forget to set signedBy in the info.
     */
    public IResponseMessage processRequest(Admin admin, CAInfo cainfo,
            IRequestMessage requestmessage) throws CAExistsException, CADoesntExistsException,
            AuthorizationDeniedException, CATokenOfflineException;

    /**
     * Add an external CA's certificate as a CA
     */
    public void importCACertificate(Admin admin, String caname, Collection certificates) throws javax.ejb.CreateException;

    /**
     * Inits an external CA service. this means that a new key and certificate
     * will be generated for this service, if it exists before. If it does not
     * exist before it will be created.
     * 
     * @throws CATokenOfflineException
     * @throws AuthorizationDeniedException
     * @throws IllegalKeyStoreException
     * @throws UnsupportedEncodingException
     */
    public void initExternalCAService(Admin admin, int caid, ExtendedCAServiceInfo info)
            throws CATokenOfflineException, AuthorizationDeniedException, CADoesntExistsException,
            UnsupportedEncodingException, IllegalKeyStoreException;

    /**
     * Renews a existing CA certificate using the same keys as before, or
     * generating new keys. Data about new CA is taken from database. This
     * method is used for renewing CAs internally in EJBCA. For renewing CAs
     * signed by external CAs, makeRequest is used to generate a certificate
     * request.
     * 
     * @param caid
     *            the caid of the CA that will be renewed
     * @param keystorepass
     *            password used when regenerating keys, can be null if
     *            regenerateKeys is false.
     * @param regenerateKeys
     *            , if true and the CA have a softCAToken the keys are
     *            regenerated before the certrequest.
     */
    public void renewCA(Admin admin, int caid, String keystorepass, boolean regenerateKeys) throws CADoesntExistsException,
            AuthorizationDeniedException, java.security.cert.CertPathValidatorException, CATokenOfflineException, CATokenAuthenticationFailedException;

    /**
     * Method that revokes the CA. After this is all certificates created by
     * this CA revoked and a final CRL is created.
     * 
     * @param reason
     *            one of RevokedCertInfo.REVOKATION_REASON values.
     */
    public void revokeCA(Admin admin, int caid, int reason) throws CADoesntExistsException, AuthorizationDeniedException;

    /**
     * Method that should be used when upgrading from EJBCA 3.1 to EJBCA 3.2,
     * changes class name of nCipher HardToken HSMs after code re-structure.
     * 
     * @param admin
     *            Administrator probably Admin.TYPE_CACOMMANDLINE_USER
     * @param caid
     *            id of CA to upgrade
     */
    public void upgradeFromOldCAHSMKeyStore(Admin admin, int caid);

    /**
     * Method that is used to create a new CA from an imported keystore from
     * another type of CA, for example OpenSSL.
     * 
     * @param admin
     *            Administrator
     * @param caname
     *            the CA-name (human readable) the newly created CA will get
     * @param p12file
     *            a byte array of old server p12 file.
     * @param keystorepass
     *            used to unlock the keystore.
     * @param privkeypass
     *            used to unlock the private key.
     * @param privateSignatureKeyAlias
     *            the alias for the private key in the keystore.
     * @param privateEncryptionKeyAlias
     *            the alias for the private encryption key in the keystore
     */
    public void importCAFromKeyStore(Admin admin, String caname, byte[] p12file, String keystorepass, String privkeypass, String privateSignatureKeyAlias,
            String privateEncryptionKeyAlias) throws Exception;

    /**
     * Method that is used to create a new CA from keys and certificates.
     * 
     * @param admin
     * @param caname
     *            The name the new CA will have
     * @param keystorepass
     *            The keystore password the CA will have
     * @param signatureCertChain
     *            The CA certificate(s)
     * @param p12PublicSignatureKey
     *            CA public signature key
     * @param p12PrivateSignatureKey
     *            CA private signature key
     * @param p12PrivateEncryptionKey
     *            CA private encryption key, or null to generate a new
     *            encryption key
     * @param p12PublicEncryptionKey
     *            CA public encryption key, or null to generate a new encryption
     *            key
     * @throws Exception
     * @throws CATokenAuthenticationFailedException
     * @throws CATokenOfflineException
     * @throws IllegalKeyStoreException
     * @throws CreateException
     */
    public void importCAFromKeys(Admin admin, String caname, String keystorepass, java.security.cert.Certificate[] signatureCertChain,
            java.security.PublicKey p12PublicSignatureKey, java.security.PrivateKey p12PrivateSignatureKey, java.security.PrivateKey p12PrivateEncryptionKey,
            java.security.PublicKey p12PublicEncryptionKey) throws Exception, CATokenAuthenticationFailedException, CATokenOfflineException,
            IllegalKeyStoreException, CreateException;

    /**
     * Method that is used to create a new CA from keys on an HSM and
     * certificates in a file.
     * 
     * @param admin
     *            Administrator
     * @param caname
     *            the CA-name (human readable) the newly created CA will get
     * @param signatureCertChain
     *            chain of certificates, this CAs certificate first.
     * @param catokenpassword
     *            used to unlock the HSM keys.
     * @param catokenclasspath
     *            classpath to one of the HardToken classes, for example
     *            org.ejbca.core.model.ca.catoken.PKCS11CAToken.
     * @param catokenproperties
     *            the catoken properties, same as usually entered in the
     *            adminGUI for hard token CAs.
     */
    public void importCAFromHSM(Admin admin, String caname, java.security.cert.Certificate[] signatureCertChain, String catokenpassword,
            String catokenclasspath, String catokenproperties) throws Exception;

    /**
     * Exports a CA to file. The method only works for soft tokens.
     * 
     * @param admin
     *            Administrator
     * @param caname
     *            the CA-name (human readable) the CA
     * @param keystorepass
     *            used to lock the keystore.
     * @param privkeypass
     *            used to lock the private key.
     * @param privateSignatureKeyAlias
     *            the alias for the private signature key in the keystore.
     * @param privateEncryptionKeyAlias
     *            the alias for the private encryption key in teh keystore
     * @return A byte array of the CAs p12 in case of X509 CA and pkcs8 private
     *         certificate signing key in case of CVC CA.
     */
    public byte[] exportCAKeyStore(Admin admin, String caname, String keystorepass, String privkeypass, String privateSignatureKeyAlias,
            String privateEncryptionKeyAlias) throws Exception;

    /**
     * Method returning a Collection of Certificate of all CA certificates known
     * to the system. Certificates for External CAs or CAs that are awaiting
     * certificate response are not returned, because we don't have certificates
     * for them. Uses getAvailableCAs to list CAs.
     */
    public Collection getAllCACertificates();

    /**
     * Retrieve fingerprint for all keys as a String. Used for testing.
     * 
     * @param admin
     *            Administrator
     * @param caname
     *            the name of the CA whose fingerprint should be retrieved.
     * @throws Exception
     *             if the CA is not a soft token CA
     */
    public String getKeyFingerPrint(Admin admin, String caname) throws java.lang.Exception;

    /**
     * Activates an 'Offline' CA Token and sets the CA status to acitve and
     * ready for use again. The admin must be authorized to
     * "/ca_functionality/basic_functions/activate_ca" inorder to
     * activate/deactivate.
     * 
     * @param admin
     *            the adomistrator calling the method
     * @param caid
     *            the is of the ca to activate
     * @param the
     *            authorizationcode used to unlock the CA tokens private keys.
     * @param gc
     *            is the GlobalConfiguration used to extract approval
     *            information
     * @throws AuthorizationDeniedException
     *             it the administrator isn't authorized to activate the CA.
     * @throws CATokenAuthenticationFailedException
     *             if the current status of the ca or authenticationcode is
     *             wrong.
     * @throws CATokenOfflineException
     *             if the CA token is still off-line when calling the method.
     * @throws ApprovalException
     *             if an approval already is waiting for specified action
     * @throws WaitingForApprovalException
     *             if approval is required and the action have been added in the
     *             approval queue.
     */
    public void activateCAToken(Admin admin, int caid, String authorizationcode, GlobalConfiguration gc) throws AuthorizationDeniedException,
            CATokenAuthenticationFailedException, CATokenOfflineException, ApprovalException, WaitingForApprovalException;

    /**
     * Deactivates an 'active' CA token and sets the CA status to offline. The
     * admin must be authorized to
     * "/ca_functionality/basic_functions/activate_ca" inorder to
     * activate/deactivate.
     * 
     * @param admin
     *            the adomistrator calling the method
     * @param caid
     *            the is of the ca to activate.
     * @throws AuthorizationDeniedException
     *             it the administrator isn't authorized to activate the CA.
     * @throws EjbcaException
     *             if the given caid couldn't be found or its status is wrong.
     */
    public void deactivateCAToken(Admin admin, int caid) throws AuthorizationDeniedException, EjbcaException;

    /**
     * Method used to check if certificate profile id exists in any CA.
     */
    public boolean exitsCertificateProfileInCAs(Admin admin, int certificateprofileid);

    /**
     * Encrypts data with a CA key.
     * 
     * @param caid
     *            identifies the CA
     * @param data
     *            is the data to process
     * @return processed data
     * @throws Exception
     */
    public byte[] encryptWithCA(int caid, byte[] data) throws Exception;

    /**
     * Decrypts data with a CA key.
     * 
     * @param caid
     *            identifies the CA
     * @param data
     *            is the data to process
     * @return processed data
     * @throws Exception
     */
    public byte[] decryptWithCA(int caid, byte[] data) throws Exception;

    /**
     * Method used to check if publishers id exists in any CAs CRLPublishers
     * Collection.
     */
    public boolean exitsPublisherInCAs(Admin admin, int publisherid);

    /**
     * Help method that checks the CA data config and the certificate profile if
     * the specified action requires approvals and how many
     * 
     * @param is
     *            the administrator requesting this operation
     * @param action
     *            one of CAInfo.REQ_APPROVAL_ constants
     * @param caid
     *            of the ca to check
     * @param certprofile
     *            of the ca to check
     * @return 0 if no approvals is required otherwise the number of approvals
     */
    public int getNumOfApprovalRequired(Admin admin, int action, int caid, int certProfileId);

    /**
     * Method that publishes the given CA certificate chain to the list of
     * publishers. Is mainly used when CA is created.
     * 
     * @param admin
     *            Information about the administrator or admin preforming the
     *            event.
     * @param certificatechain
     *            certchain of certificate to publish
     * @param usedpublishers
     *            a collection if publisher id's (Integer) indicating which
     *            publisher that should be used.
     * @param caDataDN
     *            DN from CA data. If a the CA certificate does not have a DN
     *            object to be used by the publisher this DN could be searched
     *            for the object.
     */
    public void publishCACertificate(Admin admin, Collection certificatechain, Collection usedpublishers, String caDataDN);

    /**
     * Retrives a Collection of id:s (Integer) to authorized publishers.
     * 
     * @param admin
     * @return Collection of id:s (Integer)
     */
    public Collection getAuthorizedPublisherIds(Admin admin);

    /**
     * Method that checks if there are any CRLs needed to be updated and then
     * creates their CRLs. No overlap is used. This method can be called by a
     * scheduler or a service.
     * 
     * @param admin
     *            administrator performing the task
     * @return the number of crls created.
     * @throws EJBException
     *             om ett kommunikations eller systemfel intr?ffar.
     */
    public int createCRLs(Admin admin);

    /**
     * Method that checks if there are any delta CRLs needed to be updated and
     * then creates their delta CRLs. No overlap is used. This method can be
     * called by a scheduler or a service.
     * 
     * @param admin
     *            administrator performing the task
     * @return the number of delta crls created.
     * @throws EJBException
     *             if communication or system error happens
     */
    public int createDeltaCRLs(Admin admin);

    /**
     * Method that checks if there are any CRLs needed to be updated and then
     * creates their CRLs. A CRL is created: 1. if the current CRL expires
     * within the crloverlaptime (milliseconds) 2. if a crl issue interval is
     * defined (>0) a CRL is issued when this interval has passed, even if the
     * current CRL is still valid This method can be called by a scheduler or a
     * service.
     * 
     * @param admin
     *            administrator performing the task
     * @param caids
     *            list of CA ids (Integer) that will be checked, or null in
     *            which case ALL CAs will be checked
     * @param addtocrloverlaptime
     *            given in milliseconds and added to the CRL overlap time, if
     *            set to how often this method is run (poll time), it can be
     *            used to issue a new CRL if the current one expires within the
     *            CRL overlap time (configured in CA) and the poll time. The
     *            used CRL overlap time will be (crloverlaptime +
     *            addtocrloverlaptime)
     * @return the number of crls created.
     * @throws EJBException
     *             if communication or system error occurrs
     */
    public int createCRLs(Admin admin, Collection caids, long addtocrloverlaptime);

    /**
     * Method that checks if there are any delta CRLs needed to be updated and
     * then creates them. This method can be called by a scheduler or a service.
     * 
     * @param admin
     *            administrator performing the task
     * @param caids
     *            list of CA ids (Integer) that will be checked, or null in
     *            which case ALL CAs will be checked
     * @param crloverlaptime
     *            A new delta CRL is created if the current one expires within
     *            the crloverlaptime given in milliseconds
     * @return the number of delta crls created.
     * @throws EJBException
     *             if communication or system error occurrs
     */
    public int createDeltaCRLs(Admin admin, Collection caids, long crloverlaptime);

    /**
     * Method used to perform a extended CA Service, like OCSP CA Service.
     * 
     * @param admin
     *            Information about the administrator or admin preforming the
     *            event.
     * @param caid
     *            the ca that should perform the service
     * @param request
     *            a service request.
     * @return A corresponding response.
     * @throws IllegalExtendedCAServiceRequestException
     *             if the request was invalid.
     * @throws ExtendedCAServiceNotActiveException
     *             thrown when the service for the given CA isn't activated
     * @throws CADoesntExistsException
     *             The given caid doesn't exists.
     */
    public org.ejbca.core.model.ca.caadmin.extendedcaservices.ExtendedCAServiceResponse extendedService(org.ejbca.core.model.log.Admin admin, int caid,
            org.ejbca.core.model.ca.caadmin.extendedcaservices.ExtendedCAServiceRequest request)
            throws org.ejbca.core.model.ca.caadmin.extendedcaservices.ExtendedCAServiceRequestException,
            org.ejbca.core.model.ca.caadmin.extendedcaservices.IllegalExtendedCAServiceRequestException,
            org.ejbca.core.model.ca.caadmin.extendedcaservices.ExtendedCAServiceNotActiveException, org.ejbca.core.model.ca.caadmin.CADoesntExistsException;

    /**
     * Used by healthcheck. Validate that CAs are online and optionally performs
     * a signature test.
     * 
     * @return an error message or an empty String if all are ok.
     * 
     * TODO: This should only be in the local interface.
     */
    public String healthCheck();
}
