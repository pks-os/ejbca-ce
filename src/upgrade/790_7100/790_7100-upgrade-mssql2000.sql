-- subjectDn column size is increased in ApprovalData table to conform with subjectDn in other tables e.g. UserData
-- ALTER TABLE ApprovalData MODIFY subjectDn VARCHAR(400);

-- ALTER TABLE AcmeAuthorizationData ADD identifier VARCHAR(256);
-- ALTER TABLE AcmeAuthorizationData ADD identifierType VARCHAR(20);
-- ALTER TABLE AcmeAuthorizationData ADD expires BIGINT;
-- ALTER TABLE AcmeAuthorizationData ADD status VARCHAR(20);

-- DROP INDEX IF EXISTS acmeauthorizationdata_idx1.AcmeAuthorizationData;
-- CREATE INDEX acmeauthorizationdata_idx1 ON AcmeAuthorizationData (orderId,accountId,expires,status);