
package org.ejbca.core.protocol.ws.client.gen;

import javax.xml.ws.WebFault;


/**
 * This class was generated by the JAXWS SI.
 * JAX-WS RI 2.0_01-b59-fcs
 * Generated source version: 2.0
 * 
 */
@WebFault(name = "WaitingForApprovalException", targetNamespace = "http://ws.protocol.core.ejbca.org/")
public class WaitingForApprovalException_Exception
    extends Exception
{

    /**
     * Java type that goes as soapenv:Fault detail element.
     * 
     */
    private WaitingForApprovalException faultInfo;

    /**
     * 
     * @param faultInfo
     * @param message
     */
    public WaitingForApprovalException_Exception(String message, WaitingForApprovalException faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    /**
     * 
     * @param faultInfo
     * @param message
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
