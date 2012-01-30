/*************************************************************************
 *                                                                       *
 *  CESeCore: CE Security Core                                           *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.cesecore.certificates.crl;

import java.security.cert.CRLException;
import java.security.cert.Certificate;
import java.security.cert.X509CRL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.FinderException;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.log4j.Logger;
import org.cesecore.CesecoreException;
import org.cesecore.audit.enums.EventStatus;
import org.cesecore.audit.enums.EventTypes;
import org.cesecore.audit.enums.ModuleTypes;
import org.cesecore.audit.enums.ServiceTypes;
import org.cesecore.audit.log.SecurityEventsLoggerSessionLocal;
import org.cesecore.authentication.tokens.AlwaysAllowLocalAuthenticationToken;
import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authentication.tokens.UsernamePrincipal;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.authorization.control.AccessControlSessionLocal;
import org.cesecore.authorization.control.StandardRules;
import org.cesecore.certificates.ca.CA;
import org.cesecore.certificates.ca.CAConstants;
import org.cesecore.certificates.ca.CADoesntExistsException;
import org.cesecore.certificates.ca.CAInfo;
import org.cesecore.certificates.ca.CAOfflineException;
import org.cesecore.certificates.ca.CaSession;
import org.cesecore.certificates.ca.CaSessionLocal;
import org.cesecore.certificates.ca.X509CAInfo;
import org.cesecore.certificates.certificate.CertificateConstants;
import org.cesecore.certificates.certificate.CertificateData;
import org.cesecore.certificates.certificate.CertificateStoreSessionLocal;
import org.cesecore.config.CesecoreConfiguration;
import org.cesecore.internal.InternalResources;
import org.cesecore.jndi.JndiConstants;
import org.cesecore.keys.token.CryptoTokenOfflineException;
import org.cesecore.util.CertTools;
import org.cesecore.util.CryptoProviderTools;
import org.ejbca.core.ejb.ca.publisher.PublisherSessionLocal;

/**
 * Business class for CRL actions, i.e. running CRLs. CRUD operations can be found in CrlSession.
 * 
 * Based on EJBCA version CrlCreateSessionBean.java 11386 2011-02-22 19:03:22Z jeklund
 * @version $Id$
 */
