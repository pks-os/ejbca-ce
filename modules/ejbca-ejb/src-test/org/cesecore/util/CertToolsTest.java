/*************************************************************************
 *                                                                       *
 *  CESeCore: CE Security Core                                           *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/

package org.cesecore.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.URL;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.util.ASN1Dump;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.X509DefaultEntryConverter;
import org.bouncycastle.asn1.x509.qualified.ETSIQCObjectIdentifiers;
import org.bouncycastle.asn1.x509.qualified.RFC3739QCObjectIdentifiers;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.X509KeyUsage;
import org.bouncycastle.util.encoders.Hex;
import org.cesecore.certificates.util.AlgorithmConstants;
import org.cesecore.certificates.util.cert.CrlExtensions;
import org.cesecore.certificates.util.cert.QCStatementExtension;
import org.cesecore.certificates.util.cert.SubjectDirAttrExtension;
import org.cesecore.keys.util.KeyTools;
import org.ejbca.cvc.AuthorizationRoleEnum;
import org.ejbca.cvc.CAReferenceField;
import org.ejbca.cvc.CVCAuthenticatedRequest;
import org.ejbca.cvc.CVCObject;
import org.ejbca.cvc.CVCertificate;
import org.ejbca.cvc.CardVerifiableCertificate;
import org.ejbca.cvc.CertificateGenerator;
import org.ejbca.cvc.CertificateParser;
import org.ejbca.cvc.HolderReferenceField;
import org.junit.Before;
import org.junit.Test;

import com.novell.ldap.LDAPDN;

/**
 * Tests the CertTools class .
 * 
 * @version $Id$
 */
public class CertToolsTest {
    private static Logger log = Logger.getLogger(CertToolsTest.class);
    private static byte[] testcert = Base64.decode(("MIIDATCCAmqgAwIBAgIIczEoghAwc3EwDQYJKoZIhvcNAQEFBQAwLzEPMA0GA1UE"
            + "AxMGVGVzdENBMQ8wDQYDVQQKEwZBbmFUb20xCzAJBgNVBAYTAlNFMB4XDTAzMDky" + "NDA2NDgwNFoXDTA1MDkyMzA2NTgwNFowMzEQMA4GA1UEAxMHcDEydGVzdDESMBAG"
            + "A1UEChMJUHJpbWVUZXN0MQswCQYDVQQGEwJTRTCBnTANBgkqhkiG9w0BAQEFAAOB" + "iwAwgYcCgYEAnPAtfpU63/0h6InBmesN8FYS47hMvq/sliSBOMU0VqzlNNXuhD8a"
            + "3FypGfnPXvjJP5YX9ORu1xAfTNao2sSHLtrkNJQBv6jCRIMYbjjo84UFab2qhhaJ" + "wqJgkQNKu2LHy5gFUztxD8JIuFPoayp1n9JL/gqFDv6k81UnDGmHeFcCARGjggEi"
            + "MIIBHjAPBgNVHRMBAf8EBTADAQEAMA8GA1UdDwEB/wQFAwMHoAAwOwYDVR0lBDQw" + "MgYIKwYBBQUHAwEGCCsGAQUFBwMCBggrBgEFBQcDBAYIKwYBBQUHAwUGCCsGAQUF"
            + "BwMHMB0GA1UdDgQWBBTnT1aQ9I0Ud4OEfNJkSOgJSrsIoDAfBgNVHSMEGDAWgBRj" + "e/R2qFQkjqV0pXdEpvReD1eSUTAiBgNVHREEGzAZoBcGCisGAQQBgjcUAgOgCQwH"
            + "Zm9vQGZvbzASBgNVHSAECzAJMAcGBSkBAQEBMEUGA1UdHwQ+MDwwOqA4oDaGNGh0" + "dHA6Ly8xMjcuMC4wLjE6ODA4MC9lamJjYS93ZWJkaXN0L2NlcnRkaXN0P2NtZD1j"
            + "cmwwDQYJKoZIhvcNAQEFBQADgYEAU4CCcLoSUDGXJAOO9hGhvxQiwjGD2rVKCLR4" + "emox1mlQ5rgO9sSel6jHkwceaq4A55+qXAjQVsuy76UJnc8ncYX8f98uSYKcjxo/"
            + "ifn1eHMbL8dGLd5bc2GNBZkmhFIEoDvbfn9jo7phlS8iyvF2YhC4eso8Xb+T7+BZ" + "QUOBOvc=").getBytes());

    private static byte[] guidcert = Base64.decode(("MIIC+zCCAmSgAwIBAgIIBW0F4eGmH0YwDQYJKoZIhvcNAQEFBQAwMTERMA8GA1UE"
            + "AxMIQWRtaW5DQTExDzANBgNVBAoTBkFuYVRvbTELMAkGA1UEBhMCU0UwHhcNMDQw" + "OTE2MTc1NzQ1WhcNMDYwOTE2MTgwNzQ1WjAyMRQwEgYKCZImiZPyLGQBARMEZ3Vp"
            + "ZDENMAsGA1UEAxMER3VpZDELMAkGA1UEBhMCU0UwgZ8wDQYJKoZIhvcNAQEBBQAD" + "gY0AMIGJAoGBANdjsBcLJKUN4hzJU1p3cqaXhPgEjGul62/3xv+Gow+7oOYePcK8"
            + "bM5VO4zdQVWEhuGOZFaZ70YbXhei4F9kvqlN7xuG47g7DNZ0/fnRzvGY0BHmIR4Y" + "/U87oMEDa2Giy0WTjsmT14uzy4luFgqb2ZA3USGcyJ9hoT6j1WDyOxitAgMBAAGj"
            + "ggEZMIIBFTAMBgNVHRMBAf8EAjAAMA4GA1UdDwEB/wQEAwIFoDA7BgNVHSUENDAy" + "BggrBgEFBQcDAQYIKwYBBQUHAwIGCCsGAQUFBwMEBggrBgEFBQcDBQYIKwYBBQUH"
            + "AwcwHQYDVR0OBBYEFJlDddj88zI7tz3SPfdig0gw5IWvMB8GA1UdIwQYMBaAFI1k" + "9WhE1WXpeezZx/kM0qsoZyqVMHgGA1UdEQRxMG+BDGd1aWRAZm9vLmNvbYIMZ3Vp"
            + "ZC5mb28uY29thhRodHRwOi8vZ3VpZC5mb28uY29tL4cECgwNDqAcBgorBgEEAYI3" + "FAIDoA4MDGd1aWRAZm9vLmNvbaAXBgkrBgEEAYI3GQGgCgQIEjRWeJCrze8wDQYJ"
            + "KoZIhvcNAQEFBQADgYEAq39n6CZJgJnW0CH+QkcuU5F4RQveNPGiJzIJxUeOQ1yQ" + "gSkt3hvNwG4kLBmmwe9YLdS83dgNImMWL/DgID/47aENlBNai14CvtMceokik4IN"
            + "sacc7x/Vp3xezHLuBMcf3E3VSo4FwqcUYFmu7Obke3ebmB08nC6gnQHkzjNsmQw=").getBytes());

    private static byte[] altNameCert = Base64.decode(("MIIDDzCCAfegAwIBAgIIPiL0klmu1uIwDQYJKoZIhvcNAQEFBQAwNzERMA8GA1UE"
            + "AxMIQWRtaW5DQTExFTATBgNVBAoTDEVKQkNBIFNhbXBsZTELMAkGA1UEBhMCU0Uw" + "HhcNMDUwODAyMTAxOTQ5WhcNMDcwODAyMTAyOTQ5WjAsMQwwCgYDVQQDEwNmb28x"
            + "DzANBgNVBAoTBkFuYVRvbTELMAkGA1UEBhMCU0UwXDANBgkqhkiG9w0BAQEFAANL" + "ADBIAkEAmMVWkkEMLbDNoB/NG3kJ22eC18syXqaHWRWc4DldFeCMGeLzfB2NklNv"
            + "hmr2kgIJcK+wyFpMkYm46dSMOrvovQIDAQABo4HxMIHuMAwGA1UdEwEB/wQCMAAw" + "DgYDVR0PAQH/BAQDAgWgMDsGA1UdJQQ0MDIGCCsGAQUFBwMBBggrBgEFBQcDAgYI"
            + "KwYBBQUHAwQGCCsGAQUFBwMFBggrBgEFBQcDBzAdBgNVHQ4EFgQUIV/Fck/+UVnw" + "tJigtZIF5OuuhlIwHwYDVR0jBBgwFoAUB/2KRYNOZxRDkJ5oChjNeXgwtCcwUQYD"
            + "VR0RBEowSIEKdG9tYXNAYS5zZYIId3d3LmEuc2WGEGh0dHA6Ly93d3cuYS5zZS+H" + "BAoBAQGgGAYKKwYBBAGCNxQCA6AKDAhmb29AYS5zZTANBgkqhkiG9w0BAQUFAAOC"
            + "AQEAfAGJM0/s+Yi1Ewmvt9Z/9w8X/T/02bF8P8MJG2H2eiIMCs/tkNhnlFGYYGhD" + "Km8ynveQZbdYvKFioOr/D19gMis/HNy9UDfOMrJdeGWiwxUHvKKbtcSlOPH3Hm0t"
            + "LSKomWdKfjTksfj69Tf01S0oNonprvwGxIdsa1uA9BC/MjkkPt1qEWkt/FWCfq9u" + "8Xyj2tZEJKjLgAW6qJ3ye81pEVKHgMmapWTQU2uI1qyEPYxoT9WkQtSObGI1wCqO"
            + "YmKglnd5BIUBPO9LOryyHlSRTID5z0UgDlrTAaNYuN8QOYF+DZEQxm4bSXTDooGX" + "rHjSjn/7Urb31CXWAxq0Zhk3fg==").getBytes());

    private static byte[] altNameCertWithDirectoryName = Base64
            .decode(("MIIFkjCCBPugAwIBAgIIBzGqGNsLMqwwDQYJKoZIhvcNAQEFBQAwWTEYMBYGA1UEAwwPU1VCX0NBX1dJTkRPV1MzMQ8wDQYDVQQLEwZQS0lHVkExHzAdBgNVBAoTFkdlbmVyYWxpdGF0IFZhbGVuY2lhbmExCzAJBgNVBAYTAkVTMB4XDTA2MDQyMTA5NDQ0OVoXDTA4MDQyMDA5NTQ0OVowcTEbMBkGCgmSJomT8ixkAQETC3Rlc3REaXJOYW1lMRQwEgYDVQQDEwt0ZXN0RGlyTmFtZTEOMAwGA1UECxMFbG9nb24xHzAdBgNVBAoTFkdlbmVyYWxpdGF0IFZhbGVuY2lhbmExCzAJBgNVBAYTAkVTMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCDLxMhz40RxCm21HoCBNa9x1UyPmhVkPdtt2V7dixgjOYz+ffKeebjn/jSd4nfXgd7fxpzezB8t673F2OtC3ENl1zek5Msj2KoinVu8vvZ78KMRq/H1rDFguhjSL0o19Cpob0qQFB/ukPZMNoKBNnMVnR1C4juB1eJVXWmHyJxIwIDAQABo4IDSTCCA0UwDAYDVR0TAQH/BAIwADAOBgNVHQ8BAf8EBAMCBaAwMwYDVR0lBCwwKgYIKwYBBQUHAwIGCCsGAQUFBwMEBggrBgEFBQcDBwYKKwYBBAGCNxQCAjAdBgNVHQ4EFgQUZz4hrh3dr6VWvEbAPe8pg7szNi4wHwYDVR0jBBgwFoAUTuOaap9UBpQ8dqwOufYoOQucfUowXAYDVR0RBFUwU6QhMB8xHTAbBgNVBAMMFHRlc3REaXJOYW1lfGRpcnxuYW1loC4GCisGAQQBgjcUAgOgIAwedGVzdERpck5hbWVAamFtYWRvci5wa2kuZ3ZhLmVzMIIBtgYDVR0gBIIBrTCCAakwggGlBgsrBgEEAb9VAwoBADCCAZQwggFeBggrBgEFBQcCAjCCAVAeggFMAEMAZQByAHQAaQBmAGkAYwBhAGQAbwAgAHIAZQBjAG8AbgBvAGMAaQBkAG8AIABkAGUAIABFAG4AdABpAGQAYQBkACAAZQB4AHAAZQBkAGkAZABvACAAcABvAHIAIABsAGEAIABBAHUAdABvAHIAaQBkAGEAZAAgAGQAZQAgAEMAZQByAHQAaQBmAGkAYwBhAGMAaQDzAG4AIABkAGUAIABsAGEAIABDAG8AbQB1AG4AaQB0AGEAdAAgAFYAYQBsAGUAbgBjAGkAYQBuAGEAIAAoAFAAbAAuACAATQBhAG4AaQBzAGUAcwAgADEALgAgAEMASQBGACAAUwA0ADYAMQAxADAAMAAxAEEAKQAuACAAQwBQAFMAIAB5ACAAQwBQACAAZQBuACAAaAB0AHQAcAA6AC8ALwB3AHcAdwAuAGEAYwBjAHYALgBlAHMwMAYIKwYBBQUHAgEWJGh0dHA6Ly93d3cuYWNjdi5lcy9sZWdpc2xhY2lvbl9jLmh0bTBDBgNVHR8EPDA6MDigNqA0hjJodHRwOi8vemFyYXRob3MuamFtYWRvci5ndmEuZXMvU1VCX0NBX1dJTkRPV1MzLmNybDBTBggrBgEFBQcBAQRHMEUwQwYIKwYBBQUHMAGGN2h0dHA6Ly91bGlrLnBraS5ndmEuZXM6ODA4MC9lamJjYS9wdWJsaWN3ZWIvc3RhdHVzL29jc3AwDQYJKoZIhvcNAQEFBQADgYEASofgaj06BOE847RTEgVba52lmPWADgeWxKHZAk1t9LdNzuFJ8B/SC3gi0rsAA/lQGSd4WzPbkmJKkVZ6Q9ybpqg4AJRaIZBkoQw1KNXPYAcgt5XLeIhUACdKIPhfPQr+vQtaC1wi5xV8EBCLpLmpzN9bpZdze/724UB4Y94KhII=")
                    .getBytes());

    /** The reference certificate from RFC3739 */
    private static byte[] qcRefCert = Base64.decode(("MIIDEDCCAnmgAwIBAgIESZYC0jANBgkqhkiG9w0BAQUFADBIMQswCQYDVQQGEwJE"
            + "RTE5MDcGA1UECgwwR01EIC0gRm9yc2NodW5nc3plbnRydW0gSW5mb3JtYXRpb25z" + "dGVjaG5payBHbWJIMB4XDTA0MDIwMTEwMDAwMFoXDTA4MDIwMTEwMDAwMFowZTEL"
            + "MAkGA1UEBhMCREUxNzA1BgNVBAoMLkdNRCBGb3JzY2h1bmdzemVudHJ1bSBJbmZv" + "cm1hdGlvbnN0ZWNobmlrIEdtYkgxHTAMBgNVBCoMBVBldHJhMA0GA1UEBAwGQmFy"
            + "emluMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDc50zVodVa6wHPXswg88P8" + "p4fPy1caIaqKIK1d/wFRMN5yTl7T+VOS57sWxKcdDzGzqZJqjwjqAP3DqPK7AW3s"
            + "o7lBG6JZmiqMtlXG3+olv+3cc7WU+qDv5ZXGEqauW4x/DKGc7E/nq2BUZ2hLsjh9" + "Xy9+vbw+8KYE9rQEARdpJQIDAQABo4HpMIHmMGQGA1UdCQRdMFswEAYIKwYBBQUH"
            + "CQQxBBMCREUwDwYIKwYBBQUHCQMxAxMBRjAdBggrBgEFBQcJATERGA8xOTcxMTAx" + "NDEyMDAwMFowFwYIKwYBBQUHCQIxCwwJRGFybXN0YWR0MA4GA1UdDwEB/wQEAwIG"
            + "QDASBgNVHSAECzAJMAcGBSskCAEBMB8GA1UdIwQYMBaAFAABAgMEBQYHCAkKCwwN" + "Dg/+3LqYMDkGCCsGAQUFBwEDBC0wKzApBggrBgEFBQcLAjAdMBuBGW11bmljaXBh"
            + "bGl0eUBkYXJtc3RhZHQuZGUwDQYJKoZIhvcNAQEFBQADgYEAj4yAu7LYa3X04h+C" + "7+DyD2xViJCm5zEYg1m5x4znHJIMZsYAU/vJJIJQkPKVsIgm6vP/H1kXyAu0g2Ep"
            + "z+VWPnhZK1uw+ay1KRXw8rw2mR8hQ2Ug6QZHYdky2HH3H/69rWSPp888G8CW8RLU" + "uIKzn+GhapCuGoC4qWdlGLWqfpc=").getBytes());

