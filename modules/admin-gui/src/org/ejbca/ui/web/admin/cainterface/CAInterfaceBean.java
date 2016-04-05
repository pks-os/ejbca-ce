/*************************************************************************
 *                                                                       *
 *  EJBCA Community: The OpenSource Certificate Authority                *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
 
package org.ejbca.ui.web.admin.cainterface;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.KeyStoreException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;

import javax.ejb.EJBException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.authorization.control.AccessControlSessionLocal;
import org.cesecore.authorization.control.CryptoTokenRules;
import org.cesecore.authorization.control.StandardRules;
import org.cesecore.certificates.ca.CAConstants;
import org.cesecore.certificates.ca.CADoesntExistsException;
import org.cesecore.certificates.ca.CAInfo;
import org.cesecore.certificates.ca.CAOfflineException;
import org.cesecore.certificates.ca.CVCCAInfo;
import org.cesecore.certificates.ca.CaSessionLocal;
import org.cesecore.certificates.ca.CvcCA;
import org.cesecore.certificates.ca.CvcPlugin;
import org.cesecore.certificates.ca.X509CAInfo;
import org.cesecore.certificates.ca.catoken.CAToken;
import org.cesecore.certificates.ca.catoken.CATokenConstants;
import org.cesecore.certificates.ca.extendedservices.ExtendedCAServiceInfo;
import org.cesecore.certificates.certificate.CertificateConstants;
import org.cesecore.certificates.certificate.CertificateCreateSessionLocal;
import org.cesecore.certificates.certificate.CertificateDataWrapper;
import org.cesecore.certificates.certificate.CertificateStatus;
import org.cesecore.certificates.certificate.CertificateStoreSessionLocal;
import org.cesecore.certificates.certificate.certextensions.CertificateExtensionException;
import org.cesecore.certificates.certificate.certextensions.standard.NameConstraint;
import org.cesecore.certificates.certificateprofile.CertificatePolicy;
import org.cesecore.certificates.certificateprofile.CertificateProfile;
import org.cesecore.certificates.certificateprofile.CertificateProfileConstants;
import org.cesecore.certificates.certificateprofile.CertificateProfileSession;
import org.cesecore.certificates.crl.CRLInfo;
import org.cesecore.certificates.crl.CrlStoreSession;
import org.cesecore.certificates.crl.RevokedCertInfo;
import org.cesecore.certificates.endentity.ExtendedInformation;
import org.cesecore.certificates.util.AlgorithmConstants;
import org.cesecore.certificates.util.AlgorithmTools;
import org.cesecore.certificates.util.DNFieldExtractor;
import org.cesecore.config.CesecoreConfiguration;
import org.cesecore.keys.token.CryptoToken;
import org.cesecore.keys.token.CryptoTokenInfo;
import org.cesecore.keys.token.CryptoTokenManagementSessionLocal;
import org.cesecore.keys.token.CryptoTokenNameInUseException;
import org.cesecore.keys.token.CryptoTokenOfflineException;
import org.cesecore.keys.token.KeyPairInfo;
import org.cesecore.keys.token.SoftCryptoToken;
import org.cesecore.util.Base64;
import org.cesecore.util.CertTools;
import org.cesecore.util.FileTools;
import org.cesecore.util.StringTools;
import org.cesecore.util.ValidityDate;
import org.ejbca.core.ejb.ca.caadmin.CAAdminSessionLocal;
import org.ejbca.core.ejb.ca.publisher.PublisherQueueSessionLocal;
import org.ejbca.core.ejb.ca.publisher.PublisherSessionLocal;
import org.ejbca.core.ejb.ca.sign.SignSession;
import org.ejbca.core.ejb.ca.store.CertReqHistorySessionLocal;
import org.ejbca.core.ejb.crl.PublishingCrlSessionLocal;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.CmsCAServiceInfo;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.HardTokenEncryptCAServiceInfo;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.KeyRecoveryCAServiceInfo;
import org.ejbca.core.model.ca.store.CertReqHistory;
import org.ejbca.core.model.util.EjbLocalHelper;
import org.ejbca.ui.web.CertificateView;
import org.ejbca.ui.web.ParameterException;
import org.ejbca.ui.web.RequestHelper;
import org.ejbca.ui.web.RevokedInfoView;
import org.ejbca.ui.web.admin.configuration.EjbcaWebBean;
import org.ejbca.ui.web.admin.configuration.InformationMemory;

/**
 * A class used as an interface between CA jsp pages and CA ejbca functions.
 *
 * @version $Id$
 */
public class CAInterfaceBean implements Serializable {

    /** Backing object for main page list of CA and CRL statuses. */
    public class CaCrlStatusInfo {
        final private String caName;
        final private boolean caService;
        final private boolean crlStatus;
        private CaCrlStatusInfo(final String caName, final boolean caService, final boolean crlStatus) {
            this.caName = caName;
            this.caService = caService;
            this.crlStatus = crlStatus;
        }
        public String getCaName() { return caName; }
        public boolean isCaService() { return caService; }
        public boolean isCrlStatus() { return crlStatus; }
    }

	private static final long serialVersionUID = 3L;
	private static final Logger log = Logger.getLogger(CAInterfaceBean.class);
	
    public static final int CATOKENTYPE_P12          = 1;
    public static final int CATOKENTYPE_HSM          = 2;
	public static final int CATOKENTYPE_NULL         = 3;

	private EjbLocalHelper ejbLocalHelper = new EjbLocalHelper();
    private AccessControlSessionLocal accessControlSession;
    private CAAdminSessionLocal caadminsession;
    private CaSessionLocal casession;
    private CertificateCreateSessionLocal certcreatesession;
    private CertificateProfileSession certificateProfileSession;
    private CertificateStoreSessionLocal certificatesession;
    private CertReqHistorySessionLocal certreqhistorysession;
    private CrlStoreSession crlStoreSession;
    private CryptoTokenManagementSessionLocal cryptoTokenManagementSession;
    private PublishingCrlSessionLocal publishingCrlSession;
    private PublisherQueueSessionLocal publisherqueuesession;
    private PublisherSessionLocal publishersession;
    private SignSession signsession; 
   
    private CADataHandler cadatahandler;
    private PublisherDataHandler publisherdatahandler;

    private boolean initialized;
    private AuthenticationToken authenticationToken;
    private InformationMemory informationmemory;
    private CAInfo cainfo;
    private EjbcaWebBean ejbcawebbean;
    /** The certification request in binary format */
    transient private byte[] request;
    private Certificate processedcert;
    private boolean isUniqueIndex;
	
	/** Creates a new instance of CaInterfaceBean */
    public CAInterfaceBean() { }

    // Public methods
    public void initialize(EjbcaWebBean ejbcawebbean) {
        if (!initialized) {
          certificatesession = ejbLocalHelper.getCertificateStoreSession();
          certreqhistorysession = ejbLocalHelper.getCertReqHistorySession();
          crlStoreSession = ejbLocalHelper.getCrlStoreSession();
          cryptoTokenManagementSession = ejbLocalHelper.getCryptoTokenManagementSession();
          caadminsession = ejbLocalHelper.getCaAdminSession();
          casession = ejbLocalHelper.getCaSession();
          accessControlSession = ejbLocalHelper.getAccessControlSession();
          signsession = ejbLocalHelper.getSignSession();
          certcreatesession = ejbLocalHelper.getCertificateCreateSession();
          publishersession = ejbLocalHelper.getPublisherSession();               
          publisherqueuesession = ejbLocalHelper.getPublisherQueueSession();
          certificateProfileSession = ejbLocalHelper.getCertificateProfileSession();
          publishingCrlSession = ejbLocalHelper.getPublishingCrlSession();
          informationmemory = ejbcawebbean.getInformationMemory();
          authenticationToken = ejbcawebbean.getAdminObject();
          this.ejbcawebbean = ejbcawebbean;

          cadatahandler = new CADataHandler(authenticationToken, ejbLocalHelper, ejbcawebbean);
          publisherdatahandler = new PublisherDataHandler(authenticationToken, publishersession, caadminsession, certificateProfileSession,  informationmemory);
          isUniqueIndex = certcreatesession.isUniqueCertificateSerialNumberIndex();
          initialized =true;
        }
    }

    public CertificateView[] getCACertificates(int caid) {
        final List<CertificateView> ret = new ArrayList<CertificateView>();
        for (final Certificate certificate : signsession.getCertificateChain(caid)) {
            RevokedInfoView revokedinfo = null;
            CertificateStatus revinfo = certificatesession.getStatus(CertTools.getIssuerDN(certificate), CertTools.getSerialNumber(certificate));
            if (revinfo != null && revinfo.revocationReason != RevokedCertInfo.NOT_REVOKED) {
                revokedinfo = new RevokedInfoView(revinfo, CertTools.getSerialNumber(certificate));
            }
            ret.add(new CertificateView(certificate, revokedinfo));
        }
        return ret.toArray(new CertificateView[0]);
    }

