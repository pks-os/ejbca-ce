package org.ejbca.core.protocol.ws.common;

import java.rmi.RemoteException;
import java.util.List;

import org.ejbca.core.EjbcaException;
import org.ejbca.core.model.approval.ApprovalException;
import org.ejbca.core.model.approval.ApprovalRequestExpiredException;
import org.ejbca.core.model.approval.WaitingForApprovalException;
import org.ejbca.core.model.authorization.AuthorizationDeniedException;
import org.ejbca.core.model.ca.publisher.PublisherException;
import org.ejbca.core.model.hardtoken.HardTokenDoesntExistsException;
import org.ejbca.core.model.hardtoken.HardTokenExistsException;
import org.ejbca.core.model.ra.NotFoundException;
import org.ejbca.core.model.ra.raadmin.UserDoesntFullfillEndEntityProfile;
import org.ejbca.core.model.ra.userdatasource.UserDataSourceException;
import org.ejbca.core.protocol.ws.objects.Certificate;
import org.ejbca.core.protocol.ws.objects.HardTokenDataWS;
import org.ejbca.core.protocol.ws.objects.TokenCertificateRequestWS;
import org.ejbca.core.protocol.ws.objects.TokenCertificateResponseWS;
import org.ejbca.core.protocol.ws.objects.KeyStore;
import org.ejbca.core.protocol.ws.objects.RevokeStatus;
import org.ejbca.core.protocol.ws.objects.UserDataVOWS;
import org.ejbca.core.protocol.ws.objects.UserMatch;
import org.ejbca.util.query.IllegalQueryException;

/**
 * Interface the the EJBCA RA WebService. Contains the following methods:
 * 
 * editUser    : Edits/adds  userdata
 * findUser    : Retrieves the userdata for a given user.
 * findCerts   : Retrieves the certificates generated for a user.
 * pkcs10Req   : Generates a certificate using the given userdata and the public key from the PKCS10
 * pkcs12Req   : Generates a PKCS12 keystore (with the private key) using the given userdata
 * revokeCert  : Revokes the given certificate.
 * revokeUser  : Revokes all certificates for a given user, it's also possible to delete the user.
 * revokeToken : Revokes all certificates placed on a given hard token
 * checkRevokationStatus : Checks the revokation status of a certificate.
 * isAuthorized : Checks if an admin is authorized to an resource
 * fetchUserData : Method used to fetch userdata from an existing UserDataSource
 * genTokenCertificates : Method used to add information about a generated hardtoken
 * existsHardToken : Looks up if a serial number already have been generated
 * getHardTokenData : Method fetching information about a hard token given it's hard token serial number.
 * getHardTokenDatas: Method fetching all hard token informations for a given user.
 * republishCertificate : Method performing a republication of a selected certificate
 * isApproved : Looks up if a requested action have been approved by an authorized administrator or not
 * 
 * Observere: All methods have to be called using client authenticated https
 * otherwise will a AuthorizationDenied Exception be thrown.
 * 
 * @author Philip Vendil
 * $Id: IEjbcaWS.java,v 1.1 2007-03-07 10:08:55 herrvendil Exp $
 */
public interface IEjbcaWS {

	/**
	 * Method that should be used to edit/add a user to the EJBCA database,
	 * if the user doesn't already exists it will be added othervise it will be
	 * overwritten.
	 * 
	 * Observe: if the user doesn't already exists, it's status will always be set to 'New'.
	 * 
	 * Authorization requirements: the client certificate must have the following priviledges set
	 * - Administrator flag set
	 * - /administrator
	 * - /ra_functionality/create_end_entity and/or edit_end_entity
	 * - /ra_functionality/<end entity profile of user>/create_end_entity and/or edit_end_entity
	 * - /ca/<ca of user>
	 * 
	 * @param userdata contains all the information about the user about to be added.
	 * @param clearPwd indicates it the password should be stored in cleartext, requeried
	 * when creating server generated keystores.
	 * @throws EjbcaException 
	 */
	public abstract void editUser(UserDataVOWS userdata)
			throws AuthorizationDeniedException,
			UserDoesntFullfillEndEntityProfile, EjbcaException,
			ApprovalException, WaitingForApprovalException;

