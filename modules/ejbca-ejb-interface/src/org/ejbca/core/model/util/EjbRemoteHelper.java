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
package org.ejbca.core.model.util;

import org.cesecore.core.ejb.authorization.AdminEntitySessionRemote;
import org.cesecore.core.ejb.authorization.AdminGroupSessionRemote;
import org.cesecore.core.ejb.ca.crl.CrlSessionRemote;
import org.cesecore.core.ejb.ca.crl.CrlCreateSessionRemote;
import org.cesecore.core.ejb.ca.store.CertificateProfileSessionRemote;
import org.cesecore.core.ejb.log.LogSessionRemote;
import org.cesecore.core.ejb.ra.raadmin.EndEntityProfileSessionRemote;
import org.ejbca.core.ejb.JndiHelper;
import org.ejbca.core.ejb.approval.ApprovalSessionRemote;
import org.ejbca.core.ejb.authorization.AuthorizationSessionRemote;
import org.ejbca.core.ejb.ca.auth.AuthenticationSessionRemote;
import org.ejbca.core.ejb.ca.caadmin.CAAdminSessionRemote;
import org.ejbca.core.ejb.ca.caadmin.CaSessionRemote;
import org.ejbca.core.ejb.ca.publisher.PublisherQueueSessionRemote;
import org.ejbca.core.ejb.ca.publisher.PublisherSessionRemote;
import org.ejbca.core.ejb.ca.sign.SignSessionRemote;
import org.ejbca.core.ejb.ca.store.CertificateStoreSessionRemote;
import org.ejbca.core.ejb.hardtoken.HardTokenSessionRemote;
import org.ejbca.core.ejb.keyrecovery.KeyRecoverySessionRemote;
import org.ejbca.core.ejb.protect.TableProtectSessionRemote;
import org.ejbca.core.ejb.ra.CertificateRequestSessionRemote;
import org.ejbca.core.ejb.ra.UserAdminSessionRemote;
import org.ejbca.core.ejb.ra.raadmin.RaAdminSessionRemote;
import org.ejbca.core.ejb.ra.userdatasource.UserDataSourceSessionRemote;
import org.ejbca.core.ejb.services.ServiceSessionRemote;
import org.ejbca.core.ejb.upgrade.ConfigurationSessionRemote;
import org.ejbca.core.ejb.upgrade.UpgradeSessionRemote;
import org.ejbca.core.protocol.cmp.CmpMessageDispatcherSessionRemote;

/**
 * Helper methods to get EJB session interfaces.
 * 
 * @version $Id$
 */
public class EjbRemoteHelper {

    private AdminEntitySessionRemote adminEntitySession = null;
    private AdminGroupSessionRemote adminGroupSession = null;
    private ApprovalSessionRemote approvalSession = null;
    private AuthenticationSessionRemote authenticationSession = null;
    private AuthorizationSessionRemote authorizationSession = null;
    private CAAdminSessionRemote caAdminSession = null;
    private CaSessionRemote caSession = null;
    private CertificateProfileSessionRemote certificateProfileSession = null;
    private CertificateRequestSessionRemote certificateRequestSession = null;
    private CertificateStoreSessionRemote certificateStoreSession = null;
    private CmpMessageDispatcherSessionRemote cmpMessageDispatcherSession = null;
    private ConfigurationSessionRemote configurationSession = null;
    private CrlSessionRemote crlSession = null;
    private CrlCreateSessionRemote crlStoreSession = null;
    private EndEntityProfileSessionRemote endEntityProfileSession = null;
    private HardTokenSessionRemote hardTokenSession = null;
    private KeyRecoverySessionRemote keyRecoverySession = null;
    private LogSessionRemote logSession = null;
    private PublisherQueueSessionRemote publisherQueueSession = null;
    private PublisherSessionRemote publisherSession = null;
    private RaAdminSessionRemote raAdminSession = null;
    private ServiceSessionRemote serviceSession = null;
    private SignSessionRemote signSession = null;
    private TableProtectSessionRemote tableProtectSession = null;
    private UpgradeSessionRemote upgradeSession = null;
    private UserAdminSessionRemote userAdminSession = null;
    private UserDataSourceSessionRemote userDataSourceSession = null;

    /*
     * Return the environment entries locator. ServiceLocator caches the home
     * interfaces.
     * 
     * @return return the environment entries locator / private ServiceLocator
     * getLocator() { return ServiceLocator.getInstance(); }
     */

    public AdminEntitySessionRemote getAdminEntitySession() {
        if(adminEntitySession == null) {
            adminEntitySession = JndiHelper.getRemoteSession(AdminEntitySessionRemote.class);
        }
        return adminEntitySession;
    }
    
    public AdminGroupSessionRemote getAdminGroupSession() {
        if(adminGroupSession == null) {
            adminGroupSession = JndiHelper.getRemoteSession(AdminGroupSessionRemote.class);
        }
        return adminGroupSession;
    }
    
    public CaSessionRemote getCaSession() {
        if(caSession == null) {
            caSession = JndiHelper.getRemoteSession(CaSessionRemote.class);
        }
        return caSession;
    }
    
    public CAAdminSessionRemote getCAAdminSession() {
        if (caAdminSession == null) {
            caAdminSession = JndiHelper.getRemoteSession(CAAdminSessionRemote.class);
        }
        return caAdminSession;
    }

