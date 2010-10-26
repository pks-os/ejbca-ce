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

package org.ejbca.core.ejb.hardtoken;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.apache.log4j.Logger;
import org.ejbca.core.ejb.JBossUnmarshaller;
import org.ejbca.core.model.log.Admin;
import org.ejbca.util.StringTools;

/**
 * Representation of a hard token.
 * 
 * @version $Id$
 */
@Entity
@Table(name="HardTokenData")
public class HardTokenData implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(HardTokenData.class);

	private String tokenSN;
	private String username;
	private long cTime;
	private long mTime;
	private int tokenType;
	private String significantIssuerDN;
	private Serializable data;
	private int rowVersion = 0;

	/**
	 * Entity holding data of a hard token issuer.
	 */
	public HardTokenData(String tokensn, String username, Date createtime, Date modifytime, int tokentype, String significantissuerdn, HashMap data) {
		setTokenSN(tokensn);
		setUsername(username);
		setCtime(createtime.getTime());
		setMtime(modifytime.getTime());
		setTokenType(tokentype);
		setSignificantIssuerDN(significantissuerdn);
		setData(data);
		log.debug("Created Hard Token "+ tokensn );
	}
	
	public HardTokenData() { }
	
	@Id
	@Column(name="tokenSN")
	public String getTokenSN() { return tokenSN; }
	public void setTokenSN(String tokenSN) { this.tokenSN = tokenSN; }

	@Column(name="username")
	public String getUsername() { return username; }
	public void setUsername(String username) { this.username = StringTools.strip(username); }

	@Column(name="cTime", nullable=false)
	public long getCtime() { return cTime; }
	public void setCtime(long createTime) { this.cTime = createTime; }

	@Column(name="mTime", nullable=false)
	public long getMtime() { return mTime; } 
	public void setMtime(long modifyTime) { this.mTime = modifyTime; }

	@Column(name="tokenType", nullable=false)
	public int getTokenType() { return tokenType; }
	public void setTokenType(int tokenType) { this.tokenType = tokenType; }

	@Column(name="significantIssuerDN")
	public String getSignificantIssuerDN() { return significantIssuerDN; }
	public void setSignificantIssuerDN(String significantIssuerDN) { this.significantIssuerDN = significantIssuerDN; }

	// DB2: BLOB(200K), Derby: , Informix: , Ingres: , MSSQL: , MySQL: , Oracle: , Postgres: BYTEA, Sybase: IMAGE
	@Column(name="data", length=200*1024)
	@Lob
	public Serializable getDataUnsafe() {
		HashMap h = JBossUnmarshaller.extractObject(HashMap.class, data);	// This is a workaround for JBoss J2EE CMP Serialization
		if (h != null) {
			setDataUnsafe(h);
		}
		return data;
	}
	/** DO NOT USE! Stick with setData(HashMap data) instead. */
	public void setDataUnsafe(Serializable data) { this.data = data; }

	@Version
	@Column(name = "rowVersion", nullable = false, length = 5)
	public int getRowVersion() { return rowVersion; }
	public void setRowVersion(int rowVersion) { this.rowVersion = rowVersion; }

	@Transient
	public HashMap getData() { return (HashMap) getDataUnsafe(); }
	public void setData(HashMap data) { setDataUnsafe(data); }

	@Transient
	public Date getCreateTime() { return new Date(getCtime()); }
	public void setCreateTime(Date createtime){ setCtime(createtime.getTime()); }

	@Transient
	public Date getModifyTime(){ return new Date(getCtime()); }
	public void setModifyTime(Date modifytime){ setMtime(modifytime.getTime()); }

	//
    // Search functions. 
    //

	/** @return the found entity instance or null if the entity does not exist */
    public static HardTokenData findByTokenSN(EntityManager entityManager, String tokenSN) {
    	return entityManager.find(HardTokenData.class, tokenSN);
    }

	/** @return return the query results as a List. */
    public static List<HardTokenData> findByUsername(EntityManager entityManager, String username) {
    	Query query = entityManager.createQuery("SELECT a FROM HardTokenData a WHERE a.username=:username");
    	query.setParameter("username", username);
    	return query.getResultList();
    }

	/** @return return a List<String> of all usernames where the searchPattern matches the token serial number. */
	public static List<String> findUsernamesByHardTokenSerialNumber(EntityManager entityManager, String searchPattern, int maxResults) {
    	Query query = entityManager.createNativeQuery("SELECT DISTINCT a.username FROM HardTokenData a WHERE tokenSN LIKE '%" + searchPattern + "%'");
    	query.setMaxResults(maxResults);
    	return query.getResultList();
	}
}
