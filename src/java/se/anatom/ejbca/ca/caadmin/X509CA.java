package se.anatom.ejbca.ca.caadmin;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.PublicKey;
import java.security.cert.CRL;
import java.security.cert.Certificate;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEREncodableVector;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DERInputStream;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.CRLNumber;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.PolicyInformation;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.PKCS7SignedData;
import org.bouncycastle.jce.X509KeyUsage;
import org.bouncycastle.jce.X509V2CRLGenerator;
import org.bouncycastle.jce.X509V3CertificateGenerator;

import se.anatom.ejbca.ca.auth.UserAuthData;
import se.anatom.ejbca.ca.crl.RevokedCertInfo;
import se.anatom.ejbca.ca.exception.SignRequestSignatureException;
import se.anatom.ejbca.ca.sign.SernoGenerator;
import se.anatom.ejbca.ca.store.certificateprofiles.CertificateProfile;
import se.anatom.ejbca.util.CertTools;

/**
 * X509CA is a implementation of a CA and holds data specific for Certificate and CRL generation 
 * according to the X509 standard. 
 *
 * @version $Id: X509CA.java,v 1.5 2003-10-03 14:34:20 herrvendil Exp $
 */
public class X509CA extends CA implements Serializable {

    // Default Values
    public static final float LATEST_VERSION = 1;

    private X509Name subjectx509name = null;
    
    // protected fields.
    protected static final String POLICYID                       = "policyid";
    protected static final String SUBJECTALTNAME                 = "subjectaltname";
    protected static final String USEAUTHORITYKEYIDENTIFIER      = "useauthoritykeyidentifier";
    protected static final String AUTHORITYKEYIDENTIFIERCRITICAL = "authoritykeyidentifiercritical";
    protected static final String USECRLNUMBER                   = "usecrlnumber";
    protected static final String CRLNUMBERCRITICAL              = "crlnumbercritical";

      
    // Public Methods
    /** Creates a new instance of CA, this constuctor should be used when a new CA is created */
    public X509CA(X509CAInfo cainfo) {
      super((CAInfo) cainfo);  
      
      data.put(POLICYID, cainfo.getPolicyId());
      data.put(SUBJECTALTNAME,  cainfo.getSubjectAltName());            
      setUseAuthorityKeyIdentifier(cainfo.getUseAuthorityKeyIdentifier());
      setAuthorityKeyIdentifierCritical(cainfo.getAuthorityKeyIdentifierCritical()); 
      setUseCRLNumber(cainfo.getUseCRLNumber());
      setCRLNumberCritical(cainfo.getCRLNumberCritical());
      setFinishUser(cainfo.getFinishUser());
      
      data.put(CA.CATYPE, new Integer(CAInfo.CATYPE_X509));
      data.put(VERSION, new Float(LATEST_VERSION));      
    }
    
   /** Constructor used when retrieving existing X509CA from database. */
    public X509CA(HashMap data, String name, int status, Date expiretime){
      super(data,name, status, expiretime);
    }

    // Public Methods.
    public String getPolicyId(){ return (String) data.get(POLICYID);}
    public void setPolicyId(String policyid){ data.put(POLICYID, policyid);}
    
    public String getSubjectAltName() { return (String) data.get(SUBJECTALTNAME);}
    
    public boolean  getUseAuthorityKeyIdentifier(){
      return ((Boolean)data.get(USEAUTHORITYKEYIDENTIFIER)).booleanValue();
    }
    public void setUseAuthorityKeyIdentifier(boolean useauthoritykeyidentifier) {
      data.put(USEAUTHORITYKEYIDENTIFIER, new Boolean(useauthoritykeyidentifier));
    }
    
    public boolean  getAuthorityKeyIdentifierCritical(){
      return ((Boolean)data.get(AUTHORITYKEYIDENTIFIERCRITICAL)).booleanValue();
    }
    public void setAuthorityKeyIdentifierCritical(boolean authoritykeyidentifiercritical) {
      data.put(AUTHORITYKEYIDENTIFIERCRITICAL, new Boolean(authoritykeyidentifiercritical));
    }

    public boolean  getUseCRLNumber(){return ((Boolean)data.get(USECRLNUMBER)).booleanValue();}
    public void setUseCRLNumber(boolean usecrlnumber) {data.put(USECRLNUMBER, new Boolean(usecrlnumber));}
    
