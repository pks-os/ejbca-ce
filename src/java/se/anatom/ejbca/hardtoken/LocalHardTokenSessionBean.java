package se.anatom.ejbca.hardtoken;

import java.math.BigInteger;
import java.util.Date;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.Random;
import java.sql.*;
import java.security.cert.X509Certificate;
import javax.sql.DataSource;
import javax.naming.*;
import javax.ejb.*;

import org.apache.log4j.Logger;

import se.anatom.ejbca.BaseSessionBean;
import se.anatom.ejbca.ra.UserAdminData;
import se.anatom.ejbca.log.ILogSessionLocal;
import se.anatom.ejbca.log.ILogSessionLocalHome;
import se.anatom.ejbca.log.Admin;
import se.anatom.ejbca.log.LogEntry;
import se.anatom.ejbca.util.CertTools;
import se.anatom.ejbca.hardtoken.hardtokentypes.*;
import se.anatom.ejbca.authorization.IAuthorizationSessionLocal;
import se.anatom.ejbca.authorization.IAuthorizationSessionLocalHome;
import se.anatom.ejbca.ca.store.ICertificateStoreSessionLocal;
import se.anatom.ejbca.ca.store.ICertificateStoreSessionLocalHome;

/**
 * Stores data used by web server clients.
 * Uses JNDI name for datasource as defined in env 'Datasource' in ejb-jar.xml.
 *
 * @version $Id: LocalHardTokenSessionBean.java,v 1.14 2003-09-03 12:47:24 herrvendil Exp $
 */
public class LocalHardTokenSessionBean extends BaseSessionBean  {

    private static Logger log = Logger.getLogger(LocalHardTokenSessionBean.class);

    /** Var holding JNDI name of datasource */
    private String dataSource = "";

    /** The local home interface of hard token issuer entity bean. */
    private HardTokenIssuerDataLocalHome hardtokenissuerhome = null;

    /** The local home interface of hard token entity bean. */
    private HardTokenDataLocalHome hardtokendatahome = null;

    /** The local home interface of hard token certificate map entity bean. */
    private HardTokenCertificateMapLocalHome hardtokencertificatemaphome = null;

    /** The local interface of authorization session bean */
    private IAuthorizationSessionLocal authorizationsession = null;

    /** The local interface of certificate store session bean */
    private ICertificateStoreSessionLocal certificatestoresession = null;

    /** The remote interface of  log session bean */
    private ILogSessionLocal logsession = null;

    /** Data about to the system available hard tokens, information is retrieved from META-INF.XML */
    private AvailableHardToken[] availablehardtokens = null;


     /**
     * Default create for SessionBean without any creation Arguments.
     * @throws CreateException if bean instance can't be created
     */


    public void ejbCreate() throws CreateException {
        debug(">ejbCreate()");
      try{
        dataSource = (String)lookup("java:comp/env/DataSource", java.lang.String.class);
        debug("DataSource=" + dataSource);
        hardtokenissuerhome = (HardTokenIssuerDataLocalHome) lookup("java:comp/env/ejb/HardTokenIssuerData", HardTokenIssuerDataLocalHome.class);
        hardtokendatahome = (HardTokenDataLocalHome) lookup("java:comp/env/ejb/HardTokenData", HardTokenDataLocalHome.class);
        hardtokencertificatemaphome = (HardTokenCertificateMapLocalHome) lookup("java:comp/env/ejb/HardTokenCertificateMap", HardTokenCertificateMapLocalHome.class);

        debug("<ejbCreate()");
      }catch(Exception e){
         throw new EJBException(e);
      }
    }


    /** Gets connection to Datasource used for manual SQL searches
     * @return Connection
     */
    private Connection getConnection() throws SQLException, NamingException {
        DataSource ds = (DataSource)getInitialContext().lookup(dataSource);
        return ds.getConnection();
    } //getConnection


    /** Gets connection to log session bean
     * @return Connection
     */
    private ILogSessionLocal getLogSession() {
        if(logsession == null){
          try{
            ILogSessionLocalHome logsessionhome = (ILogSessionLocalHome) lookup("java:comp/env/ejb/LogSessionLocal",ILogSessionLocalHome.class);
            logsession = logsessionhome.create();
          }catch(Exception e){
             throw new EJBException(e);
          }
        }
        return logsession;
    } //getLogSession

    /** Gets connection to certificate store session bean
     * @return Connection
     */
    private ICertificateStoreSessionLocal getCertificateStoreSession() {
        if(certificatestoresession == null){
          try{
            ICertificateStoreSessionLocalHome certificatestoresessionhome = (ICertificateStoreSessionLocalHome) lookup("java:comp/env/ejb/CertificateStoreSessionLocal",ICertificateStoreSessionLocalHome.class);
            certificatestoresession = certificatestoresessionhome.create();
          }catch(Exception e){
             throw new EJBException(e);
          }
        }
        return certificatestoresession;
    } //getCertificateStoreSession

