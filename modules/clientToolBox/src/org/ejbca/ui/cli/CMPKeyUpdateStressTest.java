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

package org.ejbca.ui.cli;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;

import org.bouncycastle.asn1.ASN1BitString;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OutputStream;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERGeneralizedTime;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.cmp.CMPCertificate;
import org.bouncycastle.asn1.cmp.CMPObjectIdentifiers;
import org.bouncycastle.asn1.cmp.CertConfirmContent;
import org.bouncycastle.asn1.cmp.CertOrEncCert;
import org.bouncycastle.asn1.cmp.CertRepMessage;
import org.bouncycastle.asn1.cmp.CertResponse;
import org.bouncycastle.asn1.cmp.CertStatus;
import org.bouncycastle.asn1.cmp.CertifiedKeyPair;
import org.bouncycastle.asn1.cmp.PBMParameter;
import org.bouncycastle.asn1.cmp.PKIBody;
import org.bouncycastle.asn1.cmp.PKIConfirmContent;
import org.bouncycastle.asn1.cmp.PKIHeader;
import org.bouncycastle.asn1.cmp.PKIHeaderBuilder;
import org.bouncycastle.asn1.cmp.PKIMessage;
import org.bouncycastle.asn1.cmp.PKIStatusInfo;
import org.bouncycastle.asn1.crmf.AttributeTypeAndValue;
import org.bouncycastle.asn1.crmf.CRMFObjectIdentifiers;
import org.bouncycastle.asn1.crmf.CertReqMessages;
import org.bouncycastle.asn1.crmf.CertReqMsg;
import org.bouncycastle.asn1.crmf.CertRequest;
import org.bouncycastle.asn1.crmf.CertTemplateBuilder;
import org.bouncycastle.asn1.crmf.OptionalValidity;
import org.bouncycastle.asn1.crmf.POPOSigningKey;
import org.bouncycastle.asn1.crmf.ProofOfPossession;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.cesecore.internal.InternalResources;
import org.cesecore.util.CertTools;
import org.cesecore.util.CryptoProviderTools;
import org.ejbca.core.protocol.cmp.CmpMessageHelper;
import org.ejbca.core.protocol.cmp.client.CMPSendHTTP;
import org.ejbca.util.PerformanceTest;
import org.ejbca.util.PerformanceTest.Command;
import org.ejbca.util.PerformanceTest.CommandFactory;
import org.ejbca.util.PerformanceTest.NrOfThreadsAndNrOfTests;

/**
 * Used to stress test the CMP interface.
 * Renewal of certificates already issued by the CA. The certificates will be
 * renewed for a key (generated by the test) different from the key of the certificate.
 * See also the printout done in {@link CLIArgs#CLIArgs(String[])}
 * 
 * @version $Id$
 */
public class CMPKeyUpdateStressTest extends ClientToolBox {
	/** Internal localization of logs and errors */
	private static final InternalResources intres = InternalResources.getInstance();

	private static class SessionData {
		private final CLIArgs cliArgs;
		private final KeyPair newKeyPair;
		private final CertificateFactory certificateFactory;
		private final Provider bcProvider = new BouncyCastleProvider();
		private final PerformanceTest performanceTest;

		private final X509Certificate cacert;
		private final X509Certificate extraCert;
		private final PrivateKey oldKey;

		private boolean isSign;
		private boolean firstTime = true;

		public SessionData(final CLIArgs clia, PerformanceTest performanceTest, KeyStore keyStore) throws Exception {
			this.cliArgs = clia;
			this.performanceTest = performanceTest;
			this.certificateFactory = CertificateFactory.getInstance("X.509", this.bcProvider);

			final KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");
			keygen.initialize(2048);
			this.newKeyPair = keygen.generateKeyPair();
			for( final Enumeration<String> aliases = keyStore.aliases(); true; ) {
				if ( !aliases.hasMoreElements() ) {
					throw new Exception("No key in keystore"+keyStore);
				}
				final String alias = aliases.nextElement();
				if ( !keyStore.isKeyEntry(alias) ) {
					continue;
				}
				this.oldKey = (PrivateKey)keyStore.getKey(alias, this.cliArgs.keystorePassword.toCharArray());
				final Certificate[] certs = keyStore.getCertificateChain(alias);
				this.extraCert = (X509Certificate)certs[0];
				this.cacert = (X509Certificate)certs[1];
				break;
			}
		}

