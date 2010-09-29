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

/**
 * Parses embedded or overridden database.properties for info.
 * @version $Id$
 */
public class DatabaseConfiguration {

	public static final String CONFIG_DATASOURCENAME             = "datasource.jndi-name";
	public static final String CONFIG_DATASOURCENAMEPREFIX       = "datasource.jndi-name-prefix";

	public static String getDataSourceJndiNamePrefix(){
		return ConfigurationHolder.getString(CONFIG_DATASOURCENAMEPREFIX, "java:/");
	}

	public static String getFullDataSourceJndiName(){
		return getDataSourceJndiNamePrefix() + ConfigurationHolder.getString(CONFIG_DATASOURCENAME, "EjbcaDS");
	}
}
