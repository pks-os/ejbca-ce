/*************************************************************************
 *                                                                       *
 *  CESeCore: CE Security Core                                           *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.cesecore.audit.log;   

import java.util.Map;

import javax.ejb.Local;

import org.cesecore.audit.enums.EventStatus;
import org.cesecore.audit.enums.EventType;
import org.cesecore.audit.enums.ModuleType;
import org.cesecore.audit.enums.ServiceType;
import org.cesecore.time.TrustedTime;

/**
 * Local internal secure audit log interface.
 *
 * @version $Id: InternalSecurityEventsLoggerSessionLocal.java 900 2011-06-21 16:33:28Z johane $
 */
@Local
public interface InternalSecurityEventsLoggerSessionLocal {

	/**
     * Creates a signed log, stored in the database.
     * 
     * @param trustedTime TrustedTime instance will be used to get a trusted timestamp.
     * @param eventType The event log type.
     * @param eventStatus The status of the operation to log.
     * @param module The module where the operation took place.
     * @param service The service(application) that performed the operation.
     * @param authToken The authentication token that invoked the operation.
     * @param customId A custom identifier related to this event (e.g. a CA's identifier)
     * @param searchDetail1 A detail of this event that can be queried for using QueryCriteria (database) searches (e.g. a certificate serialnumber)
     * @param searchDetail2 A detail of this event that can be queried for using QueryCriteria (database) searches (e.g. a username)
     * @param additionalDetails Additional details to be logged.
     * 
     * @throws AuditRecordStorageException if unable to store the log record
     */
    void log(TrustedTime trustedTime, EventType eventType, EventStatus eventStatus, ModuleType module, ServiceType service, String authToken,
    		String customId, String searchDetail1, String searchDetail2, Map<String, Object> additionalDetails) throws AuditRecordStorageException;

    /** Gets trusted time, then calls log
     * 
     * @param eventType The event log type.
     * @param eventStatus The status of the operation to log.
     * @param module The module where the operation took place.
     * @param service The service(application) that performed the operation.
     * @param authToken The authentication token that invoked the operation.
     * @param customId A custom identifier related to this event (e.g. a CA's identifier)
     * @param searchDetail1 A detail of this event that can be queried for using QueryCriteria (database) searches (e.g. a certificate serialnumber)
     * @param searchDetail2 A detail of this event that can be queried for using QueryCriteria (database) searches (e.g. a username)
     * @param additionalDetails Additional details to be logged.
     * @throws AuditRecordStorageException
     * @see {@link #log(TrustedTime, EventType, EventStatus, ModuleType, ServiceType, String, String, String, String, Map)}
     */
	void log(EventType eventType, EventStatus eventStatus, ModuleType module, ServiceType service, String authToken, 
			String customId, String searchDetail1, String searchDetail2, Map<String, Object> additionalDetails) throws AuditRecordStorageException;
}
