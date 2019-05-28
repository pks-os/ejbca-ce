/*************************************************************************
 *                                                                       *
 *  EJBCA - Proprietary Modules: Enterprise Certificate Authority        *
 *                                                                       *
 *  Copyright (c), PrimeKey Solutions AB. All rights reserved.           *
 *  The use of the Proprietary Modules are subject to specific           * 
 *  commercial license terms.                                            *
 *                                                                       *
 *************************************************************************/

package org.ejbca.ui.web.rest.api.resource;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;
import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.keys.token.CryptoTokenAuthenticationFailedException;
import org.cesecore.keys.token.CryptoTokenInfo;
import org.cesecore.keys.token.CryptoTokenManagementSessionLocal;
import org.cesecore.keys.token.CryptoTokenOfflineException;
import org.ejbca.core.ejb.rest.EjbcaRestHelperSessionLocal;
import org.ejbca.core.model.era.RaMasterApiProxyBeanLocal;
import org.ejbca.ui.web.rest.api.exception.RestException;
import org.ejbca.ui.web.rest.api.io.request.CryptoTokenActivationRestRequest;
import org.ejbca.ui.web.rest.api.io.request.CryptoTokenKeyGenerationRestRequest;
import org.ejbca.ui.web.rest.api.io.response.CryptoTokenRestResponse;
import org.ejbca.ui.web.rest.api.io.response.RestResourceStatusRestResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * JAX-RS resource handling Crypto Token related requests.
 *
 * @version $Id$
 */