@Stateless(mappedName = JndiConstants.APP_JNDI_PREFIX + "CrlCreateSessionRemote")
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class CrlCreateSessionBean implements CrlCreateSessionLocal, CrlCreateSessionRemote {

    private static final Logger log = Logger.getLogger(CrlCreateSessionBean.class);
    /** Internal localization of logs and errors */
    private static final InternalResources intres = InternalResources.getInstance();
    
    @PersistenceContext(unitName = CesecoreConfiguration.PERSISTENCE_UNIT)
    private EntityManager entityManager;
    @Resource
    private SessionContext sessionContext;
    @EJB
    private AccessControlSessionLocal accessSession;
    @EJB
    private SecurityEventsLoggerSessionLocal logSession;
    @EJB
    private CaSessionLocal caSession;
    @EJB
    private CertificateStoreSessionLocal certificateStoreSession;
    @EJB
    private CrlStoreSessionLocal crlSession;
    @EJB
    private PublisherSessionLocal publisherSession;

    private CrlCreateSessionLocal crlCreateSession;	// Used to run methods using different transaction attributes

    @PostConstruct
    public void postConstruct() {
    	crlCreateSession = sessionContext.getBusinessObject(CrlCreateSessionLocal.class);
    	// Install BouncyCastle provider if not available
    	CryptoProviderTools.installBCProviderIfNotAvailable();
    }

    @Override
    public int createCRLs(AuthenticationToken admin) throws AuthorizationDeniedException {
        return createCRLs(admin, null, 0);
    }

    @Override
	public int createDeltaCRLs(AuthenticationToken admin) throws AuthorizationDeniedException {
	    return createDeltaCRLs(admin, null, 0);
	}

    
    
	@Override
    public int createCRLs(CaSessionLocal caSession, AuthenticationToken admin, Collection<Integer> caids, long addtocrloverlaptime)
            throws AuthorizationDeniedException {
	    int createdcrls = 0;
        Iterator<Integer> iter = null;
        if (caids != null) {
            iter = caids.iterator();
        }
        if ((iter == null) || (caids.contains(Integer.valueOf(CAConstants.ALLCAS)))) {
            iter = caSession.getAvailableCAs().iterator();
        }
        while (iter.hasNext()) {
            int caid = ((Integer) iter.next()).intValue();
            log.debug("createCRLs for caid: " + caid);
            try {
                if (crlCreateSession.createCRLNewTransactionConditioned(caSession, admin, caid, addtocrloverlaptime)) {
                    createdcrls++;
                }                   
            } catch (CesecoreException e) {
                // Don't fail all generation just because one of the CAs had token offline or similar. 
                // Continue working with the others, but log an error message in system logs, use error logging 
                // since it might be something that should call for attention of the operators, CRL generation is important.
                String msg = intres.getLocalizedMessage("createcrl.errorcreate", caid, e.getMessage());
                log.error(msg, e); 
            }
        }
        return createdcrls;
    }

    @Override
    public int createCRLs(AuthenticationToken admin, Collection<Integer> caids, long addtocrloverlaptime) throws AuthorizationDeniedException {
		return createCRLs(this.caSession, admin, caids, addtocrloverlaptime);
    }
    
    

    @Override
    public int createDeltaCRLs(AuthenticationToken admin, Collection<Integer> caids, long crloverlaptime)
            throws AuthorizationDeniedException {
        int createddeltacrls = 0;
        Iterator<Integer> iter = null;
        if (caids != null) {
            iter = caids.iterator();
        }
        if ((iter == null) || (caids.contains(Integer.valueOf(CAConstants.ALLCAS)))) {
            iter = caSession.getAvailableCAs().iterator();
        }
        while (iter.hasNext()) {
            int caid = iter.next().intValue();
            log.debug("createDeltaCRLs for caid: " + caid);
            try {
                if (crlCreateSession.createDeltaCRLnewTransactionConditioned(caSession, admin, caid, crloverlaptime)) {
                    createddeltacrls++;
                }
            } catch (CesecoreException e) {
                // Don't fail all generation just because one of the CAs had token offline or similar. 
                // Continue working with the others, but log a warning message in system logs.
                String msg = intres.getLocalizedMessage("createcrl.errorcreate", caid, e.getMessage());
                log.error(msg, e);                  
                Map<String, Object> details = new LinkedHashMap<String, Object>();
                details.put("msg", msg);
                logSession.log(EventTypes.CRL_CREATION, EventStatus.FAILURE, ModuleTypes.CRL, ServiceTypes.CORE, admin.toString(), Integer.valueOf(caid).toString(), null, null, details);              
            }                
        }
        return createddeltacrls;
    }

    @Override
    public int createDeltaCRLs(CaSessionLocal caSession, AuthenticationToken admin, Collection<Integer> caids, long crloverlaptime) throws AuthorizationDeniedException {
        return createDeltaCRLs(admin, caids, crloverlaptime);
    }

    @Override
    public boolean createCRLNewTransactionConditioned(CaSessionLocal caSession, AuthenticationToken admin, int caid, long addtocrloverlaptime) throws CryptoTokenOfflineException, CADoesntExistsException, AuthorizationDeniedException, CAOfflineException {
        boolean ret = false;
        Date currenttime = new Date();
        // Get CA checks authorization to the CA
        CA ca = caSession.getCA(admin, caid);
        CAInfo cainfo = ca.getCAInfo();
        try {
            if (cainfo.getStatus() == CAConstants.CA_EXTERNAL) {
                if (log.isDebugEnabled()) {
                    log.debug("Not trying to generate CRL for external CA "+cainfo.getName());
                }
            } else if (cainfo.getStatus() == CAConstants.CA_WAITING_CERTIFICATE_RESPONSE) {
                if (log.isDebugEnabled()) {
                    log.debug("Not trying to generate CRL for CA "+cainfo.getName() +" awaiting certificate response.");
                }
            } else {
                if (cainfo instanceof X509CAInfo) {
                    Collection<Certificate> certs = cainfo.getCertificateChain();
                    final Certificate cacert;
                    if (!certs.isEmpty()) {
                        cacert = certs.iterator().next();   
                    } else {
                        cacert = null;
                    }
                    // Don't create CRLs if the CA has expired
                    if ( (cacert != null) && (CertTools.getNotAfter(cacert).after(new Date())) ) {
                        if (cainfo.getStatus() == CAConstants.CA_OFFLINE )  {
                            // Normal event to not create CRLs for CAs that are deliberately set off line
                            String msg = intres.getLocalizedMessage("createcrl.caoffline", cainfo.getName(), Integer.valueOf(cainfo.getCAId()));                                                   
                            log.info(msg);
                        } else {
                            if (log.isDebugEnabled()) {
                                log.debug("Checking to see if CA '"+cainfo.getName()+"' ("+cainfo.getCAId()+") needs CRL generation.");
                            }
                            final String certSubjectDN = CertTools.getSubjectDN(cacert);
                            CRLInfo crlinfo = crlSession.getLastCRLInfo(certSubjectDN,false);
                            if (log.isDebugEnabled()) {
                                if (crlinfo == null) {
                                    log.debug("Crlinfo was null");
                                } else {
                                    log.debug("Read crlinfo for CA: "+cainfo.getName()+", lastNumber="+crlinfo.getLastCRLNumber()+", expireDate="+crlinfo.getExpireDate());
                                }                                          
                            }
                            long crlissueinterval = cainfo.getCRLIssueInterval();
                            if (log.isDebugEnabled()) {
                                log.debug("crlissueinterval="+crlissueinterval);
                                log.debug("crloverlaptime="+cainfo.getCRLOverlapTime());                                   
                            }
                            long overlap = cainfo.getCRLOverlapTime() + addtocrloverlaptime; // Overlaptime is in minutes, default if crlissueinterval == 0
                            long nextUpdate = 0; // if crlinfo == 0, we will issue a crl now
                            if (crlinfo != null) {
                                // CRL issueinterval in hours. If this is 0, we should only issue a CRL when
                                // the old one is about to expire, i.e. when currenttime + overlaptime > expiredate
                                // if isseuinterval is > 0 we will issue a new CRL when currenttime > createtime + issueinterval
                                nextUpdate = crlinfo.getExpireDate().getTime(); // Default if crlissueinterval == 0
                                if (crlissueinterval > 0) {
                                    long u = crlinfo.getCreateDate().getTime() + crlissueinterval;
                                    // If this period for some reason (we missed to issue some?) is larger than when the CRL expires,
                                    // we need to issue one when the CRL expires
                                    if ((u + overlap) < nextUpdate) {
                                        nextUpdate = u;
                                        // When we issue CRLs before the real expiration date we don't use overlap
                                        overlap = 0;
                                    }
                                }                                   
                                if (log.isDebugEnabled()) {
                                    log.debug("Calculated nextUpdate to "+nextUpdate);
                                }
                            } else {
                                // If crlinfo is null (no crl issued yet) nextUpdate will be 0 and a new CRL should be generated
                                String msg = intres.getLocalizedMessage("createcrl.crlinfonull", cainfo.getName());                                                
                                log.info(msg);
                            }
                            if ((currenttime.getTime() + overlap) >= nextUpdate) {
                                if (log.isDebugEnabled()) {
                                    log.debug("Creating CRL for CA, because:"+currenttime.getTime()+overlap+" >= "+nextUpdate);                                                
                                }
                                if (internalCreateCRL(admin, ca) != null) {
                                    ret = true;                                 
                                }
                            }
                        }
                    } else if (cacert != null) {
                        if (log.isDebugEnabled()) {
                            log.debug("Not creating CRL for expired CA "+cainfo.getName()+". CA subjectDN='"+CertTools.getSubjectDN(cacert)+"', expired: "+CertTools.getNotAfter(cacert));
                        }
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Not creating CRL for CA without CA certificate: "+cainfo.getName());
                        }
                    }
                }                                                          
            }
        } catch (CryptoTokenOfflineException e) {
            log.warn("Crypto token is offline for CA "+caid+" generating CRL.");
            throw e;            
        }
        return ret;
    }
    
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Override
    public boolean createCRLNewTransactionConditioned(AuthenticationToken admin, int caid, long addtocrloverlaptime) throws CryptoTokenOfflineException, CADoesntExistsException, AuthorizationDeniedException, CAOfflineException {
        return createCRLNewTransactionConditioned(this.caSession, admin, caid, addtocrloverlaptime);
    }
    
    
    @Override
    public boolean createDeltaCRLnewTransactionConditioned(CaSessionLocal caSession, AuthenticationToken admin, int caid, long crloverlaptime) throws CryptoTokenOfflineException, CAOfflineException, CADoesntExistsException, AuthorizationDeniedException {
        boolean ret = false;
        Date currenttime = new Date();
        CA ca = caSession.getCA(admin, caid);
        CAInfo cainfo = ca.getCAInfo();
        try{
            if (cainfo.getStatus() == CAConstants.CA_EXTERNAL) {
                if (log.isDebugEnabled()) {
                    log.debug("Not trying to generate delta CRL for external CA "+cainfo.getName());
                }
            } else if (cainfo.getStatus() == CAConstants.CA_WAITING_CERTIFICATE_RESPONSE) {
                if (log.isDebugEnabled()) {
                    log.debug("Not trying to generate delta CRL for CA "+cainfo.getName() +" awaiting certificate response.");
                }
            } else {
                if (cainfo instanceof X509CAInfo) {
                    Collection<Certificate> certs = cainfo.getCertificateChain();
                    final Certificate cacert;
                    if (!certs.isEmpty()) {
                        cacert = certs.iterator().next();   
                    } else {
                        cacert = null;
                    }
                    // Don't create CRLs if the CA has expired
                    if ( (cacert != null) && (CertTools.getNotAfter(cacert).after(new Date())) ) {
                        if(cainfo.getDeltaCRLPeriod() > 0) {
                            if (cainfo.getStatus() == CAConstants.CA_OFFLINE) {
                                // Normal event to not create CRLs for CAs that are deliberately set off line
                                String msg = intres.getLocalizedMessage("createcrl.caoffline", cainfo.getName(), Integer.valueOf(cainfo.getCAId()));                                                   
                                log.info(msg);
                            } else {
                                if (log.isDebugEnabled()) {
                                    log.debug("Checking to see if CA '"+cainfo.getName()+"' needs Delta CRL generation.");
                                }
                                final String certSubjectDN = CertTools.getSubjectDN(cacert);
                                CRLInfo deltacrlinfo = crlSession.getLastCRLInfo(certSubjectDN, true);
                                if (log.isDebugEnabled()) {
                                    if (deltacrlinfo == null) {
                                        log.debug("DeltaCrlinfo was null");
                                    } else {
                                        log.debug("Read deltacrlinfo for CA: "+cainfo.getName()+", lastNumber="+deltacrlinfo.getLastCRLNumber()+", expireDate="+deltacrlinfo.getExpireDate());
                                    }                                          
                                }
                                if((deltacrlinfo == null) || ((currenttime.getTime() + crloverlaptime) >= deltacrlinfo.getExpireDate().getTime())){
                                    if (internalCreateDeltaCRL(admin, ca, -1, -1) != null) {
                                        ret = true;
                                    }
                                }
                            }
                        }
                    } else if (cacert != null) {
                        if (log.isDebugEnabled()) {
                            log.debug("Not creating delta CRL for expired CA "+cainfo.getName()+". CA subjectDN='"+CertTools.getSubjectDN(cacert)+"', expired: "+CertTools.getNotAfter(cacert));
                        }
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Not creating delta CRL for CA without CA certificate: "+cainfo.getName());
                        }
                    }
                }                                       
            }
        } catch (CryptoTokenOfflineException e) {
            log.warn("Crypto token is offline for CA "+caid+" generating CRL.");
            throw e;            
        }
        return ret;
    }
        
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Override
    public boolean createDeltaCRLnewTransactionConditioned(AuthenticationToken admin, int caid, long crloverlaptime) throws CryptoTokenOfflineException, CAOfflineException, CADoesntExistsException, AuthorizationDeniedException {
        return createDeltaCRLnewTransactionConditioned(caSession, admin, caid, crloverlaptime);
    }

    @Override
    public boolean forceCRL(AuthenticationToken admin, int caid) throws CADoesntExistsException, AuthorizationDeniedException, CryptoTokenOfflineException, CAOfflineException {
    	CA ca = caSession.getCA(admin, caid);
    	return internalCreateCRL(admin, ca) != null;
    }

    @Override
    public boolean forceDeltaCRL(AuthenticationToken admin, int caid) throws CADoesntExistsException, AuthorizationDeniedException, CryptoTokenOfflineException, CAOfflineException {
    	CA ca = caSession.getCA(admin, caid);
    	final CRLInfo crlInfo = crlSession.getLastCRLInfo(ca.getSubjectDN(), false);
    	// if no full CRL has been generated we can't create a delta CRL
        boolean ret = false;
    	if (crlInfo != null) {
            CAInfo cainfo = ca.getCAInfo();
            if (cainfo.getDeltaCRLPeriod() > 0) {
                byte[] crl = internalCreateDeltaCRL(admin, ca, crlInfo.getLastCRLNumber(), crlInfo.getCreateDate().getTime());
                ret = (crl != null);    
            }
    	} else {
    		log.info("No full CRL exists when trying to generate delta CRL for caid "+caid);
    	}
        return ret;
    }

	/**
     * Generates a new CRL by looking in the database for revoked certificates
     * and generating a CRL. This method also "archives" certificates when after
     * they are no longer needed in the CRL.
     * Generates the CRL and stores it in the database.
     * 
     * @param admin administrator performing the task
     * @param ca the CA this operation regards
     * @return fingerprint (primary key) of the generated CRL or null if
     *            generation failed
	 * @throws AuthorizationDeniedException 
     * @throws javax.ejb.EJBException if a communications- or system error occurs
     */
    private String internalCreateCRL(AuthenticationToken admin, CA ca) throws CAOfflineException, CryptoTokenOfflineException, AuthorizationDeniedException {
    	if (log.isTraceEnabled()) {
    		log.trace(">internalCreateCRL()");
    	}
    	if (ca == null) {
    		throw new EJBException("No CA specified.");
    	}
    	CAInfo cainfo = ca.getCAInfo();
    	String ret = null;
    	try {
    		final String caCertSubjectDN; // DN from the CA issuing the CRL to be used when searching for the CRL in the database.
    		{
    			final Collection<Certificate> certs = cainfo.getCertificateChain();
    			final Certificate cacert = !certs.isEmpty() ? certs.iterator().next(): null;
    			caCertSubjectDN = cacert!=null ? CertTools.getSubjectDN(cacert) : null;
    		}
    		// We can not create a CRL for a CA that is waiting for certificate response
    		if ( caCertSubjectDN!=null && cainfo.getStatus()==CAConstants.CA_ACTIVE )  {
    			long crlperiod = cainfo.getCRLPeriod();
    			// Find all revoked certificates for a complete CRL
    			Collection<RevokedCertInfo> revcerts = certificateStoreSession.listRevokedCertInfo(caCertSubjectDN, -1);
    			if (log.isDebugEnabled()) {
    				log.debug("Found "+revcerts.size()+" revoked certificates.");
    			}
    			// Go through them and create a CRL, at the same time archive expired certificates
    			//
    			// Archiving is only done for full CRLs, not delta CRLs.
    			// RFC5280 states that a certificate must not be removed from the CRL until it has appeared on at least on full CRL.
    			// See RFC5280 section 5.2.4, specifically:
    			//  If a certificate revocation notice first appears on a delta CRL, then
    			//  it is possible for the certificate validity period to expire before
    			//  the next complete CRL for the same scope is issued.  In this case,
    			//  the revocation notice MUST be included in all subsequent delta CRLs
    			//  until the revocation notice is included on at least one explicitly
    			//  issued complete CRL for this scope
    			Date now = new Date();
    			Date check = new Date(now.getTime() - crlperiod);
    			Iterator<RevokedCertInfo> iter = revcerts.iterator();
    			AuthenticationToken archiveAdmin = new AlwaysAllowLocalAuthenticationToken(new UsernamePrincipal("CrlCreateSession.archive_expired"));
    			while (iter.hasNext()) {
    				RevokedCertInfo data = iter.next();
    				// We want to include certificates that was revoked after the last CRL was issued, but before this one
    				// so the revoked certs are included in ONE CRL at least. See RFC5280 section 3.3.
    				if ( data.getExpireDate().before(check) ) {
    					// Certificate has expired, set status to archived in the database
    					certificateStoreSession.setStatus(archiveAdmin, data.getCertificateFingerprint(), CertificateConstants.CERT_ARCHIVED);
    				} else {
    					Date revDate = data.getRevocationDate();
    					if (revDate == null) {
    						data.setRevocationDate(now);
    						CertificateData certdata = CertificateData.findByFingerprint(entityManager, data.getCertificateFingerprint());
    						if (certdata == null) {
    							throw new FinderException("No certificate with fingerprint " + data.getCertificateFingerprint());
    						}
    						// Set revocation date in the database
    						certdata.setRevocationDate(now);
    					}
    				}
    			}
    			// a full CRL
    			byte[] crlBytes = generateAndStoreCRL(admin, ca, revcerts, -1);
    			if (crlBytes != null) {
    				ret = CertTools.getFingerprintAsString(crlBytes);                       
    			}
    			// This debug logging is very very heavy if you have large CRLs. Please don't use it :-)
    			//              if (log.isDebugEnabled()) {
    			//              X509CRL crl = CertTools.getCRLfromByteArray(crlBytes);
    			//              debug("Created CRL with expire date: "+crl.getNextUpdate());
    			//              FileOutputStream fos = new FileOutputStream("c:\\java\\srvtestcrl.der");
    			//              fos.write(crl.getEncoded());
    			//              fos.close();
    			//              }
    		} else {
    			String msg = intres.getLocalizedMessage("createcrl.errornotactive", cainfo.getName(), Integer.valueOf(cainfo.getCAId()), cainfo.getStatus());                                                      
    			log.info(msg);   
    			throw new CAOfflineException(msg);
    		}
    	} catch (CryptoTokenOfflineException e) {
    		throw e;            
    	} catch (FinderException e) {
    		// Should really not happen
    		log.error(e);
    		throw new EJBException(e);
    	} 
    	if (log.isTraceEnabled()) {
    		log.trace("<internalCreateCRL()");
    	}
    	return ret;
    }

    /**
     * Generates a new Delta CRL by looking in the database for revoked
     * certificates since the last complete CRL issued and generating a CRL with
     * the difference. If either of baseCrlNumber or baseCrlCreateTime is -1
     * this method will try to query the database for the last complete CRL.
     * Generates the CRL and stores it in the database.
     * 
     * @param admin administrator performing the task
     * @param ca the CA this operation regards
     * @param baseCrlNumber
     *            base crl number to be put in the delta CRL, this is the CRL
     *            number of the previous complete CRL. If value is -1 the value
     *            is fetched by querying the database looking for the last
     *            complete CRL.
     * @param baseCrlCreateTime
     *            the time the base CRL was issued. If value is -1 the value is
     *            fetched by querying the database looking for the last complete
     *            CRL.
     * @return the bytes of the Delta CRL generated or null of no delta CRL was
     *         generated.
     * @throws AuthorizationDeniedException 
     * @throws javax.ejb.EJBException if a communications- or system error occurs
     */
    private byte[] internalCreateDeltaCRL(AuthenticationToken admin, CA ca, int baseCrlNumber, long baseCrlCreateTime) throws CryptoTokenOfflineException, CAOfflineException, AuthorizationDeniedException {
    	if (ca == null) {
    		throw new EJBException("No CA specified.");
    	}
    	CAInfo cainfo = ca.getCAInfo();
        if (log.isTraceEnabled()) {
                log.trace(">internalCreateDeltaCRL: "+cainfo.getSubjectDN());
        }
        byte[] crlBytes = null;
        try {
        	final String caCertSubjectDN; {
        		final Collection<Certificate> certs = cainfo.getCertificateChain();
        		final Certificate cacert = !certs.isEmpty() ? certs.iterator().next(): null;
        		caCertSubjectDN = cacert!=null ? CertTools.getSubjectDN(cacert) : null;
        	}
    		// We can not create a CRL for a CA that is waiting for certificate response
    		if ( caCertSubjectDN!=null && cainfo.getStatus()==CAConstants.CA_ACTIVE )  {
        		if ( (baseCrlNumber == -1) && (baseCrlCreateTime == -1) ) {
        			CRLInfo basecrlinfo = crlSession.getLastCRLInfo(caCertSubjectDN, false);
        			baseCrlCreateTime = basecrlinfo.getCreateDate().getTime();
        			baseCrlNumber = basecrlinfo.getLastCRLNumber();                                 
        		}
        		// Find all revoked certificates
        		Collection<RevokedCertInfo> revcertinfos = certificateStoreSession.listRevokedCertInfo(caCertSubjectDN, baseCrlCreateTime);
        		if (log.isDebugEnabled()) {
        			log.debug("Found "+revcertinfos.size()+" revoked certificates.");
        		}
        		// Go through them and create a CRL, at the same time archive expired certificates
        		ArrayList<RevokedCertInfo> certs = new ArrayList<RevokedCertInfo>();
        		Iterator<RevokedCertInfo> iter = revcertinfos.iterator();
        		while (iter.hasNext()) {
        			RevokedCertInfo ci = iter.next();
        			if (ci.getRevocationDate() == null) {
        				ci.setRevocationDate(new Date());
        			}
        			certs.add(ci);
        		}
        		// create a delta CRL
        		crlBytes = generateAndStoreCRL(admin, ca, certs, baseCrlNumber);
        		X509CRL crl = CertTools.getCRLfromByteArray(crlBytes);
        		if (log.isDebugEnabled()) {
        			log.debug("Created delta CRL with expire date: "+crl.getNextUpdate());
        		}
    		} else {
    			String msg = intres.getLocalizedMessage("createcrl.errornotactive", cainfo.getName(), Integer.valueOf(cainfo.getCAId()), cainfo.getStatus());                                                      
    			log.info(msg);   
    			throw new CAOfflineException(msg);
    		}
        } catch (CryptoTokenOfflineException e) {
        	throw e;            
        } catch (CRLException e) {
    		// Should really not happen
        	log.error(e);
        	throw new EJBException(e);
		}
        if (log.isTraceEnabled()) {
        	log.trace("<internalCreateDeltaCRL: "+cainfo.getSubjectDN());
        }
        return crlBytes;
    }

    /**
     * Requests for a CRL to be created with the passed (revoked) certificates. 
     * Generates the CRL and stores it in the database.
     *
     * @param admin administrator performing the task
     * @param ca the CA this operation regards
     * @param certs collection of RevokedCertInfo object.
     * @param basecrlnumber the CRL number of the Base CRL to generate a deltaCRL, -1 to generate a full CRL
     * @return The newly created CRL in DER encoded byte form or null, use CertTools.getCRLfromByteArray to convert to X509CRL.
     * @throws AuthorizationDeniedException 
     * @throws CryptoTokenOfflineException 
     */
    private byte[] generateAndStoreCRL(AuthenticationToken admin, CA ca, Collection<RevokedCertInfo> certs, int basecrlnumber) throws CryptoTokenOfflineException, AuthorizationDeniedException {
    	if (log.isTraceEnabled()) {
    		log.trace(">createCRL(Collection)");
    	}
    	byte[] crlBytes = null; // return value

    	// Check that we are allowed to create CRLs
    	// Authorization for other things, that we have access to the CA has already been done
    	final int caid = ca.getCAId();
    	authorizedToCreateCRL(admin, caid);
    	
    	try {
    		if ( (ca.getStatus() != CAConstants.CA_ACTIVE) && (ca.getStatus() != CAConstants.CA_WAITING_CERTIFICATE_RESPONSE) ) {
    			String msg = intres.getLocalizedMessage("createcert.canotactive", ca.getSubjectDN());
    			throw new CryptoTokenOfflineException(msg);
    		}
    		final X509CRL crl;
    		final String certSubjectDN = CertTools.getSubjectDN(ca.getCACertificate());
    		int fullnumber = crlSession.getLastCRLNumber(certSubjectDN, false);
    		int deltanumber = crlSession.getLastCRLNumber(certSubjectDN, true);
    		// nextCrlNumber: The highest number of last CRL (full or delta) and increased by 1 (both full CRLs and deltaCRLs share the same series of CRL Number)
    		int nextCrlNumber = ( (fullnumber > deltanumber) ? fullnumber : deltanumber ) +1; 
    		boolean deltaCRL = (basecrlnumber > -1);
    		if (deltaCRL) {
    			// Workaround if transaction handling fails so that crlNumber for deltaCRL would happen to be the same
    			if (nextCrlNumber == basecrlnumber) {
    				nextCrlNumber++;
    			}
    			crl = (X509CRL) ca.generateDeltaCRL(certs, nextCrlNumber, basecrlnumber);       
    		} else {
    			crl = (X509CRL) ca.generateCRL(certs, nextCrlNumber);
    		}
    		if (crl != null) {
    			// Store CRL in the database, this can still fail so the whole thing is rolled back
    			String cafp = CertTools.getFingerprintAsString(ca.getCACertificate());
    			byte[] tmpcrlBytes = crl.getEncoded();                    
    			if (log.isDebugEnabled()) {
    				log.debug("Storing CRL in certificate store.");
    			}
    			crlSession.storeCRL(admin, tmpcrlBytes, cafp, nextCrlNumber, crl.getIssuerDN().getName(), crl.getThisUpdate(), crl.getNextUpdate(), (deltaCRL ? 1 : -1));
    			// TODO: publishing below is something that is NOT included in CESeCore.
    			// It is an add-one for EJBCA put here because we did not want to refactor all references to CrlCreateSessionBean in the last minute.
    			// Hard and error-prone to do that.
    			if (log.isDebugEnabled()) {
    			    log.debug("Storing CRL in publishers");
    			}
                this.publisherSession.storeCRL(admin, ca.getCRLPublishers(), tmpcrlBytes, cafp, nextCrlNumber, ca.getSubjectDN());
                
    			String msg = intres.getLocalizedMessage("createcrl.createdcrl", Integer.valueOf(nextCrlNumber), ca.getName(), ca.getSubjectDN());
    			Map<String, Object> details = new LinkedHashMap<String, Object>();
    			details.put("msg", msg);
    			logSession.log(EventTypes.CRL_CREATION, EventStatus.SUCCESS, ModuleTypes.CRL, ServiceTypes.CORE, admin.toString(), Integer.valueOf(caid).toString(), null, null, details);	                	
    			// Now all is finished and audit logged, now we are ready to "really" set the return value
    			crlBytes = tmpcrlBytes; 
    		}
    	} catch (CryptoTokenOfflineException ctoe) {
    		String msg = intres.getLocalizedMessage("error.catokenoffline", ca.getSubjectDN());
    		log.info(msg, ctoe);
    		String auditmsg = intres.getLocalizedMessage("createcrl.errorcreate", ca.getName(), ctoe.getMessage());
    		Map<String, Object> details = new LinkedHashMap<String, Object>();
    		details.put("msg", auditmsg);
    		logSession.log(EventTypes.CRL_CREATION, EventStatus.FAILURE, ModuleTypes.CRL, ServiceTypes.CORE, admin.toString(), Integer.valueOf(caid).toString(), null, null, details);
    		throw ctoe;
    	} catch (Exception e) {
    		log.info("Error generating CRL: ", e);
    		String msg = intres.getLocalizedMessage("createcrl.errorcreate", ca.getName(), e.getMessage());
    		Map<String, Object> details = new LinkedHashMap<String, Object>();
    		details.put("msg", msg);
    		logSession.log(EventTypes.CRL_CREATION, EventStatus.FAILURE, ModuleTypes.CRL, ServiceTypes.CORE, admin.toString(), Integer.valueOf(caid).toString(), null, null, details);
    		if (e instanceof EJBException) {
    			throw (EJBException)e;
    		}
    		throw new EJBException(msg, e);
    	}
    	if (log.isTraceEnabled()) {
    		log.trace("<createCRL(Collection)");
    	}
    	return crlBytes;
    }

    private void authorizedToCreateCRL(final AuthenticationToken admin, final int caid) throws AuthorizationDeniedException {
    	if (!accessSession.isAuthorized(admin, StandardRules.CREATECRL.resource())) {
    		final String msg = intres.getLocalizedMessage("createcrl.notauthorized", admin.toString(), caid);
    		throw new AuthorizationDeniedException(msg);
    	}
    }

}
