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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.certificates.ca.CADoesntExistsException;
import org.cesecore.certificates.ca.CAInfo;
import org.cesecore.certificates.ca.IllegalNameException;
import org.cesecore.certificates.certificate.exception.CertificateSerialNumberException;
import org.cesecore.certificates.certificateprofile.CertificateProfile;
import org.cesecore.certificates.endentity.EndEntityConstants;
import org.cesecore.certificates.endentity.EndEntityInformation;
import org.cesecore.certificates.endentity.ExtendedInformation;
import org.ejbca.core.ejb.ra.NoSuchEndEntityException;
import org.ejbca.core.model.approval.ApprovalException;
import org.ejbca.core.model.approval.WaitingForApprovalException;
import org.ejbca.core.model.authorization.AccessRulesConstants;
import org.ejbca.core.model.era.IdNameHashMap;
import org.ejbca.core.model.era.KeyToValueHolder;
import org.ejbca.core.model.era.RaMasterApiProxyBeanLocal;
import org.ejbca.core.model.ra.CustomFieldException;
import org.ejbca.core.model.ra.raadmin.EndEntityProfile;
import org.ejbca.core.model.ra.raadmin.EndEntityProfileValidationException;
import org.ejbca.ra.RaEndEntityDetails.Callbacks;

/**
 * Backing bean for end entity details view.
 *  
 * @version $Id$
 */
