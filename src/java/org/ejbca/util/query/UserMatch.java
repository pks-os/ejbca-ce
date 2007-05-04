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
 * UserMatch.java
 *
 * Created on den 20 juli 2002, 23:20
 */
package org.ejbca.util.query;

/**
 * A class used by Query class to build a query for ejbca ra module. Inherits BasicMatch.  Main
 * function is getQueryString which returns a fragment of SQL statment.
 *
 * @author TomSelleck
 * @version $Id: UserMatch.java,v 1.5 2007-05-04 09:06:39 anatom Exp $
 *
 * @see org.ejbca.util.query.BasicMatch
 * @see org.ejbca.util.query.TimeMatch
 * @see org.ejbca.util.query.LogMatch
 */
public class UserMatch extends BasicMatch {
    // Public Constants

    public static final int MATCH_WITH_USERNAME            = 0;
    public static final int MATCH_WITH_EMAIL               = 1;
    public static final int MATCH_WITH_STATUS              = 2; // Value must the number representation.
    public static final int MATCH_WITH_ENDENTITYPROFILE    = 3; // Matches the profile id not profilename.
    public static final int MATCH_WITH_CERTIFICATEPROFILE  = 4; // Matches the certificatetype id not name.
    public static final int MATCH_WITH_CA                  = 5; // Matches the CA id not CA name.
	public static final int MATCH_WITH_TOKEN               = 6;
	public static final int MATCH_WITH_DN                  = 7;
    // Subject DN fields.
    public static final int MATCH_WITH_UID              = 100;
    public static final int MATCH_WITH_COMMONNAME       = 101;
    public static final int MATCH_WITH_DNSERIALNUMBER   = 102;
    public static final int MATCH_WITH_GIVENNAME        = 103;
    public static final int MATCH_WITH_INITIALS         = 104;
    public static final int MATCH_WITH_SURNAME          = 105;
    public static final int MATCH_WITH_TITLE            = 106;
    public static final int MATCH_WITH_ORGANIZATIONUNIT = 107;
    public static final int MATCH_WITH_ORGANIZATION     = 108;
    public static final int MATCH_WITH_LOCALE           = 109;
    public static final int MATCH_WITH_STATE            = 110;
    public static final int MATCH_WITH_DOMAINCOMPONENT  = 111;
    public static final int MATCH_WITH_COUNTRY          = 112;
    // Subject Altname Fields
    public static final int MATCH_WITH_RFC822NAME       = 200;
    public static final int MATCH_WITH_DNSNAME          = 201;
    public static final int MATCH_WITH_IPADDRESS        = 202;
    public static final int MATCH_WITH_X400ADDRESS      = 203;
    public static final int MATCH_WITH_DIRECTORYNAME    = 204;
    public static final int MATCH_WITH_EDIPARTNAME      = 205;
    public static final int MATCH_WITH_URI              = 206;
    public static final int MATCH_WITH_REGISTEREDID     = 207;
    public static final int MATCH_WITH_UPN              = 208;
    public static final int MATCH_WITH_GUID             = 209;



    // Private Constants.
    static final String[] MATCH_WITH_SQLNAMES = {"username", "subjectEmail", "status"
                                                         , "endEntityProfileId", "certificateProfileId"
                                                         , "cAId", "tokenType"}; 
                                                         

    // Represents the column names in ra userdata table.
    private static final String MATCH_WITH_SUBJECTDN = "subjectDN";
    private static final String[] MATCH_WITH_SUBJECTDN_NAMES = {
        "UID=", "CN=", "SN=", "GIVENNAME=", "INITIALS=", "SURNAME=", "T=", "OU=", "O=", "L=", "ST=",
        "DC", "C="
    };
    
    private static final String MATCH_WITH_SUBJECTALTNAME = "subjectAltName";
    private static final String[] MATCH_WITH_SUBJECTALTNAME_NAMES = {
        "RFC822NAME=", "DNSNAME=", "IPADDRESS=", "X400ADDRESS=", "DIRECTORYNAME=",
        "EDIPARTNAME=", "UNIFORMRESOURCEIDENTIFIER=", "REGISTEREDID=", "UPN=",  "GUID="
    };
    // Public methods.

