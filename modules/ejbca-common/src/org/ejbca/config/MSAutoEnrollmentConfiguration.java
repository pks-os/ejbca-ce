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
package org.ejbca.config;

import org.cesecore.config.MSAutoEnrollmentSettingsTemplate;
import org.cesecore.configuration.ConfigurationBase;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Configuration for the Microsoft Auto Enrollment
 */
public class MSAutoEnrollmentConfiguration extends ConfigurationBase {
    private static final long serialVersionUID = 1L;
    public static final String CONFIGURATION_ID = "MS_AUTO_ENROLLMENT";

    // MSAE Kerberos
    private static final String MSAE_DOMAIN = "msaeDomain";
    private static final String MSAE_KEYTAB_FILENAME = "msaeKeyTabFilename";
    private static final String MSAE_KEYTAB_BYTES = "msaeKeyTabBytes";
    private static final String POLICY_NAME = "policyName";
    private static final String POLICY_UID = "policyUid";
    private static final String SPN = "servicePrincipalName";

    
    // MSAE Krb5Conf
    private static final Object MSAE_KRB5_CONF_BYTES = "msaeKrb5ConfBytes";
    private static final Object MSAE_KRB5_CONF_FILENAME = "msaeKrb5ConfFilename";

    // MSAE Settings
    private static final String IS_USE_SSL = "isUseSSL";
    private static final String AD_CONNECTION_PORT = "adConnectionPort";
    private static final String AD_LOGIN_DN = "adLoginDN";
    private static final String AD_LOGIN_PASSWORD = "adLoginPassword";
    private static final String AUTH_KEY_BINDING = "authKeyBinding";

    // MS Enrollment Servlet Settings
    private static final String CA_NAME = "caName";

    // Template to Settings
    private static final String MS_TEMPLATE_SETTINGS = "msTemplateSettings";


    private static int DEFAULT_AD_CONNECTION_PORT = 389;

    public MSAutoEnrollmentConfiguration() {
        initWithDefaults();
    }

    private void initWithDefaults() {
        // TODO: Set default values.

        // MSAE Kerberos
        setMsaeDomain("");
        setPolicyName("");

        // MSAE
        setIsUseSsl(false);
        setAdConnectionPort(DEFAULT_AD_CONNECTION_PORT);
        setAdLoginDN("");
        setAdLoginPassword("");
        setAuthKeyBinding(null);

        // MS Servlet Settings
        setCaName("");

        setMsTemplateSettings(new ArrayList<>());
    }

    @Override
    public void upgrade() {
        if (Float.compare(LATEST_VERSION, getVersion()) != 0) {
            data.put(VERSION, Float.valueOf(LATEST_VERSION));
        }
    }

    @Override
    public String getConfigurationId() {
        return CONFIGURATION_ID;
    }

    // MSAE Kerberos Settings
    public String getMsaeDomain() {
        return (String) data.get(MSAE_DOMAIN);
    }

    public void setMsaeDomain(final String msaeDomain) {
        data.put(MSAE_DOMAIN, msaeDomain);
    }

    public String getPolicyName() {
        return (String) data.get(POLICY_NAME);
    }

    public void setPolicyName(final String policyName) {
        data.put(POLICY_NAME, policyName);
    }
    
    public String getSpn() {
        return (String) data.get(SPN);
    }

    public void setSpn(final String spn) {
        data.put(SPN, spn);
    }
    
    public String getPolicyUid() {
        return (String) data.get(POLICY_UID);
    }

    public void setPolicyUid() {
        // Only set this once per node.
        if (data.get(POLICY_UID) == null) {
            final String policyUid = "{" + UUID.randomUUID().toString() + "}";
            data.put(POLICY_UID, policyUid);
        }
    }

    public String getMsaeKeyTabFilename() {
        return (String) data.get(MSAE_KEYTAB_FILENAME);
    }

    public void setMsaeKeyTabFilename(final String msaeKeyTabFilename) {
        data.put(MSAE_KEYTAB_FILENAME, msaeKeyTabFilename);
    }

    public byte[] getMsaeKeyTabBytes() {
        return (byte[]) data.get(MSAE_KEYTAB_BYTES);
    }

    public void setMsaeKeyTabBytes(final byte[]  msaeKeyTabBytes) {
        data.put(MSAE_KEYTAB_BYTES, msaeKeyTabBytes);
    }
    
    // MSAE Krb5 Conf
    public String getMsaeKrb5ConfFilename() {
        return (String) data.get(MSAE_KRB5_CONF_FILENAME);
    }

    public void setMsaeKrb5ConfFilename(final String msaeKrb5ConfFilename) {
        data.put(MSAE_KRB5_CONF_FILENAME, msaeKrb5ConfFilename);
    }

    public byte[] getMsaeKrb5ConfBytes() {
        return (byte[]) data.get(MSAE_KRB5_CONF_BYTES);
    }

    public void setMsaeKrb5ConfBytes(final byte[] msaeKrb5ConfBytes) {
        data.put(MSAE_KRB5_CONF_BYTES, msaeKrb5ConfBytes);
    }
    

    // MSAE Settings
    public boolean isUseSSL() {
        return Boolean.TRUE.equals(data.get(IS_USE_SSL));
    }

    public void setIsUseSsl(final boolean isUseSsl) {
        data.put(IS_USE_SSL, isUseSsl);
    }

    public int getADConnectionPort() {
        return (Integer) data.get(AD_CONNECTION_PORT);
    }

    public void setAdConnectionPort(final int port) {
        data.put(AD_CONNECTION_PORT, port);
    }

    public String getAdLoginDN() {
        return (String) data.get(AD_LOGIN_DN);
    }

    public void setAdLoginDN(final String adLoginDN) {
        data.put(AD_LOGIN_DN, adLoginDN);
    }

    public String getAdLoginPassword() {
        return (String) data.get(AD_LOGIN_PASSWORD);
    }

    public void setAdLoginPassword(final String adLoginPassword) {
        data.put(AD_LOGIN_PASSWORD, adLoginPassword);
    }

    public Integer getAuthKeyBinding() {
        return (Integer) data.get(AUTH_KEY_BINDING);
    }

    public void setAuthKeyBinding(final Integer authKeyBinding) {
        data.put(AUTH_KEY_BINDING, authKeyBinding);
    }

    // MS Enrollment Servlet Settings
    public String getCaName() {
        return (String) data.get(CA_NAME);
    }
    public void setCaName(final String caName) {
        data.put(CA_NAME, caName);
    }

    // MS Template Settings
    @SuppressWarnings("unchecked")
    public List<MSAutoEnrollmentSettingsTemplate> getMsTemplateSettings() {
        return (List<MSAutoEnrollmentSettingsTemplate>) data.get(MS_TEMPLATE_SETTINGS);
    }

    public void setMsTemplateSettings(final List<MSAutoEnrollmentSettingsTemplate> msTemplateSettings) {
        data.put(MS_TEMPLATE_SETTINGS, msTemplateSettings);
    }
}
