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

package org.ejbca.config;

import org.apache.log4j.Logger;

/**
 * This file handles configuration from web.properties
 */
public class WebConfiguration {

	private static final Logger log = Logger.getLogger(WebConfiguration.class);
	
	public static final String CONFIG_HTTPSSERVERHOSTNAME  = "httpsserver.hostname";
	public static final String CONFIG_HTTPSERVERPUBHTTP    = "httpserver.pubhttp";
	public static final String CONFIG_HTTPSSERVERPRIVHTTPS = "httpserver.privhttps";
	public static final String CONFIG_HTTPSSERVEREXTERNALPRIVHTTPS = "httpserver.external.privhttps";
	
	/**
	 * The configured server host name
	 */
	public static String getHostName() {
		return EjbcaConfigurationHolder.getExpandedString(CONFIG_HTTPSSERVERHOSTNAME, "localhost");
	}
	
	/**
	 * Port used by EJBCA public webcomponents. i.e that doesn't require client authentication
	 */
	public static int getPublicHttpPort() {
		int value = 8080;
		try {
			value = Integer.parseInt(EjbcaConfigurationHolder.getString(CONFIG_HTTPSERVERPUBHTTP, ""+value));
		} catch( NumberFormatException e ) {
			log.warn("\"httpserver.pubhttp\" is not a decimal number. Using default value: " + value);
		}
		return value;
	}
	
	/**
	 * Port used by EJBCA private webcomponents. i.e that requires client authentication
	 */
	public static int getPrivateHttpsPort() {
		int value = 8443;
		try {
			value = Integer.parseInt(EjbcaConfigurationHolder.getString(CONFIG_HTTPSSERVERPRIVHTTPS, ""+value));
		} catch( NumberFormatException e ) {
			log.warn("\"httpserver.privhttps\" is not a decimal number. Using default value: " + value);
		}
		return value;
	}

	/**
	 * Port used by EJBCA public web to construct a correct url.
	 */
	public static int getExternalPrivateHttpsPort() {
		int value = 8443;
		try {
			value = Integer.parseInt(EjbcaConfigurationHolder.getString(CONFIG_HTTPSSERVEREXTERNALPRIVHTTPS, String.valueOf(getPrivateHttpsPort())));
		} catch( NumberFormatException e ) {
			log.warn("\"httpserver.external.privhttps\" is not a decimal number. Using default value: " + value);
		}
		return value;
	}

	/**
	 * Defines the available languages by language codes separated with a comma
	 */
	public static String getAvailableLanguages() {
		return EjbcaConfigurationHolder.getExpandedString("web.availablelanguages", "EN,DE,ES,FR,IT,PT,PT_BR,SE,ZH");
	}
	
	/**
	 * Setting to indicate if the secret information stored on hard tokens (i.e initial PIN/PUK codes) should
	 * be displayed for the administrators. If false only non-sensitive information is displayed. 
	 */
	public static boolean getHardTokenDiplaySensitiveInfo() {
		String value = EjbcaConfigurationHolder.getString("hardtoken.diplaysensitiveinfo", "true");
		return "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value);
	}

	/**
	 * Setting to configure the maximum number of rows to be returned when viewing logs in the admin-GUI.
	 */
	public static int getLogMaxQueryRowCount() {
		int value = 1000;
		try {
			value = Integer.parseInt(EjbcaConfigurationHolder.getString("log.maxqueryrowcount", ""+value));
		} catch( NumberFormatException e ) {
			log.warn("\"log.maxqueryrowcount\" is not a decimal number. Using default value: " + value);
		}
		return value;
	}
	
	/**
	 * Show links to the EJBCA documentation.
	 * @return "disabled", "internal" or and URL
	 */
	public static String getDocBaseUri() {
		return EjbcaConfigurationHolder.getExpandedString("web.docbaseuri", "internal");
	}
	
	/**
	 * Require administrator certificates to be available in database for revocation checks.
	 */
	public static boolean getRequireAdminCertificateInDatabase() {
		return "true".equalsIgnoreCase(EjbcaConfigurationHolder.getExpandedString("web.reqcertindb", "true"));
	}

	/**
	 * Default content encoding used to display JSP pages
	 */
	public static String getWebContentEncoding() {
	   	return EjbcaConfigurationHolder.getString ("web.contentencoding", "UTF-8");
	}
	
	/**
	 * The request browser certificate renewal web application is deployed
	 */
	public static boolean getRenewalEnabled() {
		return "true".equalsIgnoreCase(EjbcaConfigurationHolder.getExpandedString("web.renewalenabled", "false"));
	}

    public static boolean doShowStackTraceOnErrorPage(){
        final String s=EjbcaConfigurationHolder.getString ("web.errorpage.stacktrace", null);
        return s==null || s.toLowerCase().indexOf("true")>=0;
	}

    public static String notification(String sDefault){
        return EjbcaConfigurationHolder.getString ("web.errorpage.notification", sDefault);
    }
}
