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
package org.ejbca.core.ejb.ca.sign;

import java.io.UnsupportedEncodingException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.Date;

import javax.ejb.ObjectNotFoundException;

import org.ejbca.core.EjbcaException;
import org.ejbca.core.model.ca.AuthLoginException;
import org.ejbca.core.model.ca.AuthStatusException;
import org.ejbca.core.model.ca.IllegalKeyException;
import org.ejbca.core.model.ca.SignRequestException;
import org.ejbca.core.model.ca.SignRequestSignatureException;
import org.ejbca.core.model.ca.caadmin.CADoesntExistsException;
import org.ejbca.core.model.log.Admin;

public interface SignSession {

    public boolean isUniqueCertificateSerialNumberIndex();

    /**
     * Retrieves the certificate chain for the signer. The returned certificate
     * chain MUST have the RootCA certificate in the last position.
     * 
     * @param admin
     *            Information about the administrator or admin performing the
     *            event.
     * @param caid
     *            is the issuerdn.hashCode()
     * @return Collection of Certificate, the certificate chain, never null.
     */
    public java.util.Collection<Certificate> getCertificateChain(org.ejbca.core.model.log.Admin admin, int caid);

    /**
     * Creates a signed PKCS7 message containing the whole certificate chain,
     * including the provided client certificate.
     * 
     * @param admin
     *            Information about the administrator or admin performing the
     *            event.
     * @param cert
     *            client certificate which we want encapsulated in a PKCS7
     *            together with certificate chain.
     * @return The DER-encoded PKCS7 message.
     * @throws CADoesntExistsException
     *             if the CA does not exist or is expired, or has an invalid
     *             cert
     * @throws SignRequestSignatureException
     *             if the certificate is not signed by the CA
     */
    public byte[] createPKCS7(org.ejbca.core.model.log.Admin admin, java.security.cert.Certificate cert, boolean includeChain)
            throws CADoesntExistsException, SignRequestSignatureException;

    /**
     * Creates a signed PKCS7 message containing the whole certificate chain of
     * the specified CA.
     * 
     * @param admin
     *            Information about the administrator or admin performing the
     *            event.
     * @param caId
     *            CA for which we want a PKCS7 certificate chain.
     * @return The DER-encoded PKCS7 message.
     * @throws CADoesntExistsException
     *             if the CA does not exist or is expired, or has an invalid
     *             cert
     */
    public byte[] createPKCS7(org.ejbca.core.model.log.Admin admin, int caId, boolean includeChain)
            throws CADoesntExistsException;

    /**
     * Requests for a certificate to be created for the passed public key with
     * default key usage The method queries the user database for authorization
     * of the user.
     * 
     * @param admin
     *            Information about the administrator or admin performing the
     *            event.
     * @param username
     *            unique username within the instance.
     * @param password
     *            password for the user.
     * @param pk
     *            the public key to be put in the created certificate.
     * @return The newly created certificate or null.
     * @throws EjbcaException
     *             if EJBCA did not accept any of all input parameters
     * @throws ObjectNotFoundException
     *             if the user does not exist.
     * @throws AuthStatusException
     *             If the users status is incorrect.
     * @throws AuthLoginException
     *             If the password is incorrect.
     * @throws IllegalKeyException
     *             if the public key is of wrong type.
     */
    public java.security.cert.Certificate createCertificate(org.ejbca.core.model.log.Admin admin, java.lang.String username, java.lang.String password,
            java.security.PublicKey pk) throws EjbcaException, ObjectNotFoundException;

    /**
     * Requests for a certificate to be created for the passed public key with
     * the passed key usage. The method queries the user database for
     * authorization of the user. CAs are only allowed to have certificateSign
     * and CRLSign set.
     * 
     * @param admin
     *            Information about the administrator or admin performing the
     *            event.
     * @param username
     *            unique username within the instance.
     * @param password
     *            password for the user.
     * @param pk
     *            the public key to be put in the created certificate.
     * @param keyusage
     *            integer with bit mask describing desired keys usage, overrides
     *            keyUsage from CertificateProfiles if allowed. Bit mask is
     *            packed in in integer using constants from CertificateData. -1
     *            means use default keyUsage from CertificateProfile. ex. int
     *            keyusage = CertificateData.digitalSignature |
     *            CertificateData.nonRepudiation; gives digitalSignature and
     *            nonRepudiation. ex. int keyusage = CertificateData.keyCertSign
     *            | CertificateData.cRLSign; gives keyCertSign and cRLSign
     * @param notBefore
     *            an optional validity to set in the created certificate, if the
     *            profile allows validity override, null if the profiles default
     *            validity should be used.
     * @param notAfter
     *            an optional validity to set in the created certificate, if the
     *            profile allows validity override, null if the profiles default
     *            validity should be used.
     * @return The newly created certificate or null.
     * @throws EjbcaException
     *             if EJBCA did not accept any of all input parameters
     * @throws ObjectNotFoundException
     *             if the user does not exist.
     * @throws AuthStatusException
     *             If the users status is incorrect.
     * @throws AuthLoginException
     *             If the password is incorrect.
     * @throws IllegalKeyException
     *             if the public key is of wrong type.
     */
    public java.security.cert.Certificate createCertificate(org.ejbca.core.model.log.Admin admin, java.lang.String username, java.lang.String password,
            java.security.PublicKey pk, int keyusage, java.util.Date notBefore, java.util.Date notAfter) throws EjbcaException, ObjectNotFoundException;

