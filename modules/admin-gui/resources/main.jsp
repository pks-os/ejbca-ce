<%@ page pageEncoding="ISO-8859-1"%>
<% response.setContentType("text/html; charset="+org.ejbca.config.WebConfiguration.getWebContentEncoding()); %>
<%@page errorPage="errorpage.jsp" import="org.ejbca.config.GlobalConfiguration,org.ejbca.ui.web.RequestHelper,java.net.InetAddress,java.net.UnknownHostException" %>
<html>
<jsp:useBean id="ejbcawebbean" scope="session" class="org.ejbca.ui.web.admin.configuration.EjbcaWebBean" />
<jsp:useBean id="cabean" scope="session" class="org.ejbca.ui.web.admin.cainterface.CAInterfaceBean" />
<jsp:setProperty name="ejbcawebbean" property="*" /> 
<%   // Initialize environment
  GlobalConfiguration globalconfiguration = ejbcawebbean.initialize(request,"/administrator"); 
%>
<head>
  <title><%= globalconfiguration.getEjbcaTitle() %></title>
  <base href="<%= ejbcawebbean.getBaseUrl() %>" />
  <link rel="stylesheet" type="text/css" href="<%= ejbcawebbean.getCssFile() %>" />
  <meta http-equiv="Content-Type" content="text/html; charset=<%= org.ejbca.config.WebConfiguration.getWebContentEncoding() %>" />
</head>

<body>

<div align="right" style="text-weight: bold;">
	<%= ejbcawebbean.getText("VERSION") + " " + GlobalConfiguration.EJBCA_VERSION %>
<%	if ( ejbcawebbean.isUsingExportableCryptography() ) { %>
	<div style="color: #FF0000; font-size: 0.7em;"><%= ejbcawebbean.getText("EXPORTABLE") %></div>
<%	} %>
	<noscript>
	<div style="color: #FF0000; font-size: 0.7em;"><%= ejbcawebbean.getText("JAVASCRIPTDISABLED") %></div>
	</noscript>
</div> 

<h3 id="welcome"><%= ejbcawebbean.getText("WELCOME") + " " + ejbcawebbean.getUsersCommonName() + " " + ejbcawebbean.getText("TOEJBCA")%></h3> 

<div id="information">
	<div><%= ejbcawebbean.getText("NODEHOSTNAME") + " : "%><code><%= ejbcawebbean.getHostName()%></code></div> 
	<div><%= ejbcawebbean.getText("SERVERTIME") + " : "%><code><%= ejbcawebbean.getServerTime()%></code></div>
</div>

<div id="projecthome" class="app">
   <table width="50%" align="top">
   <tr>
   
   <td>
<%@ include file="statuspages/cacrlstatuses.jspf" %>
    </td>

   <td>
<%@ include file="statuspages/publisherqueuestatuses.jspf" %>
    </td>
    
    </tr>
    </table>
</div>

<% // Include Footer 
   String footurl =   globalconfiguration.getFootBanner(); %>
   
  <jsp:include page="<%= footurl %>" />
</body>
</html>