@ManagedBean
@ViewScoped
public class RaEndEntityBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(RaEndEntityBean.class);

    @EJB
    private RaMasterApiProxyBeanLocal raMasterApiProxyBean;

    @ManagedProperty(value="#{raAccessBean}")
    private RaAccessBean raAccessBean;
    public void setRaAccessBean(final RaAccessBean raAccessBean) { this.raAccessBean = raAccessBean; }

    @ManagedProperty(value="#{raAuthenticationBean}")
    private RaAuthenticationBean raAuthenticationBean;
    public void setRaAuthenticationBean(final RaAuthenticationBean raAuthenticationBean) { this.raAuthenticationBean = raAuthenticationBean; }

    @ManagedProperty(value="#{raLocaleBean}")
    private RaLocaleBean raLocaleBean;
    public void setRaLocaleBean(final RaLocaleBean raLocaleBean) { this.raLocaleBean = raLocaleBean; }

    @ManagedProperty(value="#{msg}")
    private ResourceBundle msg;
    public void setMsg(ResourceBundle msg) {
        this.msg = msg;
    }

    private IdNameHashMap<EndEntityProfile> authorizedEndEntityProfiles = new IdNameHashMap<>();
    private IdNameHashMap<CertificateProfile> authorizedCertificateProfiles = new IdNameHashMap<>();
    private IdNameHashMap<CAInfo> authorizedCAInfos = new IdNameHashMap<>();

    private String username = null;
    private RaEndEntityDetails raEndEntityDetails = null;
    private Map<Integer, String> eepIdToNameMap = null;
    private Map<Integer, String> cpIdToNameMap = null;
    private Map<Integer,String> caIdToNameMap = new HashMap<>();
    private boolean editEditEndEntityMode = false;
    private List<RaCertificateDetails> issuedCerts = null;
    private SelectItem[] selectableStatuses = null;
    private SelectItem[] selectableTokenTypes = null;
    private int selectedStatus = -1;
    private int selectedTokenType = -1;
    private String enrollmentCode = "";
    private String enrollmentCodeConfirm = "";
    private boolean clearCsrChecked = false;
    private boolean authorized = false;
    private int maxFailedLogins;
    private int remainingLogin;
    private boolean resetRemainingLoginAttempts;
    private int eepId;
    private int cpId;
    private int caId;
    private SubjectDn subjectDistinguishNames = null;
    private SubjectAlternativeName subjectAlternativeNames = null;
    private SubjectDirectoryAttributes subjectDirectoryAttributes = null;
    private Map<Integer, String> endEntityProfiles;
    // TODO: java doc for all new methods
    private final Callbacks raEndEntityDetailsCallbacks = new RaEndEntityDetails.Callbacks() {
        @Override
        public RaLocaleBean getRaLocaleBean() {
            return raLocaleBean;
        }

        @Override
        public EndEntityProfile getEndEntityProfile(int eepId) {
            IdNameHashMap<EndEntityProfile> map = raMasterApiProxyBean.getAuthorizedEndEntityProfiles(raAuthenticationBean.getAuthenticationToken(), AccessRulesConstants.VIEW_END_ENTITY);
            KeyToValueHolder<EndEntityProfile> tuple = map.get(eepId);
            return tuple==null ? null : tuple.getValue();
        }
    };

    @PostConstruct
    public void postConstruct() {
        if (!raAccessBean.isAuthorizedToSearchEndEntities()) {
            log.debug("Not authorized to view end entities");
            return;
        }
        authorized = true;
        username = ((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest()).getParameter("ee");
        // Check if edit mode is set as a parameter
        String editParameter = ((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest()).getParameter("edit");
        if (editParameter != null && editParameter.equals("true")) {
            editEditEndEntity();
        } else {
            reload();
        }
    }

    private void reload() {
        if (username != null) {
            final EndEntityInformation endEntityInformation = raMasterApiProxyBean.searchUser(raAuthenticationBean.getAuthenticationToken(), username);
            if (endEntityInformation != null) {
                cpIdToNameMap = raMasterApiProxyBean.getAuthorizedCertificateProfileIdsToNameMap(raAuthenticationBean.getAuthenticationToken());
                eepIdToNameMap = raMasterApiProxyBean.getAuthorizedEndEntityProfileIdsToNameMap(raAuthenticationBean.getAuthenticationToken());
                final List<CAInfo> caInfos = new ArrayList<>(raMasterApiProxyBean.getAuthorizedCas(raAuthenticationBean.getAuthenticationToken()));
                for (final CAInfo caInfo : caInfos) {
                    caIdToNameMap.put(caInfo.getCAId(), caInfo.getName());
                }
                raEndEntityDetails = new RaEndEntityDetails(endEntityInformation, raEndEntityDetailsCallbacks, cpIdToNameMap, eepIdToNameMap, caIdToNameMap);
                selectedTokenType = endEntityInformation.getTokenType();

                authorizedEndEntityProfiles = raMasterApiProxyBean.getAuthorizedEndEntityProfiles(raAuthenticationBean.getAuthenticationToken(), AccessRulesConstants.CREATE_END_ENTITY);
                authorizedCertificateProfiles = raMasterApiProxyBean.getAllAuthorizedCertificateProfiles(raAuthenticationBean.getAuthenticationToken());
                authorizedCAInfos = raMasterApiProxyBean.getAuthorizedCAInfos(raAuthenticationBean.getAuthenticationToken());
                endEntityProfiles = authorizedEndEntityProfiles.getIdMap()
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue().getName()));

                eepId = raEndEntityDetails.getEndEntityInformation().getEndEntityProfileId();
                cpId = raEndEntityDetails.getEndEntityInformation().getCertificateProfileId();
                caId = raEndEntityDetails.getEndEntityInformation().getCAId();
                maxFailedLogins = raEndEntityDetails.getEndEntityInformation().getExtendedInformation().getMaxLoginAttempts();
                remainingLogin = raEndEntityDetails.getEndEntityInformation().getExtendedInformation().getRemainingLoginAttempts();
            }
        }
        issuedCerts = null;
        selectableStatuses = null;
        selectableTokenTypes = null;
        selectedStatus = -1;
        clearCsrChecked = false;
        resetRemainingLoginAttempts = false;
    }
    
    public boolean isAuthorized() {
        return authorized;
    }

    public String getUsername() { return username; }

    public void setUsername(String username) {
        this.username = username;
    }

    public RaEndEntityDetails getEndEntity() { return raEndEntityDetails; }

    /**
     * @return true if edit mode is enabled
     */
    public boolean isEditEditEndEntityMode() {
        return editEditEndEntityMode;
    }

    /**
     * Enables edit mode (given that the API version allows it) and reloads
     */
    public void editEditEndEntity() {
        editEditEndEntityMode = isApiEditCompatible();
        reload();
    }

    /**
     * Cancels edit mode and reloads
     */
    public void editEditEndEntityCancel() {
        editEditEndEntityMode = false;
        reload();
    }

    /**
     * Edits the current End Entity, cancels edit mode and reloads
     */
    public void editEditEndEntitySave() {
        boolean changed = false;
        int selectedStatus = getSelectedStatus();
        int selectedTokenType = getSelectedTokenType();
        EndEntityInformation endEntityInformation = new EndEntityInformation(raEndEntityDetails.getEndEntityInformation());
        ExtendedInformation extendedInformation = endEntityInformation.getExtendedInformation();

        if (selectedStatus > 0 && selectedStatus != endEntityInformation.getStatus()) {
            // A new status was selected, verify the enrollment codes
            if (verifyEnrollmentCodes()) {
                // Change the End Entity's status and set the new password
                endEntityInformation.setStatus(selectedStatus);
                endEntityInformation.setPassword(enrollmentCode);
                endEntityInformation.setTokenType(getNewTokenTypeValue(selectedTokenType, endEntityInformation));
                changed = true;
            }
        } else if (!StringUtils.isEmpty(enrollmentCode)
                || !StringUtils.isEmpty(enrollmentCodeConfirm)) {
            // Not a new status, but the enrollment codes were not empty
            if (verifyEnrollmentCodes()) {
                // Enrollment codes were valid, set new password but leave status unchanged
                endEntityInformation.setPassword(enrollmentCode);
                endEntityInformation.setTokenType(getNewTokenTypeValue(selectedTokenType, endEntityInformation));
                changed = true;
            }
        } else {
            int newTokenType = getNewTokenTypeValue(selectedTokenType, endEntityInformation);
            if (newTokenType != endEntityInformation.getTokenType()) {
                endEntityInformation.setTokenType(newTokenType);
                changed = true;
            }
        }
        if (clearCsrChecked) {
            if (endEntityInformation.getExtendedInformation() != null) {
                endEntityInformation.getExtendedInformation().setCertificateRequest(null);
            }            
            changed = true;
        }
        String newUsername = null;
        if (!username.equals(endEntityInformation.getUsername())) {
            newUsername = username;
            changed = true;
        }
        String subjectDn = subjectDistinguishNames.getValue();
        if(!subjectDn.equals(endEntityInformation.getDN())) {
            endEntityInformation.setDN(subjectDn);
            changed = true;
        }
        String subjectAn = subjectAlternativeNames.getValue();
        if (!subjectAn.equals(endEntityInformation.getSubjectAltName())) {
            endEntityInformation.setSubjectAltName(subjectAn);
            changed = true;
        }
        String subjectDa = subjectDirectoryAttributes.getValue();
        if (!subjectDa.equals(endEntityInformation.getExtendedInformation().getSubjectDirectoryAttributes())) {
            endEntityInformation.getExtendedInformation().setSubjectDirectoryAttributes(subjectDa);
            changed = true;
        }
        if (extendedInformation != null && maxFailedLogins != extendedInformation.getMaxLoginAttempts()) {
            endEntityInformation.getExtendedInformation().setMaxLoginAttempts(maxFailedLogins);
            changed = true;
        }
        if (extendedInformation != null && resetRemainingLoginAttempts) {
            endEntityInformation.getExtendedInformation().setRemainingLoginAttempts(maxFailedLogins);
            changed = true;
        }
        if (eepId != endEntityInformation.getEndEntityProfileId()) {
            endEntityInformation.setEndEntityProfileId(eepId);
            changed = true;
        }
        if (cpId != endEntityInformation.getCertificateProfileId()) {
            endEntityInformation.setCertificateProfileId(cpId);
            changed = true;
        }
        if (caId != endEntityInformation.getCAId()) {
            endEntityInformation.setCAId(caId);
            changed = true;
        }

        if (changed) {
            // Edit the End Entity if changes were made
            try {
                boolean result = raMasterApiProxyBean.editUser(raAuthenticationBean.getAuthenticationToken(), endEntityInformation, false, newUsername);
                if (result) {
                    raLocaleBean.addMessageError("editendentity_success");
                } else {
                    raLocaleBean.addMessageError("editendentity_failure");
                }
            } catch (WaitingForApprovalException e) {
                raLocaleBean.addMessageError("editendentity_approval_sent");
            } catch (ApprovalException e) {
                raLocaleBean.addMessageError("editendentity_approval_exists");
            } catch (AuthorizationDeniedException e) {
                raLocaleBean.addMessageError("editendentity_unauthorized");
            } catch (EndEntityProfileValidationException e) {
                raLocaleBean.addMessageError("editendentity_validation_failed");
            } catch (CADoesntExistsException e) {
                raLocaleBean.addMessageError("editendentity_no_such_ca");
            } catch (CertificateSerialNumberException | IllegalNameException
                    | NoSuchEndEntityException
                    | CustomFieldException e) {
                raLocaleBean.addMessageError("editendentity_failure");
            }
        }
        editEditEndEntityCancel();
    }

    /**
     * @return the new tokenType value to be saved/used
     */
    private int getNewTokenTypeValue(final int selectedTokenType, final EndEntityInformation endEntityInformation) {
        if (selectedTokenType == -1) {
            return endEntityInformation.getTokenType();
        }
        return selectedTokenType;
    }

    /**
     * @return true if enrollment code and confirm enrollment code are valid
     */
    private boolean verifyEnrollmentCodes() {
        if (blankEnrollmentCodes()) {
            raLocaleBean.addMessageError("editendentity_password_blank");
            return false;
        }
        if (!enrollmentCode.equals(enrollmentCodeConfirm)) {
            raLocaleBean.addMessageError("editendentity_password_nomatch");
            return false;
        }
        return true;
    }

    /**
     * @return true if enrollment code or confirm enrollment code is blank
     */
    private boolean blankEnrollmentCodes() {
        return StringUtils.isBlank(enrollmentCode) || StringUtils.isBlank(enrollmentCodeConfirm);
    }

    /**
     * @return the status currently selected in edit mode
     */
    public int getSelectedStatus() {
        if (selectedStatus == -1) {
            getSelectableStatuses();
        }
        return selectedStatus;
    }

    /**
     * Sets the selected status to a new status
     * 
     * @param selectedStatus the new status
     */
    public void setSelectedStatus(int selectedStatus) {
        this.selectedStatus = selectedStatus;
    }
    
    /**
     * @return the tokenType currently selected in edit mode
     */
    public int getSelectedTokenType() {
        if (selectedTokenType == -1) {
            getSelectableTokenTypes();
        }
        return selectedTokenType;
    }

    /**
     * Sets the selected tokenType to a new tokenType
     * 
     * @param selectedTokenType the new tokenType
     */
    public void setSelectedTokenType(int selectedTokenType) {
        this.selectedTokenType = selectedTokenType;
    }

    /**
     * Sets the enrollment code field
     * 
     * @param enrollmentCode the new enrollment code
     */
    public void setEnrollmentCode(String enrollmentCode) {
        this.enrollmentCode = enrollmentCode;
    }

    /**
     * @return the enrollment code
     */
    public String getEnrollmentCode() {
        return enrollmentCode;
    }

    /**
     * Sets the enrollment code (confirm) field
     * 
     * @param enrollmentCode the new enrollment code (confirm)
     */
    public void setEnrollmentCodeConfirm(String enrollmentCodeConfirm) {
        this.enrollmentCodeConfirm = enrollmentCodeConfirm;
    }

    /**
     * @return the enrollment code (confirm)
     */
    public String getEnrollmentCodeConfirm() {
        return enrollmentCodeConfirm;
    }

    /**
     * Generates an array of selectable statuses if not already cached and sets
     * the current selected status to "Unchanged"
     * 
     * @return an array of selectable statuses
     */
    public SelectItem[] getSelectableStatuses() {
        if (editEditEndEntityMode && selectableStatuses == null) {
            selectableStatuses = new SelectItem[] {
                    new SelectItem(0,
                            raLocaleBean.getMessage("component_eedetails_status_unchanged")),
                    new SelectItem(EndEntityConstants.STATUS_NEW,
                            raLocaleBean.getMessage("component_eedetails_status_new")),
                    new SelectItem(EndEntityConstants.STATUS_GENERATED,
                            raLocaleBean.getMessage("component_eedetails_status_generated"))
            };
            selectedStatus = (int)selectableStatuses[0].getValue();
        }
        return selectableStatuses;
    }
    
    /**
     * Generates an array of selectable tokenTypes if not already cached
     * 
     * @return an array of selectable tokenTypes
     */
    public SelectItem[] getSelectableTokenTypes() {
        if (editEditEndEntityMode && selectableTokenTypes == null) {
            selectableTokenTypes = new SelectItem[] {
                    new SelectItem(EndEntityConstants.TOKEN_USERGEN,
                            raLocaleBean.getMessage("component_eedetails_tokentype_usergen")),
                    new SelectItem(EndEntityConstants.TOKEN_SOFT_JKS,
                            raLocaleBean.getMessage("component_eedetails_tokentype_jks")),
                    new SelectItem(EndEntityConstants.TOKEN_SOFT_P12,
                            raLocaleBean.getMessage("component_eedetails_tokentype_pkcs12")),
                    new SelectItem(EndEntityConstants.TOKEN_SOFT_PEM,
                            raLocaleBean.getMessage("component_eedetails_tokentype_pem"))
            };
            selectedTokenType = (int)selectableTokenTypes[0].getValue();
        }
        return selectableTokenTypes;
    }

    /**
     * @return a list of the End Entity's certificates
     */
    public List<RaCertificateDetails> getIssuedCerts() {
        if (issuedCerts == null) {
            issuedCerts = RaEndEntityTools.searchCertsByUsernameSorted(
                    raMasterApiProxyBean, raAuthenticationBean.getAuthenticationToken(),
                    username, raLocaleBean);
        }
        return issuedCerts;
    }

    /**
     * @return true if the API is compatible with End Entity editing 
     */
    public boolean isApiEditCompatible() {
        return raMasterApiProxyBean.getApiVersion() >= 2;
    }
    
    /**
     * @return whether the Clear CSR checkbox is checked
     */
    public boolean getClearCsrChecked() {
        return clearCsrChecked;
    }
    
    /**
     * Sets the CSR to be cleared after the save button is pressed
     * 
     * @param checked Whether the checkbox is checked
     */
    public void setClearCsrChecked(boolean checked) {
        this.clearCsrChecked = checked;
    }

    public String getUsernameWarning() {
        if (!username.equals(raEndEntityDetails.getUsername())
            && username != null
            && !username.isEmpty()
            && raMasterApiProxyBean.searchUser(raAuthenticationBean.getAuthenticationToken(), username) != null) {
            return msg.getString("enroll_already_exists");
        } else
            return "";
    }

    public int getMaxFailedLogins() {
        return maxFailedLogins;
    }

    public void setMaxFailedLogins(int maxFailedLogins) {
        this.maxFailedLogins = maxFailedLogins;
    }

    public boolean isUnlimited() {
        return maxFailedLogins < 0;
    }

    public void setUnlimited(boolean unlimited) {
        if (unlimited) {
            maxFailedLogins = -1;
        } else {
            maxFailedLogins = 10; // hard coded default
        }
    }

    public int getRemainingLogin() {
        return remainingLogin;
    }

    public void setRemainingLogin(int remainingLogin) {
        this.remainingLogin = remainingLogin;
    }

    public boolean isResetRemainingLoginAttempts() {
        return resetRemainingLoginAttempts;
    }

    public void setResetRemainingLoginAttempts(boolean resetRemainingLoginAttempts) {
        this.resetRemainingLoginAttempts = resetRemainingLoginAttempts;
    }

    public Map<Integer, String> getEndEntityProfiles() {
        return endEntityProfiles;
    }

    public int getEepId() {
        return eepId;
    }

    public void setEepId(int eepId) {
        if (this.eepId != eepId) {
            this.eepId = eepId;
            EndEntityProfile eep = authorizedEndEntityProfiles.get(eepId).getValue();
            if (eep.getAvailableCertificateProfileIds().contains(cpId)) {
                setCpId(cpId);
            } else {
                setCpId(eep.getDefaultCertificateProfile());
            }

            subjectDistinguishNames = null;
            subjectAlternativeNames = null;
            subjectDirectoryAttributes = null;
        }
    }

    public int getCpId() {
        return cpId;
    }

    public void setCpId(int cpId) {
        this.cpId = cpId;
        int defaultCA = authorizedEndEntityProfiles.get(eepId).getValue().getDefaultCA();
        Map<Integer, String> cAs = getCertificateAuthorities();
        if (cAs.size() == 0) {
            caId = 0;
        } else {
            caId = cAs.keySet().contains(defaultCA) ? defaultCA : cAs.keySet().iterator().next();
        }
    }

    public Map<Integer, String> getCertificateProfiles() {
        List<Integer> availableCpIds = authorizedEndEntityProfiles.get(eepId).getValue().getAvailableCertificateProfileIds();
        return availableCpIds.stream()
                .collect(Collectors.toMap(cpId -> cpId, cpId -> authorizedCertificateProfiles.getIdMap().get(cpId).getName()));
    }

    public int getCaId() {
        return caId;
    }

    public void setCaId(int caId) {
        this.caId = caId;
    }

    public Map<Integer, String> getCertificateAuthorities() {
        int eepAnyCa = 1;
        List<Integer> eepCAs = authorizedEndEntityProfiles.get(eepId).getValue().getAvailableCAs();
        CertificateProfile cp = authorizedCertificateProfiles.get(cpId).getValue();
        List<Integer> cpCAs = authorizedCertificateProfiles.get(cpId).getValue().getAvailableCAs();
        List<Integer> allCAs = new ArrayList<>(authorizedCAInfos.idKeySet());
        List<Integer> usableCAs;
        if (eepCAs.contains(eepAnyCa)) {
            if (cp.isApplicableToAnyCA()) {
                usableCAs = allCAs;
            } else {
                usableCAs = cpCAs;
            }
        } else {
            if (cp.isApplicableToAnyCA()) {
                usableCAs = eepCAs;
            } else {
                usableCAs = eepCAs.stream()
                    .filter(cpCAs::contains)
                    .collect(Collectors.toList());
            }
        }

        return usableCAs.stream()
            .collect(Collectors.toMap(caId -> caId, caId -> authorizedCAInfos.get(caId).getValue().getName()));
    }

    public SubjectDn getSubjectDistinguishNames() {
        if (subjectDistinguishNames == null) {
            EndEntityProfile eep = authorizedEndEntityProfiles.getIdMap().get(getEepId()).getValue();
            subjectDistinguishNames = new SubjectDn(eep, raEndEntityDetails.getSubjectDn());
        }
        return subjectDistinguishNames;
    }

    public void setSubjectDistinguishNames(SubjectDn subjectDistinguishNames) {
        this.subjectDistinguishNames = subjectDistinguishNames;
    }

    public SubjectAlternativeName getSubjectAlternativeNames() {
        if (subjectAlternativeNames == null) {
            EndEntityProfile eep = authorizedEndEntityProfiles.getIdMap().get(getEepId()).getValue();
            subjectAlternativeNames = new SubjectAlternativeName(eep, raEndEntityDetails.getSubjectAn());
        }
        return subjectAlternativeNames;
    }

    public void setSubjectAlternativeNames(SubjectAlternativeName subjectAlternativeNames) {
        this.subjectAlternativeNames = subjectAlternativeNames;
    }

    public SubjectDirectoryAttributes getSubjectDirectoryAttributes() {
        if (subjectDirectoryAttributes == null) {
            EndEntityProfile eep = authorizedEndEntityProfiles.getIdMap().get(getEepId()).getValue();
            subjectDirectoryAttributes = new SubjectDirectoryAttributes(eep, raEndEntityDetails.getSubjectDa());
        }
        return subjectDirectoryAttributes;
    }

    public void setSubjectDirectoryAttributes(SubjectDirectoryAttributes subjectDirectoryAttributes) {
        this.subjectDirectoryAttributes = subjectDirectoryAttributes;
    }
}