    public CertificateProfileSessionRemote getCertificateProfileSession() {
        if(certificateProfileSession == null) {
            certificateProfileSession = JndiHelper.getRemoteSession(CertificateProfileSessionRemote.class);
        }
        return certificateProfileSession;
    }
    
	public CmpMessageDispatcherSessionRemote getCmpMessageDispatcherSession() {
        if (cmpMessageDispatcherSession == null) {
        	cmpMessageDispatcherSession = JndiHelper.getRemoteSession(CmpMessageDispatcherSessionRemote.class);
        }
        return cmpMessageDispatcherSession;
	}

    public CrlCreateSessionRemote getCrlStoreSession() {
        if (crlStoreSession == null) {
            crlStoreSession = JndiHelper.getRemoteSession(CrlCreateSessionRemote.class);
        }
        return crlStoreSession;
    }
	
	public EndEntityProfileSessionRemote getEndEntityProfileSession() {
        if(endEntityProfileSession == null) {
            endEntityProfileSession = JndiHelper.getRemoteSession(EndEntityProfileSessionRemote.class);
        }
        return endEntityProfileSession;
    }
    
    public RaAdminSessionRemote getRAAdminSession() {
        if (raAdminSession == null) {
            raAdminSession = JndiHelper.getRemoteSession(RaAdminSessionRemote.class);
        }
        return raAdminSession;
    }

    public CertificateStoreSessionRemote getCertStoreSession() {
        if (certificateStoreSession == null) {
            certificateStoreSession = JndiHelper.getRemoteSession(CertificateStoreSessionRemote.class);
        }
        return certificateStoreSession;
    }

    public SignSessionRemote getSignSession() {
        if (signSession == null) {
            signSession = JndiHelper.getRemoteSession(SignSessionRemote.class);
        }
        return signSession;
    }

    public UserAdminSessionRemote getUserAdminSession() {
        if (userAdminSession == null) {
            userAdminSession = JndiHelper.getRemoteSession(UserAdminSessionRemote.class);
        }
        return userAdminSession;
    }

    public KeyRecoverySessionRemote getKeyRecoverySession() {
        if (keyRecoverySession == null) {
            keyRecoverySession = JndiHelper.getRemoteSession(KeyRecoverySessionRemote.class);
        }
        return keyRecoverySession;
    }

    public HardTokenSessionRemote getHardTokenSession() {
        if (hardTokenSession == null) {
            hardTokenSession = JndiHelper.getRemoteSession(HardTokenSessionRemote.class);
        }
        return hardTokenSession;
    }

    public AuthorizationSessionRemote getAuthorizationSession() {
        if (authorizationSession == null) {
            authorizationSession = JndiHelper.getRemoteSession(AuthorizationSessionRemote.class);
        }
        return authorizationSession;
    }

    public AuthenticationSessionRemote getAuthenticationSession() {
        if (authenticationSession == null) {
            authenticationSession = JndiHelper.getRemoteSession(AuthenticationSessionRemote.class);
        }
        return authenticationSession;
    }

    public ApprovalSessionRemote getApprovalSession() {
        if (approvalSession == null) {
            approvalSession = JndiHelper.getRemoteSession(ApprovalSessionRemote.class);
        }
        return approvalSession;
    }

    public UserDataSourceSessionRemote getUserDataSourceSession() {
        if (userDataSourceSession == null) {
            userDataSourceSession = JndiHelper.getRemoteSession(UserDataSourceSessionRemote.class);
        }
        return userDataSourceSession;
    }

    public LogSessionRemote getLogSession() {
        if (logSession == null) {
            logSession = JndiHelper.getRemoteSession(LogSessionRemote.class);
        }
        return logSession;
    }

    public PublisherQueueSessionRemote getPublisherQueueSession() {
        if (publisherQueueSession == null) {
            publisherQueueSession = JndiHelper.getRemoteSession(PublisherQueueSessionRemote.class);
        }
        return publisherQueueSession;
    }

    public PublisherSessionRemote getPublisherSession() {
        if (publisherSession == null) {
            publisherSession = JndiHelper.getRemoteSession(PublisherSessionRemote.class);
        }
        return publisherSession;
    }

    public CrlSessionRemote getCrlSession() {
        if (crlSession == null) {
            crlSession = JndiHelper.getRemoteSession(CrlSessionRemote.class);
        }
        return crlSession;
    }

    public CertificateRequestSessionRemote getCertficateRequestSession() {
        if (certificateRequestSession == null) {
            certificateRequestSession = JndiHelper.getRemoteSession(CertificateRequestSessionRemote.class);
        }
        return certificateRequestSession;
    }

    public TableProtectSessionRemote getTableProtectSession() {
        if (tableProtectSession == null) {
            tableProtectSession = JndiHelper.getRemoteSession(TableProtectSessionRemote.class);
        }
        return tableProtectSession;
    }

    public UpgradeSessionRemote getUpgradeSession() {
        if (upgradeSession == null) {
            upgradeSession = JndiHelper.getRemoteSession(UpgradeSessionRemote.class);
        }
        return upgradeSession;
    }

    public ConfigurationSessionRemote getConfigurationSession() {
        if (configurationSession == null) {
            configurationSession = JndiHelper.getRemoteSession(ConfigurationSessionRemote.class);
        }
        return configurationSession;
    }

    public ServiceSessionRemote getServiceSession() {
        if (serviceSession == null) {
            serviceSession = JndiHelper.getRemoteSession(ServiceSessionRemote.class);
        }
        return serviceSession;
    }
}
