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
package org.ejbca.core.ejb.authorization;

import javax.ejb.Local;

/**
 * @version $Id$
 */
@Local
@Deprecated // Use AuthorizationSystemSession
public interface ComplexAccessControlSessionLocal extends ComplexAccessControlSession {

    public static final String SUPERADMIN_ROLE = "Super Administrator Role";
    public static final String TEMPORARY_SUPERADMIN_ROLE = "Temporary Super Administrator Group";
    
    /**
     * Creates a super administrator role and a default CLI user. A role and default CLI user is needed in order
     * to do operations with the CLI (command line interface).  
     */
    void createSuperAdministrator();
    
    /** 
     * initializes the authorization module, if no roles or CAs exists in the system. This is done during startup 
     * so that we can use the CLI after this to install and configure the system further.
     * This method only performs any operation of RoleData and CAData both have no entries. 
     * 
     * @return true if initialization was done. This also means that this is a fresh installation of EJBCA.
     * @deprecated superseded by AuthorizationSystemSessionLocal.initializeAuthorizationModule()
     */
    @Deprecated
    boolean initializeAuthorizationModule();
}
