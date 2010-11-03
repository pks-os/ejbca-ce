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
 
package org.ejbca.core.model.ra;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

import org.cesecore.core.ejb.ra.raadmin.EndEntityProfileSession;
import org.ejbca.core.ejb.authorization.AuthorizationSession;
import org.ejbca.core.ejb.ca.caadmin.CAAdminSession;
import org.ejbca.core.ejb.ra.raadmin.RaAdminSession;
import org.ejbca.core.model.approval.ApprovalDataVO;
import org.ejbca.core.model.authorization.AccessRulesConstants;
import org.ejbca.core.model.authorization.AuthorizationDeniedException;
import org.ejbca.core.model.log.Admin;
import org.ejbca.core.model.ra.raadmin.GlobalConfiguration;

/**
 * A class that looks up the which CA:s or end entity profiles the administrator is authorized to view.
 * 
 * @version $Id$
 */
public class RAAuthorization implements Serializable {
    
    private String authcastring = null;
    private String authendentityprofilestring = null;
    private TreeMap authprofilenames = null;
    private TreeMap authcreateprofilenames = null;
	private TreeMap authviewprofilenames = null;
    private Admin admin;
    private AuthorizationSession authorizationsession;
    private RaAdminSession raadminsession;
    private CAAdminSession caAdminSession;
    private EndEntityProfileSession endEntityProfileSession;
    
    /** Creates a new instance of RAAuthorization. */
    public RAAuthorization(Admin admin, RaAdminSession raadminsession, AuthorizationSession authorizationsession, CAAdminSession caAdminSession, EndEntityProfileSession endEntityProfileSession) {
    	this.admin = admin;
    	this.raadminsession = raadminsession;
    	this.authorizationsession = authorizationsession;
    	this.caAdminSession = caAdminSession;
    	this.endEntityProfileSession = endEntityProfileSession;
    }

    /**
     * Method that checks the administrators CA privileges and returns a string that should be used in where clause of userdata SQL queries.
     *
     * @return a string of administrators CA privileges that should be used in the where clause of SQL queries.
     */
    public String getCAAuthorizationString() {      
      if(authcastring==null){
        Iterator iter =  caAdminSession.getAvailableCAs(admin).iterator();
         
        authcastring = "";
        
        while(iter.hasNext()){
          if(authcastring.equals("")) {
            authcastring = " cAId = " + ((Integer) iter.next()).toString();   
          } else {    
            authcastring = authcastring + " OR cAId = " + ((Integer) iter.next()).toString();
          }
        }
        
        if(!authcastring.equals("")) {
          authcastring = "( " + authcastring + " )"; 
        }
      }
      
      return authcastring;
    } 

    /**
     * @return a string of end entity profile privileges that should be used in the where clause of SQL queries.
     * @throws AuthorizationDeniedException if the current requester isn't authorized to query for approvals
     */
    public String getEndEntityProfileAuthorizationString() throws AuthorizationDeniedException {
        boolean authorizedToApproveCAActions = false; // i.e approvals with endentityprofile ApprovalDataVO.ANY_ENDENTITYPROFILE
        boolean authorizedToApproveRAActions = false; // i.e approvals with endentityprofile not ApprovalDataVO.ANY_ENDENTITYPROFILE 
     
        authorizedToApproveCAActions = authorizationsession.isAuthorizedNoLog(admin, AccessRulesConstants.REGULAR_APPROVECAACTION);

        authorizedToApproveRAActions = authorizationsession.isAuthorizedNoLog(admin, AccessRulesConstants.REGULAR_APPROVEENDENTITY);

        if (!authorizedToApproveCAActions && !authorizedToApproveRAActions) {
            throw new AuthorizationDeniedException("Not authorized to query apporvals");
        }

    	String endentityauth = "";
        GlobalConfiguration globalconfiguration = raadminsession.getCachedGlobalConfiguration(admin);
        if (globalconfiguration.getEnableEndEntityProfileLimitations()){
        	endentityauth = getEndEntityProfileAuthorizationString(true);
        	if(authorizedToApproveCAActions && authorizedToApproveRAActions){
        		endentityauth = getEndEntityProfileAuthorizationString(true);
        		if(endentityauth != null){
        		  endentityauth = "(" + getEndEntityProfileAuthorizationString(false) + " OR endEntityProfileId=" + ApprovalDataVO.ANY_ENDENTITYPROFILE + " ) ";
        		}
        	}else if (authorizedToApproveCAActions) {
        		endentityauth = " endEntityProfileId=" + ApprovalDataVO.ANY_ENDENTITYPROFILE;
			}else if (authorizedToApproveRAActions) {
				endentityauth = getEndEntityProfileAuthorizationString(true);
			}        	
        	
        }
        return endentityauth.trim();
    }