		private CertRequest genKeyUpdateReq() throws IOException {
			final ASN1EncodableVector optionalValidityV = new ASN1EncodableVector();
			final int day = 1000 * 60 * 60 * 24;
			optionalValidityV.add(new DERTaggedObject(true, 0, new org.bouncycastle.asn1.x509.Time(new Date(new Date().getTime() - day))));
			optionalValidityV.add(new DERTaggedObject(true, 1, new org.bouncycastle.asn1.x509.Time(new Date(new Date().getTime() + 10 * day))));
			final OptionalValidity myOptionalValidity = OptionalValidity.getInstance(new DERSequence(optionalValidityV));

			final CertTemplateBuilder myCertTemplate = new CertTemplateBuilder();
			myCertTemplate.setValidity(myOptionalValidity);
			final SubjectPublicKeyInfo keyInfo = SubjectPublicKeyInfo.getInstance(newKeyPair.getPublic().getEncoded());
			myCertTemplate.setPublicKey(keyInfo);
			return new CertRequest(4, myCertTemplate.build(), null);
		}

		final PKIHeaderBuilder getPKIHeaderBuilder() throws IOException{
			return new PKIHeaderBuilder(
					2,
					new GeneralName(GeneralName.directoryName, ASN1Primitive.fromByteArray(this.extraCert.getSubjectX500Principal().getEncoded())),
					new GeneralName(GeneralName.directoryName, ASN1Primitive.fromByteArray(this.cacert.getSubjectX500Principal().getEncoded()))
					);
		}

		private PKIMessage genPKIMessage(final boolean raVerifiedPopo, 
				final CertRequest keyUpdateRequest, final AlgorithmIdentifier pAlg, final DEROctetString senderKID)
				throws NoSuchAlgorithmException, IOException, InvalidKeyException, SignatureException {

			final ProofOfPossession myProofOfPossession;
			if (raVerifiedPopo) {
				// raVerified POPO (meaning there is no POPO)
				myProofOfPossession = new ProofOfPossession();
			} else {
				final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		        final ASN1OutputStream mout = ASN1OutputStream.create(baos, ASN1Encoding.DER);
				mout.writeObject(keyUpdateRequest);
				mout.close();
				final byte[] popoProtectionBytes = baos.toByteArray();
				final Signature sig = Signature.getInstance(PKCSObjectIdentifiers.sha256WithRSAEncryption.getId());
				sig.initSign(this.newKeyPair.getPrivate());
				sig.update(popoProtectionBytes);

				final DERBitString bs = new DERBitString(sig.sign());

				final POPOSigningKey myPOPOSigningKey = new POPOSigningKey(null, new AlgorithmIdentifier(PKCSObjectIdentifiers.sha256WithRSAEncryption), bs);
				myProofOfPossession = new ProofOfPossession(myPOPOSigningKey);
			}

			final AttributeTypeAndValue av = new AttributeTypeAndValue(CRMFObjectIdentifiers.id_regCtrl_regToken, new DERUTF8String("foo123"));
			final AttributeTypeAndValue[] avs = {av};

			final CertReqMsg myCertReqMsg = new CertReqMsg(keyUpdateRequest, myProofOfPossession, avs);

			final CertReqMessages myCertReqMessages = new CertReqMessages(myCertReqMsg);

			final PKIHeaderBuilder myPKIHeader = getPKIHeaderBuilder();
			myPKIHeader.setMessageTime(new DERGeneralizedTime(new Date()));
			myPKIHeader.setSenderNonce(new DEROctetString(getNonce()));
			myPKIHeader.setSenderKID(new DEROctetString(getNonce()));
			myPKIHeader.setTransactionID(new DEROctetString(getTransId()));
			myPKIHeader.setProtectionAlg(pAlg);
			myPKIHeader.setSenderKID(senderKID);

			final PKIBody myPKIBody = new PKIBody(7, myCertReqMessages); // key update request
			return new PKIMessage(myPKIHeader.build(), myPKIBody);
		}

		private PKIMessage addExtraCert(PKIMessage msg) throws CertificateEncodingException, IOException {
			final ASN1InputStream ins = new ASN1InputStream(this.extraCert.getEncoded());
			final ASN1Primitive pcert = ins.readObject();
			ins.close();
			org.bouncycastle.asn1.x509.Certificate c = org.bouncycastle.asn1.x509.Certificate.getInstance(pcert.toASN1Primitive());
			final CMPCertificate cmpcert = new CMPCertificate(c);
			final CMPCertificate[] extraCerts = {cmpcert};
			return new PKIMessage(msg.getHeader(), msg.getBody(), msg.getProtection(), extraCerts);
		}

