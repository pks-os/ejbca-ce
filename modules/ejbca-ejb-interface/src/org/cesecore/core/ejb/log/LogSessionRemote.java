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
package org.cesecore.core.ejb.log;

import javax.ejb.Remote;

/**
 * Remote interface for LogSession.
 * @version $Id$
 */
@Remote
public interface LogSessionRemote extends LogSession {

	/**
     * Methods for testing that a log-row is never rolled back if the rest of
     * the transaction is.
     */
    public void testRollback(long rollbackTestTime);
}