    private static byte[] qcPrimeCert = Base64.decode(("MIIDMDCCAhigAwIBAgIIUDIxBvlO2qcwDQYJKoZIhvcNAQEFBQAwNzERMA8GA1UE"
            + "AxMIQWRtaW5DQTExFTATBgNVBAoTDEVKQkNBIFNhbXBsZTELMAkGA1UEBhMCU0Uw" + "HhcNMDYwMTIyMDgxNTU0WhcNMDgwMTIyMDgyNTU0WjAOMQwwCgYDVQQDEwNxYzIw"
            + "gZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAKkuPOqOEWCJH9xb11sS++vfKb/z" + "gHf2clwyf2vSFWTSDzQHOa2j5rwZ/F23X/mZl96fFAIfTBmr5dCwt0xAXZvTcKfO"
            + "RAcKl7ZBXvsAYvwl1KIUpA8NqEbgjwA+OaTdND2vpAhII7PoU4CkoNajy44EuL3Y" + "xP6KNWTMiks9KP5vAgMBAAGjgewwgekwDAYDVR0TAQH/BAIwADAOBgNVHQ8BAf8E"
            + "BAMCBPAwJwYDVR0lBCAwHgYIKwYBBQUHAwIGCCsGAQUFBwMEBggrBgEFBQcDBzAd" + "BgNVHQ4EFgQUZsj/dUVp1FmOJpYZ2j5fYKIdXYowHwYDVR0jBBgwFoAUs8UBsa9O"
            + "S1c8/I07DHYFJp0po0AwYAYIKwYBBQUHAQMEVDBSMCMGCCsGAQUFBwsBMBcGAykB" + "AjAQgQ5xY0BwcmltZWtleS5zZTAIBgYEAI5GAQEwFwYGBACORgECMA0TA1NFSwID"
            + "AMNQAgEAMAgGBgQAjkYBBDANBgkqhkiG9w0BAQUFAAOCAQEAjmL27XY5Wt0/axsI" + "PbtcfrJ6xEm5PlYabM+T3I6lksov6Rz1+/n/L1S5poGPG8iOdJCExcnR0HbNkeB+"
            + "2oPltqSaxyoSfGugVn/Oufz2BfFd7OCWe14dPsA181oC7/nq+mzhBpQ7App9JirA" + "aeJQrcRDNK7vVOmg2LZ2oSYno/TuRTFq0GxsEVjEdzAxpAxY7N8ff6gY7IHd7+hc"
            + "4GiFY+NnNp9Dvf6mOYTXLxsOc+093S7uK2ohhq99aYCkzJmrngtrImtKi0y/LMjq" + "oviMCQmzMLY2Ifcw+CsOyQZx7nxwafZ7BAzm6vIvSeiIe3VlskRGzYDM66NJJNNo"
            + "C2HsPA==").getBytes());

    private static byte[] aiaCert = Base64.decode(("MIIDYDCCAkigAwIBAgIIFlJveCmyW4owDQYJKoZIhvcNAQEFBQAwNzERMA8GA1UE"
            + "AwwIQWRtaW5DQTExFTATBgNVBAoMDEVKQkNBIFNhbXBsZTELMAkGA1UEBhMCU0Uw" + "HhcNMDgxMDIwMDkxOTM0WhcNMDkxMDIwMDkxOTM0WjA9MQwwCgYDVQQDDANhaWEx"
            + "DDAKBgNVBAoMA0ZvbzESMBAGA1UEBwwJU3RvY2tob2xtMQswCQYDVQQGEwJTRTCB" + "nzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAlYyB3bj/Tmf1FGoPWXCJneWYd9Th"
            + "gPi4ET5pL0JNGwOsuH6cPngIIN33fn2JiiBnBkNm7AKHx8Qt9BH4VPJRs/GdsVGO" + "ECmpGmtY6WMYmxMC99KNiXSrRQjPGZeemMj6T1KyxhKljZr8Q92tmc9YA1VFMeqA"
            + "zNzjEGBDj/h2gBcCAwEAAaOB7TCB6jB5BggrBgEFBQcBAQRtMGswKgYIKwYBBQUH" + "MAKGHmh0dHA6Ly9sb2NhbGhvc3Q6ODA4MC9jYUlzc3VlcjA9BggrBgEFBQcwAYYx"
            + "aHR0cDovL2xvY2FsaG9zdDo4MDgwL2VqYmNhL3B1YmxpY3dlYi9zdGF0dXMvb2Nz" + "cDAdBgNVHQ4EFgQUF4YFO3HJordNZlJ/7T1L1KfqgTMwDAYDVR0TAQH/BAIwADAf"
            + "BgNVHSMEGDAWgBSSeB41+0/rZ+2/qiX7X4bvrVKjWDAPBgkrBgEFBQcwAQUEAgUA" + "MA4GA1UdDwEB/wQEAwIGwDANBgkqhkiG9w0BAQUFAAOCAQEAU1BHlD6TpSnmblU4"
            + "jhECKZfU7P5JBvZMkUQH54U+lubhM4yeymaF1NJylOusLKxZzEd6+iLXkvVCBKPT" + "3aVWUI5DO4D0RW9Lia6QFiRuI8d7a39f1663ODuwpjiccuehrmF3e+P7uCyjqhhT"
            + "g3uXQh2dXcv3DbvU2lfSVXRnuOz+K0ZUMAW96nsCeT41viM6w4x18zZeb+Px8RL9" + "swtcYdObNK0qmjZ4X+DcbdGRRrh8kr9GPLHYqtVLRM6z6hH3n54WJzojeIebKCsY"
            + "MoHGmOJkaIcFRXfneXrId1/k7b1QdOagGjvLkgw3pi/7k6vOJn+DrudNMFmsNpVY" + "fkrayw==").getBytes());

    private static byte[] subjDirAttrCert = Base64.decode(("MIIGmTCCBYGgAwIBAgIQGMYCpWmOBXXOL2ODrM8FHzANBgkqhkiG9w0BAQUFADBx"
            + "MQswCQYDVQQGEwJUUjEoMCYGA1UEChMfRWxla3Ryb25payBCaWxnaSBHdXZlbmxp" + "Z2kgQS5TLjE4MDYGA1UEAxMvZS1HdXZlbiBFbGVrdHJvbmlrIFNlcnRpZmlrYSBI"
            + "aXptZXQgU2FnbGF5aWNpc2kwHhcNMDYwMzI4MDAwMDAwWhcNMDcwMzI4MjM1OTU5" + "WjCCAR0xCzAJBgNVBAYTAlRSMSgwJgYDVQQKDB9FbGVrdHJvbmlrIEJpbGdpIEd1"
            + "dmVubGlnaSBBLlMuMQ8wDQYDVQQLDAZHS05FU0kxFDASBgNVBAUTCzIyOTI0NTQ1" + "MDkyMRswGQYDVQQLDBJEb2d1bSBZZXJpIC0gQlVSU0ExIjAgBgNVBAsMGURvZ3Vt"
            + "IFRhcmloaSAtIDAxLjA4LjE5NzcxPjA8BgNVBAsMNU1hZGRpIFPEsW7EsXIgLSA1" + "MC4wMDAgWVRMLTIuMTYuNzkyLjEuNjEuMC4xLjUwNzAuMS4yMRcwFQYDVQQDDA5Z"
            + "QVPEsE4gQkVDRU7EsDEjMCEGCSqGSIb3DQEJARYUeWFzaW5AdHVya2VrdWwuYXYu" + "dHIwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAKaJXVLvXC7qyjiqTAlM582X"
            + "GPdQJxUfRxgTm6jlBZKtEhbWN5hbH4ASJTzmXWryGricejdKM+JBJECFdelyWPHs" + "UkEL/U0uft3KLIdYo72oTibaL3j4vkEhjyubikSdl9CywkY6WS8nV9JNc66QOYxE"
            + "5ZdE5CR19ScIYcOh7YpxAgMBAAGjggMBMIIC/TAJBgNVHRMEAjAAMAsGA1UdDwQE" + "AwIGwDBWBgNVHR8ETzBNMEugSaBHhkVodHRwOi8vY3JsLmUtZ3V2ZW4uY29tL0Vs"
            + "ZWt0cm9uaWtCaWxnaUd1dmVubGlnaUFTR0tORVNJL0xhdGVzdENSTC5jcmwwHwYD" + "VR0jBBgwFoAUyT6jfNNisqvczhIzwmTXZTTyfrowggEcBgNVHSAEggETMIIBDzCB"
            + "/wYJYIYYAwABAQECMIHxMDYGCCsGAQUFBwIBFipodHRwczovL3d3dy5lLWd1dmVu" + "LmNvbS9lLWltemEvYmlsZ2lkZXBvc3UwgbYGCCsGAQUFBwICMIGpGoGmQnUgc2Vy"
            + "dGlmaWthLCA1MDcwIHNhef1s/SBFbGVrdHJvbmlrIN1temEgS2FudW51bmEgZ/Zy" + "ZSBuaXRlbGlrbGkgZWxla3Ryb25payBzZXJ0aWZpa2Fk/XIuIE9JRDogMi4xNi43"
            + "OTIuMS42MS4wLjEuNTA3MC4xLjEgLSBPSUQ6IDAuNC4wLjE0NTYuMS4yIC0gT0lE" + "OiAwLjQuMC4xODYyLjEuMTALBglghhgDAAEBBQQwgaEGCCsGAQUFBwEDBIGUMIGR"
            + "MHYGCCsGAQUFBwsBMGoGC2CGGAE9AAGnTgEBMFuGWUJ1IFNlcnRpZmlrYSA1MDcw" + "IHNhef1s/SBFbGVrdHJvbmlrIN1temEgS2FudW51bmEgZ/ZyZSBuaXRlbGlrbGkg"
            + "ZWxla3Ryb25payBzZXJ0aWZpa2Fk/XIuMBcGBgQAjkYBAjANEwNZVEwCAwDDUAIB" + "ADB2BggrBgEFBQcBAQRqMGgwIwYIKwYBBQUHMAGGF2h0dHA6Ly9vY3NwLmUtZ3V2"
            + "ZW4uY29tMCIGCCsGAQUFBzAChhZodHRwOi8vd3d3LmUtZ3V2ZW4uY29tMB0GAytv" + "DoYWaHR0cDovL3d3dy5lLWd1dmVuLmNvbTAbBgNVHQkEFDASMBAGCCsGAQUFBwkE"
            + "MQQTAlRSMBEGCWCGSAGG+EIBAQQEAwIHgDANBgkqhkiG9w0BAQUFAAOCAQEA3yVY" + "rURakBcrfv1hJjhDg7+ylCjXf9q6yP2E03kG4t606TLIyqWoqGkrndMtanp+a440"
            + "rLPIe456XfRJBilj99H0NjzKACAVfLMTL8h/JBGLDYJJYA1S8PzBnMLHA8dhfBJ7" + "StYEPM9BKW/WuBfOOdBNrRZtYKCHwGK2JANfM/JlfzOyG4A+XDQcgjiNoosjes1P"
            + "qUHsaccIy0MM7FLMVV0HJNNQ84N9CuKIrBSSWopOudkajVqNtI3+FCcy+yXiH6LX" + "fmpHZ346zprcafcjQmAiKfzPSljruvGDIVI3WN7S7WOMrx6MDq54626cZzQl9GFT"
            + "D1gNo3fjOFhK33DY1Q==").getBytes());

    private static byte[] subjDirAttrCert2 = Base64.decode(("MIIEsjCCA5qgAwIBAgIIFsYK/Jx7XEEwDQYJKoZIhvcNAQEFBQAwNzERMA8GA1UE"
            + "AxMIQWRtaW5DQTExFTATBgNVBAoTDEVKQkNBIFNhbXBsZTELMAkGA1UEBhMCU0Uw" + "HhcNMDYwNTMwMDcxNjU2WhcNMDgwNTI5MDcyNjU2WjA5MRkwFwYDVQQDExBUb21h"
            + "cyBHdXN0YXZzc29uMQ8wDQYDVQQKEwZGb29PcmcxCzAJBgNVBAYTAlNFMIGfMA0G" + "CSqGSIb3DQEBAQUAA4GNADCBiQKBgQCvhUYzNVW6iG5TpYi2Dr9VX37g05jcGEyP"
            + "Lix05oxs3FnzPUf6ykxGy4nUYO12PfC6u9Gh+zelFfg6nKNQqYI48D4ufJc928Nx" + "dZQZi41UmnFT5UXn3JcG4DQe0wZp+BKCch/UbtRjuE6iNxH24R//8W4wXc1R++FG"
            + "5V6CQzHxXwIDAQABo4ICQjCCAj4wDAYDVR0TAQH/BAIwADAOBgNVHQ8BAf8EBAMC" + "BPAwHQYDVR0lBBYwFAYIKwYBBQUHAwEGCCsGAQUFBwMCMB0GA1UdDgQWBBQ54I1p"
            + "TGNwAeQEdnmcjNT+XMMjsjAfBgNVHSMEGDAWgBRzBo+b/XQZqq0DU6J10x17GoKS" + "sDBMBgNVHSAERTBDMEEGAykBATA6MB4GCCsGAQUFBwICMBIeEABGAPYA9gBCAGEA"
            + "cgDkAOQwGAYIKwYBBQUHAgEWDGh0dHA6LzExMS5zZTBuBgNVHR8EZzBlMGOgYaBf" + "hl1odHRwOi8vbG9jYWxob3N0OjgwODAvZWpiY2EvcHVibGljd2ViL3dlYmRpc3Qv"
            + "Y2VydGRpc3Q/Y21kPWNybCZpc3N1ZXI9Q049VGVzdENBLE89QW5hVG9tLEM9U0Uw" + "TQYIKwYBBQUHAQEEQTA/MD0GCCsGAQUFBzABhjFodHRwOi8vbG9jYWxob3N0Ojgw"
            + "ODAvZWpiY2EvcHVibGljd2ViL3N0YXR1cy9vY3NwMDoGCCsGAQUFBwEDBC4wLDAg" + "BggrBgEFBQcLAjAUMBKBEHJhQGNvbW1maWRlcy5jb20wCAYGBACORgEBMHYGA1Ud"
            + "CQRvMG0wEAYIKwYBBQUHCQUxBBMCU0UwEAYIKwYBBQUHCQQxBBMCU0UwDwYIKwYB" + "BQUHCQMxAxMBTTAXBggrBgEFBQcJAjELEwlTdG9ja2hvbG0wHQYIKwYBBQUHCQEx"
            + "ERgPMTk3MTA0MjUxMjAwMDBaMA0GCSqGSIb3DQEBBQUAA4IBAQA+vgNnGjw29xEs" + "cnJi7wInUBvtTzQ4+SVSBPTzNA/ZEk+CJVsr/2xbPl+SShZ0SHObj9un1kwKst4n"
            + "zcNqsnBorrluM92Z5gYwDN3mRGF0szbYEshr/KezMhY2MdXkE+i3nEx6awdemuCG" + "g+LAfL4ODLAzAJJI4MfF+fz0IK7Zeobo1aVGS6Ii9sEnDdQOsLbdfHBNccrT353d"
            + "NAwxPGnfunGBQ+Los6vjDApy/szMT32NFJDe4WTmkDxqYJQqQjhdrHTxpFEr0VQB" + "s7KRRCYjga/Z52XytwwDBLFM9CPZJfyKxZTV9I9i6e0xSn2xEW8NRplY1HOKa/2B"
            + "VzvWW9G5").getBytes());

    private static byte[] krb5principalcert = Base64
            .decode(("MIIDIzCCAgugAwIBAgIIdSCEXyq32cIwDQYJKoZIhvcNAQEFBQAwNzERMA8GA1UE"
                    + "AwwIQWRtaW5DQTExFTATBgNVBAoMDEVKQkNBIFNhbXBsZTELMAkGA1UEBhMCU0Uw"
                    + "HhcNMDgxMDIzMTEyMzAzWhcNMTgwODE2MTQ1MzA2WjAqMQ0wCwYDVQQDDARrcmIx"
                    + "MQwwCgYDVQQKDANGb28xCzAJBgNVBAYTAlNFMIGfMA0GCSqGSIb3DQEBAQUAA4GN"
                    + "ADCBiQKBgQCYkX8BcUXezxG8eKsQT0+lxjUZLeg7EQk0hdiKGsKxhS6BmLpeBOGs"
                    + "HwZgn70zhJj9XLtCQ/o8RJatL/lFtHpVX+RnRdckKDOooLUguxSiO5TK7HlQpsFG"
                    + "8AB7m/jCkIGarh5x6LSL5t1VAMyPh9DFBMXPuC5xAb5SGa6LRXoZ/QIDAQABo4HD"
                    + "MIHAMB0GA1UdDgQWBBTUIo6ZQUrVKoI5GPifVn3KbUGAljAMBgNVHRMBAf8EAjAA"
                    + "MB8GA1UdIwQYMBaAFJJ4HjX7T+tn7b+qJftfhu+tUqNYMA4GA1UdDwEB/wQEAwIF"
                    + "oDAnBgNVHSUEIDAeBggrBgEFBQcDAQYIKwYBBQUHAwIGCCsGAQUFBwMEMDcGA1Ud"
                    + "EQQwMC6gLAYGKwYBBQICoCIwIKAHGwVQLkNPTaEVMBOgAwIBAKEMMAobA2ZvbxsD"
                    + "YmFyMA0GCSqGSIb3DQEBBQUAA4IBAQBgQpzPpCUDY6P0XePJSFJ2MGBhgMOVB4SL"
                    + "iHP9biEmqcqELWQcUL5Ylf+/JYxg1kBnk2ZtALgt0adi0ZiZPbM2F5Oq9ZxxB2nY"
                    + "Alat0RwZIY8wAR0DRNXiEs4TMu5LqzvD1U6+vaHYraePBLExo2oxG9TI7gQjj2X+"
                    + "KSxEzOf3+npWo/G7ooDvKpN+w3J//kF4vdM3SQtHQaBkIuCU05Jy16AhvIkLQzq5"
                    + "+a1UI5lIKun3C6NWCSZrE5fFuoax7D+Ofw1Bdxkhvk7DUlHVPdmxb/0hpx8aO64D" + "J626d8c1b25g9hSYslbo2geP2ohV40WW/R1ZjwX6Pd/ip5KuSSzv")
                    .getBytes());
    
