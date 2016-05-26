/*************************************************************************
 *                                                                       *
 *  EJBCA Community: The OpenSource Certificate Authority                *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.ejbca.ra;

import java.io.Serializable;
import java.util.List;

import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.ejbca.core.model.approval.AdminAlreadyApprovedRequestException;
import org.ejbca.core.model.approval.ApprovalException;
import org.ejbca.core.model.approval.ApprovalRequestExecutionException;
import org.ejbca.core.model.approval.ApprovalRequestExpiredException;
import org.ejbca.core.model.approval.SelfApprovalException;
import org.ejbca.core.model.era.RaApprovalEditRequest;
import org.ejbca.core.model.era.RaApprovalRequestInfo;
import org.ejbca.core.model.era.RaApprovalResponseRequest;
import org.ejbca.core.model.era.RaEditableRequestData;
import org.ejbca.core.model.era.RaApprovalResponseRequest.Action;
import org.ejbca.core.model.era.RaMasterApiProxyBeanLocal;
import org.ejbca.ra.ApprovalRequestGUIInfo.RequestDataRow;

/**
 * Backing bean for Manage Request page (for individual requests).
 *  
 * @see RaManageRequestsBean
 * @version $Id$
 */
@ManagedBean
@ViewScoped
public class RaManageRequestBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(RaManageRequestBean.class);
    
    @EJB
    private RaMasterApiProxyBeanLocal raMasterApiProxyBean;

    @ManagedProperty(value="#{raAuthenticationBean}")
    private RaAuthenticationBean raAuthenticationBean;
    public void setRaAuthenticationBean(final RaAuthenticationBean raAuthenticationBean) { this.raAuthenticationBean = raAuthenticationBean; }

    @ManagedProperty(value="#{raLocaleBean}")
    private RaLocaleBean raLocaleBean;
    public void setRaLocaleBean(final RaLocaleBean raLocaleBean) { this.raLocaleBean = raLocaleBean; }

    
    private ApprovalRequestGUIInfo requestInfo;
    private RaApprovalRequestInfo requestData;
    private boolean editing = false;
    
    private void initializeRequestInfo() {
        if (requestInfo == null) {
            final String idHttpParam = ((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest())
                    .getParameter("id");
            final int id = Integer.parseInt(idHttpParam);
            requestData = raMasterApiProxyBean.getApprovalRequest(raAuthenticationBean.getAuthenticationToken(), id);
            if (requestData == null) {
                throw new IllegalStateException("Request does not exist, or user is not allowed to see it at this point");
            }
            requestInfo = new ApprovalRequestGUIInfo(requestData, raLocaleBean);
        }
    }
    
    public ApprovalRequestGUIInfo getRequest() {
        initializeRequestInfo();
        return requestInfo;
    }
    
    public String getPageTitle() {
        return raLocaleBean.getMessage("view_request_page_title", getRequest().getDisplayName());
    }
    
    public boolean isViewDataVisible() { return !editing; }
    public boolean isEditDataVisible() { return editing; }
    public boolean isStatusVisible() { return !editing; }
    public boolean isApprovalVisible() { return !editing; }
    
    public boolean isHasNextStep() {
        initializeRequestInfo();
        return requestInfo != null && requestInfo.getNextStep() != null;
    }
    
    public List<ApprovalRequestGUIInfo.StepControl> getNextStepControls() {
        initializeRequestInfo();
        if (requestInfo != null && requestInfo.getNextStep() != null) {
            return getRequest().getNextStep().getControls();
        } else {
            return null;
        }
    }
    
    private RaApprovalResponseRequest buildApprovalResponseRequest(final Action action) {
        final List<ApprovalRequestGUIInfo.StepControl> controls = getNextStepControls();
        final int id = getRequest().request.getId();
        final int stepId = getRequest().getNextStep().getStepId();
        final RaApprovalResponseRequest approval = new RaApprovalResponseRequest(id, stepId, "", action); // TODO comment field. should it be here for partitioned approvals also?
        for (final ApprovalRequestGUIInfo.StepControl control : controls) {
            approval.addMetadata(control.getMetadataId(), control.getOptionValue(), control.getOptionNote());
        }
        return approval;
    }
    
    public void approve() throws AuthorizationDeniedException {
        final RaApprovalResponseRequest responseReq = buildApprovalResponseRequest(Action.APPROVE);
        try {
            if (raMasterApiProxyBean.addRequestResponse(raAuthenticationBean.getAuthenticationToken(), responseReq)) {
                raLocaleBean.addMessageInfo("view_request_page_success_approve");
            } else {
                raLocaleBean.addMessageError("generic_unexpected_no_backend");
            }
        } catch (ApprovalException e) {
            raLocaleBean.addMessageError("view_request_page_error_approval_generic");
            logException("approve", e);
        } catch (ApprovalRequestExpiredException e) {
            raLocaleBean.addMessageError("view_request_page_error_approval_expired");
            logException("approve", e);
        } catch (ApprovalRequestExecutionException e) {
            raLocaleBean.addMessageError("view_request_page_error_approval_execution");
            logException("approve", e);
        } catch (AdminAlreadyApprovedRequestException e) {
            raLocaleBean.addMessageError("view_request_page_error_already_approved");
            logException("approve", e);
        } catch (SelfApprovalException e) {
            raLocaleBean.addMessageError("view_request_page_error_self_approval");
            logException("approve", e);
        }
    }
    
    public void reject() throws AuthorizationDeniedException {
        final RaApprovalResponseRequest responseReq = buildApprovalResponseRequest(Action.REJECT);
        try {
            if (raMasterApiProxyBean.addRequestResponse(raAuthenticationBean.getAuthenticationToken(), responseReq)) {
                raLocaleBean.addMessageInfo("view_request_page_success_reject");
            } else {
                raLocaleBean.addMessageError("generic_unexpected_no_backend");
            } 
        } catch (ApprovalException e) {
            raLocaleBean.addMessageError("view_request_page_error_approval_generic_reject");
            logException("reject", e);
        } catch (ApprovalRequestExpiredException e) {
            raLocaleBean.addMessageError("view_request_page_error_approval_expired");
            logException("reject", e);
        } catch (ApprovalRequestExecutionException e) {
            raLocaleBean.addMessageError("view_request_page_error_approval_execution");
            logException("reject", e);
        } catch (AdminAlreadyApprovedRequestException e) {
            raLocaleBean.addMessageError("view_request_page_error_already_approved");
            logException("reject", e);
        } catch (SelfApprovalException e) {
            raLocaleBean.addMessageError("view_request_page_error_self_approval");
            logException("reject", e);
        }
    }
    
    public void editData() {
        editing = true;
    }
    
    public void saveData() {
        if (!editing) {
            throw new IllegalStateException();
        }
        
        final RaEditableRequestData editData = requestData.getEditableData();
        for (final RequestDataRow dataRow : requestInfo.getRequestData()) {
            switch (dataRow.getKey()) {
            case "SUBJECTDN":
                editData.setSubjectDN(getDN(dataRow));
                break;
            case "SUBJECTALTNAME":
                editData.setSubjectAltName(getDN(dataRow));
                break;
            case "SUBJECTDIRATTRIBUTES":
                editData.setSubjectDirAttrs(getDN(dataRow));
                break;
            case "EMAIL":
                String email = (String) dataRow.getEditValue();
                // TODO validation
                editData.setEmail(email);
                break;
            }
        }
        
        // TODO error handling
        final RaApprovalEditRequest editReq = new RaApprovalEditRequest(requestData.getId(), editData);
        raMasterApiProxyBean.editApprovalRequest(raAuthenticationBean.getAuthenticationToken(), editReq);
        
        editing = false;
    }
    
    public String getDN(final RequestDataRow dataRow) {
        // TODO validation
        return (String) dataRow.getEditValue();
    }
    
    /** Logs the message of an exception, which usually contains some message. For example: "You may not approve an action which you requested yourself" */
    private void logException(final String action, final Throwable t) {
        if (log.isDebugEnabled()) {
            log.debug("Got exception while trying to " + action + " an approval request: " + t.getMessage());
        }
    }
    
}
