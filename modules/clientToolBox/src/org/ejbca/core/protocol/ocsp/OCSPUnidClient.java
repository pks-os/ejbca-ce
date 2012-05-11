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

package org.ejbca.core.protocol.ocsp;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.ocsp.OCSPObjectIdentifiers;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.ocsp.BasicOCSPResp;
import org.bouncycastle.cert.ocsp.CertificateID;
import org.bouncycastle.cert.ocsp.OCSPException;
import org.bouncycastle.cert.ocsp.OCSPReq;
import org.bouncycastle.cert.ocsp.OCSPReqBuilder;
import org.bouncycastle.cert.ocsp.OCSPResp;
import org.bouncycastle.cert.ocsp.RespID;
import org.bouncycastle.cert.ocsp.jcajce.JcaCertificateID;
import org.bouncycastle.cert.ocsp.jcajce.JcaRespID;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.cesecore.certificates.ocsp.cache.SHA1DigestCalculator;
import org.cesecore.keys.util.KeyTools;
import org.cesecore.util.Base64;
import org.cesecore.util.CertTools;
import org.cesecore.util.CryptoProviderTools;

/** A simple OCSP lookup client used to query the OCSPUnidExtension. Attributes needed to call the client is a keystore
 * issued from the same CA as has issued the TLS server certificate of the OCSP/Lookup server.
 * The keystore must be a PKCS#12 file.
 * If a keystore is not used, regular OCSP requests can still be made, using normal http.
 * 
 * If requesting an Fnr and the fnr rturned is null, even though the OCSP code is good there can be several reasons:
 * 1.The client was not authorized to request an Fnr
 * 2.There was no Unid Fnr mapping available
 * 3.There was no Unid in the certificate (serialNumber DN component)
 *
 * @version $Id$
 *
 */
public class OCSPUnidClient {

    final public static String requestDirectory = "ocspRequests";
    final private String httpReqPath;
    final private KeyStore ks;
    final private String passphrase;
    final private PrivateKey signKey;
    final private X509Certificate[] certChain;
    final private Extensions extensions;
    final private byte nonce[];
    private static final Logger m_log = Logger.getLogger(OCSPUnidClient.class);
	
	/**
	 * @param keystore KeyStore client keystore used to authenticate TLS client authentication, or null if TLS is not used
	 * @param pwd String password for the key store, or null if no keystore is used
	 * @param ocspurl String url to the OCSP server, or null if we should try to use the AIA extension from the cert; e.g. http://127.0.0.1:8080/ejbca/publicweb/status/ocsp (or https for TLS)
	 * @param certs certificate chain to signing key
	 * @param _signKey signing key
	 * @param getfnr true if FNR should be fetched
	 * @throws NoSuchAlgorithmException
	 * @throws IOException if ASN1 parsing error occurs
	 */
	private OCSPUnidClient(KeyStore keystore, String pwd, String ocspurl, Certificate[] certs, PrivateKey _signKey, boolean getfnr) throws NoSuchAlgorithmException, IOException {
	    this.httpReqPath = ocspurl;
	    this.passphrase = pwd;
	    this.ks = keystore;
	    this.signKey = _signKey;
	    this.certChain = certs!=null ? Arrays.asList(certs).toArray(new X509Certificate[0]) : null;
        this.nonce = new byte[16];
	    {
	        List<Extension> extensionList = new ArrayList<Extension>();
	        final Random randomSource = new Random();
            randomSource.nextBytes(nonce);
	       extensionList.add(new Extension(OCSPObjectIdentifiers.id_pkix_ocsp_nonce, false, new DEROctetString(nonce)));
	        // Don't bother adding Unid extension if we are not using client authentication
	        if ( getfnr ) {
	            extensionList.add(new Extension(FnrFromUnidExtension.FnrFromUnidOid, false, new DEROctetString(new FnrFromUnidExtension("1"))));
	        }
	        extensions = new Extensions(extensionList.toArray(new Extension[extensionList.size()]));    
	    }
	    CryptoProviderTools.installBCProviderIfNotAvailable();
	}
	
