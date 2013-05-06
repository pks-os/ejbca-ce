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
package org.ejbca.ui.cli.service;

import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.util.EjbRemoteHelper;
import org.ejbca.core.ejb.services.ServiceSessionRemote;
import org.ejbca.ui.cli.ErrorAdminCommandException;

/**
 * Common functions for the tests of the service command
 * 
 * @version $Id$
 */
class ServiceTestCase {
    
    private AuthenticationToken admin;
    private ServiceSessionRemote serviceSession;
    
    private static final String[] INFO_ARGS = { "info" }; 
    
    protected AuthenticationToken getAdmin() throws ErrorAdminCommandException {
        if (admin == null) {
            ServiceInfoCommand cmd = new ServiceInfoCommand(); // any command extending BaseServiceCommand
            cmd.execute(INFO_ARGS); // execute logs in also
            admin = cmd.getAdmin();
        }
        return admin;
    }
    
    protected ServiceSessionRemote getServiceSession() {
        if (serviceSession == null) {
            serviceSession = EjbRemoteHelper.INSTANCE.getRemoteSession(ServiceSessionRemote.class);
        }
        return serviceSession;
    }
}