    /**
     * Method that checks the administrators end entity profile privileges and returns a string that should be used in where clause of userdata SQL queries.
     *
     * @return a string of end entity profile privileges that should be used in the where clause of SQL queries.
     */
    public String getEndEntityProfileAuthorizationString(boolean includeparanteses){
      if(authendentityprofilestring==null){
    	Collection<Integer> profileIds = new ArrayList<Integer>(endEntityProfileSession.getEndEntityProfileIdToNameMap(admin).keySet());
      	Collection<Integer> result = this.authorizationsession.getAuthorizedEndEntityProfileIds(admin, AccessRulesConstants.VIEW_RIGHTS, profileIds);     	
      	result.retainAll(this.endEntityProfileSession.getAuthorizedEndEntityProfileIds(admin));
      	Iterator<Integer> iter = result.iterator();
      	                    
        while(iter.hasNext()){
          if(authendentityprofilestring == null) {
            authendentityprofilestring = " endEntityProfileId = " + iter.next().toString();   
          } else {    
            authendentityprofilestring = authendentityprofilestring + " OR endEntityProfileId = " + iter.next().toString();
          }
        }
        
        if(authendentityprofilestring != null) {
          authendentityprofilestring = "( " + authendentityprofilestring + " )"; 
        }
      }
      return authendentityprofilestring; 
    }
    
    
    public TreeMap getAuthorizedEndEntityProfileNames(){
      if(authprofilenames==null){
        authprofilenames = new TreeMap();  
        Iterator<Integer> iter = endEntityProfileSession.getAuthorizedEndEntityProfileIds(admin).iterator();      
        HashMap idtonamemap = endEntityProfileSession.getEndEntityProfileIdToNameMap(admin);
        while(iter.hasNext()){
          Integer id = iter.next();
          authprofilenames.put(idtonamemap.get(id),id);
        }
      }
      return authprofilenames;  
    }
    
	public TreeMap getCreateAuthorizedEndEntityProfileNames() {
		if(authcreateprofilenames == null){
			authcreateprofilenames = this.authEndEntityProfileNames(AccessRulesConstants.CREATE_RIGHTS);
		}
		return authcreateprofilenames;  
	}
	      
	public TreeMap getViewAuthorizedEndEntityProfileNames(){
	  if(authviewprofilenames == null){
	  	  authviewprofilenames = this.authEndEntityProfileNames(AccessRulesConstants.VIEW_RIGHTS);
	  }
	  return authviewprofilenames;
	}    
    
    public void clear(){
      authcastring=null;
      authendentityprofilestring=null;
      authprofilenames = null;
	  authcreateprofilenames = null;
	  authviewprofilenames = null;
    }
    
    
	public TreeMap authEndEntityProfileNames(String rights) {
	  TreeMap returnval = new TreeMap();	
	  HashMap profilemap = this.endEntityProfileSession.getEndEntityProfileIdToNameMap(admin);
	  Iterator<Integer> iter = endEntityProfileSession.getAuthorizedEndEntityProfileIds(admin).iterator();
	  while(iter.hasNext()){
		Integer next = iter.next();  
		if(this.endEntityAuthorization(admin, next.intValue(), rights)) { 
		  returnval.put(profilemap.get(next), next);  
		}
	  }
	  return returnval;
	}     
    
    /**
     * Help function used to check end entity profile authorization.
     */
    public boolean endEntityAuthorization(Admin admin, int profileid, String rights) {
        boolean returnval = false;
        // TODO FIX
        if (admin.getAdminInformation().isSpecialUser()) {
            returnval = true;
        } else {

            returnval = authorizationsession.isAuthorizedNoLog(admin, AccessRulesConstants.ENDENTITYPROFILEPREFIX + Integer.toString(profileid)
                    + rights);

        }
        return returnval;
    }  
}


