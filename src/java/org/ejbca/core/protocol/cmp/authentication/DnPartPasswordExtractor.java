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


package org.ejbca.core.protocol.cmp.authentication;

import org.apache.log4j.Logger;
import org.bouncycastle.asn1.cmp.PKIMessage;
import org.bouncycastle.asn1.crmf.CertReqMessages;
import org.bouncycastle.asn1.crmf.CertReqMsg;
import org.cesecore.util.CertTools;
import org.ejbca.config.CmpConfiguration;
import org.ejbca.core.protocol.cmp.CmpPKIBodyConstants;

/**
 * Extracts password from the request DN of a CMRF request message
 *
 * @version $Id$
 *
 */
public class DnPartPasswordExtractor implements ICMPAuthenticationModule {

    private static final Logger log = Logger.getLogger(DnPartPasswordExtractor.class);

    private String dnPart;
    private String password;
    
    public DnPartPasswordExtractor(String dnpart) {
        this.dnPart = dnpart;
        this.password = null;
    }
    
    /**
     * Extracts the value of 'dnPart' from the subjectDN of the certificate request template.
     * 
     * @param msg
     * @param username
     * @param authenticated
     * @return
     * @throws CMPAuthenticationException 
     */
    public boolean verifyOrExtract(final PKIMessage msg, final String username, boolean authenticated) throws CMPAuthenticationException {
        
        CertReqMsg req = getReq(msg);
        if(req == null) {
            throw new CMPAuthenticationException("No request was found in the PKIMessage");
        }
        
        final String dnString = req.getCertReq().getCertTemplate().getSubject().toString();
        if(log.isDebugEnabled()) {
            log.debug("Extracting password from SubjectDN '" + dnString + "' and DN part '" + dnPart + "'");
        }
        if (dnString != null) {
            password = CertTools.getPartFromDN(dnString, dnPart);
        }
            
        if(password == null) {
            throw new CMPAuthenticationException("Could not extract password from CRMF request using the " + getName() + " authentication module");
        }
            
        return true;
    }
    
    private CertReqMsg getReq(PKIMessage msg) {
        CertReqMsg req = null;
        int tagnr = msg.getBody().getType();
        if(tagnr == CmpPKIBodyConstants.INITIALIZATIONREQUEST || tagnr == CmpPKIBodyConstants.CERTIFICATAIONREQUEST) {
            req = ((CertReqMessages) msg.getBody().getContent()).toCertReqMsgArray()[0];
        }
        return req;
    }

    @Override
    public String getAuthenticationString() {
        return this.password;
    }

    @Override
    public String getName() {
        return CmpConfiguration.AUTHMODULE_DN_PART_PWD;
    }

}