    /** Gets connection to authorization session bean
     * @return IAuthorizationSessionLocal
     */
    private IAuthorizationSessionLocal getAuthorizationSession(Admin admin) {
        if(authorizationsession == null){
          try{
            IAuthorizationSessionLocalHome authorizationsessionhome = (IAuthorizationSessionLocalHome) lookup("java:comp/env/ejb/AuthorizationSessionLocal",IAuthorizationSessionLocalHome.class);
            authorizationsession = authorizationsessionhome.create();
          }catch(Exception e){
             throw new EJBException(e);
          }
        }
        return authorizationsession;
    } //getAuthorizationSession

    /**
     * Adds a hard token issuer to the database.
     *
     * @return false if hard token issuer already exists.
     * @throws EJBException if a communication or other error occurs.
     */

    public boolean addHardTokenIssuer(Admin admin, String alias, BigInteger certificatesn, String certissuerdn, HardTokenIssuer issuerdata){
       debug(">addHardTokenIssuer(alias: " + alias + ")");
       boolean returnval=false;
       try{
          hardtokenissuerhome.findByAlias(alias);
       }catch(FinderException e){
         try{
           hardtokenissuerhome.findByCertificateSN(certificatesn.toString(16), certissuerdn);
         }catch(FinderException f){
           try{
             hardtokenissuerhome.create(findFreeHardTokenIssuerId(), alias, certificatesn, certissuerdn, issuerdata);
             returnval = true;
           }catch(Exception g){}
         }
       }
     
       if(returnval)
         getLogSession().log(admin, certissuerdn.hashCode(), LogEntry.MODULE_HARDTOKEN, new java.util.Date(),null, null, LogEntry.EVENT_INFO_HARDTOKENISSUERDATA,"Hard token issuer " + alias + " added.");
       else
         getLogSession().log(admin, certissuerdn.hashCode(), LogEntry.MODULE_HARDTOKEN,  new java.util.Date(),null, null, LogEntry.EVENT_ERROR_HARDTOKENISSUERDATA,"Error adding hard token issuer "+ alias);
       
       debug("<addHardTokenIssuer()");
       return returnval;
    } // addHardTokenIssuer

    /**
     * Updates hard token issuer data
     *
     * @return false if  alias doesn't exists
     * @throws EJBException if a communication or other error occurs.
     */

    public boolean changeHardTokenIssuer(Admin admin, String alias, HardTokenIssuer issuerdata){
       debug(">changeHardTokenIssuer(alias: " + alias + ")");
       boolean returnvalue = false;
       int caid = ILogSessionLocal.INTERNALCAID;
       try{
         HardTokenIssuerDataLocal htih = hardtokenissuerhome.findByAlias(alias);
         htih.setHardTokenIssuer(issuerdata);
         caid = htih.getCertIssuerDN().hashCode();
         returnvalue = true;
       }catch(FinderException e){}
      
       if(returnvalue)
         getLogSession().log(admin, caid, LogEntry.MODULE_HARDTOKEN, new java.util.Date(),null, null, LogEntry.EVENT_INFO_HARDTOKENISSUERDATA,"Hard token issuer " +  alias + " edited.");
       else
         getLogSession().log(admin, ILogSessionLocal.INTERNALCAID, LogEntry.MODULE_HARDTOKEN, new java.util.Date(),null, null, LogEntry.EVENT_ERROR_HARDTOKENISSUERDATA,"Error editing hard token issuer " + alias + ".");

       debug("<changeHardTokenIssuer()");
       return returnvalue;
    } // changeHardTokenIssuer

     /**
     * Adds a hard token issuer with the same content as the original issuer,
     *
     * @return false if the new alias or certificatesn already exists.
     * @throws EJBException if a communication or other error occurs.
     */
    public boolean cloneHardTokenIssuer(Admin admin, String oldalias, String newalias, BigInteger newcertificatesn, String newcertissuerdn){
       debug(">cloneHardTokenIssuer(alias: " + oldalias + ")");
       HardTokenIssuer issuerdata = null;
       boolean returnval = false;
       try{
         HardTokenIssuerDataLocal htih = hardtokenissuerhome.findByAlias(oldalias);
         issuerdata = (HardTokenIssuer) htih.getHardTokenIssuer().clone();

         returnval = addHardTokenIssuer(admin, newalias, newcertificatesn, newcertissuerdn, issuerdata);
         if(returnval)
           getLogSession().log(admin, newcertissuerdn.hashCode(), LogEntry.MODULE_HARDTOKEN, new java.util.Date(),null, null, LogEntry.EVENT_INFO_HARDTOKENISSUERDATA,"New hard token issuer " + newalias +  ", used issuer " + oldalias + " as template.");
         else
           getLogSession().log(admin, newcertissuerdn.hashCode(), LogEntry.MODULE_HARDTOKEN,  new java.util.Date(),null, null, LogEntry.EVENT_ERROR_HARDTOKENISSUERDATA,"Error adding hard token issuer " + newalias +  " using issuer " + oldalias + " as template.");
       }catch(Exception e){
          throw new EJBException(e);
       }

       debug("<cloneHardTokenIssuer()");
       return returnval;
    } // cloneHardTokenIssuer

