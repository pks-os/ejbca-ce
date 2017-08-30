
package org.ejbca.core.protocol.ws.client.gen;

import javax.xml.ws.WebFault;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.2.4-b01
 * Generated source version: 2.2
 * 
 */
@WebFault(name = "MultipleMatchException", targetNamespace = "http://ws.protocol.core.ejbca.org/")
public class MultipleMatchException_Exception
    extends Exception
{
    
    private static final long serialVersionUID = 1L;

    /**
     * Java type that goes as soapenv:Fault detail element.
     * 
     */
    private MultipleMatchException faultInfo;

    /**
     * 
     * @param message
     * @param faultInfo
     */
    public MultipleMatchException_Exception(String message, MultipleMatchException faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    /**
     * 
     * @param message
     * @param faultInfo
     * @param cause
     */
    public MultipleMatchException_Exception(String message, MultipleMatchException faultInfo, Throwable cause) {
        super(message, cause);
        this.faultInfo = faultInfo;
    }

    /**
     * 
     * @return
     *     returns fault bean: org.ejbca.core.protocol.ws.client.gen.MultipleMatchException
     */
    public MultipleMatchException getFaultInfo() {
        return faultInfo;
    }

}
