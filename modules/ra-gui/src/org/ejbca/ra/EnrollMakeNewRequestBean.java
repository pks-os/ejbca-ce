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
package org.ejbca.ra;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;
import javax.faces.validator.ValidatorException;

import org.apache.log4j.Logger;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.certificates.ca.CAInfo;
import org.cesecore.certificates.ca.X509CAInfo;
import org.cesecore.certificates.certificateprofile.CertificateProfile;
import org.cesecore.certificates.endentity.EndEntityConstants;
import org.cesecore.certificates.endentity.EndEntityInformation;
import org.cesecore.certificates.endentity.EndEntityType;
import org.cesecore.certificates.endentity.EndEntityTypes;
import org.cesecore.certificates.endentity.ExtendedInformation;
import org.cesecore.certificates.util.AlgorithmConstants;
import org.cesecore.certificates.util.AlgorithmTools;
import org.cesecore.certificates.util.DnComponents;
import org.cesecore.config.CesecoreConfiguration;
import org.cesecore.util.Base64;
import org.cesecore.util.CeSecoreNameStyle;
import org.cesecore.util.CertTools;
import org.cesecore.util.PrintableStringNameStyle;
import org.cesecore.util.StringTools;
import org.ejbca.core.ejb.ra.EndEntityExistsException;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.approval.WaitingForApprovalException;
import org.ejbca.core.model.era.IdNameHashMap;
import org.ejbca.core.model.era.RaMasterApiProxyBeanLocal;
import org.ejbca.core.model.era.Tuple;
import org.ejbca.core.model.ra.raadmin.EndEntityProfile;

/**
 * Managed bean that backs up the enrollingmakenewrequest.xhtml page
 * 
 * @version $Id$
 */