	/**
     * @param ksfilename String Filename of PKCS#12 keystore used to authenticate TLS client authentication, or null if TLS is not used
     * @param pwd String password for the key store,or null if no keystore is used 
     * @param ocspurl String url to the OCSP server, or null if we should try to use the AIA extension from the cert; e.g. http://127.0.0.1:8080/ejbca/publicweb/status/ocsp (or https for TLS)
	 * @return the client to use
     * @throws Exception
	 */
	public static OCSPUnidClient getOCSPUnidClient(String ksfilename, String pwd, String ocspurl, boolean doSignRequst, boolean getfnr) throws Exception {
	    if ( doSignRequst && ksfilename==null ) {
            throw new Exception("You got to give the path name for a keystore to use when using signing.");
	    }
        final KeyStore ks;
        if (ksfilename != null) {
	        ks = KeyStore.getInstance("PKCS12", "BC");
	        ks.load(new FileInputStream(ksfilename), pwd.toCharArray());			
            Enumeration<String> en = ks.aliases();
            String alias = null;
            // If this alias is a trusted certificate entry, we don't want to fetch that, we want the key entry
            while ( (alias==null || ks.isCertificateEntry(alias)) && en.hasMoreElements() ) {
                alias = en.nextElement();
            }
            final Certificate[] certs = KeyTools.getCertChain(ks, alias);
            if (certs == null) {
                throw new IOException("Can not find a certificate entry in PKCS12 keystore for alias "+alias);
            }
            final PrivateKey privateKey = doSignRequst ? (PrivateKey)ks.getKey(alias, null) : null;
            return new OCSPUnidClient(ks, pwd, ocspurl, certs, privateKey, getfnr);
		} else {
            return new OCSPUnidClient(null, null, ocspurl, null, null, getfnr);
		}
	}
	/**
	 * @param cert X509Certificate to query, the DN should contain serialNumber which is Unid to be looked up
	 * @param cacert CA certificate that issued the certificate to be queried
     * @param useGet if true GET will be used instead of POST as HTTP method
	 * @return OCSPUnidResponse conatining the response and the fnr, can contain and an error code and the fnr can be null, never returns null.
	 * @throws OCSPException
	 * @throws IOException
	 * @throws GeneralSecurityException
	 * @throws OperatorCreationException 
	 * @throws IllegalArgumentException 
	 */
	public OCSPUnidResponse lookup(Certificate cert, Certificate cacert, boolean useGet) throws OCSPException, IOException, GeneralSecurityException, IllegalArgumentException, OperatorCreationException {
        return lookup( CertTools.getSerialNumber(cert), cacert, useGet);
    }
    /**
     * @param serialNr serial number of the certificate to verify
     * @param cacert issuer of the certificate to verify
     * @param useGet if true GET will be used instead of POST as HTTP method
     * @return response can contain and an error code but the fnr is allways null, never returns null.
     * @throws OCSPException 
     * @throws IllegalArgumentException 
     * @throws IOException
     * @throws GeneralSecurityException
     * @throws OperatorCreationException if Signer couldn't be created
     */
    public OCSPUnidResponse lookup(BigInteger serialNr, Certificate cacert, boolean useGet) throws OCSPException, IOException,
            IllegalArgumentException, OperatorCreationException, GeneralSecurityException {
        if (this.httpReqPath == null) {
            // If we didn't pass a url to the constructor and the cert does not have the URL, we will fail...
            OCSPUnidResponse ret = new OCSPUnidResponse();
            ret.setErrorCode(OCSPUnidResponse.ERROR_NO_OCSP_URI);
            return ret;
        }
        final OCSPReqBuilder gen = new OCSPReqBuilder();
        final CertificateID certId = new JcaCertificateID(SHA1DigestCalculator.buildSha1Instance(), (X509Certificate)cacert, serialNr);
//        System.out.println("Generating CertificateId:\n"
//                + " Hash algorithm : '" + certId.getHashAlgOID() + "'\n"
//                + " CA certificate\n"
//                + "      CA SubjectDN: '" + cacert.getSubjectDN().getName() + "'\n"
//                + "      SerialNumber: '" + cacert.getSerialNumber().toString(16) + "'\n"
//                + " CA certificate hashes\n"
//                + "      Name hash : '" + new String(Hex.encode(certId.getIssuerNameHash())) + "'\n"
//                + "      Key hash  : '" + new String(Hex.encode(certId.getIssuerKeyHash())) + "'\n");
        gen.addRequest(certId);
        if (!useGet) {
            // Add a nonce to the request
            gen.setRequestExtensions(this.extensions);        	
        }
        final OCSPReq req;
        if ( this.signKey!=null ) {
            final X509Certificate localCertChain[] = this.certChain!=null ? this.certChain : new X509Certificate[] {(X509Certificate)cacert};
            final JcaX509CertificateHolder[] certificateHolderChain = OCSPUtil.convertCertificateChainToCertificateHolderChain(localCertChain);
            gen.setRequestorName(certificateHolderChain[0].getSubject());
            req = gen.build(new JcaContentSignerBuilder("SHA1withRSA").build(this.signKey), certificateHolderChain);
        } else {
            req = gen.build();
        }
        // write request if directory exists.
        File  ocspReqDir = new File(requestDirectory);
        if ( ocspReqDir.isDirectory() ) {
            OutputStream os = new FileOutputStream(new File( ocspReqDir, serialNr.toString()));
            os.write(req.getEncoded());
            os.close();
        }
        // Send the request and receive a BasicResponse
        return sendOCSPRequest(req.getEncoded(), cacert, useGet);
	}

