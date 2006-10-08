
package org.ejbca.core.protocol.ws.jaxws;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "findCertsResponse", namespace = "http://ws.protocol.core.ejbca.org/")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "findCertsResponse", namespace = "http://ws.protocol.core.ejbca.org/")
public class FindCertsResponse {

    @XmlElement(name = "return", namespace = "")
    private List<org.ejbca.core.protocol.ws.objects.Certificate> _return;

    /**
     * 
     * @return
     *     returns List<Certificate>
     */
    public List<org.ejbca.core.protocol.ws.objects.Certificate> get_return() {
        return this._return;
    }

    /**
     * 
     * @param _return
     *     the value for the _return property
     */
    public void set_return(List<org.ejbca.core.protocol.ws.objects.Certificate> _return) {
        this._return = _return;
    }

}
