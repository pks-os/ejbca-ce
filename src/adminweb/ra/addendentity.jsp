<%@ page pageEncoding="ISO-8859-1"%>
<%@ page contentType="text/html; charset=@page.encoding@" %>
<%@page  errorPage="/errorpage.jsp" import="java.util.*, org.ejbca.ui.web.admin.configuration.EjbcaWebBean,org.ejbca.core.model.ra.raadmin.GlobalConfiguration, org.ejbca.ui.web.admin.rainterface.UserView,
    org.ejbca.ui.web.RequestHelper,org.ejbca.ui.web.admin.rainterface.RAInterfaceBean, org.ejbca.ui.web.admin.rainterface.EndEntityProfileDataHandler, org.ejbca.core.model.ra.raadmin.EndEntityProfile, org.ejbca.core.model.ra.UserDataConstants,
                 javax.ejb.CreateException, java.rmi.RemoteException, org.ejbca.util.dn.DNFieldExtractor, org.ejbca.core.model.ra.UserDataVO, org.ejbca.ui.web.admin.hardtokeninterface.HardTokenInterfaceBean, 
                 org.ejbca.core.model.hardtoken.HardTokenIssuer, org.ejbca.core.model.hardtoken.HardTokenIssuerData,   org.ejbca.core.model.SecConst, org.ejbca.util.StringTools, org.ejbca.util.dn.DnComponents,
                 java.text.DateFormat, org.ejbca.core.model.ra.ExtendedInformation" %>
<html> 
<jsp:useBean id="ejbcawebbean" scope="session" class="org.ejbca.ui.web.admin.configuration.EjbcaWebBean" />
<jsp:useBean id="rabean" scope="session" class="org.ejbca.ui.web.admin.rainterface.RAInterfaceBean" />
<jsp:useBean id="tokenbean" scope="session" class="org.ejbca.ui.web.admin.hardtokeninterface.HardTokenInterfaceBean" />
<%! // Declarations

  static final String ACTION                   = "action";
  static final String ACTION_ADDUSER           = "adduser";
  static final String ACTION_CHANGEPROFILE     = "changeprofile";

  static final String BUTTON_ADDUSER          = "buttonadduser"; 
  static final String BUTTON_RESET            = "buttonreset"; 
  static final String BUTTON_RELOAD           = "buttonreload";

  static final String TEXTFIELD_USERNAME          = "textfieldusername";
  static final String TEXTFIELD_PASSWORD          = "textfieldpassword";
  static final String TEXTFIELD_CONFIRMPASSWORD   = "textfieldconfirmpassword";
  static final String TEXTFIELD_SUBJECTDN         = "textfieldsubjectdn";
  static final String TEXTFIELD_SUBJECTALTNAME    = "textfieldsubjectaltname";
  static final String TEXTFIELD_SUBJECTDIRATTR    = "textfieldsubjectdirattr";
  static final String TEXTFIELD_EMAIL             = "textfieldemail";
  static final String TEXTFIELD_EMAILDOMAIN       = "textfieldemaildomain";
  static final String TEXTFIELD_UPNNAME           = "textfieldupnname";
  static final String TEXTFIELD_RFC822NAME        = "textfieldrfc822name";
  static final String TEXTFIELD_STARTTIME         = "textfieldstarttime";
  static final String TEXTFIELD_ENDTIME           = "textfieldendtime";

  static final String SELECT_ENDENTITYPROFILE     = "selectendentityprofile";
  static final String SELECT_CERTIFICATEPROFILE   = "selectcertificateprofile";
  static final String SELECT_TOKEN                = "selecttoken";
  static final String SELECT_USERNAME             = "selectusername";
  static final String SELECT_PASSWORD             = "selectpassword";
  static final String SELECT_CONFIRMPASSWORD      = "selectconfirmpassword";
  static final String SELECT_SUBJECTDN            = "selectsubjectdn";
  static final String SELECT_SUBJECTALTNAME       = "selectsubjectaltname";
  static final String SELECT_SUBJECTDIRATTR       = "selectsubjectaldirattr";
  static final String SELECT_EMAILDOMAIN          = "selectemaildomain";
  static final String SELECT_HARDTOKENISSUER      = "selecthardtokenissuer";
  static final String SELECT_CA                   = "selectca";
  static final String SELECT_ALLOWEDREQUESTS      = "selectallowedrequests"; 

  static final String CHECKBOX_CLEARTEXTPASSWORD          = "checkboxcleartextpassword";
  static final String CHECKBOX_SUBJECTDN                  = "checkboxsubjectdn";
  static final String CHECKBOX_SUBJECTALTNAME             = "checkboxsubjectaltname";
  static final String CHECKBOX_SUBJECTDIRATTR             = "checkboxsubjectdirattr";
  static final String CHECKBOX_ADMINISTRATOR              = "checkboxadministrator";
  static final String CHECKBOX_KEYRECOVERABLE             = "checkboxkeyrecoverable";
  static final String CHECKBOX_SENDNOTIFICATION           = "checkboxsendnotification";
  static final String CHECKBOX_PRINT                      = "checkboxprint";

  static final String CHECKBOX_REQUIRED_USERNAME          = "checkboxrequiredusername";
  static final String CHECKBOX_REQUIRED_PASSWORD          = "checkboxrequiredpassword";
  static final String CHECKBOX_REQUIRED_CLEARTEXTPASSWORD = "checkboxrequiredcleartextpassword";
  static final String CHECKBOX_REQUIRED_SUBJECTDN         = "checkboxrequiredsubjectdn";
  static final String CHECKBOX_REQUIRED_SUBJECTALTNAME    = "checkboxrequiredsubjectaltname";
  static final String CHECKBOX_REQUIRED_SUBJECTDIRATTR    = "checkboxrequiredsubjectdirattr";
  static final String CHECKBOX_REQUIRED_EMAIL             = "checkboxrequiredemail";
  static final String CHECKBOX_REQUIRED_ADMINISTRATOR     = "checkboxrequiredadministrator";
  static final String CHECKBOX_REQUIRED_KEYRECOVERABLE    = "checkboxrequiredkeyrecoverable";
  static final String CHECKBOX_REQUIRED_STARTTIME         = "checkboxrequiredstarttime";
  static final String CHECKBOX_REQUIRED_ENDTIME           = "checkboxrequiredendtime";

  static final String CHECKBOX_VALUE             = "true";

  static final String USER_PARAMETER           = "username";
  static final String SUBJECTDN_PARAMETER      = "subjectdnparameter";



  static final String HIDDEN_USERNAME           = "hiddenusername";
  static final String HIDDEN_PROFILE            = "hiddenprofile";