	/**
	 * Retreives information about a user in the database.
	 * 
	 * Authorization requirements: the client certificate must have the following priviledges set
	 * - Administrator flag set
	 * - /administrator
	 * - /ra_functionality/view_end_entity
	 * - /ra_functionality/<end entity profile of matching users>/view_end_entity
	 * - /ca/<ca of matching users>
	 * 
	 * @param username, the unique username to search for
	 * @return a array of UserDataVOWS objects (Max 100) containing the information about the user or null if user doesn't exists.
	 * @throws AuthorizationDeniedException if client isn't authorized to request
	 * @throws IllegalQueryException if query isn't valid
	 * @throws EjbcaException 
	 */

	public abstract List<UserDataVOWS> findUser(UserMatch usermatch)
			throws AuthorizationDeniedException, IllegalQueryException,
			EjbcaException;

	/**
	 * Retreives a collection of certificates generated for a user.
	 * 
	 * Authorization requirements: the client certificate must have the following priviledges set
	 * - Administrator flag set
	 * - /administrator
	 * - /ra_functionality/view_end_entity
	 * - /ra_functionality/<end entity profile of the user>/view_end_entity
	 * - /ca/<ca of user>
	 * 
	 * @param username a unique username 
	 * @param onlyValid only return valid certs not revoked or expired ones.
	 * @return a collection of X509Certificates or null if no certificates could be found
	 * @throws AuthorizationDeniedException if client isn't authorized to request
	 * @throws NotFoundException if user cannot be found
	 * @throws EjbcaException 
	 */

	public abstract List<Certificate> findCerts(String username,
			boolean onlyValid) throws AuthorizationDeniedException,
			NotFoundException, EjbcaException;

	/**
	 * Method to use to generate a certificate for a user. The method must be preceded by
	 * a editUser call, either to set the userstatus to 'new' or to add nonexisting users.
	 * 
	 * Observe, the user must first have added/set the status to new with edituser command
	 * 
	 * Authorization requirements: the client certificate must have the following priviledges set
	 * - Administrator flag set
	 * - /administrator
	 * - /ra_functionality/view_end_entity
	 * - /ra_functionality/<end entity profile of the user>/view_end_entity
	 * - /ca_functionality/create_certificate
	 * - /ca/<ca of user>
	 * 
	 * @param username the unique username
	 * @param password the password sent with editUser call
	 * @param pkcs10 the PKCS10 (only the public key is used.)
	 * @param hardTokenSN If the certificate should be connected with a hardtoken, it is
	 * possible to map it by give the hardTokenSN here, this will simplyfy revokation of a tokens
	 * certificates. Use null if no hardtokenSN should be assiciated with the certificate.
	 * @return the generated certificate.
	 * @throws AuthorizationDeniedException if client isn't authorized to request
	 * @throws NotFoundException if user cannot be found
	 */

	public abstract Certificate pkcs10Req(String username, String password,
			String pkcs10, String hardTokenSN)
			throws AuthorizationDeniedException, NotFoundException,
			EjbcaException;

	/**
	 * Method to use to generate a server generated keystore. The method must be preceded by
	 * a editUser call, either to set the userstatus to 'new' or to add nonexisting users and
	 * the users token should be set to SecConst.TOKEN_SOFT_P12.
	 * 
	 * Authorization requirements: the client certificate must have the following priviledges set
	 * - Administrator flag set
	 * - /administrator
	 * - /ra_functionality/view_end_entity
	 * - /ra_functionality/<end entity profile of the user>/view_end_entity
	 * - /ca_functionality/create_certificate
	 * - /ca/<ca of user>
	 * 
	 * @param username the unique username
	 * @param password the password sent with editUser call
	 * @param hardTokenSN If the certificate should be connected with a hardtoken, it is
	 * possible to map it by give the hardTokenSN here, this will simplyfy revokation of a tokens
	 * certificates. Use null if no hardtokenSN should be assiciated with the certificate.
	 * @param keyspec that the generated key should have, examples are 1024 for RSA or prime192v1 for ECDSA.
	 * @param keyalg that the generated key should have, RSA, ECDSA. Use one of the constants in CATokenConstants.org.ejbca.core.model.ca.catoken.KEYALGORITHM_XX.
	 * @return the generated keystore
	 * @throws AuthorizationDeniedException if client isn't authorized to request
	 * @throws NotFoundException if user cannot be found
	 */

	public abstract KeyStore pkcs12Req(String username, String password,
			String hardTokenSN, String keyspec, String keyalg)
			throws AuthorizationDeniedException, NotFoundException,
			EjbcaException;

