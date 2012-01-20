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
package org.cesecore.authorization.control;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.apache.log4j.Logger;
import org.cesecore.audit.enums.EventStatus;
import org.cesecore.audit.enums.EventTypes;
import org.cesecore.audit.enums.ModuleTypes;
import org.cesecore.audit.enums.ServiceTypes;
import org.cesecore.audit.log.InternalSecurityEventsLoggerSessionLocal;
import org.cesecore.authentication.AuthenticationFailedException;
import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authorization.cache.AccessTreeCache;
import org.cesecore.authorization.cache.AccessTreeUpdateSessionLocal;
import org.cesecore.internal.InternalResources;
import org.cesecore.jndi.JndiConstants;
import org.cesecore.roles.access.RoleAccessSessionLocal;
import org.cesecore.time.TrustedTime;
import org.cesecore.time.TrustedTimeWatcherSessionLocal;
import org.cesecore.time.providers.TrustedTimeProviderException;

/**
 * 
 * @version $Id$
 * 
 */
@Stateless(mappedName = JndiConstants.APP_JNDI_PREFIX + "AccessControlSessionRemote")
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class AccessControlSessionBean implements AccessControlSessionLocal, AccessControlSessionRemote {

    private static final Logger log = Logger.getLogger(AccessControlSessionBean.class);

    /* Internal localization of logs and errors */
    private static final InternalResources intres = InternalResources.getInstance();
    
    @EJB
    private AccessTreeUpdateSessionLocal accessTreeUpdateSession;

    @EJB
    private RoleAccessSessionLocal roleAccessSession;

    // We have to depend on the internal security events logger here, since the remote depends on us
    @EJB
    private InternalSecurityEventsLoggerSessionLocal securityEventsLoggerSession;
    @EJB
    private TrustedTimeWatcherSessionLocal trustedTimeWatcherSession;

    /** 
     * Cache for authorization data 
     * 
     * This class member knowingly breaks the EJB standard which forbids static volatile class members. The
     * spirit of this rule is to prohibit implementations from using mutexes in their SSBs, thus negating the
     * EJB bean pool. It doesn't take into account the need to cache data in a shared singleton, thus we have 
     * to knowingly break the standard, but not its spirit. 
     * 
     */
    private static volatile AccessTreeCache accessTreeCache;

    private boolean isAuthorized(final AuthenticationToken authenticationToken, final String resource, final boolean doLogging) {
        try {
            if (accessTreeCache.getAccessTree().isAuthorized(authenticationToken, resource)) {
                if (doLogging) {
                    try {
                        final TrustedTime tt = trustedTimeWatcherSession.getTrustedTime(false);
                        final Map<String, Object> details = new LinkedHashMap<String, Object>();
                        details.put("resource", resource);
                        securityEventsLoggerSession.log(tt, EventTypes.ACCESS_CONTROL, EventStatus.SUCCESS, ModuleTypes.ACCESSCONTROL,
                                ServiceTypes.CORE, authenticationToken.toString(), null, null, null, details);
                    } catch (TrustedTimeProviderException e) {
                        log.error("Error getting trusted time for audit log: ", e);
                    }
                }
                return true;
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Authorization failed for " + authenticationToken.toString() + " of type "
                            + authenticationToken.getClass().getSimpleName() + " for resource " + resource);
                }
            }
        } catch (AuthenticationFailedException e) {
            final Map<String, Object> details = new LinkedHashMap<String, Object>();
            String msg = intres.getLocalizedMessage("authentication.failed", e.getMessage());
            details.put("msg", msg);
            try {
                securityEventsLoggerSession.log(trustedTimeWatcherSession.getTrustedTime(false), EventTypes.AUTHENTICATION, EventStatus.FAILURE,
                        ModuleTypes.AUTHENTICATION, ServiceTypes.CORE, authenticationToken.toString(), null, null, null, details);
            } catch (TrustedTimeProviderException f) {
                log.error("Error getting trusted time for audit log: ", e);
            }
        }
        return false;
    }
    
    @Override
    public boolean isAuthorized(final AuthenticationToken authenticationToken, final String resource) {
        if (updateNeccessary()) {
            updateAuthorizationTree();
        }
        return isAuthorized(authenticationToken, resource, true);
    }
    
    @Override
    public boolean isAuthorizedNoLogging(final AuthenticationToken authenticationToken, final String resource) {
        if (updateNeccessary()) {
            updateAuthorizationTree();
        }
        return isAuthorized(authenticationToken, resource, false);
    }

    @Override
    public void forceCacheExpire() {
        if (log.isTraceEnabled()) {
            log.trace("forceCacheExpire");
        }
        if (accessTreeCache != null) {
            accessTreeCache.forceCacheExpire();
        }
    }

    /**
     * Method used check if a reconstruction of authorization tree is needed in the authorization beans.
     * 
     * @return true if update is needed.
     */
    private boolean updateNeccessary() {
        boolean ret = false;
        // Only do the actual SQL query if we might update the configuration due to cache time anyhow
        if (accessTreeCache == null) {
            ret = true;
        } else if (accessTreeCache.needsUpdate()) {
            ret = accessTreeUpdateSession.getAccessTreeUpdateData().updateNeccessary(accessTreeCache.getAccessTreeUpdateNumber());
            // we don't want to run the above query often
        }
        if (log.isTraceEnabled()) {
            log.trace("updateNeccessary: " + false);
        }
        return ret;
    }

    /**
     * method updating authorization tree.
     */
    private void updateAuthorizationTree() {
        if (log.isTraceEnabled()) {
            log.trace(">updateAuthorizationTree");
        }
        final int authorizationtreeupdatenumber = accessTreeUpdateSession.getAccessTreeUpdateData().getAccessTreeUpdateNumber();
        if (accessTreeCache == null) {
            accessTreeCache = new AccessTreeCache();
        }

        accessTreeCache.updateAccessTree(roleAccessSession.getAllRoles(), authorizationtreeupdatenumber);
        if (log.isTraceEnabled()) {
            log.trace("<updateAuthorizationTree");
        }

    }

}
