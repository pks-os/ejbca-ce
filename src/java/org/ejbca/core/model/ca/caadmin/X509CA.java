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
 
package org.ejbca.core.model.ca.caadmin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CRL;
import java.security.cert.CRLException;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.ocsp.OCSPObjectIdentifiers;
import org.bouncycastle.asn1.x509.AccessDescription;
import org.bouncycastle.asn1.x509.AuthorityInformationAccess;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.CRLNumber;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.X509DefaultEntryConverter;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.asn1.x509.X509ExtensionsGenerator;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.asn1.x509.X509NameEntryConverter;
import org.bouncycastle.asn1.x509.qualified.ETSIQCObjectIdentifiers;
import org.bouncycastle.asn1.x509.qualified.Iso4217CurrencyCode;
import org.bouncycastle.asn1.x509.qualified.MonetaryValue;
import org.bouncycastle.asn1.x509.qualified.QCStatement;
import org.bouncycastle.asn1.x509.qualified.RFC3739QCObjectIdentifiers;
import org.bouncycastle.asn1.x509.qualified.SemanticsInformation;
import org.bouncycastle.cms.CMSEnvelopedData;
import org.bouncycastle.cms.CMSEnvelopedDataGenerator;
import org.bouncycastle.cms.CMSProcessable;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSSignedGenerator;
import org.bouncycastle.cms.RecipientInformation;
import org.bouncycastle.cms.RecipientInformationStore;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.X509KeyUsage;
import org.bouncycastle.util.encoders.Hex;
import org.bouncycastle.x509.X509V2CRLGenerator;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.ejbca.core.ejb.ca.sign.SernoGenerator;
import org.ejbca.core.model.InternalResources;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.ca.SignRequestSignatureException;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.CmsCAService;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.CmsCAServiceInfo;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.ExtendedCAServiceInfo;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.ExtendedCAServiceNotActiveException;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.ExtendedCAServiceRequest;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.ExtendedCAServiceRequestException;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.ExtendedCAServiceResponse;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.IllegalExtendedCAServiceRequestException;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.OCSPCAServiceRequest;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.XKMSCAService;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.XKMSCAServiceInfo;
import org.ejbca.core.model.ca.catoken.CATokenConstants;
import org.ejbca.core.model.ca.catoken.CATokenContainer;
import org.ejbca.core.model.ca.catoken.CATokenInfo;
import org.ejbca.core.model.ca.catoken.CATokenOfflineException;
import org.ejbca.core.model.ca.catoken.NullCATokenInfo;
import org.ejbca.core.model.ca.certextensions.CertificateExtension;
import org.ejbca.core.model.ca.certextensions.CertificateExtensionFactory;
import org.ejbca.core.model.ca.certificateprofiles.CertificateProfile;
import org.ejbca.core.model.ca.crl.RevokedCertInfo;
import org.ejbca.core.model.ra.ExtendedInformation;
import org.ejbca.core.model.ra.UserDataVO;
import org.ejbca.core.model.ra.raadmin.EndEntityProfile;
import org.ejbca.util.CertTools;
import org.ejbca.util.cert.PrintableStringEntryConverter;
import org.ejbca.util.dn.DnComponents;




/**
 * X509CA is a implementation of a CA and holds data specific for Certificate and CRL generation 
 * according to the X509 standard. 
 *
 * @version $Id: X509CA.java,v 1.84 2008-01-10 15:27:52 anatom Exp $
 */
public class X509CA extends CA implements Serializable {

    private static final Logger log = Logger.getLogger(X509CA.class);

    /** Internal localization of logs and errors */
    private static final InternalResources intres = InternalResources.getInstance();

    // Default Values
    public static final float LATEST_VERSION = 14;

    private byte[]  keyId = new byte[] { 1, 2, 3, 4, 5 };
    
    
    // protected fields.
    protected static final String POLICIES                       = "policies";
    protected static final String SUBJECTALTNAME                 = "subjectaltname";
    protected static final String USEAUTHORITYKEYIDENTIFIER      = "useauthoritykeyidentifier";
    protected static final String AUTHORITYKEYIDENTIFIERCRITICAL = "authoritykeyidentifiercritical";
    protected static final String USECRLNUMBER                   = "usecrlnumber";
    protected static final String CRLNUMBERCRITICAL              = "crlnumbercritical";
    protected static final String DEFAULTCRLDISTPOINT            = "defaultcrldistpoint";
    protected static final String DEFAULTCRLISSUER               = "defaultcrlissuer";
    protected static final String DEFAULTOCSPSERVICELOCATOR      = "defaultocspservicelocator";
    protected static final String CADEFINEDFRESHESTCRL           = "cadefinedfreshestcrl";
    protected static final String USEUTF8POLICYTEXT              = "useutf8policytext";
    protected static final String USEPRINTABLESTRINGSUBJECTDN    = "useprintablestringsubjectdn";
    protected static final String USELDAPDNORDER                 = "useldapdnorder";

    // Public Methods
    /** Creates a new instance of CA, this constructor should be used when a new CA is created */
    public X509CA(X509CAInfo cainfo) {
      super(cainfo);  
      
      data.put(POLICIES, cainfo.getPolicies());
      data.put(SUBJECTALTNAME,  cainfo.getSubjectAltName());            
      setUseAuthorityKeyIdentifier(cainfo.getUseAuthorityKeyIdentifier());
      setAuthorityKeyIdentifierCritical(cainfo.getAuthorityKeyIdentifierCritical()); 
      setUseCRLNumber(cainfo.getUseCRLNumber());
      setCRLNumberCritical(cainfo.getCRLNumberCritical());
      setDefaultCRLDistPoint(cainfo.getDefaultCRLDistPoint());
      setDefaultCRLIssuer(cainfo.getDefaultCRLIssuer());
      setDefaultOCSPServiceLocator(cainfo.getDefaultOCSPServiceLocator());
      setFinishUser(cainfo.getFinishUser());
      setUseUTF8PolicyText(cainfo.getUseUTF8PolicyText());
      setUsePrintableStringSubjectDN(cainfo.getUsePrintableStringSubjectDN());
      setUseLdapDNOrder(cainfo.getUseLdapDnOrder());
      
      data.put(CA.CATYPE, new Integer(CAInfo.CATYPE_X509));
      data.put(VERSION, new Float(LATEST_VERSION));   
    }
    
