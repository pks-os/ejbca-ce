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

package org.ejbca.core.protocol.ws.client;

import java.io.FileOutputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.List;

import org.apache.commons.lang.RandomStringUtils;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.ECPointUtil;
import org.bouncycastle.jce.provider.asymmetric.ec.EC5Util;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Hex;
import org.ejbca.core.model.ra.UserDataConstants;
import org.ejbca.core.protocol.ws.client.gen.AuthorizationDeniedException_Exception;
import org.ejbca.core.protocol.ws.client.gen.Certificate;
import org.ejbca.core.protocol.ws.client.gen.UserDataVOWS;
import org.ejbca.core.protocol.ws.client.gen.UserDoesntFullfillEndEntityProfile_Exception;
import org.ejbca.cvc.CAReferenceField;
import org.ejbca.cvc.CVCAuthenticatedRequest;
import org.ejbca.cvc.CVCObject;
import org.ejbca.cvc.CVCertificate;
import org.ejbca.cvc.CertificateGenerator;
import org.ejbca.cvc.CertificateParser;
import org.ejbca.cvc.HolderReferenceField;
import org.ejbca.cvc.PublicKeyEC;
import org.ejbca.ui.cli.ErrorAdminCommandException;
import org.ejbca.ui.cli.IAdminCommand;
import org.ejbca.ui.cli.IllegalAdminCommandException;
import org.ejbca.util.Base64;
import org.ejbca.util.CertTools;
import org.ejbca.util.FileTools;
import org.ejbca.util.keystore.KeyTools;


/**
 * Creates or edits a user and sends a CVC request. Writes the issues CV Certificate to file
 *
 * @version $Id$
 */
public class CvcRequestCommand extends EJBCAWSRABaseCommand implements IAdminCommand{


	private static final int ARG_USERNAME           = 1;
	private static final int ARG_PASSWORD           = 2;
	private static final int ARG_SUBJECTDN          = 3;
	private static final int ARG_SEQUENCE           = 4;
	private static final int ARG_CA                 = 5;
	private static final int ARG_SIGNALG            = 6;
	private static final int ARG_KEYSPEC            = 7;
	private static final int ARG_ENDENTITYPROFILE   = 8;
	private static final int ARG_CERTIFICATEPROFILE = 9;
	private static final int ARG_GENREQ             = 10;
	private static final int ARG_BASEFILENAME       = 11;
	private static final int ARG_AUTHSIGNKEY        = 12;
	private static final int ARG_AUTHSIGNCERT       = 13;

	/**
	 * Creates a new instance of CvcRequestCommand
	 *
	 * @param args command line arguments
	 */
	public CvcRequestCommand(String[] args) {
		super(args);
	}

