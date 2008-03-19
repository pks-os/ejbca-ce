package org.ejbca.util;

import java.util.ArrayList;
import java.util.HashMap;

import javax.ejb.EJBException;

import org.apache.log4j.Logger;
import org.ejbca.core.ejb.ca.store.ICertificateStoreSessionLocal;
import org.ejbca.core.ejb.ra.raadmin.IRaAdminSessionLocal;
import org.ejbca.core.model.ca.certificateprofiles.CertificateProfile;
import org.ejbca.core.model.ca.certificateprofiles.CertificateProfileExistsException;
import org.ejbca.core.model.log.Admin;
import org.ejbca.core.model.ra.raadmin.EndEntityProfile;
import org.ejbca.core.model.ra.raadmin.EndEntityProfileExistsException;
import org.ejbca.util.dn.DNFieldExtractor;
import org.ejbca.util.dn.DnComponents;

public class MSCertTools {

	private static final Logger log = Logger.getLogger(MSCertTools.class);

	private static final String REQUESTSTART = "-----BEGIN NEW CERTIFICATE REQUEST-----";
	private static final String REQUESTEND = "-----END NEW CERTIFICATE REQUEST-----";

	private static final String CERTIFICATE_TEMPLATENAME_USER = "User";
	private static final String CERTIFICATE_TEMPLATENAME_MACHINE = "Machine";
	private static final String CERTIFICATE_TEMPLATENAME_DOMAINCONTROLLER = "DomainController";
	//private static final String CERTIFICATE_TEMPLATENAME_SMARTCARDLOGON = "SmartcardLogon";
	
	private static final String[] SUPPORTEDCERTIFICATETEMPLATES = {
		CERTIFICATE_TEMPLATENAME_USER, CERTIFICATE_TEMPLATENAME_MACHINE,
		CERTIFICATE_TEMPLATENAME_DOMAINCONTROLLER /*, CERTIFICATE_TEMPLATENAME_SMARTCARDLOGON*/};
	
	/*
	Non-crit key usage,
	NC Template Name: User
	NC CPD
	NC AIA
	NC EKU
	NC UPN
	User cert:
	SMIME Capabilities (non crit)
	[1]SMIME Capability
     Object ID=1.2.840.113549.3.2
     Parameters=02 01 38
	[2]SMIME Capability
     Object ID=1.2.840.113549.3.4
     Parameters=02 01 38
	[3]SMIME Capability
     Object ID=1.3.14.3.2.7


	Administrator:
	Microsoft Trust List Signing (1.3.6.1.4.1.311.10.3.1)

	 */

	private static final int[][] KEYUSAGES = {
		// "User" Key Usage: Digital signature, Allow key exchange only with key encryption
		{CertificateProfile.DIGITALSIGNATURE, CertificateProfile.KEYENCIPHERMENT},
		// "Machine" Key Usage: Digital signature, Allow key exchange only with key encryption
		{CertificateProfile.DIGITALSIGNATURE, CertificateProfile.KEYENCIPHERMENT},
		// "DomainController" Key Usage: 
		{CertificateProfile.DIGITALSIGNATURE},
		// "SmartcardLogon" Key Usage: 
		{CertificateProfile.DIGITALSIGNATURE, CertificateProfile.KEYENCIPHERMENT}
	};

	private static final int[][] EXTENDEDKEYUSAGES = {
		// "User" Extended Key Usage: Encrypting File System, Secure Email, Client Authentication
		{CertificateProfile.EFS_OBJECTID, CertificateProfile.EMAILPROTECTION, CertificateProfile.CLIENTAUTH},
		// "Machine" Extended Key Usage: Client Authentication, Server Authentication
		{CertificateProfile.CLIENTAUTH, CertificateProfile.SERVERAUTH},
		// "DomainController" Extended Key Usage: 
		{CertificateProfile.CLIENTAUTH, CertificateProfile.SERVERAUTH},
		// "SmartcardLogon" Extended Key Usage: 
		{CertificateProfile.CLIENTAUTH, CertificateProfile.SMARTCARDLOGON}
	};
	
	public static final String GET_SUBJECTDN_FROM_AD = "GET_SUBJECTDN_FROM_AD";
	
