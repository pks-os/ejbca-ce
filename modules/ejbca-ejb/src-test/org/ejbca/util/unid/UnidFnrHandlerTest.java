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

package org.ejbca.util.unid;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.Date;
import java.util.Vector;

import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.asn1.x509.X509Name;
import org.ejbca.core.protocol.ExtendedUserDataHandler.HandlerException;
import org.ejbca.core.protocol.IRequestMessage;
import org.ejbca.core.protocol.IResponseMessage;
import org.ejbca.util.unid.UnidFnrHandler.Storage;

import junit.framework.TestCase;

/**
 * Testing of {@link UnidFnrHandler} .
 * @author primelars
 * @version $Id$
 */
public class UnidFnrHandlerTest extends TestCase {
    public void test01() throws Exception {
    	final String unidPrefix = "1234-5678-";
    	final String fnr = "90123456789";
    	final String lra = "01234";
    	final MyStorage storage = new MyStorage(unidPrefix, fnr, lra);
    	final IRequestMessage reqIn = new MyIRequestMessage(fnr+'-'+lra);
    	final UnidFnrHandler handler = new UnidFnrHandler(storage);
    	final IRequestMessage reqOut = handler.processRequestMessage(reqIn, unidPrefix+"_a_profile_name");
    	assertEquals(storage.unid, reqOut.getRequestX509Name().getValues(X509Name.SN).firstElement());
    }
	private static class MyStorage implements Storage {
		final private String unidPrefix;
		final private String fnr;
		final private String lra;
		String unid;
		MyStorage( String _unidPrefix, String _fnr, String _lra) {
			this.unidPrefix = _unidPrefix;
			this.fnr = _fnr;
			this.lra = _lra;
		}
		@Override
		public void storeIt(String _unid, String _fnr) throws HandlerException {
			assertEquals(this.fnr, _fnr);
			assertEquals(this.unidPrefix, _unid.substring(0, 10));
			assertEquals(this.lra, _unid.substring(10, 15));
			this.unid = _unid;
		}
	}
	private static class MyIRequestMessage implements IRequestMessage {
		final X509Name dn;

		MyIRequestMessage(String serialNumber) {
			final Vector<DERObjectIdentifier> oids = new Vector<DERObjectIdentifier>();
			final Vector<String> values = new Vector<String>();
			oids.add(X509Name.SN);
			values.add(serialNumber);
			this.dn = new X509Name(oids, values);
		}
		@Override
		public String getUsername() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getPassword() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getIssuerDN() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public BigInteger getSerialNo() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getRequestDN() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public X509Name getRequestX509Name() {
			return this.dn;
		}

		@Override
		public String getRequestAltNames() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Date getRequestValidityNotBefore() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Date getRequestValidityNotAfter() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public X509Extensions getRequestExtensions() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getCRLIssuerDN() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public BigInteger getCRLSerialNo() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public PublicKey getRequestPublicKey() throws InvalidKeyException,
				NoSuchAlgorithmException, NoSuchProviderException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean verify() throws InvalidKeyException,
				NoSuchAlgorithmException, NoSuchProviderException {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean requireKeyInfo() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void setKeyInfo(Certificate cert, PrivateKey key, String provider) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public int getErrorNo() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public String getErrorText() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getSenderNonce() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getTransactionId() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public byte[] getRequestKeyInfo() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getPreferredDigestAlg() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean includeCACert() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public int getRequestType() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getRequestId() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public IResponseMessage createResponseMessage(Class responseClass,
				IRequestMessage req, Certificate cert, PrivateKey signPriv,
				String provider) {
			// TODO Auto-generated method stub
			return null;
		}
	}
}
