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

package org.ejbca.core.model.approval;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.x500.X500Principal;

import junit.framework.TestCase;

import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authentication.tokens.X509CertificateAuthenticationToken;
import org.cesecore.util.Base64;
import org.cesecore.util.CertTools;
import org.cesecore.util.CryptoProviderTools;
import org.ejbca.core.model.approval.approvalrequests.DummyApprovalRequest;

/**
 * @version $Id$
 */
public class ApprovalRequestTest extends TestCase {

    private static byte[] testcertenc = Base64.decode(("MIIDATCCAmqgAwIBAgIIczEoghAwc3EwDQYJKoZIhvcNAQEFBQAwLzEPMA0GA1UE"
            + "AxMGVGVzdENBMQ8wDQYDVQQKEwZBbmFUb20xCzAJBgNVBAYTAlNFMB4XDTAzMDky"
            + "NDA2NDgwNFoXDTA1MDkyMzA2NTgwNFowMzEQMA4GA1UEAxMHcDEydGVzdDESMBAG"
            + "A1UEChMJUHJpbWVUZXN0MQswCQYDVQQGEwJTRTCBnTANBgkqhkiG9w0BAQEFAAOB"
            + "iwAwgYcCgYEAnPAtfpU63/0h6InBmesN8FYS47hMvq/sliSBOMU0VqzlNNXuhD8a"
            + "3FypGfnPXvjJP5YX9ORu1xAfTNao2sSHLtrkNJQBv6jCRIMYbjjo84UFab2qhhaJ"
            + "wqJgkQNKu2LHy5gFUztxD8JIuFPoayp1n9JL/gqFDv6k81UnDGmHeFcCARGjggEi"
            + "MIIBHjAPBgNVHRMBAf8EBTADAQEAMA8GA1UdDwEB/wQFAwMHoAAwOwYDVR0lBDQw"
            + "MgYIKwYBBQUHAwEGCCsGAQUFBwMCBggrBgEFBQcDBAYIKwYBBQUHAwUGCCsGAQUF"
            + "BwMHMB0GA1UdDgQWBBTnT1aQ9I0Ud4OEfNJkSOgJSrsIoDAfBgNVHSMEGDAWgBRj"
            + "e/R2qFQkjqV0pXdEpvReD1eSUTAiBgNVHREEGzAZoBcGCisGAQQBgjcUAgOgCQwH"
            + "Zm9vQGZvbzASBgNVHSAECzAJMAcGBSkBAQEBMEUGA1UdHwQ+MDwwOqA4oDaGNGh0"
            + "dHA6Ly8xMjcuMC4wLjE6ODA4MC9lamJjYS93ZWJkaXN0L2NlcnRkaXN0P2NtZD1j"
            + "cmwwDQYJKoZIhvcNAQEFBQADgYEAU4CCcLoSUDGXJAOO9hGhvxQiwjGD2rVKCLR4"
            + "emox1mlQ5rgO9sSel6jHkwceaq4A55+qXAjQVsuy76UJnc8ncYX8f98uSYKcjxo/"
            + "ifn1eHMbL8dGLd5bc2GNBZkmhFIEoDvbfn9jo7phlS8iyvF2YhC4eso8Xb+T7+BZ"
            + "QUOBOvc=").getBytes());
	
	public void testWriteExternal() throws Exception {
		X509Certificate testcert = (X509Certificate)CertTools.getCertfromByteArray(testcertenc);
        Set<X509Certificate> credentials = new HashSet<X509Certificate>();
        credentials.add(testcert);
        Set<X500Principal> principals = new HashSet<X500Principal>();
        principals.add(testcert.getSubjectX500Principal());
        AuthenticationToken token = new X509CertificateAuthenticationToken(principals, credentials);

		DummyApprovalRequest ar = new DummyApprovalRequest(token, null, 1, 2, false);
		
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	ObjectOutputStream oos = new ObjectOutputStream(baos);
    	oos.writeObject(ar);
    	oos.flush();
    	String result = new String(Base64.encode(baos.toByteArray(),false));

    	
    	ApprovalRequest readrequest = ApprovalDataUtil.getApprovalRequest(result);
    	assertTrue(readrequest.getApprovalType() == ApprovalDataVO.APPROVALTYPE_DUMMY);
    	assertTrue(readrequest.getApprovalRequestType() == ApprovalRequest.REQUESTTYPE_SIMPLE);
    	assertTrue(readrequest.getRequestSignature() == null);
    	assertTrue(CertTools.getSerialNumber(readrequest.getRequestAdminCert()).equals(CertTools.getSerialNumber(testcert)));
    	assertTrue(readrequest.getCAId() == 1);
    	assertTrue(readrequest.getEndEntityProfileId() == 2);
    	assertTrue(!readrequest.isExecutable());
		
	}

	public void testGenerateApprovalId() throws Exception {
		X509Certificate testcert = (X509Certificate)CertTools.getCertfromByteArray(testcertenc);
        Set<X509Certificate> credentials = new HashSet<X509Certificate>();
        credentials.add(testcert);
        Set<X500Principal> principals = new HashSet<X500Principal>();
        principals.add(testcert.getSubjectX500Principal());
        AuthenticationToken token = new X509CertificateAuthenticationToken(principals, credentials);

        DummyApprovalRequest ar = new DummyApprovalRequest(token, null, 1, 2, false);
		
    	int id1 = ar.generateApprovalId();
    	int id2 = ar.generateApprovalId();
    	assertEquals(id1, id2);
	}

	public void setUp() throws Exception {		
		super.setUp();
		CryptoProviderTools.installBCProviderIfNotAvailable();
	}

}
