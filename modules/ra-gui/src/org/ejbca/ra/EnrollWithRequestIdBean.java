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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.apache.log4j.Logger;
import org.cesecore.ErrorCode;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.certificates.endentity.EndEntityConstants;
import org.cesecore.certificates.endentity.EndEntityInformation;
import org.cesecore.util.StringTools;
import org.ejbca.core.EjbcaException;
import org.ejbca.core.model.approval.ApprovalDataVO;
import org.ejbca.core.model.era.RaApprovalRequestInfo;
import org.ejbca.core.model.era.RaMasterApiProxyBeanLocal;

/**
 * Managed bean that backs up the enrollwithrequestid.xhtml page
 * 
 * @version $Id$
 */
@ManagedBean
@ViewScoped
public class EnrollWithRequestIdBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(EnrollWithRequestIdBean.class);

    @EJB
    private RaMasterApiProxyBeanLocal raMasterApiProxyBean;

    @ManagedProperty(value = "#{raAuthenticationBean}")
    private RaAuthenticationBean raAuthenticationBean;

    public void setRaAuthenticationBean(final RaAuthenticationBean raAuthenticationBean) {
        this.raAuthenticationBean = raAuthenticationBean;
    }

    @ManagedProperty(value = "#{raLocaleBean}")
    private RaLocaleBean raLocaleBean;

    public void setRaLocaleBean(final RaLocaleBean raLocaleBean) {
        this.raLocaleBean = raLocaleBean;
    }

    private String requestId;
    private int requestStatus;
    private EndEntityInformation endEntityInformation;
    private byte[] generatedToken;
    private String password;

    @PostConstruct
    private void postConstruct() {
        reset();
    }

    public void reset() {
        requestStatus = ApprovalDataVO.STATUS_WAITINGFORAPPROVAL;
        endEntityInformation = null;
    }

    /**
     * Check the status of request ID
     */
    public void checkRequestId() {
        if (Integer.parseInt(requestId) != 0) {
            RaApprovalRequestInfo raApprovalRequestInfo = raMasterApiProxyBean
                    .getApprovalRequestByRequestHash(raAuthenticationBean.getAuthenticationToken(), Integer.parseInt(requestId));
            if (raApprovalRequestInfo == null) {
                raLocaleBean.addMessageError("enrollwithrequestid_could_not_find_request_with_request_id", Integer.parseInt(requestId));
                return;
            }

            requestStatus = raApprovalRequestInfo.getStatus();
            switch (requestStatus) {
            case ApprovalDataVO.STATUS_WAITINGFORAPPROVAL:
                raLocaleBean.addMessageInfo("enrollwithrequestid_request_with_request_id_is_still_waiting_for_approval", Integer.parseInt(requestId));
                return;
            case ApprovalDataVO.STATUS_REJECTED:
            case ApprovalDataVO.STATUS_EXECUTIONDENIED:
                raLocaleBean.addMessageInfo("enrollwithrequestid_request_with_request_id_has_been_rejected", Integer.parseInt(requestId));
                return;
            case ApprovalDataVO.STATUS_APPROVED:
            case ApprovalDataVO.STATUS_EXECUTED:
                raLocaleBean.addMessageInfo("enrollwithrequestid_request_with_request_id_has_been_approved", Integer.parseInt(requestId));
                break;
            default:
                raLocaleBean.addMessageInfo("enrollwithrequestid_request_with_request_id_is_still_waiting_for_approval", Integer.parseInt(requestId));
                return;
            }

            //Get username and set the password to be the same as username
            String username = raApprovalRequestInfo.getEditableData().getUsername();
            endEntityInformation = raMasterApiProxyBean.searchUser(raAuthenticationBean.getAuthenticationToken(), username);
            if (endEntityInformation == null) {
                log.error("Could not find endEntity for the username='" + username + "'");
                return;
            }

        }
    }

    public boolean isFinalizeEnrollmentRendered() {
        return (requestStatus == ApprovalDataVO.STATUS_APPROVED || requestStatus == ApprovalDataVO.STATUS_EXECUTED) && endEntityInformation.getStatus() == EndEntityConstants.STATUS_NEW;
    }

    public void finalizeEnrollment() {
        //Generate token
        if (endEntityInformation.getTokenType() == EndEntityConstants.TOKEN_USERGEN) {
            byte[] certificateRequest = endEntityInformation.getExtendedinformation().getCertificateRequest();
            if (certificateRequest == null) {
                raLocaleBean.addMessageError("enrollwithrequestid_could_not_find_csr_inside_enrollment_request_with_request_id", requestId);
                log.info("Could not find CSR inside enrollment request with request ID " + requestId);
                return;
            }
            try {
                endEntityInformation.setPassword(endEntityInformation.getUsername());
                generatedToken = raMasterApiProxyBean.createCertificate(raAuthenticationBean.getAuthenticationToken(), endEntityInformation,
                        certificateRequest);
                log.info(endEntityInformation.getTokenType() + " token has been generated for the end entity with username " +
                        endEntityInformation.getUsername());
                downloadAsDer();
            } catch (AuthorizationDeniedException e){
                raLocaleBean.addMessageInfo("enroll_unauthorized_operation", e.getMessage());
                log.info("You are not authorized to execute this operation", e);
            } catch (EjbcaException e) {
                ErrorCode errorCode = EjbcaException.getErrorCode(e);
                if (errorCode != null) {
                    raLocaleBean.addMessageError(errorCode);
                    log.info("EjbcaException has been caught. Error Code: " + errorCode, e);
                } else {
                    raLocaleBean.addMessageError("enroll_certificate_could_not_be_generated", endEntityInformation.getUsername(), e.getMessage());
                    log.info("Certificate could not be generated for end entity with username " + endEntityInformation.getUsername(), e);
                }
                return;
            }
        } else {
            try {
                byte[] keystoreAsByteArray = raMasterApiProxyBean.generateKeystore(raAuthenticationBean.getAuthenticationToken(), endEntityInformation);
                log.info(endEntityInformation.getTokenType() + " token has been generated for the end entity with username " +
                        endEntityInformation.getUsername());
                try(ByteArrayOutputStream buffer = new ByteArrayOutputStream()){
                    buffer.write(keystoreAsByteArray);
                    generatedToken = buffer.toByteArray();
                }        
                if (endEntityInformation.getTokenType() == EndEntityConstants.TOKEN_SOFT_JKS) {
                    downloadJks();
                } else if (endEntityInformation.getTokenType() == EndEntityConstants.TOKEN_SOFT_P12) {
                    downloadPkcs12();
                }
            } catch (AuthorizationDeniedException e){
                raLocaleBean.addMessageInfo("enroll_unauthorized_operation", e.getMessage());
                log.info("You are not authorized to execute this operation", e);
            } catch (EjbcaException | IOException e) {
                ErrorCode errorCode = EjbcaException.getErrorCode(e);
                if (errorCode != null) {
                    raLocaleBean.addMessageError(errorCode);
                    log.info("EjbcaException has been caught. Error Code: " + errorCode, e);
                } else {
                    raLocaleBean.addMessageError("enroll_keystore_could_not_be_generated", endEntityInformation.getUsername(), e.getMessage());
                    log.info("Keystore could not be generated for user " + endEntityInformation.getUsername());
                }
                return;
            }
        }
        
        reset();

    }

    public final void downloadAsDer() {
        if (endEntityInformation.getTokenType() != EndEntityConstants.TOKEN_USERGEN) {
            throw new IllegalStateException();
        }
        downloadToken(generatedToken, "application/octet-stream", ".der");
    }

    public final void downloadPkcs12() {
        if (endEntityInformation.getTokenType() != EndEntityConstants.TOKEN_SOFT_P12) {
            throw new IllegalStateException();
        }
        downloadToken(generatedToken, "application/x-pkcs12", ".p12");
    }

    public final void downloadJks() {
        if (endEntityInformation.getTokenType() != EndEntityConstants.TOKEN_SOFT_JKS) {
            throw new IllegalStateException();
        }
        downloadToken(generatedToken, "application/octet-stream", ".jks");
    }

    private final void downloadToken(byte[] token, String responseContentType, String fileExtension) {
        if (token == null) {
            return;
        }

        //Download the token
        FacesContext fc = FacesContext.getCurrentInstance();
        ExternalContext ec = fc.getExternalContext();
        ec.responseReset(); // Some JSF component library or some Filter might have set some headers in the buffer beforehand. We want to get rid of them, else it may collide.
        ec.setResponseContentType(responseContentType);
        ec.setResponseContentLength(token.length);
        String commonName = "dummyTokenName";
        try {
            LdapName ldapName = new LdapName(endEntityInformation.getDN());
            for(Rdn rdn : ldapName.getRdns()) {
                if(rdn.getType().equalsIgnoreCase("CN")) {
                    commonName = (String) rdn.getValue();
                    break;
                }
            }
        } catch (InvalidNameException e1) {
            log.info(e1);
        } 
                
        final String filename = StringTools.stripFilename(commonName + fileExtension);
        ec.setResponseHeader("Content-Disposition", "attachment; filename=\"" + filename + "\""); // The Save As popup magic is done here. You can give it any file name you want, this only won't work in MSIE, it will use current request URL as file name instead.
        OutputStream output = null;
        try {
            output = ec.getResponseOutputStream();
            output.write(token);
            output.flush();
            fc.responseComplete(); // Important! Otherwise JSF will attempt to render the response which obviously will fail since it's already written with a file and closed.
        } catch (IOException e) {
            log.info("Token " + filename + " could not be downloaded", e);
            raLocaleBean.addMessageError("enroll_token_could_not_be_downloaded", filename);
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public boolean isRenderPassword() {
        return endEntityInformation.getTokenType() == EndEntityConstants.TOKEN_SOFT_JKS ||
                endEntityInformation.getTokenType() == EndEntityConstants.TOKEN_SOFT_P12||
                        endEntityInformation.getTokenType() == EndEntityConstants.TOKEN_SOFT_PEM;
    }

    //-----------------------------------------------------------------
    //Getters/setters
    public EndEntityInformation getEndEntityInformation() {
        return endEntityInformation;
    }

    public void setEndEntityInformation(EndEntityInformation endEntityInformation) {
        this.endEntityInformation = endEntityInformation;
    }

    /**
     * @return the requestId
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * @param requestId the requestId to set
     */
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public int getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(int requestStatus) {
        this.requestStatus = requestStatus;
    }

    /**
     * @return the generatedToken (.p12, .jks or .pem without full chain)
     */
    public byte[] getGeneratedToken() {
        return generatedToken;
    }

    public void setGeneratedToken(byte[] generatedToken) {
        this.generatedToken = generatedToken;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
