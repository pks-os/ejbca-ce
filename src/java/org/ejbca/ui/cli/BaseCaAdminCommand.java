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
 
package org.ejbca.ui.cli;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Collection;

import javax.naming.Context;

import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.ejbca.core.ejb.ca.crl.ICreateCRLSessionHome;
import org.ejbca.core.ejb.ca.store.ICertificateStoreSessionHome;
import org.ejbca.core.ejb.ca.store.ICertificateStoreSessionRemote;
import org.ejbca.core.model.ca.caadmin.CAInfo;
import org.ejbca.core.model.log.Admin;
import org.ejbca.util.Base64;
import org.ejbca.util.CertTools;


/**
 * Base for CA commands, contains comom functions for CA operations
 *
 * @version $Id: BaseCaAdminCommand.java,v 1.5 2007-12-21 09:02:33 anatom Exp $
 */
public abstract class BaseCaAdminCommand extends BaseAdminCommand {
    /** Private key alias in PKCS12 keystores */
    protected String privKeyAlias = "privateKey";
    protected char[] privateKeyPass = null;
    
    /**
     * Creates a new instance of BaseCaAdminCommand
     *
     * @param args command line arguments
     */
    public BaseCaAdminCommand(String[] args) {
        super(args, Admin.TYPE_CACOMMANDLINE_USER, "cli");
        // Install BouncyCastle provider
        CertTools.installBCProvider();
    }
    
    /** Retrieves the complete certificate chain from the CA
     *
     * @param human readable name of CA 
     * @return array of certificates, from ISignSession.getCertificateChain()
     */   
    protected Collection getCertChain(String caname) throws Exception{
        debug(">getCertChain()");
        Collection returnval = new ArrayList();
        try {
            CAInfo cainfo = this.getCAAdminSessionRemote().getCAInfo(administrator,caname);
            if (cainfo != null) {
                returnval = cainfo.getCertificateChain();
            } 
        } catch (Exception e) {
            error("Error while getting certfificate chain from CA.", e);
        }
        debug("<getCertChain()");
        return returnval;
    } // getCertChain 

    protected void makeCertRequest(String dn, KeyPair rsaKeys, String reqfile)
        throws NoSuchAlgorithmException, IOException, NoSuchProviderException, InvalidKeyException, 
            SignatureException {
        debug(">makeCertRequest: dn='" + dn + "', reqfile='" + reqfile + "'.");

        PKCS10CertificationRequest req = new PKCS10CertificationRequest("SHA1WithRSA",
                CertTools.stringToBcX509Name(dn), rsaKeys.getPublic(), null, rsaKeys.getPrivate());

        /* We don't use these uneccesary attributes
        DERConstructedSequence kName = new DERConstructedSequence();
        DERConstructedSet  kSeq = new DERConstructedSet();
        kName.addObject(PKCSObjectIdentifiers.pkcs_9_at_emailAddress);
        kSeq.addObject(new DERIA5String("foo@bar.se"));
        kName.addObject(kSeq);
        req.setAttributes(kName);
         */
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        DEROutputStream dOut = new DEROutputStream(bOut);
        dOut.writeObject(req);
        dOut.close();

        PKCS10CertificationRequest req2 = new PKCS10CertificationRequest(bOut.toByteArray());
        boolean verify = req2.verify();
        getOutputStream().println("Verify returned " + verify);

        if (verify == false) {
            getOutputStream().println("Aborting!");
            return;
        }

        FileOutputStream os1 = new FileOutputStream(reqfile);
        os1.write("-----BEGIN CERTIFICATE REQUEST-----\n".getBytes());
        os1.write(Base64.encode(bOut.toByteArray()));
        os1.write("\n-----END CERTIFICATE REQUEST-----\n".getBytes());
        os1.close();
        getOutputStream().println("CertificationRequest '" + reqfile + "' generated successfully.");
        debug("<makeCertRequest: dn='" + dn + "', reqfile='" + reqfile + "'.");
    } // makeCertRequest

    protected void createCRL(String issuerdn, boolean deltaCRL) {
        debug(">createCRL()");

        try {
            Context context = getInitialContext();
            ICreateCRLSessionHome home = (ICreateCRLSessionHome) javax.rmi.PortableRemoteObject.narrow(context.lookup(
                        "CreateCRLSession"), ICreateCRLSessionHome.class);
            if(issuerdn != null){
            	if (!deltaCRL) {
                    home.create().run(administrator, issuerdn);
                    ICertificateStoreSessionHome storehome = (ICertificateStoreSessionHome) javax.rmi.PortableRemoteObject.narrow(context.lookup(
                              "CertificateStoreSession"), ICertificateStoreSessionHome.class);
                    ICertificateStoreSessionRemote storeremote = storehome.create();
                    int number = storeremote.getLastCRLNumber(administrator, issuerdn, false);
                    getOutputStream().println("CRL with number " + number + " generated.");            		
            	} else {
                    home.create().runDeltaCRL(administrator, issuerdn);
                    ICertificateStoreSessionHome storehome = (ICertificateStoreSessionHome) javax.rmi.PortableRemoteObject.narrow(context.lookup(
                              "CertificateStoreSession"), ICertificateStoreSessionHome.class);
                    ICertificateStoreSessionRemote storeremote = storehome.create();
                    int number = storeremote.getLastCRLNumber(administrator, issuerdn, true);
                    getOutputStream().println("Delta CRL with number " + number + " generated.");
            	}
            }else{
            	int createdcrls = home.create().createCRLs(administrator);
            	getOutputStream().println("  " + createdcrls + " CRLs have been created.");	
            	int createddeltacrls = home.create().createDeltaCRLs(administrator);
            	getOutputStream().println("  " + createddeltacrls + " delta CRLs have been created.");	
            }
        } catch (Exception e) {
            error("Error while getting certficate chain from CA.", e);
        }

        debug(">createCRL()");
   } // createCRL
    
   protected String getIssuerDN(String caname) throws Exception{            
      CAInfo cainfo = getCAAdminSessionRemote().getCAInfo(administrator, caname);
      return cainfo.getSubjectDN();  
   }
   
   protected CAInfo getCAInfo(String caname) throws Exception {
	   CAInfo result;
	   try {
		   result = getCAAdminSessionRemote().getCAInfo(administrator, caname);
	   } catch (Exception e) {
		   debug("Error retriving CA " + caname + " info.", e);
		   throw new Exception("Error retriving CA " + caname + " info.");
	   }
	   if (result == null) {
		   debug("CA " + caname + " not found.");
		   throw new Exception("CA " + caname + " not found.");
	   }
	   return result;
   }
   
   
}
