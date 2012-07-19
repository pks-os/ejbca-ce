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

package org.ejbca.ui.web.pub.cluster;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.cesecore.config.OcspConfiguration;
import org.ejbca.core.ejb.config.HealthCheckSessionLocal;
import org.ejbca.ui.web.protocol.IHealtChecker;

/**
 * External OCSP Health Checker. 
 * 
 * Does the following system checks.
 * 
 * * Not about to run out if memory
 * * Database connection can be established.
 * * All OCSPSignTokens are active if not set as offline.
 * 
 * @author Philip Vendil
 * @version $Id$
 */
public class ValidationAuthorityHealthCheck extends CommonHealthCheck {
	
	private static final Logger log = Logger.getLogger(ValidationAuthorityHealthCheck.class);
	private static IHealtChecker healthChecker;

	private boolean doSignTest = OcspConfiguration.getHealthCheckSignTest();
	private boolean doValidityTest = OcspConfiguration.getHealthCheckCertificateValidity();

	static public void setHealtChecker(IHealtChecker hc) {
		healthChecker = hc;
	}

	public ValidationAuthorityHealthCheck(final HealthCheckSessionLocal healthCheckSession) {
	    super(healthCheckSession);
	}

	@Override
	public String checkHealth(HttpServletRequest request) {
	    if (log.isDebugEnabled()) {
	        log.debug("Starting HealthCheck requested by : " + request.getRemoteAddr());
	    }
        final StringBuilder sb = new StringBuilder(0);
		checkMaintenance(sb);
		if (sb.length()>0) { 
			// if Down for maintenance do not perform more checks
			return sb.toString(); 
		}
        checkMemory(sb);
        if (sb.length()==0) { 
            checkDB(sb);
        }
		if (sb.length()==0) { 
		    checkOCSPSignTokens(sb);
		}
        if( sb.length()==0 ) {
            return null; 
        }
        return sb.toString(); 
	}

	private void checkOCSPSignTokens(final StringBuilder sb) {
		if (healthChecker==null) {
			sb.append("No OCSP token health checker set");
		} else {
		    sb.append(healthChecker.healthCheck(this.doSignTest, this.doValidityTest));
		}
	}
}
