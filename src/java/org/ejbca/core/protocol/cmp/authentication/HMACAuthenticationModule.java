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

package org.ejbca.core.protocol.cmp.authentication;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.cmp.PKIMessage;
import org.bouncycastle.asn1.cmp.RevReqContent;
import org.bouncycastle.asn1.crmf.CertReqMessages;
import org.bouncycastle.asn1.crmf.CertTemplate;
import org.bouncycastle.asn1.x500.X500Name;
import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.certificates.ca.CAInfo;
import org.cesecore.certificates.ca.X509CAInfo;
import org.cesecore.certificates.endentity.EndEntityInformation;
import org.ejbca.config.CmpConfiguration;
import org.ejbca.core.ejb.ra.EndEntityAccessSession;
import org.ejbca.core.model.InternalEjbcaResources;
import org.ejbca.core.protocol.cmp.CmpPKIBodyConstants;
import org.ejbca.core.protocol.cmp.CmpPbeVerifyer;

/**
 * Checks the authentication of the PKIMessage.
 * 
 * In RA mode, the authenticity is checked through a shared secret specified either in 
 * the configuration file or in the CA.
 * 
 * In client mode, the authenticity is checked through the clear-text-password of the 
 * pre-registered endentity from the database. 
 * 
 * @version $Id$
 *
 */
public class HMACAuthenticationModule implements ICMPAuthenticationModule {

    private static final Logger LOG = Logger.getLogger(HMACAuthenticationModule.class);
    private static final InternalEjbcaResources INTRES = InternalEjbcaResources.getInstance();


    
    private AuthenticationToken admin;
    private EndEntityAccessSession eeAccessSession;
    
    private String globalSharedSecret;
    private CAInfo cainfo;
    private String password;
    private String confAlias;
    private CmpConfiguration cmpConfiguration;
    
    private CmpPbeVerifyer verifyer;
        
    public HMACAuthenticationModule(AuthenticationToken admin, String authParameter, String confAlias, CmpConfiguration cmpConfig, 
            CAInfo cainfo, EndEntityAccessSession eeSession) {
        this.globalSharedSecret = authParameter;
        this.confAlias = confAlias;
        this.cainfo = cainfo;
        this.cmpConfiguration = cmpConfig;
        
        this.admin = admin;
        this.eeAccessSession = eeSession;
        
        this.verifyer = null;
        this.password = null;
    }
    
    /**
     * Returns the name of this authentication module as String
     * 
     * @return the name of this authentication module as String
     */
    public String getName() {
        return CmpConfiguration.AUTHMODULE_HMAC;
    }
    
    @Override
    /**
     * Returns the password resulted from the verification process.
     * 
     * This password is set if verify() returns true.
     * 
     * @return The password as String. Null if the verification had failed.
     */
    public String getAuthenticationString() {
        return this.password;
    }
    
    public CmpPbeVerifyer getCmpPbeVerifyer() {
        return this.verifyer;
    }
    
