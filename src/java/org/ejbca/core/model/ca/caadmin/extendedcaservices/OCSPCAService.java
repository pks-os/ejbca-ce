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

import java.io.UnsupportedEncodingException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.JCEECPublicKey;
import org.bouncycastle.ocsp.BasicOCSPResp;
import org.bouncycastle.ocsp.OCSPException;
import org.ejbca.core.ejb.ServiceLocator;
import org.ejbca.core.model.ca.NotSupportedException;
import org.ejbca.core.model.ca.caadmin.CA;
import org.ejbca.core.model.ca.caadmin.IllegalKeyStoreException;
import org.ejbca.core.model.ca.catoken.CATokenConstants;
import org.ejbca.core.model.ca.certificateprofiles.OCSPSignerCertificateProfile;
import org.ejbca.core.model.ra.UserDataVO;
import org.ejbca.core.protocol.ocsp.OCSPUtil;
import org.ejbca.util.Base64;
import org.ejbca.util.CertTools;
import org.ejbca.util.KeyTools;



/** Handles and maintains the CA-part of the OCSP functionality
 * 
 * @version $Id: OCSPCAService.java,v 1.9 2006-11-07 15:45:40 anatom Exp $
 */
public class OCSPCAService extends ExtendedCAService implements java.io.Serializable{

    private static Logger m_log = Logger.getLogger(OCSPCAService.class);

    public static final float LATEST_VERSION = 2; 
    
    public static final String SERVICENAME = "OCSPCASERVICE";
    public static final int TYPE = 1; 
      

    private PrivateKey      ocspsigningkey        = null;
    private List            ocspcertificatechain  = null;
    
    private OCSPCAServiceInfo info = null;  
    
    private static final String OCSPKEYSTORE   = "ocspkeystore"; 
    private static final String KEYSPEC        = "keyspec";
	private static final String KEYALGORITHM   = "keyalgorithm";
	private static final String SUBJECTDN      = "subjectdn";
	private static final String SUBJECTALTNAME = "subjectaltname";
    
	private static final String PRIVATESIGNKEYALIAS = "privatesignkeyalias";   

	/** kept for upgrade purposes 3.3 -> 3.4 */
    private static final String KEYSIZE        = "keysize";
            
    public OCSPCAService(ExtendedCAServiceInfo serviceinfo)  {
      m_log.debug("OCSPCAService : constructor " + serviceinfo.getStatus()); 
      CertTools.installBCProvider();
	  // Currently only RSA keys are supported
	  OCSPCAServiceInfo info = (OCSPCAServiceInfo) serviceinfo;	
      data = new HashMap();   
      data.put(EXTENDEDCASERVICETYPE, new Integer(ExtendedCAServiceInfo.TYPE_OCSPEXTENDEDSERVICE));

	  data.put(KEYSPEC, info.getKeySpec());
	  data.put(KEYALGORITHM, info.getKeyAlgorithm());
	  setSubjectDN(info.getSubjectDN());
	  setSubjectAltName(info.getSubjectAltName());                       
	  setStatus(serviceinfo.getStatus());
        
      data.put(VERSION, new Float(LATEST_VERSION));
    }
    
    public OCSPCAService(HashMap data) throws IllegalArgumentException, IllegalKeyStoreException {
      CertTools.installBCProvider();
      loadData(data);  
      if(data.get(OCSPKEYSTORE) != null){    
         // lookup keystore passwords      
         String keystorepass = ServiceLocator.getInstance().getString("java:comp/env/OCSPKeyStorePass");      
         if (keystorepass == null)
        	 throw new IllegalArgumentException("Missing OCSPKeyStorePass property.");
               
        try {
        	m_log.debug("Loading OCSP keystore");
            KeyStore keystore=KeyStore.getInstance("PKCS12", "BC");
            keystore.load(new java.io.ByteArrayInputStream(Base64.decode(((String) data.get(OCSPKEYSTORE)).getBytes())),keystorepass.toCharArray());
        	m_log.debug("Finished loading OCSP keystore");
      
            this.ocspsigningkey = (PrivateKey) keystore.getKey(PRIVATESIGNKEYALIAS, null);
            this.ocspcertificatechain =  Arrays.asList(keystore.getCertificateChain(PRIVATESIGNKEYALIAS));      
            this.info = new OCSPCAServiceInfo(getStatus(),
                                              getSubjectDN(),
                                              getSubjectAltName(), 
                                              (String)data.get(KEYSPEC), 
                                              (String) data.get(KEYALGORITHM),
                                              this.ocspcertificatechain);
      
        } catch (Exception e) {
            throw new IllegalKeyStoreException(e);
        }
        
        data.put(EXTENDEDCASERVICETYPE, new Integer(ExtendedCAServiceInfo.TYPE_OCSPEXTENDEDSERVICE));        
     } 
   }
    
    