	/**
	 * Method used to revoke a certificate.
	 * 
	 * * Authorization requirements: the client certificate must have the following priviledges set
	 * - Administrator flag set
	 * - /administrator
	 * - /ra_functionality/revoke_end_entity
	 * - /ra_functionality/<end entity profile of the user owning the cert>/revoke_end_entity
	 * - /ca/<ca of certificate>
	 * 
	 * @param issuerDN of the certificate to revoke
	 * @param certificateSN of the certificate to revoke
	 * @param reason for revokation, one of RevokedCertInfo.REVOKATION_REASON_ constants
	 * @throws AuthorizationDeniedException if client isn't authorized.
	 * @throws NotFoundException if certificate doesn't exist
	 */

	public abstract void revokeCert(String issuerDN, String certificateSN,
			int reason) throws AuthorizationDeniedException, NotFoundException,
			EjbcaException;

	/**
	 * Method used to revoke all a users certificates. It is also possible to delete
	 * a user after all certificates have been revoked.
	 * 
	 * Authorization requirements: the client certificate must have the following priviledges set
	 * - Administrator flag set
	 * - /administrator
	 * - /ra_functionality/revoke_end_entity
	 * - /ra_functionality/<end entity profile of the user>/revoke_end_entity
	 * - /ca/<ca of users certificate>
	 * 
	 * @param username unique username i EJBCA
	 * @param reasonfor revokation, one of RevokedCertInfo.REVOKATION_REASON_ constants
	 * @param deleteUser deletes the users after all the certificates have been revoked.
	 * @throws AuthorizationDeniedException if client isn't authorized.
	 * @throws NotFoundException if user doesn't exist
	 */
	public abstract void revokeUser(String username, int reason,
			boolean deleteUser) throws AuthorizationDeniedException,
			NotFoundException, EjbcaException;

	/**
	 * Method used to revoke all certificates mapped to one hardtoken.
	 *
	 * Authorization requirements: the client certificate must have the following priviledges set
	 * - Administrator flag set
	 * - /administrator
	 * - /ra_functionality/revoke_end_entity
	 * - /ra_functionality/<end entity profile of the user owning the token>/revoke_end_entity
	 * - /ca/<ca of certificates on token>
	 * 
	 * @param hardTokenSN of the hardTokenSN
	 * @param reasonfor revokation, one of RevokedCertInfo.REVOKATION_REASON_ constants
	 * @throws AuthorizationDeniedException if client isn't authorized.
	 * @throws NotFoundException if token doesn't exist
	 */

	public abstract void revokeToken(String hardTokenSN, int reason)
			throws RemoteException, AuthorizationDeniedException,
			NotFoundException, EjbcaException;

	/**
	 * Method returning the revokestatus for given user
	 * 
	 * Authorization requirements: the client certificate must have the following priviledges set
	 * - Administrator flag set
	 * - /administrator
	 * - /ca/<ca of certificate>
	 * 
	 * @param issuerDN 
	 * @param certificateSN a hexadecimal string
	 * @return the revokestatus of null i certificate doesn't exists.
	 * @throws AuthorizationDeniedException if client isn't authorized.
	 * @see org.ejbca.core.protocol.ws.RevokeStatus
	 */

	public abstract RevokeStatus checkRevokationStatus(String issuerDN,
			String certificateSN) throws AuthorizationDeniedException,
			EjbcaException;

	/**
	 * Method checking if a user is authorixed to a given resource
	 * 
	 * Authorization requirements: a valid client certificate
	 * 
	 * @param resource the access rule to test
	 * @return true if the user is authorized to the resource othervise false.
	 * @throws AuthorizationDeniedException if client isn't authorized.
	 * @see org.ejbca.core.protocol.ws.RevokeStatus
	 */
	public abstract boolean isAuthorized(String resource) throws EjbcaException;

	/**
	 * Method used to fetch userdata from an existing UserDataSource.
	 * 
	 * Authorization requirements: A valid certificate
	 * 
	 * 
	 * @param userDataSourceIds a List of User Data Source Ids
	 * @param searchString to identify the userdata.
	 * @return a List of UserDataVOWS of the data in the specified UserDataSources, if no user data is found will an empty list be returned. 
	 * @throws UserDataSourceException if an error occured connecting to one of 
	 * UserDataSources.
	 */
	public abstract List<UserDataVOWS> fetchUserData(
			List<Integer> userDataSourceIds, String searchString)
			throws UserDataSourceException, EjbcaException;