    /**
     * Certificate with two subject alternative names:
     * <pre>
     *            SEQUENCE {
     *              OBJECT IDENTIFIER subjectAltName (2 5 29 17)
     *              OCTET STRING, encapsulates {
     *                SEQUENCE {
     *                  [0] {
     *                    OBJECT IDENTIFIER
     *                      universalPrincipalName (1 3 6 1 4 1 311 20 2 3)
     *                    [0] {
     *                      UTF8String 'upn1@example.com'
     *                      }
     *                    }
     *                  [0] {
     *                    OBJECT IDENTIFIER
     *                      permanentIdentifier (1 3 6 1 5 5 7 8 3)
     *                    [0] {
     *                      SEQUENCE {
     *                        UTF8String 'identifier 10003'
     *                        OBJECT IDENTIFIER '1 2 3 4 5 6'
     *                        }
     *                      }
     *                    }
     *                  }
     *                }
     *              }
     *            }
     * </pre>
     */
    private static byte[] permanentIdentifierCert = Base64
        .decode(("MIIDpjCCAo6gAwIBAgIIR+ghrp5GOgEwDQYJKoZIhvcNAQEFBQAwNzERMA8GA1UE"
                + "AwwIQWRtaW5DQTExFTATBgNVBAoMDEVKQkNBIFNhbXBsZTELMAkGA1UEBhMCU0Uw"
                + "HhcNMTExMTI2MTkyMzU5WhcNMTMxMTI1MTkyMzU5WjAWMRQwEgYDVQQDDAtQZXJt"
                + "IHRlc3QgMTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAMZsjgEEXS09"
                + "98tJAiEJVj/Jjw0TUoyrzkvwwHF6Zny41aMLKSVYqynNOJurEapp+EdSGqu2ajYj"
                + "BQG0+RWjyhhBQuGQa1sv99y2sYHu5BL6Wep+eQJGuWR9rCMGaVrXkNgkCrghxj/A"
                + "U8ag5aDn6H5xgqK9OFQ6q5SFp6PJKFUZHppEdU+YSJGLrNMYRc4hegrH+tqnXLIY"
                + "BW4vrME0eaRlWNVlSVb3E6EwAwgYMads5EQrKuZosnIPqhGHWUoelK7LSg4PG6AY"
                + "JRSHI8EpM7a08Q4haxPbmX5FgTYhCnwsz3ZswB0pflMbNGso7hmqlpelzr2CKZla"
                + "DOFgKFrEiYcCAwEAAaOB1jCB0zAdBgNVHQ4EFgQU8oFZJcr7pYNHOvpTPNyZmDb/"
                + "ZOowDAYDVR0TAQH/BAIwADAfBgNVHSMEGDAWgBRpi5L9rci0UKa3/vvJGyr2nhdS"
                + "qDAOBgNVHQ8BAf8EBAMCBeAwHQYDVR0lBBYwFAYIKwYBBQUHAwIGCCsGAQUFBwME"
                + "MFQGA1UdEQRNMEugIAYKKwYBBAGCNxQCA6ASDBB1cG4xQGV4YW1wbGUuY29toCcG"
                + "CCsGAQUFBwgDoBswGQwQaWRlbnRpZmllciAxMDAwMwYFKgMEBQYwDQYJKoZIhvcN"
                + "AQEFBQADggEBAD6uWly6kndApp4L7cuDeRy3w2dLn0JhwETXPWX1yAOtzClPWZeb"
                + "SbZdDW5zChSd3DgoL5lUiDA+bBDUBIgstkg/4CnlaTeZbIXsxxHvLA0489PiDuEE"
                + "qpX4zJcJUDCMW5OSwUynm6kgkV6IZWn33gwxqBnHKHi2PuqpCSB4iC/XhGYTfC7H"
                + "Jcj5w+sqMgKWR2+Kem2BCufBEy6tlq75Unjm2IE0tvYv6myM5yYW9qxPyjXtrtLi"
                + "fOX1lzhtH1LUCzXPLPYTk6aJ08zsMZxbBe2cHXQibpcwvo3NyaTPlhsZL63e22Ru"
                + "KoAwF60lmxnqTzGP8w0HNHvm+Ybj1Qor3lQ=").getBytes());

    private static byte[] p10ReqWithAltNames = Base64.decode(("MIICtDCCAZwCAQAwNDELMAkGA1UEBhMCU0UxDDAKBgNVBAoTA1JQUzEXMBUGA1UE"
            + "AxMOMTAuMjUyLjI1NS4yMzcwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIB" + "AQC45+Dh1dO/qaZR2TLnWB44wmYXvBuZ5sGXotlLvuRR09DGlSyPrTG/OVg4xVZa"
            + "AzNMpWCyk1OAl4qJkmzrnQa/Tq6Hv6Y8QrZNSAJooL+kHmFSD9h8tyM9nBkpb90l" + "o+qbXeFmB3II0KJjGXiXZVSKwUsjYRSzf9hfVz4U7ZwwmH9vMFNwuOIsAR9O5CTr"
            + "8ofsshze9bxJpKY6/iyaEhQDoNl9jyxsZ1NuyNme3w1yoeGP5OXYcSVVY9cW4ze8" + "o5ZE4jTy1Q8U41OHiG3TevMvJ7l+/Ps+xyu3Qi68Lajeimemf118M0eqAY26Xiw2"
            + "wS8CCbj6UmUjcem3XOZhSfkZAgMBAAGgOzA5BgkqhkiG9w0BCQ4xLDAqMCgGA1Ud" + "EQQhMB+CF29ydDMta3J1Lm5ldC5wb2xpc2VuLnNlhwQK/P/tMA0GCSqGSIb3DQEB"
            + "BQUAA4IBAQCzAPsZdMqhPwCGpnq/Eywm5KQ4zYLuP8dQVdgvo4Wca2w4QxxjPlVI" + "X/yyXLhA1CpiKq4PtkpTBpJiByowj8g/7Q/pLY/EQcfYOrut7CMx1FzmwghZ2lUn"
            + "DDhFw2hD7TcmoAZpr4neXYR4HbaFpBc39nlqDa4XGi8J7d9AU4iaQE53LC3WzIq1" + "/3ZCXboQAoeLMoPCDvzAiXKDBApMMzrBwhgdsiOe5k1e6jlpURsbuhiKs+0FxtMp"
            + "snKPO0WbwXFyFTSWoKRH5rHrpD6lybn7c0uPkaQzrLoIRMld4osqeaImfZuJztZy" + "C0elzlLYWFbX6zHEqvsUAZy/8Khgyw5Q").getBytes());

    private static byte[] p10ReqWithAltNames2 = Base64.decode(("MIIBMzCB3gIBADAzMREwDwYDVQQDDAhzY2VwdGVzdDERMA8GA1UECgwIUHJpbWVL"
            + "ZXkxCzAJBgNVBAYTAlNFMFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAIMasNAoxA9N" + "6UknbjigXz5tJWWydLoVSQFUxcJM8cR4Kfb2bRLh3RDqCVyJQ0XITFUnmIJFU9Z8"
            + "1W+nw1Gx8b0CAwEAAaBGMBUGCSqGSIb3DQEJBzEIDAZmb28xMjMwLQYJKoZIhvcN" + "AQkOMSAwHjAcBgNVHREEFTATggtmb28uYmFyLmNvbYcECgAAATANBgkqhkiG9w0B"
            + "AQUFAANBADUO2tpAkxaeB/2zY9wsfcwE5hGvcuA0oJwXlcMq1wm32MJFV1G9JJQI" + "Exz4OC1eT1LH/6i5SU8Op3VOKVLpTTo=").getBytes());

    private static byte[] cvccert = Base64.decode(("fyGCAWF/ToHZXykBAEIKU0VSUFMxMDExMH9JgZUGCgQAfwAHAgICAQGBgYEAk4Aq"
            + "LqYXchIouF9yBv/2hFnf5N65hdpvQPUdfH1k2qnHAlOL5DYYlKCBh8YFCC2RZD+K" + "nJ99cHxh8oxh28U23Z/MqTOKv5tR8JIUUm3G3Hjj2erVVTEJ49MqLzsyVGfw4yCu"
            + "YRdwBYFWJu2t6PcS5KPnpNtbNdBzrDJAqxPAsO2CAwEAAV8gClNFUlBTMTAxMTB/" + "TA4GCQQAfwAHAwECAVMBw18lBgAIAAUABV8kBgEAAAUABV83gYB88jfXZ3njYpuD"
            + "4fpS6BV53y9+iz3KAQM/74LPMI49elGtcAVyMn1EMn/bU4MeMARfv3Njd2Go4ZhM" + "j5xuY2Pvktz3Dq4ogjkgqAJqqIvG+M9KXh9XAv2m2wjmsueKbXUJ8TpJR87k4o97"
            + "buZXbuStDOb5FibhxyVgWIxuCn8quQ==").getBytes());

    private static byte[] cvcreqrenew = Base64.decode(("Z4IBtn8hggFmf06CASZfKQEAQg5TRUlTQlBPT0wwMDAwNn9Jgf0GCgQAfwAHAgIC"
            + "AgKBHNfBNKomQ2aGKhgwJXXR14ewnwdXl9qJ9X7IwP+CHGil5iypzmwcKZgDpsFT" + "C1FOGCrYsAQqWcrSn0ODHCWA9jzP5EE4hwcTsakjaeM+ITXSZtuzcjhsQAuEOQQN"
            + "kCmtLH5c9DQII7KofcaMnkzjF0webv3uEsB9WKpW93LAcm8kxrieTs2sJDVLnpnK" + "o/bTdhQCzYUc18E0qiZDZoYqGDAlddD7mNEWvEtt3ryjpaeTn4Y5BBQte2aU3YOQ"
            + "Ykf73/UluNQOpMlnHt9PXplomqhuAZ0QxwXb6TCG3rZJhVwe0wx0R1mqz3U+fJnU" + "hwEBXyAOU0VJU0JQT09MMDAwMDZfNzgErOAjPCoQ+WN8K6pzztZp+Mt6YGNkJzkk"
            + "WdLnvfPGZkEF0oUjcw+NjexaNCLOA0mCfu4oQwsjrUIOU0VJU0JQT09MMDAwMDVf" + "NzhSmH1c7YJhbLTRzwuSozUd9hlBHKEIfFqSUE9/FrbWXEtR+rHRYKAGu/nw8PAH"
            + "oM+HPMzMVVLDVg==").getBytes());

    private static byte[] cvcreq = Base64.decode(("fyGCAWZ/ToIBJl8pAQBCDlNFSVNCUE9PTDAwMDA1f0mB/QYKBAB/AAcCAgICAoEc"
            + "18E0qiZDZoYqGDAlddHXh7CfB1eX2on1fsjA/4IcaKXmLKnObBwpmAOmwVMLUU4Y" + "KtiwBCpZytKfQ4McJYD2PM/kQTiHBxOxqSNp4z4hNdJm27NyOGxAC4Q5BA2QKa0s"
            + "flz0NAgjsqh9xoyeTOMXTB5u/e4SwH1Yqlb3csBybyTGuJ5OzawkNUuemcqj9tN2" + "FALNhRzXwTSqJkNmhioYMCV10PuY0Ra8S23evKOlp5OfhjkEOwPDLflRVBj2iayW"
            + "VzpO2BICGO+PqFeuce1EZM4o1EIfLzoackPowabEMANfNltZvt5bWyzkZleHAQFf" + "IA5TRUlTQlBPT0wwMDAwNV83OEnwL+XYDhXqK/0fBuZ6lZV0HncoZyn3oo8MmaUL"
            + "2mNzpezLAoZMux0l5aYperrSDsuHw0zrf0yo").getBytes());

    private static byte[] cvccertchainroot = Base64.decode(("fyGCAmx/ToIBYl8pAQBCDlNFSFNNQ1ZDQTAwMDAxf0mCARUGCgQAfwAHAgICAQKB"
            + "ggEAyGju6NHTACB+pl2x27/VJVKuGBTgf98j3gQOyW5vDzXI7PkiwR1/ObPjFiuW" + "iBRH0WsPzHX7A3jysZr7IohLjy4oQMdP5z282/ZT4mBwlVu5pAEcHt2eHbpILwIJ"
            + "Hbv6130T+RoG/3bI/eHk9HWi3/ipVnwRX1CsylczFfdyPTMyGOJmmElT0GQgV8Rt" + "b5Us/Hz66qiUX67eRBrahJfwiVwawYzmZ5Rn9u/vXHQYeUh+lLja+H+kXof9ARuw"
            + "p5S09DO2VZWbbR2BZHk0IaNgo54Xoih+5c/nIA/2+j9Afdf+wuqmxqib5aPOMHO3" + "WOVmVMF84Xo2V+duIZ4b7KkRXYIDAQABXyAOU0VIU01DVkNBMDAwMDF/TA4GCQQA"
            + "fwAHAwECAVMBw18lBgAIAAUCBl8kBgEAAAUCBl83ggEAMiiqI+HF8DyhPfH8dTeU" + "4/0/DNnjZ2/Qy1a5GATWU04da+L2iWI8QclN64cw0l/zroBGyeq+flDKzVWnqril"
            + "HX/PD3/xoCEhZSfZ/1AQZBP39/t1lYZLJ36VeFwrsmvN8rq6RnNtR2CrDYDFkFRq" + "A6v9dNYMbnEDN7m8wD/DWM2fZr+loqznT1/egx+SBqUY+KnU6ntxQyw7gzL1DV9Z"
            + "OlyxjDaWY8i2Q/tcdDxdZYBBMgFhxivXV5ou2YiBZKKIlP2ots6P8TlSVwdyaHTI" + "8z8Hpvx1QcB2maOVn6IFAyq/X71p9Zb626YLhjaFO6v80SYnlefVu5Uir5n/HzpW"
            + "kg==").getBytes());

    private static byte[] cvccertchainsub = Base64.decode(("fyGCAeV/ToHcXykBAEIOU0VIU01DVkNBMDAwMDF/SYGUBgoEAH8ABwICAgECgYGA"
            + "rdRouw7ksS6M5kw28YkWAD350vbDlnPCmqsKPfKiNvDxowviWDUTn9Ai3xpTIzGO" + "cl40DqxYPA2X4XO52+r5ZUazsVyyx6F6XwznHdjUpDff4QFyG74Vjq7DDrCCKOzH"
            + "b0H6rNJFC5YEKI4wpEPou+3bq2jhLWkzU35EfydJHXWCAwEAAV8gClNFUlBTRFZF" + "WDJ/TA4GCQQAfwAHAwECAVMBgl8lBgAIAAYABV8kBgEAAAUCBl83ggEAbawFepay"
            + "gX+VrBOsGzbQCpG2mR1NrJbaNdBJcouWYTNzlDP/hRssU9/lTzHulRPupkarepAI" + "GMIDMOo3lNImlYlU8ZlaV6mbKRgWZVjtZmVgq+wLARS4dXNlHRJvS2AustfseGVr"
            + "kqJ0+UYo8x8UL13fB7VCSVqADnOnbemtvE1cIdFcIAqP1JLh91ACJ4lpoaAn10+g" + "5coIGGa01BYEDtiA++SFnRl7kYFykAZrs3eXq+zuPmOo9hr4JxLZuiN5DnIrZdLA"
            + "DWq7GeCFr6wCMg2jPuK9Kqvl06tqylVy4ravVHv58WvAxWFgyuezdRbyV7YAfVF3" + "tlcVDXa3R+mfYg==").getBytes());

    private static byte[] x509certchainsubsub = Base64
            .decode(("MIICAzCCAWygAwIBAgIINrHHHchdmfMwDQYJKoZIhvcNAQEFBQAwEDEOMAwGA1UE"
                    + "AwwFU3ViQ0EwHhcNMTAwNjA1MTIwNzMxWhcNMzAwNjA1MTIwNjUyWjATMREwDwYD"
                    + "VQQDDAhTdWJTdWJDQTCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAiySbgP2A"
                    + "QuZXWZk9C1pOrpVhzWNDAFc4ttVGgh5TS1/wA6Y6nf2ci8gfxkQx1rhR784QUap4"
                    + "id6mwGV/af3WFj34YsTXdozsO/SFi7vvOGA/jU6ZUuQPYpmsSDQ3ZNLcx/MkgkrP"
                    + "WDlFhD7b079oVva5zZsF8w91KlX+KG9usXECAwEAAaNjMGEwHQYDVR0OBBYEFJ9y"
                    + "tRy1CFwUavq8OP25jRybKyElMA8GA1UdEwEB/wQFMAMBAf8wHwYDVR0jBBgwFoAU"
                    + "2GDNoIpTVxc9y953THJoWkS5wjAwDgYDVR0PAQH/BAQDAgGGMA0GCSqGSIb3DQEB"
                    + "BQUAA4GBAE0vHf3iyJ0EqOyN+LUfBkCBTPHl6sEV1bwdgkdVwj9cBbmSEDCOlmYA"
                    + "K0bvAY/1qbgEjkn+Sc32PP/3dmHX5EUKliAodguAu8vK/Rp7kefdUQHnJHwRUMF5" + "9YJDdGtDZx+WLBihYhnTzGVzuP6Qaff3aNyY69O+rwSDm06Au8Zc")
                    .getBytes());

    private static byte[] x509certchainsub = Base64
            .decode(("MIICATCCAWqgAwIBAgIIRzc+cItydm0wDQYJKoZIhvcNAQEFBQAwETEPMA0GA1UE"
                    + "AwwGUm9vdENBMB4XDTEwMDYwNTEyMDcxMVoXDTMwMDYwNTEyMDY1MlowEDEOMAwG"
                    + "A1UEAwwFU3ViQ0EwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAMl3EN+V1M39"
                    + "OVrmHp7PncZ16AkffAtgLk+gIgk+cHZYIGPPN1V/AtCmzteYRrPwGM9Vs2nJ4OZ7"
                    + "F8cJ1MDpyjRdjKC6sVlhkdNq+s1Q/yNtG0AxvlH2KyIZkHU02UNnJGARMaRpZipe"
                    + "VonnAD8D+FkhTt8BM2T7/Grck5QYgJUhAgMBAAGjYzBhMB0GA1UdDgQWBBTYYM2g"
                    + "ilNXFz3L3ndMcmhaRLnCMDAPBgNVHRMBAf8EBTADAQH/MB8GA1UdIwQYMBaAFCua"
                    + "Xc8f/BC8CeBLOVaC5N0Zb4BqMA4GA1UdDwEB/wQEAwIBhjANBgkqhkiG9w0BAQUF"
                    + "AAOBgQBM2lvlZmEJdFXsfoHSt2h4XN6q8Z+sIHDXyuyanNCoQx+9lM2liY+tXAUq"
                    + "Sj1fZAzqjdptIgvG5APnFrnWHeEYjYpYsROs//xF6CUKo8iJEIyRpmx9pSmwA8Rb" + "U0RmY/62tBLr758ZzRGKKoX7znxsXZ5/bouT6g+IxmNuM2EiyA==")
                    .getBytes());

