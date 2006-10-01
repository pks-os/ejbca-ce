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
package org.ejbca.core.model.services.intervals;

import org.apache.log4j.Logger;
import org.ejbca.core.model.services.BaseInterval;
import org.ejbca.core.model.services.IInterval;
import org.ejbca.core.model.services.actions.DummyAction;

/**
 * Dummy class used for demonstration and test puporses
 * Only implement one method 
 * @author Philip Vendil 2006 sep 27
 *
 * @version $Id: DummyInterval.java,v 1.1 2006-10-01 17:46:47 herrvendil Exp $
 */
public class DummyInterval extends BaseInterval {

	private static final Logger log = Logger.getLogger(DummyInterval.class);
	/**
	 * @see org.ejbca.core.model.services.IInterval#getTimeToExecution()
	 */
	public long getTimeToExecution() {
		log.debug(">DummyInterval.performAction");
		return IInterval.DONT_EXECUTE;
	}

}
