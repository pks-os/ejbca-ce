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

package org.ejbca.ui.web.pub;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.cesecore.authentication.tokens.AlwaysAllowLocalAuthenticationToken;
import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authentication.tokens.UsernamePrincipal;
import org.cesecore.certificates.certificateprofile.CertificateProfileSessionLocal;
import org.cesecore.certificates.endentity.EndEntityConstants;
import org.cesecore.certificates.endentity.EndEntityInformation;
import org.cesecore.certificates.endentity.EndEntityType;
import org.cesecore.certificates.endentity.EndEntityTypes;
import org.cesecore.certificates.endentity.ExtendedInformation;
import org.cesecore.certificates.util.DNFieldExtractor;
import org.ejbca.config.EjbcaConfigurationHolder;
import org.ejbca.config.GlobalConfiguration;
import org.ejbca.core.EjbcaException;
import org.ejbca.core.ejb.approval.ApprovalSessionLocal;
import org.ejbca.core.ejb.ra.EndEntityManagementSessionLocal;
import org.ejbca.core.ejb.ra.raadmin.EndEntityProfileSessionLocal;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.approval.approvalrequests.AddEndEntityApprovalRequest;
import org.ejbca.core.model.ra.raadmin.EndEntityProfile;
import org.ejbca.core.model.ra.raadmin.EndEntityProfileNotFoundException;
import org.ejbca.core.model.ra.raadmin.UserDoesntFullfillEndEntityProfile;
import org.ejbca.core.model.util.EjbLocalHelper;
import org.ejbca.util.DNFieldDescriber;

/**
 * Used by enrol/reg*.jsp for self-registration. This bean implements
 * implements listing of certificate types (defined in web.properties),
 * listing of modifiable end-entity fields and submission of requests.
 * 
 * @version $Id$
 */
public class RegisterReqBean {
    
    private static final Logger log = Logger.getLogger(RegisterReqBean.class);
    
    
    private final EjbLocalHelper ejbLocalHelper = new EjbLocalHelper();
    private final EndEntityProfileSessionLocal endEntityProfileSession = ejbLocalHelper.getEndEntityProfileSession();
    private final CertificateProfileSessionLocal certificateProfileSession = ejbLocalHelper.getCertificateProfileSession();
    private final EndEntityManagementSessionLocal endEntityManagementSession = ejbLocalHelper.getEndEntityManagementSession();
    private final ApprovalSessionLocal approvalSession = ejbLocalHelper.getApprovalSession();
    private final GlobalConfiguration globalConfiguration = (GlobalConfiguration) ejbLocalHelper.getGlobalConfigurationSession().getCachedConfiguration(GlobalConfiguration.GLOBAL_CONFIGURATION_ID);

    // Form fields
    private final Map<String,String> formDNFields = new HashMap<String,String>();
    private String subjectAltName = "";
    private String subjectDirAttrs = "";
    
    private String certType;
    private EndEntityProfile eeprofile; // of cert type
    
    private String username;
    private String email;
    private int tokenType;
    private String captcha;
    
    // Form errors
    private final List<String> errors = new ArrayList<String>();
    private boolean initialized = false;
    private String remoteAddress;
    
    /**
     * Finds all properties matching web.selfreg.certtypes.KEY.description=VALUE
     * and returns a map with these keys and values.
     */
    public Map<String,String> getCertificateTypes() {
        Map<String,String> certtypes = new HashMap<String,String>();
        for (Entry<Object,Object> entry : EjbcaConfigurationHolder.getAsProperties().entrySet()) {
            final Object k = entry.getKey();
            final Object v = entry.getValue();
            if (k instanceof String && v instanceof String) {
                String key = (String)k;
                if (key.matches("web\\.selfreg\\.certtypes\\.([^.]+)\\.description")) {
                    certtypes.put(key.split("\\.")[3], (String)v);
                }
            }
        }
        return certtypes;
    }
    
    /**
     * Reads config property web.selfreg.certtypes.CERTTYPE.xxxxx from web.xml
     */
    private String getCertTypeInfo(String certType, String subproperty) {
        String key = "web.selfreg.certtypes."+certType+"."+subproperty;
        String value = EjbcaConfigurationHolder.getString(key);
        if (value == null) {
            internalError("Configuration property "+key+" not defined");
        }
        return value;
    }
    
    public String getCertType() {
        return certType;
    }
    
    public String getCertTypeDescription() {
        return getCertTypeInfo(certType, "description");
    }
    
    public int getEndEntityProfileId() throws EndEntityProfileNotFoundException {
        return endEntityProfileSession.getEndEntityProfileId(getCertTypeInfo(certType, "eeprofile"));
    }
    
    public EndEntityProfile getEndEntityProfile() {
        return endEntityProfileSession.getEndEntityProfile(getCertTypeInfo(certType, "eeprofile"));
    }
    