		private PKIMessage signPKIMessage(final PKIMessage msg) throws NoSuchAlgorithmException, NoSuchProviderException,
		InvalidKeyException, SignatureException {
			final Signature sig = Signature.getInstance(PKCSObjectIdentifiers.sha256WithRSAEncryption.getId(), "BC");
			sig.initSign(this.oldKey);
			sig.update(CmpMessageHelper.getProtectedBytes(msg));
			byte[] eeSignature = sig.sign();
			return new PKIMessage(msg.getHeader(), msg.getBody(), new DERBitString(eeSignature), msg.getExtraCerts());
		}

		private PKIMessage protectPKIMessage(final PKIMessage msg, final boolean badObjectId) throws NoSuchAlgorithmException,
		InvalidKeyException, IOException {
			// SHA1
			final AlgorithmIdentifier owfAlg = new AlgorithmIdentifier(new ASN1ObjectIdentifier("1.3.14.3.2.26"));
			// 567 iterations
			final int iterationCount = 567;
			// HMAC/SHA1
			final AlgorithmIdentifier macAlg = new AlgorithmIdentifier(new ASN1ObjectIdentifier("1.2.840.113549.2.7"));
			final byte[] salt = "foo123".getBytes();
			final DEROctetString derSalt = new DEROctetString(salt);

			// Create the PasswordBased protection of the message
			final PKIHeaderBuilder headBuilder = CmpMessageHelper.getHeaderBuilder(msg.getHeader());
			headBuilder.setSenderKID(new DEROctetString("EMPTY".getBytes()));
			final ASN1Integer iteration = new ASN1Integer(iterationCount);

			// Create the new protected return message
			String objectId = "1.2.840.113533.7.66.13";
			if (badObjectId) {
				objectId += ".7";
			}
			final PBMParameter pp = new PBMParameter(derSalt, owfAlg, iteration, macAlg);
			final AlgorithmIdentifier pAlg = new AlgorithmIdentifier(new ASN1ObjectIdentifier(objectId), pp);
			headBuilder.setProtectionAlg(pAlg);
			final PKIHeader header = headBuilder.build();

			// Calculate the protection bits
			final byte[] raSecret = this.cliArgs.keystorePassword.getBytes();
			byte[] basekey = new byte[raSecret.length + salt.length];
			System.arraycopy(raSecret, 0, basekey, 0, raSecret.length);
			System.arraycopy(salt, 0, basekey, raSecret.length, salt.length);
			// Construct the base key according to rfc4210, section 5.1.3.1
			final MessageDigest dig = MessageDigest.getInstance(owfAlg.getAlgorithm().getId(), this.bcProvider);
			for (int i = 0; i < iterationCount; i++) {
				basekey = dig.digest(basekey);
				dig.reset();
			}
			// For HMAC/SHA1 there is another oid, that is not known in BC, but the result is the same so...
			final String macOid = macAlg.getAlgorithm().getId();
			final byte[] protectedBytes = CmpMessageHelper.getProtectedBytes(header, msg.getBody());
			final Mac mac = Mac.getInstance(macOid, this.bcProvider);
			final SecretKey key = new SecretKeySpec(basekey, macOid);
			mac.init(key);
			mac.reset();
			mac.update(protectedBytes, 0, protectedBytes.length);
			final byte[] out = mac.doFinal();
			final DERBitString bs = new DERBitString(out);

			return new PKIMessage(header, msg.getBody(), bs, msg.getExtraCerts());
		}

		@SuppressWarnings("synthetic-access")
		private byte[] sendCmpHttp(final byte[] message) throws Exception {
			final CMPSendHTTP send = CMPSendHTTP.sendMessage(message, this.cliArgs.hostName, this.cliArgs.port, this.cliArgs.urlPath, false);
			if (send.responseCode != HttpURLConnection.HTTP_OK) {
				this.performanceTest.getLog().error(
						intres.getLocalizedMessage("cmp.responsecodenotok", send.responseCode));
				return null;
			}
			if (send.contentType == null) {
				this.performanceTest.getLog().error("No content type received.");
				return null;
			}
			// Some appserver (Weblogic) responds with "application/pkixcmp; charset=UTF-8"
			if (!send.contentType.startsWith("application/pkixcmp")) {
				this.performanceTest.getLog().info("wrong content type: " + send.contentType);
			}
			return send.response;
		}

