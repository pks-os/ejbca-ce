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
 
package se.anatom.ejbca.hardtoken.hardtokenprofiles;

import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.io.IOException;

import se.anatom.ejbca.ra.UserAdminData;



/**
 * Interface contating methods that need to be implementet in order 
 * to have a hard token profile contain adress label, either sent to 
 * a label printer or printed directly on an envelope.
 * 
 * @version $Id: IAdressLabelSettings.java,v 1.1 2005-04-11 05:44:42 herrvendil Exp $
 */

public interface IAdressLabelSettings {


	/**
	 * Constant indicating that no adress label should be printed.
	 */    
	public static int ADRESSLABELTYPE_NONE               = 0;
	
    /**
     * Constants indicating what type of adress label that 
     * should be printed.
     */ 
    public static int ADRESSLABELTYPE_GENERAL       = 1;


    /**      
     * @return the type of adress label to print.
     */
    public abstract int getAdressLabelType();    

	/**      
	 * sets the adress label type.
	 */
	public abstract void setAdressLabelType(int type);    
    
    /**
     * @return the filename of the current adress label template.
     */
    public abstract String getAdressLabelTemplateFilename();

	/**
	 * Sets the filename of the current adress label template.
	 */    
	public abstract void setAdressLabelTemplateFilename(String filename);
    
	/**
	 * Returns the image data of the adress label, should be a SVG image.
	 */
	public abstract String getAdressLabelData();		
	 

	/**
	 * Sets the imagedata of the adress label.
	 */
	public abstract void setAdressLabelData(String templatedata);
	
    /**
     * @return the number of copies of this PIN Envelope that should be printed.
     */
    public abstract int getNumberOfAdressLabelCopies();

	/**
	 * Sets the number of copies of this PIN Envelope that should be printed.
	 */
	public abstract void setNumberOfAdressLabelCopies(int copies);
	

   /**
    * Method that parses the template, replaces the userdata
    * and returning a printable byte array 
    */	
	public abstract Printable printVisualValidity(UserAdminData userdata, 
	                                        String[] pincodes, String[] pukcodes,
	                                        String hardtokensn, String copyoftokensn)
	                                          throws IOException, PrinterException;
}

