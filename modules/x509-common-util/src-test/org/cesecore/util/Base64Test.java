/*************************************************************************
 *                                                                       *
 *  CESeCore: CE Security Core                                           *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General           *
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

import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.security.cert.Certificate;

import org.apache.commons.lang.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.DecoderException;
import org.junit.BeforeClass;
import org.junit.Test;

import com.keyfactor.util.certificates.X509CertificateTools;

/**
 * Tests base64 encoding and decoding
 *
 */
public class Base64Test {

    private static final String testcert_oneline = ("MIIDATCCAmqgAwIBAgIIczEoghAwc3EwDQYJKoZIhvcNAQEFBQAwLzEPMA0GA1UE"
            + "AxMGVGVzdENBMQ8wDQYDVQQKEwZBbmFUb20xCzAJBgNVBAYTAlNFMB4XDTAzMDky" + "NDA2NDgwNFoXDTA1MDkyMzA2NTgwNFowMzEQMA4GA1UEAxMHcDEydGVzdDESMBAG"
            + "A1UEChMJUHJpbWVUZXN0MQswCQYDVQQGEwJTRTCBnTANBgkqhkiG9w0BAQEFAAOB" + "iwAwgYcCgYEAnPAtfpU63/0h6InBmesN8FYS47hMvq/sliSBOMU0VqzlNNXuhD8a"
            + "3FypGfnPXvjJP5YX9ORu1xAfTNao2sSHLtrkNJQBv6jCRIMYbjjo84UFab2qhhaJ" + "wqJgkQNKu2LHy5gFUztxD8JIuFPoayp1n9JL/gqFDv6k81UnDGmHeFcCARGjggEi"
            + "MIIBHjAPBgNVHRMBAf8EBTADAQEAMA8GA1UdDwEB/wQFAwMHoAAwOwYDVR0lBDQw" + "MgYIKwYBBQUHAwEGCCsGAQUFBwMCBggrBgEFBQcDBAYIKwYBBQUHAwUGCCsGAQUF"
            + "BwMHMB0GA1UdDgQWBBTnT1aQ9I0Ud4OEfNJkSOgJSrsIoDAfBgNVHSMEGDAWgBRj" + "e/R2qFQkjqV0pXdEpvReD1eSUTAiBgNVHREEGzAZoBcGCisGAQQBgjcUAgOgCQwH"
            + "Zm9vQGZvbzASBgNVHSAECzAJMAcGBSkBAQEBMEUGA1UdHwQ+MDwwOqA4oDaGNGh0" + "dHA6Ly8xMjcuMC4wLjE6ODA4MC9lamJjYS93ZWJkaXN0L2NlcnRkaXN0P2NtZD1j"
            + "cmwwDQYJKoZIhvcNAQEFBQADgYEAU4CCcLoSUDGXJAOO9hGhvxQiwjGD2rVKCLR4" + "emox1mlQ5rgO9sSel6jHkwceaq4A55+qXAjQVsuy76UJnc8ncYX8f98uSYKcjxo/"
            + "ifn1eHMbL8dGLd5bc2GNBZkmhFIEoDvbfn9jo7phlS8iyvF2YhC4eso8Xb+T7+BZ" + "QUOBOvc=");

    private static final String testcert_crlf = ("MIIDATCCAmqgAwIBAgIIczEoghAwc3EwDQYJKoZIhvcNAQEFBQAwLzEPMA0GA1UE\n"
            + "AxMGVGVzdENBMQ8wDQYDVQQKEwZBbmFUb20xCzAJBgNVBAYTAlNFMB4XDTAzMDky\n"
            + "NDA2NDgwNFoXDTA1MDkyMzA2NTgwNFowMzEQMA4GA1UEAxMHcDEydGVzdDESMBAG\n"
            + "A1UEChMJUHJpbWVUZXN0MQswCQYDVQQGEwJTRTCBnTANBgkqhkiG9w0BAQEFAAOB\n"
            + "iwAwgYcCgYEAnPAtfpU63/0h6InBmesN8FYS47hMvq/sliSBOMU0VqzlNNXuhD8a\n"
            + "3FypGfnPXvjJP5YX9ORu1xAfTNao2sSHLtrkNJQBv6jCRIMYbjjo84UFab2qhhaJ\n"
            + "wqJgkQNKu2LHy5gFUztxD8JIuFPoayp1n9JL/gqFDv6k81UnDGmHeFcCARGjggEi\n"
            + "MIIBHjAPBgNVHRMBAf8EBTADAQEAMA8GA1UdDwEB/wQFAwMHoAAwOwYDVR0lBDQw\n"
            + "MgYIKwYBBQUHAwEGCCsGAQUFBwMCBggrBgEFBQcDBAYIKwYBBQUHAwUGCCsGAQUF\n"
            + "BwMHMB0GA1UdDgQWBBTnT1aQ9I0Ud4OEfNJkSOgJSrsIoDAfBgNVHSMEGDAWgBRj\n"
            + "e/R2qFQkjqV0pXdEpvReD1eSUTAiBgNVHREEGzAZoBcGCisGAQQBgjcUAgOgCQwH\n"
            + "Zm9vQGZvbzASBgNVHSAECzAJMAcGBSkBAQEBMEUGA1UdHwQ+MDwwOqA4oDaGNGh0\n"
            + "dHA6Ly8xMjcuMC4wLjE6ODA4MC9lamJjYS93ZWJkaXN0L2NlcnRkaXN0P2NtZD1j\n"
            + "cmwwDQYJKoZIhvcNAQEFBQADgYEAU4CCcLoSUDGXJAOO9hGhvxQiwjGD2rVKCLR4\n"
            + "emox1mlQ5rgO9sSel6jHkwceaq4A55+qXAjQVsuy76UJnc8ncYX8f98uSYKcjxo/\n"
            + "ifn1eHMbL8dGLd5bc2GNBZkmhFIEoDvbfn9jo7phlS8iyvF2YhC4eso8Xb+T7+BZ\n" + "QUOBOvc=");