		private boolean checkCmpResponseGeneral(final byte[] retMsg, final boolean requireProtection) throws Exception {
			// Parse response message
			final ASN1InputStream ais = new ASN1InputStream(new ByteArrayInputStream(retMsg));
			final PKIMessage respObject = PKIMessage.getInstance(ais.readObject());
			ais.close();
			if (respObject == null) {
				this.performanceTest.getLog().error("No command response message.");
				return false;
			}

			// The signer, i.e. the CA, check it's the right CA
			final PKIHeader header = respObject.getHeader();
			if (header == null) {
				this.performanceTest.getLog().error("No header in response message.");
				return false;
			}
			// Check that the signer is the expected CA
			final X500Name name = X500Name.getInstance(header.getSender().getName());
			if (header.getSender().getTagNo() != 4 || name == null || !name.equals(X500Name.getInstance(this.cacert.getSubjectX500Principal().getEncoded())) ) {
				this.performanceTest.getLog().error("Not signed by right issuer. Tag "+header.getSender().getTagNo()+". Issuer was '"+name+"' but should be '"+this.cacert.getSubjectX500Principal()+"'.");
				return false;
			}

			if (header.getSenderNonce().getOctets().length != 16) {
				this.performanceTest.getLog().error(
						"Wrong length of received sender nonce (made up by server). Is " + header.getSenderNonce().getOctets().length
						+ " byte but should be 16.");
				return false;
			}

			if (!Arrays.equals(header.getRecipNonce().getOctets(), getNonce())) {
				this.performanceTest.getLog().error(
						"recipient nonce not the same as we sent away as the sender nonce. Sent: " + Arrays.toString(getNonce())
						+ " Received: " + Arrays.toString(header.getRecipNonce().getOctets()));
				return false;
			}

			if (!Arrays.equals(header.getTransactionID().getOctets(), getTransId())) {
				this.performanceTest.getLog().error("transid is not the same as the one we sent");
				return false;
			}

			// Check that the message is signed with the correct digest alg
			final AlgorithmIdentifier algId = header.getProtectionAlg();
			if (algId == null || algId.getAlgorithm() == null || algId.getAlgorithm().getId() == null) {
				if (requireProtection) {
					this.performanceTest.getLog().error("Not possible to get algorithm.");
					return false;
				}
				return true;
			}
			final String id = algId.getAlgorithm().getId();
			if (id.equals(PKCSObjectIdentifiers.sha1WithRSAEncryption.getId()) || id.equals(PKCSObjectIdentifiers.sha256WithRSAEncryption.getId()) || id.equals(X9ObjectIdentifiers.ecdsa_with_SHA256.getId())) {
				if (this.firstTime) {
					this.firstTime = false;
					this.isSign = true;
					this.performanceTest.getLog().info("Signature protection used.");
				} else if (!this.isSign) {
					this.performanceTest.getLog().error("Message password protected but should be signature protected.");
					return false;
				}
			} else if (id.equals(CMPObjectIdentifiers.passwordBasedMac.getId())) {
				if (this.firstTime) {
					this.firstTime = false;
					this.isSign = false;
					this.performanceTest.getLog().info("Password (PBE) protection used.");
				} else if (this.isSign) {
					this.performanceTest.getLog().error("Message signature protected but should be password protected.");
					return false;
				}
			} else {
				this.performanceTest.getLog().error(String.format("No valid algorithm: '%s'", id));
				return false;
			}

			if (this.isSign) {
				// Verify the signature
				byte[] protBytes = CmpMessageHelper.getProtectedBytes(respObject);
				final ASN1BitString bs = respObject.getProtection();
				final Signature sig;
				try {
					sig = Signature.getInstance(id);
					sig.initVerify(this.cacert);
					sig.update(protBytes);
					if (!sig.verify(bs.getBytes())) {
						this.performanceTest.getLog().error("CA signature not verifying");
						return false;
					}
				} catch (Exception e) {
					this.performanceTest.getLog().error("Not possible to verify signature.", e);
					return false;
				}
			} else {
				// Verify the PasswordBased protection of the message
				final PBMParameter pp;

				final AlgorithmIdentifier pAlg = header.getProtectionAlg();
				pp = PBMParameter.getInstance(pAlg.getParameters());

				final int iterationCount = pp.getIterationCount().getPositiveValue().intValue();
				final AlgorithmIdentifier owfAlg = pp.getOwf();
				// Normal OWF alg is 1.3.14.3.2.26 - SHA1
				final AlgorithmIdentifier macAlg = pp.getMac();
				// Normal mac alg is 1.3.6.1.5.5.8.1.2 - HMAC/SHA1
				final byte[] salt = pp.getSalt().getOctets();
				final byte[] raSecret = new String("password").getBytes();
				// HMAC/SHA1 os normal 1.3.6.1.5.5.8.1.2 or 1.2.840.113549.2.7 
				final String macOid = macAlg.getAlgorithm().getId();
				final SecretKey key;

				byte[] basekey = new byte[raSecret.length + salt.length];
				System.arraycopy(raSecret, 0, basekey, 0, raSecret.length);
				System.arraycopy(salt, 0, basekey, raSecret.length, salt.length);
				// Construct the base key according to rfc4210, section 5.1.3.1
				final MessageDigest dig = MessageDigest.getInstance(owfAlg.getAlgorithm().getId(), this.bcProvider);
				for (int i = 0; i < iterationCount; i++) {
					basekey = dig.digest(basekey);
					dig.reset();
				}
				key = new SecretKeySpec(basekey, macOid);

				final Mac mac = Mac.getInstance(macOid, this.bcProvider);
				mac.init(key);
				mac.reset();
				final byte[] protectedBytes = CmpMessageHelper.getProtectedBytes(respObject);
				final ASN1BitString protection = respObject.getProtection();
				mac.update(protectedBytes, 0, protectedBytes.length);
				byte[] out = mac.doFinal();
				// My out should now be the same as the protection bits
				byte[] pb = protection.getBytes();
				if (!Arrays.equals(out, pb)) {
					this.performanceTest.getLog().error("Wrong PBE hash");
					return false;
				}
			}
			return true;
		}

