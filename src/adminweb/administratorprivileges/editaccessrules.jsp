<% /* editacccessrules.jsp
    *
    * 
    * Created on  13 feb 2004, 20:49
    *
    * author  Philip Vendil */ %>

<% // Check actions submitted


    AccessRule accessrule = null;
    String accessrulesstring = null;

    String mode = MODE_BASIC;

    

    if(request.getParameter(MODE) == null || request.getParameter(MODE).equals(MODE_BASIC)){
       // Always start with basic mode

        includefile = "editbasicaccessrules.jsp";
    }else{      
      if(request.getParameter(MODE).equals(MODE_ADVANCED)){
        mode = MODE_ADVANCED;   
           
        if( request.getParameter(BUTTON_ADD_ACCESSRULES) != null ){
            // Add selected access rules.
           Enumeration parameters = request.getParameterNames();
           ArrayList indexes = new  ArrayList();
           int index;
           while(parameters.hasMoreElements()){
             String parameter = (String) parameters.nextElement();
             if(parameter.startsWith(CHECKBOX_ADDROW) && request.getParameter(parameter).equals(CHECKBOX_VALUE)) {
               index = java.lang.Integer.parseInt(parameter.substring(CHECKBOX_ADDROW.length())); //Without []
               indexes.add(new Integer(index));
             }
           }
       
           if(indexes.size() > 0){
             ArrayList accessrules = new ArrayList();
             Iterator iter = indexes.iterator();
             while(iter.hasNext()){
               index = ((java.lang.Integer) iter.next()).intValue();
               accessrules.add(new AccessRule(request.getParameter(HIDDEN_ADDRESOURCE+index), 
                           Integer.parseInt(request.getParameter(SELECT_ADDRULE+index)),
                           request.getParameter(CHECKBOX_RECURSIVEROW+index)!= null));
             }
             adh.addAccessRules(admingroup[ADMINGROUPNAME],caid,accessrules);
           }
        }
        if( request.getParameter(BUTTON_DELETE_ACCESSRULES) != null ){
          // Delete selected access rules
          Enumeration parameters = request.getParameterNames();
          ArrayList indexes = new  ArrayList();
          int index;
          while(parameters.hasMoreElements()) {
            String parameter = (String) parameters.nextElement();
            if(parameter.startsWith(CHECKBOX_DELETEROW) && request.getParameter(parameter).equals(CHECKBOX_VALUE)){
              index = java.lang.Integer.parseInt(parameter.substring(CHECKBOX_DELETEROW.length())); //Without []   
              indexes.add(new Integer(index)); 
             }
          }
       
          if(indexes.size() > 0){
            ArrayList accessrules = new ArrayList();
            Iterator iter = indexes.iterator();
            while(iter.hasNext()){
              index = ((java.lang.Integer) iter.next()).intValue();
              accessrules.add(request.getParameter(HIDDEN_DELETEROW+index));  
            }
            adh.removeAccessRules(admingroup[ADMINGROUPNAME],caid,accessrules);   
          }
        }

        includefile = "editadvancedaccessrules.jsp";
      } 
    }

    // Get Access Rules
     AdminGroup ag = adh.getAdminGroup(admingroup[ADMINGROUPNAME],caid);
     AccessRulesView admingroupaccessrules = new AccessRulesView(ag.getAccessRules());
     AccessRulesView nonusedavailableaccessrule = new AccessRulesView(ag.nonUsedAccessRules(adh.getAvailableAccessRules()));

     BasicAccessRuleSetEncoder basicruleset = new BasicAccessRuleSetEncoder(ag.getAccessRules(), adh.getAvailableAccessRules(), globalconfiguration.getIssueHardwareTokens(), globalconfiguration.getEnableKeyRecovery());
    
     if(basicruleset.getForceAdvanced())
       includefile = "editadvancedaccessrules.jsp";
%>

<div align="center">
  <p><H1><%= ejbcawebbean.getText("ACCESSRULES") %></H1></p>
  <p><H2><%= ejbcawebbean.getText("FORADMINGROUP") + " " + admingroup[ADMINGROUPNAME] + ", " + ejbcawebbean.getText("CA") + ": " + caidtonamemap.get(new Integer(caid)) %></H2></p>
  <form name="toadminentities" method="post" action="<%=THIS_FILENAME %>">
  <div align="right"><A href="<%=THIS_FILENAME %>"><u><%= ejbcawebbean.getText("BACKTOADMINGROUPS") %></u></A>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
    <input type="hidden" name='<%= HIDDEN_GROUPNAME %>' value='<%= admingroup[ADMINGROUPNAME] + ";" + caid %>'>
    <input type="hidden" name='<%= ACTION %>' value='<%=ACTION_EDIT_ADMINENTITIES%>'>
    <A href='javascript:document.toadminentities.submit();'><u><%= ejbcawebbean.getText("EDITADMINS") %></u></A>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
<!--    <A  onclick='displayHelpWindow("<%= ejbcawebbean.getHelpfileInfix("authorization_help.html") + "#accessrules" %>")'>
    <u><%= ejbcawebbean.getText("HELP") %></u> </A> -->
  </div>
  </form>
  <form name="mode" method="post" action="<%=THIS_FILENAME %>">
    <input type="hidden" name='<%= HIDDEN_GROUPNAME %>' value='<%= admingroup[ADMINGROUPNAME] + ";" + caid %>'>
    <input type="hidden" name='<%= ACTION %>' value='<%=ACTION_EDIT_ACCESSRULES %>'>
  <div align="center">
  <select name="<%=MODE%>" onchange='document.mode.submit()' >
    <% if(!basicruleset.getForceAdvanced()) { %>
      <option  value="<%= MODE_BASIC %>" <% if(mode.equals(MODE_BASIC)) out.write(" selected "); %>><%= ejbcawebbean.getText("BASICMODE") %></option>
    <% } %> 
      <option  value="<%= MODE_ADVANCED %>" <% if(mode.equals(MODE_ADVANCED)) out.write(" selected "); %>><%= ejbcawebbean.getText("ADVANCEDMODE") %></option>
  </select>
  </div>
  </form>
</div>
<%
  if( includefile.equals("editbasicaccessrules.jsp")){ %>
   <%@ include file="editbasicaccessrules.jsp" %> 
<%}
  if( includefile.equals("editadvancedaccessrules.jsp")){ %>
    <%@ include file="editadvancedaccessrules.jsp" %>
<%} %>