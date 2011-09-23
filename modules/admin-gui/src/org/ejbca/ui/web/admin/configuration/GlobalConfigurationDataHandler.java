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

package org.ejbca.ui.web.admin.configuration;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.authorization.control.AccessControlSessionLocal;
import org.ejbca.config.GlobalConfiguration;
import org.ejbca.config.WebConfiguration;
import org.ejbca.core.ejb.config.GlobalConfigurationSessionLocal;

/**
 * A class handling the saving and loading of global configuration data.
 * By default all data are saved to a database.
 *
 * @author  Philip Vendil
 * @version $Id$
 */
public class GlobalConfigurationDataHandler implements java.io.Serializable {
    
	private static final long serialVersionUID = 2L;
    private static InitialContext initialContext;	// Expensive to create
	private GlobalConfigurationSessionLocal globalconfigurationsession;
    private AccessControlSessionLocal authorizationsession;
    private AuthenticationToken administrator;

    /** Creates a new instance of GlobalConfigurationDataHandler */
    public GlobalConfigurationDataHandler(AuthenticationToken administrator, GlobalConfigurationSessionLocal globalconfigurationsession, AccessControlSessionLocal authorizationsession){
        this.globalconfigurationsession = globalconfigurationsession;
        this.authorizationsession = authorizationsession;
        this.administrator = administrator;
    }
    
    public GlobalConfiguration loadGlobalConfiguration() throws NamingException{
        GlobalConfiguration ret = null;
        // TODO: These should be dropped or moved to property files!!
        ret = globalconfigurationsession.getCachedGlobalConfiguration();
        if (initialContext == null) {
            initialContext = new InitialContext();
        }
        Context myenv = (Context) initialContext.lookup("java:comp/env");      
        ret.initialize( (String) myenv.lookup("ADMINDIRECTORY"),
        		WebConfiguration.getAvailableLanguages(), (String) myenv.lookup("AVAILABLETHEMES"), 
                ""+WebConfiguration.getPublicHttpPort(), ""+WebConfiguration.getPrivateHttpsPort(),
                (String) myenv.lookup("PUBLICPROTOCOL"),(String) myenv.lookup("PRIVATEPROTOCOL"));
        return ret;
    }
    
    public void saveGlobalConfiguration(GlobalConfiguration gc) throws AuthorizationDeniedException {
        if(this.authorizationsession.isAuthorizedNoLogging(administrator, "/super_administrator")) {
            globalconfigurationsession.saveGlobalConfiguration(administrator,  gc);
        }
    }
}
