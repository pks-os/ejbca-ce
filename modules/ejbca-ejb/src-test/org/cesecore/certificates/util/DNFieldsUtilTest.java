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

package org.cesecore.certificates.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.cesecore.certificates.util.dn.DNFieldsUtil;
import org.junit.Test;

/**
 * @version $Id$
 */
public class DNFieldsUtilTest {
    final private static String trickyValue1=" 10/2=5; 2 backs and a comma\\\\\\\\\\, 8/2=4 2 backs\\\\\\\\";// last comma is end of value since it is a even number (4) of \ before
    final private static String trickyValue2="\\,";// a single comma
    final private static String trickyValue3="\\\\\\\\\\\\\\,";// 3 backs and a comma
    final private static String trickyValue4="\\\\\\\\\\\\";// 3 backs
    final private static String trickyValue5="\\,\\\\\\\\\\\\\\,";// comma 3 backs comma
    final private static String trickyValue6="\\,\\\\\\\\\\\\";// comma 3 backs
    final private static String trickyValue7="\\,\\,\\,\\,\\,\\,";// 6 commas
    final private static String trickyValue8="\\\\\\,\\,\\,\\\\\\,\\,\\,\\\\";// 1 back, 3 commas, 1 back, 3 commas, 1 back
    final private static String key1 = "key1=";
    final private static String key2 = "key2=";
    final private static String c = ",";
    final private static String cKey1 = c+key1;
    final private static String cKey2 = c+key2;
    final private static String empty1 = key1+c;
    final private static String empty2 = key2+c;
    final private static String originalDN = key2+trickyValue4+c+empty1+empty2+empty1+empty1+key1+trickyValue1+c+empty1+key2+trickyValue5+c+empty1+empty2+key1+trickyValue2+cKey2+trickyValue6+c+empty1+key2+trickyValue7+cKey1+trickyValue3+c+empty1+empty2+empty1+key2+trickyValue8+c+empty1+empty2+empty2+empty1+empty1+empty2+empty1+key2;
    final private static String trailingSpacesRemovedDN = key2+trickyValue4+c+empty1+empty2+empty1+empty1+key1+trickyValue1+c+empty1+key2+trickyValue5+c+empty1+empty2+key1+trickyValue2+cKey2+trickyValue6+c+empty1+key2+trickyValue7+cKey1+trickyValue3+c+empty2+key2+trickyValue8+c;
    final private static String allSpacesRemovedDN = key2+trickyValue4+cKey1+trickyValue1+cKey2+trickyValue5+cKey1+trickyValue2+cKey2+trickyValue6+cKey2+trickyValue7+cKey1+trickyValue3+cKey2+trickyValue8+c;
    final private static String defaultEmptyBefore = "UNSTRUCTUREDNAME=, DN=, POSTALADDRESS=, NAME=, UID=, OU=, 1.3.6.1.4.1.18838.1.1=, 1.3.6.1.4.1.4710.1.3.2=, ST=, UNSTRUCTUREDADDRESS=, BUSINESSCATEGORY=, STREET=, CN=test1, POSTALCODE=, O=, PSEUDONYM=, DC=, SURNAME=, C=, INITIALS=, SN=, L=, GIVENNAME=, TELEPHONENUMBER=, T=, DC=";
    final private static String defaultEmptyAfter = "CN=test1";
    final private static String simpleBeforeAfter = "CN=userName,O=linagora";
    final private static String simple2Before = "CN=userName,O=, O=linagora, O=";
    final private static String simple2AfterA = "CN=userName,O=linagora";
    final private static String simple2AfterT = "CN=userName,O=, O=linagora";

    @Test
    public void testRemoveAllEmpties() throws Exception {
    	assertEquals(allSpacesRemovedDN, removeEmpties(originalDN, false));
    	assertEquals(defaultEmptyAfter, removeEmpties(defaultEmptyBefore, false));
    	assertEquals(simpleBeforeAfter, removeEmpties(simpleBeforeAfter, false));
    	assertEquals(simple2AfterA, removeEmpties(simple2Before, false));
    }
    
    @Test
    public void testRemoveTrailingEmpties() {
    	assertEquals(trailingSpacesRemovedDN, removeEmpties(originalDN, true));
    	assertEquals(defaultEmptyAfter, removeEmpties(defaultEmptyBefore, true));
    	assertEquals(simpleBeforeAfter, removeEmpties(simpleBeforeAfter, true));
    	assertEquals(simple2AfterT, removeEmpties(simple2Before, true));
    }
    
    @Test
    public void testRemoveSingleEmpty() {
        assertEquals("", DNFieldsUtil.removeAllEmpties("CN="));
    }
    @Test
    public void testRemoveSingleEscapedComma() {
        assertEquals("CN=\\,", DNFieldsUtil.removeAllEmpties("CN=\\,"));
    }


    @Test
    public void testRemoveTrailingEmptiesError() {
    	final String BAD_DN_STRING = "ddddddd=, sdfdf, sdfsdf=44";
    	final String FAIL_MESSAGE = "Behavioral change in DNFieldsUtil.";
    	try {
    		removeEmpties(BAD_DN_STRING, true);
    		fail(FAIL_MESSAGE);
    	} catch (Exception e) {
    		// What we expect if something goes wrong
    	}
    	try {
    		removeEmpties(BAD_DN_STRING, false);
    		fail(FAIL_MESSAGE);
    	} catch (Exception e) {
    		// What we expect if something goes wrong
    	}
    }

    private String removeEmpties(String dn, boolean onlyTrailing) {
    	final StringBuilder sb2 = new StringBuilder();
    	final StringBuilder sb1 = DNFieldsUtil.removeEmpties(dn, sb2, true);
    	final String removedEmpties1 = DNFieldsUtil.removeAllEmpties(dn);
    	final String removedEmpties2 = sb2.toString();
		assertEquals(removedEmpties1, removedEmpties2);
    	if (sb1 == null) {
    		return removedEmpties2;
    	}
    	if (onlyTrailing) {
    		return sb1.toString();
    	}
		return removedEmpties2;
    }
}
