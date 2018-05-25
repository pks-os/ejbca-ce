/*************************************************************************
 *                                                                       *
 *  EJBCA Community: The OpenSource Certificate Authority                *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/

package org.ejbca.core.ejb.rest;

import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.util.encoders.Hex;
import org.cesecore.authentication.tokens.AuthenticationSubject;
import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.certificates.ca.CADoesntExistsException;
import org.cesecore.certificates.ca.CAInfo;
import org.cesecore.certificates.ca.CaSessionLocal;
import org.cesecore.certificates.certificateprofile.CertificateProfileDoesNotExistException;
import org.cesecore.certificates.certificateprofile.CertificateProfileSessionLocal;
import org.cesecore.certificates.endentity.EndEntityConstants;
import org.cesecore.certificates.endentity.EndEntityInformation;
import org.cesecore.certificates.endentity.EndEntityType;
import org.cesecore.certificates.endentity.EndEntityTypes;
import org.cesecore.certificates.endentity.ExtendedInformation;
import org.cesecore.jndi.JndiConstants;
import org.cesecore.util.CertTools;
import org.ejbca.core.EjbcaException;
import org.ejbca.core.ejb.authentication.web.WebAuthenticationProviderSessionLocal;
import org.ejbca.core.ejb.ra.raadmin.EndEntityProfileSessionLocal;
import org.ejbca.core.model.InternalEjbcaResources;
import org.ejbca.core.model.approval.WaitingForApprovalException;
import org.ejbca.core.model.authorization.AccessRulesConstants;
import org.ejbca.core.model.era.RaMasterApiProxyBeanLocal;
import org.ejbca.core.model.ra.raadmin.EndEntityProfile;
import org.ejbca.core.model.ra.raadmin.EndEntityProfileNotFoundException;
import org.ejbca.core.protocol.rest.EnrollPkcs10CertificateRequest;


/**
 *
 * @version $Id$
 *
 */
