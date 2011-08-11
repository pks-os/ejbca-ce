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

package org.ejbca.core.ejb.log;

import java.util.ArrayList;
import java.util.Date;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.cesecore.authentication.tokens.AlwaysAllowLocalAuthenticationToken;
import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authentication.tokens.UsernamePrincipal;
import org.ejbca.core.model.log.LogConstants;
import org.ejbca.util.InterfaceCache;

public class LoggingStressTest extends TestCase {
	
	private static Logger log = Logger.getLogger(LoggingStressTest.class);
	
	private static final int NUMBER_OF_THREADS = 10; 
	private static final int TIME_TO_RUN = 15*60000; // Run for 15 minutes

    public void setUp() throws Exception {
    }

    public void tearDown() throws Exception {
    }

    public void test01LogALot() throws Exception {
		ArrayList<Thread> threads = new ArrayList<Thread>(); // NOPMD, it's not a JEE app
		for (int i=0; i<NUMBER_OF_THREADS; i++) {
	        Thread thread = new Thread(new LogTester(i, TIME_TO_RUN), "LogTester-"+i); // NOPMD, it's not a JEE app
	        thread.start();
	        log.info("Started LogTester-"+i);
	        threads.add(thread);
		}
		for (Thread thread : threads) { // NOPMD, it's not a JEE app
			thread.join();
		}
    }
    
    private class LogTester implements Runnable { // NOPMD, it's not a JEE app
    	
    	private long runTime = 0;
    	private long startTime = 0;
    	private int threadId = 0;
    	
    	//private LogSessionRemote logSession = InterfaceCache.getLogSession();
    	
    	LogTester(int threadId, long runTime) {
    		this.threadId = threadId;
    		this.startTime = new Date().getTime();
    		this.runTime = runTime;
    	}
    	
    	AuthenticationToken internalAdmin = new AlwaysAllowLocalAuthenticationToken(new UsernamePrincipal("SYSTEMTEST"));
    	public void run() {
            try {
            	int i = 0;
            	long delta = 0;
            	while ((delta = System.currentTimeMillis() - startTime) < runTime) {
                	//logSession.log(internalAdmin, internalAdmin.getCaId(), LogConstants.MODULE_LOG, new Date(), null, null, LogConstants.EVENT_INFO_UNKNOWN, "This LogEvent was generated by the log stress test.");
                	i++;
                	if (((delta * 100) / runTime) % 10 == 0) {
                		log.info(threadId+" has logged "+i+" events.");
                	}
                	Thread.yield();
            	}
                log.info("\nThread "+threadId+" finished in "+((delta)/1000) + "."+((delta)%1000)+" seconds.");
                log.info("Throughput: "+(i/(delta/1000)) + "." + (i%(delta/1000)) + " log-invocations/second.");
			} catch (Exception e) {
				e.printStackTrace();
            	assertTrue(false);
			}    		
    	}
    }
}
