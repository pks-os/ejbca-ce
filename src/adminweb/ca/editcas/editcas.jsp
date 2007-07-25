<%@ page pageEncoding="ISO-8859-1"%>
<%@ page contentType="text/html; charset=@page.encoding@" %>
<%@page errorPage="/errorpage.jsp" import="java.util.*, java.io.*, org.apache.commons.fileupload.*, org.ejbca.ui.web.admin.configuration.EjbcaWebBean,org.ejbca.core.model.ra.raadmin.GlobalConfiguration, org.ejbca.core.model.SecConst, org.ejbca.util.FileTools, org.ejbca.util.CertTools, org.ejbca.core.model.authorization.AuthorizationDeniedException,
    org.ejbca.ui.web.RequestHelper, org.ejbca.ui.web.admin.cainterface.CAInterfaceBean, org.ejbca.core.model.ca.caadmin.CAInfo, org.ejbca.core.model.ca.caadmin.X509CAInfo, org.ejbca.core.model.ca.catoken.CATokenInfo, org.ejbca.core.model.ca.catoken.SoftCAToken, org.ejbca.core.model.ca.catoken.SoftCATokenInfo, org.ejbca.ui.web.admin.cainterface.CADataHandler,
               org.ejbca.ui.web.admin.rainterface.RevokedInfoView, org.ejbca.ui.web.admin.configuration.InformationMemory, org.bouncycastle.asn1.x509.X509Name, org.bouncycastle.jce.PKCS10CertificationRequest, org.ejbca.core.EjbcaException,
               org.ejbca.core.protocol.PKCS10RequestMessage, org.ejbca.core.model.ca.caadmin.CAExistsException, org.ejbca.core.model.ca.caadmin.CADoesntExistsException, org.ejbca.core.model.ca.catoken.CATokenOfflineException, org.ejbca.core.model.ca.catoken.CATokenAuthenticationFailedException,
               org.ejbca.core.model.ca.caadmin.extendedcaservices.OCSPCAServiceInfo,org.ejbca.core.model.ca.caadmin.extendedcaservices.XKMSCAServiceInfo, org.ejbca.core.model.ca.caadmin.extendedcaservices.CmsCAServiceInfo, org.ejbca.core.model.ca.caadmin.extendedcaservices.ExtendedCAServiceInfo, org.ejbca.core.model.ca.catoken.HardCATokenManager, org.ejbca.core.model.ca.catoken.AvailableHardCAToken, org.ejbca.core.model.ca.catoken.HardCATokenInfo, org.ejbca.core.model.ca.catoken.CATokenConstants,
               org.ejbca.util.dn.DNFieldExtractor,org.ejbca.core.model.ca.catoken.IHardCAToken " %>

<html>
<jsp:useBean id="ejbcawebbean" scope="session" class="org.ejbca.ui.web.admin.configuration.EjbcaWebBean" />
<jsp:useBean id="cabean" scope="session" class="org.ejbca.ui.web.admin.cainterface.CAInterfaceBean" />

<%! // Declarations 
  static final String ACTION                              = "action";
  static final String ACTION_EDIT_CAS                     = "editcas";
  static final String ACTION_EDIT_CA                      = "editca";
  static final String ACTION_CREATE_CA                    = "createca";
  static final String ACTION_CHOOSE_CATYPE                = "choosecatype";
  static final String ACTION_CHOOSE_CATOKENTYPE           = "choosecatokentype";
  static final String ACTION_MAKEREQUEST                  = "makerequest";
  static final String ACTION_RECEIVERESPONSE              = "receiveresponse";
  static final String ACTION_PROCESSREQUEST               = "processrequest";
  static final String ACTION_PROCESSREQUEST2              = "processrequest2";
  static final String ACTION_RENEWCA_MAKEREQUEST          = "renewcamakeresponse";  
  static final String ACTION_RENEWCA_RECIEVERESPONSE      = "renewcarecieveresponse";  
  static final String ACTION_IMPORTCA		              = "importca";

  static final String CHECKBOX_VALUE           = "true";

//  Used in choosecapage.jsp
  static final String BUTTON_EDIT_CA                       = "buttoneditca"; 
  static final String BUTTON_DELETE_CA                     = "buttondeleteca";
  static final String BUTTON_CREATE_CA                     = "buttoncreateca"; 
  static final String BUTTON_RENAME_CA                     = "buttonrenameca";
  static final String BUTTON_PROCESSREQUEST                = "buttonprocessrequest";
  static final String BUTTON_IMPORTCA		               = "buttonimportca";
  static final String BUTTON_EXPORTCA		               = "buttonexportca";
  

  static final String SELECT_CAS                           = "selectcas";
  static final String TEXTFIELD_CANAME                     = "textfieldcaname";
  static final String HIDDEN_CANAME                        = "hiddencaname";
  static final String HIDDEN_CAID                          = "hiddencaid";
  static final String HIDDEN_CATYPE                        = "hiddencatype";
  static final String HIDDEN_CATOKENPATH                   = "hiddencatokenpath";
  static final String HIDDEN_CATOKENTYPE                   = "hiddencatokentype";
 
