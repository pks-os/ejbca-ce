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
package org.ejbca.core.protocol.ws.client;

import java.io.ByteArrayInputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Iterator;

import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.util.encoders.Base64;
import org.ejbca.core.model.ra.UserDataConstants;
import org.ejbca.core.protocol.ws.client.gen.CertificateResponse;
import org.ejbca.core.protocol.ws.client.gen.EjbcaWS;
import org.ejbca.core.protocol.ws.client.gen.UserDataVOWS;
import org.ejbca.core.protocol.ws.common.CertificateHelper;
import org.ejbca.ui.cli.ErrorAdminCommandException;
import org.ejbca.ui.cli.IAdminCommand;
import org.ejbca.ui.cli.IllegalAdminCommandException;
import org.ejbca.util.CertTools;
import org.ejbca.util.PerformanceTest;
import org.ejbca.util.PerformanceTest.Command;
import org.ejbca.util.PerformanceTest.CommandFactory;

/**
 * @author Lars Silven, PrimeKey Solutions AB
 * @version $Id: StressTestCommand.java,v 1.11 2008-03-07 23:01:09 primelars Exp $
 */
public class StressTestCommand extends EJBCAWSRABaseCommand implements IAdminCommand {

    final private PerformanceTest performanceTest;
    private class MyCommandFactory implements CommandFactory {
        final String caName;
        final String endEntityProfileName;
        final String certificateProfileName;
        MyCommandFactory( String _caName, String _endEntityProfileName, String _certificateProfileName) {
            this.caName = _caName;
            this.endEntityProfileName = _endEntityProfileName;
            this.certificateProfileName = _certificateProfileName;
        }
        public Command[] getCommands() throws Exception {
            final KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(1024);
            final EjbcaWS ejbcaWS = getEjbcaRAWSFNewReference();
            final JobData jobData = new JobData();
            return new Command[]{
                                 new EditUserCommand(ejbcaWS, caName, endEntityProfileName, certificateProfileName, jobData),
                                 new Pkcs10RequestCommand(ejbcaWS, kpg.generateKeyPair(), jobData)
            };
        }
    }
    private class JobData {
        String userName;
        String passWord;
    }
    private class Pkcs10RequestCommand implements Command {
        final private EjbcaWS ejbcaWS;
        final private JobData jobData;
        final private PKCS10CertificationRequest pkcs10;
        Pkcs10RequestCommand(EjbcaWS _ejbcaWS, KeyPair keys, JobData _jobData) throws Exception {
            this.pkcs10 = new PKCS10CertificationRequest("SHA1WithRSA", CertTools.stringToBcX509Name("CN=NOUSED"), keys.getPublic(), null, keys.getPrivate());
            this.jobData = _jobData;
            this.ejbcaWS = _ejbcaWS;
        }
        public void doIt() throws Exception {
            final CertificateResponse certificateResponse = this.ejbcaWS.pkcs10Request(jobData.userName, jobData.passWord,
                                                                                       new String(Base64.encode(pkcs10.getEncoded())),null,CertificateHelper.RESPONSETYPE_CERTIFICATE);
            final Iterator<X509Certificate> i = (Iterator<X509Certificate>)CertificateFactory.getInstance("X.509").generateCertificates(new ByteArrayInputStream(Base64.decode(certificateResponse.getData()))).iterator();
            X509Certificate cert = null;
            while ( i.hasNext() )
                cert = i.next();
            final String commonName = CertTools.getPartFromDN(cert.getSubjectDN().getName(), "CN");
            if ( commonName.equals(jobData.userName) )
                performanceTest.getLog().info("Cert created. Subject DN: \""+cert.getSubjectDN()+"\".");
            else
                performanceTest.getLog().error("Cert not created for right user. Username: \""+jobData.userName+"\" Subject DN: \""+cert.getSubjectDN()+"\".");
            performanceTest.getLog().result(cert.getSerialNumber());
        }
        public String getJobTimeDescription() {
            return "Relative time spent signing certificates";
        }
    }
    private class EditUserCommand implements Command {
        final private EjbcaWS ejbcaWS;
        final private UserDataVOWS user;
        final private JobData jobData;
        EditUserCommand(EjbcaWS _ejbcaWS, String caName, String endEntityProfileName, String certificateProfileName, JobData _jobData) {
            this.jobData = _jobData;
            ejbcaWS = _ejbcaWS;
            this.user = new UserDataVOWS();
            this.user.setClearPwd(true);
            this.user.setCaName(caName);
            this.user.setEmail(null);
            this.user.setSubjectAltName(null);
            this.user.setStatus(UserDataConstants.STATUS_NEW);
            this.user.setTokenType(org.ejbca.core.protocol.ws.objects.UserDataVOWS.TOKEN_TYPE_USERGENERATED);
            this.user.setEndEntityProfileName(endEntityProfileName);
            this.user.setCertificateProfileName(certificateProfileName);
        }
        public void doIt() throws Exception {
            jobData.passWord = "foo123";
            jobData.userName = "WSTESTUSER"+performanceTest.getRandom().nextInt();
            this.user.setSubjectDN("CN="+jobData.userName);
            this.user.setUsername(jobData.userName);
            this.user.setPassword(jobData.passWord);
            this.ejbcaWS.editUser(user);
        }
        public String getJobTimeDescription() {
            return "Relative time spent registring new users";
        }
    }
    /**
     * @param args
     */
    public StressTestCommand(String[] _args) {
        super(_args);
        performanceTest = new PerformanceTest();
    }

    /* (non-Javadoc)
     * @see org.ejbca.core.protocol.ws.client.EJBCAWSRABaseCommand#usage()
     */
    @Override
    protected void usage() {
        getPrintStream().println("Command used to perform a \"stress\" test of EJBCA.");
        getPrintStream().println("The command will start up a number of threads.");
        getPrintStream().println("Each thread will continuously add new users to EJBCA. After adding a new user the thread will fetch a certificate for it.");
        getPrintStream().println();
        getPrintStream().println("Usage : stress <caname> <nr of threads> <max wait time in ms to fetch cert after adding user> [<end entity profile name>] [<certificate profile name>]");
        getPrintStream().println();
        getPrintStream().println("Here is an example of how the test could be started:");
        getPrintStream().println("./ejbcawsracli.sh stress AdminCA1 20 5000");
        getPrintStream().println("20 threads is started. After adding a user the thread waits between 0-500 ms before requesting a certificate for it. The certificates will all be signed by the CA AdminCA1.");
    }

    /* (non-Javadoc)
     * @see org.ejbca.ui.cli.IAdminCommand#execute()
     */
    public void execute() throws IllegalAdminCommandException, ErrorAdminCommandException {

        try {
            if(args.length <  2){
                usage();
                System.exit(-1);
            }
            final int numberOfThreads = args.length>2 ? Integer.parseInt(args[2]) : 1;
            final int waitTime = args.length>3 ? Integer.parseInt(args[3]) : -1;
            final String caName = args[1];
            final String endEntityProfileName = args.length>4 ? args[4] : "EMPTY";
            final String certificateProfileName = args.length>5 ? args[5] : "ENDUSER";
            performanceTest.execute(new MyCommandFactory(caName, endEntityProfileName, certificateProfileName),
                                    numberOfThreads, waitTime, getPrintStream());
            getPrintStream().println("A test key for each thread is generated. This could take some time if you have specified many threads and long keys.");
            synchronized(this) {
                wait();
            }
        } catch( InterruptedException e) {
            // do nothing since user wants to exit.
        } catch( Exception e) {
            throw new ErrorAdminCommandException(e);
        }finally{
            performanceTest.getLog().close();
        }
    }
}
