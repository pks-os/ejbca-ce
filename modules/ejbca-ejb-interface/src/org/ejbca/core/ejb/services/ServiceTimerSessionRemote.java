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
package org.ejbca.core.ejb.services;

import javax.ejb.Remote;
import javax.ejb.Timer;

/**
 * Remote interface for ServiceTimerSession.
 */
@Remote
public interface ServiceTimerSessionRemote extends ServiceTimerSession {

	// TODO: Is this required for timer service?
	public void ejbTimeout(Timer timer);
}