// Buttons used in editcapage.jsp
  static final String BUTTON_SAVE                       = "buttonsave";
  static final String BUTTON_CREATE                     = "buttoncreate";
  static final String BUTTON_CANCEL                     = "buttoncancel";
  static final String BUTTON_MAKEREQUEST                = "buttonmakerequest";
  static final String BUTTON_RECEIVEREQUEST             = "buttonreceiverequest";
  static final String BUTTON_RENEWCA                    = "buttonrenewca";
  static final String BUTTON_REVOKECA                   = "buttonrevokeca";  
  static final String BUTTON_RECIEVEFILE                = "buttonrecievefile";     
  static final String BUTTON_PUBLISHCA                  = "buttonpublishca";     
  static final String BUTTON_REVOKERENEWOCSPCERTIFICATE = "checkboxrenewocspcertificate";
  static final String BUTTON_REVOKERENEWXKMSCERTIFICATE = "checkboxrenewxkmscertificate";
  static final String BUTTON_REVOKERENEWCMSCERTIFICATE  = "checkboxrenewcmscertificate";
  static final String BUTTON_GENDEFAULTCRLDISTPOINT     = "checkboxgeneratedefaultcrldistpoint";
  static final String BUTTON_GENDEFAULTOCSPLOCATOR      = "checkbexgeneratedefaultocsplocator";

  static final String TEXTFIELD_SUBJECTDN             = "textfieldsubjectdn";
  static final String TEXTFIELD_SUBJECTALTNAME        = "textfieldsubjectaltname";  
  static final String TEXTFIELD_CRLPERIOD             = "textfieldcrlperiod";
  static final String TEXTFIELD_CRLISSUEINTERVAL      = "textfieldcrlissueinterval";
  static final String TEXTFIELD_CRLOVERLAPTIME        = "textfieldcrloverlaptime";
  static final String TEXTFIELD_DESCRIPTION           = "textfielddescription";
  static final String TEXTFIELD_VALIDITY              = "textfieldvalidity";
  static final String TEXTFIELD_POLICYID              = "textfieldpolicyid";
  static final String TEXTFIELD_HARDCATOKENPROPERTIES = "textfieldhardcatokenproperties";
  static final String TEXTFIELD_AUTHENTICATIONCODE    = "textfieldauthenticationcode";
  static final String TEXTFIELD_DEFAULTCRLDISTPOINT   = "textfielddefaultcrldistpoint";
  static final String TEXTFIELD_DEFAULTCRLISSUER      = "textfielddefaultcrlissuer";
  static final String TEXTFIELD_DEFAULTOCSPLOCATOR    = "textfielddefaultocsplocator";
  static final String TEXTFIELD_KEYSPEC               = "textfieldkeyspec";
  static final String TEXTFIELD_IMPORTCA_PASSWORD	  = "textfieldimportcapassword";
  static final String TEXTFIELD_IMPORTCA_SIGKEYALIAS  = "textfieldimportcasigkeyalias";
  static final String TEXTFIELD_IMPORTCA_ENCKEYALIAS  = "textfieldimportcaenckeyalias";
  static final String TEXTFIELD_IMPORTCA_NAME		  = "textfieldimportcaname";


  static final String CHECKBOX_AUTHORITYKEYIDENTIFIER             = "checkboxauthoritykeyidentifier";
  static final String CHECKBOX_AUTHORITYKEYIDENTIFIERCRITICAL     = "checkboxauthoritykeyidentifiercritical";
  static final String CHECKBOX_USECRLNUMBER                       = "checkboxusecrlnumber";
  static final String CHECKBOX_CRLNUMBERCRITICAL                  = "checkboxcrlnumbercritical";
  static final String CHECKBOX_FINISHUSER                         = "checkboxfinishuser";
  static final String CHECKBOX_USEUTF8POLICYTEXT                  = "checkboxuseutf8policytext";
  static final String CHECKBOX_USEPRINTABLESTRINGSUBJECTDN        = "checkboxuseprintablestringsubjectdn";
  
  static final String CHECKBOX_ACTIVATEOCSPSERVICE                = "checkboxactivateocspservice";  
  static final String CHECKBOX_ACTIVATEXKMSSERVICE                = "checkboxactivatexkmsservice";
  static final String CHECKBOX_ACTIVATECMSSERVICE                 = "checkboxactivatecmsservice";
  static final String CHECKBOX_RENEWKEYS                          = "checkboxrenewkeys";  
  static final String CHECKBOX_AUTHENTICATIONCODEAUTOACTIVATE     = "checkboxauthcodeautoactivate";
  
  static final String HIDDEN_CATOKEN                              = "hiddencatoken";  
  
  static final String SELECT_REVOKEREASONS                        = "selectrevokereasons";
  static final String SELECT_CATYPE                               = "selectcatype";  
  static final String SELECT_CATOKEN                              = "selectcatoken";
  static final String SELECT_SIGNEDBY                             = "selectsignedby";  
  static final String SELECT_KEYSIZE                              = "selectsize";
  static final String SELECT_AVAILABLECRLPUBLISHERS               = "selectavailablecrlpublishers";
  static final String SELECT_CERTIFICATEPROFILE                   = "selectcertificateprofile";
  static final String SELECT_SIGNATUREALGORITHM                   = "selectsignaturealgorithm";
  static final String SELECT_APPROVALSETTINGS                     = "approvalsettings";
  static final String SELECT_NUMOFREQUIREDAPPROVALS               = "numofrequiredapprovals";

  static final String FILE_RECIEVEFILE                            = "filerecievefile";
  static final String FILE_CACERTFILE                             = "filecacertfile";
  static final String FILE_REQUESTFILE                            = "filerequestfile";   

  static final String CERTSERNO_PARAMETER       = "certsernoparameter"; 

  static final int    MAKEREQUESTMODE     = 0;
  static final int    RECIEVERESPONSEMODE = 1;
  static final int    PROCESSREQUESTMODE  = 2;   
  
  static final int    CERTREQGENMODE      = 0;
  static final int    CERTGENMODE         = 1;
%>
<% 
         
  // Initialize environment
  int caid = 0;
  String caname = null;
  String includefile = "choosecapage.jspf"; 
  String processedsubjectdn = "";
  int catype = CAInfo.CATYPE_X509;  // default
  int catokentype = CATokenInfo.CATOKENTYPE_P12; // default
  String catokenpath = "NONE";
  String importcaname = null;
  String importpassword = null;
  String importsigalias = null;
  String importencalias = null;

  InputStream file = null;

  boolean  caexists             = false;
  boolean  cadeletefailed       = false;
  boolean  illegaldnoraltname   = false;
  boolean  errorrecievingfile   = false;
  boolean  ocsprenewed          = false;
  boolean  xkmsrenewed          = false;
  boolean  cmsrenewed           = false;
  boolean  catokenoffline       = false;
  boolean  catokenauthfailed    = false;
  String errormessage = null;
  

  GlobalConfiguration globalconfiguration = ejbcawebbean.initialize(request, "/super_administrator"); 
                                            cabean.initialize(request, ejbcawebbean); 

  CADataHandler cadatahandler     = cabean.getCADataHandler(); 
  String THIS_FILENAME            =  globalconfiguration.getCaPath()  + "/editcas/editcas.jsp";
  String action = "";

  final String VIEWCERT_LINK            = ejbcawebbean.getBaseUrl() + globalconfiguration.getAdminWebPath() + "viewcertificate.jsp";
  
  boolean issuperadministrator = false;
  boolean editca = false;
  boolean processrequest = false;
  boolean buttoncancel = false; 
  boolean caactivated = false;
  boolean carenewed = false;
  boolean capublished = false;

  int filemode = 0;
  int row = 0;

  HashMap caidtonamemap = cabean.getCAIdToNameMap();
  InformationMemory info = ejbcawebbean.getInformationMemory();

%>
 
<head>
  <title><%= globalconfiguration .getEjbcaTitle() %></title>
  <base href="<%= ejbcawebbean.getBaseUrl() %>">
  <link rel=STYLESHEET href="<%= ejbcawebbean.getCssFile() %>">
  <script language=javascript src="<%= globalconfiguration.getAdminWebPath() %>ejbcajslib.js"></script>
</head>