%><%
  // Initialize environment.

  GlobalConfiguration globalconfiguration = ejbcawebbean.initialize(request,"/ra_functionality/create_end_entity"); 
                                            rabean.initialize(request, ejbcawebbean);
                                            if(globalconfiguration.getIssueHardwareTokens())
                                              tokenbean.initialize(request, ejbcawebbean);

  final String VIEWUSER_LINK            = ejbcawebbean.getBaseUrl() + globalconfiguration.getRaPath()  + "/viewendentity.jsp";
  final String EDITUSER_LINK            = ejbcawebbean.getBaseUrl() + globalconfiguration.getRaPath()  + "/editendentity.jsp";

  String THIS_FILENAME             =  globalconfiguration.getRaPath()  + "/addendentity.jsp";
  EndEntityProfile  profile        = null;
  String[] profilenames            = null; 
  boolean noprofiles               = false; 
  int profileid = 0;


  profilenames                  = (String[]) ejbcawebbean.getInformationMemory().getCreateAuthorizedEndEntityProfileNames().keySet().toArray(new String[0]);


  if(profilenames== null || profilenames.length == 0) 
     noprofiles=true;
  else 
    profileid = rabean.getEndEntityProfileId(profilenames[0]);

  boolean chooselastprofile = false;
  if(ejbcawebbean.getLastEndEntityProfile() != 0 && rabean.getEndEntityProfileName(ejbcawebbean.getLastEndEntityProfile()) != null){
    for(int i=0 ; i< profilenames.length; i++){
       if(rabean.getEndEntityProfileName(ejbcawebbean.getLastEndEntityProfile()).equals(profilenames[i]))
         chooselastprofile=true;
    }
  }

  if(!noprofiles){
    if(!chooselastprofile)
      profileid = rabean.getEndEntityProfileId(profilenames[0]);
    else
      profileid = ejbcawebbean.getLastEndEntityProfile();
  } 

  boolean userexists               = false;
  boolean useradded                = false;
  boolean useoldprofile            = false;
  boolean usehardtokenissuers      = false;
  boolean usekeyrecovery           = false;
  boolean issuperadministrator     = false;

  try{
    issuperadministrator = ejbcawebbean.isAuthorizedNoLog("/super_administrator");
  }catch(org.ejbca.core.model.authorization.AuthorizationDeniedException ade){}   

 
  EndEntityProfile oldprofile      = null;
  String addedusername             = ""; 

  String approvalmessage           = null;
  String oldemail = "";
  String lastselectedusername           = "";
  String lastselectedpassword           = "";
  String lastselectedemaildomain        = "";
  String lastselectedcertificateprofile = "";
  String lastselectedtoken              = "";
  String lastselectedca                  = "";
  int lastselectedhardtokenissuer       = 1;

  String[] lastselectedsubjectdns       =null;
  String[] lastselectedsubjectaltnames  =null;  
  String[] lastselectedsubjectdirattrs  =null;  
  int[] fielddata = null;

  HashMap caidtonamemap = ejbcawebbean.getInformationMemory().getCAIdToNameMap();

  RequestHelper.setDefaultCharacterEncoding(request);

  if( request.getParameter(ACTION) != null){
    if(request.getParameter(ACTION).equals(ACTION_CHANGEPROFILE)){
      profileid = Integer.parseInt(request.getParameter(SELECT_ENDENTITYPROFILE)); 
      ejbcawebbean.setLastEndEntityProfile(profileid);
    }
    if( request.getParameter(ACTION).equals(ACTION_ADDUSER)){
      if( request.getParameter(BUTTON_ADDUSER) != null){
         UserView newuser = new UserView();
         int oldprofileid = UserDataVO.NO_ENDENTITYPROFILE;
 
         // Get previous chosen profile.
         String hiddenprofileid = request.getParameter(HIDDEN_PROFILE); 
         oldprofileid = Integer.parseInt(hiddenprofileid);       
         if(globalconfiguration.getEnableEndEntityProfileLimitations()){
           // Check that adminsitrator is authorized to given profileid
           boolean authorizedtoprofile = false;
           for(int i=0 ; i< profilenames.length; i++){
             if(oldprofileid == rabean.getEndEntityProfileId(profilenames[i]))
               authorizedtoprofile=true;
           }
           if(!authorizedtoprofile)
             throw new Exception("Error when trying to add user to non authorized profile");
         }
         

         oldprofile = rabean.getEndEntityProfile(oldprofileid);
         lastselectedsubjectdns       = new String[oldprofile.getSubjectDNFieldOrderLength()];
         lastselectedsubjectaltnames  = new String[oldprofile.getSubjectAltNameFieldOrderLength()];
         lastselectedsubjectdirattrs  = new String[oldprofile.getSubjectDirAttrFieldOrderLength()];
         newuser.setEndEntityProfileId(oldprofileid);         

         String value = request.getParameter(TEXTFIELD_USERNAME);
         if(value !=null){
           value=value.trim(); 
           if(!value.equals("")){
             newuser.setUsername(value);
             oldprofile.setValue(EndEntityProfile.USERNAME,0,value);
             addedusername = value;
           }
         }

         value = request.getParameter(SELECT_USERNAME);
          if(value !=null){
           if(!value.equals("")){
             newuser.setUsername(value);
             lastselectedusername = value;
             addedusername = value;
           }
         } 

         value = request.getParameter(TEXTFIELD_PASSWORD);
         if(value !=null){
           value=value.trim(); 
           if(!value.equals("")){
             newuser.setPassword(value);
             oldprofile.setValue(EndEntityProfile.PASSWORD, 0, value);            
           }
         }

         value = request.getParameter(SELECT_PASSWORD);
          if(value !=null){
           if(!value.equals("")){
             newuser.setPassword(value);
             lastselectedpassword = value;
           }
         } 

         value = request.getParameter(CHECKBOX_CLEARTEXTPASSWORD);
         if(value !=null){
           if(value.equals(CHECKBOX_VALUE)){
             newuser.setClearTextPassword(true);
             oldprofile.setValue(EndEntityProfile.CLEARTEXTPASSWORD, 0, EndEntityProfile.TRUE);             
           }
           else{
               newuser.setClearTextPassword(false);
               oldprofile.setValue(EndEntityProfile.CLEARTEXTPASSWORD, 0, EndEntityProfile.FALSE);    
             }
           }

 
           value = request.getParameter(TEXTFIELD_EMAIL);
           if(value !=null){
             value=value.trim(); 
             oldemail = value;
             if(!value.equals("")){
               String emaildomain = request.getParameter(TEXTFIELD_EMAILDOMAIN);
               if(emaildomain !=null){
                 emaildomain=emaildomain.trim(); 
                 if(!emaildomain.equals("")){
                   newuser.setEmail(value + "@" + emaildomain);
                   oldprofile.setValue(EndEntityProfile.EMAIL, 0,  emaildomain); 
                 }
               }

               emaildomain = request.getParameter(SELECT_EMAILDOMAIN);
               if(emaildomain !=null){
                 if(!emaildomain.equals("")){
                   newuser.setEmail(value + "@" + emaildomain);
                   lastselectedemaildomain = emaildomain;
                 }
               }
             }
           }

           String subjectdn = "";
           int numberofsubjectdnfields = oldprofile.getSubjectDNFieldOrderLength();
           for(int i=0; i < numberofsubjectdnfields; i++){
             value=null;
             fielddata = oldprofile.getSubjectDNFieldsInOrder(i); 

             if (!EndEntityProfile.isFieldOfType(fielddata[EndEntityProfile.FIELDTYPE], DnComponents.DNEMAIL))
               value = request.getParameter(TEXTFIELD_SUBJECTDN+i);
             else{
               if ( oldprofile.isRequired(fielddata[EndEntityProfile.FIELDTYPE],fielddata[EndEntityProfile.NUMBER]) ||
               		(request.getParameter(CHECKBOX_SUBJECTDN+i)!=null &&
               		request.getParameter(CHECKBOX_SUBJECTDN+i).equals(CHECKBOX_VALUE)) )
                   value = newuser.getEmail();
             }
             if(value !=null){
               value= value.trim(); 
               if(!value.equals("")){
                 oldprofile.setValue(fielddata[EndEntityProfile.FIELDTYPE],fielddata[EndEntityProfile.NUMBER], value);   
                 value = org.ietf.ldap.LDAPDN.escapeRDN(DNFieldExtractor.getFieldComponent(DnComponents.profileIdToDnId(fielddata[EndEntityProfile.FIELDTYPE]), DNFieldExtractor.TYPE_SUBJECTDN) +value);  
                 if(subjectdn.equals(""))
                   subjectdn = value;
                 else
                   subjectdn += ", " + value;
                   
               }
             }
             value = request.getParameter(SELECT_SUBJECTDN+i);
             if(value !=null){
               if(!value.equals("")){
                 lastselectedsubjectdns[i] = value;
                 value = org.ietf.ldap.LDAPDN.escapeRDN(DNFieldExtractor.getFieldComponent(DnComponents.profileIdToDnId(fielddata[EndEntityProfile.FIELDTYPE]), DNFieldExtractor.TYPE_SUBJECTDN) +value);
                 if(subjectdn.equals(""))
                   subjectdn = value;
                 else
                   subjectdn += ", " + value;
                 
               }
             }
           }      

           newuser.setSubjectDN(subjectdn);

           String subjectaltname = "";
           int numberofsubjectaltnamefields = oldprofile.getSubjectAltNameFieldOrderLength();
           for(int i=0; i < numberofsubjectaltnamefields; i++){
             fielddata = oldprofile.getSubjectAltNameFieldsInOrder(i); 
             value=null;               
             if ( EndEntityProfile.isFieldOfType(fielddata[EndEntityProfile.FIELDTYPE], DnComponents.RFC822NAME) ) {
             	if ( oldprofile.getUse(fielddata[EndEntityProfile.FIELDTYPE],fielddata[EndEntityProfile.NUMBER]) ) {
					if ( oldprofile.isRequired(fielddata[EndEntityProfile.FIELDTYPE],fielddata[EndEntityProfile.NUMBER]) ||
						(request.getParameter(CHECKBOX_SUBJECTALTNAME+i) != null &&
						request.getParameter(CHECKBOX_SUBJECTALTNAME+i).equals(CHECKBOX_VALUE)) ) {
						value = newuser.getEmail();
					}
             	} else {
					if ( request.getParameter(TEXTFIELD_SUBJECTALTNAME+i) != null && !request.getParameter(TEXTFIELD_SUBJECTALTNAME+i).equals("") &&
							request.getParameter(TEXTFIELD_RFC822NAME+i) != null && !request.getParameter(TEXTFIELD_RFC822NAME+i).equals("") ) {
						value = request.getParameter(TEXTFIELD_RFC822NAME+i) + "@" + request.getParameter(TEXTFIELD_SUBJECTALTNAME+i);
					}
             	}
             } else {
               if(EndEntityProfile.isFieldOfType(fielddata[EndEntityProfile.FIELDTYPE], DnComponents.UPN)){
                  if(request.getParameter(TEXTFIELD_SUBJECTALTNAME+i) != null && !request.getParameter(TEXTFIELD_SUBJECTALTNAME+i).equals("") 
		             && request.getParameter(TEXTFIELD_UPNNAME+i) != null && !request.getParameter(TEXTFIELD_UPNNAME+i).equals("")){
                    value = request.getParameter(TEXTFIELD_UPNNAME+i) + "@" + 
                            request.getParameter(TEXTFIELD_SUBJECTALTNAME+i);
                  }
               }else{
                 value = request.getParameter(TEXTFIELD_SUBJECTALTNAME+i);
               }
             }
             if(value !=null){
               value=value.trim(); 
               if(!value.equals("")){
                 oldprofile.setValue(fielddata[EndEntityProfile.FIELDTYPE],fielddata[EndEntityProfile.NUMBER], value);   
                 value = org.ietf.ldap.LDAPDN.escapeRDN(DNFieldExtractor.getFieldComponent(DnComponents.profileIdToDnId(fielddata[EndEntityProfile.FIELDTYPE]), DNFieldExtractor.TYPE_SUBJECTALTNAME) +value);
                 if(subjectaltname.equals(""))
                   subjectaltname = value;
                 else
                   subjectaltname += ", " +value;

               }
             }
             value = request.getParameter(SELECT_SUBJECTALTNAME+i);
             if(value !=null){
               if(!value.equals("")){
                 lastselectedsubjectaltnames[i] = value;
	             if ( EndEntityProfile.isFieldOfType(fielddata[EndEntityProfile.FIELDTYPE], DnComponents.RFC822NAME) ) {
	             	if ( !oldprofile.getUse(fielddata[EndEntityProfile.FIELDTYPE],fielddata[EndEntityProfile.NUMBER]) ) {
						if ( request.getParameter(SELECT_SUBJECTALTNAME+i) != null && !request.getParameter(SELECT_SUBJECTALTNAME+i).equals("") &&
								request.getParameter(TEXTFIELD_RFC822NAME+i) != null && !request.getParameter(TEXTFIELD_RFC822NAME+i).equals("") ) {
							value = request.getParameter(TEXTFIELD_RFC822NAME+i) + "@" + value;
						} else {
							value = null;
						}
	             	}
	             }
                 if(EndEntityProfile.isFieldOfType(fielddata[EndEntityProfile.FIELDTYPE], DnComponents.UPN)){
                   if(request.getParameter(TEXTFIELD_UPNNAME+i) != null){
                     value = request.getParameter(TEXTFIELD_UPNNAME+i)+ "@" + value;
                   } 
                 }
                 if ( value != null ) {
	                 value = org.ietf.ldap.LDAPDN.escapeRDN(DNFieldExtractor.getFieldComponent(DnComponents.profileIdToDnId(fielddata[EndEntityProfile.FIELDTYPE]), DNFieldExtractor.TYPE_SUBJECTALTNAME) +value);
	                 if(subjectaltname.equals(""))
    	               subjectaltname = value;
        	         else
            	       subjectaltname += ", " + value;
                 }
              }
             }
           }
           newuser.setSubjectAltName(subjectaltname);
 
           String subjectdirattr = "";
           int numberofsubjectdirattrfields = oldprofile.getSubjectDirAttrFieldOrderLength();
           for(int i=0; i < numberofsubjectdirattrfields; i++){
               fielddata = oldprofile.getSubjectDirAttrFieldsInOrder(i); 
               value = request.getParameter(TEXTFIELD_SUBJECTDIRATTR+i);
             if(value !=null){
               value=value.trim(); 
               if(!value.equals("")){
                 oldprofile.setValue(fielddata[EndEntityProfile.FIELDTYPE],fielddata[EndEntityProfile.NUMBER], value);   
                 value = org.ietf.ldap.LDAPDN.escapeRDN(DNFieldExtractor.getFieldComponent(DnComponents.profileIdToDnId(fielddata[EndEntityProfile.FIELDTYPE]), DNFieldExtractor.TYPE_SUBJECTDIRATTR) +value);
                 if(subjectdirattr.equals(""))
                   subjectdirattr = value;
                 else
                   subjectdirattr += ", " +value;

               }
             }
             value = request.getParameter(SELECT_SUBJECTDIRATTR+i);
             if(value !=null){
               if(!value.equals("")){
                 lastselectedsubjectdirattrs[i] = value;
                 value = org.ietf.ldap.LDAPDN.escapeRDN(DNFieldExtractor.getFieldComponent(DnComponents.profileIdToDnId(fielddata[EndEntityProfile.FIELDTYPE]), DNFieldExtractor.TYPE_SUBJECTDIRATTR) +value);
                 if(subjectdirattr.equals(""))
                   subjectdirattr = value;
                 else
                   subjectdirattr += ", " + value;
                 
              }
             }
           }
           newuser.setSubjectDirAttributes(subjectdirattr);

           value = request.getParameter(SELECT_ALLOWEDREQUESTS);
           if(value !=null){
          	 ExtendedInformation ei = newuser.getExtendedInformation();
           	 ei.setCustomData(ExtendedInformation.CUSTOM_REQUESTCOUNTER, value);
           	 newuser.setExtendedInformation(ei);
           }

           value = request.getParameter(CHECKBOX_ADMINISTRATOR);
           if(value !=null){
             if(value.equals(CHECKBOX_VALUE)){
               newuser.setAdministrator(true);   
               oldprofile.setValue(EndEntityProfile.ADMINISTRATOR, 0, EndEntityProfile.TRUE);  
             }
             else{
               newuser.setAdministrator(false);  
               oldprofile.setValue(EndEntityProfile.ADMINISTRATOR, 0, EndEntityProfile.FALSE); 
             }
           }
           value = request.getParameter(CHECKBOX_KEYRECOVERABLE);
           if(value !=null){
             if(value.equals(CHECKBOX_VALUE)){
               newuser.setKeyRecoverable(true);   
               oldprofile.setValue(EndEntityProfile.KEYRECOVERABLE, 0, EndEntityProfile.TRUE);                          
             }
             else{
               newuser.setKeyRecoverable(false);   
               oldprofile.setValue(EndEntityProfile.KEYRECOVERABLE, 0, EndEntityProfile.FALSE);               
             }
           }  
           value = request.getParameter(CHECKBOX_SENDNOTIFICATION);
           if(value !=null){
             if(value.equals(CHECKBOX_VALUE)){
               newuser.setSendNotification(true);   
               oldprofile.setValue(EndEntityProfile.SENDNOTIFICATION, 0, EndEntityProfile.TRUE);                          
             }
             else{
               newuser.setSendNotification(false);   
               oldprofile.setValue(EndEntityProfile.SENDNOTIFICATION, 0, EndEntityProfile.FALSE);               
             }
           } 
           value = request.getParameter(CHECKBOX_PRINT);
           if(value !=null){
             if(value.equals(CHECKBOX_VALUE)){
               newuser.setPrintUserData(true);   
               oldprofile.setPrintingDefault(true);                          
             }
             else{
               newuser.setPrintUserData(false);   
               oldprofile.setPrintingDefault(false);               
             }
           }            
            
           
           

           value = request.getParameter(SELECT_CERTIFICATEPROFILE);
           newuser.setCertificateProfileId(Integer.parseInt(value));   
           oldprofile.setValue(EndEntityProfile.DEFAULTCERTPROFILE, 0, value);         
           lastselectedcertificateprofile = value;

           value = request.getParameter(SELECT_CA);
           newuser.setCAId(Integer.parseInt(value));   
           oldprofile.setValue(EndEntityProfile.DEFAULTCA, 0, value);         
           lastselectedca = value;

           value = request.getParameter(SELECT_TOKEN);
           int tokentype = Integer.parseInt(value); 
           newuser.setTokenType(tokentype);   
           oldprofile.setValue(EndEntityProfile.DEFKEYSTORE, 0, value);         
           lastselectedtoken = value;

           int hardtokenissuer = SecConst.NO_HARDTOKENISSUER;
           if(tokentype > SecConst.TOKEN_SOFT && request.getParameter(SELECT_HARDTOKENISSUER) != null){
             value = request.getParameter(SELECT_HARDTOKENISSUER);
             hardtokenissuer = Integer.parseInt(value);  
             oldprofile.setValue(EndEntityProfile.DEFAULTTOKENISSUER, 0, value);  
           }
           lastselectedhardtokenissuer = hardtokenissuer;
           newuser.setHardTokenIssuerId(lastselectedhardtokenissuer);   
       
			if ( oldprofile.getUse(EndEntityProfile.STARTTIME, 0) ) {
				value = request.getParameter(TEXTFIELD_STARTTIME);
				if ( value != null ) {
					String storeValue = value;
					value = value.trim();
					if ( value.equals("") ) {
						value = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, ejbcawebbean.getLocale()).format(new Date());
						storeValue = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.US).format(new Date());
					} else if ( !value.matches("^\\d+:\\d?\\d:\\d?\\d$") ) {
						storeValue = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.US).format(
							DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, ejbcawebbean.getLocale()).parse(value));
	        		}
					ExtendedInformation ei = newuser.getExtendedInformation();
					if ( ei == null ) {
						ei = new ExtendedInformation();
					}
					ei.setCustomData(EndEntityProfile.STARTTIME, storeValue);
					newuser.setExtendedInformation(ei);
					oldprofile.setValue(EndEntityProfile.STARTTIME, 0, value);
				}
			}
			if ( oldprofile.getUse(EndEntityProfile.ENDTIME, 0) ) {
				value = request.getParameter(TEXTFIELD_ENDTIME);
				if ( value != null ) {
					String storeValue = value;
					value = value.trim();
					if ( value.equals("") ) {
						value = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, ejbcawebbean.getLocale()).format(new Date(Long.MAX_VALUE));
						storeValue = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.US).format(new Date(Long.MAX_VALUE));
					} else if ( !value.matches("^\\d+:\\d?\\d:\\d?\\d$") ) {
						storeValue = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.US).format(
							DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, ejbcawebbean.getLocale()).parse(value));
	        		}
					ExtendedInformation ei = newuser.getExtendedInformation();
					if ( ei == null ) {
						ei = new ExtendedInformation();
					}
					ei.setCustomData(EndEntityProfile.ENDTIME, storeValue);
					newuser.setExtendedInformation(ei);
					oldprofile.setValue(EndEntityProfile.ENDTIME, 0, value);
				}
			}

           // See if user already exists
           if(rabean.userExist(newuser.getUsername())){
             userexists = true;
             useoldprofile = true;   
           } else{
             if( request.getParameter(BUTTON_RELOAD) != null ){
              useoldprofile = true;   
             }else{
               try{
                 rabean.addUser(newuser); 
                 useradded=true;
               }catch(org.ejbca.core.model.approval.ApprovalException e){
            	   approvalmessage = ejbcawebbean.getText("THEREALREADYEXISTSAPPROVAL");
               }catch(org.ejbca.core.model.approval.WaitingForApprovalException e){
            	   approvalmessage = ejbcawebbean.getText("REQHAVEBEENADDEDFORAPPR");
               }
               
             }
           }
         }
      }
    }

    int numberofrows = ejbcawebbean.getEntriesPerPage();
    UserView[] addedusers = rabean.getAddedUsers(numberofrows);
    int row = 0;
    int tabindex = 0;
  
    if(!noprofiles){
      if(!useoldprofile){
        profile = rabean.getEndEntityProfile(profileid);
        oldemail = "";
      }else
        profile = oldprofile;
    }else
        profile = new EndEntityProfile();



     String[] tokentexts = RAInterfaceBean.tokentexts;
     int[] tokenids = RAInterfaceBean.tokenids;

     if(globalconfiguration.getIssueHardwareTokens()){
        TreeMap hardtokenprofiles = ejbcawebbean.getInformationMemory().getHardTokenProfiles();

        tokentexts = new String[RAInterfaceBean.tokentexts.length + hardtokenprofiles.keySet().size()];
        tokenids   = new int[tokentexts.length];
        for(int i=0; i < RAInterfaceBean.tokentexts.length; i++){
          tokentexts[i]= RAInterfaceBean.tokentexts[i];
          tokenids[i] = RAInterfaceBean.tokenids[i];
        }

        Iterator iter = hardtokenprofiles.keySet().iterator();
        int index=0;
        while(iter.hasNext()){       
          String name = (String) iter.next();
          tokentexts[index+RAInterfaceBean.tokentexts.length]= name;
          tokenids[index+RAInterfaceBean.tokentexts.length] = ((Integer) hardtokenprofiles.get(name)).intValue();
          index++;
        }
     }

      String[] availabletokens = profile.getValue(EndEntityProfile.AVAILKEYSTORE, 0).split(EndEntityProfile.SPLITCHAR);
      String[] availablehardtokenissuers = profile.getValue(EndEntityProfile.AVAILTOKENISSUER, 0).split(EndEntityProfile.SPLITCHAR);
      if(lastselectedhardtokenissuer==-1){
        String value = profile.getValue(EndEntityProfile.DEFAULTTOKENISSUER,0);
        if(value != null && !value.equals(""))
          lastselectedhardtokenissuer = Integer.parseInt(value);
      }
      ArrayList[] tokenissuers = null;

      usekeyrecovery = globalconfiguration.getEnableKeyRecovery() && profile.getUse(EndEntityProfile.KEYRECOVERABLE,0);
      usehardtokenissuers = globalconfiguration.getIssueHardwareTokens() && profile.getUse(EndEntityProfile.AVAILTOKENISSUER,0);
      if(usehardtokenissuers){       
        tokenissuers = new ArrayList[availabletokens.length];
        for(int i=0;i < availabletokens.length;i++){
          if(Integer.parseInt(availabletokens[i]) > SecConst.TOKEN_SOFT){
            tokenissuers[i] = new ArrayList();
            for(int j=0; j < availablehardtokenissuers.length; j++){
              HardTokenIssuerData issuerdata = tokenbean.getHardTokenIssuerData(Integer.parseInt(availablehardtokenissuers[j]));
              if(issuerdata !=null){
                Iterator iter = issuerdata.getHardTokenIssuer().getAvailableHardTokenProfiles().iterator();
                while(iter.hasNext()){
                  if(Integer.parseInt(availabletokens[i]) == ((Integer) iter.next()).intValue())
                    tokenissuers[i].add(new Integer(availablehardtokenissuers[j]));
                }
              }
            }
          }  
        } 
      }

      HashMap availablecas = null;
      Collection authcas = null;

      if(issuperadministrator)
        if(profileid == SecConst.EMPTY_ENDENTITYPROFILE)
          authcas = ejbcawebbean.getAuthorizedCAIds();
        else
          authcas = profile.getAvailableCAs();
      else
        availablecas = ejbcawebbean.getInformationMemory().getEndEntityAvailableCAs(profileid);
  