     /**
     * Removes a hard token issuer from the database.
     *
     * @throws EJBException if a communication or other error occurs.
     */
    public void removeHardTokenIssuer(Admin admin, String alias){
      debug(">removeHardTokenIssuer(alias: " + alias + ")");
      int caid = ILogSessionLocal.INTERNALCAID;
      try{
        HardTokenIssuerDataLocal htih = hardtokenissuerhome.findByAlias(alias);
        caid = htih.getCertIssuerDN().hashCode();
        htih.remove();
        getLogSession().log(admin, caid, LogEntry.MODULE_HARDTOKEN, new java.util.Date(),null, null, LogEntry.EVENT_INFO_HARDTOKENISSUERDATA,"Hard token issuer " + alias + " removed.");
      }catch(Exception e){
         getLogSession().log(admin, caid, LogEntry.MODULE_HARDTOKEN, new java.util.Date(),null, null, LogEntry.EVENT_ERROR_HARDTOKENISSUERDATA,"Error removing hard token issuer " + alias + ".",e);
      }
      debug("<removeHardTokenIssuer()");
    } // removeHardTokenIssuer

     /**
     * Renames a hard token issuer
     *
     * @return false if new alias or certificatesn already exists
     * @throws EJBException if a communication or other error occurs.
     */
    public boolean renameHardTokenIssuer(Admin admin, String oldalias, String newalias,
                                         BigInteger newcertificatesn, String newcertissuerdn){
       debug(">renameHardTokenIssuer(from " + oldalias + " to " + newalias + ")");
       boolean returnvalue = false;
       try{
          hardtokenissuerhome.findByAlias(newalias);
       }catch(FinderException e){
         try{
           hardtokenissuerhome.findByCertificateSN(newcertificatesn.toString(16), newcertissuerdn);
         }catch(FinderException f){
           try{
             HardTokenIssuerDataLocal htih = hardtokenissuerhome.findByAlias(oldalias);
             htih.setAlias(newalias);
             htih.setCertSN(newcertificatesn);
             htih.setCertIssuerDN(newcertissuerdn);
             returnvalue = true;
           }catch(FinderException g){}
         }
       }

       
       if(returnvalue)
         getLogSession().log(admin, newcertissuerdn.hashCode(), LogEntry.MODULE_HARDTOKEN, new java.util.Date(),null, null, LogEntry.EVENT_INFO_HARDTOKENISSUERDATA,"Hard token issuer " + oldalias + " renamed to " + newalias +  "." );
       else
         getLogSession().log(admin, newcertissuerdn.hashCode(), LogEntry.MODULE_HARDTOKEN, new java.util.Date(),null, null, LogEntry.EVENT_ERROR_HARDTOKENISSUERDATA," Error renaming hard token issuer  " + oldalias +  " to " + newalias + "." );


       debug("<renameHardTokenIssuer()");
       return returnvalue;
    } // renameHardTokenIssuer

      /**
       * Returns the available hard token issuers.
       *
       * @return A collection of available HardTokenIssuerData.
       * @throws EJBException if a communication or other error occurs.
       */
    public Collection getHardTokenIssuerDatas(Admin admin){
      debug(">getHardTokenIssuerDatas()");
      ArrayList returnval = new ArrayList();
      Collection result = null;
      HardTokenIssuerDataLocal htih = null;
      try{
        result = hardtokenissuerhome.findAll();
        if(result.size()>0){
          Iterator i = result.iterator();
          while(i.hasNext()){
            htih = (HardTokenIssuerDataLocal) i.next();
            returnval.add(new HardTokenIssuerData(htih.getId().intValue(), htih.getAlias(), htih.getCertSN(), htih.getCertIssuerDN(), htih.getHardTokenIssuer()));
          }
        }
        Collections.sort(returnval);
      }catch(Exception e){}

      debug("<getHardTokenIssuerDatas()");
      return returnval;
    } // getHardTokenIssuers

