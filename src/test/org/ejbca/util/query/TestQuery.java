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

package org.ejbca.util.query;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.ejbca.core.model.ra.UserDataConstants;


/**
 * Tests the CertTools class .
 *
 * @version $Id: TestQuery.java,v 1.1 2007-05-04 09:06:38 anatom Exp $
 */
public class TestQuery extends TestCase {
    private static Logger log = Logger.getLogger(TestQuery.class);
    /**
     * Creates a new Test object.
     *
     */
    public TestQuery(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    /**
     * @throws Exception DOCUMENT ME!
     */
    public void test01TestUserQuery() throws Exception {
        log.debug(">test01TestUserQuery()");
        Query query = new Query(Query.TYPE_USERQUERY);
        query.add(UserMatch.MATCH_WITH_STATUS, BasicMatch.MATCH_TYPE_EQUALS, Integer.toString(UserDataConstants.STATUS_NEW));
        String str = query.getQueryString();
        assertEquals("status = 10", str);

        query = new Query(Query.TYPE_USERQUERY);
        query.add(UserMatch.MATCH_WITH_USERNAME, BasicMatch.MATCH_TYPE_EQUALS, "foo");
        str = query.getQueryString();
        assertEquals("username = 'foo'", str);
        
        log.debug("<test01TestUserQuery()");
    }

}
