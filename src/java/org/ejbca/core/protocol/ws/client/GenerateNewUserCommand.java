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
 
package org.ejbca.core.protocol.ws.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.ra.UserDataConstants;
import org.ejbca.core.protocol.ws.client.gen.AuthorizationDeniedException_Exception;
import org.ejbca.core.protocol.ws.client.gen.Certificate;
import org.ejbca.core.protocol.ws.client.gen.UserDataVOWS;
import org.ejbca.core.protocol.ws.client.gen.UserDoesntFullfillEndEntityProfile_Exception;
import org.ejbca.core.protocol.ws.common.CertificateHelper;
import org.ejbca.ui.cli.ErrorAdminCommandException;
import org.ejbca.ui.cli.IAdminCommand;
import org.ejbca.ui.cli.IllegalAdminCommandException;
import org.ejbca.util.CertTools;





/**
 * Adds a user to the database.
 *
 * @version $Id: GenerateNewUserCommand.java,v 1.2 2006-10-08 22:53:26 herrvendil Exp $
 */
public class GenerateNewUserCommand extends EJBCAWSRABaseCommand implements IAdminCommand{

	
	private static final int ARG_USERNAME           = 1;
	private static final int ARG_PASSWORD           = 2;
	private static final int ARG_CLEARPWD           = 3;
	private static final int ARG_SUBJECTDN          = 4;
	private static final int ARG_SUBJECTALTNAME     = 5;
	private static final int ARG_EMAIL              = 6;
	private static final int ARG_CA                 = 7;
	private static final int ARG_TYPE               = 8;
	private static final int ARG_TOKEN              = 9;
	private static final int ARG_STATUS             = 10;
	private static final int ARG_ENDENTITYPROFILE   = 11;
	private static final int ARG_CERTIFICATEPROFILE = 12;
	private static final int ARG_ISSUERALIAS        = 13;
	private static final int ARG_PKCS10             = 14;
	private static final int ARG_ENCODING           = 15;
	private static final int ARG_HARDTOKENSN        = 16;
	private static final int ARG_OUTPUTPATH         = 17;
	
    /**
     * Creates a new instance of RaAddUserCommand
     *
     * @param args command line arguments
     */
    public GenerateNewUserCommand(String[] args) {
        super(args);
    }

    /**
     * Runs the command
     *
     * @throws IllegalAdminCommandException Error in command args
     * @throws ErrorAdminCommandException Error running command
     */
    public void execute() throws IllegalAdminCommandException, ErrorAdminCommandException {

        try {   
           
            if(args.length < 17 || args.length > 18){
            	usage();
            	System.exit(-1);
            }
            
            UserDataVOWS userdata = new UserDataVOWS();
            userdata.setUsername(args[ARG_USERNAME]);
            userdata.setPassword(args[ARG_PASSWORD]);
            userdata.setClearPwd(args[ARG_CLEARPWD].equalsIgnoreCase("true"));
            userdata.setSubjectDN(args[ARG_SUBJECTDN]);
            if(!args[ARG_SUBJECTALTNAME].equalsIgnoreCase("NULL")){                        
            	userdata.setSubjectAltName(args[ARG_SUBJECTALTNAME]);
            }
            if(!args[ARG_EMAIL].equalsIgnoreCase("NULL")){
            	userdata.setEmail(args[ARG_EMAIL]);
            }
            userdata.setCaName(args[ARG_CA]);
            userdata.setTokenType(args[ARG_TOKEN]);
            userdata.setStatus(getStatus(args[ARG_STATUS]));
            userdata.setEndEntityProfileName(args[ARG_ENDENTITYPROFILE]);
            userdata.setCertificateProfileName(args[ARG_CERTIFICATEPROFILE]);
            
            int type = Integer.parseInt(args[ARG_TYPE]);
            
            if((type & SecConst.USER_SENDNOTIFICATION) != 0){
            	userdata.setSendNotification(true);
            }
            if((type & SecConst.USER_KEYRECOVERABLE) != 0){
            	userdata.setKeyRecoverable(true);
            }

            if(!args[ARG_ISSUERALIAS].equalsIgnoreCase("NONE")){
            	userdata.setEmail(args[ARG_ISSUERALIAS]);
            }
            
            String username = args[ARG_USERNAME];
            String password = args[ARG_PASSWORD];
            String pkcs10 = getPKCS10(args[ARG_PKCS10]);
            String encoding = getEncoding(args[ARG_ENCODING]);
            String hardtokensn = getHardTokenSN(args[ARG_HARDTOKENSN]);
            String outputPath = null;           
            if(args.length == 18){
                outputPath = getOutputPath(args[ARG_OUTPUTPATH]);
            }
            
            getPrintStream().println("Trying to add user:");
            getPrintStream().println("Username: "+userdata.getUsername());
            getPrintStream().println("Subject DN: "+userdata.getSubjectDN());
            getPrintStream().println("Subject Altname: "+userdata.getSubjectAltName());
            getPrintStream().println("Email: "+userdata.getEmail());
            getPrintStream().println("CA Name: "+userdata.getCaName());                        
            getPrintStream().println("Type: "+type);
            getPrintStream().println("Token: "+userdata.getTokenType());
            getPrintStream().println("Status: "+userdata.getStatus());
            getPrintStream().println("End entity profile: "+userdata.getEndEntityProfileName());
            getPrintStream().println("Certificate profile: "+userdata.getCertificateProfileName());

            if(userdata.getHardTokenIssuerName() == null){
            	getPrintStream().println("Hard Token Issuer Alias: NONE");
            }else{
            	getPrintStream().println("Hard Token Issuer Alias: " + userdata.getHardTokenIssuerName());
            }
            
            
            try{
            	getEjbcaRAWS().editUser(userdata);            	
            	getPrintStream().println("User '"+userdata.getUsername()+"' has been added/edited.");
            	getPrintStream().println();       
            	
             	Certificate result = getEjbcaRAWS().pkcs10Req(username,password,pkcs10,hardtokensn);
            	
            	if(result==null){
            		getPrintStream().println("No certificate could be generated for user, check server logs for error.");
            	}else{
            		String filepath = username;
            		if(encoding.equals("DER")){
            			filepath += ".cer";
            		}else{
            			filepath += ".pem";
            		}
            		if(outputPath != null){
            			filepath = outputPath + "/" + filepath;
            		}
            		
            		
            		if(encoding.equals("DER")){
            			FileOutputStream fos = new FileOutputStream(filepath);
            			fos.write(CertificateHelper.getCertificate(result.getCertificateData()).getEncoded());
            			fos.close();
            		}else{
            			FileOutputStream fos = new FileOutputStream(filepath);
            			ArrayList<java.security.cert.Certificate> list = new ArrayList<java.security.cert.Certificate>();
            			list.add(CertificateHelper.getCertificate(result.getCertificateData()));
            			fos.write(CertTools.getPEMFromCerts(list));
            			fos.close();            				            				
            		}
            		getPrintStream().println("Certificate generated, written to " + filepath);
            	}
            }catch(AuthorizationDeniedException_Exception e){
            	getPrintStream().println("Error : " + e.getMessage());
            }catch(UserDoesntFullfillEndEntityProfile_Exception e){
            	getPrintStream().println("Error : Given userdata doesn't fullfill end entity profile. : " +  e.getMessage());
            }            
        } catch (Exception e) {
            throw new ErrorAdminCommandException(e);
        }
    }

