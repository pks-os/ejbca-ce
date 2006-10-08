
package org.ejbca.core.protocol.ws.client.gen;

import javax.xml.ws.WebFault;


/**
 * This class was generated by the JAXWS SI.
 * JAX-WS RI 2.0_01-b59-fcs
 * Generated source version: 2.0
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
     * @param message
     * @param cause
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