    /**
     * Method that returns a HashMap connecting available CAIds (Integer) to CA Names (String).
     */ 
    public Map<Integer, String>  getCAIdToNameMap(){
    	return informationmemory.getCAIdToNameMap();      
    }

    /**
     * Return the name of the CA based on its ID
     * @param caId the CA ID
     * @return the name of the CA or null if it does not exists.
     */
    public String getName(Integer caId) {
        return (String)informationmemory.getCAIdToNameMap().get(caId);
    }

    public Collection<Integer> getAuthorizedCAs(){
      return informationmemory.getAuthorizedCAIds();
    }
    
    public List<CaCrlStatusInfo> getAuthorizedInternalCaCrlStatusInfos() throws Exception {
        final List<CaCrlStatusInfo> ret = new ArrayList<CaCrlStatusInfo>();
        final Collection<Integer> caIds = getAuthorizedCAs();
        for (final Integer caId : caIds) {
            final CAInfoView cainfo = getCAInfo(caId.intValue());
            final int caStatus = cainfo.getCAInfo().getStatus();
            if (cainfo == null || caStatus == CAConstants.CA_EXTERNAL) {
                continue;
            }
            final String caName = cainfo.getCAInfo().getName();
            boolean caTokenStatus = false;
            final int cryptoTokenId = cainfo.getCAToken().getCryptoTokenId();
            try {
                caTokenStatus = cryptoTokenManagementSession.isCryptoTokenStatusActive(cryptoTokenId);
            } catch (Exception e) {
                final String msg = authenticationToken.toString() + " failed to load CryptoToken status for " + cryptoTokenId;
                if (log.isDebugEnabled()) {
                    log.debug(msg, e);
                } else {
                    log.info(msg);
                }
            }
            final boolean caService = (caStatus == CAConstants.CA_ACTIVE) && caTokenStatus;
            boolean crlStatus = true;
            final Date now = new Date();
            final CRLInfo crlinfo = getLastCRLInfo(cainfo.getCAInfo(), false);
            if ((crlinfo != null) && (now.after(crlinfo.getExpireDate()))) {
                crlStatus = false;
            }
            final CRLInfo deltacrlinfo = getLastCRLInfo(cainfo.getCAInfo(), true);
            if ((deltacrlinfo != null) && (now.after(deltacrlinfo.getExpireDate()))) {
                crlStatus = false;
            }
            ret.add(new CaCrlStatusInfo(caName, caService, crlStatus));
        }

        return ret;
    }

    /** Returns the profile name from id proxied */
    public String getCertificateProfileName(int profileid) {
    	return this.informationmemory.getCertificateProfileNameProxy().getCertificateProfileName(profileid);
    }
    
    public int getCertificateProfileId(String profilename) {
        return certificateProfileSession.getCertificateProfileId(profilename);
    }

    public CertificateProfile getCertificateProfile(final String name) throws AuthorizationDeniedException {
        final CertificateProfile certificateProfile = certificateProfileSession.getCertificateProfile(name);
        if (!authorizedToViewProfile(certificateProfile, getCertificateProfileId(name))) {
            throw new AuthorizationDeniedException("Not authorized to certificate profile");
        }
        return certificateProfile;
    }

    public CertificateProfile getCertificateProfile(final int id) throws AuthorizationDeniedException {
        final CertificateProfile certificateProfile = certificateProfileSession.getCertificateProfile(id);
        if (!authorizedToViewProfile(certificateProfile, id)) {
            throw new AuthorizationDeniedException("Not authorized to certificate profile");
        }
        return certificateProfile;
    }

    /** Help function that checks if administrator is authorized to view profile. */
    private boolean authorizedToViewProfile(CertificateProfile profile, final int id) {
        return certificateProfileSession.getAuthorizedCertificateProfileIds(authenticationToken, profile.getType()).contains(Integer.valueOf(id));
    }

    public void createCRL(int caid) throws CryptoTokenOfflineException, CAOfflineException  {      
        try {
            publishingCrlSession.forceCRL(authenticationToken, caid);
        } catch (CADoesntExistsException e) {
            throw new RuntimeException(e);
        } catch (AuthorizationDeniedException e) {
            throw new RuntimeException(e);
		}
    }

    public void createDeltaCRL(int caid) throws CryptoTokenOfflineException, CAOfflineException {      
        try {
            publishingCrlSession.forceDeltaCRL(authenticationToken, caid);
        } catch (CADoesntExistsException e) {
            throw new RuntimeException(e);
        } catch (AuthorizationDeniedException e) {
            throw new RuntimeException(e);
		}
    }

    public int getLastCRLNumber(String  issuerdn) {
    	return crlStoreSession.getLastCRLNumber(issuerdn, false);      
    }

    /**
     * @param caInfo of the CA that has issued the CRL.
     * @param deltaCRL false for complete CRL info, true for delta CRLInfo
     * @return CRLInfo of last CRL by CA or null if no CRL exists.
     */
	public CRLInfo getLastCRLInfo(CAInfo caInfo, boolean deltaCRL) {
		final String issuerdn;// use issuer DN from CA certificate. Might differ from DN in CAInfo.
		{
			final Collection<Certificate> certs = caInfo.getCertificateChain();
			final Certificate cacert = !certs.isEmpty() ? (Certificate)certs.iterator().next(): null;
			issuerdn = cacert!=null ? CertTools.getSubjectDN(cacert) : null;
		}
		return crlStoreSession.getLastCRLInfo(issuerdn, deltaCRL);          
	}

    public HashMap<Integer, String> getAvailablePublishers() {
      return publishersession.getPublisherIdToNameMap();
    }
    
    public int getPublisherQueueLength(int publisherId) {
    	return publisherqueuesession.getPendingEntriesCountForPublisher(publisherId);
    }
    
    public int[] getPublisherQueueLength(int publisherId, int[] intervalLower, int[] intervalUpper) {
    	return publisherqueuesession.getPendingEntriesCountForPublisherInIntervals(publisherId, intervalLower, intervalUpper);
    }
    
    public PublisherDataHandler getPublisherDataHandler() {    
    	return this.publisherdatahandler;
    }
    
    public CADataHandler getCADataHandler(){
      return cadatahandler;   
    }
    
    public CAInfoView getCAInfo(String name) throws CADoesntExistsException, AuthorizationDeniedException {
      return cadatahandler.getCAInfo(name);   
    }
    
    public CAInfoView getCAInfoNoAuth(String name) throws CADoesntExistsException {
        return cadatahandler.getCAInfoNoAuth(name);   
     }

    public CAInfoView getCAInfo(int caid) throws CADoesntExistsException, AuthorizationDeniedException {
      return cadatahandler.getCAInfo(caid);   
    }  
    
    public CAInfoView getCAInfoNoAuth(int caid) throws CADoesntExistsException {
        return cadatahandler.getCAInfoNoAuth(caid);   
     }
    
    @Deprecated
    public void saveRequestInfo(CAInfo cainfo){
    	this.cainfo = cainfo;
    }
    
    @Deprecated
    public CAInfo getRequestInfo(){
    	return this.cainfo;
    }
    
	public void saveRequestData(byte[] request){
		this.request = request;
	}
    
	public byte[] getRequestData(){
		return this.request;
	}    
	
	public String getRequestDataAsString() throws Exception{
		String returnval = null;	
		if(request != null ){
			returnval = RequestHelper.BEGIN_CERTIFICATE_REQUEST_WITH_NL
			+ new String(Base64.encode(request, true))
			+ RequestHelper.END_CERTIFICATE_REQUEST_WITH_NL;  
		}      
		return returnval;
	}
    
	public void saveProcessedCertificate(Certificate cert){
		this.processedcert =cert;
	}

	public Certificate getProcessedCertificate(){
		return this.processedcert;
	}    

    public byte[] getLinkCertificate(final int caId) {
        try {
            return caadminsession.getLatestLinkCertificate(caId);
        } catch (CADoesntExistsException e) {
            return null;
        }
    }    

	public String getProcessedCertificateAsString() throws Exception{
		String returnval = null;	
		if(request != null ){
			byte[] b64cert = Base64.encode(this.processedcert.getEncoded(), true);
			returnval = RequestHelper.BEGIN_CERTIFICATE_WITH_NL;
			returnval += new String(b64cert);
			returnval += RequestHelper.END_CERTIFICATE_WITH_NL;  	    
		}      
		return returnval;
	}