		private X509Certificate checkCmpCertRepMessage(final byte[] retMsg) throws IOException,
		CertificateException {
			// Parse response message
			final ASN1InputStream ais = new ASN1InputStream(new ByteArrayInputStream(retMsg));
			final PKIMessage respObject = PKIMessage.getInstance(ais.readObject());
			ais.close();
			if (respObject == null) {
				this.performanceTest.getLog().error("No PKIMessage for certificate received.");
				return null;
			}
			final PKIBody body = respObject.getBody();
			if (body == null) {
				this.performanceTest.getLog().error("No PKIBody for certificate received.");
				return null;
			}
			if (body.getType() != 8) {
				this.performanceTest.getLog().error("Cert body tag not 8.");
				return null;
			}
			final CertRepMessage c = (CertRepMessage) body.getContent();
			if (c == null) {
				this.performanceTest.getLog().error("No CertRepMessage for certificate received.");
				return null;
			}
			final CertResponse resp = c.getResponse()[0];
			if (resp == null) {
				this.performanceTest.getLog().error("No CertResponse for certificate received.");
				return null;
			}
			if (resp.getCertReqId().getValue().intValue() != getReqId()) {
				this.performanceTest.getLog().error(
						"Received CertReqId is " + resp.getCertReqId().getValue().intValue() + " but should be " + getReqId());
				return null;
			}
			final PKIStatusInfo info = resp.getStatus();
			if (info == null) {
				this.performanceTest.getLog().error("No PKIStatusInfo for certificate received.");
				return null;
			}
			if (info.getStatus().intValue() != 0) {
				this.performanceTest.getLog().error("Received Status is " + info.getStatus().intValue() + " but should be 0");
				return null;
			}
			final CertifiedKeyPair kp = resp.getCertifiedKeyPair();
			if (kp == null) {
				this.performanceTest.getLog().error("No CertifiedKeyPair for certificate received.");
				return null;
			}
			final CertOrEncCert cc = kp.getCertOrEncCert();
			if (cc == null) {
				this.performanceTest.getLog().error("No CertOrEncCert for certificate received.");
				return null;
			}
			final CMPCertificate cmpcert = cc.getCertificate();
			if (cmpcert == null) {
				this.performanceTest.getLog().error("No X509CertificateStructure for certificate received.");
				return null;
			}
			final byte[] encoded = cmpcert.getEncoded();
			if (encoded == null || encoded.length <= 0) {
				this.performanceTest.getLog().error("No encoded certificate received.");
				return null;
			}
			final X509Certificate cert = (X509Certificate) this.certificateFactory.generateCertificate(new ByteArrayInputStream(encoded));
			if (cert == null) {
				this.performanceTest.getLog().error("Not possbile to create certificate.");
				return null;
			}
			{
				final X500Principal newCertDN = cert.getIssuerX500Principal();
				final X500Principal oldCertDN = this.extraCert.getIssuerX500Principal();
				// Remove this test to be able to test unid-fnr
				if ( !oldCertDN.equals(newCertDN) ) {
					this.performanceTest.getLog().error(
							"Subject is '" + newCertDN + "' but should be '" + oldCertDN + '\'');
					return null;
				}
			}
			if (cert.getIssuerX500Principal().hashCode() != this.cacert.getSubjectX500Principal().hashCode()) {
				this.performanceTest.getLog().error(
						"Issuer is '" + cert.getIssuerDN() + "' but should be '" + this.cacert.getSubjectDN() + '\'');
				return null;
			}
			try {
				cert.verify(this.cacert.getPublicKey());
			} catch (Exception e) {
				this.performanceTest.getLog().error("Certificate not verifying. See exception", e);
				return null;
			}
			return cert;
		}

