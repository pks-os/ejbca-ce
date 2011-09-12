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
<%@ page pageEncoding="UTF-8"%>
<% response.setContentType("text/html; charset="+org.ejbca.config.WebConfiguration.getWebContentEncoding()); %>
<%@page errorPage="/errorpage.jsp" import="org.ejbca.ui.web.admin.configuration.EjbcaWebBean,org.ejbca.config.GlobalConfiguration" %>

<jsp:useBean id="ejbcawebbean" scope="session" class="org.ejbca.ui.web.admin.configuration.EjbcaWebBean" />
<% GlobalConfiguration globalconfiguration = ejbcawebbean.initialize(request, "/log_functionality/view_log"); %>
<html>
<f:view>
<head>
  <title><h:outputText value="#{web.ejbcaWebBean.globalConfiguration.ejbcaTitle}" /></title>
  <base href="<%= ejbcawebbean.getBaseUrl() %>" />
  <link rel="stylesheet" type="text/css" href="<%= ejbcawebbean.getCssFile() %>" />
  <script language="javascript" src="<%= globalconfiguration.getAdminWebPath() %>ejbcajslib.js"></script>
</head>
<body>

<h1><h:outputText value="#{web.text.AUDITHEADER}" /></h1>

<%
	//TODO: Clean up style-mess. Fix translation keys.
%>

