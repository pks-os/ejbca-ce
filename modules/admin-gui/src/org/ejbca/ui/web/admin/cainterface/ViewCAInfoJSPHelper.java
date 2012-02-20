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
 
package org.ejbca.ui.web.admin.cainterface;

import javax.servlet.http.HttpServletRequest;

import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.certificates.ca.CAConstants;
import org.cesecore.keys.token.CryptoToken;
import org.cesecore.keys.token.CryptoTokenAuthenticationFailedException;
import org.cesecore.keys.token.CryptoTokenOfflineException;
import org.ejbca.core.model.approval.ApprovalException;
import org.ejbca.core.model.approval.WaitingForApprovalException;
import org.ejbca.core.model.authorization.AccessRulesConstants;
import org.ejbca.core.model.ca.publisher.BasePublisher;
import org.ejbca.ui.web.RequestHelper;
import org.ejbca.ui.web.admin.configuration.EjbcaWebBean;


/**
 * Contains help methods used to parse a viewcainfo jsp page requests.
 *
 * @author  Philip Vendil
 * @version $Id$
 */
public class ViewCAInfoJSPHelper implements java.io.Serializable {
		 
	private static final long serialVersionUID = 109073226626366410L;

    public static final String CA_PARAMETER             = "caid";

	public static final String CERTSERNO_PARAMETER      = "certsernoparameter"; 
	  
	public static final String PASSWORD_AUTHENTICATIONCODE  = "passwordactivationcode";
	
    public static final String CHECKBOX_VALUE                = BasePublisher.TRUE;
	  
	public static final String BUTTON_ACTIVATE          = "buttonactivate";
	public static final String BUTTON_MAKEOFFLINE       = "buttonmakeoffline";
	public static final String BUTTON_CLOSE             = "buttonclose"; 
	public static final String CHECKBOX_INCLUDEINHEALTHCHECK = "includeinhealthcheck";
	public static final String SUBMITHS					= "submiths";

    private CAInterfaceBean cabean;
    private boolean initialized=false;
	public String   generalerrormessage = null;
	public String   activationerrormessage = null;
	public String   activationerrorreason = null;
	public String   activationmessage = null;
    public boolean  can_activate = false;    
    public boolean  authorized = false; 
    public CAInfoView cainfo = null;
    public  int status = 0; 
    public boolean tokenoffline = false;
    public  int caid = 0; 

    /** Creates new LogInterfaceBean */
    public ViewCAInfoJSPHelper(){     	    	
    }
    // Public methods.
    /**
     * Method that initialized the bean.
     *
     * @param request is a reference to the http request.
     */
    public void initialize(HttpServletRequest request, EjbcaWebBean ejbcawebbean,
                           CAInterfaceBean cabean) throws  Exception{

      if(!initialized){		  
        this.cabean = cabean;                        		
        initialized = true;
        can_activate = false;
        authorized = false;
		try{
			authorized = ejbcawebbean.isAuthorizedNoLog(AccessRulesConstants.REGULAR_CABASICFUNCTIONS);
			can_activate = ejbcawebbean.isAuthorizedNoLog(AccessRulesConstants.REGULAR_ACTIVATECA);
		}catch(AuthorizationDeniedException ade){}
      }
    }

