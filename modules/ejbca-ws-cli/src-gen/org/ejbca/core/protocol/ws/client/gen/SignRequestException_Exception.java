
package org.ejbca.core.protocol.ws.client.gen;

import javax.xml.ws.WebFault;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.2.9-b130926.1035
 * Generated source version: 2.2
 * 
 */
@WebFault(name = "SignRequestException", targetNamespace = "http://ws.protocol.core.ejbca.org/")
public class SignRequestException_Exception
    extends Exception
{

    /**
     * Java type that goes as soapenv:Fault detail element.
     * 
     */
    private SignRequestException faultInfo;

    /**
     * 
     * @param faultInfo
     * @param message
     */
    public SignRequestException_Exception(String message, SignRequestException faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    /**
     * 
     * @param faultInfo
     * @param cause
     * @param message
     */
    public SignRequestException_Exception(String message, SignRequestException faultInfo, Throwable cause) {
        super(message, cause);
        this.faultInfo = faultInfo;
    }

    /**
     * 
     * @return
     *     returns fault bean: org.ejbca.core.protocol.ws.client.gen.SignRequestException
     */
    public SignRequestException getFaultInfo() {
        return faultInfo;
    }

}
