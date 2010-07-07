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
 
package org.ejbca.core.ejb.ca.caadmin;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.cert.Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.ejbca.core.model.UpgradeableDataHashMap;
import org.ejbca.core.model.ca.caadmin.CA;
import org.ejbca.core.model.ca.caadmin.CACacheManager;
import org.ejbca.core.model.ca.caadmin.CAInfo;
import org.ejbca.core.model.ca.caadmin.CVCCA;
import org.ejbca.core.model.ca.caadmin.IllegalKeyStoreException;
import org.ejbca.core.model.ca.caadmin.X509CA;
import org.ejbca.util.Base64GetHashMap;
import org.ejbca.util.Base64PutHashMap;
import org.ejbca.util.CertTools;

/**
 * Representation of a CA instance.
 * 
 * @version $Id$
 */
@Entity
@Table(name="CAData")
public class CAData implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(CAData.class);

	private Integer cAId;
	private String name;
	private String subjectDN;
	private int status;
	private long expireTime;
	private long updateTime;
	private String data;

	/**
	 * Entity Bean holding data of a CA.
	 * @param subjectdn
	 * @param name of CA
	 * @param status initial status
	 * @param ca CA to store
	 */
	public CAData(String subjectdn, String name, int status, CA ca) {
		try {
    		setCaId(new Integer(subjectdn.hashCode()));
    		setName(name);        
    		setSubjectDN(subjectdn);
    		if (ca.getCertificateChain().size() != 0) {
    			Certificate cacert = ca.getCACertificate();
    			setExpireTime(CertTools.getNotAfter(cacert).getTime());  
    			ca.setExpireTime(CertTools.getNotAfter(cacert)); 
    		}  
    		setCA(ca);        
    		// Set status last, because it can occur in the ca object as well, but we think the one passed as argument here is what
    		// is desired primarily
    		setStatus(status);        
    		log.debug("Created CA "+ name);
		} catch(java.io.UnsupportedEncodingException e) {
			log.error("CAData caught exception trying to create: ", e);
			throw new RuntimeException(e.toString());
		}
	}
	
	public CAData() { }
	
	@Id
	@Column(name="cAId")
	public Integer getCaId() { return cAId; }
	public void setCaId(Integer cAId) { this.cAId = cAId; }

	@Column(name="name")
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }

	@Column(name="subjectDN")
	public String getSubjectDN() { return subjectDN; }
	public void setSubjectDN(String subjectDN) { this.subjectDN = subjectDN; }

	@Column(name="status", nullable=false)
	public int getStatus() { return status; }
	public void setStatus(int status) { this.status = status; }

	@Column(name="expireTime", nullable=false)
	public long getExpireTime() { return expireTime; }
	public void setExpireTime(long expireTime) { this.expireTime = expireTime; }

	/** When was this CA updated in the database */
	@Column(name="updateTime", nullable=false)
	public long getUpdateTime() { return updateTime; }
	public void setUpdateTime(long updateTime){ this.updateTime = updateTime; }

	// DB2: CLOB(100K) [100K (2GBw/o)], Derby: CLOB [2,147,483,647 characters], Informix: TEXT (2147483648 b?), Ingres: CLOB [2GB], MSSQL: TEXT [2,147,483,647 bytes], MySQL: TEXT [65535 chars], Oracle: CLOB [4G chars], Sapdb: LONG [2G chars], Sybase: TEXT [2,147,483,647 chars]  
	@Column(name="data", length=65535)
	@Lob
	public String getData() { return data; }
	public void setData(String data) { this.data = data; }

	@Transient
	public Date getUpdateTimeAsDate() {
		return new Date(getUpdateTime());
	}

	/** 
	 * Method that retrieves the CA from the database.
     * @return CA
     * @throws java.io.UnsupportedEncodingException
     * @throws IllegalKeyStoreException 
	 */
	@Transient
	public CA getCA() throws java.io.UnsupportedEncodingException, IllegalKeyStoreException {
    	// Because get methods are marked as read-only above, this method will actually not be able to upgrade
    	// use upgradeCA above for that.
		// TODO: Mark as read only?
    	return readAndUpgradeCAInternal();
	}

    /** We have an internal method for this read operation with a side-effect. 
     * This is because getCA() is a read-only method, so the possible side-effect of upgrade will not happen,
     * and therefore this internal method can be called from another non-read-only method, upgradeCA().
     * @return CA
     * @throws java.io.UnsupportedEncodingException
     * @throws IllegalKeyStoreException
     */
    private CA readAndUpgradeCAInternal() throws java.io.UnsupportedEncodingException, IllegalKeyStoreException {
        CA ca = null;
        // First check if we already have a cached instance of the CA
        ca = CACacheManager.instance().getAndUpdateCA(getCaId().intValue(), getStatus(), getExpireTime(), getName(), getSubjectDN());
        boolean isUpdated = false;
        if (ca != null) {
        	if (log.isDebugEnabled()) {
        		log.debug("Found CA ('"+ca.getName()+"', "+getCaId().intValue()+") in cache.");
        	}
        	long update = ca.getCAInfo().getUpdateTime().getTime();
        	long t = getUpdateTime();
        	//log.debug("updateTime from ca = "+update);
        	//log.debug("updateTime from db = "+t);
        	if (update < t) {
        		log.debug("CA '"+ca.getName()+"' has been updated in database, need to refresh cache");
        		isUpdated = true;
        	}
        }
        if ( (ca == null) || isUpdated) {
        	log.debug("Re-reading CA from database: "+getCaId().intValue());
            java.beans.XMLDecoder decoder = new  java.beans.XMLDecoder(new java.io.ByteArrayInputStream(getData().getBytes("UTF8")));
            HashMap h = (HashMap) decoder.readObject();
            decoder.close();
            // Handle Base64 encoded string values
            HashMap<String, ?> data = new Base64GetHashMap(h);
            
            // If CA-data is upgraded we want to save the new data, so we must get the old version before loading the data 
            // and perhaps upgrading
            float oldversion = ((Float) data.get(UpgradeableDataHashMap.VERSION)).floatValue();
            switch(((Integer)(data.get(CA.CATYPE))).intValue()){
                case CAInfo.CATYPE_X509:
                    ca = new X509CA(data, getCaId().intValue(), getSubjectDN(), getName(), getStatus(), getUpdateTimeAsDate(), new Date(getExpireTime()));                    
                    break;
                case CAInfo.CATYPE_CVC:
                    ca = new CVCCA(data, getCaId().intValue(), getSubjectDN(), getName(), getStatus(), getUpdateTimeAsDate());                    
                    break;
            }
            boolean upgradedExtendedService = ca.upgradeExtendedCAServices();
            // Compare old version with current version and save the data if there has been a change
            if ( ((ca != null) && (Float.compare(oldversion, ca.getVersion()) != 0)) || upgradedExtendedService) {
            	// Make sure we upgrade the CAToken as well, if needed
                ca.getCAToken();
                setCA(ca);
                log.debug("Stored upgraded CA ('"+ca.getName()+"', "+getCaId().intValue()+") with version "+ca.getVersion());
            }
            // We have to do the same if CAToken was upgraded
            // Add CA to the cache
            CACacheManager.instance().addCA(getCaId().intValue(), ca);
        }
        return ca;              
    }

	/** 
	 * Method that saves the CA to database.
	 * @ejb.interface-method
	 */
	public void setCA(CA ca) throws UnsupportedEncodingException {
        // We must base64 encode string for UTF safety
        HashMap a = new Base64PutHashMap();
        a.putAll((HashMap)ca.saveData());
        
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.beans.XMLEncoder encoder = new java.beans.XMLEncoder(baos);
        encoder.writeObject(a);
        encoder.close();
        String data = baos.toString("UTF8");
        log.debug("Saving CA data with length: "+data.length()+" for CA '"+ca.getName()+"'.");
        setData(data);
        setUpdateTime(new Date().getTime());
        // We have to update status as well, because it is kept in it's own database column, but only do that if it was actually provided in the request
        if (ca.getStatus() > 0) {
            setStatus(ca.getStatus());        	
        }
        // remove the CA from the cache to force an update the next time we load it
        CACacheManager.instance().removeCA(getCaId().intValue());
        // .. and we try to load it right away
        try {
			readAndUpgradeCAInternal();
		} catch (IllegalKeyStoreException e) {
			// Ok.. so we failed after all.. try loading it next time so the error is displayed as it used to..
	        CACacheManager.instance().removeCA(getCaId().intValue());
		}
	}   

	//
	// Search functions. 
	//

	public static CAData findById(EntityManager entityManager, Integer cAId) {
		return entityManager.find(CAData.class,  cAId);
	}
	
	public static CAData findByName(EntityManager entityManager, String name) {
		Query query = entityManager.createQuery("from CAData a WHERE a.name=:name");
		query.setParameter("name", name);
		return (CAData) query.getSingleResult();
	}

	public static Collection<CAData> findAll(EntityManager entityManager) {
		Query query = entityManager.createQuery("from CAData a");
		return query.getResultList();
	}
}