	public String republish(CertificateView certificateView) throws AuthorizationDeniedException {
		String returnval = "CERTREPUBLISHFAILED";
		int certificateProfileId = CertificateProfileConstants.CERTPROFILE_NO_PROFILE;
		String password = null;
		String dn = certificateView.getSubjectDN();
		ExtendedInformation ei = null;
		final CertReqHistory certreqhist = certreqhistorysession.retrieveCertReqHistory(certificateView.getSerialNumberBigInt(), certificateView.getIssuerDN());
		if (certreqhist != null) {
			// First try to look up all info using the Certificate Request History from when the certificate was issued
			// We need this since the certificate subjectDN might be a subset of the subjectDN in the template
			certificateProfileId = certreqhist.getEndEntityInformation().getCertificateProfileId();
			password = certreqhist.getEndEntityInformation().getPassword();
			dn = certreqhist.getEndEntityInformation().getCertificateDN();
			ei = certreqhist.getEndEntityInformation().getExtendedinformation();
		}
		final String fingerprint = certificateView.getSHA1Fingerprint().toLowerCase();
		final CertificateDataWrapper cdw = certificatesession.getCertificateData(fingerprint);
		if (cdw != null) {
			// If we are missing Certificate Request History for this certificate, we can at least recover some of this info
			if (certificateProfileId == CertificateProfileConstants.CERTPROFILE_NO_PROFILE) {
				certificateProfileId = cdw.getCertificateData().getCertificateProfileId();
			}
		}
		if (certificateProfileId == CertificateProfileConstants.CERTPROFILE_NO_PROFILE) {
			// If there is no cert req history and the cert profile was not defined in the CertificateData row, so we can't do anything about it..
			returnval = "CERTREQREPUBLISHFAILED";
		} else {
			final CertificateProfile certprofile = certificateProfileSession.getCertificateProfile(certificateProfileId);
			if (certprofile != null) {
				if (certprofile.getPublisherList().size() > 0) {
                    if (publishersession.storeCertificate(authenticationToken, certprofile.getPublisherList(), cdw, password, dn, ei)) {
                        returnval = "CERTREPUBLISHEDSUCCESS";
                    }
				} else {
					returnval = "NOPUBLISHERSDEFINED";
				}
			} else {
				returnval = "CERTPROFILENOTFOUND";
			}
		}
		return returnval; 
	}

	/** Class used to sort CertReq History by users modified time, with latest first*/
	private class CertReqUserCreateComparator implements Comparator<CertReqHistory> {
		@Override
		public int compare(CertReqHistory o1, CertReqHistory o2) {
			return 0 - (o1.getEndEntityInformation().getTimeModified().compareTo(o2.getEndEntityInformation().getTimeModified()));
		}
	}

	/**
	 * Returns a List of CertReqHistUserData from the certreqhist database in an collection sorted by timestamp.
	 */
	public List<CertReqHistory> getCertReqUserDatas(String username){
		List<CertReqHistory> history = this.certreqhistorysession.retrieveCertReqHistory(username);
		// Sort it by timestamp, newest first;
		Collections.sort(history, new CertReqUserCreateComparator());
		return history;
	}

	/** @return true if serial number unique indexing is supported by DB. */
	public boolean isUniqueIndexForSerialNumber() {
		return this.isUniqueIndex;
	}

	//
	// Methods from editcas.jsp refactoring
	//
    public boolean actionCreateCaMakeRequest(String caName, String signatureAlgorithm,
            String extendedServiceSignatureKeySpec,
            String keySequenceFormat, String keySequence, int catype, String subjectdn,
            String certificateProfileIdString, String signedByString, String description, String validityString,
            String approvalSettingValues, String numofReqApprovalsParam, String approvalProfileParam, boolean finishUser, boolean isDoEnforceUniquePublicKeys,
            boolean isDoEnforceUniqueDistinguishedName, boolean isDoEnforceUniqueSubjectDNSerialnumber,
            boolean useCertReqHistory, boolean useUserStorage, boolean useCertificateStorage, String subjectaltname,
            String policyid, boolean useauthoritykeyidentifier, boolean authoritykeyidentifiercritical,
            long crlperiod, long crlIssueInterval, long crlOverlapTime, long deltacrlperiod,
            String availablePublisherValues, boolean usecrlnumber, boolean crlnumbercritical,
            String defaultcrldistpoint, String defaultcrlissuer, String defaultocsplocator,
            String authorityInformationAccessString, String nameConstraintsPermittedString, String nameConstraintsExcludedString,
            String caDefinedFreshestCrlString, boolean useutf8policytext,
            boolean useprintablestringsubjectdn, boolean useldapdnorder, boolean usecrldistpointoncrl,
            boolean crldistpointoncrlcritical, boolean includeInHealthCheck, boolean serviceOcspActive,
            boolean serviceCmsActive, String sharedCmpRaSecret, boolean buttonCreateCa, boolean buttonMakeRequest,
            String cryptoTokenIdString, String keyAliasCertSignKey, String keyAliasCrlSignKey, String keyAliasDefaultKey,
            String keyAliasHardTokenEncryptKey, String keyAliasKeyEncryptKey, String keyAliasKeyTestKey,
            byte[] fileBuffer) throws Exception {
        int cryptoTokenId = Integer.parseInt(cryptoTokenIdString);
        try {
            if (cryptoTokenId==0) {
                // The admin has requested a quick setup and wants to generate a soft keystore with some usable keys
                keyAliasDefaultKey = "defaultKey";
                keyAliasCertSignKey = "signKey";
                keyAliasCrlSignKey = keyAliasCertSignKey;
                keyAliasHardTokenEncryptKey = "";
                keyAliasKeyEncryptKey = "";
                keyAliasKeyTestKey = "testKey";
                // First create a new soft auto-activated CryptoToken with the same name as the CA
                final Properties cryptoTokenProperties = new Properties();
                cryptoTokenProperties.setProperty(CryptoToken.AUTOACTIVATE_PIN_PROPERTY, CesecoreConfiguration.getCaKeyStorePass());
                try {
                    cryptoTokenId = cryptoTokenManagementSession.createCryptoToken(authenticationToken, caName, SoftCryptoToken.class.getName(),
                            cryptoTokenProperties, null, null);
                } catch (CryptoTokenNameInUseException e) {
                    // If the name was already in use we simply add a timestamp to the name to manke it unique
                    final String postfix = "_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
                    cryptoTokenId = cryptoTokenManagementSession.createCryptoToken(authenticationToken, caName+postfix, SoftCryptoToken.class.getName(),
                            cryptoTokenProperties, null, null);
                }
                // Next generate recommended RSA key pairs for decryption and test
                cryptoTokenManagementSession.createKeyPair(authenticationToken, cryptoTokenId, keyAliasDefaultKey, AlgorithmConstants.KEYALGORITHM_RSA + "2048");
                cryptoTokenManagementSession.createKeyPair(authenticationToken, cryptoTokenId, keyAliasKeyTestKey, AlgorithmConstants.KEYALGORITHM_RSA + "1024");
                // Next, create a CA signing key
                final String caSignKeyAlgo = AlgorithmTools.getKeyAlgorithmFromSigAlg(signatureAlgorithm);
                String caSignKeySpec = AlgorithmConstants.KEYALGORITHM_RSA + "2048";
                extendedServiceSignatureKeySpec = "2048";
                if (AlgorithmConstants.KEYALGORITHM_DSA.equals(caSignKeyAlgo)) {
                    caSignKeySpec = AlgorithmConstants.KEYALGORITHM_DSA + "1024";
                    extendedServiceSignatureKeySpec = caSignKeySpec;
                } else if (AlgorithmConstants.KEYALGORITHM_ECDSA.equals(caSignKeyAlgo)) {
                    caSignKeySpec = "prime256v1";
                    extendedServiceSignatureKeySpec = caSignKeySpec;
                } else if (AlgorithmTools.isGost3410Enabled() && AlgorithmConstants.KEYALGORITHM_ECGOST3410.equals(caSignKeyAlgo)) {
                    caSignKeySpec = CesecoreConfiguration.getExtraAlgSubAlgName("gost3410", "B");
                    extendedServiceSignatureKeySpec = caSignKeySpec;
                } else if (AlgorithmTools.isDstu4145Enabled() && AlgorithmConstants.KEYALGORITHM_DSTU4145.equals(caSignKeyAlgo)) {
                    caSignKeySpec = CesecoreConfiguration.getExtraAlgSubAlgName("dstu4145", "233");
                    extendedServiceSignatureKeySpec = caSignKeySpec;
                }
                cryptoTokenManagementSession.createKeyPair(authenticationToken, cryptoTokenId, keyAliasCertSignKey, caSignKeySpec);
            }
            return actionCreateCaMakeRequestInternal(caName, signatureAlgorithm, extendedServiceSignatureKeySpec,
                    keySequenceFormat, keySequence, catype, subjectdn, certificateProfileIdString, signedByString,
                    description, validityString, approvalSettingValues, numofReqApprovalsParam, approvalProfileParam, finishUser,
                    isDoEnforceUniquePublicKeys, isDoEnforceUniqueDistinguishedName, isDoEnforceUniqueSubjectDNSerialnumber,
                    useCertReqHistory, useUserStorage, useCertificateStorage, subjectaltname, policyid,
                    useauthoritykeyidentifier, authoritykeyidentifiercritical, crlperiod, crlIssueInterval,
                    crlOverlapTime, deltacrlperiod, availablePublisherValues, usecrlnumber, crlnumbercritical,
                    defaultcrldistpoint, defaultcrlissuer, defaultocsplocator, authorityInformationAccessString,
                    nameConstraintsPermittedString, nameConstraintsExcludedString,
                    caDefinedFreshestCrlString, useutf8policytext, useprintablestringsubjectdn, useldapdnorder,
                    usecrldistpointoncrl, crldistpointoncrlcritical, includeInHealthCheck, serviceOcspActive,
                    serviceCmsActive, sharedCmpRaSecret, buttonCreateCa, buttonMakeRequest, cryptoTokenId,
                    keyAliasCertSignKey, keyAliasCrlSignKey, keyAliasDefaultKey, keyAliasHardTokenEncryptKey,
                    keyAliasKeyEncryptKey, keyAliasKeyTestKey, fileBuffer);
        } catch (Exception e) {
            // If we failed during the creation we manually roll back any created soft CryptoToken
            // The more proper way of doing it would be to implement a CaAdminSession call for one-shot
            // CryptoToken and CA creation, but this would currently push a lot of GUI specific code
            // to the business logic. Until we have a new GUI this is probably the best way of doing it.
            if (cryptoTokenId != 0 && "0".equals(cryptoTokenIdString)) {
                cryptoTokenManagementSession.deleteCryptoToken(authenticationToken, cryptoTokenId);
            }
            throw e;
        }
    }

