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
 
package se.anatom.ejbca.webdist.cainterface;

import java.io.IOException;
import java.io.PrintStream;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import se.anatom.ejbca.apply.RequestHelper;
import se.anatom.ejbca.ca.sign.ISignSessionLocal;
import se.anatom.ejbca.ca.sign.ISignSessionLocalHome;
import se.anatom.ejbca.log.Admin;
import se.anatom.ejbca.util.Base64;
import se.anatom.ejbca.util.ServiceLocator;
import se.anatom.ejbca.webdist.webconfiguration.EjbcaWebBean;

/**
 * Servlet used to distribute CA certificates <br>
 *
 * cacert - returns ca certificate in PEM-format
 * nscacert - returns ca certificate for Netscape/Mozilla
 * iecacert - returns ca certificate for Internet Explorer
 *
 * cacert, nscacert and iecacert also takes optional parameter level=<int 1,2,...>, where the level is
 * which ca certificate in a hierachy should be returned. 0=root (default), 1=sub to root etc.
 *
 * @version $Id: CACertServlet.java,v 1.25 2005-02-13 18:38:24 anatom Exp $
 *
 * @web.servlet name = "CACert"
 *              display-name = "CACertServlet"
 *              description="Returns the specified CA certificate"
 *              load-on-startup = "99"
 *
 * @web.servlet-mapping url-pattern = "/ca/cacert"
 *
 * @web.ejb-local-ref
 *  name="ejb/RSASignSessionLocal"
 *  type="Session"
 *  link="RSASignSession"
 *  home="se.anatom.ejbca.ca.sign.ISignSessionLocalHome"
 *  local="se.anatom.ejbca.ca.sign.ISignSessionLocal"
 *
 */
public class CACertServlet extends HttpServlet {

    private static final Logger log = Logger.getLogger(CACertServlet.class);

    private static final String COMMAND_PROPERTY_NAME = "cmd";
    private static final String COMMAND_NSCACERT = "nscacert";
    private static final String COMMAND_IECACERT = "iecacert";
    private static final String COMMAND_CACERT = "cacert";

    private static final String LEVEL_PROPERTY = "level";
    private static final String ISSUER_PROPERTY = "issuer";

