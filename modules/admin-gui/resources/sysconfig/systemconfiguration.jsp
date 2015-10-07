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

 // Version: $Id$
%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://myfaces.apache.org/tomahawk" prefix="t" %>
<%@ page pageEncoding="UTF-8"%>
<% response.setContentType("text/html; charset="+org.ejbca.config.WebConfiguration.getWebContentEncoding()); %>
<%@ page errorPage="/errorpage.jsp" import="
org.ejbca.ui.web.admin.configuration.EjbcaWebBean,
org.ejbca.config.GlobalConfiguration,
org.ejbca.core.model.authorization.AccessRulesConstants,
org.cesecore.authorization.control.StandardRules,
org.cesecore.authorization.control.AccessControlSession
"%>
<jsp:useBean id="ejbcawebbean" scope="session" class="org.ejbca.ui.web.admin.configuration.EjbcaWebBean" />
<% 
	AccessControlSession accessControlSession = ejbcawebbean.getEjb().getAccessControlSession();
	GlobalConfiguration globalconfiguration = null;
	if(accessControlSession.isAuthorized(ejbcawebbean.getAdminObject(), StandardRules.REGULAR_EDITSYSTEMCONFIGURATION.resource())) {
		globalconfiguration = ejbcawebbean.initialize(request, AccessRulesConstants.ROLE_ADMINISTRATOR, StandardRules.REGULAR_EDITSYSTEMCONFIGURATION.resource());
	} else if(accessControlSession.isAuthorized(ejbcawebbean.getAdminObject(), StandardRules.REGULAR_EDITAVAILABLEEKU.resource())) {
		globalconfiguration = ejbcawebbean.initialize(request, AccessRulesConstants.ROLE_ADMINISTRATOR, StandardRules.REGULAR_EDITAVAILABLEEKU.resource());
	} else if(accessControlSession.isAuthorized(ejbcawebbean.getAdminObject(), StandardRules.REGULAR_EDITAVAILABLECUSTOMCERTEXTENSION.resource())) {
		globalconfiguration = ejbcawebbean.initialize(request, AccessRulesConstants.ROLE_ADMINISTRATOR, StandardRules.REGULAR_EDITAVAILABLECUSTOMCERTEXTENSION.resource());
	}
%>
<html>
<f:view>
<head>
  <title><h:outputText value="#{web.ejbcaWebBean.globalConfiguration.ejbcaTitle}" /></title>
  <base href="<%= ejbcawebbean.getBaseUrl() %>" />
  <link rel="stylesheet" type="text/css" href="<c:out value='<%=ejbcawebbean.getCssFile() %>' />" />
  <script src="<%= globalconfiguration.getAdminWebPath() %>ejbcajslib.js"></script>
  <style type="text/css">
  	input[type='checkbox'].checkBoxOverlay {
  		-moz-user-focus: ignore;
  	}
  	input[type='submit'].checkBoxOverlay {
  		vertical-align: text-bottom;
  		${web.legacyInternetExplorer ? '' : 'position:relative; z-index: 1; left: -20px;'}
  		${web.legacyInternetExplorer ? 'color: #000;' : 'color: transparent; background-color: transparent; border: 0px;'}
  		width: 20px;
  		height: 20px;
  		font-size: 8px;
  		padding: 0px;
  		margin: 0px;
  		
  	}
  	label.checkBoxOverlay {
  		${web.legacyInternetExplorer ? '' : 'position:relative; z-index: 0; left: -20px;'}
  	}
  	label.subItem {
  		padding-left: 10px;
  	}
  </style>