	private static final String[][] DNFIELDS = {
		// Required fields for "User"
		{GET_SUBJECTDN_FROM_AD, DnComponents.UPN},
		// Required fields for "Machine"
		{DnComponents.COMMONNAME},
		// Required fields for "DomainController"
		{DnComponents.COMMONNAME, DnComponents.DNSNAME, DnComponents.GUID},
		// Required fields for "SmartcardLogon"
		{GET_SUBJECTDN_FROM_AD, DnComponents.UPN}
	};

	// Special properties:
	// User: UPN is remote user? Is UPN even required?
	// Machine:
	// DomainController - Use CDP, Use CA defined CDP, Use MS Template Value, DomainController, GUID in request, DNS-name in request
	// SmartcardLogon - Use CDP, Use CA defined CDP, UPN is remote user
	
	private static final boolean[] USE_CA_CDP = {
		false, false, true, true
	};
	
	private static final String[] MS_TEMPLATE_VALUE = {
		null, null, "DomainController", null
	};

	
	public static String extractRequestFromRawData(String requestData) {
		if (requestData == null || "".equals(requestData)) {
			return null;
		}
		requestData = requestData.replaceFirst(REQUESTSTART, "").replaceFirst(REQUESTEND, "");
		return requestData.replaceAll(" ", "+");	// Replace lost +-chars in b64-encoding
	}

	public static int getTemplateIndex(String certificateTemplate) {
		int templateIndex = -1;
		if (certificateTemplate != null) {
			for (int i=0; i<SUPPORTEDCERTIFICATETEMPLATES.length; i++) {
				if (SUPPORTEDCERTIFICATETEMPLATES[i].equalsIgnoreCase(certificateTemplate)) {
					certificateTemplate = SUPPORTEDCERTIFICATETEMPLATES[i];
					templateIndex = i;
				}
			}
		}
		if (templateIndex == -1) {
			templateIndex = 0;
			log.warn("Got request for a unsupported certificate template \"" + certificateTemplate + "\" using \"" + SUPPORTEDCERTIFICATETEMPLATES[templateIndex] + "\" instead.");
			certificateTemplate = SUPPORTEDCERTIFICATETEMPLATES[templateIndex];
		}
		return templateIndex;
	}
	
	public static boolean isRequired(int templateIndex, String dnComponent, int count) {
		int counter = 0;
		for (int i=0; i<DNFIELDS[templateIndex].length; i++) {
			if (DNFIELDS[templateIndex][i].equals(dnComponent)) {
				if (counter == count) {
					return true;
				}
				counter++;
			}
		}
		return false;
	}

	public static int getOrCreateCertificateProfile(Admin admin, int templateIndex, ICertificateStoreSessionLocal certificateStoreSession) {
		String certProfileName = "Autoenroll-" + SUPPORTEDCERTIFICATETEMPLATES[templateIndex];
		// Create certificate profile if neccesary
		boolean newCertificateProfile = false;
		CertificateProfile certProfile = certificateStoreSession.getCertificateProfile(admin, certProfileName);
		if (certProfile == null) {
			certProfile = new CertificateProfile();
			try {
				certificateStoreSession.addCertificateProfile(admin, certProfileName, certProfile);
				newCertificateProfile = true;
			} catch (CertificateProfileExistsException e) {
				throw new EJBException(e);	// We just checked for this so this cannot happen
			}
		}
		// Add User-specifics to profiles if nessesary
		int[] keyUsages = KEYUSAGES[templateIndex];
		int[] extendedKeyUsages = EXTENDEDKEYUSAGES[templateIndex];
		if (newCertificateProfile) {
			certProfile.setUseKeyUsage(true);
			certProfile.setKeyUsageCritical(true);
			certProfile.setKeyUsage(new boolean[9]);
			for (int i=0; i<keyUsages.length; i++) {
				certProfile.setKeyUsage(keyUsages[i], true);
			}
			certProfile.setUseExtendedKeyUsage(true);
			certProfile.setExtendedKeyUsageCritical(true);
			ArrayList eku = new ArrayList();
			for (int i=0; i<extendedKeyUsages.length; i++) {
				eku.add(new Integer(extendedKeyUsages[i]));
			}
			certProfile.setExtendedKeyUsage(eku);
			if (USE_CA_CDP[templateIndex]) {
				certProfile.setUseCRLDistributionPoint(true);
				certProfile.setUseDefaultCRLDistributionPoint(true);
			}
			if (MS_TEMPLATE_VALUE[templateIndex] != null) {
				certProfile.setUseMicrosoftTemplate(true);
				certProfile.setMicrosoftTemplate(MS_TEMPLATE_VALUE[templateIndex]);
			}
		}
		certificateStoreSession.changeCertificateProfile(admin, certProfileName, certProfile);
		return certificateStoreSession.getCertificateProfileId(admin, certProfileName);
	}