    private ISignSessionLocalHome signhome = null;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            signhome = (ISignSessionLocalHome) ServiceLocator.getInstance().getLocalHome(ISignSessionLocalHome.COMP_NAME);
        } catch( Exception e ) {
            throw new ServletException(e);
        }
    }
    
    public void doPost(HttpServletRequest req, HttpServletResponse res)
        throws IOException, ServletException {
        log.debug(">doPost()");
        doGet(req, res);
        log.debug("<doPost()");
    } //doPost

    public void doGet(HttpServletRequest req,  HttpServletResponse res) throws java.io.IOException, ServletException {
        log.debug(">doGet()");
        // Check if authorized
        EjbcaWebBean ejbcawebbean= (se.anatom.ejbca.webdist.webconfiguration.EjbcaWebBean)
                                   req.getSession().getAttribute("ejbcawebbean");
        if ( ejbcawebbean == null ){
          try {
            ejbcawebbean = (se.anatom.ejbca.webdist.webconfiguration.EjbcaWebBean) java.beans.Beans.instantiate(this.getClass().getClassLoader(), "se.anatom.ejbca.webdist.webconfiguration.EjbcaWebBean");
           } catch (ClassNotFoundException exc) {
               throw new ServletException(exc.getMessage());
           }catch (Exception exc) {
               throw new ServletException (" Cannot create bean of class "+"se.anatom.ejbca.webdist.webconfiguration.EjbcaWebBean", exc);
           }
           req.getSession().setAttribute("ejbcawebbean", ejbcawebbean);
        }

        try{
          ejbcawebbean.initialize(req,"/ca_functionality/basic_functions");
        } catch(Exception e){
           throw new java.io.IOException("Authorization Denied");
        }
        
        String issuerdn = req.getParameter(ISSUER_PROPERTY);        
        

        String command;
        // Keep this for logging.
        log.debug("Got request from "+req.getRemoteAddr());
        command = req.getParameter(COMMAND_PROPERTY_NAME);
        if (command == null)
            command = "";
        if ((command.equalsIgnoreCase(COMMAND_NSCACERT) || command.equalsIgnoreCase(COMMAND_IECACERT) || command.equalsIgnoreCase(COMMAND_CACERT)) && issuerdn != null ) {
            String lev = req.getParameter(LEVEL_PROPERTY);
            int level = 0;
            if (lev != null)
                level = Integer.parseInt(lev);
            // Root CA is level 0, next below root level 1 etc etc
            try {
                ISignSessionLocal ss = signhome.create();
                Admin admin = new Admin(((X509Certificate[]) req.getAttribute( "javax.servlet.request.X509Certificate" ))[0]);
                Certificate[] chain = (Certificate[]) ss.getCertificateChain(admin, issuerdn.hashCode()).toArray(new Certificate[0]);
                                                            
                // chain.length-1 is last cert in chain (root CA)
                if ( (chain.length-1-level) < 0 ) {
                    PrintStream ps = new PrintStream(res.getOutputStream());
                    ps.println("No CA certificate of level "+level+"exist.");
                    log.error("No CA certificate of level "+level+"exist.");
                    return;
                }
                X509Certificate cacert = (X509Certificate)chain[level];
                byte[] enccert = cacert.getEncoded();
                if (res.containsHeader("Pragma")) {
                    log.debug("Removing Pragma header to avoid caching issues in IE");
                    res.setHeader("Pragma",null);
                }
                if (res.containsHeader("Cache-Control")) {
                    log.debug("Removing Cache-Control header to avoid caching issues in IE");
                    res.setHeader("Cache-Control",null);
                }
                if (command.equalsIgnoreCase(COMMAND_NSCACERT)) {
                    res.setContentType("application/x-x509-ca-cert");
                    res.setContentLength(enccert.length);
                    res.getOutputStream().write(enccert);
                    log.debug("Sent CA cert to NS client, len="+enccert.length+".");
                } else if (command.equalsIgnoreCase(COMMAND_IECACERT)) {
                    res.setHeader("Content-disposition", "attachment; filename=ca.crt");
                    res.setContentType("application/octet-stream");
                    res.setContentLength(enccert.length);
                    res.getOutputStream().write(enccert);
                    log.debug("Sent CA cert to IE client, len="+enccert.length+".");
                } else if (command.equalsIgnoreCase(COMMAND_CACERT)) {
                    byte[] b64cert = Base64.encode(enccert);
                    String out = RequestHelper.BEGIN_CERTIFICATE_WITH_NL;                   
                    out += new String(b64cert);
                    out += RequestHelper.END_CERTIFICATE_WITH_NL;
                    res.setHeader("Content-disposition", "attachment; filename=ca.pem");
                    res.setContentType("application/octet-stream");
                    res.setContentLength(out.length());
                    res.getOutputStream().write(out.getBytes());
                    log.debug("Sent CA cert to client, len="+out.length()+".");
                } else {
                    res.setContentType("text/plain");
                    res.getOutputStream().println("Commands="+COMMAND_NSCACERT+" || "+COMMAND_IECACERT+" || "+COMMAND_CACERT);
                    return;
                }
            } catch (Exception e) {
                PrintStream ps = new PrintStream(res.getOutputStream());
                e.printStackTrace(ps);
                res.sendError(HttpServletResponse.SC_NOT_FOUND, "Error getting CA certificates.");
                log.error("Error getting CA certificates.");
                log.error(e);
                return;
            }
        }
        else {
            res.setContentType("text/plain");
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Commands=lastcert | listcerts | crl | revoked");
            return;
        }

    } // doGet

}
