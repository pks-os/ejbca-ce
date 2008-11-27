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
 
package org.ejbca.ui.web.pub;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Enumeration;

import javax.ejb.EJBException;
import javax.ejb.ObjectNotFoundException;
import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.ejbca.core.ejb.ServiceLocator;
import org.ejbca.core.ejb.ca.sign.ISignSessionLocal;
import org.ejbca.core.ejb.ca.sign.ISignSessionLocalHome;
import org.ejbca.core.ejb.ca.store.ICertificateStoreSessionHome;
import org.ejbca.core.ejb.ca.store.ICertificateStoreSessionRemote;
import org.ejbca.core.ejb.ra.IUserAdminSessionHome;
import org.ejbca.core.ejb.ra.IUserAdminSessionRemote;
import org.ejbca.core.ejb.ra.raadmin.IRaAdminSessionHome;
import org.ejbca.core.ejb.ra.raadmin.IRaAdminSessionRemote;
import org.ejbca.core.model.InternalResources;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.ca.AuthLoginException;
import org.ejbca.core.model.ca.AuthStatusException;
import org.ejbca.core.model.ca.SignRequestException;
import org.ejbca.core.model.ca.SignRequestSignatureException;
import org.ejbca.core.model.ca.catoken.CATokenConstants;
import org.ejbca.core.model.ca.catoken.CATokenOfflineException;
import org.ejbca.core.model.log.Admin;
import org.ejbca.core.model.ra.UserDataConstants;
import org.ejbca.core.model.ra.UserDataVO;
import org.ejbca.core.model.ra.raadmin.EndEntityProfile;
import org.ejbca.core.model.util.GenerateToken;
import org.ejbca.ui.web.RequestHelper;
import org.ejbca.util.Base64;
import org.ejbca.util.CertTools;
import org.ejbca.util.keystore.KeyTools;




/**
 * Servlet used to install a private key with a corresponding certificate in a browser. A new
 * certificate is installed in the browser in following steps:<br>
 * 1. The key pair is generated by the browser. <br>
 * 2. The public part is sent to the servlet in a POST together with user info ("pkcs10|keygen",
 * "inst", "user", "password"). For internet explorer the public key is sent as a PKCS10
 * certificate request. <br>
 * 3. The new certificate is created by calling the RSASignSession session bean. <br>
 * 4. A page containing the new certificate and a script that installs it is returned to the
 * browser. <br>
 * 
 * <p></p>
 * 
 * <p>
 * The following initiation parameters are needed by this servlet: <br>
 * "responseTemplate" file that defines the response to the user (IE). It should have one line
 * with the text "cert =". This line is replaced with the new certificate. "keyStorePass".
 * Password needed to load the key-store. If this parameter is none existing it is assumed that no
 * password is needed. The path could be absolute or relative.<br>
 * </p>
 *
 * @author Original code by Lars Silv?n
 * @version $Id$
 */
public class CertReqServlet extends HttpServlet {
    private static final Logger log = Logger.getLogger(CertReqServlet.class);
    /** Internal localization of logs and errors */
    private static final InternalResources intres = InternalResources.getInstance();

    private byte[] bagattributes = "Bag Attributes\n".getBytes();
    private byte[] friendlyname = "    friendlyName: ".getBytes();
    private byte[] subject = "subject=/".getBytes();
    private byte[] issuer = "issuer=/".getBytes();
    private byte[] beginCertificate = "-----BEGIN CERTIFICATE-----".getBytes();
    private byte[] endCertificate = "-----END CERTIFICATE-----".getBytes();
    private byte[] beginPrivateKey = "-----BEGIN PRIVATE KEY-----".getBytes();
    private byte[] endPrivateKey = "-----END PRIVATE KEY-----".getBytes();
    private byte[] NL = "\n".getBytes();

    private IUserAdminSessionHome useradminhome = null;
    private ICertificateStoreSessionHome storehome = null;
    private IRaAdminSessionHome raadminhome = null;

    private ISignSessionLocal signsession = null;

    private synchronized ISignSessionLocal getSignSession(){
    	if(signsession == null){	
    		try {
    			ISignSessionLocalHome signhome = (ISignSessionLocalHome)ServiceLocator.getInstance().getLocalHome(ISignSessionLocalHome.COMP_NAME);
    			signsession = signhome.create();
    		}catch(Exception e){
    			throw new EJBException(e);      	  	    	  	
    		}
    	}
    	return signsession;
    }
    