<div id="home" class="app">
	<p><h:messages layout="table" errorClass="alert"/></p>

	<h:outputText value="#{web.text.NO_SEARCHABLE_AUDIT}" rendered="#{auditor.device == null}"/>
	<h:form id="search" rendered="#{auditor.device != null}">
	<h:outputLabel for="device" value="Audit Log Device" rendered="#{auditor.oneLogDevice == false}"/>
	<h:selectOneMenu id="device" value="#{auditor.device}" rendered="#{auditor.oneLogDevice == false}"><f:selectItems value="#{auditor.devices}" /></h:selectOneMenu>
	<!-- 
	<h:outputLabel rendered="false" for="sortColumn" value="Order by"/>
	<h:selectOneMenu rendered="false" id="sortColumn" value="#{auditor.sortColumn}"><f:selectItems value="#{auditor.sortColumns}" /></h:selectOneMenu>
	<h:outputLabel rendered="false" for="sortOrder" value="Order"/>
	<h:selectOneMenu rendered="false" id="sortOrder" value="#{auditor.sortOrder}"><f:selectItems value="#{auditor.sortOrders}" /></h:selectOneMenu>
 	-->
	<p>
   	<h:dataTable value="#{auditor.conditions}" var="condition" captionStyle="text-align: left; background-color: #5B8CCD; color: #FFF;" headerClass="listHeader" styleClass="grid" rowClasses="a">
		<f:facet name="caption"><h:outputText value="Current conditions"/></f:facet>
		<h:column rendered="false"><f:facet name="header"><h:outputText value="operation"/></f:facet><h:outputText value="#{condition.operation}"></h:outputText></h:column>
		<h:column>
			<f:facet name="header"><h:outputText value="#{web.text.COLUMN}"/></f:facet>
			<h:outputText value="#{auditor.nameFromColumn[(condition.column)]}"></h:outputText>
			<f:facet name="footer">
			<h:panelGroup>
				<h:selectOneMenu rendered="#{auditor.conditionToAdd == null}" id="conditionColumn" value="#{auditor.conditionColumn}"><f:selectItems value="#{auditor.columns}"/></h:selectOneMenu>
				<h:commandButton rendered="#{auditor.conditionToAdd == null}" action="#{auditor.newCondition}" styleClass="commandLink" value="#{web.text.CONDITIONS_NEW}"/>
				<h:outputText rendered="#{auditor.conditionToAdd != null}" value="#{auditor.nameFromColumn[(auditor.conditionToAdd.column)]}"></h:outputText>
			</h:panelGroup>
			</f:facet>
		</h:column>
		<h:column>
			<f:facet name="header"><h:outputText value="#{web.text.CONDITION}"/></f:facet>
			<h:outputText value="#{web.text[(condition.condition)]}"></h:outputText>
			<f:facet name="footer">
			<h:panelGroup>
				<h:selectOneMenu rendered="#{auditor.conditionToAdd != null}" value="#{auditor.conditionToAdd.condition}"><f:selectItems value="#{auditor.definedConditions}"/></h:selectOneMenu>
			</h:panelGroup>
			</f:facet>
		</h:column>
		<h:column>
			<f:facet name="header"><h:outputText value="#{web.text.VALUE}"/></f:facet>
			<h:outputText value="#{web.text[(condition.value)]}"></h:outputText>
			<f:facet name="footer">
			<h:panelGroup rendered="#{auditor.conditionToAdd != null}">
				<h:inputText rendered="#{empty auditor.conditionToAdd.options}" value="#{auditor.conditionToAdd.value}"></h:inputText>
				<h:selectOneMenu rendered="#{not empty auditor.conditionToAdd.options}" value="#{auditor.conditionToAdd.value}"><f:selectItems value="#{auditor.conditionToAdd.options}"/></h:selectOneMenu>
				<h:commandButton action="#{auditor.addConditionAndReload}" styleClass="commandLink" image="#{web.ejbcaBaseURL}#{web.ejbcaWebBean.globalConfiguration.adminWebPath}images/verify-log-success.png"/>
				<!-- 
				<h:commandButton rendered="false" action="#{auditor.addCondition}" styleClass="commandLink" value="#{web.text.CONDITIONS_ADD}"/>
				 -->
				<h:commandButton action="#{auditor.cancelCondition}" styleClass="commandLink" image="#{web.ejbcaBaseURL}#{web.ejbcaWebBean.globalConfiguration.adminWebPath}images/verify-log-failed.png"/>
			</h:panelGroup>
			</f:facet>
		</h:column>
	</h:dataTable>
	</p>
	<h:panelGroup>
		<h:commandButton rendered="#{not empty auditor.conditions}" action="#{auditor.clearConditions}" styleClass="commandLink" value="#{web.text.CONDITIONS_CLEAR}"/>
		<h:commandButton rendered="#{not empty auditor.conditions}" action="#{auditor.clearConditionsAndReload}" styleClass="commandLink" value="#{web.text.CONDITIONS_CLEAR_RELOAD}"/>
	</h:panelGroup>
	</h:form>
	<h:form id="search2" rendered="#{auditor.device != null}">
	<p>
		<h:commandButton disabled="#{auditor.startIndex == 1}" action="#{auditor.first}" styleClass="commandLink" value="First"/>
		<h:commandButton disabled="#{auditor.startIndex == 1}" action="#{auditor.previous}" styleClass="commandLink" value="#{web.text.PREVIOUS}"/>
		<h:commandButton action="#{auditor.reload}" styleClass="commandLink" value="#{web.text.RELOAD}"/>
		<h:commandButton disabled="#{auditor.renderNext==false}" action="#{auditor.next}" styleClass="commandLink" value="#{web.text.NEXT}"/>
		<!-- 
		<h:outputLabel rendered="false" for="startIndex" value="Results start at index"/>
		<h:inputText rendered="false" id="startIndex" value="#{auditor.startIndex}" style="border: 0px; width: 6em; text-align:right;"><f:convertNumber type="number"/></h:inputText>
		 -->
		<h:outputLabel for="maxResults" value="#{web.text.ENTRIESPERPAGE}"/>
		<h:inputText id="maxResults" value="#{auditor.maxResults}" style="width: 4em; text-align:right;"><f:convertNumber type="number"/></h:inputText>
		<!-- 
		<h:outputText rendered="false" value="Displaying results #{auditor.startIndex} - #{auditor.startIndex + auditor.maxResults - 1}."/>
		<h:outputText rendered="false" value="Ordered by #{auditor.nameFromColumn[(auditor.sortColumn)]}, asc=#{auditor.sortOrder}."/>
		 -->
		<h:outputText value="Displaying results "/>
		<h:inputText id="startIndex2" value="#{auditor.startIndex}" style="width: 6em; text-align:right;"><f:convertNumber type="number"/></h:inputText>
		<h:outputText value=" to #{auditor.startIndex + auditor.resultSize - 1}."/>
	</p>
	<h:dataTable value="#{auditor.results}" var="auditLogEntry" captionStyle="text-align: left; background-color: #5B8CCD; color: #FFF;" headerClass="results" styleClass="grid" rowClasses="LogTextRow0,LogTextRow1" rendered="#{not empty auditor.results}">
		<f:facet name="caption">
			<h:panelGroup>
				<h:outputText value="Search results "/>
			</h:panelGroup>
		</f:facet>
		<h:column>
			<f:facet name="header">
				<h:panelGroup>
					<h:outputText value="#{auditor.nameFromColumn['timeStamp']}"/>
					<h:commandButton action="#{auditor.reorderAscByTime}" image="#{web.ejbcaBaseURL}#{web.ejbcaWebBean.globalConfiguration.adminWebPath}images/downarrow.gif"/>
					<h:commandButton action="#{auditor.reorderDescByTime}" image="#{web.ejbcaBaseURL}#{web.ejbcaWebBean.globalConfiguration.adminWebPath}images/uparrow.gif"/>
				</h:panelGroup>
			</f:facet>
			<h:outputText style="white-space: nowrap;" value="#{auditLogEntry.timeStamp}"><f:convertDateTime pattern="yyyy-MM-dd HH:mm:ssZZ" /></h:outputText>
		</h:column>
		<h:column>
			<f:facet name="header">
				<h:panelGroup>
					<h:outputText value="#{auditor.nameFromColumn['eventType']}"/>
					<h:commandButton action="#{auditor.reorderAscByEvent}" image="#{web.ejbcaBaseURL}#{web.ejbcaWebBean.globalConfiguration.adminWebPath}images/downarrow.gif"/>
					<h:commandButton action="#{auditor.reorderDescByEvent}" image="#{web.ejbcaBaseURL}#{web.ejbcaWebBean.globalConfiguration.adminWebPath}images/uparrow.gif"/>
				</h:panelGroup>
			</f:facet>
			<h:outputText style="white-space: nowrap;" value="#{web.text[(auditLogEntry.eventTypeValue)]}"/>
		</h:column>
		<h:column>
			<f:facet name="header">
				<h:panelGroup>
					<h:outputText value="#{auditor.nameFromColumn['eventStatus']}"/>
					<h:commandButton action="#{auditor.reorderAscByStatus}" image="#{web.ejbcaBaseURL}#{web.ejbcaWebBean.globalConfiguration.adminWebPath}images/downarrow.gif"/>
					<h:commandButton action="#{auditor.reorderDescByStatus}" image="#{web.ejbcaBaseURL}#{web.ejbcaWebBean.globalConfiguration.adminWebPath}images/uparrow.gif"/>
				</h:panelGroup>
			</f:facet>
			<h:outputText value="#{web.text[(auditLogEntry.eventStatusValue)]}"/>
		</h:column>
		<h:column>
			<f:facet name="header">
				<h:panelGroup>
					<h:outputText value="#{auditor.nameFromColumn['authToken']}"/>
					<h:commandButton action="#{auditor.reorderAscByAuthToken}" image="#{web.ejbcaBaseURL}#{web.ejbcaWebBean.globalConfiguration.adminWebPath}images/downarrow.gif"/>
					<h:commandButton action="#{auditor.reorderDescByAuthToken}" image="#{web.ejbcaBaseURL}#{web.ejbcaWebBean.globalConfiguration.adminWebPath}images/uparrow.gif"/>
				</h:panelGroup>
			</f:facet>
			<h:outputText value="#{auditLogEntry.authToken}"/>
		</h:column>
		<!-- 
		<h:column rendered="false">
			<f:facet name="header">
				<h:panelGroup>
					<h:outputText value="#{auditor.nameFromColumn['service']}"/>
					<h:commandButton action="#{auditor.reorderAscByService}" styleClass="commandLink" value="↓"/>
					<h:commandButton action="#{auditor.reorderDescByService}" styleClass="commandLink" value="↑"/>
				</h:panelGroup>
			</f:facet>
			<h:outputText value="#{auditLogEntry.serviceTypeValue}"/>
		</h:column>
		 -->
		<h:column>
			<f:facet name="header">
				<h:panelGroup>
					<h:outputText value="#{auditor.nameFromColumn['module']}"/>
					<h:commandButton action="#{auditor.reorderAscByModule}" image="#{web.ejbcaBaseURL}#{web.ejbcaWebBean.globalConfiguration.adminWebPath}images/downarrow.gif"/>
					<h:commandButton action="#{auditor.reorderDescByModule}" image="#{web.ejbcaBaseURL}#{web.ejbcaWebBean.globalConfiguration.adminWebPath}images/uparrow.gif"/>
				</h:panelGroup>
			</f:facet>
			<h:outputText style="white-space: nowrap;" value="#{web.text[(auditLogEntry.moduleTypeValue)]}"/>
		</h:column>
		<h:column>
			<f:facet name="header">
				<h:panelGroup>
					<h:outputText value="#{auditor.nameFromColumn['customId']}"/>
					<h:commandButton action="#{auditor.reorderAscByCustomId}" image="#{web.ejbcaBaseURL}#{web.ejbcaWebBean.globalConfiguration.adminWebPath}images/downarrow.gif"/>
					<h:commandButton action="#{auditor.reorderDescByCustomId}" image="#{web.ejbcaBaseURL}#{web.ejbcaWebBean.globalConfiguration.adminWebPath}images/uparrow.gif"/>
				</h:panelGroup>
			</f:facet>
		    <h:outputLink value="#{web.ejbcaBaseURL}#{web.ejbcaWebBean.globalConfiguration.adminWebPath}viewcertificate.jsf?caid=#{auditLogEntry.customId}" rendered="#{auditor.caIdToName[(auditLogEntry.customId)] != null}"><h:outputText value="#{auditor.caIdToName[(auditLogEntry.customId)]}"/></h:outputLink>
		    <h:outputText value="#{auditLogEntry.customId}" rendered="#{auditor.caIdToName[(auditLogEntry.customId)] == null}"/>
		</h:column>
		<h:column>
			<f:facet name="header">
				<h:panelGroup>
					<h:outputText value="#{auditor.nameFromColumn['searchDetail1']}"/>
					<h:commandButton action="#{auditor.reorderAscBySearchDetail1}" image="#{web.ejbcaBaseURL}#{web.ejbcaWebBean.globalConfiguration.adminWebPath}images/downarrow.gif"/>
					<h:commandButton action="#{auditor.reorderDescBySearchDetail1}" image="#{web.ejbcaBaseURL}#{web.ejbcaWebBean.globalConfiguration.adminWebPath}images/uparrow.gif"/>
				</h:panelGroup>
			</f:facet>
		    <h:outputLink value="#{web.ejbcaBaseURL}#{web.ejbcaWebBean.globalConfiguration.adminWebPath}viewcertificate.jsf?serno=#{auditLogEntry.searchDetail1}&caid=#{auditLogEntry.customId}"><h:outputText value="#{auditLogEntry.searchDetail1}"/></h:outputLink>
		</h:column>
		<h:column>
			<f:facet name="header">
				<h:panelGroup>
					<h:outputText value="#{auditor.nameFromColumn['searchDetail2']}"/>
					<h:commandButton action="#{auditor.reorderAscBySearchDetail2}" image="#{web.ejbcaBaseURL}#{web.ejbcaWebBean.globalConfiguration.adminWebPath}images/downarrow.gif"/>
					<h:commandButton action="#{auditor.reorderDescBySearchDetail2}" image="#{web.ejbcaBaseURL}#{web.ejbcaWebBean.globalConfiguration.adminWebPath}images/uparrow.gif"/>
				</h:panelGroup>
			</f:facet>
		    <h:outputLink value="#{web.ejbcaBaseURL}#{web.ejbcaWebBean.globalConfiguration.adminWebPath}viewcertificate.jsf?username=#{auditLogEntry.searchDetail2}"><h:outputText value="#{auditLogEntry.searchDetail2}"/></h:outputLink>
		</h:column>
		<h:column>
			<f:facet name="header">
				<h:panelGroup>
					<h:outputText value="#{auditor.nameFromColumn['nodeId']}"/>
					<h:commandButton action="#{auditor.reorderAscByNodeId}" image="#{web.ejbcaBaseURL}#{web.ejbcaWebBean.globalConfiguration.adminWebPath}images/downarrow.gif"/>
					<h:commandButton action="#{auditor.reorderDescByNodeId}" image="#{web.ejbcaBaseURL}#{web.ejbcaWebBean.globalConfiguration.adminWebPath}images/uparrow.gif"/>
				</h:panelGroup>
			</f:facet>
			<h:outputText value="#{auditLogEntry.nodeId}"/>
		</h:column>
			<!-- 
		<h:column rendered="false">
			<f:facet name="header">
				<h:panelGroup>
					<h:outputText value="#{auditor.nameFromColumn['sequenceNumber']}"/>
					<h:commandButton action="#{auditor.reorderAscBySequenceNumber}" styleClass="commandLink" value="↓"/>
					<h:commandButton action="#{auditor.reorderDescBySequenceNumber}" styleClass="commandLink" value="↑"/>
				</h:panelGroup>
			</f:facet>
			<h:outputText value="#{auditLogEntry.sequenceNumber}"/>
		</h:column>
		 -->
		<h:column>
			<f:facet name="header">
				<h:outputText value="#{auditor.nameFromColumn['additionalDetails']}"/>
			</f:facet>
		<h:outputText value="#{auditLogEntry.mapAdditionalDetails}"><f:converter converterId="mapToStringConverter"/></h:outputText></h:column>
	</h:dataTable>
	<p>
		<h:commandButton rendered="#{not empty auditor.results}" disabled="#{auditor.startIndex == 1}" action="#{auditor.first}" styleClass="commandLink" value="First"/>
		<h:commandButton rendered="#{not empty auditor.results}" disabled="#{auditor.startIndex == 1}" action="#{auditor.previous}" styleClass="commandLink" value="#{web.text.PREVIOUS}"/>
		<h:commandButton rendered="#{not empty auditor.results}" action="#{auditor.reload}" styleClass="commandLink" value="#{web.text.RELOAD}"/>
		<h:commandButton rendered="#{not empty auditor.results}" disabled="#{auditor.renderNext==false}" action="#{auditor.next}" styleClass="commandLink" value="#{web.text.NEXT}"/>
		<h:outputText rendered="#{not empty auditor.results}" value="Displaying results #{auditor.startIndex} to #{auditor.startIndex + auditor.resultSize - 1}."/>
	</p>
	</h:form >
</div>

<%	// Include Footer 
	String footurl = globalconfiguration.getFootBanner(); %>
	<jsp:include page="<%= footurl %>" />

</body>
</f:view>
</html>
