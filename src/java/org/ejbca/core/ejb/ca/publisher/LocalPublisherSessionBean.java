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

package org.ejbca.core.ejb.ca.publisher;

import java.io.UnsupportedEncodingException;
import java.security.cert.Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

import javax.ejb.CreateException;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.log4j.Logger;
import org.ejbca.core.ejb.JndiHelper;
import org.ejbca.core.ejb.authorization.AuthorizationSessionLocal;
import org.ejbca.core.ejb.log.LogSessionLocal;
import org.ejbca.core.model.InternalResources;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.authorization.AccessRulesConstants;
import org.ejbca.core.model.authorization.AuthorizationDeniedException;
import org.ejbca.core.model.ca.publisher.ActiveDirectoryPublisher;
import org.ejbca.core.model.ca.publisher.BasePublisher;
import org.ejbca.core.model.ca.publisher.CustomPublisherContainer;
import org.ejbca.core.model.ca.publisher.ExternalOCSPPublisher;
import org.ejbca.core.model.ca.publisher.LdapPublisher;
import org.ejbca.core.model.ca.publisher.LdapSearchPublisher;
import org.ejbca.core.model.ca.publisher.PublisherConnectionException;
import org.ejbca.core.model.ca.publisher.PublisherException;
import org.ejbca.core.model.ca.publisher.PublisherExistsException;
import org.ejbca.core.model.ca.publisher.PublisherQueueData;
import org.ejbca.core.model.ca.publisher.PublisherQueueVolatileData;
import org.ejbca.core.model.log.Admin;
import org.ejbca.core.model.log.LogConstants;
import org.ejbca.core.model.ra.ExtendedInformation;
import org.ejbca.util.Base64GetHashMap;
import org.ejbca.util.CertTools;

/**
 * Handles management of Publishers.
 * 
 * @ejb.bean description="Session bean handling interface with publisher data"
 *           display-name="PublisherSessionSB" name="PublisherSession"
 *           jndi-name="PublisherSession"
 *           local-jndi-name="PublisherSessionLocal" view-type="both"
 *           type="Stateless" transaction-type="Container"
 * 
 * @ejb.transaction type="Required"
 * 
 * @weblogic.enable-call-by-reference True
 * 
 * 
 * @ejb.ejb-external-ref description="The Publisher entity bean"
 *                       view-type="local" ref-name="ejb/PublisherDataLocal"
 *                       type="Entity"
 *                       home="org.ejbca.core.ejb.ca.publisher.PublisherDataLocalHome"
 *                       business
 *                       ="org.ejbca.core.ejb.ca.publisher.PublisherDataLocal"
 *                       link="PublisherData"
 * 
 * @ejb.ejb-external-ref description="The Authorization Session Bean"
 *                       view-type="local"
 *                       ref-name="ejb/AuthorizationSessionLocal" type="Session"
 *                       home=
 *                       "org.ejbca.core.ejb.authorization.IAuthorizationSessionLocalHome"
 *                       business=
 *                       "org.ejbca.core.ejb.authorization.IAuthorizationSessionLocal"
 *                       link="AuthorizationSession"
 * 
 * @ejb.ejb-external-ref description="The log session bean" view-type="local"
 *                       ref-name="ejb/LogSessionLocal" type="Session"
 *                       home="org.ejbca.core.ejb.log.ILogSessionLocalHome"
 *                       business="org.ejbca.core.ejb.log.ILogSessionLocal"
 *                       link="LogSession"
 * 
 * @ejb.ejb-external-ref description="The publisher queue" view-type="local"
 *                       ref-name="ejb/PublisherQueueSessionLocal"
 *                       type="Session"
 *                       home="org.ejbca.core.ejb.ca.publisher.IPublisherQueueSessionLocalHome"
 *                       business=
 *                       "org.ejbca.core.ejb.ca.publisher.IPublisherQueueSessionLocal"
 *                       link="PublisherQueueSession"
 * 
 * @ejb.home extends="javax.ejb.EJBHome" local-extends="javax.ejb.EJBLocalHome"
 *           local-class=
 *           "org.ejbca.core.ejb.ca.publisher.IPublisherSessionLocalHome"
 *           remote-
 *           class="org.ejbca.core.ejb.ca.publisher.IPublisherSessionHome"
 * 
 * @ejb.interface extends="javax.ejb.EJBObject"
 *                local-extends="javax.ejb.EJBLocalObject"
 *                local-class="org.ejbca.core.ejb.ca.publisher.IPublisherSessionLocal"
 *                remote-class=
 *                "org.ejbca.core.ejb.ca.publisher.IPublisherSessionRemote"
 * 
 * @jonas.bean ejb-name="PublisherSession"
 */
