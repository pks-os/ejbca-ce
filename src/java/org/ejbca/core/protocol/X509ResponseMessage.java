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

package org.ejbca.core.protocol;


import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.cert.CRL;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.ejbca.util.CertTools;


/**
 * A response message consisting of a single X509 Certificate.
 *
 * @version $Id: X509ResponseMessage.java,v 1.2 2006-02-28 08:25:28 anatom Exp $
 */
public class X509ResponseMessage implements IResponseMessage {
    static final long serialVersionUID = -2157072605987735912L;

    /** Certificate to be in response message, */
    private Certificate cert = null;

    /** status for the response */
    private ResponseStatus status = ResponseStatus.SUCCESS;

    /** Possible fail information in the response. Defaults to null. */
    private FailInfo failInfo = null;

    /**
     * Sets the complete certificate in the response message.
     *
     * @param cert certificate in the response message.
     */
    public void setCertificate(Certificate cert) {
        this.cert = cert;
    }

    /**
     * Sets the CRL (if present) in the response message.
     *
     * @param crl crl in the response message.
     */
    public void setCrl(CRL crl) {
        // This message type does not contain a CRL
    }

    /** @see org.ejbca.core.protocol.IResponseMessage#setIncludeCACert
     * 
     */
    public void setIncludeCACert(boolean incCACert) {
    	// Do nothing, not applicable
    }

    /**
     * Gets the complete certificate in the response message.
     *
     * @return certificate in the response message.
     */
    public Certificate getCertificate() throws CertificateEncodingException, CertificateException, IOException {
        return CertTools.getCertfromByteArray(getResponseMessage());
    }

    /**
     * Gets the response message in the default encoding format.
     *
     * @return the response message in the default encoding format.
     */
    public byte[] getResponseMessage() throws IOException, CertificateEncodingException {
        return cert.getEncoded();
    }

    /**
     * Sets the status of the response message.
     *
     * @param status status of the response.
     */
    public void setStatus(ResponseStatus status) {
        this.status = status;
    }

    /**
     * Gets the status of the response message.
     *
     * @return status status of the response.
     */
    public ResponseStatus getStatus() {
        return status;
    }

    /**
     * Sets info about reason for failure.
     *
     * @param failInfo reason for failure.
     */
    public void setFailInfo(FailInfo failInfo) {
        this.failInfo = failInfo;
    }

    /**
     * Gets info about reason for failure.
     *
     * @return failInfo reason for failure.
     */
    public FailInfo getFailInfo() {
        return failInfo;
    }

    /**
     * Create encrypts and creates signatures as needed to produce a complete response message.  If
     * needed setSignKeyInfo and setEncKeyInfo must be called before this method. After this is
     * called the response message can be retrieved with getResponseMessage();
     *
     * @return True if signature/encryption was successful, false if it failed, request should not
     *         be sent back i failed.
     *
     * @throws IOException If input/output or encoding failed.
     * @throws InvalidKeyException If the key used for signing/encryption is invalid.
     * @throws NoSuchProviderException if there is an error with the Provider.
     * @throws NoSuchAlgorithmException if the signature on the request is done with an unhandled
     *         algorithm.
     *
     * @see #setSignKeyInfo()
     * @see #setEncKeyInfo()
     */
    public boolean create()
            throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException {
        // Nothing needs to be done here
        return true;
    }

    /**
     * indicates if this message needs recipients public and private key to sign. If this returns
     * true, setSignKeyInfo() should be called.
     *
     * @return True if public and private key is needed.
     */
    public boolean requireSignKeyInfo() {
        return false;
    }

    /**
     * indicates if this message needs recipients public and private key to encrypt. If this
     * returns true, setEncKeyInfo() should be called.
     *
     * @return True if public and private key is needed.
     */
    public boolean requireEncKeyInfo() {
        return false;
    }

    /**
     * Sets the public and private key needed to sign the message. Must be set if
     * requireSignKeyInfo() returns true.
     *
     * @param cert certificate containing the public key.
     * @param key private key.
     * @param provider the provider to use, if the private key is on a HSM you must use a special provider. If null is given, the default BC provider is used.
     *
     * @see #requireSignKeyInfo()
     */
    public void setSignKeyInfo(X509Certificate cert, PrivateKey key, String provider) {
    }

    /**
     * Sets the public and private key needed to encrypt the message. Must be set if
     * requireEncKeyInfo() returns true.
     *
     * @param cert certificate containing the public key.
     * @param key private key.
     * @param provider the provider to use, if the private key is on a HSM you must use a special provider. If null is given, the default BC provider is used.
     *
     * @see #requireEncKeyInfo()
     */
    public void setEncKeyInfo(X509Certificate cert, PrivateKey key, String provider) {
    }

    /**
     * Sets a senderNonce if it should be present in the response
     *
     * @param senderNonce a string of base64 encoded bytes
     */
    public void setSenderNonce(String senderNonce) {
    }

    /**
     * Sets a recipient if it should be present in the response
     *
     * @param recipientNonce a string of base64 encoded bytes
     */
    public void setRecipientNonce(String recipientNonce) {
    }

    /**
     * Sets a transaction identifier if it should be present in the response
     *
     * @param transactionId transaction id
     */
    public void setTransactionId(String transactionId) {
    }

    /**
     * Sets recipient key info, key id or similar. This is usually the request key info from the
     * request message.
     *
     * @param recipientKeyInfo key info
     */
    public void setRecipientKeyInfo(byte[] recipientKeyInfo) {
    }
    
    /** @see org.ejca.core.protocol.IResponseMessage
     */
    public void setPreferredDigestAlg(String digest) {
    }
}