    private static byte[] x509certchainroot = Base64
            .decode(("MIICAjCCAWugAwIBAgIIPXgH6TfNMlYwDQYJKoZIhvcNAQEFBQAwETEPMA0GA1UE"
                    + "AwwGUm9vdENBMB4XDTEwMDYwNTEyMDY1MloXDTMwMDYwNTEyMDY1MlowETEPMA0G"
                    + "A1UEAwwGUm9vdENBMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCK+F3eOoGI"
                    + "Kwa1g68vD6aZlkTCMbbEyoa9Wr4baapdCHvO8YHwVC9UDh8snDtEQT9yZKLlU4nM"
                    + "n05O7yL8FfvgB2j3xN6In1fLq8JizrYVpL49C3ewTwaKMTFjde3BtWDZ4ufJdFuZ"
                    + "+LSw98dM2zhQWme7LnrJQou85LbNt2v6XQIDAQABo2MwYTAdBgNVHQ4EFgQUK5pd"
                    + "zx/8ELwJ4Es5VoLk3RlvgGowDwYDVR0TAQH/BAUwAwEB/zAfBgNVHSMEGDAWgBQr"
                    + "ml3PH/wQvAngSzlWguTdGW+AajAOBgNVHQ8BAf8EBAMCAYYwDQYJKoZIhvcNAQEF"
                    + "BQADgYEAYB5munksWVndUxStBZmsg5uwryu8bf/esxYLlxkO8rG/UnJ9DNw4tJsh"
                    + "YxwGeslPeG9+y8O8MsXKSjdNw3I3avMpj+QqzmqD/MVlHX6+CSyUbhFGPR2TRQCp" + "m+VsfwOl8/INVAySpBf3Uk2rUYhvdUqhCOcE67d0tYdJAqiIDvc=")
                    .getBytes());

    private static byte[] pemcert = ("-----BEGIN CERTIFICATE-----\n" + "MIIDUzCCAjugAwIBAgIIMK64QB5XErowDQYJKoZIhvcNAQEFBQAwNzERMA8GA1UE\n"
            + "AwwIQWRtaW5DQTExFTATBgNVBAoMDEVKQkNBIFNhbXBsZTELMAkGA1UEBhMCU0Uw\n"
            + "HhcNMTAwMTI5MTI0NDIwWhcNMjAwMTI3MTI0NDIwWjA3MREwDwYDVQQDDAhBZG1p\n"
            + "bkNBMTEVMBMGA1UECgwMRUpCQ0EgU2FtcGxlMQswCQYDVQQGEwJTRTCCASIwDQYJ\n"
            + "KoZIhvcNAQEBBQADggEPADCCAQoCggEBAJQNal//KwRHhBg7BLsuclOpH7xyb1AP\n"
            + "Mc5RsEQANOtCBoRlgEDh7tQUfRnPxvmHM+HH9osEV2c+9L23K2l8EVjZRlo2vltJ\n"
            + "duzbBGIo7swdCWGvFxE6W0lkv/YsGVmmt/dL2lO4V4YTuu5CX3PU2LrBR6mxtEsM\n"
            + "mM/YgHo/QWN4/YDWfnXkNpDDjRxLzdsSqvcoLrZavqrMS1Avv2utY6ECyl6PTBG+\n"
            + "qPpozenPRi2QKbiKOpeCgT+2Y4JbMqm+d7go0KKu6wxKE16R/tX9OwT4ObJaKJ/W\n"
            + "j0mm5deJHNDEvgWi2beTVcc16LVZUiyKmZXlYLEdV+CH0NrRc4ck6WsCAwEAAaNj\n"
            + "MGEwHQYDVR0OBBYEFLGsC1OtcOyAklW2b3eN678a6wsZMA8GA1UdEwEB/wQFMAMB\n"
            + "Af8wHwYDVR0jBBgwFoAUsawLU61w7ICSVbZvd43rvxrrCxkwDgYDVR0PAQH/BAQD\n"
            + "AgGGMA0GCSqGSIb3DQEBBQUAA4IBAQBHCJ3ZQEtpfYBHnrwKuNzMykH+yKIkO2mj\n"
            + "c/65e8r7tMjV/9YEb2pAYaix7WAdJ46KcE2ldKl+MHJx8Ev1ZbLPbLYcwPkP+8Ll\n"
            + "pCrhc6riSTaUBZrIA2uuXPPREKj+e8CnnMdCfLy4x6uIMDVAa4mb0akEmLvqFR2X\n"
            + "J4Z8eEhwf6EPRniie6GKcBOWSP0podlWkn8SCLzd+eJZ9H0YMTN7nfLfUdENznuO\n"
            + "5DKfkfT4rOyAGFs+KVwigq1kbSNZJC4Kjo7diMBtWRiXSXTQKE+JgrQbFCdVYQos\n" + "VvSwSiSMW5Rs5ZCtRXMmXz2HdxpbTayCAYPBh6XKfvq4x06gxfll\n"
            + "-----END CERTIFICATE-----").getBytes();

    /** Cert with CDP with URI */
    /*
     * Cert with CDP with URI. <pre> Certificate: Data: Version: 3 (0x2) Serial Number: 52:32:6f:be:9d:3c:4d:d7 Signature Algorithm:
     * sha1WithRSAEncryption Issuer: CN=DemoSubCA11, O=Demo Organization 10, C=SE Validity Not Before: Apr 3 22:17:41 2010 GMT Not After : Apr 2
     * 22:17:41 2012 GMT Subject: CN=pdfsigner12-2testcrl-with-subca Subject Public Key Info: Public Key Algorithm: rsaEncryption RSA Public Key:
     * (1024 bit) Modulus (1024 bit): 00:de:99:da:80:ad:03:21:3c:18:cc:41:1f:ad:4a: fc:2d:69:21:3d:34:52:7c:a4:9c:33:df:a8:36:5a:
     * ee:bd:74:f6:0b:b1:93:79:3c:e7:66:a1:72:d4:1f: 08:b6:43:a3:0a:1a:94:8c:64:e4:10:71:32:be:4b: 00:08:a3:25:11:85:2a:d3:af:fa:dc:d4:ac:7a:48:
     * e8:d3:63:d0:06:4a:cf:ce:84:0e:a5:88:6e:1f:44: c1:9f:ad:89:1e:8b:d0:17:53:20:40:b5:e9:b3:7d: 16:74:e0:22:a7:43:44:99:6a:ba:5c:26:ed:f8:c7:
     * 8c:a5:14:a2:40:83:d6:52:75 Exponent: 65537 (0x10001) X509v3 extensions: X509v3 Subject Key Identifier:
     * 8F:23:26:05:9D:03:57:4F:66:08:F5:E3:34:D3:AA:70:76:9C:99:B2 X509v3 Basic Constraints: critical CA:FALSE X509v3 Authority Key Identifier:
     * keyid:90:FD:A7:F6:EC:98:47:56:4C:10:96:C2:AD:85:2F:50:EB:26:E9:34
     * 
     * X509v3 CRL Distribution Points:
     * URI:http://vmserver1:8080/ejbca/publicweb/webdist/certdist?cmd=crl&issuer=CN=DemoSubCA11,O=Demo%20Organization%2010,C=SE
     * 
     * X509v3 Key Usage: critical Digital Signature Signature Algorithm: sha1WithRSAEncryption 6e:f0:b9:26:b8:7d:eb:b2:ab:ec:e7:1b:a5:97:5c:5b:88:fe:
     * 8a:ec:bb:3d:7a:f5:00:4c:72:38:36:19:53:d4:47:21:30:4c: 62:7c:02:69:00:8c:ac:57:3c:f2:bf:38:57:13:0b:4b:7e:92:
     * 74:56:4c:1b:9c:04:9d:08:e8:8e:20:4d:bc:ec:bc:13:c7:55: 80:da:1a:01:9f:9f:be:96:11:d4:7c:64:f2:37:91:01:9f:c0:
     * 91:af:b6:8a:62:80:71:75:e6:34:f5:57:85:79:d8:7d:e3:71: 71:fa:7c:ca:c8:03:13:d5:0c:12:f5:f6:27:29:36:99:e4:ec: 8b:b1 </pre>
     */
    private static final String CERT_WITH_URI = "-----BEGIN CERTIFICATE-----\n" + "MIIC0zCCAjygAwIBAgIIUjJvvp08TdcwDQYJKoZIhvcNAQEFBQAwQjEUMBIGA1UE"
            + "AwwLRGVtb1N1YkNBMTExHTAbBgNVBAoMFERlbW8gT3JnYW5pemF0aW9uIDEwMQsw" + "CQYDVQQGEwJTRTAeFw0xMDA0MDMyMjE3NDFaFw0xMjA0MDIyMjE3NDFaMCoxKDAm"
            + "BgNVBAMMH3BkZnNpZ25lcjEyLTJ0ZXN0Y3JsLXdpdGgtc3ViY2EwgZ8wDQYJKoZI" + "hvcNAQEBBQADgY0AMIGJAoGBAN6Z2oCtAyE8GMxBH61K/C1pIT00UnyknDPfqDZa"
            + "7r109guxk3k852ahctQfCLZDowoalIxk5BBxMr5LAAijJRGFKtOv+tzUrHpI6NNj" + "0AZKz86EDqWIbh9EwZ+tiR6L0BdTIEC16bN9FnTgIqdDRJlqulwm7fjHjKUUokCD"
            + "1lJ1AgMBAAGjgekwgeYwHQYDVR0OBBYEFI8jJgWdA1dPZgj14zTTqnB2nJmyMAwG" + "A1UdEwEB/wQCMAAwHwYDVR0jBBgwFoAUkP2n9uyYR1ZMEJbCrYUvUOsm6TQwgYUG"
            + "A1UdHwR+MHwweqB4oHaGdGh0dHA6Ly92bXNlcnZlcjE6ODA4MC9lamJjYS9wdWJs" + "aWN3ZWIvd2ViZGlzdC9jZXJ0ZGlzdD9jbWQ9Y3JsJmlzc3Vlcj1DTj1EZW1vU3Vi"
            + "Q0ExMSxPPURlbW8lMjBPcmdhbml6YXRpb24lMjAxMCxDPVNFMA4GA1UdDwEB/wQE" + "AwIHgDANBgkqhkiG9w0BAQUFAAOBgQBu8LkmuH3rsqvs5xull1xbiP6K7Ls9evUA"
            + "THI4NhlT1EchMExifAJpAIysVzzyvzhXEwtLfpJ0VkwbnASdCOiOIE287LwTx1WA" + "2hoBn5++lhHUfGTyN5EBn8CRr7aKYoBxdeY09VeFedh943Fx+nzKyAMT1QwS9fYn"
            + "KTaZ5OyLsQ==" + "\n-----END CERTIFICATE-----";

    /** Cert with CDP without URI. */
    /*
     * Cert with CDP without URI. <pre> Certificate: Data: Version: 3 (0x2) Serial Number: 1042070824 (0x3e1cbd28) Signature Algorithm:
     * sha1WithRSAEncryption Issuer: C=US, O=Adobe Systems Incorporated, OU=Adobe Trust Services, CN=Adobe Root CA Validity Not Before: Jan 8 23:37:23
     * 2003 GMT Not After : Jan 9 00:07:23 2023 GMT Subject: C=US, O=Adobe Systems Incorporated, OU=Adobe Trust Services, CN=Adobe Root CA Subject
     * Public Key Info: Public Key Algorithm: rsaEncryption RSA Public Key: (2048 bit) Modulus (2048 bit):
     * 00:cc:4f:54:84:f7:a7:a2:e7:33:53:7f:3f:9c:12: 88:6b:2c:99:47:67:7e:0f:1e:b9:ad:14:88:f9:c3: 10:d8:1d:f0:f0:d5:9f:69:0a:2f:59:35:b0:cc:6c:
     * a9:4c:9c:15:a0:9f:ce:20:bf:a0:cf:54:e2:e0:20: 66:45:3f:39:86:38:7e:9c:c4:8e:07:22:c6:24:f6: 01:12:b0:35:df:55:ea:69:90:b0:db:85:37:1e:e2:
     * 4e:07:b2:42:a1:6a:13:69:a0:66:ea:80:91:11:59: 2a:9b:08:79:5a:20:44:2d:c9:bd:73:38:8b:3c:2f: e0:43:1b:5d:b3:0b:f0:af:35:1a:29:fe:ef:a6:92:
     * dd:81:4c:9d:3d:59:8e:ad:31:3c:40:7e:9b:91:36: 06:fc:e2:5c:8d:d1:8d:26:d5:5c:45:cf:af:65:3f: b1:aa:d2:62:96:f4:a8:38:ea:ba:60:42:f4:f4:1c:
     * 4a:35:15:ce:f8:4e:22:56:0f:95:18:c5:f8:96:9f: 9f:fb:b0:b7:78:25:e9:80:6b:bd:d6:0a:f0:c6:74: 94:9d:f3:0f:50:db:9a:77:ce:4b:70:83:23:8d:a0:
     * ca:78:20:44:5c:3c:54:64:f1:ea:a2:30:19:9f:ea: 4c:06:4d:06:78:4b:5e:92:df:22:d2:c9:67:b3:7a: d2:01 Exponent: 65537 (0x10001) X509v3 extensions:
     * Netscape Cert Type: SSL CA, S/MIME CA, Object Signing CA X509v3 CRL Distribution Points: DirName:/C=US/O=Adobe Systems Incorporated/OU=Adobe
     * Trust Services/CN=Adobe Root CA/CN=CRL1
     * 
     * X509v3 Private Key Usage Period: Not Before: Jan 8 23:37:23 2003 GMT, Not After: Jan 9 00:07:23 2023 GMT X509v3 Key Usage: Certificate Sign,
     * CRL Sign X509v3 Authority Key Identifier: keyid:82:B7:38:4A:93:AA:9B:10:EF:80:BB:D9:54:E2:F1:0F:FB:80:9C:DE
     * 
     * X509v3 Subject Key Identifier: 82:B7:38:4A:93:AA:9B:10:EF:80:BB:D9:54:E2:F1:0F:FB:80:9C:DE X509v3 Basic Constraints: CA:TRUE
     * 1.2.840.113533.7.65.0: 0...V6.0:4.0.... Signature Algorithm: sha1WithRSAEncryption 32:da:9f:43:75:c1:fa:6f:c9:6f:db:ab:1d:36:37:3e:bc:61:
     * 19:36:b7:02:3c:1d:23:59:98:6c:9e:ee:4d:85:e7:54:c8:20: 1f:a7:d4:bb:e2:bf:00:77:7d:24:6b:70:2f:5c:c1:3a:76:49:
     * b5:d3:e0:23:84:2a:71:6a:22:f3:c1:27:29:98:15:f6:35:90: e4:04:4c:c3:8d:bc:9f:61:1c:e7:fd:24:8c:d1:44:43:8c:16:
     * ba:9b:4d:a5:d4:35:2f:bc:11:ce:bd:f7:51:37:8d:9f:90:e4: 14:f1:18:3f:be:e9:59:12:35:f9:33:92:f3:9e:e0:d5:6b:9a:
     * 71:9b:99:4b:c8:71:c3:e1:b1:61:09:c4:e5:fa:91:f0:42:3a: 37:7d:34:f9:72:e8:cd:aa:62:1c:21:e9:d5:f4:82:10:e3:7b:
     * 05:b6:2d:68:56:0b:7e:7e:92:2c:6f:4d:72:82:0c:ed:56:74: b2:9d:b9:ab:2d:2b:1d:10:5f:db:27:75:70:8f:fd:1d:d7:e2:
     * 02:a0:79:e5:1c:e5:ff:af:64:40:51:2d:9e:9b:47:db:42:a5: 7c:1f:c2:a6:48:b0:d7:be:92:69:4d:a4:f6:29:57:c5:78:11:
     * 18:dc:87:51:ca:13:b2:62:9d:4f:2b:32:bd:31:a5:c1:fa:52: ab:05:88:c8 </pre>
     */
    private static final String CERT_WITHOUT_URI = "-----BEGIN CERTIFICATE-----\n"
            + "MIIEoTCCA4mgAwIBAgIEPhy9KDANBgkqhkiG9w0BAQUFADBpMQswCQYDVQQGEwJV" + "UzEjMCEGA1UEChMaQWRvYmUgU3lzdGVtcyBJbmNvcnBvcmF0ZWQxHTAbBgNVBAsT"
            + "FEFkb2JlIFRydXN0IFNlcnZpY2VzMRYwFAYDVQQDEw1BZG9iZSBSb290IENBMB4X" + "DTAzMDEwODIzMzcyM1oXDTIzMDEwOTAwMDcyM1owaTELMAkGA1UEBhMCVVMxIzAh"
            + "BgNVBAoTGkFkb2JlIFN5c3RlbXMgSW5jb3Jwb3JhdGVkMR0wGwYDVQQLExRBZG9i" + "ZSBUcnVzdCBTZXJ2aWNlczEWMBQGA1UEAxMNQWRvYmUgUm9vdCBDQTCCASIwDQYJ"
            + "KoZIhvcNAQEBBQADggEPADCCAQoCggEBAMxPVIT3p6LnM1N/P5wSiGssmUdnfg8e" + "ua0UiPnDENgd8PDVn2kKL1k1sMxsqUycFaCfziC/oM9U4uAgZkU/OYY4fpzEjgci"
            + "xiT2ARKwNd9V6mmQsNuFNx7iTgeyQqFqE2mgZuqAkRFZKpsIeVogRC3JvXM4izwv" + "4EMbXbML8K81Gin+76aS3YFMnT1Zjq0xPEB+m5E2BvziXI3RjSbVXEXPr2U/sarS"
            + "Ypb0qDjqumBC9PQcSjUVzvhOIlYPlRjF+Jafn/uwt3gl6YBrvdYK8MZ0lJ3zD1Db" + "mnfOS3CDI42gynggRFw8VGTx6qIwGZ/qTAZNBnhLXpLfItLJZ7N60gECAwEAAaOC"
            + "AU8wggFLMBEGCWCGSAGG+EIBAQQEAwIABzCBjgYDVR0fBIGGMIGDMIGAoH6gfKR6" + "MHgxCzAJBgNVBAYTAlVTMSMwIQYDVQQKExpBZG9iZSBTeXN0ZW1zIEluY29ycG9y"
            + "YXRlZDEdMBsGA1UECxMUQWRvYmUgVHJ1c3QgU2VydmljZXMxFjAUBgNVBAMTDUFk" + "b2JlIFJvb3QgQ0ExDTALBgNVBAMTBENSTDEwKwYDVR0QBCQwIoAPMjAwMzAxMDgy"
            + "MzM3MjNagQ8yMDIzMDEwOTAwMDcyM1owCwYDVR0PBAQDAgEGMB8GA1UdIwQYMBaA" + "FIK3OEqTqpsQ74C72VTi8Q/7gJzeMB0GA1UdDgQWBBSCtzhKk6qbEO+Au9lU4vEP"
            + "+4Cc3jAMBgNVHRMEBTADAQH/MB0GCSqGSIb2fQdBAAQQMA4bCFY2LjA6NC4wAwIE" + "kDANBgkqhkiG9w0BAQUFAAOCAQEAMtqfQ3XB+m/Jb9urHTY3PrxhGTa3AjwdI1mY"
            + "bJ7uTYXnVMggH6fUu+K/AHd9JGtwL1zBOnZJtdPgI4QqcWoi88EnKZgV9jWQ5ARM" + "w428n2Ec5/0kjNFEQ4wWuptNpdQ1L7wRzr33UTeNn5DkFPEYP77pWRI1+TOS857g"
            + "1WuacZuZS8hxw+GxYQnE5fqR8EI6N300+XLozapiHCHp1fSCEON7BbYtaFYLfn6S" + "LG9NcoIM7VZ0sp25qy0rHRBf2yd1cI/9HdfiAqB55Rzl/69kQFEtnptH20KlfB/C"
            + "pkiw176SaU2k9ilXxXgRGNyHUcoTsmKdTysyvTGlwfpSqwWIyA==" + "\n-----END CERTIFICATE-----";