	public static int getOrCreateEndEndtityProfile(Admin admin, int templateIndex, int certProfileId, int caid, String usernameShort, String fetchedSubjectDN, IRaAdminSessionLocal raAdminSession) {
		// Create end endity profile if neccesary
		String endEntityProfileName = "Autoenroll-" + SUPPORTEDCERTIFICATETEMPLATES[templateIndex];

		boolean newEndEntityProfile = false;
		raAdminSession.removeEndEntityProfile(admin, endEntityProfileName);	// TODO: This for debug and really innefficient..
		EndEntityProfile endEntityProfile = raAdminSession.getEndEntityProfile(admin, endEntityProfileName);

		if (endEntityProfile == null) {
			endEntityProfile = new EndEntityProfile(false);
			try {
				endEntityProfile.setValue(EndEntityProfile.DEFAULTCERTPROFILE, 0, "" + certProfileId);
				endEntityProfile.setValue(EndEntityProfile.AVAILCERTPROFILES, 0, "" + certProfileId);
				endEntityProfile.setValue(EndEntityProfile.DEFAULTCA, 0, "" + caid);
				endEntityProfile.setValue(EndEntityProfile.AVAILCAS, 0, "" + caid);
				endEntityProfile.setUse(EndEntityProfile.CLEARTEXTPASSWORD, 0,true);
				endEntityProfile.setValue(EndEntityProfile.CLEARTEXTPASSWORD,0,EndEntityProfile.TRUE);
				endEntityProfile.removeField(DnComponents.COMMONNAME, 0);	// We will add the right number of CNs later
				raAdminSession.addEndEntityProfile(admin, endEntityProfileName, endEntityProfile);
				newEndEntityProfile = true;
			} catch (EndEntityProfileExistsException e) {
				throw new EJBException(e);	// We just checked for this so this cannot happen
			}
		}
		String[] requiredFields = DNFIELDS[templateIndex];
		for (int i=0; i<requiredFields.length; i++) {
			if (GET_SUBJECTDN_FROM_AD.equals(requiredFields[i])) {
				log.info("Got DN "+ fetchedSubjectDN + " for user " + usernameShort);
				if (fetchedSubjectDN == null) {
					return -1;
				}
			}
		}
		if (newEndEntityProfile) {
			for (int i=0; i<requiredFields.length; i++) {
				if (GET_SUBJECTDN_FROM_AD.equals(requiredFields[i])) {
					DNFieldExtractor dnfe = new DNFieldExtractor(fetchedSubjectDN, DNFieldExtractor.TYPE_SUBJECTDN);
					// Loop through all fields in DN
					HashMap hmFields = dnfe.getNumberOfFields();
					for (int j=0; j<100; j++) {	// TODO: 100 is really an internal constant..
						Integer fieldsOfType = (Integer) hmFields.get(new Integer(j));
						if (fieldsOfType != null) {
							log.info("fieldsOfType="+fieldsOfType);
							for (int k = 0; k<fieldsOfType; k++) {
								endEntityProfile.addField(DnComponents.dnIdToProfileId(j));
								endEntityProfile.setRequired(DnComponents.dnIdToProfileId(j), k, true);
								log.info("Added a " + DnComponents.dnIdToProfileId(j) + " field and set it required.");
							}
						}
					}
				} else {
					int count = 0;
					for (int j=0; j<i; j++) {
						if (requiredFields[i].equals(requiredFields[j])) {
							count++;
						}
					}
					endEntityProfile.addField(requiredFields[i]);
					endEntityProfile.setRequired(requiredFields[i], count, true);
				}
			}
		}
		raAdminSession.changeEndEntityProfile(admin, endEntityProfileName, endEntityProfile);
		return raAdminSession.getEndEntityProfileId(admin, endEntityProfileName);
	}

	
}