    /**
     * Requests for a certificate to be created for the passed public key
     * wrapped in a self-signed certificate. Verification of the signature
     * (proof-of-possession) on the request is performed, and an exception thrown
     * if verification fails. The method queries the user database for
     * authorization of the user.
     * 
     * @param admin
     *            Information about the administrator or admin performing the
     *            event.
     * @param username
     *            unique username within the instance.
     * @param password
     *            password for the user.
     * @param incert
     *            a certificate containing the public key to be put in the
     *            created certificate. Other (requested) parameters in the
     *            passed certificate can be used, such as DN, Validity, KeyUsage
     *            etc. Currently only KeyUsage is considered!
     * @return The newly created certificate or null.
     * @throws EjbcaException
     *             if EJBCA did not accept any of all input parameters
     * @throws ObjectNotFoundException
     *             if the user does not exist.
     * @throws AuthStatusException
     *             If the users status is incorrect.
     * @throws AuthLoginException
     *             If the password is incorrect.
     * @throws IllegalKeyException
     *             if the public key is of wrong type.
     * @throws SignRequestSignatureException
     *             if the provided client certificate was not signed by the CA.
     */
    public java.security.cert.Certificate createCertificate(org.ejbca.core.model.log.Admin admin, java.lang.String username, java.lang.String password,
            java.security.cert.Certificate incert) throws EjbcaException, ObjectNotFoundException;

    /**
     * Requests for a certificate to be created for the passed public key
     * wrapped in a certification request message (ex PKCS10). Verification of
     * the signature (proof-of-possession) on the request is performed, and an
     * exception thrown if verification fails. The method queries the user
     * database for authorization of the user.
     * 
     * @param admin
     *            Information about the administrator or admin performing the
     *            event.
     * @param req
     *            a Certification Request message, containing the public key to
     *            be put in the created certificate. Currently no additional
     *            parameters in requests are considered! Currently no additional
     *            parameters in the PKCS10 request is considered!
     * @param responseClass
     *            The implementation class that will be used as the response
     *            message.
     * @return The newly created response message or null.
     * @throws ObjectNotFoundException
     *             if the user does not exist.
     * @throws AuthStatusException
     *             If the users status is incorrect.
     * @throws AuthLoginException
     *             If the password is incorrect.
     * @throws IllegalKeyException
     *             if the public key is of wrong type.
     * @throws SignRequestException
     *             if the provided request is invalid.
     * @throws SignRequestSignatureException
     *             if the provided client certificate was not signed by the CA.
     * @see org.ejbca.core.protocol.IRequestMessage
     * @see org.ejbca.core.protocol.IResponseMessage
     * @see org.ejbca.core.protocol.X509ResponseMessage
     */
    public org.ejbca.core.protocol.IResponseMessage createCertificate(org.ejbca.core.model.log.Admin admin, org.ejbca.core.protocol.IRequestMessage req,
            java.lang.Class responseClass) throws EjbcaException;

	/**
	 * Requests for a certificate to be created for the passed public key with the passed key
	 * usage and using the given certificate profile. This method is primarily intended to be used when
	 * issuing hardtokens having multiple certificates per user.
	 * The method queries the user database for authorization of the user. CAs are only
	 * allowed to have certificateSign and CRLSign set.
	 *
	 * @param admin                Information about the administrator or admin performing the event.
	 * @param username             unique username within the instance.
	 * @param password             password for the user.
	 * @param pk                   the public key to be put in the created certificate.
	 * @param keyusage             integer with bit mask describing desired keys usage, overrides keyUsage from
	 *                             CertificateProfiles if allowed. Bit mask is packed in in integer using constants
	 *                             from CertificateData. -1 means use default keyUsage from CertificateProfile. ex. int
	 *                             keyusage = CertificateData.digitalSignature | CertificateData.nonRepudiation; gives
	 *                             digitalSignature and nonRepudiation. ex. int keyusage = CertificateData.keyCertSign
	 *                             | CertificateData.cRLSign; gives keyCertSign and cRLSign
	 * @param notBefore an optional validity to set in the created certificate, if the profile allows validity override, null if the profiles default validity should be used.
	 * @param notAfter an optional validity to set in the created certificate, if the profile allows validity override, null if the profiles default validity should be used.
	 * @param certificateprofileid used to override the one set in userdata.
	 *                             Should be set to SecConst.PROFILE_NO_PROFILE if the usedata certificateprofileid should be used
	 * @param caid                 used to override the one set in userdata.
	 *                             Should be set to SecConst.CAID_USEUSERDEFINED if the regular certificateprofileid should be used
	 * 
	 * 
	 * @return The newly created certificate or null.
	 * @throws EjbcaException          if EJBCA did not accept any of all input parameters
	 * @throws ObjectNotFoundException if the user does not exist.
	 * @throws AuthStatusException     If the users status is incorrect.
	 * @throws AuthLoginException      If the password is incorrect.
	 * @throws IllegalKeyException     if the public key is of wrong type.
	 * 
	 */
    public Certificate createCertificate(Admin admin, String username, String password, PublicKey pk, int keyusage, Date notBefore, Date notAfter, int certificateprofileid, int caid) 
    	throws EjbcaException, ObjectNotFoundException;

