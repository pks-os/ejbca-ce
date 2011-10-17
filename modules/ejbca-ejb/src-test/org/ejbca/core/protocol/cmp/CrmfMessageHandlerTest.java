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
package org.ejbca.core.protocol.cmp;

import static org.junit.Assert.assertEquals;

import org.cesecore.authentication.tokens.AlwaysAllowLocalAuthenticationToken;
import org.cesecore.authentication.tokens.UsernamePrincipal;
import org.cesecore.certificates.endentity.EndEntityInformation;
import org.ejbca.core.ejb.ca.sign.SignSessionRemote;
import org.ejbca.core.ejb.ra.EndEntityAccessSessionRemote;
import org.ejbca.util.SimpleMock;
import org.junit.Test;

/**
 * Unit tests for CrmfMessageHandler. 
 * 
 * This test verifies that the request has it's username set to the same as an existing
 * user's with the same subject DN. 
 * 
 * @author mikek
 * @version $Id$
 */
public class CrmfMessageHandlerTest {

    private static String USER_NAME = "foobar";


    @Test
    public void testExtractUserNameComponent() {
        CrmfMessageHandler crmfMessageHandler = new CrmfMessageHandler();
        /*
         * Some slight reflective manipulation of crmfMessageHandler here in
         * order to get around the fact that we're not running any of the logic
         * in its usual constructor, instead using the empty default one.
         */
        SimpleMock.inject(crmfMessageHandler, "admin", new AlwaysAllowLocalAuthenticationToken(new UsernamePrincipal("CrmfMessageHandlerTest")));
        final EndEntityAccessSessionRemote endEntitySessionMock = new SimpleMock(EndEntityAccessSessionRemote.class) {{
        	map("findUserBySubjectDN", new EndEntityInformation() {
				private static final long serialVersionUID = 1L;
				public String getUsername() { return USER_NAME; };
			});
        }}.mock();
        SimpleMock.inject(crmfMessageHandler, "endEntityAccessSession", endEntitySessionMock);
        SimpleMock.inject(crmfMessageHandler, "signSession", new SimpleMock(SignSessionRemote.class).mock());
        final CrmfRequestMessage requestMock = new CrmfRequestMessage() {
            private static final long serialVersionUID = 1L;
            public String getSubjectDN() {
                return "foo";	// Just return something that isn't null
            }
        };
        try {
            crmfMessageHandler.handleMessage(requestMock);
        } catch (NullPointerException e) {
            // NOPMD: Since we don't pass an actual CMP message her it will fail with an NPE
            // But the setting of username that we test here is done in anyway. 
            // Since this is a JUnit test we can't do the calling of session beans that crmfMessageHandler does
        }
        assertEquals("crmfMessageHandler.handleMessage did not process user name correctly", USER_NAME, requestMock.getUsername());
    }
}
