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

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.cms.CMSSignedGenerator;
import org.ejbca.core.protocol.IRequestMessage;
import org.ejbca.util.Base64;
import org.ejbca.util.CertTools;

import com.novosec.pkix.asn1.cmp.PKIBody;
import com.novosec.pkix.asn1.cmp.PKIHeader;
import com.novosec.pkix.asn1.crmf.AttributeTypeAndValue;
import com.novosec.pkix.asn1.crmf.CRMFObjectIdentifiers;
import com.novosec.pkix.asn1.crmf.CertReqMessages;
import com.novosec.pkix.asn1.crmf.CertReqMsg;
import com.novosec.pkix.asn1.crmf.CertRequest;
import com.novosec.pkix.asn1.crmf.CertTemplate;
import com.novosec.pkix.asn1.crmf.ProofOfPossession;

/**
 * Certificate request message (crmf) according to RFC4211.
 * - Supported POPO: raVerified (null), i.e. no POPO verification is done, it should be configurable if the CA should allow this or require a real POPO
 * 
 * @author tomas
 * @version $Id: CrmfRequestMessage.java,v 1.2 2006-09-21 11:33:33 anatom Exp $
 */
public class CrmfRequestMessage extends BaseCmpMessage implements IRequestMessage {
	
	private static final Logger log = Logger.getLogger(CrmfRequestMessage.class);
	
    /**
     * Determines if a de-serialized file is compatible with this class.
     *
     * Maintainers must change this value if and only if the new version
     * of this class is not compatible with old versions. See Sun docs
     * for <a href=http://java.sun.com/products/jdk/1.1/docs/guide
     * /serialization/spec/version.doc.html> details. </a>
     *
     */
    static final long serialVersionUID = 1000L;

    private int requestType = 0;
    private int requestId = 0;
	private CertReqMsg req;
	private String b64SenderNonce = null;
	private String b64TransId = null;
	private String defaultCA = null;
	private boolean allowRaVerifyPopo = false;
	private String extractUsernameComponent = null;
    /** manually set username */
    private String username = null;
    /** manually set password */
    private String password = null;

    /** preferred digest algorithm to use in replies, if applicable */
    private transient String preferredDigestAlg = CMSSignedGenerator.DIGEST_SHA1;

    /**
     * 
     * @param header PKIHeader
     * @param body PKIBody
     * @param defaultCA possibility to enforce a certain CA, instead of taking the CA name from the request, if set to null the CA is taken from the request
     * @param allowRaVerifyPopo true if we allows the user/RA to specify the POP should not be verified
     * @param extractUsernameComponent Defines which component from the DN should be used as username in EJBCA. Can be CN, UID or nothing. Null means that the DN should have been pre-set, here it is the same as CN.
     */
	public CrmfRequestMessage(PKIHeader header, PKIBody body, String defaultCA, boolean allowRaVerifyPopo, String extractUsernameComponent) {
		requestType = body.getTagNo();
		CertReqMessages msgs = getCertReqFromTag(body, requestType);
		requestId = msgs.getCertReqMsg(0).getCertReq().getCertReqId().getValue().intValue();
		this.defaultCA = defaultCA;
		this.allowRaVerifyPopo = allowRaVerifyPopo;
		this.extractUsernameComponent = extractUsernameComponent;
		this.req = msgs.getCertReqMsg(0);
		DEROctetString os = header.getTransactionID();
		if (os != null) {
			byte[] val = os.getOctets();
			if (val != null) {
				setTransactionId(new String(Base64.encode(val)));							
			}
		}
		os = header.getSenderNonce();
		if (os != null) {
			byte[] val = os.getOctets();
			if (val != null) {
				setSenderNonce(new String(Base64.encode(val)));							
			}
		}

	}
	
	public PublicKey getRequestPublicKey() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException {
		CertRequest request = req.getCertReq();
		CertTemplate templ = request.getCertTemplate();
		SubjectPublicKeyInfo keyInfo = templ.getPublicKey();
		PublicKey pk = getPublicKey(keyInfo, "BC");
		return pk;
	}
	private PublicKey getPublicKey(SubjectPublicKeyInfo subjectPKInfo, String  provider) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException {		
		try {
			X509EncodedKeySpec xspec = new X509EncodedKeySpec(new DERBitString(subjectPKInfo).getBytes());
			AlgorithmIdentifier keyAlg = subjectPKInfo.getAlgorithmId ();
			return KeyFactory.getInstance(keyAlg.getObjectId().getId (), provider).generatePublic(xspec);
		} catch (InvalidKeySpecException e) {
			throw new InvalidKeyException("Error decoding public key: ", e);
		}
	}
	