      /**
       * Returns the available hard token issuer alliases.
       *
       * @return A collection of available hard token issuer aliases.
       * @throws EJBException if a communication or other error occurs.
       */
    public Collection getHardTokenIssuerAliases(Admin admin){
      debug(">getHardTokenIssuerAliases()");
      ArrayList returnval = new ArrayList();
      Collection result = null;
      HardTokenIssuerDataLocal htih = null;
      try{
        result = hardtokenissuerhome.findAll();
        if(result.size()>0){
          Iterator i = result.iterator();
          while(i.hasNext()){
            htih = (HardTokenIssuerDataLocal) i.next();
            returnval.add(htih.getAlias());
          }
        }
        Collections.sort(returnval);
      }catch(Exception e){}

      debug("<getHardTokenIssuerAliases()");
      return returnval;
    }// getHardTokenIssuerAliases

      /**
       * Returns the available hard token issuers.
       *
       * @return A treemap of available hard token issuers.
       * @throws EJBException if a communication or other error occurs.
       */
    public TreeMap getHardTokenIssuers(Admin admin){
      debug(">getHardTokenIssuers()");
      TreeMap returnval = new TreeMap();
      Collection result = null;
      try{
        result = hardtokenissuerhome.findAll();
        if(result.size()>0){
          Iterator i = result.iterator();
          while(i.hasNext()){
            HardTokenIssuerDataLocal htih = (HardTokenIssuerDataLocal) i.next();
            returnval.put(htih.getAlias(), new HardTokenIssuerData(htih.getId().intValue(), htih.getAlias(), htih.getCertSN() ,htih.getCertIssuerDN(), htih.getHardTokenIssuer()));
          }
        }
      }catch(FinderException e){}

      debug("<getHardTokenIssuers()");
      return returnval;
    } // getHardTokenIssuers

      /**
       * Returns the specified hard token issuer.
       *
       * @return the hard token issuer data or null if hard token issuer doesn't exists.
       * @throws EJBException if a communication or other error occurs.
       */
    public HardTokenIssuerData getHardTokenIssuerData(Admin admin, String alias){
      debug(">getHardTokenIssuerData(alias: " + alias + ")");
      HardTokenIssuerData returnval = null;
      HardTokenIssuerDataLocal htih = null;
      try{
        htih = hardtokenissuerhome.findByAlias(alias);
        if(htih != null){
          returnval = new HardTokenIssuerData(htih.getId().intValue(), htih.getAlias(), htih.getCertSN() ,htih.getCertIssuerDN(), htih.getHardTokenIssuer());
        }
      }catch(Exception e){}

      debug("<getHardTokenIssuerData()");
      return returnval;
    } // getHardTokenIssuerData

       /**
       * Returns the specified  hard token issuer.
       *
       * @return the  hard token issuer data or null if  hard token issuer doesn't exists.
       * @throws EJBException if a communication or other error occurs.
       */
    public HardTokenIssuerData getHardTokenIssuerData(Admin admin, int id){
      debug(">getHardTokenIssuerData(id: " + id +")" );
      HardTokenIssuerData returnval = null;
      HardTokenIssuerDataLocal htih = null;
      try{
        htih = hardtokenissuerhome.findByPrimaryKey(new Integer(id));
        if(htih != null){
          returnval = new HardTokenIssuerData(htih.getId().intValue(), htih.getAlias(), htih.getCertSN() ,htih.getCertIssuerDN(), htih.getHardTokenIssuer());
        }
      }catch(Exception e){}

      debug("<getHardTokenIssuerData()");
      return returnval;
    } // getHardTokenIssuerData

       /**
       * Returns the specified  hard token issuer.
       *
       * @return the  hard token issuer data or null if  hard token issuer doesn't exists.
       * @throws EJBException if a communication or other error occurs.
       */
    public HardTokenIssuerData getHardTokenIssuerData(Admin admin, X509Certificate issuercertificate){
      debug(">getHardTokenIssuerData()");
      HardTokenIssuerData returnval = null;
      HardTokenIssuerDataLocal htih = null;
      try{
        htih = hardtokenissuerhome.findByCertificateSN(issuercertificate.getSerialNumber().toString(16), CertTools.getIssuerDN(issuercertificate));
        if(htih != null){
          returnval = new HardTokenIssuerData(htih.getId().intValue(), htih.getAlias(), htih.getCertSN() ,htih.getCertIssuerDN(), htih.getHardTokenIssuer());
        }
      }catch(Exception e){}

      debug("<getHardTokenIssuerData()");
      return returnval;
    } // getHardTokenIssuerData

      /**
       * Returns the number of available hard token issuer.
       *
       * @return the number of available hard token issuer.
       * @throws EJBException if a communication or other error occurs.
       */
    public int getNumberOfHardTokenIssuers(Admin admin){
      debug(">getNumberOfHardTokenIssuers()");
      int returnval =0;
      try{
        returnval = (hardtokenissuerhome.findAll()).size();
      }catch(FinderException e){}

      debug("<getNumberOfHardTokenIssuers()");
      return returnval;
    } // getNumberOfHardTokenIssuers