    public boolean  getCRLNumberCritical(){return ((Boolean)data.get(CRLNUMBERCRITICAL)).booleanValue();}
    public void setCRLNumberCritical(boolean crlnumbercritical) {data.put(CRLNUMBERCRITICAL, new Boolean(crlnumbercritical));}
    
    
    public void updateCA(CAInfo cainfo){
      super.updateCA(cainfo); 
      X509CAInfo info = (X509CAInfo) cainfo;

      setUseAuthorityKeyIdentifier(info.getUseAuthorityKeyIdentifier());
      setAuthorityKeyIdentifierCritical(info.getAuthorityKeyIdentifierCritical()); 
      setUseCRLNumber(info.getUseCRLNumber());
      setCRLNumberCritical(info.getCRLNumberCritical());
    }
    
    public CAInfo getCAInfo() throws Exception{                
      return new X509CAInfo(getSubjectDN(), getName(), getStatus(), getSubjectAltName() ,getCertificateProfileId(),  
                    getValidity(), getExpireTime(), getCAType(), getSignedBy(), getCertificateChain(), 
                    getCAToken().getCATokenInfo(), getDescription(), getRevokationReason(), getRevokationDate(), getPolicyId(), getCRLPeriod(), getCRLPublishers(),
                    getUseAuthorityKeyIdentifier(), getAuthorityKeyIdentifierCritical(),
                    getUseCRLNumber(), getCRLNumberCritical(), getFinishUser()); 
    }