		private boolean checkCmpPKIConfirmMessage(final byte[] retMsg) throws IOException, CertificateEncodingException {
			// Parse response message
			final ASN1InputStream ais = new ASN1InputStream(new ByteArrayInputStream(retMsg));
			final PKIMessage respObject = PKIMessage.getInstance(ais.readObject());
			ais.close();
			if (respObject == null) {
				this.performanceTest.getLog().error("Not possbile to get response message.");
				return false;
			}
			final PKIHeader header = respObject.getHeader();
			if (header.getSender().getTagNo() != 4) {
				this.performanceTest.getLog().error(
						"Wrong tag in response message header. Is " + header.getSender().getTagNo() + " should be 4.");
				return false;
			}
			{
				final X500Name senderName;
				{
					final ASN1Encodable encodeAble = header.getSender().getName();
					if ( ! (encodeAble instanceof X500Name) ) {
						this.performanceTest.getLog().error("Sender in header is not a "+X500Name.class.getName()+" it is a " + encodeAble.getClass().getName());
						return false;
					}
					senderName = (X500Name)encodeAble;
				}
				final X500Name cacertName = new X509CertificateHolder(this.cacert.getEncoded()).getSubject();
				if ( !Arrays.equals(senderName.getEncoded(), cacertName.getEncoded()) ) {
					this.performanceTest.getLog().error("Wrong sender DN. Is  '" + senderName + "' should be '" + cacertName + "' (CA certificate).");
					return false;
				}
			}
			{
				final X500Name recipientName;
				{
					final ASN1Encodable encodeAble = header.getRecipient().getName();
					if ( ! (encodeAble instanceof X500Name) ) {
						this.performanceTest.getLog().error("Recipient in header is not a "+X500Name.class.getName()+" it is a " + encodeAble.getClass().getName());
						return false;
					}
					recipientName = (X500Name)encodeAble;
				}
				final X500Name extraCertName = new X509CertificateHolder(this.extraCert.getEncoded()).getSubject();
				if ( !Arrays.equals(recipientName.getEncoded(), extraCertName.getEncoded()) ) {
					this.performanceTest.getLog().error("Wrong recipient DN. Is '" + recipientName + "' should be '" + extraCertName + "'.");
					return false;
				}
			}
			final PKIBody body = respObject.getBody();
			if (body == null) {
				this.performanceTest.getLog().error("No PKIBody for response received.");
				return false;
			}
			if (body.getType() != 19) {
				this.performanceTest.getLog().error("Cert body tag not 19. It was " + body.getType());

				final PKIStatusInfo err = (PKIStatusInfo) body.getContent();
				this.performanceTest.getLog().error(err.getStatusString().getStringAtUTF8(0).getString());

				return false;
			}
			final PKIConfirmContent n = (PKIConfirmContent) body.getContent();
			if (n == null) {
				this.performanceTest.getLog().error("Confirmation is null.");
				return false;
			}
			if(!n.toASN1Primitive().equals(DERNull.INSTANCE)) {
				this.performanceTest.getLog().error("Confirmation is not DERNull.");
				return false;
			}

			return true;
		}

		private PKIMessage genCertConfirm(final String hash) throws IOException {
			final PKIHeaderBuilder myPKIHeader = getPKIHeaderBuilder();
			myPKIHeader.setMessageTime(new DERGeneralizedTime(new Date()));
			// senderNonce
			myPKIHeader.setSenderNonce(new DEROctetString(getNonce()));
			// TransactionId
			myPKIHeader.setTransactionID(new DEROctetString(getTransId()));
			final PKIHeader header = myPKIHeader.build();

			final CertStatus cs = new CertStatus(hash.getBytes(), new BigInteger(Integer.toString(getReqId())));

			final ASN1EncodableVector v = new ASN1EncodableVector();
			v.add(cs);
			final CertConfirmContent cc = CertConfirmContent.getInstance(new DERSequence(v));

			final PKIBody myPKIBody = new PKIBody(24, cc); // Cert Confirm
			return new PKIMessage(header, myPKIBody);
		}
		private final byte[] nonce = new byte[16];
		private final byte[] transid = new byte[16];
		private int reqId;
		private String newcertfp;

		void newSession() {
			this.performanceTest.getRandom().nextBytes(this.nonce);
			this.performanceTest.getRandom().nextBytes(this.transid);
		}

		int getReqId() {
			return this.reqId;
		}

		void setReqId(int i) {
			this.reqId = i;
		}

		void setFP(String fp) {
			this.newcertfp = fp;
		}

		String getFP() {
			return this.newcertfp;
		}

		byte[] getTransId() {
			return this.transid;
		}