      /**
       * Returns a hard token issuer id given its alias.
       *
       * @return id number of hard token issuer.
       * @throws EJBException if a communication or other error occurs.
       */
    public int getHardTokenIssuerId(Admin admin, String alias){
      debug(">getHardTokenIssuerId(alias: " + alias + ")");
      int returnval = IHardTokenSessionRemote.NO_ISSUER;
      HardTokenIssuerDataLocal htih = null;
      try{
        htih = hardtokenissuerhome.findByAlias(alias);
        if(htih != null){
          returnval = htih.getId().intValue();
        }
      }catch(Exception e){}

      debug("<getHardTokenIssuerId()");
      return returnval;
    } // getNumberOfHardTokenIssuersId

      /**
       * Returns a hard token issuer id given the issuers certificate.
       *
       * @return id number of hard token issuer.
       * @throws EJBException if a communication or other error occurs.
       */
    public int getHardTokenIssuerId(Admin admin, X509Certificate issuercertificate){
      debug(">getHardTokenIssuerId()");
      int returnval = IHardTokenSessionRemote.NO_ISSUER;
      HardTokenIssuerDataLocal htih = null;
      try{
        htih = hardtokenissuerhome.findByCertificateSN(issuercertificate.getSerialNumber().toString(16), CertTools.getIssuerDN(issuercertificate));
        if(htih != null){
          returnval = htih.getId().intValue();
        }
      }catch(Exception e){}

      debug("<getHardTokenIssuerId()");
      return returnval;
    } // getNumberOfHardTokenIssuersId

       /**
       * Returns a hard token issuer alias given its id.
       *
       * @return the alias or null if id noesnt exists
       * @throws EJBException if a communication or other error occurs.
       */
    public String getHardTokenIssuerAlias(Admin admin, int id){
      debug(">getHardTokenIssuerAlias(id: " + id + ")");
      String returnval = null;
      HardTokenIssuerDataLocal htih = null;
      try{
        htih = hardtokenissuerhome.findByPrimaryKey(new Integer(id));
        if(htih != null){
          returnval = htih.getAlias();
        }
      }catch(Exception e){}

      debug("<getHardTokenIssuerAlias()");
      return returnval;
    } // getHardTokenIssuerAlias

        /**
       * Checks if a tokentype is among a hard tokens issuers available token types.
       *
       * @param admin, the administrator calling the function
       * @param isserid, the id of the issuer to check.
       * @param userdata, the data of user about to be generated
       *
       * @throws UnavalableTokenException if users tokentype isn't among hard token issuers available tokentypes.
       * @throws EJBException if a communication or other error occurs.
       */

    public void getIsTokenTypeAvailableToIssuer(Admin admin, int issuerid, UserAdminData userdata) throws UnavailableTokenException{
        debug(">getIsTokenTypeAvailableToIssuer(issuerid: " + issuerid + ", tokentype: " + userdata.getTokenType()+ ")");
        boolean returnval = false;
        ArrayList availabletokentypes = getHardTokenIssuerData(admin, issuerid).getHardTokenIssuer().getAvailableHardTokens();

        for(int i=0; i < availabletokentypes.size(); i++){
          if(((Integer) availabletokentypes.get(i)).intValue() == userdata.getTokenType())
            returnval = true;
        }

        if(!returnval)
          throw new UnavailableTokenException("Error hard token issuer cannot issue specified tokentype for user " + userdata.getUsername() + ". Change tokentype or issuer for user");
        debug("<getIsTokenTypeAvailableToIssuer()");
    } // getIsTokenTypeAvailableToIssuer

       /**
       * Adds a hard token to the database
       *
       * @param admin, the administrator calling the function
       * @param tokensn, The serialnumber of token.
       * @param username, the user owning the token.
       * @param significantissuerdn, indicates which CA the hard token should belong to.
       * @param hardtoken, the hard token data
       * @param certificates,  a collection of certificates places in the hard token
       *
       * @throws EJBException if a communication or other error occurs.
       * @throws HardTokenExistsException if tokensn already exists in databas.
       */
    public void addHardToken(Admin admin, String tokensn, String username, String significantissuerdn, int tokentype,  HardToken hardtokendata, Collection certificates) throws HardTokenExistsException{
        debug(">addHardToken(tokensn : " + tokensn + ")");
        try {
            hardtokendatahome.create(tokensn, username,new java.util.Date(), new java.util.Date(), tokentype, significantissuerdn, hardtokendata);
            if(certificates != null){
              Iterator i = certificates.iterator();
              while(i.hasNext()){
                addHardTokenCertificateMapping(admin, tokensn, (X509Certificate) i.next());
              }
            }
            getLogSession().log(admin, significantissuerdn.hashCode(), LogEntry.MODULE_HARDTOKEN, new java.util.Date(),username, null, LogEntry.EVENT_INFO_HARDTOKENDATA,"Hard token with serial number : " + tokensn + " added.");
        }
        catch (Exception e) {
          getLogSession().log(admin, significantissuerdn.hashCode(), LogEntry.MODULE_HARDTOKEN, new java.util.Date(),username, null, LogEntry.EVENT_ERROR_HARDTOKENDATA,"Trying to add hard tokensn that already exists.");
          throw new HardTokenExistsException("Tokensn : " + tokensn);
        }
        debug("<addHardToken()");
    } // addHardToken

