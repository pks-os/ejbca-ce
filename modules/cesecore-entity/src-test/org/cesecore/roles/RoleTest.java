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
package org.cesecore.roles;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.junit.Test;

/**
 * Unit tests on methods in Role.
 * 
 * @version $Id$
 */
public class RoleTest {

    private static final Logger log = Logger.getLogger(RoleTest.class);
    private static final String ERRMSG_ALLOWED_TO_DENIED = "Access granted that should have been denied.";
    private static final String ERRMSG_DENIED_TO_ALLOWED = "Access denied that should have been granted.";

    @Test
    public void testHasAccessToResource() {
        log.trace(">testHasAccessToResource");
        final Role role = new Role(null, "role");
        role.getAccessRules().put("/fuu", Role.STATE_ALLOW);
        role.getAccessRules().put("/foo/bar", Role.STATE_DENY);
        role.getAccessRules().put("/xyz", Role.STATE_DENY);
        role.getAccessRules().put("/xyz_abc", Role.STATE_ALLOW);
        role.getAccessRules().put("/1/2/3/4", Role.STATE_DENY);
        role.getAccessRules().put("/1", Role.STATE_ALLOW);
        role.getAccessRules().put("/1/2", Role.STATE_ALLOW);
        role.getAccessRules().put("/a/b/c/d", Role.STATE_ALLOW);
        role.getAccessRules().put("/a/b", Role.STATE_DENY);
        role.getAccessRules().put("/", Role.STATE_DENY);
        debugLogAccessRules(role);
        hasAccessToResourcesInternal(role);
        role.normalizeAccessRules();
        debugLogAccessRules(role);
        hasAccessToResourcesInternal(role);
        role.minimizeAccessRules();
        debugLogAccessRules(role);
        hasAccessToResourcesInternal(role);
        assertNull("Minimization did not remove deny rule.", role.getAccessRules().get("/xyz"));
        assertNull("Minimization did not remove allow rule.", role.getAccessRules().get("/1/2"));
        assertNull("Minimization did not remove top deny rule.", role.getAccessRules().get("/"));
        log.trace("<testHasAccessToResource");
    }
    
    private void hasAccessToResourcesInternal(final Role role) {
        // Check explicitly configured access
        assertTrue( ERRMSG_DENIED_TO_ALLOWED, role.hasAccessToResource("/fuu"));
        assertFalse(ERRMSG_ALLOWED_TO_DENIED, role.hasAccessToResource("/foo/bar"));
        assertFalse(ERRMSG_ALLOWED_TO_DENIED, role.hasAccessToResource("/xyz"));
        assertTrue( ERRMSG_DENIED_TO_ALLOWED, role.hasAccessToResource("/xyz_abc"));
        assertTrue( ERRMSG_DENIED_TO_ALLOWED, role.hasAccessToResource("/a/b/c/d"));
        assertFalse(ERRMSG_ALLOWED_TO_DENIED, role.hasAccessToResource("/a/b"));
        assertFalse(ERRMSG_ALLOWED_TO_DENIED, role.hasAccessToResource("/"));
        assertTrue( ERRMSG_DENIED_TO_ALLOWED, role.hasAccessToResource("/1"));
        assertTrue( ERRMSG_DENIED_TO_ALLOWED, role.hasAccessToResource("/1/2"));
        assertFalse(ERRMSG_ALLOWED_TO_DENIED, role.hasAccessToResource("/1/2/3/4"));
        // Check implicitly configured access
        assertTrue( ERRMSG_DENIED_TO_ALLOWED, role.hasAccessToResource("/fuu/anything"));
        assertTrue( ERRMSG_DENIED_TO_ALLOWED, role.hasAccessToResource("/fuu/anything/something"));
        assertFalse(ERRMSG_ALLOWED_TO_DENIED, role.hasAccessToResource("/xyz/abc"));
        assertTrue( ERRMSG_DENIED_TO_ALLOWED, role.hasAccessToResource("/xyz_abc/foo"));
        assertTrue( ERRMSG_DENIED_TO_ALLOWED, role.hasAccessToResource("/a/b/c/d/e"));
        assertFalse(ERRMSG_ALLOWED_TO_DENIED, role.hasAccessToResource("/a/b/c"));
        assertFalse(ERRMSG_ALLOWED_TO_DENIED, role.hasAccessToResource("/a/b/f"));
        assertFalse(ERRMSG_ALLOWED_TO_DENIED, role.hasAccessToResource("/a"));
        assertTrue( ERRMSG_DENIED_TO_ALLOWED, role.hasAccessToResource("/1/2/3"));
        assertFalse(ERRMSG_ALLOWED_TO_DENIED, role.hasAccessToResource("/1/2/3/4/5"));
        // Check explicitly configured access (normalized form)
        assertTrue( ERRMSG_DENIED_TO_ALLOWED, role.hasAccessToResource("/fuu/"));
        assertFalse(ERRMSG_ALLOWED_TO_DENIED, role.hasAccessToResource("/foo/bar/"));
        assertFalse(ERRMSG_ALLOWED_TO_DENIED, role.hasAccessToResource("/xyz/"));
        assertTrue( ERRMSG_DENIED_TO_ALLOWED, role.hasAccessToResource("/xyz_abc/"));
        assertTrue( ERRMSG_DENIED_TO_ALLOWED, role.hasAccessToResource("/a/b/c/d/"));
        assertFalse(ERRMSG_ALLOWED_TO_DENIED, role.hasAccessToResource("/a/b/"));
        assertFalse(ERRMSG_ALLOWED_TO_DENIED, role.hasAccessToResource("/"));
        assertTrue( ERRMSG_DENIED_TO_ALLOWED, role.hasAccessToResource("/1/"));
        assertTrue( ERRMSG_DENIED_TO_ALLOWED, role.hasAccessToResource("/1/2/"));
        assertFalse(ERRMSG_ALLOWED_TO_DENIED, role.hasAccessToResource("/1/2/3/4/"));
        // Check implicitly configured access (normalized form)
        assertTrue( ERRMSG_DENIED_TO_ALLOWED, role.hasAccessToResource("/fuu/anything/"));
        assertTrue( ERRMSG_DENIED_TO_ALLOWED, role.hasAccessToResource("/fuu/anything/something/"));
        assertFalse(ERRMSG_ALLOWED_TO_DENIED, role.hasAccessToResource("/xyz/abc/"));
        assertTrue( ERRMSG_DENIED_TO_ALLOWED, role.hasAccessToResource("/xyz_abc/foo/"));
        assertTrue( ERRMSG_DENIED_TO_ALLOWED, role.hasAccessToResource("/a/b/c/d/e/"));
        assertFalse(ERRMSG_ALLOWED_TO_DENIED, role.hasAccessToResource("/a/b/c/"));
        assertFalse(ERRMSG_ALLOWED_TO_DENIED, role.hasAccessToResource("/a/b/f/"));
        assertFalse(ERRMSG_ALLOWED_TO_DENIED, role.hasAccessToResource("/a/"));
        assertTrue( ERRMSG_DENIED_TO_ALLOWED, role.hasAccessToResource("/1/2/3/"));
        assertFalse(ERRMSG_ALLOWED_TO_DENIED, role.hasAccessToResource("/1/2/3/4/5/"));
    }
    
