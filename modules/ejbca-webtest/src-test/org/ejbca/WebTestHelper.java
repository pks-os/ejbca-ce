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

package org.ejbca;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import static org.junit.Assert.*;

/**
 * Helper class for EJBCA Web Tests.
 * 
 * @version $Id$
 */
public final class WebTestHelper {

    private static final String endEntityProfileSaveMessage = "End Entity Profile saved.";

    private WebTestHelper() {};

    /* --- End Entity Profile operations --- */
    /**
     * Opens the Manage End Entity Profiles page and adds a new End Entity Profile,
     * then asserts that the add was successful.
     * 
     * @param webDriver the WebDriver to use
     * @param adminWebUrl the URL of the AdminWeb
     * @param eepName the name of the End Entity Profile to be added
     */
    public static void addEndEntityProfile(WebDriver webDriver, String adminWebUrl, String eepName) {
        // Go to Manage End Entity Profiles page
        webDriver.get(adminWebUrl);
        webDriver.findElement(By.xpath("//a[contains(@href,'editendentityprofiles.jsp')]")).click();

        // Add End Entity Profile
        WebElement nameInput = webDriver.findElement(By.xpath("//input[@name='textfieldprofilename']"));
        nameInput.sendKeys(eepName);
        webDriver.findElement(By.xpath("//input[@name='buttonaddprofile']")).click();

        // Assert add successful
        WebElement eepList = webDriver.findElement(By.xpath("//select[@name='selectprofile']"));
        WebElement eep = eepList.findElement(By.xpath("//option[@value='" + eepName + "']"));
        assertEquals(eepName + " was not found in the List of End Entity Profiles", eepName, eep.getText());
    }

    /**
     * Opens the edit page for an End Entity Profile, then asserts that the
     * correct End Entity Profile is being edited.
     * 
     * @param webDriver the WebDriver to use
     * @param adminWebUrl the URL of the AdminWeb
     * @param eepName the name of the End Entity Profile to be added
     */
    public static void editEndEntityProfile(WebDriver webDriver, String adminWebUrl, String eepName) {
        // Go to Manage End Entity Profiles page
        webDriver.get(adminWebUrl);
        webDriver.findElement(By.xpath("//a[contains(@href,'editendentityprofiles.jsp')]")).click();

        // Select End Entity Profile in list
        WebElement eepList = webDriver.findElement(By.xpath("//select[@name='selectprofile']"));
        WebElement eep = eepList.findElement(By.xpath("//option[@value='" + eepName + "']"));
        assertEquals(eepName + " was not found in the List of End Entity Profiles", eepName, eep.getText());
        eep.click();

        // Click edit button
        webDriver.findElement(By.xpath("//input[@name='buttoneditprofile']")).click();

        // Assert correct edit page
        WebElement currentProfile = webDriver.findElement(By.xpath("//input[@name='hiddenprofilename']"));
        assertEquals("The profile being edited was not " + eepName, eepName, currentProfile.getAttribute("value"));
    }

    /**
     * Clicks the Save button when editing an End Entity Profile.
     * 
     * @param webDriver the WebDriver to use
     */
    public static void saveEndEntityProfile(WebDriver webDriver, boolean assertSuccess) {
        webDriver.findElement(By.xpath("//input[@name='buttonsave']")).click();
        if (assertSuccess) {
            // Assert that the save was successful
            assertTrue("The End Entity Profile was not saved successfully",
                    webDriver.findElements(By.xpath("//td[contains(text(), '"
                            + endEntityProfileSaveMessage + "')]")).size() == 1);
        }
    }

    /**
     * Adds an attribute to 'Subject DN Attributes', 'Subject Alternative Name' or
     * 'Subject Directory Attributes' while editing an End Entity Profile.
     * 
     * @param webDriver the WebDriver to use
     * @param attributeType either 'subjectdn', 'subjectaltname' or 'subjectdirattr'
     * @param attributeName the displayed name of the attribute, e.g. 'O, Organization'
     */
    public static void addAttributeEndEntityProfile(WebDriver webDriver, String attributeType, String attributeName) {
        // Select attribute in list
        Select attributeSelect = new Select(webDriver.findElement(By.xpath("//select[@name='selectadd" + attributeType + "']")));
        attributeSelect.selectByVisibleText(attributeName);
        WebElement attributeItem = attributeSelect.getFirstSelectedOption();
        assertEquals("The attribute " + attributeName + " was not found", attributeName, attributeItem.getText());
        attributeItem.click();

        // Add attribute and assert that it was added
        webDriver.findElement(By.xpath("//input[@name='buttonadd" + attributeType + "']")).click();
        assertTrue("The attribute " + attributeName + " was not added",
                webDriver.findElements(By.xpath("//td[contains(text(), '"
                        + attributeName + "')]")).size() == 1);
    }

    /* --- Miscellaneous operations --- */
    /**
     * Used to assert that there was an alert, and optionally if there was a
     * specific alert message.
     * 
     * @param webDriver the WebDriver to use
     * @param expectedMessage the expected message from the alert (or null for no assertion)
     * @param accept true if the alert should be accepted, false if it should be dismissed
     */
    public static void assertAlert(WebDriver webDriver, String expectedMessage, boolean accept) {
        Boolean alertExists = true;
        try {
            Alert alert = webDriver.switchTo().alert();
            // Assert that the correct alert message is displayed (if not null)
            if (expectedMessage != null) {
                assertEquals("Unexpected alert message: " + alert.getText(), expectedMessage, alert.getText());
            }
            // Accept or dismiss the alert message
            if (accept) {
                alert.accept();
            } else {
                alert.dismiss();
            }
        } catch (NoAlertPresentException e) {
            // No alert found
            alertExists = false;
        }
        assertTrue("Expected an alert but there was none", alertExists);
    }
}