</head>
<body>
	<h1>
		<h:outputText value="#{web.text.SYSTEMCONFIGURATION}"/>
	</h1>
	<div class="message"><h:messages layout="table" errorClass="alert" infoClass="infoMessage"/></div>
	
	<div class="tabLinks">
		<c:forEach items="#{systemConfigMBean.availableTabs}" var="tab">
		<span>
			<h:outputLink value="adminweb/sysconfig/systemconfiguration.jsf?tab=#{tab}"
				styleClass="tabLink#{tab eq systemConfigMBean.selectedTab}">
				<h:outputText value="#{tab}"/>
			</h:outputLink>
		</span>
		</c:forEach>
	</div>

	<p>
		<h:panelGroup rendered="#{systemConfigMBean.selectedTab eq 'CTLogs'}">
			<h:outputText value="#{web.text.CTLOGCONFIGURATION_HELP}"/>
			<%= ejbcawebbean.getHelpReference("/adminguide.html#Certificate%20Transparency%20(Enterprise%20only)") %>
		</h:panelGroup>
	</p>


	<h:form id="systemconfiguration" rendered="#{systemConfigMBean.selectedTab eq 'Basic Configurations'}">
		<h:panelGrid columns="2" styleClass="edit-top" cellspacing="3" cellpadding="3" border="0" width="100%" rowClasses="Row0" columnClasses="editColumnSystem1,editColumn2">
			<h:panelGroup>
				&nbsp;
			</h:panelGroup>
			<h:panelGroup>
				&nbsp;
			</h:panelGroup>
			
			<h:panelGroup>
				<h:outputLabel for="title" value="#{web.text.EJBCATITLE}" styleClass="titles"/>
				<br/>
				<h:outputText value="#{web.text.EJBCATITLE_HELP}" styleClass="help"/>
			</h:panelGroup>
			<h:inputText id="title" value="#{systemConfigMBean.currentConfig.title}" size="45" title="#{web.text.FORMAT_STRING}"/>
		
			<h:panelGroup>
				<h:outputLabel for="headbanner" value="#{web.text.HEADBANNER}" styleClass="titles"/>
				<br/>
				<h:outputText value="#{web.text.HEADBANNER_HELP}" styleClass="help"/>
			</h:panelGroup>
			<h:inputText id="headbanner" value="#{systemConfigMBean.currentConfig.headBanner}" size="45" title="#{web.text.FORMAT_FILENAME}"/>
		
			<h:panelGroup>
				<h:outputLabel for="footbanner" value="#{web.text.FOOTBANNER}" styleClass="titles"/>
				<br/>
				<h:outputText value="#{web.text.FOOTBANNER_HELP}" styleClass="help"/>
			</h:panelGroup>
			<h:inputText id="footbanner" value="#{systemConfigMBean.currentConfig.footBanner}" size="45" title="#{web.text.FORMAT_FILENAME}"/>
		
			<h:panelGroup>
				<h:outputLabel for="enableeeplimit" value="#{web.text.ENABLEENDENTITYPROFILELIM}" styleClass="titles"/>
				<br/>
				<h:outputText value="#{web.text.ENABLEENDENTITYPROFILELIM_HELP}" styleClass="help"/>
			</h:panelGroup>
			<h:panelGroup>
				<h:selectBooleanCheckbox id="enableeeplimit" value="#{systemConfigMBean.currentConfig.enableEndEntityProfileLimitations}"/>
				<h:outputLabel for="enableeeplimit" value="#{web.text.ACTIVATE}" />
			</h:panelGroup>
		
			<h:panelGroup>
				<h:outputLabel for="enablekeyrecovery" value="#{web.text.ENABLEKEYRECOVERY}" styleClass="titles"/>
				<%= ejbcawebbean.getHelpReference("/adminguide.html#Key%20Recovery") %>
			</h:panelGroup>
			<h:panelGroup>
				<h:selectBooleanCheckbox id="enablekeyrecovery" value="#{systemConfigMBean.currentConfig.enableKeyRecovery}"/>
				<h:outputLabel for="enablekeyrecovery" value="#{web.text.ACTIVATE}" />
			</h:panelGroup>
		
			<h:panelGroup>
				<h:outputLabel for="issuehwtokens" value="#{web.text.ISSUEHARDWARETOKENS}" styleClass="titles"/>
				<br/>
				<h:outputText value="#{web.text.ISSUEHARDWARETOKENS_HELP}" styleClass="help"/>
			</h:panelGroup>
			<h:panelGroup>
				<h:selectBooleanCheckbox id="issuehwtokens" value="#{systemConfigMBean.currentConfig.issueHardwareToken}"/>
				<h:outputLabel for="issuehwtokens" value="#{web.text.ACTIVATE}" />
			</h:panelGroup>	

			<h:panelGroup>
				<h:outputLabel for="htdEncryptCa" value="#{web.text.HARDTOKENENCRYPTCA}" styleClass="titles"/>
				<br/>
				<h:outputText value="#{web.text.HARDTOKENENCRYPTCA_HELP}" styleClass="help"/>
			</h:panelGroup>
			<h:panelGroup id="htdEncryptCa">
				<h:selectOneMenu value="#{systemConfigMBean.currentConfig.hardTokenDataEncryptCA}">
					<f:selectItems value="#{systemConfigMBean.availableCAsAndNoEncryptionOption}"/>
				</h:selectOneMenu>
			</h:panelGroup>
			<!-- Ordering for certificate chains in public web -->
			<h:panelGroup>
				<h:outputLabel for="certChainOrder" value="#{web.text.CERTIFICATECHAINORDER}" styleClass="titles"/>
				<br/>
				<h:outputText value="#{web.text.CERTIFICATECHAINROOTFIRST_HELP}" styleClass="help"/>
			</h:panelGroup>
			<h:panelGroup>
				<h:selectBooleanCheckbox id="certChainOrder" value="#{systemConfigMBean.currentConfig.publicWebCertChainOrderRootFirst}"/>
				<h:outputLabel for="certChainOrder" value="#{web.text.CERTIFICATECHAINROOTFIRST}" />
			</h:panelGroup>
		</h:panelGrid>
		
		
		<%-- Approval Notifications --%>

		<h:panelGrid columns="2" styleClass="edit-top" cellspacing="3" cellpadding="3" border="0" width="100%" rowClasses="Row1" columnClasses="editColumnSystem1,editColumn2">
			<h:outputLabel for="header_approvalNotifications" value="#{web.text.APPROVALNOTIFICATIONS}" style="font-weight:bold; font-size:1.2em;"/>
			<h:panelGroup id="header_approvalNotifications"/>
			
			<h:panelGroup>
				<h:outputLabel for="useApprovalNotifications" value="#{web.text.USEAPPROVALNOTIFICATIONS}" styleClass="titles"/>
				<br/>
				<h:outputText value="#{web.text.USEAPPROVALNOTIFICATIONS_HELP}" styleClass="help"/>
			</h:panelGroup>	
			<h:panelGroup id="useApprovalNotifications">
				<h:selectBooleanCheckbox styleClass="checkBoxOverlay" value="#{systemConfigMBean.currentConfig.useApprovalNotifications}" />
				<h:commandButton id="toggleUseApprovalNotifications" styleClass="checkBoxOverlay" action="#{systemConfigMBean.toggleUseApprovalNotification}"
					value="#{systemConfigMBean.currentConfig.useApprovalNotifications?web.text.BOOL_TRUE:web.text.BOOL_FALSE}"/>
				<h:outputLabel for="toggleUseApprovalNotifications" value="#{web.text.USE}" styleClass="checkBoxOverlay"/>	
			</h:panelGroup>
		
			<h:panelGroup>
				<h:outputLabel for="emailToApprovalAdmin" value="#{web.text.EMAILADDRESSTOAPPROVING}" styleClass="titles"/>
				<br/>
				<h:outputText value="#{web.text.EMAILADDRESSTOAPPROVING_HELP}" styleClass="help"/>
			</h:panelGroup>	
			<h:inputText id="emailToApprovalAdmin" disabled="#{!systemConfigMBean.currentConfig.useApprovalNotifications}" value="#{systemConfigMBean.currentConfig.approvalAdminEmail}" size="45" title="#{web.text.FORMAT_EMAILADDRESS}"/>
		
			<h:panelGroup>
				<h:outputLabel for="approvalNoteFromAddress" value="#{web.text.APPROVALNOTIFICATIONFROM}" styleClass="titles"/>
				<br/>
				<h:outputText value="#{web.text.APPROVALNOTIFICATIONFROM_HELP}" styleClass="help"/>
			</h:panelGroup>
			<h:inputText id="approvalNoteFromAddress" disabled="#{!systemConfigMBean.currentConfig.useApprovalNotifications}" value="#{systemConfigMBean.currentConfig.approvalNoteFromAddress}" size="45" title="#{web.text.FORMAT_EMAILADDRESS}"/>
		</h:panelGrid>
	
	
		<%-- Auto Enrollment --%>

		<h:panelGrid columns="2" styleClass="edit-top" cellspacing="3" cellpadding="3" border="0" width="100%" rowClasses="Row0" columnClasses="editColumnSystem1,editColumn2">
			<h:panelGroup>
				<h:outputLabel for="header_autoenrollment" value="#{web.text.AUTOENROLLMENT}" style="font-weight: bold; font-size:1.2em;"/>
				<%= ejbcawebbean.getExternalHelpReference("http://www.ejbca.org/guides.html#Setting%20up%20Autoenrollment%20for%20Windows%20clients%20with%20EJBCA") %>
			</h:panelGroup>
			<h:panelGroup id="header_autoenrollment"/>
			
			<h:panelGroup>
				<h:outputLabel for="useAutoEnrollment" value="#{web.text.AUTOENROLLUSE}" styleClass="titles"/>
				<br/>
				<h:outputText value="#{web.text.AUTOENROLLUSE_HELP}" styleClass="help"/>
			</h:panelGroup>
			<h:panelGroup id="useAutoEnrollment">
				<h:selectBooleanCheckbox styleClass="checkBoxOverlay" value="#{systemConfigMBean.currentConfig.useAutoEnrollment}" />
				<h:commandButton id="toggleUseAutoEnrollment" styleClass="checkBoxOverlay" action="#{systemConfigMBean.toggleUseAutoEnrollment}"
					value="#{systemConfigMBean.currentConfig.useAutoEnrollment?web.text.BOOL_TRUE:web.text.BOOL_FALSE}"/>
				<h:outputLabel for="toggleUseAutoEnrollment" value="#{web.text.USE}" styleClass="checkBoxOverlay"/>
			</h:panelGroup>
		
			<h:panelGroup>
				<h:outputLabel for="autoEnrollCA" value="#{web.text.AUTOENROLLCA}" styleClass="titles"/>
				<br/>
				<h:outputText value="#{web.text.AUTOENROLLCA_HELP}" styleClass="help"/>
			</h:panelGroup>
			<h:panelGroup id="autoEnrollCA">
				<h:selectOneMenu disabled="#{!systemConfigMBean.currentConfig.useAutoEnrollment}" value="#{systemConfigMBean.currentConfig.autoEnrollmentCA}">
					<f:selectItems value="#{systemConfigMBean.availableCAs}"/>
				</h:selectOneMenu>
			</h:panelGroup>
		
			<h:panelGroup>
				<h:outputLabel for="useSSLconnection" value="#{web.text.AUTOENROLLSSLCONNECTION}" styleClass="titles"/>
				<br/>
				<h:outputText value="#{web.text.AUTOENROLLSSLCONNECTION_HELP}" styleClass="help"/>
			</h:panelGroup>
			<h:panelGroup>
				<h:selectBooleanCheckbox id="useSSLconnection" disabled="#{!systemConfigMBean.currentConfig.useAutoEnrollment}" value="#{systemConfigMBean.currentConfig.autoEnrollUseSSLConnection}"/>
				<h:outputLabel for="useSSLconnection" value="#{web.text.USE}" />
			</h:panelGroup>
		
			<h:panelGroup>
				<h:outputLabel for="dcserver" value="#{web.text.AUTOENROLLADSERVER}" styleClass="titles"/>
				<br/>
				<h:outputText value="#{web.text.AUTOENROLLADSERVER_HELP}" styleClass="help"/>
			</h:panelGroup>
			<h:inputText id="dcserver" disabled="#{!systemConfigMBean.currentConfig.useAutoEnrollment}" value="#{systemConfigMBean.currentConfig.autoEnrollAdServer}" size="45" title="#{web.text.FORMAT_DOMAINNAME}"/>
		
			<h:panelGroup>
				<h:outputLabel for="dcport" value="#{web.text.AUTOENROLLADPORT}" styleClass="titles"/>
				<br/>
				<h:outputText value="#{web.text.AUTOENROLLADPORT_HELP}" styleClass="help"/>
			</h:panelGroup>
			<h:inputText id="dcport" disabled="#{!systemConfigMBean.currentConfig.useAutoEnrollment}" value="#{systemConfigMBean.currentConfig.autoEnrollAdServerPort}" size="5" title="#{web.text.FORMAT_INTEGER}"/>
		
			<h:panelGroup>
				<h:outputLabel for="dcdn" value="#{web.text.AUTOENROLLCONNECTIONDN}" styleClass="titles"/>
				<br/>
				<h:outputText value="#{web.text.AUTOENROLLCONNECTIONDN_HELP}" styleClass="help"/>
			</h:panelGroup>
			<h:inputText id="dcdn" disabled="#{!systemConfigMBean.currentConfig.useAutoEnrollment}" value="#{systemConfigMBean.currentConfig.autoEnrollConnectionDN}" size="45" title="#{web.text.FORMAT_DN}"/>
		
			<h:panelGroup>
				<h:outputLabel for="dcpwd" value="#{web.text.AUTOENROLLCONNECTIONPWD}" styleClass="titles"/>
				<br/>
				<h:outputText value="#{web.text.AUTOENROLLCONNECTIONPWD_HELP}" styleClass="help"/>
			</h:panelGroup>
			<h:inputText id="dcpwd" disabled="#{!systemConfigMBean.currentConfig.useAutoEnrollment}" value="#{systemConfigMBean.currentConfig.autoEnrollConnectionPassword}" size="20" title="#{web.text.FORMAT_STRING}"/>
		
			<h:panelGroup>
				<h:outputLabel for="dcBaseUserDN" value="#{web.text.AUTOENROLLBASEDNUSER}" styleClass="titles"/>
				<br/>
				<h:outputText value="#{web.text.AUTOENROLLBASEDNUSER_HELP}" styleClass="help"/>			
			</h:panelGroup>
			<h:inputText id="dcBaseUserDN" disabled="#{!systemConfigMBean.currentConfig.useAutoEnrollment}" value="#{systemConfigMBean.currentConfig.autoEnrollUserBaseDN}" size="45" title="#{web.text.FORMAT_DN}"/>
		</h:panelGrid>
	
	
		<%-- Nodes in Cluster --%>

		<h:panelGrid columns="2" styleClass="edit-top" cellspacing="3" cellpadding="3" border="0" width="100%" rowClasses="Row1" columnClasses="editColumnSystem1,editColumn2">
			<h:outputLabel for="header_nodes" value="#{web.text.NODESINCLUSTER}" style="font-weight: bold; font-size:1.2em;"/>
			<h:panelGroup id="header_nodes"/>

			<h:outputLabel for="nodes" value="#{web.text.NODESINCLUSTER_HELP}"/>
			<h:panelGrid columns="1">
				<h:dataTable id="nodes" value="#{systemConfigMBean.nodesInCluster}" var="nodeEntry">
					<h:column>
						<h:outputText value="#{nodeEntry}"/>
						<f:facet name="footer">
							<h:inputText value="#{systemConfigMBean.currentNode}" size="20" maxlength="4096" title="#{web.text.FORMAT_DOMAINNAME}"/>
						</f:facet>
					</h:column>
					<h:column>
						<h:commandButton value="#{web.text.REMOVE}" action="#{systemConfigMBean.removeNode}"/>
						<f:facet name="footer">
							<h:commandButton value="#{web.text.ADD}" action="#{systemConfigMBean.addNode}"/>
						</f:facet>
					</h:column>
				</h:dataTable>
			</h:panelGrid>
		</h:panelGrid>
	
	
		<%-- Application Caches --%>

		<h:panelGrid columns="2" styleClass="edit-top" cellspacing="3" cellpadding="3" border="0" width="100%" rowClasses="Row0" columnClasses="editColumnSystem1,editColumn2">
			<h:outputLabel for="header_applicationcaches" value="#{web.text.APPLICATIONCACHES}" style="font-weight: bold; font-size:1.2em;"/>
			<h:panelGroup id="header_applicationcaches"/>

			<h:panelGroup>
				<h:outputLabel for="clearAllCaches" value="#{web.text.CLEARALLCACHES}" styleClass="titles"/>
				<%= ejbcawebbean.getHelpReference("/adminguide.html#Clear%20All%20Caches") %>
				<br/>
				<h:outputText value="#{web.text.CLEARALLCACHES_HELP1}" styleClass="help"/>			
				<br/>
				<br/>
				<h:outputText value="#{web.text.CLEARALLCACHES_HELP2}" styleClass="help"/>			
			</h:panelGroup>
			<h:panelGroup id="clearAllCaches">
				<h:selectBooleanCheckbox id="excludetokens" value="#{systemConfigMBean.excludeActiveCryptoTokensFromClearCaches}"/>
				<h:outputLabel for="excludetokens" value="#{web.text.CLEARALLCACHES_EXCLUDE_CRYPTOTOKEN_CACHE}" />
				<br/>
				<br/>
				<h:commandButton value="#{web.text.CLEARALLCACHES}" action="#{systemConfigMBean.clearAllCaches}" />
			</h:panelGroup>
		</h:panelGrid>
	
	
		<%-- Command Line Interface --%>

		<h:panelGrid columns="2" styleClass="edit-top" cellspacing="3" cellpadding="3" border="0" width="100%" rowClasses="Row1" columnClasses="editColumnSystem1,editColumn2">
			<h:outputLabel for="header_commandline" value="#{web.text.COMMANDLINEINTERFACE}" style="font-weight: bold; font-size:1.2em;"/>
			<h:panelGroup id="header_commandline"/>

    		<h:panelGroup>
				<h:outputLabel for="enableCommandLine" value="#{web.text.ENABLECLIACCESS}" styleClass="titles"/>
				<%= ejbcawebbean.getHelpReference("/adminguide.html#Disabling%20the%20Command%20Line%20Interface") %>
				<br/>
				<h:outputText value="#{web.text.ENABLECLIACCESS_HELP}" styleClass="help"/>
			</h:panelGroup>
			<h:panelGroup>
				<h:selectBooleanCheckbox id="enableCommandLine" value="#{systemConfigMBean.currentConfig.enableCommandLine}"/>
				<h:outputLabel for="enableCommandLine" value="#{web.text.ACTIVATE}" />
			</h:panelGroup>
		
			<h:panelGroup>
				<h:outputLabel for="enableCommandLineDefUser" value="#{web.text.ENABLECLIDEFAULTUSER}" styleClass="titles"/>
				<%= ejbcawebbean.getHelpReference("/adminguide.html#Local%20CLI%20Authentication") %>
				<br/>
				<h:outputText value="#{web.text.ENABLECLIDEFAULTUSERHELPER}" styleClass="help"/>
			</h:panelGroup>
			<h:panelGroup>
				<h:selectBooleanCheckbox id="enableCommandLineDefUser" value="#{systemConfigMBean.currentConfig.enableCommandLineDefaultUser}"/>
				<h:outputLabel for="enableCommandLineDefUser" value="#{web.text.ACTIVATE}" />
			</h:panelGroup>	
		</h:panelGrid>

		<h:panelGrid columns="2" styleClass="edit-bottom" cellspacing="3" cellpadding="3" border="0" width="100%" rowClasses="Row0" columnClasses="editColumnSystem1,editColumn2">
			<h:panelGroup>
				&nbsp;
			</h:panelGroup>
			<h:panelGroup>
				<h:commandButton value="#{web.text.SAVE}" action="#{systemConfigMBean.saveCurrentConfig}" />
				<h:commandButton value="#{web.text.CANCEL}" action="#{systemConfigMBean.flushCache}" />
			</h:panelGroup>
		</h:panelGrid>
	</h:form>
	
	
	
	<%-- CTLogs --%>

	<h:form id="ctlogsform" enctype="multipart/form-data" rendered="#{systemConfigMBean.selectedTab eq 'Certificate Transparency Logs'}">
		<h:panelGroup>
			<h4><h:outputText value="#{web.text.CTLOGCONFIGURATION_EDIT_CTLOG_TITLE}"/>
			<%= ejbcawebbean.getHelpReference("/adminguide.html#Certificate%20Transparency%20(Enterprise%20only)") %></h4>
			</br>
		</h:panelGroup>
		
		
		<h:dataTable value="#{systemConfigMBean.ctLogs}" var="ctlog"
					styleClass="grid" style="border-collapse: collapse; right: auto; left: auto">
			<h:column>
   				<f:facet name="header"><h:outputText value="#{web.text.CTLOGCONFIGURATION_URL}"/></f:facet>
				<h:outputText value="#{systemConfigMBean.ctLogUrl}" title="#{web.text.CTLOGCONFIGURATION_URL} #{ctlog.url}"/>
				<f:facet name="footer">
					<h:inputText id="currentURL" value="#{systemConfigMBean.currentCTLogURL}" size="45" title="#{web.text.FORMAT_URI}"/>
				</f:facet>
			</h:column>
			<h:column>
   				<f:facet name="header"><h:outputText value="#{web.text.CTLOGCONFIGURATION_PUBLICKEY}"/></f:facet>
				<h:outputText value="#{systemConfigMBean.ctLogPublicKeyID}"/>
				<f:facet name="footer">
					<h:panelGroup>
 	 	 	 			<h:outputText value="#{web.text.CTLOGCONFIGURATION_PUBLICKEYFILE} " />
 	 	 	 			<t:inputFileUpload id="currentCTLogKeyFile" value="#{systemConfigMBean.currentCTLogPublicKeyFile}"
 	 	 	 					       title="#{web.text.CTLOGCONFIGURATION_PUBLICKEYFILE}" />
 	 	 	 		</h:panelGroup>
				</f:facet>
			</h:column>
			<h:column>
   				<f:facet name="header"><h:outputText value="#{web.text.CTLOGCONFIGURATION_TIMEOUT}"/></f:facet>
				<h:outputText value="#{systemConfigMBean.ctLogTimeout}"/>
				<f:facet name="footer">
					<h:inputText id="currentTimeout" required="false"
									value="#{systemConfigMBean.currentCTLogTimeout}"
									title="#{web.text.FORMAT_INTEGER}"
									size="10">
   					</h:inputText>
				</f:facet>
			</h:column>
			<h:column>
   				<f:facet name="header">
   					<h:outputText value="#{web.text.INTERNALKEYBINDING_ACTION}"/>
   				</f:facet>
				<h:commandButton action="#{systemConfigMBean.removeCTLog}"	value="#{web.text.REMOVE}" title="#{web.text.REMOVE}"/>
				<f:facet name="footer">
					<h:commandButton  value="#{web.text.ADD}" action="#{systemConfigMBean.addCTLog}" />
				</f:facet>
			</h:column>
		</h:dataTable>
	</h:form>


	<%-- Administrator Preferences --%>

	<h:form id="adminprefform" rendered="#{systemConfigMBean.selectedTab eq 'Administrator Preferences'}">
		<h:panelGrid columns="2" styleClass="edit-top" cellspacing="3" cellpadding="3" border="0" width="100%" rowClasses="Row1" columnClasses="editColumnSystem1,editColumn2">
			<h:outputLabel for="header_defaultadminpreferences" value="#{web.text.DEFAULTADMINPREFERENCES}" style="font-weight: bold; font-size:1.2em;"/>
			<h:panelGroup id="header_defaultadminpreferences"/>

			<h:panelGroup>
				<h:outputLabel for="preferedLanguage" value="#{web.text.PREFEREDLANGUAGE}" styleClass="titles"/>
				<br/>
				<h:outputText value="#{web.text.PREFEREDLANGUAGE_HELP}" styleClass="help"/>
			</h:panelGroup>
			<h:panelGroup id="preferedLanguage">
				<h:selectOneMenu value="#{systemConfigMBean.currentConfig.preferedLanguage}">
					<f:selectItems value="#{systemConfigMBean.availableLanguages}"/>
				</h:selectOneMenu>
			</h:panelGroup>
		
			<h:panelGroup>
				<h:outputLabel for="secondaryLanguage" value="#{web.text.SECONDARYLANGUAGE}" styleClass="titles"/>
				<br/>
				<h:outputText value="#{web.text.SECONDARYLANGUAGE_HELP}" styleClass="help"/>
			</h:panelGroup>
			<h:panelGroup id="secondaryLanguage">
				<h:selectOneMenu value="#{systemConfigMBean.currentConfig.secondaryLanguage}">
					<f:selectItems value="#{systemConfigMBean.availableLanguages}"/>
				</h:selectOneMenu>
			</h:panelGroup>
		
			<h:panelGroup>
				<h:outputLabel for="theme" value="#{web.text.THEME}" styleClass="titles"/>
				<br/>
				<h:outputText value="#{web.text.THEME_HELP}" styleClass="help"/>
			</h:panelGroup>
			<h:panelGroup id="theme">
				<h:selectOneMenu value="#{systemConfigMBean.currentConfig.theme}">
					<f:selectItems value="#{systemConfigMBean.availableThemes}"/>
				</h:selectOneMenu>
			</h:panelGroup>
		
			<h:panelGroup>
				<h:outputLabel for="entriesPerPage" value="#{web.text.NUMBEROFRECORDSPERPAGE}" styleClass="titles"/>
				<br/>
				<h:outputText value="#{web.text.NUMBEROFRECORDSPERPAGE_HELP}" styleClass="help"/>
			</h:panelGroup>
			<h:panelGroup id="entriesPerPage">
				<h:selectOneMenu value="#{systemConfigMBean.currentConfig.entriesPerPage}" styleClass="number">
					<f:selectItems value="#{systemConfigMBean.possibleEntriesPerPage}"/>
				</h:selectOneMenu>
			</h:panelGroup>
		</h:panelGrid>
		
		<h:panelGrid columns="2" styleClass="edit-bottom" cellspacing="3" cellpadding="3" border="0" width="100%" rowClasses="Row0" columnClasses="editColumnSystem1,editColumn2">
			<h:panelGroup>
				&nbsp;
			</h:panelGroup>
			<h:panelGroup>
				<h:commandButton value="#{web.text.SAVE}" action="#{systemConfigMBean.saveCurrentAdminPreferences}" />
				<h:commandButton value="#{web.text.CANCEL}" action="#{systemConfigMBean.flushCache}" />
			</h:panelGroup>
		</h:panelGrid>
	</h:form>
	
	
	<%-- Extended Key Usages --%>
	
	<h:form id="extkeyusageform" enctype="multipart/form-data" rendered="#{systemConfigMBean.selectedTab eq 'Extended Key Usages'}">
		<h:panelGroup>
			<h4><h:outputText value="#{web.text.EKU_EDIT_EKU_TITLE}"/>
			<%= ejbcawebbean.getHelpReference("/adminguide.html#Extended%20Key%20Usages") %></h4>
			</br>
		</h:panelGroup>
	
		<h:dataTable value="#{systemConfigMBean.availableExtendedKeyUsages}" var="eku"
					styleClass="grid" style="border-collapse: collapse; right: auto; left: auto">
			<h:column>
   				<f:facet name="header"><h:outputText value="#{web.text.OID}"/></f:facet>
				<h:outputText value="#{eku.oid}" title="#{eku.oid}"/>
				<f:facet name="footer">
					<h:inputText id="currentOid" value="#{systemConfigMBean.currentEKUOid}" size="25" title="#{web.text.FORMAT_OID}"/>
				</f:facet>
			</h:column>
			<h:column>
   				<f:facet name="header"><h:outputText value="#{web.text.LABEL}"/></f:facet>
				<h:outputText value="#{eku.name}"/>
				<f:facet name="footer">
					<h:inputText id="currentReadableName" value="#{systemConfigMBean.currentEKUReadableName}" size="35" title="#{web.text.FORMAT_STRING}"/>
				</f:facet>
			</h:column>
			<h:column>
   				<f:facet name="header">
   					<h:outputText value="#{web.text.ACTION}"/>
   				</f:facet>
				<h:commandButton action="#{systemConfigMBean.removeEKU}"	value="#{web.text.REMOVE}" title="#{web.text.REMOVE}"/>
				<f:facet name="footer">
					<h:commandButton  value="#{web.text.ADD}" action="#{systemConfigMBean.addEKU}" />
				</f:facet>
			</h:column>
		</h:dataTable>
	</h:form>
	
	
	<%-- Custom Certificate Extensions --%>
	
	<h:form id="customcertextensionsform" enctype="multipart/form-data" rendered="#{systemConfigMBean.selectedTab eq 'Custom Certificate Extensions'}">
		<h:panelGroup>
			<h4><h:outputText value="#{web.text.CUSTOMCERTEXTENSION_EDIT_CCE_TITLE}"/>
			<%= ejbcawebbean.getHelpReference("/adminguide.html#Custom%20Certificate%20Extensions") %></h4>
			</br>
		</h:panelGroup>
		
		<h:dataTable value="#{systemConfigMBean.availableCustomCertExtensions}" var="extension"
					styleClass="grid" style="border-collapse: collapse; right: auto; left: auto">
			<h:column>
				<f:facet name="header"><h:outputText value="#{web.text.IDENTIFIER}"/></f:facet>
				<h:outputText value="#{extension.id}" title="#{extension.id}"/>
				<f:facet name="footer">
					<h:outputText id="newCEID" value=""/>
				</f:facet>
			</h:column>
			<h:column>
   				<f:facet name="header"><h:outputText value="#{web.text.OID}"/></f:facet>
				<h:outputText value="#{extension.oid}" title="#{extension.oid}"/>
				<f:facet name="footer">
					<h:inputText id="newCEOID" value="#{systemConfigMBean.newOID}" size="25" title="#{web.text.FORMAT_OID}"/>
				</f:facet>
			</h:column>
			<h:column>
   				<f:facet name="header"><h:outputText value="#{web.text.LABEL}"/></f:facet>
				<h:outputText value="#{extension.displayName}"/>
				<f:facet name="footer">
					<h:inputText id="newCELabel" value="#{systemConfigMBean.newDisplayName}" size="35" title="#{web.text.FORMAT_STRING}"/>
				</f:facet>
			</h:column>
			<h:column>
				<f:facet name="header"><h:outputText value="#{web.text.ACTION}"/></f:facet>
				<h:panelGroup>
					<h:commandButton value="#{web.text.EDIT}" action="#{systemConfigMBean.actionEdit}"  
								rendered="#{systemConfigMBean.allowedToModify}"/>
					<h:commandButton value="#{web.text.REMOVE}" action="#{systemConfigMBean.removeCustomCertExtension}"
								rendered="#{systemConfigMBean.allowedToModify}" onclick="return confirm('#{web.text.CUSTOMCERTEXTENSION_CONF_DELETE}')"/>
				</h:panelGroup>
				<f:facet name="footer">
					<h:commandButton value="#{web.text.ADD}" action="#{systemConfigMBean.addCustomCertExtension}" />					 			   
				</f:facet>
			</h:column>
		</h:dataTable>
	</h:form>

	<%	// Include Footer 
	String footurl = globalconfiguration.getFootBanner(); %>
	<jsp:include page="<%= footurl %>" />
</body>
</f:view>
</html>