<%
  RequestHelper.setDefaultCharacterEncoding(request);

   if(FileUpload.isMultipartContent(request)){     
     errorrecievingfile = true;
     DiskFileUpload upload = new DiskFileUpload();
     upload.setSizeMax(60000);                   
     upload.setSizeThreshold(59999);
     List /* FileItem */ items = upload.parseRequest(request);     

     Iterator iter = items.iterator();
     while (iter.hasNext()) {     
     FileItem item = (FileItem) iter.next();


       if (item.isFormField()) {         
         if(item.getFieldName().equals(ACTION))
           action = item.getString(); 
         if(item.getFieldName().equals(HIDDEN_CAID))
           caid = Integer.parseInt(item.getString());
         if(item.getFieldName().equals(HIDDEN_CANAME))
           caname = item.getString();
         if(item.getFieldName().equals(BUTTON_CANCEL))
           buttoncancel = true; 
         if(item.getFieldName().equals(TEXTFIELD_IMPORTCA_NAME))
           importcaname = item.getString();
         if(item.getFieldName().equals(TEXTFIELD_IMPORTCA_PASSWORD))
           importpassword = item.getString();
         if(item.getFieldName().equals(TEXTFIELD_IMPORTCA_SIGKEYALIAS))
           importsigalias = item.getString();
         if(item.getFieldName().equals(TEXTFIELD_IMPORTCA_ENCKEYALIAS))
           importencalias = item.getString();
       }else{         
         file = item.getInputStream(); 
         errorrecievingfile = false;                          
       }
     } 
   }else{
     action = request.getParameter(ACTION);
   }
  try{
  // Determine action 
  if( action != null){
    if( action.equals(ACTION_EDIT_CAS)){
      // Actions in the choose CA page.
      if( request.getParameter(BUTTON_EDIT_CA) != null){
          // Display  profilepage.jsp         
         includefile="choosecapage.jspf";
         if(request.getParameter(SELECT_CAS) != null && !request.getParameter(SELECT_CAS).equals("")){
           caid = Integer.parseInt(request.getParameter(SELECT_CAS));
           if(caid != 0){             
             editca = true;
             includefile="editcapage.jspf";              
           }
         } 
      }
      if( request.getParameter(BUTTON_DELETE_CA) != null) {
          // Delete profile and display choosecapage. 
          if(request.getParameter(SELECT_CAS) != null && !request.getParameter(SELECT_CAS).equals("")){
            caid = Integer.parseInt(request.getParameter(SELECT_CAS));
            if(caid != 0){             
                cadeletefailed = !cadatahandler.removeCA(caid);
            }
          }
          includefile="choosecapage.jspf";             
      }
      if( request.getParameter(BUTTON_RENAME_CA) != null){ 
         // Rename selected profile and display profilespage.
       if(request.getParameter(SELECT_CAS) != null  && !request.getParameter(SELECT_CAS).equals("") && request.getParameter(TEXTFIELD_CANAME) != null){
         String newcaname = request.getParameter(TEXTFIELD_CANAME).trim();
         String oldcaname = (String) caidtonamemap.get(new Integer(request.getParameter(SELECT_CAS)));    
         if(!newcaname.equals("") ){           
           try{
             cadatahandler.renameCA(oldcaname, newcaname);
           }catch( CAExistsException e){
             caexists=true;
           }                
         }
        }      
        includefile="choosecapage.jspf"; 
      }
      if( request.getParameter(BUTTON_IMPORTCA) != null){ 
         // Import CA from p12-file. Start by prompting for file and keystore password.
		includefile="importca.jspf";
      }
      if( request.getParameter(BUTTON_CREATE_CA) != null){
         // Add profile and display profilespage.
         includefile="choosecapage.jspf"; 
         caname = request.getParameter(TEXTFIELD_CANAME);
         if(caname != null){
           caname = caname.trim();
           if(!caname.equals("")){             
             editca = false;
             includefile="editcapage.jspf";              
           }      
         }         
      }
      if( request.getParameter(BUTTON_PROCESSREQUEST) != null){
         caname = request.getParameter(TEXTFIELD_CANAME);
         if(caname != null){
           caname = caname.trim();
           if(!caname.equals("")){             
             filemode = PROCESSREQUESTMODE;
             includefile="recievefile.jspf";               
           }      
         }                        
      }
    }
    if( action.equals(ACTION_CREATE_CA)){
      if( request.getParameter(BUTTON_CREATE)  != null || request.getParameter(BUTTON_MAKEREQUEST)  != null){
         // Create and save CA                          
         caname = request.getParameter(HIDDEN_CANAME);
          
         CATokenInfo catoken = null;
         catokentype = Integer.parseInt(request.getParameter(HIDDEN_CATOKENTYPE));
         String signkeyspec = "2048"; // Default signature key, for OCSP, CMS and XKMS, is 2048 bit RSA
         String signkeytype = CATokenConstants.KEYALGORITHM_RSA;
         
         if(catokentype == CATokenInfo.CATOKENTYPE_P12){
           String signalg = request.getParameter(SELECT_SIGNATUREALGORITHM);
           String encalg = request.getParameter(SELECT_SIGNATUREALGORITHM);
           signkeyspec = request.getParameter(SELECT_KEYSIZE);
           String authenticationcode = request.getParameter(TEXTFIELD_AUTHENTICATIONCODE);
           String autoactivate = request.getParameter(CHECKBOX_AUTHENTICATIONCODEAUTOACTIVATE);
           signkeytype = CATokenConstants.KEYALGORITHM_RSA;
           String enckeyspec = request.getParameter(SELECT_KEYSIZE);
           String enckeytype = CATokenConstants.KEYALGORITHM_RSA;
           if (signalg.indexOf("ECDSA") != -1) {
        	   signkeyspec = request.getParameter(TEXTFIELD_KEYSPEC);
               signkeytype = CATokenConstants.KEYALGORITHM_ECDSA;
               encalg = CATokenConstants.SIGALG_SHA1_WITH_RSA;
           }
           if(signkeyspec == null || signalg == null)
             throw new Exception("Error in CATokenData");  
           catoken = new SoftCATokenInfo();
           catoken.setSignatureAlgorithm(signalg);
           ((SoftCATokenInfo) catoken).setSignKeyAlgorithm(signkeytype);
           ((SoftCATokenInfo) catoken).setSignKeySpec(signkeyspec);              
           catoken.setEncryptionAlgorithm(encalg);
           ((SoftCATokenInfo) catoken).setEncKeyAlgorithm(enckeytype);
           ((SoftCATokenInfo) catoken).setEncKeySpec(enckeyspec); 
           catoken.setAuthenticationCode(authenticationcode);
           if ( (autoactivate != null) && (autoactivate.equals("true")) ) {
               String properties = IHardCAToken.AUTOACTIVATE_PIN_PROPERTY + " " + authenticationcode;
               catoken.setProperties(properties);
           }          
         } 
         if(catokentype == CATokenInfo.CATOKENTYPE_HSM){
            catokenpath = request.getParameter(HIDDEN_CATOKENPATH);
            String properties = request.getParameter(TEXTFIELD_HARDCATOKENPROPERTIES);
            String signalg = request.getParameter(SELECT_SIGNATUREALGORITHM);
            String authenticationcode = request.getParameter(TEXTFIELD_AUTHENTICATIONCODE);
            if(catokenpath == null || catokenpath == null || signalg == null)
              throw new Exception("Error in CATokenData");  
            catoken = new HardCATokenInfo();           
            catoken.setClassPath(catokenpath);
            catoken.setProperties(properties);
            catoken.setSignatureAlgorithm(signalg);
            catoken.setAuthenticationCode(authenticationcode);
         }

         catype  = Integer.parseInt(request.getParameter(HIDDEN_CATYPE));
         String subjectdn = request.getParameter(TEXTFIELD_SUBJECTDN);
         try{
             X509Name dummy = CertTools.stringToBcX509Name(subjectdn);
         }catch(Exception e){
             illegaldnoraltname = true;
         }
         int certprofileid = 0;
         if(request.getParameter(SELECT_CERTIFICATEPROFILE) != null)
           certprofileid = Integer.parseInt(request.getParameter(SELECT_CERTIFICATEPROFILE));
         int signedby = 0;
         if(request.getParameter(SELECT_SIGNEDBY) != null)
            signedby = Integer.parseInt(request.getParameter(SELECT_SIGNEDBY));
         String description = request.getParameter(TEXTFIELD_DESCRIPTION);        
         if(description == null)
           description = "";
         
         int validity = 0;
         if(request.getParameter(TEXTFIELD_VALIDITY) != null)
           validity = Integer.parseInt(request.getParameter(TEXTFIELD_VALIDITY));  

         if(catoken != null && catype != 0 && subjectdn != null && caname != null 
            && signedby != 0  ){
           if(catype == CAInfo.CATYPE_X509){
              // Create a X509 CA
              String subjectaltname = request.getParameter(TEXTFIELD_SUBJECTALTNAME);             
              if(subjectaltname == null)
                subjectaltname = ""; 
              else{
                if(!subjectaltname.trim().equals("")){
                   DNFieldExtractor subtest = 
                     new DNFieldExtractor(subjectaltname,DNFieldExtractor.TYPE_SUBJECTALTNAME);                   
                   if(subtest.isIllegal() || subtest.existsOther()){
                     illegaldnoraltname = true;
                   }
                }
              }    

              String policyid = request.getParameter(TEXTFIELD_POLICYID);
              if(policyid == null || policyid.trim().equals(""))
                 policyid = null; 

              int crlperiod = Integer.parseInt(request.getParameter(TEXTFIELD_CRLPERIOD));
              int crlIssueInterval = 0;
              String crlissueint = request.getParameter(TEXTFIELD_CRLISSUEINTERVAL);
              if (crlissueint != null && !crlissueint.trim().equals(""))
                  crlIssueInterval = Integer.parseInt(crlissueint);
              int crlOverlapTime = 0;
              String crloverlapint = request.getParameter(TEXTFIELD_CRLOVERLAPTIME);
              if (crloverlapint != null && !crloverlapint.trim().equals(""))
            	  crlOverlapTime = Integer.parseInt(crloverlapint);
              
              boolean useauthoritykeyidentifier = false;
              boolean authoritykeyidentifiercritical = false;
              String value = request.getParameter(CHECKBOX_AUTHORITYKEYIDENTIFIER);
              if(value != null){
                 useauthoritykeyidentifier = value.equals(CHECKBOX_VALUE);                 
                 value = request.getParameter(CHECKBOX_AUTHORITYKEYIDENTIFIERCRITICAL); 
                 if(value != null){
                   authoritykeyidentifiercritical = value.equals(CHECKBOX_VALUE);
                 } 
                 else
                   authoritykeyidentifiercritical = false;
              }

              boolean usecrlnumber = false;
              boolean crlnumbercritical = false;
              value = request.getParameter(CHECKBOX_USECRLNUMBER);
              if(value != null){
                 usecrlnumber = value.equals(CHECKBOX_VALUE);                 
                 value = request.getParameter(CHECKBOX_CRLNUMBERCRITICAL); 
                 if(value != null){
                   crlnumbercritical = value.equals(CHECKBOX_VALUE);
                 } 
                 else
                   crlnumbercritical = false;
              }              
              
             String defaultcrldistpoint = request.getParameter(TEXTFIELD_DEFAULTCRLDISTPOINT);
             String defaultcrlissuer = request.getParameter(TEXTFIELD_DEFAULTCRLISSUER);
             String defaultocsplocator  = request.getParameter(TEXTFIELD_DEFAULTOCSPLOCATOR);
              
             boolean finishuser = false;
             value = request.getParameter(CHECKBOX_FINISHUSER);
             if(value != null)
               finishuser = value.equals(CHECKBOX_VALUE);         

             boolean useutf8policytext = false;
             value = request.getParameter(CHECKBOX_USEUTF8POLICYTEXT);
             if(value != null) {
            	 useutf8policytext = value.equals(CHECKBOX_VALUE);                             
             }
             boolean useprintablestringsubjectdn = false;
             value = request.getParameter(CHECKBOX_USEPRINTABLESTRINGSUBJECTDN);
             if(value != null) {
            	 useprintablestringsubjectdn = value.equals(CHECKBOX_VALUE);                             
             }

             String[] values = request.getParameterValues(SELECT_AVAILABLECRLPUBLISHERS);
             ArrayList crlpublishers = new ArrayList(); 
             if(values != null){
               for(int i=0; i < values.length; i++){
                  crlpublishers.add(new Integer(values[i]));
               }
             }
             
             values = request.getParameterValues(SELECT_APPROVALSETTINGS);
             ArrayList approvalsettings = new ArrayList(); 
             if(values != null){
               for(int i=0; i < values.length; i++){
            	   approvalsettings.add(new Integer(values[i]));
               }
             }
             
             value = request.getParameter(SELECT_NUMOFREQUIREDAPPROVALS);
             int numofreqapprovals = 1;
             if(value != null){
            	 numofreqapprovals = Integer.parseInt(value);
             }

             int ocspactive = ExtendedCAServiceInfo.STATUS_INACTIVE;
             value = request.getParameter(CHECKBOX_ACTIVATEOCSPSERVICE);
             if(value != null && value.equals(CHECKBOX_VALUE))
                ocspactive = ExtendedCAServiceInfo.STATUS_ACTIVE;
             
             int xkmsactive = ExtendedCAServiceInfo.STATUS_INACTIVE;
             value = request.getParameter(CHECKBOX_ACTIVATEXKMSSERVICE);
             if(value != null && value.equals(CHECKBOX_VALUE))
                xkmsactive = ExtendedCAServiceInfo.STATUS_ACTIVE; 
              
             int cmsactive = ExtendedCAServiceInfo.STATUS_INACTIVE;
             value = request.getParameter(CHECKBOX_ACTIVATECMSSERVICE);
             if(value != null && value.equals(CHECKBOX_VALUE))
                cmsactive = ExtendedCAServiceInfo.STATUS_ACTIVE; 
             
             if(crlperiod != 0 && !illegaldnoraltname){
               if(request.getParameter(BUTTON_CREATE) != null){           
      
		 // Create and active OSCP CA Service.
		 ArrayList extendedcaservices = new ArrayList();
		 String keySpec = signkeyspec;
		 String keyAlg = signkeytype;
		 if (keyAlg.equals(CATokenConstants.KEYALGORITHM_RSA)) {
			 // Never use larger keys than 2048 bit RSA for OCSP, CMS and XKMS signing
			 int len = Integer.parseInt(keySpec);
			 if (len > 2048) {
				 keySpec = "2048";				 
			 }
		 }
		 extendedcaservices.add(
		             new OCSPCAServiceInfo(ocspactive,
						  "CN=OCSPSignerCertificate, " + subjectdn,
			     		  "",
			     		  keySpec,
						  keyAlg));
		 extendedcaservices.add(
	             new XKMSCAServiceInfo(xkmsactive,
					  "CN=XKMSCertificate, " + subjectdn,
		     		  "",
		     		  keySpec,
					  keyAlg));
		 extendedcaservices.add(
	             new CmsCAServiceInfo(cmsactive,
					  "CN=CMSCertificate, " + subjectdn,
		     		  "",
		     		  keySpec,
					  keyAlg));
                 X509CAInfo x509cainfo = new X509CAInfo(subjectdn, caname, 0, new Date(), subjectaltname,
                                                        certprofileid, validity, 
                                                        null, catype, signedby,
                                                        null, catoken, description, -1, null,
                                                        policyid, crlperiod, crlIssueInterval, crlOverlapTime, crlpublishers, 
                                                        useauthoritykeyidentifier, 
                                                        authoritykeyidentifiercritical,
                                                        usecrlnumber, 
                                                        crlnumbercritical, 
                                                        defaultcrldistpoint,
                                                        defaultcrlissuer,
                                                        defaultocsplocator,
                                                        finishuser, extendedcaservices,
                                                        useutf8policytext,
                                                        approvalsettings,
                                                        numofreqapprovals,
                                                        useprintablestringsubjectdn);
                 try{
                   cadatahandler.createCA((CAInfo) x509cainfo);
                 }catch(CAExistsException caee){
                    caexists = true; 
                 }catch(CATokenAuthenticationFailedException catfe){
                    catokenauthfailed = true;
                 }
                 includefile="choosecapage.jspf"; 
               }
               if(request.getParameter(BUTTON_MAKEREQUEST) != null){
                 caid = CertTools.stringToBCDNString(subjectdn).hashCode();  
		 // Create and OSCP CA Service.
		 ArrayList extendedcaservices = new ArrayList();
		 String keySpec = signkeyspec;
		 String keyAlg = signkeytype;
		 if (keyAlg.equals(CATokenConstants.KEYALGORITHM_RSA)) {
			 // Never use larger keys than 2048 bit RSA for OCSP, CMS and XKMS signing
			 int len = Integer.parseInt(keySpec);
			 if (len > 2048) {
				 keySpec = "2048";				 
			 }
		 }
		 extendedcaservices.add(
		             new OCSPCAServiceInfo(ocspactive,
						  "CN=OCSPSignerCertificate, " + subjectdn,
			     		          "",
						  keySpec,
						  keyAlg));
		 extendedcaservices.add(
	             new XKMSCAServiceInfo(xkmsactive,
					  "CN=XKMSCertificate, " + subjectdn,
		     		          "",
					  keySpec,
					  keyAlg));
		 extendedcaservices.add(
	             new CmsCAServiceInfo(cmsactive,
					  "CN=CMSCertificate, " + subjectdn,
		     		          "",
					  keySpec,
					  keyAlg));
                 X509CAInfo x509cainfo = new X509CAInfo(subjectdn, caname, 0, new Date(), subjectaltname,
                                                        certprofileid, validity,
                                                        null, catype, CAInfo.SIGNEDBYEXTERNALCA,
                                                        null, catoken, description, -1, null, 
                                                        policyid, crlperiod, crlIssueInterval, crlOverlapTime, crlpublishers, 
                                                        useauthoritykeyidentifier, 
                                                        authoritykeyidentifiercritical,
                                                        usecrlnumber, 
                                                        crlnumbercritical, 
                                                        defaultcrldistpoint,
                                                        defaultcrlissuer,
                                                        defaultocsplocator,
                                                        finishuser, extendedcaservices,
                                                        useutf8policytext,
                                                        approvalsettings,
                                                        numofreqapprovals,
                                                        useprintablestringsubjectdn);
                 cabean.saveRequestInfo(x509cainfo);                
                 filemode = MAKEREQUESTMODE;
                 includefile="recievefile.jspf"; 
               }
             }                          
           } 
         } 
       } 
       if(request.getParameter(BUTTON_CANCEL) != null){
         // Don't save changes.
         includefile="choosecapage.jspf"; 
       }                        
      }
    if( action.equals(ACTION_EDIT_CA)){
      if( request.getParameter(BUTTON_SAVE)  != null || 
          request.getParameter(BUTTON_RECEIVEREQUEST)  != null || 
          request.getParameter(BUTTON_RENEWCA)  != null ||
          request.getParameter(BUTTON_REVOKECA)  != null ||
          request.getParameter(BUTTON_PUBLISHCA) != null ||
          request.getParameter(BUTTON_REVOKERENEWOCSPCERTIFICATE) != null ||
          request.getParameter(BUTTON_REVOKERENEWCMSCERTIFICATE) != null ||
          request.getParameter(BUTTON_REVOKERENEWXKMSCERTIFICATE) != null){
         // Create and save CA                          
         caid = Integer.parseInt(request.getParameter(HIDDEN_CAID));
         caname = request.getParameter(HIDDEN_CANAME);
         catype = Integer.parseInt(request.getParameter(HIDDEN_CATYPE));
         
         CATokenInfo catoken = null;
         catokentype = Integer.parseInt(request.getParameter(HIDDEN_CATOKENTYPE));
         if(catokentype == CATokenInfo.CATOKENTYPE_P12){
           String authenticationcode = request.getParameter(TEXTFIELD_AUTHENTICATIONCODE);
           String autoactivate = request.getParameter(CHECKBOX_AUTHENTICATIONCODEAUTOACTIVATE);
           catoken = new SoftCATokenInfo();          
           catoken.setAuthenticationCode(authenticationcode);
           if ( (autoactivate != null) && (autoactivate.equals("true")) ) {
               String properties = IHardCAToken.AUTOACTIVATE_PIN_PROPERTY + " " + authenticationcode;
               catoken.setProperties(properties);
           } else {
               catoken.setProperties("");
           }
           
         } 
         if(catokentype == CATokenInfo.CATOKENTYPE_HSM){
            String properties = request.getParameter(TEXTFIELD_HARDCATOKENPROPERTIES);
            if(catokenpath == null)
              throw new Exception("Error in CATokenData");  
            catoken = new HardCATokenInfo();                       
            catoken.setProperties(properties);
         }

          
         String description = request.getParameter(TEXTFIELD_DESCRIPTION);        
         if(description == null){
        	 description = "";
         }
         
         int validity = 0;
         if(request.getParameter(TEXTFIELD_VALIDITY) != null)
           validity = Integer.parseInt(request.getParameter(TEXTFIELD_VALIDITY));
            

         if(caid != 0  && catype !=0 ){
           if(catype == CAInfo.CATYPE_X509){
              // Edit X509 CA data              
              
              int crlperiod = 0;
              int crlIssueInterval = 0;
              int crlOverlapTime = 0;
              if(request.getParameter(TEXTFIELD_CRLPERIOD) != null){
                crlperiod = Integer.parseInt(request.getParameter(TEXTFIELD_CRLPERIOD));
                crlIssueInterval = Integer.parseInt(request.getParameter(TEXTFIELD_CRLISSUEINTERVAL));
                crlOverlapTime = Integer.parseInt(request.getParameter(TEXTFIELD_CRLOVERLAPTIME));
              }
              
              boolean useauthoritykeyidentifier = false;
              boolean authoritykeyidentifiercritical = false;
              String value = request.getParameter(CHECKBOX_AUTHORITYKEYIDENTIFIER);
              if(value != null){
                 useauthoritykeyidentifier = value.equals(CHECKBOX_VALUE);                 
                 value = request.getParameter(CHECKBOX_AUTHORITYKEYIDENTIFIERCRITICAL); 
                 if(value != null){
                   authoritykeyidentifiercritical = value.equals(CHECKBOX_VALUE);
                 } 
                 else
                   authoritykeyidentifiercritical = false;
              }


              boolean usecrlnumber = false;
              boolean crlnumbercritical = false;

              value = request.getParameter(CHECKBOX_USECRLNUMBER);
              if(value != null){
                 usecrlnumber = value.equals(CHECKBOX_VALUE);                 
                 value = request.getParameter(CHECKBOX_CRLNUMBERCRITICAL); 
                 if(value != null){
                   crlnumbercritical = value.equals(CHECKBOX_VALUE);
                 } 
                 else
                   crlnumbercritical = false;
              }              
              
             String defaultcrldistpoint = request.getParameter(TEXTFIELD_DEFAULTCRLDISTPOINT);
             String defaultcrlissuer = request.getParameter(TEXTFIELD_DEFAULTCRLISSUER);
             String defaultocsplocator  = request.getParameter(TEXTFIELD_DEFAULTOCSPLOCATOR);
              
             boolean finishuser = false;
             value = request.getParameter(CHECKBOX_FINISHUSER);
             if(value != null)
               finishuser = value.equals(CHECKBOX_VALUE);         

             boolean  useutf8policytext = false;
             value = request.getParameter(CHECKBOX_USEUTF8POLICYTEXT);
             if(value != null) {
            	 useutf8policytext = value.equals(CHECKBOX_VALUE);         
             }
             
             boolean useprintablestringsubjectdn = false;
             value = request.getParameter(CHECKBOX_USEPRINTABLESTRINGSUBJECTDN);
             if(value != null) {
            	 useprintablestringsubjectdn = value.equals(CHECKBOX_VALUE);                             
             }

             String[] values = request.getParameterValues(SELECT_AVAILABLECRLPUBLISHERS);
             ArrayList crlpublishers = new ArrayList(); 
             if(values != null){
                 for(int i=0; i < values.length; i++){
                    crlpublishers.add(new Integer(values[i]));
                 }
              }
             
             values = request.getParameterValues(SELECT_APPROVALSETTINGS);
             ArrayList approvalsettings = new ArrayList(); 
             if(values != null){
               for(int i=0; i < values.length; i++){
            	   approvalsettings.add(new Integer(values[i]));
               }
             }
             
             value = request.getParameter(SELECT_NUMOFREQUIREDAPPROVALS);
             int numofreqapprovals = 1;
             if(value != null){
            	 numofreqapprovals = Integer.parseInt(value);
             }
              
              // Create extended CA Service updatedata.
              int active = ExtendedCAServiceInfo.STATUS_INACTIVE;
              value = request.getParameter(CHECKBOX_ACTIVATEOCSPSERVICE);
              if(value != null && value.equals(CHECKBOX_VALUE))
                active = ExtendedCAServiceInfo.STATUS_ACTIVE; 
              
              int xkmsactive = ExtendedCAServiceInfo.STATUS_INACTIVE;
              value = request.getParameter(CHECKBOX_ACTIVATEXKMSSERVICE);
              if(value != null && value.equals(CHECKBOX_VALUE))
            	  xkmsactive = ExtendedCAServiceInfo.STATUS_ACTIVE; 

              int cmsactive = ExtendedCAServiceInfo.STATUS_INACTIVE;
              value = request.getParameter(CHECKBOX_ACTIVATECMSSERVICE);
              if(value != null && value.equals(CHECKBOX_VALUE))
            	  cmsactive = ExtendedCAServiceInfo.STATUS_ACTIVE; 

              boolean renew = false;
              if(active == ExtendedCAServiceInfo.STATUS_ACTIVE && 
                 request.getParameter(BUTTON_REVOKERENEWOCSPCERTIFICATE) != null){
                 cadatahandler.revokeOCSPCertificate(caid);
                 renew=true;
                 ocsprenewed = true;             
                 includefile="choosecapage.jspf"; 
               }
              
              boolean xkmsrenew = false;
              if(xkmsactive == ExtendedCAServiceInfo.STATUS_ACTIVE && 
                 request.getParameter(BUTTON_REVOKERENEWXKMSCERTIFICATE) != null){
                 cadatahandler.revokeXKMSCertificate(caid);
                 xkmsrenew=true;
                 xkmsrenewed = true;             
                 includefile="choosecapage.jspf"; 
               }
              
              boolean cmsrenew = false;
              if(cmsactive == ExtendedCAServiceInfo.STATUS_ACTIVE && 
                 request.getParameter(BUTTON_REVOKERENEWCMSCERTIFICATE) != null){
                 cadatahandler.revokeCmsCertificate(caid);
                 cmsrenew=true;
                 cmsrenewed = true;             
                 includefile="choosecapage.jspf"; 
               }

	      ArrayList extendedcaservices = new ArrayList();
              extendedcaservices.add(
		             new OCSPCAServiceInfo(active, renew));    
              extendedcaservices.add(
 		             new XKMSCAServiceInfo(xkmsactive, xkmsrenew)); 
              extendedcaservices.add(
  		             new CmsCAServiceInfo(cmsactive, cmsrenew)); 

             if(crlperiod != 0){
               X509CAInfo x509cainfo = new X509CAInfo(caid, validity,
                                                      catoken, description, 
                                                      crlperiod, crlIssueInterval, crlOverlapTime, crlpublishers, 
                                                      useauthoritykeyidentifier, 
                                                      authoritykeyidentifiercritical,
                                                      usecrlnumber, 
                                                      crlnumbercritical, 
                                                      defaultcrldistpoint,
                                                      defaultcrlissuer,
                                                      defaultocsplocator,
                                                      finishuser,extendedcaservices,
                                                      useutf8policytext,
                                                      approvalsettings,
                                                      numofreqapprovals,
                                                      useprintablestringsubjectdn);
                 
               cadatahandler.editCA((CAInfo) x509cainfo);
                 

               
               if(request.getParameter(BUTTON_SAVE) != null){
                  // Do nothing More

                  includefile="choosecapage.jspf"; 
               }
               if(request.getParameter(BUTTON_RECEIVEREQUEST) != null){                  
                  filemode = RECIEVERESPONSEMODE;
                  includefile="recievefile.jspf"; 
               }
               if(request.getParameter(BUTTON_RENEWCA) != null){
                 int signedby = cadatahandler.getCAInfo(caid).getCAInfo().getSignedBy();
                 if(signedby != CAInfo.SIGNEDBYEXTERNALCA){
                   boolean reGenerateKeys = false;
                   if(request.getParameter(CHECKBOX_RENEWKEYS) != null && catokentype == CATokenInfo.CATOKENTYPE_P12){
                	   reGenerateKeys = request.getParameter(CHECKBOX_RENEWKEYS).equals(CHECKBOX_VALUE);                	   
                   }
                   cadatahandler.renewCA(caid, null, reGenerateKeys);
                   carenewed = true;
                 }else{                   
                   includefile="renewexternal.jspf"; 
                 }  
               }
                

             }  
             if(request.getParameter(BUTTON_REVOKECA) != null){
                 int revokereason = Integer.parseInt(request.getParameter(SELECT_REVOKEREASONS));
                 cadatahandler.revokeCA(caid, revokereason);                   
                 includefile="choosecapage.jspf"; 
             }
             if(request.getParameter(BUTTON_PUBLISHCA) != null){
                 cadatahandler.publishCA(caid);
                 capublished = true;             
                 includefile="choosecapage.jspf"; 
             }
           } 
         } 
       } 
       if(request.getParameter(BUTTON_CANCEL) != null){
         // Don't save changes.
         includefile="choosecapage.jspf"; 
       }               

         
      }
      if( action.equals(ACTION_MAKEREQUEST)){         
       if(!buttoncancel){
         try{
           Collection certchain = CertTools.getCertsFromPEM(file);           
           try{
             CAInfo cainfo = cabean.getRequestInfo();              
             cadatahandler.createCA(cainfo);                           
             PKCS10CertificationRequest certreq = null;
             try{ 
               certreq=cadatahandler.makeRequest(caid, certchain, true);
               cabean.savePKCS10RequestData(certreq);     
               filemode = CERTREQGENMODE;
               includefile = "displayresult.jspf";
             }catch(CATokenOfflineException e){  
        	  includefile="choosecapage.jspf"; 
        	  cadatahandler.removeCA(caid); 
              throw e;
             }catch(EjbcaException e){ 
        	  includefile="choosecapage.jspf"; 
        	  cadatahandler.removeCA(caid); 
              errormessage = e.getMessage(); 
             } catch(Exception e){   
        	  includefile="choosecapage.jspf";
        	  cadatahandler.removeCA(caid); 
              errorrecievingfile = true; 
             } 
           }catch(CAExistsException caee){
              caexists = true; 
           } 
         }catch(CATokenOfflineException e){  
          throw e;
      }catch(EjbcaException e){ 
          errormessage = e.getMessage(); 
      } catch(Exception e){   
          errorrecievingfile = true; 
      } 
       }else{
         cabean.saveRequestInfo((CAInfo) null); 
       }
      }

      if( action.equals(ACTION_RECEIVERESPONSE)){        
        if(!buttoncancel){
          try{                                                                                     
            if (caid != 0) {                             
              cadatahandler.receiveResponse(caid, file);   
              caactivated = true;
            }           
          }catch(CATokenOfflineException e){  
              throw e;
          }catch(EjbcaException e){ 
              errormessage = e.getMessage(); 
          } catch(Exception e){   
              errorrecievingfile = true; 
          } 
        }
      }
      if( action.equals(ACTION_PROCESSREQUEST)){       
       if(!buttoncancel){
         try{           
           BufferedReader bufRdr = new BufferedReader(new InputStreamReader(file));
           while (bufRdr.ready()) {
            ByteArrayOutputStream ostr = new ByteArrayOutputStream();
            PrintStream opstr = new PrintStream(ostr);
            String temp;
            while ((temp = bufRdr.readLine()) != null){            
              opstr.print(temp + "\n");                
            }  
            opstr.close();                
                                         
            PKCS10RequestMessage certreq = org.ejbca.ui.web.RequestHelper.genPKCS10RequestMessageFromPEM(ostr.toByteArray());
            
             if (certreq != null) {               
               cabean.savePKCS10RequestData(certreq.getCertificationRequest());                                
               processedsubjectdn = certreq.getCertificationRequest().getCertificationRequestInfo().getSubject().toString();
               processrequest = true;
               includefile="editcapage.jspf";
             }
           }
         }catch(Exception e){                      
           errorrecievingfile = true; 
         } 
       }else{
         cabean.savePKCS10RequestData((PKCS10CertificationRequest) null);  
       }
      }
      if( action.equals(ACTION_PROCESSREQUEST2)){        
        if(request.getParameter(BUTTON_CANCEL) == null){
         // Create and process CA                          
         caname = request.getParameter(HIDDEN_CANAME);
          
         catype  = Integer.parseInt(request.getParameter(HIDDEN_CATYPE));
         String subjectdn = request.getParameter(TEXTFIELD_SUBJECTDN);
         try{
             X509Name dummy = CertTools.stringToBcX509Name(subjectdn);
         }catch(Exception e){
           illegaldnoraltname = true;
         }
         
         int certprofileid = 0;
         if(request.getParameter(SELECT_CERTIFICATEPROFILE) != null)
           certprofileid = Integer.parseInt(request.getParameter(SELECT_CERTIFICATEPROFILE));
         int signedby = 0;
         if(request.getParameter(SELECT_SIGNEDBY) != null)
            signedby = Integer.parseInt(request.getParameter(SELECT_SIGNEDBY));
         String description = request.getParameter(TEXTFIELD_DESCRIPTION);        
         if(description == null)
           description = "";
         
         int validity = 0;
         if(request.getParameter(TEXTFIELD_VALIDITY) != null)
           validity = Integer.parseInt(request.getParameter(TEXTFIELD_VALIDITY));         

         if(catype != 0 && subjectdn != null && caname != null && 
            certprofileid != 0 && signedby != 0 && validity !=0 ){
           if(catype == CAInfo.CATYPE_X509){
              // Create a X509 CA
              String subjectaltname = request.getParameter(TEXTFIELD_SUBJECTALTNAME);
              if(subjectaltname == null)
                subjectaltname = ""; 
              else{
                if(!subjectaltname.trim().equals("")){
                   DNFieldExtractor subtest = 
                     new DNFieldExtractor(subjectaltname,DNFieldExtractor.TYPE_SUBJECTALTNAME);                   
                   if(subtest.isIllegal() || subtest.existsOther()){
                     illegaldnoraltname = true;
                   }
                }
              }

              String policyid = request.getParameter(TEXTFIELD_POLICYID);
              if(policyid == null || policyid.trim().equals(""))
                 policyid = null; 

              int crlperiod = 0;
              int crlIssueInterval = 0;
              int crlOverlapTime = 10;

              boolean useauthoritykeyidentifier = false;
              boolean authoritykeyidentifiercritical = false;              

              boolean usecrlnumber = false;
              boolean crlnumbercritical = false;
                                                                      
              boolean finishuser = false;
              boolean useutf8policytext = false;
              boolean useprintablestringsubjectdn = false;
              ArrayList crlpublishers = new ArrayList(); 
              ArrayList approvalsettings = new ArrayList(); 
              int numofreqapprovals = 1;
                            
             if(!illegaldnoraltname){
               if(request.getParameter(BUTTON_PROCESSREQUEST) != null){
                 X509CAInfo x509cainfo = new X509CAInfo(subjectdn, caname, 0, new Date(), subjectaltname,
                                                        certprofileid, validity, 
                                                        null, catype, signedby,
                                                        null, null, description, -1, null,
                                                        policyid, crlperiod, crlIssueInterval, crlOverlapTime, crlpublishers, 
                                                        useauthoritykeyidentifier, 
                                                        authoritykeyidentifiercritical,
                                                        usecrlnumber, 
                                                        crlnumbercritical, 
                                                        "","","",
                                                        finishuser, 
                                                        new ArrayList(),
                                                        useutf8policytext,
                                                        approvalsettings,
                                                        numofreqapprovals, 
                                                        useprintablestringsubjectdn);
                 try{
                   PKCS10CertificationRequest req = cabean.getPKCS10RequestData(); 
                   java.security.cert.Certificate result = cadatahandler.processRequest(x509cainfo, new PKCS10RequestMessage(req));
                   cabean.saveProcessedCertificate(result);
                   filemode = CERTGENMODE;   
                   includefile="displayresult.jspf";
                 }catch(CAExistsException caee){
                    caexists = true;
                 }                  
               }
             }
           }
         }
        } 
      }

      if( action.equals(ACTION_RENEWCA_MAKEREQUEST)){
        if(!buttoncancel){
          try{
           Collection certchain = CertTools.getCertsFromPEM(file);                       
           PKCS10CertificationRequest certreq = cadatahandler.makeRequest(caid, certchain, false);
           cabean.savePKCS10RequestData(certreq);   
               
           filemode = CERTREQGENMODE;
           includefile = "displayresult.jspf";
          }catch(CATokenOfflineException e){  
        	  includefile="choosecapage.jspf"; 
              throw e;
          }catch(EjbcaException e){ 
        	  includefile="choosecapage.jspf"; 
              errormessage = e.getMessage(); 
          } catch(Exception e){   
        	  includefile="choosecapage.jspf"; 
              errorrecievingfile = true; 
          }  
        }else{
          cabean.saveRequestInfo((CAInfo) null); 
        }      
      }
      if( action.equals(ACTION_RENEWCA_RECIEVERESPONSE)){
        if(!buttoncancel){
          try{                                                                                     
            if (caid != 0) {                             
              cadatahandler.receiveResponse(caid, file);   
              carenewed = true;
            }           
          }catch(CATokenOfflineException e){                       
              throw e;
          }catch(EjbcaException e){                       
              errormessage = e.getMessage(); 
          } catch(Exception e){                       
              errorrecievingfile = true; 
          }  
        }        
      }
      if( action.equals(ACTION_CHOOSE_CATYPE)){
        // Currently not need        
      }
      if( action.equals(ACTION_CHOOSE_CATOKENTYPE)){
        
        catokenpath = request.getParameter(SELECT_CATOKEN);   
        caname = request.getParameter(HIDDEN_CANAME);   
        if(catokenpath.equals(SoftCAToken.class.getName())){
          catokentype = CATokenInfo.CATOKENTYPE_P12;
        }else{
          catokentype = CATokenInfo.CATOKENTYPE_HSM;
        }
        editca = false;
        includefile="editcapage.jspf";              
      }
      if( action.equals(ACTION_IMPORTCA) ) {
		if( !buttoncancel ) {
	        try {
	        	String caName			= importcaname;
	            String kspwd			= importpassword;
	            InputStream p12file		= file;
	            String alias			= importsigalias;
	            String encryptionAlias	= importencalias;

				java.security.KeyStore ks = java.security.KeyStore.getInstance("PKCS12","BC");
				ks.load(file, kspwd.toCharArray());
				if ( alias.equals("") ) {
					Enumeration aliases = ks.aliases();
		            if ( aliases == null || !aliases.hasMoreElements() ) {
						throw new Exception("This file does not contain any aliases.");
		            } 
		            alias = (String)aliases.nextElement();
		            if ( aliases.hasMoreElements() ) {
			            while (aliases.hasMoreElements()) {
							alias += " " + (String)aliases.nextElement();
						}
						throw new Exception("You have to specify any of the following aliases: " + alias);
					}
		        }
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
	            ks.store(baos, kspwd.toCharArray());
	    		byte[] keystorebytes = baos.toByteArray();
	            if ( encryptionAlias.equals("") ) {
	            	encryptionAlias = null;
	            }
				cadatahandler.importCAFromKeyStore(caName, keystorebytes, kspwd, kspwd, alias, encryptionAlias);
			} catch (Exception e) {
			%> <div style="color: #FF0000;"> <%
				out.println( e.getMessage() );
			%> </div> <%
		        includefile="importca.jspf";              
			}
		}
      } // ACTION_IMPORTCA
    }
  }catch(CATokenOfflineException ctoe){
    catokenoffline = true;
    includefile="choosecapage.jspf";
  }   


 // Include page
  if( includefile.equals("editcapage.jspf")){ 
%>
   <%@ include file="editcapage.jspf" %>
<%}
  if( includefile.equals("choosecapage.jspf")){ %>
   <%@ include file="choosecapage.jspf" %> 
<%}  
  if( includefile.equals("recievefile.jspf")){ %>
   <%@ include file="recievefile.jspf" %> 
<%} 
  if( includefile.equals("displayresult.jspf")){ %>
   <%@ include file="displayresult.jspf" %> 
<%}
  if( includefile.equals("renewexternal.jspf")){ %>
   <%@ include file="renewexternal.jspf" %> 
<%}
  if( includefile.equals("importca.jspf")){ %>
   <%@ include file="importca.jspf" %> 
<%}


   // Include Footer 
   String footurl =   globalconfiguration.getFootBanner(); %>
   
  <jsp:include page="<%= footurl %>" />

</body>
</html>
