<%@page contentType="text/html"%>

<%@page isErrorPage="true" import="se.anatom.ejbca.webdist.webconfiguration.EjbcaWebBean,se.anatom.ejbca.ra.GlobalConfiguration, se.anatom.ejbca.ra.authorization.AuthorizationDeniedException,
                                   se.anatom.ejbca.ra.authorization.AuthenticationFailedException"%>

<jsp:useBean id="ejbcawebbean" scope="session" class="se.anatom.ejbca.webdist.webconfiguration.EjbcaWebBean" />
<jsp:setProperty name="ejbcawebbean" property="*" /> 

<%   // Initialize environment
   GlobalConfiguration globalconfiguration = ejbcawebbean.initialize_errorpage(request);

%>
<html>
<head>
  <title><%= globalconfiguration.getEjbcaTitle() %></title>
  <base href="<%= ejbcawebbean.getBaseUrl() %>">
  <link rel=STYLESHEET href="<%= ejbcawebbean.getCssFile() %>">
  <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
</head>
<body>
<br>
<br>
<% if( exception instanceof AuthorizationDeniedException){
       // Print Authorization Denied Exception.
     out.write("<H2>" + ejbcawebbean.getText("AUTHORIZATIONDENIED") + "</H2>");
     out.write("<H4>" + ejbcawebbean.getText("CAUSE") + " : " + exception.getMessage() + "</H4>");
   }
   else
   if( exception instanceof AuthenticationFailedException){
       // Print Authorization Denied Exception.
     out.write("<H2>" + ejbcawebbean.getText("AUTHORIZATIONDENIED") + "</H2>");
     out.write("<H4>" + ejbcawebbean.getText("CAUSE") + " : " + exception.getMessage() + "</H4>");
   }else{
       // Other exception occured, print exception and stack trace.   
     out.write("<H2>" + ejbcawebbean.getText("EXCEPTIONOCCURED") + "</H2>");
     out.write("<H4>" + exception.toString() + " : " + exception.getMessage() + "</H4>");
     exception.printStackTrace() ;
   }
%>


</body>
</html>