    private static byte[] testcrl = Base64.decode(("MIHGMHICAQEwDQYJKoZIhvcNAQELBQAwDzENMAsGA1UEAwwEVEVTVBcNMTEwMTMx"
    		+"MTMzOTE3WhcNMTEwMTMxMTMzOTE3WqAvMC0wHwYDVR0jBBgwFoAUt39s38+I8fP0"
    		+"diUs8Y8TYtCar8gwCgYDVR0UBAMCAQEwDQYJKoZIhvcNAQELBQADQQBcr4CF0sy3"
    		+"5sVvEafzh67itIasqcv/PwUT6DwQxoiX85h53cFtvXQxi/2Xqn+PaNBOqWShByX7"
    		+"TQlMX0Bmoz9/").getBytes());

    private static byte[] testdeltacrl = Base64.decode(("MIHWMIGBAgEBMA0GCSqGSIb3DQEBCwUAMA8xDTALBgNVBAMMBFRFU1QXDTExMDEz"
    		+"MTEzNDcxOFoXDTExMDEzMTEzNDcxOFqgPjA8MB8GA1UdIwQYMBaAFJ5BHYGqJr3K"
    		+"j9IMQxmMP6ad8gDdMAoGA1UdFAQDAgEDMA0GA1UdGwEB/wQDAgECMA0GCSqGSIb3"
    		+"DQEBCwUAA0EAP8CIPLll5m/wmhcLL5SXlb+aYrPGsUlBFNBKYKO0iV1QjBHeDMp5"
    		+"z70nU3g2tIfiEX4IKNFyzFvn5m6e8m0JQQ==").getBytes());

    @Before
    public void setUp() throws Exception {
        CryptoProviderTools.installBCProvider();
    }

    /**
     * DOCUMENT ME!
     * 
     * @throws Exception
     *             DOCUMENT ME!
     */
    @Test
    public void test01GetPartFromDN() throws Exception {
        log.trace(">test01GetPartFromDN()");

        // We try to examine the general case and some special cases, which we
        // want to be able to handle
        String dn0 = "C=SE, O=AnaTom, CN=foo";
        assertEquals(CertTools.getPartFromDN(dn0, "CN"), "foo");
        assertEquals(CertTools.getPartFromDN(dn0, "O"), "AnaTom");
        assertEquals(CertTools.getPartFromDN(dn0, "C"), "SE");
        assertEquals(CertTools.getPartFromDN(dn0, "cn"), "foo");
        assertEquals(CertTools.getPartFromDN(dn0, "o"), "AnaTom");
        assertEquals(CertTools.getPartFromDN(dn0, "c"), "SE");

        String dn1 = "c=SE, o=AnaTom, cn=foo";
        assertEquals(CertTools.getPartFromDN(dn1, "CN"), "foo");
        assertEquals(CertTools.getPartFromDN(dn1, "O"), "AnaTom");
        assertEquals(CertTools.getPartFromDN(dn1, "C"), "SE");
        assertEquals(CertTools.getPartFromDN(dn1, "cn"), "foo");
        assertEquals(CertTools.getPartFromDN(dn1, "o"), "AnaTom");
        assertEquals(CertTools.getPartFromDN(dn1, "c"), "SE");

        String dn2 = "C=SE, O=AnaTom, CN=cn";
        assertEquals(CertTools.getPartFromDN(dn2, "CN"), "cn");

        String dn3 = "C=SE, O=AnaTom, CN=CN";
        assertEquals(CertTools.getPartFromDN(dn3, "CN"), "CN");

        String dn4 = "C=CN, O=AnaTom, CN=foo";
        assertEquals(CertTools.getPartFromDN(dn4, "CN"), "foo");

        String dn5 = "C=cn, O=AnaTom, CN=foo";
        assertEquals(CertTools.getPartFromDN(dn5, "CN"), "foo");

        String dn6 = "CN=foo, O=PrimeKey, C=SE";
        assertEquals(CertTools.getPartFromDN(dn6, "CN"), "foo");
        assertEquals(CertTools.getPartFromDN(dn6, "O"), "PrimeKey");
        assertEquals(CertTools.getPartFromDN(dn6, "C"), "SE");

        String dn7 = "CN=foo, O=PrimeKey, C=cn";
        assertEquals(CertTools.getPartFromDN(dn7, "CN"), "foo");
        assertEquals(CertTools.getPartFromDN(dn7, "C"), "cn");

        String dn8 = "CN=foo, O=PrimeKey, C=CN";
        assertEquals(CertTools.getPartFromDN(dn8, "CN"), "foo");
        assertEquals(CertTools.getPartFromDN(dn8, "C"), "CN");

        String dn9 = "CN=foo, O=CN, C=CN";
        assertEquals(CertTools.getPartFromDN(dn9, "CN"), "foo");
        assertEquals(CertTools.getPartFromDN(dn9, "O"), "CN");

        String dn10 = "CN=foo, CN=bar,O=CN, C=CN";
        assertEquals(CertTools.getPartFromDN(dn10, "CN"), "foo");
        assertEquals(CertTools.getPartFromDN(dn10, "O"), "CN");

        String dn11 = "CN=foo,CN=bar, O=CN, C=CN";
        assertEquals(CertTools.getPartFromDN(dn11, "CN"), "foo");
        assertEquals(CertTools.getPartFromDN(dn11, "O"), "CN");

        String dn12 = "CN=\"foo, OU=bar\", O=baz\\\\\\, quux,C=C";
        assertEquals("Extraction of CN from: "+dn12, "foo, OU=bar", CertTools.getPartFromDN(dn12, "CN"));
        // This is the old test, that seems invalid
        // assertEquals("Extraction of O from: "+dn12, "baz\\, quux", CertTools.getPartFromDN(dn12, "O"));
        assertEquals("Extraction of O from: "+dn12, "baz\\\\\\, quux", CertTools.getPartFromDN(dn12, "O"));
        assertNull(CertTools.getPartFromDN(dn12, "OU"));

        String dn13 = "C=SE, O=PrimeKey, EmailAddress=foo@primekey.se";
        ArrayList<String> emails = CertTools.getEmailFromDN(dn13);
        assertEquals((String) emails.get(0), "foo@primekey.se");

        String dn14 = "C=SE, E=foo@primekey.se, O=PrimeKey";
        emails = CertTools.getEmailFromDN(dn14);
        assertEquals((String) emails.get(0), "foo@primekey.se");

        String dn15 = "C=SE, E=foo@primekey.se, O=PrimeKey, EmailAddress=bar@primekey.se";
        emails = CertTools.getEmailFromDN(dn15);
        assertEquals((String) emails.get(0), "bar@primekey.se");

        log.trace("<test01GetPartFromDN()");
    }

    @Test
    public void test02StringToBCDNString() throws Exception {
        log.trace(">test02StringToBCDNString()");

        // We try to examine the general case and som special cases, which we
        // want to be able to handle
        String dn1 = "C=SE, O=AnaTom, CN=foo";
        assertEquals(CertTools.stringToBCDNString(dn1), "CN=foo,O=AnaTom,C=SE");

        String dn2 = "C=SE, O=AnaTom, CN=cn";
        assertEquals(CertTools.stringToBCDNString(dn2), "CN=cn,O=AnaTom,C=SE");

        String dn3 = "CN=foo, O=PrimeKey, C=SE";
        assertEquals(CertTools.stringToBCDNString(dn3), "CN=foo,O=PrimeKey,C=SE");

        String dn4 = "cn=foo, o=PrimeKey, c=SE";
        assertEquals(CertTools.stringToBCDNString(dn4), "CN=foo,O=PrimeKey,C=SE");

        String dn5 = "cn=foo,o=PrimeKey,c=SE";
        assertEquals(CertTools.stringToBCDNString(dn5), "CN=foo,O=PrimeKey,C=SE");

        String dn6 = "C=SE, O=AnaTom, CN=CN";
        assertEquals(CertTools.stringToBCDNString(dn6), "CN=CN,O=AnaTom,C=SE");

        String dn7 = "C=CN, O=AnaTom, CN=foo";
        assertEquals(CertTools.stringToBCDNString(dn7), "CN=foo,O=AnaTom,C=CN");

        String dn8 = "C=cn, O=AnaTom, CN=foo";
        assertEquals(CertTools.stringToBCDNString(dn8), "CN=foo,O=AnaTom,C=cn");

        String dn9 = "CN=foo, O=PrimeKey, C=CN";
        assertEquals(CertTools.stringToBCDNString(dn9), "CN=foo,O=PrimeKey,C=CN");

        String dn10 = "CN=foo, O=PrimeKey, C=cn";
        assertEquals(CertTools.stringToBCDNString(dn10), "CN=foo,O=PrimeKey,C=cn");

        String dn11 = "CN=foo, O=CN, C=CN";
        assertEquals(CertTools.stringToBCDNString(dn11), "CN=foo,O=CN,C=CN");

        String dn12 = "O=PrimeKey,C=SE,CN=CN";
        assertEquals(CertTools.stringToBCDNString(dn12), "CN=CN,O=PrimeKey,C=SE");

        String dn13 = "O=PrimeKey,C=SE,CN=CN, OU=FooOU";
        assertEquals(CertTools.stringToBCDNString(dn13), "CN=CN,OU=FooOU,O=PrimeKey,C=SE");

        String dn14 = "O=PrimeKey,C=CN,CN=CN, OU=FooOU";
        assertEquals(CertTools.stringToBCDNString(dn14), "CN=CN,OU=FooOU,O=PrimeKey,C=CN");

        String dn15 = "O=PrimeKey,C=CN,CN=cn, OU=FooOU";
        assertEquals(CertTools.stringToBCDNString(dn15), "CN=cn,OU=FooOU,O=PrimeKey,C=CN");

        String dn16 = "CN=foo, CN=bar,O=CN, C=CN";
        assertEquals(CertTools.stringToBCDNString(dn16), "CN=foo,CN=bar,O=CN,C=CN");

        String dn17 = "CN=foo,CN=bar, O=CN, O=C, C=CN";
        assertEquals(CertTools.stringToBCDNString(dn17), "CN=foo,CN=bar,O=CN,O=C,C=CN");

        String dn18 = "cn=jean,cn=EJBCA,dc=home,dc=jean";
        assertEquals(CertTools.stringToBCDNString(dn18), "CN=jean,CN=EJBCA,DC=home,DC=jean");

        String dn19 = "cn=bar, cn=foo,o=oo, O=EJBCA,DC=DC2, dc=dc1, C=SE";
        assertEquals(CertTools.stringToBCDNString(dn19), "CN=bar,CN=foo,O=oo,O=EJBCA,DC=DC2,DC=dc1,C=SE");

        String dn20 = " CN=\"foo, OU=bar\",  O=baz\\\\\\, quux,C=SE ";
        // BC always escapes with backslash, it doesn't use quotes.
        assertEquals("Conversion of: "+dn20, "CN=foo\\, OU\\=bar,O=baz\\\\\\, quux,C=SE", CertTools.stringToBCDNString(dn20));

        String dn21 = "C=SE,O=Foo\\, Inc, OU=Foo\\, Dep, CN=Foo\\'";
        String bcdn21 = CertTools.stringToBCDNString(dn21);
        assertEquals(bcdn21, "CN=Foo\',OU=Foo\\, Dep,O=Foo\\, Inc,C=SE");
        // it is allowed to escape ,
        assertEquals(StringTools.strip(bcdn21), "CN=Foo',OU=Foo\\, Dep,O=Foo\\, Inc,C=SE");

        String dn22 = "C=SE,O=Foo\\, Inc, OU=Foo, Dep, CN=Foo'";
        String bcdn22 = CertTools.stringToBCDNString(dn22);
        assertEquals(bcdn22, "CN=Foo',OU=Foo,O=Foo\\, Inc,C=SE");
        assertEquals(StringTools.strip(bcdn22), "CN=Foo',OU=Foo,O=Foo\\, Inc,C=SE");

        String dn23 = "C=SE,O=Foo, OU=FooOU, CN=Foo, DN=qualf";
        String bcdn23 = CertTools.stringToBCDNString(dn23);
        assertEquals(bcdn23, "DN=qualf,CN=Foo,OU=FooOU,O=Foo,C=SE");
        assertEquals(StringTools.strip(bcdn23), "DN=qualf,CN=Foo,OU=FooOU,O=Foo,C=SE");

        String dn24 = "telephonenumber=08555-666,businesscategory=Surf boards,postaladdress=Stockholm,postalcode=11122,CN=foo,CN=bar, O=CN, O=C, C=CN";
        assertEquals(CertTools.stringToBCDNString(dn24),
                "TelephoneNumber=08555-666,PostalAddress=Stockholm,BusinessCategory=Surf boards,PostalCode=11122,CN=foo,CN=bar,O=CN,O=C,C=CN");

        // This isn't a legal SubjectDN. Since legacy BC did not support multivalues, we assume that the user meant \+.
        String dn25 = "CN=user+name, C=CN";
        assertEquals("CN=user\\+name,C=CN", CertTools.stringToBCDNString(dn25));

        String dn26 = "CN=user\\+name, C=CN";
        assertEquals("CN=user\\+name,C=CN", CertTools.stringToBCDNString(dn26));
        
        String dn27 = "CN=test123456, O=\\\"foo+b\\+ar\\, C=SE\\\"";
        assertEquals("CN=test123456,O=\\\"foo\\+b\\+ar\\, C\\=SE\\\"", CertTools.stringToBCDNString(dn27));
    }

    @Test
    public void test03AltNames() throws Exception {
        log.trace(">test03AltNames()");

        // We try to examine the general case and som special cases, which we
        // want to be able to handle
        String alt1 = "rfc822Name=ejbca@primekey.se, dNSName=www.primekey.se, uri=http://www.primekey.se/ejbca";
        assertEquals(CertTools.getPartFromDN(alt1, CertTools.EMAIL), "ejbca@primekey.se");
        assertNull(CertTools.getPartFromDN(alt1, CertTools.EMAIL1));
        assertNull(CertTools.getPartFromDN(alt1, CertTools.EMAIL2));
        assertEquals(CertTools.getPartFromDN(alt1, CertTools.DNS), "www.primekey.se");
        assertNull(CertTools.getPartFromDN(alt1, CertTools.URI));
        assertEquals(CertTools.getPartFromDN(alt1, CertTools.URI1), "http://www.primekey.se/ejbca");

        String alt2 = "email=ejbca@primekey.se, dNSName=www.primekey.se, uniformResourceIdentifier=http://www.primekey.se/ejbca";
        assertEquals(CertTools.getPartFromDN(alt2, CertTools.EMAIL1), "ejbca@primekey.se");
        assertEquals(CertTools.getPartFromDN(alt2, CertTools.URI), "http://www.primekey.se/ejbca");

        String alt3 = "EmailAddress=ejbca@primekey.se, dNSName=www.primekey.se, uniformResourceIdentifier=http://www.primekey.se/ejbca";
        assertEquals(CertTools.getPartFromDN(alt3, CertTools.EMAIL2), "ejbca@primekey.se");

        Certificate cert = CertTools.getCertfromByteArray(guidcert);
        String upn = CertTools.getUPNAltName(cert);
        assertEquals(upn, "guid@foo.com");
        String guid = CertTools.getGuidAltName(cert);
        assertEquals("1234567890abcdef", guid);
        String altName = CertTools.getSubjectAlternativeName(cert);
        // The returned string does not always have the same order so we can't compare strings directly
        assertTrue(altName.contains("guid=1234567890abcdef"));
        assertTrue(altName.contains("rfc822name=guid@foo.com"));
        assertTrue(altName.contains("upn=guid@foo.com"));
        assertTrue(altName.contains("dNSName=guid.foo.com"));
        assertTrue(altName.contains("iPAddress=10.12.13.14"));
        assertTrue(altName.contains("uniformResourceIdentifier=http://guid.foo.com/"));
        assertFalse(altName.contains("foobar"));
        GeneralNames gns = CertTools.getGeneralNamesFromAltName(altName);
        assertNotNull(gns);
        
        // Test cert containing permanentIdentifier
        cert = CertTools.getCertfromByteArray(permanentIdentifierCert);
        upn = CertTools.getUPNAltName(cert);
        assertEquals("upn1@example.com", upn);
        String permanentIdentifier = CertTools.getPermanentIdentifierAltName(cert);
        assertEquals("identifier 10003/1.2.3.4.5.6", permanentIdentifier);

        String customAlt = "rfc822Name=foo@bar.com";
        ArrayList<String> oids = CertTools.getCustomOids(customAlt);
        assertEquals(0, oids.size());
        customAlt = "rfc822Name=foo@bar.com, 1.1.1.1.2=foobar, 1.2.2.2.2=barfoo";
        oids = CertTools.getCustomOids(customAlt);
        assertEquals(2, oids.size());
        String oid1 = (String) oids.get(0);
        assertEquals("1.1.1.1.2", oid1);
        String oid2 = (String) oids.get(1);
        assertEquals("1.2.2.2.2", oid2);
        String val1 = CertTools.getPartFromDN(customAlt, oid1);
        assertEquals("foobar", val1);
        String val2 = CertTools.getPartFromDN(customAlt, oid2);
        assertEquals("barfoo", val2);

        customAlt = "rfc822Name=foo@bar.com, 1.1.1.1.2=foobar, 1.1.1.1.2=barfoo";
        oids = CertTools.getCustomOids(customAlt);
        assertEquals(1, oids.size());
        oid1 = (String) oids.get(0);
        assertEquals("1.1.1.1.2", oid1);
        List<String> list = CertTools.getPartsFromDN(customAlt, oid1);
        assertEquals(2, list.size());
        val1 = (String) list.get(0);
        assertEquals("foobar", val1);
        val2 = (String) list.get(1);
        assertEquals("barfoo", val2);

        log.trace("<test03AltNames()");
    }

