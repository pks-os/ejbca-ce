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
package org.ejbca.webtest.scenario;

import org.cesecore.authorization.AuthorizationDeniedException;
import org.ejbca.webtest.WebTestBase;
import org.ejbca.webtest.helper.AuditLogHelper;
import org.ejbca.webtest.helper.EndEntityProfileHelper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.openqa.selenium.WebDriver;

import java.util.Collections;

/**
 * Test to verify that End Entity Profile management operations work as expected.
 * <br/>
 * Reference: <a href="https://jira.primekey.se/browse/ECAQA-99>ECAQA-99</a>
 * 
 * @version $Id$
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class EcaQa99_EEPManagement extends WebTestBase {

    // Helpers
    private static EndEntityProfileHelper endEntityProfileHelper;
    private static AuditLogHelper auditLogHelper;
    // Test Data
    private static class TestData {
        static final String END_ENTITY_PROFILE_NAME = "ECAQA-99-EndEntityProfile";
        static final String END_ENTITY_PROFILE_NAME_CLONED = "ECAQA-99-EndEntityProfile-Cloned";
        static final String END_ENTITY_PROFILE_NAME_RENAMED = "ECAQA-99-EndEntityProfile-Renamed";
    }

    @BeforeClass
    public static void init() {
        // super
        beforeClass(true, null);
        final WebDriver webDriver = getWebDriver();
        // Init helpers
        endEntityProfileHelper = new EndEntityProfileHelper(webDriver);
        auditLogHelper = new AuditLogHelper(webDriver);
    }

    @AfterClass
    public static void exit() throws AuthorizationDeniedException {
        // Remove generated artifacts
        removeEndEntityProfileByName(TestData.END_ENTITY_PROFILE_NAME);
        removeEndEntityProfileByName(TestData.END_ENTITY_PROFILE_NAME_CLONED);
        removeEndEntityProfileByName(TestData.END_ENTITY_PROFILE_NAME_RENAMED);
        // super
        afterClass();
    }

    @Test
    public void stepA_addEEP() {
        // Update default timestamp
        auditLogHelper.initFilterTime();
        // Create EEP and enter edit mode
        endEntityProfileHelper.openPage(getAdminWebUrl());
        endEntityProfileHelper.addEndEntityProfile(TestData.END_ENTITY_PROFILE_NAME);
        // Edit EEP and save
        endEntityProfileHelper.openEditEndEntityProfilePage(TestData.END_ENTITY_PROFILE_NAME);
        endEntityProfileHelper.triggerUsernameAutoGenerated();
        endEntityProfileHelper.saveEndEntityProfile();
        // Assert that the EEP exists and then enter edit mode
        endEntityProfileHelper.assertEndEntityProfileNameExists(TestData.END_ENTITY_PROFILE_NAME);
        endEntityProfileHelper.openEditEndEntityProfilePage(TestData.END_ENTITY_PROFILE_NAME);
        endEntityProfileHelper.assertUsernameAutoGeneratedIsSelected(true);
        // Click 'Back to End Entity Profiles' link and assert EEP still exists
        endEntityProfileHelper.triggerBackToEndEntityProfiles();
        endEntityProfileHelper.assertIsOnStartPage();
        endEntityProfileHelper.assertEndEntityProfileNameExists(TestData.END_ENTITY_PROFILE_NAME);
        // Verify Audit Log
        auditLogHelper.openPage(getAdminWebUrl());
        auditLogHelper.assertLogEntryByEventText(
                "End Entity Profile Add",
                "Success",
                null,
                Collections.singletonList("End entity profile " + TestData.END_ENTITY_PROFILE_NAME + " added.")
        );
        auditLogHelper.assertLogEntryByEventText(
                "End Entity Profile Edit",
                "Success",
                null,
                Collections.singletonList("End entity profile " + TestData.END_ENTITY_PROFILE_NAME + " edited.")
        );
    }

    @Test
    public void stepB_addEEPClone() {
        // Update default timestamp
        auditLogHelper.initFilterTime();
        endEntityProfileHelper.openPage(getAdminWebUrl());
        // Clone EEP
        endEntityProfileHelper.cloneEndEntityProfile(TestData.END_ENTITY_PROFILE_NAME, TestData.END_ENTITY_PROFILE_NAME_CLONED);
        endEntityProfileHelper.assertEndEntityProfileNameExists(TestData.END_ENTITY_PROFILE_NAME_CLONED);
        endEntityProfileHelper.assertEndEntityProfileNameExists(TestData.END_ENTITY_PROFILE_NAME);
        // Verify Audit Log
        auditLogHelper.openPage(getAdminWebUrl());
        auditLogHelper.assertLogEntryByEventText(
                "End Entity Profile Add",
                "Success",
                null,
                Collections.singletonList(
                        "Added new end entity profile " + TestData.END_ENTITY_PROFILE_NAME_CLONED + " using profile " + TestData.END_ENTITY_PROFILE_NAME + " as template."
                )
        );
        // Rename EEP
        auditLogHelper.initFilterTime();
        endEntityProfileHelper.openPage(getAdminWebUrl());
        endEntityProfileHelper.renameEndEntityProfile(TestData.END_ENTITY_PROFILE_NAME, TestData.END_ENTITY_PROFILE_NAME_RENAMED);
        endEntityProfileHelper.assertEndEntityProfileNameExists(TestData.END_ENTITY_PROFILE_NAME_RENAMED);
        endEntityProfileHelper.assertEndEntityProfileNameDoesNotExist(TestData.END_ENTITY_PROFILE_NAME);
        // Verify Audit Log
        auditLogHelper.openPage(getAdminWebUrl());
        auditLogHelper.assertLogEntryByEventText(
                "End Entity Profile Rename",
                "Success",
                null,
                Collections.singletonList(
                        "End entity profile " + TestData.END_ENTITY_PROFILE_NAME + " renamed to " + TestData.END_ENTITY_PROFILE_NAME_RENAMED + "."
                )
        );
    }

    @Test
    public void stepC_removeEEP() throws InterruptedException {
        // Update default timestamp
        auditLogHelper.initFilterTime();
        // Try to remove EEP
        endEntityProfileHelper.openPage(getAdminWebUrl());
        endEntityProfileHelper.deleteEndEntityProfile(TestData.END_ENTITY_PROFILE_NAME_RENAMED);
        // Dismiss alert, assert that EEP still exists
        endEntityProfileHelper.confirmEndEntityProfileDeletion(false);
        endEntityProfileHelper.assertEndEntityProfileNameExists(TestData.END_ENTITY_PROFILE_NAME_RENAMED);
        // Actually delete EEP
        endEntityProfileHelper.deleteEndEntityProfile(TestData.END_ENTITY_PROFILE_NAME_RENAMED);
        endEntityProfileHelper.confirmEndEntityProfileDeletion(true);
        // TODO Review after JSP->JSF conversion
        Thread.sleep(2000); // Not pretty but WebDriverWait didn't work here for some reason.
        endEntityProfileHelper.assertEndEntityProfileNameDoesNotExist(TestData.END_ENTITY_PROFILE_NAME_RENAMED);
        // Verify Audit Log
        auditLogHelper.openPage(getAdminWebUrl());
        auditLogHelper.assertLogEntryByEventText(
                "End Entity Profile Remove",
                "Success",
                null,
                Collections.singletonList(
                        "End entity profile " + TestData.END_ENTITY_PROFILE_NAME_RENAMED + " removed."
                )
        );
    }
}