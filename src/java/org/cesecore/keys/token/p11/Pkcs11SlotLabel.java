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
package org.cesecore.keys.token.p11;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.security.InvalidParameterException;
import java.security.Provider;
import java.security.Security;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;
import org.cesecore.util.FileTools;

/**
 * @version $Id$
 *
 * Object to handle a p11 SLOT.
 * You can get a provider for the slot with {@link Pkcs11SlotLabel#getP11Provider(String, String, String)}
 *
 */
public class Pkcs11SlotLabel {
    
    /** The name of Suns pkcs11 implementation */
    public static final String SUN_PKCS11_CLASS = "sun.security.pkcs11.SunPKCS11";
    public static final String IAIK_PKCS11_CLASS = "iaik.pkcs.pkcs11.provider.IAIKPkcs11";
    public static final String IAIK_JCEPROVIDER_CLASS = "iaik.security.provider.IAIK";
    
    private static final Logger log = Logger.getLogger(Pkcs11SlotLabel.class);
    
    /**
     * Defines how the slot is specified.
     */
    public enum Type {
        TOKEN_LABEL("Token Label"),
        SLOT_LIST_IX("Slot list index"),
        SLOT_ID("slot ID"),
        SUN_FILE("Sun configuration file");

        private final String description;
        private Type( String _description ) {
            this.description = _description;
        }
        @Override
        public String toString() {
            return this.description;
        }
    }
    
    private final static String DELIMETER = ":";
    private final Type type;
    private final String value;

    private static final Lock slotIDLock = new ReentrantLock();

    /**
     * Create an instance with a string that defines the slot.
     * @param taggedString Defines type and value this like this '<Type>:<value>'. Example slot with token label "Toledo": TOKEN_LABEL:Toledo
     */
    public Pkcs11SlotLabel( final String taggedString ) {
        final String[] split = taggedString.split(DELIMETER, 2);
        try {
            this.type = Type.valueOf(split[0].trim());
        } catch( IllegalArgumentException e ) {
            throw new IllegalArgumentException("P11 Slot specifier '"+taggedString+"' has a tag that is not existing: '"+split[0]+"'");
        }
        this.value = split.length>1 ? split[1].trim() : null;
    }
    /**
     * Use explicit values.
     * @param _type
     * @param _value
     */
    public Pkcs11SlotLabel( final Type _type, final String _value) {
        this.type = _type;
        this.value = _value.trim();
    }
    /**
     * Get a string that later could be used to create a new object with {@link Pkcs11SlotLabel#PKCS11Slot(String)}.
     * Use it when you want to store a reference to the slot.
     * @return the string.
     */
    public String getTaggedString() {
        return this.type.name() + DELIMETER + this.value;
    }
    
    @Override
    public String toString() {
        return "Slot type: '"+this.type+"'. Slot value: '"+this.value+"'.";
    }
    /**
     * Get provider for the slot.
     * @param fileName path name to the P11 module so file or sun config file (only in the case of {@link #type}=={@link Type#SUN_FILE})
     * @param attributesFile Path to file with P11 attributes to be used when generating keys with the provider. If null a good default will be used.
     * @param privateKeyLabel Label that will be set to all private keys generated by the provider. If null no label will be set.
     * @return the provider.
     * 
     */
    public Provider getP11Provider(final String fileName, final String attributesFile, final String privateKeyLabel) {
        if (StringUtils.isEmpty(fileName)) {
            throw new IllegalArgumentException("A file name must be supplied.");
        }
        final File libFile = new File(fileName);
        if (!libFile.isFile() || !libFile.canRead()) {
            throw new IllegalArgumentException("The file " + fileName + " can't be read.");
        }
        // We will construct the PKCS11 provider (sun.security..., or iaik...) using reflection, because
        // the sun class does not exist on all platforms in jdk5, and we want to be able to compile everything.

        final long slot;
        final boolean isIndex;
        log.debug("slot spec: "+this.toString());
        switch ( this.type ) {
        case TOKEN_LABEL:
            getSunP11Provider(-1, libFile, true, null, null);// creating dummy provider just to have C_Initialize executed with multiple thread arguments.
            slot = getSlotID(this.value, libFile);
            isIndex = false;
            //catch (Exception e) {
            //  throw new IOException("Slot nr " + this.value + " not an integer and sun classes to find slot for token label are not available.", e);
            //   }
            if (slot < 0) {
                throw new IllegalStateException("Token label '" + this.value + "' not found.");
            }
            break;
        case SLOT_ID:
            slot = Long.parseLong( this.value );
            isIndex = false;
            break;
        case SLOT_LIST_IX:
            slot = Long.parseLong( this.value );
            isIndex = true;
            break;
        case SUN_FILE:
            FileInputStream fileInputStream;
            try {
                fileInputStream = new FileInputStream(libFile);
            } catch (FileNotFoundException e) {
                throw new IllegalArgumentException("File " + libFile + " was not found.");
            }        
            return getSunP11Provider(fileInputStream);
        default:
            throw new IllegalStateException("This should not ever happen if all type of slots are tested.");
        }
        {// We will first try to construct the more competent IAIK provider, if it exists in the classpath
            final Provider prov = getIAIKP11Provider(slot, libFile, isIndex);
            if ( prov!=null ) {
                return prov;
            }
        }
        {// if that does not exist, we will revert back to use the SUN provider
            final Provider prov = getSunP11Provider(slot, libFile, isIndex, attributesFile, privateKeyLabel);
            if ( prov!=null ) {
                return prov;
            }
        }
        log.error("No provider available.");
        return null;
    }
    