		byte[] getNonce() {
			return this.nonce;
		}

	}
	private static class GetCertificate implements Command {
		private final SessionData sessionData;

		private GetCertificate(final SessionData sd) {
			this.sessionData = sd;
		}
		@SuppressWarnings("synthetic-access")
		@Override
		public boolean doIt() throws Exception {
			this.sessionData.newSession();

			final CertRequest keyUpdateReq = this.sessionData.genKeyUpdateReq();
			final AlgorithmIdentifier pAlg = new AlgorithmIdentifier(PKCSObjectIdentifiers.sha256WithRSAEncryption);
			final PKIMessage certMsg = this.sessionData.genPKIMessage(false, keyUpdateReq, pAlg, null);
			if (certMsg == null) {
				this.sessionData.performanceTest.getLog().error("No certificate request.");
				return false;
			}

			final PKIMessage signedMsg = this.sessionData.addExtraCert(this.sessionData.signPKIMessage(certMsg));
			if (signedMsg == null) {
				this.sessionData.performanceTest.getLog().error("No protected message.");
				return false;
			}

			final CertReqMessages kur = (CertReqMessages) signedMsg.getBody().getContent();
			this.sessionData.setReqId(kur.toCertReqMsgArray()[0].getCertReq().getCertReqId().getValue().intValue());
			final ByteArrayOutputStream bao = new ByteArrayOutputStream();
			final ASN1OutputStream out = ASN1OutputStream.create(bao, ASN1Encoding.DER);
			out.writeObject(signedMsg);
			out.close();
			final byte[] ba = bao.toByteArray();
			// Send request and receive response
			final byte[] resp = this.sessionData.sendCmpHttp(ba);
			if (resp == null || resp.length <= 0) {
				this.sessionData.performanceTest.getLog().error("No response message.");
				return false;
			}
			if (!this.sessionData.checkCmpResponseGeneral(resp, true)) {
				return false;
			}
			final X509Certificate cert = this.sessionData.checkCmpCertRepMessage( resp );
			if (cert == null) {
				return false;
			}
			final String fp = CertTools.getFingerprintAsString(cert);
			this.sessionData.setFP(fp);
			final BigInteger serialNumber = CertTools.getSerialNumber(cert);
			if (this.sessionData.cliArgs.resultFilePrefix != null) {
				final OutputStream os = new FileOutputStream(this.sessionData.cliArgs.resultFilePrefix + serialNumber + ".dat");
				os.write(cert.getEncoded());
				os.close();
			}
			this.sessionData.performanceTest.getLog().result(serialNumber);

			return true;
		}
		@Override
		public String getJobTimeDescription() {
			return "Get certificate";
		}

	}

	private static class SendConfirmMessageToCA implements Command {
		private final SessionData sessionData;

		private SendConfirmMessageToCA(final SessionData sd) {
			this.sessionData = sd;
		}

		@SuppressWarnings("synthetic-access")
		@Override
		public boolean doIt() throws Exception {
			final String hash = this.sessionData.getFP(); //"foo123";
			final PKIMessage con = this.sessionData.genCertConfirm(hash);
			if (con == null) {
				this.sessionData.performanceTest.getLog().error("Not possible to generate PKIMessage.");
				return false;
			}
			final PKIMessage confirm = this.sessionData.protectPKIMessage(con, false);
			final ByteArrayOutputStream bao = new ByteArrayOutputStream();
			final ASN1OutputStream out = ASN1OutputStream.create(bao, ASN1Encoding.DER);
			out.writeObject(confirm);
			out.close();
			final byte[] ba = bao.toByteArray();
			// Send request and receive response
			final byte[] resp = this.sessionData.sendCmpHttp(ba);
			if (resp == null || resp.length <= 0) {
				this.sessionData.performanceTest.getLog().error("No response message.");
				return false;
			}
			if (!this.sessionData.checkCmpResponseGeneral(resp, false)) {
				return false;
			}
			return this.sessionData.checkCmpPKIConfirmMessage(resp);
		}

		@Override
		public String getJobTimeDescription() {
			return "Send confirmation to CA";
		}
	}

	private static class CLIArgs {
		final String hostName;
		final String keystoreFile;
		final String keystorePassword;
		final int numberOfThreads;
		final int numberOfTests;
		final int waitTime;
		final int port;
		//	  final boolean isHttp;
		final String urlPath;
		final String resultFilePrefix;

