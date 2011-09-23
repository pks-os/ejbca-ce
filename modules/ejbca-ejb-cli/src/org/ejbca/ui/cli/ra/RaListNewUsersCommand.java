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
 
package org.ejbca.ui.cli.ra;

import java.util.Collection;
import java.util.Iterator;

import org.cesecore.certificates.endentity.EndEntityInformation;
import org.ejbca.core.model.ra.UserDataConstants;
import org.ejbca.ui.cli.ErrorAdminCommandException;

/**
 * List users with status NEW in the database.
 *
 * @version $Id$
 *
 * @see org.ejbca.core.ejb.ra.UserDataLocal
 */
public class RaListNewUsersCommand extends BaseRaAdminCommand {

	public String getMainCommand() { return MAINCOMMAND; }
	public String getSubCommand() { return "listnewusers"; }
	public String getDescription() { return "List users with status 'NEW'"; }

    public void execute(String[] args) throws ErrorAdminCommandException {
        args = parseUsernameAndPasswordFromArgs(args);
        
        try {
            Collection<EndEntityInformation> coll = ejb.getUserAdminSession().findAllUsersByStatus(getAdmin(cliUserName, cliPassword), UserDataConstants.STATUS_NEW);
            Iterator<EndEntityInformation> iter = coll.iterator();
            while (iter.hasNext()) {
            	EndEntityInformation data = iter.next();
                getLogger().info("New User: " + data.getUsername() + ", \"" + data.getDN() +
                    "\", \"" + data.getSubjectAltName() + "\", " + data.getEmail() + ", " +
                    data.getStatus() + ", " + data.getType() + ", " + data.getTokenType());
            }
        } catch (Exception e) {
            throw new ErrorAdminCommandException(e);
        }
    }
}
