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
package org.ejbca.ui.web.admin.services.servicetypes;


/**
 * Class representing an Interval Type, should be registered in the 
 * ServiceTypesManager. Should be inhereted by all interval managed beans.
 *
 * @author Philip Vendil 2006 sep 29
 *
 * @version $Id: IntervalType.java,v 1.1 2006-10-01 17:46:47 herrvendil Exp $
 */
public abstract class IntervalType extends ServiceType {

	public IntervalType(String subViewPage, String name, boolean translatable) {
		super(subViewPage, name, translatable);
	}

}
