CREATE TABLE HARDTOKENDATA(TOKENSN VARCHAR(256) NOT NULL PRIMARY KEY,USERNAME VARCHAR(256),CTIME BIGINT NOT NULL,MTIME BIGINT NOT NULL,TOKENTYPE INTEGER NOT NULL,SIGNIFICANTISSUERDN VARCHAR(256),DATA VARBINARY)
CREATE TABLE HARDTOKENISSUERDATA(ID INTEGER NOT NULL PRIMARY KEY,ALIAS VARCHAR(256),CERTIFICATESN VARCHAR(256),CERTISSUERDN VARCHAR(256),DATA VARBINARY)
CREATE TABLE HARDTOKENPROFILEDATA(ID INTEGER NOT NULL PRIMARY KEY,NAME VARCHAR(256),UPDATECOUNTER INTEGER NOT NULL,DATA VARBINARY(1100000))
CREATE TABLE HARDTOKENCERTIFICATEMAP(CERTIFICATEFINGERPRINT VARCHAR(256) NOT NULL PRIMARY KEY,TOKENSN VARCHAR(256))
CREATE TABLE KEYRECOVERYDATA(PK INTEGER NOT NULL PRIMARY KEY,CERTSN VARCHAR(256),ISSUERDN VARCHAR(256),USERNAME VARCHAR(256),MARKEDASRECOVERABLE BIT NOT NULL,KEYPAIR VARBINARY)
CREATE TABLE ADMINGROUPDATA(PK INTEGER NOT NULL PRIMARY KEY,ADMINGROUPNAME VARCHAR(256),CAID INTEGER NOT NULL)
CREATE TABLE ACCESSRULESDATA(PK INTEGER NOT NULL PRIMARY KEY,ACCESSRULE VARCHAR(256),RULE INTEGER NOT NULL,ISRECURSIVE BIT NOT NULL,ADMINGROUPDATA_ACCESSRULES INTEGER)
CREATE TABLE AUTHORIZATIONTREEUPDATEDATA(PK INTEGER NOT NULL PRIMARY KEY,AUTHORIZATIONTREEUPDATENUMBER INTEGER NOT NULL)
CREATE TABLE ADMINENTITYDATA(PK INTEGER NOT NULL PRIMARY KEY,MATCHWITH INTEGER NOT NULL,MATCHTYPE INTEGER NOT NULL,MATCHVALUE VARCHAR(256),ADMINGROUPDATA_ADMINENTITIES INTEGER)
CREATE TABLE CADATA(CAID INTEGER NOT NULL PRIMARY KEY,NAME VARCHAR(256),SUBJECTDN VARCHAR(256),STATUS INTEGER NOT NULL,EXPIRETIME BIGINT NOT NULL,DATA VARCHAR(256))
CREATE TABLE CERTIFICATEDATA(BASE64CERT VARCHAR(256),FINGERPRINT VARCHAR(256) NOT NULL PRIMARY KEY,SUBJECTDN VARCHAR(256),ISSUERDN VARCHAR(256),CAFINGERPRINT VARCHAR(256),SERIALNUMBER VARCHAR(256),STATUS INTEGER NOT NULL,TYPE INTEGER NOT NULL,USERNAME VARCHAR(256),EXPIREDATE BIGINT NOT NULL,REVOCATIONDATE BIGINT NOT NULL,REVOCATIONREASON INTEGER NOT NULL)
CREATE TABLE CERTIFICATEPROFILEDATA(ID INTEGER NOT NULL PRIMARY KEY,CERTIFICATEPROFILENAME VARCHAR(256),DATA VARBINARY)
CREATE TABLE CRLDATA(CRLNUMBER INTEGER NOT NULL,ISSUERDN VARCHAR(256),FINGERPRINT VARCHAR(256) NOT NULL PRIMARY KEY,CAFINGERPRINT VARCHAR(256),THISUPDATE BIGINT NOT NULL,NEXTUPDATE BIGINT NOT NULL,BASE64CRL VARCHAR(256))
CREATE TABLE LOGCONFIGURATIONDATA(ID INTEGER NOT NULL PRIMARY KEY,LOGCONFIGURATION VARBINARY,LOGENTRYROWNUMBER INTEGER NOT NULL)
CREATE TABLE LOGENTRYDATA(ID INTEGER NOT NULL PRIMARY KEY,ADMINTYPE INTEGER NOT NULL,ADMINDATA VARCHAR(256),CAID INTEGER NOT NULL,MODULE INTEGER NOT NULL,TIME BIGINT NOT NULL,USERNAME VARCHAR(256),CERTIFICATESNR VARCHAR(256),EVENT INTEGER NOT NULL,COMMENT VARCHAR(256))
CREATE TABLE ADMINPREFERENCESDATA(ID VARCHAR(256) NOT NULL PRIMARY KEY,DATA VARBINARY)
CREATE TABLE USERDATA(USERNAME VARCHAR(256) NOT NULL PRIMARY KEY,SUBJECTDN VARCHAR(256),CAID INTEGER NOT NULL,SUBJECTALTNAME VARCHAR(256),SUBJECTEMAIL VARCHAR(256),STATUS INTEGER NOT NULL,TYPE INTEGER NOT NULL,CLEARPASSWORD VARCHAR(256),PASSWORDHASH VARCHAR(256),TIMECREATED BIGINT NOT NULL,TIMEMODIFIED BIGINT NOT NULL,ENDENTITYPROFILEID INTEGER NOT NULL,CERTIFICATEPROFILEID INTEGER NOT NULL,TOKENTYPE INTEGER NOT NULL,HARDTOKENISSUERID INTEGER NOT NULL,KEYSTOREPASSWORD VARCHAR(256),EXTENDEDINFORMATIONDATA VARBINARY)
CREATE TABLE ENDENTITYPROFILEDATA(ID INTEGER NOT NULL PRIMARY KEY,PROFILENAME VARCHAR(256),DATA VARBINARY)
CREATE TABLE GLOBALCONFIGURATIONDATA(CONFIGURATIONID VARCHAR(256) NOT NULL PRIMARY KEY,DATA VARBINARY)
CREATE TABLE HARDTOKENPROPERTYDATA(ID INTEGER NOT NULL,PROPERTY VARCHAR(256) NOT NULL,VALUE VARCHAR(256),CONSTRAINT PK_HARDTOKENPROPERTYDATA PRIMARY KEY(ID,PROPERTY))
