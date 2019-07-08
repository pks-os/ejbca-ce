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
import org.ejbca.webtest.helper.AddEndEntityHelper;
import org.ejbca.webtest.helper.CertificateProfileHelper;
import org.ejbca.webtest.helper.SearchEndEntitiesHelper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.openqa.selenium.WebDriver;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class EcaQa77_EndEntitySearch extends WebTestBase {

    private static WebDriver webDriver;

    // Helpers
    //private static EndEntityProfileHelper endEntityProfileHelper;
    private static CertificateProfileHelper certificateProfileHelper;
    private static AddEndEntityHelper addEndEntityHelper;
    private static SearchEndEntitiesHelper searchEndEntitiesHelper;
    

    public static class TestData {
        private static final String ROOTCA_NAME = "ECAQA5";
        private static final String SUBCA_NAME = "subCA ECAQA5";
        private static final String END_ENTITY_NAME_1 = "TestEndEntityEMPTY_1";
        private static final String END_ENTITY_NAME_2 = "TestEndEntityEMPTY_2";
        private static final String CERTIFICATE_PROFILE_NAME = "ShortValidity";
    }

    @BeforeClass
    public static void init() {
        beforeClass(true, null);
        webDriver = getWebDriver();
        addEndEntityHelper = new AddEndEntityHelper(webDriver);
        searchEndEntitiesHelper = new SearchEndEntitiesHelper(webDriver);
        //endEntityProfileHelper = new EndEntityProfileHelper(webDriver);
        certificateProfileHelper = new CertificateProfileHelper(webDriver);
    }

    @AfterClass
    public static void exit() throws AuthorizationDeniedException {
        // Remove generated artifacts
//        removeCaAndCryptoToken(TestData.ROOTCA_NAME);
//        removeCaByName(TestData.SUBCA_NAME);
//        removeEndEntityByUsername(TestData.END_ENTITY_NAME_1);
//        removeEndEntityByUsername(TestData.END_ENTITY_NAME_2);
//        removeEndEntityByUsername(TestData.END_ENTITY_NAME_3);

        //afterClass();
    }

    @Test
    public void stepA_addEndEntityProfile() {
        certificateProfileHelper.openPage(getAdminWebUrl());
        certificateProfileHelper.cloneCertificateProfile("SERVER", TestData.CERTIFICATE_PROFILE_NAME);
        certificateProfileHelper.openEditCertificateProfilePage(TestData.CERTIFICATE_PROFILE_NAME);
        certificateProfileHelper.editCertificateProfile(null, null, null, null, "2d");
        certificateProfileHelper.saveCertificateProfile();
    }
    
//    @Test
//    public void stepA_AddEndEntitySubjectDn1of3() throws InterruptedException {
//        
//    }
    
    /*
    @Test
    public void stepF_SearchEndEntitySubjectDn3of3() {
        searchEndEntitiesHelper.openPage(getAdminWebUrl());

        searchEndEntitiesHelper.switchViewModeFromAdvancedToBasic(); //Note: the search panel needs to be in "basic mode" for 'fillSearchCriteria' method to work properly.
        searchEndEntitiesHelper.fillSearchCriteria(TestData.END_ENTITY_NAME_3, null, null, null);

        searchEndEntitiesHelper.clickSearchByUsernameButton();
        searchEndEntitiesHelper.assertNumberOfSearchResults(1);
    }
    */
}