	private boolean actionCreateCaMakeRequestInternal(String caName, String signatureAlgorithm,
	        String extendedServiceSignatureKeySpec,
	        String keySequenceFormat, String keySequence, int catype, String subjectdn,
	        String certificateProfileIdString, String signedByString, String description, String validityString,
	        String approvalSettingValues, String numofReqApprovalsParam, String approvalProfileParam, boolean finishUser, boolean isDoEnforceUniquePublicKeys,
	        boolean isDoEnforceUniqueDistinguishedName, boolean isDoEnforceUniqueSubjectDNSerialnumber,
	        boolean useCertReqHistory, boolean useUserStorage, boolean useCertificateStorage, String subjectaltname,
	        String policyid, boolean useauthoritykeyidentifier, boolean authoritykeyidentifiercritical,
            long crlperiod, long crlIssueInterval, long crlOverlapTime, long deltacrlperiod,
            String availablePublisherValues, boolean usecrlnumber, boolean crlnumbercritical,
            String defaultcrldistpoint, String defaultcrlissuer, String defaultocsplocator,
            String authorityInformationAccessString, String nameConstraintsPermittedString, String nameConstraintsExcludedString, String caDefinedFreshestCrlString, boolean useutf8policytext,
            boolean useprintablestringsubjectdn, boolean useldapdnorder, boolean usecrldistpointoncrl,
            boolean crldistpointoncrlcritical, boolean includeInHealthCheck, boolean serviceOcspActive,
            boolean serviceCmsActive, String sharedCmpRaSecret, boolean buttonCreateCa, boolean buttonMakeRequest,
            int cryptoTokenId, String keyAliasCertSignKey, String keyAliasCrlSignKey, String keyAliasDefaultKey,
            String keyAliasHardTokenEncryptKey, String keyAliasKeyEncryptKey, String keyAliasKeyTestKey,
            byte[] fileBuffer) throws Exception {

	    boolean illegaldnoraltname = false;

	    final List<String> keyPairAliases = cryptoTokenManagementSession.getKeyPairAliases(authenticationToken, cryptoTokenId);
	    if (!keyPairAliases.contains(keyAliasDefaultKey)) {
	        log.info(authenticationToken.toString() + " attempted to createa a CA with a non-existing defaultKey alias: "+keyAliasDefaultKey);
	        throw new Exception("Invalid default key alias!");
	    }
	    final String[] suppliedAliases = {keyAliasCertSignKey,keyAliasCrlSignKey,keyAliasHardTokenEncryptKey,keyAliasHardTokenEncryptKey,keyAliasKeyEncryptKey,keyAliasKeyTestKey};
        for (final String currentSuppliedAlias : suppliedAliases) {
            if (currentSuppliedAlias.length()>0 && !keyPairAliases.contains(currentSuppliedAlias)) {
                log.info(authenticationToken.toString() + " attempted to create a CA with a non-existing key alias: "+currentSuppliedAlias);
                throw new Exception("Invalid key alias!");
            }
        }
        final Properties caTokenProperties = new Properties();
        caTokenProperties.setProperty(CATokenConstants.CAKEYPURPOSE_DEFAULT_STRING, keyAliasDefaultKey);
        if (keyAliasCertSignKey.length()>0) {
            caTokenProperties.setProperty(CATokenConstants.CAKEYPURPOSE_CERTSIGN_STRING, keyAliasCertSignKey);
        }
        if (keyAliasCrlSignKey.length()>0) {
            caTokenProperties.setProperty(CATokenConstants.CAKEYPURPOSE_CRLSIGN_STRING, keyAliasCrlSignKey);
        }
        if (keyAliasHardTokenEncryptKey.length()>0) {
            caTokenProperties.setProperty(CATokenConstants.CAKEYPURPOSE_HARDTOKENENCRYPT_STRING, keyAliasHardTokenEncryptKey);
        }
        if (keyAliasKeyEncryptKey.length()>0) {
            caTokenProperties.setProperty(CATokenConstants.CAKEYPURPOSE_KEYENCRYPT_STRING, keyAliasKeyEncryptKey);
        }
        if (keyAliasKeyTestKey.length()>0) {
            caTokenProperties.setProperty(CATokenConstants.CAKEYPURPOSE_TESTKEY_STRING, keyAliasKeyTestKey);
        }
	    final CAToken catoken = new CAToken(cryptoTokenId, caTokenProperties);
        if (signatureAlgorithm == null) {
            throw new Exception("No signature algorithm supplied!");  
        }
        catoken.setSignatureAlgorithm(signatureAlgorithm);
        catoken.setEncryptionAlgorithm(AlgorithmTools.getEncSigAlgFromSigAlg(signatureAlgorithm));

        if (extendedServiceSignatureKeySpec == null || extendedServiceSignatureKeySpec.length()==0) {
            throw new Exception("No key specification supplied.");
        }
        if (keySequenceFormat==null) {
            catoken.setKeySequenceFormat(StringTools.KEY_SEQUENCE_FORMAT_NUMERIC);
        } else {
            catoken.setKeySequenceFormat(Integer.parseInt(keySequenceFormat));
        }
        if (keySequence==null) {
            catoken.setKeySequence(CAToken.DEFAULT_KEYSEQUENCE);
        } else {
            catoken.setKeySequence(keySequence);
        }
	    try {
	        CertTools.stringToBcX500Name(subjectdn);
	    } catch (Exception e) {
	        illegaldnoraltname = true;
	    }
        int certprofileid = (certificateProfileIdString==null ? 0 : Integer.parseInt(certificateProfileIdString));
	    int signedby = (signedByString==null ? 0 : Integer.parseInt(signedByString));

	    if (description == null) {
            description = "";
	    }

        final long validity;
        if (buttonMakeRequest) {
            validity = 0; // not applicable
        } else {
            validity = ValidityDate.encode(validityString);
    	    if (validity<0) {
    	        throw new ParameterException(ejbcawebbean.getText("INVALIDVALIDITYORCERTEND"));
    	    }
        }

	    if (catoken != null && catype != 0 && subjectdn != null && caName != null && signedby != 0) {
	        // Approvals is generic for all types of CAs
	        final ArrayList<Integer> approvalsettings = new ArrayList<Integer>();
	        if (approvalSettingValues != null) {
	            for (int i=0; i<approvalSettingValues.split(";").length; i++) {
	                approvalsettings.add(Integer.valueOf(approvalSettingValues.split(";")[i]));
	            }
	        }
            final int numofreqapprovals = (numofReqApprovalsParam==null ? 1 : Integer.parseInt(numofReqApprovalsParam));
            final int approvalProfileID = (approvalProfileParam==null ? -1 : Integer.parseInt(approvalProfileParam));

	        if (catype == CAInfo.CATYPE_X509) {
	            // Create a X509 CA
	            if (subjectaltname == null) {
                    subjectaltname = ""; 
	            }
	            if (!checkSubjectAltName(subjectaltname)) {
	               illegaldnoraltname = true;
	            }
	            /* Process certificate policies. */
	            final List<CertificatePolicy> policies = parsePolicies(policyid);
	            // Certificate policies from the CA and the CertificateProfile will be merged for cert creation in the CAAdminSession.createCA call

	            final ArrayList<Integer> crlpublishers = new ArrayList<Integer>(); 
	            if (availablePublisherValues != null) {
	                for (final String availablePublisherId : availablePublisherValues.split(";")) {
	                    crlpublishers.add(Integer.valueOf(availablePublisherId));
	                }
	            }
	            final List<String> authorityInformationAccess = new ArrayList<String>();
	            authorityInformationAccess.add(authorityInformationAccessString);
	            String cadefinedfreshestcrl = "";
	            if (caDefinedFreshestCrlString != null) {
	                cadefinedfreshestcrl = caDefinedFreshestCrlString;
	            }
	            
	            final List<String> nameConstraintsPermitted = parseNameConstraintsInput(nameConstraintsPermittedString);
	            final List<String> nameConstraintsExcluded = parseNameConstraintsInput(nameConstraintsExcludedString);
	            final boolean hasNameConstraints = !nameConstraintsPermitted.isEmpty() || !nameConstraintsExcluded.isEmpty();
	            if (hasNameConstraints && !isNameConstraintAllowedInProfile(certprofileid)) {
	               throw new ParameterException(ejbcawebbean.getText("NAMECONSTRAINTSNOTENABLED"));
	            }

	            if (crlperiod != 0 && !illegaldnoraltname) {
	                if (buttonCreateCa) {
	                    List<ExtendedCAServiceInfo> extendedcaservices = makeExtendedServicesInfos(extendedServiceSignatureKeySpec, subjectdn, serviceCmsActive);
	                    X509CAInfo x509cainfo = new X509CAInfo(subjectdn, caName, CAConstants.CA_ACTIVE, new Date(), subjectaltname,
	                            certprofileid, validity, 
	                            null, catype, signedby,
	                            null, catoken, description, -1, null,
	                            policies, crlperiod, crlIssueInterval, crlOverlapTime, deltacrlperiod, crlpublishers, 
	                            useauthoritykeyidentifier, 
	                            authoritykeyidentifiercritical,
	                            usecrlnumber, 
	                            crlnumbercritical, 
	                            defaultcrldistpoint,
	                            defaultcrlissuer,
	                            defaultocsplocator, 
	                            authorityInformationAccess,
	                            nameConstraintsPermitted, nameConstraintsExcluded,
	                            cadefinedfreshestcrl,
	                            finishUser, extendedcaservices,
	                            useutf8policytext,
	                            approvalsettings,
	                            numofreqapprovals,
	                            approvalProfileID,
	                            useprintablestringsubjectdn,
	                            useldapdnorder,
	                            usecrldistpointoncrl,
	                            crldistpointoncrlcritical,
	                            includeInHealthCheck,
	                            isDoEnforceUniquePublicKeys,
	                            isDoEnforceUniqueDistinguishedName,
	                            isDoEnforceUniqueSubjectDNSerialnumber,
	                            useCertReqHistory,
	                            useUserStorage,
	                            useCertificateStorage,
	                            sharedCmpRaSecret);
                        try {
                            cadatahandler.createCA((CAInfo) x509cainfo);
                        } catch (EJBException e) {
                            if (e.getCausedByException() instanceof IllegalArgumentException) {
                                //Couldn't create CA from the given parameters
                                illegaldnoraltname = true;
                            } else {
                                throw e;
                            }
                        }
	                }

	                if (buttonMakeRequest) {
	                    List<ExtendedCAServiceInfo> extendedcaservices = makeExtendedServicesInfos(extendedServiceSignatureKeySpec, subjectdn, serviceCmsActive);
	                    X509CAInfo x509cainfo = new X509CAInfo(subjectdn, caName, CAConstants.CA_ACTIVE, new Date(), subjectaltname,
	                            certprofileid, validity,
	                            null, catype, CAInfo.SIGNEDBYEXTERNALCA,
	                            null, catoken, description, -1, null, 
	                            policies, crlperiod, crlIssueInterval, crlOverlapTime, deltacrlperiod, crlpublishers, 
	                            useauthoritykeyidentifier, 
	                            authoritykeyidentifiercritical,
	                            usecrlnumber, 
	                            crlnumbercritical, 
	                            defaultcrldistpoint,
	                            defaultcrlissuer,
	                            defaultocsplocator, 
	                            authorityInformationAccess,
	                            nameConstraintsPermitted, nameConstraintsExcluded,
	                            cadefinedfreshestcrl,
	                            finishUser, extendedcaservices,
	                            useutf8policytext,
	                            approvalsettings,
	                            numofreqapprovals,
	                            approvalProfileID,
	                            useprintablestringsubjectdn,
	                            useldapdnorder,
	                            usecrldistpointoncrl,
	                            crldistpointoncrlcritical,
	                            false, // Do not automatically include new CAs in health-check
	                            isDoEnforceUniquePublicKeys,
	                            isDoEnforceUniqueDistinguishedName,
	                            isDoEnforceUniqueSubjectDNSerialnumber,
	                            useCertReqHistory,
	                            useUserStorage,
	                            useCertificateStorage,
	                            null);
	                    saveRequestInfo(x509cainfo);                
	                }
	            }                          
	        }

	        if (catype == CAInfo.CATYPE_CVC) {
	            // Only default values for these that are not used
	            crlperiod = 2400;
	            crlIssueInterval = 0;
	            crlOverlapTime = 0;
	            deltacrlperiod = 0;
	            List<Integer> crlpublishers = new ArrayList<Integer>(); 
	            if(crlperiod != 0 && !illegaldnoraltname){
	                // A CVC CA does not have any of the external services OCSP, CMS
	                List<ExtendedCAServiceInfo> extendedcaservices = new ArrayList<ExtendedCAServiceInfo>();
	                if (buttonMakeRequest) {
	                    signedby = CAInfo.SIGNEDBYEXTERNALCA;
	                }
	                // Create the CAInfo to be used for either generating the whole CA or making a request
	                CVCCAInfo cvccainfo = new CVCCAInfo(subjectdn, caName, CAConstants.CA_ACTIVE, new Date(),
	                        certprofileid, validity, 
	                        null, catype, signedby,
	                        null, catoken, description, -1, null,
	                        crlperiod, crlIssueInterval, crlOverlapTime, deltacrlperiod, crlpublishers, 
	                        finishUser, extendedcaservices,
	                        approvalsettings,
	                        numofreqapprovals,
	                        false, // Do not automatically include new CAs in health-check
	                        isDoEnforceUniquePublicKeys,
	                        isDoEnforceUniqueDistinguishedName,
	                        isDoEnforceUniqueSubjectDNSerialnumber,
	                        useCertReqHistory,
	                        useUserStorage,
	                        useCertificateStorage);
	                if (buttonCreateCa) {
	                    cadatahandler.createCA(cvccainfo);
	                } else if (buttonMakeRequest) {
	                    saveRequestInfo(cvccainfo);                
	                }
	            }
	        }
	    }
        if (buttonMakeRequest && !illegaldnoraltname) {
            CAInfo cainfo = getRequestInfo();
            cadatahandler.createCA(cainfo);                           
            int caid = cainfo.getCAId();
            try {
                byte[] certreq = cadatahandler.makeRequest(caid, fileBuffer, catoken.getAliasFromPurpose(CATokenConstants.CAKEYPURPOSE_CERTSIGN));
                saveRequestData(certreq);
            } catch (CryptoTokenOfflineException e) {
                cadatahandler.removeCA(caid);
            }
        }
	    return illegaldnoraltname;
	}

