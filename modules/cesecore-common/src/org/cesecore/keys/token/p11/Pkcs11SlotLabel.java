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
import java.lang.reflect.InvocationTargetException;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;
import org.cesecore.keys.token.p11.exception.NoSuchSlotException;
import org.cesecore.util.FileTools;

/**
 * @version $Id$
 *
 * Object for handling a PKCS#11 Slot Label.
 *
 */
public class Pkcs11SlotLabel {

    /** The name of Suns pkcs11 implementation */
    public static final String SUN_PKCS11_CLASS = "sun.security.pkcs11.SunPKCS11";
    public static final String IAIK_PKCS11_CLASS = "iaik.pkcs.pkcs11.provider.IAIKPkcs11";
    public static final String IAIK_JCEPROVIDER_CLASS = "iaik.security.provider.IAIK";

    private static final Logger log = Logger.getLogger(Pkcs11SlotLabel.class);

    private final static String DELIMETER = ":";
    private final Pkcs11SlotLabelType type;
    private final String value;

	private static final Lock slotIDLock = new ReentrantLock();

    /**
     * Use explicit values.
     * @param type
     * @param value
     */
    public Pkcs11SlotLabel(final Pkcs11SlotLabelType type, final String value) {
        if(type == null) {
            throw new IllegalArgumentException("Type can not be null");
        }
        this.type = type;
        this.value = value == null ? null : value.trim();
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
        return "Slot type: '" + this.type + "'. Slot value: '" + this.value + "'.";
    }

    /**
     * Get provider for the slot.
     * @param fileName path name to the P11 module so file or sun config file (only in the case of {@link #type}=={@link Pkcs11SlotLabelType#SUN_FILE})
     * @param attributesFile Path to file with P11 attributes to be used when generating keys with the provider. If null a good default will be used.
     * @param privateKeyLabel Label that will be set to all private keys generated by the provider. If null no label will be set.
     * @return the provider, or null if none is available.
     * @throws NoSuchSlotException if no slot as defined by this slot label was found
     * 
     */
    public Provider getProvider(final String fileName, final String attributesFile, final String privateKeyLabel) throws NoSuchSlotException {
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
        log.debug("slot spec: " + this.toString());
        switch (this.type) {
        case SLOT_LABEL:
            doC_Initialize(libFile);
            slot = getSlotID(this.value, libFile);
            if (slot < 0) {
                throw new IllegalStateException("Token label '" + this.value + "' not found.");
            }
            break;
        case SLOT_NUMBER:
            slot = Long.parseLong(this.value);
            break;
        case SLOT_INDEX:
            //Be generous and allow numbers to act as indexes as well
            slot = Long.parseLong((this.value.charAt(0) == 'i' ? this.value.substring(1) : this.value));
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
            final Provider prov = getIAIKP11Provider(slot, libFile, this.type);
            if (prov != null) {
                return prov;
            }
        }
        {// if that does not exist, we will revert back to use the SUN provider
            final Provider prov = getSunP11Provider( getSunP11ProviderInputStream(slot, libFile, this.type, attributesFile, privateKeyLabel) );
            if (prov != null) {
                return prov;
            }
        }
        log.error("No provider available.");
        return null;
    }

    /** @return a List of "slotId;tokenLabel" in the (indexed) order we get the from the P11 */
    public static List<String> getExtendedTokenLabels(final File libFile) {
        final List<String> tokenLabels = new ArrayList<String>();
        doC_Initialize(libFile);
        slotIDLock.lock(); // only one thread at a time may use the p11 object
        try {
            final Pkcs11Wrapper p11 = Pkcs11Wrapper.getInstance(libFile);
            final long slots[] = p11.C_GetSlotList();
            if (log.isDebugEnabled()) {
                log.debug("Found numer of slots:\t" + slots.length);
            }
            for (int i=0; i<slots.length; i++) {
                final long slotID = slots[i];
                final char label[] = p11.getTokenLabel(slotID);
                if (label == null) {
                    continue;
                }
                final String tokenLabel = new String(label);
                if (log.isDebugEnabled()) {
                    log.debug(i+": Found token label:\t" + tokenLabel + "\tid="+slotID);
                }
                tokenLabels.add(slotID+";"+tokenLabel.trim());
            }
        } finally {
            slotIDLock.unlock();// lock must always be unlocked.
        }
        return tokenLabels;
    }
    