    /**
     * Method that generates a request failed response message. The request
     * should already have been decrypted and verified.
     * 
     * @param admin
     *            Information about the administrator or admin performing the
     *            event.
     * @param req
     *            a Certification Request message, containing the public key to
     *            be put in the created certificate. Currently no additional
     *            parameters in requests are considered!
     * @param responseClass
     *            The implementation class that will be used as the response
     *            message.
     * @return A decrypted and verified IReqeust message
     * @throws AuthStatusException
     *             If the users status is incorrect.
     * @throws AuthLoginException
     *             If the password is incorrect.
     * @throws CADoesntExistsException
     *             if the targeted CA does not exist
     * @throws SignRequestException
     *             if the provided request is invalid.
     * @throws SignRequestSignatureException
     *             if the the request couldn't be verified.
     * @throws IllegalKeyException
     * @see org.ejbca.core.protocol.IRequestMessage
     * @see org.ejbca.core.protocol.IResponseMessage
     * @see org.ejbca.core.protocol.X509ResponseMessage
     */
    public org.ejbca.core.protocol.IResponseMessage createRequestFailedResponse(org.ejbca.core.model.log.Admin admin,
            org.ejbca.core.protocol.IRequestMessage req, java.lang.Class responseClass) throws org.ejbca.core.model.ca.AuthLoginException,
            AuthStatusException, IllegalKeyException, CADoesntExistsException,
            SignRequestSignatureException, SignRequestException;

    /**
     * Method that just decrypts and verifies a request and should be used in
     * those cases a when encrypted information needs to be extracted and
     * presented to an RA for approval.
     * 
     * @param admin
     *            Information about the administrator or admin performing the
     *            event.
     * @param req
     *            a Certification Request message, containing the public key to
     *            be put in the created certificate. Currently no additional
     *            parameters in requests are considered!
     * @return A decrypted and verified IReqeust message
     * @throws AuthStatusException
     *             If the users status is incorrect.
     * @throws AuthLoginException
     *             If the password is incorrect.
     * @throws IllegalKeyException
     *             if the public key is of wrong type.
     * @throws CADoesntExistsException
     *             if the targeted CA does not exist
     * @throws SignRequestException
     *             if the provided request is invalid.
     * @throws SignRequestSignatureException
     *             if the the request couldn't be verified.
     * @see org.ejbca.core.protocol.IRequestMessage
     * @see org.ejbca.core.protocol.IResponseMessage
     * @see org.ejbca.core.protocol.X509ResponseMessage
     */
    public org.ejbca.core.protocol.IRequestMessage decryptAndVerifyRequest(org.ejbca.core.model.log.Admin admin, org.ejbca.core.protocol.IRequestMessage req)
            throws ObjectNotFoundException, AuthStatusException, AuthLoginException,
            IllegalKeyException, CADoesntExistsException, SignRequestException,
            SignRequestSignatureException;

    /**
     * Implements ISignSession::getCRL
     * 
     * @param admin
     *            Information about the administrator or admin performing the
     *            event.
     * @param req
     *            a CRL Request message
     * @param responseClass
     *            the implementation class of the desired response
     * @return The CRL packaged in a response message or null.
     * @throws IllegalKeyException
     *             if the public key is of wrong type.
     * @throws CADoesntExistsException
     *             if the targeted CA does not exist
     * @throws SignRequestException
     *             if the provided request is invalid.
     * @throws SignRequestSignatureException
     *             if the provided client certificate was not signed by the CA.
     */
    public org.ejbca.core.protocol.IResponseMessage getCRL(org.ejbca.core.model.log.Admin admin, org.ejbca.core.protocol.IRequestMessage req,
            java.lang.Class responseClass) throws AuthStatusException, AuthLoginException,
            IllegalKeyException, CADoesntExistsException, SignRequestException,
            SignRequestSignatureException, UnsupportedEncodingException;

}