%>
<head>
  <title><%= globalconfiguration.getEjbcaTitle() %></title>
  <base href="<%= ejbcawebbean.getBaseUrl() %>">
  <link rel=STYLESHEET href="<%= ejbcawebbean.getCssFile() %>">
  <script language=javascript>

  <% if(!noprofiles){ %>
   <!--
      var TRUE  = "<%= EndEntityProfile.TRUE %>";
      var FALSE = "<%= EndEntityProfile.FALSE %>";


   <% if(usehardtokenissuers){ %>

       var TOKENID         = 0;
       var NUMBEROFISSUERS = 1;
       var ISSUERIDS       = 2;
       var ISSUERNAMES     = 3;

       var tokenissuers = new Array(<%=availabletokens.length%>);
       <% for(int i=0; i < availabletokens.length; i++){
            int numberofissuers = 0;
            if (Integer.parseInt(availabletokens[i]) > SecConst.TOKEN_SOFT) numberofissuers=tokenissuers[i].size();           
           %>
         tokenissuers[<%=i%>] = new Array(4);
         tokenissuers[<%=i%>][TOKENID] = <%= availabletokens[i] %>;
         tokenissuers[<%=i%>][NUMBEROFISSUERS] = <%= numberofissuers %>;
         tokenissuers[<%=i%>][ISSUERIDS] = new Array(<%= numberofissuers %>);
         tokenissuers[<%=i%>][ISSUERNAMES] = new Array(<%= numberofissuers %>);    
         <%  for(int j=0; j < numberofissuers; j++){ %>
         tokenissuers[<%=i%>][ISSUERIDS][<%=j%>]= <%= ((Integer) tokenissuers[i].get(j)).intValue() %>;
         tokenissuers[<%=i%>][ISSUERNAMES][<%=j%>]= "<%= tokenbean.getHardTokenIssuerAlias(((Integer) tokenissuers[i].get(j)).intValue())%>";
         <%  }
           } %>
       
function setAvailableHardTokenIssuers(){
    var seltoken = document.adduser.<%=SELECT_TOKEN%>.options.selectedIndex;
    issuers   =  document.adduser.<%=SELECT_HARDTOKENISSUER%>;

    numofissuers = issuers.length;
    for( i=numofissuers-1; i >= 0; i-- ){
       issuers.options[i]=null;
    }    
    issuers.disabled=true;

    if( seltoken > -1){
      var token = document.adduser.<%=SELECT_TOKEN%>.options[seltoken].value;
      if(token > <%= SecConst.TOKEN_SOFT%>){
        issuers.disabled=false;
        var tokenindex = 0;  
        for( i=0; i < tokenissuers.length; i++){
          if(tokenissuers[i][TOKENID] == token)
            tokenindex = i;
        }
        for( i=0; i < tokenissuers[tokenindex][NUMBEROFISSUERS] ; i++){
          issuers.options[i]=new Option(tokenissuers[tokenindex][ISSUERNAMES][i],tokenissuers[tokenindex][ISSUERIDS][i]);
          if(tokenissuers[tokenindex][ISSUERIDS][i] == <%=lastselectedhardtokenissuer %>)
            issuers.options.selectedIndex=i;
        }      
      }
    }
}

   <% } 
      if(usekeyrecovery){ %>
function isKeyRecoveryPossible(){
   var seltoken = document.adduser.<%=SELECT_TOKEN%>.options.selectedIndex; 
   var token = document.adduser.<%=SELECT_TOKEN%>.options[seltoken].value;
   if(token == <%=SecConst.TOKEN_SOFT_BROWSERGEN %>){
     document.adduser.<%=CHECKBOX_KEYRECOVERABLE%>.checked=false;
     document.adduser.<%=CHECKBOX_KEYRECOVERABLE%>.disabled=true;
   }else{
     <% if(profile.isRequired(EndEntityProfile.KEYRECOVERABLE,0)){ %>
       document.adduser.<%=CHECKBOX_KEYRECOVERABLE%>.disabled=true; 
     <% }else{ %>
     document.adduser.<%=CHECKBOX_KEYRECOVERABLE%>.disabled=false;
     <%}
       if(profile.getValue(EndEntityProfile.KEYRECOVERABLE,0).equals(EndEntityProfile.TRUE)){ %>
     document.adduser.<%=CHECKBOX_KEYRECOVERABLE%>.checked=true;
   <% }else{ %>  
     document.adduser.<%=CHECKBOX_KEYRECOVERABLE%>.checked=false;
     <% } %>
   }
}

   <% } %>


  

  <% if(issuperadministrator){ %>
  var availablecas = new Array(<%= authcas.size()%>);
 
  var CANAME       = 0;
  var CAID         = 1;
<%
      Iterator iter = authcas.iterator();
      int i = 0;
      while(iter.hasNext()){
        Object next = iter.next();
        Integer nextca = null;   
        if(next instanceof String)
           nextca =  new Integer((String) next);
        else
           nextca = (Integer) next;
    %> 
    
    availablecas[<%=i%>] = new Array(2);
    availablecas[<%=i%>][CANAME] = "<%= caidtonamemap.get(nextca) %>";      
    availablecas[<%=i%>][CAID] = <%= nextca.intValue() %>;
    
   <%   i++; 
      } %>

function fillCAField(){
   var caselect   =  document.adduser.<%=SELECT_CA%>; 

   var numofcas = caselect.length;
   for( i=numofcas-1; i >= 0; i-- ){
       caselect.options[i]=null;
    }   

   for( i=0; i < availablecas.length; i ++){
     caselect.options[i]=new Option(availablecas[i][CANAME],
                                     availablecas[i][CAID]);    
     if(availablecas[i][CAID] == "<%= lastselectedca %>")
       caselect.options.selectedIndex=i;
   }
}

 <% } else { %>

  var certprofileids = new Array(<%= availablecas.keySet().size()%>);
  var CERTPROFID   = 0;
  var AVAILABLECAS = 1;

  var CANAME       = 0;
  var CAID         = 1;
<%
  Iterator iter = availablecas.keySet().iterator();
  int i = 0;
  while(iter.hasNext()){ 
    Integer next = (Integer) iter.next();
    Collection nextcaset = (Collection) availablecas.get(next);
  %>
    certprofileids[<%=i%>] = new Array(2);
    certprofileids[<%=i%>][CERTPROFID] = <%= next.intValue() %> ;
    certprofileids[<%=i%>][AVAILABLECAS] = new Array(<%= nextcaset.size() %>);
<% Iterator iter2 = nextcaset.iterator();
   int j = 0;
   while(iter2.hasNext()){
     Integer nextca = (Integer) iter2.next(); %>
    certprofileids[<%=i%>][AVAILABLECAS][<%=j%>] = new Array(2);
    certprofileids[<%=i%>][AVAILABLECAS][<%=j%>][CANAME] = "<%= caidtonamemap.get(nextca) %>";      
    certprofileids[<%=i%>][AVAILABLECAS][<%=j%>][CAID] = <%= nextca.intValue() %>;
  <% j++ ;
   }
   i++;
 } %>     

function fillCAField(){
   var selcertprof = document.adduser.<%=SELECT_CERTIFICATEPROFILE%>.options.selectedIndex; 
   var certprofid = document.adduser.<%=SELECT_CERTIFICATEPROFILE%>.options[selcertprof].value; 
   var caselect   =  document.adduser.<%=SELECT_CA%>; 

   var numofcas = caselect.length;
   for( i=numofcas-1; i >= 0; i-- ){
       caselect.options[i]=null;
    }   

    if( selcertprof > -1){
      for( i=0; i < certprofileids.length; i ++){
        if(certprofileids[i][CERTPROFID] == certprofid){
          for( j=0; j < certprofileids[i][AVAILABLECAS].length; j++ ){
            caselect.options[j]=new Option(certprofileids[i][AVAILABLECAS][j][CANAME],
                                           certprofileids[i][AVAILABLECAS][j][CAID]);    
            if(certprofileids[i][AVAILABLECAS][j][CAID] == "<%= lastselectedca %>")
              caselect.options.selectedIndex=j;
          }
        }
      }
    }
}

  <% } %> 

function checkallfields(){
    var illegalfields = 0;

    <% if(profile.isModifyable(EndEntityProfile.USERNAME,0)){ %>
    if(!checkfieldforlegalchars("document.adduser.<%=TEXTFIELD_USERNAME%>","<%= ejbcawebbean.getText("ONLYCHARACTERS") + " " + ejbcawebbean.getText("USERNAME") %>"))
      illegalfields++;
    <%  if(profile.isRequired(EndEntityProfile.USERNAME,0)){%>
    if((document.adduser.<%= TEXTFIELD_USERNAME %>.value == "")){
      alert("<%= ejbcawebbean.getText("REQUIREDUSERNAME", true) %>");
      illegalfields++;
    } 
    <%    }
        }
       if(profile.getUse(EndEntityProfile.PASSWORD,0)){
         if(profile.isModifyable(EndEntityProfile.PASSWORD,0)){%>

    <%  if(profile.isRequired(EndEntityProfile.PASSWORD,0)){%>
    if((document.adduser.<%= TEXTFIELD_PASSWORD %>.value == "")){
      alert("<%= ejbcawebbean.getText("REQUIREDPASSWORD", true) %>");
      illegalfields++;
    } 
    <%    }
        }
       }
       for(int i=0; i < profile.getSubjectDNFieldOrderLength(); i++){
         fielddata = profile.getSubjectDNFieldsInOrder(i);
         if( !EndEntityProfile.isFieldOfType(fielddata[EndEntityProfile.FIELDTYPE], DnComponents.DNEMAIL) ){
           if(profile.isModifyable(fielddata[EndEntityProfile.FIELDTYPE],fielddata[EndEntityProfile.NUMBER])){ %>
    if(!checkfieldforlegaldnchars("document.adduser.<%=TEXTFIELD_SUBJECTDN+i%>","<%= ejbcawebbean.getText("ONLYCHARACTERS") + " " + ejbcawebbean.getText(DnComponents.getLanguageConstantFromProfileId(fielddata[EndEntityProfile.FIELDTYPE])) %>"))
      illegalfields++;
    <%     if(profile.isRequired(fielddata[EndEntityProfile.FIELDTYPE],fielddata[EndEntityProfile.NUMBER])){%>
    if((document.adduser.<%= TEXTFIELD_SUBJECTDN+i %>.value == "")){
      alert("<%= ejbcawebbean.getText("YOUAREREQUIRED", true) + " " + ejbcawebbean.getText(DnComponents.getLanguageConstantFromProfileId(fielddata[EndEntityProfile.FIELDTYPE]), true)%>");
      illegalfields++;
    } 
    <%     }
          }
         }
         else{ %>
    document.adduser.<%= CHECKBOX_SUBJECTDN+i %>.disabled = false;          
     <%  }
       }
       for(int i=0; i < profile.getSubjectAltNameFieldOrderLength(); i++){
         fielddata = profile.getSubjectAltNameFieldsInOrder(i);
         int fieldtype = fielddata[EndEntityProfile.FIELDTYPE];
         if(EndEntityProfile.isFieldImplemented(fieldtype)) {
           if(!EndEntityProfile.isFieldOfType(fielddata[EndEntityProfile.FIELDTYPE], DnComponents.RFC822NAME)){
             if(EndEntityProfile.isFieldOfType(fielddata[EndEntityProfile.FIELDTYPE],DnComponents.UPN)){%>
    if(!checkfieldforlegaldnchars("document.adduser.<%=TEXTFIELD_UPNNAME+i%>","<%= ejbcawebbean.getText("ONLYCHARACTERS") + " " + ejbcawebbean.getText(DnComponents.getLanguageConstantFromProfileId(fielddata[EndEntityProfile.FIELDTYPE])) %>"))
      illegalfields++;
          <%   if(profile.isRequired(fielddata[EndEntityProfile.FIELDTYPE],fielddata[EndEntityProfile.NUMBER])){ %>            
              if((document.adduser.<%= TEXTFIELD_UPNNAME+i %>.value == "")){ 
                alert("<%= ejbcawebbean.getText("YOUAREREQUIRED", true) + " " + ejbcawebbean.getText(DnComponents.getLanguageConstantFromProfileId(fielddata[EndEntityProfile.FIELDTYPE]), true)%>");
                illegalfields++;
              }
           <%  }
             }   
             if(profile.isModifyable(fielddata[EndEntityProfile.FIELDTYPE],fielddata[EndEntityProfile.NUMBER])){
               if(EndEntityProfile.isFieldOfType(fielddata[EndEntityProfile.FIELDTYPE], DnComponents.IPADDRESS)) { %>
    if(!checkfieldforipaddess("document.adduser.<%=TEXTFIELD_SUBJECTALTNAME+i%>","<%= ejbcawebbean.getText("ONLYNUMBERALSANDDOTS") + " " + ejbcawebbean.getText(DnComponents.getLanguageConstantFromProfileId(fielddata[EndEntityProfile.FIELDTYPE])) %>"))
      illegalfields++;
           <%  }else{ %> 

    if(!checkfieldforlegaldnchars("document.adduser.<%=TEXTFIELD_SUBJECTALTNAME+i%>","<%= ejbcawebbean.getText("ONLYCHARACTERS") + " " + ejbcawebbean.getText(DnComponents.getLanguageConstantFromProfileId(fielddata[EndEntityProfile.FIELDTYPE])) %>"))
      illegalfields++;
    <%    if(profile.isRequired(fielddata[EndEntityProfile.FIELDTYPE],fielddata[EndEntityProfile.NUMBER])){ %>
    if((document.adduser.<%= TEXTFIELD_SUBJECTALTNAME+i %>.value == "")){
      alert("<%= ejbcawebbean.getText("YOUAREREQUIRED", true) + " " + ejbcawebbean.getText(DnComponents.getLanguageConstantFromProfileId(fielddata[EndEntityProfile.FIELDTYPE]), true)%>");
      illegalfields++;
    } 
    <%      }
           }
          }
         }
         else{ %>
      document.adduser.<%= CHECKBOX_SUBJECTALTNAME+i %>.disabled = false;          
     <%    }
         } 
       }
       if(profile.getUse(EndEntityProfile.EMAIL,0)){ %>
    if(!checkfieldforlegalemailcharswithoutat("document.adduser.<%=TEXTFIELD_EMAIL%>","<%= ejbcawebbean.getText("ONLYEMAILCHARSNOAT") %>"))
      illegalfields++;

    <%  if(profile.isRequired(EndEntityProfile.EMAIL,0)){%>
    if((document.adduser.<%= TEXTFIELD_EMAIL %>.value == "")){
      alert("<%= ejbcawebbean.getText("REQUIREDEMAIL", true) %>");
      illegalfields++;
    } 
    <%    }

          if(profile.isModifyable(EndEntityProfile.EMAIL,0)){%>
    if(!checkfieldforlegalemailcharswithoutat("document.adduser.<%=TEXTFIELD_EMAILDOMAIN%>","<%= ejbcawebbean.getText("ONLYEMAILCHARSNOAT") %>"))
      illegalfields++;
          
      <%  if(profile.isRequired(EndEntityProfile.EMAIL,0)){%>
    if((document.adduser.<%= TEXTFIELD_EMAILDOMAIN %>.value == "")){
      alert("<%= ejbcawebbean.getText("REQUIREDEMAIL", true) %>");
      illegalfields++;
    } 
    <%    }
        }
      }
 
       if(profile.getUse(EndEntityProfile.PASSWORD,0)){
         if(profile.isModifyable(EndEntityProfile.PASSWORD,0)){%>  
    if(document.adduser.<%= TEXTFIELD_PASSWORD %>.value != document.adduser.<%= TEXTFIELD_CONFIRMPASSWORD %>.value){
      alert("<%= ejbcawebbean.getText("PASSWORDSDOESNTMATCH", true) %>");
      illegalfields++;
    } 
    <%   }else{ %>
    if(document.adduser.<%=SELECT_PASSWORD%>.options.selectedIndex != document.adduser.<%=SELECT_CONFIRMPASSWORD%>.options.selectedIndex ){
      alert("<%= ejbcawebbean.getText("PASSWORDSDOESNTMATCH", true) %>");
      illegalfields++; 
    }
<%        }   
     } %>
    if(document.adduser.<%=SELECT_CERTIFICATEPROFILE%>.options.selectedIndex == -1){
      alert("<%=  ejbcawebbean.getText("CERTIFICATEPROFILEMUST", true) %>");
      illegalfields++;
    }
    if(document.adduser.<%=SELECT_CA%>.options.selectedIndex == -1){
      alert("<%=  ejbcawebbean.getText("CAMUST", true) %>");
      illegalfields++;
    }
    if(document.adduser.<%=SELECT_TOKEN%>.options.selectedIndex == -1){
      alert("<%=  ejbcawebbean.getText("TOKENMUST", true) %>");
      illegalfields++;
    }

    <%  if(profile.getUse(EndEntityProfile.SENDNOTIFICATION,0) && profile.isModifyable(EndEntityProfile.EMAIL,0)){%>
    if(document.adduser.<%=CHECKBOX_SENDNOTIFICATION %>.checked && (document.adduser.<%= TEXTFIELD_EMAIL %>.value == "")){
      alert("<%= ejbcawebbean.getText("NOTIFICATIONADDRESSMUSTBE", true) %>");
      illegalfields++;
    } 
    <% } %>

    if(illegalfields == 0){
      <% if(profile.getUse(EndEntityProfile.CLEARTEXTPASSWORD,0)){%> 
      document.adduser.<%= CHECKBOX_CLEARTEXTPASSWORD %>.disabled = false;
      <% } if(profile.getUse(EndEntityProfile.ADMINISTRATOR,0)){%> 
      document.adduser.<%= CHECKBOX_ADMINISTRATOR %>.disabled = false;
      <% } if(profile.getUse(EndEntityProfile.KEYRECOVERABLE,0) && globalconfiguration.getEnableKeyRecovery()){%> 
      document.adduser.<%= CHECKBOX_KEYRECOVERABLE %>.disabled = false;
      <% } if(profile.getUse(EndEntityProfile.SENDNOTIFICATION,0)){%> 
      document.adduser.<%= CHECKBOX_SENDNOTIFICATION %>.disabled = false;
      <% } if(profile.getUsePrinting()){%> 
      document.adduser.<%= CHECKBOX_PRINT %>.disabled = false;
      <% }%>
    }

     return illegalfields == 0;  
}
  <% } %>
   -->
  </script>
  <script language=javascript src="<%= globalconfiguration .getAdminWebPath() %>ejbcajslib.js"></script>
