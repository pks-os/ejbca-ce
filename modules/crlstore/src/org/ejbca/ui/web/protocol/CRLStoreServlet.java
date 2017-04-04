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

package org.ejbca.ui.web.protocol;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.cert.X509Certificate;

import javax.ejb.EJB;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.cesecore.certificates.certificate.HashID;
import org.cesecore.certificates.crl.CrlStoreSessionLocal;
import org.cesecore.util.StringTools;
import org.ejbca.core.protocol.crlstore.CRLCache;
import org.ejbca.util.HTMLTools;

/** 
 * Servlet implementing server side of the CRL Store.
 * For a detailed description see RFC 4387.
 * Addition to RFC 4387 is the ability to specify delta CRL with the parameter "delta="
 * Addition to RFC 4387 is the ability to specify download of a specific CRL by crlNumber with the parameter "crlnumber=<number>"
 * 
 * 
 * @version  $Id$
 */
public class CRLStoreServlet extends StoreServletBase {

	private static final long serialVersionUID = 1L;

	@EJB
	private CrlStoreSessionLocal crlSession;
	
	private CRLCache crlCache;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		this.crlCache = CRLCache.getInstance(crlSession, certCache);		
	}

	@Override
	public void sHash(String iHash, HttpServletResponse resp, HttpServletRequest req) throws IOException, ServletException {
		// do nothing for CRLs
	}

	@Override
	public void iHash(String iHash, HttpServletResponse resp, HttpServletRequest req) throws IOException, ServletException {
		returnCrl( this.crlCache.findByIssuerDN(HashID.getFromB64(iHash), isDelta(req), getCrlNumber(req, resp)), resp, iHash, isDelta(req) );		
	}

	@Override
	public void sKIDHash(String sKIDHash, HttpServletResponse resp, HttpServletRequest req) throws IOException, ServletException {
		sKIDHash( sKIDHash, resp, req, sKIDHash);
	}

	@Override
	public void sKIDHash(String sKIDHash, HttpServletResponse resp, HttpServletRequest req, String name) throws IOException, ServletException {
		returnCrl( this.crlCache.findBySubjectKeyIdentifier(HashID.getFromB64(sKIDHash), isDelta(req), getCrlNumber(req, resp)), resp, name, isDelta(req) );
	}

	@Override
	public void printInfo(X509Certificate cert, String indent, PrintWriter pw, String url) {
        // Important to escape output that have an even small chance of coming from untrusted source
		pw.println(indent+HTMLTools.htmlescape(cert.getSubjectX500Principal().toString()));
		pw.println(indent+" "+RFC4387URL.iHash.getRef(url, HashID.getFromSubjectDN(cert)));
		pw.println(indent+" "+RFC4387URL.sKIDHash.getRef(url, HashID.getFromKeyID(cert)));
		pw.println(indent+" "+RFC4387URL.iHash.getRef(url, HashID.getFromSubjectDN(cert), true));
		pw.println(indent+" "+RFC4387URL.sKIDHash.getRef(url, HashID.getFromKeyID(cert), true));
	}

	@Override
	public String getTitle() {
		return "CRLs";
	}

	private boolean isDelta(HttpServletRequest req) {
		return req.getParameterMap().get("delta")!=null;
	}

	private int getCrlNumber(HttpServletRequest req, HttpServletResponse resp) throws IOException {
	    final String crlNumber = req.getParameter("crlnumber");
        if (StringUtils.isNumeric(crlNumber) && (Integer.valueOf(crlNumber) >= 0) ) {
            return Integer.valueOf(crlNumber);
        }
        return -1;
	}

	private void returnCrl( byte crl[], HttpServletResponse resp, String name, boolean isDelta ) throws IOException {
		if ( crl==null || crl.length<1 ) {
			resp.sendError(HttpServletResponse.SC_NO_CONTENT, "No CRL with hash: "+HTMLTools.htmlescape(name));
			return;
		}
		resp.setContentType("application/pkix-crl");
		resp.setHeader("Content-disposition", "attachment; filename=\""+(isDelta?"delta":"") + StringTools.stripFilename(name) + ".crl\"");
		resp.setContentLength(crl.length);
		resp.getOutputStream().write(crl);
	}
}