   /** Constructor used when retrieving existing X509CA from database. 
 * @throws IllegalKeyStoreException */
    public X509CA(HashMap data, int caId, String subjectDN, String name, int status, Date updateTime) throws IllegalKeyStoreException{
    	super(data);
    	ArrayList externalcaserviceinfos = new ArrayList();
    	Iterator iter = getExternalCAServiceTypes().iterator(); 	
    	while(iter.hasNext()){
    		ExtendedCAServiceInfo info = this.getExtendedCAServiceInfo(((Integer) iter.next()).intValue());
    		if (info != null) {
        		externalcaserviceinfos.add(info);  	    			
    		}
    	}
        CAInfo info = new X509CAInfo(subjectDN, name, status, updateTime, getSubjectAltName() ,getCertificateProfileId(),  
        		  getValidity(), getExpireTime(), getCAType(), getSignedBy(), getCertificateChain(),
        		  getCAToken(caId).getCATokenInfo(), getDescription(), getRevokationReason(), getRevokationDate(), getPolicies(), getCRLPeriod(), getCRLIssueInterval(), getCRLOverlapTime(), getDeltaCRLPeriod(), getCRLPublishers(),
        		  getUseAuthorityKeyIdentifier(), getAuthorityKeyIdentifierCritical(),
        		  getUseCRLNumber(), getCRLNumberCritical(), getDefaultCRLDistPoint(), getDefaultCRLIssuer(), getDefaultOCSPServiceLocator(), getCADefinedFreshestCRL(), getFinishUser(), externalcaserviceinfos, 
        		  getUseUTF8PolicyText(), getApprovalSettings(), getNumOfRequiredApprovals(), getUsePrintableStringSubjectDN(), getUseLdapDNOrder());
        super.setCAInfo(info);
    }

    // Public Methods.
    public List getPolicies() {
    	return (List) data.get(POLICIES);
    }
    public void setPolicies(List policies) {
    	data.put(POLICIES, policies);
    }
    
    public String getSubjectAltName() { return (String) data.get(SUBJECTALTNAME);}
    
    public boolean  getUseAuthorityKeyIdentifier(){
      return ((Boolean)data.get(USEAUTHORITYKEYIDENTIFIER)).booleanValue();
    }
    public void setUseAuthorityKeyIdentifier(boolean useauthoritykeyidentifier) {
      data.put(USEAUTHORITYKEYIDENTIFIER, Boolean.valueOf(useauthoritykeyidentifier));
    }
    
    public boolean  getAuthorityKeyIdentifierCritical(){
      return ((Boolean)data.get(AUTHORITYKEYIDENTIFIERCRITICAL)).booleanValue();
    }
    public void setAuthorityKeyIdentifierCritical(boolean authoritykeyidentifiercritical) {
      data.put(AUTHORITYKEYIDENTIFIERCRITICAL, Boolean.valueOf(authoritykeyidentifiercritical));
    }

    public boolean  getUseCRLNumber(){return ((Boolean)data.get(USECRLNUMBER)).booleanValue();}
    public void setUseCRLNumber(boolean usecrlnumber) {data.put(USECRLNUMBER, Boolean.valueOf(usecrlnumber));}
    
    public boolean  getCRLNumberCritical(){return ((Boolean)data.get(CRLNUMBERCRITICAL)).booleanValue();}
    public void setCRLNumberCritical(boolean crlnumbercritical) {data.put(CRLNUMBERCRITICAL, Boolean.valueOf(crlnumbercritical));}
    
    public String  getDefaultCRLDistPoint(){return (String) data.get(DEFAULTCRLDISTPOINT);}
    public void setDefaultCRLDistPoint(String defaultcrldistpoint) {
    	if(defaultcrldistpoint == null){
    		data.put(DEFAULTCRLDISTPOINT, "");
    	}else{
    		data.put(DEFAULTCRLDISTPOINT, defaultcrldistpoint);
    	}     
    }
    public String  getDefaultCRLIssuer(){return (String) data.get(DEFAULTCRLISSUER);}
    public void setDefaultCRLIssuer(String defaultcrlissuer) {
    	if(defaultcrlissuer == null){
    		data.put(DEFAULTCRLISSUER, "");
    	}else{
    		data.put(DEFAULTCRLISSUER, defaultcrlissuer);
    	}     
    }
    
    public String  getCADefinedFreshestCRL(){
        return (String) data.get(CADEFINEDFRESHESTCRL);
    }
    
    public void setCADefinedFreshestCRL(String cadefinedfreshestcrl) {
        if(cadefinedfreshestcrl == null){
            data.put(CADEFINEDFRESHESTCRL, "");
        }else{
            data.put(CADEFINEDFRESHESTCRL, cadefinedfreshestcrl);
        }     
    }
    public String  getDefaultOCSPServiceLocator(){return (String) data.get(DEFAULTOCSPSERVICELOCATOR);}
    public void setDefaultOCSPServiceLocator(String defaultocsplocator) {
    	if(defaultocsplocator == null){
    		data.put(DEFAULTOCSPSERVICELOCATOR, "");
    	}else{
    		data.put(DEFAULTOCSPSERVICELOCATOR, defaultocsplocator);
    	}     
    }

    public boolean  getUseUTF8PolicyText(){
        return ((Boolean)data.get(USEUTF8POLICYTEXT)).booleanValue();
      }
      public void setUseUTF8PolicyText(boolean useutf8) {
        data.put(USEUTF8POLICYTEXT, Boolean.valueOf(useutf8));
      }

