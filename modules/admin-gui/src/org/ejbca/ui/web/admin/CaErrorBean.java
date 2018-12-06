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
package org.ejbca.ui.web.admin;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import javax.faces.application.ViewExpiredException;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;
import org.cesecore.authentication.AuthenticationFailedException;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.keys.token.CryptoTokenOfflineException;
import org.ejbca.config.WebConfiguration;
import org.ejbca.core.model.ra.raadmin.EndEntityProfileValidationException;
import org.ejbca.ui.web.ParameterException;
import org.ejbca.ui.web.admin.configuration.EjbcaWebBean;


/**
 * Bean used to display a summary of unexpected errors and debug log the cause.
 * @version $Id$
 */
@ManagedBean
@ViewScoped
public class CaErrorBean extends BaseManagedBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(CaErrorBean.class);
    
    private EjbcaWebBean ejbcaWebBean;
    
    private List<Throwable> throwables = null;
    private Integer httpErrorCode = null;
    private String error;
    private String errorMessage;
    private String stackTrace;

    public String getStackTrace() {
        return stackTrace;
    }
    
    /** @return the general error which occurred */
    public String getError() {
        return error;
    }
    
    /** @return error message generated by application exceptions */
    public String getErrorMessage() {
        return errorMessage;
    }
    
    /** 
     * without access to template, we have to fetch the CSS manually
     * @return path to admin web CSS file
     **/
    public String getCssFile() {
        return ejbcaWebBean.getBaseUrl() + "/" +ejbcaWebBean.getCssFile();
    }
    
    /** Invoked when error.xhtml is rendered. Add all errors using the current localization. 
     * @throws Exception 
     */
    @SuppressWarnings("unchecked")
    public void onErrorPageLoad() throws Exception {
        ejbcaWebBean = getEjbcaErrorWebBean();
        // Default error
        error = ejbcaWebBean.getText("EXCEPTIONOCCURED");
        final Map<String, Object> requestMap = FacesContext.getCurrentInstance().getExternalContext().getRequestMap();
        // Render error caught by CaExceptionHandlerFactory
        if (throwables==null) {
            throwables = (List<Throwable>) requestMap.get(CaExceptionHandlerFactory.REQUESTMAP_KEY);
            requestMap.remove(CaExceptionHandlerFactory.REQUESTMAP_KEY);
        }
        if (throwables==null) {
            log.debug("No exception thrown, could not renderer error messages.");
        } else {
            for (final Throwable throwable : throwables) {
                if (throwable instanceof ViewExpiredException) {
                    error = ejbcaWebBean.getText("SESSION_TIMEOUT");
                } else if (throwable instanceof AuthorizationDeniedException || throwable instanceof AuthenticationFailedException) {
                    error = ejbcaWebBean.getText("AUTHORIZATIONDENIED");
                    errorMessage = ejbcaWebBean.getText("CAUSE") + ": " + throwable.getMessage();
                } else if (throwable instanceof CryptoTokenOfflineException) {
                    error = ejbcaWebBean.getText("CATOKENISOFFLINE");
                    errorMessage = ejbcaWebBean.getText("CAUSE") + ": " + throwable.getMessage();
                } else if (throwable instanceof ParameterException) {
                    errorMessage = throwable.getLocalizedMessage();
                } else if (throwable instanceof EndEntityProfileValidationException) {
                    error = ejbcaWebBean.getText("EXCEPTIONOCCURED");
                } else {
                    errorMessage = throwable.getLocalizedMessage();
                    // Return the message of the root cause exception
                    Throwable cause = throwable;
                    if (cause.getCause() != null) {
                        cause = cause.getCause();
                    }
                    // Log the entire exception stack trace
                    if (log.isDebugEnabled()) {
                        log.debug("Client got the following error message: " + cause.getMessage(), throwable);
                    }
                    if (WebConfiguration.doShowStackTraceOnErrorPage()) {
                        StringWriter stringWriter = new StringWriter();
                        PrintWriter printWriter = new PrintWriter(stringWriter, true);
                        throwable.printStackTrace(printWriter);
                        stackTrace = stringWriter.toString();
                    }
                }
            }
        }
        // Render error caught by web.xml error-page definition
        if (httpErrorCode==null) {
            final Object httpErrorCodeObject = requestMap.get("javax.servlet.error.status_code");
            if (httpErrorCodeObject!=null && httpErrorCodeObject instanceof Integer) {
                httpErrorCode = (Integer) httpErrorCodeObject;
                if (log.isDebugEnabled()) {
                    final String httpErrorUri = String.valueOf(requestMap.get("javax.servlet.error.request_uri"));
                    final String httpErrorMsg = String.valueOf(requestMap.get("javax.servlet.error.message"));
                    log.debug("Client got HTTP error " + httpErrorCode + " when trying to access '" + httpErrorUri + "'. Message was: " + httpErrorMsg);
                }
            }
        }
        if (httpErrorCode != null) {
            switch (httpErrorCode) {
                case 403: 
                    error = ejbcaWebBean.getText("AUTHORIZATIONDENIED");
                    break;
                case 404:
                    error = "404";
                    errorMessage = ejbcaWebBean.getText("UNKNOWN_PAGE"); 
                    break;
                default: 
                    errorMessage = "Server returned: " + httpErrorCode; 
                    break;
            }
        }
    }
}



