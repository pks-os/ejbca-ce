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

package org.ejbca.core.protocol.xkms;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;

import org.apache.log4j.Logger;

/**
 * This class was generated by the JAXWS SI.
 * JAX-WS RI 2.1-10/21/2006 12:56 AM(vivek)-EA2
 * Generated source version: 2.0
 * 
 */
@WebServiceClient(name = "XKMSService", targetNamespace = "http://www.w3.org/2002/03/xkms#wsdl", wsdlLocation = "src/xkms/wsdl/xkms.wsdl")
public class XKMSService
    extends Service
{

    private static final Logger log = Logger.getLogger(XKMSService.class);

    private final static URL XKMSSERVICE_WSDL_LOCATION;

    static {
        URL url = null;
        try {
            url = new URL("file:/C:/workspace/ejbca/src/xkms/wsdl/xkms.wsdl");
        } catch (MalformedURLException e) {
            log.warn("MalformedUrl creating XKMSService. ", e);
        }
        XKMSSERVICE_WSDL_LOCATION = url;
    }

    public XKMSService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public XKMSService() {
        super(XKMSSERVICE_WSDL_LOCATION, new QName("http://www.w3.org/2002/03/xkms#wsdl", "XKMSService"));
    }

    /**
     * 
     * @return
     *     returns XKMSPortType
     */
    @WebEndpoint(name = "XKMSPort")
    public XKMSPortType getXKMSPort() {
        return (XKMSPortType)super.getPort(new QName("http://www.w3.org/2002/03/xkms#wsdl", "XKMSPort"), XKMSPortType.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns XKMSPortType
     */
  /*  @WebEndpoint(name = "XKMSPort")
    public XKMSPortType getXKMSPort(WebServiceFeature... features) {
        return (XKMSPortType)super.getPort(new QName("http://www.w3.org/2002/03/xkms#wsdl", "XKMSPort"), XKMSPortType.class, features);
    }*/

}