	/**
	 * Method used to add information about a generated hardtoken
	 * 
	 * Authorization requirements:
	 * If the caller is an administrator
	 * - Administrator flag set
	 * - /administrator
	 * - /ra_functionality/create_end_entity and/or edit_end_entity
	 * - /ra_functionality/<end entity profile of user>/create_end_entity and/or edit_end_entity
	 * - /ra_functionality/view_end_entity
	 * - /ra_functionality/<end entity profile of the user>/view_end_entity
	 * - /ca_functionality/create_certificate
	 * - /ca/<ca of user>
	 * 
	 * If the user isn't an administrator will it be added to the queue for approval.
	 * 
	 * @param userData of the user that should be generated
	 * @param tokenRequests a list of certificate requests
	 * @param hardTokenData data containin PIN/PUK info
	 * @param hardTokenSN Serial number of the generated hard token.
	 * @return a List of the generated certificates. 
	 * @throws AuthorizationDeniedException if the administrator isn't authorized.
	 * @throws WaitingForApprovalException if the caller is a non-admin a must be approved before it is executed.
	 * @throws HardTokenExistsException if the given hardtokensn already exists.
	 */

	public abstract List<TokenCertificateResponseWS> genTokenCertificates(
			UserDataVOWS userData,
			List<TokenCertificateRequestWS> tokenRequests,
			HardTokenDataWS hardTokenData) throws AuthorizationDeniedException,
			WaitingForApprovalException, HardTokenExistsException,
			EjbcaException;

	/**
	 * Looks up if a serial number already have been generated
	 * 
	 * Authorization requirements: A valid certificate
	 * 
	 * @param hardTokenSN the serial number of the token to look for.
	 * @return true if hard token exists
	 * @throws EjbcaException if error occured server side
	 */
	public abstract boolean existsHardToken(String hardTokenSN)
			throws EjbcaException;

	/**
	 * Method fetching information about a hard token given it's hard token serial number.
	 * 
	 * If the caller is an administrator
	 * - Administrator flag set
	 * - /administrator
	 * - /ra_functionality/view_hardtoken
	 * - /ra_functionality/<end entity profile of user>/view_hardtoken
	 * - /ca/<ca of user>
	 * 
	 * If the user isn't an administrator will it be added to the queue for approval.
	 * 
	 * @param hardTokenSN of the token to look for.
	 * @return the HardTokenData
	 * @throws HardTokenDoesntExistsException if the hardtokensn don't exist in database.
	 * @throws EjbcaException if an exception occured on server side.
	 */
	public abstract HardTokenDataWS getHardTokenData(String hardTokenSN)
			throws AuthorizationDeniedException,
			HardTokenDoesntExistsException, EjbcaException;

	/**
	 * Method fetching all hard token informations for a given user.
	 * 
	 * If the caller is an administrator
	 * - Administrator flag set
	 * - /administrator
	 * - /ra_functionality/view_hardtoken
	 * - /ra_functionality/<end entity profile of user>/view_hardtoken
	 * - /ca/<ca of user>
	 * 
	 * 
	 * @param username to look for.
	 * @return a list of the HardTokenData generated for the user never null.
	 * @throws EjbcaException if an exception occured on server side.
	 */
	public abstract List<HardTokenDataWS> getHardTokenDatas(String username)
			throws AuthorizationDeniedException, EjbcaException;

	/**
	 * Method performing a republication of a selected certificate
	 * 
	 * Authorization requirements:
	 * - Administrator flag set
	 * - /administrator
	 * - /ra_functionality/view_end_entity
	 * - /ra_functionality/<end entity profile of the user>/view_end_entity
	 * - /ca/<ca of user>
	 * 
	 * @param serialNumberInHex of the certificate to republish
	 * @param issuerDN of the certificate to republish
	 * @throws AuthorizationDeniedException if the administratior isn't authorized to republish
	 * @throws PublisherException if something went wrong during publication
	 * @throws EjbcaException if other error occured on the server side.
	 */
	public abstract void republishCertificate(String serialNumberInHex,
			String issuerDN) throws AuthorizationDeniedException,
			PublisherException, EjbcaException;

	/**
	 * Looks up if a requested action have been approved by an authorized administrator or not
	 * 
	 * Authorization requirements: A valid certificate
	 * 
	 * @param approvalId unique id for the action
	 * @return the number of approvals left, 0 if approved othervis is the ApprovalDataVO.STATUS constants returned indicating the statys.
	 * @throws ApprovalException if approvalId doesn't exists
	 * @throws ApprovalRequestExpiredException Throws this exception one time if one of the approvals have expired, once notified it wount throw it anymore.
	 * @throws EjbcaException if error occured server side
	 */
	public abstract int isApproved(int approvalId) throws ApprovalException,
			EjbcaException, ApprovalRequestExpiredException;

}