	private List<String> parseNameConstraintsInput(String input) throws ParameterException {
        try {
            return NameConstraint.parseNameConstraintsList(input);
        } catch (CertificateExtensionException e) {
            throw new ParameterException(MessageFormat.format(ejbcawebbean.getText("INVALIDNAMECONSTRAINT"), e.getMessage()));
        }
    }
    
    private boolean isNameConstraintAllowedInProfile(int certProfileId) {
        final CertificateProfile certProfile = certificateProfileSession.getCertificateProfile(certProfileId);
        
        final boolean isCA = (certProfile.getType() == CertificateConstants.CERTTYPE_SUBCA ||
                certProfile.getType() == CertificateConstants.CERTTYPE_ROOTCA);
        
        return isCA && certProfile.getUseNameConstraints();
    }

    public List<ExtendedCAServiceInfo> makeExtendedServicesInfos(String keySpec, String subjectdn, boolean serviceCmsActive) {
	    String keyType = AlgorithmConstants.KEYALGORITHM_RSA;
        try {
            Integer.parseInt(keySpec);
        } catch (NumberFormatException e) {
            if (keySpec.startsWith("DSA")) {
                keyType = AlgorithmConstants.KEYALGORITHM_DSA;
            } else if (keySpec.startsWith(AlgorithmConstants.KEYSPECPREFIX_ECGOST3410)) {
                keyType = AlgorithmConstants.KEYALGORITHM_ECGOST3410;
            } else if (AlgorithmTools.isDstu4145Enabled() && keySpec.startsWith(CesecoreConfiguration.getOidDstu4145())) {
                keyType = AlgorithmConstants.KEYALGORITHM_DSTU4145;
            } else {
                keyType = AlgorithmConstants.KEYALGORITHM_ECDSA;
            }
        }
        
        final int cmsactive = serviceCmsActive ? ExtendedCAServiceInfo.STATUS_ACTIVE : ExtendedCAServiceInfo.STATUS_INACTIVE;
	    
        List<ExtendedCAServiceInfo> extendedcaservices = new ArrayList<ExtendedCAServiceInfo>();
        // Create and active External CA Services.
        extendedcaservices.add(
                new CmsCAServiceInfo(cmsactive,
                        "CN=CMSCertificate, " + subjectdn, "",
                        keySpec, keyType));
        extendedcaservices.add(new HardTokenEncryptCAServiceInfo(ExtendedCAServiceInfo.STATUS_ACTIVE));
        extendedcaservices.add(new KeyRecoveryCAServiceInfo(ExtendedCAServiceInfo.STATUS_ACTIVE));
        return extendedcaservices;
    }

