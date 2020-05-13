
package org.ejbca.core.protocol.ws.client.gen;

import javax.xml.ws.WebFault;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.2.9-b130926.1035
 * Generated source version: 2.2
 * 
 */
@WebFault(name = "AuthorizationDeniedException", targetNamespace = "http://ws.protocol.core.ejbca.org/")
public class AuthorizationDeniedException_Exception
    extends Exception
{

    /**
     * Java type that goes as soapenv:Fault detail element.
     * 
     */
    private AuthorizationDeniedException faultInfo;

    /**
     * 
     * @param faultInfo
     * @param message
     */
    public AuthorizationDeniedException_Exception(String message, AuthorizationDeniedException faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    /**
     * 
     * @param faultInfo
     * @param cause
     * @param message
     */
    public AuthorizationDeniedException_Exception(String message, AuthorizationDeniedException faultInfo, Throwable cause) {
        super(message, cause);
        this.faultInfo = faultInfo;
    }

    /**
     * 
     * @return
     *     returns fault bean: org.ejbca.core.protocol.ws.client.gen.AuthorizationDeniedException
     */
    public AuthorizationDeniedException getFaultInfo() {
        return faultInfo;
    }

}