      public boolean  getUsePrintableStringSubjectDN(){
    	  return ((Boolean)data.get(USEPRINTABLESTRINGSUBJECTDN)).booleanValue();
      }
      public void setUsePrintableStringSubjectDN(boolean useprintablestring) {
    	  data.put(USEPRINTABLESTRINGSUBJECTDN, Boolean.valueOf(useprintablestring));
      }

      public boolean  getUseLdapDNOrder(){
    	  return ((Boolean)data.get(USELDAPDNORDER)).booleanValue();
      }
      public void setUseLdapDNOrder(boolean useldapdnorder) {
    	  data.put(USELDAPDNORDER, Boolean.valueOf(useldapdnorder));
      }

      public void updateCA(CAInfo cainfo) throws Exception{
    	  super.updateCA(cainfo); 
    	  X509CAInfo info = (X509CAInfo) cainfo;
    	  
    	  setUseAuthorityKeyIdentifier(info.getUseAuthorityKeyIdentifier());
    	  setAuthorityKeyIdentifierCritical(info.getAuthorityKeyIdentifierCritical()); 
    	  setUseCRLNumber(info.getUseCRLNumber());
    	  setCRLNumberCritical(info.getCRLNumberCritical());
    	  setDefaultCRLDistPoint(info.getDefaultCRLDistPoint());
    	  setDefaultCRLIssuer(info.getDefaultCRLIssuer());
          setCADefinedFreshestCRL(info.getCADefinedFreshestCRL());
    	  setDefaultOCSPServiceLocator(info.getDefaultOCSPServiceLocator());
    	  setUseUTF8PolicyText(info.getUseUTF8PolicyText());
          setUsePrintableStringSubjectDN(info.getUsePrintableStringSubjectDN());
          setUseLdapDNOrder(info.getUseLdapDnOrder());
      }
    

    public byte[] createPKCS7(Certificate cert, boolean includeChain) throws SignRequestSignatureException {
        // First verify that we signed this certificate
        try {
            if (cert != null)
                cert.verify(getCAToken().getPublicKey(SecConst.CAKEYPURPOSE_CERTSIGN));
        } catch (Exception e) {
            throw new SignRequestSignatureException("Cannot verify certificate in createPKCS7(), did I sign this?");
        }
        Collection chain = getCertificateChain();
        ArrayList certList = new ArrayList();
        if (cert != null) {
            certList.add(cert);
        } 
        if (includeChain) {
            certList.addAll(chain);
        }
        try {
            CMSProcessable msg = new CMSProcessableByteArray("EJBCA".getBytes());
            CertStore certs = CertStore.getInstance("Collection", new CollectionCertStoreParameters(certList), "BC");
            CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
            if (getCAToken().getPrivateKey(SecConst.CAKEYPURPOSE_CERTSIGN) == null) {
            	String msg1 = "createPKCS7: Private key does not exist!";
            	log.debug(msg1);
            	throw new SignRequestSignatureException(msg1);
            }
            gen.addSigner(getCAToken().getPrivateKey(SecConst.CAKEYPURPOSE_CERTSIGN), (X509Certificate)getCACertificate(), CMSSignedGenerator.DIGEST_SHA1);
            gen.addCertificatesAndCRLs(certs);
            CMSSignedData s = null;
            CATokenContainer catoken = getCAToken();
            CATokenInfo tokeninfo = catoken.getCATokenInfo();
            if (catoken != null && !(tokeninfo instanceof NullCATokenInfo)) {
            	log.debug("createPKCS7: Provider="+catoken.getProvider()+" using algorithm "+getCAToken().getPrivateKey(SecConst.CAKEYPURPOSE_CERTSIGN).getAlgorithm());
            	s = gen.generate(msg, true, catoken.getProvider());
            } else {
            	String msg1 = "CA Token does not exist!";
            	log.debug(msg);
            	throw new SignRequestSignatureException(msg1);
            }
            return s.getEncoded();
        } catch (CATokenOfflineException e) {
        	throw new javax.ejb.EJBException(e);        	
        } catch (Exception e) {
            throw new javax.ejb.EJBException(e);
        }   
    }    
    
    public byte[] createRequest(Collection attributes) throws CATokenOfflineException {
    	ASN1Set attrset = null;
    	if (attributes != null) {
    		log.debug("Adding attributes in the request");
    		Iterator iter = attributes.iterator();
			ASN1EncodableVector vec = new ASN1EncodableVector();
    		while (iter.hasNext()) {
    			DEREncodable o = (DEREncodable)iter.next();
    			vec.add(o);
    			attrset = new DERSet(vec);
    		}
    	}
        X509NameEntryConverter converter = null;
        if (getUsePrintableStringSubjectDN()) {
        	converter = new PrintableStringEntryConverter();
        } else {
        	converter = new X509DefaultEntryConverter();
        }
        Vector dnorder = CertTools.getX509FieldOrder(getUseLdapDNOrder());
        X509Name x509dn = CertTools.stringToBcX509Name(getSubjectDN(), converter, dnorder);
        PKCS10CertificationRequest req;
		try {
			req = new PKCS10CertificationRequest("SHA1WithRSA",
					x509dn, getCAToken().getPublicKey(SecConst.CAKEYPURPOSE_CERTSIGN), attrset, getCAToken().getPrivateKey(SecConst.CAKEYPURPOSE_CERTSIGN), getCAToken().getProvider());
	        return req.getEncoded();
		} catch (CATokenOfflineException e) {
			throw e;
		} catch (Exception e) {
            throw new javax.ejb.EJBException(e);
		} 
    }

