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

package org.ejbca.core.protocol.msae;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.NamingException;

/**
 * Extends javax.naming.NamingException and maps error code to 
 * human readable error messages.
 */
public class LDAPException extends NamingException {

    private static final long serialVersionUID = 1L;
    private static Map<Integer, String> errorCodes = new HashMap<>();
    
    static {
        errorCodes.put(2, "Protocol error");
        errorCodes.put(3, "Time limit exceeded");
        errorCodes.put(4, "Size limit exceeded.");
        errorCodes.put(7, "Authentication method not supported.");
        errorCodes.put(8, "Strong authentication required.");
        errorCodes.put(32, "No such object exists");
        errorCodes.put(49, "Invalid credentials");
    }
    
    public LDAPException(final Exception e) {
        setRootCause(e);
    }
    
    public LDAPException(String message) {
        super(message);
    }
    
    public String getFriendlyMessage() {
        final int errorCode = getErrorCodeFromExceptionMessage(getMessage());
        if (errorCodes.containsKey(errorCode)) {
            return errorCodes.get(errorCode);
        }
        return getMessage();
    }
    
    private int getErrorCodeFromExceptionMessage(final String exceptionMsg) {
        String errorCodePattern="-?\\d+";
        Pattern pattern = Pattern.compile(errorCodePattern);
        Matcher  matcher = pattern.matcher(exceptionMsg);
        if (matcher.find()) {
            return Integer.valueOf(matcher.group(0));
        }
        return -1;
    }
}
