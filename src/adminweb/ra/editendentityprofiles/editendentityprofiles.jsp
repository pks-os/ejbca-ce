<html>
<%@page contentType="text/html"%>
<%@page errorPage="/errorpage.jsp" import="java.util.*, se.anatom.ejbca.webdist.webconfiguration.EjbcaWebBean,se.anatom.ejbca.ra.raadmin.GlobalConfiguration, se.anatom.ejbca.SecConst
               ,se.anatom.ejbca.webdist.rainterface.RAInterfaceBean, se.anatom.ejbca.ra.raadmin.EndEntityProfile, se.anatom.ejbca.webdist.rainterface.EndEntityProfileDataHandler, 
                se.anatom.ejbca.ra.raadmin.EndEntityProfileExistsException, se.anatom.ejbca.webdist.hardtokeninterface.HardTokenInterfaceBean, se.anatom.ejbca.hardtoken.HardTokenIssuer,
                se.anatom.ejbca.hardtoken.HardTokenIssuerData,  se.anatom.ejbca.webdist.cainterface.CAInterfaceBean"%>

<jsp:useBean id="ejbcawebbean" scope="session" class="se.anatom.ejbca.webdist.webconfiguration.EjbcaWebBean" />
<jsp:useBean id="ejbcarabean" scope="session" class="se.anatom.ejbca.webdist.rainterface.RAInterfaceBean" />
<jsp:useBean id="cabean" scope="session" class="se.anatom.ejbca.webdist.cainterface.CAInterfaceBean" />
<jsp:useBean id="tokenbean" scope="session" class="se.anatom.ejbca.webdist.hardtokeninterface.HardTokenInterfaceBean" />

<%! // Declarations 
  static final String ACTION                        = "action";
  static final String ACTION_EDIT_PROFILES          = "editprofiles";
  static final String ACTION_EDIT_PROFILE           = "editprofile";

  static final String CHECKBOX_VALUE           = EndEntityProfile.TRUE;

//  Used in profiles.jsp
  static final String BUTTON_EDIT_PROFILE      = "buttoneditprofile"; 
  static final String BUTTON_DELETE_PROFILE    = "buttondeleteprofile";
  static final String BUTTON_ADD_PROFILE       = "buttonaddprofile"; 
  static final String BUTTON_RENAME_PROFILE    = "buttonrenameprofile";
  static final String BUTTON_CLONE_PROFILE     = "buttoncloneprofile";

  static final String SELECT_PROFILE           = "selectprofile";
  static final String TEXTFIELD_PROFILENAME    = "textfieldprofilename";
  static final String HIDDEN_PROFILENAME       = "hiddenprofilename";
 