    /**
     * Get slot ID for a token label.
     * @param tokenLabel the label.
     * @param the P11 module so file.
     * @return the slot ID.
     * @throws NoSuchSlotException if no slot as defined by tokenLabel was found

     */
    private static long getSlotID(final String tokenLabel, final File file) throws NoSuchSlotException {
        slotIDLock.lock(); // only one thread at a time may use the p11 object
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
            throw new NoSuchSlotException("Token label '" + tokenLabel + "' not found.");
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
    private static Provider getIAIKP11Provider(final long slot, final File libFile, final Pkcs11SlotLabelType type) {
        // Properties for the IAIK PKCS#11 provider
        final Properties prop = new Properties();
        try {
            prop.setProperty("PKCS11_NATIVE_MODULE", libFile.getCanonicalPath());
        } catch (IOException e) {
            throw new RuntimeException("Could for unknown reason not construct canonical filename.", e);
        }
        // If using Slot Index it is denoted by brackets in iaik
        prop.setProperty("SLOT_ID", type.equals(Pkcs11SlotLabelType.SLOT_INDEX) ? ("[" + slot + "]") : Long.toString(slot));
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
        } catch (InvocationTargetException e) {
            // NOPMD: Ignore, reflection related errors are handled elsewhere
        } catch (InstantiationException e) {
            // NOPMD: Ignore, reflection related errors are handled elsewhere
        } catch (IllegalAccessException e) {
            // NOPMD: Ignore, reflection related errors are handled elsewhere
        } catch (IllegalArgumentException e) {
            // NOPMD: Ignore, reflection related errors are handled elsewhere
        } catch (NoSuchMethodException e) {
            // NOPMD: Ignore, reflection related errors are handled elsewhere
        } catch (SecurityException e) {
            // NOPMD: Ignore, reflection related errors are handled elsewhere
        } catch (ClassNotFoundException e) {
            // NOPMD: Ignore, reflection related errors are handled elsewhere
        }
        return ret;
    }