    @Test
    public void test04DNComponents() throws Exception {
        log.trace(">test04DNComponents()");

        // We try to examine the general case and som special cases, which we
        // want to be able to handle
        String dn1 = "CN=CommonName, O=Org, OU=OrgUnit, SerialNumber=SerialNumber, SurName=SurName, GivenName=GivenName, Initials=Initials, C=SE";
        String bcdn1 = CertTools.stringToBCDNString(dn1);
        log.debug("dn1: " + dn1);
        log.debug("bcdn1: " + bcdn1);
        assertEquals("CN=CommonName,SN=SerialNumber,GIVENNAME=GivenName,INITIALS=Initials,SURNAME=SurName,OU=OrgUnit,O=Org,C=SE", bcdn1);

        dn1 = "CN=CommonName, O=Org, OU=OrgUnit, SerialNumber=SerialNumber, SurName=SurName, GivenName=GivenName,"
                +" Initials=Initials, C=SE, 1.1.1.1=1111Oid, 2.2.2.2=2222Oid";
        bcdn1 = CertTools.stringToBCDNString(dn1);
        log.debug("dn1: " + dn1);
        log.debug("bcdn1: " + bcdn1);
        assertEquals("CN=CommonName,SN=SerialNumber,GIVENNAME=GivenName,INITIALS=Initials,SURNAME=SurName,OU=OrgUnit,"
                +"O=Org,C=SE,2.2.2.2=2222Oid,1.1.1.1=1111Oid", bcdn1);

        dn1 = "CN=CommonName, 3.3.3.3=3333Oid,O=Org, OU=OrgUnit, SerialNumber=SerialNumber, SurName=SurName,"+
                " GivenName=GivenName, Initials=Initials, C=SE, 1.1.1.1=1111Oid, 2.2.2.2=2222Oid";
        bcdn1 = CertTools.stringToBCDNString(dn1);
        log.debug("dn1: " + dn1);
        log.debug("bcdn1: " + bcdn1);
        // 3.3.3.3 is not a valid OID so it should be silently dropped
        assertEquals("CN=CommonName,SN=SerialNumber,GIVENNAME=GivenName,INITIALS=Initials,SURNAME=SurName,"
                        +"OU=OrgUnit,O=Org,C=SE,2.2.2.2=2222Oid,1.1.1.1=1111Oid", bcdn1);

        dn1 = "CN=CommonName, 2.3.3.3=3333Oid,O=Org, K=KKK, OU=OrgUnit, SerialNumber=SerialNumber, SurName=SurName,"
                +" GivenName=GivenName, Initials=Initials, C=SE, 1.1.1.1=1111Oid, 2.2.2.2=2222Oid";
        bcdn1 = CertTools.stringToBCDNString(dn1);
        log.debug("dn1: " + dn1);
        log.debug("bcdn1: " + bcdn1);
        assertEquals(
                bcdn1,
                "CN=CommonName,SN=SerialNumber,GIVENNAME=GivenName,INITIALS=Initials,SURNAME=SurName,OU=OrgUnit,O=Org,C=SE,2.2.2.2=2222Oid,1.1.1.1=1111Oid,2.3.3.3=3333Oid");

        log.trace("<test04DNComponents()");
    }

    /**
     * Tests string coding/decoding international (swedish characters)
     * 
     * @throws Exception
     *             if error...
     */
    @Test
    public void test05IntlChars() throws Exception {
        log.trace(">test05IntlChars()");
        // We try to examine the general case and som special cases, which we
        // want to be able to handle
        String dn1 = "CN=Tomas?????????, O=?????????-Org, OU=??????-Unit, C=SE";
        String bcdn1 = CertTools.stringToBCDNString(dn1);
        log.debug("dn1: " + dn1);
        log.debug("bcdn1: " + bcdn1);
        assertEquals("CN=Tomas?????????,OU=??????-Unit,O=?????????-Org,C=SE", bcdn1);
        log.trace("<test05IntlChars()");
    }

    /**
     * Tests some of the other methods of CertTools
     * 
     * @throws Exception
     *             if error...
     */
    @Test
    public void test06CertOps() throws Exception {
        log.trace(">test06CertOps()");
        Certificate cert = CertTools.getCertfromByteArray(testcert);
        assertFalse(CertTools.isCA(cert));
        Certificate gcert = CertTools.getCertfromByteArray(guidcert);
        assertEquals("Wrong issuerDN", CertTools.getIssuerDN(cert), CertTools.stringToBCDNString("CN=TestCA,O=AnaTom,C=SE"));
        assertEquals("Wrong subjectDN", CertTools.getSubjectDN(cert), CertTools.stringToBCDNString("CN=p12test,O=PrimeTest,C=SE"));
        assertEquals("Wrong subject key id", new String(Hex.encode(CertTools.getSubjectKeyId(cert))),
                "E74F5690F48D147783847CD26448E8094ABB08A0".toLowerCase());
        assertEquals("Wrong authority key id", new String(Hex.encode(CertTools.getAuthorityKeyId(cert))),
                "637BF476A854248EA574A57744A6F45E0F579251".toLowerCase());
        assertEquals("Wrong upn alt name", "foo@foo", CertTools.getUPNAltName(cert));
        assertEquals("Wrong guid alt name", "1234567890abcdef", CertTools.getGuidAltName(gcert));
        assertEquals("Wrong certificate policy", "1.1.1.1.1.1", CertTools.getCertificatePolicyId(cert, 0));
        assertNull("Not null policy", CertTools.getCertificatePolicyId(cert, 1));
        // log.debug(cert);
        // FileOutputStream fos = new FileOutputStream("foo.cert");
        // fos.write(cert.getEncoded());
        // fos.close();
        log.trace("<test06CertOps()");
    }

    /**
     * Tests the handling of DC components
     * 
     * @throws Exception
     *             if error...
     */
    @Test
    public void test07TestDC() throws Exception {
        log.trace(">test07TestDC()");
        // We try to examine the that we handle modern dc components for ldap
        // correctly
        String dn1 = "dc=bigcorp,dc=com,dc=se,ou=users,cn=Mike Jackson";
        String bcdn1 = CertTools.stringToBCDNString(dn1);
        log.debug("dn1: " + dn1);
        log.debug("bcdn1: " + bcdn1);
        // assertEquals("CN=Mike Jackson,OU=users,DC=se,DC=bigcorp,DC=com",
        // bcdn1);
        String dn2 = "cn=Mike Jackson,ou=users,dc=se,dc=bigcorp,dc=com";
        String bcdn2 = CertTools.stringToBCDNString(dn2);
        log.debug("dn2: " + dn2);
        log.debug("bcdn2: " + bcdn2);
        assertEquals("CN=Mike Jackson,OU=users,DC=se,DC=bigcorp,DC=com", bcdn2);
        log.trace("<test07TestDC()");
    }

    /**
     * Tests the handling of unstructuredName/Address
     * 
     * @throws Exception
     *             if error...
     */
    @Test
    public void test08TestUnstructured() throws Exception {
        log.trace(">test08TestUnstructured()");
        // We try to examine the that we handle modern dc components for ldap
        // correctly
        String dn1 = "C=SE,O=PrimeKey,unstructuredName=10.1.1.2,unstructuredAddress=foo.bar.se,cn=test";
        String bcdn1 = CertTools.stringToBCDNString(dn1);
        log.debug("dn1: " + dn1);
        log.debug("bcdn1: " + bcdn1);
        assertEquals("unstructuredAddress=foo.bar.se,unstructuredName=10.1.1.2,CN=test,O=PrimeKey,C=SE", bcdn1);
        log.trace("<test08TestUnstructured()");
    }

    /**
     * Tests the reversing of a DN
     * 
     * @throws Exception
     *             if error...
     */
    @Test
    public void test09TestReverseDN() throws Exception {
        log.trace(">test09TestReverse()");
        // We try to examine the that we handle modern dc components for ldap
        // correctly
        String dn1 = "dc=com,dc=bigcorp,dc=se,ou=orgunit,ou=users,cn=Tomas G";
        String dn2 = "cn=Tomas G,ou=users,ou=orgunit,dc=se,dc=bigcorp,dc=com";
        assertTrue(CertTools.isDNReversed(dn1));
        assertTrue(!CertTools.isDNReversed(dn2));
        assertTrue(CertTools.isDNReversed("C=SE,CN=Foo"));
        assertTrue(!CertTools.isDNReversed("CN=Foo,O=FooO"));
        String revdn1 = CertTools.reverseDN(dn1);
        log.debug("dn1: " + dn1);
        log.debug("revdn1: " + revdn1);
        assertEquals(dn2, revdn1);

        String dn3 = "cn=toto,cn=titi,dc=domain,dc=tld";
        String revdn3 = CertTools.reverseDN(dn3);
        assertEquals("dc=tld,dc=domain,cn=titi,cn=toto", revdn3);
        
        X500Name dn4 = CertTools.stringToBcX500Name(dn3, new X509DefaultEntryConverter(), true);
        assertEquals("CN=toto,CN=titi,DC=domain,DC=tld", dn4.toString());
        X500Name dn5 = CertTools.stringToBcX500Name(dn3, new X509DefaultEntryConverter(), false);
        assertEquals("DC=tld,DC=domain,CN=titi,CN=toto", dn5.toString());
        assertEquals("CN=toto,CN=titi,DC=domain,DC=tld", CertTools.stringToBCDNString(dn3));

        String dn6 = "dc=tld,dc=domain,cn=titi,cn=toto";
        String revdn6 = CertTools.reverseDN(dn6);
        assertEquals("cn=toto,cn=titi,dc=domain,dc=tld", revdn6);
        assertEquals("CN=toto,CN=titi,DC=domain,DC=tld", CertTools.stringToBCDNString(dn3));

        X500Name dn7 = CertTools.stringToBcX500Name(dn6, new X509DefaultEntryConverter(), true);
        assertEquals("CN=toto,CN=titi,DC=domain,DC=tld", dn7.toString());
        X500Name revdn7 = CertTools.stringToBcX500Name(dn6, new X509DefaultEntryConverter(), false);
        assertEquals("DC=tld,DC=domain,CN=titi,CN=toto", revdn7.toString());

        // Test the test strings from ECA-1699, to prove that we fixed this issue
        String dn8 = "dc=org,dc=foo,o=FOO,cn=FOO Root CA";
        String dn9 = "cn=FOO Root CA,o=FOO,dc=foo,dc=org";
        String revdn8 = CertTools.reverseDN(dn8);
        assertEquals("cn=FOO Root CA,o=FOO,dc=foo,dc=org", revdn8);
        String revdn9 = CertTools.reverseDN(dn9);
        assertEquals("dc=org,dc=foo,o=FOO,cn=FOO Root CA", revdn9);
        X500Name xdn8ldap = CertTools.stringToBcX500Name(dn8, new X509DefaultEntryConverter(), true);
        X500Name xdn8x500 = CertTools.stringToBcX500Name(dn8, new X509DefaultEntryConverter(), false);
        assertEquals("CN=FOO Root CA,O=FOO,DC=foo,DC=org", xdn8ldap.toString());
        assertEquals("DC=org,DC=foo,O=FOO,CN=FOO Root CA", xdn8x500.toString());
        X500Name xdn9ldap = CertTools.stringToBcX500Name(dn9, new X509DefaultEntryConverter(), true);
        X500Name xdn9x500 = CertTools.stringToBcX500Name(dn9, new X509DefaultEntryConverter(), false);
        assertEquals("CN=FOO Root CA,O=FOO,DC=foo,DC=org", xdn9ldap.toString());
        assertEquals("DC=org,DC=foo,O=FOO,CN=FOO Root CA", xdn9x500.toString());
        assertEquals("CN=FOO Root CA,O=FOO,DC=foo,DC=org", CertTools.stringToBCDNString(dn8));
        assertEquals("CN=FOO Root CA,O=FOO,DC=foo,DC=org", CertTools.stringToBCDNString(dn9));

        // Test reversing DNs with multiple OU
        String dn10 = "CN=something,OU=A,OU=B,O=someO,C=SE";
        X500Name x500dn10 = CertTools.stringToBcX500Name(dn10, new X509DefaultEntryConverter(), true);
        assertEquals("CN=something,OU=A,OU=B,O=someO,C=SE", x500dn10.toString());
        assertEquals("CN=something,OU=A,OU=B,O=someO,C=SE", CertTools.stringToBCDNString(dn10));

        // When we order forwards (LdapOrder) from the beginning, and request !LdapOrder, everything should be reversed
        X500Name ldapdn11 = CertTools.stringToBcX500Name(dn10, new X509DefaultEntryConverter(), false);
        assertEquals("C=SE,O=someO,OU=B,OU=A,CN=something", ldapdn11.toString());

        // When we order backwards (X.509, !LdapOrder) from the beginning, we should not reorder anything
        String dn11 = "C=SE,O=someO,OU=B,OU=A,CN=something";
        X500Name x500dn11 = CertTools.stringToBcX500Name(dn11, new X509DefaultEntryConverter(), false);
        assertEquals("C=SE,O=someO,OU=B,OU=A,CN=something", x500dn11.toString());
        assertEquals("CN=something,OU=A,OU=B,O=someO,C=SE", CertTools.stringToBCDNString(dn11));

        log.trace("<test09TestReverse()");
    }

    /**
     * Tests the handling of DC components
     * 
     * @throws Exception
     *             if error...
     */
    @Test
    public void test10TestMultipleReversed() throws Exception {
        log.trace(">test10TestMultipleReversed()");
        // We try to examine the that we handle modern dc components for ldap
        // correctly
        String dn1 = "dc=com,dc=bigcorp,dc=se,ou=orgunit,ou=users,cn=Tomas G";
        String bcdn1 = CertTools.stringToBCDNString(dn1);
        log.debug("dn1: " + dn1);
        log.debug("bcdn1: " + bcdn1);
        assertEquals("CN=Tomas G,OU=users,OU=orgunit,DC=se,DC=bigcorp,DC=com", bcdn1);

        String dn19 = "C=SE, dc=dc1,DC=DC2,O=EJBCA, O=oo, cn=foo, cn=bar";
        assertEquals("CN=bar,CN=foo,O=oo,O=EJBCA,DC=DC2,DC=dc1,C=SE", CertTools.stringToBCDNString(dn19));
        String dn20 = " C=SE,CN=\"foo, OU=bar\",  O=baz\\\\\\, quux  ";
        // BC always escapes with backslash, it doesn't use quotes.
        assertEquals("Conversion of: " + dn20, "CN=foo\\, OU\\=bar,O=baz\\\\\\, quux,C=SE", CertTools.stringToBCDNString(dn20));

        String dn21 = "C=SE,O=Foo\\, Inc, OU=Foo\\, Dep, CN=Foo\\'";
        String bcdn21 = CertTools.stringToBCDNString(dn21);
        assertEquals("CN=Foo\',OU=Foo\\, Dep,O=Foo\\, Inc,C=SE", bcdn21);
        assertEquals("CN=Foo',OU=Foo\\, Dep,O=Foo\\, Inc,C=SE", StringTools.strip(bcdn21));
        log.trace("<test10TestMultipleReversed()");
    }

    /**
     * Tests the insertCNPostfix function
     * 
     * @throws Exception
     *             if error...
     */
    @Test
    public void test11TestInsertCNPostfix() throws Exception {
        log.trace(">test11TestInsertCNPostfix()");

        // Test the regular case with one CN beging replaced with " (VPN)"
        // postfix
        String dn1 = "CN=Tomas G,OU=users,OU=orgunit,DC=se,DC=bigcorp,DC=com";
        String cnpostfix1 = " (VPN)";
        String newdn1 = CertTools.insertCNPostfix(dn1, cnpostfix1);
        assertEquals("CN=Tomas G (VPN),OU=users,OU=orgunit,DC=se,DC=bigcorp,DC=com", newdn1);

        // Test case when CN doesn't exist
        String dn2 = "OU=users,OU=orgunit,DC=se,DC=bigcorp,DC=com";
        String newdn2 = CertTools.insertCNPostfix(dn2, cnpostfix1);
        assertEquals("OU=users,OU=orgunit,DC=se,DC=bigcorp,DC=com", newdn2);

        // Test case with two CNs in DN only first one should be replaced.
        String dn3 = "CN=Tomas G,CN=Bagare,OU=users,OU=orgunit,DC=se,DC=bigcorp,DC=com";
        String newdn3 = CertTools.insertCNPostfix(dn3, cnpostfix1);
        assertEquals("CN=Tomas G (VPN),CN=Bagare,OU=users,OU=orgunit,DC=se,DC=bigcorp,DC=com", newdn3);

        // Test case with two CNs in reversed DN
        String dn4 = "dc=com,dc=bigcorp,dc=se,ou=orgunit,ou=users,cn=Tomas G,CN=Bagare";
        String newdn4 = CertTools.insertCNPostfix(dn4, cnpostfix1);
        assertEquals("DC=com,DC=bigcorp,DC=se,OU=orgunit,OU=users,CN=Tomas G (VPN),CN=Bagare", newdn4);

        // Test case with two CNs in reversed DN
        String dn5 = "UID=tomas,CN=tomas,OU=users,OU=orgunit,DC=se,DC=bigcorp,DC=com";
        String cnpostfix5 = " (VPN)";
        String newdn5 = CertTools.insertCNPostfix(dn5, cnpostfix5);
        assertEquals("UID=tomas,CN=tomas (VPN),OU=users,OU=orgunit,DC=se,DC=bigcorp,DC=com", newdn5);

        log.trace("<test11TestInsertCNPostfix()");
    }

