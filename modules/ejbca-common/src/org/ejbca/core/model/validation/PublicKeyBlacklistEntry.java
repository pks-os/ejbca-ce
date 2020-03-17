/*************************************************************************
 *                                                                       *
 *  CESeCore: CE Security Core                                           *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General                  *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/

package org.ejbca.core.model.validation;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;

import javax.xml.bind.DatatypeConverter;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;
import org.cesecore.internal.InternalResources;
import org.cesecore.util.CertTools;

/**
 * Domain class representing a public key blacklist entry.
 *
 * @version $Id$
 */
public class PublicKeyBlacklistEntry extends BlacklistEntry implements Serializable, Cloneable {
    private static final long serialVersionUID = -315759758359854900L;
    private static final Logger log = Logger.getLogger(PublicKeyBlacklistEntry.class);
    public static final String TYPE="PUBLICKEY";

    protected static final InternalResources intres = InternalResources.getInstance();

    /** Public key reference (set while validate). */
    private transient PublicKey publicKey;

    /**
     * Creates a new instance.
     */
    public PublicKeyBlacklistEntry() {
        super(PublicKeyBlacklistEntry.TYPE);
    }

    /**
     * Creates a new instance.
     */
    public PublicKeyBlacklistEntry(int id, String fingerprint, String keyspec) {
        super(id, PublicKeyBlacklistEntry.TYPE, fingerprint, keyspec);
    }

    @Override
    public String getType() {
        return PublicKeyBlacklistEntry.TYPE;
    }
    
    /**
     * Gets the fingerprint of the public key to blacklist. See {@link #setFingerprint(String)}.
     * 
     * @return the fingerprint of the blacklisted public key.
     */
    public String getFingerprint() {
        return getValue();
    }

    /**
     * Sets the fingerprint of the public key to blacklist. Two different formats are supported:
     * <ol>
     *      <li>If the key is an RSA key, a SHA-256 hash of the DER encoded public key modulus, or if the
     *      key is an EC or DSA key, the fingerprint of the whole DER encoded public key. This is the format
     *      traditionally used by EJBCA.</li>
     *      <li>The last 10 bytes of the SHA-1 fingerprint of the RSA pulbic key modulus, computed with <code>ssh-keygen -l</code>.
     *      This is the format used by <code>ssh-vulnkey</code>.</li>
     *</ol>
     *
     * @param fingerprint the fingerprint of the blacklisted public key.
     */
    public void setFingerprint(String fingerprint) {
        setValue(fingerprint);
    }

    /** 
     * Sets the fingerprint in the correct format from a public key object
     * see {@link #setFingerprint(String)}
     * @param publicKey an RSA public key
     */
    public void setFingerprint(PublicKey publicKey) {
        setValue(createFingerprint(publicKey));
    }
    
    /** 
     * Creates a SHA-256 fingerprint from a public key object. 
     * 
     * <p>If the key is an RSA key, the fingerprint 
     * is computed as the SHA-256 hash of the DER encoded public key modulus. This because blacklist is typically due to 
     * weak random number generator (Debian weak keys) and we then want to capture all keys generated by this, so we don't 
     * want to include the chosen <i>e</i>, only the randomly generated <i>N</i>.
     * 
     * <p>If the key is an EC or DSA key, the fingerprint is computed as the SHA-256 hash of the whole DER encoded public key. 
     * 
     * <p>See {@link #setFingerprint(String)}.
     * 
     * @param pk a public key, can be RSA, ECDSA or DSA 
     * @return public a public key fingerprint to use in the public key blacklist.
     */
    public static String createFingerprint(final PublicKey pk) {
        if (pk == null) {
            return null;
        }
        try {
            if (pk instanceof RSAPublicKey) {
                final RSAPublicKey rsapk = (RSAPublicKey) pk;
                final byte[] modulusBytes = rsapk.getModulus().toByteArray();
                final MessageDigest digest = MessageDigest.getInstance("SHA-256");
                digest.reset();
                digest.update(modulusBytes);
                final String fingerprint = Hex.toHexString(digest.digest());
                if (log.isTraceEnabled()) {
                    log.trace("Created fingerprint for RSA public key: " + fingerprint);
                }
                return fingerprint;
            } else {
                final String fingerprint = CertTools.createPublicKeyFingerprint(pk, "SHA-256");
                if (log.isTraceEnabled()) {
                    log.trace("Created fingerprint for " + pk.getFormat() + " public key: " + fingerprint);
                }
                return fingerprint;
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to create SHA-256 fingerprint. Is the algorithm supported on this system?", e);
        }
    }
    
    /** 
     * Creates a truncated SHA-1 fingerprint compatible with <code>ssh-vulnkey</code> from a public key object. 
     * 
     * @param pk an RSA key to create a Debian fingerprint for.
     * @return public a public key fingerprint to use in the public key blacklist.
     */
    public static String createDebianFingerprint(final RSAPublicKey publicKey) {
        if (publicKey == null) {
            return null;
        }
        try {
            log.error(new String("Modulus=" + publicKey.getModulus().toString(16).toUpperCase()));
            return DatatypeConverter
                    .printHexBinary(MessageDigest.getInstance("SHA-1")
                            .digest(new String("Modulus=" + publicKey.getModulus().toString(16).toUpperCase() + "\n")
                                    .getBytes(StandardCharsets.US_ASCII)))
                    .substring(20, 40).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to create SHA-1 fingerprint. Is the algorithm supported on this system?", e);
        }
    }

    /**
     * Gets the public key, have to be set transient with {@link #setPublicKey(PublicKey)}, not available after serialization
     * or storage.
     * @return the public key.
     */
    public PublicKey getPublicKey() {
        return publicKey;
    }

    /**
     * Sets the transient public key, see {@link #getPublicKey()}
     * @param publicKey the public key.
     */
    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

}