    //
    // Private helper methods
    //
    
    private OCSPUnidResponse sendOCSPRequest(byte[] ocspPackage, Certificate cacert, boolean useGet) throws IOException, OCSPException, GeneralSecurityException, OperatorCreationException {
    	final HttpURLConnection con;
    	if (useGet) {
        	String b64 = new String(Base64.encode(ocspPackage, false));
        	//String urls = URLEncoder.encode(b64, "UTF-8");
        	//URL url = new URL(httpReqPath + '/' + urls);
        	URL url = new URL(httpReqPath + '/' + b64);
            con = (HttpURLConnection)url.openConnection();
    	} else {
            // POST the OCSP request
            URL url = new URL(httpReqPath);
            con = (HttpURLConnection)getUrlConnection(url);
            // we are going to do a POST
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            // POST it
            con.setRequestProperty("Content-Type", "application/ocsp-request");
            OutputStream os = null;
            try {
                os = con.getOutputStream();
                os.write(ocspPackage);
            } finally {
                if (os != null) {
                	os.close();
                }
            }
    	}
        final OCSPUnidResponse ret = new OCSPUnidResponse();
        ret.setHttpReturnCode(con.getResponseCode());
        if (ret.getHttpReturnCode() != 200) {
        	if (ret.getHttpReturnCode() == 401) {
        		ret.setErrorCode(OCSPUnidResponse.ERROR_UNAUTHORIZED);
        	} else {
        		ret.setErrorCode(OCSPUnidResponse.ERROR_UNKNOWN);
        	}
        	return ret;
        }
        final OCSPResp response; {
            final InputStream in = con.getInputStream();
            if ( in!=null ) {
                try {
                    response = new OCSPResp(IOUtils.toByteArray(in));
                } finally {
                    in.close();
                }
            } else {
                response = null;
            }
        }
        if (response == null) {
        	ret.setErrorCode(OCSPUnidResponse.ERROR_NO_RESPONSE);
        	return ret;
        }
        ret.setResp(response);
        final BasicOCSPResp brep = (BasicOCSPResp) response.getResponseObject();
        if ( brep==null ) {
            ret.setErrorCode(OCSPUnidResponse.ERROR_NO_RESPONSE);
            return ret;
        }
        // Compare nonces to see if the server sent the same nonce as we sent
    	final byte[] noncerep = brep.getExtension(OCSPObjectIdentifiers.id_pkix_ocsp_nonce).getExtnValue().getEncoded();
    	if (noncerep != null) {
        	ASN1InputStream ain = new ASN1InputStream(noncerep);
        	ASN1OctetString oct = ASN1OctetString.getInstance(ain.readObject());
        	boolean eq = ArrayUtils.isEquals(this.nonce, oct.getOctets());    		
            if (!eq) {
            	ret.setErrorCode(OCSPUnidResponse.ERROR_INVALID_NONCE);
            	return ret;
            }
    	}

		final RespID id = brep.getResponderId();
		final DERTaggedObject to = (DERTaggedObject)id.toASN1Object().toASN1Object();
		final RespID respId;
        final X509CertificateHolder[] chain = brep.getCerts();
        JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
        X509Certificate signerCertificate = converter.getCertificate(chain[0]);
        final PublicKey signerPub = signerCertificate.getPublicKey();
		if (to.getTagNo() == 1) {
			// This is Name
			respId = new JcaRespID(signerCertificate.getSubjectX500Principal());
		} else {
			// This is KeyHash
			respId = new JcaRespID(signerPub, SHA1DigestCalculator.buildSha1Instance());
		}
		if (!id.equals(respId)) {
			// Response responderId does not match signer certificate responderId!
			ret.setErrorCode(OCSPUnidResponse.ERROR_INVALID_SIGNERID);
		}
        if (!brep.isSignatureValid(new JcaContentVerifierProviderBuilder().build(signerPub))) {
        	ret.setErrorCode(OCSPUnidResponse.ERROR_INVALID_SIGNATURE);
        	return ret;
        }
        // Verify the certificate chain.
        for (int i=0; i<chain.length; i++) {
            final X509Certificate cert1 = converter.getCertificate(chain[i]);
            final X509Certificate cert2 = converter.getCertificate(chain[Math.min(i+1, chain.length-1)]);
            try {
                cert1.verify(cert2.getPublicKey());          
            } catch (GeneralSecurityException e) {
                m_log.debug("verifying problem with", e);
                m_log.debug("cert to be verified: "+cert1);
                m_log.debug("verifying cert: "+cert2);
                ret.setErrorCode(OCSPUnidResponse.ERROR_INVALID_SIGNERCERT);
                return ret;         
            }
        }
        // the CA could either have signed the respons directly or else it might have been signed a special ocsp responder signing key with a certificate signed by the CA.
        if ( !chain[0].equals(cacert) && !chain[1].equals(cacert) ) {
            ret.setErrorCode(OCSPUnidResponse.ERROR_INVALID_SIGNERCERT);
            return ret;         
        }
        String fnr = getFnr(brep);
        if (fnr != null) {
        	ret.setFnr(fnr);
        }
        return ret;
    }