    /**
     * Get slot ID for a token label.
     * @param tokenLabel the label.
     * @param the P11 module so file.
     * @return the slot ID.
     * @throws IllegalArgumentException 

     */
    private static long getSlotID(final String tokenLabel, final File file) throws IllegalArgumentException {
        slotIDLock.lock(); // only one thread at a time may use the p11 object.
        try {
            final Pkcs11Wrapper p11 = Pkcs11Wrapper.getInstance(file);
            final long slots[] = p11.C_GetSlotList();
            if (log.isDebugEnabled()) {
                log.debug("Searching for token label:\t" + tokenLabel);
            }
            for (final long slotID : slots) {
                final char label[] = p11.getTokenLabel(slotID);
                if (label == null) {
                    continue;
                }
                final String candidateTokenLabel = new String(label);
                if (log.isDebugEnabled()) {
                    log.debug("Candidate token label:\t" + candidateTokenLabel);
                }
                if (!tokenLabel.equals(candidateTokenLabel.trim())) {
                    continue;
                }
                if (log.isDebugEnabled()) {
                    log.debug("Label '" + tokenLabel + "' found. The slot ID is:\t" + slotID);
                }
                return slotID;
            }
            throw new InvalidParameterException("Token label '" + tokenLabel + "' not found.");
        } finally {
            slotIDLock.unlock();// lock must always be unlocked.
        }
    }
        
