/*
 * Created on 2004-jan-24
 *
 * Class used as an install script of ejbca
 */
package se.anatom.ejbca.admin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.regex.Pattern;

import org.ietf.ldap.LDAPDN;

/**
 * @author philip
 *
 * The main porpose of this program is to provide easy installment of EJBCA.
 */
public class Install {

	public static int ARG_OS               =  0;
	public static int ARG_LANGUAGE   =  1;
	public static int ARG_VERSION      =  2;
	public static int ARG_APPSERVER  =  3;
	public static int ARG_WEBSERVER =  4;
	
	private final static int OS_UNIX          = 0; 
	private final static int OS_WINDOWS  = 1;
		
	private final static int APPSERVER_JBOSS         = 0; 
	private final static int APPSERVER_WEBLOGIC  = 1;
	
    private final static int WEBSERVER_JETTY      = 0;
    private final static int WEBSERVER_TOMCAT  = 1;

    private int appserver = APPSERVER_JBOSS;
    private int webserver = WEBSERVER_JETTY;

    private int os = OS_UNIX;
    
	private String caname = "";
	private String cadn = "";
	private int keysize = 0;
	private int validity = 0;
	private String policyid = "";

	private String computername = "";
	private String servercertdn = "";
	private String serverkeystorepasswd = "";
	private String superadminpasswd = "";
	private String javacacertspasswd = "";
			
	private Pattern nondigit = Pattern.compile("\\D");
	private Pattern nondigitordot = Pattern.compile("[^0-9\\.]");
	private Pattern notcomputername = Pattern.compile("[^0-9a-zA-z\\-\\.]");
	private Pattern nonword = Pattern.compile("\\W");
	private Properties text; 
	
	private BufferedReader reader = null;
	
	public Install(String osstring, String language, String version, String appserverstring, String webserverstring) throws Exception{
			
		reader = new BufferedReader(new InputStreamReader(System.in));
		
		text = new Properties();
		text.load(this.getClass().getResourceAsStream("/" + "install." + language.toLowerCase() + ".properties"));
						
		//text.load(new FileInputStream("install." + language.toLowerCase() + ".properties"));
		if(version.equalsIgnoreCase("primeca")){
			text.load(this.getClass().getResourceAsStream("/" + "installprimeca." + language.toLowerCase() + ".properties"));			
		}
   
		if(osstring.equalsIgnoreCase("unix")){
			this.os = OS_UNIX;
		}
		if(osstring.equalsIgnoreCase("windows")){
			this.os = OS_WINDOWS;
		}
		
		if(appserverstring.equalsIgnoreCase("tomcat")){
			this.webserver = WEBSERVER_TOMCAT;
		}
		
		
        if(appserverstring.equalsIgnoreCase("weblogic")){
            this.appserver = APPSERVER_WEBLOGIC;
        }

        if(appserverstring.equalsIgnoreCase("tomcat")){
            this.webserver = WEBSERVER_TOMCAT;
        }
	}			
	
	public void run(){
		displayWelcomeScreen();
	    while(!collectData());
	    runInstall();	   	   
	}
					
	public static void main(String[] args) throws Exception {
		if(args.length != 5){
			System.out.println("Usage: install <unix|windows><language> <ejbca|primeca> <jboss|weblogic> <jetty|tomcat>");
			System.exit(-1);
		}
		Install install = new Install(args[ARG_OS], args[ARG_LANGUAGE], args[ARG_VERSION], args[ARG_APPSERVER], args[ARG_WEBSERVER]);
		install.run();
	}
	
	private void displayWelcomeScreen(){
		System.out.print(text.getProperty("WELCOMETO"));
		System.out.print(text.getProperty("THISSCRIPT"));
		System.out.print(text.getProperty("APPLICATIONSERVERISRUNNING"));
		System.out.print(text.getProperty("ENVIRONMENTVARIABLESJBOSS"));
		System.out.print(text.getProperty("THEAPPLICATIONISDEPLOYED"));
		System.out.print(text.getProperty("YOUSHOULDPERFORM"));
		System.out.print(text.getProperty("ISTHESEREQUIREMENTSMEET"));
		String answer = getAnswer();
		while(!answerInBoolean(answer)){
			System.out.print(text.getProperty("PLEASETYPEEITHERYESORNO"));
			answer = getAnswer();
		}
		boolean cont = getBooleanAnswer(answer);
		if(!cont)
		  System.exit(0);	 
		
	}
	