    public byte[] createPKCS7(Certificate cert) throws SignRequestSignatureException {
        // First verify that we signed this certificate
        try {
            if (cert != null)
                cert.verify(getCAToken().getPublicSignKey(), getCAToken().getProvider());
        } catch (Exception e) {
            throw new SignRequestSignatureException("Cannot verify certificate in createPKCS7(), did I sign this?");
        }
        Collection chain = getCertificateChain();
        Certificate[] certs;
        if (cert != null) {
            certs = new Certificate[chain.size()+1];
            certs[0] = cert;
            Iterator iter = chain.iterator();
            int i=1;
            while(iter.hasNext()){
              certs[i] = (Certificate) iter.next();
              i++;
            }                
        } else {
            certs = (Certificate[]) chain.toArray(new Certificate[chain.size()]);
        }
        try {
            PKCS7SignedData pkcs7 = new PKCS7SignedData(getCAToken().getPrivateSignKey(),certs,"SHA1",getCAToken().getProvider());

            return pkcs7.getEncoded();
        } catch (Exception e) {
            throw new javax.ejb.EJBException(e);
        }   
    }    
    
    
    public Certificate generateCertificate(UserAuthData subject, 
                                           PublicKey publicKey, 
                                           int keyusage, 
                                           long validity,
                                           CertificateProfile certProfile) throws Exception{
                                               
        System.out.println("X509 : signalg="+getCAToken().getCATokenInfo().getSignatureAlgorithm());
        final String sigAlg = getCAToken().getCATokenInfo().getSignatureAlgorithm();
        Date firstDate = new Date();
        // Set back startdate ten minutes to avoid some problems with wrongly set clocks.
        firstDate.setTime(firstDate.getTime() - 10 * 60 * 1000);
        Date lastDate = new Date();
        // validity in days = validity*24*60*60*1000 milliseconds
        long val = validity;
        if(val == -1)
          val = certProfile.getValidity();
        
        lastDate.setTime(lastDate.getTime() + ( val * 24 * 60 * 60 * 1000));
        X509V3CertificateGenerator certgen = new X509V3CertificateGenerator();
        // Serialnumber is random bits, where random generator is initialized when this
        // bean is created.
        BigInteger serno = SernoGenerator.instance().getSerno();
        certgen.setSerialNumber(serno);
        certgen.setNotBefore(firstDate);
        certgen.setNotAfter(lastDate);
        certgen.setSignatureAlgorithm(sigAlg);
        // Make DNs
        String dn = subject.getDN(); 
        String altName = subject.getAltName(); 
      
        certgen.setSubjectDN(CertTools.stringToBcX509Name(dn));
        X509Name caname = getSubjectDNAsX509Name();
        certgen.setIssuerDN(caname);
        certgen.setPublicKey(publicKey);

        // Basic constranits, all subcerts are NOT CAs
        if (certProfile.getUseBasicConstraints() == true) {
            boolean isCA = false;
            if ((certProfile.getType() == CertificateProfile.TYPE_SUBCA)
                || (certProfile.getType() == CertificateProfile.TYPE_ROOTCA))
                isCA = true;
            BasicConstraints bc = new BasicConstraints(isCA);
            certgen.addExtension(
                X509Extensions.BasicConstraints.getId(),
                certProfile.getBasicConstraintsCritical(),
                bc);
        }
        // Key usage
        int newKeyUsage = -1;
        if (certProfile.getAllowKeyUsageOverride() && (keyusage >= 0)) {
            newKeyUsage = keyusage;
        } else {
            newKeyUsage = sunKeyUsageToBC(certProfile.getKeyUsage());
        }
        if ( (certProfile.getUseKeyUsage() == true) && (newKeyUsage >=0) ){
            X509KeyUsage ku = new X509KeyUsage(newKeyUsage);
            certgen.addExtension(
                X509Extensions.KeyUsage.getId(),
                certProfile.getKeyUsageCritical(),
                ku);
        }
        // Extended Key usage
        if (certProfile.getUseExtendedKeyUsage() == true) {
            // Get extended key usage from certificate profile
            Collection c = certProfile.getExtendedKeyUsageAsOIDStrings();
            Vector usage = new Vector();
            Iterator iter = c.iterator();
            while (iter.hasNext()) {
                usage.add(new DERObjectIdentifier((String)iter.next()));
            }
            ExtendedKeyUsage eku = new ExtendedKeyUsage(usage);
            // Extended Key Usage may be either critical or non-critical
            certgen.addExtension(
                X509Extensions.ExtendedKeyUsage.getId(),
                certProfile.getExtendedKeyUsageCritical(),
                eku);
        }
        // Subject key identifier
        if (certProfile.getUseSubjectKeyIdentifier() == true) {
            SubjectPublicKeyInfo spki =
                new SubjectPublicKeyInfo(
                    (ASN1Sequence) new DERInputStream(new ByteArrayInputStream(publicKey.getEncoded())).readObject());
            SubjectKeyIdentifier ski = new SubjectKeyIdentifier(spki);
            certgen.addExtension(
                X509Extensions.SubjectKeyIdentifier.getId(),
                certProfile.getSubjectKeyIdentifierCritical(), ski);
        }
        // Authority key identifier
        if (certProfile.getUseAuthorityKeyIdentifier() == true) {
            SubjectPublicKeyInfo apki =
                new SubjectPublicKeyInfo(
                    (ASN1Sequence) new DERInputStream(new ByteArrayInputStream(getCAToken().getPublicSignKey().getEncoded())).readObject());
            AuthorityKeyIdentifier aki = new AuthorityKeyIdentifier(apki);
            certgen.addExtension(
                X509Extensions.AuthorityKeyIdentifier.getId(),
                certProfile.getAuthorityKeyIdentifierCritical(), aki);
        }
        // Subject Alternative name
        if ( (certProfile.getUseSubjectAlternativeName() == true) && (altName != null) && (altName.length() > 0) ) {
            String email = CertTools.getEmailFromDN(altName);
            DEREncodableVector vec = new DEREncodableVector();
            if (email != null) {
                GeneralName gn = new GeneralName(new DERIA5String(email), 1);
                vec.add(gn);
            }
            String dns = CertTools.getPartFromDN(altName, CertTools.DNS);
            if (dns != null) {
                GeneralName gn = new GeneralName(new DERIA5String(dns), 2);
                vec.add(gn);
            }
            String uri = CertTools.getPartFromDN(altName, CertTools.URI);
            if (uri == null){            
                uri  = CertTools.getPartFromDN(altName, CertTools.URI1);
            }
            if (uri != null) {
                GeneralName gn = new GeneralName(new DERIA5String(uri), 6);
                vec.add(gn);
            }
            String upn =  CertTools.getPartFromDN(altName, CertTools.UPN);
            if (upn != null) {
                ASN1EncodableVector v = new ASN1EncodableVector();
                v.add(new DERObjectIdentifier(CertTools.UPN_OBJECTID));
                v.add(new DERTaggedObject(true, 0, new DERUTF8String(upn)));
                //GeneralName gn = new GeneralName(new DERSequence(v), 0);
                DERObject gn = new DERTaggedObject(false, 0, new DERSequence(v));
                vec.add(gn);
            }
            if (vec.size() > 0) {
                GeneralNames san = new GeneralNames(new DERSequence(vec));
                certgen.addExtension(X509Extensions.SubjectAlternativeName.getId(), certProfile.getSubjectAlternativeNameCritical(), san);
            }
        }
        
        // Certificate Policies
         if (certProfile.getUseCertificatePolicies() == true) {
                 PolicyInformation pi = new PolicyInformation(new DERObjectIdentifier(certProfile.getCertificatePolicyId()));
                 DERSequence seq = new DERSequence(pi);
                 certgen.addExtension(X509Extensions.CertificatePolicies.getId(),
                         certProfile.getCertificatePoliciesCritical(), seq);
         }

         // CRL Distribution point URI
         if (certProfile.getUseCRLDistributionPoint() == true) {
             // Multiple CDPs are spearated with the ';' sign
            StringTokenizer tokenizer = new StringTokenizer(certProfile.getCRLDistributionPointURI(), ";", false);
            DEREncodableVector vec = new DEREncodableVector();
            while (tokenizer.hasMoreTokens()) {
                GeneralName gn = new GeneralName(new DERIA5String(tokenizer.nextToken()), 6);
                GeneralNames gns = new GeneralNames(new DERSequence(gn));
                DistributionPointName dpn = new DistributionPointName(0, gns);
                DistributionPoint distp = new DistributionPoint(dpn, null, null);
                vec.add(distp);
            }
            if (vec.size() > 0) {
                certgen.addExtension(X509Extensions.CRLDistributionPoints.getId(),
                    certProfile.getCRLDistributionPointCritical(), new DERSequence(vec));
            }
         }
		 
        X509Certificate cert =
            certgen.generateX509Certificate(getCAToken().getPrivateSignKey(), 
                                            getCAToken().getProvider());
        
        
        // Verify before returning
        cert.verify(getCAToken().getPublicSignKey());
        
      return (X509Certificate) cert;                                                                                        
    }
    