       /**
       * changes a hard token data in the database
       *
       * @param admin, the administrator calling the function
       * @param tokensn, The serialnumber of token.      
       * @param hardtoken, the hard token data
       *
       * @throws EJBException if a communication or other error occurs.
       * @throws HardTokenDoesntExistsException if tokensn doesn't exists in databas.
       */
    public void changeHardToken(Admin admin, String tokensn, int tokentype, HardToken hardtokendata) throws HardTokenDoesntExistsException{
        debug(">changeHardToken(tokensn : " + tokensn + ")");
        int caid = ILogSessionLocal.INTERNALCAID;
        try {
            HardTokenDataLocal htd = hardtokendatahome.findByPrimaryKey(tokensn);
            htd.setTokenType(tokentype);
            htd.setHardToken(hardtokendata);
            htd.setModifyTime(new java.util.Date());
            caid = htd.getSignificantIssuerDN().hashCode();
            getLogSession().log(admin, caid, LogEntry.MODULE_HARDTOKEN, new java.util.Date(),htd.getUsername(), null, LogEntry.EVENT_INFO_HARDTOKENDATA,"Hard token with serial number : " + tokensn + " changed.");
        }
        catch (Exception e) {
            getLogSession().log(admin, caid, LogEntry.MODULE_HARDTOKEN, new java.util.Date(),null, null, LogEntry.EVENT_ERROR_HARDTOKENDATA,"Error when trying to update token with sn : " + tokensn + ".");
          throw new HardTokenDoesntExistsException("Tokensn : " + tokensn);
        }
        debug("<changeHardToken()");
    } // changeHardToken

       /**
       * removes a hard token data from the database
       *
       * @param admin, the administrator calling the function
       * @param tokensn, The serialnumber of token.
       *
       * @throws EJBException if a communication or other error occurs.
       * @throws HardTokenDoesntExistsException if tokensn doesn't exists in databas.
       */
    public void removeHardToken(Admin admin, String tokensn) throws HardTokenDoesntExistsException{
      debug(">removeHardToken(tokensn : " + tokensn + ")");
      int caid = ILogSessionLocal.INTERNALCAID;      
      try{
        HardTokenDataLocal htd = hardtokendatahome.findByPrimaryKey(tokensn);
        caid = htd.getSignificantIssuerDN().hashCode();
        htd.remove();
        getLogSession().log(admin, caid, LogEntry.MODULE_HARDTOKEN, new java.util.Date(),null, null, LogEntry.EVENT_INFO_HARDTOKENDATA,"Hard token with sn " + tokensn + " removed.");
      }catch(Exception e){
         getLogSession().log(admin, caid, LogEntry.MODULE_HARDTOKEN, new java.util.Date(),null, null, LogEntry.EVENT_ERROR_HARDTOKENDATA,"Error removing hard token with sn " + tokensn + ".");
         throw new HardTokenDoesntExistsException("Tokensn : " + tokensn);
      }
      debug("<removeHardToken()");
    } // removeHardToken

       /**
       * Checks if a hard token serialnumber exists in the database
       *
       * @param admin, the administrator calling the function
       * @param tokensn, The serialnumber of token.
       *
       * @return true if it exists or false otherwise.
       * @throws EJBException if a communication or other error occurs.
       */
    public boolean existsHardToken(Admin admin, String tokensn){
       debug(">existsHardToken(tokensn : " + tokensn + ")");
       boolean ret = false;
        try {
            hardtokendatahome.findByPrimaryKey(tokensn);
            ret = true;
        } catch (javax.ejb.FinderException fe) {
             ret=false;
        } catch(Exception e){
          throw new EJBException(e);
        }
       debug("<existsHardToken()");
       return ret;
    } // existsHardToken