	private boolean collectData(){
	    
	    System.out.print(text.getProperty("THISINSTALLATIONWILL"));
	    getCAName();
	    getCADN();
	    getKeySize();
	    getValidity();
	    getPolicyId();
	    
	    System.out.print(text.getProperty("NOWSOMEADMINWEB"));
	    
	    getComputerName();
	    getSSLServerCertDN();
	    getSSLKeyStorePasswd();
	    getSuperAdminPasswd();    
	    getJavaCACertsPasswd();
	     	    	   
		return isDataCorrect();
	}
	
	private void getCAName(){
		System.out.print(text.getProperty("ENTERSHORTNAME"));
		String answer = getAnswer();
		while(!answerLegalName(answer)){
			System.out.print(text.getProperty("ILLEGALCANAME"));
			System.out.print(text.getProperty("ENTERSHORTNAME"));
			answer = getAnswer();
		}
		this.caname = answer;
	   	
	}
	
	private void getCADN(){
		System.out.print(text.getProperty("ENTERDN"));
		String answer = getAnswer();
		while(!answerLegalDN(answer)){
			System.out.print(text.getProperty("ILLEGALDN"));
			System.out.print(text.getProperty("ENTERDN"));
			answer = getAnswer();
		}
		this.cadn = answer;				
	}
	
	private void getKeySize(){
		System.out.print(text.getProperty("ENTERKEYSIZE"));
		String answer = getAnswer();
		while(!answerLegalKeySize(answer)){
			System.out.print(text.getProperty("ILLEGALKEYSIZE"));
			System.out.print(text.getProperty("ENTERKEYSIZE"));
			answer = getAnswer();
		}
		this.keysize = this.getDigitAnser(answer);		
	}
	
	private void getValidity(){
		System.out.print(text.getProperty("ENTERVALIDITY"));
		String answer = getAnswer();
		while(!answerLegalValidity(answer)){
			System.out.print(text.getProperty("ILLEGALVALIDITY"));
			System.out.print(text.getProperty("ENTERVALIDITY"));
			answer = getAnswer();
		}
		this.validity = this.getDigitAnser(answer);				
	}
	
	private void getPolicyId(){
		System.out.print(text.getProperty("ENTERPOLICYID"));
		String answer = getAnswer();
		while(!answerLegalPolicyId(answer)){
			System.out.print(text.getProperty("ILLEGALPOLICYID"));
			System.out.print(text.getProperty("ENTERPOLICYID"));
			answer = getAnswer();
		}
		
		this.policyid = this.getPolicyId(answer);					
	}
	
	
	
	private void getComputerName(){
		System.out.print(text.getProperty("ENTERCOMPUTERNAME"));
		String answer = getAnswer();
		while(!answerLegalComputerName(answer)){
			System.out.print(text.getProperty("ILLEGALCOMPUTERNAME"));
			System.out.print(text.getProperty("ENTERCOMPUTERNAME"));
			answer = getAnswer();
		}
		
		this.computername = answer;											
	}
	
	private void getSSLServerCertDN(){
		System.out.print(text.getProperty("ENTERSERVERDN"));
		String answer = getAnswer();
		while(!answerLegalDN(answer)){
			System.out.print(text.getProperty("ILLEGALSERVERDN"));
			System.out.print(text.getProperty("ENTERSERVERDN"));
			answer = getAnswer();
		}
		this.servercertdn = answer;						
	}
	
	private void getSSLKeyStorePasswd(){
		System.out.print(text.getProperty("ENTERADMINWEBPASSWORD"));
		String answer = getAnswer();
		while(!answerLegalPassword(answer)){
			System.out.print(text.getProperty("ILLEGALADMINWEBPASSWORD"));
			System.out.print(text.getProperty("ENTERADMINWEBPASSWORD"));
			answer = getAnswer();
		}
		this.serverkeystorepasswd = answer;										
	}
	