// Buttons used in profile.jsp
  static final String BUTTON_SAVE              = "buttonsave";
  static final String BUTTON_CANCEL            = "buttoncancel";
 
  static final String TEXTFIELD_USERNAME             = "textfieldusername";
  static final String TEXTFIELD_PASSWORD             = "textfieldpassword";
  static final String TEXTFIELD_SUBJECTDN            = "textfieldsubjectdn";
  static final String TEXTFIELD_SUBJECTALTNAME       = "textfieldsubjectaltname";
  static final String TEXTFIELD_EMAIL                = "textfieldemail";
  static final String TEXTFIELD_NOTIFICATIONSENDER   = "textfieldnotificationsender";
  static final String TEXTFIELD_NOTIFICATIONSUBJECT  = "textfieldnotificationsubject";

  static final String TEXTAREA_NOTIFICATIONMESSAGE  = "textareanotificationmessage";

  static final String CHECKBOX_CLEARTEXTPASSWORD          = "checkboxcleartextpassword";
  static final String CHECKBOX_ADMINISTRATOR              = "checkboxadministrator";
  static final String CHECKBOX_KEYRECOVERABLE             = "checkboxkeyrecoverable";
  static final String CHECKBOX_SENDNOTIFICATION           = "checkboxsendnotification";

  static final String CHECKBOX_REQUIRED_USERNAME          = "checkboxrequiredusername";
  static final String CHECKBOX_REQUIRED_PASSWORD          = "checkboxrequiredpassword";
  static final String CHECKBOX_REQUIRED_CLEARTEXTPASSWORD = "checkboxrequiredcleartextpassword";
  static final String CHECKBOX_REQUIRED_SUBJECTDN         = "checkboxrequiredsubjectdn";
  static final String CHECKBOX_REQUIRED_SUBJECTALTNAME    = "checkboxrequiredsubjectaltname";
  static final String CHECKBOX_REQUIRED_EMAIL             = "checkboxrequiredemail";
  static final String CHECKBOX_REQUIRED_ADMINISTRATOR     = "checkboxrequiredadministrator";
  static final String CHECKBOX_REQUIRED_SENDNOTIFICATION  = "checkboxrequiredsendnotification";
  static final String CHECKBOX_REQUIRED_KEYRECOVERABLE    = "checkboxrequiredkeyrecoverable";


  static final String CHECKBOX_MODIFYABLE_USERNAME          = "checkboxmodifyableusername";
  static final String CHECKBOX_MODIFYABLE_PASSWORD          = "checkboxmodifyablepassword";
  static final String CHECKBOX_MODIFYABLE_SUBJECTDN         = "checkboxmodifyablesubjectdn";
  static final String CHECKBOX_MODIFYABLE_SUBJECTALTNAME    = "checkboxmodifyablesubjectaltname";
  static final String CHECKBOX_MODIFYABLE_EMAIL             = "checkboxmodifyableemail";


  static final String CHECKBOX_USE_USERNAME          = "checkboxuseusername";
  static final String CHECKBOX_USE_PASSWORD          = "checkboxusepassword";
  static final String CHECKBOX_USE_CLEARTEXTPASSWORD = "checkboxusecleartextpassword";
  static final String CHECKBOX_USE_SUBJECTDN         = "checkboxusesubjectdn";
  static final String CHECKBOX_USE_SUBJECTALTNAME    = "checkboxusesubjectaltname";
  static final String CHECKBOX_USE_EMAIL             = "checkboxuseemail";
  static final String CHECKBOX_USE_ADMINISTRATOR     = "checkboxuseadministrator";
  static final String CHECKBOX_USE_KEYRECOVERABLE    = "checkboxusekeyrecoverable";
  static final String CHECKBOX_USE_SENDNOTIFICATION  = "checkboxusesendnotification";
  static final String CHECKBOX_USE_HARDTOKENISSUERS  = "checkboxusehardtokenissuers";

  static final String SELECT_DEFAULTCERTPROFILE             = "selectdefaultcertprofile";
  static final String SELECT_AVAILABLECERTPROFILES          = "selectavailablecertprofiles";

  static final String SELECT_DEFAULTTOKENTYPE               = "selectdefaulttokentype";
  static final String SELECT_AVAILABLETOKENTYPES            = "selectavailabletokentypes";


  static final String SELECT_DEFAULTCA                      = "selectdefaultca";
  static final String SELECT_AVAILABLECAS                   = "selectavailablecas";

  static final String SELECT_DEFAULTHARDTOKENISSUER         = "selectdefaulthardtokenissuer";
  static final String SELECT_AVAILABLEHARDTOKENISSUERS      = "selectavailablehardtokenissuers";


  static final String SELECT_ADDSUBJECTDN                   = "selectaddsubjectdn";
  static final String BUTTON_DELETESUBJECTDN                = "buttondeletesubjectdn";
  static final String BUTTON_ADDSUBJECTDN                   = "buttonaddsubjectdn";
  static final String CHECKBOX_SELECTSUBJECTDN              = "checkboxselectsubjectdn";
  static final String SELECT_ADDSUBJECTALTNAME              = "selectaddsubjectaltname";
  static final String BUTTON_DELETESUBJECTALTNAME           = "buttondeletesubjectaltname";
  static final String BUTTON_ADDSUBJECTALTNAME              = "buttonaddsubjectaltname";
  static final String CHECKBOX_SELECTSUBJECTALTNAME         = "checkboxselectsubjectaltname";


  static final String SELECT_TYPE                         = "selecttype";
  String profile = null;
  // Declare Language file.