    /**
     * Servlet init
     *
     * @param config servlet configuration
     *
     * @throws ServletException on error
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        try {
            // Install BouncyCastle provider
            CertTools.installBCProvider();

            // Get EJB context and home interfaces
            InitialContext ctx = new InitialContext();
            useradminhome = (IUserAdminSessionHome) PortableRemoteObject.narrow(
                             ctx.lookup(IUserAdminSessionHome.JNDI_NAME), IUserAdminSessionHome.class );
            raadminhome   = (IRaAdminSessionHome) PortableRemoteObject.narrow(
                             ctx.lookup(IRaAdminSessionHome.JNDI_NAME), IRaAdminSessionHome.class );            
            storehome   = (ICertificateStoreSessionHome) PortableRemoteObject.narrow(
                    ctx.lookup(ICertificateStoreSessionHome.JNDI_NAME), ICertificateStoreSessionHome.class );            
        } catch( Exception e ) {
            throw new ServletException(e);
        }
    }

    /**
     * Handles HTTP POST
     *
     * @param request servlet request
     * @param response servlet response
     *
     * @throws IOException input/output error
     * @throws ServletException on error
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {
        ServletDebug debug = new ServletDebug(request, response);
        boolean usekeyrecovery = false;

        RequestHelper.setDefaultCharacterEncoding(request);
        String iErrorMessage = null;
        try {
            String username = request.getParameter("user");
            String password = request.getParameter("password");
            String keylengthstring = request.getParameter("keylength");
            String keyalgstring = request.getParameter("keyalg");
            String openvpn = request.getParameter("openvpn");
            String certprofile = request.getParameter("certprofile");
			String keylength = "1024";
			String keyalg = CATokenConstants.KEYALGORITHM_RSA;
			
            int resulttype = 0;
            if(request.getParameter("resulttype") != null)
              resulttype = Integer.parseInt(request.getParameter("resulttype")); // Indicates if certificate or PKCS7 should be returned on manual PKCS10 request.
            

            String classid = "clsid:127698e4-e730-4e5c-a2b1-21490a70c8a1\" CODEBASE=\"/CertControl/xenroll.cab#Version=5,131,3659,0";

            if ((request.getParameter("classid") != null) &&
                    !request.getParameter("classid").equals("")) {
                classid = request.getParameter("classid");
            }

            if (keylengthstring != null) {
                keylength = keylengthstring;
            }
            if (keyalgstring != null) {
                keyalg = keyalgstring;
            }

            Admin administrator = new Admin(Admin.TYPE_PUBLIC_WEB_USER, request.getRemoteAddr());

            IUserAdminSessionRemote adminsession = useradminhome.create();
            ICertificateStoreSessionRemote storesession = storehome.create();
            IRaAdminSessionRemote raadminsession = raadminhome.create();            
            ISignSessionLocal signsession = getSignSession();
            RequestHelper helper = new RequestHelper(administrator, debug);

            log.info(intres.getLocalizedMessage("certreq.receivedcertreq", username, request.getRemoteAddr()));
            debug.print("Username: " + username);

            // Check user
            int tokentype = SecConst.TOKEN_SOFT_BROWSERGEN;

            usekeyrecovery = (raadminsession.loadGlobalConfiguration(administrator)).getEnableKeyRecovery();

            UserDataVO data = adminsession.findUser(administrator, username);

            if (data == null) {
                throw new ObjectNotFoundException();
            }

            boolean savekeys = data.getKeyRecoverable() && usekeyrecovery &&  (data.getStatus() != UserDataConstants.STATUS_KEYRECOVERY);
            boolean loadkeys = (data.getStatus() == UserDataConstants.STATUS_KEYRECOVERY) && usekeyrecovery;

            int endEntityProfileId = data.getEndEntityProfileId();
            int certificateProfileId = data.getCertificateProfileId();
            EndEntityProfile endEntityProfile = raadminsession.getEndEntityProfile(administrator, endEntityProfileId);
            boolean reusecertificate = endEntityProfile.getReUseKeyRevoceredCertificate();
            // Set a new certificate profile, if we have requested one specific
            if (StringUtils.isNotEmpty(certprofile)) {
            	boolean clearpwd = StringUtils.isNotEmpty(data.getPassword());
            	int id = storesession.getCertificateProfileId(administrator, certprofile);
            	// Change the value if there exists a certprofile with the requested name, and it is not the same as 
            	// the one already registered to be used by default
            	if ( (id > 0) && (id != certificateProfileId) ) {
            		// Check if it is in allowed profiles in the entity profile
            		Collection c = endEntityProfile.getAvailableCertificateProfileIds();
            		if (c.contains(String.valueOf(id))) {
                    	data.setCertificateProfileId(id);
                    	// This admin can be the public web user, which may not be allowed to change status,
                    	// this is a bit ugly, but what can a man do...
                    	Admin tempadmin = new Admin(Admin.TYPE_INTERNALUSER);
                    	adminsession.changeUser(tempadmin, data, clearpwd);            		            			
            		} else {
            			String defaultCertificateProfileName = storesession.getCertificateProfileName(administrator, certificateProfileId);
                		log.error(intres.getLocalizedMessage("certreq.badcertprofile", certprofile, defaultCertificateProfileName));
            		}
            	} else {
        			String defaultCertificateProfileName = storesession.getCertificateProfileName(administrator, certificateProfileId);
            		log.error(intres.getLocalizedMessage("certreq.nosuchcertprofile", certprofile, defaultCertificateProfileName));
            	}
            }

            // get users Token Type.
            tokentype = data.getTokenType();
            GenerateToken tgen = new GenerateToken(true);
            if(tokentype == SecConst.TOKEN_SOFT_P12){
              KeyStore ks = tgen.generateOrKeyRecoverToken(administrator, username, password, data.getCAId(), keylength, keyalg, false, loadkeys, savekeys, reusecertificate, endEntityProfileId);
              if (StringUtils.equals(openvpn, "on")) {            	  
                  sendOpenVPNToken(ks, username, password, response);
              } else {
            	  sendP12Token(ks, username, password, response);
              }
            }
            if(tokentype == SecConst.TOKEN_SOFT_JKS){
              KeyStore ks = tgen.generateOrKeyRecoverToken(administrator, username, password, data.getCAId(), keylength, keyalg, true, loadkeys, savekeys, reusecertificate, endEntityProfileId);
              sendJKSToken(ks, username, password, response);
            }
            if(tokentype == SecConst.TOKEN_SOFT_PEM){
              KeyStore ks = tgen.generateOrKeyRecoverToken(administrator, username, password, data.getCAId(), keylength, keyalg, false, loadkeys, savekeys, reusecertificate, endEntityProfileId);
              sendPEMTokens(ks, username, password, response);
            }
            if(tokentype == SecConst.TOKEN_SOFT_BROWSERGEN){

              // first check if it is a netscape request,
              if (request.getParameter("keygen") != null) {
                  byte[] reqBytes=request.getParameter("keygen").getBytes();
                  if (reqBytes != null) {
                      log.debug("Received NS request: "+new String(reqBytes));
                      byte[] certs = helper.nsCertRequest(signsession, reqBytes, username, password);
                      RequestHelper.sendNewCertToNSClient(certs, response);
                  }
              } else if ( request.getParameter("iidPkcs10") != null && !request.getParameter("iidPkcs10").equals("")) {
                  // NetID iid?
                  byte[] reqBytes=request.getParameter("iidPkcs10").getBytes();
                  if (reqBytes != null) {
                      log.debug("Received iidPkcs10 request: "+new String(reqBytes));
                      byte[] b64cert=helper.pkcs10CertRequest(signsession, reqBytes, username, password, RequestHelper.ENCODED_CERTIFICATE, false);
                      response.setContentType("text/html");
                      RequestHelper.sendNewCertToIidClient(b64cert, request, response.getOutputStream(), getServletContext(), getInitParameter("responseIidTemplate"),classid);
                  }
              } else if ( (request.getParameter("pkcs10") != null) || (request.getParameter("PKCS10") != null) ) {
                  // if not netscape, check if it's IE
                  byte[] reqBytes=request.getParameter("pkcs10").getBytes();
                  if (reqBytes == null)
                      reqBytes=request.getParameter("PKCS10").getBytes();
                  if (reqBytes != null) {
                      log.debug("Received IE request: "+new String(reqBytes));
                      byte[] b64cert=helper.pkcs10CertRequest(signsession, reqBytes, username, password, RequestHelper.ENCODED_PKCS7);
                      debug.ieCertFix(b64cert);
                      RequestHelper.sendNewCertToIEClient(b64cert, response.getOutputStream(), getServletContext(), getInitParameter("responseTemplate"),classid);
                  }
              } else if (request.getParameter("pkcs10req") != null && resulttype != 0) {
                  // if not IE, check if it's manual request
                  byte[] reqBytes=request.getParameter("pkcs10req").getBytes();
                  if (reqBytes != null) {
                      log.debug("Received PKCS10 request: "+new String(reqBytes));
                      byte[] b64cert=helper.pkcs10CertRequest(signsession, reqBytes, username, password, resulttype);
                      if(resulttype == RequestHelper.ENCODED_PKCS7)  
                        RequestHelper.sendNewB64Cert(b64cert, response, RequestHelper.BEGIN_PKCS7_WITH_NL, RequestHelper.END_PKCS7_WITH_NL);
                      if(resulttype == RequestHelper.ENCODED_CERTIFICATE)
                        RequestHelper.sendNewB64Cert(b64cert, response, RequestHelper.BEGIN_CERTIFICATE_WITH_NL, RequestHelper.END_CERTIFICATE_WITH_NL);
                  }
              } else if (request.getParameter("cvcreq") != null && resulttype != 0) {
                  // It's a CVC certificate request (EAC ePassports)
                  byte[] reqBytes=request.getParameter("cvcreq").getBytes();
                  if (reqBytes != null) {
                      log.debug("Received CVC request: "+new String(reqBytes));
                      byte[] b64cert=helper.cvcCertRequest(signsession, reqBytes, username, password);
                      if(resulttype == RequestHelper.BINARY_CERTIFICATE)  
                        RequestHelper.sendBinaryBytes(Base64.decode(b64cert), response, "application/octet-stream", username+".cvcert");
                      if(resulttype == RequestHelper.ENCODED_CERTIFICATE)
                        RequestHelper.sendNewB64Cert(b64cert, response, RequestHelper.BEGIN_CERTIFICATE_WITH_NL, RequestHelper.END_CERTIFICATE_WITH_NL);
                  }
              }
            }
        } catch (ObjectNotFoundException oe) {
        	iErrorMessage = intres.getLocalizedMessage("certreq.nosuchusername");
        } catch (AuthStatusException ase) {
        	iErrorMessage = intres.getLocalizedMessage("certreq.wrongstatus");
        } catch (AuthLoginException ale) {
        	iErrorMessage = intres.getLocalizedMessage("certreq.wrongpassword");
        } catch (SignRequestException re) {
        	iErrorMessage = intres.getLocalizedMessage("certreq.invalidreq");
        } catch (SignRequestSignatureException se) {
        	String iMsg = intres.getLocalizedMessage("certreq.invalidsign");
            log.error(iMsg, se);
            debug.printMessage(iMsg);
            debug.printDebugInfo();
            return;
        } catch (ArrayIndexOutOfBoundsException ae) {
        	iErrorMessage = intres.getLocalizedMessage("certreq.invalidreq");
        } catch (org.ejbca.core.model.ca.IllegalKeyException e) {
        	iErrorMessage = intres.getLocalizedMessage("certreq.invalidkey", e.getMessage());
        } catch (Exception e) {
        	Throwable e1 = e.getCause();
        	if (e1 instanceof CATokenOfflineException) {
            	String iMsg = intres.getLocalizedMessage("certreq.catokenoffline", e1.getMessage());
	            // this is already logged as an error, so no need to log it one more time
	            debug.printMessage(iMsg);
	            debug.printDebugInfo();
	            return;				
			} else {
            	String iMsg = intres.getLocalizedMessage("certreq.errorgeneral", e1.getMessage());
	            log.debug(iMsg, e);
            	iMsg = intres.getLocalizedMessage("certreq.parameters", e1.getMessage());
	            debug.print(iMsg + ":\n");
	            Enumeration paramNames = request.getParameterNames();
	            while (paramNames.hasMoreElements()) {
	                String name = paramNames.nextElement().toString();
	                String parameter = request.getParameter(name);
	                if (!StringUtils.equals(name, "password")) {
	                    debug.print(name + ": '" + parameter + "'\n");                	
	                } else {
	                	debug.print(name + ": <hidden>\n");
	                }
	            }
	            debug.takeCareOfException(e);
	            debug.printDebugInfo();
			}
        }
        if (iErrorMessage != null) {
            log.debug(iErrorMessage);
            debug.printMessage(iErrorMessage);
            debug.printDebugInfo();
            return;
        }
    }

    //doPost

    /**
     * Handles HTTP GET
     *
     * @param request servlet request
     * @param response servlet response
     *
     * @throws IOException input/output error
     * @throws ServletException on error
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {
        log.debug(">doGet()");
        response.setHeader("Allow", "POST");

        ServletDebug debug = new ServletDebug(request, response);
    	String iMsg = intres.getLocalizedMessage("certreq.postonly");
        debug.print(iMsg);
        debug.printDebugInfo();
        log.debug("<doGet()");
    }

    // doGet
    /**
     * method to create an install package for OpenVPN including keys and send to user.
     * Contributed by: Jon Bendtsen, jon.bendtsen(at)laerdal.dk
     */
    private void sendOpenVPNToken(KeyStore ks, String username, String kspassword, HttpServletResponse out) throws Exception {
    	ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    	ks.store(buffer, kspassword.toCharArray());
    	
    	File fout = new File("/usr/local/tmp/" + username + ".p12");
    	FileOutputStream certfile = new FileOutputStream(fout);
    	
    	Enumeration en = ks.aliases();
    	String alias = (String)en.nextElement();
    	// Then get the certificates
    	Certificate[] certs = KeyTools.getCertChain(ks, alias);
    	// The first  one (certs[0]) is the users cert and the last
    	// one (certs [certs.lenght-1]) is the CA-cert
    	X509Certificate x509cert = (X509Certificate) certs[0];
    	String IssuerDN = x509cert.getIssuerDN().toString();
    	String SubjectDN = x509cert.getSubjectDN().toString();
    	
    	// export the users certificate to file
    	buffer.writeTo(certfile);
    	buffer.flush();
    	buffer.close();
    	certfile.close();
    	
    	// run shell script, which will also remove the created files
    	// parameters are the username, IssuerDN and SubjectDN
    	// IssuerDN and SubjectDN will be used to select the right
    	// openvpn configuration file
    	// they have to be written to stdin of the script to support
    	// spaces in the username, IssuerDN or SubjectDN
    	Runtime rt = Runtime.getRuntime();
    	if (rt==null) {
    		log.error(intres.getLocalizedMessage("certreq.ovpntnoruntime"));
    	} else {
    		final String script = "/usr/local/ejbca/bin/mk_openvpn_" + "windows_installer.sh";
    		Process p = rt.exec(script);
    		if (p==null) {
        		log.error(intres.getLocalizedMessage("certreq.ovpntfailedexec", script));
    		} else {
    			OutputStream pstdin = p.getOutputStream();
    			PrintStream stdoutp = new PrintStream(pstdin);
    			stdoutp.println(username);
    			stdoutp.println(IssuerDN);
    			stdoutp.println(SubjectDN);
    			stdoutp.flush();
    			stdoutp.close();
    			pstdin.close();
    			int exitVal = p.waitFor();
    			if (exitVal != 0) {
            		log.error(intres.getLocalizedMessage("certreq.ovpntexiterror", exitVal));
    			} else {
            		log.debug(intres.getLocalizedMessage("certreq.ovpntexiterror", exitVal));
    			}
    		}
    	}
    	
    	// we ought to check if the script was okay or not, but in a little
    	// while we will look for the openvpn-gui-install-$username.exe
    	// and fail there if the script failed. Also, one could question
    	// what to do if it did fail, serve the user the certificate?
    	
    	// sending the OpenVPN windows installer
    	String filename = "openvpn-gui-install-" + username + ".exe";
    	File fin =  new File("/usr/local/tmp/" + filename);
    	FileInputStream vpnfile = new FileInputStream(fin);
    	
    	out.setContentType("application/x-msdos-program");
    	out.setHeader("Content-disposition", "filename=" + filename);
		out.setContentLength( new Long(fin.length()).intValue() );
		OutputStream os = out.getOutputStream(); 
    	byte[] buf = new byte[4096];
    	int offset = 0;
    	int bytes = 0;
    	while ( (bytes=vpnfile.read(buf)) != -1 ) {
    		os.write(buf,0,bytes);
    		offset += bytes;
    	}
    	vpnfile.close();
    	// delete OpenVPN windows installer, the script will delete cert.
    	fin.delete();
    	out.flushBuffer();    	
    } // sendOpenVPNToken
    
