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

import java.io.ByteArrayOutputStream;
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
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.cmp.CMPObjectIdentifiers;
import org.bouncycastle.asn1.cmp.PKIBody;
import org.bouncycastle.asn1.cmp.PKIFailureInfo;
import org.bouncycastle.asn1.cmp.PKIFreeText;
import org.bouncycastle.asn1.cmp.PKIHeaderBuilder;
import org.bouncycastle.asn1.cmp.PKIMessage;
import org.bouncycastle.asn1.cmp.PKIStatus;
import org.bouncycastle.asn1.cmp.PKIStatusInfo;
import org.bouncycastle.asn1.cmp.RevRepContent;
import org.bouncycastle.asn1.cmp.RevRepContentBuilder;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cms.CMSSignedGenerator;
import org.cesecore.certificates.ca.SignRequestException;
import org.cesecore.certificates.certificate.request.FailInfo;
import org.cesecore.certificates.certificate.request.RequestMessage;
import org.cesecore.certificates.certificate.request.ResponseMessage;
import org.cesecore.certificates.certificate.request.ResponseStatus;

/**
 * A very simple confirmation message, no protection and a nullbody
 * @author tomas
 * @version $Id$
 */
public class CmpRevokeResponseMessage extends BaseCmpMessage implements ResponseMessage {

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

	private static final Logger log = Logger.getLogger(CmpRevokeResponseMessage .class);

    /** Default digest algorithm for SCEP response message, can be overridden */
    private String digestAlg = CMSSignedGenerator.DIGEST_SHA1;
    /** The default provider is BC, if nothing else is specified when setting SignKeyInfo */
    private String provider = "BC";
	
    /** Certificate for the signer of the response message (CA) */
    private transient Certificate signCert = null;
    /** Private key used to sign the response message */
    private transient PrivateKey signKey = null;

    
	/** The encoded response message */
    private byte[] responseMessage = null;
    private String failText = null;
    private FailInfo failInfo = FailInfo.BAD_REQUEST;
    private ResponseStatus status = ResponseStatus.FAILURE;

	@Override
	public void setCrl(CRL crl) {
	}

	@Override
	public void setIncludeCACert(boolean incCACert) {
	}

	@Override
	public void setCACert(Certificate cACert) {
	}

	@Override
	public byte[] getResponseMessage() throws IOException,
			CertificateEncodingException {
        return responseMessage;
	}

	@Override
	public void setStatus(ResponseStatus status) {
		this.status = status;
	}

	@Override
	public ResponseStatus getStatus() {
		return status;
	}

	@Override
	public void setFailInfo(FailInfo failInfo) {
		this.failInfo = failInfo;
	}

	@Override
	public FailInfo getFailInfo() {
		return failInfo;
	}

	@Override
	public void setFailText(String failText) {
		this.failText = failText;
	}

	@Override
	public String getFailText() {
		return failText;
	}

	@Override
	public boolean create() throws IOException, InvalidKeyException,
			NoSuchAlgorithmException, NoSuchProviderException,
			SignRequestException {

		final PKIHeaderBuilder myPKIHeader = CmpMessageHelper.createPKIHeaderBuilder(getSender(), getRecipient(), getSenderNonce(), getRecipientNonce(), getTransactionId());

		PKIStatusInfo myPKIStatusInfo = new PKIStatusInfo(PKIStatus.granted); // 0 = accepted
		if (status != ResponseStatus.SUCCESS && status != ResponseStatus.GRANTED_WITH_MODS) {
			if (log.isDebugEnabled()) {
				log.debug("Creating a rejection message");
			}
			myPKIStatusInfo = new PKIStatusInfo(PKIStatus.rejection, null, new PKIFailureInfo(failInfo.getCMPValue()));
			if(failText != null && failInfo != null) {
			    myPKIStatusInfo = new PKIStatusInfo(PKIStatus.rejection, new PKIFreeText(failText), new PKIFailureInfo(failInfo.getCMPValue()));
			}
		}
		RevRepContentBuilder revBuilder = new RevRepContentBuilder();
		revBuilder.add(myPKIStatusInfo);
		RevRepContent myRevrepMessage = revBuilder.build();

		PKIBody myPKIBody = new PKIBody(CmpPKIBodyConstants.REVOCATIONRESPONSE, myRevrepMessage);
		PKIMessage myPKIMessage;

		if ((getPbeDigestAlg() != null) && (getPbeMacAlg() != null) && (getPbeKeyId() != null) && (getPbeKey() != null) ) {
		    myPKIHeader.setProtectionAlg(new AlgorithmIdentifier(CMPObjectIdentifiers.passwordBasedMac));
		    myPKIMessage = new PKIMessage(myPKIHeader.build(), myPKIBody);
			responseMessage = CmpMessageHelper.protectPKIMessageWithPBE(myPKIMessage, getPbeKeyId(), getPbeKey(), getPbeDigestAlg(), getPbeMacAlg(), getPbeIterationCount());
		} else {
		    myPKIHeader.setProtectionAlg(new AlgorithmIdentifier(digestAlg));
		    myPKIMessage = new PKIMessage(myPKIHeader.build(), myPKIBody);
            try {
                responseMessage = CmpMessageHelper.signPKIMessage(myPKIMessage, (X509Certificate)signCert, signKey, digestAlg, provider);
            } catch (CertificateEncodingException e) {
                log.error("Failed to sign CMPRevokeResponseMessage");
                log.error(e.getLocalizedMessage(), e);
                responseMessage = getUnprotectedResponseMessage(myPKIMessage);
            } catch (SecurityException e) {
                log.error("Failed to sign CMPRevokeResponseMessage");
                log.error(e.getLocalizedMessage(), e);
                responseMessage = getUnprotectedResponseMessage(myPKIMessage);
            } catch (SignatureException e) {
                log.error("Failed to sign CMPRevokeResponseMessage");
                log.error(e.getLocalizedMessage(), e);
                responseMessage = getUnprotectedResponseMessage(myPKIMessage);
            }
		}
		return true;
	}

	private byte[] getUnprotectedResponseMessage(PKIMessage msg) {
	    byte[] resp = null;
	    try {
	        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        final DEROutputStream mout = new DEROutputStream( baos );
	        mout.writeObject( msg );
	        mout.close();
	        resp = baos.toByteArray();
	    } catch (IOException e) {
	        log.error(e.getLocalizedMessage(), e);
	    }
	    return resp;
	}
	   
	@Override
	public boolean requireSignKeyInfo() {
		return false;
	}

	@Override
	public void setSignKeyInfo(Certificate cert, PrivateKey key,
			String provider) {
	    
        this.signCert = cert;
        this.signKey = key;
        if (provider != null) {
            this.provider = provider;
        }
	}

	@Override
	public void setRecipientKeyInfo(byte[] recipientKeyInfo) {
	}

	@Override
	public void setPreferredDigestAlg(String digest) {
	}

	@Override
	public void setRequestType(int reqtype) {
	}

	@Override
	public void setRequestId(int reqid) {
	}

	@Override
    public void setProtectionParamsFromRequest(RequestMessage reqMsg) {
    }

}
