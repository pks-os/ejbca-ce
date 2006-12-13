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

package org.ejbca.core.protocol.cmp;

import java.io.IOException;
import java.rmi.RemoteException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Properties;

import javax.ejb.CreateException;
import javax.ejb.FinderException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.x509.X509Name;
import org.ejbca.core.ejb.ServiceLocator;
import org.ejbca.core.ejb.ca.store.ICertificateStoreSessionHome;
import org.ejbca.core.ejb.ca.store.ICertificateStoreSessionRemote;
import org.ejbca.core.ejb.ra.IUserAdminSessionHome;
import org.ejbca.core.ejb.ra.IUserAdminSessionRemote;
import org.ejbca.core.model.InternalResources;
import org.ejbca.core.model.authorization.AuthorizationDeniedException;
import org.ejbca.core.model.ca.SignRequestException;
import org.ejbca.core.model.log.Admin;
import org.ejbca.core.model.ra.NotFoundException;
import org.ejbca.core.protocol.FailInfo;
import org.ejbca.core.protocol.IResponseMessage;
import org.ejbca.core.protocol.ResponseStatus;
import org.ejbca.util.Base64;
import org.ejbca.util.CertTools;

import com.novosec.pkix.asn1.cmp.PKIBody;
import com.novosec.pkix.asn1.cmp.PKIHeader;
import com.novosec.pkix.asn1.cmp.PKIMessage;
import com.novosec.pkix.asn1.cmp.RevDetails;
import com.novosec.pkix.asn1.cmp.RevReqContent;
import com.novosec.pkix.asn1.crmf.CertTemplate;

/**
 * Message handler for certificate request messages in the CRMF format
 * @author tomas
 * @version $Id: RevocationMessageHandler.java,v 1.2 2006-12-13 09:49:05 anatom Exp $
 */
public class RevocationMessageHandler implements ICmpMessageHandler {
	
	private static Logger log = Logger.getLogger(RevocationMessageHandler.class);
    /** Internal localization of logs and errors */
    private InternalResources intres = InternalResources.getInstance();
	
	/** Parameter used to authenticate RA messages if we are using RA mode to create users */
	private String raAuthenticationSecret = null;
	/** Parameter used to determine the type of prtection for the response message */
	private String responseProtection = null;
	
	private Admin admin;
	private IUserAdminSessionRemote usersession = null;
	private ICertificateStoreSessionRemote storesession = null;
	