    private static final byte[] longMsg = { 77, 73, 65, 67, 65, 81, 77, 119, 103, 65, 89, 74, 75, 111, 90, 73, 104, 118, 99, 78, 65, 81, 99, 66, 111,
            73, 65, 107, 103, 65, 83, 67, 67, 118, 73, 119, 103, 68, 67, 65, 66, 103, 107, 113, 104, 107, 105, 71, 57, 119, 48, 66, 66, 119, 71, 103,
            103, 67, 83, 65, 66, 73, 73, 75, 32, 50, 106, 67, 67, 67, 116, 89, 119, 103, 103, 86, 109, 66, 103, 115, 113, 104, 107, 105, 71, 57, 119,
            48, 66, 68, 65, 111, 66, 65, 113, 67, 67, 66, 80, 107, 119, 103, 103, 84, 49, 77, 67, 99, 71, 67, 105, 113, 71, 83, 73, 98, 51, 68, 81,
            69, 77, 65, 81, 77, 119, 71, 81, 81, 85, 32, 112, 119, 114, 74, 76, 56, 72, 47, 115, 121, 53, 115, 43, 67, 122, 56, 47, 118, 112, 107,
            81, 109, 56, 111, 57, 82, 69, 67, 65, 87, 81, 69, 103, 103, 84, 73, 115, 88, 73, 68, 106, 107, 49, 78, 81, 55, 69, 113, 100, 79, 49, 99,
            67, 89, 116, 104, 47, 121, 55, 69, 76, 50, 86, 52, 32, 117, 82, 113, 119, 71, 105, 80, 51, 67, 102, 80, 66, 109, 73, 76, 82, 86, 84, 110,
            70, 84, 120, 53, 66, 80, 121, 121, 87, 65, 54, 102, 100, 55, 100, 105, 78, 98, 99, 102, 106, 109, 115, 57, 71, 108, 114, 50, 75, 75, 84,
            104, 75, 121, 78, 82, 86, 50, 81, 82, 102, 56, 106, 84, 117, 32, 55, 117, 78, 47, 113, 79, 49, 112, 88, 57, 50, 111, 68, 82, 119, 51, 56,
            104, 107, 50, 65, 100, 118, 66, 76, 81, 105, 119, 113, 122, 104, 80, 48, 99, 55, 120, 121, 53, 48, 122, 104, 50, 118, 89, 112, 49, 106,
            110, 100, 109, 90, 117, 68, 85, 112, 48, 87, 104, 114, 47, 43, 68, 54, 120, 32, 109, 77, 78, 108, 121, 117, 111, 78, 73, 68, 122, 57,
            117, 102, 102, 68, 106, 73, 98, 111, 117, 116, 97, 81, 82, 52, 54, 55, 88, 86, 88, 55, 105, 106, 53, 98, 74, 103, 56, 88, 75, 98, 118,
            56, 81, 67, 106, 47, 52, 119, 48, 121, 102, 56, 89, 116, 88, 82, 99, 70, 122, 85, 118, 83, 32, 119, 49, 78, 79, 77, 48, 90, 109, 55, 70,
            83, 85, 49, 86, 75, 49, 71, 106, 86, 52, 65, 98, 107, 90, 115, 49, 83, 90, 51, 84, 49, 68, 89, 109, 88, 83, 112, 109, 101, 84, 100, 82,
            103, 122, 101, 69, 115, 53, 73, 78, 80, 74, 77, 110, 99, 116, 83, 84, 114, 66, 66, 113, 119, 81, 32, 83, 84, 109, 102, 43, 47, 87, 73,
            78, 104, 114, 72, 54, 118, 79, 84, 88, 77, 66, 98, 85, 53, 122, 101, 73, 57, 115, 81, 78, 56, 86, 87, 89, 82, 114, 82, 80, 89, 48, 111,
            86, 78, 53, 107, 56, 71, 84, 65, 89, 83, 43, 119, 102, 55, 80, 84, 121, 69, 115, 53, 74, 107, 111, 66, 32, 108, 122, 75, 105, 52, 120,
            88, 104, 120, 56, 72, 97, 105, 119, 113, 116, 115, 50, 73, 69, 86, 84, 99, 77, 69, 78, 98, 77, 111, 89, 99, 83, 108, 89, 122, 103, 103,
            109, 114, 47, 78, 120, 114, 51, 77, 116, 100, 52, 54, 55, 65, 49, 56, 85, 113, 53, 107, 98, 72, 81, 81, 103, 106, 72, 32, 122, 79, 100,
            50, 83, 107, 85, 71, 104, 122, 43, 115, 51, 82, 98, 55, 86, 116, 53, 99, 98, 67, 89, 67, 53, 86, 114, 57, 50, 121, 116, 67, 88, 100, 88,
            122, 43, 105, 49, 109, 69, 73, 104, 90, 74, 71, 119, 109, 84, 47, 47, 98, 47, 122, 53, 114, 52, 105, 109, 78, 53, 98, 120, 86, 32, 79,
            80, 97, 77, 87, 120, 78, 65, 51, 113, 100, 85, 70, 116, 84, 121, 77, 70, 116, 52, 73, 79, 82, 107, 100, 72, 120, 43, 73, 118, 84, 89, 83,
            48, 78, 54, 82, 79, 69, 87, 113, 108, 100, 111, 98, 100, 116, 87, 86, 70, 109, 57, 80, 110, 89, 72, 116, 111, 54, 51, 105, 87, 55, 73,
            32, 98, 70, 70, 114, 99, 115, 118, 50, 119, 97, 120, 108, 85, 110, 119, 108, 121, 83, 121, 121, 87, 56, 50, 107, 75, 101, 69, 55, 74, 70,
            66, 121, 114, 68, 76, 102, 50, 66, 102, 65, 49, 77, 111, 110, 102, 69, 85, 87, 81, 84, 79, 78, 113, 80, 54, 99, 109, 116, 114, 56, 53,
            120, 68, 98, 32, 56, 82, 53, 67, 65, 121, 51, 116, 81, 52, 88, 83, 54, 98, 112, 74, 84, 84, 102, 55, 82, 110, 88, 80, 105, 120, 77, 71,
            111, 110, 81, 74, 57, 69, 79, 80, 75, 78, 85, 57, 100, 118, 77, 107, 69, 65, 75, 51, 65, 110, 114, 48, 105, 106, 65, 57, 50, 105, 69,
            106, 101, 79, 48, 87, 32, 115, 71, 67, 117, 56, 53, 55, 90, 81, 88, 73, 85, 79, 65, 109, 76, 107, 83, 53, 111, 90, 76, 119, 84, 50, 73,
            68, 113, 110, 66, 81, 78, 67, 57, 102, 84, 100, 86, 86, 75, 87, 75, 100, 102, 118, 111, 54, 67, 53, 75, 68, 105, 111, 116, 109, 119, 75,
            56, 68, 97, 53, 79, 110, 97, 32, 109, 81, 113, 112, 119, 88, 56, 80, 66, 80, 120, 120, 65, 106, 79, 99, 51, 57, 48, 87, 120, 50, 57, 48,
            106, 110, 87, 54, 43, 55, 72, 112, 49, 99, 66, 55, 120, 73, 90, 112, 53, 53, 114, 109, 111, 57, 82, 74, 121, 99, 102, 71, 121, 83, 105,
            51, 122, 108, 117, 68, 47, 83, 109, 100, 32, 114, 90, 73, 79, 86, 83, 100, 69, 72, 88, 81, 104, 103, 119, 104, 119, 83, 106, 122, 101,
            119, 97, 70, 112, 104, 107, 71, 80, 117, 104, 105, 110, 85, 72, 55, 57, 77, 105, 67, 70, 105, 114, 106, 86, 97, 83, 118, 79, 78, 106, 71,
            88, 65, 86, 71, 100, 90, 84, 117, 77, 80, 103, 120, 49, 32, 47, 73, 99, 113, 110, 119, 65, 75, 75, 52, 100, 87, 78, 89, 47, 101, 100, 99,
            108, 88, 55, 90, 80, 66, 43, 102, 86, 57, 97, 109, 52, 73, 115, 111, 89, 77, 99, 110, 70, 81, 108, 100, 99, 71, 74, 74, 67, 83, 56, 101,
            65, 99, 81, 83, 121, 86, 119, 120, 48, 47, 122, 103, 54, 89, 32, 86, 80, 89, 79, 86, 105, 73, 68, 85, 109, 48, 54, 76, 66, 98, 78, 118,
            55, 75, 103, 122, 49, 112, 53, 107, 107, 113, 49, 106, 121, 103, 85, 81, 51, 65, 113, 70, 83, 110, 97, 57, 74, 90, 66, 97, 85, 57, 112,
            110, 55, 113, 87, 43, 99, 122, 107, 119, 115, 98, 112, 83, 117, 55, 83, 32, 77, 110, 120, 73, 76, 84, 102, 90, 73, 78, 74, 81, 103, 76,
            114, 50, 110, 74, 43, 87, 76, 105, 80, 47, 105, 98, 102, 85, 104, 85, 116, 112, 97, 55, 49, 49, 53, 57, 119, 71, 118, 105, 68, 111, 112,
            107, 53, 90, 54, 51, 78, 67, 102, 82, 113, 84, 97, 69, 101, 122, 122, 87, 99, 49, 32, 118, 82, 54, 110, 66, 109, 105, 73, 99, 97, 70, 67,
            81, 77, 110, 97, 73, 77, 50, 120, 117, 48, 78, 54, 77, 88, 55, 78, 53, 75, 72, 111, 115, 107, 80, 56, 54, 99, 66, 99, 68, 107, 71, 116,
            106, 66, 98, 43, 104, 82, 83, 98, 50, 70, 114, 100, 47, 88, 102, 56, 81, 86, 72, 105, 32, 117, 73, 116, 84, 107, 107, 115, 53, 103, 47,
            101, 43, 70, 119, 90, 105, 105, 89, 80, 109, 55, 115, 80, 107, 51, 48, 43, 75, 66, 86, 68, 87, 68, 48, 105, 78, 47, 86, 85, 70, 99, 51,
            100, 80, 90, 116, 80, 90, 112, 98, 49, 57, 114, 88, 105, 105, 47, 71, 47, 104, 81, 88, 57, 109, 32, 103, 84, 72, 54, 89, 54, 101, 113,
            82, 108, 105, 79, 52, 114, 84, 75, 89, 98, 112, 107, 76, 50, 79, 120, 76, 110, 102, 56, 75, 57, 102, 50, 49, 80, 88, 118, 89, 115, 83,
            85, 83, 119, 119, 120, 43, 89, 116, 105, 110, 74, 90, 69, 68, 88, 89, 118, 76, 54, 97, 53, 79, 56, 69, 102, 32, 101, 69, 50, 112, 109,
            48, 101, 65, 75, 56, 113, 104, 99, 82, 49, 71, 107, 81, 89, 116, 86, 87, 112, 72, 115, 76, 109, 121, 122, 53, 75, 78, 78, 49, 73, 121,
            75, 107, 73, 109, 78, 90, 72, 66, 114, 47, 114, 122, 86, 121, 112, 71, 75, 113, 74, 82, 112, 119, 106, 77, 97, 75, 51, 66, 32, 104, 75,
            80, 108, 99, 101, 73, 90, 115, 54, 48, 99, 115, 85, 78, 119, 117, 71, 52, 98, 80, 97, 73, 43, 90, 48, 66, 122, 101, 47, 114, 57, 118, 54,
            120, 99, 57, 69, 43, 119, 53, 43, 78, 74, 105, 70, 68, 52, 102, 75, 54, 83, 55, 56, 72, 72, 116, 43, 75, 119, 113, 113, 106, 53, 32, 110,
            100, 50, 85, 108, 51, 85, 120, 110, 56, 115, 115, 114, 51, 97, 70, 51, 99, 69, 87, 50, 67, 118, 69, 54, 109, 100, 56, 82, 88, 108, 69,
            108, 57, 75, 109, 51, 77, 47, 87, 50, 65, 117, 74, 111, 83, 107, 82, 86, 90, 122, 90, 65, 110, 85, 80, 82, 56, 122, 78, 70, 75, 80, 114,
            32, 86, 115, 98, 88, 65, 114, 80, 68, 52, 73, 118, 72, 50, 118, 82, 88, 102, 98, 49, 78, 81, 109, 114, 118, 83, 55, 99, 122, 86, 78, 47,
            114, 56, 107, 104, 66, 104, 111, 107, 56, 120, 120, 90, 80, 101, 78, 73, 98, 112, 88, 105, 55, 100, 98, 104, 98, 113, 121, 81, 56, 74,
            113, 111, 100, 32, 55, 108, 67, 76, 109, 51, 81, 73, 119, 76, 71, 97, 112, 79, 53, 87, 89, 67, 89, 103, 56, 76, 98, 67, 103, 119, 122,
            43, 118, 70, 102, 49, 111, 116, 104, 98, 81, 81, 100, 117, 79, 74, 118, 104, 70, 48, 76, 97, 76, 72, 114, 102, 103, 113, 88, 52, 48, 86,
            76, 69, 110, 118, 80, 76, 32, 87, 116, 66, 109, 121, 106, 67, 112, 119, 56, 104, 102, 50, 114, 85, 72, 103, 113, 69, 114, 119, 80, 84,
            71, 88, 75, 54, 102, 68, 68, 43, 90, 54, 69, 81, 117, 54, 110, 105, 67, 78, 76, 56, 114, 53, 76, 55, 83, 98, 68, 115, 50, 75, 103, 56,
            103, 73, 118, 112, 113, 80, 87, 84, 119, 32, 50, 105, 77, 73, 77, 86, 111, 119, 73, 119, 89, 74, 75, 111, 90, 73, 104, 118, 99, 78, 65,
            81, 107, 86, 77, 82, 89, 69, 70, 75, 122, 80, 76, 53, 102, 53, 119, 103, 75, 76, 119, 108, 98, 54, 77, 119, 105, 75, 71, 69, 101, 65, 99,
            109, 113, 84, 77, 68, 77, 71, 67, 83, 113, 71, 32, 83, 73, 98, 51, 68, 81, 69, 74, 70, 68, 69, 109, 72, 105, 81, 65, 99, 65, 66, 121, 65,
            71, 107, 65, 100, 103, 66, 104, 65, 72, 81, 65, 90, 81, 66, 107, 65, 71, 85, 65, 89, 119, 66, 114, 65, 71, 85, 65, 101, 81, 66, 104, 65,
            71, 119, 65, 97, 81, 66, 104, 65, 72, 77, 119, 32, 103, 103, 86, 111, 66, 103, 115, 113, 104, 107, 105, 71, 57, 119, 48, 66, 68, 65, 111,
            66, 65, 113, 67, 67, 66, 80, 107, 119, 103, 103, 84, 49, 77, 67, 99, 71, 67, 105, 113, 71, 83, 73, 98, 51, 68, 81, 69, 77, 65, 81, 77,
            119, 71, 81, 81, 85, 78, 56, 52, 112, 47, 100, 107, 115, 32, 75, 120, 55, 101, 71, 71, 55, 110, 120, 102, 112, 67, 76, 57, 100, 75, 55,
            113, 103, 67, 65, 87, 81, 69, 103, 103, 84, 73, 57, 116, 100, 79, 89, 109, 74, 109, 102, 83, 98, 118, 111, 120, 55, 106, 88, 78, 84, 89,
            69, 53, 77, 47, 76, 48, 82, 90, 98, 100, 47, 56, 111, 112, 119, 98, 32, 76, 72, 80, 82, 97, 66, 69, 107, 55, 114, 47, 53, 81, 77, 47, 43,
            105, 110, 122, 69, 100, 77, 54, 88, 97, 110, 77, 84, 112, 49, 115, 48, 70, 50, 85, 83, 55, 68, 112, 54, 98, 97, 67, 73, 100, 115, 107,
            54, 66, 85, 107, 108, 55, 82, 109, 103, 105, 50, 101, 70, 76, 108, 55, 122, 32, 120, 109, 70, 104, 115, 108, 78, 82, 52, 114, 99, 105,
            71, 107, 116, 47, 90, 66, 104, 71, 103, 102, 115, 68, 100, 48, 81, 57, 110, 69, 114, 69, 47, 73, 114, 75, 121, 66, 70, 75, 103, 77, 122,
            72, 75, 52, 88, 89, 108, 105, 84, 98, 74, 111, 99, 68, 87, 80, 81, 80, 110, 49, 66, 99, 32, 47, 100, 69, 54, 72, 99, 99, 80, 52, 43, 78,
            118, 73, 102, 74, 50, 120, 89, 84, 117, 115, 73, 50, 120, 67, 52, 101, 112, 52, 87, 84, 68, 83, 107, 90, 100, 84, 86, 76, 112, 67, 99,
            81, 104, 47, 67, 87, 82, 89, 110, 106, 88, 53, 50, 101, 80, 99, 116, 103, 76, 74, 121, 106, 98, 32, 57, 53, 116, 105, 56, 99, 73, 65,
            116, 54, 86, 89, 88, 76, 115, 72, 79, 103, 85, 120, 48, 100, 101, 86, 119, 50, 109, 101, 75, 77, 117, 76, 73, 84, 101, 78, 90, 107, 117,
            105, 79, 107, 57, 83, 67, 119, 76, 100, 118, 57, 81, 75, 117, 68, 97, 71, 108, 54, 51, 47, 108, 73, 72, 111, 32, 97, 86, 84, 99, 56, 80,
            53, 121, 90, 102, 73, 116, 120, 118, 71, 72, 51, 76, 109, 103, 68, 116, 121, 105, 101, 99, 115, 43, 101, 50, 110, 119, 115, 67, 103, 97,
            101, 118, 103, 87, 106, 108, 103, 100, 81, 110, 122, 83, 117, 90, 81, 76, 76, 76, 73, 66, 121, 120, 89, 98, 117, 84, 48, 47, 32, 75, 117,
            72, 113, 82, 48, 74, 68, 75, 43, 107, 83, 49, 82, 50, 57, 79, 57, 88, 89, 51, 76, 117, 111, 65, 51, 48, 67, 52, 56, 120, 112, 90, 74,
            113, 111, 109, 74, 81, 89, 90, 121, 88, 115, 102, 98, 122, 119, 88, 69, 104, 86, 52, 70, 108, 85, 111, 67, 76, 110, 78, 43, 97, 47, 32,
            72, 52, 101, 90, 84, 111, 97, 70, 85, 119, 85, 109, 72, 47, 121, 80, 116, 89, 110, 55, 66, 48, 111, 78, 109, 112, 117, 120, 79, 112, 69,
            122, 86, 101, 111, 71, 72, 121, 115, 105, 86, 107, 81, 84, 53, 112, 52, 117, 83, 112, 75, 50, 49, 107, 76, 74, 47, 48, 70, 115, 108, 89,
            110, 56, 32, 66, 108, 77, 90, 79, 107, 80, 98, 53, 82, 122, 69, 53, 48, 82, 81, 80, 83, 56, 84, 113, 72, 55, 122, 119, 69, 82, 97, 110,
            103, 80, 111, 118, 66, 70, 85, 101, 121, 76, 108, 87, 117, 107, 65, 43, 55, 84, 116, 100, 50, 121, 122, 86, 50, 51, 111, 65, 66, 102, 85,
            113, 69, 116, 74, 32, 89, 57, 81, 104, 76, 103, 54, 77, 83, 83, 49, 120, 43, 84, 66, 98, 51, 84, 52, 117, 110, 47, 81, 69, 86, 56, 53,
            84, 112, 90, 111, 109, 56, 49, 99, 47, 116, 53, 53, 51, 97, 116, 119, 86, 104, 76, 106, 102, 81, 48, 70, 87, 88, 69, 97, 108, 120, 79,
            52, 88, 97, 86, 122, 109, 32, 122, 115, 76, 116, 117, 104, 75, 54, 53, 98, 105, 72, 85, 49, 51, 98, 83, 99, 109, 53, 49, 107, 71, 121,
            122, 67, 121, 67, 117, 72, 73, 98, 47, 88, 67, 115, 89, 87, 98, 49, 120, 103, 52, 87, 50, 57, 77, 69, 112, 104, 77, 57, 103, 70, 110, 57,
            67, 100, 89, 84, 75, 43, 90, 121, 32, 98, 101, 85, 54, 66, 75, 65, 114, 107, 112, 71, 81, 51, 55, 112, 49, 109, 102, 113, 103, 43, 71,
            68, 119, 68, 86, 117, 71, 118, 117, 65, 89, 69, 101, 107, 112, 48, 87, 106, 84, 115, 53, 116, 122, 121, 47, 66, 113, 105, 86, 116, 110,
            110, 119, 84, 85, 90, 88, 90, 107, 89, 111, 65, 109, 32, 100, 109, 84, 73, 98, 109, 106, 77, 67, 89, 88, 51, 72, 85, 107, 108, 55, 74,
            49, 84, 66, 49, 118, 108, 97, 88, 77, 100, 100, 74, 70, 69, 83, 114, 81, 81, 108, 66, 66, 89, 56, 110, 52, 72, 74, 74, 50, 52, 79, 48,
            99, 110, 90, 89, 82, 117, 73, 53, 100, 66, 99, 48, 119, 102, 32, 82, 99, 107, 113, 99, 76, 105, 111, 104, 56, 72, 88, 100, 105, 84, 100,
            100, 66, 82, 101, 79, 101, 122, 100, 52, 107, 109, 49, 50, 81, 52, 49, 118, 87, 82, 114, 50, 105, 78, 103, 66, 56, 97, 77, 55, 52, 111,
            86, 54, 65, 81, 105, 69, 54, 98, 97, 98, 77, 115, 122, 84, 115, 85, 115, 32, 75, 67, 106, 122, 85, 105, 110, 114, 100, 74, 97, 87, 106,
            99, 111, 69, 68, 52, 84, 101, 88, 89, 114, 98, 55, 116, 78, 57, 98, 66, 106, 85, 55, 65, 71, 117, 107, 67, 84, 51, 117, 71, 122, 52, 52,
            82, 68, 71, 82, 106, 120, 70, 81, 88, 108, 52, 72, 50, 120, 49, 81, 81, 75, 88, 32, 51, 54, 120, 47, 111, 75, 56, 80, 74, 80, 103, 52,
            47, 119, 116, 84, 83, 117, 111, 81, 117, 47, 118, 89, 107, 113, 88, 87, 47, 67, 69, 67, 55, 49, 43, 99, 71, 89, 114, 50, 77, 116, 57, 53,
            57, 74, 47, 43, 79, 75, 98, 75, 122, 77, 110, 100, 113, 77, 55, 107, 71, 90, 120, 53, 32, 114, 71, 72, 108, 118, 116, 70, 78, 111, 112,
            121, 69, 71, 110, 80, 97, 122, 74, 105, 54, 67, 69, 116, 107, 86, 56, 69, 105, 98, 81, 85, 121, 66, 115, 89, 98, 68, 66, 89, 110, 108,
            107, 80, 110, 97, 117, 121, 113, 90, 66, 79, 98, 102, 66, 54, 105, 76, 122, 113, 107, 52, 99, 104, 82, 32, 53, 114, 72, 72, 57, 65, 49,
            56, 72, 48, 110, 113, 111, 89, 74, 67, 109, 86, 108, 66, 75, 80, 52, 113, 49, 72, 114, 104, 109, 117, 48, 117, 48, 90, 53, 99, 84, 106,
            52, 49, 121, 90, 122, 99, 117, 117, 97, 56, 79, 105, 108, 99, 83, 83, 83, 99, 72, 90, 77, 53, 80, 105, 97, 54, 32, 73, 79, 110, 87, 53,
            69, 104, 99, 90, 53, 101, 78, 70, 67, 100, 78, 117, 50, 57, 53, 49, 107, 78, 85, 54, 49, 69, 50, 109, 102, 99, 85, 89, 101, 98, 122, 88,
            43, 84, 98, 48, 73, 119, 84, 82, 67, 121, 86, 73, 120, 76, 80, 81, 49, 118, 116, 113, 107, 98, 84, 116, 57, 85, 114, 32, 112, 74, 121,
            89, 121, 65, 77, 65, 54, 57, 114, 66, 80, 49, 57, 97, 80, 116, 56, 105, 84, 66, 66, 119, 75, 106, 88, 98, 109, 68, 117, 53, 67, 54, 43,
            47, 106, 47, 104, 104, 55, 68, 101, 81, 82, 100, 87, 49, 54, 83, 113, 71, 55, 48, 121, 51, 90, 97, 52, 86, 114, 101, 86, 87, 32, 97, 82,
            121, 121, 115, 54, 53, 88, 70, 115, 74, 118, 121, 54, 102, 118, 77, 55, 111, 107, 85, 54, 99, 112, 104, 51, 86, 83, 50, 122, 98, 90, 81,
            50, 74, 115, 122, 71, 112, 89, 111, 49, 66, 111, 113, 73, 65, 101, 76, 88, 66, 50, 114, 69, 106, 75, 106, 122, 72, 81, 50, 70, 77, 104,
            32, 97, 89, 52, 84, 57, 57, 65, 52, 120, 54, 118, 68, 117, 54, 114, 81, 78, 48, 87, 55, 77, 43, 89, 112, 50, 117, 107, 52, 50, 51, 70,
            122, 118, 77, 83, 70, 105, 115, 119, 113, 98, 76, 104, 43, 86, 74, 81, 101, 117, 105, 81, 50, 106, 106, 72, 51, 82, 56, 72, 52, 53, 117,
            50, 81, 32, 84, 52, 112, 86, 57, 85, 67, 97, 57, 117, 110, 43, 73, 98, 82, 107, 113, 69, 120, 117, 87, 51, 89, 84, 119, 88, 114, 49, 84,
            77, 78, 67, 43, 73, 105, 106, 82, 113, 55, 49, 119, 116, 83, 51, 111, 70, 55, 67, 90, 81, 102, 112, 78, 122, 52, 86, 99, 50, 99, 80, 53,
            81, 79, 108, 32, 54, 76, 86, 89, 116, 101, 78, 118, 112, 120, 121, 89, 48, 74, 107, 68, 69, 82, 99, 105, 81, 104, 54, 121, 108, 110, 88,
            67, 90, 108, 79, 120, 117, 100, 71, 49, 108, 99, 87, 67, 83, 70, 52, 57, 49, 83, 78, 68, 100, 78, 80, 67, 101, 57, 116, 111, 103, 84,
            120, 80, 107, 54, 110, 81, 32, 121, 89, 106, 73, 114, 79, 80, 115, 105, 43, 70, 104, 119, 103, 113, 48, 83, 116, 103, 56, 117, 76, 81,
            117, 84, 74, 121, 114, 81, 84, 119, 105, 72, 121, 73, 47, 87, 53, 108, 86, 105, 71, 74, 84, 75, 51, 53, 49, 84, 100, 65, 48, 114, 70, 81,
            77, 72, 81, 79, 117, 112, 116, 100, 47, 32, 81, 99, 57, 86, 54, 53, 103, 68, 97, 72, 89, 113, 97, 48, 67, 83, 78, 110, 89, 116, 89, 105,
            106, 72, 90, 118, 87, 90, 69, 115, 100, 104, 106, 102, 83, 74, 49, 117, 108, 85, 75, 100, 122, 53, 97, 73, 101, 89, 120, 84, 89, 98, 71,
            100, 84, 88, 73, 71, 106, 66, 77, 86, 119, 119, 32, 73, 119, 89, 74, 75, 111, 90, 73, 104, 118, 99, 78, 65, 81, 107, 86, 77, 82, 89, 69,
            70, 68, 65, 86, 116, 120, 109, 48, 99, 78, 118, 114, 97, 116, 104, 67, 110, 76, 121, 56, 118, 115, 84, 90, 47, 53, 100, 103, 77, 68, 85,
            71, 67, 83, 113, 71, 83, 73, 98, 51, 68, 81, 69, 74, 32, 70, 68, 69, 111, 72, 105, 89, 65, 99, 65, 66, 121, 65, 71, 107, 65, 100, 103,
            66, 104, 65, 72, 81, 65, 90, 81, 66, 122, 65, 71, 107, 65, 90, 119, 66, 117, 65, 71, 115, 65, 90, 81, 66, 53, 65, 71, 69, 65, 98, 65, 66,
            112, 65, 71, 69, 65, 99, 119, 65, 69, 65, 81, 65, 69, 32, 65, 81, 65, 69, 65, 81, 65, 69, 65, 81, 65, 69, 103, 103, 101, 75, 65, 68, 67,
            65, 66, 103, 107, 113, 104, 107, 105, 71, 57, 119, 48, 66, 66, 119, 97, 103, 103, 68, 67, 65, 65, 103, 69, 65, 77, 73, 65, 71, 67, 83,
            113, 71, 83, 73, 98, 51, 68, 81, 69, 72, 65, 84, 65, 110, 32, 66, 103, 111, 113, 104, 107, 105, 71, 57, 119, 48, 66, 68, 65, 69, 71, 77,
            66, 107, 69, 70, 72, 51, 86, 107, 104, 109, 56, 116, 105, 57, 66, 57, 47, 74, 69, 86, 65, 76, 47, 87, 68, 74, 120, 55, 82, 67, 82, 65,
            103, 70, 107, 111, 73, 65, 69, 103, 103, 99, 52, 105, 70, 69, 106, 32, 56, 65, 56, 69, 84, 99, 89, 115, 71, 79, 68, 114, 79, 103, 74, 89,
            51, 77, 50, 74, 111, 109, 111, 106, 107, 98, 98, 115, 82, 48, 57, 70, 113, 57, 108, 51, 97, 119, 108, 89, 49, 71, 105, 117, 83, 74, 117,
            105, 54, 52, 99, 119, 108, 49, 108, 84, 89, 112, 85, 78, 97, 121, 115, 71, 32, 117, 122, 48, 111, 80, 69, 83, 52, 104, 77, 90, 53, 54,
            75, 114, 43, 119, 108, 68, 97, 78, 103, 76, 104, 43, 68, 50, 121, 87, 48, 104, 101, 53, 101, 102, 108, 113, 101, 71, 87, 67, 56, 122, 79,
            48, 57, 101, 112, 103, 67, 118, 74, 115, 120, 116, 79, 53, 113, 119, 50, 65, 103, 102, 43, 32, 115, 56, 99, 71, 68, 56, 65, 50, 101, 90,
            50, 53, 119, 76, 72, 50, 56, 80, 57, 117, 43, 53, 116, 109, 107, 120, 69, 77, 54, 47, 72, 113, 112, 54, 112, 90, 65, 43, 82, 90, 79, 57,
            101, 115, 76, 77, 90, 50, 51, 103, 78, 112, 113, 73, 89, 76, 66, 72, 110, 90, 81, 78, 112, 122, 32, 110, 77, 57, 55, 87, 100, 55, 66, 49,
            72, 75, 49, 47, 120, 76, 90, 100, 98, 72, 57, 72, 70, 75, 89, 77, 89, 53, 118, 88, 117, 101, 67, 81, 54, 109, 87, 112, 105, 116, 101,
            105, 90, 84, 115, 86, 88, 57, 82, 98, 48, 77, 67, 104, 43, 113, 117, 75, 68, 83, 87, 81, 112, 52, 55, 32, 101, 82, 77, 70, 116, 51, 116,
            98, 75, 52, 78, 51, 97, 57, 122, 55, 76, 57, 73, 77, 51, 70, 84, 109, 108, 49, 118, 98, 97, 56, 105, 75, 119, 84, 113, 109, 66, 83, 53,
            87, 51, 77, 109, 43, 83, 51, 90, 51, 117, 108, 97, 114, 107, 118, 116, 73, 97, 117, 56, 73, 82, 109, 82, 106, 32, 85, 102, 54, 122, 122,
            71, 79, 65, 74, 51, 67, 52, 109, 80, 99, 103, 101, 104, 70, 121, 117, 110, 109, 106, 83, 110, 86, 76, 88, 70, 70, 72, 88, 78, 111, 70,
            119, 81, 80, 89, 117, 97, 117, 102, 54, 55, 56, 52, 68, 66, 100, 83, 103, 88, 73, 72, 66, 108, 99, 121, 86, 88, 68, 70, 32, 116, 119,
            105, 87, 77, 73, 47, 71, 90, 43, 115, 54, 66, 116, 79, 67, 90, 115, 110, 48, 86, 49, 106, 113, 55, 102, 105, 88, 52, 115, 50, 104, 98,
            71, 114, 85, 112, 85, 122, 99, 104, 99, 110, 113, 76, 113, 101, 114, 109, 71, 118, 67, 90, 89, 99, 65, 57, 115, 116, 112, 114, 100, 43,
            74, 32, 121, 78, 78, 81, 113, 66, 73, 117, 77, 117, 118, 51, 49, 57, 109, 65, 117, 116, 78, 117, 47, 109, 70, 55, 51, 103, 77, 111, 116,
            86, 119, 90, 112, 84, 87, 114, 110, 118, 65, 81, 80, 76, 115, 55, 52, 52, 67, 109, 99, 89, 70, 51, 113, 74, 67, 68, 81, 54, 98, 47, 82,
            89, 108, 112, 32, 103, 81, 114, 79, 121, 119, 122, 79, 57, 43, 86, 76, 43, 70, 83, 80, 110, 90, 102, 109, 77, 56, 103, 49, 119, 116, 67,
            99, 90, 114, 50, 49, 122, 107, 77, 112, 78, 115, 114, 66, 53, 106, 115, 43, 79, 98, 73, 74, 65, 85, 56, 99, 119, 77, 87, 54, 65, 85, 99,
            106, 113, 103, 49, 82, 32, 72, 110, 122, 114, 71, 82, 97, 77, 79, 99, 103, 67, 73, 77, 69, 76, 115, 111, 88, 121, 100, 117, 78, 73, 81,
            71, 103, 119, 83, 52, 111, 55, 120, 50, 90, 107, 120, 102, 101, 80, 67, 52, 49, 110, 72, 57, 47, 49, 51, 88, 121, 77, 105, 113, 104, 90,
            104, 86, 106, 102, 73, 107, 48, 117, 32, 97, 122, 109, 109, 106, 47, 55, 81, 113, 108, 70, 113, 57, 51, 52, 111, 102, 104, 54, 106, 108,
            85, 55, 99, 47, 78, 47, 82, 53, 108, 111, 113, 80, 109, 104, 47, 106, 87, 79, 57, 101, 108, 90, 67, 107, 118, 115, 73, 113, 105, 121, 79,
            115, 103, 73, 115, 89, 78, 55, 67, 100, 53, 47, 47, 32, 57, 82, 87, 122, 48, 83, 55, 80, 109, 116, 68, 69, 66, 56, 53, 107, 114, 104, 83,
            109, 103, 50, 107, 47, 116, 117, 110, 51, 121, 89, 104, 50, 116, 76, 80, 98, 55, 57, 76, 50, 74, 99, 84, 48, 97, 54, 77, 109, 73, 76, 56,
            119, 88, 76, 104, 107, 120, 49, 56, 53, 53, 113, 111, 81, 32, 111, 122, 117, 108, 73, 72, 104, 48, 83, 69, 90, 71, 53, 110, 104, 56, 104,
            56, 65, 52, 112, 122, 120, 107, 49, 80, 71, 71, 110, 97, 88, 101, 54, 82, 113, 107, 47, 122, 84, 47, 67, 57, 106, 49, 120, 86, 57, 47,
            121, 101, 74, 67, 110, 97, 119, 57, 56, 110, 122, 73, 80, 121, 108, 100, 32, 99, 73, 87, 89, 102, 89, 103, 85, 115, 119, 108, 104, 101,
            112, 101, 49, 48, 82, 98, 83, 106, 76, 88, 119, 90, 67, 75, 51, 75, 98, 116, 55, 109, 100, 67, 104, 72, 70, 113, 51, 119, 67, 82, 57, 50,
            88, 52, 74, 89, 54, 103, 98, 101, 100, 101, 77, 109, 75, 74, 85, 116, 68, 109, 67, 32, 81, 116, 54, 121, 122, 65, 68, 85, 109, 105, 52,
            109, 119, 111, 98, 117, 75, 103, 90, 113, 104, 85, 97, 85, 106, 77, 111, 99, 117, 117, 74, 115, 99, 48, 52, 67, 86, 102, 68, 113, 69, 97,
            65, 50, 97, 98, 112, 73, 111, 67, 47, 99, 97, 80, 109, 106, 57, 104, 71, 74, 73, 90, 53, 111, 32, 57, 73, 56, 52, 48, 113, 49, 69, 117,
            113, 110, 80, 70, 81, 66, 88, 110, 107, 102, 107, 70, 83, 115, 80, 57, 98, 98, 117, 120, 82, 121, 82, 107, 67, 84, 114, 89, 88, 75, 98,
            49, 76, 90, 97, 90, 56, 83, 83, 74, 101, 118, 118, 97, 114, 90, 117, 98, 69, 79, 89, 81, 107, 85, 97, 32, 81, 112, 87, 51, 107, 86, 84,
            52, 97, 111, 73, 54, 57, 67, 105, 79, 104, 102, 84, 121, 73, 105, 75, 102, 79, 110, 75, 97, 101, 85, 53, 90, 71, 106, 108, 79, 113, 112,
            48, 47, 115, 111, 75, 84, 85, 65, 114, 122, 113, 76, 122, 116, 105, 87, 57, 67, 56, 102, 57, 88, 118, 81, 47, 111, 32, 52, 87, 82, 112,
            104, 121, 113, 116, 120, 72, 80, 49, 118, 104, 83, 100, 122, 55, 101, 72, 100, 103, 56, 82, 112, 98, 107, 106, 84, 103, 73, 49, 65, 71,
            90, 69, 104, 78, 120, 83, 68, 67, 81, 83, 85, 122, 82, 82, 107, 115, 108, 108, 67, 122, 80, 80, 100, 56, 51, 110, 83, 112, 119, 113, 32,
            88, 82, 115, 51, 66, 115, 119, 78, 78, 106, 57, 100, 85, 71, 55, 121, 120, 68, 71, 66, 117, 84, 84, 109, 104, 98, 73, 65, 90, 55, 122,
            88, 86, 90, 87, 112, 113, 51, 57, 102, 84, 80, 99, 83, 48, 115, 110, 109, 116, 114, 115, 88, 85, 76, 111, 107, 119, 104, 116, 104, 70,
            53, 118, 52, 32, 109, 49, 56, 116, 76, 108, 70, 106, 53, 78, 53, 114, 48, 79, 48, 50, 67, 109, 106, 117, 79, 48, 73, 121, 54, 74, 106,
            97, 65, 121, 104, 117, 80, 75, 102, 121, 57, 106, 82, 117, 49, 84, 48, 67, 78, 112, 48, 76, 87, 90, 81, 78, 110, 83, 77, 117, 118, 72,
            100, 55, 69, 121, 79, 98, 32, 97, 67, 84, 66, 102, 106, 87, 69, 53, 99, 77, 80, 71, 75, 122, 90, 71, 49, 56, 108, 81, 80, 69, 121, 87,
            72, 86, 106, 100, 54, 65, 73, 56, 108, 57, 75, 47, 112, 115, 99, 103, 86, 66, 83, 84, 87, 84, 101, 97, 97, 97, 79, 101, 57, 79, 111, 72,
            115, 51, 107, 69, 43, 100, 66, 32, 72, 97, 71, 103, 106, 111, 66, 48, 106, 52, 80, 55, 104, 97, 106, 117, 68, 82, 81, 104, 115, 79, 79,
            76, 82, 119, 43, 67, 76, 86, 119, 119, 111, 104, 49, 112, 85, 105, 106, 76, 103, 104, 107, 68, 99, 49, 84, 103, 101, 87, 78, 99, 121,
            104, 107, 74, 113, 54, 101, 73, 80, 117, 113, 109, 32, 103, 51, 116, 97, 68, 82, 99, 82, 78, 113, 81, 75, 80, 52, 122, 75, 72, 104, 57,
            77, 121, 107, 84, 73, 121, 104, 79, 119, 49, 72, 73, 51, 67, 75, 74, 82, 74, 97, 76, 109, 55, 71, 49, 69, 89, 74, 48, 48, 102, 76, 68,
            120, 71, 110, 103, 110, 65, 112, 85, 109, 77, 105, 107, 84, 32, 101, 111, 85, 90, 102, 69, 119, 120, 73, 116, 74, 73, 73, 104, 80, 75,
            110, 49, 43, 109, 78, 107, 84, 48, 110, 73, 110, 66, 65, 86, 43, 117, 81, 87, 72, 69, 114, 119, 72, 71, 115, 121, 51, 98, 119, 87, 75,
            56, 48, 56, 122, 81, 122, 79, 100, 54, 111, 56, 118, 70, 101, 56, 108, 112, 32, 102, 87, 117, 99, 75, 81, 65, 120, 80, 83, 111, 117, 118,
            101, 119, 101, 47, 85, 50, 121, 80, 71, 110, 55, 113, 70, 115, 101, 106, 97, 76, 114, 117, 72, 90, 72, 72, 51, 109, 56, 77, 107, 86, 54,
            105, 90, 105, 102, 84, 68, 66, 48, 87, 113, 66, 86, 71, 65, 104, 85, 103, 90, 102, 73, 32, 69, 106, 75, 47, 70, 98, 54, 74, 87, 107, 102,
            43, 78, 99, 101, 70, 52, 80, 87, 121, 82, 50, 65, 113, 87, 48, 115, 90, 120, 72, 121, 110, 111, 110, 56, 117, 75, 72, 112, 104, 76, 120,
            69, 85, 103, 75, 88, 75, 84, 90, 57, 99, 88, 76, 80, 76, 74, 77, 117, 80, 78, 65, 77, 54, 32, 86, 121, 79, 78, 106, 74, 57, 54, 66, 83,
            48, 72, 99, 121, 83, 48, 118, 87, 53, 121, 77, 80, 102, 99, 103, 68, 122, 121, 113, 85, 55, 118, 83, 77, 117, 85, 43, 53, 118, 77, 87,
            52, 108, 102, 83, 51, 82, 89, 65, 117, 105, 107, 65, 114, 71, 43, 98, 108, 68, 121, 116, 99, 88, 116, 32, 116, 77, 119, 54, 102, 82, 97,
            78, 47, 113, 76, 50, 53, 77, 103, 55, 114, 54, 119, 57, 110, 69, 57, 102, 113, 112, 120, 97, 81, 98, 103, 49, 101, 74, 49, 110, 119, 107,
            114, 67, 87, 108, 106, 85, 57, 112, 112, 77, 119, 119, 120, 88, 73, 101, 103, 113, 97, 81, 56, 88, 53, 65, 121, 56, 32, 56, 100, 55, 51,
            76, 88, 43, 84, 107, 65, 86, 57, 78, 86, 122, 119, 56, 102, 109, 104, 83, 104, 82, 122, 116, 98, 75, 122, 73, 105, 97, 48, 118, 56, 80,
            81, 108, 118, 84, 72, 87, 76, 56, 108, 73, 122, 120, 79, 71, 97, 108, 70, 102, 52, 113, 102, 53, 51, 117, 119, 50, 100, 109, 108, 32, 99,
            81, 119, 103, 87, 97, 43, 106, 78, 69, 98, 120, 77, 97, 69, 90, 83, 109, 50, 104, 53, 48, 83, 116, 113, 78, 85, 79, 50, 48, 89, 107, 110,
            115, 48, 109, 115, 65, 73, 48, 98, 65, 89, 107, 117, 99, 120, 65, 114, 107, 85, 108, 101, 82, 103, 83, 104, 55, 104, 101, 54, 82, 115,
            87, 32, 74, 79, 106, 108, 47, 104, 110, 101, 83, 75, 79, 68, 100, 117, 110, 50, 56, 74, 116, 86, 97, 85, 97, 79, 103, 100, 102, 103, 88,
            107, 112, 102, 55, 47, 43, 74, 114, 111, 111, 66, 109, 104, 87, 47, 66, 121, 79, 88, 80, 83, 84, 90, 74, 115, 69, 113, 65, 78, 86, 103,
            74, 52, 56, 82, 32, 43, 74, 117, 84, 117, 54, 52, 73, 85, 90, 80, 43, 98, 80, 83, 86, 112, 86, 114, 117, 102, 99, 54, 118, 81, 122, 98,
            73, 119, 121, 101, 117, 90, 52, 68, 54, 98, 74, 106, 89, 89, 104, 119, 111, 76, 84, 89, 67, 43, 74, 55, 122, 66, 51, 48, 78, 112, 118,
            75, 90, 114, 73, 75, 120, 32, 103, 82, 115, 69, 118, 108, 65, 48, 107, 87, 107, 112, 80, 54, 80, 67, 122, 116, 79, 101, 97, 100, 55, 100,
            97, 68, 104, 121, 79, 102, 66, 106, 73, 73, 87, 48, 115, 75, 119, 117, 50, 119, 52, 52, 70, 116, 122, 82, 97, 68, 52, 81, 66, 112, 87,
            71, 49, 49, 76, 87, 90, 110, 54, 114, 32, 71, 111, 110, 120, 84, 76, 74, 75, 57, 77, 88, 116, 69, 104, 65, 72, 69, 114, 106, 85, 82, 122,
            106, 102, 48, 121, 90, 101, 82, 111, 117, 122, 86, 116, 52, 106, 114, 49, 89, 104, 65, 115, 77, 71, 78, 108, 111, 98, 76, 109, 100, 100,
            108, 108, 89, 72, 104, 82, 78, 110, 76, 118, 114, 65, 32, 66, 117, 89, 86, 90, 113, 90, 56, 114, 86, 81, 110, 102, 51, 83, 87, 76, 99,
            78, 52, 79, 105, 105, 67, 69, 52, 73, 110, 79, 122, 52, 66, 56, 120, 108, 73, 76, 113, 66, 106, 115, 77, 116, 54, 73, 105, 54, 54, 115,
            54, 121, 65, 114, 53, 122, 99, 84, 86, 101, 87, 48, 83, 77, 77, 32, 97, 109, 70, 51, 69, 87, 48, 79, 69, 115, 85, 71, 53, 79, 118, 85,
            112, 108, 49, 120, 73, 82, 81, 84, 106, 120, 53, 50, 106, 48, 118, 90, 104, 67, 74, 86, 99, 118, 43, 99, 104, 101, 78, 105, 72, 43, 100,
            71, 68, 84, 90, 65, 80, 70, 82, 111, 118, 57, 49, 53, 82, 110, 48, 52, 32, 97, 116, 72, 105, 114, 103, 89, 84, 100, 109, 116, 90, 85, 99,
            70, 54, 118, 65, 78, 106, 109, 101, 43, 89, 122, 84, 121, 120, 84, 86, 114, 72, 115, 113, 122, 65, 98, 122, 109, 86, 90, 57, 102, 104,
            115, 68, 73, 56, 111, 86, 51, 79, 111, 52, 68, 84, 108, 106, 97, 65, 50, 99, 76, 77, 32, 88, 50, 109, 108, 122, 69, 77, 102, 88, 122, 75,
            52, 48, 90, 56, 69, 120, 89, 100, 66, 103, 100, 112, 48, 70, 48, 88, 104, 89, 73, 100, 54, 48, 83, 55, 98, 78, 67, 76, 77, 57, 54, 111,
            57, 118, 83, 101, 65, 72, 75, 57, 87, 88, 77, 104, 104, 99, 89, 111, 80, 71, 98, 75, 118, 32, 99, 101, 54, 51, 90, 105, 87, 70, 54, 76,
            100, 106, 87, 112, 72, 85, 51, 121, 73, 97, 104, 114, 50, 97, 72, 107, 57, 89, 65, 65, 81, 66, 65, 65, 81, 66, 65, 65, 81, 66, 65, 65,
            81, 66, 65, 65, 81, 66, 65, 65, 81, 66, 65, 65, 81, 66, 65, 65, 81, 66, 65, 65, 81, 66, 32, 65, 65, 81, 66, 65, 65, 81, 66, 65, 65, 65,
            65, 65, 65, 65, 65, 65, 68, 65, 56, 77, 67, 69, 119, 67, 81, 89, 70, 75, 119, 52, 68, 65, 104, 111, 70, 65, 65, 81, 85, 48, 112, 76, 90,
            43, 86, 110, 73, 89, 107, 50, 43, 83, 121, 75, 72, 77, 67, 116, 89, 112, 117, 121, 57, 32, 55, 70, 65, 69, 70, 68, 52, 72, 57, 54, 113,
            105, 122, 81, 112, 97, 74, 67, 51, 105, 75, 111, 104, 114, 102, 43, 119, 90, 84, 105, 80, 111, 65, 103, 70, 107, 65, 65, 65, 61 };