    public boolean checkSubjectAltName(String subjectaltname) {
        if (subjectaltname != null && !subjectaltname.trim().equals("")) {
            final DNFieldExtractor subtest = new DNFieldExtractor(subjectaltname,DNFieldExtractor.TYPE_SUBJECTALTNAME);                   
            if (subtest.isIllegal() || subtest.existsOther()) {
                return false;
            }
        }
        return true;
    }

    public List<CertificatePolicy> parsePolicies(String policyid) {
        final ArrayList<CertificatePolicy> policies = new ArrayList<CertificatePolicy>();
        if (!(policyid == null || policyid.trim().equals(""))) {
            final String[] str = policyid.split("\\s+");
            if (str.length > 1) {
                policies.add(new CertificatePolicy(str[0], CertificatePolicy.id_qt_cps, str[1]));
            } else {
                policies.add(new CertificatePolicy((policyid.trim()),null,null));
            }
        }
        return policies;
    }

    public CAInfo createCaInfo(int caid, String caname, String subjectDn, int catype,
	        String keySequenceFormat, String keySequence, String signedByString, String description, String validityString,
	        long crlperiod, long crlIssueInterval, long crlOverlapTime, long deltacrlperiod, boolean finishUser,
	        boolean isDoEnforceUniquePublicKeys, boolean isDoEnforceUniqueDistinguishedName, boolean isDoEnforceUniqueSubjectDNSerialnumber,
	        boolean useCertReqHistory, boolean useUserStorage, boolean useCertificateStorage, String approvalSettingValues, String numofReqApprovalsParam, 
	        String approvalProfileParam,
	        String availablePublisherValues, boolean useauthoritykeyidentifier, boolean authoritykeyidentifiercritical, boolean usecrlnumber,
	        boolean crlnumbercritical, String defaultcrldistpoint, String defaultcrlissuer, String defaultocsplocator, String authorityInformationAccessParam,
	        String nameConstraintsPermittedString, String nameConstraintsExcludedString,
	        String caDefinedFreshestCrl, boolean useutf8policytext, boolean useprintablestringsubjectdn, boolean useldapdnorder, boolean usecrldistpointoncrl,
	        boolean crldistpointoncrlcritical, boolean includeInHealthCheck, boolean serviceOcspActive, boolean serviceCmsActive, String sharedCmpRaSecret
	        ) throws Exception {
        // We need to pick up the old CAToken, so we don't overwrite with default values when we save the CA further down
        CAInfoView infoView = cadatahandler.getCAInfo(caid);  
        CAToken catoken = infoView.getCAToken();
        if (catoken == null) {
            catoken = new CAToken(caid, new Properties());
        }
        if (keySequenceFormat==null) {
            catoken.setKeySequenceFormat(StringTools.KEY_SEQUENCE_FORMAT_NUMERIC);
        } else {
            catoken.setKeySequenceFormat(Integer.parseInt(keySequenceFormat));
        }
        if (keySequence==null) {
            catoken.setKeySequence(CAToken.DEFAULT_KEYSEQUENCE);
        } else {
            catoken.setKeySequence(keySequence);
        }
        if (description == null) {
            description = "";
        }
        final int signedby = (signedByString==null ? 0 : Integer.parseInt(signedByString));
        final long validity;
        if (validityString == null && signedby == CAInfo.SIGNEDBYEXTERNALCA) {
            // A validityString of null is allowed, when using a validity is not applicable
            validity = 0;
        } else {
            validity = ValidityDate.encode(validityString);
            if (validity<0) {
                throw new ParameterException(ejbcawebbean.getText("INVALIDVALIDITYORCERTEND"));
            }
        }
        if (caid != 0  && catype !=0) {
            // First common info for both X509 CAs and CVC CAs
           CAInfo cainfo = null;
           final ArrayList<Integer> approvalsettings = new ArrayList<Integer>();
           if (approvalSettingValues != null) {
               for (int i=0; i<approvalSettingValues.split(";").length; i++) {
                   approvalsettings.add(Integer.valueOf(approvalSettingValues.split(";")[i]));
               }
           }
           final int numofreqapprovals = (numofReqApprovalsParam==null ? 1 : Integer.parseInt(numofReqApprovalsParam));
           final int approvalProfileID = (approvalProfileParam==null ? -1 : Integer.parseInt(approvalProfileParam));
           final ArrayList<Integer> crlpublishers = new ArrayList<Integer>(); 
           if (availablePublisherValues != null) {
               for (final String availablePublisherId : availablePublisherValues.split(";")) {
                   crlpublishers.add(Integer.valueOf(availablePublisherId));
               }
           }
           // Info specific for X509 CA
           if (catype == CAInfo.CATYPE_X509) {
               final List<String> authorityInformationAccess = new ArrayList<String>();
               authorityInformationAccess.add(authorityInformationAccessParam);
               final String cadefinedfreshestcrl = (caDefinedFreshestCrl==null ? "" : caDefinedFreshestCrl);
               // Create extended CA Service updatedata.
               final int cmsactive = serviceCmsActive ? ExtendedCAServiceInfo.STATUS_ACTIVE : ExtendedCAServiceInfo.STATUS_INACTIVE;
               final ArrayList<ExtendedCAServiceInfo> extendedcaservices = new ArrayList<ExtendedCAServiceInfo>();
               extendedcaservices.add(new CmsCAServiceInfo(cmsactive, false)); 
               // No need to add the HardTokenEncrypt or Keyrecovery extended service here, because they are only "updated" in EditCA, and there
               // is not need to update them.
               cainfo = new X509CAInfo(caid, validity,
                       catoken, description, 
                       crlperiod, crlIssueInterval, crlOverlapTime, deltacrlperiod, crlpublishers, 
                       useauthoritykeyidentifier, 
                       authoritykeyidentifiercritical,
                       usecrlnumber, 
                       crlnumbercritical, 
                       defaultcrldistpoint,
                       defaultcrlissuer,
                       defaultocsplocator, 
                       authorityInformationAccess,
                       parseNameConstraintsInput(nameConstraintsPermittedString), parseNameConstraintsInput(nameConstraintsExcludedString),
                       cadefinedfreshestcrl,
                       finishUser,extendedcaservices,
                       useutf8policytext,
                       approvalsettings,
                       numofreqapprovals,
                       approvalProfileID,
                       useprintablestringsubjectdn,
                       useldapdnorder,
                       usecrldistpointoncrl,
                       crldistpointoncrlcritical,
                       includeInHealthCheck,
                       isDoEnforceUniquePublicKeys,
                       isDoEnforceUniqueDistinguishedName,
                       isDoEnforceUniqueSubjectDNSerialnumber,
                       useCertReqHistory,
                       useUserStorage,
                       useCertificateStorage,
                       sharedCmpRaSecret);
           }
           // Info specific for CVC CA
           if (catype == CAInfo.CATYPE_CVC) {
               // Edit CVC CA data                            
               // A CVC CA does not have any of the external services OCSP, CMS
               final List<ExtendedCAServiceInfo> extendedcaservices = new ArrayList<ExtendedCAServiceInfo>();
               // Create the CAInfo to be used for either generating the whole CA or making a request
               cainfo = new CVCCAInfo(caid, validity, 
                       catoken, description,
                       crlperiod, crlIssueInterval, crlOverlapTime, deltacrlperiod, crlpublishers, 
                       finishUser, extendedcaservices,
                       approvalsettings,
                       numofreqapprovals,
                       includeInHealthCheck,
                       isDoEnforceUniquePublicKeys,
                       isDoEnforceUniqueDistinguishedName,
                       isDoEnforceUniqueSubjectDNSerialnumber,
                       useCertReqHistory,
                       useUserStorage,
                       useCertificateStorage);
           }
            cainfo.setSubjectDN(subjectDn);
            cainfo.setStatus(infoView.getCAInfo().getStatus());
            return cainfo;
        }
        return null;
	}