@Stateless(mappedName = JndiConstants.APP_JNDI_PREFIX + "EjbcaRestHelperSessionRemote")
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class EjbcaRestHelperSessionBean implements EjbcaRestHelperSessionLocal, EjbcaRestHelperSessionRemote {
    
    private static final Logger log = Logger.getLogger(EjbcaRestHelperSessionBean.class);

    private static final InternalEjbcaResources intres = InternalEjbcaResources.getInstance();

    @EJB
    private WebAuthenticationProviderSessionLocal authenticationSession;
    @EJB
    private RaMasterApiProxyBeanLocal raMasterApiProxyBean;
    
    @EJB
    private EndEntityProfileSessionLocal endEntityProfileSessionBean;
    
    @EJB
    private CaSessionLocal caSessionBean;
    
    @EJB
    private CertificateProfileSessionLocal certificateProfileSessionBean;
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @Override
    public AuthenticationToken getAdmin(final boolean allowNonAdmins, X509Certificate cert) throws AuthorizationDeniedException {
        final Set<X509Certificate> credentials = new HashSet<>();
        credentials.add(cert);
        final AuthenticationSubject subject = new AuthenticationSubject(null, credentials);
        final AuthenticationToken admin = authenticationSession.authenticate(subject);

        if ((admin != null) && (!allowNonAdmins)) {
            if(!raMasterApiProxyBean.isAuthorizedNoLogging(admin, AccessRulesConstants.ROLE_ADMINISTRATOR)) {
                final String msg = intres.getLocalizedMessage("authorization.notauthorizedtoresource", AccessRulesConstants.ROLE_ADMINISTRATOR, null);
                throw new AuthorizationDeniedException(msg);
            }
        } else if (admin == null) {
            final String msg = intres.getLocalizedMessage("authentication.failed", "No admin authenticated for certificate with serialNumber " +
                    CertTools.getSerialNumber(cert) + " and issuerDN '" + CertTools.getIssuerDN(cert)+"'.");
            throw new AuthorizationDeniedException(msg);
        }
        return admin;
    }
    
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public byte[] createCertificateRest(final AuthenticationToken authenticationToken, EnrollPkcs10CertificateRequest enrollcertificateRequest) 
            throws AuthorizationDeniedException, EjbcaException, WaitingForApprovalException, IOException, EndEntityProfileNotFoundException, 
            CertificateProfileDoesNotExistException, CADoesntExistsException {
        
        EndEntityInformation endEntityInformation = convertToEndEntityInformation(authenticationToken, enrollcertificateRequest);
        addEndEntity(authenticationToken, endEntityInformation);
        endEntityInformation.getExtendedInformation().setCertificateRequest(CertTools.getCertificateRequestFromPem(enrollcertificateRequest.getCertificateRequest()).getEncoded());
        byte[] certificate = raMasterApiProxyBean.createCertificate(authenticationToken, endEntityInformation);
        return certificate;
    }
    
    public EndEntityInformation convertToEndEntityInformation(AuthenticationToken authenticationToken, EnrollPkcs10CertificateRequest enrollcertificateRequest) 
            throws AuthorizationDeniedException, EndEntityProfileNotFoundException, EjbcaException, CertificateProfileDoesNotExistException, CADoesntExistsException {
        
        EndEntityInformation endEntityInformation = new EndEntityInformation();
        ExtendedInformation extendedInformation = new ExtendedInformation();

        endEntityInformation.setExtendedInformation(extendedInformation);
        
        CAInfo caInfo = getCAInfo(enrollcertificateRequest.getCertificateAuthorityName(), authenticationToken);
        if (caInfo == null) {
            String errorMessage = "CA with name \"" + enrollcertificateRequest.getCertificateAuthorityName() + "\" doesn't exist";
            throw new CADoesntExistsException(errorMessage);
        }
        endEntityInformation.setCAId(caInfo.getCAId());
        
        int certificateProfileId = getCertificateProfileId(enrollcertificateRequest.getCertificateProfileName());
        if (certificateProfileId == 0) {
            String errorMessage = "Certificate profile with name \"" + enrollcertificateRequest.getCertificateProfileName() + "\" doesn't exist";
            throw new CertificateProfileDoesNotExistException(errorMessage);
        }
        endEntityInformation.setCertificateProfileId(certificateProfileId);
        
        Integer endEntityProfileId = getEndEntityProfileId(enrollcertificateRequest.getEndEntityProfileName());
        if (endEntityProfileId == null) {
            String errorMessage = "End entity profile with name \"" + enrollcertificateRequest.getEndEntityProfileName() + "\" doesn't exist";
            throw new EndEntityProfileNotFoundException(errorMessage);
        }
        endEntityInformation.setEndEntityProfileId(endEntityProfileId);
        
        PKCS10CertificationRequest pkcs10CertificateRequest = CertTools.getCertificateRequestFromPem(enrollcertificateRequest.getCertificateRequest());
        if (pkcs10CertificateRequest == null) {
            throw new EjbcaException("Invalid certificate request");
        }
        
        String altName = getSubjectAltName(pkcs10CertificateRequest);
        endEntityInformation.setSubjectAltName(altName);
        
        String subjectDn = getSubjectDn(pkcs10CertificateRequest);
        endEntityInformation.setDN(subjectDn);
        
        endEntityInformation.setCardNumber("");
        endEntityInformation.setHardTokenIssuerId(0);
        endEntityInformation.setStatus(EndEntityConstants.STATUS_NEW);

        Date timecreated = new Date();
        endEntityInformation.setTimeCreated(timecreated);
        endEntityInformation.setTimeModified(timecreated);
        
        EndEntityProfile endEntityProfile = getEndEntityProfile(endEntityProfileId);
        
        endEntityInformation.setType(new EndEntityType(EndEntityTypes.ENDUSER));
        boolean isSendNotificationDefaultInProfile = EndEntityProfile.TRUE.equals(endEntityProfile.getValue(EndEntityProfile.SENDNOTIFICATION, 0));
        endEntityInformation.setSendNotification(isSendNotificationDefaultInProfile && !endEntityInformation.getSendNotification());

        endEntityInformation.setPrintUserData(false);
        endEntityInformation.setTokenType(EndEntityConstants.TOKEN_USERGEN);
        
        endEntityInformation.setUsername(enrollcertificateRequest.getUsername());
        
        // Fill end-entity password
        final byte[] randomData = new byte[16];
        final Random random = new SecureRandom();
        
        if (endEntityProfile.useAutoGeneratedPasswd()) {
            // If auto-generated passwords are used, this is set on the CA side when adding or changing the EE as long as the password is null
            endEntityInformation.setPassword(null);
        } else if (StringUtils.isEmpty(enrollcertificateRequest.getPassword())) {
            // If not needed just use some random data
            random.nextBytes(randomData);
            endEntityInformation.setPassword(new String(Hex.encode(CertTools.generateSHA256Fingerprint(randomData))));
        } else {
            endEntityInformation.setPassword(enrollcertificateRequest.getPassword());
        }
        
        return endEntityInformation;
    }
    
    protected String getSubjectAltName(PKCS10CertificationRequest pkcs10CertificateRequest) {
        String altName = null;
        final Extension subjectAlternativeNameExtension = CertTools.getExtension(pkcs10CertificateRequest, Extension.subjectAlternativeName.getId());
        if (subjectAlternativeNameExtension != null) {
            altName = CertTools.getAltNameStringFromExtension(subjectAlternativeNameExtension);
        }
        return altName;
    }


    protected String getSubjectDn(PKCS10CertificationRequest pkcs10CertificateRequest) {
        String subject = "";
        if (pkcs10CertificateRequest.getSubject() != null) {
            subject = pkcs10CertificateRequest.getSubject().toString();
        }
        return subject;
    }

    private CAInfo getCAInfo(String certificateAuthorityName, AuthenticationToken authenticationToken) throws AuthorizationDeniedException {
        CAInfo caInfo = caSessionBean.getCAInfo(authenticationToken, certificateAuthorityName);
        return caInfo;
    }

    public Integer getEndEntityProfileId(String endEntityProfileName) {
        try {
            int endEntityProfileId = endEntityProfileSessionBean.getEndEntityProfileId(endEntityProfileName);
            return endEntityProfileId;
        } catch (EndEntityProfileNotFoundException e) {
            return null;
        }
    }
    
    public EndEntityProfile getEndEntityProfile(int endEntityProfileId) {
        EndEntityProfile endEntityProfile = endEntityProfileSessionBean.getEndEntityProfile(endEntityProfileId);
        return endEntityProfile;
    }

    private int getCertificateProfileId(String certificateProfileName) {
        int certificateProfileId = certificateProfileSessionBean.getCertificateProfileId(certificateProfileName);
        return certificateProfileId;
    }
    
    private void addEndEntity(AuthenticationToken authenticationToken, EndEntityInformation endEntityInformation)
            throws AuthorizationDeniedException, EjbcaException, WaitingForApprovalException {
        if (raMasterApiProxyBean.addUser(authenticationToken, endEntityInformation, false)) {
            log.info("End entity with username " + endEntityInformation.getUsername() + " has been successfully added by client " + authenticationToken);
        } else {
            String errorMessage = "Problem with adding end entity with username " + endEntityInformation.getUsername();
            throw new EjbcaException(errorMessage);
        }
    }
}