    public int getCertificateProfileId() {
        return certificateProfileSession.getCertificateProfileId(getCertTypeInfo(certType, "certprofile"));
    }
    
    public String getUsernameMapping() {
        String um = EjbcaConfigurationHolder.getString("web.selfreg.certtypes."+certType+".usernamemapping");
        return um != null ? um.toLowerCase(Locale.ROOT) : null; 
    }
    
    public String getDefaultCertType() {
        String s = EjbcaConfigurationHolder.getString("web.selfreg.defaultcerttype");
        return (s != null ? s : "1");
    }
    
    /**
     * Returns a list of all certificate DN fields in the
     * end-entity profile of the given certtype.
     */
    public List<DNFieldDescriber> getDnFields() {
        List<DNFieldDescriber> fields = new ArrayList<DNFieldDescriber>();
        
        int numberofsubjectdnfields = eeprofile.getSubjectDNFieldOrderLength();
        for (int i=0; i < numberofsubjectdnfields; i++) {
            int[] fielddata = eeprofile.getSubjectDNFieldsInOrder(i);
            fields.add(new DNFieldDescriber(i, fielddata, eeprofile, DNFieldExtractor.TYPE_SUBJECTDN));
        }
        
        return fields;
    }
    
    public List<DNFieldDescriber> getAltNameFields() {
        List<DNFieldDescriber> fields = new ArrayList<DNFieldDescriber>();
        
        int numberofaltnamefields = eeprofile.getSubjectAltNameFieldOrderLength();
        for (int i=0; i < numberofaltnamefields; i++) {
            int[] fielddata = eeprofile.getSubjectAltNameFieldsInOrder(i);
            fields.add(new DNFieldDescriber(i, fielddata, eeprofile, DNFieldExtractor.TYPE_SUBJECTALTNAME));
        }
        
        return fields;
    }
    
    public List<DNFieldDescriber> getDirAttrFields() {
        List<DNFieldDescriber> fields = new ArrayList<DNFieldDescriber>();
        
        int count = eeprofile.getSubjectDirAttrFieldOrderLength();
        for (int i=0; i < count; i++) {
            int[] fielddata = eeprofile.getSubjectDirAttrFieldsInOrder(i);
            fields.add(new DNFieldDescriber(i, fielddata, eeprofile, DNFieldExtractor.TYPE_SUBJECTDIRATTR));
        }
        
        return fields;
    }
    
    public boolean isEmailDomainFrozen() {
        if (eeprofile.isModifyable(EndEntityProfile.EMAIL, 0)) return false;
        String value = eeprofile.getValue(EndEntityProfile.EMAIL, 0);
        return !value.contains(";");
    }
    
    public boolean isEmailDomainSelectable() {
        if (eeprofile.isModifyable(EndEntityProfile.EMAIL, 0)) return false;
        String value = eeprofile.getValue(EndEntityProfile.EMAIL, 0);
        return value.contains(";");
    }
    
    public String[] getSelectableEmailDomains() {
        String value = eeprofile.getValue(EndEntityProfile.EMAIL, 0);
        return value.trim().split(";");
    }
    
    private String[] getAvailableTokenTypes() {
        String[] tokenTypes = eeprofile.getValue(EndEntityProfile.AVAILKEYSTORE, 0).split(EndEntityProfile.SPLITCHAR).clone();
        Arrays.sort(tokenTypes);
        return tokenTypes;
    }
    
    public boolean isTokenTypeVisible() {
        return getAvailableTokenTypes().length >= 2;
    }
    
    public String getDefaultTokenType() {
        return eeprofile.getValue(EndEntityProfile.DEFKEYSTORE, 0);
    }
    
    private String getTokenTypeName(String idString) {
        int id = Integer.parseInt(idString);
        switch (id) {
        case SecConst.TOKEN_SOFT_BROWSERGEN: return "User generated";
        case SecConst.TOKEN_SOFT_P12: return "PKCS12 file";
        case SecConst.TOKEN_SOFT_PEM: return "PEM file";
        case SecConst.TOKEN_SOFT_JKS: return "JKS file";
        default: return idString;
        }
    }
    
    public static class TokenTypeInfo {
        private final String key;
        private final String text;
        TokenTypeInfo(String key, String text) {
            this.key = key;
            this.text = text;
        }
        public String getKey() { return key; }
        public String getText() { return text; }
    }
    
    public List<TokenTypeInfo> getSelectableTokenTypeItems() {
        List<TokenTypeInfo> items = new ArrayList<TokenTypeInfo>();
        for (String keystore : getAvailableTokenTypes()) {
            items.add(new TokenTypeInfo(keystore, getTokenTypeName(keystore)));
        }
        return items;
    }
    
