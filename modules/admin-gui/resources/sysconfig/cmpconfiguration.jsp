<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page pageEncoding="ISO-8859-1"%>
<% response.setContentType("text/html; charset="+org.ejbca.config.WebConfiguration.getWebContentEncoding()); %>
<%@page errorPage="/errorpage.jsp" import="java.util.*, org.ejbca.ui.web.admin.configuration.EjbcaWebBean,org.ejbca.config.GlobalConfiguration, 
				org.ejbca.core.model.SecConst, org.cesecore.authorization.AuthorizationDeniedException, org.ejbca.ui.web.RequestHelper,
				org.ejbca.ui.web.admin.cainterface.CAInterfaceBean, org.cesecore.certificates.certificateprofile.CertificateProfile, 
				org.ejbca.ui.web.admin.cainterface.CertificateProfileDataHandler, org.cesecore.certificates.certificateprofile.CertificateProfileExistsException, 
				org.cesecore.certificates.certificateprofile.CertificateProfileConstants, org.ejbca.ui.web.CertificateView, 
				org.cesecore.certificates.util.DNFieldExtractor, org.cesecore.certificates.util.DnComponents, 
				org.cesecore.certificates.certificate.certextensions.CertificateExtensionFactory, 
				org.cesecore.certificates.certificate.certextensions.AvailableCertificateExtension, org.cesecore.certificates.certificateprofile.CertificatePolicy,
                org.cesecore.certificates.ca.CAInfo, org.cesecore.util.ValidityDate, org.ejbca.ui.web.ParameterException, 
                org.cesecore.certificates.util.AlgorithmConstants, org.cesecore.certificates.certificate.CertificateConstants, 
                org.ejbca.core.model.authorization.AccessRulesConstants,org.ejbca.config.CmpConfiguration, org.ejbca.core.model.ra.UsernameGeneratorParams"%>
                
<%@page import="org.cesecore.util.YearMonthDayTime"%>
<html>
<jsp:useBean id="ejbcawebbean" scope="session" class="org.ejbca.ui.web.admin.configuration.EjbcaWebBean" />
<jsp:useBean id="cabean" scope="session" class="org.ejbca.ui.web.admin.cainterface.CAInterfaceBean" />

