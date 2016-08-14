-- These columns are added by the JPA provider if there are sufficient privileges
-- ALTER TABLE CertificateData ADD notBefore BIGINT(20);
-- ALTER TABLE CertificateData ADD endEntityProfileId INT(11);
-- ALTER TABLE CertificateData ADD subjectAltName VARCHAR(2000) BINARY;
--
-- Table ProfileData is new and is added by the JPA provider if there are sufficient privileges. 
-- See create-tables-database.sql
--
-- subjectDN and subjectAltName columns in UserData has been extended to accommodate longer names
-- subjectDN from 250 to 400 characters and subjectAltName from 250 to 2000 characters
-- ALTER TABLE UserData MODIFY subjectAltName VARCHAR(2000) BINARY;
-- ALTER TABLE UserData MODIFY subjectDN VARCHAR(400) BINARY;
-- ALTER TABLE CertificateData MODIFY subjectDN VARCHAR(400) BINARY;