	/**
	 * Runs the command
	 *
	 * @throws IllegalAdminCommandException Error in command args
	 * @throws ErrorAdminCommandException Error running command
	 */
	public void execute() throws IllegalAdminCommandException, ErrorAdminCommandException {

		try {   
			if(args.length < 12 || args.length > 14){
				getPrintStream().println("Number of arguments: "+args.length);
				usage();
				System.exit(-1);
			}

			UserDataVOWS userdata = new UserDataVOWS();
			userdata.setUsername(args[ARG_USERNAME]);
			userdata.setPassword(args[ARG_PASSWORD]);
			userdata.setClearPwd(false);
			userdata.setSubjectDN(args[ARG_SUBJECTDN]);
			String sequence = args[ARG_SEQUENCE];
			userdata.setCaName(args[ARG_CA]);
			userdata.setEndEntityProfileName(args[ARG_ENDENTITYPROFILE]);
			userdata.setCertificateProfileName(args[ARG_CERTIFICATEPROFILE]);
			userdata.setTokenType("USERGENERATED");
			userdata.setStatus(UserDataConstants.STATUS_NEW);
			String signatureAlg = args[ARG_SIGNALG];
			String keySpec = args[ARG_KEYSPEC];
			boolean genrequest = args[ARG_GENREQ].equalsIgnoreCase("true");
			String basefilename = args[ARG_BASEFILENAME];
			String authSignKeyFile = null;
			if (args.length > (ARG_AUTHSIGNKEY)) {
				authSignKeyFile = args[ARG_AUTHSIGNKEY];				
			}
			String authSignCertFile = null;
			if (args.length > (ARG_AUTHSIGNCERT)) {
				authSignCertFile = args[ARG_AUTHSIGNCERT];				
			}

			getPrintStream().println("Trying to add user:");
			getPrintStream().println("Username: "+userdata.getUsername());
			getPrintStream().println("Subject name: "+userdata.getSubjectDN());
			getPrintStream().println("Sequence: "+sequence);
			getPrintStream().println("CA Name: "+userdata.getCaName());                        
			getPrintStream().println("Signature algorithm: "+signatureAlg);                        
			getPrintStream().println("Key spec: "+keySpec);                        
			getPrintStream().println("End entity profile: "+userdata.getEndEntityProfileName());
			getPrintStream().println("Certificate profile: "+userdata.getCertificateProfileName());

			try{
				CertTools.installBCProvider();
				
				String cvcreq = null;
				if (genrequest) {
					getPrintStream().println("Generating a new request with base filename: "+basefilename);
					// Generate keys for the request
					String keytype = "RSA";
					if (signatureAlg.contains("ECDSA")) {
						keytype = "ECDSA";
					}
					KeyPair keyPair = KeyTools.genKeys(keySpec, keytype);
					String dn = userdata.getSubjectDN();
					String country = CertTools.getPartFromDN(dn, "C");
					String mnemonic = CertTools.getPartFromDN(dn, "CN");
					if (sequence.equalsIgnoreCase("null")) {
						sequence = RandomStringUtils.randomNumeric(5);
						getPrintStream().println("No sequence given, using random 5 number sequence: "+sequence);
					}
					CAReferenceField caRef = new CAReferenceField(country,mnemonic,sequence);
					// We are making a self signed request, so holder ref is same as ca ref
					HolderReferenceField holderRef = new HolderReferenceField(caRef.getCountry(), caRef.getMnemonic(), caRef.getSequence());
					CVCertificate request = CertificateGenerator.createRequest(keyPair, signatureAlg, caRef, holderRef);
					byte[] der = request.getDEREncoded();
					if (authSignKeyFile != null) {
						getPrintStream().println("Reading private key from pkcs8 file "+authSignKeyFile+" to create an authenticated request");
						byte[] keybytes = FileTools.readFiletoBuffer(authSignKeyFile);
				        KeyFactory keyfact = KeyFactory.getInstance(keytype, "BC");
				        PrivateKey privKey = keyfact.generatePrivate(new PKCS8EncodedKeySpec(keybytes));
				        KeyPair authKeyPair = new KeyPair(null, privKey); // We don't need the public key
						CAReferenceField authCaRef = caRef;
						CVCertificate authCert = null;
						if (authSignCertFile != null) {
							getPrintStream().println("Reading cert from cvcert file "+authSignCertFile+" to create an authenticated request");							
							byte[] cert = FileTools.readFiletoBuffer(authSignCertFile);
							CVCObject parsedObject = CvcPrintCommand.getCVCObject(authSignCertFile);
							authCert = (CVCertificate)parsedObject;
							String c = authCert.getCertificateBody().getHolderReference().getCountry();
							String m = authCert.getCertificateBody().getHolderReference().getMnemonic();
							String s = authCert.getCertificateBody().getHolderReference().getSequence();
							authCaRef = new CAReferenceField(c, m, s);
						}
						CVCAuthenticatedRequest authRequest = CertificateGenerator.createAuthenticatedRequest(request, authKeyPair, signatureAlg, authCaRef);
						// Test to verify it yourself first
						if (authCert != null) {
							getPrintStream().println("Verifying the request before sending it...");
							PublicKey pk = KeyTools.getECPublicKeyWithParams(authCert.getCertificateBody().getPublicKey(), keySpec);
							authRequest.verify(pk);							
						}
						der = authRequest.getDEREncoded();						
					}
					cvcreq = new String(Base64.encode(der));
					// Print the generated request to file
					FileOutputStream fos = new FileOutputStream(basefilename+".cvrqst");
					fos.write(der);
					fos.close();					
					getPrintStream().println("Wrote binary request to: "+basefilename+".cvrqst");
					fos = new FileOutputStream(basefilename+".pkcs8");
					fos.write(keyPair.getPrivate().getEncoded());
					fos.close();					
					getPrintStream().println("Wrote private key in "+keyPair.getPrivate().getFormat()+" format to to: "+basefilename+".pkcs8");
				} else {
					// Read request from file
					getPrintStream().println("Reading request from filename: "+basefilename+".cvrqst");
					byte[] der = FileTools.readFiletoBuffer(basefilename+".cvrqst");
					cvcreq = new String(Base64.encode(der));
				}
				
				// Edit a user, creating it if it does not exist
				getEjbcaRAWS().editUser(userdata);
				// Use the request and request a certificate
				List<Certificate> resp = getEjbcaRAWS().cvcRequest(userdata.getUsername(), userdata.getPassword(), cvcreq);

				getPrintStream().println("CVC request submitted for user '"+userdata.getUsername()+"'.");
				getPrintStream().println();              

				// Handle the response
				Certificate cert = resp.get(0);
				byte[] b64cert = cert.getCertificateData();
				CVCObject parsedObject = CertificateParser.parseCertificate(Base64.decode(b64cert));
				CVCertificate cvcert = (CVCertificate)parsedObject;
				FileOutputStream fos = new FileOutputStream(basefilename+".cvcert");
				fos.write(cvcert.getDEREncoded());
				fos.close();
				getPrintStream().println("Wrote binary certificate to: "+basefilename+".cvcert");
				getPrintStream().println("You can look at the certificate with the command cvcwscli.sh cvcprint "+basefilename+".cvcert");
			}catch(AuthorizationDeniedException_Exception e){
				getPrintStream().println("Error : " + e.getMessage());
			}catch(UserDoesntFullfillEndEntityProfile_Exception e){
				getPrintStream().println("Error : Given userdata doesn't fullfill end entity profile. : " +  e.getMessage());
			}

		} catch (Exception e) {
			throw new ErrorAdminCommandException(e);
		}
	}