   /* 
	* @see org.ejbca.core.model.ca.caadmin.extendedcaservices.ExtendedCAService#extendedService(org.ejbca.core.model.ca.caadmin.extendedcaservices.ExtendedCAServiceRequest)
	*/   
   public void init(CA ca) throws Exception {
   	 m_log.debug("OCSPCAService : init ");
	 // lookup keystore passwords      
     String keystorepass = ServiceLocator.getInstance().getString("java:comp/env/OCSPKeyStorePass");      
	 if (keystorepass == null)
	   throw new IllegalArgumentException("Missing OCSPKeyPass property.");
        
	  // Currently only RSA keys are supported
	 OCSPCAServiceInfo info = (OCSPCAServiceInfo) getExtendedCAServiceInfo();       
                  
	 // Create OSCP KeyStore	    
	 KeyStore keystore = KeyStore.getInstance("PKCS12", "BC");
	 keystore.load(null, null);                              
      
	 KeyPair ocspkeys = KeyTools.genKeys(info.getKeySpec(), info.getKeyAlgorithm());
	   	  
	 Certificate ocspcertificate =
	  ca.generateCertificate(new UserDataVO("NOUSERNAME", 	                                          
											info.getSubjectDN(),
											0, 
											info.getSubjectAltName(),
											"NOEMAIL",
											0,0,0,0, null,null,0,0,null)																																
						   , ocspkeys.getPublic(),
						   -1, // KeyUsage
						   ca.getValidity(), 
						   new OCSPSignerCertificateProfile());
	  
	 ocspcertificatechain = new ArrayList();
	 ocspcertificatechain.add(ocspcertificate);
	 ocspcertificatechain.addAll(ca.getCertificateChain());
	 this.ocspsigningkey = ocspkeys.getPrivate(); 	  	 	  
	  	  	  
     keystore.setKeyEntry(PRIVATESIGNKEYALIAS,ocspkeys.getPrivate(),null,(Certificate[]) ocspcertificatechain.toArray(new Certificate[ocspcertificatechain.size()]));              
     java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
     keystore.store(baos, keystorepass.toCharArray());
     data.put(OCSPKEYSTORE, new String(Base64.encode(baos.toByteArray())));      
     // Store OCSP KeyStore
      
	 setStatus(info.getStatus());
	 this.info = new OCSPCAServiceInfo(info.getStatus(),
									  getSubjectDN(),
									  getSubjectAltName(), 
									  (String)data.get(KEYSPEC), 
									  (String) data.get(KEYALGORITHM),
	                                   ocspcertificatechain);
      
   }   

   /* 
	* @see org.ejbca.core.model.ca.caadmin.extendedcaservices.ExtendedCAService#extendedService(org.ejbca.core.model.ca.caadmin.extendedcaservices.ExtendedCAServiceRequest)
	*/   
   public void update(ExtendedCAServiceInfo serviceinfo, CA ca) throws Exception{		   
   	   OCSPCAServiceInfo info = (OCSPCAServiceInfo) serviceinfo; 
	   m_log.debug("OCSPCAService : update " + serviceinfo.getStatus());
	   setStatus(serviceinfo.getStatus());
   	   if(info.getRenewFlag()){  	 
   	     // Renew The OCSP Signers certificate.	                            	       		 										  
		this.init(ca);
   	   }  
   	    	 
   	   // Only status is updated
	   this.info = new OCSPCAServiceInfo(serviceinfo.getStatus(),
										  getSubjectDN(),
										  getSubjectAltName(), 
										  (String) data.get(KEYSPEC), 
										  (String) data.get(KEYALGORITHM),
	                                      this.ocspcertificatechain);
										         									    	 									  
   }   