@Api(tags = {"v1/cryptotoken"}, value = "Crypto Token REST Management API")
@Path("/v1/cryptotoken")
@Produces(MediaType.APPLICATION_JSON)
@Stateless
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class CryptoTokenRestResource extends BaseRestResource {

    @EJB
    private EjbcaRestHelperSessionLocal ejbcaRestHelperSession;
    @EJB
    private RaMasterApiProxyBeanLocal raMasterApiProxy;
    @EJB
    private CryptoTokenManagementSessionLocal cryptoTokenManagementSession;
    
    private static final Logger log = Logger.getLogger(CryptoTokenRestResource.class);

    @GET
    @Path("/status")
    @ApiOperation(value = "Get the status of this REST Resource", 
                  notes = "Returns status, API version and EJBCA version.",  
                  response = RestResourceStatusRestResponse.class)
    @Override
    public Response status() {
        return super.status();
    }
    
    @PUT
    @Path("/{cryptotoken_name}/activate")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Activate a Crypto Token",
        notes = "Activates Crypto Token given name and activation code",
        code = 200)
    public Response activate(
            @Context HttpServletRequest requestContext,
            @ApiParam(value = "Name of the token to activate")
            @PathParam("cryptotoken_name") String cryptoTokenName,
            @ApiParam (value="activation code") CryptoTokenActivationRestRequest request) throws AuthorizationDeniedException, RestException, CryptoTokenOfflineException {
        final AuthenticationToken admin = getAdmin(requestContext, false);
        final char[] activationCode = request.getActivationCode().toCharArray();
        final Integer cryptoTokenId = cryptoTokenManagementSession.getIdFromName(cryptoTokenName);
        if (cryptoTokenId == null) {
            throw new RestException(HTTP_STATUS_CODE_UNPROCESSABLE_ENTITY, "Unknown crypto token");
        }
        try {
            cryptoTokenManagementSession.activate(admin, cryptoTokenId, activationCode);
        } catch (CryptoTokenOfflineException e) {
            log.info("Activation of CryptoToken '" + cryptoTokenName + "' (" + cryptoTokenId +
                    ") by administrator " + admin.toString() + " failed. Device was unavailable.");
            throw e;
        } catch (CryptoTokenAuthenticationFailedException e) {
            log.info("Activation of CryptoToken '" + cryptoTokenName + "' (" + cryptoTokenId +
                    ") by administrator " + admin.toString() + " failed. Authentication code was not correct.");
            throw new RestException(HTTP_STATUS_CODE_UNPROCESSABLE_ENTITY, "Invalid activation code");
        }
        
        return Response.status(Status.OK).build();
    }

    @PUT
    @Path("/{cryptotoken_name}/deactivate")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Deactivate a Crypto Token",
        notes = "Deactivates Crypto Token given name",
        code = 200)
    public Response deactivate(
            @Context HttpServletRequest requestContext,
            @ApiParam(value = "Name of the token to deactivate")
            @PathParam("cryptotoken_name") String cryptoTokenName) throws AuthorizationDeniedException, RestException {
        final AuthenticationToken admin = getAdmin(requestContext, false);
        final Integer cryptoTokenId = cryptoTokenManagementSession.getIdFromName(cryptoTokenName);
        if (cryptoTokenId == null) {
            throw new RestException(HTTP_STATUS_CODE_UNPROCESSABLE_ENTITY, "Unknown crypto token");
        }
        cryptoTokenManagementSession.deactivate(admin, cryptoTokenId);
        CryptoTokenInfo info = cryptoTokenManagementSession.getCryptoTokenInfo(cryptoTokenId);
        String message = "The crypto token was deactivated successfully.";
        if (info.isActive() && info.isAutoActivation()) {
            message = "The crypto token was reactivated due to automatic reactivation being enabled.";
        }
        
        return Response.ok(CryptoTokenRestResponse.builder()
                .message(message)
                .build()
        ).build();
    }
    
    @POST
    @Path("/{cryptotoken_name}/generatekeys")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Generate keys",
        notes = "Generates a key pair given crypto token name, key pair alias, key algorithm and key specification",
        code = 201)
    public Response generateKeys(
            @Context HttpServletRequest requestContext,
            @ApiParam(value = "Name of the token to generate keys for")
            @PathParam("cryptotoken_name") String cryptoTokenName,
            CryptoTokenKeyGenerationRestRequest request) throws AuthorizationDeniedException, RestException, CryptoTokenOfflineException {
        final AuthenticationToken admin = getAdmin(requestContext, false);
        final String keyPairAlias = request.getKeyPairAlias();
        final String keyAlg = request.getKeyAlg();
        final String keySpec = request.getKeySpec();
        final Integer cryptoTokenId = cryptoTokenManagementSession.getIdFromName(cryptoTokenName);
        if (cryptoTokenId == null) {
            throw new RestException(HTTP_STATUS_CODE_UNPROCESSABLE_ENTITY, "Unknown crypto token");
        }
        try {
            cryptoTokenManagementSession.createKeyPair(admin, cryptoTokenId, keyPairAlias, keyAlg + keySpec);
        } catch (CryptoTokenOfflineException e) {
            String errorMessage = "Key generation for CryptoToken '" + cryptoTokenName + "' (" + cryptoTokenId +
                    ") by administrator " + admin.toString() + " failed. Device was unavailable.";
            log.info(errorMessage);
            throw e;
        } catch (InvalidKeyException e) {
            String errorMessage = "Key generation for CryptoToken '" + cryptoTokenName + "' (" + cryptoTokenId +
                    ") by administrator " + admin.toString() + " failed. Alias is in use, key length is invalid or testing the key pair fails.";
            log.info(errorMessage);
            throw new RestException(HTTP_STATUS_CODE_UNPROCESSABLE_ENTITY, errorMessage);
        } catch (InvalidAlgorithmParameterException e) {
            String errorMessage = "Key generation for CryptoToken '" + cryptoTokenName + "' (" + cryptoTokenId +
                    ") by administrator " + admin.toString() + " failed. Invalid algorithm parameter(s). Is the chosen key algorithm correct?";
            log.info(errorMessage);
            throw new RestException(HTTP_STATUS_CODE_UNPROCESSABLE_ENTITY, errorMessage);            
        }
        
        return Response.status(Status.CREATED).build();
    }
    
    @POST
    @Path("/{cryptotoken_name}/{key_pair_alias}/removekeys")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Remove keys",
        notes = "Remove a key pair given crypto token name and key pair alias to be removed.",
        code = 200)
    public Response removeKeys(
            @Context HttpServletRequest requestContext,
            @ApiParam(value = "Name of the token to remove keys for.") 
            @PathParam("cryptotoken_name") String cryptoTokenName,
            @ApiParam(value = "Alias for the key to be removed from the crypto token.") 
            @PathParam("key_pair_alias") String keyPairAlias) throws AuthorizationDeniedException, RestException, CryptoTokenOfflineException {
        final AuthenticationToken admin = getAdmin(requestContext, false);
        final Integer cryptoTokenId = cryptoTokenManagementSession.getIdFromName(cryptoTokenName);
        if (cryptoTokenId == null) {
            throw new RestException(HTTP_STATUS_CODE_UNPROCESSABLE_ENTITY, "Unknown crypto token");
        }
        try {
            cryptoTokenManagementSession.removeKeyPair(admin, cryptoTokenId, keyPairAlias);
        } catch (CryptoTokenOfflineException e) {
            String errorMessage = "Key generation for CryptoToken '" + cryptoTokenName + "' (" + cryptoTokenId +
                    ") by administrator " + admin.toString() + " failed. Device was unavailable.";
            log.info(errorMessage);
            throw e;
        } catch (InvalidKeyException e) {
            String errorMessage = "Key generation for CryptoToken '" + cryptoTokenName + "' (" + cryptoTokenId +
                    ") by administrator " + admin.toString() + " failed. Alias is in use, key length is invalid or testing the key pair fails.";
            log.info(errorMessage);
            throw new RestException(HTTP_STATUS_CODE_UNPROCESSABLE_ENTITY, errorMessage);
        }
        return Response.status(Status.OK).build();
    }
}