    /** force a password, i.e. ignore the password in the request
     */
    public void setPassword(String pwd) {
        this.password = pwd;
    }
	public String getPassword() {
		String ret = null;
		if (password != null) {
			ret = password;
		} else {
			// If there is "Registration Token Control" containing a password, we can use that
			AttributeTypeAndValue av = null;
			int i = 0;
			do {
				av = req.getRegInfo(i);
				if (av != null) {
					if (StringUtils.equals(CRMFObjectIdentifiers.regCtrl_regToken.getId(), av.getObjectId().getId())) {
						DEREncodable enc = av.getParameters();
						DERUTF8String str = DERUTF8String.getInstance(enc);
						ret = str.getString();
						log.debug("Found a request password in CRMF request regCtrl_regToken");
					}
				}
				i++;
			} while ( (av != null) && (ret == null) );
		}		
		if (ret == null) {
			// Otherwise there may be Password Based HMAC/SHA-1 protection
			// TODO 			
		}
		return ret;
	}

    /** force a username, i.e. ignore the DN/username in the request
     */
    public void setUsername(String username) {
        this.username = username;
    }
    
	public String getUsername() {
		String ret = null;
        if (username != null) {
            ret = username;
        } else {
        	// We can configure which part of the users DN should be used as username in EJBCA, for example CN or UID
        	String component = extractUsernameComponent;
        	if (StringUtils.isEmpty(component)) {
        		component = "CN";
        	}
            String name = CertTools.getPartFromDN(getRequestDN(), component);
            if (name == null) {
                log.error("No component "+component+" in DN: "+getRequestDN());
            } else {
            	ret = name;
            }
        }
        return ret;
	}

	public void setIssuerDN(String issuer) {
		this.defaultCA = issuer;
	}
	public String getIssuerDN() {
		String ret = null;
		CertTemplate templ = req.getCertReq().getCertTemplate();
		X509Name name = templ.getIssuer();
		if (name != null) {
			ret = CertTools.stringToBCDNString(name.toString());
		} else {
			ret = defaultCA;
		}
		return ret;
	}

	public BigInteger getSerialNo() {
		return null;
	}

	public String getCRLIssuerDN() {
		return null;
	}

	public BigInteger getCRLSerialNo() {
		return null;
	}

	public String getRequestDN() {
		String ret = null;
		CertTemplate templ = req.getCertReq().getCertTemplate();
		X509Name name = templ.getSubject();
		if (name != null) {
			ret = CertTools.stringToBCDNString(name.toString());
		}
		return ret;
	}

	public boolean verify() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException {
		boolean ret = false;
		ProofOfPossession pop = req.getPop();
		if ( (pop.getRaVerified() != null) && allowRaVerifyPopo) {
			ret = true;
		} else {
			// TODO: actually verify POP
		}
		return ret;
	}

	public boolean requireKeyInfo() {
		return false;
	}

	public void setKeyInfo(X509Certificate cert, PrivateKey key, String provider) {
	}

	public int getErrorNo() {
		return 0;
	}

	public String getErrorText() {
		return null;
	}

	public void setSenderNonce(String b64nonce) {
		this.b64SenderNonce = b64nonce;
	}
	public String getSenderNonce() {
		return b64SenderNonce;
	}

	public void setTransactionId(String b64transid) {
		this.b64TransId = b64transid;
	}
	public String getTransactionId() {
		return b64TransId;
	}

	public byte[] getRequestKeyInfo() {
		return null;
	}

	public String getPreferredDigestAlg() {
		return preferredDigestAlg;
	}

	public boolean includeCACert() {
		return false;
	}

    /** @see org.ejbca.core.protocol.IRequestMessage
     */
    public int getRequestType() {
    	return requestType;
    }

    /** @see org.ejbca.core.protocol.IRequestMessage
     */
    public int getRequestId() {
    	return requestId;
    }

	// Returns the subject DN from the request
	public String getSubjectDN() {
		String ret = null;
		CertTemplate templ = req.getCertReq().getCertTemplate();
		X509Name name = templ.getSubject();
		if (name != null) {
			ret = CertTools.stringToBCDNString(name.toString());
		}
		return ret;
	}

	private CertReqMessages getCertReqFromTag(PKIBody body, int tag) {
		CertReqMessages msgs = null;
		switch (tag) {
		case 0:
			msgs = body.getIr();
			break;
		case 2:
			msgs = body.getCr();
			break;
		case 7:
			msgs = body.getKur();
			break;
		case 9:
			msgs = body.getKrr();
			break;
		case 13:
			msgs = body.getCcr();
			break;
		default:
			break;
		}
		return msgs;
	}
}