    public boolean isUsernameVisible() {
        return getUsernameMapping() == null;
    }
    
    private void checkCertEEProfilesExist() {
        String eeprofName = getCertTypeInfo(certType, "eeprofile");
        if (eeprofName != null && endEntityProfileSession.getEndEntityProfile(eeprofName) == null) {
            internalError("End entity profile "+eeprofName+" does not exist. Check web.selfreg.certtypes."+certType+".eeprofile configuration");
        }
        
        String certprofName = getCertTypeInfo(certType, "certprofile");
        if (certprofName != null && certificateProfileSession.getCertificateProfile(certprofName) == null) {
            internalError("Certificate profile "+certprofName+" does not exist. Check web.selfreg.certtypes."+certType+".certprofile configuration");
        }
    }
    
    public void checkConfig() {
        String s = EjbcaConfigurationHolder.getString("web.selfreg.defaultcerttype");
        if (s != null && getCertTypeInfo(s, "description") == null) {
            internalError("Please check the default certificate type. It is configured by web.selfreg.defaultcerttype.");
        }
    }
    
    public void initialize(final HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            internalError("Internal error: Invalid request method.");
        }
        
        certType = request.getParameter("certType");
        
        checkConfig();
        checkCertEEProfilesExist();
        eeprofile = getEndEntityProfile();
        String usernameMapping = getUsernameMapping();

        // Get all fields
        @SuppressWarnings("rawtypes")
        Enumeration en = request.getParameterNames();
        while (en.hasMoreElements()) {
            String key = (String)en.nextElement();
            String value = request.getParameter(key).trim();
            
            String id = key.replaceFirst("^[a-z]+_", ""); // format is e.g. dnfield_cn, altnamefield_123 or dirattrfield_123 
            if (key.startsWith("dnfield_")) {
                if (!value.isEmpty()) {
                    String dnName = DNFieldDescriber.extractSubjectDnNameFromId(eeprofile, id);
                    formDNFields.put(dnName, value);
                }
            }
            
            if (key.startsWith("altnamefield_")) {
                if (!value.isEmpty()) {
                    String altName = DNFieldDescriber.extractSubjectAltNameFromId(eeprofile, id);
                    String field = org.ietf.ldap.LDAPDN.escapeRDN(altName + "=" + value);
                    
                    if (subjectAltName.isEmpty()) {
                        subjectAltName = field;
                    } else {
                        subjectAltName += ", " + field;
                    }
                }
            }
            
            if (key.startsWith("dirattrfield_")) {
                if (!value.isEmpty()) {
                    String dirAttr = DNFieldDescriber.extractSubjectDirAttrFromId(eeprofile, id);
                    String field = org.ietf.ldap.LDAPDN.escapeRDN(dirAttr + "=" + value);
                    
                    if (subjectDirAttrs.isEmpty()) {
                        subjectDirAttrs = field;
                    } else {
                        subjectDirAttrs += ", " + field;
                    }
                }
            }
        }
        
        // User account
        if (isUsernameVisible()) {
            username = request.getParameter("username");
        } else {
            username = formDNFields.get(usernameMapping);
            if (!formDNFields.isEmpty() && username == null) {
                internalError("DN field of usernamemapping doesn't exist: "+usernameMapping);
            }
        }
        
        email = request.getParameter("email");
        String domain = request.getParameter("emaildomain");
        if (domain != null && !email.isEmpty()) email += "@" + domain;
        captcha = request.getParameter("code");
        
        String tokenStr = request.getParameter("tokenType");
        tokenType = Integer.parseInt(tokenStr != null ? tokenStr : getDefaultTokenType());
        if ("1".equals(request.getParameter("emailindn"))) {
            formDNFields.put("e", email);
        }
        
        if (request.getParameter("emailinaltname") != null) {
            String id = request.getParameter("emailinaltname");
            String altName = DNFieldDescriber.extractSubjectAltNameFromId(eeprofile, id);
            String field = org.ietf.ldap.LDAPDN.escapeRDN(altName + "=" + email);
            
            if (subjectAltName.isEmpty()) {
                subjectAltName = field;
            } else {
                subjectAltName += ", " + field;
            }
        }
        