    /**
	 */
    @Test
    public void test12GetPartsFromDN() throws Exception {
        log.trace(">test01GetPartFromDN()");

        // We try to examine the general case and som special cases, which we
        // want to be able to handle
        String dn0 = "C=SE, O=AnaTom, CN=foo";
        assertEquals(CertTools.getPartsFromDN(dn0, "CN").size(), 1);
        assertTrue(CertTools.getPartsFromDN(dn0, "CN").contains("foo"));
        assertEquals(CertTools.getPartsFromDN(dn0, "O").size(), 1);
        assertTrue(CertTools.getPartsFromDN(dn0, "O").contains("AnaTom"));
        assertEquals(CertTools.getPartsFromDN(dn0, "C").size(), 1);
        assertTrue(CertTools.getPartsFromDN(dn0, "C").contains("SE"));
        assertEquals(CertTools.getPartsFromDN(dn0, "cn").size(), 1);
        assertTrue(CertTools.getPartsFromDN(dn0, "cn").contains("foo"));
        assertEquals(CertTools.getPartsFromDN(dn0, "o").size(), 1);
        assertTrue(CertTools.getPartsFromDN(dn0, "o").contains("AnaTom"));
        assertEquals(CertTools.getPartsFromDN(dn0, "c").size(), 1);
        assertTrue(CertTools.getPartsFromDN(dn0, "c").contains("SE"));

        String dn1 = "uri=http://www.a.se, C=SE, O=AnaTom, CN=foo";
        assertEquals(CertTools.getPartsFromDN(dn1, "CN").size(), 1);
        assertTrue(CertTools.getPartsFromDN(dn1, "CN").contains("foo"));
        assertEquals(CertTools.getPartsFromDN(dn1, CertTools.URI).size(), 0);
        assertEquals(CertTools.getPartsFromDN(dn1, CertTools.URI1).size(), 1);
        assertTrue(CertTools.getPartsFromDN(dn1, CertTools.URI1).contains("http://www.a.se"));

        String dn2 = "uri=http://www.a.se, uri=http://www.b.se, C=SE, O=AnaTom, CN=foo";
        assertEquals(CertTools.getPartsFromDN(dn2, "CN").size(), 1);
        assertTrue(CertTools.getPartsFromDN(dn2, "CN").contains("foo"));
        assertEquals(CertTools.getPartsFromDN(dn2, CertTools.URI1).size(), 2);
        assertTrue(CertTools.getPartsFromDN(dn2, CertTools.URI1).contains("http://www.a.se"));
        assertTrue(CertTools.getPartsFromDN(dn2, CertTools.URI1).contains("http://www.b.se"));

        log.trace("<test12GetPartsFromDN()");
    }

    @Test
    public void test13GetSubjectAltNameString() throws Exception {
        log.trace(">test13GetSubjectAltNameString()");

        String altNames = CertTools.getSubjectAlternativeName(CertTools.getCertfromByteArray(altNameCert));
        log.debug(altNames);
        String name = CertTools.getPartFromDN(altNames, CertTools.UPN);
        assertEquals("foo@a.se", name);
        assertEquals("foo@a.se", CertTools.getUPNAltName(CertTools.getCertfromByteArray(altNameCert)));
        name = CertTools.getPartFromDN(altNames, CertTools.URI);
        assertEquals("http://www.a.se/", name);
        name = CertTools.getPartFromDN(altNames, CertTools.EMAIL);
        assertEquals("tomas@a.se", name);
        name = CertTools.getEMailAddress(CertTools.getCertfromByteArray(altNameCert));
        assertEquals("tomas@a.se", name);
        name = CertTools.getEMailAddress(CertTools.getCertfromByteArray(testcert));
        assertNull(name);
        name = CertTools.getEMailAddress(null);
        assertNull(name);
        name = CertTools.getPartFromDN(altNames, CertTools.DNS);
        assertEquals("www.a.se", name);
        name = CertTools.getPartFromDN(altNames, CertTools.IPADDR);
        assertEquals("10.1.1.1", name);
        log.trace("<test13GetSubjectAltNameString()");
    }

    @Test
    public void test14QCStatement() throws Exception {
        Certificate cert = CertTools.getCertfromByteArray(qcRefCert);
        // log.debug(cert);
        assertEquals("rfc822name=municipality@darmstadt.de", QCStatementExtension.getQcStatementAuthorities(cert));
        Collection<String> ids = QCStatementExtension.getQcStatementIds(cert);
        assertTrue(ids.contains(RFC3739QCObjectIdentifiers.id_qcs_pkixQCSyntax_v2.getId()));
        Certificate cert2 = CertTools.getCertfromByteArray(qcPrimeCert);
        assertEquals("rfc822name=qc@primekey.se", QCStatementExtension.getQcStatementAuthorities(cert2));
        ids = QCStatementExtension.getQcStatementIds(cert2);
        assertTrue(ids.contains(RFC3739QCObjectIdentifiers.id_qcs_pkixQCSyntax_v1.getId()));
        assertTrue(ids.contains(ETSIQCObjectIdentifiers.id_etsi_qcs_QcCompliance.getId()));
        assertTrue(ids.contains(ETSIQCObjectIdentifiers.id_etsi_qcs_QcSSCD.getId()));
        assertTrue(ids.contains(ETSIQCObjectIdentifiers.id_etsi_qcs_LimiteValue.getId()));
        String limit = QCStatementExtension.getQcStatementValueLimit(cert2);
        assertEquals("50000 SEK", limit);
    }

    @Test
    public void test15AiaOcspUri() throws Exception {
        Certificate cert = CertTools.getCertfromByteArray(aiaCert);
        // log.debug(cert);
        assertEquals("http://localhost:8080/ejbca/publicweb/status/ocsp", CertTools.getAuthorityInformationAccessOcspUrl(cert));
    }

    @Test
    public void test16GetSubjectAltNameStringWithDirectoryName() throws Exception {
        log.trace(">test16GetSubjectAltNameStringWithDirectoryName()");

        Certificate cer = CertTools.getCertfromByteArray(altNameCertWithDirectoryName);
        String altNames = CertTools.getSubjectAlternativeName(cer);
        log.debug(altNames);

        String name = CertTools.getPartFromDN(altNames, CertTools.UPN);
        assertEquals("testDirName@jamador.pki.gva.es", name);
        assertEquals("testDirName@jamador.pki.gva.es", CertTools.getUPNAltName(cer));

        name = CertTools.getPartFromDN(altNames, CertTools.DIRECTORYNAME);
        assertEquals("CN=testDirName|dir|name", name.replace("cn=", "CN="));
        assertEquals(name.substring("CN=".length()), (new X500Name("CN=testDirName|dir|name").getRDNs()[0].getFirst().getValue()).toString());

        String altName = "rfc822name=foo@bar.se, uri=http://foo.bar.se, directoryName=" + LDAPDN.escapeRDN("CN=testDirName, O=Foo, OU=Bar, C=SE")
                + ", dnsName=foo.bar.se";
        GeneralNames san = CertTools.getGeneralNamesFromAltName(altName);
        GeneralName[] gns = san.getNames();
        boolean found = false;
        for (int i = 0; i < gns.length; i++) {
            int tag = gns[i].getTagNo();
            if (tag == 4) {
                found = true;
                ASN1Encodable enc = gns[i].getName();
                X500Name dir = (X500Name) enc;
                String str = dir.toString();
                log.debug("DirectoryName: " + str);
                assertEquals("CN=testDirName,O=Foo,OU=Bar,C=SE", str);
            }

        }
        assertTrue(found);

        altName = "rfc822name=foo@bar.se, rfc822name=foo@bar.com, uri=http://foo.bar.se, directoryName="
                + LDAPDN.escapeRDN("CN=testDirName, O=Foo, OU=Bar, C=SE") + ", dnsName=foo.bar.se, dnsName=foo.bar.com";
        san = CertTools.getGeneralNamesFromAltName(altName);
        gns = san.getNames();
        int dnscount = 0;
        int rfc822count = 0;
        for (int i = 0; i < gns.length; i++) {
            int tag = gns[i].getTagNo();
            if (tag == 2) {
                dnscount++;
                ASN1Encodable enc = gns[i].getName();
                DERIA5String dir = (DERIA5String) enc;
                String str = dir.getString();
                log.info("DnsName: " + str);
            }
            if (tag == 1) {
                rfc822count++;
                ASN1Encodable enc = gns[i].getName();
                DERIA5String dir = (DERIA5String) enc;
                String str = dir.getString();
                log.info("Rfc822Name: " + str);
            }

        }
        assertEquals(2, dnscount);
        assertEquals(2, rfc822count);
        log.trace("<test16GetSubjectAltNameStringWithDirectoryName()");
    }

    @Test
    public void test17SubjectDirectoryAttributes() throws Exception {
        log.trace(">test17SubjectDirectoryAttributes()");
        Certificate cer = CertTools.getCertfromByteArray(subjDirAttrCert);
        String ret = SubjectDirAttrExtension.getSubjectDirectoryAttributes(cer);
        assertEquals("countryOfCitizenship=TR", ret);
        cer = CertTools.getCertfromByteArray(subjDirAttrCert2);
        ret = SubjectDirAttrExtension.getSubjectDirectoryAttributes(cer);
        assertEquals("countryOfResidence=SE, countryOfCitizenship=SE, gender=M, placeOfBirth=Stockholm, dateOfBirth=19710425", ret);
        log.trace("<test17SubjectDirectoryAttributes()");
    }