    /**
     * Get the IAIK provider.
     * @param slot Slot list index or slot ID.
     * @param libFile P11 module so file.
     * @param isIndex true if first parameter is a slot list index, false if slot ID.
     * @return the provider
     */
    private static Provider getIAIKP11Provider(final long slot, final File libFile, final boolean isIndex) {
        // Properties for the IAIK PKCS#11 provider
        final Properties prop = new Properties();
        try {
            prop.setProperty("PKCS11_NATIVE_MODULE", libFile.getCanonicalPath());
        } catch (IOException e) {
            throw new RuntimeException("Could for unknown reason not construct canonical filename.", e);
        }
        // If using Slot Index it is denoted by brackets in iaik
        prop.setProperty("SLOT_ID", isIndex ? ("[" + slot + "]") : Long.toString(slot));
        if (log.isDebugEnabled()) {
            log.debug(prop.toString());
        }
        Provider ret = null;
        try {
            @SuppressWarnings("unchecked")
            final Class<? extends Provider> implClass = (Class<? extends Provider>) Class.forName(IAIK_PKCS11_CLASS);
            log.info("Using IAIK PKCS11 provider: " + IAIK_PKCS11_CLASS);
            // iaik PKCS11 has Properties as constructor argument
            ret = implClass.getConstructor(Properties.class).newInstance(new Object[] { prop });
            // It's not enough just to add the p11 provider. Depending on algorithms we may have to install the IAIK JCE provider as well in order
            // to support algorithm delegation
            @SuppressWarnings("unchecked")
            final Class<? extends Provider> jceImplClass = (Class<? extends Provider>) Class.forName(IAIK_JCEPROVIDER_CLASS);
            Provider iaikProvider = jceImplClass.getConstructor().newInstance();
            if (Security.getProvider(iaikProvider.getName()) == null) {
                log.info("Adding IAIK JCE provider for Delegation: " + IAIK_JCEPROVIDER_CLASS);
                Security.addProvider(iaikProvider);
            }
        } catch (Exception e) {
            // do nothing here. Sun provider is tested below.
        }
        return ret;
    }
    /**
     * Get the Sun provider.
     * @param slot Slot list index or slot ID.
     * @param libFile P11 module so file.
     * @param isIndex true if first parameter is a slot list index, false if slot ID.
     * @param attributesFile Path to file with P11 attributes to be used when generating keys with the provider. If null a good default will be used.
     * @param privateKeyLabel Label that will be set to all private keys generated by the provider. If null no label will be set.
     * @return the provider
     */
    private static Provider getSunP11Provider(final long slot, final File libFile, final boolean isIndex, final String attributesFile,
            String privateKeyLabel) {

        // Properties for the SUN PKCS#11 provider
        final String sSlot = Long.toString(slot);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintWriter pw = new PrintWriter(baos);
        pw.println("name = " + libFile.getName() + "-slot" + sSlot);
        try {
            pw.println("library = " + libFile.getCanonicalPath());
        } catch (IOException e) {
            throw new RuntimeException("Could for unknown reason not construct canonical filename.", e);
        }
        if ( sSlot!=null ) {
            pw.println("slot" + (isIndex ? "ListIndex" : "") + " = " + sSlot);
        }
        if (attributesFile != null) {
            byte[] attrs;
            try {
                attrs = FileTools.readFiletoBuffer(attributesFile);
            } catch (FileNotFoundException e) {
              throw new IllegalArgumentException("File " + attributesFile + " was not found.", e);
            }
            pw.println(new String(attrs));
        } else {
            // setting the attributes like this should work for most HSMs.
            pw.println("attributes(*, *, *) = {");
            pw.println("  CKA_TOKEN = true"); // all created objects should be permanent. They should not only exiswt during the session.
            pw.println("}");
            pw.println("attributes(*, CKO_PUBLIC_KEY, *) = {");
            pw.println("  CKA_ENCRYPT = true");
            pw.println("  CKA_VERIFY = true");
            pw.println("  CKA_WRAP = true");// no harm allowing wrapping of keys. created private keys can not be wrapped anyway since CKA_EXTRACTABLE
            // is false.
            pw.println("}");
            pw.println("attributes(*, CKO_PRIVATE_KEY, *) = {");
            pw.println("  CKA_PRIVATE = true"); // always require logon with password to use the key
            pw.println("  CKA_SENSITIVE = true"); // not possible to read the key
            pw.println("  CKA_EXTRACTABLE = false"); // not possible to wrap the key with another key
            pw.println("  CKA_DECRYPT = true");
            pw.println("  CKA_SIGN = true");
            if ( privateKeyLabel!=null && privateKeyLabel.length()>0 ) {
                pw.print("  CKA_LABEL = 0h");
                pw.println(new String(Hex.encode(privateKeyLabel.getBytes())));
            }
            pw.println("  CKA_UNWRAP = true");// for unwrapping of session keys,
            pw.println("}");
            pw.println("attributes(*, CKO_SECRET_KEY, *) = {");
            pw.println("  CKA_SENSITIVE = true"); // not possible to read the key
            pw.println("  CKA_EXTRACTABLE = false"); // not possible to wrap the key with another key
            pw.println("  CKA_ENCRYPT = true");
            pw.println("  CKA_DECRYPT = true");
            pw.println("  CKA_SIGN = true");
            pw.println("  CKA_VERIFY = true");
            pw.println("  CKA_WRAP = true");// for unwrapping of session keys,
            pw.println("  CKA_UNWRAP = true");// for unwrapping of session keys,
            pw.println("}");
        }
        pw.flush();
        pw.close();
        if (log.isDebugEnabled()) {
            log.debug(baos.toString());
        }
        return getSunP11Provider( new ByteArrayInputStream(baos.toByteArray()) );
    }

