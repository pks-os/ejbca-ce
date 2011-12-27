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
 
package org.ejbca.ui.web.admin.hardtokeninterface;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authorization.control.AccessControlSessionLocal;
import org.cesecore.roles.RoleData;
import org.cesecore.roles.management.RoleManagementSessionLocal;
import org.ejbca.core.ejb.hardtoken.HardTokenSession;
import org.ejbca.core.model.authorization.AccessRulesConstants;
import org.ejbca.core.model.hardtoken.HardTokenIssuerData;

/**
 * A class that looks up the which Hard Token Issuers the administrator is authorized to view and edit
 * 
 * @version $Id$
 */
public class HardTokenAuthorization implements Serializable {

    private static final long serialVersionUID = 164749645578145734L;
  
    private TreeMap<String, HardTokenIssuerData> hardtokenissuers = null;
    private TreeMap<String, Integer> hardtokenprofiles = null;
    private HashMap<Integer, String>  hardtokenprofilesnamemap = null;
    private Collection<RoleData> authissueingadmgrps = null;

    private AuthenticationToken admin;
    private HardTokenSession hardtokensession;
    private AccessControlSessionLocal authorizationsession;    
    private RoleManagementSessionLocal roleManagementSession;

    /** Creates a new instance of CAAuthorization. */
    public HardTokenAuthorization(AuthenticationToken admin, HardTokenSession hardtokensession, 
    		AccessControlSessionLocal authorizationsession, RoleManagementSessionLocal roleManagementSession) {
      this.admin=admin;
      this.hardtokensession=hardtokensession;            
      this.authorizationsession = authorizationsession;
      this.roleManagementSession = roleManagementSession;
    }

    /**
     * Method returning a TreeMap containing Hard Token Alias -> Hard Token Issuer Data
     * the administrator is authorized to view and edit
     * @return A TreeMap Hard Token Alias (String) -> HardTokenIssuerData
     */    
    public TreeMap<String, HardTokenIssuerData> getHardTokenIssuers() {
        if (hardtokenissuers == null) {
            hardtokenissuers = new TreeMap<String, HardTokenIssuerData>();
            HashSet<Integer> authadmingroupids = new HashSet<Integer>();
            for (RoleData next : roleManagementSession.getAllRolesAuthorizedToEdit(admin)) {
                authadmingroupids.add(Integer.valueOf(next.getPrimaryKey()));
            }
            TreeMap<String, HardTokenIssuerData> allhardtokenissuers = this.hardtokensession.getHardTokenIssuers(admin);
            for (String alias : allhardtokenissuers.keySet()) {
                if (authadmingroupids.contains(Integer.valueOf(((HardTokenIssuerData) allhardtokenissuers.get(alias)).getRoleDataId()))) {
                    hardtokenissuers.put(alias, allhardtokenissuers.get(alias));
                }
            }
        }
        return hardtokenissuers;
    }
    
	/**
	 * Method returning a TreeMap containing Hard Token Profile Name -> Hard Token Profile Id
	 * the administrator is authorized to view and edit
	 * @return A TreeMap Hard Token Profile Name (String) -> Hard Token Profile Id
	 */    
	public TreeMap<String, Integer> getHardTokenProfiles(){  
	  if(hardtokenprofiles==null){            
		hardtokenprofiles = new TreeMap<String, Integer>();                	
		for(Integer id :  hardtokensession.getAuthorizedHardTokenProfileIds(admin)){		       
		  String name = hardtokensession.getHardTokenProfileName(admin,id.intValue());
		  hardtokenprofiles.put(name, id);		    
		}        
	  }      
	  return hardtokenprofiles;  
	}
    
    
    /**
     * Checks if administrator is authorized to edit the specified hard token
     * issuer.
     * 
     * @param alias
     *            of hard token issuer
     * @return true if administrator is authorized to edit ahrd token issuer.
     */

    public boolean authorizedToHardTokenIssuer(String alias) {
        return authorizationsession.isAuthorizedNoLogging(admin, AccessRulesConstants.HARDTOKEN_EDITHARDTOKENISSUERS)
                && this.getHardTokenIssuers().keySet().contains(alias);
    }

    /**
     * Checks if administrator is authorized to edit the specified hard token
     * profile.
     * 
     * @param alias
     *            of hard token profile
     * @return true if administrator is authorized to edit hard token profile.
     */

    public boolean authorizedToHardTokenProfile(String name) {
        return authorizationsession.isAuthorizedNoLogging(admin, AccessRulesConstants.HARDTOKEN_EDITHARDTOKENPROFILES)
                && this.getHardTokenProfiles().keySet().contains(name);
    }

    /**
     * Returns a Map of hard token profile id (Integer) -> hard token profile
     * name (String).
     */
    public HashMap<Integer, String>  getHardTokenProfileIdToNameMap() {
        if (hardtokenprofilesnamemap == null) {
            hardtokenprofilesnamemap = this.hardtokensession.getHardTokenProfileIdToNameMap(admin);
        }

        return hardtokenprofilesnamemap;
    }

    /**
     * Returns a Collection of role names authorized to issue hard tokens,
     * it also only returns the admin groups the administrator is authorized to edit.
     */
    public Collection<RoleData> getHardTokenIssuingAdminGroups() {
        if (authissueingadmgrps == null) {
            authissueingadmgrps = roleManagementSession.getAuthorizedRoles(admin, AccessRulesConstants.HARDTOKEN_ISSUEHARDTOKENS);
        }
        return authissueingadmgrps;        	
    }

    public void clear(){      
	  hardtokenissuers=null;
	  hardtokenprofiles=null;
	  hardtokenprofilesnamemap=null;
	  authissueingadmgrps=null;
    }
}
