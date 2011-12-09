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
package org.cesecore.authorization.control;

/**
 * @version $Id$
 *
 */
public enum AuditLogRules {
    BASE("/secureaudit"),
    CONFIGURE(BASE.resource() + "/management/manage"),
    EXPORT_LOGS(BASE.resource() + "/auditor/export"),
    VIEW(BASE.resource() + "/auditor/select"),
    VERIFY(BASE.resource() + "/auditor/verify"),
    LOG(BASE.resource() + "/log"),
    LOG_CUSTOM(BASE.resource() + "/log_custom_events");

    private final String resource;
    
    private AuditLogRules(String resource) {
        this.resource = resource;
    }

    public String resource() {
        return this.resource;
    }

    public String toString() {
        return this.resource;
    }
}