</head>
<body onload='<% if(usehardtokenissuers) out.write("setAvailableHardTokenIssuers();");
                 if(usekeyrecovery) out.write(" isKeyRecoveryPossible();");%>
                 fillCAField();'>
  <h2 align="center"><%= ejbcawebbean.getText("ADDENDENTITY") %></h2>
  <!-- <div align="right"><A  onclick='displayHelpWindow("<%= ejbcawebbean.getHelpfileInfix("ra_help.html") + "#addendentity"%>")'>
    <u><%= ejbcawebbean.getText("HELP") %></u> </A> 
  </div> -->
  <% if(noprofiles){ %>
    <div align="center"><h4 id="alert"><%=ejbcawebbean.getText("NOTAUTHORIZEDTOCREATEENDENTITY") %></h4></div>
  <% }else{
       if(userexists){ %>
  <div align="center"><h4 id="alert"><%=ejbcawebbean.getText("ENDENTITYALREADYEXISTS") %></h4></div>
  <% } %>
    <% if(approvalmessage != null){ %>
  <div align="center"><h4 id="alert"><%= approvalmessage%></h4></div>
  <% } %>
  <% if(useradded){ %>
  <div align="center"><h4 ><% out.write(ejbcawebbean.getText("ENDENTITY")+ " ");
                                        out.write(addedusername + " ");
                                        out.write(ejbcawebbean.getText("ADDEDSUCCESSFULLY"));%></h4></div>
  <% } %>


     <table border="0" cellpadding="0" cellspacing="2" width="792">
       <form name="changeprofile" action="<%= THIS_FILENAME %>" method="post">
       <input type="hidden" name='<%= ACTION %>' value='<%=ACTION_CHANGEPROFILE %>'>
       <tr>
         <td></td>
	 <td align="right"><%= ejbcawebbean.getText("ENDENTITYPROFILE") %></td>
	 <td><select name="<%=SELECT_ENDENTITYPROFILE %>" size="1" tabindex="<%=tabindex++%>" onchange="document.changeprofile.submit()"'>
                <% for(int i = 0; i < profilenames.length;i++){
                      int pid = rabean.getEndEntityProfileId(profilenames[i]);
                      %>                
	 	<option value="<%=pid %>" <% if(pid == profileid)
                                             out.write("selected"); %>>
 
                         <%= profilenames[i] %>
                </option>
                <% } %>
	     </select>
         </td>
	<td><%= ejbcawebbean.getText("REQUIRED") %></td>
      </tr>
      <tr>
	<td></td>
	<td></td>
	<td></td>
	<td></td>
      </tr>
      </form>
       <form name="adduser" action="<%= THIS_FILENAME %>" method="post">   
         <input type="hidden" name='<%= ACTION %>' value='<%=ACTION_ADDUSER %>'>   
         <input type="hidden" name='<%= HIDDEN_PROFILE %>' value='<%=profileid %>'>    
          <% if(profile.getUse(EndEntityProfile.USERNAME,0)){ %>
      <tr id="Row<%=(row++)%2%>">
	<td></td>
	<td align="right"><%= ejbcawebbean.getText("USERNAME") %></td> 
	<td>
            <% if(!profile.isModifyable(EndEntityProfile.USERNAME,0)){ 
                 String[] options = profile.getValue(EndEntityProfile.USERNAME, 0).split(EndEntityProfile.SPLITCHAR);
               %>
           <select name="<%= SELECT_USERNAME %>" size="1" tabindex="<%=tabindex++%>">
               <% if( options != null){
                    for(int i=0;i < options.length;i++){ %>
             <option value="<%=options[i].trim()%>" <% if(lastselectedusername.equals(options[i])) out.write(" selected "); %>> 
               <%=options[i].trim()%>
             </option>                
               <%   }
                  }
                %>
           </select>
           <% }else{ %> 
             <input type="text" name="<%= TEXTFIELD_USERNAME %>" size="40" maxlength="255" tabindex="<%=tabindex++%>" value="<%= profile.getValue(EndEntityProfile.USERNAME,0) %>">
           <% } %>

        </td>
	<td><input type="checkbox" name="<%= CHECKBOX_REQUIRED_USERNAME %>" value="<%= CHECKBOX_VALUE %>"  disabled="true" <% if(profile.isRequired(EndEntityProfile.USERNAME,0)) out.write(" CHECKED "); %>></td>
      </tr>
         <% }%>
          <% if(profile.getUse(EndEntityProfile.PASSWORD,0)){ %>
      <tr id="Row<%=(row++)%2%>">
        <td>&nbsp&nbsp&nbsp&nbsp&nbsp;&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp
&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp
        </td>
	<td align="right"><%= ejbcawebbean.getText("PASSWORD") %></td>
        <td>   
             <%
               if(!profile.isModifyable(EndEntityProfile.PASSWORD,0)){ 
               %>
           <select name="<%= SELECT_PASSWORD %>" size="1" tabindex="3">
               <% if(profile.getValue(EndEntityProfile.PASSWORD,0) != null){ %>
             <option value='<%=profile.getValue(EndEntityProfile.PASSWORD,0).trim()%>' > <%=profile.getValue(EndEntityProfile.PASSWORD,0)  %>
             </option>                
               <%   
                  }
                %>
           </select>
           <% }else{ %> 
             <input type="password" name="<%= TEXTFIELD_PASSWORD %>" size="40" maxlength="255" tabindex="<%=tabindex++%>" value='<%= profile.getValue(EndEntityProfile.PASSWORD,0) %>'>
           <% } %>
 
        </td>
	<td><input type="checkbox" name="<%= CHECKBOX_REQUIRED_PASSWORD %>" value="<%= CHECKBOX_VALUE %>"  disabled="true" <% if(profile.isRequired(EndEntityProfile.PASSWORD,0)) out.write(" CHECKED "); %>></td>
      </tr>
       <% } 
          if(profile.getUse(EndEntityProfile.PASSWORD,0)){%>
      <tr id="Row<%=(row++)%2%>">
	<td></td>
	<td align="right"><%= ejbcawebbean.getText("CONFIRMPASSWORD") %></td>
        <td>
          <%   if(!profile.isModifyable(EndEntityProfile.PASSWORD,0)){ 
               %>
           <select name="<%= SELECT_CONFIRMPASSWORD %>" size="1" tabindex="4">
               <% if( profile.getValue(EndEntityProfile.PASSWORD,0) != null){ %>
             <option value='<%=profile.getValue(EndEntityProfile.PASSWORD,0).trim()%>'> 
                 <%=profile.getValue(EndEntityProfile.PASSWORD,0).trim() %>
             </option>                
               <%   
                  }
                %>
           </select>
           <% }else{ %> 
             <input type="password" name="<%= TEXTFIELD_CONFIRMPASSWORD %>" size="40" maxlength="255" tabindex="<%=tabindex++%>" value='<%= profile.getValue(EndEntityProfile.PASSWORD,0) %>'>
           <% } %>
        </td>
	<td>&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp
&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp</td> 
      </tr>
      <% }
          if(profile.getUse(EndEntityProfile.CLEARTEXTPASSWORD,0)){%>
      <tr id="Row<%=(row++)%2%>">
	<td></td>
	<td align="right"><%= ejbcawebbean.getText("USEINBATCH") %></td>
	<td><input type="checkbox" name="<%= CHECKBOX_CLEARTEXTPASSWORD %>" value="<%= CHECKBOX_VALUE %>" tabindex="<%=tabindex++%>" <% if(profile.getValue(EndEntityProfile.CLEARTEXTPASSWORD,0).equals(EndEntityProfile.TRUE))
                                                                                                                 out.write(" CHECKED "); 
                                                                                                               if(profile.isRequired(EndEntityProfile.CLEARTEXTPASSWORD,0))
                                                                                                                 out.write(" disabled='true' "); 
                                                                                                             %>> 
        </td>
	<td></td> 
      </tr>
      <% } 
         if(profile.getUse(EndEntityProfile.EMAIL,0)){ %>
       <tr id="Row<%=(row++)%2%>">
	 <td></td>
	 <td align="right"><%= ejbcawebbean.getText("EMAIL") %></td>
	 <td>      
           <input type="text" name="<%= TEXTFIELD_EMAIL %>" size="20" maxlength="255" tabindex="<%=tabindex++%>" value="<%=oldemail%>">@
          <% if(!profile.isModifyable(EndEntityProfile.EMAIL,0)){ 
                 String[] options = profile.getValue(EndEntityProfile.EMAIL, 0).split(EndEntityProfile.SPLITCHAR);
               %>
           <select name="<%= SELECT_EMAILDOMAIN %>" size="1" tabindex="<%=tabindex++%>">
               <% if( options != null){
                    for(int i=0;i < options.length;i++){ %>
             <option value='<%=options[i].trim()%>' <% if(lastselectedemaildomain.equals(options[i])) out.write(" selected "); %>>
                <%=options[i].trim()%>  
             </option>                
               <%   }
                  }
                %>
           </select>
           <% }else{ %> 
             <input type="text" name="<%= TEXTFIELD_EMAILDOMAIN %>" size="20" maxlength="255" tabindex="<%=tabindex++%>"  value='<%= profile.getValue(EndEntityProfile.EMAIL,0) %>'>
           <% } %>
        </td>
	<td><input type="checkbox" name="<%= CHECKBOX_REQUIRED_EMAIL %>" value="<%= CHECKBOX_VALUE %>"  disabled="true" <% if(profile.isRequired(EndEntityProfile.EMAIL,0)) out.write(" CHECKED "); %>></td>
       </tr>
       <% }%>
      <tr id="Row<%=(row++)%2%>">
	<td></td>
	<td align="right"><b><%= ejbcawebbean.getText("SUBJECTDNFIELDS") %></b></td>
	<td>&nbsp;</td>
	<td></td>
       </tr>
       <% int numberofsubjectdnfields = profile.getSubjectDNFieldOrderLength();
          for(int i=0; i < numberofsubjectdnfields; i++){
            fielddata = profile.getSubjectDNFieldsInOrder(i);  %>
       <tr id="Row<%=(row++)%2%>">
	 <td></td>
	 <td align="right"><%= ejbcawebbean.getText(DnComponents.getLanguageConstantFromProfileId(fielddata[EndEntityProfile.FIELDTYPE])) %></td>
	 <td>      
          <% 
             if( !EndEntityProfile.isFieldOfType(fielddata[EndEntityProfile.FIELDTYPE], DnComponents.DNEMAIL) ){  
                if(!profile.isModifyable(fielddata[EndEntityProfile.FIELDTYPE],fielddata[EndEntityProfile.NUMBER])){ 
                 String[] options = profile.getValue(fielddata[EndEntityProfile.FIELDTYPE],fielddata[EndEntityProfile.NUMBER]).split(EndEntityProfile.SPLITCHAR);
               %>
           <select name="<%= SELECT_SUBJECTDN + i %>" size="1" tabindex="<%=tabindex++%>">
               <% if( options != null){
                    for(int j=0;j < options.length;j++){ %>
             <option value="<%=options[j].trim()%>" <% if( lastselectedsubjectdns != null && lastselectedsubjectdns[i] != null) 
                                                         if(lastselectedsubjectdns[i].equals(options[j])) out.write(" selected "); %>> 
                <%=options[j].trim()%>
             </option>                
               <%   }
                  }
                %>
           </select>
           <% }else{ %> 
             <input type="text" name="<%= TEXTFIELD_SUBJECTDN + i %>" size="40" maxlength="255" tabindex="<%=tabindex++%>" value="<%= profile.getValue(fielddata[EndEntityProfile.FIELDTYPE],fielddata[EndEntityProfile.NUMBER]) %>">
           <% }
            }
            else{ %>
              <%= ejbcawebbean.getText("USESEMAILFIELDDATA")+ " :"%>&nbsp;
        <input type="checkbox" name="<%=CHECKBOX_SUBJECTDN + i%>" value="<%=CHECKBOX_VALUE %>" tabindex="<%=tabindex++%>" <% if(profile.isRequired(fielddata[EndEntityProfile.FIELDTYPE],fielddata[EndEntityProfile.NUMBER]))
                                                                                                                 out.write(" CHECKED "); 
                                                                                                               if(profile.isRequired(fielddata[EndEntityProfile.FIELDTYPE],fielddata[EndEntityProfile.NUMBER]))
                                                                                                                 out.write(" disabled='true' "); 
                                                                                                             %>>
         <% } %>       
        </td>
	<td><input type="checkbox" name="<%= CHECKBOX_REQUIRED_SUBJECTDN + i %>" value="<%= CHECKBOX_VALUE %>"  disabled="true" <% if(profile.isRequired(fielddata[EndEntityProfile.FIELDTYPE],fielddata[EndEntityProfile.NUMBER])) out.write(" CHECKED "); %>></td>
      </tr>
     <% } 
        int numberofsubjectaltnamefields = profile.getSubjectAltNameFieldOrderLength();
        if(numberofsubjectaltnamefields > 0){
%> 
      <tr id="Row<%=(row++)%2%>">
	<td></td>
	<td align="right"><b><%= ejbcawebbean.getText("SUBJECTALTNAMEFIELDS") %></b></td>
	<td>&nbsp;</td>
	<td></td>
       </tr>
       <% }
          for(int i=0; i < numberofsubjectaltnamefields; i++){
            fielddata = profile.getSubjectAltNameFieldsInOrder(i);  
            int fieldtype = fielddata[EndEntityProfile.FIELDTYPE];
            if(EndEntityProfile.isFieldImplemented(fieldtype)) { %>
       <tr id="Row<%=(row++)%2%>">
	 <td></td>
	 <td align="right"><%= ejbcawebbean.getText(DnComponents.getLanguageConstantFromProfileId(fielddata[EndEntityProfile.FIELDTYPE])) %></td>
	 <td>      
		<%	if( EndEntityProfile.isFieldOfType(fielddata[EndEntityProfile.FIELDTYPE], DnComponents.RFC822NAME ) ) {
				// Handle RFC822NAME separately
            	if ( profile.getUse(fielddata[EndEntityProfile.FIELDTYPE],fielddata[EndEntityProfile.NUMBER]) ) { %>
					<%= ejbcawebbean.getText("USESEMAILFIELDDATA") + " :"%>&nbsp;
					<input type="checkbox" name="<%=CHECKBOX_SUBJECTALTNAME + i%>" value="<%=CHECKBOX_VALUE %>" tabindex="<%=tabindex++%>"
					<%	if ( profile.isRequired(fielddata[EndEntityProfile.FIELDTYPE],fielddata[EndEntityProfile.NUMBER]) ) { %>
							CHECKED disabled="true"
					<%	} %> >
            <%	} else {
            		String rfc822NameString = profile.getValue(fielddata[EndEntityProfile.FIELDTYPE],fielddata[EndEntityProfile.NUMBER]);
            		String[] rfc822NameArray = new String[2];
            		if ( rfc822NameString.indexOf("@") != -1 ) {
            			rfc822NameArray = rfc822NameString.split("@");
            		} else {
	            		rfc822NameArray[0] = "";
            			rfc822NameArray[1] = rfc822NameString;
            		} %>
					<input type="text" name="<%= TEXTFIELD_RFC822NAME+i %>" size="20" maxlength="255" tabindex="<%=tabindex++%>"
						value="<%= rfc822NameArray[0] %>" />@
				<%	if ( profile.isModifyable(fielddata[EndEntityProfile.FIELDTYPE],fielddata[EndEntityProfile.NUMBER]) ) { %>
					<input type="text" name="<%= TEXTFIELD_SUBJECTALTNAME + i %>" size="40" maxlength="255" tabindex="<%=tabindex++%>"
						value="<%= rfc822NameArray[1] %>" />
				<%	} else {
						String[] options = rfc822NameString.split(EndEntityProfile.SPLITCHAR);
						if( options != null && options.length > 0 ) { %>
							<select name="<%= SELECT_SUBJECTALTNAME + i %>" size="1" tabindex="<%=tabindex++%>">
							<%	for ( int j=0; j < options.length; j++ ) { %>
									<option value="<%= options[j].trim() %>"
									<%	if ( lastselectedsubjectaltnames != null && lastselectedsubjectaltnames[i] != null &&
												lastselectedsubjectaltnames[i].equals(options[j]) ) { %>
												SELECTED
									<%	} %> > 
										<%=	options[j].trim() %>
									</option>
							<%	} %>
							</select>
					<%	}
					} %>
			<%	}
		} else {
				// Handle all non-RFC822NAME-fields
				if ( !profile.isModifyable(fielddata[EndEntityProfile.FIELDTYPE],fielddata[EndEntityProfile.NUMBER]) ) {
					// Display fixed subject altname fields
					String[] options = profile.getValue(fielddata[EndEntityProfile.FIELDTYPE],fielddata[EndEntityProfile.NUMBER]).split(EndEntityProfile.SPLITCHAR);
					if ( EndEntityProfile.isFieldOfType(fielddata[EndEntityProfile.FIELDTYPE], DnComponents.UPN) ) { %>
						<input type="text" name="<%= TEXTFIELD_UPNNAME+i %>" size="20" maxlength="255" tabindex="<%=tabindex++%>" />@
				<%	} %>
				<%	if( options != null && options.length > 0 ) { %>
						<select name="<%= SELECT_SUBJECTALTNAME + i %>" size="1" tabindex="<%=tabindex++%>">
						<%	for ( int j=0; j < options.length; j++ ) { %>
								<option value="<%=options[j].trim()%>"
								<%	if ( lastselectedsubjectaltnames != null &&  lastselectedsubjectaltnames[i] != null) {
										if ( lastselectedsubjectaltnames[i].equals(options[j])) {
											out.write(" selected ");
										}
									} %> > 
									<%=	options[j].trim() %>
								</option>                
						<%	} %>
						</select>
				<%	} %>
			<%	} else {
					// Display modifyable subject altname fields
	               	if(EndEntityProfile.isFieldOfType(fielddata[EndEntityProfile.FIELDTYPE], DnComponents.UPN)) { %>
						<input type="text" name="<%= TEXTFIELD_UPNNAME+i %>" size="20" maxlength="255" tabindex="<%=tabindex++%>" >@
				<%	} %>
					<input type="text" name="<%= TEXTFIELD_SUBJECTALTNAME + i %>" size="40" maxlength="255" tabindex="<%=tabindex++%>" value="<%= profile.getValue(fielddata[EndEntityProfile.FIELDTYPE],fielddata[EndEntityProfile.NUMBER]) %>">
			<%	} %>
		<%	} %>
        </td>
	<td><input type="checkbox" name="<%= CHECKBOX_REQUIRED_SUBJECTALTNAME + i %>" value="<%= CHECKBOX_VALUE %>"  disabled="true" <% if(profile.isRequired(fielddata[EndEntityProfile.FIELDTYPE],fielddata[EndEntityProfile.NUMBER])) out.write(" CHECKED "); %>></td>
      </tr>
     <%  } 
       } 

        int numberofsubjectdirattrfields = profile.getSubjectDirAttrFieldOrderLength();
        if(numberofsubjectdirattrfields > 0){
%> 
      <tr id="Row<%=(row++)%2%>">
	<td></td>
	<td align="right"><b><%= ejbcawebbean.getText("SUBJECTDIRATTRFIELDS") %></b></td>
	<td>&nbsp;</td>
	<td></td>
       </tr>
       <% }
          for(int i=0; i < numberofsubjectdirattrfields; i++){
            fielddata = profile.getSubjectDirAttrFieldsInOrder(i);  
            int fieldtype = fielddata[EndEntityProfile.FIELDTYPE];
			{ %>
       <tr id="Row<%=(row++)%2%>">
	 <td></td>
	 <td align="right"><%= ejbcawebbean.getText(DnComponents.getLanguageConstantFromProfileId(fielddata[EndEntityProfile.FIELDTYPE])) %></td>
	 <td>      
          <%
               if(!profile.isModifyable(fielddata[EndEntityProfile.FIELDTYPE],fielddata[EndEntityProfile.NUMBER])){ 
                 String[] options = profile.getValue(fielddata[EndEntityProfile.FIELDTYPE],fielddata[EndEntityProfile.NUMBER]).split(EndEntityProfile.SPLITCHAR);
                %>
           <select name="<%= SELECT_SUBJECTDIRATTR + i %>" size="1" tabindex="<%=tabindex++%>">
               <% if( options != null){
                    for(int j=0;j < options.length;j++){ %>
             <option value="<%=options[j].trim()%>" <% if( lastselectedsubjectdirattrs != null &&  lastselectedsubjectdirattrs[i] != null) 
                                                         if(lastselectedsubjectdirattrs[i].equals(options[j])) out.write(" selected "); %>> 
                <%=options[j].trim()%>
             </option>                
               <%   }
                  }
                %>
           </select>
           <% } else { %> 
             <input type="text" name="<%= TEXTFIELD_SUBJECTDIRATTR + i %>" size="40" maxlength="255" tabindex="<%=tabindex++%>" value="<%= profile.getValue(fielddata[EndEntityProfile.FIELDTYPE],fielddata[EndEntityProfile.NUMBER]) %>">
           <% } %>
        </td>
	<td><input type="checkbox" name="<%= CHECKBOX_REQUIRED_SUBJECTDIRATTR + i %>" value="<%= CHECKBOX_VALUE %>"  disabled="true" <% if(profile.isRequired(fielddata[EndEntityProfile.FIELDTYPE],fielddata[EndEntityProfile.NUMBER])) out.write(" CHECKED "); %>></td>
      </tr>
     <%  } %>
	<%	} if( profile.getUse(EndEntityProfile.STARTTIME, 0) || profile.getUse(EndEntityProfile.ENDTIME, 0) ) { %>
		<tr id="Row<%=(row++)%2%>"><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>
	<%	} if( profile.getUse(EndEntityProfile.STARTTIME, 0) ) { %>
		<tr  id="Row<%=(row++)%2%>"> 
			<td></td><td align="right"> 
				<%= ejbcawebbean.getText("TIMEOFSTART") %> <br />
				(<%= ejbcawebbean.getText("EXAMPLE").toLowerCase() %> <%= DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT,
				ejbcawebbean.getLocale()).format(new Date()) %> <%= ejbcawebbean.getText("OR").toLowerCase() %> <%= ejbcawebbean.getText("DAYS").toLowerCase()
				%>:<%= ejbcawebbean.getText("HOURS").toLowerCase() %>:<%= ejbcawebbean.getText("MINUTES").toLowerCase() %>)
			</td><td> 
				<input type="text" name="<%= TEXTFIELD_STARTTIME %>" size="40" maxlength="40" tabindex="<%=tabindex++%>"
					<%	String startTime = profile.getValue(EndEntityProfile.STARTTIME, 0);
					if ( startTime == null || startTime.equals("") ) {
						startTime = DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT, ejbcawebbean.getLocale()
							).format(new Date());
					} %>
					value="<%= startTime %>"
					<%	if ( !profile.isModifyable(EndEntityProfile.STARTTIME, 0) ) { %>
					readonly="true"
					<%	} %>
					/>
			</td>
			<td>
				<input type="checkbox" name="<%= CHECKBOX_REQUIRED_STARTTIME %>" value="<%= CHECKBOX_VALUE %>"  disabled="true"
				<%	if ( profile.isRequired(EndEntityProfile.STARTTIME, 0) ) {
						out.write(" CHECKED ");
					} %>
				/>
			</td>
		</tr>
	<%	} if( profile.getUse(EndEntityProfile.ENDTIME, 0) ) { %>
		<tr  id="Row<%=(row++)%2%>"> 
			<td></td><td align="right"> 
				<%= ejbcawebbean.getText("TIMEOFEND") %> <br />
				(<%= ejbcawebbean.getText("EXAMPLE").toLowerCase() %> <%= DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT,
				ejbcawebbean.getLocale()).format(new Date()) %> <%= ejbcawebbean.getText("OR").toLowerCase() %> <%= ejbcawebbean.getText("DAYS").toLowerCase()
				%>:<%= ejbcawebbean.getText("HOURS").toLowerCase() %>:<%= ejbcawebbean.getText("MINUTES").toLowerCase() %>)
			</td><td> 
				<input type="text" name="<%= TEXTFIELD_ENDTIME %>" size="40" maxlength="40" tabindex="<%=tabindex++%>"
					value="<%= profile.getValue(EndEntityProfile.ENDTIME, 0) %>"
					<%	if ( !profile.isModifyable(EndEntityProfile.ENDTIME, 0) ) { %>
					readonly="true"
					<%	} %>
					/>
			</td>
			<td>
				<input type="checkbox" name="<%= CHECKBOX_REQUIRED_ENDTIME %>" value="<%= CHECKBOX_VALUE %>"  disabled="true"
				<%	if ( profile.isRequired(EndEntityProfile.ENDTIME, 0) ) {
						out.write(" CHECKED ");
					} %>
				/>
			</td>
		</tr>
	<% } %> 
       <tr id="Row<%=(row++)%2%>">
	 <td>&nbsp;</td>
	 <td>&nbsp;</td>
	 <td>&nbsp;</td>
	 <td>&nbsp;</td>
       </tr>
       <tr id="Row<%=(row++)%2%>">
	 <td></td>
	 <td align="right"><%= ejbcawebbean.getText("CERTIFICATEPROFILE") %></td>
	 <td>
         <select name="<%= SELECT_CERTIFICATEPROFILE %>" size="1" tabindex="<%=tabindex++%>" onchange='fillCAField()'>
         <%
           String[] availablecertprofiles = profile.getValue(EndEntityProfile.AVAILCERTPROFILES, 0).split(EndEntityProfile.SPLITCHAR);
           if(lastselectedcertificateprofile.equals(""))
             lastselectedcertificateprofile= profile.getValue(EndEntityProfile.DEFAULTCERTPROFILE,0);

           if( availablecertprofiles != null){
             for(int i =0; i< availablecertprofiles.length;i++){
         %>
         <option value='<%=availablecertprofiles[i]%>' <% if(lastselectedcertificateprofile.equals(availablecertprofiles[i])) out.write(" selected "); %> >
            <%= rabean.getCertificateProfileName(Integer.parseInt(availablecertprofiles[i])) %>
         </option>
         <%
             }
           }
         %>
         </select>
         </td>
	 <td><input type="checkbox" name="checkbox" value="true"  disabled="true" CHECKED></td>
       </tr>
       <tr id="Row<%=(row++)%2%>">
	 <td></td>
	 <td align="right"><%= ejbcawebbean.getText("CA") %></td>
	 <td>
         <select name="<%= SELECT_CA %>" size="1" tabindex="<%=tabindex++%>">
         </select>
         </td>
	 <td><input type="checkbox" name="checkbox" value="true"  disabled="true" CHECKED></td>
       </tr>
       <tr id="Row<%=(row++)%2%>">
	 <td></td>
	 <td align="right"><%= ejbcawebbean.getText("TOKEN") %></td>
	 <td>
         <select name="<%= SELECT_TOKEN %>" size="1" tabindex="<%=tabindex++%>" onchange='<% if(usehardtokenissuers) out.write("setAvailableHardTokenIssuers();");
                                                                                             if(usekeyrecovery) out.write(" isKeyRecoveryPossible();");%>'>
         <%
           if(lastselectedtoken.equals(""))
             lastselectedtoken= profile.getValue(EndEntityProfile.DEFKEYSTORE,0);

           if( availabletokens != null){
             for(int i =0; i < availabletokens.length;i++){
         %>
         <option value='<%=availabletokens[i]%>' <% if(lastselectedtoken.equals(availabletokens[i])) out.write(" selected "); %> >
            <% for(int j=0; j < tokentexts.length; j++){
                 if( tokenids[j] == Integer.parseInt(availabletokens[i])) {
                   if( tokenids[j] > SecConst.TOKEN_SOFT)
                     out.write(tokentexts[j]);
                   else
                     out.write(ejbcawebbean.getText(tokentexts[j]));
                 }
               }%>
         </option>
         <%
             }
           }
         %>
         </select>
         </td>
	 <td><input type="checkbox" name="checkbox" value="true"  disabled="true" CHECKED></td>
       </tr>
       <% if(usehardtokenissuers){ %>
       <tr id="Row<%=(row++)%2%>">
	 <td></td>
	 <td align="right"><%= ejbcawebbean.getText("HARDTOKENISSUER") %></td>
	 <td>
         <select name="<%= SELECT_HARDTOKENISSUER %>" size="1" tabindex="<%=tabindex++%>">
         </select>
         </td>
	 <td></td>
       </tr>
       <% } %>
       <% if( profile.getUse(EndEntityProfile.ADMINISTRATOR,0) || usekeyrecovery){ %>
       <tr id="Row<%=(row++)%2%>">
	 <td></td>
	 <td align="right"><%= ejbcawebbean.getText("TYPES") %></td>
	 <td>
         </td>
	 <td></td>
       </tr>
       <% } %>
       
       <!--  Max number of allowed requests for a password -->
       <% if(profile.getUse(EndEntityProfile.ALLOWEDREQUESTS,0)){ %>
       <% 
           String defaultnrofrequests = profile.getValue(EndEntityProfile.ALLOWEDREQUESTS,0);
           if (defaultnrofrequests == null) {
        	   defaultnrofrequests = "1";
           }
       %>
       <tr id="Row<%=(row++)%2%>">
       <td></td>
  	   <td align="right"><%= ejbcawebbean.getText("ALLOWEDREQUESTS") %></td>
	   <td>
            <select name="<%=SELECT_ALLOWEDREQUESTS %>" size="1" >
	            <% for(int j=0;j< 6;j++){
	            %>
	            <option
	            <%     if(defaultnrofrequests.equals(Integer.toString(j)))
	                       out.write(" selected "); 
	            %>
	            value='<%=j%>'><%=j%></option>
	            <% }%>
            </select>
         </td>
       </tr>
       <%} %>
       
      <% if(profile.getUse(EndEntityProfile.ADMINISTRATOR,0)){ %>
    <tr  id="Row<%=(row++)%2%>"> 
      <td></td>
      <td  align="right"> 
        <%= ejbcawebbean.getText("ADMINISTRATOR") %> <br>
      </td>
      <td > 
        <input type="checkbox" name="<%=CHECKBOX_ADMINISTRATOR%>" value="<%=CHECKBOX_VALUE %>" tabindex="<%=tabindex++%>" <% if(profile.getValue(EndEntityProfile.ADMINISTRATOR,0).equals(EndEntityProfile.TRUE))
                                                                                                                 out.write(" CHECKED "); 
                                                                                                               if(profile.isRequired(EndEntityProfile.ADMINISTRATOR,0))
                                                                                                                 out.write(" disabled='true' "); 
                                                                                                             %>> 
      </td>
      <td></td>
    </tr>
      <%} if(usekeyrecovery){ %>
    <tr  id="Row<%=(row++)%2%>"> 
      <td></td>
      <td  align="right"> 
        <%= ejbcawebbean.getText("KEYRECOVERABLE") %> 
      </td>
      <td> 
        <input type="checkbox" name="<%=CHECKBOX_KEYRECOVERABLE%>" value="<%=CHECKBOX_VALUE %>" tabindex="<%=tabindex++%>"<% if(profile.getValue(EndEntityProfile.KEYRECOVERABLE,0).equals(EndEntityProfile.TRUE))
                                                                                                                 out.write(" CHECKED "); 
                                                                                                               if(profile.isRequired(EndEntityProfile.KEYRECOVERABLE,0))
                                                                                                                 out.write(" disabled='true' "); 
                                                                                                             %>>  
      </td>
      <td></td>
    </tr>
     <% }if(profile.getUse(EndEntityProfile.SENDNOTIFICATION,0)){ %>
    <tr  id="Row<%=(row++)%2%>"> 
      <td></td>
      <td  align="right"> 
        <%= ejbcawebbean.getText("SENDNOTIFICATION") %> <br>
      </td>
      <td > 
        <input type="checkbox" name="<%=CHECKBOX_SENDNOTIFICATION%>" value="<%=CHECKBOX_VALUE %>" tabindex="<%=tabindex++%>" <% if(profile.getValue(EndEntityProfile.SENDNOTIFICATION,0).equals(EndEntityProfile.TRUE))
                                                                                                                 out.write(" CHECKED "); 
                                                                                                               if(profile.isRequired(EndEntityProfile.SENDNOTIFICATION,0))
                                                                                                                 out.write(" disabled='true' "); 
                                                                                                             %>> 
      </td>
      <td></td>
    </tr>
        </tr>
     <% }if(profile.getUsePrinting()){ %>
    <tr  id="Row<%=(row++)%2%>"> 
      <td></td>
      <td  align="right"> 
        <%= ejbcawebbean.getText("PRINTUSERDATA") %> <br>
      </td>
      <td > 
        <input type="checkbox" name="<%=CHECKBOX_PRINT%>" value="<%=CHECKBOX_VALUE %>" tabindex="<%=tabindex++%>" <% if(profile.getPrintingDefault())
                                                                                                                 out.write(" CHECKED "); 
                                                                                                               if(profile.getPrintingRequired())
                                                                                                                 out.write(" disabled='true' "); 
                                                                                                             %>> 
      </td>
      <td></td>
    </tr>
	<%	} %>


       <tr id="Row<%=(row++)%2%>">
	 <td></td>
	 <td></td>
	 <td><input type="submit" name="<%= BUTTON_ADDUSER %>" value="<%= ejbcawebbean.getText("ADDENDENTITY") %>" tabindex="<%=tabindex++%>"
                    onClick='return checkallfields()'> 
             <input type="reset" name="<%= BUTTON_RESET %>" value="<%= ejbcawebbean.getText("RESET") %>" tabindex="<%=tabindex++%>"></td>
         <td></td>
       </tr> 
     </table> 
   
  <script language=javascript>