        remoteAddress = request.getRemoteAddr();
        initialized = true;
    }
    
    private void checkFormFields() {
        boolean cantDoCaptcha = false;
        
        if (certType == null || certType.isEmpty()) {
            errors.add("Certificate type is not specified.");
        }
        
        // User account
        if (username == null || username.isEmpty()) {
            errors.add("Username is not specified.");
            if (isUsernameVisible()) cantDoCaptcha = true;
        }
        
        if (email == null || !email.matches("[^@]+@.+")) {
            errors.add("E-mail is not specified.");
            if (!isUsernameVisible()) cantDoCaptcha = true;
        }
        
        // Token type
        if (!ArrayUtils.contains(getAvailableTokenTypes(), String.valueOf(tokenType))) {
            errors.add("Token type is not allowed by end-entity profile.");
        }

        String captchaField = isUsernameVisible() ? username : email;
        
        // The captcha simply is the last character of the name
        if (!cantDoCaptcha && (captcha == null || !captcha.equalsIgnoreCase(captchaField.substring(captchaField.length()-1)))) {
            errors.add("Captcha code is incorrect.");
        }
    }
    
    /**
     * Returns a list of errors to be displayed by the .jsp
     */
    public List<String> getErrors() {
        return new ArrayList<String>(errors);
    }
    
    /**
     * Adds and logs an internal or configuration error.
     */
    public void internalError(String message) {
        errors.add(message);
        log.info(message);
    }
    
    private String getSubjectDN() {
        boolean first = true;
        StringBuilder sb = new StringBuilder();
        for (Entry<String,String> field : formDNFields.entrySet()) {
            if (first) { first = false; } 
            else { sb.append(", "); }
            
            sb.append(org.ietf.ldap.LDAPDN.escapeRDN(field.getKey().toUpperCase(Locale.ROOT) + "=" + field.getValue()));
        }
        return sb.toString();
    }
    
    private void assignDirAttrs(EndEntityInformation endEntity) {
        ExtendedInformation ext = endEntity.getExtendedinformation();
        if (ext == null) {
            ext = new ExtendedInformation();
        }
        ext.setSubjectDirectoryAttributes(subjectDirAttrs);
        endEntity.setExtendedinformation(ext);
    }
    
    /**
     * Creates a approval request from the given information in the form.
     * initialize() must have been called before this method is called.  
     */
    public void submit() {
        if (!initialized) {
            throw new IllegalStateException("initialize not called before submit");
        }
        
        // Set up config for admingui (e.g. for e-mails to admins with links to it)
        // This should be OK to do here and to do per request, since it just
        // sets up some hard-coded config strings, etc.
        globalConfiguration.initializeAdminWeb();
        
        checkFormFields();
        
        if (!errors.isEmpty()) {
            return;
        }
        
        int eeProfileId;
        try {
            eeProfileId = getEndEntityProfileId();
        } catch (EndEntityProfileNotFoundException e) {
            errors.add("Validation error: "+e.getMessage());
            return;
        }
        final EndEntityProfile eeprofile = endEntityProfileSession.getEndEntityProfile(eeProfileId);
        final int caid = eeprofile.getDefaultCA();
        if (caid == -1) {
            internalError("The end-entity profile "+getCertTypeInfo(certType, "eeprofile")+" for cert type "+certType+" does not have any default CA.");
        }
        
        final int certProfileId = getCertificateProfileId();
        
        if (endEntityManagementSession.existsUser(username)) {
            errors.add("A user with that name exists already");
        }
        
        if (!errors.isEmpty()) {
            return;
        }
        
        final String subjectDN = getSubjectDN();
        final int numApprovalsRequired = 1;
        final AuthenticationToken admin = new AlwaysAllowLocalAuthenticationToken(new UsernamePrincipal("Public web registration, from IP "+remoteAddress));
        
        final EndEntityInformation endEntity = new EndEntityInformation(username, subjectDN, caid, subjectAltName, 
                null, EndEntityConstants.STATUS_NEW, new EndEntityType(EndEntityTypes.ENDUSER), eeProfileId, certProfileId,
                null,null, tokenType, 0, null);
        if (eeprofile.getUse(EndEntityProfile.SENDNOTIFICATION, 0)) {
            endEntity.setSendNotification(true);
        }
        assignDirAttrs(endEntity);
        if (email != null) {
            endEntity.setEmail(email);
        }
        
        try {
            endEntityManagementSession.canonicalizeUser(endEntity);
            if (globalConfiguration.getEnableEndEntityProfileLimitations()) {
                eeprofile.doesUserFullfillEndEntityProfile(endEntity, false);
                
            }
        } catch (EjbcaException e) {
            errors.add("Validation error: "+e.getMessage());
            return;
        } catch (UserDoesntFullfillEndEntityProfile e) {
            errors.add("User information does not fulfill requirements: "+e.getMessage());
            return;
        }
        
        // Add approval request
        final AddEndEntityApprovalRequest approvalReq = new AddEndEntityApprovalRequest(endEntity,
                false, admin, null, numApprovalsRequired, caid, eeProfileId);
        
        try {
            approvalSession.addApprovalRequest(admin, approvalReq);
        } catch (EjbcaException e) {
            errors.add("Could not submit the information for approval: "+e.getMessage());
            log.info("Approval request could not be added", e);
        }
    }
    
}



