package se.anatom.ejbca.ra.junit;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import junit.framework.*;


/**
 * Main test class
 *
 * @version $Id: TestRunner.java,v 1.11 2003-09-09 09:58:59 herrvendil Exp $
 */
public class TestRunner extends Object {
    private static Logger log = Logger.getLogger(TestRunner.class);

    /**
     * Main
     *
     * @param args cmd line arge
     */
    public static void main(String[] args) {
        BasicConfigurator.configure();
        junit.textui.TestRunner.run(suite());
    }

    private void cleanUp() {
        log.debug(">cleanUp()");
        log.debug("<cleanUp()");
    }

    /**
     * Sets up test suite
     *
     * @return none
     */
    public static Test suite() {
        log.debug(">suite()");

        TestSuite suite = new TestSuite();
        suite.addTest(new TestSuite(TestUserData.class));
      
        //suite.addTest( new TestSuite( TestAddLotsofUsers.class ));
        log.debug("<suite()");

        return suite;
    }
}
