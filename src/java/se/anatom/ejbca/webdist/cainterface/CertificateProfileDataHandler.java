package se.anatom.ejbca.webdist.cainterface;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;

import se.anatom.ejbca.SecConst;
import se.anatom.ejbca.authorization.AuthorizationDeniedException;
import se.anatom.ejbca.authorization.IAuthorizationSessionLocal;
import se.anatom.ejbca.ca.exception.CertificateProfileExistsException;
import se.anatom.ejbca.ca.store.ICertificateStoreSessionLocal;
import se.anatom.ejbca.ca.store.certificateprofiles.CertificateProfile;
import se.anatom.ejbca.log.Admin;
import se.anatom.ejbca.webdist.webconfiguration.InformationMemory;
/**
 * A class handling the certificate type data. It saves and retrieves them currently from a database.
 *
 * @author  TomSelleck
 */
public class CertificateProfileDataHandler implements Serializable {

    public static final int FIXED_CERTIFICATEPROFILE_BOUNDRY        = SecConst.FIXED_CERTIFICATEPROFILE_BOUNDRY;
    /** Creates a new instance of CertificateProfileDataHandler */
    public CertificateProfileDataHandler(Admin administrator, ICertificateStoreSessionLocal certificatesession, IAuthorizationSessionLocal authorizationsession, InformationMemory info) {
       this.certificatestoresession = certificatesession;           
       this.authorizationsession = authorizationsession;
       this.administrator = administrator;          
       this.info = info;       
    }
    
       /** Method to add a certificate profile. Throws CertificateProfileExitsException if profile already exists  */
    public void addCertificateProfile(String name, CertificateProfile profile) throws CertificateProfileExistsException, AuthorizationDeniedException {
      if(authorizedToProfile(profile, true)){
        certificatestoresession.addCertificateProfile(administrator, name, profile);
        this.info.certificateProfilesEdited();
      }else
        throw new AuthorizationDeniedException("Not authorized to add certificate profile");  
    }    

       /** Method to change a certificate profile. */     
    public void changeCertificateProfile(String name, CertificateProfile profile) throws AuthorizationDeniedException{
      if(authorizedToProfile(profile, true)){ 
        certificatestoresession.changeCertificateProfile(administrator, name,profile);   
        this.info.certificateProfilesEdited();
      }else
        throw new AuthorizationDeniedException("Not authorized to edit certificate profile");      
    }
    
    /** Method to remove a end entity profile.*/ 
    public void removeCertificateProfile(String name) throws AuthorizationDeniedException{
     if(authorizedToProfileName(name, true)){    
        certificatestoresession.removeCertificateProfile(administrator, name);
        this.info.certificateProfilesEdited();
     }else
        throw new AuthorizationDeniedException("Not authorized to remove certificate profile");        
    }
    
    /** Metod to rename a end entity profile */
    public void renameCertificateProfile(String oldname, String newname) throws CertificateProfileExistsException, AuthorizationDeniedException{
     if(authorizedToProfileName(oldname, true)){    
       certificatestoresession.renameCertificateProfile(administrator, oldname,newname);
       this.info.certificateProfilesEdited();
     }else
       throw new AuthorizationDeniedException("Not authorized to rename certificate profile");
    }
    

    public void cloneCertificateProfile(String originalname, String newname) throws CertificateProfileExistsException, AuthorizationDeniedException{         
      if(authorizedToProfileName(originalname, true)){
        certificatestoresession.cloneCertificateProfile(administrator, originalname,newname);
        this.info.certificateProfilesEdited();
      }else
         throw new AuthorizationDeniedException("Not authorized to clone certificate profile");          
    }        
    


      /** Method to get a reference to a end entity profile.*/ 
    public CertificateProfile getCertificateProfile(int id) throws AuthorizationDeniedException{
      if(!authorizedToProfileId(id, false))
        throw new AuthorizationDeniedException("Not authorized to certificate profile");            
      
      return certificatestoresession.getCertificateProfile(administrator, id); 
    }      
          
    public CertificateProfile getCertificateProfile(String profilename) throws AuthorizationDeniedException{
     if(!authorizedToProfileName(profilename, false))
        throw new AuthorizationDeniedException("Not authorized to certificate profile");            
         
      return certificatestoresession.getCertificateProfile(administrator, profilename);
    }
   
      
    public int getCertificateProfileId(String profilename){
      return certificatestoresession.getCertificateProfileId(administrator, profilename);  
    }
    
    
    /**
     * Help function that checks if administrator is authorized to edit profile with given name.
     */
    private boolean authorizedToProfileName(String profilename, boolean editcheck){
      CertificateProfile profile = certificatestoresession.getCertificateProfile(administrator, profilename);
      return authorizedToProfile(profile, editcheck);
    }
     
    
    /**
     * Help function that checks if administrator is authorized to edit profile with given name.
     */
    private boolean authorizedToProfileId(int profileid, boolean editcheck){
      CertificateProfile profile = certificatestoresession.getCertificateProfile(administrator, profileid);
      return authorizedToProfile(profile, editcheck);
    }
    
    /**
     * Help function that checks if administrator is authorized to edit profile.
     */    
    private boolean authorizedToProfile(CertificateProfile profile, boolean editcheck){
      boolean returnval = false;  
      boolean allexists = false;  
      try{  
        boolean issuperadministrator = false;
        try{
          issuperadministrator = authorizationsession.isAuthorizedNoLog(administrator, "/super_administrator");  
        }catch(AuthorizationDeniedException ade){}
        
        if(editcheck)  
          authorizationsession.isAuthorizedNoLog(administrator, "/ca_functionality/edit_certificate_profiles");
        HashSet authorizedcaids = new HashSet(authorizationsession.getAuthorizedCAIds(administrator));

        if(profile != null){       
          Collection availablecas = profile.getAvailableCAs();
          if(availablecas.contains(new Integer(CertificateProfile.ANYCA))){
            if(issuperadministrator)
              returnval = true;  
          }else
            returnval = authorizedcaids.containsAll(availablecas);                       
        }
      }catch(AuthorizationDeniedException e){}
         
      return returnval;  
    }    
   
    private ICertificateStoreSessionLocal  certificatestoresession; 
    private Admin                          administrator;
    private IAuthorizationSessionLocal     authorizationsession;
    private InformationMemory              info;
}