<!--
function viewuser(row){
    var hiddenusernamefield = eval("document.adduser.<%= HIDDEN_USERNAME %>" + row);
    var username = hiddenusernamefield.value;
    var link = "<%= VIEWUSER_LINK %>?<%= USER_PARAMETER %>="+username;
    link = encodeURI(link);
    win_popup = window.open(link, 'view_user','height=600,width=500,scrollbars=yes,toolbar=no,resizable=1');
    win_popup.focus();
}

function edituser(row){
    var hiddenusernamefield = eval("document.adduser.<%= HIDDEN_USERNAME %>" + row);
    var username = hiddenusernamefield.value;
    var link = "<%= EDITUSER_LINK %>?<%= USER_PARAMETER %>="+username;
    link = encodeURI(link);
    win_popup = window.open(link, 'edit_user','height=600,width=550,scrollbars=yes,toolbar=no,resizable=1');
    win_popup.focus();
}

-->
</script>

 

  <% if(addedusers == null || addedusers.length == 0){     %>
  <table width="100%" border="0" cellspacing="1" cellpadding="0">
  <tr id="Row0"> 
    <td width="10%">&nbsp;</td>
    <td width="20%">&nbsp;</td>
    <td width="20%">&nbsp;</td>
    <td width="20%">&nbsp;</td>
    <td width="30%">&nbsp;</td>
  </tr>
  <% } else{ %>
  <div align="center"><H4><%= ejbcawebbean.getText("PREVIOUSLYADDEDENDENTITIES") %> </H4></div>
  <p>
    <input type="submit" name="<%=BUTTON_RELOAD %>" value="<%= ejbcawebbean.getText("RELOAD") %>">
  </p>
  <table width="100%" border="0" cellspacing="1" cellpadding="0">
  <tr> 
    <td width="10%"><%= ejbcawebbean.getText("USERNAME") %>              
    </td>
    <td width="20%"><%= ejbcawebbean.getText("COMMONNAME") %>
    </td>
    <td width="20%"><%= ejbcawebbean.getText("ORGANIZATIONUNIT") %>
    </td>
    <td width="20%"><%= ejbcawebbean.getText("ORGANIZATION") %>                 
    </td>
    <td width="30%"> &nbsp;
    </td>
  </tr>
    <%   for(int i=0; i < addedusers.length; i++){
            if(addedusers[i] != null){ 
      %>
     
  <tr id="Row<%= i%2 %>"> 

    <td width="15%"><%= addedusers[i].getUsername() %>
       <input type="hidden" name='<%= HIDDEN_USERNAME + i %>' value='<%=java.net.URLEncoder.encode(addedusers[i].getUsername(),"UTF-8")%>'>
    </td>
    <td width="20%"><%= addedusers[i].getSubjectDNField(DNFieldExtractor.CN,0)  %></td>
    <td width="20%"><%= addedusers[i].getSubjectDNField(DNFieldExtractor.OU,0) %></td>
    <td width="20%"><%= addedusers[i].getSubjectDNField(DNFieldExtractor.O,0) %></td>
    <td width="25%">
        <A style="cursor:hand;" onclick='viewuser(<%= i %>)'>
        <u><%= ejbcawebbean.getText("VIEWENDENTITY") %></u> </A>
        <A style="cursor:hand;" onclick='edituser(<%= i %>)'>
        <u><%= ejbcawebbean.getText("EDITENDENTITY") %></u> </A>
    </td>
  </tr>
 <%        }
         }
       }
     }%>
  </table>
  </form>
   <p></p>

  <%// Include Footer 
   String footurl =   globalconfiguration .getFootBanner(); %>
   
  <jsp:include page="<%= footurl %>" />
</body>
</html>