    public List<Entry<String, String>> getAvailableCryptoTokens(final String caSigingAlgorithm, boolean isEditingCA)
            throws AuthorizationDeniedException, KeyStoreException, CryptoTokenOfflineException {
	    final List<Entry<String, String>> availableCryptoTokens = new ArrayList<Entry<String, String>>();
        if (!isEditingCA && accessControlSession.isAuthorizedNoLogging(authenticationToken, CryptoTokenRules.MODIFY_CRYPTOTOKEN.resource())) {
            // Add a quick setup option for key generation (not visible when editing an uninitialized CA)
            availableCryptoTokens.add(new AbstractMap.SimpleEntry<String,String>(Integer.toString(0), ejbcawebbean.getText("CRYPTOTOKEN_NEWFROMCA")));
        }
	    if (caSigingAlgorithm != null && caSigingAlgorithm.length()>0) {
	        final List<CryptoTokenInfo> cryptoTokenInfos = cryptoTokenManagementSession.getCryptoTokenInfos(authenticationToken);
            for (final CryptoTokenInfo cryptoTokenInfo : cryptoTokenInfos) {
                // Make sure we may use it
                if (accessControlSession.isAuthorizedNoLogging(authenticationToken, CryptoTokenRules.USE.resource() + '/' + cryptoTokenInfo.getCryptoTokenId())
                        && cryptoTokenInfo.isActive()) {
	                final int cryptoTokenId = cryptoTokenInfo.getCryptoTokenId();
	                try {
    	                // Fetch a list of all keys and their specs
    	                final List<KeyPairInfo> cryptoTokenKeyPairInfos = cryptoTokenManagementSession.getKeyPairInfos(authenticationToken, cryptoTokenId);
    	                // Only allow tokens with at least one keypair
    	                if (cryptoTokenKeyPairInfos.size()>0) {
    	                    for (final KeyPairInfo cryptoTokenKeyPairInfo : cryptoTokenKeyPairInfos) {
    	                        String requiredKeyAlgorithm = AlgorithmTools.getKeyAlgorithmFromSigAlg(caSigingAlgorithm);
    	                        if (requiredKeyAlgorithm.equals(cryptoTokenKeyPairInfo.getKeyAlgorithm())) {
    	                            // We have at least on key in this token with the right key algorithm mathing the CA's singing algorithm, so add the token!
    	                            availableCryptoTokens.add(new AbstractMap.SimpleEntry<String,String>(Integer.toString(cryptoTokenId), cryptoTokenInfo.getName()));
    	                            break; // This token is fine, proceed with next token
    	                        }
    	                    }
    	                }
	                } catch (CryptoTokenOfflineException ctoe) {
	                   // The CryptoToken might have timed out
	                }
	            }
	        }
	    }
	    return availableCryptoTokens;
	}
	
	public List<Entry<String, String>> getFailedCryptoTokens(final String caSigingAlgorithm) throws AuthorizationDeniedException, KeyStoreException, CryptoTokenOfflineException {
        final List<Entry<String, String>> failedCryptoTokens = new ArrayList<Entry<String, String>>();
        if (caSigingAlgorithm != null && caSigingAlgorithm.length()>0) {
            final List<CryptoTokenInfo> cryptoTokenInfos = cryptoTokenManagementSession.getCryptoTokenInfos(authenticationToken);
            for (final CryptoTokenInfo cryptoTokenInfo : cryptoTokenInfos) {
                // Make sure we may use it
                if (accessControlSession.isAuthorizedNoLogging(authenticationToken, CryptoTokenRules.USE.resource() + '/' + cryptoTokenInfo.getCryptoTokenId())
                        && cryptoTokenInfo.isActive()) {
                    final int cryptoTokenId = cryptoTokenInfo.getCryptoTokenId();
                    try {
                        // Try to access to keys
                        cryptoTokenManagementSession.getKeyPairInfos(authenticationToken, cryptoTokenId);
                    } catch (CryptoTokenOfflineException ctoe) {
                       failedCryptoTokens.add(new AbstractMap.SimpleEntry<String,String>(Integer.toString(cryptoTokenId), cryptoTokenInfo.getName()));
                    }
                }
            }
        }
        return failedCryptoTokens;
    }

    /** @return a list of key pair aliases that can be used for either signing or encryption under the supplied CA signing algorithm */
    public List<String> getAvailableCryptoTokenMixedAliases(int cryptoTokenId, final String caSigingAlgorithm) throws KeyStoreException, CryptoTokenOfflineException, AuthorizationDeniedException {
        final List<String> aliases = new ArrayList<String>();
        aliases.addAll(getAvailableCryptoTokenAliases(cryptoTokenId, caSigingAlgorithm));
        final List<String> encAliases = getAvailableCryptoTokenEncryptionAliases(cryptoTokenId, caSigingAlgorithm);
        aliases.removeAll(encAliases);  // Avoid duplicates
        aliases.addAll(encAliases);
        return aliases;
    }

    /** @return a list of key pair aliases that can be used for signing using the supplied CA signing algorithm */
	public List<String> getAvailableCryptoTokenAliases(int cryptoTokenId, final String caSigingAlgorithm) throws KeyStoreException, CryptoTokenOfflineException, AuthorizationDeniedException {
	    final List<String> aliases = new ArrayList<String>();
	    if (cryptoTokenManagementSession.getCryptoTokenInfo(cryptoTokenId) == null) {
	       log.debug("CryptoToken didn't exist when trying to get aliases");
	    } else {
            final List<KeyPairInfo> cryptoTokenKeyPairInfos = cryptoTokenManagementSession.getKeyPairInfos(authenticationToken, cryptoTokenId);
            for (final KeyPairInfo cryptoTokenKeyPairInfo : cryptoTokenKeyPairInfos) {
                if (AlgorithmTools.getKeyAlgorithmFromSigAlg(caSigingAlgorithm).equals(cryptoTokenKeyPairInfo.getKeyAlgorithm())) {
                    aliases.add(cryptoTokenKeyPairInfo.getAlias());
                }
            }
	    }
        return aliases;
	}

    /** @return a list of key pair aliases that can be used for encryption using the supplied CA signing algorithm to derive encryption algo. */
    public List<String> getAvailableCryptoTokenEncryptionAliases(int cryptoTokenId, final String caSigingAlgorithm) throws KeyStoreException, CryptoTokenOfflineException, AuthorizationDeniedException {
        final List<String> aliases = new ArrayList<String>();
        if (cryptoTokenManagementSession.getCryptoTokenInfo(cryptoTokenId) == null) {
            log.debug("CryptoToken didn't exist when trying to get aliases");
        } else {
            final List<KeyPairInfo> cryptoTokenKeyPairInfos = cryptoTokenManagementSession.getKeyPairInfos(authenticationToken, cryptoTokenId);
            for (final KeyPairInfo cryptoTokenKeyPairInfo : cryptoTokenKeyPairInfos) {
                if (AlgorithmTools.getKeyAlgorithmFromSigAlg(AlgorithmTools.getEncSigAlgFromSigAlg(caSigingAlgorithm)).equals(cryptoTokenKeyPairInfo.getKeyAlgorithm())) {
                    aliases.add(cryptoTokenKeyPairInfo.getAlias());
                }
            }
        }
        return aliases;
    }
    