	public RevocationMessageHandler(Admin admin, Properties prop) throws CreateException, RemoteException {
		String str = prop.getProperty("raAuthenticationSecret");
		if (StringUtils.isNotEmpty(str)) {
			log.debug("raAuthenticationSecret is not null");
			raAuthenticationSecret = str;
		}			
		str = prop.getProperty("responseProtection");
		if (StringUtils.isNotEmpty(str)) {
			log.debug("responseProtection="+str);
			responseProtection = str;
		}			
		this.admin = admin;
		// Get EJB beans, we can not use local beans here because the MBean used for the TCP listener does not work with that
		IUserAdminSessionHome userHome = (IUserAdminSessionHome) ServiceLocator.getInstance().getRemoteHome(IUserAdminSessionHome.JNDI_NAME, IUserAdminSessionHome.class);
		ICertificateStoreSessionHome storeHome = (ICertificateStoreSessionHome) ServiceLocator.getInstance().getRemoteHome(ICertificateStoreSessionHome.JNDI_NAME, ICertificateStoreSessionHome.class);
		this.usersession = userHome.create();
		this.storesession = storeHome.create();

	}
	public IResponseMessage handleMessage(BaseCmpMessage msg) {
		log.debug(">handleMessage");
		IResponseMessage resp = null;
		// if version == 1 it is cmp1999 and we should not return a message back
		// Try to find a HMAC/SHA1 protection key
		String owfAlg = null;
		String macAlg = null;
		String keyId = null;
		int iterationCount = 1024;
		PKIHeader head = msg.getHeader();
		DEROctetString os = head.getSenderKID();
		if (os != null) {
			keyId = new String(os.getOctets());
			log.debug("Found a sender keyId: "+keyId);
			try {
				ResponseStatus status = ResponseStatus.FAILURE;
				FailInfo failInfo = FailInfo.BAD_MESSAGE_CHECK;
				String failText = null;
				CmpPbeVerifyer verifyer = new CmpPbeVerifyer(raAuthenticationSecret, msg.getMessage());				
				boolean ret = verifyer.verify();
				owfAlg = verifyer.getOwfOid();
				macAlg = verifyer.getMacOid();
				iterationCount = verifyer.getIterationCount();
				if (ret) {
					// If authentication was correct, we will now try to find the certificate to revoke
					PKIMessage pkimsg = msg.getMessage();
					PKIBody body = pkimsg.getBody();
					RevReqContent rr = body.getRr();
					RevDetails rd = rr.getRevDetails(0);
					CertTemplate ct = rd.getCertDetails();
					DERInteger serno = ct.getSerialNumber();
					X509Name issuer = ct.getIssuer();
					DERBitString reasonbits = rd.getRevocationReason();
					int reason = CertTools.bitStringToRevokedCertInfo(reasonbits);
					if ( (serno != null) && (issuer != null) ) {
						String iMsg = intres.getLocalizedMessage("cmp.receivedrevreq", issuer.toString(), serno.getValue().toString(16));
						log.info(iMsg);
						try {
							String username = storesession.findUsernameByCertSerno(admin, serno.getValue(), issuer.toString());
							usersession.revokeCert(admin, serno.getValue(), issuer.toString(), username, reason);
							status = ResponseStatus.SUCCESS;
						} catch (AuthorizationDeniedException e) {
							failInfo = FailInfo.NOT_AUTHORIZED;
							String errMsg = intres.getLocalizedMessage("cmp.errornotauthrevoke", issuer.toString(), serno.getValue().toString(16));
							failText = errMsg; 
							log.error(failText);
						} catch (FinderException e) {
							failInfo = FailInfo.BAD_CERTIFICATE_ID;
							String errMsg = intres.getLocalizedMessage("cmp.errorcertnofound", issuer.toString(), serno.getValue().toString(16));
							failText = errMsg; 
							log.error(failText);
						}
					} else {
						failInfo = FailInfo.BAD_CERTIFICATE_ID;
						String errMsg = intres.getLocalizedMessage("cmp.errormissingissuerrevoke", issuer.toString(), serno.getValue().toString(16));
						failText = errMsg; 
						log.error(failText);
					}
				} else {
					String errMsg = intres.getLocalizedMessage("cmp.errorauthmessage");
					log.error(errMsg);
					failText = errMsg;
					if (verifyer.getErrMsg() != null) {
						failText = verifyer.getErrMsg();
					}
				}
				log.debug("Creating a PKI revocation message response");
				CmpRevokeResponseMessage rresp = new CmpRevokeResponseMessage();
				rresp.setRecipientNonce(msg.getSenderNonce());
				rresp.setSenderNonce(new String(Base64.encode(CmpMessageHelper.createSenderNonce())));
				rresp.setSender(msg.getRecipient());
				rresp.setRecipient(msg.getSender());
				rresp.setTransactionId(msg.getTransactionId());
				rresp.setFailInfo(failInfo);
				rresp.setFailText(failText);
				rresp.setStatus(status);
	    		// Set all protection parameters
				log.debug(responseProtection+", "+owfAlg+", "+macAlg+", "+keyId+", "+raAuthenticationSecret);
	    		if (StringUtils.equals(responseProtection, "pbe") && (owfAlg != null) && (macAlg != null) && (keyId != null) && (raAuthenticationSecret != null) ) {
	    			rresp.setPbeParameters(keyId, raAuthenticationSecret, owfAlg, macAlg, iterationCount);
	    		}
	    		resp = rresp;
				try {
					resp.create();
				} catch (InvalidKeyException e) {
					String errMsg = intres.getLocalizedMessage("cmp.errorgeneral");
					log.error(errMsg, e);			
				} catch (NoSuchAlgorithmException e) {
					String errMsg = intres.getLocalizedMessage("cmp.errorgeneral");
					log.error(errMsg, e);			
				} catch (NoSuchProviderException e) {
					String errMsg = intres.getLocalizedMessage("cmp.errorgeneral");
					log.error(errMsg, e);			
				} catch (SignRequestException e) {
					String errMsg = intres.getLocalizedMessage("cmp.errorgeneral");
					log.error(errMsg, e);			
				} catch (NotFoundException e) {
					String errMsg = intres.getLocalizedMessage("cmp.errorgeneral");
					log.error(errMsg, e);			
				} catch (IOException e) {
					String errMsg = intres.getLocalizedMessage("cmp.errorgeneral");
					log.error(errMsg, e);			
				}							

			} catch (NoSuchAlgorithmException e) {
				String errMsg = intres.getLocalizedMessage("cmp.errorcalcprotection");
				log.error(errMsg, e);			
				resp = CmpMessageHelper.createUnprotectedErrorMessage(msg, ResponseStatus.FAILURE, FailInfo.BAD_MESSAGE_CHECK, e.getMessage());
			} catch (NoSuchProviderException e) {
				String errMsg = intres.getLocalizedMessage("cmp.errorcalcprotection");
				log.error(errMsg, e);			
				resp = CmpMessageHelper.createUnprotectedErrorMessage(msg, ResponseStatus.FAILURE, FailInfo.BAD_MESSAGE_CHECK, e.getMessage());
			} catch (InvalidKeyException e) {
				String errMsg = intres.getLocalizedMessage("cmp.errorcalcprotection");
				log.error(errMsg, e);			
				resp = CmpMessageHelper.createUnprotectedErrorMessage(msg, ResponseStatus.FAILURE, FailInfo.BAD_MESSAGE_CHECK, e.getMessage());
			} catch (RemoteException e) {
				// Fatal error
				String errMsg = intres.getLocalizedMessage("cmp.errorrevoke");
				log.error(errMsg, e);			
				resp = null;
			}							
		} else {
			// If we don't have any protection to verify, we fail
			String errMsg = intres.getLocalizedMessage("cmp.errornoprot");
			resp = CmpMessageHelper.createUnprotectedErrorMessage(msg, ResponseStatus.FAILURE, FailInfo.BAD_MESSAGE_CHECK, errMsg);
		}
		
		return resp;
	}
	
}