      /**
       * returns hard token data for the specified tokensn
       *
       * @param admin, the administrator calling the function
       * @param tokensn, The serialnumber of token.
       *
       * @return the hard token data or NULL if tokensn doesnt exists in database.
       * @throws EJBException if a communication or other error occurs.
       */
    public HardTokenData getHardToken(Admin admin, String tokensn){
       debug("<getHardToken(tokensn :" + tokensn +")");
       HardTokenData returnval = null;
       HardTokenDataLocal htd = null;
       try{
         htd = hardtokendatahome.findByPrimaryKey(tokensn);
         if(htd != null){
           returnval = new HardTokenData(htd.getTokenSN(),htd.getUsername(), htd.getCreateTime(),htd.getModifyTime(),htd.getTokenType(),htd.getHardToken());
           getLogSession().log(admin, htd.getSignificantIssuerDN().hashCode(), LogEntry.MODULE_HARDTOKEN, new java.util.Date(),htd.getUsername(), null, LogEntry.EVENT_INFO_HARDTOKENVIEWED,"Hard token with sn " + tokensn + " viewed.");
         }
       }catch(Exception e){}

       debug("<getHardToken()");
       return returnval;
    } // getHardToken

      /**
       * returns hard token data for the specified user
       *
       * @param admin, the administrator calling the function
       * @param username, The username owning the tokens.
       *
       * @return a Collection of all hard token user data.
       * @throws EJBException if a communication or other error occurs.
       */
    public Collection getHardTokens(Admin admin, String username){
       debug("<getHardToken(username :" + username +")");
       ArrayList returnval = new ArrayList();
       HardTokenDataLocal htd = null;
       try{
         Collection result = hardtokendatahome.findByUsername(username);
         Iterator i = result.iterator();
         while(i.hasNext()){
           htd = (HardTokenDataLocal) i.next();
           returnval.add(new HardTokenData(htd.getTokenSN(),htd.getUsername(), htd.getCreateTime(),htd.getModifyTime(),htd.getTokenType(),htd.getHardToken()));
           getLogSession().log(admin, htd.getSignificantIssuerDN().hashCode(), LogEntry.MODULE_HARDTOKEN, new java.util.Date(),htd.getUsername(), null, LogEntry.EVENT_INFO_HARDTOKENVIEWED,"Hard token with sn " + htd.getTokenSN() + " viewed.");
         }
       }catch(Exception e){}

       debug("<getHardToken()");
       return returnval;
    } // getHardTokens

       /**
       * Adds a mapping between a hard token and a certificate
       *
       * @param admin, the administrator calling the function
       * @param tokensn, The serialnumber of token.
       * @param certificate, the certificate to map to.
       *
       * @return true if addition went successful. False if map already exists.
       * @throws EJBException if a communication or other error occurs.
       */
    public void addHardTokenCertificateMapping(Admin admin, String tokensn, X509Certificate certificate){
        String certificatesn = certificate.getSerialNumber().toString(16);
        debug(">addHardTokenCertificateMapping(certificatesn : "+ certificatesn  +", tokensn : " + tokensn + ")");
         
        try {
            hardtokencertificatemaphome.create(CertTools.getFingerprintAsString(certificate),tokensn);
            getLogSession().log(admin, certificate.getIssuerDN().toString().hashCode(), LogEntry.MODULE_HARDTOKEN, new java.util.Date(),null, null, LogEntry.EVENT_INFO_HARDTOKENCERTIFICATEMAP,"Certificate mapping added, certificatesn: "  + certificatesn +", tokensn: " + tokensn + " added.");
        }
        catch (Exception e) {
          getLogSession().log(admin, certificate.getIssuerDN().toString().hashCode(), LogEntry.MODULE_HARDTOKEN, new java.util.Date(),null, null, LogEntry.EVENT_ERROR_HARDTOKENCERTIFICATEMAP,"Error adding certificate mapping, certificatesn: "  + certificatesn +", tokensn: " + tokensn);
        }
        debug("<addHardTokenCertificateMapping()");
    } // addHardTokenCertificateMapping

      /**
       * Removes a mapping between a hard token and a certificate
       *
       * @param admin, the administrator calling the function
       * @param certificate, the certificate to map to.
       *
       * @return true if removal went successful.
       * @throws EJBException if a communication or other error occurs.
       */
    public void removeHardTokenCertificateMapping(Admin admin, X509Certificate certificate){
       String certificatesn = certificate.getSerialNumber().toString(16);
       debug(">removeHardTokenCertificateMapping(Certificatesn: " + certificatesn + ")");
      try{
        HardTokenCertificateMapLocal htcm =hardtokencertificatemaphome.findByPrimaryKey(CertTools.getFingerprintAsString(certificate));
        htcm.remove();
        getLogSession().log(admin, certificate.getIssuerDN().toString().hashCode(), LogEntry.MODULE_HARDTOKEN, new java.util.Date(),null, null, LogEntry.EVENT_INFO_HARDTOKENCERTIFICATEMAP, "Certificate mapping with certificatesn: "  + certificatesn +" removed.");
      }catch(Exception e){
         try{
           getLogSession().log(admin, certificate.getIssuerDN().toString().hashCode(), LogEntry.MODULE_HARDTOKEN, new java.util.Date(),null, null, LogEntry.EVENT_ERROR_HARDTOKENCERTIFICATEMAP, "Error removing certificate mapping with certificatesn " + certificatesn + ".");
         }catch(Exception re){
            throw new EJBException(e);
         }
      }
      debug("<removeHardTokenCertificateMapping()");
    } // removeHardTokenCertificateMapping

