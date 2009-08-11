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

package org.ejbca.util;

import junit.framework.TestCase;

/** 
 * Test of the YearMonthDayTime helper class.
 *  
 * @version $Id$
 */
public class TestYearMonthDayTime extends TestCase {
    
    public TestYearMonthDayTime(String name) { super(name); }

    protected void setUp() throws Exception { }
    protected void tearDown() throws Exception { }

    /**
     * Test parsing without default value.
     */
	public void test01ParseWithoutDefault() throws Exception {
	    // Test empty and bad input without default
		testFailHelper("", null);
		testFailHelper("0", null);
		testFailHelper("d", null);
		testFailHelper("1d0", null);
		testFailHelper("10s-10ms", null);
	    // Test parsing full format
		testHelper("0y0mo0d", null, false, 0, 0, 0);
		testHelper("1y1mo1d", null, false, 1, 1, 1);
		testHelper("1y2mo3d", null, false, 1, 2, 3);
		testHelper("10y10mo10d", null, false, 10, 10, 10);
	    // Test parsing one value at the time
		testHelper("10y", null, false, 10, 0, 0);
		testHelper("10mo", null, false, 0, 10, 0);
		testHelper("10d", null, false, 0, 0, 10);
		// Test parsing of capital letters
		testHelper("1Y2MO3D", null, false, 1, 2, 3);
		testHelper("5mO", null, false, 0, 5, 0);
		testHelper("5Mo", null, false, 0, 5, 0);
		// Test with spaces
		testHelper(" 1y2mo3d ", null, false, 1, 2, 3);
		testHelper("1y 2mo 3d", null, false, 1, 2, 3);
	}
	
    /**
     * Test parsing with default value.
     */
	public void test02ParseWithDefault() throws Exception {
	    // Test empty and bad input with bad default
		testFailHelper("", "");
		testFailHelper("", "0");
		testFailHelper("0", "");
	    // Test empty input with ok default
	    // Test parsing full format
		testHelper("", "0y0mo0d", false, 0, 0, 0);
		testHelper(null, "1y1mo1d", false, 1, 1, 1);
		testHelper("", "1y2mo3d", false, 1, 2, 3);
		testHelper(null, "10y10mo10d", false, 10, 10, 10);
	    // Test parsing one value at the time
		testHelper("", "10y", false, 10, 0, 0);
		testHelper(null, "10mo", false, 0, 10, 0);
		testHelper("", "10d", false, 0, 0, 10);
		// Test parsing of capital letters
		testHelper("", "1Y2MO3D", false, 1, 2, 3);
		testHelper("", "5mO", false, 0, 5, 0);
		testHelper("", "5Mo", false, 0, 5, 0);
		// Test with spaces
		testHelper("", " 1y2mo3d ", false, 1, 2, 3);
		testHelper("", "1y 2mo 3d", false, 1, 2, 3);
	}
	
    /**
     * Test formatting.
     */
	public void test03Format() throws Exception {
		assertEquals("10y", YearMonthDayTime.getInstance("10y").toString());
		assertEquals("10mo", YearMonthDayTime.getInstance("10mo").toString());
		assertEquals("10d", YearMonthDayTime.getInstance("10d").toString());
		assertEquals("0d", YearMonthDayTime.getInstance("0y").toString());
		assertEquals("1y3d", YearMonthDayTime.getInstance("1y3d").toString().replaceAll("\\s", ""));
		assertEquals("1y2mo3d", YearMonthDayTime.getInstance("1y2mo3d").toString().replaceAll("\\s", ""));
	}
	
	/**
	 * Helper for tests that we expect to fail.
	 */
	private void testFailHelper(String time, String defaultTime) {
		testHelper(time, defaultTime, true, 0, 0, 0);
	}
	
	/**
	 * Get a new YearMonthDayTime object and verify that it was created correctly.
	 */
	private void testHelper(String time, String defaultTime, boolean fail, long years, long months, long days) {
		YearMonthDayTime ymodTime;
		if (defaultTime == null) {
			ymodTime = YearMonthDayTime.getInstance(time);
		} else {
			ymodTime = YearMonthDayTime.getInstance(time, defaultTime);
		}
		if (fail) {
			assertNull("'"+time+"' input.", ymodTime);
			return;
		} else {
			assertNotNull("'"+time+"' input.", ymodTime);
		}
		assertEquals("'"+time+"' input.", years, ymodTime.getYears());
		assertEquals("'"+time+"' input.", months, ymodTime.getMonths());
		assertEquals("'"+time+"' input.", days, ymodTime.getDays());
		
		if(ymodTime.getYears() != 0 || ymodTime.getMonths() != 0 || ymodTime.getDays() != 0) {
			if (defaultTime != null) {
				assertEquals("'"+defaultTime+"' input.", defaultTime.toLowerCase().replaceAll("\\s", ""), ymodTime.toString().replaceAll("\\s", ""));
			} else {
				assertEquals("'"+time+"' input.", time.toLowerCase().replaceAll("\\s", ""), ymodTime.toString().replaceAll("\\s", ""));
			}
		}
	}
}