    /**
     * Method that parses the request and take appropriate actions.
     * @param request the http request
     * @throws Exception
     */
    public void parseRequest(HttpServletRequest request) throws Exception{
    	  generalerrormessage = null;
    	  activationerrormessage = null;   
    	  activationmessage = null;
          RequestHelper.setDefaultCharacterEncoding(request);

          //See if includeinHealthCheck should be enabled.
          if(request.getParameter(SUBMITHS) != null ) {
        	  try{
        		  caid = Integer.parseInt(request.getParameter(CA_PARAMETER));
        		  cainfo = cabean.getCAInfo(caid);
        		  status = cainfo.getCAInfo().getStatus();
        	  } catch(AuthorizationDeniedException e){
        		  generalerrormessage = "NOTAUTHORIZEDTOVIEWCA";
        		  return;
        	  }
        	  String value = request.getParameter(CHECKBOX_INCLUDEINHEALTHCHECK);
        	  if(value != null) {
        		  cainfo.getCAInfo().setIncludeInHealthCheck( true );
        	  } else {
        		  cainfo.getCAInfo().setIncludeInHealthCheck( false );
        	  }
        	  // persist to database
        	  cabean.getCADataHandler().editCA(cainfo.getCAInfo());
          }
          
          if( request.getParameter(CA_PARAMETER) != null ){
    	    caid = Integer.parseInt(request.getParameter(CA_PARAMETER));
    	             	    
    	    if(request.getParameter(BUTTON_ACTIVATE) != null || request.getParameter(BUTTON_MAKEOFFLINE) != null){
    	      // Get currentstate
    	      status = CAConstants.CA_OFFLINE;
    	      try{
    	      	cainfo = cabean.getCAInfo(caid);
    	      	status = cainfo.getCAInfo().getStatus();
    	      } catch(AuthorizationDeniedException e){
    	      	generalerrormessage = "NOTAUTHORIZEDTOVIEWCA";
    	      	return;
    	      } 
    	      
    	      
    	      
    	      // If Activate button is pressed, the admin is authorized and the current status is offline then activate.
    	      if(request.getParameter(BUTTON_ACTIVATE) != null &&
    	      	 can_activate &&
				 ( (status == CAConstants.CA_OFFLINE) ||
				   ((status == CAConstants.CA_ACTIVE || status == CAConstants.CA_WAITING_CERTIFICATE_RESPONSE || status == CAConstants.CA_EXPIRED) && (cainfo.getCAInfo().getCATokenInfo().getTokenStatus() == CryptoToken.STATUS_OFFLINE)) )) {
    	         
    	         String authorizationcode = request.getParameter(PASSWORD_AUTHENTICATIONCODE);
    	         try {
    	        	 cabean.getCADataHandler().activateCAToken(caid,authorizationcode);
    	        	 activationmessage = "CAACTIVATIONSUCCESSFUL";
	         	 } catch (CryptoTokenAuthenticationFailedException catafe) {
	         		 activationerrormessage = "AUTHENTICATIONERROR";
	         		 activationerrorreason = catafe.getMessage();
	         		 Throwable t = catafe.getCause();
	         		 while (t != null) {
	         			 String msg = t.getMessage();
	         			 if (msg != null) {
	         				 activationerrorreason = activationerrorreason + "<br/>" + msg;							
	         			 }
	         			 t = t.getCause();
	         		 }
	         	 } catch (CryptoTokenOfflineException catoe) {
	         		 activationerrormessage = "ERROR";
	         		 activationerrorreason = catoe.getMessage();
	         	 } catch (ApprovalException e) {
	         		activationerrormessage = "CAACTIVATIONREQEXISTS";
	         		activationerrorreason = cainfo.getCAInfo().getName();
	         	 } catch (WaitingForApprovalException e){
	         		 activationmessage = "CAACTIVATIONSENTAPPROVAL";
	         	 }
    	      }
    	      // If Make off-line button is pressed, the admin is authorized and the current status is active then de-activate.
    	      if (request.getParameter(BUTTON_MAKEOFFLINE) != null && can_activate && status == CAConstants.CA_ACTIVE) {    	         
    	          cabean.getCADataHandler().deactivateCAToken(caid);
    	          activationmessage = "MAKEOFFLINESUCCESSFUL";
    	      }   	    
    	    }
    	        	        	        	        	        	      	  
    	    
    	    try{
    	      cainfo = cabean.getCAInfo(caid);
    	      status = cainfo.getCAInfo().getStatus();
    	      tokenoffline = cainfo.getCAInfo().getCATokenInfo().getTokenStatus() == CryptoToken.STATUS_OFFLINE;
    	    } catch(AuthorizationDeniedException e){
    	    	generalerrormessage = "NOTAUTHORIZEDTOVIEWCA";
    	    }

    	    if(cainfo==null){
    	      generalerrormessage = "CADOESNTEXIST";	
    	    }
    	  }else{
    	  	generalerrormessage = "YOUMUSTSPECIFYCAID";
    	  }
  
    }
}
