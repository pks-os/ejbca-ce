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
package org.ejbca.ui.web.admin.configuration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.ejbca.config.EstConfiguration;
import org.ejbca.core.model.authorization.AccessRulesConstants;
import org.ejbca.core.model.ra.UsernameGeneratorParams;
import org.ejbca.ui.web.admin.BaseManagedBean;
import org.ejbca.ui.web.jsf.configuration.EjbcaJSFHelper;

/**
 * Backing bean for edit EST alias view.
 *
 */
@ManagedBean
@ViewScoped
public class EditEstConfigMBean extends BaseManagedBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final List<String> dnfields = Arrays.asList("CN", "UID", "OU", "O", "L", "ST", "DC", "C", "emailAddress", "SN", "givenName", "initials", "surname", "title", 
            "unstructuredAddress", "unstructuredName", "postalCode", "businessCategory", "dnQualifier", "postalAddress", 
            "telephoneNumber", "pseudonym", "streetAddress", "name", "role", "CIF", "NIF");

    private String selectedRaNameSchemeDnPart;

    @ManagedProperty(value = "#{estConfigMBean}")
    private EstConfigMBean estConfigMBean;
    EstAliasGui estAliasGui = null;

    @PostConstruct
    public void initialize() {
        getEjbcaWebBean().clearEstConfigClone();
    }

    public class EstAliasGui {
        private String name;
        private String caId;
        private String endEntityProfileId;
        private String certificateProfileId;
        private Boolean certificateRequired;
        private String userName;
        private String password;
        private Boolean allowSameKey;
        private String extUsernameComponent;
        private String operationmode;
        private String authModule;
        private String vendorCa;
        private String selectedVendorCa;
        private boolean vendorMode;
        private boolean changeSubjectName;
        private String extDnPartPwdComponent;
        
        
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCaId() {
            return caId;
        }

        public void setCaId(String caId) {
            this.caId = caId;
        }

        public String getEndEntityProfileId() {
            return endEntityProfileId;
        }

        public void setEndEntityProfileId(String endEntityProfileId) {
            this.endEntityProfileId = endEntityProfileId;
        }

        public String getCertificateProfileId() {
            return certificateProfileId;
        }

        public void setCertificateProfileId(String certificateProfileId) {
            this.certificateProfileId = certificateProfileId;
        }

        public Boolean getCertificateRequired() {
            return certificateRequired;
        }

        public void setCertificateRequired(Boolean certificateRequired) {
            this.certificateRequired = certificateRequired;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public Boolean getAllowSameKey() {
            return allowSameKey;
        }

        public void setAllowSameKey(Boolean allowSameKey) {
            this.allowSameKey = allowSameKey;
        }
        
        public String getOperationMode() {
            return operationmode;
        }
        
        public void setOperationMode(String operationmode) {
            this.operationmode = operationmode;
        }
        
        public void setVendorMode(boolean vendorMode) {
            this.vendorMode = vendorMode;
        }
        
        public  boolean getVendorMode() {
            return this.vendorMode;
        }
        
        public void setChangeSubjectName(boolean changeSubjectName) {
            this.changeSubjectName = changeSubjectName;
        }
        
        public  boolean getChangeSubjectName() {
            return this.changeSubjectName;
        }
        
        public void setSelectedVendorCa(String selectedVendorCa) {
            this.selectedVendorCa = selectedVendorCa;
        }
        
        public  String getSelectedVendorCa() {
            return selectedVendorCa == null ? "" : selectedVendorCa;
        }
        
        public void setVendorCa(String vendorCa) {
            this.vendorCa = vendorCa;
        }
        
        
        public  String getVendorCa() {
            return vendorCa == null ? "" : vendorCa;
        }
        
        public void setAuthenticationModule(final String authModule) {
            this.authModule = authModule;
        }
        
        public String getAuthenticationModule() {
            return this.authModule;
        }
        
        public String getExtDnPartPwdComponent() {
            return extDnPartPwdComponent;
        }
        
        public void setExtDnPartPwdComponent(String extDnPartPwdComponent) {
            this.extDnPartPwdComponent = extDnPartPwdComponent;
        }
        
        public boolean getDnPartPwdSelected() {
            if (getAuthenticationModule().equals(EstConfiguration.CONFIG_AUTHMODULE_DN_PART_PWD)) {
                return true;  
            }
            return false;
        }
        
        public void setDnPartPwdSelected(boolean dnPartPwdSelected) {
            if (dnPartPwdSelected) {
                setAuthenticationModule(EstConfiguration.CONFIG_AUTHMODULE_DN_PART_PWD);
            } else if (getAuthenticationModule().equals(EstConfiguration.CONFIG_AUTHMODULE_DN_PART_PWD)) {
                setAuthenticationModule("");
            }
        }
        
        
        public boolean getChallengePwdSelected() {
            if (getAuthenticationModule().equals(EstConfiguration.CONFIG_AUTHMODULE_CHALLENGE_PWD)) {
                return true;  
            }
            return false;
        }
        
        public void setChallengePwdSelected(boolean challengePwdSelected) {
            if (challengePwdSelected) {
                setAuthenticationModule(EstConfiguration.CONFIG_AUTHMODULE_CHALLENGE_PWD);
            } else if (getAuthenticationModule().equals(EstConfiguration.CONFIG_AUTHMODULE_CHALLENGE_PWD)) {
                setAuthenticationModule("");
            }
        }
        
        public void setExtUsernameComponent(String extUsernameComponent) {
            this.extUsernameComponent = extUsernameComponent;
        }
        
        public String getExtUsernameComponent() {
            return extUsernameComponent;
        }
    }

    public EstAliasGui getEstAlias() {
        if (estAliasGui == null) {
            EstAliasGui estAliasGui = new EstAliasGui();
            String aliasName = estConfigMBean.getSelectedAlias();
            estAliasGui.setName(aliasName);
            EstConfiguration estConfiguration = getEjbcaWebBean().getEstConfiguration();
            estAliasGui.setCaId(estConfiguration.getDefaultCAID(aliasName));
            estAliasGui.setEndEntityProfileId(String.valueOf(estConfiguration.getEndEntityProfileID(aliasName)));
            String certProfileID = estConfiguration.getCertProfileID(aliasName);
            // If we had the old type, EJBCA 6.11 of CP, which is the name, convert it to ID
            if (!NumberUtils.isNumber(certProfileID)) {
                Map<String, Integer> certificateProfiles = getEjbcaWebBean().getCertificateProfilesNoKeyId(estAliasGui.getEndEntityProfileId());
                if (certificateProfiles.get(certProfileID) != null) {
                    certProfileID = String.valueOf(certificateProfiles.get(certProfileID));
                }
            }
            estAliasGui.setCertificateProfileId(certProfileID);
            estAliasGui.setCertificateRequired(estConfiguration.getCert(aliasName));
            estAliasGui.setUserName(estConfiguration.getUsername(aliasName));
            estAliasGui.setPassword(estConfiguration.getPassword(aliasName));
            estAliasGui.setAllowSameKey(estConfiguration.getKurAllowSameKey(aliasName));
            estAliasGui.setExtUsernameComponent(estConfiguration.getExtractUsernameComponent(aliasName));
            estAliasGui.setOperationMode(estConfiguration.getRAMode(aliasName));
            estAliasGui.setVendorMode(estConfiguration.getVendorMode(aliasName));
            estAliasGui.setSelectedVendorCa(estConfiguration.getVendorCA(aliasName));
            estAliasGui.setAuthenticationModule(estConfiguration.getAuthenticationModule(aliasName));
            estAliasGui.setChallengePwdSelected(estConfiguration.getAuthenticationModule(aliasName).equals(EstConfiguration.CONFIG_AUTHMODULE_CHALLENGE_PWD));
            estAliasGui.setDnPartPwdSelected(estConfiguration.getAuthenticationModule(aliasName).equals(EstConfiguration.CONFIG_AUTHMODULE_DN_PART_PWD));
            estAliasGui.setExtDnPartPwdComponent(estConfiguration.getExtractDnPwdComponent(aliasName));
            estAliasGui.setChangeSubjectName(estConfiguration.getChangeSubjectName(aliasName));
            estAliasGui.setVendorCa(estConfiguration.getVendorCA(aliasName));
            this.estAliasGui = estAliasGui;
        }
        return estAliasGui;
    }


    public boolean isViewOnly() {
        return estConfigMBean.isViewOnly();
    }
    
    public boolean isRaMode() {
        return estAliasGui.getOperationMode().equals("ra");
    }
    
    public boolean isVendorMode() {
       return estAliasGui.getVendorMode();
    }
       
    public List<SelectItem> getExtUsernameComponentSelectItems() {
        final List<SelectItem> selectItems = getDnFieldSelectItems(); 
        selectItems.add(0, new SelectItem("DN"));
        return selectItems;
    }
    
    public List<SelectItem> getCaItemList() {
        final List<SelectItem> ret = new ArrayList<>();
        if (StringUtils.isEmpty(getEstAlias().getCaId())) {
            ret.add(new SelectItem("", EjbcaJSFHelper.getBean().getText().get("ESTDEFAULTCA_DISABLED")));
        }
        Map<String, Integer> canames = getEjbcaWebBean().getCANames();
        for (String caname : canames.keySet()) {
            final Integer cadi = canames.get(caname);
            ret.add(new SelectItem(cadi, caname));
        }
        return ret;
    }

    public List<SelectItem> getEndEntityProfileItemList() {
        final List<SelectItem> ret = new ArrayList<>();
        Map<String, String> nameToIdMap = getEjbcaWebBean().getAuthorizedEEProfilesAndIdsNoKeyId(AccessRulesConstants.CREATE_END_ENTITY);
        for (String endEntityProfileName : nameToIdMap.keySet()) {
            String endEntityProfileId = nameToIdMap.get(endEntityProfileName);
            ret.add(new SelectItem(endEntityProfileId, endEntityProfileName));
        }
        return ret;
    }

    public List<SelectItem> getCertificateProfileItemList() {
        final List<SelectItem> ret = new ArrayList<>();
        if (estAliasGui.getEndEntityProfileId() != null) {
            Map<String, Integer> certificateProfiles = getEjbcaWebBean().getCertificateProfilesNoKeyId(estAliasGui.getEndEntityProfileId());
            for (String certificateProfileName : certificateProfiles.keySet()) {
                int certificateProfileId = certificateProfiles.get(certificateProfileName);
                ret.add(new SelectItem(certificateProfileId, certificateProfileName));
            }
        }
        return ret;
    }

    public String cancel() {
        reset();
        return "done";
    }

    public String save() throws AuthorizationDeniedException {
        String alias = estAliasGui.getName();
        EstConfiguration estConfiguration = getEjbcaWebBean().getEstConfigForEdit(alias);
        if (StringUtils.isEmpty(estAliasGui.getCaId())) {
            estConfiguration.setDefaultCAID(alias);
        } else {
            estConfiguration.setDefaultCAID(alias, Integer.valueOf(estAliasGui.getCaId()));
        }
        if (estAliasGui.getEndEntityProfileId() != null) {
            estConfiguration.setEndEntityProfileID(alias, Integer.valueOf(estAliasGui.getEndEntityProfileId()));
        }
        if (estAliasGui.getCertificateProfileId() != null) {
            estConfiguration.setCertProfileID(alias, Integer.valueOf(estAliasGui.getCertificateProfileId()));
        }
        estConfiguration.setCert(alias, estAliasGui.getCertificateRequired());
        estConfiguration.setUsername(alias, estAliasGui.getUserName());
        estConfiguration.setPassword(alias, estAliasGui.getPassword());
        estConfiguration.setKurAllowSameKey(alias, estAliasGui.getAllowSameKey());
        estConfiguration.setExtractUsernameComponent(alias, estAliasGui.getExtUsernameComponent());
        estConfiguration.setExtractDnPwdComponent(alias, estAliasGui.getExtDnPartPwdComponent());
        estConfiguration.setRAMode(alias, estAliasGui.getOperationMode());
        estConfiguration.setVendorMode(alias, estAliasGui.getVendorMode());
        estConfiguration.setVendorCA(alias, estAliasGui.getSelectedVendorCa());
        estConfiguration.setAuthenticationModule(alias, estAliasGui.getAuthenticationModule());
        estConfiguration.setChangeSubjectName(alias, estAliasGui.getChangeSubjectName());
        estConfiguration.setVendorCA(alias, getCurrentVendorCa());
        getEjbcaWebBean().updateEstConfigFromClone(alias);
        reset();
        return "done";
    }
    
    
    public void actionAddVendorCa() {
        final String currentVendorCas = getCurrentVendorCa();
        List<String> currentVendorCaList = new ArrayList<>();
        if (StringUtils.isNotBlank(currentVendorCas)) {
            currentVendorCaList = new ArrayList<>(Arrays.asList( currentVendorCas.split(";"))); 
        }
        if (!currentVendorCaList.contains(estAliasGui.getSelectedVendorCa())) {
            currentVendorCaList.add(estAliasGui.getSelectedVendorCa());
        }
        setCurrentVendorCa(StringUtils.join(currentVendorCaList, ";"));
    }
    
    public void actionRemoveVendorCa() {
        final String currentVendorCas = getCurrentVendorCa();
        if (StringUtils.isNotBlank(currentVendorCas)) {
            final List<String> currentVendorCaList = new ArrayList<>(Arrays.asList( currentVendorCas.split(";")));
            if (currentVendorCaList.remove(estAliasGui.getSelectedVendorCa())) {
                setCurrentVendorCa(StringUtils.join(currentVendorCaList, ";"));
            }
        }
    }
    
    public List<SelectItem> getVendorCaSelectItems() {
        final List<SelectItem> selectItems = new ArrayList<>();
        final TreeMap<String, Integer> caOptions = getEjbcaWebBean().getCAOptions();
        for (String ca : caOptions.keySet()) {
            selectItems.add(new SelectItem(ca));
        }
        return selectItems;
    }
    
    private String vendorCas;
    
    public String getCurrentVendorCa() {
        return vendorCas == null? estAliasGui.getVendorCa(): vendorCas;
    }
    
    public void setCurrentVendorCa(String vendorCas) {
        this.vendorCas = vendorCas;
    }
           
    /**
     * Add DN field to name generation parameter
     *
     */
    public void actionAddRaNameSchemeDnPart() {
    	String alias = estAliasGui.getName();
    	EstConfiguration estConfiguration = getEjbcaWebBean().getEstConfigForEdit(alias);
        String currentNameGenParam = estConfiguration.getRANameGenParams(getSelectedEstAlias());
        String[] params = currentNameGenParam == null ? new String[0] : currentNameGenParam.split(";");
        // Verify that current param is instance of DN fields
        if((params.length > 0) && ( dnfields.contains(params[0]) )) {
            if(!ArrayUtils.contains(params, getSelectedRaNameSchemeDnPart())) {
                currentNameGenParam += ";" + getSelectedRaNameSchemeDnPart();
            }
        } else {
                currentNameGenParam = getSelectedRaNameSchemeDnPart();
        }
        estConfiguration.setRANameGenParams(getSelectedEstAlias(), currentNameGenParam);
    }

    /**
     * Remove DN field from name generation parameter
     *
     */    
    public void actionRemoveRaNameSchemeDnPart() {
    	String alias = estAliasGui.getName();
    	EstConfiguration estConfiguration = getEjbcaWebBean().getEstConfigForEdit(alias);
        String currentNameGenParam = estConfiguration.getRANameGenParams(getSelectedEstAlias());
        if(StringUtils.contains(currentNameGenParam, getSelectedRaNameSchemeDnPart())) {
            String[] params = currentNameGenParam.split(";");
            if(params.length == 1) {
                currentNameGenParam = "";
            } else {
                if(StringUtils.equals(params[0], getSelectedRaNameSchemeDnPart())) {
                    currentNameGenParam = StringUtils.remove(currentNameGenParam, getSelectedRaNameSchemeDnPart() + ";");
                } else {
                    currentNameGenParam = StringUtils.remove(currentNameGenParam, ";" + getSelectedRaNameSchemeDnPart());
                }
            }
            estConfiguration.setRANameGenParams(getSelectedEstAlias(), currentNameGenParam);
        }
    }

    /**
     * Set the name generation scheme: DN/RANDOM/FIXED/USERNAME
     *
     */ 
    public void setRaNameGenScheme(final String scheme) {
    	String alias = estAliasGui.getName();
    	EstConfiguration estConfiguration = getEjbcaWebBean().getEstConfigForEdit(alias);
        estConfiguration.setRANameGenScheme(getSelectedEstAlias(), scheme);
    }
    
    /**
     * Get the name generation scheme: DN/RANDOM/FIXED/USERNAME
     *
     */     
    public String getRaNameGenScheme() {
    	String alias = estAliasGui.getName();
    	EstConfiguration estConfiguration = getEjbcaWebBean().getEstConfigForEdit(alias);
        return estConfiguration.getRANameGenScheme(getSelectedEstAlias());
    }    

    public String getSelectedEstAlias() {
        return estConfigMBean.getSelectedAlias();
    }

    /**
     * Get the available RA name generation schemes for radio buttons
     *
     */ 
    public List<SelectItem> getAvailableRaNameGenSchemes() {
        List<SelectItem> selectItems = new ArrayList<>();
        selectItems.add(new SelectItem(UsernameGeneratorParams.DN));
        selectItems.add(new SelectItem(UsernameGeneratorParams.RANDOM));
        selectItems.add(new SelectItem(UsernameGeneratorParams.FIXED));
        selectItems.add(new SelectItem(UsernameGeneratorParams.USERNAME));
        return selectItems;
    }

    /**
     * Set the name generation parameters
     * Semicolon delimited DN Fields
     *
     */ 
    public void setRaNameGenParams(final String params) {
    	String alias = estAliasGui.getName();
    	EstConfiguration estConfiguration = getEjbcaWebBean().getEstConfigForEdit(alias);
        estConfiguration.setRANameGenParams(getSelectedEstAlias(), params);
    }

    /**
     * Get the name generation parameters
     * Semicolon delimited DN Fields
     *
     */     
    public String getRaNameGenParams() {
    	String alias = estAliasGui.getName();
    	EstConfiguration estConfiguration = getEjbcaWebBean().getEstConfigForEdit(alias);
        return estConfiguration.getRANameGenParams(getSelectedEstAlias());
    }

    /**
     * Get the selected name generation DN part for addition or removal
     *
     */ 
    public String getSelectedRaNameSchemeDnPart() {
        return selectedRaNameSchemeDnPart == null ? dnfields.get(0) : selectedRaNameSchemeDnPart;
    }

    /**
     * Set the selected name generation DN part for addition or removal
     *
     */ 
    public void setSelectedRaNameSchemeDnPart(final String selectedRaNameSchemeDnPart) {
        this.selectedRaNameSchemeDnPart = selectedRaNameSchemeDnPart;
    }

    /**
     * Get the DN field select items. Full list of available DN fields.
     *
     */ 
    public List<SelectItem> getDnFieldSelectItems() {
        final List<SelectItem> selectItems = new ArrayList<>();
        for (String  dnField : dnfields) {
            selectItems.add(new SelectItem(dnField));
        }
        return selectItems;
    }

    /**
     * Set the RA name generation prefix
     *
     */ 
    public void setRaNameGenPrefix(final String prefix) {
        String alias = estAliasGui.getName();
        EstConfiguration estConfiguration = getEjbcaWebBean().getEstConfigForEdit(alias);
        estConfiguration.setRANameGenPrefix(getSelectedEstAlias(), prefix);
    }

    /**
     * Get the RA name generation prefix
     *
     */     
    public String getRaNameGenPrefix() {
        String alias = estAliasGui.getName();
        EstConfiguration estConfiguration = getEjbcaWebBean().getEstConfigForEdit(alias);
        return estConfiguration.getRANameGenPrefix(getSelectedEstAlias());
    }

    /**
     * Set the RA name generation postfix
     *
     */ 
    public void setRaNameGenPostfix(final String prefix) {
        String alias = estAliasGui.getName();
        EstConfiguration estConfiguration = getEjbcaWebBean().getEstConfigForEdit(alias);
        estConfiguration.setRANameGenPostfix(getSelectedEstAlias(), prefix);
    }

    /**
     * Get the RA name generation postfix
     *
     */     
    public String getRaNameGenPostfix() {
        String alias = estAliasGui.getName();
        EstConfiguration estConfiguration = getEjbcaWebBean().getEstConfigForEdit(alias);
        return estConfiguration.getRANameGenPostfix(getSelectedEstAlias());
    }

    private void reset() {
        estAliasGui = null;
        getEjbcaWebBean().clearEstConfigClone();
        estConfigMBean.actionCancel();
    }

    public EstConfigMBean getEstConfigMBean() {
        return estConfigMBean;
    }

    public void setEstConfigMBean(EstConfigMBean estConfigMBean) {
        this.estConfigMBean = estConfigMBean;
    }
}