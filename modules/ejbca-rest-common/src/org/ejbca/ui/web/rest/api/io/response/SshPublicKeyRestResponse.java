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
package org.ejbca.ui.web.rest.api.io.response;

import java.security.cert.Certificate;
import java.util.Date;
import java.util.List;

import org.cesecore.certificates.ca.CADoesntExistsException;
import org.cesecore.certificates.ca.CAInfo;
import org.cesecore.util.CertTools;

/**
 * A class containing a public key in SSH format
 *
 */
public class SshPublicKeyRestResponse {

    private String name;


    /**
     * Simple constructor.
     */
    public SshPublicKeyRestResponse() {
    }

    private SshPublicKeyRestResponse(final String name) {
        this.name = name;
    }

    /**
     * Return a builder instance for this class.
     *
     * @return builder instance for this class.
     */
    public static CaInfoRestResponseBuilder builder() {
        return new CaInfoRestResponseBuilder();
    }


    /**
     * Return the name.
     *
     * @return name.
     */
    public String getName() {
        return name;
    }


    /**
     * Sets a name.
     *
     * @param name name.
     */
    public void setName(final String name) {
        this.name = name;
    }


    /**
     * Builder of this class.
     */
    public static class SshPublicKeyRestResponseResponseBuilder {

        private Integer id;
        private String name;
        private String subjectDn;
        private String issuerDn;
        private Date expirationDate;

        CaInfoRestResponseBuilder() {
        }

        /**
         * Sets an identifier in this builder.
         *
         * @param id identifier.
         *
         * @return instance of this builder.
         */
        public CaInfoRestResponseBuilder id(final Integer id) {
            this.id = id;
            return this;
        }

        /**
         * Sets a name in this builder.
         *
         * @param name name.
         *
         * @return instance of this builder.
         */
        public CaInfoRestResponseBuilder name(final String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets a Subject DN in this builder.
         *
         * @param subjectDn Subject DN.
         *
         * @return instance of this builder.
         */
        public CaInfoRestResponseBuilder subjectDn(final String subjectDn) {
            this.subjectDn = subjectDn;
            return this;
        }

        /**
         * Sets an Issuer DN in this builder.
         *
         * @param issuerDn Issuer DN.
         *
         * @return instance of this builder.
         */
        public CaInfoRestResponseBuilder issuerDn(final String issuerDn) {
            this.issuerDn = issuerDn;
            return this;
        }

        /**
         * Sets an expiration date in this builder.
         *
         * @param expirationDate expiration date.
         *
         * @return instance of this builder.
         */
        public CaInfoRestResponseBuilder expirationDate(final Date expirationDate) {
            this.expirationDate = expirationDate;
            return this;
        }

        /**
         * Builds an instance of CaInfoRestResponse using this builder.
         *
         * @return instance of CaInfoRestResponse using this builder.
         */
        public SshPublicKeyRestResponse build() {
            return new SshPublicKeyRestResponse(
                    id,
                    name,
                    subjectDn,
                    issuerDn,
                    expirationDate
            );
        }
    }

    /**
     * Returns a converter instance for this class.
     *
     * @return instance of converter for this class.
     */
    public static CaInfoRestResponseConverter converter() {
        return new CaInfoRestResponseConverter();
    }

    /**
     * Converter of this class.
     */
    public static class CaInfoRestResponseConverter {

        CaInfoRestResponseConverter() {
        }

        /**
         * Converts a non-null instance of CAInfo into CaInfoRestResponse.
         *
         * @param caInfo CAInfo.
         *
         * @return CaInfoRestResponse.
         */
        public SshPublicKeyRestResponse toRestResponse(final CAInfo caInfo) throws CADoesntExistsException {
            return SshPublicKeyRestResponse.builder()
                    .id(caInfo.getCAId())
                    .name(caInfo.getName())
                    .subjectDn(caInfo.getSubjectDN())
                    .issuerDn(extractIssuerDn(caInfo))
                    .expirationDate(caInfo.getExpireTime())
                    .build();
        }

        // Extracts the Issuer DN using certificate chain
        private String extractIssuerDn(final CAInfo caInfo) throws CADoesntExistsException {
            final List<Certificate> caInfoCertificateChain = caInfo.getCertificateChain();
            if(caInfoCertificateChain != null && !caInfoCertificateChain.isEmpty()) {
                // Get last it should be RootCA
                final Certificate rootCa = caInfoCertificateChain.get(caInfoCertificateChain.size() - 1);
                return CertTools.getIssuerDN(rootCa);
            }
            throw new CADoesntExistsException("Cannot extract the Issuer DN for CA certificate with id " + caInfo.getCAId());
        }
    }
}
