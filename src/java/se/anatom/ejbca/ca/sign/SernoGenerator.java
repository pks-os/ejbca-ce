package se.anatom.ejbca.ca.sign;

import java.security.SecureRandom;
import java.util.Date;

import org.apache.log4j.*;

/** Implements a serial number generator using SecureRandom, implementes the Singleton pattern.
 *
 * @version $Id: SernoGenerator.java,v 1.1 2002-08-20 12:18:23 anatom Exp $
 */
public class SernoGenerator implements ISernoGenerator {

    /** Log4j instance */
    private static Category cat = Category.getInstance( SernoGenerator.class.getName() );
    /* random generator */
    private SecureRandom random;

   /**
    * A handle to the unique Singleton instance.
    */
    static private SernoGenerator instance = null;

   /** Creates a serialn number generator using SecureRandom
    */
    protected SernoGenerator() throws Exception {
        cat.debug(">SernoGenerator()");
        // Init random number generator for random serialnumbers
        random = SecureRandom.getInstance("SHA1PRNG");
        // Using this seed we should get a different seed every time.
        // We are not concerned about the security of the random bits, only that they are different every time.
        // Extracting 64 bit random numbers out of this should give us 2^32 (4 294 967 296) serialnumbers before
        // collisions (which are seriously BAD), well anyhow sufficient for pretty large scale installations.
        // Design criteria: 1. No counter to keep track on. 2. Multiple threads can generate numbers at once, in
        // a clustered environment etc.
        long seed = Math.abs((new Date().getTime()) + this.hashCode());
        random.setSeed(seed);
        /* Another possibility is to use SecureRandom's default seeding which is designed to be secure:
        * <p>The seed is produced by counting the number of times the VM
        * manages to loop in a given period. This number roughly
        * reflects the machine load at that point in time.
        * The samples are translated using a permutation (s-box)
        * and then XORed together. This process is non linear and
        * should prevent the samples from "averaging out". The s-box
        * was designed to have even statistical distribution; it's specific
        * values are not crucial for the security of the seed.
        * We also create a number of sleeper threads which add entropy
        * to the system by keeping the scheduler busy.
        * Twenty such samples should give us roughly 160 bits of randomness.
        * <P> These values are gathered in the background by a daemon thread
        * thus allowing the system to continue performing it's different
        * activites, which in turn add entropy to the random seed.
        * <p> The class also gathers miscellaneous system information, some
        * machine dependent, some not. This information is then hashed together
        * with the 20 seed bytes. */
        cat.debug("<SernoGenerator()");
    }

   /** Creates (if needed) the serial number generator and returns the object.
    * @return An instance of the serial number generator.
    */
    static public synchronized ISernoGenerator instance() throws Exception {
       if(instance == null) {
         instance = new SernoGenerator();
       }
       return instance;
    }

   /** Generates a number of serial number bytes.
    *
    * @return an array of serial number bytes.
    */
    public synchronized byte[] getSerno() {
        //cat.debug(">getSerno()");
        byte[] serno = new byte[8];
        random.nextBytes(serno);
        //cat.debug("<getSerno()");
        return serno;
    }
   /** Returns the number of serial number byutes generated by this generator.
    *
    * @return The number of serial number byutes generated by this generator.
    */
    public int getNoSernoBytes() {
        return 8;
    }

}