    @Test
    public void test18DNSpaceTrimming() throws Exception {
        log.trace(">test18DNSpaceTrimming()");
        String dn1 = "CN=CommonName, O= Org,C=SE";
        String bcdn1 = CertTools.stringToBCDNString(dn1);
        log.debug("dn1: " + dn1);
        log.debug("bcdn1: " + bcdn1);
        assertEquals("CN=CommonName,O=Org,C=SE", bcdn1);

        dn1 = "CN=CommonName, O =Org,C=SE";
        bcdn1 = CertTools.stringToBCDNString(dn1);
        log.debug("dn1: " + dn1);
        log.debug("bcdn1: " + bcdn1);
        assertEquals("CN=CommonName,O=Org,C=SE", bcdn1);

        dn1 = "CN=CommonName, O = Org,C=SE";
        bcdn1 = CertTools.stringToBCDNString(dn1);
        log.debug("dn1: " + dn1);
        log.debug("bcdn1: " + bcdn1);
        assertEquals("CN=CommonName,O=Org,C=SE", bcdn1);
        log.trace("<test18DNSpaceTrimming()");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test19getAltNameStringFromExtension() throws Exception {
        PKCS10CertificationRequest p10 = new PKCS10CertificationRequest(p10ReqWithAltNames);
        CertificationRequestInfo info = p10.getCertificationRequestInfo();
        ASN1Set set = info.getAttributes();
        // The set of attributes contains a sequence of with type oid
        // PKCSObjectIdentifiers.pkcs_9_at_extensionRequest
        Enumeration<Object> en = set.getObjects();
        boolean found = false;
        while (en.hasMoreElements()) {
            ASN1Sequence seq = ASN1Sequence.getInstance(en.nextElement());
            ASN1ObjectIdentifier oid = (ASN1ObjectIdentifier) seq.getObjectAt(0);
            if (oid.equals(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest)) {
                // The object at position 1 is a SET of extensions
                DERSet s = (DERSet) seq.getObjectAt(1);
                Extensions exts = Extensions.getInstance(s.getObjectAt(0));
                Extension ext = exts.getExtension(Extension.subjectAlternativeName);
                if (ext != null) {
                    found = true;
                    String altNames = CertTools.getAltNameStringFromExtension(ext);
                    assertEquals("dNSName=ort3-kru.net.polisen.se, iPAddress=10.252.255.237", altNames);
                }
            }
        }
        assertTrue(found);

        p10 = new PKCS10CertificationRequest(p10ReqWithAltNames2);
        info = p10.getCertificationRequestInfo();
        set = info.getAttributes();
        // The set of attributes contains a sequence of with type oid
        // PKCSObjectIdentifiers.pkcs_9_at_extensionRequest

        en = set.getObjects();
        found = false;
        while (en.hasMoreElements()) {
            ASN1Sequence seq = ASN1Sequence.getInstance(en.nextElement());
            ASN1ObjectIdentifier oid = (ASN1ObjectIdentifier) seq.getObjectAt(0);
            if (oid.equals(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest)) {
                // The object at position 1 is a SET of extensions
                DERSet s = (DERSet) seq.getObjectAt(1);
                Extensions exts = Extensions.getInstance(s.getObjectAt(0));
                Extension ext = exts.getExtension(Extension.subjectAlternativeName);
                if (ext != null) {
                    found = true;
                    String altNames = CertTools.getAltNameStringFromExtension(ext);
                    assertEquals("dNSName=foo.bar.com, iPAddress=10.0.0.1", altNames);
                }
            }
        }
        assertTrue(found);

    }

    @Test
    public void test20cvcCert() throws Exception {
        Certificate cert = CertTools.getCertfromByteArray(cvccert);
        assertNotNull(cert);
        PublicKey pk = cert.getPublicKey();
        assertNotNull(pk);
        assertEquals("RSA", pk.getAlgorithm());
        if (pk instanceof RSAPublicKey) {
            BigInteger modulus = ((RSAPublicKey) pk).getModulus();
            int len = modulus.bitLength();
            assertEquals(1024, len);
        } else {
            assertTrue(false);
        }
        String subjectdn = CertTools.getSubjectDN(cert);
        assertEquals("CN=RPS,C=SE", subjectdn);
        String issuerdn = CertTools.getIssuerDN(cert);
        assertEquals("CN=RPS,C=SE", issuerdn);
        assertEquals("10110", CertTools.getSerialNumberAsString(cert));
        assertEquals("10110", CertTools.getSerialNumber(cert).toString());
        // Get signature field
        byte[] sign = CertTools.getSignature(cert);
        assertEquals(128, sign.length);
        // Check validity dates
        final long MAY5_0000_2008_GMT = 1209945600000L; 
        final long MAY5_0000_2008_GMT_MINUS1MS = 1209945599999L; 
        final long MAY5_2359_2010_GMT = 1273103999000L; 
        final long MAY5_2359_2010_GMT_PLUS1MS = 1273103999001L;
        
    	assertEquals(MAY5_0000_2008_GMT, CertTools.getNotBefore(cert).getTime());
    	assertEquals(MAY5_2359_2010_GMT, CertTools.getNotAfter(cert).getTime());
    	assertTrue(CertTools.isCA(cert));
        CardVerifiableCertificate cvcert = (CardVerifiableCertificate) cert;
        assertEquals("CVCA", cvcert.getCVCertificate().getCertificateBody().getAuthorizationTemplate().getAuthorizationField().getRole().name());
    	CertTools.checkValidity(cert, new Date(MAY5_0000_2008_GMT));
    	CertTools.checkValidity(cert, new Date(MAY5_2359_2010_GMT));
    	try {
    		CertTools.checkValidity(cert, new Date(MAY5_0000_2008_GMT_MINUS1MS));
    		assertTrue("Should throw", false);
    	} catch (CertificateNotYetValidException e) {
    		// NOPMD
    	}
    	try {
    		CertTools.checkValidity(cert, new Date(MAY5_2359_2010_GMT_PLUS1MS));
    		assertTrue("Should throw", false);
    	} catch (CertificateExpiredException e) {
    		// NOPMD
    	}    	

        // Serialization, CVC provider is installed by CryptoProviderTools.installBCProvider
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(cert);
        oos.close();
        baos.close();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        Object o = ois.readObject();
        Certificate ocert = (Certificate) o;
        assertEquals("CVC", ocert.getType());

        // Test CVC certificate request encoding
        CVCObject parsedObject = CertificateParser.parseCVCObject(cvcreq);
        CVCertificate req = (CVCertificate) parsedObject;
        PublicKey pubKey = req.getCertificateBody().getPublicKey();
        assertNotNull(pubKey);
        assertEquals("CVC", pubKey.getFormat());
        BigInteger modulus = ((RSAPublicKey) pk).getModulus();
        int len = modulus.bitLength();
        assertEquals(1024, len);

        // Test verification of an authenticated request
        parsedObject = CertificateParser.parseCVCObject(cvcreqrenew);
        CVCAuthenticatedRequest authreq = (CVCAuthenticatedRequest) parsedObject;
        try {
            authreq.verify(pubKey);
        } catch (Exception e) {
            assertTrue(false);
        }
        // Test verification of an authenticated request that fails
        parsedObject = CertificateParser.parseCVCObject(cvcreqrenew);
        authreq = (CVCAuthenticatedRequest) parsedObject;
        req = authreq.getRequest();
        try {
            authreq.verify(req.getCertificateBody().getPublicKey());
            assertTrue(false);
        } catch (Exception e) {
        }
        
        // IS cert
    	KeyPair keyPair = KeyTools.genKeys("prime192v1", "ECDSA");
        CAReferenceField caRef = new CAReferenceField("SE", "CAREF001", "00000");
        HolderReferenceField holderRef = new HolderReferenceField("SE", "HOLDERRE", "00000");
        CVCertificate cv = CertificateGenerator.createTestCertificate(keyPair.getPublic(), keyPair.getPrivate(), caRef, holderRef, "SHA1WithECDSA", AuthorizationRoleEnum.IS);
        CardVerifiableCertificate cvsha1 = new CardVerifiableCertificate(cv);
        assertFalse(CertTools.isCA(cvsha1));
    }

    @Test
    public void test21GenSelfCert() throws Exception {
        KeyPair kp = KeyTools.genKeys("1024", "RSA");
        Certificate cert = CertTools.genSelfCertForPurpose("CN=foo1", 10, null, kp.getPrivate(), kp.getPublic(),
                AlgorithmConstants.SIGALG_SHA256_WITH_RSA_AND_MGF1, true, X509KeyUsage.keyCertSign);
        assertNotNull(cert);
        PublicKey pk = cert.getPublicKey();
        assertNotNull(pk);
        assertEquals("RSA", pk.getAlgorithm());
        if (pk instanceof RSAPublicKey) {
            BigInteger modulus = ((RSAPublicKey) pk).getModulus();
            int len = modulus.bitLength();
            assertEquals(1024, len);
        } else {
            assertTrue(false);
        }
        assertTrue(CertTools.isCA(cert));
        String subjectdn = CertTools.getSubjectDN(cert);
        assertEquals("CN=foo1", subjectdn);
        String issuerdn = CertTools.getIssuerDN(cert);
        assertEquals("CN=foo1", issuerdn);
        
        // Get signature field
        byte[] sign = CertTools.getSignature(cert);
        assertEquals(128, sign.length);
    }

    @Test
    public void test22CreateCertChain() throws Exception {
        // Test creating a certificate chain for CVC CAs
        Certificate cvccertroot = CertTools.getCertfromByteArray(cvccertchainroot);
        Certificate cvccertsub = CertTools.getCertfromByteArray(cvccertchainsub);
        assertTrue(CertTools.isCA(cvccertsub)); // DV is a CA also
        assertTrue(CertTools.isCA(cvccertroot));

        ArrayList<Certificate> certlist = new ArrayList<Certificate>();
        certlist.add(cvccertsub);
        certlist.add(cvccertroot);
        Collection<Certificate> col = CertTools.createCertChain(certlist);
        assertEquals(2, col.size());
        Iterator<Certificate> iter = col.iterator();
        Certificate certsub = (Certificate) iter.next();
        assertEquals("CN=RPS,C=SE", CertTools.getSubjectDN(certsub));
        Certificate certroot = (Certificate) iter.next();
        assertEquals("CN=HSMCVCA,C=SE", CertTools.getSubjectDN(certroot));

        // Test creating a certificate chain for X509CAs
        Certificate x509certsubsub = CertTools.getCertfromByteArray(x509certchainsubsub);
        assertTrue(CertTools.isCA(x509certsubsub));
        Certificate x509certsub = CertTools.getCertfromByteArray(x509certchainsub);
        assertTrue(CertTools.isCA(x509certsub));
        Certificate x509certroot = CertTools.getCertfromByteArray(x509certchainroot);
        assertTrue(CertTools.isCA(x509certroot));
        certlist = new ArrayList<Certificate>();
        certlist.add(x509certsub);
        certlist.add(x509certroot);
        certlist.add(x509certsubsub);
        col = CertTools.createCertChain(certlist);
        assertEquals(3, col.size());
        iter = col.iterator();
        Certificate certsubsub = (Certificate) iter.next();
        assertEquals("CN=SubSubCA", CertTools.getSubjectDN(certsubsub));
        certsub = (Certificate) iter.next();
        assertEquals("CN=SubCA", CertTools.getSubjectDN(certsub));
        certroot = (Certificate) iter.next();
        assertEquals("CN=RootCA", CertTools.getSubjectDN(certroot));

    }

    @Test
    public void test23GenSelfCertDSA() throws Exception {
        KeyPair kp = KeyTools.genKeys("1024", "DSA");
        Certificate cert = CertTools.genSelfCertForPurpose("CN=foo1", 10, null, kp.getPrivate(), kp.getPublic(),
                AlgorithmConstants.SIGALG_SHA1_WITH_DSA, true, X509KeyUsage.keyCertSign);
        assertNotNull(cert);
        PublicKey pk = cert.getPublicKey();
        assertNotNull(pk);
        assertEquals("DSA", pk.getAlgorithm());
        assertTrue(pk instanceof DSAPublicKey);
        String subjectdn = CertTools.getSubjectDN(cert);
        assertEquals("CN=foo1", subjectdn);
        String issuerdn = CertTools.getIssuerDN(cert);
        assertEquals("CN=foo1", issuerdn);
    }

    @Test
    public void test24GetCrlDistributionPoint() throws Exception {
        log.trace(">test24GetCrlDistributionPoint()");

        Collection<Certificate> certs;
        URL url;
        // Test with normal cert
        try {
            certs = CertTools.getCertsFromPEM(new ByteArrayInputStream(CERT_WITH_URI.getBytes()));
            url = CertTools.getCrlDistributionPoint(certs.iterator().next());
            assertNotNull(url);
        } catch (CertificateParsingException ex) {
            fail("Exception: " + ex.getMessage());
        }
        // Test with cert that contains CDP without URI
        try {
            certs = CertTools.getCertsFromPEM(new ByteArrayInputStream(CERT_WITHOUT_URI.getBytes()));
            url = CertTools.getCrlDistributionPoint(certs.iterator().next());
            assertNull(url);
        } catch (CertificateParsingException ex) {
            fail("Exception: " + ex.getMessage());
        }

        log.trace("<test24GetCrlDistributionPoint()");
    }

    @Test
    public void testKrb5PrincipalName() throws Exception {
        String altName = "krb5principal=foo/bar@P.SE, upn=upn@u.com";
        GeneralNames gn = CertTools.getGeneralNamesFromAltName(altName);
        assertNotNull("getGeneralNamesFromAltName failed for " + altName, gn);

        GeneralName[] names = gn.getNames();
        String ret = CertTools.getGeneralNameString(0, names[1].getName());
        assertEquals("krb5principal=foo/bar@P.SE", ret);

        altName = "krb5principal=foo@P.SE";
        gn = CertTools.getGeneralNamesFromAltName(altName);
        names = gn.getNames();
        ret = CertTools.getGeneralNameString(0, names[0].getName());
        assertEquals("krb5principal=foo@P.SE", ret);

        altName = "krb5principal=foo/A.SE@P.SE";
        gn = CertTools.getGeneralNamesFromAltName(altName);
        names = gn.getNames();
        ret = CertTools.getGeneralNameString(0, names[0].getName());
        assertEquals("krb5principal=foo/A.SE@P.SE", ret);

        Certificate krbcert = CertTools.getCertfromByteArray(krb5principalcert);
        String s = CertTools.getSubjectAlternativeName(krbcert);
        assertEquals("krb5principal=foo/bar@P.COM", s);
    }

    @Test
    public void testPseudonymAndName() throws Exception {
        String dn1 = "c=SE,O=Prime,OU=Tech,TelephoneNumber=555-666,Name=Kalle,PostalAddress=footown,PostalCode=11122,Pseudonym=Shredder,cn=Tomas Gustavsson";
        String bcdn1 = CertTools.stringToBCDNString(dn1);
        assertEquals(
                "Pseudonym=Shredder,TelephoneNumber=555-666,PostalAddress=footown,PostalCode=11122,CN=Tomas Gustavsson,Name=Kalle,OU=Tech,O=Prime,C=SE",
                bcdn1);
    }

    @Test
    public void testEscapedCharacters() throws Exception {
        final String input = "O=\\<fff\\>\\\",CN=oid,SN=12345,NAME=name,C=se";
        final String dn = CertTools.stringToBCDNString(input);
        assertEquals("Conversion of: "+input, "CN=oid,Name=name,SN=12345,O=\\<fff\\>\\\",C=se", dn);
    }

    @Test
    public void testSerialNumberFromString() throws Exception {
        // Test numerical format
        BigInteger serno = CertTools.getSerialNumberFromString("00001");
        assertEquals(1, serno.intValue());
        // Test SE001 format
        serno = CertTools.getSerialNumberFromString("SE021");
        assertEquals(21, serno.intValue());

        // Test numeric and hexadecimal string, will get the numerical part in the middle
        serno = CertTools.getSerialNumberFromString("F53AA");
        assertEquals(53, serno.intValue());

        // Test pure letters
        serno = CertTools.getSerialNumberFromString("FXBAA");
        assertEquals(26748514, serno.intValue());

        // Test a strange format...
        serno = CertTools.getSerialNumberFromString("SE02K");
        assertEquals(2, serno.intValue());

        // Test a real biginteger
        serno = CertTools.getSerialNumberFromString("7331288210307371");
        assertEquals(271610737, serno.intValue());

        // Test a real certificate
        Certificate cert = CertTools.getCertfromByteArray(testcert);
        serno = CertTools.getSerialNumber(cert);
        assertEquals(271610737, serno.intValue());
        String str = CertTools.getSerialNumberAsString(cert);
        assertEquals(serno.toString(16), str);
    }

    @Test
    public void testReadPEMCertificate() throws Exception {
        X509Certificate cert = (X509Certificate) CertTools.getCertfromByteArray(pemcert);
        assertNotNull(cert);
        assertEquals("CN=AdminCA1,O=EJBCA Sample,C=SE", cert.getSubjectDN().toString());
    }

    @Test
    public void testNullInput() {
        assertNull(CertTools.stringToBcX500Name(null));
        assertNull(CertTools.stringToBCDNString(null));
        assertNull(CertTools.reverseDN(null));
        assertFalse(CertTools.isDNReversed(null));
        assertNull(CertTools.getPartFromDN(null, null));
        assertEquals(0, CertTools.getPartsFromDN(null, null).size());
        assertEquals(0, CertTools.getCustomOids(null).size());
        try {
        	assertNull(CertTools.getSerialNumber(null));
        	assertTrue("Should throw", false);
        } catch (IllegalArgumentException e) {
        	// NOPMD
        }
        try {
            assertNull(CertTools.getSerialNumberAsString(null));
        	assertTrue("Should throw", false);
        } catch (IllegalArgumentException e) {
        	// NOPMD
        }
        try {
            assertNull(CertTools.getSerialNumberFromString(null));
        	assertTrue("Should throw", false);
        } catch (IllegalArgumentException e) {
        	// NOPMD
        }
    }
    
    @Test
    public void testCertCollectionFromArray() throws Exception {
    	Certificate[] certarray = new Certificate[3];
    	certarray[0] = CertTools.getCertfromByteArray(testcert);
    	certarray[1] = CertTools.getCertfromByteArray(guidcert);
    	certarray[2] = CertTools.getCertfromByteArray(altNameCert);
    	Collection<Certificate> certs = CertTools.getCertCollectionFromArray(certarray, "BC");
    	assertEquals(3, certs.size());
    	Iterator<Certificate> iter = certs.iterator();
    	assertEquals("CN=p12test,O=PrimeTest,C=SE", CertTools.getSubjectDN(iter.next()));
    	assertEquals("UID=guid,CN=Guid,C=SE", CertTools.getSubjectDN(iter.next()));
    	assertEquals("CN=foo,O=AnaTom,C=SE", CertTools.getSubjectDN(iter.next()));
    	byte[] bytes = CertTools.getPEMFromCerts(certs);
    	String str = new String(bytes);
    	assertTrue(str.contains("BEGIN CERTIFICATE"));
    }

    @Test
    public void testCheckValidity() throws Exception {
    	// NotBefore: Wed Sep 24 08:48:04 CEST 2003 (1064386084000)
    	// NotAfter: Fri Sep 23 08:58:04 CEST 2005 (1127458684000)
    	//
    	Certificate cert = CertTools.getCertfromByteArray(testcert);
//    	System.out.println(CertTools.getNotBefore(cert).getTime());
//    	System.out.println(CertTools.getNotAfter(cert).getTime());
    	assertEquals(1064386084000L, CertTools.getNotBefore(cert).getTime());
    	assertEquals(1127458684000L, CertTools.getNotAfter(cert).getTime());
    	CertTools.checkValidity(cert, new Date(1064386084001L));
    	CertTools.checkValidity(cert, new Date(1127458683999L));
    	try {
    		CertTools.checkValidity(cert, new Date(1064386083999L));
    		assertTrue("Should throw", false);
    	} catch (CertificateNotYetValidException e) {
    		// NOPMD
    	}
    	try {
    		CertTools.checkValidity(cert, new Date(1127458684001L));
    		assertTrue("Should throw", false);
    	} catch (CertificateExpiredException e) {
    		// NOPMD
    	}    	
    }
    
    @Test
    public void testFingerprint() throws Exception {
    	Certificate cert = CertTools.getCertfromByteArray(testcert);
    	assertEquals("4d66df0017deb32f669346c51c80600964816c84", CertTools.getFingerprintAsString(cert));
    	assertEquals("4d66df0017deb32f669346c51c80600964816c84", CertTools.getFingerprintAsString(testcert));
    	assertEquals("c61bfaa15d733532c5e795756c8001d4", new String(Hex.encode(CertTools.generateMD5Fingerprint(testcert))));
    }

    @Test
    public void testCRLs() throws Exception {
    	X509CRL crl = CertTools.getCRLfromByteArray(testcrl);
    	assertEquals("CN=TEST", CertTools.getIssuerDN(crl));
    	byte[] pembytes = CertTools.getPEMFromCrl(testcrl);
    	String pem = new String(pembytes);
    	assertTrue(pem.contains("BEGIN X509 CRL"));
    	assertEquals(1, CrlExtensions.getCrlNumber(crl).intValue());
    	assertEquals(-1, CrlExtensions.getDeltaCRLIndicator(crl).intValue());

    	X509CRL deltacrl = CertTools.getCRLfromByteArray(testdeltacrl);
    	assertEquals(3, CrlExtensions.getCrlNumber(deltacrl).intValue());
    	assertEquals(2, CrlExtensions.getDeltaCRLIndicator(deltacrl).intValue());

    }
    
    private DERSequence permanentIdentifier(String identifierValue, String assigner) {
        DERSequence result;
        ASN1EncodableVector v = new ASN1EncodableVector(); // this is the OtherName
        v.add(new ASN1ObjectIdentifier(CertTools.PERMANENTIDENTIFIER_OBJECTID));

        // First the PermanentIdentifier sequence
        ASN1EncodableVector piSeq = new ASN1EncodableVector();
        if (identifierValue != null) {
            piSeq.add(new DERUTF8String(identifierValue));
        }
        if (assigner != null) {
            piSeq.add(new ASN1ObjectIdentifier(assigner));
        }
        v.add(new DERTaggedObject(true, 0, new DERSequence(piSeq)));
        result = new DERSequence(v);
        
        log.info(ASN1Dump.dumpAsString(result));
        return result;
    }
    
    @Test
    public void testGetPermanentIdentifierStringFromSequence() throws Exception {
        assertEquals("abc123/1.2.3.4", CertTools.getPermanentIdentifierStringFromSequence(permanentIdentifier("abc123", "1.2.3.4")));
        assertEquals("defg456/", CertTools.getPermanentIdentifierStringFromSequence(permanentIdentifier("defg456", null)));
        assertEquals("/1.2.3.5", CertTools.getPermanentIdentifierStringFromSequence(permanentIdentifier(null, "1.2.3.5")));
        assertEquals("/", CertTools.getPermanentIdentifierStringFromSequence(permanentIdentifier(null, null)));
        
        assertEquals("ident with \\/ slash/1.2.3.4", CertTools.getPermanentIdentifierStringFromSequence(permanentIdentifier("ident with / slash", "1.2.3.4")));
        assertEquals("ident with \\/ slash/", CertTools.getPermanentIdentifierStringFromSequence(permanentIdentifier("ident with / slash", null)));
        assertEquals("ident with \\\\/ slash/1.2.3.6", CertTools.getPermanentIdentifierStringFromSequence(permanentIdentifier("ident with \\/ slash", "1.2.3.6")));
        assertEquals("ident with \\\\/ slash/", CertTools.getPermanentIdentifierStringFromSequence(permanentIdentifier("ident with \\/ slash", null)));
    }
    
    @Test
    public void testGetPermanentIdentifierValues() throws Exception {
        assertEquals("[abc123, 1.2.3.7]", Arrays.toString(CertTools.getPermanentIdentifierValues("abc123/1.2.3.7")));
        assertEquals("[abc123, null]", Arrays.toString(CertTools.getPermanentIdentifierValues("abc123/")));
        assertEquals("[abc123, null]", Arrays.toString(CertTools.getPermanentIdentifierValues("abc123")));
        assertEquals("[null, 1.2.3.8]", Arrays.toString(CertTools.getPermanentIdentifierValues("/1.2.3.8")));
        assertEquals("[null, null]", Arrays.toString(CertTools.getPermanentIdentifierValues("/")));
        assertEquals("[null, null]", Arrays.toString(CertTools.getPermanentIdentifierValues("")));
    }
    
    @Test
    public void testGetGeneralNamesFromAltName4permanentIdentifier() throws Exception {
        // One permanentIdentifier
        String altName = "permanentIdentifier=def321/1.2.5, upn=upn@u.com";
        GeneralNames gn = CertTools.getGeneralNamesFromAltName(altName);
        assertNotNull("getGeneralNamesFromAltName failed for " + altName, gn);
        String[] result = new String[] { 
            CertTools.getGeneralNameString(0, gn.getNames()[0].getName()), 
            CertTools.getGeneralNameString(0, gn.getNames()[1].getName())
        };
        Arrays.sort(result);
        assertEquals("[permanentIdentifier=def321/1.2.5, upn=upn@u.com]", Arrays.toString(result));
        
        // Two permanentIdentifiers
        gn = CertTools.getGeneralNamesFromAltName("permanentIdentifier=def321/1.2.5, upn=upn@example.com, permanentIdentifier=abcd 456/1.2.7");    
        result = new String[] { 
            CertTools.getGeneralNameString(0, gn.getNames()[0].getName()),
            CertTools.getGeneralNameString(0, gn.getNames()[1].getName()),
            CertTools.getGeneralNameString(0, gn.getNames()[2].getName())
        };
        Arrays.sort(result);
        assertEquals("[permanentIdentifier=abcd 456/1.2.7, permanentIdentifier=def321/1.2.5, upn=upn@example.com]", Arrays.toString(result));
    }
    
  
}