    private void debugLogAccessRules(final Role role) {
        log.debug("Role: " + role.getRoleNameFull());
        debugLogAccessRules(role.getAccessRules());
    }
    
    private void debugLogAccessRules(final HashMap<String, Boolean> accessRules) {
        List<Entry<String, Boolean>> accessRulesList = new ArrayList<>(accessRules.entrySet());
        Collections.sort(accessRulesList, new Comparator<Entry<String, Boolean>>() {
            @Override
            public int compare(Entry<String, Boolean> o1, Entry<String, Boolean> o2) {
                return o1.getKey().compareTo(o2.getKey());
            }
        });
        for (final Entry<String,Boolean> entry : accessRulesList) {
            log.debug(" " + entry.getKey() + ":" + (entry.getValue().booleanValue()?"allow":"deny"));
        }
    }
    
    /** Make sure that access to root is never given by mistake. */
    @Test
    public void testNoDefaultAccessToRoot() {
        log.trace(">testNoDefaultAccessToRoot");
        final Role role = new Role(null, "role");
        role.getAccessRules().put("/fuu", Role.STATE_ALLOW);
        assertFalse(ERRMSG_ALLOWED_TO_DENIED, role.hasAccessToResource("/"));
        log.trace("<testNoDefaultAccessToRoot");
    }

    @Test
    public void testMergeOfAccessRules() {
        log.trace(">testMergeOfAccessRules");
        final HashMap<String, Boolean> accessRules1 = new HashMap<>();
        accessRules1.put("/a/", Role.STATE_ALLOW);
        accessRules1.put("/a/b/", Role.STATE_DENY);
        accessRules1.put("/b/", Role.STATE_DENY);
        accessRules1.put("/b/a/", Role.STATE_ALLOW);
        final HashMap<String, Boolean> accessRules2 = new HashMap<>();
        accessRules2.put("/a/", Role.STATE_ALLOW);
        accessRules2.put("/a/c/", Role.STATE_DENY);
        accessRules2.put("/c/", Role.STATE_ALLOW);
        accessRules2.put("/c/d/", Role.STATE_DENY);
        final HashMap<String, Boolean> accessRules = AccessRulesHelper.mergeTotalAccess(accessRules1, accessRules2);
        debugLogAccessRules(accessRules);
        assertFalse(ERRMSG_ALLOWED_TO_DENIED, AccessRulesHelper.hasAccessToResource(accessRules, "/"));
        assertTrue( ERRMSG_DENIED_TO_ALLOWED, AccessRulesHelper.hasAccessToResource(accessRules, "/a/"));
        assertTrue( ERRMSG_DENIED_TO_ALLOWED, AccessRulesHelper.hasAccessToResource(accessRules, "/a/b/"));
        assertTrue( ERRMSG_DENIED_TO_ALLOWED, AccessRulesHelper.hasAccessToResource(accessRules, "/a/c/"));
        assertFalse(ERRMSG_ALLOWED_TO_DENIED, AccessRulesHelper.hasAccessToResource(accessRules, "/b/"));
        assertTrue( ERRMSG_DENIED_TO_ALLOWED, AccessRulesHelper.hasAccessToResource(accessRules, "/b/a/"));
        assertTrue( ERRMSG_DENIED_TO_ALLOWED, AccessRulesHelper.hasAccessToResource(accessRules, "/c/"));
        assertFalse(ERRMSG_ALLOWED_TO_DENIED, AccessRulesHelper.hasAccessToResource(accessRules, "/c/d/"));
        log.trace("<testMergeOfAccessRules");
    }
}