%>
<% 

  // Initialize environment
  String includefile = "endentityprofilespage.jsp";
  boolean  triedtoeditemptyprofile   = false;
  boolean  triedtodeleteemptyprofile = false;
  boolean  profileexists             = false;
  boolean  profiledeletefailed       = false;
  boolean  cannotcloneempty          = false;

  int numberofsubjectdnfields=0;
  int numberofsubjectaltnamefields=0;
  String value=null;
  EndEntityProfile profiledata=null;
  int[] fielddata = null;

  GlobalConfiguration globalconfiguration = ejbcawebbean.initialize(request,"/ra_functionality/edit_end_entity_profiles"); 
                                            ejbcarabean.initialize(request, ejbcawebbean);
                                            cabean.initialize(request, ejbcawebbean);
                                            tokenbean.initialize(request, ejbcawebbean);
  String THIS_FILENAME            =  globalconfiguration .getRaPath()  + "/editendentityprofiles/editendentityprofiles.jsp";
%>
 
<head>
  <title><%= globalconfiguration .getEjbcaTitle() %></title>
  <base href="<%= ejbcawebbean.getBaseUrl() %>">
  <link rel=STYLESHEET href="<%= ejbcawebbean.getCssFile() %>">
  <script language=javascript src="<%= globalconfiguration .getAdminWebPath() %>ejbcajslib.js"></script>
</head>
<body>