    /**
     * Get an InputStream to be used to create the Sun provider.
     * @param slot Slot list index or slot ID.
     * @param libFile P11 module so file.
     * @param isIndex true if first parameter is a slot list index, false if slot ID.
     * @param attributesFile Path to file with P11 attributes to be used when generating keys with the provider. If null a good default will be used.
     * @param privateKeyLabel Label that will be set to all private keys generated by the provider. If null no label will be set.
     * @return the stream
     */
    private static InputStream getSunP11ProviderInputStream(final long slot, final File libFile, final Pkcs11SlotLabelType type, final String attributesFile,
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
        if (sSlot != null) {
            pw.println("slot" + (type.isEqual(Pkcs11SlotLabelType.SLOT_INDEX) ? "ListIndex" : "") + " = " + sSlot);
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
            pw.println("attributes(*, CKO_PUBLIC_KEY, *) = {");
            pw.println("  CKA_TOKEN = false"); // Do not save public keys permanent in order to save space.
            pw.println("  CKA_ENCRYPT = true");
            pw.println("  CKA_VERIFY = true");
            pw.println("  CKA_WRAP = true");// no harm allowing wrapping of keys. created private keys can not be wrapped anyway since CKA_EXTRACTABLE
            // is false.
            pw.println("}");
            pw.println("attributes(*, CKO_PRIVATE_KEY, *) = {");
            pw.println("  CKA_TOKEN = true"); // all created private keys should be permanent. They should not only exist during the session.
            pw.println("  CKA_PRIVATE = true"); // always require logon with password to use the key
            pw.println("  CKA_SENSITIVE = true"); // not possible to read the key
            pw.println("  CKA_EXTRACTABLE = false"); // not possible to wrap the key with another key
            pw.println("  CKA_DECRYPT = true");
            pw.println("  CKA_SIGN = true");
            if (privateKeyLabel != null && privateKeyLabel.length() > 0) {
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
        return new ByteArrayInputStream(baos.toByteArray());
    }

    /**
     * Get the provider without taking care of exceptions.
     * @param is InputStream for sun configuration file.
     * @return The Sun provider
     * @throws ClassNotFoundException
     * @throws IllegalArgumentException
     * @throws SecurityException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     */
    private static Provider getSunP11ProviderNoExceptionHandeling(final InputStream is) throws ClassNotFoundException, IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        // Sun PKCS11 has InputStream as constructor argument
        @SuppressWarnings("unchecked")
        final Class<? extends Provider> implClass = (Class<? extends Provider>) Class.forName(SUN_PKCS11_CLASS);
        log.info("Using SUN PKCS11 provider: " + SUN_PKCS11_CLASS);
        return implClass.getConstructor(InputStream.class).newInstance(new Object[] { is });
    }

    /**
     * @param is InputStream for sun configuration file.
     * @return The Sun provider
     */
    private static Provider getSunP11Provider(final InputStream is) {
        try {
            return getSunP11ProviderNoExceptionHandeling(is);
        } catch (Exception e) {
            final String msg = "Error constructing pkcs11 provider: " + e.getMessage();
            log.error(msg);
            throw new IllegalStateException(msg, e);
        }
    }

    /**
     * Creating dummy provider just to have C_Initialize executed with multiple
     * thread argument. If we don't call this method and getSlotID is called
     * then C_Initialize will be called with null argument causing multi threading
     * to be disabled for the token.
     * There is a boolean in the sun code ensuring that C_Initialize is only called
     * once.
     * To check this implementation the p11 spy utils could be used. Check that
     * it is only one C_Initialize call and that null is not passed.
     * @param a file with the path of the p11 module on which C_Finalize should be called.
     */
    private static void doC_Initialize(final File libFile) {
        try {
            getSunP11ProviderNoExceptionHandeling( getSunP11ProviderInputStream(-1, libFile, Pkcs11SlotLabelType.SLOT_NUMBER, null, null) );
        } catch (InvocationTargetException e) {
            // the p11 module don't like the bogus arguments and throws an exception but we don't bother about this since
            // C_Initialize has already been called with multithread arguments.
            log.debug("Get dummy sun provider throws an exception. This is OK.", e);
        } catch (Exception e) {
            final String msg = "Error constructing pkcs11 provider: " + e.getMessage();
            log.error(msg);
            throw new IllegalStateException(msg, e);
        }
    }

    /**
     * Creates a SUN or IAIK PKCS#11 provider using the passed in pkcs11 library. First we try to see if the IAIK provider is available, because it
     * supports more algorithms. If the IAIK provider is not available in the classpath, we try the SUN provider.
     * 
     * @param sSlot
     *            The value of the slot, which may be a number ([0...9]*), an index i[0...9] or a label, but may also be labels matching the former.
     *            To solve this ambiguity, slots will be presumed to be numbers or indexes if the names match, and if no slot is found by that number
     *            or index will then be presumed to be labels (for legacy reasons). 
     * @param fileName
     *            the manufacturers provided pkcs11 library (.dll or .so) or config file name if slot is null

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
     * @return AuthProvider of type "sun.security.pkcs11.SunPKCS11", or null if none is available
     * @throws NoSuchSlotException is no slot as defined by sSlot and slotLabelType was found
     */
    public static Provider getP11Provider(final String sSlot, Pkcs11SlotLabelType slotLabelType, final String fileName, final String attributesFile) throws NoSuchSlotException {
        return getP11Provider(sSlot, slotLabelType, fileName, attributesFile, null);
    }

    /**
     * Creates a SUN or IAIK PKCS#11 provider using the passed in pkcs11 library. First we try to see if the IAIK provider is available, because it
     * supports more algorithms. If the IAIK provider is not available in the classpath, we try the SUN provider.
     * 
     * @param sSlot
     *            The value of the slot, which may be a number ([0...9]*), an index i[0...9] or a label, but may also be labels matching the former.
     *            To solve this ambiguity, slots will be presumed to be numbers or indexes if the names match, and if no slot is found by that number
     *            or index will then be presumed to be labels (for legacy reasons). Can be null if slotLabelType is SUN_FILE, then the slot must be specified in the attributesFile.
     * @param fileName
     *            the manufacturers provided pkcs11 library (.dll or .so) or config file name if slot is null

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
     * @return AuthProvider of type "sun.security.pkcs11.SunPKCS11", or null if none is available
     * @throws NoSuchSlotException if no slot as defined by this label was found
     */
    public static Provider getP11Provider(final String sSlot, final Pkcs11SlotLabelType slotLabelType, final String fileName,
            final String attributesFile, final String privateKeyLabel) throws NoSuchSlotException {
        if ((sSlot == null || sSlot.length() < 1) && !slotLabelType.isEqual(Pkcs11SlotLabelType.SUN_FILE)) {
            return null;
        }
        final Pkcs11SlotLabel slotSpec = new Pkcs11SlotLabel(slotLabelType, sSlot);
        return slotSpec.getProvider(fileName, attributesFile, privateKeyLabel);
    }
}