	private int getStatus(String status) {
		if(status.equalsIgnoreCase("NEW")){
			return UserDataConstants.STATUS_NEW;
		}
		if(status.equalsIgnoreCase("INPROCESS")){
			return UserDataConstants.STATUS_INPROCESS;
		}
		if(status.equalsIgnoreCase("FAILED")){
			return UserDataConstants.STATUS_FAILED;
		}
		if(status.equalsIgnoreCase("HISTORICAL")){
			return UserDataConstants.STATUS_HISTORICAL;
		}		
		
		getPrintStream().println("Error in status string : " + status );
		usage();
		System.exit(-1);
		return 0;
	}
	
	private String getHardTokenSN(String hardtokensn) {
		if(hardtokensn.equalsIgnoreCase("NONE")){
		  return null;
		}
		
		return hardtokensn;
	}
	
	private String getPKCS10(String pkcs10Path) {
		String retval=null;
		try {
			FileInputStream fis = new FileInputStream(pkcs10Path);
			byte[] contents = new byte[fis.available()];
			fis.read(contents);			
			fis.close();
			retval = new String(contents);
		} catch (FileNotFoundException e) {
			getPrintStream().println("Error : PKCS10 file couln't be found.");
			System.exit(-1);		
		} catch (IOException e) {
			getPrintStream().println("Error reading content of PKCS10 file.");
			System.exit(-1);	
		}
		
		
		return retval;
	}

	private String getOutputPath(String outputpath) {
		File dir = new File(outputpath);
		if(!dir.exists()){
			getPrintStream().println("Error : Output directory doesn't seem to exist.");
			System.exit(-1);
		}
		if(!dir.isDirectory()){
			getPrintStream().println("Error : Output directory doesn't seem to be a directory.");
			System.exit(-1);			
		}
		if(!dir.canWrite()){
			getPrintStream().println("Error : Output directory isn't writeable.");
			System.exit(-1);

		}
		return outputpath;
	}

	private String getEncoding(String encoding) {
		if(!encoding.equalsIgnoreCase("PEM") && !encoding.equalsIgnoreCase("DER")){
			usage();
			System.exit(-1);
		}
		
		return encoding.toUpperCase();
	}


	protected void usage() {
		getPrintStream().println("Command used to add or edit userdata and to generate the user in one step.");
		getPrintStream().println("Usage : generatenewuser <username> <password> <clearpwd (true|false)> <subjectdn> <subjectaltname or NULL> <email or NULL> <caname> <type> <token> <status> <endentityprofilename> <certificateprofilename> <issueralias (or NONE)> <pkcs10path> <encoding (DER|PEM)> <hardtokensn (or NONE)> <outputpath (optional)>\n\n");
        getPrintStream().println("DN is of form \"C=SE, O=MyOrg, OU=MyOrgUnit, CN=MyName\" etc.");
        getPrintStream().println(
            "SubjectAltName is of form \"rfc822Name=<email>, dNSName=<host name>, uri=<http://host.com/>, ipaddress=<address>, guid=<globally unique id>\"");

        getPrintStream().println("Type (mask): INVALID=0; END-USER=1; KEYRECOVERABLE=128; SENDNOTIFICATION=256");
		
        getPrintStream().print("Existing tokens      : " + "USERGENERATED" + ", " +
        		"P12" + ", "+ "JKS" + ", "  + "PEM");
        getPrintStream().print("Existing statuses (new users will always be set as NEW) : NEW, INPROCESS, FAILED, HISTORICAL");
        getPrintStream().println("outputpath : directory where certificate is written in form username+.cer|.pem ");
	}


}