<%  // Determine action 
  if( request.getParameter(ACTION) != null){
    if( request.getParameter(ACTION).equals(ACTION_EDIT_PROFILES)){
      if( request.getParameter(BUTTON_EDIT_PROFILE) != null){
          // Display  profilepage.jsp
         profile = request.getParameter(SELECT_PROFILE);
         if(profile != null){
           if(!profile.trim().equals("")){
             if(!profile.equals(EndEntityProfileDataHandler.EMPTY_PROFILE)){ 
               ejbcarabean.setTemporaryEndEntityProfile(null);
               includefile="endentityprofilepage.jsp"; 
             }else{
                triedtoeditemptyprofile=true;
                profile= null;
             }
           } 
           else{ 
            profile= null;
          } 
        }
        if(profile == null){   
          includefile="endentityprofilespage.jsp";     
        }
      }
      if( request.getParameter(BUTTON_DELETE_PROFILE) != null) {
          // Delete profile and display profilespage. 
          profile = request.getParameter(SELECT_PROFILE);
          if(profile != null){
            if(!profile.trim().equals("")){
              if(!profile.equals(EndEntityProfileDataHandler.EMPTY_PROFILE)){ 
                profiledeletefailed = !ejbcarabean.removeEndEntityProfile(profile); 
              }else{
                triedtodeleteemptyprofile=true;
              }
            }
          }
          includefile="endentityprofilespage.jsp";             
      }
      if( request.getParameter(BUTTON_RENAME_PROFILE) != null){ 
         // Rename selected profile and display profilespage.
       String newprofilename = request.getParameter(TEXTFIELD_PROFILENAME);
       String oldprofilename = request.getParameter(SELECT_PROFILE);
       if(oldprofilename != null && newprofilename != null){
         if(!newprofilename.trim().equals("") && !oldprofilename.trim().equals("")){
           if(!oldprofilename.equals(EndEntityProfileDataHandler.EMPTY_PROFILE)){ 
             try{
               ejbcarabean.renameEndEntityProfile(oldprofilename.trim(),newprofilename.trim());
             }catch( EndEntityProfileExistsException e){
               profileexists=true;
             }
           }else{
              triedtoeditemptyprofile=true;
           }        
         }
       }      
       includefile="endentityprofilespage.jsp"; 
      }
      if( request.getParameter(BUTTON_ADD_PROFILE) != null){
         // Add profile and display profilespage.
         profile = request.getParameter(TEXTFIELD_PROFILENAME);
         if(profile != null){
           if(!profile.trim().equals("")){
             try{
               ejbcarabean.addEndEntityProfile(profile.trim());
             }catch( EndEntityProfileExistsException e){
               profileexists=true;
             }
           }      
         }
         includefile="endentityprofilespage.jsp"; 
      }
      if( request.getParameter(BUTTON_CLONE_PROFILE) != null){
         // clone profile and display profilespage.
       String newprofilename = request.getParameter(TEXTFIELD_PROFILENAME);
       String oldprofilename = request.getParameter(SELECT_PROFILE);
       if(oldprofilename != null && newprofilename != null){
         if(!newprofilename.trim().equals("") && !oldprofilename.trim().equals("")){
             if(!oldprofilename.equals(EndEntityProfileDataHandler.EMPTY_PROFILE)){ 
               try{ 
                 ejbcarabean.cloneEndEntityProfile(oldprofilename.trim(),newprofilename.trim());
               }catch( EndEntityProfileExistsException e){
                 profileexists=true;
               }
             }else{
                cannotcloneempty = true;                
             }
         }
       }      
          includefile="endentityprofilespage.jsp"; 
      }
    }
    if( request.getParameter(ACTION).equals(ACTION_EDIT_PROFILE)){
         // Display edit access rules page.
       profile = request.getParameter(HIDDEN_PROFILENAME);
       if(profile != null){
         if(!profile.trim().equals("")){
           if(request.getParameter(BUTTON_SAVE) != null || 
              request.getParameter(BUTTON_DELETESUBJECTDN) != null ||
              request.getParameter(BUTTON_ADDSUBJECTDN) != null ||
              request.getParameter(BUTTON_DELETESUBJECTALTNAME) != null ||
              request.getParameter(BUTTON_ADDSUBJECTALTNAME) != null){
               
             profiledata = ejbcarabean.getTemporaryEndEntityProfile();
             if(profiledata == null){
               profiledata = ejbcarabean.getEndEntityProfile(profile);
             }
             // Save changes.
             profiledata.setValue(EndEntityProfile.USERNAME , 0, request.getParameter(TEXTFIELD_USERNAME));
             profiledata.setRequired(EndEntityProfile.USERNAME, 0 ,ejbcarabean.getEndEntityParameter(request.getParameter(CHECKBOX_REQUIRED_USERNAME)));
             profiledata.setModifyable(EndEntityProfile.USERNAME, 0 ,ejbcarabean.getEndEntityParameter(request.getParameter(CHECKBOX_MODIFYABLE_USERNAME)));

             profiledata.setValue(EndEntityProfile.PASSWORD, 0  ,request.getParameter(TEXTFIELD_PASSWORD));
             profiledata.setUse(EndEntityProfile.PASSWORD, 0  , !ejbcarabean.getEndEntityParameter(request.getParameter(CHECKBOX_USE_PASSWORD)));
             profiledata.setRequired(EndEntityProfile.PASSWORD, 0  ,ejbcarabean.getEndEntityParameter(request.getParameter(CHECKBOX_REQUIRED_PASSWORD)));
             profiledata.setModifyable(EndEntityProfile.PASSWORD, 0 , true);
 
             profiledata.setValue(EndEntityProfile.CLEARTEXTPASSWORD, 0  ,request.getParameter(CHECKBOX_CLEARTEXTPASSWORD));
             profiledata.setRequired(EndEntityProfile.CLEARTEXTPASSWORD, 0 ,ejbcarabean.getEndEntityParameter(request.getParameter(CHECKBOX_REQUIRED_CLEARTEXTPASSWORD))); 
             profiledata.setUse(EndEntityProfile.CLEARTEXTPASSWORD, 0 ,ejbcarabean.getEndEntityParameter(request.getParameter(CHECKBOX_USE_CLEARTEXTPASSWORD))); 

             numberofsubjectdnfields = profiledata.getSubjectDNFieldOrderLength();

             for(int i=0; i < numberofsubjectdnfields; i ++){
                fielddata = profiledata.getSubjectDNFieldsInOrder(i);
                profiledata.setValue(fielddata[EndEntityProfile.FIELDTYPE],fielddata[EndEntityProfile.NUMBER] , request.getParameter(TEXTFIELD_SUBJECTDN + i));                
                profiledata.setRequired(fielddata[EndEntityProfile.FIELDTYPE],fielddata[EndEntityProfile.NUMBER] , 
                                        ejbcarabean.getEndEntityParameter(request.getParameter(CHECKBOX_REQUIRED_SUBJECTDN + i)));
                profiledata.setModifyable(fielddata[EndEntityProfile.FIELDTYPE],fielddata[EndEntityProfile.NUMBER] , 
                                        ejbcarabean.getEndEntityParameter(request.getParameter(CHECKBOX_MODIFYABLE_SUBJECTDN + i)));
             }

             numberofsubjectaltnamefields = profiledata.getSubjectAltNameFieldOrderLength();

             for(int i=0; i < numberofsubjectaltnamefields; i ++){
                fielddata = profiledata.getSubjectAltNameFieldsInOrder(i);
                profiledata.setValue(fielddata[EndEntityProfile.FIELDTYPE],fielddata[EndEntityProfile.NUMBER] , request.getParameter(TEXTFIELD_SUBJECTALTNAME + i));                
                profiledata.setRequired(fielddata[EndEntityProfile.FIELDTYPE],fielddata[EndEntityProfile.NUMBER] , 
                                        ejbcarabean.getEndEntityParameter(request.getParameter(CHECKBOX_REQUIRED_SUBJECTALTNAME + i)));
                profiledata.setModifyable(fielddata[EndEntityProfile.FIELDTYPE],fielddata[EndEntityProfile.NUMBER] , 
                                        ejbcarabean.getEndEntityParameter(request.getParameter(CHECKBOX_MODIFYABLE_SUBJECTALTNAME + i)));
             } 
            

             profiledata.setValue(EndEntityProfile.EMAIL, 0,request.getParameter(TEXTFIELD_EMAIL));
             profiledata.setRequired(EndEntityProfile.EMAIL, 0,ejbcarabean.getEndEntityParameter(request.getParameter(CHECKBOX_REQUIRED_EMAIL)));
             profiledata.setModifyable(EndEntityProfile.EMAIL, 0,ejbcarabean.getEndEntityParameter(request.getParameter(CHECKBOX_MODIFYABLE_EMAIL))); 
             profiledata.setUse(EndEntityProfile.EMAIL, 0,ejbcarabean.getEndEntityParameter(request.getParameter(CHECKBOX_USE_EMAIL))); 
 
             if(ejbcarabean.getEndEntityParameter(request.getParameter(CHECKBOX_ADMINISTRATOR)))
               profiledata.setValue(EndEntityProfile.ADMINISTRATOR, 0 ,EndEntityProfile.TRUE);
             else
               profiledata.setValue(EndEntityProfile.ADMINISTRATOR, 0 ,EndEntityProfile.FALSE);

             profiledata.setRequired(EndEntityProfile.ADMINISTRATOR, 0 ,ejbcarabean.getEndEntityParameter(request.getParameter(CHECKBOX_REQUIRED_ADMINISTRATOR)));
             profiledata.setUse(EndEntityProfile.ADMINISTRATOR, 0 ,ejbcarabean.getEndEntityParameter(request.getParameter(CHECKBOX_USE_ADMINISTRATOR))); 

             if(ejbcarabean.getEndEntityParameter(request.getParameter(CHECKBOX_KEYRECOVERABLE)))
               profiledata.setValue(EndEntityProfile.KEYRECOVERABLE, 0 ,EndEntityProfile.TRUE);
             else
               profiledata.setValue(EndEntityProfile.KEYRECOVERABLE, 0 ,EndEntityProfile.FALSE);
             profiledata.setRequired(EndEntityProfile.KEYRECOVERABLE, 0 ,ejbcarabean.getEndEntityParameter(request.getParameter(CHECKBOX_REQUIRED_KEYRECOVERABLE)));
             profiledata.setUse(EndEntityProfile.KEYRECOVERABLE, 0 ,ejbcarabean.getEndEntityParameter(request.getParameter(CHECKBOX_USE_KEYRECOVERABLE))); 
 
             if(ejbcarabean.getEndEntityParameter(request.getParameter(CHECKBOX_SENDNOTIFICATION)))
               profiledata.setValue(EndEntityProfile.SENDNOTIFICATION, 0 ,EndEntityProfile.TRUE);
             else
               profiledata.setValue(EndEntityProfile.SENDNOTIFICATION, 0 ,EndEntityProfile.FALSE);
             profiledata.setRequired(EndEntityProfile.SENDNOTIFICATION, 0 ,ejbcarabean.getEndEntityParameter(request.getParameter(CHECKBOX_REQUIRED_SENDNOTIFICATION)));
             profiledata.setUse(EndEntityProfile.SENDNOTIFICATION, 0 ,ejbcarabean.getEndEntityParameter(request.getParameter(CHECKBOX_USE_SENDNOTIFICATION))); 

             String defaultcertprof =  request.getParameter(SELECT_DEFAULTCERTPROFILE);
             profiledata.setValue(EndEntityProfile.DEFAULTCERTPROFILE, 0,defaultcertprof);
             profiledata.setRequired(EndEntityProfile.DEFAULTCERTPROFILE, 0,true);

             String[] values = request.getParameterValues(SELECT_AVAILABLECERTPROFILES);
 
             if(defaultcertprof != null){
               String availablecert =defaultcertprof;
               if(values!= null){
                 for(int i=0; i< values.length; i++){
                     if(!values[i].equals(defaultcertprof))
                       availablecert += EndEntityProfile.SPLITCHAR + values[i];                      
                 }
               } 
               
               profiledata.setValue(EndEntityProfile.AVAILCERTPROFILES, 0,availablecert);
               profiledata.setRequired(EndEntityProfile.AVAILCERTPROFILES, 0,true);    
             }

             String defaultca =  request.getParameter(SELECT_DEFAULTCA);
             profiledata.setValue(EndEntityProfile.DEFAULTCA, 0,defaultca);
             profiledata.setRequired(EndEntityProfile.DEFAULTCA, 0,true);

             values = request.getParameterValues(SELECT_AVAILABLECAS);
 
             if(defaultca != null){
               String availablecas = defaultca;
               if(values!= null){
                 for(int i=0; i< values.length; i++){
                     if(!values[i].equals(defaultca))
                       availablecas += EndEntityProfile.SPLITCHAR + values[i];                      
                 }
               } 
               
               profiledata.setValue(EndEntityProfile.AVAILCAS, 0,availablecas);
               profiledata.setRequired(EndEntityProfile.AVAILCAS, 0,true);    
             }


             String defaulttokentype =  request.getParameter(SELECT_DEFAULTTOKENTYPE);
             profiledata.setValue(EndEntityProfile.DEFKEYSTORE, 0,defaulttokentype);
             profiledata.setRequired(EndEntityProfile.DEFKEYSTORE, 0,true);

             values = request.getParameterValues(SELECT_AVAILABLETOKENTYPES);
 
             if(defaulttokentype != null){
               String availabletokentypes =defaulttokentype;
               if(values!= null){
                 for(int i=0; i< values.length; i++){
                     if(!values[i].equals(defaulttokentype))
                       availabletokentypes += EndEntityProfile.SPLITCHAR + values[i];                      
                 }
               } 
               profiledata.setValue(EndEntityProfile.AVAILKEYSTORE, 0, availabletokentypes);
               profiledata.setRequired(EndEntityProfile.AVAILKEYSTORE, 0, true);    
             }

             profiledata.setUse(EndEntityProfile.AVAILTOKENISSUER, 0 ,ejbcarabean.getEndEntityParameter(request.getParameter(CHECKBOX_USE_HARDTOKENISSUERS))); 

             String defaulthardtokenissuer =  request.getParameter(SELECT_DEFAULTHARDTOKENISSUER);
             profiledata.setValue(EndEntityProfile.DEFAULTTOKENISSUER, 0,defaulthardtokenissuer);
             profiledata.setRequired(EndEntityProfile.DEFAULTTOKENISSUER, 0,true);

             values = request.getParameterValues(SELECT_AVAILABLEHARDTOKENISSUERS);
 
             if(defaulthardtokenissuer != null){
               String availablehardtokenissuers =defaulthardtokenissuer;
               if(values!= null){
                 for(int i=0; i< values.length; i++){
                     if(!values[i].equals(defaulthardtokenissuer))
                       availablehardtokenissuers += EndEntityProfile.SPLITCHAR + values[i];                      
                 }
               } 
               profiledata.setValue(EndEntityProfile.AVAILTOKENISSUER, 0, availablehardtokenissuers);
               profiledata.setRequired(EndEntityProfile.AVAILTOKENISSUER, 0, true);    
             }

             profiledata.setNotificationSender(request.getParameter(TEXTFIELD_NOTIFICATIONSENDER));
             profiledata.setNotificationSubject(request.getParameter(TEXTFIELD_NOTIFICATIONSUBJECT));
             profiledata.setNotificationMessage(request.getParameter(TEXTAREA_NOTIFICATIONMESSAGE));
          
             if(request.getParameter(BUTTON_DELETESUBJECTDN) != null){  
               numberofsubjectdnfields = profiledata.getSubjectDNFieldOrderLength();
               int pointer = 0;
               for(int i=0; i < numberofsubjectdnfields; i++){
                 if(ejbcarabean.getEndEntityParameter(request.getParameter(CHECKBOX_SELECTSUBJECTDN + i))){
                   fielddata = profiledata.getSubjectDNFieldsInOrder(pointer);  
                   profiledata.removeField(fielddata[EndEntityProfile.FIELDTYPE],fielddata[EndEntityProfile.NUMBER]);
                 }
                 else
                   pointer++;
               }                          
             }
             if(request.getParameter(BUTTON_ADDSUBJECTDN) != null){             
               value = request.getParameter(SELECT_ADDSUBJECTDN);
               if(value!=null){
                 profiledata.addField(Integer.parseInt(value));             
               }                   
             }
             if(request.getParameter(BUTTON_DELETESUBJECTALTNAME) != null){             
               numberofsubjectaltnamefields = profiledata.getSubjectAltNameFieldOrderLength();
               int pointer = 0;
               for(int i=0; i < numberofsubjectaltnamefields; i++){
                 if(ejbcarabean.getEndEntityParameter(request.getParameter(CHECKBOX_SELECTSUBJECTALTNAME+i))){
                   fielddata = profiledata.getSubjectAltNameFieldsInOrder(pointer);  
                   profiledata.removeField(fielddata[EndEntityProfile.FIELDTYPE],fielddata[EndEntityProfile.NUMBER]);
                 }
                 else
                   pointer++;
               }             
             }
             if(request.getParameter(BUTTON_ADDSUBJECTALTNAME) != null){             
               value = request.getParameter(SELECT_ADDSUBJECTALTNAME);
               if(value!=null){
                 profiledata.addField(Integer.parseInt(value));                
               }                       
             }
             includefile="endentityprofilepage.jsp";
             ejbcarabean.setTemporaryEndEntityProfile(profiledata);
           
             if(request.getParameter(BUTTON_SAVE) != null){             
               ejbcarabean.changeEndEntityProfile(profile,profiledata);
               ejbcarabean.setTemporaryEndEntityProfile(null);
               includefile="endentityprofilespage.jsp";  
             }
           }
           if(request.getParameter(BUTTON_CANCEL) != null){
              // Don't save changes.
             ejbcarabean.setTemporaryEndEntityProfile(null);
             includefile="endentityprofilespage.jsp";
           }
         }
      }
    }
  }
 // Include page
  if( includefile.equals("endentityprofilepage.jsp")){ %>
   <%@ include file="endentityprofilepage.jsp" %>
<%}
  if( includefile.equals("endentityprofilespage.jsp")){ %>
   <%@ include file="endentityprofilespage.jsp" %> 
<%}

   // Include Footer 
   String footurl =   globalconfiguration.getFootBanner(); %>
   
  <jsp:include page="<%= footurl %>" />

</body>
</html>