    public Certificate generateCertificate(UserDataVO subject, 
                                           PublicKey publicKey, 
                                           int keyusage, 
                                           Date notBefore,
                                           Date notAfter,
                                           CertificateProfile certProfile) throws Exception{
                                               
    	    	
        final String sigAlg = getCAToken().getCATokenInfo().getSignatureAlgorithm();
        X509Certificate cacert = (X509Certificate)getCACertificate();
        String dn = subject.getDN();        
        // Check if this is a root CA we are creating
        boolean isRootCA = false;
        if (certProfile.getType() == CertificateProfile.TYPE_ROOTCA) {
        	isRootCA = true;
        }
        // Set back startdate ten minutes to avoid some problems with unsynchronized clocks.
        Date now = new Date((new Date()).getTime() - 10 * 60 * 1000);
        Date firstDate = null;
        Date lastDate = null;
		Date startTimeDate = null; 
		Date endTimeDate = null; 
        // Extract requested start and endtime from end endtity profile / user data
        ExtendedInformation ei = subject.getExtendedinformation();
        if ( ei != null ) {
            String eiStartTime = ei.getCustomData(EndEntityProfile.STARTTIME);
	        String eiEndTime = ei.getCustomData(EndEntityProfile.ENDTIME);
        	if ( eiStartTime != null ) {
        		if ( eiStartTime.matches("^\\d+:\\d?\\d:\\d?\\d$") ) {
        			String[] startTimeArray = eiStartTime.split(":");
        			long relative = (Long.parseLong(startTimeArray[0])*24*60 + Long.parseLong(startTimeArray[1])*60 +
        					Long.parseLong(startTimeArray[2])) * 60 * 1000;
        			startTimeDate = new Date(now.getTime() + relative);
        		} else {
        			try {
        				startTimeDate = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.US).parse(eiStartTime);
        			} catch (ParseException e) {
        				log.error(intres.getLocalizedMessage("signsession.errorinvalidstarttime",eiStartTime));
        			}
        		}
    			if ( startTimeDate != null && startTimeDate.before(now)) {
                	startTimeDate = now;
    			}
	        }
	        if ( eiEndTime != null ) {
        		if ( eiEndTime.matches("^\\d+:\\d?\\d:\\d?\\d$") ) {
        			String[] endTimeArray = eiEndTime.split(":");
        			long relative = (Long.parseLong(endTimeArray[0])*24*60 + Long.parseLong(endTimeArray[1])*60 +
        					Long.parseLong(endTimeArray[2])) * 60 * 1000;
        			endTimeDate = new Date(now.getTime() + relative);
        		} else {
        			try {
        				endTimeDate = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.US).parse(eiEndTime);
        			} catch (ParseException e) {
        				log.error(intres.getLocalizedMessage("signsession.errorinvalidstarttime",eiEndTime));
        			}
        		}
	        }
        }
        // Find out what start and end time to actually use..
        if (certProfile.getAllowValidityOverride()) {
            // Prio 1 is infomation supplied in Extended information object. This allows RA-users to set the time-span.
            firstDate = startTimeDate;
            lastDate = endTimeDate;
            // Prio 2 is the information supplied in the arguments
            if (firstDate == null) {
            	firstDate = notBefore;
            }
            if (lastDate == null) {
            	lastDate = notAfter;
            }    	
        }
        // Prio 3 is default values
        if (firstDate == null) {
        	firstDate = now;
        }
        long val = certProfile.getValidity();        
        Date certProfileLastDate = new Date(firstDate.getTime() + ( val * 24 * 60 * 60 * 1000));
        if (lastDate == null) {
        	lastDate = certProfileLastDate;
        }
        // Limit validity: Do not allow last date to be before first date
        if (!lastDate.after(firstDate)) {
			log.error(intres.getLocalizedMessage("signsession.errorinvalidcausality",firstDate,lastDate));
        	Date tmp = lastDate;
        	lastDate = firstDate;
        	firstDate = tmp;
        }
		// Limit validity: We do not allow a certificate to be valid before the current date, i.e. not backdated start dates
    	if (firstDate.before(now)) {
			log.error(intres.getLocalizedMessage("signsession.errorbeforecurrentdate",firstDate,subject.getUsername()));
    		firstDate = now;
    		// Update valid length from the profile since the starting point has changed
			certProfileLastDate = new Date(firstDate.getTime() + ( val * 24 * 60 * 60 * 1000));
    		// Update lastDate if we use maximum validity
    		if (lastDate.equals(certProfileLastDate)) {
    			lastDate = certProfileLastDate;
    		}
    	}
		// Limit validity: We do not allow a certificate to be valid after the the validity of the certificate profile
    	if (lastDate.after(certProfileLastDate)) {
    		log.error(intres.getLocalizedMessage("signsession.errorbeyondmaxvalidity",lastDate,subject.getUsername(),certProfileLastDate));
    		lastDate = certProfileLastDate;
    	}
		// Limit validity: We do not allow a certificate to be valid after the the validity of the CA (unless it's RootCA during renewal)
        if (cacert != null && lastDate.after(cacert.getNotAfter()) && !isRootCA) {
        	log.info(intres.getLocalizedMessage("signsession.limitingvalidity", lastDate.toString(), cacert.getNotAfter()));
            lastDate = cacert.getNotAfter();
        }            
        
        X509V3CertificateGenerator certgen = new X509V3CertificateGenerator();
        // Serialnumber is random bits, where random generator is initialized by the
        // serno generator.
        BigInteger serno = SernoGenerator.instance().getSerno();
        certgen.setSerialNumber(serno);
        certgen.setNotBefore(firstDate);
        certgen.setNotAfter(lastDate);
        certgen.setSignatureAlgorithm(sigAlg);

        // Make DNs
        if(certProfile.getUseSubjectDNSubSet()){
        	dn= certProfile.createSubjectDNSubSet(dn);	
        }
        
        if(certProfile.getUseCNPostfix()){
          dn = CertTools.insertCNPostfix(dn,certProfile.getCNPostfix());	
        }
                
        X509NameEntryConverter converter = null;
        if (getUsePrintableStringSubjectDN()) {
        	converter = new PrintableStringEntryConverter();
        } else {
        	converter = new X509DefaultEntryConverter();
        }
        Vector dnorder = CertTools.getX509FieldOrder(getUseLdapDNOrder());
        certgen.setSubjectDN(CertTools.stringToBcX509Name(dn, converter, dnorder));
        // We must take the issuer DN directly from the CA-certificate otherwise we risk re-ordering the DN
        // which many applications do not like.
        if (isRootCA) {
        	// This will be an initial root CA, since no CA-certificate exists
        	// Or it is a root CA, since the cert is self signed. If it is a root CA we want to use the same encoding for subject and issuer,
        	// it might have changed over the years.
        	if (log.isDebugEnabled()) {
        		log.debug("Using subject DN also as issuer DN, because it is a root CA");
        	}
            X509Name caname = CertTools.stringToBcX509Name(getSubjectDN(), converter, dnorder);
            certgen.setIssuerDN(caname);
        } else {
        	if (log.isDebugEnabled()) {
        		log.debug("Using issuer DN directly from the CA certificate");
        	}
            certgen.setIssuerDN(cacert.getSubjectX500Principal());        	
        }
        certgen.setPublicKey(publicKey);

        //
        // X509 Certificate Extensions
        //
        
        X509ExtensionsGenerator extgen = new X509ExtensionsGenerator();
        
        // Key usage override
        if (certProfile.getAllowKeyUsageOverride() && (keyusage >= 0)) {
        	log.debug("AllowKeyUsageOverride=true. Using KeyUsage from parameter: "+keyusage);
            if ( (certProfile.getUseKeyUsage() == true) && (keyusage >=0) ){
                X509KeyUsage ku = new X509KeyUsage(keyusage);
                extgen.addExtension(
                    X509Extensions.KeyUsage, certProfile.getKeyUsageCritical(), ku);
            }
        } 
        
        // Check for standard Certificate Extensions that should be added.
        // Standard certificate extensions are defined in CertificateProfile and CertificateExtensionFactory
        // and implemented in package org.ejbca.core.model.certextensions.standard
        CertificateExtensionFactory fact = CertificateExtensionFactory.getInstance();
        List usedStdCertExt = certProfile.getUsedStandardCertificateExtensions();
        Iterator certStdExtIter = usedStdCertExt.iterator();
    	X509Extensions overridenexts = extgen.generate();
        while(certStdExtIter.hasNext()){
        	String oid = (String)certStdExtIter.next();
        	if (overridenexts.getExtension(new DERObjectIdentifier(oid)) != null) {
            	CertificateExtension certExt = fact.getStandardCertificateExtension(oid, certProfile);
            	if (certExt != null) {
            		DEREncodable value = certExt.getValue(subject, this, certProfile, publicKey);
            		if (value != null) {
            			extgen.addExtension(new DERObjectIdentifier(certExt.getOID()),certExt.isCriticalFlag(),value);        	         		         			 
            		}
            	}        		
        	} else {
        		log.debug("Extension with oid "+oid+" has been overridden, standard extension will not be added.");
        	}
        }

         // Check for custom Certificate Extensions that should be added.
         // Custom certificate extensions is defined in certextensions.properties
         fact = CertificateExtensionFactory.getInstance();
         List usedCertExt = certProfile.getUsedCertificateExtensions();
         Iterator certExtIter = usedCertExt.iterator();
         while(certExtIter.hasNext()){
        	 Integer id = (Integer) certExtIter.next();
        	 CertificateExtension certExt = fact.getCertificateExtensions(id);
        	 if (certExt != null) {
        		 if (overridenexts.getExtension(new DERObjectIdentifier(certExt.getOID())) != null) {
        			 DEREncodable value = certExt.getValue(subject, this, certProfile, publicKey);
        			 if (value != null) {
        				 extgen.addExtension(new DERObjectIdentifier(certExt.getOID()),certExt.isCriticalFlag(),value);        	         		         			 
        			 }             		
        		 } else {
             		log.debug("Extension with oid "+certExt.getOID()+" has been overridden, custom extension will not be added.");
             	}
        	 }
         }
         
         // Finally add extensions to certificate generator
         X509Extensions exts = extgen.generate();
         Enumeration en = exts.oids();
         while (en.hasMoreElements()) {
        	 DERObjectIdentifier oid = (DERObjectIdentifier)en.nextElement();
        	 X509Extension ext = exts.getExtension(oid);
        	 certgen.addExtension(oid, ext.isCritical(), ext.getValue().getOctets());
         }
         
         //
         // End of extensions
         //
         
         X509Certificate cert;
         try{
           cert = certgen.generate(getCAToken().getPrivateKey(SecConst.CAKEYPURPOSE_CERTSIGN), 
                                            getCAToken().getProvider());
         }catch(CATokenOfflineException e){
             log.debug("X509CA : CA Token STATUS OFFLINE: ", e);
             throw e; 
         }
        
        // Verify before returning
        cert.verify(getCAToken().getPublicKey(SecConst.CAKEYPURPOSE_CERTSIGN));
        
        // If we have a CA-certificate, verify that we have all path verification stuff correct
        if (cacert != null) {
        	byte[] aki = CertTools.getAuthorityKeyId(cert);
        	byte[] ski = CertTools.getSubjectKeyId(cacert);
        	if ( (aki != null) && (ski != null) ) {
            	boolean eq = Arrays.equals(aki, ski);
            	if (!eq) {
            		String akistr = new String(Hex.encode(aki));
            		String skistr = new String(Hex.encode(ski));
    				log.error(intres.getLocalizedMessage("signsession.errorpathverifykeyid",akistr, skistr));
            	}        		
        	}
        	Principal issuerDN = cert.getIssuerX500Principal();
        	Principal subjectDN = cacert.getSubjectX500Principal();
        	if ( (issuerDN != null) && (subjectDN != null) ) {
        		boolean eq = issuerDN.equals(subjectDN);
            	if (!eq) {
    				log.error(intres.getLocalizedMessage("signsession.errorpathverifydn",issuerDN.getName(), subjectDN.getName()));
            	}        		
        	}
        }
        log.debug(">X509CA: generate certificate, CA "+ this.getCAId() + " for DN: " + subject.getDN());
      return cert;                                                                                        
    }

    
    public CRL generateCRL(Collection certs, int crlnumber) 
    throws CATokenOfflineException, IllegalKeyStoreException, IOException, SignatureException, NoSuchProviderException, InvalidKeyException, CRLException, NoSuchAlgorithmException {
    	return generateCRL(certs, getCRLPeriod(), crlnumber, false, 0, null);
    }

    public CRL generateDeltaCRL(Collection certs, int crlnumber, int basecrlnumber, CertificateProfile certprof)
        throws CATokenOfflineException, IllegalKeyStoreException, IOException, SignatureException, NoSuchProviderException, InvalidKeyException, CRLException, NoSuchAlgorithmException {
    	return generateCRL(certs, getDeltaCRLPeriod(), crlnumber, true, basecrlnumber, certprof);
    }

    
    /** Generate a CRL or a deltaCRL
     * 
     * @param certs list of revoked certificates
     * @param crlnumber CRLNumber for this CRL
     * @param isDeltaCRL true if we should generate a DeltaCRL
     * @param basecrlnumber caseCRLNumber for a delta CRL, use 0 for full CRLs
     * @param certProfile certificate profile for CRL Distribution point in the CRL, or null
     * @return CRL
     * @throws CATokenOfflineException
     * @throws IllegalKeyStoreException
     * @throws IOException
     * @throws SignatureException
     * @throws NoSuchProviderException
     * @throws InvalidKeyException
     * @throws CRLException
     * @throws NoSuchAlgorithmException
     */
    private CRL generateCRL(Collection certs, int crlPeriod, int crlnumber, boolean isDeltaCRL, int basecrlnumber, CertificateProfile certProfile) 
    throws CATokenOfflineException, IllegalKeyStoreException, IOException, SignatureException, NoSuchProviderException, InvalidKeyException, CRLException, NoSuchAlgorithmException {
        final String sigAlg= getCAToken().getCATokenInfo().getSignatureAlgorithm();

        if (log.isDebugEnabled()) {
            log.debug("generateCRL("+certs.size()+", "+crlPeriod+", "+crlnumber+", "+isDeltaCRL+", "+basecrlnumber+", certProfile");        	
        }
        Date thisUpdate = new Date();
        Date nextUpdate = new Date();

        // crlperiod is hours = crlperiod*60*60*1000 milliseconds
        nextUpdate.setTime(nextUpdate.getTime() + (crlPeriod * (long)(60 * 60 * 1000)));
        X509V2CRLGenerator crlgen = new X509V2CRLGenerator();
        crlgen.setThisUpdate(thisUpdate);
        crlgen.setNextUpdate(nextUpdate);
        crlgen.setSignatureAlgorithm(sigAlg);
        // Make DNs
        X509Certificate cacert = (X509Certificate)getCACertificate();
        if (cacert == null) {
        	// This is an initial root CA, since no CA-certificate exists
        	// (I don't think we can ever get here!!!)
            X509NameEntryConverter converter = null;
            if (getUsePrintableStringSubjectDN()) {
            	converter = new PrintableStringEntryConverter();
            } else {
            	converter = new X509DefaultEntryConverter();
            }

            X509Name caname = CertTools.stringToBcX509Name(getSubjectDN(), converter, CertTools.getX509FieldOrder(getUseLdapDNOrder()));
            crlgen.setIssuerDN(caname);
        } else {
        	crlgen.setIssuerDN(cacert.getSubjectX500Principal());
        }
        if (certs != null) {            
            Iterator it = certs.iterator();
            while( it.hasNext() ) {
                RevokedCertInfo certinfo = (RevokedCertInfo)it.next();
                crlgen.addCRLEntry(certinfo.getUserCertificate(), certinfo.getRevocationDate(), certinfo.getReason());
            }
        }

        // Authority key identifier
        if (getUseAuthorityKeyIdentifier() == true) {
            SubjectPublicKeyInfo apki = new SubjectPublicKeyInfo((ASN1Sequence)new ASN1InputStream(
                new ByteArrayInputStream(getCAToken().getPublicKey(SecConst.CAKEYPURPOSE_CRLSIGN).getEncoded())).readObject());
            AuthorityKeyIdentifier aki = new AuthorityKeyIdentifier(apki);
            crlgen.addExtension(X509Extensions.AuthorityKeyIdentifier.getId(), getAuthorityKeyIdentifierCritical(), aki);
        }
        // CRLNumber extension
        if (getUseCRLNumber() == true) {
            CRLNumber crlnum = new CRLNumber(BigInteger.valueOf(crlnumber));
            crlgen.addExtension(X509Extensions.CRLNumber.getId(),  this.getCRLNumberCritical(), crlnum);
        }

        if (isDeltaCRL) {
        	// DeltaCRLIndicator extension
        	CRLNumber basecrlnum = new CRLNumber(BigInteger.valueOf(basecrlnumber));
        	crlgen.addExtension(X509Extensions.DeltaCRLIndicator.getId(), true, basecrlnum);        	
        }
    	// CRL Distribution point URI
    	if(certProfile != null) {
    		if(certProfile.getUseCRLDistributionPointOnCRL() == true) {
    			if (certProfile.getUseCRLDistributionPoint() == true) {
    				String crldistpoint = certProfile.getCRLDistributionPointURI();
    				if(certProfile.getUseDefaultCRLDistributionPoint() == true){
    					crldistpoint = getDefaultCRLDistPoint();
    				}
    				// Multiple CDPs are spearated with the ';' sign  
    				StringTokenizer tokenizer = new StringTokenizer(crldistpoint, ";", false);
    				ArrayList distpoints = new ArrayList();
    				while (tokenizer.hasMoreTokens()) {
    					String uri = tokenizer.nextToken();
    					GeneralName gn = new GeneralName(GeneralName.uniformResourceIdentifier, new DERIA5String(uri));
    					log.debug("Added CRL distpoint: "+uri);
    					ASN1EncodableVector vec = new ASN1EncodableVector();
    					vec.add(gn);
    					GeneralNames gns = new GeneralNames(new DERSequence(vec));
    					DistributionPointName dpn = new DistributionPointName(0, gns);
    					distpoints.add(new DistributionPoint(dpn, null, null));
    				}
    				if (distpoints.size() > 0) {
    					CRLDistPoint ext = new CRLDistPoint((DistributionPoint[])distpoints.toArray(new DistributionPoint[0]));
    					crlgen.addExtension(X509Extensions.CRLDistributionPoints.getId(),
    							certProfile.getCRLDistributionPointCritical(), ext);
    				}
    			}
    		}
    	}

        X509CRL crl;
        crl = crlgen.generate(getCAToken().getPrivateKey(SecConst.CAKEYPURPOSE_CRLSIGN),getCAToken().getProvider());
        // Verify before sending back
        crl.verify(getCAToken().getPublicKey(SecConst.CAKEYPURPOSE_CRLSIGN));

        return crl;        
    }    


    /** Implementation of UpgradableDataHashMap function getLatestVersion */
    public float getLatestVersion(){
       return LATEST_VERSION;
    }

    /** Implementation of UpgradableDataHashMap function upgrade. 
     */
    public void upgrade(){
    	if(Float.compare(LATEST_VERSION, getVersion()) != 0) {
            // New version of the class, upgrade
            log.info("Upgrading X509CA with version "+getVersion());

            if (data.get(DEFAULTOCSPSERVICELOCATOR) == null) {
                setDefaultCRLDistPoint("");
                setDefaultOCSPServiceLocator("");
            }
            if (data.get(CRLISSUEINTERVAL) == null) {
                setCRLIssueInterval(0);
            }
            if (data.get(CRLOVERLAPTIME) == null) {
            	// Default value 10 minutes
            	setCRLOverlapTime(10);
            }
            boolean useprintablestring = true;
            if (data.get("alwaysuseutf8subjectdn") == null) {
            	// Default value false
                if (data.get(USEUTF8POLICYTEXT) == null) {
                	setUseUTF8PolicyText(false);
                }
            } else {
            	// Use the same value as we had before when we had alwaysuseutf8subjectdn
                boolean useutf8 = ((Boolean)data.get("alwaysuseutf8subjectdn")).booleanValue();
                if (data.get(USEUTF8POLICYTEXT) == null) {
                	setUseUTF8PolicyText(useutf8);                	
                }
            	// If we had checked to use utf8 on an old CA, we do not want to use PrintableString after upgrading
            	useprintablestring = !useutf8;
            }
            if (data.get(USEPRINTABLESTRINGSUBJECTDN) == null) {
            	// Default value true (as before)
            	setUsePrintableStringSubjectDN(useprintablestring);
            }
            if (data.get(DEFAULTCRLISSUER) == null) {
            	setDefaultCRLIssuer(null);
            }
            if (data.get(USELDAPDNORDER) == null) {
            	if (DnComponents.isReverseOrder()) {
            		setUseLdapDNOrder(false);
            	} else {
                	setUseLdapDNOrder(true);            		
            	}
            }            
            if (data.get(DELTACRLPERIOD) == null) {
            	setDeltaCRLPeriod(0); // v14
            }
            
            data.put(VERSION, new Float(LATEST_VERSION));
        }  
    }

    /**
     * Method to upgrade new (or existing externacaservices)
     * This method needs to be called outside the regular upgrade
     * since the CA isn't instansiated in the regular upgrade.
     *
     */
    public boolean upgradeExtendedCAServices() {
    	boolean retval = false;
    	Collection extendedServiceTypes = getExternalCAServiceTypes();

    	if(getCAInfo().getStatus() != SecConst.CA_EXTERNAL){
    		// Create XKMS service if it does not exist
    		if (!extendedServiceTypes.contains(new Integer(ExtendedCAServiceInfo.TYPE_XKMSEXTENDEDSERVICE))){

    			String keytype = CATokenConstants.KEYALGORITHM_RSA;
    			String keyspec = "2048";

    			XKMSCAServiceInfo xKMSCAInfo =  new XKMSCAServiceInfo(ExtendedCAServiceInfo.STATUS_INACTIVE,
    					"CN=XKMSCertificate, " + getSubjectDN(),
    					"",
    					keyspec,
    					keytype);

    			XKMSCAService xkmsservice = new XKMSCAService(xKMSCAInfo);
    			try {
    				xkmsservice.init(this);
    				retval = true;
    			} catch (Exception e) {
    				CAInfo info = this.getCAInfo();
    				String caname = null;
    				if (info != null) {
    					caname = info.getName();
    				}
    				log.error(intres.getLocalizedMessage("signsession.errorupgradingxkmsservice",caname), e);
    			}
    			setExtendedCAService(xkmsservice);
    			extendedServiceTypes.add(new Integer(ExtendedCAServiceInfo.TYPE_XKMSEXTENDEDSERVICE));
    			data.put(EXTENDEDCASERVICES, extendedServiceTypes);
    		}		

    		// Create CMS service if it does not exist
    		if (!extendedServiceTypes.contains(new Integer(ExtendedCAServiceInfo.TYPE_CMSEXTENDEDSERVICE))){

    			String keytype = CATokenConstants.KEYALGORITHM_RSA;
    			String keyspec = "2048";

    			CmsCAServiceInfo cmsCAInfo =  new CmsCAServiceInfo(ExtendedCAServiceInfo.STATUS_INACTIVE,
    					"CN=CMSCertificate, " + getSubjectDN(),
    					"",
    					keyspec,
    					keytype);

    			CmsCAService cmsservice = new CmsCAService(cmsCAInfo);
    			try {
    				cmsservice.init(this);
    				retval = true;
    			} catch (Exception e) {
    				CAInfo info = this.getCAInfo();
    				String caname = null;
    				if (info != null) {
    					caname = info.getName();
    				}
    				log.error(intres.getLocalizedMessage("signsession.errorupgradingcmsservice",caname), e);
    			}
    			setExtendedCAService(cmsservice);
    			extendedServiceTypes.add(new Integer(ExtendedCAServiceInfo.TYPE_CMSEXTENDEDSERVICE));
    			data.put(EXTENDEDCASERVICES, extendedServiceTypes);
    		}		
    	}
    	return retval;
    }
	
	/** 
	 * Method used to perform an extended service.
	 */
    public ExtendedCAServiceResponse extendedService(ExtendedCAServiceRequest request) 
      throws ExtendedCAServiceRequestException, IllegalExtendedCAServiceRequestException, ExtendedCAServiceNotActiveException{
          log.debug(">extendedService()");
          if(request instanceof OCSPCAServiceRequest) {
        	  OCSPCAServiceRequest ocspServiceReq = (OCSPCAServiceRequest)request;
              boolean useCACert = ocspServiceReq.useCACert();
              try {
                  if (useCACert) {
                	  ocspServiceReq.setPrivKey(getCAToken().getPrivateKey(SecConst.CAKEYPURPOSE_CERTSIGN));
                	  ocspServiceReq.setPrivKeyProvider(getCAToken().getProvider());
                	  X509Certificate[] signerChain = (X509Certificate[])getCertificateChain().toArray(new X509Certificate[0]);
                	  List chain = Arrays.asList(signerChain);
                	  ocspServiceReq.setCertificateChain(chain);
                      // Super class handles signing with the OCSP signing certificate
                      log.debug("<extendedService(super with ca cert)");
                      return super.extendedService(ocspServiceReq);                      
                  } else {
                      // Super class handles signing with the OCSP signing certificate
                      log.debug("<extendedService(super no ca cert)");
                      return super.extendedService(request);                      
                  }
              } catch (IllegalKeyStoreException ike) {
            	  throw new ExtendedCAServiceRequestException(ike);
              } catch (CATokenOfflineException ctoe) {
            	  throw new ExtendedCAServiceRequestException(ctoe);
              } catch (IllegalArgumentException e) {
            	  log.error("IllegalArgumentException: ", e);
            	  throw new IllegalExtendedCAServiceRequestException(e);
              }
          } else {
              log.debug("<extendedService(super)");
              return super.extendedService(request);
          }
    }
    
    public byte[] encryptKeys(KeyPair keypair) throws IOException, CATokenOfflineException{    
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	ObjectOutputStream os = new ObjectOutputStream(baos);
    	os.writeObject(keypair);    	    
    	
    	CertTools.installBCProvider();
    		
        CMSEnvelopedDataGenerator edGen = new CMSEnvelopedDataGenerator();    	    	    	    	      
       
    	CMSEnvelopedData ed;
		try {
			edGen.addKeyTransRecipient( this.getCAToken().getPublicKey(SecConst.CAKEYPURPOSE_KEYENCRYPT), this.keyId);
			ed = edGen.generate(
					new CMSProcessableByteArray(baos.toByteArray()), CMSEnvelopedDataGenerator.AES256_CBC,"BC");
		} catch (Exception e) {
            log.error("-encryptKeys: ", e);
            throw new IOException(e.getMessage());        
		}
				
		
		return ed.getEncoded(); 
    }
    
    public KeyPair decryptKeys(byte[] data) throws Exception {
    	CMSEnvelopedData ed = new CMSEnvelopedData(data);   	    	
    	     
		RecipientInformationStore  recipients = ed.getRecipientInfos();           	
    	Iterator    it =  recipients.getRecipients().iterator();
    	RecipientInformation   recipient = (RecipientInformation) it.next();
    	ObjectInputStream ois = null;
    	byte[] recdata = recipient.getContent(getCAToken().getPrivateKey(SecConst.CAKEYPURPOSE_KEYENCRYPT),getCAToken().getProvider());
    	ois = new ObjectInputStream(new ByteArrayInputStream(recdata));
    	    	    	
    	return (KeyPair) ois.readObject();  
    }

	public byte[] decryptData(byte[] data, int cAKeyPurpose) throws Exception {
    	CMSEnvelopedData ed = new CMSEnvelopedData(data);
		RecipientInformationStore  recipients = ed.getRecipientInfos();           	
    	Iterator    it =  recipients.getRecipients().iterator();
    	RecipientInformation   recipient = (RecipientInformation) it.next();
    	byte[] recdata = recipient.getContent(getCAToken().getPrivateKey(cAKeyPurpose),getCAToken().getProvider());    	
    	    	    	
    	return recdata;  
	}

	public byte[] encryptData(byte[] data, int keyPurpose) throws Exception {
    	CertTools.installBCProvider();
        CMSEnvelopedDataGenerator edGen = new CMSEnvelopedDataGenerator();    	    	    	    	             
    	CMSEnvelopedData ed;
		try {
			edGen.addKeyTransRecipient( this.getCAToken().getPublicKey(keyPurpose), this.keyId);
			ed = edGen.generate(
					new CMSProcessableByteArray(data), CMSEnvelopedDataGenerator.AES256_CBC,"BC");
		} catch (Exception e) {
            log.error("-encryptKeys: ", e);
            throw new IOException(e.getMessage());        
		}				
		
		return ed.getEncoded(); 
	}
    
}
