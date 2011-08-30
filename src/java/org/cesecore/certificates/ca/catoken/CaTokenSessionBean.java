/*************************************************************************
 *                                                                       *
 *  CESeCore: CE Security Core                                           *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.cesecore.certificates.ca.catoken;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.apache.log4j.Logger;
import org.cesecore.audit.enums.EventStatus;
import org.cesecore.audit.enums.EventTypes;
import org.cesecore.audit.enums.ModuleTypes;
import org.cesecore.audit.enums.ServiceTypes;
import org.cesecore.audit.log.SecurityEventsLoggerSessionLocal;
import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.certificates.ca.CA;
import org.cesecore.certificates.ca.CADoesntExistsException;
import org.cesecore.certificates.ca.CaSessionLocal;
import org.cesecore.internal.InternalResources;
import org.cesecore.jndi.JndiConstants;
import org.cesecore.keys.token.CryptoTokenAuthenticationFailedException;
import org.cesecore.keys.token.CryptoTokenOfflineException;
import org.cesecore.keys.token.IllegalCryptoTokenException;
import org.cesecore.keys.util.KeyTools;
import org.cesecore.util.CryptoProviderTools;

/**
 * Implementation of CaTokenSession
 * 
 * @version $Id: CaTokenSessionBean.java 897 2011-06-20 11:17:25Z johane $
 */
