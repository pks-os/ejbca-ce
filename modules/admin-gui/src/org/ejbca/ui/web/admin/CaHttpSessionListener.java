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

package org.ejbca.ui.web.admin;

import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.ejb.EJB;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.log4j.Logger;
import org.cesecore.audit.enums.EventStatus;
import org.cesecore.audit.log.SecurityEventsLoggerSessionLocal;
import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authentication.tokens.X509CertificateAuthenticationToken;
import org.cesecore.util.CertTools;
import org.ejbca.config.WebConfiguration;
import org.ejbca.core.ejb.audit.enums.EjbcaEventTypes;
import org.ejbca.core.ejb.audit.enums.EjbcaModuleTypes;
import org.ejbca.core.ejb.audit.enums.EjbcaServiceTypes;
import org.ejbca.ui.web.admin.configuration.EjbcaWebBean;

/**
 * Listener detecting individual session timeouts. A session generally times out when after
 * a period of inactivity defined in web.xml but may also be terminated while an administrator
 * actively logs out from the Admin Web, or if the inactivity timer configured in System Configuration
 * is reached.
 * 
 * All ended sessions will be audit logged by this listener. 
 * 
 * @version $Id$
 */
public class CaHttpSessionListener implements HttpSessionListener {

    private static final Logger log = Logger.getLogger(CaHttpSessionListener.class);
    
    @EJB
    private SecurityEventsLoggerSessionLocal auditLogSession;
    
    
    @Override
    public void sessionCreated(HttpSessionEvent httpSessionEvent) {
        if (log.isDebugEnabled()) {
            log.debug("HTTP session from client started. jsessionId=" + httpSessionEvent.getSession().getId());
        }
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent httpSessionEvent) {
        final EjbcaWebBean ejbcaWebBean = getEjbcaWebBean(httpSessionEvent.getSession());
        final Map<String, Object> logDetails = new LinkedHashMap<>();
        if (ejbcaWebBean == null) {
            // Since this method is invoked right before the session is actually terminated,
            // this should never happen. If it does, audit logging will fail but with a log error.
            return;
        }
        final AuthenticationToken admin = ejbcaWebBean.getAdminObject();
        final Certificate x509Certificate= getCertificate(admin);
        final String issuerDn = Integer.toString(CertTools.getIssuerDN(x509Certificate).hashCode());
        final String serialNr = CertTools.getSerialNumberAsString(x509Certificate);
        if (WebConfiguration.getAdminLogRemoteAddress()) {
            logDetails.put("remoteip", ejbcaWebBean.getCurrentRemoteIp());
        }
        
        if (log.isDebugEnabled()) {
            log.debug("HTTP session from client destroyed. jsessionId=" + httpSessionEvent.getSession().getId());
        }
        // Audit log the event
        auditLogSession.log(EjbcaEventTypes.ADMINWEB_ADMINISTRATORLOGGEDOUT, EventStatus.SUCCESS, EjbcaModuleTypes.ADMINWEB, EjbcaServiceTypes.EJBCA, 
                admin.toString(), issuerDn, serialNr, null, logDetails);
    }
    
    private Certificate getCertificate(final AuthenticationToken admin) {
        if (admin instanceof X509CertificateAuthenticationToken) {
            return ((X509CertificateAuthenticationToken)admin).getCertificate();
        }
        return null;
    }
    
    private final EjbcaWebBean getEjbcaWebBean(HttpSession session) {
        EjbcaWebBean ejbcawebbean = (EjbcaWebBean) session.getAttribute("ejbcawebbean");
        if (ejbcawebbean == null) {
            try {
                ejbcawebbean = (EjbcaWebBean) java.beans.Beans.instantiate(Thread.currentThread().getContextClassLoader(),
                        org.ejbca.ui.web.admin.configuration.EjbcaWebBean.class.getName());
            } catch (ClassNotFoundException e) {
                log.error("Failed to audit log ended session with Id" + session.getId() + "\n" + e.getMessage());
            } catch (Exception e) {
                log.error("Failed to audit log ended session with Id" + session.getId() + "\n" + e.getMessage());
            }
            session.setAttribute("ejbcawebbean", ejbcawebbean);
        }
        return ejbcawebbean;
    }
}
