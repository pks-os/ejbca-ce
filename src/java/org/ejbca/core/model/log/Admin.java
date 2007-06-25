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

/*
 * Admin.java
 *
 * Created on den 25 august 2002, 10:02
 */

package org.ejbca.core.model.log;


import java.io.Serializable;
import java.security.cert.X509Certificate;

import org.ejbca.core.model.authorization.AdminEntity;
import org.ejbca.core.model.authorization.AdminInformation;
import org.ejbca.util.CertTools;

/**
 * This is a class containing information about the administrator or admin preforming the event.
 * Data contained in the class is preferbly
 *
 * @author TomSelleck
 * @version $Id: Admin.java,v 1.5 2007-06-25 14:45:32 herrvendil Exp $
 */
public class Admin implements Serializable {

    /**
     * Determines if a de-serialized file is compatible with this class.
     *
     * Maintainers must change this value if and only if the new version
     * of this class is not compatible with old versions. See Sun docs
     * for <a href=http://java.sun.com/products/jdk/1.1/docs/guide
     * /serialization/spec/version.doc.html> details. </a>
     *
     */
    private static final long serialVersionUID = -9221031402622809524L;
    
    // Public Constants
    // Indicates the type of administrator.
    /** An administrator authenticated with client certificate */
    public static final int TYPE_CLIENTCERT_USER = 0;
    /** A user of the public web pages */
    public static final int TYPE_PUBLIC_WEB_USER = 1;
    /** An internal RA function, such as cmd line or CMP */
    public static final int TYPE_RA_USER = 2;
    /** An internal CA admin function, such as cms line */
    public static final int TYPE_CACOMMANDLINE_USER = 3;
    /** Batch generation tool */
    public static final int TYPE_BATCHCOMMANDLINE_USER = 4;
    /** Internal user in EJBCA, such as automatic job */
    public static final int TYPE_INTERNALUSER = 5;

    public static final int SPECIAL_ADMIN_BOUNDRARY = 100;

    public static final String[] ADMINTYPETEXTS = {"CLIENTCERT", "PUBLICWEBUSER", "RACMDLINE", "CACMDLINE", "BATCHCMDLINE", "INTERNALUSER"};

    private static final int[] ADMINTYPETOADMINENTITY = {0, AdminEntity.SPECIALADMIN_PUBLICWEBUSER, AdminEntity.SPECIALADMIN_RAADMIN,
                                                         AdminEntity.SPECIALADMIN_CACOMMANDLINEADMIN, AdminEntity.SPECIALADMIN_BATCHCOMMANDLINEADMIN,
                                                         AdminEntity.SPECIALADMIN_INTERNALUSER};

    protected int type = -1;
    protected String data;
    protected X509Certificate certificate;

    // Public Constructors
    public Admin(X509Certificate certificate) {
        this(TYPE_CLIENTCERT_USER, certificate.getSerialNumber().toString(16) + ", " + CertTools.getIssuerDN(certificate));
        this.certificate = certificate;
    }

    public Admin(int type, String ip) {
        this.type = type;
        this.data = ip;
    }
    

    public Admin(int type) {
        this(type, null);
    }


    // Public Methods

    public int getAdminType() {
        return this.type;
    }

    public String getAdminData() {
        return this.data;
    }

    // Method that takes the internal data and returns a AdminInformation object required by the Authorization module.
    public AdminInformation getAdminInformation() {
        if (type == TYPE_CLIENTCERT_USER)
            return new AdminInformation(certificate);

        return new AdminInformation(ADMINTYPETOADMINENTITY[type]);
    }

    /**
     * Method thar returns the caid of the CA, the admin belongs to.
     * Doesn't work properly for public web and special users so use with care.
     */

    public int getCaId() {
        int returnval = LogConstants.INTERNALCAID;
        if (type == TYPE_CLIENTCERT_USER)
            returnval = CertTools.getIssuerDN(certificate).hashCode();
        return returnval;
    }
    public String toString() {
    	if ((type > -1) && (type < ADMINTYPETEXTS.length-1)) {
        	return ADMINTYPETEXTS[type];    		
    	}
    	return "UNKNOWN";
    }

}
