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

package org.ejbca.core.model.log;

import java.util.Collection;

/** This interface is used for exporting a number of log entries to 
 * any format defined by the implementing class.
 * 
 * @author tomas
 * @version $Id$
 */
public interface ILogExporter {

	/** Sets the entries to be exported. Entries can also be set in the constructor if it is more suitable for 
	 * the implementing class.  
	 * 
	 * @param logentries a Collection of LogEntry
	 */
	public void setEntries(Collection<LogEntry> logentries);
	
	/** Returns the number of log entries that are about to be exported
	 * 
	 * @return positive integer or 0
	 */
	public int getNoOfEntries();

	/** Returns the exported data, determined by the exporting class. Can be binary or text data.
	 * 
	 * @throws Exception if an error occurs during export
	 * @return byte data or null if no of exported entries are 0.
	 */
	public byte[] export(Admin admin) throws Exception;

}

