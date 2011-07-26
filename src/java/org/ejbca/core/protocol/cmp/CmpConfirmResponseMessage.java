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
package org.ejbca.core.protocol.cmp;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.CRL;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import org.apache.log4j.Logger;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.cms.CMSSignedGenerator;
import org.ejbca.core.model.ca.SignRequestException;
import org.ejbca.core.model.ra.NotFoundException;
import org.ejbca.core.protocol.FailInfo;
import org.ejbca.core.protocol.IRequestMessage;
import org.ejbca.core.protocol.IResponseMessage;
import org.ejbca.core.protocol.ResponseStatus;

import com.novosec.pkix.asn1.cmp.PKIBody;
import com.novosec.pkix.asn1.cmp.PKIHeader;
import com.novosec.pkix.asn1.cmp.PKIMessage;


/**
 * A very simple confirmation message, no protection and a nullbody
 * @author tomas
 * @version $Id$
 */
public class CmpConfirmResponseMessage extends BaseCmpMessage implements IResponseMessage {

	/**
	 * Determines if a de-serialized file is compatible with this class.
	 *
	 * Maintainers must change this value if and only if the new version
	 * of this class is not compatible with old versions. See Sun docs
	 * for <a href=http://java.sun.com/products/jdk/1.1/docs/guide
	 * /serialization/spec/version.doc.html> details. </a>
	 *
	 */
	static final long serialVersionUID = 10003L;

	private static final Logger log = Logger.getLogger(CmpConfirmResponseMessage.class);
	
    /** Default digest algorithm for CMP response message, can be overridden */
	private String digestAlg = CMSSignedGenerator.DIGEST_SHA1;
	/** The default provider is BC, if nothing else is specified when setting SignKeyInfo */
	private String provider = "BC";
	/** Certificate for the signer of the response message (CA) */
	private transient Certificate signCert = null;
	/** Private key used to sign the response message */
	private transient PrivateKey signKey = null;

	/** The encoded response message */
    private byte[] responseMessage = null;

    public void setCertificate(Certificate cert) {
	}

	public void setCrl(CRL crl) {
	}

	public void setIncludeCACert(boolean incCACert) {
	}
	public void setCACert(Certificate cACert) {
	}

	public byte[] getResponseMessage() throws IOException,
			CertificateEncodingException {
        return responseMessage;
	}

	public void setStatus(ResponseStatus status) {
	}

	public ResponseStatus getStatus() {
		return ResponseStatus.SUCCESS;
	}

	public void setFailInfo(FailInfo failInfo) {
	}

	public FailInfo getFailInfo() {
		return null;
	}

	public void setFailText(String failText) {
	}

	public String getFailText() {
		return null;
	}

	public boolean create() throws IOException, InvalidKeyException,
			NoSuchAlgorithmException, NoSuchProviderException,
			SignRequestException, NotFoundException {

		X509Name sender = X509Name.getInstance(getSender().getName());
		X509Name recipient = X509Name.getInstance(getRecipient().getName());
		PKIHeader myPKIHeader = CmpMessageHelper.createPKIHeader(sender, recipient, getSenderNonce(), getRecipientNonce(), getTransactionId());
		PKIBody myPKIBody = new PKIBody(new DERNull(), 19);
		PKIMessage myPKIMessage = new PKIMessage(myPKIHeader, myPKIBody);

		if ((getPbeDigestAlg() != null) && (getPbeMacAlg() != null) && (getPbeKeyId() != null) && (getPbeKey() != null) ) {
			responseMessage = CmpMessageHelper.protectPKIMessageWithPBE(myPKIMessage, getPbeKeyId(), getPbeKey(), getPbeDigestAlg(), getPbeMacAlg(), getPbeIterationCount());
		} else {
			//responseMessage = CmpMessageHelper.pkiMessageToByteArray(myPKIMessage);
			if ((signCert != null) && (signKey != null)) {
				try {
					responseMessage = CmpMessageHelper.signPKIMessage(myPKIMessage, (X509Certificate)signCert, signKey, digestAlg, provider);
				} catch (CertificateEncodingException e) {
					log.error("Error creating CmpConfirmMessage: ", e);
				} catch (SecurityException e) {
					log.error("Error creating CmpConfirmMessage: ", e);
				} catch (SignatureException e) {
					log.error("Error creating CmpConfirmMessage: ", e);
				}				
			} else {
				if (log.isDebugEnabled()) {
					log.debug("Not signing CMP Confirm Response, because signCert or signKey is not set.");
				}
			}
			// If we could not create the signed response message, create a non-protected one instead.
			if (responseMessage == null) {
				responseMessage = CmpMessageHelper.pkiMessageToByteArray(myPKIMessage);
			}
		}
		return true;
	}

	public boolean requireSignKeyInfo() {
		return false;
	}
/*
	public void setSignKeyInfo(Certificate cert, PrivateKey key,
			String provider) {
	}
*/
	public void setSignKeyInfo(Certificate cert, PrivateKey key, String provider) {
		this.signCert = cert;
		this.signKey = key;
		if (provider != null) {
			this.provider = provider;
		}
	}

	public void setSenderNonce(String senderNonce) {
		super.setSenderNonce(senderNonce);
	}

	public void setRecipientNonce(String recipientNonce) {
		super.setRecipientNonce(recipientNonce);
	}

	public void setTransactionId(String transactionId) {
		super.setTransactionId(transactionId);
	}

	public void setRecipientKeyInfo(byte[] recipientKeyInfo) {
	}

	public void setPreferredDigestAlg(String digest) {
	}

	public void setRequestType(int reqtype) {
	}

	public void setRequestId(int reqid) {
	}

    /** @see org.ejca.core.protocol.IResponseMessage
     */
    public void setProtectionParamsFromRequest(IRequestMessage reqMsg) {
    }
}