    /**
     * Verifies that 'msg' is sent by a trusted source. 
     * 
     * In RA mode:
     *      - A globally configured shared secret for all CAs will be used to authenticate the message.
     *      - If the globally shared secret fails, the password set in the CA will be used to authenticate the message.
     *  In client mode, the clear-text password set in the pre-registered end entity in the database will be used to 
     *  authenticate the message. 
     * 
     * When successful, the authentication string will be set to the password that was successfully used in authenticating the message.
     * 
     * @param msg
     * @param username
     * @throws CmpAuthenticationException if the verification fails.
     */
    public void verifyOrExtract(final PKIMessage msg, final String username) throws CmpAuthenticationException {
        
        if(msg == null) {
            throw new CmpAuthenticationException("No PKIMessage was found");
        }
        
        if((msg.getProtection() == null) || (msg.getHeader().getProtectionAlg() == null)) {
            throw new CmpAuthenticationException("PKI Message is not athenticated properly. No HMAC protection was found.");
        }

        try {   
            verifyer = new CmpPbeVerifyer(msg);
        } catch(Exception e) {
            throw new CmpAuthenticationException("Could not create CmpPbeVerifyer. "+e.getMessage());
        }
        
        if(verifyer == null) {
            throw new CmpAuthenticationException("Could not create CmpPbeVerifyer Object");
        }
            
        if(this.cmpConfiguration.getRAMode(this.confAlias)) { //RA mode
            if(LOG.isDebugEnabled()) {
                LOG.debug("Verifying HMAC in RA mode");
            }
            
            // If we use a globally configured shared secret for all CAs we check it right away
            String authSecret = globalSharedSecret;

            if (globalSharedSecret != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Verifying message using Global Shared secret");
                }
                try {
                    if(verifyer.verify(authSecret)) {
                        this.password = authSecret;
                    } else {
                        String errmsg = INTRES.getLocalizedMessage("cmp.errorauthmessage", "Global auth secret");
                        LOG.info(errmsg); // info because this is something we should expect and we handle it
                        if (verifyer.getErrMsg() != null) {
                            errmsg = verifyer.getErrMsg();
                            LOG.info(errmsg);
                        }   
                    }
                } catch (InvalidKeyException e) {
                    String errmsg = e.getLocalizedMessage();
                    if(LOG.isDebugEnabled()) {
                        LOG.debug(errmsg, e);
                    }
                    throw new CmpAuthenticationException(errmsg);
                } catch (NoSuchAlgorithmException e) {
                    String errmsg = e.getLocalizedMessage();
                    if(LOG.isDebugEnabled()) {
                        LOG.debug(errmsg, e);
                    }
                    throw new CmpAuthenticationException(errmsg);
                } catch (NoSuchProviderException e) {
                    String errmsg = e.getLocalizedMessage();
                    if(LOG.isDebugEnabled()) {
                        LOG.debug(errmsg, e);
                    }
                    throw new CmpAuthenticationException(errmsg);
                }
            }

            // If password is null, then we failed verification using global shared secret
            if (this.password == null) {
                
                if (cainfo instanceof X509CAInfo) {
                    authSecret = ((X509CAInfo) cainfo).getCmpRaAuthSecret();
               
                    if (StringUtils.isNotEmpty(authSecret)) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Verify message using 'CMP RA Authentication Secret' from CA '"+cainfo.getName()+"'.");
                        }
                        try {
                            if(verifyer.verify(authSecret)) {
                                this.password = authSecret;
                            } else {
                                // info because this is something we should expect and we handle it
                                LOG.info(INTRES.getLocalizedMessage("cmp.errorauthmessage", "Auth secret for CA="+cainfo.getName()));
                                if (verifyer.getErrMsg() != null) {
                                    LOG.info(verifyer.getErrMsg());
                                }
                            }
                        } catch (InvalidKeyException e) {
                            String errmsg = INTRES.getLocalizedMessage("cmp.errorgeneral");
                            LOG.error(errmsg, e);
                            throw new CmpAuthenticationException(errmsg);
                        } catch (NoSuchAlgorithmException e) {
                            String errmsg = INTRES.getLocalizedMessage("cmp.errorgeneral");
                            LOG.error(errmsg, e);
                            throw new CmpAuthenticationException(errmsg);
                        } catch (NoSuchProviderException e) {
                            String errmsg = INTRES.getLocalizedMessage("cmp.errorgeneral");
                            LOG.error(errmsg, e);
                            throw new CmpAuthenticationException(errmsg);
                        }
                    } else {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("CMP password is null from CA '"+cainfo.getName()+"'.");
                        }
                    }
                }
            }
            
            // If password is still null, then we have failed verification with CA authentication secret too.
            if(password == null) {
                throw new CmpAuthenticationException("Failed to verify message using both Global Shared Secret and CMP RA Authentication Secret");
            }

        } else { //client mode
            if(LOG.isDebugEnabled()) {
                LOG.debug("Verifying HMAC in Client mode");
            }
            //If client mode, we try to get the pre-registered endentity from the DB, and if there is a 
            //clear text password we check HMAC using this password.
            EndEntityInformation userdata = null;
            String subjectDN = null;

            try {
                if (username != null) {
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("Searching for an end entity with username='" + username+"'.");
                    }
                    userdata = this.eeAccessSession.findUser(admin, username);
                } else {
                    // No username given, so we try to find from subject/issuerDN from the certificate request
                    final CertTemplate certTemp = getCertTemplate(msg);
                    subjectDN = certTemp.getSubject().toString();
                    
                    String issuerDN = null;
                    final X500Name issuer = certTemp.getIssuer();
                    if ((issuer != null) && (subjectDN != null)) {
                        issuerDN = issuer.toString();
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Searching for an end entity with SubjectDN='" + subjectDN + "' and isserDN='" + issuerDN + "'");
                        }
                        
                        List<EndEntityInformation> userdataList = eeAccessSession.findUserBySubjectAndIssuerDN(this.admin, subjectDN, issuerDN);
                        userdata = userdataList.get(0);
                        if (userdataList.size() > 1) {
                            LOG.warn("Multiple end entities with subject DN " + subjectDN + " and issuer DN" + issuerDN
                                    + " were found. This may lead to unexpected behavior.");
                        }
                    } else if (subjectDN != null) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Searching for an end entity with SubjectDN='" + subjectDN + "'.");
                        }
                        List<EndEntityInformation> userdataList = this.eeAccessSession.findUserBySubjectDN(admin, subjectDN);
                        if (userdataList.size() > 0) {
                            userdata = userdataList.get(0);
                        }
                        if (userdataList.size() > 1) {
                            LOG.warn("Multiple end entities with subject DN " + subjectDN + " were found. This may lead to unexpected behavior.");
                        }
                    }                    
                }
            } catch (AuthorizationDeniedException e) {
                LOG.info("No EndEntity with subjectDN '" + subjectDN + "' could be found, which is expected if the request had been send in Client mode.");
            }
            
            if(userdata != null) {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Comparing HMAC password authentication for user '"+userdata.getUsername()+"'.");
                }

                final String eepassword = userdata.getPassword();
                if(StringUtils.isNotEmpty(eepassword)) {
                    try {
                        if(verifyer.verify(eepassword)) {
                            this.password = eepassword;
                        } else {
                            String errmsg = INTRES.getLocalizedMessage("cmp.errorauthmessage", userdata.getUsername());
                            LOG.info(errmsg); // info because this is something we should expect and we handle it
                            if (verifyer.getErrMsg() != null) {
                                errmsg = verifyer.getErrMsg();
                                LOG.info(errmsg);
                            }
                            throw new CmpAuthenticationException(errmsg);
                        }
                    } catch (InvalidKeyException e) {
                        String errmsg = INTRES.getLocalizedMessage("cmp.errorgeneral");
                        LOG.error(errmsg, e);
                        throw new CmpAuthenticationException(errmsg);
                    } catch (NoSuchAlgorithmException e) {
                        String errmsg = INTRES.getLocalizedMessage("cmp.errorgeneral");
                        LOG.error(errmsg, e);
                        throw new CmpAuthenticationException(errmsg);
                    } catch (NoSuchProviderException e) {
                        String errmsg = INTRES.getLocalizedMessage("cmp.errorgeneral");
                        LOG.error(errmsg, e);
                        throw new CmpAuthenticationException(errmsg);
                    }
                } else {
                    throw new CmpAuthenticationException("No clear text password for user '"+userdata.getUsername()+"', not possible to check authentication.");
                }
            } else {
                throw new CmpAuthenticationException("End Entity with subjectDN '" + subjectDN +"' or username '" + username + "' was not found");
            }
        }
    }

    
    /**
     * Returns the certificate template specified in the request impeded in msg.
     * 
     * @param msg
     * @return the certificate template imbeded in msg. Null if no such template was found.
     */
    private CertTemplate getCertTemplate(final PKIMessage msg) {
        final int tagnr = msg.getBody().getType();
        if(tagnr == CmpPKIBodyConstants.INITIALIZATIONREQUEST 
                || tagnr==CmpPKIBodyConstants.CERTIFICATAIONREQUEST
                || tagnr==CmpPKIBodyConstants.KEYUPDATEREQUEST) {
            CertReqMessages reqmsgs = (CertReqMessages) msg.getBody().getContent();
            return reqmsgs.toCertReqMsgArray()[0].getCertReq().getCertTemplate();
        }
        if(tagnr==CmpPKIBodyConstants.REVOCATIONREQUEST) {
            RevReqContent rev  =(RevReqContent) msg.getBody().getContent();
            return rev.toRevDetailsArray()[0].getCertDetails();
        }
        return null;
    }

}
