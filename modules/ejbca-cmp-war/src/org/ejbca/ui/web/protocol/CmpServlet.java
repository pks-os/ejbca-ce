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

package org.ejbca.ui.web.protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.cesecore.authentication.tokens.AlwaysAllowLocalAuthenticationToken;
import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authentication.tokens.UsernamePrincipal;
import org.cesecore.certificates.certificate.request.ResponseMessage;
import org.ejbca.core.model.InternalEjbcaResources;
import org.ejbca.core.protocol.cmp.CmpMessageDispatcherSessionLocal;
import org.ejbca.ui.web.LimitLengthASN1Reader;
import org.ejbca.ui.web.RequestHelper;
import org.ejbca.ui.web.pub.ServletUtils;

/**
 * Servlet implementing server side of the Certificate Management Protocols (CMP)
 * 
 * @author tomas
 * @version $Id$
 */
public class CmpServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(CmpServlet.class);
    private static final InternalEjbcaResources intres = InternalEjbcaResources.getInstance(); // Internal localization of logs and errors

    @EJB
    private CmpMessageDispatcherSessionLocal cmpMessageDispatcherLocal;

    /**
     * Handles HTTP post
     * 
     * @param request java standard arg
     * @param response java standard arg
     * 
     * @throws IOException input/output error
     * @throws ServletException if the post could not be handled
     */
    public void doPost(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
        if (log.isTraceEnabled()) {
            log.trace(">doPost()");
        }
        /* 
         POST
         <binary CMP message>
         */
        try {
            final ServletInputStream sin = request.getInputStream();
            // This small code snippet is inspired/copied by apache IO utils to Tomas Gustavsson...
            final ByteArrayOutputStream output = new ByteArrayOutputStream();
            final byte[] buf = new byte[1024];
            int n = 0;
            int bytesRead = 0;
            while (-1 != (n = sin.read(buf))) {
                bytesRead += n;
                if (bytesRead > LimitLengthASN1Reader.MAX_REQUEST_SIZE) {
                    throw new IllegalArgumentException("Request is larger than "+LimitLengthASN1Reader.MAX_REQUEST_SIZE+" bytes.");
                }
                output.write(buf, 0, n);
            }
            service(output.toByteArray(), request.getRemoteAddr(), response);
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            log.info(intres.getLocalizedMessage("cmp.errornoasn1"), e);
        }
        if (log.isTraceEnabled()) {
            log.trace("<doPost()");
        }
    }

    /**
     * Handles HTTP get
     * 
     * @param request java standard arg
     * @param response java standard arg
     * 
     * @throws IOException input/output error
     * @throws ServletException if the post could not be handled
     */
    public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws java.io.IOException, ServletException {
        if (log.isTraceEnabled()) {
            log.trace(">doGet()");
        }
        log.info("Received un-allowed method GET in CMP servlet: query string=" + request.getQueryString());
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "You can only use POST!");
        if (log.isTraceEnabled()) {
            log.trace("<doGet()");
        }
    }

    private void service(final byte[] ba, final String remoteAddr, final HttpServletResponse response) throws IOException {
        try {
            log.info(intres.getLocalizedMessage("cmp.receivedmsg", remoteAddr));
            final long startTime = System.currentTimeMillis();
            final ResponseMessage resp;
            try {
                // We must use an administrator with rights to create users
                final AuthenticationToken admin = new AlwaysAllowLocalAuthenticationToken(new UsernamePrincipal("CmpServlet: "+remoteAddr));
                resp = cmpMessageDispatcherLocal.dispatch(admin, ba);
            } catch (IOException e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
                log.info(intres.getLocalizedMessage("cmp.errornoasn1"), e);
                return;
            }
            if (resp == null) { // If resp is null, it means that the dispatcher failed to process the message.
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, intres.getLocalizedMessage("cmp.errornullresp"));
                return;
            }
            // Add no-cache headers as defined in 
            // http://tools.ietf.org/html/draft-ietf-pkix-cmp-transport-protocols-14
            ServletUtils.addCacheHeaders(response);
            // Send back CMP response
            RequestHelper.sendBinaryBytes(resp.getResponseMessage(), response, "application/pkixcmp", null);
            final long endTime = System.currentTimeMillis();
            log.info(intres.getLocalizedMessage("cmp.sentresponsemsg", remoteAddr, Long.valueOf(endTime - startTime)));
        } catch (Exception e) {
            log.error("Error in CmpServlet:", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

}