@ManagedBean
@ViewScoped
public class EnrollMakeNewRequestBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(EnrollMakeNewRequestBean.class);
    private static final String PEM_CSR_BEGIN = "-----BEGIN CERTIFICATE REQUEST-----";
    private static final String PEM_CSR_END = "-----END CERTIFICATE REQUEST-----";

    @EJB
    private RaMasterApiProxyBeanLocal raMasterApiProxyBean;

    @ManagedProperty(value = "#{raAuthenticationBean}")
    private RaAuthenticationBean raAuthenticationBean;

    public void setRaAuthenticationBean(final RaAuthenticationBean raAuthenticationBean) {
        this.raAuthenticationBean = raAuthenticationBean;
    }

    @ManagedProperty(value = "#{raLocaleBean}")
    private RaLocaleBean raLocaleBean;

    public void setRaLocaleBean(final RaLocaleBean raLocaleBean) {
        this.raLocaleBean = raLocaleBean;
    }

    public enum TokenDownloadType {
        PEM(1), PEM_FULL_CHAIN(2), PKCS7(3), P12(4), JKS(5), DER(6);
        private int value;

        private TokenDownloadType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
    
    //1. Select Request Template
    private boolean selectRequestTemplateRendered = true;
    private IdNameHashMap<EndEntityProfile> authorizedEndEntityProfiles = new IdNameHashMap<EndEntityProfile>();
    private IdNameHashMap<CertificateProfile> authorizedCertificateProfiles = new IdNameHashMap<>();
    private IdNameHashMap<CAInfo> authorizedCAInfos = new IdNameHashMap<CAInfo>();
    private String selectedEndEntityProfile;
    private boolean endEntityProfileChanged;
    private Map<String, String> availableEndEntityProfiles = new HashMap<String, String>();

    private Map<String, String> availableCertificateProfiles = new HashMap<String, String>();
    private String selectedCertificateProfile;
    private boolean certificateProfileChanged;

    private Map<String, String> availableCertificateAuthorities = new HashMap<String, String>();
    private String selectedCertificateAuthority;
    private boolean certificateAuthorityChanged;

    public enum KeyPairGeneration {
        ON_SERVER,
        PROVIDED_BY_USER;
    }
    private KeyPairGeneration selectedKeyPairGeneration;
    private boolean keyPairGenerationChanged;

    //2. Select Key Algorithm / Upload CSR
    private boolean selectKeyAlgorithmRendered;
    private boolean uploadCsrRendered;
    private Map<String, String> availableAlgorithms = new TreeMap<String, String>();
    private String selectedAlgorithm; //GENERATED ON SERVER
    private String algorithmFromCsr; //PROVIDED BY USER
    private boolean algorithmChanged;
    private String certificateRequest;

    //3. Request Info
    private boolean provideRequestInfoRendered;
    private SubjectDn subjectDn;
    private SubjectAlternativeName subjectAlternativeName;
    private SubjectDirectoryAttributes subjectDirectoryAttributes;

    //4. Provide request metadata
    private boolean provideRequestMetadataRendered;
    private EndEntityInformation endEntityInformation;
    private String confirmPassword;
    private boolean downloadCredentialsChanged;
    
    //8. Request data
    private int requestId;
    
    //9. Certificate preview
    private boolean confirmRequestRendered;
    private RaRequestPreview requestPreview;

    @PostConstruct
    private void postContruct() {
        initAll();
    }

    public void initAll() {
        initAuthorizedEndEntityProfiles();
        if (availableEndEntityProfiles.size() == 1) {
            setSelectedEndEntityProfile(availableEndEntityProfiles.keySet().iterator().next());
            selectEndEntityProfile();
        }
    }

    //-----------------------------------------------------------
    //All init* methods should contain ONLY application logic 

    public void initAuthorizedEndEntityProfiles() {
        setAuthorizedEndEntityProfiles(raMasterApiProxyBean.getAuthorizedEndEntityProfiles(raAuthenticationBean.getAuthenticationToken()));
        setAuthorizedCertificateProfiles(raMasterApiProxyBean.getAuthorizedCertificateProfiles(raAuthenticationBean.getAuthenticationToken()));
        setAuthorizedCAInfos(raMasterApiProxyBean.getAuthorizedCAInfos(raAuthenticationBean.getAuthenticationToken()));

        for (Tuple<EndEntityProfile> tuple : authorizedEndEntityProfiles.values()) {
            availableEndEntityProfiles.put(tuple.getName(), tuple.getName());
        }
    }

    private void initAvailableCertificateProfiles() {
        EndEntityProfile endEntityProfile = getEndEntityProfile();
        if (endEntityProfile == null) {
            return;
        }
        String[] availableCertificateProfileIds = endEntityProfile.getValue(EndEntityProfile.AVAILCERTPROFILES, 0).split(EndEntityProfile.SPLITCHAR);
        for (String id : availableCertificateProfileIds) {
            Tuple<CertificateProfile> tuple = authorizedCertificateProfiles.get(Integer.parseInt(id));
            if (tuple != null) {
                String defaultCertProfileId = endEntityProfile.getValue(EndEntityProfile.DEFAULTCERTPROFILE, 0);
                if (id.equalsIgnoreCase(defaultCertProfileId)) {
                    availableCertificateProfiles.put(tuple.getName(), tuple.getName() + " (default)");
                } else {
                    availableCertificateProfiles.put(tuple.getName(), tuple.getName());
                }
            }
        }
    }

    private void initAvailableCertificateAuthorities() {
        //Get all available CAs from the selected EEP
        EndEntityProfile endEntityProfile = getEndEntityProfile();
        if (endEntityProfile == null) {
            return;
        }
        String[] availableCAsFromEEPArray = endEntityProfile.getValue(EndEntityProfile.AVAILCAS, 0).split(EndEntityProfile.SPLITCHAR);
        boolean anyCAAvailableFromEEP = availableCAsFromEEPArray.length == 1 && availableCAsFromEEPArray[0].equalsIgnoreCase(SecConst.ALLCAS + "");

        //Get all available CAs from the selected CP
        CertificateProfile certificateProfile = getCertificateProfile();
        if (certificateProfile == null) {
            return;
        }
        List<Integer> availableCAsFromCP = certificateProfile.getAvailableCAs();
        boolean anyCAAvailableFromCP = availableCAsFromCP.size() == 1 && availableCAsFromCP.iterator().next() == CertificateProfile.ANYCA;

        //Intersect both with authorized CAs
        for (Tuple<CAInfo> tuple : authorizedCAInfos.values()) {
            if ((anyCAAvailableFromEEP || Arrays.asList(availableCAsFromEEPArray).contains(tuple.getId() + ""))
                    && (anyCAAvailableFromCP || availableCAsFromCP.contains(tuple.getId()))) {
                String defaultCAId = endEntityProfile.getValue(EndEntityProfile.DEFAULTCA, 0);
                if (!defaultCAId.isEmpty() && tuple.getId() == Integer.parseInt(defaultCAId)) {
                    availableCertificateAuthorities.put(tuple.getName(), tuple.getName() + " (default)");
                } else {
                    availableCertificateAuthorities.put(tuple.getName(), tuple.getName());
                }
            }
        }
    }

    private void initAvailableAlgorithms() {
        CertificateProfile certificateProfile = getCertificateProfile();
        final List<String> availableKeyAlgorithms = certificateProfile.getAvailableKeyAlgorithmsAsList();
        final List<Integer> availableBitLengths = certificateProfile.getAvailableBitLengthsAsList();
        if (availableKeyAlgorithms.contains(AlgorithmConstants.KEYALGORITHM_DSA)) {
            for (final int availableBitLength : availableBitLengths) {
                if (availableBitLength == 1024) {
                    availableAlgorithms.put(AlgorithmConstants.KEYALGORITHM_DSA + "_" + availableBitLength,
                            AlgorithmConstants.KEYALGORITHM_DSA + " " + availableBitLength + " bits");
                }
            }
        }
        if (availableKeyAlgorithms.contains(AlgorithmConstants.KEYALGORITHM_RSA)) {
            for (final int availableBitLength : availableBitLengths) {
                if (availableBitLength >= 1024) {
                    availableAlgorithms.put(AlgorithmConstants.KEYALGORITHM_RSA + "_" + availableBitLength,
                            AlgorithmConstants.KEYALGORITHM_RSA + " " + availableBitLength + " bits");
                }
            }
        }
        if (availableKeyAlgorithms.contains(AlgorithmConstants.KEYALGORITHM_ECDSA)) {
            final Set<String> ecChoices = new HashSet<>();
            final Map<String, List<String>> namedEcCurvesMap = AlgorithmTools.getNamedEcCurvesMap(false);
            if (certificateProfile.getAvailableEcCurvesAsList().contains(CertificateProfile.ANY_EC_CURVE)) {
                final String[] keys = namedEcCurvesMap.keySet().toArray(new String[namedEcCurvesMap.size()]);
                for (final String ecNamedCurve : keys) {
                    if (CertificateProfile.ANY_EC_CURVE.equals(ecNamedCurve)) {
                        continue;
                    }
                    final int bitLength = AlgorithmTools.getNamedEcCurveBitLength(ecNamedCurve);
                    if (availableBitLengths.contains(Integer.valueOf(bitLength))) {
                        ecChoices.add(ecNamedCurve);
                    }
                }
            }
            ecChoices.addAll(certificateProfile.getAvailableEcCurvesAsList());
            ecChoices.remove(CertificateProfile.ANY_EC_CURVE);
            final List<String> ecChoicesList = new ArrayList<>(ecChoices);
            Collections.sort(ecChoicesList);
            for (final String ecNamedCurve : ecChoicesList) {
                availableAlgorithms.put(AlgorithmConstants.KEYALGORITHM_ECDSA + "_" + ecNamedCurve, AlgorithmConstants.KEYALGORITHM_ECDSA + " "
                        + StringTools.getAsStringWithSeparator(" / ", namedEcCurvesMap.get(ecNamedCurve)));
            }
        }
        for (final String algName : CesecoreConfiguration.getExtraAlgs()) {
            if (availableKeyAlgorithms.contains(CesecoreConfiguration.getExtraAlgTitle(algName))) {
                for (final String subAlg : CesecoreConfiguration.getExtraAlgSubAlgs(algName)) {
                    final String name = CesecoreConfiguration.getExtraAlgSubAlgName(algName, subAlg);
                    final int bitLength = AlgorithmTools.getNamedEcCurveBitLength(name);
                    if (availableBitLengths.contains(Integer.valueOf(bitLength))) {
                        availableAlgorithms.put(CesecoreConfiguration.getExtraAlgTitle(algName) + "_" + name,
                                CesecoreConfiguration.getExtraAlgSubAlgTitle(algName, subAlg));
                    } else {
                        if (log.isTraceEnabled()) {
                            log.trace("Excluding " + name + " from enrollment options since bit length " + bitLength + " is not available.");
                        }
                    }
                }
            }
        }
    }

    private void initCsrUpload() {
        certificateRequest = PEM_CSR_BEGIN + "\n...base 64 encoded request...\n" + PEM_CSR_END;
    }

    private void initCertificateData() {
        EndEntityProfile endEntityProfile = getEndEntityProfile();
        if (endEntityProfile == null) {
            return;
        }
        
        if(subjectDn == null){
            subjectDn = new SubjectDn(endEntityProfile);
            final X509CAInfo x509cainfo = (X509CAInfo) getCAInfo();
            subjectDn.setLdapOrder(x509cainfo.getUseLdapDnOrder() && getCertificateProfile().getUseLdapDnOrder());
            subjectDn.setNameStyle(x509cainfo.getUsePrintableStringSubjectDN() ? PrintableStringNameStyle.INSTANCE : CeSecoreNameStyle.INSTANCE);
            subjectAlternativeName = new SubjectAlternativeName(endEntityProfile);
            subjectDirectoryAttributes = new SubjectDirectoryAttributes(endEntityProfile);
        }
        
        //If PROVIDED BY USER key generation is selected, try fill Subject DN fields from CSR (Overwrite the fields set by previous CSR upload if any)
        if (selectedKeyPairGeneration != null && KeyPairGeneration.PROVIDED_BY_USER.equals(selectedKeyPairGeneration)) {
            PKCS10CertificationRequest pkcs10CertificateRequest = CertTools.getCertificateRequestFromPem(certificateRequest); //pkcs10CertificateRequest will not be null at this point
            List<String> subjectDnFieldsFromParsedCsr = CertTools.getX500NameComponents(pkcs10CertificateRequest.getSubject().toString());
            bothLoops: for (String subjectDnField : subjectDnFieldsFromParsedCsr) {
                if(log.isDebugEnabled()){
                    log.debug("Parsing the subject DN field '" + subjectDnField + "'...");
                }
                String[] nameValue = subjectDnField.split("=");
                if (nameValue != null && nameValue.length == 2) {
                    Integer dnId = DnComponents.getDnIdFromDnName(nameValue[0]);
                    if (dnId != null) {
                        String profileName = DnComponents.dnIdToProfileName(dnId);
                        if (profileName != null) {
                            //In the case of multiple fields (etc. two CNs), find the first one with an empty value
                            for(EndEntityProfile.FieldInstance fieldInstance : subjectDn.getFieldInstancesMap().get(profileName).values()){
                                if(fieldInstance.isModifiable()){
                                    fieldInstance.setValue(nameValue[1]);
                                    subjectDn.getFieldInstancesMap().get(profileName).put(fieldInstance.getNumber(), fieldInstance);
                                    if (log.isDebugEnabled()) {
                                        log.debug(raLocaleBean.getMessage("enroll_subject_dn_field_successfully_parsed_from_csr", subjectDnField));
                                    }
                                    continue bothLoops;
                                }
                            }
                        }
                    }
                }
                log.info(raLocaleBean.getMessage("enroll_unparsable_subject_dn_field_from_csr", subjectDnField));
            }
            subjectDn.update();
        }
    }
   
    private void initEndEntityInformation() {
        endEntityInformation = new EndEntityInformation();
    }
    
    private void updateRequestPreview(){
        
        requestPreview = new RaRequestPreview();
        requestPreview.updateSubjectDn(subjectDn);
        requestPreview.updateSubjectAlternativeName(subjectAlternativeName);
        requestPreview.updateSubjectDirectoryAttributes(subjectDirectoryAttributes);
        requestPreview.setPublicKeyAlgorithm(getAlgorithm());
        requestPreview.updateCA(getCAInfo());
    }


    //-----------------------------------------------------------------------------------------------
    // Helpers and get*Rendered() methods
    
    public boolean isUsernameRendered(){
        return !getEndEntityProfile().useAutoGeneratedPasswd();
    }

    public boolean isPasswordRendered() {
        return selectedKeyPairGeneration != null && KeyPairGeneration.ON_SERVER.equals(selectedKeyPairGeneration) && !getEndEntityProfile().useAutoGeneratedPasswd();
    }

    public boolean isGenerateJksButtonRendered() {
        EndEntityProfile endEntityProfile = getEndEntityProfile();
        if (endEntityProfile == null) {
            return false;
        }
        String availableKeyStores = endEntityProfile.getValue(EndEntityProfile.AVAILKEYSTORE, 0);
        return availableKeyStores != null && availableKeyStores.contains(SecConst.TOKEN_SOFT_JKS + "")
                && selectedKeyPairGeneration != null && KeyPairGeneration.ON_SERVER.equals(selectedKeyPairGeneration)
                && !isApprovalRequired();
    }

    public boolean isGenerateP12ButtonRendered() {
        EndEntityProfile endEntityProfile = getEndEntityProfile();
        if (endEntityProfile == null) {
            return false;
        }
        String availableKeyStores = endEntityProfile.getValue(EndEntityProfile.AVAILKEYSTORE, 0);
        return availableKeyStores != null && availableKeyStores.contains(SecConst.TOKEN_SOFT_P12 + "")
                && selectedKeyPairGeneration != null && KeyPairGeneration.ON_SERVER.equals(selectedKeyPairGeneration)
                && !isApprovalRequired();
    }

    public boolean isGenerateFromCsrButtonRendered() {
        EndEntityProfile endEntityProfile = getEndEntityProfile();
        if (endEntityProfile == null) {
            return false;
        }
        String availableKeyStores = endEntityProfile.getValue(EndEntityProfile.AVAILKEYSTORE, 0);
        return availableKeyStores != null && availableKeyStores.contains(EndEntityConstants.TOKEN_USERGEN + "")
                && selectedKeyPairGeneration != null && KeyPairGeneration.PROVIDED_BY_USER.equals(selectedKeyPairGeneration)
                && !isApprovalRequired();
    }
    
    public boolean isConfirmRequestButtonRendered(){
        return isApprovalRequired();
    }
    
    private boolean isApprovalRequired(){
        return raMasterApiProxyBean.getApprovalProfileForAction(1, getCAInfo(), getCertificateProfile()) != null;
    }
    
    public boolean getUpdateRequestPreviewButtonRendered(){
        return getEndEntityInformation() != null;
    }
    
    //-----------------------------------------------------------------------------------------------
    //All reset* methods should be able to clear/reset states that have changed during init* methods.
    //Always make sure that reset methods are properly chained

    //Invoked by commandButton id="resetButton"
    public final String reset() {
        //Invalidate view tree by redirecting to the same page
        String viewId = FacesContext.getCurrentInstance().getViewRoot().getViewId();
        return viewId+"?faces-redirect=true";
    }

    private final void resetCertificateProfile() {
        availableCertificateProfiles.clear();
        selectedCertificateProfile = null;
        certificateProfileChanged = false;

        resetCertificateAuthority();
    }

    private final void resetCertificateAuthority() {
        availableCertificateAuthorities.clear();
        selectedCertificateAuthority = null;
        certificateAuthorityChanged = false;

        resetKeyPairGeneration();
    }

    private final void resetKeyPairGeneration() {
        selectedKeyPairGeneration = null;
        keyPairGenerationChanged = false;

        resetAlgorithmCsrUpload();
    }

    private final void resetAlgorithmCsrUpload() {
        availableAlgorithms.clear();
        selectedAlgorithm = null;
        algorithmChanged = false;
        certificateRequest = null;
        uploadCsrRendered = false;
        selectKeyAlgorithmRendered = false;

        resetRequestInfo();
    }

    private final void resetRequestInfo() {
        subjectDn = null;
        subjectAlternativeName = null;
        subjectDirectoryAttributes = null;
        setProvideRequestInfoRendered(false);

        resetRequestMetadata();
    }

    private final void resetRequestMetadata() {
        endEntityInformation = null;
        setProvideRequestMetadataRendered(false);
        setConfirmRequestRendered(false);
        setRequestId(0);
    }

    //-----------------------------------------------------------------------------------------------
    //Action methods

    private final void selectEndEntityProfile() {
        setEndEntityProfileChanged(false);

        resetCertificateProfile();
        initAvailableCertificateProfiles();
        if (availableCertificateProfiles.size() == 1) {
            setSelectedCertificateProfile(availableCertificateProfiles.keySet().iterator().next());
            selectCertificateProfile();
        }
    }

    private final void selectCertificateProfile() {
        setCertificateProfileChanged(false);

        resetCertificateAuthority();
        initAvailableCertificateAuthorities();
        if (availableCertificateAuthorities.size() == 1) {
            setSelectedCertificateAuthority(availableCertificateAuthorities.keySet().iterator().next());
            selectCertificateAuthority();
        }
    }

    private final void selectCertificateAuthority() {
        setCertificateAuthorityChanged(false);

        resetKeyPairGeneration();
        if (getAvailableKeyPairGenerations().size() == 1) {
            selectedKeyPairGeneration = getAvailableKeyPairGenerations().get(0);
            selectKeyPairGeneration();
        }
    }
    
    public final void applyRequestTemplate(){
        if (endEntityProfileChanged) {
            selectEndEntityProfile();
        } else if (certificateProfileChanged) {
            selectCertificateProfile();
        } else if (certificateAuthorityChanged) {
            selectCertificateAuthority();
        } else if (keyPairGenerationChanged) {
            selectKeyPairGeneration();
        }
    }

    private final void selectKeyPairGeneration() {
        setKeyPairGenerationChanged(false);

        resetAlgorithmCsrUpload();
        if (KeyPairGeneration.ON_SERVER.equals(selectedKeyPairGeneration)) {
            selectKeyAlgorithmRendered = true;
            initAvailableAlgorithms();
            
            setProvideRequestInfoRendered(true);
            initCertificateData();
            
            setProvideRequestMetadataRendered(true);
            setConfirmRequestRendered(true);
            initEndEntityInformation();
            updateRequestPreview();
        } else if (KeyPairGeneration.PROVIDED_BY_USER.equals(selectedKeyPairGeneration)) {
            uploadCsrRendered = true;
            initCsrUpload();
        }
    }

    private final void selectAlgorithm() {
        setAlgorithmChanged(false);
    }
    
    public final void uploadCsr() {
        setProvideRequestInfoRendered(true);
        initCertificateData();
        
        setProvideRequestMetadataRendered(true);
        setConfirmRequestRendered(true);
        initEndEntityInformation();
        updateRequestPreview();
    }

    public final void addEndEntityAndGenerateCertificeDer() {
        byte[] token = addEndEntityAndGenerateToken(EndEntityConstants.TOKEN_USERGEN, "DER", TokenDownloadType.DER);
        downloadToken(token, "application/octet-stream", ".der");
    }

    public final void addEndEntityAndGenerateCertificePksc7() {
        byte[] token = addEndEntityAndGenerateToken(EndEntityConstants.TOKEN_USERGEN, "PKCS#7", TokenDownloadType.PKCS7);
        downloadToken(token, "application/octet-stream", ".p7b");
    }

    public final void addEndEntityAndGenerateCertificePemFullChain() {
        byte[] token = addEndEntityAndGenerateToken(EndEntityConstants.TOKEN_USERGEN, "PEM", TokenDownloadType.PEM_FULL_CHAIN);
        downloadToken(token, "application/octet-stream", ".pem");
    }

    public final void addEndEntityAndGenerateCertificePem() {
        byte[] token = addEndEntityAndGenerateToken(EndEntityConstants.TOKEN_USERGEN, "PEM", TokenDownloadType.PEM);
        downloadToken(token, "application/octet-stream", ".pem");
    }

    public final void addEndEntityAndGenerateP12() {
        byte[] token = addEndEntityAndGenerateToken(EndEntityConstants.TOKEN_SOFT_P12, "PKCS#12", null);
        downloadToken(token, "application/x-pkcs12", ".p12");
    }

    public final void addEndEntityAndGenerateJks() {
        byte[] token = addEndEntityAndGenerateToken(EndEntityConstants.TOKEN_SOFT_JKS, "JKS", null);
        downloadToken(token, "application/octet-stream", ".jks");
    }
    
    public final void updateRequestDataToPreview(){
        subjectDn.update();
        subjectAlternativeName.update();
        subjectDirectoryAttributes.updateValue();
        
        updateRequestPreview();
    }

    /**
     * Adds end entity and creates its token that will be downloaded. This method is responsible for deleting the end entity if something goes wrong with token creation.
     * @param tokenType the type of the token that will be created (one of: TOKEN_USERGEN, TOKEN_SOFT_P12, TOKEN_SOFT_JKS from EndEntityConstants)
     * @param tokenName the name of the token. It will be used only in messages and logs
     * @param tokenDownloadType the download type/format of the token. This is used only with TOKEN_USERGEN since this is the only one that have different formats: PEM, DER,...)
     * @return generated token as byte array
     */
    private final byte[] addEndEntityAndGenerateToken(int tokenType, String tokenName, TokenDownloadType tokenDownloadType) {
        //Update the EndEntityInformation data
        subjectDn.update();
        subjectAlternativeName.update();
        subjectDirectoryAttributes.updateValue();
        
        //Fill End Entity information
        endEntityInformation.setCAId(getCAInfo().getCAId());
        endEntityInformation.setCardNumber(""); //TODO Card Number
        endEntityInformation.setCertificateProfileId(authorizedCertificateProfiles.get(selectedCertificateProfile).getId());
        endEntityInformation.setDN(subjectDn.toString());
        endEntityInformation.setEndEntityProfileId(authorizedEndEntityProfiles.get(selectedEndEntityProfile).getId());
        endEntityInformation.setExtendedinformation(new ExtendedInformation());//TODO don't know anything about it...
        endEntityInformation.setHardTokenIssuerId(0); //TODO not sure....
        endEntityInformation.setKeyRecoverable(false); //TODO not sure...
        endEntityInformation.setPrintUserData(false); // TODO not sure...
        endEntityInformation.setSendNotification(getEndEntityProfile().isRequired(EndEntityProfile.SENDNOTIFICATION, 0)
                && getEndEntityProfile().getValue(EndEntityProfile.SENDNOTIFICATION, 0).equals(EndEntityProfile.TRUE)
                && !endEntityInformation.getSendNotification());
        endEntityInformation.setStatus(EndEntityConstants.STATUS_NEW);
        endEntityInformation.setSubjectAltName(subjectAlternativeName.toString());
        endEntityInformation.setTimeCreated(new Date());
        endEntityInformation.setTimeModified(new Date());
        endEntityInformation.setType(new EndEntityType(EndEntityTypes.ENDUSER));
        //TODO how to set subject directory attributes?
        endEntityInformation.setTokenType(tokenType);

        //Fill end-entity information (Username and Password)
        if(endEntityInformation.getUsername() == null || endEntityInformation.getUsername().isEmpty()){
            Map<Integer, EndEntityProfile.FieldInstance> commonNameFieldInstances = subjectDn.getFieldInstancesMap().get(DnComponents.COMMONNAME);
            for(EndEntityProfile.FieldInstance commonNameFieldInstance : commonNameFieldInstances.values()){
                if(commonNameFieldInstance.isRequired()){
                    MessageDigest md;
                    try {
                        md = MessageDigest.getInstance("SHA-256");
                        byte[] random = new byte[20];
                        new Random().nextBytes(random);
                        md.update(random);
                        byte[] digest = md.digest();
                        String encodedRandomSha256 = new String(Base64.encode(digest));
                        endEntityInformation.setUsername(encodedRandomSha256);
                        if(endEntityInformation.getPassword() == null){
                            md.update(random);
                            digest = md.digest();
                            encodedRandomSha256 = new String(Base64.encode(digest));
                            endEntityInformation.setPassword(encodedRandomSha256);
                        }
                        
                    } catch (NoSuchAlgorithmException e) {
                    }
                    break;
                }
            }
        }

        //Fill end-entity information (KeyStoreAlgorithm* or CertificateRequest)
        if (KeyPairGeneration.ON_SERVER.equals(selectedKeyPairGeneration)) {
            final String[] tokenKeySpecSplit = selectedAlgorithm.split("_");
            endEntityInformation.getExtendedinformation().setKeyStoreAlgorithm(tokenKeySpecSplit[0]);
            endEntityInformation.getExtendedinformation().setKeyStoreAlgorithmLength(tokenKeySpecSplit[1]);
        } else if (KeyPairGeneration.PROVIDED_BY_USER.equals(selectedKeyPairGeneration)) {
            try {
                endEntityInformation.getExtendedinformation().setCertificateRequest(CertTools.getCertificateRequestFromPem(certificateRequest).getEncoded());
            } catch (IOException e) {
                raLocaleBean.addMessageError("enroll_invalid_certificate_request");
                return null;
            }
        }
        
        //Add end-entity
        try {
            if (raMasterApiProxyBean.addUser(raAuthenticationBean.getAuthenticationToken(), endEntityInformation, /*clearpwd=*/false)) {
                log.info(raLocaleBean.getMessage("enroll_end_entity_has_been_successfully_added", endEntityInformation.getUsername()));
            } else {
                raLocaleBean.addMessageInfo("enroll_end_entity_could_not_be_added", endEntityInformation.getUsername());
                log.error(raLocaleBean.getMessage("enroll_end_entity_could_not_be_added", endEntityInformation.getUsername()));
                return null;
            }
        } catch (EndEntityExistsException e) {
            raLocaleBean.addMessageInfo("enroll_username_already_exists", endEntityInformation.getUsername(), e.getMessage());
            log.error(raLocaleBean.getMessage("enroll_username_already_exists", endEntityInformation.getUsername(), e.getMessage()), e);
            return null;
        } catch (AuthorizationDeniedException e) {
            raLocaleBean.addMessageInfo("enroll_unauthorized_operation", endEntityInformation.getUsername(), e.getMessage());
            log.error(raLocaleBean.getMessage("enroll_unauthorized_operation", endEntityInformation.getUsername(), e.getMessage()), e);
            return null;
        } catch (WaitingForApprovalException e) {
            requestId = e.getApprovalId();
            log.info(requestId);
            log.info(e);
            return null;
        }
        
        //The end-entity has been added now! Make sure clean-up is done in this "try-finally" block if something goes wrong
        try{
            //Generates a keystore token if user has specified "ON SERVER" key pair generation.
            //Generates a certificate token if user has specified "PROVIDED_BY_USER" key pair generation
            byte[] ret = null;
            if (KeyPairGeneration.ON_SERVER.equals(selectedKeyPairGeneration)) {
                try {
                    ret = raMasterApiProxyBean.generateKeystore(raAuthenticationBean.getAuthenticationToken(), endEntityInformation);
                } catch (KeyStoreException | AuthorizationDeniedException e) {
                    raLocaleBean.addMessageError("enroll_keystore_could_not_be_generated", endEntityInformation.getUsername(), e.getMessage());
                    log.error(raLocaleBean.getMessage("enroll_keystore_could_not_be_generated", endEntityInformation.getUsername(), e.getMessage()), e);
                }
            } else if (KeyPairGeneration.PROVIDED_BY_USER.equals(selectedKeyPairGeneration)) {
                try {
                    endEntityInformation.getExtendedinformation().setCertificateRequest(CertTools.getCertificateRequestFromPem(certificateRequest).getEncoded());
                    final byte[] certificateDataToDownload = raMasterApiProxyBean.createCertificate(raAuthenticationBean.getAuthenticationToken(),
                            endEntityInformation, CertTools.getCertificateRequestFromPem(certificateRequest).getEncoded());
                    if (tokenDownloadType == TokenDownloadType.PEM_FULL_CHAIN) {
                        X509Certificate certificate = CertTools.getCertfromByteArray(certificateDataToDownload, X509Certificate.class);
                        LinkedList<Certificate> chain = new LinkedList<Certificate>(getCAInfo().getCertificateChain());
                        chain.addFirst(certificate);
                        ret = CertTools.getPemFromCertificateChain(chain);
                    } else if (tokenDownloadType == TokenDownloadType.PKCS7) {
                        X509Certificate certificate = CertTools.getCertfromByteArray(certificateDataToDownload, X509Certificate.class);
                        LinkedList<Certificate> chain = new LinkedList<Certificate>(getCAInfo().getCertificateChain());
                        chain.addFirst(certificate);
                        ret = CertTools.getPemFromPkcs7(CertTools.createCertsOnlyCMS(CertTools.convertCertificateChainToX509Chain(chain)));
                    } else if (tokenDownloadType == TokenDownloadType.PEM) {
                        X509Certificate certificate = CertTools.getCertfromByteArray(certificateDataToDownload, X509Certificate.class);
                        ret = CertTools.getPemFromCertificateChain(Arrays.asList((Certificate) certificate));
                    } else {
                        ret = certificateDataToDownload;
                    }
                } catch (CertificateParsingException | CertificateEncodingException | CMSException | AuthorizationDeniedException | IOException | ClassCastException e) {
                    raLocaleBean.addMessageError("enroll_certificate_could_not_be_generated", endEntityInformation.getUsername(), e.getMessage());
                    log.error(raLocaleBean.getMessage("enroll_certificate_could_not_be_generated", endEntityInformation.getUsername(), e.getMessage()), e);
                }
            }
            return ret;
        } finally {
            //End entity clean-up must be done if enrollment could not be completed (but end-entity has been added)
            try {
                EndEntityInformation fromCA = raMasterApiProxyBean.searchUser(raAuthenticationBean.getAuthenticationToken(), endEntityInformation.getUsername());
                if(fromCA != null && fromCA.getStatus() != EndEntityConstants.STATUS_GENERATED){
                    raMasterApiProxyBean.deleteUser(raAuthenticationBean.getAuthenticationToken(), endEntityInformation.getUsername());
                }
            } catch (AuthorizationDeniedException e) {
                throw new IllegalStateException(e);
            }
        }
    }
    
    private final void downloadToken(byte[] token, String responseContentType, String fileExtension) {
        if (token == null) {
            return;
        }
        //Download the token
        FacesContext fc = FacesContext.getCurrentInstance();
        ExternalContext ec = fc.getExternalContext();
        ec.responseReset(); // Some JSF component library or some Filter might have set some headers in the buffer beforehand. We want to get rid of them, else it may collide.
        ec.setResponseContentType(responseContentType);
        ec.setResponseContentLength(token.length);
        final String filename = StringTools.stripFilename(subjectDn.getValue() + fileExtension);
        ec.setResponseHeader("Content-Disposition",
                "attachment; filename=\"" + filename + "\""); // The Save As popup magic is done here. You can give it any file name you want, this only won't work in MSIE, it will use current request URL as file name instead.
        try (final OutputStream output = ec.getResponseOutputStream();) {
            output.write(token);
            output.flush();
            fc.responseComplete(); // Important! Otherwise JSF will attempt to render the response which obviously will fail since it's already written with a file and closed.
        } catch (IOException e) {
            log.error(raLocaleBean.getMessage("enroll_token_could_not_be_downloaded", filename), e);
            raLocaleBean.addMessageError("enroll_token_could_not_be_downloaded", filename);
        }
    }
   
    //-----------------------------------------------------------------------------------------------
    //Listeners that will be invoked from xhtml
    public final void endEntityProfileChangedListener(ValueChangeEvent e) {
        setEndEntityProfileChanged(true);
    }

    public final void certificateProfileChangedListener(ValueChangeEvent e) {
        setCertificateProfileChanged(true);
    }

    public final void certificateAuthorityChangedListener(ValueChangeEvent e) {
        setCertificateAuthorityChanged(true);
    }

    public final void keyPairGenerationChangedListener(ValueChangeEvent e) {
        setKeyPairGenerationChanged(true);
    }

    public final void algorithmChangedListener(ValueChangeEvent e) {
        setAlgorithmChanged(true);
    }
    
    public final void certificateRequestAjaxListener(final AjaxBehaviorEvent event) {
        uploadCsr();
    }

    public final void downloadCredentialsTypeChangedListener(ValueChangeEvent e) {
        setDownloadCredentialsChanged(true);
    }

    public final void endEntityProfileAjaxListener(final AjaxBehaviorEvent event) {
        selectEndEntityProfile();
    }

    public final void certificateProfileAjaxListener(final AjaxBehaviorEvent event) {
        selectCertificateProfile();
    }

    public final void certificateAuthorityAjaxListener(final AjaxBehaviorEvent event) {
        selectCertificateAuthority();
    }

    public final void keyPairGenerationAjaxListener(final AjaxBehaviorEvent event) {
        selectKeyPairGeneration();
    }

    public final void algorithmAjaxListener(final AjaxBehaviorEvent event) {
        selectAlgorithm();
        
        updateRequestPreview();
    }

    public final void csrInputTextAjaxListener(final AjaxBehaviorEvent event) {
        uploadCsr();
        
        updateRequestPreview();
    }
    
    public final void subjectDnAjaxListener(final AjaxBehaviorEvent event){
        updateRequestPreview();
    }
    
    public final void subjectAlternativeNameAjaxListener(final AjaxBehaviorEvent event){
        updateRequestPreview();
    }
    
    public final void subjectDirectoryAttributesAjaxListener(final AjaxBehaviorEvent event){
        updateRequestPreview();
    }
    
    //-----------------------------------------------------------------------------------------------
    //Validators
    
    public void validatePassword(ComponentSystemEvent event) {
        if(isPasswordRendered()){
            FacesContext fc = FacesContext.getCurrentInstance();
            UIComponent components = event.getComponent();
            UIInput uiInputPassword = (UIInput) components.findComponent("passwordField");
            String password = uiInputPassword.getLocalValue() == null ? "" : uiInputPassword.getLocalValue().toString();
            UIInput uiInputConfirmPassword = (UIInput) components.findComponent("passwordConfirmField");
            String confirmPassword = uiInputConfirmPassword.getLocalValue() == null ? "" : uiInputConfirmPassword.getLocalValue().toString();
            /*if (password.isEmpty() || confirmPassword.isEmpty()) {
                FacesContext.getCurrentInstance().addMessage("passwordFieldMessage", raLocaleBean.getFacesMessage("enroll_password_can_not_be_empty"));
                fc.renderResponse();
                
            }else */if (!password.equals(confirmPassword)) {
                FacesContext.getCurrentInstance().addMessage("passwordFieldMessage", raLocaleBean.getFacesMessage("enroll_passwords_are_not_equal"));
                fc.renderResponse();
            }
        }
    }
    
    public void validateCsr(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        PKCS10CertificationRequest pkcs10CertificateRequest = CertTools.getCertificateRequestFromPem(value.toString());
        if (pkcs10CertificateRequest == null) {
            throw new ValidatorException(new FacesMessage(raLocaleBean.getMessage("enroll_invalid_certificate_request")));
        }
        
        //Get public key algorithm from CSR and check if it's allowed in certificate profile
        final JcaPKCS10CertificationRequest jcaPKCS10CertificationRequest = new JcaPKCS10CertificationRequest(pkcs10CertificateRequest);
        try {
            final String keySpecification = AlgorithmTools.getKeySpecification(jcaPKCS10CertificationRequest.getPublicKey());
            final String keyAlgorithm = AlgorithmTools.getKeyAlgorithm(jcaPKCS10CertificationRequest.getPublicKey());
            final CertificateProfile certificateProfile = getCertificateProfile();
            final List<String> availableKeyAlgorithms = certificateProfile.getAvailableKeyAlgorithmsAsList();
            final List<Integer> availableBitLengths = certificateProfile.getAvailableBitLengthsAsList();
            if(!availableKeyAlgorithms.contains(keyAlgorithm) ||
                    !availableBitLengths.contains(Integer.parseInt(keySpecification))){
                throw new ValidatorException(new FacesMessage(raLocaleBean.getMessage("enroll_key_algorithm_is_not_available", keyAlgorithm + "_" + keySpecification)));
            }
            algorithmFromCsr = keyAlgorithm + " " + keySpecification;// Save for later use
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            throw new ValidatorException(new FacesMessage(raLocaleBean.getMessage("enroll_unknown_key_algorithm")));
        }
    }
    
    
    
    //-----------------------------------------------------------------------------------------------
    //Automatically generated getters/setters
    /**
     * @return the authorizedEndEntityProfiles
     */
    public IdNameHashMap<EndEntityProfile> getAuthorizedEndEntityProfiles() {
        return authorizedEndEntityProfiles;
    }

    /**
     * @param authorizedEndEntityProfiles the authorizedEndEntityProfiles to set
     */
    private void setAuthorizedEndEntityProfiles(IdNameHashMap<EndEntityProfile> authorizedEndEntityProfiles) {
        this.authorizedEndEntityProfiles = authorizedEndEntityProfiles;
    }

    /**
     * @return the selectedEndEntityProfile
     */
    public String getSelectedEndEntityProfile() {
        return selectedEndEntityProfile;
    }

    public EndEntityProfile getEndEntityProfile() {
        if (selectedEndEntityProfile == null) {
            return null;
        }
        Tuple<EndEntityProfile> temp = authorizedEndEntityProfiles.get(selectedEndEntityProfile);
        if (temp == null) {
            return null;
        }
        return temp.getValue();
    }

    public CertificateProfile getCertificateProfile() {
        if (selectedCertificateProfile == null) {
            return null;
        }
        Tuple<CertificateProfile> temp = authorizedCertificateProfiles.get(selectedCertificateProfile);
        if (temp == null) {
            return null;
        }
        return temp.getValue();
    }

    public CAInfo getCAInfo() {
        if (selectedCertificateAuthority == null) {
            return null;
        }
        Tuple<CAInfo> temp = authorizedCAInfos.get(selectedCertificateAuthority);
        if (temp == null) {
            return null;
        }
        return temp.getValue();
    }
    
    public String getAlgorithm(){
        if (KeyPairGeneration.ON_SERVER.equals(selectedKeyPairGeneration)) {
            return selectedAlgorithm;
        } else {
            return algorithmFromCsr;
        }
    }

    /**
     * @param selectedEndEntityProfile the selectedEndEntityProfile to set
     */
    public void setSelectedEndEntityProfile(String selectedEndEntityProfile) {
        this.selectedEndEntityProfile = selectedEndEntityProfile;
    }

    /**
     * @return the selectedKeyPairGeneration
     */
    public String getSelectedKeyPairGeneration() {
        return selectedKeyPairGeneration==null ? null : selectedKeyPairGeneration.name();
    }

    /**
     * @param selectedKeyPairGeneration the selectedKeyPairGeneration to set
     */
    public void setSelectedKeyPairGeneration(final String selectedKeyStoreGeneration) {
        this.selectedKeyPairGeneration = KeyPairGeneration.valueOf(selectedKeyStoreGeneration);
    }

    /**
     * @return the endEntityProfileChanged
     */
    public boolean isEndEntityProfileChanged() {
        return endEntityProfileChanged;
    }

    /**
     * @param endEntityProfileChanged the endEntityProfileChanged to set
     */
    public void setEndEntityProfileChanged(boolean endEntityProfileChanged) {
        this.endEntityProfileChanged = endEntityProfileChanged;
    }

    public List<SelectItem> getAvailableKeyPairGenerationSelectItems() {
        final List<SelectItem> ret = new ArrayList<>();
        for (final KeyPairGeneration keyPairGeneration : getAvailableKeyPairGenerations()) {
            final String label = raLocaleBean.getMessage("enroll_key_pair_generation_" + keyPairGeneration.name().toLowerCase());
            ret.add(new SelectItem(keyPairGeneration.name(), label));
        }
        return ret;
    }

    private List<KeyPairGeneration> getAvailableKeyPairGenerations() {
        final List<KeyPairGeneration> ret = new ArrayList<>();
        final EndEntityProfile endEntityProfile = getEndEntityProfile();
        if (endEntityProfile != null) {
            final String availableKeyStores = endEntityProfile.getValue(EndEntityProfile.AVAILKEYSTORE, 0);
            if (availableKeyStores.contains(String.valueOf(SecConst.TOKEN_SOFT_P12))
                    || availableKeyStores.contains(String.valueOf(SecConst.TOKEN_SOFT_JKS))
                    || availableKeyStores.contains(String.valueOf(SecConst.TOKEN_SOFT_PEM))) {
                ret.add(KeyPairGeneration.ON_SERVER);
            }
            if (availableKeyStores.contains(String.valueOf(SecConst.TOKEN_SOFT_BROWSERGEN))) {
                ret.add(KeyPairGeneration.PROVIDED_BY_USER);
            }
        }
        return ret;
    }

    /**
     * @return the keyPairGenerationChanged
     */
    public boolean isKeyPairGenerationChanged() {
        return keyPairGenerationChanged;
    }

    /**
     * @param keyPairGenerationChanged the keyPairGenerationChanged to set
     */
    public void setKeyPairGenerationChanged(boolean keyPairGenerationChanged) {
        this.keyPairGenerationChanged = keyPairGenerationChanged;
    }

    /**
     * @return the availableCertificateProfiles
     */
    public Map<String, String> getAvailableCertificateProfiles() {
        return availableCertificateProfiles;
    }

    /**
     * @param availableCertificateProfiles the availableCertificateProfiles to set
     */
    public void setAvailableCertificateProfiles(Map<String, String> availableCertificateProfiles) {
        this.availableCertificateProfiles = availableCertificateProfiles;
    }

    /**
     * @return the selectedCertificateProfile
     */
    public String getSelectedCertificateProfile() {
        return selectedCertificateProfile;
    }

    /**
     * @param selectedCertificateProfile the selectedCertificateProfile to set
     */
    public void setSelectedCertificateProfile(String selectedCertificateProfile) {
        this.selectedCertificateProfile = selectedCertificateProfile;
    }

    /**
     * @return the certificateProfileChanged
     */
    public boolean isCertificateProfileChanged() {
        return certificateProfileChanged;
    }

    /**
     * @param certificateProfileChanged the certificateProfileChanged to set
     */
    public void setCertificateProfileChanged(boolean certificateProfileChanged) {
        this.certificateProfileChanged = certificateProfileChanged;
    }

    /**
     * @return the availableAlgorithms
     */
    public Map<String, String> getAvailableAlgorithms() {
        return availableAlgorithms;
    }

    /**
     * @param availableAlgorithms the availableAlgorithms to set
     */
    public void setAvailableAlgorithms(Map<String, String> availableAlgorithms) {
        this.availableAlgorithms = availableAlgorithms;
    }

    /**
     * @return the selectedAlgorithm
     */
    public String getSelectedAlgorithm() {
        return selectedAlgorithm;
    }

    /**
     * @param selectedAlgorithm the selectedAlgorithm to set
     */
    public void setSelectedAlgorithm(String selectedAlgorithm) {
        this.selectedAlgorithm = selectedAlgorithm;
    }

    /**
     * @return the algorithmChanged
     */
    public boolean isAlgorithmChanged() {
        return algorithmChanged;
    }

    /**
     * @param algorithmChanged the algorithmChanged to set
     */
    public void setAlgorithmChanged(boolean algorithmChanged) {
        this.algorithmChanged = algorithmChanged;
    }

    /**
     * @return the endEntityInformation
     */
    public EndEntityInformation getEndEntityInformation() {
        return endEntityInformation;
    }

    /**
     * @param endEntityInformation the endEntityInformation to set
     */
    public void setEndEntityInformation(EndEntityInformation endEntityInformation) {
        this.endEntityInformation = endEntityInformation;
    }

    /**
     * @return the confirmPassword
     */
    public String getConfirmPassword() {
        return confirmPassword;
    }

    /**
     * @param confirmPassword the confirmPassword to set
     */
    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    /**
     * @return the availableCertificateAuthorities
     */
    public Map<String, String> getAvailableCertificateAuthorities() {
        return availableCertificateAuthorities;
    }

    /**
     * @param availableCertificateAuthorities the availableCertificateAuthorities to set
     */
    public void setAvailableCertificateAuthorities(Map<String, String> availableCertificateAuthorities) {
        this.availableCertificateAuthorities = availableCertificateAuthorities;
    }

    /**
     * @return the selectedCertificateAuthority
     */
    public String getSelectedCertificateAuthority() {
        return selectedCertificateAuthority;
    }

    /**
     * @param selectedCertificateAuthority the selectedCertificateAuthority to set
     */
    public void setSelectedCertificateAuthority(String selectedCertificateAuthority) {
        this.selectedCertificateAuthority = selectedCertificateAuthority;
    }

    /**
     * @return the certificateAuthorityChanged
     */
    public boolean isCertificateAuthorityChanged() {
        return certificateAuthorityChanged;
    }

    /**
     * @param certificateAuthorityChanged the certificateAuthorityChanged to set
     */
    public void setCertificateAuthorityChanged(boolean certificateAuthorityChanged) {
        this.certificateAuthorityChanged = certificateAuthorityChanged;
    }

    public IdNameHashMap<CertificateProfile> getAuthorizedCertificateProfiles() {
        return authorizedCertificateProfiles;
    }

    public void setAuthorizedCertificateProfiles(IdNameHashMap<CertificateProfile> authorizedCertificateProfiles) {
        this.authorizedCertificateProfiles = authorizedCertificateProfiles;
    }

    /**
     * @return the authorizedCAInfos
     */
    public IdNameHashMap<CAInfo> getAuthorizedCAInfos() {
        return authorizedCAInfos;
    }

    /**
     * @param authorizedCAInfos the authorizedCAInfos to set
     */
    public void setAuthorizedCAInfos(IdNameHashMap<CAInfo> authorizedCAInfos) {
        this.authorizedCAInfos = authorizedCAInfos;
    }

    /**
     * @return the availableEndEntityProfiles
     */
    public Map<String, String> getAvailableEndEntityProfiles() {
        return availableEndEntityProfiles;
    }

    /**
     * @param availableEndEntityProfiles the availableEndEntityProfiles to set
     */
    public void setAvailableEndEntityProfiles(Map<String, String> availableEndEntities) {
        this.availableEndEntityProfiles = availableEndEntities;
    }

    /**
     * @return the subjectDN
     */
    public SubjectDn getSubjectDn() {
        return subjectDn;
    }

    /**
     * @param subjectDn the subjectDN to set
     */
    public void setSubjectDn(SubjectDn subjectDn) {
        this.subjectDn = subjectDn;
    }

    /**
     * @return the subjectAlternativeName
     */
    public SubjectAlternativeName getSubjectAlternativeName() {
        return subjectAlternativeName;
    }

    /**
     * @param subjectAlternativeName the subjectAlternativeName to set
     */
    public void setSubjectAlternativeName(SubjectAlternativeName subjectAlternativeName) {
        this.subjectAlternativeName = subjectAlternativeName;
    }

    /**
     * @return the subjectDirectoryAttributes
     */
    public SubjectDirectoryAttributes getSubjectDirectoryAttributes() {
        return subjectDirectoryAttributes;
    }

    /**
     * @param subjectDirectoryAttributes the subjectDirectoryAttributes to set
     */
    public void setSubjectDirectoryAttributes(SubjectDirectoryAttributes subjectDirectoryAttributes) {
        this.subjectDirectoryAttributes = subjectDirectoryAttributes;
    }

    /**
     * @return the downloadCredentialsChanged
     */
    public boolean isDownloadCredentialsChanged() {
        return downloadCredentialsChanged;
    }

    /**
     * @param downloadCredentialsChanged the downloadCredentialsChanged to set
     */
    public void setDownloadCredentialsChanged(boolean downloadCredentialsChanged) {
        this.downloadCredentialsChanged = downloadCredentialsChanged;
    }

    /**
     * @return the certificateRequest
     */
    public String getCertificateRequest() {
        return certificateRequest;
    }

    /**
     * @param certificateRequest the certificateRequest to set
     */
    public void setCertificateRequest(String certificateRequest) {
        this.certificateRequest = certificateRequest;
    }

    /**
     * @return the requestId
     */
    public int getRequestId() {
        return requestId;
    }

    /**
     * @param requestId the requestId to set
     */
    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public RaRequestPreview getRequestPreview() {
        return requestPreview;
    }

    public void setRequestPreview(RaRequestPreview requestPreview) {
        this.requestPreview = requestPreview;
    }

    /**
     * @return the selectRequestTemplateRendered
     */
    public boolean isSelectRequestTemplateRendered() {
        return selectRequestTemplateRendered;
    }

    /**
     * @param selectRequestTemplateRendered the selectRequestTemplateRendered to set
     */
    public void setSelectRequestTemplateRendered(boolean selectRequestTemplateRendered) {
        this.selectRequestTemplateRendered = selectRequestTemplateRendered;
    }

    /**
     * @return the selectKeyAlgorithmRendered
     */
    public boolean isSelectKeyAlgorithmRendered() {
        return selectKeyAlgorithmRendered;
    }

    /**
     * @param selectKeyAlgorithmRendered the selectKeyAlgorithmRendered to set
     */
    public void setSelectKeyAlgorithmRendered(boolean selectKeyAlgorithmRendered) {
        this.selectKeyAlgorithmRendered = selectKeyAlgorithmRendered;
    }

    /**
     * @return the uploadCsrRendered
     */
    public boolean isUploadCsrRendered() {
        return uploadCsrRendered;
    }

    /**
     * @param uploadCsrRendered the uploadCsrRendered to set
     */
    public void setUploadCsrRendered(boolean uploadCsrRendered) {
        this.uploadCsrRendered = uploadCsrRendered;
    }

    /**
     * @return the provideRequestInfoRendered
     */
    public boolean isProvideRequestInfoRendered() {
        return provideRequestInfoRendered;
    }

    /**
     * @param provideRequestInfoRendered the provideRequestInfoRendered to set
     */
    public void setProvideRequestInfoRendered(boolean provideRequestInfoRendered) {
        this.provideRequestInfoRendered = provideRequestInfoRendered;
    }

    /**
     * @return the provideRequestMetadataRendered
     */
    public boolean isProvideRequestMetadataRendered() {
        return provideRequestMetadataRendered;
    }

    /**
     * @param provideRequestMetadataRendered the provideRequestMetadataRendered to set
     */
    public void setProvideRequestMetadataRendered(boolean provideRequestMetadataRendered) {
        this.provideRequestMetadataRendered = provideRequestMetadataRendered;
    }

    /**
     * @return the confirmRequestRendered
     */
    public boolean isConfirmRequestRendered() {
        return confirmRequestRendered;
    }

    /**
     * @param confirmRequestRendered the confirmRequestRendered to set
     */
    public void setConfirmRequestRendered(boolean confirmRequestRendered) {
        this.confirmRequestRendered = confirmRequestRendered;
    }

    
}
