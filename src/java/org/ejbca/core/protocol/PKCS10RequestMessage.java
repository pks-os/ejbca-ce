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
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.util.Date;

import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERPrintableString;
import org.bouncycastle.asn1.DERString;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.cms.CMSSignedGenerator;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.ejbca.util.CertTools;



/**
 * Class to handle PKCS10 request messages sent to the CA.
 *
 * @version $Id$
 */
public class PKCS10RequestMessage implements IRequestMessage {
    /**
     * Determines if a de-serialized file is compatible with this class.
     *
     * Maintainers must change this value if and only if the new version
     * of this class is not compatible with old versions. See Sun docs
     * for <a href=http://java.sun.com/products/jdk/1.1/docs/guide
     * /serialization/spec/version.doc.html> details. </a>
     *
     */
    static final long serialVersionUID = 3597275157018205137L;

    private static final Logger log = Logger.getLogger(PKCS10RequestMessage.class);

    /** Raw form of the PKCS10 message */
    protected byte[] p10msg;

    /** manually set password */
    protected String password = null;

    /** manually set username */
    protected String username = null;
    
    /** If the CA certificate should be included in the response or not, default to true = yes */
    protected boolean includeCACert = true;

    /** preferred digest algorithm to use in replies, if applicable */
    private transient String preferredDigestAlg = CMSSignedGenerator.DIGEST_SHA1;

    /** The pkcs10 request message, not serialized. */
    protected transient PKCS10CertificationRequest pkcs10 = null;

    /** Type of error */
    private int error = 0;

    /** Error text */
    private String errorText = null;

    /**
     * Constructs a new empty PKCS#10 message handler object.
     *
     * @throws IOException if the request can not be parsed.
     */
    public PKCS10RequestMessage() {
    	// No constructor
    }

    /**
     * Constructs a new PKCS#10 message handler object.
     *
     * @param msg The DER encoded PKCS#10 request.
     *
     * @throws IOException if the request can not be parsed.
     */
    public PKCS10RequestMessage(byte[] msg) {
        log.debug(">PKCS10RequestMessage(byte[])");
        this.p10msg = msg;
        init();
        log.debug("<PKCS10RequestMessage(byte[])");
    }

    /**
     * Constructs a new PKCS#10 message handler object.
     *
     * @param p10 the PKCS#10 request
     */
    public PKCS10RequestMessage(PKCS10CertificationRequest p10) {
        log.debug(">PKCS10RequestMessage(ExtendedPKCS10CertificationRequest)");
        p10msg = p10.getEncoded();
        pkcs10 = p10;
        log.debug("<PKCS10RequestMessage(ExtendedPKCS10CertificationRequest)");
    }

