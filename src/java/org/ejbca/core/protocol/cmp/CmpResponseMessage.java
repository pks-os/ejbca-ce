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

import java.io.ByteArrayInputStream;
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
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.cms.CMSSignedGenerator;
import org.ejbca.core.protocol.FailInfo;
import org.ejbca.core.protocol.IResponseMessage;
import org.ejbca.core.protocol.ResponseStatus;

import com.novosec.pkix.asn1.cmp.CertOrEncCert;
import com.novosec.pkix.asn1.cmp.CertRepMessage;
import com.novosec.pkix.asn1.cmp.CertResponse;
import com.novosec.pkix.asn1.cmp.CertifiedKeyPair;
import com.novosec.pkix.asn1.cmp.ErrorMsgContent;
import com.novosec.pkix.asn1.cmp.PKIBody;
import com.novosec.pkix.asn1.cmp.PKIFreeText;
import com.novosec.pkix.asn1.cmp.PKIHeader;
import com.novosec.pkix.asn1.cmp.PKIMessage;
import com.novosec.pkix.asn1.cmp.PKIStatusInfo;

/**
 * CMP certificate response message
 * @author tomas
 * @version $Id: CmpResponseMessage.java,v 1.4 2006-10-20 15:18:46 anatom Exp $
 */
public class CmpResponseMessage implements IResponseMessage {
	
	/**
	 * Determines if a de-serialized file is compatible with this class.
	 *
	 * Maintainers must change this value if and only if the new version
	 * of this class is not compatible with old versions. See Sun docs
	 * for <a href=http://java.sun.com/products/jdk/1.1/docs/guide
	 * /serialization/spec/version.doc.html> details. </a>
	 *
	 */
	static final long serialVersionUID = 10001L;
	
	private static Logger log = Logger.getLogger(CmpResponseMessage.class);
	
    /** The encoded response message */
    private byte[] responseMessage = null;

    /** status for the response */
	private ResponseStatus status = ResponseStatus.SUCCESS;
	
	/** Possible fail information in the response. Defaults to 'badRequest (2)'. */
	private FailInfo failInfo = FailInfo.BAD_REQUEST;
	
    /** Possible clear text error information in the response. Defaults to null. */
    private String failText = null;

    /**
	 * SenderNonce. This is base64 encoded bytes
	 */
	private String senderNonce = null;
	/**
	 * RecipientNonce in a response is the senderNonce from the request. This is base64 encoded bytes
	 */
	private String recipientNonce = null;
	
	/** transaction id */
	private String transactionId = null;
	
	/** Certificate to be in certificate response message, not serialized */
	private transient Certificate cert = null;
	/** Default digest algorithm for SCEP response message, can be overridden */
	private transient String digestAlg = CMSSignedGenerator.DIGEST_SHA1;
	/** Certificate for the signer of the response message (CA) */
	private transient X509Certificate signCert = null;
	/** Private key used to sign the response message */
	private transient PrivateKey signKey = null;
	/** The default provider is BC, if nothing else is specified when setting SignKeyInfo */
	private transient String provider = "BC";
	/** used to choose response body type */
	private transient int requestType;
	/** used to match request with response */
	private transient int requestId;
	
	public void setCertificate(Certificate cert) {
		this.cert = cert;
	}
	
	public void setCrl(CRL crl) {
		// TODO Auto-generated method stub
		
	}
	
	public void setIncludeCACert(boolean incCACert) {
		// TODO Auto-generated method stub
		
	}
	
	public byte[] getResponseMessage() throws IOException, CertificateEncodingException {
        return responseMessage;
	}
	
	public void setStatus(ResponseStatus status) {
        this.status = status;
	}
	
	public ResponseStatus getStatus() {
        return status;
	}
	
	public void setFailInfo(FailInfo failInfo) {
        this.failInfo = failInfo;
	}
	
	public FailInfo getFailInfo() {
        return failInfo;
	}
	
    public void setFailText(String failText) {
    	this.failText = failText;
    }

    public String getFailText() {
    	return this.failText;
    }

