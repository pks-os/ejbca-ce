
package org.ejbca.core.protocol.ws.client.gen;

import javax.xml.ws.WebFault;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.1.6
 * Generated source version: 2.1
 * 
 */
@WebFault(name = "CesecoreException", targetNamespace = "http://ws.protocol.core.ejbca.org/")
public class CesecoreException_Exception
    extends Exception
{

    private static final long serialVersionUID = -5666352655365545678L;
    /**
     * Java type that goes as soapenv:Fault detail element.
     * 
     */
    private CesecoreException faultInfo;

    /**
     * 
     * @param message
     * @param faultInfo
     */
    public CesecoreException_Exception(String message, CesecoreException faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    /**
     * 
     * @param message
     * @param faultInfo
     * @param cause
     */
    public CesecoreException_Exception(String message, CesecoreException faultInfo, Throwable cause) {
        super(message, cause);
        this.faultInfo = faultInfo;
    }

    /**
     * 
     * @return
     *     returns fault bean: org.ejbca.core.protocol.ws.client.gen.CesecoreException
     */
    public CesecoreException getFaultInfo() {
        return faultInfo;
    }

}