    private String getFnr(BasicOCSPResp brep) throws IOException {
        byte[] fnrrep = brep.getExtension(FnrFromUnidExtension.FnrFromUnidOid).getExtnValue().getEncoded();
        if (fnrrep == null) {
            return null;            
        }
        ASN1InputStream aIn = new ASN1InputStream(new ByteArrayInputStream(fnrrep));
        ASN1OctetString octs = (ASN1OctetString) aIn.readObject();
        aIn = new ASN1InputStream(new ByteArrayInputStream(octs.getOctets()));
        FnrFromUnidExtension fnrobj = FnrFromUnidExtension.getInstance(aIn.readObject());
        return fnrobj.getFnr();
    }

    private SSLSocketFactory getSSLFactory() throws GeneralSecurityException, IOException {

        final KeyManager km[];
        final TrustManager tm[];

        // Put the key and certs in the user keystore (if available)
        if (this.ks != null) {
            final KeyManagerFactory kmf;
            kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(this.ks, this.passphrase.toCharArray());
            km = kmf.getKeyManagers();
        } else {
            km= null;
        }
        // Now make a truststore to verify the server
        if ( this.certChain!=null && this.certChain.length>0) {
            final KeyStore trustks = KeyStore.getInstance("jks");
            trustks.load(null, "foo123".toCharArray());
            // add trusted CA cert
            trustks.setCertificateEntry("trusted", this.certChain[this.certChain.length-1]);
            final TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(trustks);
            tm = tmf.getTrustManagers();
        } else {
            tm = null;
        }
        if ( km==null && tm==null ) {
            return (SSLSocketFactory)SSLSocketFactory.getDefault();
        }
        final SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(km, tm, null);

        return ctx.getSocketFactory();
    }

    /**
     * 
     * @param url
     * @return URLConnection
     * @throws IOException
     * @throws GeneralSecurityException
     */
    private URLConnection getUrlConnection(URL url) throws IOException, GeneralSecurityException {
        final URLConnection orgcon = url.openConnection();
        if (orgcon instanceof HttpsURLConnection) {
            HttpsURLConnection con = (HttpsURLConnection) orgcon;
            con.setHostnameVerifier(new SimpleVerifier());
            con.setSSLSocketFactory(getSSLFactory());
        } 
        return orgcon;
    }

    class SimpleVerifier implements HostnameVerifier {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }
	
}