<%! // Declarations 


	static final String ACTION                              		= "action";
	static final String ACTION_EDIT_ALIAS                  		= "actioneditcmpalias";
	static final String ACTION_EDIT_ALIASES						= "actioneditcmpaliases";

	static final String TEXTFIELD_ALIAS                       	 	= "textfieldalias";
	static final String TEXTFIELD_CMP_RANAMEGENPARAM			  	= "textfieldcmpranamegenerationparameter";
	static final String TEXTFIELD_CMP_RANAMEGENPREFIX		  		= "textfieldcmpranamegenerationprefix";
	static final String TEXTFIELD_CMP_RANAMEGENPOSTFIX		  		= "textfieldcmpranamegenerationpostfix";
	static final String TEXTFIELD_CMP_RAPASSWORDGENPARAM		  	= "textfieldcmprapasswordgenerationparameter";
	static final String TEXTFIELD_HMACPASSWORD						= "textfieldhmacpassword";
	static final String TEXTFIELD_NESTEDMESSAGETRUSTEDCERTPATH		= "textfieldnestedmessagetrustedcertificatespath";
	
	static final String BUTTON_ADD_ALIAS						 	= "buttonaliasadd";
	static final String BUTTON_DELETE_ALIAS					 	= "buttondeletealias";
	static final String BUTTON_EDIT_ALIAS					 		= "buttoneditalias";
	static final String BUTTON_RENAME_ALIAS					 	= "buttonaliasrename";
	static final String BUTTON_CLONE_ALIAS						 	= "buttonaliasclone";
	static final String BUTTON_SAVE							 	= "buttonsave";
	static final String BUTTON_CANCEL							 	= "buttoncancel";
	static final String BUTTON_RELOAD								= "buttonreload";
	static final String BUTTON_ADDVENDORCA							= "buttonaddvendorca";
	static final String BUTTON_REMOVEVENDORCA						= "buttonremovevendorca";
	static final String BUTTON_ADD_NAMEGENPARAM_DN					= "buttonaddnamegenparamdn";
	static final String BUTTON_REMOVE_NAMEGENPARAM_DN				= "buttonremovenamegenparamdn";
	
	static final String RADIO_CMPMODE								= "radiocmpmode";
	static final String RADIO_NAMEGENSCHEME						= "radionnamegenscheme";
	static final String RADIO_HMACPASSWORD							= "radiohmacpassword";

	
	static final String CHECKBOX_CMP_VENDORMODE					= "checkcmpvendormode";
	static final String CHECKBOX_CMP_KUR_USEAUTOMATICKEYUPDATE  	= "checkboxcmpuseautomatickeyupdate";
	static final String CHECKBOX_CMP_KUR_USESAMEKEYS				= "checkboxcmpkurusesamekeys";
	static final String CHECKBOX_CMP_ALLOWRAVERIFYPOPO				= "checkboxcmpallowraverifypopo";
	static final String CHECKBOX_CMP_ALLOWCUSTOMSERNO				= "checkboxcmpallowcustomserno";
	static final String CHECKBOX_HMAC								= "checkboxhmac";
	static final String CHECKBOX_EEC								= "checkboxeec";
	static final String CHECKBOX_REGTOKEN							= "checkboxregtoken";
	static final String CHECKBOX_DNPART							= "checkboxdnpart";
	static final String CHECKBOX_OMITVERIFICATIONINECC				= "checkboxomitverificationsinecc";

	
	static final String LIST_CMPDEFAULTCAS					   		= "listcmpdefaultcas";
	static final String LIST_CMPRACAS						   		= "listcmpracas";
	static final String LIST_CMPRESPONSEPROTECTION		   		    = "listcmpresponseprotection";
	static final String LIST_CMPEEPROFILES					   		= "listcmpeeprofile";
	static final String LIST_CMPCERTPROFILES				   		= "listcmpcertprofiles";
	static final String LIST_ECCCAS								= "listecccas";
	static final String LIST_DNPARTS								= "listdnparts";
	static final String LIST_EXTRACTUSERNAMECOMP					= "listextractusernamecomp";
	static final String LIST_VENDORCA								= "listvendorca";
	static final String LIST_NAMEGENPARAM_DN						= "listnamegenparamdn";
		
	static final String SELECT_ALIASES                       		= "selectaliases";
	static final String HIDDEN_ALIAS                         		= "hiddenalias";
	static final String CHECKBOX_VALUE								= "true"; 
	 
	 
	List<String> dnfields = Arrays.asList("CN", "UID", "OU", "O", "L","ST", "DC", "C", "emailAddress",  "serialNumber", "givenName", "initials", "surname", "title", 
			   		"unstructuredAddress", "unstructuredName", "postalCode", "businessCaegory", "dnQualifier", "postalAddress", 
			   		"telephoneNumber", "pseudonym", "streetAddress", "name", "CIF", "NIF");


 

  // Declare Language file.
%>
<% 

  // Initialize environment
  String alias = null;
  String includefile = "cmpaliasespage.jspf"; 

  boolean  triedtoaddexistingalias    = false;
  boolean  aliasDeletionFailed = false;
  boolean  triedrenametoexistingalias = false;
  boolean  triedclonetoexistingalias = false;
  
  boolean ramode = false;
  boolean pbe = false;

  GlobalConfiguration gc = ejbcawebbean.initialize(request, AccessRulesConstants.ROLE_ADMINISTRATOR, AccessRulesConstants.REGULAR_EDITCERTIFICATEPROFILES); 
                                            cabean.initialize(ejbcawebbean); 
  
  CmpConfiguration cmpconfig = ejbcawebbean.getCMPConfiguration();

  String THIS_FILENAME            = gc.getAdminWebPath() +  "/sysconfig/cmpconfiguration.jsp";
  
  boolean issuperadministrator = false;
  try{
    issuperadministrator = ejbcawebbean.isAuthorizedNoLog("/super_administrator");
  }catch(AuthorizationDeniedException ade){}   

%>
 