    /**
     * @param is InputStream for sun configuration file.
     * @return The Sun provider
     */
    private static Provider getSunP11Provider(final InputStream is) {
        try {
            // Sun PKCS11 has InputStream as constructor argument
            @SuppressWarnings("unchecked")
            final Class<? extends Provider> implClass = (Class<? extends Provider>) Class.forName(SUN_PKCS11_CLASS);
            log.info("Using SUN PKCS11 provider: " + SUN_PKCS11_CLASS);
            return implClass.getConstructor(InputStream.class).newInstance(new Object[] { is });
        } catch (Exception e) {
            String msg = "Error constructing pkcs11 provider: " + e.getMessage();
            log.error(msg);
            throw new IllegalStateException(msg, e);
        }
    }
    
    /**
     * Creates a SUN or IAIK PKCS#11 provider using the passed in pkcs11 library. First we try to see if the IAIK provider is available, because it
     * supports more algorithms. If the IAIK provider is not available in the classpath, we try the SUN provider.
     * 
     * @param sSlot
     *            pkcs11 slot number (ID or IX) or null if a config file name is provided as fileName. Could also be any of: TOKEN_LABEL:<string> SLOT_LIST_IX:<int> SLOT_ID:<long> SUN_FILE:<string>
     * @param fileName
     *            the manufacturers provided pkcs11 library (.dll or .so) or config file name if slot is null
     * @param isIndex
     *            specifies if the slot is a slot number or a slotIndex
     * @param attributesFile
     *            a file specifying PKCS#11 attributes (used mainly for key generation) in the format specified in the
     *            "JavaTM PKCS#11 Reference Guide", http://java.sun.com/javase/6/docs/technotes/guides/security/p11guide.html
     * 
     *            Example contents of attributes file:
     * 
     *            attributes(generate,CKO_PRIVATE_KEY,*) = { CKA_PRIVATE = true CKA_SIGN = true CKA_DECRYPT = true CKA_TOKEN = true }
     * 
     *            See also html documentation for PKCS#11 HSMs in EJBCA.
     * 
     * @return AuthProvider of type "sun.security.pkcs11.SunPKCS11" 
     */
    public static Provider getP11Provider(final String slot, final String fileName, final boolean isIndex, final String attributesFile) {
        return getP11Provider(slot, fileName, isIndex, attributesFile, null);
    }

    /**
     * Creates a SUN or IAIK PKCS#11 provider using the passed in pkcs11 library. First we try to see if the IAIK provider is available, because it
     * supports more algorithms. If the IAIK provider is not available in the classpath, we try the SUN provider.
     * 
     * @param sSlot
     *            pkcs11 slot number (ID or IX) or null if a config file name is provided as fileName. Could also be any of: TOKEN_LABEL:<string> SLOT_LIST_IX:<int> SLOT_ID:<long> SUN_FILE:<string>
     * @param fileName
     *            the manufacturers provided pkcs11 library (.dll or .so) or config file name if slot is null
     * @param isIndex
     *            specifies if the slot is a slot number or a slotIndex
     * @param attributesFile
     *            a file specifying PKCS#11 attributes (used mainly for key generation) in the format specified in the
     *            "JavaTM PKCS#11 Reference Guide", http://java.sun.com/javase/6/docs/technotes/guides/security/p11guide.html
     * 
     *            Example contents of attributes file:
     * 
     *            attributes(generate,CKO_PRIVATE_KEY,*) = { CKA_PRIVATE = true CKA_SIGN = true CKA_DECRYPT = true CKA_TOKEN = true }
     * 
     *            See also html documentation for PKCS#11 HSMs in EJBCA.
     * @param privateKeyLabel
     *            The private key label to be set to generated keys. null means no label.
     * 
     * @return AuthProvider of type "sun.security.pkcs11.SunPKCS11" 
     */
    public static Provider getP11Provider(final String sSlot, final String fileName, final boolean isIndex, final String attributesFile, final String privateKeyLabel) {
        Pkcs11SlotLabel slotSpec;
        if ( sSlot!=null && sSlot.length()>0 ) {
            try {
                Long.parseLong(sSlot);
                slotSpec = new Pkcs11SlotLabel( isIndex ? Pkcs11SlotLabel.Type.SLOT_LIST_IX : Pkcs11SlotLabel.Type.SLOT_ID, sSlot );
            } catch (NumberFormatException e) {
                slotSpec = new Pkcs11SlotLabel(sSlot);
            }
        } else {
            slotSpec = new Pkcs11SlotLabel(Pkcs11SlotLabel.Type.SUN_FILE, null);
        }
        return slotSpec.getP11Provider(fileName, attributesFile, privateKeyLabel);
    }
}
