
package org.ejbca.core.protocol.ws.client.gen;

import javax.xml.ws.WebFault;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.2.4-b01
 * Generated source version: 2.2
 * 
 */
@WebFault(name = "WaitingForApprovalException", targetNamespace = "http://ws.protocol.core.ejbca.org/")
public class WaitingForApprovalException_Exception
    extends Exception
{
    
    private static final long serialVersionUID = 1L;

    /**
     * Java type that goes as soapenv:Fault detail element.
     * 
     */
    private WaitingForApprovalException faultInfo;

    /**
     * 
     * @param message
     * @param faultInfo
     */
    public WaitingForApprovalException_Exception(String message, WaitingForApprovalException faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    /**
     * 
     * @param message
     * @param faultInfo
     * @param cause
     */
    public WaitingForApprovalException_Exception(String message, WaitingForApprovalException faultInfo, Throwable cause) {
        super(message, cause);
        this.faultInfo = faultInfo;
    }

    /**
     * 
     * @return
     *     returns fault bean: org.ejbca.core.protocol.ws.client.gen.WaitingForApprovalException
     */
    public WaitingForApprovalException getFaultInfo() {
        return faultInfo;
    }

}
