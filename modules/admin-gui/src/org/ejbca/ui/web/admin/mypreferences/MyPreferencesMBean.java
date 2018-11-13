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
package org.ejbca.ui.web.admin.mypreferences;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;

import org.ejbca.core.model.authorization.AccessRulesConstants;
import org.ejbca.core.model.ra.raadmin.AdminPreference;
import org.ejbca.ui.web.admin.BaseManagedBean;
import org.ejbca.ui.web.admin.configuration.AdminDoesntExistException;
import org.ejbca.ui.web.admin.configuration.AdminExistsException;
import org.ejbca.ui.web.admin.configuration.WebLanguage;

/**
 * JavaServer Faces Managed Bean for managing MyPreferences.
 * Session scoped and will cache the user preferences.
 *
 * @version $Id: MyPreferencesMBean.java 30173 2018-10-24 14:26:46Z tarmor $
 */
public class MyPreferencesMBean extends BaseManagedBean implements Serializable {

    private static final long serialVersionUID = 2L;
    //private static final Logger log = Logger.getLogger(MyPreferencesMBean.class);

    private AdminPreference adminPreference;
    
    List<SelectItem> availableLanguages;
    List<SelectItem> availableThemes;
    
    // Authentication check and audit log page access request
    public void initialize(final ComponentSystemEvent event) throws Exception {
        // Invoke on initial request only
        if (!FacesContext.getCurrentInstance().isPostback()) {
            final HttpServletRequest request = (HttpServletRequest)FacesContext.getCurrentInstance().getExternalContext().getRequest();
            getEjbcaWebBean().initialize(request, AccessRulesConstants.ROLE_ADMINISTRATOR);
            
            adminPreference = getEjbcaWebBean().getAdminPreference();
            initAvailableLanguages();
            initThemes();
        }
    }
    
    public void initAvailableLanguages() {
        
        availableLanguages = new ArrayList<>();
        
        final List<WebLanguage> availableWebLanguages = getEjbcaWebBean().getWebLanguages();
        for (final WebLanguage availableWebLanguage : availableWebLanguages) {
            final SelectItem availableLanguage = new SelectItem(availableWebLanguage.getId(), availableWebLanguage.toString());
            availableLanguages.add(availableLanguage);
        }
    }
    
    public void initThemes() {
        availableThemes = new ArrayList<>();
        final String[] themes = getEjbcaWebBean().getGlobalConfiguration().getAvailableThemes();
        for (final String theme : themes) {
            final SelectItem availableTheme = new SelectItem(theme);
            availableThemes.add(availableTheme);
        }
    }
    
    public AdminPreference getAdminPreference() {
        return adminPreference;
    }
    
    public List<SelectItem> getAvailableLanguages() {
        return availableLanguages;
    }
    
    public List<SelectItem> getAvailableThemes() {
        return availableThemes;
    }

    /**
     * Save action.
     * @return the navigation outcome defined in faces-config.xml.
     */
    public String save() {
        try {
            if(!getEjbcaWebBean().existsAdminPreference()){
                getEjbcaWebBean().addAdminPreference(adminPreference);
            }
            else{
                getEjbcaWebBean().changeAdminPreference(adminPreference);
            }
        } catch (final AdminExistsException e) {
            addNonTranslatedErrorMessage(e.getMessage());
        } catch (final AdminDoesntExistException e) {
            addNonTranslatedErrorMessage(e.getMessage());
        }
        return "done";
    }
    
    public String cancel() {
        reset();
        return "done";
    }
    
    private void reset() {
        adminPreference = getEjbcaWebBean().getAdminPreference();
    }
}