@Stateless(mappedName = JndiHelper.APP_JNDI_PREFIX + "PublisherSessionRemote")
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class LocalPublisherSessionBean implements PublisherSessionLocal, PublisherSessionRemote {

    private static final Logger log = Logger.getLogger(LocalPublisherSessionBean.class);

    /** Internal localization of logs and errors */
    private static final InternalResources intres = InternalResources.getInstance();

    @PersistenceContext(unitName = "ejbca")
    private EntityManager entityManager;

    /**
     * The local interface of authorization session bean
     */
    @EJB
    private AuthorizationSessionLocal authorizationsession;

    /**
     * Local interface to the publisher queue, that handles failed publishings.
     */
    @EJB
    private PublisherQueueSessionLocal pubqueuesession;

    /**
     * The remote interface of log session bean
     */
    @EJB
    private LogSessionLocal logsession;

    /**
     * Default create for SessionBean without any creation Arguments.
     * 
     * @throws CreateException
     *             if bean instance can't be created
     */
    public void ejbCreate() throws CreateException {

    }

    /**
     * Stores the certificate to the given collection of publishers. See
     * BasePublisher class for further documentation about function
     * 
     * @param publisherids
     *            a Collection (Integer) of publisherids.
     * @return true if sucessfull result on all given publishers
     * @ejb.interface-method view-type="both"
     * @see org.ejbca.core.model.ca.publisher.BasePublisher
     */
    public boolean storeCertificate(Admin admin, Collection publisherids, Certificate incert, String username, String password, String userDN, String cafp,
            int status, int type, long revocationDate, int revocationReason, String tag, int certificateProfileId, long lastUpdate,
            ExtendedInformation extendedinformation) {
        return storeCertificate(admin, LogConstants.EVENT_INFO_STORECERTIFICATE, LogConstants.EVENT_ERROR_STORECERTIFICATE, publisherids, incert, username,
                password, userDN, cafp, status, type, revocationDate, revocationReason, tag, certificateProfileId, lastUpdate, extendedinformation);
    }

    /**
     * Revokes the certificate in the given collection of publishers. See
     * BasePublisher class for further documentation about function
     * 
     * @param publisherids
     *            a Collection (Integer) of publisherids.
     * @ejb.interface-method view-type="both"
     * @see org.ejbca.core.model.ca.publisher.BasePublisher
     */
    public void revokeCertificate(Admin admin, Collection publisherids, Certificate cert, String username, String userDN, String cafp, int type, int reason,
            long revocationDate, String tag, int certificateProfileId, long lastUpdate) {
        storeCertificate(admin, LogConstants.EVENT_INFO_REVOKEDCERT, LogConstants.EVENT_ERROR_REVOKEDCERT, publisherids, cert, username, null, userDN, cafp,
                SecConst.CERT_REVOKED, type, revocationDate, reason, tag, certificateProfileId, lastUpdate, null);
    }

    /**
     * The same basic method is be used for both store and revoke
     * 
     * @param admin
     * @param logInfoEvent
     * @param logErrorEvent
     * @param publisherids
     * @param cert
     * @param username
     * @param password
     * @param userDN
     * @param cafp
     * @param status
     * @param type
     * @param revocationDate
     * @param revocationReason
     * @param tag
     * @param certificateProfileId
     * @param lastUpdate
     * @param extendedinformation
     * @return true if publishing was successful for all publishers, false if
     *         not or if was enqued for any of the publishers
     */
    private boolean storeCertificate(Admin admin, int logInfoEvent, int logErrorEvent, Collection<Integer> publisherids, Certificate cert, String username,
            String password, String userDN, String cafp, int status, int type, long revocationDate, int revocationReason, String tag, int certificateProfileId,
            long lastUpdate, ExtendedInformation extendedinformation) {
    
        boolean returnval = true;
        for (Integer id : publisherids) {
            int publishStatus = PublisherQueueData.STATUS_PENDING;
            PublisherData pdl = PublisherData.findById(entityManager, id);
            String fingerprint = CertTools.getFingerprintAsString(cert);
            // If it should be published directly
            if (!getPublisher(pdl).getOnlyUseQueue()) {
                try {
                    if (getPublisher(pdl).storeCertificate(admin, cert, username, password, userDN, cafp, status, type, revocationDate, revocationReason, tag,
                            certificateProfileId, lastUpdate, extendedinformation)) {
                        publishStatus = PublisherQueueData.STATUS_SUCCESS;
                    }
                    String msg = intres.getLocalizedMessage("publisher.store", CertTools.getSubjectDN(cert), pdl.getName());
                    logsession.log(admin, cert, LogConstants.MODULE_CA, new java.util.Date(), username, cert, logInfoEvent, msg);
                } catch (PublisherException pe) {
                    String msg = intres.getLocalizedMessage("publisher.errorstore", pdl.getName(), fingerprint);
                    logsession.log(admin, cert, LogConstants.MODULE_CA, new java.util.Date(), username, cert, logErrorEvent, msg, pe);
                }
            }
            if (publishStatus != PublisherQueueData.STATUS_SUCCESS) {
                returnval = false;
            }
            if (log.isDebugEnabled()) {
                log.debug("KeepPublishedInQueue: " + getPublisher(pdl).getKeepPublishedInQueue());
                log.debug("UseQueueForCertificates: " + getPublisher(pdl).getUseQueueForCertificates());
            }
            if ((publishStatus != PublisherQueueData.STATUS_SUCCESS || getPublisher(pdl).getKeepPublishedInQueue())
                    && getPublisher(pdl).getUseQueueForCertificates()) {
                // Write to the publisher queue either for audit reasons or
                // to be able try again
                PublisherQueueVolatileData pqvd = new PublisherQueueVolatileData();
                pqvd.setUsername(username);
                pqvd.setPassword(password);
                pqvd.setExtendedInformation(extendedinformation);
                pqvd.setUserDN(userDN);
                String fp = CertTools.getFingerprintAsString(cert);
                try {
                    pubqueuesession.addQueueData(id.intValue(), PublisherQueueData.PUBLISH_TYPE_CERT, fp, pqvd, publishStatus);
                    String msg = intres.getLocalizedMessage("publisher.storequeue", pdl.getName(), fp, status);
                    logsession.log(admin, cert, LogConstants.MODULE_CA, new java.util.Date(), username, cert, logInfoEvent, msg);
                } catch (CreateException e) {
                    String msg = intres.getLocalizedMessage("publisher.errorstorequeue", pdl.getName(), fp, status);
                    logsession.log(admin, cert, LogConstants.MODULE_CA, new java.util.Date(), username, cert, logErrorEvent, msg, e);
                }
            }

        }
        return returnval;
    }

    /**
     * Stores the crl to the given collection of publishers. See BasePublisher
     * class for further documentation about function
     * 
     * @param publisherids
     *            a Collection (Integer) of publisherids.
     * @return true if sucessfull result on all given publishers
     * @ejb.interface-method view-type="both"
     * @see org.ejbca.core.model.ca.publisher.BasePublisher
     */
    public boolean storeCRL(Admin admin, Collection publisherids, byte[] incrl, String cafp, String userDN) {
        log.trace(">storeCRL");
        Iterator iter = publisherids.iterator();
        boolean returnval = true;
        while (iter.hasNext()) {
            int publishStatus = PublisherQueueData.STATUS_PENDING;
            Integer id = (Integer) iter.next();

            PublisherData pdl = PublisherData.findById(entityManager, id);
            // If it should be published directly
            if (!getPublisher(pdl).getOnlyUseQueue()) {
                try {
                    if (getPublisher(pdl).storeCRL(admin, incrl, cafp, userDN)) {
                        publishStatus = PublisherQueueData.STATUS_SUCCESS;
                    }
                    String msg = intres.getLocalizedMessage("publisher.store", "CRL", pdl.getName());
                    logsession.log(admin, admin.getCaId(), LogConstants.MODULE_CA, new java.util.Date(), null, null, LogConstants.EVENT_INFO_STORECRL, msg);
                } catch (PublisherException pe) {
                    String msg = intres.getLocalizedMessage("publisher.errorstore", pdl.getName(), "CRL");
                    logsession
                            .log(admin, admin.getCaId(), LogConstants.MODULE_CA, new java.util.Date(), null, null, LogConstants.EVENT_ERROR_STORECRL, msg, pe);
                }
            }
            if (publishStatus != PublisherQueueData.STATUS_SUCCESS) {
                returnval = false;
            }
            if (log.isDebugEnabled()) {
                log.debug("KeepPublishedInQueue: " + getPublisher(pdl).getKeepPublishedInQueue());
                log.debug("UseQueueForCRLs: " + getPublisher(pdl).getUseQueueForCRLs());
            }
            if ((publishStatus != PublisherQueueData.STATUS_SUCCESS || getPublisher(pdl).getKeepPublishedInQueue()) && getPublisher(pdl).getUseQueueForCRLs()) {
                // Write to the publisher queue either for audit reasons or
                // to be able try again
                final PublisherQueueVolatileData pqvd = new PublisherQueueVolatileData();
                pqvd.setUserDN(userDN);
                String fp = CertTools.getFingerprintAsString(incrl);
                try {
                    pubqueuesession.addQueueData(id.intValue(), PublisherQueueData.PUBLISH_TYPE_CRL, fp, pqvd, PublisherQueueData.STATUS_PENDING);
                    String msg = intres.getLocalizedMessage("publisher.storequeue", pdl.getName(), fp, "CRL");
                    logsession.log(admin, admin.getCaId(), LogConstants.MODULE_CA, new java.util.Date(), null, null, LogConstants.EVENT_INFO_STORECRL, msg);
                } catch (CreateException e) {
                    String msg = intres.getLocalizedMessage("publisher.errorstorequeue", pdl.getName(), fp, "CRL");
                    logsession.log(admin, admin.getCaId(), LogConstants.MODULE_CA, new java.util.Date(), null, null, LogConstants.EVENT_ERROR_STORECRL, msg, e);
                }
            }

        }
        log.trace("<storeCRL");
        return returnval;
    }

    /**
     * Test the connection to of a publisher
     * 
     * @param publisherid
     *            the id of the publisher to test.
     * @ejb.interface-method view-type="both"
     * @see org.ejbca.core.model.ca.publisher.BasePublisher
     */
    public void testConnection(Admin admin, int publisherid) throws PublisherConnectionException {
        if (log.isTraceEnabled()) {
            log.trace(">testConnection(id: " + publisherid + ")");
        }

        PublisherData pdl = PublisherData.findById(entityManager, publisherid);
        String name = pdl.getName();
        try {
            getPublisher(pdl).testConnection(admin);
            String msg = intres.getLocalizedMessage("publisher.testedpublisher", name);
            logsession.log(admin, admin.getCaId(), LogConstants.MODULE_CA, new java.util.Date(), null, null, LogConstants.EVENT_INFO_PUBLISHERDATA, msg);
        } catch (PublisherConnectionException pe) {
            String msg = intres.getLocalizedMessage("publisher.errortestpublisher", name);
            logsession.log(admin, admin.getCaId(), LogConstants.MODULE_CA, new java.util.Date(), null, null, LogConstants.EVENT_ERROR_PUBLISHERDATA, msg, pe);
            throw new PublisherConnectionException(pe.getMessage());
        }

        if (log.isTraceEnabled()) {
            log.trace("<testConnection(id: " + publisherid + ")");
        }
    }

    /**
     * Adds a publisher to the database.
     * 
     * @throws PublisherExistsException
     *             if hard token already exists.
     * @throws EJBException
     *             if a communication or other error occurs.
     * @ejb.interface-method view-type="both"
     */

    public void addPublisher(Admin admin, String name, BasePublisher publisher) throws PublisherExistsException {
        if (log.isTraceEnabled()) {
            log.trace(">addPublisher(name: " + name + ")");
        }
        addPublisher(admin, findFreePublisherId().intValue(), name, publisher);
        log.trace("<addPublisher()");
    } // addPublisher

    /**
     * Adds a publisher to the database. Used for importing and exporting
     * profiles from xml-files.
     * 
     * @throws PublisherExistsException
     *             if publisher already exists.
     * @throws EJBException
     *             if a communication or other error occurs.
     * @ejb.interface-method view-type="both"
     */
    public void addPublisher(Admin admin, int id, String name, BasePublisher publisher) throws PublisherExistsException {
        if (log.isTraceEnabled()) {
            log.trace(">addPublisher(name: " + name + ", id: " + id + ")");
        }

        PublisherData.findByName(entityManager, name);

        PublisherData.findById(entityManager, id);

        try {
            entityManager.persist(new PublisherData(id, name, publisher));
        } catch (EntityExistsException e) {
            String msg = intres.getLocalizedMessage("publisher.erroraddpublisher", name);
            logsession.log(admin, admin.getCaId(), LogConstants.MODULE_CA, new java.util.Date(), null, null, LogConstants.EVENT_ERROR_PUBLISHERDATA, msg);
            throw new PublisherExistsException();

        }

        log.trace("<addPublisher()");
    }

    /**
     * Updates publisher data
     * 
     * @throws EJBException
     *             if a communication or other error occurs.
     * @ejb.interface-method view-type="both"
     */

    public void changePublisher(Admin admin, String name, BasePublisher publisher) {
        if (log.isTraceEnabled()) {
            log.trace(">changePublisher(name: " + name + ")");
        }
        boolean success = false;

        PublisherData htp = PublisherData.findByName(entityManager, name);
        htp.setPublisher(publisher);
        success = true;

        if (success) {
            String msg = intres.getLocalizedMessage("publisher.changedpublisher", name);
            logsession.log(admin, admin.getCaId(), LogConstants.MODULE_CA, new java.util.Date(), null, null, LogConstants.EVENT_INFO_PUBLISHERDATA, msg);
        } else {
            String msg = intres.getLocalizedMessage("publisher.errorchangepublisher", name);
            logsession.log(admin, admin.getCaId(), LogConstants.MODULE_CA, new java.util.Date(), null, null, LogConstants.EVENT_ERROR_PUBLISHERDATA, msg);
        }

        log.trace("<changePublisher()");
    } // changePublisher

    /**
     * Adds a publisher with the same content as the original.
     * 
     * @throws PublisherExistsException
     *             if publisher already exists.
     * @throws EJBException
     *             if a communication or other error occurs.
     * @ejb.interface-method view-type="both"
     */
    public void clonePublisher(Admin admin, String oldname, String newname) {
        if (log.isTraceEnabled()) {
            log.trace(">clonePublisher(name: " + oldname + ")");
        }
        BasePublisher publisherdata = null;
        try {
            PublisherData htp = PublisherData.findByName(entityManager, oldname);
            publisherdata = (BasePublisher) getPublisher(htp).clone();
            try {
                addPublisher(admin, newname, publisherdata);
                String msg = intres.getLocalizedMessage("publisher.clonedpublisher", newname, oldname);
                logsession.log(admin, admin.getCaId(), LogConstants.MODULE_CA, new java.util.Date(), null, null, LogConstants.EVENT_INFO_PUBLISHERDATA, msg);
            } catch (PublisherExistsException f) {
                String msg = intres.getLocalizedMessage("publisher.errorclonepublisher", newname, oldname);
                logsession.log(admin, admin.getCaId(), LogConstants.MODULE_CA, new java.util.Date(), null, null, LogConstants.EVENT_ERROR_PUBLISHERDATA, msg);
                throw f;
            }
        } catch (Exception e) {
            String msg = intres.getLocalizedMessage("publisher.errorclonepublisher", newname, oldname);
            log.error(msg, e);
            throw new EJBException(e);
        }
        log.trace("<clonePublisher()");
    } // clonePublisher

    /**
     * Removes a publisher from the database.
     * 
     * @throws EJBException
     *             if a communication or other error occurs.
     * @ejb.interface-method view-type="both"
     */
    public void removePublisher(Admin admin, String name) {
        if (log.isTraceEnabled()) {
            log.trace(">removePublisher(name: " + name + ")");
        }
        try {
            PublisherData htp = PublisherData.findByName(entityManager, name);
            entityManager.remove(htp);

            String msg = intres.getLocalizedMessage("publisher.removedpublisher", name);
            logsession.log(admin, admin.getCaId(), LogConstants.MODULE_CA, new java.util.Date(), null, null, LogConstants.EVENT_INFO_PUBLISHERDATA, msg);
        } catch (Exception e) {
            String msg = intres.getLocalizedMessage("publisher.errorremovepublisher", name);
            logsession.log(admin, admin.getCaId(), LogConstants.MODULE_CA, new java.util.Date(), null, null, LogConstants.EVENT_ERROR_PUBLISHERDATA, msg, e);
        }
        log.trace("<removePublisher()");
    } // removePublisher

    /**
     * Renames a publisher
     * 
     * @throws PublisherExistsException
     *             if publisher already exists.
     * @throws EJBException
     *             if a communication or other error occurs.
     * @ejb.interface-method view-type="both"
     */
    public void renamePublisher(Admin admin, String oldname, String newname) throws PublisherExistsException {
        if (log.isTraceEnabled()) {
            log.trace(">renamePublisher(from " + oldname + " to " + newname + ")");
        }
        boolean success = false;

        PublisherData.findByName(entityManager, newname);

        PublisherData htp = PublisherData.findByName(entityManager, oldname);
        htp.setName(newname);
        success = true;

        if (success) {
            String msg = intres.getLocalizedMessage("publisher.renamedpublisher", oldname, newname);
            logsession.log(admin, admin.getCaId(), LogConstants.MODULE_CA, new java.util.Date(), null, null, LogConstants.EVENT_INFO_PUBLISHERDATA, msg);
        } else {
            String msg = intres.getLocalizedMessage("publisher.errorrenamepublisher", oldname, newname);
            logsession.log(admin, admin.getCaId(), LogConstants.MODULE_CA, new java.util.Date(), null, null, LogConstants.EVENT_ERROR_PUBLISHERDATA, msg);
        }
        if (!success) {
            throw new PublisherExistsException();
        }
        log.trace("<renamePublisher()");
    } // renameHardTokenProfile

    /**
     * Retrives a Collection of id:s (Integer) for all authorized publishers if
     * the Admin has the SUPERADMIN role.
     * 
     * Use CAAdminSession.getAuthorizedPublisherIds to get the list for any
     * administrator.
     * 
     * @param admin
     *            Should be an Admin with superadmin credentials
     * @return Collection of id:s (Integer)
     * @throws AuthorizationDeniedException
     *             if the admin does not have superadmin credentials
     * @ejb.interface-method view-type="both"
     */
    public Collection getAllPublisherIds(Admin admin) throws AuthorizationDeniedException {
        HashSet returnval = new HashSet();

        authorizationsession.isAuthorizedNoLog(admin, AccessRulesConstants.ROLE_SUPERADMINISTRATOR);
        Collection allPublishers = PublisherData.findAll(entityManager);
        Iterator i = allPublishers.iterator();
        while (i.hasNext()) {
            PublisherDataLocal next = (PublisherDataLocal) i.next();
            returnval.add(next.getId());
        }

        return returnval;
    }

    /**
     * Method creating a hashmap mapping publisher id (Integer) to publisher
     * name (String).
     * 
     * @ejb.transaction type="Supports"
     * @ejb.interface-method view-type="both"
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public HashMap getPublisherIdToNameMap(Admin admin) {
        HashMap<Integer, String> returnval = new HashMap<Integer, String>();

        for (PublisherData next : PublisherData.findAll(entityManager)) {
     
            returnval.put(next.getId(), next.getName());
        }

        return returnval;
    } // getPublisherIdToNameMap

    /**
     * Retrives a named publisher.
     * 
     * @return a BasePublisher or null of a publisher with the given id does not
     *         exist
     * 
     * @ejb.transaction type="Supports"
     * @ejb.interface-method view-type="both"
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public BasePublisher getPublisher(Admin admin, String name) {

        return getPublisher(PublisherData.findByName(entityManager, name));

    } // getPublisher

    /**
     * Finds a publisher by id.
     * 
     * @return a BasePublisher or null of a publisher with the given id does not
     *         exist
     * 
     * @ejb.transaction type="Supports"
     * @ejb.interface-method view-type="both"
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public BasePublisher getPublisher(Admin admin, int id) {
        return getPublisher(PublisherData.findById(entityManager, id));
    }

    /**
     * Help method used by publisher proxys to indicate if it is time to update
     * it's data.
     * 
     * @ejb.transaction type="Supports"
     * @ejb.interface-method view-type="both"
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public int getPublisherUpdateCount(Admin admin, int publisherid) {
        return (PublisherData.findById(entityManager, publisherid)).getUpdateCounter();
    }

    /**
     * Returns a publisher id, given it's publishers name
     * 
     * @return the id or 0 if the publisher cannot be found.
     * @ejb.transaction type="Supports"
     * @ejb.interface-method view-type="both"
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public int getPublisherId(Admin admin, String name) {
        return PublisherData.findByName(entityManager, name).getId();
    } // getPublisherId

    /**
     * Returns a publishers name given its id.
     * 
     * @return the name or null if id doesnt exists
     * @throws EJBException
     *             if a communication or other error occurs.
     * @ejb.transaction type="Supports"
     * @ejb.interface-method view-type="both"
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public String getPublisherName(Admin admin, int id) {
        if (log.isTraceEnabled()) {
            log.trace(">getPublisherName(id: " + id + ")");
        }
        String returnval = null;
        PublisherData htp = null;

        htp = PublisherData.findById(entityManager, id);
        if (htp != null) {
            returnval = htp.getName();
        }

        log.trace("<getPublisherName()");
        return returnval;
    } // getPublisherName

    /**
     * Use from Healtcheck only! Test connection for all publishers. No
     * authorization checks are performed.
     * 
     * @return an error message or an empty String if all are ok.
     * @ejb.transaction type="Supports"
     * @ejb.interface-method view-type="local"
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public String testAllConnections() {
        log.trace(">testAllPublishers");
        String returnval = "";
        Admin admin = new Admin(Admin.TYPE_INTERNALUSER);

        for (PublisherData pdl : PublisherData.findAll(entityManager)) {

            String name = pdl.getName();
            try {
                getPublisher(pdl).testConnection(admin);
            } catch (PublisherConnectionException pe) {
                String msg = intres.getLocalizedMessage("publisher.errortestpublisher", name);
                logsession.log(admin, admin.getCaId(), LogConstants.MODULE_CA, new java.util.Date(), null, null, LogConstants.EVENT_ERROR_PUBLISHERDATA, msg,
                        pe);
                returnval += "\n" + msg;
            }
        }

        log.trace("<testAllPublishers");
        return returnval;
    }

    private Integer findFreePublisherId() {
        Random ran = (new Random((new Date()).getTime()));
        int id = ran.nextInt();
        boolean foundfree = false;

        while (!foundfree) {

            if (id > 1) {
                PublisherData.findById(entityManager, id);
            }
            id = ran.nextInt();

        }
        return id;
    } // findFreePublisherId

    /**
     * Method that returns the publisher data and updates it if necessary.
     */
    private BasePublisher getPublisher(PublisherData pData) {
        BasePublisher publisher = pData.getCachedPublisher();
        if (publisher == null) {
            java.beans.XMLDecoder decoder;
            try {
                decoder = new java.beans.XMLDecoder(new java.io.ByteArrayInputStream(pData.getData().getBytes("UTF8")));
            } catch (UnsupportedEncodingException e) {
                throw new EJBException(e);
            }
            HashMap h = (HashMap) decoder.readObject();
            decoder.close();
            // Handle Base64 encoded string values
            HashMap data = new Base64GetHashMap(h);

            switch (((Integer) (data.get(BasePublisher.TYPE))).intValue()) {
            case LdapPublisher.TYPE_LDAPPUBLISHER:
                publisher = new LdapPublisher();
                break;
            case LdapSearchPublisher.TYPE_LDAPSEARCHPUBLISHER:
                publisher = new LdapSearchPublisher();
                break;
            case ActiveDirectoryPublisher.TYPE_ADPUBLISHER:
                publisher = new ActiveDirectoryPublisher();
                break;
            case CustomPublisherContainer.TYPE_CUSTOMPUBLISHERCONTAINER:
                publisher = new CustomPublisherContainer();
                break;
            case ExternalOCSPPublisher.TYPE_EXTOCSPPUBLISHER:
                publisher = new ExternalOCSPPublisher();
                break;
            }
            publisher.loadData(data);
        }
        return publisher;
    }

} // LocalPublisherSessionBean