		CLIArgs( final String[] args) throws Exception {
			if (args.length < 5) {
				System.out.println(
						args[0] +
						" <host name> <keystore (p12) directory> <keystore password> <alias> [<'m:n' m # of threads, n # of tests>] [<wait time (ms) between each thread is started>] [<port>] [<URL path of servlet. use 'null' to get EJBCA (not proxy) default>] [<certificate file prefix. set this if you want all received certificates stored on files>]"
						);
				System.out.println();
				System.out.println("Requirements for the 'CMP Alias':");
				System.out.println("\t'Operational Mode' must be 'Client Mode'.");
				System.out.println("\t'Automatic Key Update' must be 'Allow'.");
				System.out.println();
				System.out.println("Ejbca expects an end entity with a generated certificate for each thread in the test. ");
				System.out.println("The certificate and private key of each end entity are stored in a keystore file receding in the directory given by the command line.");
				System.out.println();
				System.out.println("A keystore can be obtained, for example, by specifying the token to be 'P12' when creating the end entity and then download the keystore by choosing 'create keystore' from the public web");
				System.exit(-1);
			}
			this.hostName = args[1];
			this.keystoreFile = args[2];
			this.keystorePassword = args[3];
			final String alias = args[4];
			final NrOfThreadsAndNrOfTests notanot = new NrOfThreadsAndNrOfTests(args.length>5 ? args[5] : null);
			this.numberOfThreads = notanot.getThreads();
			this.numberOfTests = notanot.getTests();
			this.waitTime = args.length > 6 ? Integer.parseInt(args[6].trim()) : 0;
			this.port = args.length > 7 ? Integer.parseInt(args[7].trim()) : 8080;
			this.urlPath = (args.length > 8 && args[8].toLowerCase().indexOf("null") < 0 ? args[8].trim() : "/ejbca/publicweb/cmp") + '/' + alias;
			this.resultFilePrefix = args.length > 9 ? args[9].trim() : null;
		}
	}

	private static class MyCommandFactory implements CommandFactory {
		final private PerformanceTest performanceTest;
		final private CLIArgs cliArgs;
		final private KeyStore[] keyStores;
		static private int keyStoreIx = 0;
		MyCommandFactory( final CLIArgs clia, final PerformanceTest performanceTest, final KeyStore[] keyStores) {
			this.performanceTest = performanceTest;
			this.cliArgs = clia;
			this.keyStores = keyStores;
		}
		@SuppressWarnings("synthetic-access")
		@Override
		public Command[] getCommands() {		
			final SessionData sessionData;
			try {
				sessionData = new SessionData(this.cliArgs, this.performanceTest, this.keyStores[keyStoreIx++]);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
				return null;
			}
			return new Command[] { new GetCertificate(sessionData), new SendConfirmMessageToCA(sessionData) };//, new Revoke(sessionData)};
		}
	}

	private static KeyStore getKeyStore( final File file, final String keystorePassword ) {
		FileInputStream is = null;
		try {
			is = new FileInputStream(file);
			final KeyStore keyStore = KeyStore.getInstance("PKCS12");
			keyStore.load(is, keystorePassword.toCharArray());
			return keyStore;
		} catch( final Throwable t ) {
			return null;
		} finally {
			if ( is!=null ) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static KeyStore[] getKeyStores( final String fileDirectory, final String keystorePassword ) throws Exception {
		final File dir = new File(fileDirectory);
		if (dir.listFiles() == null) {
			System.out.println(fileDirectory+" does not exist or is not a directory.");
			System.exit(-1);
		}
		final List<KeyStore> keyStores = new LinkedList<KeyStore>();
		for ( final File file : dir.listFiles() ) {
			final KeyStore keyStore = getKeyStore(file, keystorePassword);
			if ( keyStore!=null ) {
				keyStores.add(keyStore);
			}
		}
		return keyStores.toArray(new KeyStore[0]);
	}

	@Override
	protected void execute(String[] args) {
		CryptoProviderTools.installBCProviderIfNotAvailable();
		try {
			final CLIArgs cliArgs = new CLIArgs(args);
			final PerformanceTest performanceTest = new PerformanceTest();
			final KeyStore[] keyStores = getKeyStores(cliArgs.keystoreFile, cliArgs.keystorePassword);
			if ( cliArgs.numberOfThreads>keyStores.length ) {
				System.out.println("There are only "+keyStores.length+" key store files but "+cliArgs.numberOfThreads+" threads was specified.");
				System.exit(-1);
			}
			performanceTest.execute(new MyCommandFactory(cliArgs, performanceTest, keyStores), cliArgs.numberOfThreads, cliArgs.numberOfTests, cliArgs.waitTime, System.out);
		} catch ( SecurityException e ) {
			throw e; // this exception was thrown by the clientToolBoxTest at exit. let it be handled by the testing framework.
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	protected String getName() {
		return "CMPKeyUpdateStressTest";
	}

}