    public CRL generateCRL(Vector certs, int crlnumber) throws Exception {
        final String sigAlg= getCAToken().getCATokenInfo().getSignatureAlgorithm();

        Date thisUpdate = new Date();
        Date nextUpdate = new Date();

        // crlperiod is hours = crlperiod*60*60*1000 milliseconds
        nextUpdate.setTime(nextUpdate.getTime() + (getCRLPeriod() * 60 * 60 * 1000));
        X509V2CRLGenerator crlgen = new X509V2CRLGenerator();
        crlgen.setThisUpdate(thisUpdate);
        crlgen.setNextUpdate(nextUpdate);
        crlgen.setSignatureAlgorithm(sigAlg);
        // Make DNs
        X509Name caname = new X509Name(getSubjectDN());
        crlgen.setIssuerDN(caname);
        if (certs != null) {            
            Iterator it = certs.iterator();
            while( it.hasNext() ) {
                RevokedCertInfo certinfo = (RevokedCertInfo)it.next();
                crlgen.addCRLEntry(certinfo.getUserCertificate(), certinfo.getRevocationDate(), certinfo.getReason());
            }
        }

        // Authority key identifier
        if (getUseAuthorityKeyIdentifier() == true) {
            SubjectPublicKeyInfo apki = new SubjectPublicKeyInfo((ASN1Sequence)new DERInputStream(
                new ByteArrayInputStream(getCAToken().getPublicSignKey().getEncoded())).readObject());
            AuthorityKeyIdentifier aki = new AuthorityKeyIdentifier(apki);
            crlgen.addExtension(X509Extensions.AuthorityKeyIdentifier.getId(), getAuthorityKeyIdentifierCritical(), aki);
        }
        // CRLNumber extension
        if (getUseCRLNumber() == true) {
            CRLNumber crlnum = new CRLNumber(BigInteger.valueOf(crlnumber));
            crlgen.addExtension(X509Extensions.CRLNumber.getId(),  this.getCRLNumberCritical(), crlnum);
        }
        X509CRL crl = crlgen.generateX509CRL(getCAToken().getPrivateSignKey(),getCAToken().getProvider());
                                
        // Verify before sending back
        crl.verify(getCAToken().getPublicSignKey());

        return (X509CRL)crl;        
    }    
    
    /** Implemtation of UpgradableDataHashMap function getLatestVersion */
    public float getLatestVersion(){
       return LATEST_VERSION;
    }

    /** Implemtation of UpgradableDataHashMap function upgrade. */

    public void upgrade(){
      if(LATEST_VERSION != getVersion()){
        // New version of the class, upgrade

        data.put(VERSION, new Float(LATEST_VERSION));
      }  
    }
    
   // private help methods
    private int sunKeyUsageToBC(boolean[] sku) {
        int bcku = 0;
        if (sku[0] == true)
            bcku = bcku | X509KeyUsage.digitalSignature;
        if (sku[1] == true)
            bcku = bcku | X509KeyUsage.nonRepudiation;
        if (sku[2] == true)
            bcku = bcku | X509KeyUsage.keyEncipherment;
        if (sku[3] == true)
            bcku = bcku | X509KeyUsage.dataEncipherment;
        if (sku[4] == true)
            bcku = bcku | X509KeyUsage.keyAgreement;
        if (sku[5] == true)
            bcku = bcku | X509KeyUsage.keyCertSign;
        if (sku[6] == true)
            bcku = bcku | X509KeyUsage.cRLSign;
        if (sku[7] == true)
            bcku = bcku | X509KeyUsage.encipherOnly;
        if (sku[8] == true)
            bcku = bcku | X509KeyUsage.decipherOnly;
        return bcku;
    }
    
    private X509Name getSubjectDNAsX509Name(){
      if(subjectx509name == null){
        subjectx509name = CertTools.stringToBcX509Name(getSubjectDN());  
      }
        
      return subjectx509name;  
    }
   
    
}