	/* 
	 * @see org.ejbca.core.model.ca.caadmin.extendedcaservices.ExtendedCAService#extendedService(org.ejbca.core.model.ca.caadmin.extendedcaservices.ExtendedCAServiceRequest)
	 */
	public ExtendedCAServiceResponse extendedService(ExtendedCAServiceRequest request) throws ExtendedCAServiceRequestException, IllegalExtendedCAServiceRequestException,ExtendedCAServiceNotActiveException {
        m_log.debug(">extendedService");
        if (!(request instanceof OCSPCAServiceRequest)) {
            throw new IllegalExtendedCAServiceRequestException();            
        }
        if (this.getStatus() != ExtendedCAServiceInfo.STATUS_ACTIVE) {
            throw new ExtendedCAServiceNotActiveException();                            
        }
        ExtendedCAServiceResponse returnval = null;
    	X509Certificate signerCert = (X509Certificate)ocspcertificatechain.get(0);
        OCSPCAServiceRequest ocspServiceReq = (OCSPCAServiceRequest)request;

        String sigAlg = ocspServiceReq.getSigAlg();
        String[] algs = StringUtils.split(sigAlg, ';');
        if ( (algs != null) && (algs.length > 1) ) {
        	PublicKey pk = signerCert.getPublicKey();
        	if (pk instanceof RSAPublicKey) {
        		if (StringUtils.contains(algs[0], CATokenConstants.KEYALGORITHM_RSA)) {
        			sigAlg = algs[0];
        		}
        		if (StringUtils.contains(algs[1], CATokenConstants.KEYALGORITHM_RSA)) {
        			sigAlg = algs[1];
        		}
        	} else if (pk instanceof JCEECPublicKey) {
        		if (StringUtils.contains(algs[0], CATokenConstants.KEYALGORITHM_ECDSA)) {
        			sigAlg = algs[0];
        		}
        		if (StringUtils.contains(algs[1], CATokenConstants.KEYALGORITHM_ECDSA)) {
        			sigAlg = algs[1];
        		}
        	}
        	m_log.debug("Using signature algorithm for response: "+sigAlg);
        }
        boolean includeChain = ocspServiceReq.includeChain();
        X509Certificate[] chain = null;
        if (includeChain) {
            chain = (X509Certificate[])this.ocspcertificatechain.toArray(new X509Certificate[0]);
        }        
        try {
        	BasicOCSPResp ocspresp = OCSPUtil.generateBasicOCSPResp(ocspServiceReq, sigAlg, signerCert, this.ocspsigningkey, "BC", chain);
            returnval = new OCSPCAServiceResponse(ocspresp, chain == null ? null : Arrays.asList(chain));             
        } catch (OCSPException ocspe) {
            throw new ExtendedCAServiceRequestException(ocspe);
        } catch (NoSuchProviderException nspe) {
            throw new ExtendedCAServiceRequestException(nspe);            
        } catch (NotSupportedException e) {
        	m_log.error("Request type not supported: ", e);
        	throw new IllegalExtendedCAServiceRequestException(e);
		} catch (IllegalArgumentException e) {
        	m_log.error("IllegalArgumentException: ", e);
        	throw new IllegalExtendedCAServiceRequestException(e);
		}
        m_log.debug("<extendedService");		  		
		return returnval;
	}

	
	public float getLatestVersion() {		
		return LATEST_VERSION;
	}

	public void upgrade() {
    	if(Float.compare(LATEST_VERSION, getVersion()) != 0) {
		  // New version of the class, upgrade
            m_log.info("upgrading OCSPCAService with version "+getVersion());
            if(data.get(KEYSPEC) == null) {
            	// Upgrade old rsa keysize to new general keyspec
            	Integer oldKeySize = (Integer)data.get(KEYSIZE);            	
                data.put(KEYSPEC, oldKeySize.toString());
            }            

		  data.put(VERSION, new Float(LATEST_VERSION));
		}  		
	}

	/* 
	 * @see org.ejbca.core.model.ca.caadmin.extendedcaservices.ExtendedCAService#getExtendedCAServiceInfo()
	 */
	public ExtendedCAServiceInfo getExtendedCAServiceInfo() {		
		if(info == null)
		  info = new OCSPCAServiceInfo(getStatus(),
		                              getSubjectDN(),
		                              getSubjectAltName(), 
		                              (String) data.get(KEYSPEC), 
		                              (String) data.get(KEYALGORITHM),
		                              this.ocspcertificatechain);
		
		return this.info;
	}
    
	
	public String getSubjectDN(){
		String retval = null;
		String str = (String)data.get(SUBJECTDN);
		 try {
			retval = new String(Base64.decode((str).getBytes("UTF-8")));
		} catch (UnsupportedEncodingException e) {
			m_log.error("Could not decode OCSP data from Base64",e);
		} catch (ArrayIndexOutOfBoundsException e) {
			// This is an old CA, where it's not Base64encoded
			m_log.debug("Old non base64 encoded DN: "+str);
			retval = str; 
		}
		
		return retval;		 
	}
    
	public void setSubjectDN(String dn){
		
		 try {
			data.put(SUBJECTDN,new String(Base64.encode(dn.getBytes("UTF-8"),false)));
		} catch (UnsupportedEncodingException e) {
			m_log.error("Could not encode OCSP data from Base64",e);
		}
	}
	
	public String getSubjectAltName(){
		String retval = null;
		String str= (String) data.get(SUBJECTALTNAME);
		 try {
			retval = new String(Base64.decode((str).getBytes("UTF-8")));
		} catch (UnsupportedEncodingException e) {
			m_log.error("Could not decode OCSP data from Base64",e);
		} catch (ArrayIndexOutOfBoundsException e) {
			// This is an old CA, where it's not Base64encoded
			m_log.debug("Old non base64 encoded altname: "+str);
			retval = str; 
		}
		
		return retval;		 
	}
    
	public void setSubjectAltName(String dn){
		
		 try {
			data.put(SUBJECTALTNAME,new String(Base64.encode(dn.getBytes("UTF-8"), false)));
		} catch (UnsupportedEncodingException e) {
			m_log.error("Could not encode OCSP data from Base64",e);
		}
	}
}

