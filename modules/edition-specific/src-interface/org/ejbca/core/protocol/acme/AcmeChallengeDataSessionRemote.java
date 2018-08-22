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
package org.ejbca.core.protocol.acme;

import javax.ejb.Remote;

/**
 * Remote interface for AcmeChallengeDataSession
 * @version $Id: AcmeChallengeDataSessionRemote.java 25797 2018-08-10 15:52:00Z jekaterina $
 */
@Remote
public interface AcmeChallengeDataSessionRemote extends AcmeChallengeDataSession {
}