	private void getSuperAdminPasswd(){
		System.out.print(text.getProperty("ENTERSUPERADMINPASSWORD"));
		String answer = getAnswer();
		while(!answerLegalPassword(answer)){
			System.out.print(text.getProperty("ILLEGALSUPERADMINPASSWORD"));
			System.out.print(text.getProperty("ENTERSUPERADMINPASSWORD"));
			answer = getAnswer();
		}
		this.superadminpasswd = answer;												
	}
	
	private void getJavaCACertsPasswd(){
		System.out.print(text.getProperty("ENTERCACERTSPASSWORD"));
		String answer = getAnswer();
		while(!answerLegalPassword(answer)){
			System.out.print(text.getProperty("ILLEGALCACERTSPASSWORD"));
			System.out.print(text.getProperty("ENTERCACERTSPASSWORD"));
			answer = getAnswer();
		}
		this.javacacertspasswd = answer;		
	}
	
	private boolean isDataCorrect(){
		System.out.print(text.getProperty("YOUHAVEENTEREDTHEFOLLOWING"));
		
		System.out.println(text.getProperty("CANAME") + " " + this.caname);
		System.out.println(text.getProperty("CADN") + " " + this.cadn);
		System.out.println(text.getProperty("KEYSIZE") + " " + this.keysize);
		System.out.println(text.getProperty("VALIDITY") + " " + this.validity);
		System.out.print(text.getProperty("POLICYID") + " ");
		if(this.policyid.equalsIgnoreCase("null"))
			System.out.println(text.getProperty("NOPOLICYID"));
		else
			System.out.println(this.policyid);
		System.out.println(text.getProperty("COMPUTERNAME") + " " + this.computername);
		System.out.println(text.getProperty("SERVERDN") + " " + this.servercertdn);
		System.out.println(text.getProperty("ADMINWEBPASSWORD") + " " + this.serverkeystorepasswd);
		System.out.println(text.getProperty("SUPERADMINPASSWORD") + " " + this.superadminpasswd);
		System.out.println(text.getProperty("CACERTSPASSWORD") + " " + this.javacacertspasswd);
																																			
		 boolean correct = false;
		 System.out.print(text.getProperty("ISTHISCORRECT"));
         String answer = getAnswer();
         while(!this.answerInBoolean(answer) && !answer.equalsIgnoreCase("e") && !answer.equalsIgnoreCase("exit")){
         	System.out.print(text.getProperty("PLEASETYPEEITHERYESNOEXIT"));
         	answer = getAnswer();
         }         
         if(answer.equalsIgnoreCase("e") || answer.equalsIgnoreCase("Exit"))
         	System.exit(0);
		 
		return getBooleanAnswer(answer);
	}
	
	
	private void runInstall(){
		displayInstallingMessage();
        
		if(this.os == OS_WINDOWS){
		  try {
			Process runcainit = Runtime.getRuntime().exec("ls");
			if(runcainit.exitValue() != 0){
				System.out.print(text.getProperty("ERRORINITCA"));
			}
			
		  } catch (IOException e) {
			System.out.print(text.getProperty("ERRORINITCA"));
			System.exit(-1);
		  }
		  try {
		  	Process setupadminweb = Runtime.getRuntime().exec("ls");
		  	if(setupadminweb.exitValue() != 0){
		  		System.out.print(text.getProperty("ERRORSETTINGUPADMINWEB"));
		  	}
		  } catch (IOException e) {
		  	System.out.print(text.getProperty("ERRORSETTINGUPADMINWEB"));
		  	System.exit(-1);
		  }
		}
		if(os == OS_UNIX){
			try {
				System.out.println("./ca.sh init '" + this.caname + "' '" + this.cadn + "' " + this.keysize + " " + this.validity + " '" + this.policyid + "'");
				Process runcainit = Runtime.getRuntime().exec("./ca.sh init '" + this.caname + "' '" + this.cadn + "' " + this.keysize + " " + this.validity + " '" + this.policyid + "'");
				if(runcainit.exitValue() != 0){
					System.out.print(text.getProperty("ERRORINITCA"));
				}				
			} catch (IOException e) {
				System.out.print(text.getProperty("ERRORINITCA"));
				System.exit(-1);
			}
			try {
				System.out.println("./setup-adminweb.sh '" + this.caname + "'  '" + this.servercertdn + "' '" + this.serverkeystorepasswd + "' '" + this.superadminpasswd + "' '" +  this.javacacertspasswd + "'");
			   Process setupadminweb = Runtime.getRuntime().exec("./setup-adminweb.sh '" + this.caname + "'  '" + this.servercertdn + "' '" + this.serverkeystorepasswd + "' '" + this.superadminpasswd + "' '" +  this.javacacertspasswd + "'");
				if(setupadminweb.exitValue() != 0){
					System.out.print(text.getProperty("ERRORSETTINGUPADMINWEB"));
				}
			} catch (IOException e) {
				System.out.print(text.getProperty("ERRORSETTINGUPADMINWEB"));
				System.exit(-1);
			}
	    }
		displayEndMessage();
	}
	
