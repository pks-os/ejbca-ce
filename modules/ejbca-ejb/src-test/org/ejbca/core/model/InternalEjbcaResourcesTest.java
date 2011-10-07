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

package org.ejbca.core.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @version $Id$
 */
public class InternalEjbcaResourcesTest {

	private static final String TEST_RESOURCE_PATH = "/intresources";
	// Classpath issues, use "src/intresources" when running from within eclipse
	//private static final String TEST_RESOURCE_PATH = "src/intresources";

	@Test
    public void testGetLocalizedMessageString() {
        InternalEjbcaResources intres = new InternalEjbcaResources(TEST_RESOURCE_PATH);
        String res = intres.getLocalizedMessage("test.testmsg");
        assertEquals("Test ENG", res);
        assertEquals("Test ENG", intres.getLocalizedMessageCs("test.testmsg").toString());
        // This message will only exist in the secondary language file
        res = intres.getLocalizedMessage("test.testmsgsv");
        assertEquals("Test SV", res);
        assertEquals("Test SV", intres.getLocalizedMessageCs("test.testmsgsv").toString());
    }

	@Test
    public void testNonExistingLocalizedMessageString() {
        InternalEjbcaResources intres = new InternalEjbcaResources(TEST_RESOURCE_PATH);
        String res = intres.getLocalizedMessage("test.foo");
        assertEquals("test.foo", res);
        assertEquals("test.foo", intres.getLocalizedMessageCs("test.foo").toString());
    }

	@Test
    public void testGetLocalizedMessageStringObject() {
        InternalEjbcaResources intres = new InternalEjbcaResources(TEST_RESOURCE_PATH);
        String res = intres.getLocalizedMessage("test.testparams", new Long(1), new Integer(3), "hi", new Boolean(true), "bye");
        assertEquals("Test 1 3 hi true bye message 1", res);
        assertEquals("Test 1 3 hi true bye message 1", intres.getLocalizedMessageCs("test.testparams", new Long(1), new Integer(3), "hi", new Boolean(true), "bye").toString());
    }

	@Test
    public void testGetLocalizedMessageStringObjectWithNull() {
        InternalEjbcaResources intres = new InternalEjbcaResources(TEST_RESOURCE_PATH);
        String res = intres.getLocalizedMessage("test.testparams", null, new Integer(3), null, new Boolean(true), "bye");
        assertEquals("Test  3  true bye message ", res);
        assertEquals("Test  3  true bye message ", intres.getLocalizedMessageCs("test.testparams", null, new Integer(3), null, new Boolean(true), "bye").toString());

        res = intres.getLocalizedMessage("test.testparams");
        assertEquals("Test      message ", res);
        assertEquals("Test      message ", intres.getLocalizedMessageCs("test.testparams").toString());
    }

	@Test
    public void testMessageStringWithExtraParameter() {
        InternalEjbcaResources intres = new InternalEjbcaResources(TEST_RESOURCE_PATH);
        String res = intres.getLocalizedMessage("test.testmsgsv");
        assertEquals("Test SV", res);
        assertEquals("Test SV", intres.getLocalizedMessageCs("test.testmsgsv").toString());
        res = intres.getLocalizedMessage("test.testmsgsv", "foo $bar \\haaaar");
        assertEquals("Test SV", res);
        assertEquals("Test SV", intres.getLocalizedMessageCs("test.testmsgsv", "foo $bar \\haaaar").toString());
    }
    
	@Test
    public void testCeSecoreMessage() {
        InternalEjbcaResources intres = new InternalEjbcaResources(TEST_RESOURCE_PATH);
        String res = intres.getLocalizedMessage("raadmin.testparams", new Long(1), Integer.valueOf(3), "hi", new Boolean(true), "bye");
        assertEquals("Test 1 3 hi true bye message 1", res);    	
        assertEquals("Test 1 3 hi true bye message 1", intres.getLocalizedMessageCs("raadmin.testparams", new Long(1), Integer.valueOf(3), "hi", new Boolean(true), "bye").toString());
    }

}