<head>
  <title><c:out value="<%= gc.getEjbcaTitle() %>" /></title>
  <base href="<%= ejbcawebbean.getBaseUrl() %>" />
  <link rel="stylesheet" type="text/css" href="<%= ejbcawebbean.getCssFile() %>" />
  <script type="text/javascript" src="<%= gc.getAdminWebPath() %>ejbcajslib.js"></script>
</head>

<body>

<%
	// Determine action 
 	RequestHelper.setDefaultCharacterEncoding(request);

  	if( request.getParameter(ACTION) != null){
    		if( request.getParameter(ACTION).equals(ACTION_EDIT_ALIASES)){
    					
      				if( request.getParameter(BUTTON_EDIT_ALIAS) != null){
          					// Display  cmpaliaspage.jsp
         					alias = request.getParameter(SELECT_ALIASES);
         					if(alias != null){
           							if(!alias.trim().equals("")){
        	   								if(!cmpconfig.aliasExists(alias)) {
        	   										cmpconfig.addAlias(alias);
        	   								}
        	   								ejbcawebbean.setTempCmpConfig(null);
               								includefile="cmpaliaspage.jspf"; 
           							}
         					}
         					if(alias == null){   
          							includefile="cmpaliasespage.jspf";     
         					}
      				}
      			
      				if( request.getParameter(BUTTON_DELETE_ALIAS) != null) {
          					// Delete profile and display profilespage. 
          					alias = request.getParameter(SELECT_ALIASES);
          					if(alias != null && (!alias.trim().equals("")) ){
              						cmpconfig.removeAlias(alias);
                					ejbcawebbean.saveCMPConfiguration();
                					if(cmpconfig.aliasExists(alias)) {
                						aliasDeletionFailed = true;
                					}
          					}
          					includefile="cmpaliasespage.jspf";             
      				}

      				if( request.getParameter(BUTTON_RENAME_ALIAS) != null){ 
      						// Rename selected profile and display profilespage.
      					    String newalias = request.getParameter(TEXTFIELD_ALIAS);
      					    String oldalias = request.getParameter(SELECT_ALIASES);
      					    if(oldalias != null && newalias != null && !newalias.trim().equals("") && !oldalias.trim().equals("") ){
      					    		if(cmpconfig.aliasExists(newalias)) {
      					    				triedrenametoexistingalias = true;
      					    		} else {
      					    				cmpconfig.renameAlias(oldalias, newalias);
			      					    	ejbcawebbean.saveCMPConfiguration();
      					    		}
      					    }
      					    includefile="cmpaliasespage.jspf"; 
      				}
      				
      				if( request.getParameter(BUTTON_ADD_ALIAS) != null){
      						alias = request.getParameter(TEXTFIELD_ALIAS);
      					    if(alias != null && (!alias.trim().equals("")) ) {
      					    		if(cmpconfig.aliasExists(alias)) {
      					    			triedtoaddexistingalias = true;
      					    		} else {
      					    			cmpconfig.addAlias(alias);
      					    			ejbcawebbean.saveCMPConfiguration();
      					    		}
      					    }
      					    includefile="cmpaliasespage.jspf"; 
      				}
      				
      				if( request.getParameter(BUTTON_CLONE_ALIAS) != null){
      						// clone profile and display profilespage.
      					    String newalias = request.getParameter(TEXTFIELD_ALIAS);
      					    String oldalias = request.getParameter(SELECT_ALIASES);
      					    if(oldalias != null && newalias != null && !newalias.trim().equals("") && !oldalias.trim().equals("")){
      					    			if(cmpconfig.aliasExists(newalias)) {
      					    					triedclonetoexistingalias = true;
      					    			} else {
      					        				cmpconfig.cloneAlias(oldalias, newalias);
					      					    ejbcawebbean.saveCMPConfiguration();
      					    			}
      					    }
      					    includefile="cmpaliasespage.jspf"; 
      				}

    		} // if( request.getParameter(ACTION).equals(ACTION_EDIT_ALIASES))      				
      				
      				
      			
    
    		
    		if(request.getParameter(ACTION).equals(ACTION_EDIT_ALIAS)) {
		    	if(request.getParameter(BUTTON_CANCEL) != null) {
	    			// Don't save changes.
	            	//ejbcawebbean.clearCMPCache();
	            	ejbcawebbean.setTempCmpConfig(null);
	            	includefile="cmpaliasespage.jspf";
	    		} else {
    			
    				alias = request.getParameter(HIDDEN_ALIAS);
    		       	if(alias != null) {
    		       		if(!alias.trim().equals("")) {
    		       	
    		       			cmpconfig = ejbcawebbean.getTempCmpConfig();
    		       			if(cmpconfig == null) {
    		       					cmpconfig = ejbcawebbean.getCMPConfiguration();
    		       			}
    		
    		       						
    		       			//Save changes
    		       						
    		       			//defaultCA
    		       			String value = request.getParameter(LIST_CMPDEFAULTCAS);
    		       			if((value==null) || (value.length() == 0)) {
    		       					cmpconfig.setCMPDefaultCA(alias, "");
    		       			} else {
    		                		String cadn = cabean.getCAInfo(value).getCAInfo().getSubjectDN();
    		    					cmpconfig.setCMPDefaultCA(alias, cadn);
    		       			}
    		       						
    		    			//operational mode
    		    			String mode = request.getParameter(RADIO_CMPMODE);
    		    			if(mode!=null) {
    		    					if(mode.equals("client")) {
    		    							ramode = false;
    		    					} else if(mode.equals("ra")) {
    		    							ramode = true;
    		    					}
    		           		}
    		           		cmpconfig.setRAMode(alias, ramode);
    		           					
    		           		//response protection
    		    			value = request.getParameter(LIST_CMPRESPONSEPROTECTION);
    						cmpconfig.setResponseProtection(alias, value);
    						if(value.equals("pbe")) {
    								pbe = true;
    						} else {
    								pbe = false;
    						}
    									
    						// authentication module and parameters
    						// TODO fix it better
    			            ArrayList<String> authmodule = new ArrayList<String>();
    			            ArrayList<String> authparam = new ArrayList<String>();
    			            if(pbe && ramode) {
    			            		value = CmpConfiguration.AUTHMODULE_HMAC;
    			            } else {
    			            		value = request.getParameter(CHECKBOX_HMAC);
    			            }
    			            if(value !=null) {
    			            		authmodule.add(value);
    			            		if(ramode) {
    			            				value = request.getParameter(RADIO_HMACPASSWORD);
    			            				if((value != null) && value.equals("hmacsecret")) {
    												String secret = request.getParameter(TEXTFIELD_HMACPASSWORD);
    			            						if(secret != null) {
    			            								authparam.add(secret);
    			            						}
    			            				} else {
    			            						authparam.add("-");
    			            				}
    			            		} else {
    			            			authparam.add("-");
    			            		}
    			            }
    			            if(!pbe) {
    			            		value = request.getParameter(CHECKBOX_EEC);
    			            		if(value != null) {
    			            				authmodule.add(value);
    			            				authparam.add(ramode ? request.getParameter(LIST_ECCCAS) : "-");
    			            		}
    			            		if(!ramode) {
    			            				value = request.getParameter(CHECKBOX_REGTOKEN);
    			            				if(value != null) {
    			            						authmodule.add(value);
    			            						authparam.add("-");
    			            				}
    			            				value = request.getParameter(CHECKBOX_DNPART);
    			            				if(value != null) {
    			            						authmodule.add(value);
    			            						authparam.add(request.getParameter(LIST_DNPARTS));
    			            				}
    			            		}
    			            }
	    			        cmpconfig.setAuthenticationProperties(alias, authmodule, authparam);
    		
    			            			
	    			        
	    			        
    			            if(!ramode) { // client mode
    			            		// extract username component
    			            		value = request.getParameter(LIST_EXTRACTUSERNAMECOMP);
    			            		if(value != null){
    			            				cmpconfig.setExtractUsernameComponent(alias, value);
    			            		}
    			            		
    			            		// vendor mode
    			            		value = request.getParameter(CHECKBOX_CMP_VENDORMODE);
    			            		boolean vendormode = false;
    			            		if(value != null){
    			            				vendormode = true;
    			            		}
    			            		cmpconfig.setVendorMode(alias, vendormode);
    			            } else { // ra mode
    			            		// allow verify popo
    			            		value = request.getParameter(CHECKBOX_CMP_ALLOWRAVERIFYPOPO);
    			            		cmpconfig.setAllowRAVerifyPOPO(alias, (value != null));
    			            		
    			            		// ra name generation scheme	           					
    			           			String namegenscheme = request.getParameter(RADIO_NAMEGENSCHEME);
    			           			if(namegenscheme != null) {
    			           					cmpconfig.setRANameGenScheme(alias, namegenscheme);
    										if(namegenscheme.equals(UsernameGeneratorParams.FIXED)) {
    												value = request.getParameter(TEXTFIELD_CMP_RANAMEGENPARAM);
    												if((value != null) && (value.length() > 0)) {
    														cmpconfig.setRANameGenParams(alias, value);
    												}
											} else if(namegenscheme.equals(UsernameGeneratorParams.DN)) {
    												// do nothing here. handle it with the buttons
											} else { 
													cmpconfig.setRANameGenParams(alias, "");
											}
    			           			}
    			           			
    			           			// ra name generation prefix
    			            		value = request.getParameter(TEXTFIELD_CMP_RANAMEGENPREFIX);
    			            		cmpconfig.setRANameGenPrefix(alias, value == null ? "" : value);
    			            		
    			            		// ra name generation postfix
    			            		value = request.getParameter(TEXTFIELD_CMP_RANAMEGENPOSTFIX);
    			            		cmpconfig.setRANameGenPostfix(alias, value==null ? "" : value);
    			            		
    			            		// ra password generation parameters
    			            		value = request.getParameter(TEXTFIELD_CMP_RAPASSWORDGENPARAM);
    			            		cmpconfig.setRAPwdGenParams(alias, value==null ? "random" : value);
    			            		
    			            		// allow custom serno
    			            		value = request.getParameter(CHECKBOX_CMP_ALLOWCUSTOMSERNO);
    			            		cmpconfig.setAllowRACustomSerno(alias, (value != null));
    			            		
    			            		// ra endentity profile
    			            		value = request.getParameter(LIST_CMPEEPROFILES);
    			            		if(value != null){
    			            				cmpconfig.setRAEEProfile(alias, value);
    			            		}
    			            		
    			            		// ra certprofile
    			            		value = request.getParameter(LIST_CMPCERTPROFILES);
    			            		if(value != null) {
    			            				cmpconfig.setRACertProfile(alias, value);
    			            		}
    			            		
    			            		// ra CA  
    					 			value = request.getParameter(LIST_CMPRACAS);
    			     				if ( (value != null) && (value.trim().length() > 0) ) {
    			     					cmpconfig.setRACAName(alias, value);
    			     				}
    			     				
    			            } // if(ramode)
    			            
    			            	
    			            // KUR automatic keyupdate
    			            value = request.getParameter(CHECKBOX_CMP_KUR_USEAUTOMATICKEYUPDATE);
    			            cmpconfig.setKurAllowAutomaticUpdate(alias, (value != null));
    			            
    			            // KUR update with same key
    			            value = request.getParameter(CHECKBOX_CMP_KUR_USESAMEKEYS);
    			            cmpconfig.setKurAllowSameKey(alias, (value != null));
    			            
    			            
    			            
    			            // Nested message content
    			            value = request.getParameter(TEXTFIELD_NESTEDMESSAGETRUSTEDCERTPATH);
    			            cmpconfig.setRACertPath(alias, value == null ? "" : value);
    			            
    			            // Nested message content - omit some verifications in EndEntityCertificate authentication module
    			            value = request.getParameter(CHECKBOX_OMITVERIFICATIONINECC);
    			            cmpconfig.setOmitVerificationsInECC(alias, (value != null));
    			            
    			            
    			            
    			            // ------------------- BUTTONS -------------------------
    			            if(request.getParameter(BUTTON_RELOAD) != null) {
    			            		includefile = "cmpaliaspage.jspf";
    			            }
    			            
    			            if(request.getParameter(BUTTON_ADDVENDORCA) != null) {
    			            		if(request.getParameter(CHECKBOX_CMP_VENDORMODE) != null) {
    			            				value = request.getParameter(LIST_VENDORCA);
    			            				String vendorcas = cmpconfig.getVendorCA(alias);
    			            				String[] vcas = vendorcas.split(";");
    			            				boolean present = false;
    			            				for(String vca : vcas) {
    			            						if(vca.equals(value)) {
    			            								present = true;
    			            								break;
    			            						}
    			            				}
    			            				if(!present) {
    			            						vendorcas += (vendorcas.length() > 0 ? ";" : "") + value;
    				            					cmpconfig.setVendorCA(alias, vendorcas);
    			            				}
    			            		} else {
    			            				cmpconfig.setVendorCA(alias, "");
    			            		}
    			            		includefile = "cmpaliaspage.jspf";
    			            }
    			            
    			            if(request.getParameter(BUTTON_REMOVEVENDORCA) != null) {
    			            		value = request.getParameter(LIST_VENDORCA);
    			            		String vendorcas = cmpconfig.getVendorCA(alias);
    			            		String[] cas = vendorcas.split(";");
    			            		ArrayList<String> vcas = new ArrayList<String>();
    			            		for(String ca : cas) {
    			            				if(!ca.equals(value)) {
    			            						vcas.add(ca);
    			            				}
    			            		}
    			            		if(vcas.size() > 0) { 
    			            				cmpconfig.setVendorCA(alias, vcas);
    			            		} else {
    			            				cmpconfig.setVendorCA(alias, "");
    			            		}
    			            		includefile = "cmpaliaspage.jspf";
    			            }
    			            
    			            if(request.getParameter(BUTTON_ADD_NAMEGENPARAM_DN)!= null) {
    			            		if(request.getParameter(RADIO_NAMEGENSCHEME).equals(UsernameGeneratorParams.DN)) {
    			            				value = request.getParameter(LIST_NAMEGENPARAM_DN);
    			            				String namegenparam = cmpconfig.getRANameGenParams(alias);
    			            				String[] params = namegenparam.split(";");
    			            				if((params.length > 0) && ( dnfields.contains(params[0]) )) {
    			            						boolean present = false;
    			            						for(String p : params) {
    			            								if(p.equals(value)) {
    			            										present = true;
    			            										break;
    			            								}
    			            						}
    			            						if(!present)	namegenparam += ";" + value;
    			            				} else {
    			            						namegenparam = value;
    			            				}
    										cmpconfig.setRANameGenParams(alias, namegenparam);
    			            		}
    								includefile = "cmpaliaspage.jspf";
    			            }
    			            			
    			            if(request.getParameter(BUTTON_REMOVE_NAMEGENPARAM_DN) != null) {
    			            		value = request.getParameter(LIST_NAMEGENPARAM_DN);
    			            		String namegenparam = cmpconfig.getRANameGenParams(alias);
    			            		String[] params = namegenparam.split(";");
    			            		String newparams = "";
    			            		for(String p : params) {
    			            				if(!p.equals(value)) {
    			            						newparams += ";" + p;
    			            				}
    			            		}
    			            		if(newparams.length() > 0) { 
    			            				newparams = newparams.substring(1);
    			            		}
    			            		cmpconfig.setRANameGenParams(alias, newparams);
    			            		includefile = "cmpaliaspage.jspf";
    			            }
    			            			
    			            if(request.getParameter(BUTTON_SAVE) != null) {
    			            		ejbcawebbean.saveCMPConfiguration();
    			                	ejbcawebbean.setTempCmpConfig(null);
    			              		includefile="cmpaliasespage.jspf";
    			            }
    		       		} // if(!alias.trim().equals(""))
    			            			

    									
    		       	} // if((alias != null) )
	    		} // if(request.getParameter(BUTTON_CANCEL) != null) else
    		   } // if(request.getParameter(ACTION).equals(ACTION_EDIT_ALIAS))

  		} // if( request.getParameter(ACTION) != null)

 // Include page
  if( includefile.equals("cmpaliaspage.jspf")){
%>
   <%@ include file="cmpaliaspage.jspf" %>
<%}
  if( includefile.equals("cmpaliasespage.jspf")){ %>
   <%@ include file="cmpaliasespage.jspf" %> 
<%}

   // Include Footer 
   String footurl =   gc.getFootBanner(); %>
   
  <jsp:include page="<%= footurl %>" />

</body>
</html>