	public boolean isCryptoTokenActive(final int cryptoTokenId) throws AuthorizationDeniedException {
	    return cryptoTokenManagementSession.isCryptoTokenStatusActive(authenticationToken, cryptoTokenId);
	}
	
    public boolean isCryptoTokenPresent(final int cryptoTokenId) throws AuthorizationDeniedException {
        return cryptoTokenManagementSession.getCryptoTokenInfo(authenticationToken, cryptoTokenId) != null;
    }
    
	public String getCryptoTokenName(final int cryptoTokenId) throws AuthorizationDeniedException {
	    final CryptoTokenInfo cryptoTokenInfo = cryptoTokenManagementSession.getCryptoTokenInfo(authenticationToken, cryptoTokenId);
	    if (cryptoTokenInfo == null) {
	        return "CryptoToken " + cryptoTokenId + " not found.";
	    }
	    return cryptoTokenInfo.getName();
	}
	
	public boolean isAuthorizedToCa(int caid) {
	    return casession.authorizedToCANoLogging(authenticationToken, caid);
	}
	
	/**
	 * 
	 * @return true if admin has general read rights to CAs, but no edit rights. 
	 */
    public boolean hasEditRight() {
        return accessControlSession.isAuthorizedNoLogging(authenticationToken, StandardRules.CAEDIT.resource());
    }
    
    public boolean hasCreateRight() {
        return accessControlSession.isAuthorizedNoLogging(authenticationToken, StandardRules.CAADD.resource());
    }
	
	public boolean isCaExportable(CAInfo caInfo) throws AuthorizationDeniedException {
	    boolean ret = false;
	    final int caInfoStatus = caInfo.getStatus();
	    if (caInfoStatus != CAConstants.CA_EXTERNAL && caInfoStatus != CAConstants.CA_WAITING_CERTIFICATE_RESPONSE) {
	        final int cryptoTokenId = caInfo.getCAToken().getCryptoTokenId();
	        final CryptoTokenInfo cryptoTokenInfo = cryptoTokenManagementSession.getCryptoTokenInfo(authenticationToken, cryptoTokenId);
	        if (cryptoTokenInfo!=null) {
	            ret = (SoftCryptoToken.class.getSimpleName().equals(cryptoTokenInfo.getType())) && cryptoTokenInfo.isAllowExportPrivateKey();
	        }
	    }
	    return ret;
	}

	public List<Entry<String,String>> getAvailableCaCertificateProfiles() {
	    final int[] types = { CertificateConstants.CERTTYPE_ROOTCA, CertificateConstants.CERTTYPE_SUBCA };
        final Map<Integer, String> idToNameMap = certificateProfileSession.getCertificateProfileIdToNameMap();
        final List<Entry<String,String>> ret = new ArrayList<Entry<String,String>>();
	    for (int type : types) {
	        final Collection<Integer> ids = certificateProfileSession.getAuthorizedCertificateProfileIds(authenticationToken, type);
	        for (final Integer id : ids) {
	            ret.add(new SimpleEntry<String,String>(id.toString(), (type==CertificateConstants.CERTTYPE_ROOTCA ? "(RootCAs) " : "(SubCAs) ") + idToNameMap.get(id)));
	        }
	    }
        return ret;
	}

    public List<Entry<String,String>> getAvailableKeySpecs() {
        final List<Entry<String,String>> ret = new ArrayList<Entry<String,String>>();
        // Legacy idea: Never use larger keys than 2048 bit RSA for CMS signing
        // Reference: RFC 6485 - The Profile for Algorithms and Key Sizes for Use in the Resource PKI. [§ 3, and 5]
        final int[] SIZES_RSA = {1024, 1536, 2048, 3072, 4096/*, 6144, 8192*/};
        final int[] SIZES_DSA = {1024};
        for (int size : SIZES_RSA) {
            ret.add(new SimpleEntry<String,String>(String.valueOf(size), "RSA "+size));
        }
        for (int size : SIZES_DSA) {
            ret.add(new SimpleEntry<String,String>("DSA"+size, "DSA "+size));
        }
        @SuppressWarnings("unchecked")
        final Enumeration<String> ecNamedCurves = ECNamedCurveTable.getNames();
        while (ecNamedCurves.hasMoreElements()) {
            final String ecNamedCurve = ecNamedCurves.nextElement();
            ret.add(new SimpleEntry<String,String>(ecNamedCurve, "ECDSA "+ecNamedCurve));
        }
        
        for (String alg : CesecoreConfiguration.getExtraAlgs()) {
            for (String subalg : CesecoreConfiguration.getExtraAlgSubAlgs(alg)) {
                final String title = CesecoreConfiguration.getExtraAlgSubAlgTitle(alg, subalg);
                final String name = CesecoreConfiguration.getExtraAlgSubAlgName(alg, subalg);
                ret.add(new SimpleEntry<String,String>(name, title));
            }
        }

        return ret;
    }
    
    public boolean createAuthCertSignRequest(int caid, byte[] request) throws CADoesntExistsException, AuthorizationDeniedException, CertPathValidatorException, CryptoTokenOfflineException {
        if (request != null) {
            byte[] signedreq = cadatahandler.createAuthCertSignRequest(caid, request);
            saveRequestData(signedreq);
            return true;
        }
        return false;
    }
    
    public byte[] parseRequestParameters(HttpServletRequest request, Map<String, String> requestMap) throws IOException {
        byte[] fileBuffer = null;
        try {
            if (ServletFileUpload.isMultipartContent(request)) {
                final DiskFileItemFactory diskFileItemFactory = new DiskFileItemFactory();
                diskFileItemFactory.setSizeThreshold(59999);
                ServletFileUpload upload = new ServletFileUpload(diskFileItemFactory);
                upload.setSizeMax(60000);                   
                final List<FileItem> items = upload.parseRequest(request);     
                for (final FileItem item : items) {
                    if (item.isFormField()) {
                        final String fieldName = item.getFieldName();
                        final String currentValue = requestMap.get(fieldName);
                        if (currentValue != null) {
                            requestMap.put(fieldName, currentValue + ";" + item.getString("UTF8"));
                        } else {
                            requestMap.put(fieldName, item.getString("UTF8"));
                        }
                    } else {
                        //final String itemName = item.getName();
                        final InputStream file = item.getInputStream();
                        byte[] fileBufferTmp = FileTools.readInputStreamtoBuffer(file);
                        if (fileBuffer == null && fileBufferTmp.length > 0) {
                            fileBuffer = fileBufferTmp;
                        }
                    }
                } 
            } else {
                final Set<String> keySet = request.getParameterMap().keySet();
                for (final String key : keySet) {
                    requestMap.put(key, request.getParameter(key));
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (FileUploadException e) {
            throw new IOException(e);
        }
        return fileBuffer;
    }
    
    /** Returns true if any CVC CA implementation is available, false otherwise.
     * Used to hide/give warning when no CVC CA implementation is available.
     */
    public boolean isCVCAvailable() {
        boolean ret = false;
        ServiceLoader<? extends CvcPlugin> loader = CvcCA.getImplementationClasses();
        if (loader.iterator().hasNext()) {
            ret = true;
        }
        return ret;
    }
    /** Returns true if a unique (issuerDN,serialNumber) is present in the database. 
     * If this is available, you can not use CVC CAs. Returns false if a unique index is 
     * Used to hide/give warning when no CVC CA implementation is available.
     */
    public boolean isUniqueIssuerDNSerialNoIndexPresent() {
        return certificatesession.isUniqueCertificateSerialNumberIndex();
    }
    
    /** Returns the "not before" date of the next certificate during a rollover period, or null if no next certificate exists.
     * @throws CADoesntExistsException If the CA doesn't exist.
     */
    public Date getRolloverNotBefore(int caid) throws CADoesntExistsException {
        final Certificate nextCert = casession.getFutureRolloverCertificate(caid);
        if (nextCert != null) {
            return CertTools.getNotBefore(nextCert);
        } else {
            return null;
        }
    }
    
    /** Returns the current CA validity "not after" date. 
     * @throws AuthorizationDeniedException 
     * @throws CADoesntExistsException */
    public Date getRolloverNotAfter(int caid) throws CADoesntExistsException, AuthorizationDeniedException {
        final Collection<Certificate> chain = casession.getCAInfo(authenticationToken, caid).getCertificateChain();
        return CertTools.getNotAfter(chain.iterator().next());
    }
}