	protected void usage() {
		getPrintStream().println("Command used to make a CVC request. If user does not exist a new will be created and if user exist will the data be overwritten.");
		getPrintStream().println("Usage : cvcrequest <username> <password> <subjectdn> <sequence> <caname> <signatureAlg> <keyspec (1024/2048/curve)> <endentityprofilename> <certificateprofilename> <genreq=true|false> <basefilename> [<auth-sign-key>] [<auth-sign-cert>]\n\n");
		getPrintStream().println("SignatureAlg can be SHA1WithRSA, SHA256WithRSA, SHA256WithRSAAndMGF1, SHA1WithECDSA, SHA224WithECDSA, SHA256WithECDSA");
		getPrintStream().println("Keyspec is 1024, 2048 etc for RSA keys and the name of a named curve for ECDSA, see User Guide for supported curves.");
		getPrintStream().println("DN is of form \"C=SE, CN=ISTEST2\", where SE is the country and ISTEST2 the mnemonic.");
		getPrintStream().println("Sequence is a sequence number for the public key, recomended form 00001 etc. If 'null' a random 5 number sequence will be generated.");
		getPrintStream().println("If genreq is true a new request is generated and the generated request is written to <basefilename>.cvrqst, and the private key to <basefilename>.pkcs8.");
		getPrintStream().println("If genreq is false a request is read from <reqfilename>.cvrqst and sent to the CA, the sequence from the command line is ignored.");
		getPrintStream().println("The issued certificate is written to <basefilename>.cvcert\n");
		getPrintStream().println("auth-sign-key is optional and if given the CVC request is signed by this key to create an authenticated CVC request.");
		getPrintStream().println("auth-sign-cert is optional and if given the caRef of the authenticated CVC request is taken from this CVC certificate.");
	}


}
