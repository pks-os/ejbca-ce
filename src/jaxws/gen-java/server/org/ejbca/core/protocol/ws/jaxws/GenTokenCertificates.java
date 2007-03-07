
package org.ejbca.core.protocol.ws.jaxws;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.ejbca.core.protocol.ws.objects.HardTokenDataWS;
import org.ejbca.core.protocol.ws.objects.UserDataVOWS;

@XmlRootElement(name = "genTokenCertificates", namespace = "http://ws.protocol.core.ejbca.org/")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "genTokenCertificates", namespace = "http://ws.protocol.core.ejbca.org/", propOrder = {
    "arg0",
    "arg1",
    "arg2"
})
public class GenTokenCertificates {

    @XmlElement(name = "arg0", namespace = "")
    private UserDataVOWS arg0;
    @XmlElement(name = "arg1", namespace = "")
    private List<org.ejbca.core.protocol.ws.objects.TokenCertificateRequestWS> arg1;
    @XmlElement(name = "arg2", namespace = "")
    private HardTokenDataWS arg2;

    /**
     * 
     * @return
     *     returns UserDataVOWS
     */
    public UserDataVOWS getArg0() {
        return this.arg0;
    }

    /**
     * 
     * @param arg0
     *     the value for the arg0 property
     */
    public void setArg0(UserDataVOWS arg0) {
        this.arg0 = arg0;
    }

    /**
     * 
     * @return
     *     returns List<TokenCertificateRequestWS>
     */
    public List<org.ejbca.core.protocol.ws.objects.TokenCertificateRequestWS> getArg1() {
        return this.arg1;
    }

    /**
     * 
     * @param arg1
     *     the value for the arg1 property
     */
    public void setArg1(List<org.ejbca.core.protocol.ws.objects.TokenCertificateRequestWS> arg1) {
        this.arg1 = arg1;
    }

    /**
     * 
     * @return
     *     returns HardTokenDataWS
     */
    public HardTokenDataWS getArg2() {
        return this.arg2;
    }

    /**
     * 
     * @param arg2
     *     the value for the arg2 property
     */
    public void setArg2(HardTokenDataWS arg2) {
        this.arg2 = arg2;
    }

}