@Stateless(mappedName = JndiConstants.APP_JNDI_PREFIX + "CaTokenSessionRemote")
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class CaTokenSessionBean implements CaTokenSessionLocal, CaTokenSessionRemote {

    private static final Logger log = Logger.getLogger(CaTokenSessionBean.class);

    /** Internal localization of logs and errors */
    private static final InternalResources intres = InternalResources.getInstance();

    @EJB
    private SecurityEventsLoggerSessionLocal logSession;

    @EJB
    private CaSessionLocal caSession;

    @PostConstruct
    public void postConstruct() {
        // Install BouncyCastle provider if not available
        CryptoProviderTools.installBCProviderIfNotAvailable();
    }

    @Override
    public void deactivateCAToken(final AuthenticationToken admin, final int caid) throws CADoesntExistsException, AuthorizationDeniedException,
            IllegalCryptoTokenException {
        if (log.isTraceEnabled()) {
            log.trace(">deactivateCAToken: " + caid);
        }
        CA ca = caSession.getCA(admin, caid);
        ca.getCAToken().getCryptoToken().deactivate();
        // Update CA tokeninfo 
        int tokenstatus = ca.getCAToken().getCryptoToken().getTokenStatus();
        ca.getCAInfo().getCATokenInfo().setTokenStatus(tokenstatus);
        String msg = intres.getLocalizedMessage("catoken.deactivated", caid);
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("msg", msg);
        logSession.log(EventTypes.CA_TOKENDEACTIVATE, EventStatus.SUCCESS, ModuleTypes.CA, ServiceTypes.CORE, admin.toString(), Integer.valueOf(caid).toString(), null, null, details);
        if (log.isTraceEnabled()) {
            log.trace("<deactivateCAToken: " + caid);
        }
    }

    @Override
    public void activateCAToken(final AuthenticationToken admin, final int caid, final char[] authenticationcode) throws CADoesntExistsException,
            AuthorizationDeniedException, CryptoTokenOfflineException, CryptoTokenAuthenticationFailedException, IllegalCryptoTokenException {
        if (log.isTraceEnabled()) {
            log.trace(">activateCAToken: " + caid);
        }
        CA ca = caSession.getCA(admin, caid);
        ca.getCAToken().getCryptoToken().activate(authenticationcode);
        // Update CA tokeninfo 
        int tokenstatus = ca.getCAToken().getCryptoToken().getTokenStatus();
        ca.getCAInfo().getCATokenInfo().setTokenStatus(tokenstatus);
        String msg = intres.getLocalizedMessage("catoken.activated", caid);
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("msg", msg);
        logSession.log(EventTypes.CA_TOKENACTIVATE, EventStatus.SUCCESS, ModuleTypes.CA, ServiceTypes.CORE, admin.toString(), Integer.valueOf(caid).toString(), null, null, details);
        if (log.isTraceEnabled()) {
            log.trace("<activateCAToken: " + caid);
        }
    }

    @Override
    public void generateKeys(final AuthenticationToken admin, final int caid, final char[] authenticationcode, final boolean renew,
            final boolean activate) throws CADoesntExistsException, AuthorizationDeniedException, InvalidKeyException,
            CryptoTokenAuthenticationFailedException, CryptoTokenOfflineException, NoSuchAlgorithmException, CertificateException, KeyStoreException,
            NoSuchProviderException, InvalidAlgorithmParameterException, SignatureException, IllegalCryptoTokenException, IOException {
        if (log.isTraceEnabled()) {
            log.trace(">generateKeys: " + caid);
        }
        CA ca = caSession.getCA(admin, caid);
        CAToken token = ca.getCAToken();
        CATokenInfo oldinfo = ca.getCAToken().getTokenInfo();
        Properties oldprop = oldinfo.getProperties();
        String oldsequence = oldinfo.getKeySequence();
        token.generateKeys(authenticationcode, renew, activate);
        ca.setCAToken(token);
        caSession.editCA(admin, ca, false);
        ca.getCAToken().getCryptoToken().activate(authenticationcode);
        CATokenInfo info = ca.getCAToken().getTokenInfo();
        Properties prop = info.getProperties();
        String sequence = info.getKeySequence();
        String msg = intres.getLocalizedMessage("catoken.generatedkeys", caid, renew, activate);
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("msg", msg);
        details.put("oldproperties", oldprop);
        details.put("oldsequence", oldsequence);
        details.put("properties", prop);
        details.put("sequence", sequence);
        logSession.log(EventTypes.CA_KEYGEN, EventStatus.SUCCESS, ModuleTypes.CA, ServiceTypes.CORE, admin.toString(), Integer.valueOf(caid).toString(), null, null, details);
        if (log.isTraceEnabled()) {
            log.trace("<generateKeys: " + caid);
        }
    }

    @Override
    public void activateNextSignKey(final AuthenticationToken admin, final int caid, final char[] authenticationcode) throws CADoesntExistsException,
            AuthorizationDeniedException, InvalidKeyException, CryptoTokenAuthenticationFailedException, CryptoTokenOfflineException,
            KeyStoreException, NoSuchProviderException, NoSuchAlgorithmException, CertificateException, SignatureException,
            IllegalCryptoTokenException, IOException {
        if (log.isTraceEnabled()) {
            log.trace(">activateNextSignKey: " + caid);
        }
        CA ca = caSession.getCA(admin, caid);
        CAToken token = ca.getCAToken();
        token.activateNextSignKey(authenticationcode);
        ca.setCAToken(token);
        caSession.editCA(admin, ca, false);
        ca.getCAToken().getCryptoToken().activate(authenticationcode);
        CATokenInfo info = ca.getCAToken().getTokenInfo();
        Properties prop = info.getProperties();
        String sequence = info.getKeySequence();
        String msg = intres.getLocalizedMessage("catoken.activatednextkey", caid);
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("msg", msg);
        details.put("properties", prop);
        details.put("sequence", sequence);
        logSession.log(EventTypes.CA_KEYACTIVATE, EventStatus.SUCCESS, ModuleTypes.CA, ServiceTypes.CORE, admin.toString(), Integer.valueOf(caid).toString(), null, null, details);
        if (log.isTraceEnabled()) {
            log.trace("<activateNextSignKey: " + caid);
        }
    }

    @Override
    public void setTokenProperty(final AuthenticationToken admin, final int caid, final char[] authenticationcode, final String key,
            final String value) throws CADoesntExistsException, AuthorizationDeniedException, IllegalCryptoTokenException,
            CryptoTokenOfflineException, CryptoTokenAuthenticationFailedException {
        if (log.isTraceEnabled()) {
            log.trace(">setTokenProperty: " + caid);
        }
        CA ca = caSession.getCA(admin, caid);
        CAToken token = ca.getCAToken();
        CATokenInfo oldinfo = ca.getCAToken().getTokenInfo();
        Properties oldprop = oldinfo.getProperties();

        CATokenInfo info = ca.getCAToken().getTokenInfo();
        Properties prop = info.getProperties();
        prop.setProperty(key, value);
        info.setProperties(prop);
        token.updateTokenInfo(info);

        ca.setCAToken(token);
        caSession.editCA(admin, ca, false);
        ca.getCAToken().getCryptoToken().activate(authenticationcode);

        String msg = intres.getLocalizedMessage("catoken.setproperty", caid, key, value);
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("msg", msg);
        details.put("oldproperties", oldprop);
        details.put("properties", prop);
        logSession.log(EventTypes.CA_EDITING, EventStatus.SUCCESS, ModuleTypes.CA, ServiceTypes.CORE, admin.toString(), Integer.valueOf(caid).toString(), null, null, details);
        if (log.isTraceEnabled()) {
            log.trace("<setTokenProperty: " + caid);
        }
    }

    @Override
    public void deleteTokenEntry(final AuthenticationToken admin, final int caid, char[] authenticationcode, String alias)
            throws CADoesntExistsException, AuthorizationDeniedException, IllegalCryptoTokenException, CryptoTokenOfflineException,
            CryptoTokenAuthenticationFailedException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        if (log.isTraceEnabled()) {
            log.trace(">deleteTokenEntry: " + caid);
        }
        CA ca = caSession.getCA(admin, caid);
        CAToken token = ca.getCAToken();
        token.getCryptoToken().activate(authenticationcode);
        token.getCryptoToken().deleteEntry(authenticationcode, alias);
        ca.setCAToken(token);
        caSession.editCA(admin, ca, false);
        ca.getCAToken().getCryptoToken().activate(authenticationcode);

        String msg = intres.getLocalizedMessage("token.deleteentry", alias, caid);
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("msg", msg);
        logSession.log(EventTypes.CA_KEYDELETE, EventStatus.SUCCESS, ModuleTypes.CA, ServiceTypes.CORE, admin.toString(), Integer.valueOf(caid).toString(), null, null, details);
        if (log.isTraceEnabled()) {
            log.trace("<deleteTokenEntry: " + caid);
        }
    }

    @Override
    public void generateKeyPair(final AuthenticationToken admin, final int caid, final char[] authenticationcode, final String keySpec,
            final String alias) throws CADoesntExistsException, AuthorizationDeniedException, CryptoTokenOfflineException,
            CryptoTokenAuthenticationFailedException, IllegalCryptoTokenException, InvalidKeyException, NoSuchAlgorithmException,
            NoSuchProviderException, InvalidAlgorithmParameterException, SignatureException, KeyStoreException, CertificateException, IOException {
        if (log.isTraceEnabled()) {
            log.trace(">generateKeyPair: " + caid);
        }
        CA ca = caSession.getCA(admin, caid);
        CAToken token = ca.getCAToken();
        token.getCryptoToken().activate(authenticationcode);
        token.getCryptoToken().generateKeyPair(keySpec, alias);
        ca.setCAToken(token);
        caSession.editCA(admin, ca, false);
        ca.getCAToken().getCryptoToken().activate(authenticationcode);

        String msg = intres.getLocalizedMessage("token.generatedkeypair", keySpec, alias, caid);
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("msg", msg);
        logSession.log(EventTypes.CA_KEYGEN, EventStatus.SUCCESS, ModuleTypes.CA, ServiceTypes.CORE, admin.toString(), Integer.valueOf(caid).toString(), null, null, details);
        if (log.isTraceEnabled()) {
            log.trace(">generateKeyPair: " + caid);
        }
    }

    @Override
    public void generateKeyPair(final AuthenticationToken admin, final int caid, final char[] authenticationcode, final PublicKey template,
            final String alias) throws CADoesntExistsException, AuthorizationDeniedException, CryptoTokenOfflineException,
            CryptoTokenAuthenticationFailedException, IllegalCryptoTokenException, InvalidKeyException, NoSuchAlgorithmException,
            NoSuchProviderException, InvalidAlgorithmParameterException, SignatureException, KeyStoreException, CertificateException, IOException {
        if (log.isTraceEnabled()) {
            log.trace(">generateKeyPair: " + caid);
        }
        CA ca = caSession.getCA(admin, caid);
        CAToken token = ca.getCAToken();
        token.getCryptoToken().activate(authenticationcode);
        AlgorithmParameterSpec spec = KeyTools.getKeyGenSpec(template);
        token.getCryptoToken().generateKeyPair(spec, alias);
        ca.setCAToken(token);
        caSession.editCA(admin, ca, false);
        ca.getCAToken().getCryptoToken().activate(authenticationcode);

        String msg = intres.getLocalizedMessage("token.generatedkeypair", spec.getClass(), alias, caid);
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("msg", msg);
        logSession.log(EventTypes.CA_KEYGEN, EventStatus.SUCCESS, ModuleTypes.CA, ServiceTypes.CORE, admin.toString(), Integer.valueOf(caid).toString(), null, null, details);
        if (log.isTraceEnabled()) {
            log.trace(">generateKeyPair: " + caid);
        }
    }

    @Override
    public PublicKey getPublicKey(final AuthenticationToken admin, final int caid, final char[] authenticationcode, final String alias)
            throws CADoesntExistsException, AuthorizationDeniedException, IllegalCryptoTokenException, CryptoTokenOfflineException,
            CryptoTokenAuthenticationFailedException {
        if (log.isTraceEnabled()) {
            log.trace(">getPublicKey (alias): " + caid+", "+alias);
        }
        final CA ca = caSession.getCA(admin, caid);
        final CAToken token = ca.getCAToken();
        token.getCryptoToken().activate(authenticationcode);
        final PublicKey pubK = token.getCryptoToken().getPublicKey(alias);
        if (log.isTraceEnabled()) {
            log.trace(">getPublicKey (alias): " + caid+", "+alias);
        }
        return pubK;
    }

    @Override
    public PublicKey getPublicKey(final AuthenticationToken admin, final int caid, final char[] authenticationcode, final int keyPurpose)
            throws CADoesntExistsException, AuthorizationDeniedException, IllegalCryptoTokenException, CryptoTokenOfflineException,
            CryptoTokenAuthenticationFailedException {
        if (log.isTraceEnabled()) {
            log.trace(">getPublicKey (purpose): " + caid+", "+keyPurpose);
        }
        final CA ca = caSession.getCA(admin, caid);
        final CAToken token = ca.getCAToken();
        token.getCryptoToken().activate(authenticationcode);
        final String alias = token.getKeyLabel(keyPurpose);
        final PublicKey pubK = token.getCryptoToken().getPublicKey(alias);
        if (log.isTraceEnabled()) {
            log.trace(">getPublicKey (purpose): " + caid+", "+keyPurpose);
        }
        return pubK;
    }

}
