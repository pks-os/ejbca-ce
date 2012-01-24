<%
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
 
 // Original version by Philip Vendil.
 
%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ page pageEncoding="ISO-8859-1"%>
<% response.setContentType("text/html; charset="+org.ejbca.config.WebConfiguration.getWebContentEncoding()); %>
<%@page errorPage="/errorpage.jsp" import="org.ejbca.ui.web.admin.configuration.EjbcaWebBean,org.ejbca.config.GlobalConfiguration"%>

<jsp:useBean id="ejbcawebbean" scope="session" class="org.ejbca.ui.web.admin.configuration.EjbcaWebBean" />

<%
	GlobalConfiguration globalconfiguration = ejbcawebbean.initialize(request, "/system_functionality/edit_administrator_privileges"); 
%>

<html>
<f:view>
<head>
  <title><h:outputText value="#{web.ejbcaWebBean.globalConfiguration.ejbcaTitle}" /></title>
  <base href="<%= ejbcawebbean.getBaseUrl() %>" />
  <link rel="stylesheet" type="text/css" href="<%= ejbcawebbean.getCssFile() %>" />
  <script language="javascript" src="<%= globalconfiguration.getAdminWebPath() %>ejbcajslib.js"></script>
</head>

<body>

<div align="center">

	<h2><h:outputText value="#{web.text.ADMINSINROLE} #{rolesManagedBean.currentRole}"
  			rendered="#{not empty rolesManagedBean.currentRole}"/></h2>

	<h:outputText value="#{web.text.AUTHORIZATIONDENIED}" rendered="#{empty rolesManagedBean.currentRole && !rolesManagedBean.authorizedToRole}"/>
	<h:panelGroup rendered="#{not empty rolesManagedBean.currentRole && rolesManagedBean.authorizedToRole}">
 
	<div align="right">
	<h:panelGrid columns="1" style="text-align: right;">
		<h:outputLink value="#{web.ejbcaWebBean.globalConfiguration.authorizationPath}/administratorprivileges.jsf"
			title="#{web.text.BACKTOROLES}">
			<h:outputText value="#{web.text.BACKTOROLES}"/>
		</h:outputLink>
		<h:outputLink value="#{web.ejbcaWebBean.globalConfiguration.authorizationPath}/editbasicaccessrules.jsf?currentRole=#{rolesManagedBean.currentRole}"
			title="#{web.text.EDITACCESSRULES}" rendered="#{not empty rolesManagedBean.currentRole && not rolesManagedBean.basicRuleSet.forceAdvanced}">
			<h:outputText value="#{web.text.EDITACCESSRULES}"/>
		</h:outputLink>
		<h:outputLink value="#{web.ejbcaWebBean.globalConfiguration.authorizationPath}/editadvancedaccessrules.jsf?currentRole=#{rolesManagedBean.currentRole}"
			title="#{web.text.EDITACCESSRULES}" rendered="#{not empty rolesManagedBean.currentRole && rolesManagedBean.basicRuleSet.forceAdvanced}">
			<h:outputText value="#{web.text.EDITACCESSRULES}"/>
		</h:outputLink>
	</h:panelGrid>
	</div>
  
	<div align="center">
	<h:messages layout="table" errorClass="alert"/>

	<h:form id="adminListForm" rendered="#{not empty rolesManagedBean.currentRole}">
	<h:inputHidden id="currentRole" value="#{rolesManagedBean.currentRole}" />
	<h:dataTable value="#{rolesManagedBean.admins}" var="admin" style="width: 100%;"
		headerClass="listHeader" rowClasses="listRow1,listRow2" columnClasses="caColumn,withColumn,typeColumn,valueColumn,commandColumn">
		<h:column>
			<f:facet name="header">
				<h:panelGroup>
					<h:outputText value="#{web.text.CA}" /><br />
					<h:selectOneMenu id="caId" value="#{rolesManagedBean.matchCaId}">
						<f:selectItems value="#{rolesManagedBean.availableCaIds}" />
					</h:selectOneMenu>
					<br /><h:outputText value="&nbsp;" escape="false"/>
				</h:panelGroup>
			</f:facet>
			<h:outputText value="#{rolesManagedBean.issuingCA}"/>
		</h:column>
		<h:column>
			<f:facet name="header">
				<h:panelGroup>
					<h:outputText value="#{web.text.MATCHWITH}" /><br />
					<h:selectOneMenu id="matchWith" value="#{rolesManagedBean.matchWith}">
						<f:selectItems value="#{rolesManagedBean.matchWithTexts}" />
					</h:selectOneMenu> 
					<br /><h:outputText value="&nbsp;" escape="false"/>
				</h:panelGroup>
			</f:facet>
			<h:outputText value="#{rolesManagedBean.adminsMatchWith}"/>
		</h:column>
		<h:column>
			<f:facet name="header">
				<h:panelGroup>
					<h:outputText value="#{web.text.MATCHTYPE}" /><br />
					<h:selectOneMenu id="matchType" value="#{rolesManagedBean.matchType}">
						<f:selectItems value="#{rolesManagedBean.matchTypeTexts}" />
					</h:selectOneMenu> 
					<br /><h:outputText value="&nbsp;" escape="false"/>
				</h:panelGroup>
			</f:facet>
			<h:outputText value="#{rolesManagedBean.adminsMatchType}"/>
		</h:column>
		<h:column>
			<f:facet name="header">
				<h:panelGroup>
					<h:outputText value="#{web.text.MATCHVALUE}" /><br />
					<h:inputText id="matchValue" value="#{rolesManagedBean.matchValue}">
						<f:validator validatorId="legalCharsValidator" />
						<f:validator validatorId="hexSerialNumberValidator" />
					</h:inputText>
					<br /><h:outputText value="&nbsp;" escape="false"/>
				</h:panelGroup>
			</f:facet>
			<h:outputText value="#{admin.matchValue}" rendered="#{rolesManagedBean.accessMatchValuesAsMap.WITH_SERIALNUMBER ne admin.matchWith}"/>
			<h:outputLink
				value="#{web.ejbcaWebBean.baseUrl}#{web.ejbcaWebBean.globalConfiguration.raPath}/listendentities.jsp?action=listusers&buttonisrevoked=value&textfieldserialnumber=#{admin.matchValue}"
				rendered="#{rolesManagedBean.accessMatchValuesAsMap.WITH_SERIALNUMBER eq admin.matchWith}">
				<h:outputText value="#{admin.matchValue}"/>
			</h:outputLink>
		</h:column>
		<h:column>
			<f:facet name="header">
				<h:panelGroup>
					<br />
					<h:commandButton action="#{rolesManagedBean.addAdmin}" value="#{web.text.ADD}"
						styleClass="commandButton"/>
					<br /><h:outputText value="&nbsp;" escape="false"/>
				</h:panelGroup>
			</f:facet>
			<h:commandLink action="#{rolesManagedBean.deleteAdmin}" title="#{web.text.DELETE}"
				styleClass="commandLink" onclick="return confirm('#{web.text.AREYOUSURE}');" >
				<h:outputText value="#{web.text.DELETE}"/>
			</h:commandLink>
		</h:column>
	</h:dataTable>
	</h:form >
	<h:outputText value="#{web.text.NOADMINSDEFINED}" rendered="#{empty rolesManagedBean.admins}"/>
	</div>
	</h:panelGroup>
</div>
 
<%	// Include Footer 
	String footurl = globalconfiguration.getFootBanner(); %>
	<jsp:include page="<%= footurl %>" />
 
</body>
</f:view>
</html>
