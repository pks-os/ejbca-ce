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
package org.ejbca.ui.cli.ca;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authentication.tokens.UsernamePrincipal;
import org.cesecore.certificates.ca.CaSessionRemote;
import org.cesecore.mock.authentication.tokens.TestAlwaysAllowLocalAuthenticationToken;
import org.cesecore.util.CryptoProviderTools;
import org.cesecore.util.EjbRemoteHelper;
import org.ejbca.core.ejb.ca.CaTestCase;
import org.ejbca.ui.cli.infrastructure.command.CommandResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @version $Id$
 *
 */
public class CaGetFieldValueCommandTest {

    private static final String CA_NAME = "CaGetFieldValueCommandTest";

    private CaGetFieldValueCommand caGetFieldValueCommand;

    private AuthenticationToken admin = new TestAlwaysAllowLocalAuthenticationToken(new UsernamePrincipal("CaGetFieldValueCommandTest"));

    private CaSessionRemote caSession = EjbRemoteHelper.INSTANCE.getRemoteSession(CaSessionRemote.class);

    int caid = 0;

    @Before
    public void setUp() throws Exception {
        CryptoProviderTools.installBCProviderIfNotAvailable();
        caGetFieldValueCommand = new CaGetFieldValueCommand();
        CaTestCase.removeTestCA(CA_NAME);
        CaTestCase.createTestCA(CA_NAME);
        caid = caSession.getCAInfo(admin, CA_NAME).getCAId();
    }

    @After
    public void tearDown() throws Exception {
        CaTestCase.removeTestCA(caid);
    }

    @Test
    public void testSanity() {
        try {
            assertEquals("CA get value command is broken.", CommandResult.SUCCESS, caGetFieldValueCommand.execute("--caname", CA_NAME, "--value", "CRLPeriod"));
        } catch (Exception e) {
            fail("CA get value command is broken: " + e.getMessage());
        }
    }
}