    private void sendP12Token(KeyStore ks, String username, String kspassword,
        HttpServletResponse out) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ks.store(buffer, kspassword.toCharArray());

        out.setContentType("application/x-pkcs12");
        out.setHeader("Content-disposition", "filename=" + username + ".p12");
        out.setContentLength(buffer.size());
        buffer.writeTo(out.getOutputStream());
        out.flushBuffer();
        buffer.close();
    }

    private void sendJKSToken(KeyStore ks, String username, String kspassword,
        HttpServletResponse out) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ks.store(buffer, kspassword.toCharArray());

        out.setContentType("application/octet-stream");
        out.setHeader("Content-disposition", "filename=" + username + ".jks");
        out.setContentLength(buffer.size());
        buffer.writeTo(out.getOutputStream());
        out.flushBuffer();
        buffer.close();
    }

    private void sendPEMTokens(KeyStore ks, String username, String kspassword,
        HttpServletResponse out) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        String alias = "";

        // Find the key private key entry in the keystore
        Enumeration e = ks.aliases();
        Object o = null;
        PrivateKey serverPrivKey = null;

        while (e.hasMoreElements()) {
            o = e.nextElement();

            if (o instanceof String) {
                if ((ks.isKeyEntry((String) o)) &&
                        ((serverPrivKey = (PrivateKey) ks.getKey((String) o,
                                kspassword.toCharArray())) != null)) {
                    alias = (String) o;

                    break;
                }
            }
        }

        byte[] privKeyEncoded = "".getBytes();

        if (serverPrivKey != null) {
            privKeyEncoded = serverPrivKey.getEncoded();
        }

        //Certificate chain[] = ks.getCertificateChain((String) o);
        Certificate[] chain = KeyTools.getCertChain(ks, (String) o);
        X509Certificate userX509Certificate = (X509Certificate) chain[0];

        byte[] output = userX509Certificate.getEncoded();
        String sn = CertTools.getSubjectDN(userX509Certificate);

        String subjectdnpem = sn.replace(',', '/');
        String issuerdnpem = CertTools.getIssuerDN(userX509Certificate).replace(',', '/');

        buffer.write(bagattributes);
        buffer.write(friendlyname);
        buffer.write(alias.getBytes());
        buffer.write(NL);
        buffer.write(beginPrivateKey);
        buffer.write(NL);

        byte[] privKey = Base64.encode(privKeyEncoded);
        buffer.write(privKey);
        buffer.write(NL);
        buffer.write(endPrivateKey);
        buffer.write(NL);
        buffer.write(bagattributes);
        buffer.write(friendlyname);
        buffer.write(alias.getBytes());
        buffer.write(NL);
        buffer.write(subject);
        buffer.write(subjectdnpem.getBytes());
        buffer.write(NL);
        buffer.write(issuer);
        buffer.write(issuerdnpem.getBytes());
        buffer.write(NL);
        buffer.write(beginCertificate);
        buffer.write(NL);

        byte[] userCertB64 = Base64.encode(output);
        buffer.write(userCertB64);
        buffer.write(NL);
        buffer.write(endCertificate);
        buffer.write(NL);

        if (CertTools.isSelfSigned(userX509Certificate)) {
        } else {
            for (int num = 1; num < chain.length; num++) {
                X509Certificate tmpX509Cert = (X509Certificate) chain[num];
                sn = CertTools.getSubjectDN(tmpX509Cert);

                String cn = CertTools.getPartFromDN(sn, "CN");
                if (StringUtils.isEmpty(cn)) {
                	cn="Unknown";
                }

                subjectdnpem = sn.replace(',', '/');
                issuerdnpem = CertTools.getIssuerDN(tmpX509Cert).replace(',', '/');

                buffer.write(bagattributes);
                buffer.write(friendlyname);
                buffer.write(cn.getBytes());
                buffer.write(NL);
                buffer.write(subject);
                buffer.write(subjectdnpem.getBytes());
                buffer.write(NL);
                buffer.write(issuer);
                buffer.write(issuerdnpem.getBytes());
                buffer.write(NL);

                byte[] tmpOutput = tmpX509Cert.getEncoded();
                buffer.write(beginCertificate);
                buffer.write(NL);

                byte[] tmpCACertB64 = Base64.encode(tmpOutput);
                buffer.write(tmpCACertB64);
                buffer.write(NL);
                buffer.write(endCertificate);
                buffer.write(NL);
            }
        }

        out.setContentType("application/octet-stream");
        out.setHeader("Content-disposition", " attachment; filename=" + username + ".pem");
        buffer.writeTo(out.getOutputStream());
        out.flushBuffer();
        buffer.close();
    }

}


// CertReqServlet
