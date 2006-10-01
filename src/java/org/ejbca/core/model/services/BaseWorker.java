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
package org.ejbca.core.model.services;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.ejbca.core.model.services.intervals.DummyInterval;

/**
 * Abstract base class that initializes the worker and its interval and action.
 * 
 * @author Philip Vendil 2006 sep 27
 *
 * @version $Id: BaseWorker.java,v 1.2 2006-10-01 17:46:25 herrvendil Exp $
 */
public abstract class BaseWorker implements IWorker {

	private static final Logger log = Logger.getLogger(BaseWorker.class);
	
    protected Properties properties = null;
    protected String serviceName = null;
    private IAction action = null;
    private IInterval interval = null;

	/**
	 * @see org.ejbca.core.model.services.IWorker#init(org.ejbca.core.model.services.ServiceConfiguration, java.lang.String)
	 */
	public void init(ServiceConfiguration serviceConfiguration,
			String serviceName) {
		this.serviceName = serviceName;
		this.properties = serviceConfiguration.getWorkerProperties();
		
		String actionClassPath = serviceConfiguration.getActionClassPath();
		if(actionClassPath != null){
			try {
				action = (IAction) this.getClass().getClassLoader().loadClass(actionClassPath).newInstance();
				action.init(serviceConfiguration.getActionProperties(), serviceName);
			} catch (Exception e) {
				log.error("Error Monitoring Service " + serviceName + " actionClassPath is missconfigured",e);
			}       
		}else{
			log.debug("Warning no action class i defined for the service " + serviceName);
		}
		
		String intervalClassPath = serviceConfiguration.getIntervalClassPath();
		if(intervalClassPath != null){
			try {
				interval = (IInterval) this.getClass().getClassLoader().loadClass(intervalClassPath).newInstance();
				interval.init(serviceConfiguration.getIntervalProperties(), serviceName);
			} catch (Exception e) {
				log.error("Error Monitoring Service " + serviceName + " intervalClassPath is missconfigured, service won't execute",e);
			}       
		}else{
			log.error("Error Monitoring Service " + serviceName + " intervalClassPath is missconfigured, service won't execute");
		}
		
		if(interval == null){
			interval = new DummyInterval();
		}

	}

	
	/**
	 * @see org.ejbca.core.model.services.IWorker#getNextInterval()
	 */
	public long getNextInterval() {		
		return interval.getTimeToExecution();
	}
	
	protected IAction getAction(){
		if(action == null){
			log.error("Error Monitoring Service " + serviceName + " actionClassPath is missconfigured");
		}
		return action;
	}
	
	

}