    /**
     * Creates a new instance of UserMatch.
     *
     * @param matchwith determines which field i userdata table to match with.
     * @param matchtype determines how to match the field. SubjectDN fields can only be matched
     *        with 'begins with'.
     * @param matchvalue the value to match with.
     *
     * @throws NumberFormatException if matchvalue constains illegal numbervalue when matching
     *         number field.
     */
    public UserMatch(int matchwith, int matchtype, String matchvalue)
        throws NumberFormatException {
        this.matchwith = matchwith;
        this.matchtype = matchtype;
        this.matchvalue = matchvalue;

        if ((matchwith >= MATCH_WITH_STATUS) && (matchwith <= MATCH_WITH_CA)) {
            new Integer(matchvalue);
        }
    }

    /**
     * Returns a SQL statement fragment from the given data.
     *
     * @return sql string
     */
    public String getQueryString() {
        String returnval = "";

        if (isSubjectDNMatch()) {
        	// Ignore MATCH_TYPE_EQUALS.
        	returnval = MATCH_WITH_SUBJECTDN + " LIKE '%" +
        	MATCH_WITH_SUBJECTDN_NAMES[matchwith - 100] + matchvalue + "%'";
        }else{
        	if (isSubjectAltNameMatch()){
        		returnval = MATCH_WITH_SUBJECTALTNAME + " LIKE '%" +
        		MATCH_WITH_SUBJECTALTNAME_NAMES[matchwith - 200] + matchvalue + "%'";        	
        	}else {
        		if(matchwith == MATCH_WITH_DN){
        			if (matchtype == BasicMatch.MATCH_TYPE_EQUALS) {
        				returnval = MATCH_WITH_SUBJECTDN + " = '" + matchvalue + "'";
        			}

        			if (matchtype == BasicMatch.MATCH_TYPE_BEGINSWITH) {
        				returnval = MATCH_WITH_SUBJECTDN + " LIKE '" + matchvalue + "%'";
        			} 

        			if (matchtype == BasicMatch.MATCH_TYPE_CONTAINS) {
        				returnval = MATCH_WITH_SUBJECTDN + " LIKE '%" + matchvalue + "%'";
        			} 

        		}else{
        			if (matchtype == BasicMatch.MATCH_TYPE_EQUALS) {
        				// Because some databases (read JavaDB/Derby) does not allow matching of integer with a string expression
        				// like "where status='10'" instead of "where status=10", we have to hav e some special handling here.
        				String stringChar = "'";
        		        if ((matchwith == MATCH_WITH_STATUS) || (matchwith == MATCH_WITH_CA) || (matchwith == MATCH_WITH_CERTIFICATEPROFILE) || (matchwith == MATCH_WITH_ENDENTITYPROFILE) || (matchwith == MATCH_WITH_TOKEN)) {
            				stringChar = "";
        		        }
        				returnval = MATCH_WITH_SQLNAMES[matchwith] + " = "+stringChar + matchvalue + stringChar;
        			}

        			if (matchtype == BasicMatch.MATCH_TYPE_BEGINSWITH) {
        				returnval = MATCH_WITH_SQLNAMES[matchwith] + " LIKE '" + matchvalue + "%'";
        			}
        		}
        	}
        }
        return returnval;
    }

    // getQueryString

    /**
     * Checks if query data is ok.
     *
     * @return true if query is legal, false otherwise
     */
    public boolean isLegalQuery() {
    	if(matchvalue == null){
    		return false;
    	}
        return !(matchvalue.trim().equals(""));
    }

    // Private Methods
    private boolean isSubjectDNMatch() {
        return this.matchwith >= 100 && this.matchwith < 200;
    }
    
    // Private Methods
    private boolean isSubjectAltNameMatch() {
        return this.matchwith >= 200 && this.matchwith < 300;
    }

    // Private Fields.
    private int matchwith;
    private int matchtype;
    private String matchvalue;
}
