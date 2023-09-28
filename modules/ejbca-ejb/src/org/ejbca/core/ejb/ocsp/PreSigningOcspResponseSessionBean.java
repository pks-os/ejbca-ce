/*************************************************************************
 *                                                                       *
 *  EJBCA - Proprietary Modules: Enterprise Certificate Authority        *
 *                                                                       *
 *  Copyright (c), PrimeKey Solutions AB. All rights reserved.           *
 *  The use of the Proprietary Modules are subject to specific           *
 *  commercial license terms.                                            *
 *                                                                       *
 *************************************************************************/
package org.ejbca.core.ejb.ocsp;

import org.cesecore.certificates.ca.CA;
import org.cesecore.certificates.certificate.BaseCertificateData;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.math.BigInteger;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.cesecore.certificates.certificate.CertificateConstants.DEFAULT_CERTID_HASH_ALGORITHM;
import static org.ejbca.util.CAUtils.isDoPreProduceOcspResponses;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class PreSigningOcspResponseSessionBean implements PreSigningOcspResponseSessionLocal {

	@EJB
	private OcspResponseGeneratorSessionLocal ocspResponseGeneratorSession;

	@Override
	public boolean isOcspExists(Integer caId, String serialNumber) {
		return ocspResponseGeneratorSession.isOcspExists(caId, serialNumber);
	}

	@Override
	public void deleteOcspDataByCaIdSerialNumber(final int caId, final String serialNumber) {
		ocspResponseGeneratorSession.deleteOcspDataByCaIdSerialNumber(caId, serialNumber);
	}

	@Override
	public void preSignOcspResponse(CA ca, BaseCertificateData certData) {
		if (isDoPreProduceOcspResponses(ca)) {
			preSign(ca, certData);
		}
	}

	private void preSign(CA ca, BaseCertificateData certData) {
		List<Certificate> certificateChain = ca.getCertificateChain();
		if (!certificateChain.isEmpty()) {
			Optional.ofNullable(certificateChain.get(0)).ifPresent(preSignOcspResponse(certData));
		}
	}

	private Consumer<Certificate> preSignOcspResponse(BaseCertificateData certData) {
		return caCertFromTheChain -> {
			if (caCertFromTheChain instanceof X509Certificate) {
				ocspResponseGeneratorSession.preSignOcspResponse((X509Certificate) caCertFromTheChain,
						new BigInteger(certData.getSerialNumber()), true, true, DEFAULT_CERTID_HASH_ALGORITHM);
			}
		};
	}
}