    @BeforeClass
    public static void beforeClass() {
        Security.addProvider(new BouncyCastleProvider());
    }
    

    @Test
    public void testBase64Small() throws Exception {
        // Testcert is on long line of base 64 encoded stuff
        byte[] certBytes = Base64.decode(testcert_oneline.getBytes());
        assertNotNull(certBytes);
        // This should be a cert
        Certificate cert = X509CertificateTools.parseCertificate(BouncyCastleProvider.PROVIDER_NAME, certBytes);
        assertNotNull(cert);
        // Base64 encode it again
        byte[] encBytes = Base64.encode(cert.getEncoded(), false);
        assertEquals(new String(encBytes), testcert_oneline);
        // Testcert_crlf has \n after each line
        certBytes = Base64.decode(testcert_crlf.getBytes());
        assertNotNull(certBytes);
        // This should be a cert
        cert = X509CertificateTools.parseCertificate(BouncyCastleProvider.PROVIDER_NAME, certBytes);
        assertNotNull(cert);
        // Base64 encode it again
        encBytes = Base64.encode(cert.getEncoded(), true);
        assertEquals(new String(encBytes), testcert_crlf);
        // This is the same method as above
        encBytes = Base64.encode(cert.getEncoded());
        assertEquals(new String(encBytes), testcert_crlf);
    }

    @Test
    public void testBase64Long() throws Exception {
        // This one has spaces in it
        byte[] bytes = Base64.decode(longMsg);
        assertNotNull(bytes);
        byte[] encBytes = Base64.encode(bytes, false);
        String str1 = new String(encBytes);
        String str2 = new String(longMsg);
        // Should not be same, str2 has blanks in it
        assertFalse(str1 == str2);
        str2 = StringUtils.deleteWhitespace(str2);
        // now it should be same
        assertEquals(str1, str2);
    }

    @Test(expected = DecoderException.class)
    public void testIncorrectPadding1() {
        Base64.decode("DAxFSkJDQSBTYW".getBytes(StandardCharsets.UTF_8));
    }

    @Test(expected = DecoderException.class)
    public void testIncorrectPadding2() {
        Base64.decode("DAxFSkJDQSBTYW=".getBytes(StandardCharsets.UTF_8));
    }
}