    public boolean create() throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException {
		boolean ret = false;
		if (status.equals(ResponseStatus.SUCCESS)) {
			log.debug("Creating a 'accepted' message.");
		} else {
			if (status.equals(ResponseStatus.FAILURE)) {
				log.debug("Creating a 'rejection' message.");
			} else {
				log.debug("Creating a 'waiting' message?");
			}               
		}
		// Some general stuff, common for all types of messages
		String issuer = null;
		String subject = null;
		if (cert != null) {
			X509Certificate x509cert = (X509Certificate)cert;
			issuer = x509cert.getIssuerDN().getName();
			subject = x509cert.getSubjectDN().getName();
		} else if (signCert != null) {
			issuer = signCert.getSubjectDN().getName();
			subject = "CN=fooSubject";
		} else {
			issuer = "CN=fooIssuer";
			subject = "CN=fooSubject";
		}
		
		X509Name issuerName = new X509Name(issuer);
		X509Name subjectName = new X509Name(subject);
		PKIHeader myPKIHeader = CmpMessageHelper.createPKIHeader(issuerName, subjectName, senderNonce, recipientNonce, transactionId);

		try {
			if (status.equals(ResponseStatus.SUCCESS)) {
				if (cert != null) {
					log.debug("Creating a CertRepMessage");
					PKIStatusInfo myPKIStatusInfo = new PKIStatusInfo(new DERInteger(0)); // 0 = accepted
					CertResponse myCertResponse = new CertResponse(new DERInteger(requestId), myPKIStatusInfo);
					
					X509CertificateStructure struct = X509CertificateStructure.getInstance(new ASN1InputStream(new ByteArrayInputStream(cert.getEncoded())).readObject());
					CertOrEncCert retCert = new CertOrEncCert(struct, 0);
					CertifiedKeyPair myCertifiedKeyPair = new CertifiedKeyPair(retCert);
					myCertResponse.setCertifiedKeyPair(myCertifiedKeyPair);
					//myCertResponse.setRspInfo(new DEROctetString(new byte[] { 101, 111, 121 }));
					
					CertRepMessage myCertRepMessage = new CertRepMessage(myCertResponse);
					
					int respType = requestType + 1; // 1 = intitialization response, 3 = certification response etc
					log.debug("Creating response body of type respType.");
					PKIBody myPKIBody = new PKIBody(myCertRepMessage, respType); 
					PKIMessage myPKIMessage = new PKIMessage(myPKIHeader, myPKIBody);
					
					responseMessage = CmpMessageHelper.signPKIMessage(myPKIMessage, signCert, signKey, digestAlg, provider);
					ret = true;	
				}
			} else {
				// Create a failure message
				PKIStatusInfo myPKIStatusInfo = new PKIStatusInfo(new DERInteger(2)); // 2 = rejection
				myPKIStatusInfo.setFailInfo(failInfo.getAsBitString());
				myPKIStatusInfo.setStatusString(new PKIFreeText(new DERUTF8String(failText)));
				
				ErrorMsgContent myErrorContent = new ErrorMsgContent(myPKIStatusInfo);
				PKIBody myPKIBody = new PKIBody(myErrorContent, 23); // 23 = error
				PKIMessage myPKIMessage = new PKIMessage(myPKIHeader, myPKIBody);
				
				responseMessage = CmpMessageHelper.signPKIMessage(myPKIMessage, signCert, signKey, digestAlg, provider);
				ret = true;
			}
		} catch (CertificateEncodingException e) {
			log.error("Error creating CertRepMessage: ", e);
		} catch (InvalidKeyException e) {
			log.error("Error creating CertRepMessage: ", e);
		} catch (NoSuchProviderException e) {
			log.error("Error creating CertRepMessage: ", e);
		} catch (NoSuchAlgorithmException e) {
			log.error("Error creating CertRepMessage: ", e);
		} catch (SecurityException e) {
			log.error("Error creating CertRepMessage: ", e);
		} catch (SignatureException e) {
			log.error("Error creating CertRepMessage: ", e);
		}
		
		return ret;
	}
	
	public boolean requireSignKeyInfo() {
		return true;
	}
	
	public boolean requireEncKeyInfo() {
		return false;
	}
	
	public void setSignKeyInfo(X509Certificate cert, PrivateKey key, String provider) {
		this.signCert = cert;
		this.signKey = key;
		if (provider != null) {
			this.provider = provider;
		}
	}
	
	public void setEncKeyInfo(X509Certificate cert, PrivateKey key,
			String provider) {
	}
	
	public void setSenderNonce(String senderNonce) {
		this.senderNonce = senderNonce;
	}
	
	public void setRecipientNonce(String recipientNonce) {
		this.recipientNonce = recipientNonce;
	}
	
	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}
	
	public void setRecipientKeyInfo(byte[] recipientKeyInfo) {
	}
	
	public void setPreferredDigestAlg(String digest) {
		this.digestAlg = digest;
	}

    /** @see org.ejca.core.protocol.IResponseMessage
     */
	public void setRequestType(int reqtype) {
		this.requestType = reqtype;
	}

    /** @see org.ejca.core.protocol.IResponseMessage
     */
    public void setRequestId(int reqid) {
    	this.requestId = reqid;
    }

}
