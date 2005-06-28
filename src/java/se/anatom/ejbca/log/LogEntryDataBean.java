/*************************************************************************
 *                                                                       *
 *  EJBCA: The OpenSource Certificate Authority                          *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/

package se.anatom.ejbca.log;

import se.anatom.ejbca.BaseEntityBean;
import se.anatom.ejbca.util.StringTools;

import javax.ejb.CreateException;
import java.util.Date;

/** Entity bean should not be used directly, use though Session beans.
 *
 * Entity Bean representing a log entry in the log database.
 * Information stored:
 * <pre>
 *  id (Primary Key)
 *  admintype is pricipally the type of data stored in the admindata field, should be one of se.anatom.ejbca.log.Admin.TYPE_ constants.
 *  admindata is the data identifying the administrator, should be certificate snr or ip-address when no certificate could be retrieved.
 *  time is the time the event occured.
 *  username the name of the user involved or null if no user is involved.
 *  certificate the certificate involved in the event or null if no certificate is involved.
 *  event is id of the event, should be one of the se.anatom.ejbca.log.LogEntry.EVENT_ constants.
 *  comment an optional comment of the event.
 * </pre>
 *
 * @ejb.bean
 *   description="This enterprise bean entity represents a Log Entry with accompanying data"
 *   display-name="LogEntryDataEB"
 *   name="LogEntryData"
 *   view-type="local"
 *   type="CMP"
 *   reentrant="False"
 *   cmp-version="2.x"
 *   transaction-type="Container"
 *   schema="LogEntryDataBean"
 *   primkey-field="id"
 *
 * @ejb.permission role-name="InternalUser"
 *
 * @ejb.pk
 *   generate="false"
 *   class="java.lang.Integer"
 *
 * @ejb.home
 *   generate="local"
 *   local-extends="javax.ejb.EJBLocalHome"
 *   local-class="se.anatom.ejbca.log.LogEntryDataLocalHome"
 *
 * @ejb.interface
 *   generate="local"
 *   local-extends="javax.ejb.EJBLocalObject"
 *   local-class="se.anatom.ejbca.log.LogEntryDataLocal"
 *
 * @ejb.transaction
 *    type="Supports"
 *
 * @jonas.jdbc-mapping
 *   jndi-name="${datasource.jndi-name}"
 *   
 * @version $Id: LogEntryDataBean.java,v 1.18 2005-06-28 08:43:59 anatom Exp $
 */
public abstract class LogEntryDataBean extends BaseEntityBean {

    /**
     * @ejb.pk-field
     * @ejb.persistence
     * @ejb.interface-method view-type="local"
     */
    public abstract Integer getId();

    /**
     * @ejb.persistence
     */
    public abstract void setId(Integer id);

    /**
     * @ejb.persistence
     * @ejb.interface-method view-type="local"
     */
    public abstract int getAdminType();

    /**
     * @ejb.persistence
     */
    public abstract void setAdminType(int admintype);

    /**
     * @ejb.persistence
     * @ejb.interface-method view-type="local"
     */
    public abstract String getAdminData();

    /**
     * @ejb.persistence
     */
    public abstract void setAdminData(String admindata);

    /** The id of the CA performing the event.
     * @ejb.persistence
     * @ejb.interface-method view-type="local"
     */
    public abstract int getCaId();

    /**
     * @ejb.persistence
     */
    public abstract void setCaId(int caid);

    /** Indicates the module (CA,RA ...) using the logsession bean.
     * @ejb.persistence
     * @ejb.interface-method view-type="local"
     */
    public abstract int getModule();

    /**
     * @ejb.persistence
     */
    public abstract void setModule(int module);

    /**
     * @ejb.persistence
     */
    public abstract long getTime();

    /**
     * @ejb.persistence
     */
    public abstract void setTime(long time);

    /**
     * @ejb.persistence
     * @ejb.interface-method view-type="local"
     */
    public abstract String getUsername();

    /** username must be called 'stripped' using StringTools.strip()
     * @ejb.persistence
     */
    public abstract void setUsername(String username);

    /**
     * @ejb.persistence
     * @ejb.interface-method view-type="local"
     */
    public abstract String getCertificateSNR();

    /**
     * @ejb.persistence
     */
    public abstract void setCertificateSNR(String certificatesnr);

    /**
     * @ejb.persistence
     * @ejb.interface-method view-type="local"
     */
    public abstract int getEvent();

    /**
     * @ejb.persistence
     */
    public abstract void setEvent(int event);

    /**
     * @ejb.persistence
     * @ejb.interface-method view-type="local"
     */
    public abstract String getComment();

    /**
     * @ejb.persistence
     */
    public abstract void setComment(String comment);

    /**
     * @ejb.interface-method view-type="local"
     */
    public Date getTimeAsDate() {
        return new Date(getTime());
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public LogEntry getLogEntry() {
        return new LogEntry(getAdminType(), getAdminData(), getCaId(), getModule(), getTimeAsDate(), getUsername(), getCertificateSNR(), getEvent(), getComment());
    }

    /**
     *
     * @ejb.create-method view-type="local"
     */
    public Integer ejbCreate(Integer id, int admintype, String admindata, int caid, int module, Date time, String username, String certificatesnr, int event, String comment) throws CreateException {
        setId(id);
        setAdminType(admintype);
        setAdminData(admindata);
        setCaId(caid);
        setModule(module);
        setTime(time.getTime());
        setUsername(StringTools.strip(username));
        setCertificateSNR(certificatesnr);
        setEvent(event);
        setComment(comment);
        return null;
    }

    /**
     */
    public void ejbPostCreate(Integer id, int admintype, String admindata, int caid, int module, Date time, String username, String certificatesnr, int event, String comment) {
// Do nothing. Required.
    }
}

