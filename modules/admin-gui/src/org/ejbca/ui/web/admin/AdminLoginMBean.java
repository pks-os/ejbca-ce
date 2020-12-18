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
package org.ejbca.ui.web.admin;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.cesecore.authentication.oauth.OAuthGrantResponseInfo;
import org.cesecore.authentication.oauth.OAuthKeyInfo;
import org.cesecore.authentication.oauth.OAuthTokenRequest;
import org.ejbca.config.GlobalConfiguration;
import org.ejbca.ui.web.jsf.configuration.EjbcaWebBean;
import org.ejbca.util.HttpTools;
import org.keycloak.OAuthErrorException;
import org.keycloak.adapters.ServerRequest;
import org.keycloak.adapters.installed.KeycloakInstalled;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;

/**
 * Bean used to display a login page.
 */
@ManagedBean
@SessionScoped
public class AdminLoginMBean extends BaseManagedBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(AdminLoginMBean.class);

    private EjbcaWebBean ejbcaWebBean;

    private List<Throwable> throwables = null;
    private String errorMessage;
    private Collection<OAuthKeyInfoGui> oauthKeys = null;
    /** A random identifier used to link requests, to avoid CSRF attacks. */
    private String stateInSession = null;

    public class OAuthKeyInfoGui{
        String label;
        String url;

        public OAuthKeyInfoGui(String label, String url) {
            this.label = label;
            this.url = url;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    /**
     * @return the general error which occurred
     */
    public String getError() {
        return ejbcaWebBean.getText("AUTHORIZATIONDENIED");
    }

    /**
     * @return please login message
     */
    public String getPleaseLogin() {
        return ejbcaWebBean.getText("PLEASE_LOGIN");
    }

    /**
     * @return error message generated by application exceptions
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * without access to template, we have to fetch the CSS manually
     *
     * @return path to admin web CSS file
     **/
    public String getCssFile() {
        try {
            return ejbcaWebBean.getBaseUrl() + "/" + ejbcaWebBean.getCssFile();
        } catch (Exception e) {
            // This happens when EjbcaWebBeanImpl fails to initialize.
            // That is already logged in EjbcaWebBeanImpl.getText, so log at debug level here.
            final String msg = "Caught exception when trying to get stylesheet URL, most likely EjbcaWebBean failed to initialized";
            if (log.isTraceEnabled()) {
                log.debug(msg, e);
            } else {
                log.debug(msg);
            }
            return "exception_in_getCssFile";
        }
    }

    /**
     * Invoked when login.xhtml is rendered. Show errors and possible login links.
     */
    @SuppressWarnings("unchecked")
    public void onLoginPageLoad() throws Exception {
        ejbcaWebBean = getEjbcaErrorWebBean();
        ejbcaWebBean.initialize_errorpage((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest());
        final Map<String, Object> requestMap = FacesContext.getCurrentInstance().getExternalContext().getRequestMap();
        final Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        final String authCode = params.get("code");
        final String state = params.get("state");
        final String error = params.get("error");
        // Render error caught by CaExceptionHandlerFactory
        if (throwables == null) {
            throwables = (List<Throwable>) requestMap.get(CaExceptionHandlerFactory.REQUESTMAP_KEY);
            requestMap.remove(CaExceptionHandlerFactory.REQUESTMAP_KEY);
        }
        if (error != null) {
            log.info("Server reported user authentication failure: " + error.replaceAll("[^a-zA-Z0-9_]", "")); // sanitize untrusted parameter
            if (verifyStateParameter(state)) {
                errorMessage = params.getOrDefault("error_description", "");
            } else {
                log.info("Received 'error' parameter without valid 'state' parameter.");
                errorMessage = "Internal error.";
            }
        } else if (CollectionUtils.isEmpty(throwables)) {
            log.debug("No exception thrown.");
            for (final Throwable throwable : throwables) {
                if (log.isDebugEnabled()) {
                    log.debug("Error occurred.", throwable);
                }
                errorMessage = ejbcaWebBean.getText("CAUSE") + ": " + throwable.getMessage();
            }
        } else if (StringUtils.isNotEmpty(authCode)) {
            log.debug("Received authorization code. Requesting token from authorization server.");
            if (verifyStateParameter(state)) {
                final OAuthTokenRequest request = new OAuthTokenRequest();
                request.setUri("http://localhost:8049/auth/realms/EJBCA/protocol/openid-connect/token");
                request.setClientId("EJBCAAdminWeb"); // TODO configuration
                request.setRedirectUri("https://localhost:8442/ejbca/adminweb/"); // TODO configuration
                final OAuthGrantResponseInfo token = request.execute(authCode);
                if (token.compareTokenType(HttpTools.AUTHORIZATION_SCHEME_BEARER)) {
                    log.info("XXX RECEIVED TOKEN " + token.getAccessToken()); // XXX remove me
                    // TODO
                } else {
                    log.info("Received OAuth token of unsupported type '" + token.getTokenType() + "'");
                    errorMessage = "Internal error.";
                }
            } else {
                log.info("Received 'code' parameter without valid 'state' parameter.");
                errorMessage = "Internal error.";
            }
        } else {
            log.debug("Generating randomized 'state' string.");
            final byte[] stateBytes = new byte[32];
            new SecureRandom().nextBytes(stateBytes);
            stateInSession = Base64.encodeBase64URLSafeString(stateBytes);
            log.debug("Showing login links.");
        }
    }

    private boolean verifyStateParameter(final String state) {
        return stateInSession != null && stateInSession.equals(state);
    }

    /**
     * Returns a list of OAuth Keys containing url.
     *
     * @return a list of OAuth Keys containing url
     */
    public Collection<OAuthKeyInfoGui> getAllOauthKeys() {
        if (oauthKeys == null) {
            oauthKeys = new ArrayList<>();
            ejbcaWebBean.reloadGlobalConfiguration();
            GlobalConfiguration globalConfiguration = ejbcaWebBean.getGlobalConfiguration();
            Collection<OAuthKeyInfo> oAuthKeyInfos = globalConfiguration.getOauthKeys().values();
            if (!oAuthKeyInfos.isEmpty()) {
                for (OAuthKeyInfo oauthKeyInfo : oAuthKeyInfos) {
                    if (!StringUtils.isEmpty(oauthKeyInfo.getUrl())) {
                        URI uri = UriBuilder.fromUri(oauthKeyInfo.getUrl())
                                .queryParam("response_type", "code")
                                .queryParam("state", stateInSession)
                                .build();
                        oauthKeys.add(new OAuthKeyInfoGui(oauthKeyInfo.getKeyIdentifier(), uri.toString()));
                    }
                }
            }
        }
        return oauthKeys;
    }

    public void doSomething() throws InterruptedException, IOException, URISyntaxException, OAuthErrorException, ServerRequest.HttpFailure, VerificationException {
        String keycloakConfigs = "{\n" +
                "  \"realm\": \"EJBCA\",\n" +
                "  \"auth-server-url\": \"http://localhost:8049/auth/\",\n" +
                "  \"ssl-required\": \"external\",\n" +
                "  \"resource\": \"EJBCAAdminWeb\",\n" +
                "  \"public-client\": true,\n" +
                "  \"verify-token-audience\": true,\n" +
                "  \"use-resource-role-mappings\": true,\n" +
                "  \"confidential-port\": 0\n" +
                "}";
        InputStream inputStream = new ByteArrayInputStream(keycloakConfigs.getBytes(StandardCharsets.UTF_8));
        KeycloakInstalled keycloak = new KeycloakInstalled(inputStream);
        keycloak.loginDesktop();
        AccessToken token = keycloak.getToken();
        String tokenString = keycloak.getTokenString(3000, TimeUnit.SECONDS);
        System.out.println(tokenString);
        System.out.println(token.getIssuer());
    }
}
