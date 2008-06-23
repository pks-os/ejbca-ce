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

package org.ejbca.ui.cli;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import org.ejbca.util.keystore.KeyStoreContainer;



/**
 * @author lars
 * @version $Id$
 *
 */
public class HSMKeyTool {
//    private static String CREATE_CA_SWITCH = "createca";
    private static final String ENCRYPT_SWITCH = "encrypt";
    private static final String DECRYPT_SWITCH = "decrypt";
    private static final String GENERATE_SWITCH = "generate";
    private static final String GENERATE_MODULE_SWITCH = GENERATE_SWITCH+"module";
    private static final String DELETE_SWITCH = "delete";
    private static final String TEST_SWITCH = "test";
    private static final String CREATE_KEYSTORE_SWITCH = "createkeystore";
    private static final String CREATE_KEYSTORE_MODULE_SWITCH = CREATE_KEYSTORE_SWITCH+"module";
    private static final String MOVE_SWITCH = "move";
    private static final String CERT_REQ = "certreq";
    private static final String INSTALL_CERT = "installcert";
    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            final boolean isP11 = args.length>4 && KeyStoreContainer.isP11(args[4]);
            final String sKeyStore =  isP11 ? "<slot number. start with \'i\' to indicate index in list>" : "<keystore ID>";
            final String commandString = args[0] + " " + args[1] + (isP11 ? " <shared library name> " : " ");
            /*if ( args.length > 1 && args[1].toLowerCase().trim().equals(CREATE_CA_SWITCH)) {
                try {
                    new HwCaInitCommand(args).execute();
                } catch (Exception e) {
                    System.out.println(e.getMessage());            
                    //e.printStackTrace();
                    System.exit(-1);
                }
                return;
            } else */
            if ( args.length > 1 && args[1].toLowerCase().trim().contains(GENERATE_SWITCH) ) {
                if ( args.length < 6 ) {
                    System.err.println(commandString + "<key size> <key entry name> " + '['+sKeyStore+']');
                    if ( isP11 )
                        System.err.println("If <slot number> is omitted then <the shared library name> will specify the sun config file.");
                } else {
                    if ( args[1].toLowerCase().trim().contains(GENERATE_MODULE_SWITCH) ) {
                        System.setProperty("protect", "module");
                    }
                    KeyStoreContainer store = KeyStoreContainer.getInstance(args[4], args[2], args[3], args.length>7 ? args[7] : null, null, null);
                    String keyEntryName = args.length>6 ? args[6] :"myKey";
                    store.generate(Integer.parseInt(args[5].trim()), keyEntryName);
                    System.err.println("Created certificate with entry "+keyEntryName+'.');
                }
                return;
            } else if ( args.length > 1 && args[1].toLowerCase().trim().equals(DELETE_SWITCH)) {
                if ( args.length < 6 ) {
                    System.err.println(commandString + sKeyStore + " [<key entry name>]");
                } else {
                	String alias = args.length>6 ? args[6] : null;
                    System.err.println("Deleting certificate with alias "+alias+'.');
                    KeyStoreContainer.getInstance(args[4], args[2], args[3], args[5], null, null).delete(alias);
                }
                return;
            } else if ( args.length > 1 && args[1].toLowerCase().trim().equals(CERT_REQ)) {
                if ( args.length < 7 )
                    System.err.println(commandString + sKeyStore + " <key entry name>");
                else
                    KeyStoreContainer.getInstance(args[4], args[2], args[3], args[5], null, null).generateCertReq(args[6]);
                return;
            } else if ( args.length > 1 && args[1].toLowerCase().trim().equals(INSTALL_CERT)) {
                if ( args.length < 7 )
                    System.err.println(commandString + sKeyStore + " <certificate in PEM format>");
                else
                    KeyStoreContainer.getInstance(args[4], args[2], args[3], args[5], null, null).installCertificate(args[6]);
                return;
            } else if ( args.length > 1 && args[1].toLowerCase().trim().equals(ENCRYPT_SWITCH)) {
                if ( args.length < 9 )
                    System.err.println(commandString + sKeyStore + " <input file> <output file> <key alias>");
                else
                    KeyStoreContainer.getInstance(args[4], args[2], args[3], args[5], null, null).encrypt(new FileInputStream(args[6]), new FileOutputStream(args[7]), args[8]);
                return;
            } else if ( args.length > 1 && args[1].toLowerCase().trim().equals(DECRYPT_SWITCH)) {
                if ( args.length < 9 )
                    System.err.println(commandString + sKeyStore + " <input file> <output file> <key alias>");
                else
                    KeyStoreContainer.getInstance(args[4], args[2], args[3], args[5], null, null).decrypt(new FileInputStream(args[6]), new FileOutputStream(args[7]), args[8]);
                return;
            } else if( args.length > 1 && args[1].toLowerCase().trim().equals(TEST_SWITCH)) {
                if ( args.length < 6 )
                    System.err.println(commandString + sKeyStore + " [<# of tests or threads>] [alias for stress test] [type of stress test]");
                else
                    KeyStoreContainerTest.test(args[2], args[3], args[4], args[5],
                                               args.length>6 ? Integer.parseInt(args[6].trim()) : 1, args.length>7 ? args[7].trim() : null, args.length>8 ? args[8].trim() : null);
                return;
            } else if( args.length > 1 && args[1].toLowerCase().trim().equals(MOVE_SWITCH)) {
                if ( args.length < 7 ) {
                    System.err.println(commandString +
                                       (isP11 ? "<from slot number> <to slot number>" : "<from keystore ID> <to keystore ID>"));
                } else {
                	String fromId = args[5];                	
                	String toId = args[6];
                    System.err.println("Moving entry with alias '"+fromId+"' to alias '"+toId+'.');
                    KeyStoreContainer.move(args[2], args[3], args[4], fromId, toId, null);
                }
                return;
            } else if ( !isP11 ){
                if( args.length > 1 && args[1].toLowerCase().trim().contains(CREATE_KEYSTORE_SWITCH)) {
                    if( args[1].toLowerCase().trim().contains(CREATE_KEYSTORE_MODULE_SWITCH))
                        System.setProperty("protect", "module");
                    KeyStoreContainer.getInstance(args[4], args[2], args[3], null, null, null).storeKeyStore();
                    return;
                }
            }
            PrintWriter pw = new PrintWriter(System.err);
            pw.println("Use one of following commands: ");
//            pw.println("  "+args[0]+" "+CREATE_CA_SWITCH);
            pw.println("  "+args[0]+" "+GENERATE_SWITCH);
            if ( !isP11 ){
                pw.println("  "+args[0]+" "+GENERATE_MODULE_SWITCH);
            }
            pw.println("  "+args[0]+" "+CERT_REQ);
            pw.println("  "+args[0]+" "+INSTALL_CERT);
            pw.println("  "+args[0]+" "+DELETE_SWITCH);
            pw.println("  "+args[0]+" "+TEST_SWITCH);
            if ( !isP11 ){
                pw.println("  "+args[0]+" "+CREATE_KEYSTORE_SWITCH);
                pw.println("  "+args[0]+" "+CREATE_KEYSTORE_MODULE_SWITCH);
            }
            pw.println("  "+args[0]+" "+ENCRYPT_SWITCH);
            pw.println("  "+args[0]+" "+DECRYPT_SWITCH);
            pw.println("  "+args[0]+" "+MOVE_SWITCH);
            pw.flush();
        } catch (Throwable e) {
            e.printStackTrace(System.err);
        }
    }
}
