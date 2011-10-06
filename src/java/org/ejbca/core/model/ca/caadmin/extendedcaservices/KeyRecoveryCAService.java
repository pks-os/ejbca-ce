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

package org.ejbca.core.model.ca.caadmin.extendedcaservices;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;
import org.bouncycastle.cms.CMSException;
import org.cesecore.certificates.ca.CA;
import org.cesecore.certificates.ca.extendedservices.ExtendedCAService;
import org.cesecore.certificates.ca.extendedservices.ExtendedCAServiceInfo;
import org.cesecore.certificates.ca.extendedservices.ExtendedCAServiceNotActiveException;
import org.cesecore.certificates.ca.extendedservices.ExtendedCAServiceRequest;
import org.cesecore.certificates.ca.extendedservices.ExtendedCAServiceRequestException;
import org.cesecore.certificates.ca.extendedservices.ExtendedCAServiceResponse;
import org.cesecore.certificates.ca.extendedservices.IllegalExtendedCAServiceRequestException;
import org.cesecore.util.CryptoProviderTools;
import org.ejbca.core.model.InternalEjbcaResources;

/** Handles and maintains the CA-part of the Key Recovery functionality
 * 
 * @version $Id$
 */
public class KeyRecoveryCAService extends ExtendedCAService implements Serializable {

	private static Logger log = Logger.getLogger(KeyRecoveryCAService.class);
	/** Internal localization of logs and errors */
	private static final InternalEjbcaResources intres = InternalEjbcaResources.getInstance();

	public static final float LATEST_VERSION = 1; 

	public static final String SERVICENAME = "KEYRECOVERYCASERVICE";

	public KeyRecoveryCAService(final ExtendedCAServiceInfo serviceinfo)  {
		super(serviceinfo);
		log.debug("KeyRecoveryCAService : constructor " + serviceinfo.getStatus());
		CryptoProviderTools.installBCProviderIfNotAvailable();
		data = new LinkedHashMap<Object, Object>();
		data.put(ExtendedCAServiceInfo.IMPLEMENTATIONCLASS, this.getClass().getName());
		data.put(EXTENDEDCASERVICETYPE, Integer.valueOf(ExtendedCAServiceTypes.TYPE_KEYRECOVERYEXTENDEDSERVICE));
		data.put(VERSION, new Float(LATEST_VERSION));
		setStatus(serviceinfo.getStatus());
	}

	public KeyRecoveryCAService(final HashMap data) {
		super(data);
		CryptoProviderTools.installBCProviderIfNotAvailable();
		loadData(data);
	}

	@Override
	public void init(final CA ca) throws Exception {
		log.debug("OCSPCAService : init ");
		setCA(ca);
		final ExtendedCAServiceInfo info = getExtendedCAServiceInfo();
		setStatus(info.getStatus());
	}   

	@Override
	public void update(final ExtendedCAServiceInfo serviceinfo, final CA ca) {		   
		log.debug("OCSPCAService : update " + serviceinfo.getStatus());
		setStatus(serviceinfo.getStatus());
		setCA(ca);
	}

	@Override
	public ExtendedCAServiceResponse extendedService(final ExtendedCAServiceRequest request) throws ExtendedCAServiceRequestException, IllegalExtendedCAServiceRequestException,ExtendedCAServiceNotActiveException {
		log.trace(">extendedService");
		if (this.getStatus() != ExtendedCAServiceInfo.STATUS_ACTIVE) {
			String msg = intres.getLocalizedMessage("caservice.notactive", "KeyRecovery");
			log.error(msg);
			throw new ExtendedCAServiceNotActiveException(msg);                            
		}
		if (!(request instanceof KeyRecoveryCAServiceRequest)) {
			throw new IllegalExtendedCAServiceRequestException("Not a KeyRecoveryCAServiceRequest: "+request.getClass().getName());            
		}

		final KeyRecoveryCAServiceRequest serviceReq = (KeyRecoveryCAServiceRequest)request;
		ExtendedCAServiceResponse returnval = null; 
		if (serviceReq.getCommand() == KeyRecoveryCAServiceRequest.COMMAND_ENCRYPTKEYS) {
			try{	
				returnval = new KeyRecoveryCAServiceResponse(KeyRecoveryCAServiceResponse.TYPE_ENCRYPTKEYSRESPONSE, 
						getCa().encryptKeys(serviceReq.getKeyPair()));	
			} catch(CMSException e) {
				log.error("encrypt:", e.getUnderlyingException());
				throw new IllegalExtendedCAServiceRequestException(e);
			} catch(Exception e) {
				throw new IllegalExtendedCAServiceRequestException(e);
			}
		} else {
			if (serviceReq.getCommand() == KeyRecoveryCAServiceRequest.COMMAND_DECRYPTKEYS) {
				try{
					returnval = new KeyRecoveryCAServiceResponse(KeyRecoveryCAServiceResponse.TYPE_DECRYPTKEYSRESPONSE, 
							getCa().decryptKeys(serviceReq.getKeyData()));
				} catch(CMSException e) {
					log.error("decrypt:", e.getUnderlyingException());
					throw new IllegalExtendedCAServiceRequestException(e);
				} catch(Exception e) {
					throw new IllegalExtendedCAServiceRequestException(e);
				}
			} else {
				throw new IllegalExtendedCAServiceRequestException("Illegal command: "+serviceReq.getCommand()); 
			}
		}
		return returnval;
	}

	@Override
	public float getLatestVersion() {		
		return LATEST_VERSION;
	}

	@Override
	public void upgrade() {
		if (Float.compare(LATEST_VERSION, getVersion()) != 0) {
			String msg = intres.getLocalizedMessage("caservice.upgrade", new Float(getVersion()));
			log.info(msg);
			data.put(VERSION, new Float(LATEST_VERSION));
		}  		
	}

	@Override
	public ExtendedCAServiceInfo getExtendedCAServiceInfo() {	
		return new KeyRecoveryCAServiceInfo(getStatus());
	}
}