	private void displayInstallingMessage(){
		System.out.print(text.getProperty("THEINSTALLATIONWILLNOWSTART"));		
	}
	
	private void displayEndMessage(){
		System.out.print(text.getProperty("INSTALLATIONCOMPLETE"));
		System.out.print(text.getProperty("REMAININGSTEPS"));
		System.out.print(text.getProperty("GOTOURLSTART") + this.computername + text.getProperty("GOTOURLEND"));
		System.out.print(text.getProperty("ANDYOUAREALLSET"));
		System.out.print(text.getProperty("INTERESTEDINSUPPORT"));		
	}
	
	
	private String getAnswer(){
		String returnval = "";			
		try {
			returnval=  reader.readLine();
		} catch (Exception e) {}
		
		return returnval;
	}
	
	private boolean answerInBoolean(String answer) {		
		return answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("n") 
		           || answer.equalsIgnoreCase("yes") || answer.equalsIgnoreCase("no");
	}
	
	private boolean getBooleanAnswer(String answer){								
		return answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes"); 
	}
	
	private boolean answerInDigits(String answer){
		return ! nondigit.matcher(answer).find();
	}
	
	private boolean answerInDigitsAndDots(String answer){
	    return !nondigitordot.matcher(answer).find();
    }
	
	private int getDigitAnser(String answer){
		return Integer.parseInt(answer);
	}
	
	private boolean answerLegalDN(String answer){
		return !answer.trim().equals("") && LDAPDN.isValid(answer);
	}
	
	private boolean answerLegalName(String answer){
		 return !answer.trim().equals("") && !nonword.matcher(answer).find();
	}
	
	private boolean answerLegalPolicyId(String answer){
		if(answer.equalsIgnoreCase("NO"))
			return true;
		
		return !answer.trim().equals("") && !nondigitordot.matcher(answer).find();
	}
	
	private String getPolicyId(String answer){
		if(answer.equalsIgnoreCase("NO"))
			return "null";
		
		return answer;		
	}
	
	private boolean answerLegalKeySize(String answer){		
		if(!answerInDigits(answer))
			return false;
		
		int keysize = getInt(answer);
		
		return keysize == 512 || keysize == 1024 || keysize == 2048 || keysize == 4096;
	}
	
	private boolean answerLegalValidity(String answer){
		if(!answerInDigits(answer))
			return false;
				
		  int val = getInt(answer);
		
		return val > 0 && val < 35600;		
	}
	
	private int getInt(String answer){
		int returnval = -1;
		try{
			returnval = Integer.parseInt(answer);			
		}catch(Exception e){}
		return returnval;
	}
	
	private boolean answerLegalComputerName(String answer){
		return !answer.trim().equals("") && !this.notcomputername.matcher(answer).find();
	}
	
	private boolean answerLegalPassword(String answer){
		int len = answer.length();
		
		return len > 1 && len < 14;
	    			
	}
	
	
}
