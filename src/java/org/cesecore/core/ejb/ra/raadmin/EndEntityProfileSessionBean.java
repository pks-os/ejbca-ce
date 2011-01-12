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
package org.cesecore.core.ejb.ra.raadmin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.log4j.Logger;
import org.cesecore.core.ejb.log.LogSessionLocal;
import org.ejbca.core.ejb.authorization.AuthorizationSessionLocal;
import org.ejbca.core.ejb.ca.caadmin.CaSessionLocal;
import org.ejbca.core.ejb.ra.raadmin.EndEntityProfileData;
import org.ejbca.core.model.InternalResources;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.log.Admin;
import org.ejbca.core.model.log.LogConstants;
import org.ejbca.core.model.ra.raadmin.EndEntityProfile;
import org.ejbca.core.model.ra.raadmin.EndEntityProfileExistsException;

/**
 * Session bean for handling EndEntityProfiles
 * 
 * @version $Id$
 */
@Stateless(mappedName = org.ejbca.core.ejb.JndiHelper.APP_JNDI_PREFIX + "EndEntityProfileSessionRemote")
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class EndEntityProfileSessionBean implements EndEntityProfileSessionLocal, EndEntityProfileSessionRemote {

    private static final Logger LOG = Logger.getLogger(EndEntityProfileSessionBean.class);

    /** Internal localization of logs and errors */
    private static final InternalResources INTRES = InternalResources.getInstance();

    private static final Random RANDOM = new Random(new Date().getTime());

    /** Cache of end entity profiles and id-name mappings */
    private static final EndEntityProfileCache profileCache = new EndEntityProfileCache();

    @PersistenceContext(unitName = "ejbca")
    private EntityManager entityManager;

    @EJB
    private AuthorizationSessionLocal authSession;
    @EJB
    private CaSessionLocal caSession;
    @EJB
    private LogSessionLocal logSession;

    /**
     * Adds a profile to the database.
     * 
     * @param admin
     *            administrator performing task
     * @param profilename
     *            readable profile name
     * @param profile
     *            profile to be added
     * 
     */
    public void addEndEntityProfile(final Admin admin, final String profilename, final EndEntityProfile profile) throws EndEntityProfileExistsException {
        addEndEntityProfile(admin, findFreeEndEntityProfileId(), profilename, profile);
    }

    /**
     * Adds a profile to the database.
     * 
     * @param admin
     *            administrator performing task
     * @param profileid
     *            internal ID of new profile, use only if you know it's right.
     * @param profilename
     *            readable profile name
     * @param profile
     *            profile to be added
     * 
     */
    public void addEndEntityProfile(final Admin admin, final int profileid, final String profilename, final EndEntityProfile profile) throws EndEntityProfileExistsException {
        if (profilename.trim().equalsIgnoreCase(EMPTY_ENDENTITYPROFILENAME)) {
            final String msg = INTRES.getLocalizedMessage("ra.erroraddprofile", profilename);
            logSession.log(admin, admin.getCaId(), LogConstants.MODULE_RA, new java.util.Date(), null, null,
                    LogConstants.EVENT_ERROR_ENDENTITYPROFILE, msg);
            final String error = "Attempted to add an end entity profile matching " + EMPTY_ENDENTITYPROFILENAME;
            LOG.error(error);
            throw new EndEntityProfileExistsException(error);
        } else if (!isFreeEndEntityProfileId(profileid)) {
        	final String msg = INTRES.getLocalizedMessage("ra.erroraddprofile", profilename);
            logSession.log(admin, admin.getCaId(), LogConstants.MODULE_RA, new java.util.Date(), null, null,
                    LogConstants.EVENT_ERROR_ENDENTITYPROFILE, msg);
            final String error = "Attempted to add an end entity profile with id: " + profileid + ", which is already in the database.";
            LOG.error(error);
            throw new EndEntityProfileExistsException(error);
        } else if (EndEntityProfileData.findByProfileName(entityManager, profilename) != null) {
        	final String msg = INTRES.getLocalizedMessage("ra.erroraddprofile", profilename);
            logSession.log(admin, admin.getCaId(), LogConstants.MODULE_RA, new java.util.Date(), null, null,
                    LogConstants.EVENT_ERROR_ENDENTITYPROFILE, msg);
            final String errorMessage = "Attempted to add an end entity profile with name " + profilename + ", which already exists in the database.";
            LOG.error(errorMessage);
            throw new EndEntityProfileExistsException(errorMessage);
        } else {
            try {
                entityManager.persist(new EndEntityProfileData(Integer.valueOf(profileid), profilename, profile));
                flushProfileCache();
                final String msg = INTRES.getLocalizedMessage("ra.addedprofile", profilename);
                logSession.log(admin, admin.getCaId(), LogConstants.MODULE_RA, new java.util.Date(), null, null,
                        LogConstants.EVENT_INFO_ENDENTITYPROFILE, msg);
            } catch (Exception e) {
            	final String msg = INTRES.getLocalizedMessage("ra.erroraddprofile", profilename);
                LOG.error(msg, e);
                logSession.log(admin, admin.getCaId(), LogConstants.MODULE_RA, new java.util.Date(), null, null,
                        LogConstants.EVENT_ERROR_ENDENTITYPROFILE, msg);
            }
        }
    }

    /**
     * Updates profile data
     */
    public void changeEndEntityProfile(final Admin admin, final String profilename, final EndEntityProfile profile) {
        internalChangeEndEntityProfileNoFlushCache(admin, profilename, profile);
        flushProfileCache();
    }

    /**
     * Adds a end entity profile to a group with the same content as the
     * original profile.
     */
    public void cloneEndEntityProfile(final Admin admin, final String orgname, final String newname) throws EndEntityProfileExistsException {
        if (newname.trim().equalsIgnoreCase(EMPTY_ENDENTITYPROFILENAME)) {
        	final String msg = INTRES.getLocalizedMessage("ra.errorcloneprofile", newname, orgname);
            logSession.log(admin, admin.getCaId(), LogConstants.MODULE_RA, new java.util.Date(), null, null,
                    LogConstants.EVENT_ERROR_ENDENTITYPROFILE, msg);
            throw new EndEntityProfileExistsException();
        }
        if (EndEntityProfileData.findByProfileName(entityManager, newname) == null) {
        	final EndEntityProfileData pdl = EndEntityProfileData.findByProfileName(entityManager, orgname);
            boolean success = false;
            if (pdl != null) {
            	try {
            		entityManager.persist(new EndEntityProfileData(Integer.valueOf(findFreeEndEntityProfileId()), newname, (EndEntityProfile) pdl.getProfile().clone()));
            		flushProfileCache();
            		final String msg = INTRES.getLocalizedMessage("ra.clonedprofile", newname, orgname);
            		logSession.log(admin, admin.getCaId(), LogConstants.MODULE_RA, new java.util.Date(), null, null,
            				LogConstants.EVENT_INFO_ENDENTITYPROFILE, msg);
            		success = true;
            	} catch (CloneNotSupportedException e) {
            	}
            }
            if (!success) {
            	final String msg = INTRES.getLocalizedMessage("ra.errorcloneprofile", newname, orgname);
                logSession.log(admin, admin.getCaId(), LogConstants.MODULE_RA, new java.util.Date(), null, null,
                        LogConstants.EVENT_ERROR_ENDENTITYPROFILE, msg);
            }
        } else {
        	final String msg = INTRES.getLocalizedMessage("ra.errorcloneprofile", newname, orgname);
            logSession.log(admin, admin.getCaId(), LogConstants.MODULE_RA, new java.util.Date(), null, null,
                    LogConstants.EVENT_ERROR_ENDENTITYPROFILE, msg);
            throw new EndEntityProfileExistsException();
        }
    }

    /**
     * Method to check if a certificateprofile exists in any of the end entity
     * profiles. Used to avoid desyncronization of certificate profile data.
     * 
     * @param profileid
     *            the certificatetype id to search for.
     * @return true if certificateprofile exists in any of the end entity
     *         profiles.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public boolean existsCertificateProfileInEndEntityProfiles(final Admin admin, final int profileid) {
        String[] availprofiles = null;
        boolean exists = false;
        final Collection<EndEntityProfileData> result = EndEntityProfileData.findAll(entityManager);
        final Iterator<EndEntityProfileData> i = result.iterator();
        while (i.hasNext() && !exists) {
            availprofiles = i.next().getProfile().getValue(EndEntityProfile.AVAILCERTPROFILES, 0).split(EndEntityProfile.SPLITCHAR);
            for (int j = 0; j < availprofiles.length; j++) {
                if (Integer.parseInt(availprofiles[j]) == profileid) {
                    exists = true;
                    break;
                }
            }
        }
        return exists;
    }

    /**
     * Method to check if a CA exists in any of the end entity profiles. Used to
     * avoid desyncronization of CA data.
     * 
     * @param caid
     *            the caid to search for.
     * @return true if ca exists in any of the end entity profiles.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public boolean existsCAInEndEntityProfiles(final Admin admin, final int caid) {
        String[] availablecas = null;
        boolean exists = false;
        final Collection<EndEntityProfileData> result = EndEntityProfileData.findAll(entityManager);
        final Iterator<EndEntityProfileData> i = result.iterator();
        while (i.hasNext() && !exists) {
        	final EndEntityProfileData ep = i.next();
            availablecas = ep.getProfile().getValue(EndEntityProfile.AVAILCAS, 0).split(EndEntityProfile.SPLITCHAR);
            for (int j = 0; j < availablecas.length; j++) {
                if (Integer.parseInt(availablecas[j]) == caid) {
                    exists = true;
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("CA exists in entity profile " + ep.getProfileName());
                    }
                    break;
                }
            }
        }
        return exists;
    }

    public int findFreeEndEntityProfileId() {
    	int id = Math.abs(RANDOM.nextInt(Integer.MAX_VALUE));
    	// Never generate id's less than 10000
        while ((id < 10000) || (EndEntityProfileData.findById(entityManager, id) != null)) {
            id = Math.abs(RANDOM.nextInt(Integer.MAX_VALUE));
        }
        return id;
    }

    /**
     * Clear and reload end entity profile caches.
     */
    public void flushProfileCache() {
        if (LOG.isTraceEnabled()) {
            LOG.trace(">flushProfileCache");
        }
        profileCache.updateProfileCache(entityManager);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Flushed profile cache");
        }
        if (LOG.isTraceEnabled()) {
            LOG.trace("<flushProfileCache");
        }
    }

    /**
     * Finds a end entity profile by id.
     * 
     * @return EndEntityProfile (cloned) or null if it does not exist
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public EndEntityProfile getEndEntityProfile(final Admin admin, final String profilename) {
        if (LOG.isTraceEnabled()) {
            LOG.trace(">getEndEntityProfile(" + profilename + ")");
        }
        EndEntityProfile returnval = null;
        if (profilename.equals(EMPTY_ENDENTITYPROFILENAME)) {
            returnval = new EndEntityProfile(true);
        } else {
        	final Integer id = (Integer) getEndEntityProfileNameIdMapInternal().get(profilename);
        	if (id != null) {
        		returnval = getEndEntityProfile(admin, id);
        	}
        }
        if (LOG.isTraceEnabled()) {
            LOG.trace("<getEndEntityProfile(" + profilename + "): " + (returnval == null ? "null" : "not null"));
        }
        return returnval;
    }

    /**
     * Retrieves a Collection of id:s (Integer) to authorized profiles.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Collection<Integer> getAuthorizedEndEntityProfileIds(final Admin admin) {
    	final ArrayList<Integer> returnval = new ArrayList<Integer>();
    	final HashSet<Integer> authorizedcaids = new HashSet<Integer>(caSession.getAvailableCAs(admin));
   
        if (authSession.isAuthorizedNoLog(admin, "/super_administrator")) {
            returnval.add(SecConst.EMPTY_ENDENTITYPROFILE);
        }

        try {
        	final Iterator<EndEntityProfileData> i = EndEntityProfileData.findAll(entityManager).iterator();
            while (i.hasNext()) {
            	final EndEntityProfileData next = i.next();
                // Check if all profiles available CAs exists in
                // authorizedcaids.
            	final String value = next.getProfile().getValue(EndEntityProfile.AVAILCAS, 0);
                // debug("AvailCAs: "+value);
                if (value != null) {
                	final String[] availablecas = value.split(EndEntityProfile.SPLITCHAR);
                    // debug("No of available CAs: "+availablecas.length);
                    boolean allexists = true;
                    for (int j = 0; j < availablecas.length; j++) {
                        // debug("Available CA["+j+"]: "+availablecas[j]);
                    	final Integer caid = Integer.valueOf(availablecas[j]);
                        // If this is the special value ALLCAs we are authorized
                        if ((caid.intValue() != SecConst.ALLCAS) && (!authorizedcaids.contains(caid))) {
                            allexists = false;
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Profile " + next.getId() + " not authorized");
                            }
                            break;
                        }
                    }
                    if (allexists) {
                        // debug("Adding "+next.getId());
                        returnval.add(next.getId());
                    }
                }
            }
        } catch (Exception e) {
        	final String msg = INTRES.getLocalizedMessage("ra.errorgetids");
            LOG.error(msg, e);
        }
        return returnval;
    }

    /**
     * Finds a end entity profile by id.
     * 
     * @return EndEntityProfile (cloned) or null if it does not exist
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public EndEntityProfile getEndEntityProfile(final Admin admin, final int id) {
        if (LOG.isTraceEnabled()) {
            LOG.trace(">getEndEntityProfile(" + id + ")");
        }
        EndEntityProfile returnval = null;
        if (id == SecConst.EMPTY_ENDENTITYPROFILE) {
            returnval = new EndEntityProfile(true);
        } else {
    		// We need to clone the profile, otherwise the cache contents will be modifyable from the outside
        	EndEntityProfile eep = getProfileCacheInternal().get(Integer.valueOf(id));
    		try {
    			if (eep != null) {
    				returnval = (EndEntityProfile)eep.clone();
    			}
    		} catch (CloneNotSupportedException e) {
    			LOG.error("Should never happen: ", e);
    			throw new RuntimeException(e);
    		}
        }
        if (LOG.isTraceEnabled()) {
            LOG.trace("<getEndEntityProfile(id): " + (returnval == null ? "null" : "not null"));
        }
        return returnval;
    }

    /**
     * Returns a end entity profiles id, given it's profilename
     * 
     * @return the id or 0 if profile cannot be found.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public int getEndEntityProfileId(final Admin admin, final String profilename) {
        if (LOG.isTraceEnabled()) {
            LOG.trace(">getEndEntityProfileId(" + profilename + ")");
        }
        int returnval = 0;
        if (profilename.trim().equalsIgnoreCase(EMPTY_ENDENTITYPROFILENAME)) {
            return SecConst.EMPTY_ENDENTITYPROFILE;
        }
        final Integer id = (Integer) getEndEntityProfileNameIdMapInternal().get(profilename);
        if (id != null) {
            returnval = id.intValue();
        }
        if (LOG.isTraceEnabled()) {
            LOG.trace("<getEndEntityProfileId(" + profilename + "): " + returnval);
        }
        return returnval;
    }

    /**
     * Returns a end entity profiles name given it's id.
     * 
     * @return profilename or null if profile id doesn't exists.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public String getEndEntityProfileName(final Admin admin, final int id) {
        if (LOG.isTraceEnabled()) {
            LOG.trace(">getEndEntityProfilename(" + id + ")");
        }
        String returnval = null;
        if (id == SecConst.EMPTY_ENDENTITYPROFILE) {
            return EMPTY_ENDENTITYPROFILENAME;
        }
        returnval = (String) getEndEntityProfileIdNameMapInternal().get(Integer.valueOf(id));
        if (LOG.isTraceEnabled()) {
            LOG.trace("<getEndEntityProfilename(" + id + "): " + returnval);
        }
        return returnval;
    }

    /**
     * Method creating a hashmap mapping profile id (Integer) to profile name
     * (String).
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public HashMap<Integer, String> getEndEntityProfileIdToNameMap(final Admin admin) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("><getEndEntityProfileIdToNameMap");
        }
        return getEndEntityProfileIdNameMapInternal();
    }


    /**
     * A method designed to be called at startuptime to (possibly) upgrade end
     * entity profiles. This method will read all End Entity Profiles and as a
     * side-effect upgrade them if the version if changed for upgrade. Can have
     * a side-effect of upgrading a profile, therefore the Required transaction
     * setting.
     * 
     * @param admin
     *            administrator calling the method
     */
    public void initializeAndUpgradeProfiles(final Admin admin) {
    	final Collection<EndEntityProfileData> result = EndEntityProfileData.findAll(entityManager);
    	final Iterator<EndEntityProfileData> iter = result.iterator();
        while (iter.hasNext()) {
        	final EndEntityProfileData pdata = iter.next();
            if (LOG.isDebugEnabled()) {
            	final String name = pdata.getProfileName();
                LOG.debug("Loaded end entity profile: " + name);
            }
            pdata.upgradeProfile();
        }
        flushProfileCache();
    }
    
    /**
     * Do not use, use changeEndEntityProfile instead. Used internally for
     * testing only. Updates a profile without flushing caches.
     */
    public void internalChangeEndEntityProfileNoFlushCache(final Admin admin, final String profilename, final EndEntityProfile profile) {
    	final EndEntityProfileData pdl = EndEntityProfileData.findByProfileName(entityManager, profilename);
        if (pdl == null) {
        	final String msg = INTRES.getLocalizedMessage("ra.errorchangeprofile", profilename);
            logSession.log(admin, admin.getCaId(), LogConstants.MODULE_RA, new java.util.Date(), null, null,
                    LogConstants.EVENT_ERROR_ENDENTITYPROFILE, msg);
        } else {
            pdl.setProfile(profile);
            final String msg = INTRES.getLocalizedMessage("ra.changedprofile", profilename);
            logSession.log(admin, admin.getCaId(), LogConstants.MODULE_RA, new java.util.Date(), null, null,
                    LogConstants.EVENT_INFO_ENDENTITYPROFILE, msg);
        }
    }

    /**
     * Removes an end entity profile from the database, does not throw any errors if the profile does not exist.
     */
    public void removeEndEntityProfile(final Admin admin, final String profilename) {
        try {
        	final EndEntityProfileData pdl = EndEntityProfileData.findByProfileName(entityManager, profilename);
        	if (pdl == null) {
        		if (LOG.isDebugEnabled()) {
        			LOG.debug("Trying to remove an end entity profile that does not exist: "+profilename);                		
        		}
        	} else {
        		entityManager.remove(pdl);
        		flushProfileCache();
        		final String msg = INTRES.getLocalizedMessage("ra.removedprofile", profilename);
        		logSession.log(admin, admin.getCaId(), LogConstants.MODULE_RA, new java.util.Date(), null, null,
        				LogConstants.EVENT_INFO_ENDENTITYPROFILE, msg);
        	}
        } catch (Exception e) {
            LOG.error("Error was caught when trying to remove end entity profile " + profilename, e);
        	final String msg = INTRES.getLocalizedMessage("ra.errorremoveprofile", profilename);
            logSession.log(admin, admin.getCaId(), LogConstants.MODULE_RA, new java.util.Date(), null, null, LogConstants.EVENT_ERROR_ENDENTITYPROFILE, msg);
        }
    }

    /**
     * Renames a end entity profile
     */
    public void renameEndEntityProfile(final Admin admin, final String oldprofilename, final String newprofilename) throws EndEntityProfileExistsException {
        if (newprofilename.trim().equalsIgnoreCase(EMPTY_ENDENTITYPROFILENAME) || oldprofilename.trim().equalsIgnoreCase(EMPTY_ENDENTITYPROFILENAME)) {
        	final String msg = INTRES.getLocalizedMessage("ra.errorrenameprofile", oldprofilename, newprofilename);
            logSession.log(admin, admin.getCaId(), LogConstants.MODULE_RA, new java.util.Date(), null, null,
                    LogConstants.EVENT_ERROR_ENDENTITYPROFILE, msg);
            throw new EndEntityProfileExistsException();
        }
        if (EndEntityProfileData.findByProfileName(entityManager, newprofilename) == null) {
        	final EndEntityProfileData pdl = EndEntityProfileData.findByProfileName(entityManager, oldprofilename);
            if (pdl == null) {
            	final String msg = INTRES.getLocalizedMessage("ra.errorrenameprofile", oldprofilename, newprofilename);
                logSession.log(admin, admin.getCaId(), LogConstants.MODULE_RA, new java.util.Date(), null, null,
                        LogConstants.EVENT_ERROR_ENDENTITYPROFILE, msg);
            } else {
                pdl.setProfileName(newprofilename);
                flushProfileCache();
                final String msg = INTRES.getLocalizedMessage("ra.renamedprofile", oldprofilename, newprofilename);
                logSession.log(admin, admin.getCaId(), LogConstants.MODULE_RA, new java.util.Date(), null, null,
                        LogConstants.EVENT_INFO_ENDENTITYPROFILE, msg);
            }
        } else {
        	final String msg = INTRES.getLocalizedMessage("ra.errorrenameprofile", oldprofilename, newprofilename);
            logSession.log(admin, admin.getCaId(), LogConstants.MODULE_RA, new java.util.Date(), null, null,
                    LogConstants.EVENT_ERROR_ENDENTITYPROFILE, msg);
            throw new EndEntityProfileExistsException();
        }
    }

    private HashMap<Integer, String> getEndEntityProfileIdNameMapInternal() {
    	if (profileCache.needsUpdate()) {
    		flushProfileCache();
    	}
        return profileCache.getIdNameMapCache();
    }

    private Map<String, Integer> getEndEntityProfileNameIdMapInternal() {
    	if (profileCache.needsUpdate()) {
    		flushProfileCache();
    	}
    	return profileCache.getNameIdMapCache();
    }

    private Map<Integer, EndEntityProfile> getProfileCacheInternal() {
    	if (profileCache.needsUpdate()) {
    		flushProfileCache();
    	}
        return profileCache.getProfileCache();
    }

    private boolean isFreeEndEntityProfileId(final int id) {
        boolean foundfree = false;
        if ( (id > 1) && (EndEntityProfileData.findById(entityManager, Integer.valueOf(id)) == null) ) {
        	foundfree = true;
        }
        return foundfree;
    }
}
