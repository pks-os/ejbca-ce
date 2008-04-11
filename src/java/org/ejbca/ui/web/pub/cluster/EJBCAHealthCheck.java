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

import java.util.Iterator;

import javax.ejb.EJBException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.ejbca.core.ejb.ca.caadmin.ICAAdminSessionLocal;
import org.ejbca.core.ejb.ca.caadmin.ICAAdminSessionLocalHome;
import org.ejbca.core.ejb.ca.publisher.IPublisherSessionLocal;
import org.ejbca.core.ejb.ca.publisher.IPublisherSessionLocalHome;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.ca.caadmin.CAInfo;
import org.ejbca.core.model.ca.catoken.CATokenInfo;
import org.ejbca.core.model.ca.catoken.ICAToken;
import org.ejbca.core.model.ca.publisher.PublisherConnectionException;
import org.ejbca.core.model.log.Admin;



/**
 * EJBCA Health Checker. 
 * 
 * Does the following system checks.
 * 
 * * If a maintenance file is specific and the property is set to true, this message will be returned
 * * Not about to run out if memory i below value (configurable through web.xml with param "MinimumFreeMemory")
 * * Database connection can be established.
 * * All CATokens are active, if not set as offline and not set to specifically not be monitored
 * * All Publishers can establish connection
 * 
 * @author Philip Vendil
 * @version $Id: EJBCAHealthCheck.java,v 1.9 2008-04-11 18:04:14 anatom Exp $
 */

public class EJBCAHealthCheck extends CommonHealthCheck {
	
	private static Logger log = Logger.getLogger(EJBCAHealthCheck.class);

	private Admin admin = new Admin(Admin.TYPE_INTERNALUSER);
	
	private boolean checkPublishers = false;
	
	public void init(ServletConfig config) {
		super.init(config);
		if(config.getInitParameter("CheckPublishers") != null){
			checkPublishers = config.getInitParameter("CheckPublishers").equalsIgnoreCase("TRUE");
		}
	}

	public String checkHealth(HttpServletRequest request) {
		log.debug("Starting HealthCheck requested by : " + request.getRemoteAddr());
		String errormessage = "";
		
		errormessage += checkMaintenance();
		if( !errormessage.equals("") ) { 
			// if Down for maintenance do not perform more checks
			return errormessage; 
		} 
		errormessage += checkDB();
		if(errormessage.equals("")) {
			errormessage += checkMemory();								
			errormessage += checkCAs();	

			if(checkPublishers){
				errormessage += checkPublishers();
			}
		}
		
		if(errormessage.equals("")){
			// everything seems ok.
			errormessage = null;
		}
		
		return errormessage;
	}
		
	private String checkCAs(){
		String retval = "";
		Iterator iter = getCAAdminSession().getAvailableCAs(admin).iterator();
		while(iter.hasNext()){
			CAInfo cainfo = getCAAdminSession().getCAInfo(admin,((Integer) iter.next()).intValue());
			CATokenInfo tokeninfo = cainfo.getCATokenInfo(); 
			if((cainfo.getStatus() == SecConst.CA_ACTIVE) && cainfo.getIncludeInHealthCheck()){
			  if(tokeninfo.getCATokenStatus() == ICAToken.STATUS_OFFLINE){
				retval +="\nCA: Error CA Token is disconnected, CA Name : " + cainfo.getName();
				log.error("Error CA Token is disconnected, CA Name : " + cainfo.getName());
			  }
			}
		}				
		return retval;
	}
	
	private String checkPublishers(){
		String retval = "";
		Iterator iter = getPublisherSession().getAuthorizedPublisherIds(admin).iterator();
		while(iter.hasNext()){
			Integer publisherId = (Integer) iter.next();
			try {
				getPublisherSession().testConnection(admin,publisherId.intValue());
			} catch (PublisherConnectionException e) {
				String publishername = getPublisherSession().getPublisherName(admin,publisherId.intValue());
				retval +="\nPUBL: Cannot connect to publisher " + publishername;
				log.error("Cannot connect to publisher " + publishername);
			}
		}
		return retval;
	}
	
	private IPublisherSessionLocal publishersession = null;	
	private IPublisherSessionLocal getPublisherSession(){
		if(publishersession == null){

			try {
				Context context = new InitialContext();
				publishersession = ((IPublisherSessionLocalHome) javax.rmi.PortableRemoteObject.narrow(context.lookup(
				  "PublisherSessionLocal"), IPublisherSessionLocalHome.class)).create();
			} catch (Exception e) {
				throw new EJBException(e);
			} 
			
		}
		
		return publishersession;
	}
	
	private ICAAdminSessionLocal caadminsession = null;	
	private ICAAdminSessionLocal getCAAdminSession(){
		if(caadminsession == null){

			try {
				Context context = new InitialContext();
				caadminsession = ((ICAAdminSessionLocalHome) javax.rmi.PortableRemoteObject.narrow(context.lookup(
				  "CAAdminSessionLocal"), ICAAdminSessionLocalHome.class)).create();
			} catch (Exception e) {
				throw new EJBException(e);
			} 
		}	
		return caadminsession;
	}
}