       /**
       * Returns all the X509Certificates places in a hard token.
       *
       * @param admin, the administrator calling the function
       * @param tokensn, The serialnumber of token.
       *
       * @return a collection of X509Certificates
       * @throws EJBException if a communication or other error occurs.
       */
    public Collection findCertificatesInHardToken(Admin admin, String tokensn){
       debug("<findCertificatesInHardToken(username :" + tokensn +")");
       ArrayList returnval = new ArrayList();
       HardTokenCertificateMapLocal htcm = null;
       try{
         Collection result = hardtokencertificatemaphome.findByTokenSN(tokensn);
         Iterator i = result.iterator();
         while(i.hasNext()){
           htcm = (HardTokenCertificateMapLocal) i.next();
           returnval.add(getCertificateStoreSession().findCertificateByFingerprint(admin, htcm.getCertificateFingerprint()));
         }
       }catch(Exception e){
          throw new EJBException(e);
       }

       debug("<findCertificatesInHardToken()");
       return returnval;
    } // findCertificatesInHardToken

       /**
       * Retrieves an array of to the system avaliable hardware tokens defines in the hard token modules ejb-jar.XML
       *
       *
       * @return an array of to the system available hard tokens.
       * @throws EJBException if a communication or other error occurs.
       */
    public AvailableHardToken[] getAvailableHardTokens(){
      debug(">getAvailableHardTokens()");
      if(availablehardtokens==null){
        String[] hardtokensclasses = null;
        String[] hardtokensnames = null;
        String[] hardtokensids = null;

        // Get configuration of log device classes from ejb-jar.xml
        String hardtokensclassstring = (String)lookup("java:comp/env/hardTokenClasses", java.lang.String.class);
        String hardtokensnamestring  = (String)lookup("java:comp/env/hardTokenNames", java.lang.String.class);
        String hardtokensidstring    = (String)lookup("java:comp/env/hardTokenIds", java.lang.String.class);

        try{
          hardtokensclasses = hardtokensclassstring.split(";");
          hardtokensnames  = hardtokensnamestring.split(";");
          hardtokensids  = hardtokensidstring.split(";");
        }catch(Exception e){
          throw new EJBException(e);
        }

        availablehardtokens = new AvailableHardToken[hardtokensclasses.length];
        for(int i=0; i < hardtokensclasses.length; i++){
          availablehardtokens[i] = new AvailableHardToken(hardtokensids[i], hardtokensnames[i], hardtokensclasses[i]);
        }
      }

      debug("<getAvailableHardTokens()");
      return availablehardtokens;
    } // getAvailableHardTokens

    /**
     * Method used to signal to the log that token was generated successfully.
     *
     * @param admin, administrator performing action
     * @param tokensn, tokensn of token generated
     * @param username, username of user token was generated for.
     * @param significantissuerdn, indicates which CA the hard token should belong to.
     *
     */
    public void tokenGenerated(Admin admin, String tokensn, String username, String significantissuerdn){
      try{
        getLogSession().log(admin, significantissuerdn.hashCode(), LogEntry.MODULE_HARDTOKEN, new java.util.Date(),username, null, LogEntry.EVENT_INFO_HARDTOKENGENERATED, "Token with serialnumber : " + tokensn + " generated successfully.");
      }catch(Exception e){
        throw new EJBException(e);
      }
    } // tokenGenerated

    /**
     * Method used to signal to the log that error occured when generating token.
     *
     * @param admin, administrator performing action
     * @param tokensn, tokensn of token.
     * @param username, username of user token was generated for.
     * @param significantissuerdn, indicates which CA the hard token should belong to.
     *
     */
    public void errorWhenGeneratingToken(Admin admin, String tokensn, String username, String significantissuerdn){
      try{
        getLogSession().log(admin, significantissuerdn.hashCode(), LogEntry.MODULE_HARDTOKEN, new java.util.Date(),username, null, LogEntry.EVENT_ERROR_HARDTOKENGENERATED, "Error when generating token with serialnumber : " + tokensn + ".");
      }catch(Exception e){
        throw new EJBException(e);
      }
    } // errorWhenGeneratingToken


    private Integer findFreeHardTokenIssuerId(){
      int id = (new Random((new Date()).getTime())).nextInt();
      boolean foundfree = false;

      while(!foundfree){
        try{
          if(id > 1)
            hardtokenissuerhome.findByPrimaryKey(new Integer(id));
          id++;
        }catch(FinderException e){
           foundfree = true;
        }
      }
      return new Integer(id);
    } // findFreeHardTokenIssuerId

} // LocalRaAdminSessionBean
