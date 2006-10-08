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
 
package org.ejbca.core.protocol.ws.client;

import org.ejbca.ui.cli.IAdminCommand;

/**
 * Factory for EJBCA WS RA Admin Commands.
 *
 * @version $Id: EJBCAWSRACommandFactory.java,v 1.2 2006-10-08 22:53:26 herrvendil Exp $
 */
public class EJBCAWSRACommandFactory {
    /**
     * Cannot create an instance of this class, only use static methods.
     */
    private EJBCAWSRACommandFactory() {
    }

    /**
     * Returns an Admin Command object based on contents in args[0].
     *
     * @param args array of arguments typically passed from main().
     *
     * @return Command object or null if args[0] does not specify a valid command.
     */
    public static IAdminCommand getCommand(String[] args) {
        if (args.length < 1) {
            return null;
        }        
        
        if (args[0].equals("edituser")) {
            return new EditUserCommand(args);
        }else if (args[0].equals("finduser")) {
            return new FindUserCommand(args);
        }else if (args[0].equals("findcerts")) {
            return new FindCertsCommand(args);
        }else if (args[0].equals("pkcs10req")) {
            return new PKCS10ReqCommand(args);
        } else if (args[0].equals("pkcs12req")) {
            return new PKCS12ReqCommand(args);
        } else if (args[0].equals("revokecert")) {
            return new RevokeCertCommand(args);
        } else if (args[0].equals("revoketoken")) {
            return new RevokeTokenCommand(args);
        } else if (args[0].equals("revokeuser")) {
            return new RevokeUserCommand(args);
        } else if (args[0].equals("checkrevokationstatus")) {
            return new CheckRevokeStatusCommand(args);        
        }else if (args[0].equals("generatenewuser")) {
            return new GenerateNewUserCommand(args);        
        }
        
        else {
            return null;
        }
    }

    // getCommand
}


// RaAdminCommandFactory