    private void init() {
        pkcs10 = new PKCS10CertificationRequest(p10msg);
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws InvalidKeyException DOCUMENT ME!
     * @throws NoSuchAlgorithmException DOCUMENT ME!
     * @throws NoSuchProviderException DOCUMENT ME!
     */
    public PublicKey getRequestPublicKey()
            throws InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException {
        try {
            if (pkcs10 == null) {
                init();
            }
        } catch (IllegalArgumentException e) {
            log.error("PKCS10 not inited!");

            return null;
        }

        return pkcs10.getPublicKey();
    }

    /** force a password, i.e. ignore the challenge password in the request
     */
    public void setPassword(String pwd) {
        this.password = pwd;
    }

    /**
     * Returns the challenge password from the certificattion request.
     *
     * @return challenge password from certification request or null if none exist in the request.
     */
    public String getPassword() {
        if (password != null)
            return password;
        try {
            if (pkcs10 == null) {
                init();
            }
        } catch (IllegalArgumentException e) {
            log.error("PKCS10 not inited!");
            return null;
        }

        String ret = null;

        // Get attributes
        // The password attribute can be either a pkcs_9_at_challengePassword directly
        // or
        // a pkcs_9_at_extensionRequest containing a pkcs_9_at_challengePassword as a
        // X509Extension.
        AttributeTable attributes = null;
        CertificationRequestInfo info = pkcs10.getCertificationRequestInfo();
        if (info != null) {
        	ASN1Set attrs = info.getAttributes();
        	if (attrs != null) {
        		attributes = new AttributeTable(attrs);		
        	}
        }
        if (attributes == null) {
            return null;
        }        
        Attribute attr = attributes.get(PKCSObjectIdentifiers.pkcs_9_at_challengePassword);
        DEREncodable obj = null;
        if (attr == null) {
            // See if we have it embedded in an extension request instead
            attr = attributes.get(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest);
            if (attr == null) {
                return null;                
            }
            log.debug("got extension request");
            ASN1Set values = attr.getAttrValues();
            if (values.size() == 0) {
                return null;
            }
            X509Extensions exts = X509Extensions.getInstance(values.getObjectAt(0));
            X509Extension ext = exts.getExtension(PKCSObjectIdentifiers.pkcs_9_at_challengePassword);
            if (ext == null) {
                log.debug("no challenge password extension");
                return null;
            }
            obj = ext.getValue();
        } else {
            // If it is a challengePassword directly, it's just to grab the value
            ASN1Set values = attr.getAttrValues();
            obj = values.getObjectAt(0);
        }

        if (obj != null) {
            DERString str = null;

            try {
                str = DERPrintableString.getInstance((obj));
            } catch (IllegalArgumentException ie) {
                // This was not printable string, should be utf8string then according to pkcs#9 v2.0
                str = DERUTF8String.getInstance((obj));
            }

            if (str != null) {
                ret = str.getString();
            }
        }

        return ret;
    }

    /** force a username, i.e. ignore the DN/username in the request
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Returns the string representation of the CN field from the DN of the certification request,
     * to be used as username.
     *
     * @return username, which is the CN field from the subject DN in certification request.
     */
    public String getUsername() {
        if (username != null)
            return username;
        String name = CertTools.getPartFromDN(getRequestDN(), "CN");
        if (name == null) {
            log.error("No CN in DN: "+getRequestDN());
            return null;
        }
        // Special if the DN contains unstructuredAddress where it becomes: 
        // CN=pix.primekey.se + unstructuredAddress=pix.primekey.se
        // We only want the CN and not the oid-part.
        String ret = name;
        if (name != null) {
            int index = name.indexOf(' ');
            if (index > 0) {
                ret = name.substring(0, index);
            } else {
                // Perhaps there is no space, only +
                index = name.indexOf('+');
                if (index > 0) {
                    ret = name.substring(0, index);
                }            	
            }
        }
        log.debug("UserName='" + ret + "'");
        return ret;
    }

    /**
     * Gets the issuer DN if contained in the request (the CA the request is targeted at).
     *
     * @return issuerDN of receiving CA or null.
     */
    public String getIssuerDN() {
        return null;
    }

    /**
     * Gets the number (of CA cert) from IssuerAndSerialNumber. Combined with getIssuerDN to identify
     * the CA-certificate of the CA the request is targeted for.
     *
     * @return serial number of CA certificate for CA issuing CRL or null.
     */
    public BigInteger getSerialNo() {
    	return null;
    }
    
    /**
     * Gets the issuer DN (of CA cert) from IssuerAndSerialNumber when this is a CRL request.
     *
     * @return issuerDN of CA issuing CRL.
     */
    public String getCRLIssuerDN() {
        return null;
    }

    /**
     * Gets the number (of CA cert) from IssuerAndSerialNumber when this is a CRL request.
     *
     * @return serial number of CA certificate for CA issuing CRL.
     */
    public BigInteger getCRLSerialNo() {
        return null;
    }

    /**
     * Returns the string representation of the subject DN from the certification request.
     *
     * @return subject DN from certification request or null.
     */
    public String getRequestDN() {
        try {
            if (pkcs10 == null) {
                init();
            }
        } catch (IllegalArgumentException e) {
            log.error("PKCS10 not inited!");
            return null;
        }

        String ret = null;

        // Get subject name from request
        CertificationRequestInfo info = pkcs10.getCertificationRequestInfo();

        if (info != null) {
            X509Name name = info.getSubject();
            ret = name.toString();
        }

        return ret;
    }

    public String getRequestAltNames() {
    	return null;
    }

    /**
     * @see org.ejbca.core.protocol.IRequestMessage
     */
	public Date getRequestValidityNotBefore() {
		return null;
	}
	
    /**
     * @see org.ejbca.core.protocol.IRequestMessage
     */
	public Date getRequestValidityNotAfter() {
		return null;
	}
	
    /**
     * @see org.ejbca.core.protocol.IRequestMessage
     */
	public X509Extensions getRequestExtensions() {
		return null;
	}
	
    /**
     * Gets the underlying BC <code>PKCS10CertificationRequest</code> object.
     *
     * @return the request object
     */
    public PKCS10CertificationRequest getCertificationRequest() {
        try {
            if (pkcs10 == null) {
                init();
            }
        } catch (IllegalArgumentException e) {
            log.error("PKCS10 not inited!");

            return null;
        }

        return pkcs10;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws InvalidKeyException DOCUMENT ME!
     * @throws NoSuchAlgorithmException DOCUMENT ME!
     * @throws NoSuchProviderException DOCUMENT ME!
     */
    public boolean verify()
    throws InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException {
        return verify(null);
    }
    public boolean verify(PublicKey pubKey)
            throws InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException {
        log.debug(">verify()");

        boolean ret = false;

        try {
            if (pkcs10 == null) {
                init();
            }
            if (pubKey == null) {
            	ret = pkcs10.verify();
            } else {
                ret = pkcs10.verify(pubKey, "BC");            	
            }
        } catch (IllegalArgumentException e) {
            log.error("PKCS10 not inited!");
        } catch (InvalidKeyException e) {
            log.error("Error in PKCS10-request:", e);
            throw e;
        } catch (SignatureException e) {
            log.error("Error in PKCS10-signature:", e);
        }

        log.debug("<verify()");

        return ret;
    }

    /**
     * indicates if this message needs recipients public and private key to verify, decrypt etc. If
     * this returns true, setKeyInfo() should be called.
     *
     * @return True if public and private key is needed.
     */
    public boolean requireKeyInfo() {
        return false;
    }

    /**
     * Sets the public and private key needed to decrypt/verify the message. Must be set if
     * requireKeyInfo() returns true.
     *
     * @param cert certificate containing the public key.
     * @param key private key.
     * @param provider the provider to use, if the private key is on a HSM you must use a special provider. If null is given, the default BC provider is used.
     *
     * @see #requireKeyInfo()
     */
    public void setKeyInfo(Certificate cert, PrivateKey key, String Provider) {
    }

    /**
     * Returns an error number after an error has occured processing the request
     *
     * @return class specific error number
     */
    public int getErrorNo() {
        return error;
    }

    /**
     * Returns an error message after an error has occured processing the request
     *
     * @return class specific error message
     */
    public String getErrorText() {
        return errorText;
    }

    /**
     * Returns a senderNonce if present in the request
     *
     * @return senderNonce
     */
    public String getSenderNonce() {
        return null;
    }

    /**
     * Returns a transaction identifier if present in the request
     *
     * @return transaction id
     */
    public String getTransactionId() {
        return null;
    }

    /**
     * Returns requesters key info, key id or similar
     *
     * @return request key info
     */
    public byte[] getRequestKeyInfo() {
        return null;
    }
    
    /** @see org.ejbca.core.protocol.IRequestMessage
     */
    public String getPreferredDigestAlg() {
    	return preferredDigestAlg;
    }
    /** @see org.ejbca.core.protocol.IRequestMessage
     */
    public boolean includeCACert() {
    	return includeCACert;
    }

    /** @see org.ejbca.core.protocol.IRequestMessage
     */
    public int getRequestType() {
    	return 0;
    }
    
    /** @see org.ejbca.core.protocol.IRequestMessage
     */
    public int getRequestId() {
    	return 0;
    }
    
    /** @see org.ejbca.core.protocol.IRequestMessage
     */
    public IResponseMessage createResponseMessage(Class responseClass, IRequestMessage req, Certificate cert, PrivateKey signPriv, PrivateKey encPriv, String provider) {
    	return RequestMessageUtils.createResponseMessage(responseClass, req, cert, signPriv, encPriv, provider);
    }
} // PKCS10RequestMessage
