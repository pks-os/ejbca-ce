CREATE TABLE AccessRulesData (
    pK INTEGER NOT NULL,
    accessRule VARCHAR(256) NOT NULL,
    isRecursive BOOLEAN NOT NULL,
    rowProtection VARCHAR,
    rowVersion INTEGER NOT NULL,
    rule INTEGER NOT NULL,
    AdminGroupData_accessRules INTEGER,
    PRIMARY KEY (pK)
);

CREATE TABLE AdminEntityData (
    pK INTEGER NOT NULL,
    cAId INTEGER NOT NULL,
    matchType INTEGER NOT NULL,
    matchValue VARCHAR(256),
    matchWith INTEGER NOT NULL,
    rowProtection VARCHAR,
    rowVersion INTEGER NOT NULL,
    tokenType VARCHAR(256),
    AdminGroupData_adminEntities INTEGER,
    PRIMARY KEY (pK)
);

CREATE TABLE AdminGroupData (
    pK INTEGER NOT NULL,
    adminGroupName VARCHAR(256) NOT NULL,
    rowProtection VARCHAR,
    rowVersion INTEGER NOT NULL,
    PRIMARY KEY (pK)
);

CREATE TABLE AdminPreferencesData (
    id VARCHAR(256) NOT NULL,
    data VARBINARY NOT NULL,
    rowProtection VARCHAR,
    rowVersion INTEGER NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE ApprovalData (
    id INTEGER NOT NULL,
    approvalData VARCHAR NOT NULL,
    approvalId INTEGER NOT NULL,
    approvalType INTEGER NOT NULL,
    cAId INTEGER NOT NULL,
    endEntityProfileId INTEGER NOT NULL,
    expireDate BIGINT NOT NULL,
    remainingApprovals INTEGER NOT NULL,
    reqAdminCertIssuerDn VARCHAR(256),
    reqAdminCertSn VARCHAR(256),
    requestData VARCHAR NOT NULL,
    requestDate BIGINT NOT NULL,
    rowProtection VARCHAR,
    rowVersion INTEGER NOT NULL,
    status INTEGER NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE AuditRecordData (
    pk VARCHAR(256) NOT NULL,
    additionalDetails VARCHAR,
    authToken VARCHAR(256) NOT NULL,
    customId VARCHAR(256),
    eventStatus VARCHAR(256) NOT NULL,
    eventType VARCHAR(256) NOT NULL,
    module VARCHAR(256) NOT NULL,
    nodeId VARCHAR(256) NOT NULL,
    rowProtection VARCHAR,
    rowVersion INTEGER NOT NULL,
    searchDetail1 VARCHAR(256),
    searchDetail2 VARCHAR(256),
    sequenceNumber BIGINT NOT NULL,
    service VARCHAR(256) NOT NULL,
    timeStamp BIGINT NOT NULL,
    PRIMARY KEY (pk)
);

CREATE TABLE AuthorizationTreeUpdateData (
    pK INTEGER NOT NULL,
    authorizationTreeUpdateNumber INTEGER NOT NULL,
    rowProtection VARCHAR,
    rowVersion INTEGER NOT NULL,
    PRIMARY KEY (pK)
);

CREATE TABLE Base64CertData (
    fingerprint VARCHAR(256) NOT NULL,
    base64Cert VARCHAR,
    rowProtection VARCHAR,
    rowVersion INTEGER NOT NULL,
    certificateRequest VARCHAR,
    PRIMARY KEY (fingerprint)
);

CREATE TABLE CAData (
    cAId INTEGER NOT NULL,
    data VARCHAR NOT NULL,
    expireTime BIGINT NOT NULL,
    name VARCHAR(256),
    rowProtection VARCHAR,
    rowVersion INTEGER NOT NULL,
    status INTEGER NOT NULL,
    subjectDN VARCHAR(256),
    updateTime BIGINT NOT NULL,
    PRIMARY KEY (cAId)
);

CREATE TABLE CRLData (
    fingerprint VARCHAR(256) NOT NULL,
    base64Crl VARCHAR NOT NULL,
    cAFingerprint VARCHAR(256) NOT NULL,
    crlPartitionIndex INTEGER,
    cRLNumber INTEGER NOT NULL,
    deltaCRLIndicator INTEGER NOT NULL,
    issuerDN VARCHAR(256) NOT NULL,
    nextUpdate BIGINT NOT NULL,
    rowProtection VARCHAR,
    rowVersion INTEGER NOT NULL,
    thisUpdate BIGINT NOT NULL,
    PRIMARY KEY (fingerprint)
);

CREATE TABLE CertReqHistoryData (
    fingerprint VARCHAR(256) NOT NULL,
    issuerDN VARCHAR(256) NOT NULL,
    rowProtection VARCHAR,
    rowVersion INTEGER NOT NULL,
    serialNumber VARCHAR(256) NOT NULL,
    timestamp BIGINT NOT NULL,
    userDataVO VARCHAR NOT NULL,
    username VARCHAR(256) NOT NULL,
    PRIMARY KEY (fingerprint)
);

CREATE TABLE CertificateData (
    fingerprint VARCHAR(256) NOT NULL,
    base64Cert VARCHAR,
    cAFingerprint VARCHAR(256),
    certificateProfileId INTEGER NOT NULL,
    endEntityProfileId INTEGER,
    crlPartitionIndex INTEGER,
    expireDate BIGINT NOT NULL,
    issuerDN VARCHAR(256) NOT NULL,
    notBefore BIGINT,
    revocationDate BIGINT NOT NULL,
    revocationReason INTEGER NOT NULL,
    rowProtection VARCHAR,
    rowVersion INTEGER NOT NULL,
    serialNumber VARCHAR(256) NOT NULL,
    status INTEGER NOT NULL,
    subjectAltName VARCHAR(2000),
    subjectDN VARCHAR(400) NOT NULL,
    subjectKeyId VARCHAR(256),
    tag VARCHAR(256),
    type INTEGER NOT NULL,
    updateTime BIGINT NOT NULL,
    username VARCHAR(256),
    certificateRequest VARCHAR,
    PRIMARY KEY (fingerprint)
);

CREATE TABLE CertificateProfileData (
    id INTEGER NOT NULL,
    certificateProfileName VARCHAR(256) NOT NULL,
    data VARBINARY NOT NULL,
    rowProtection VARCHAR,
    rowVersion INTEGER NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE CryptoTokenData (
    id INTEGER NOT NULL,
    lastUpdate BIGINT NOT NULL,
    rowProtection VARCHAR,
    rowVersion INTEGER NOT NULL,
    tokenData VARCHAR,
    tokenName VARCHAR(256) NOT NULL,
    tokenProps VARCHAR,
    tokenType VARCHAR(256) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE EndEntityProfileData (
    id INTEGER NOT NULL,
    data VARBINARY NOT NULL,
    profileName VARCHAR(256) NOT NULL,
    rowProtection VARCHAR,
    rowVersion INTEGER NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE GlobalConfigurationData (
    configurationId VARCHAR(256) NOT NULL,
    data VARBINARY NOT NULL,
    rowProtection VARCHAR,
    rowVersion INTEGER NOT NULL,
    PRIMARY KEY (configurationId)
);

CREATE TABLE HardTokenCertificateMap (
    certificateFingerprint VARCHAR(256) NOT NULL,
    rowProtection VARCHAR,
    rowVersion INTEGER NOT NULL,
    tokenSN VARCHAR(256) NOT NULL,
    PRIMARY KEY (certificateFingerprint)
);

CREATE TABLE HardTokenData (
    tokenSN VARCHAR(256) NOT NULL,
    cTime BIGINT NOT NULL,
    data VARBINARY,
    mTime BIGINT NOT NULL,
    rowProtection VARCHAR,
    rowVersion INTEGER NOT NULL,
    significantIssuerDN VARCHAR(256),
    tokenType INTEGER NOT NULL,
    username VARCHAR(256),
    PRIMARY KEY (tokenSN)
);

CREATE TABLE HardTokenIssuerData (
    id INTEGER NOT NULL,
    adminGroupId INTEGER NOT NULL,
    alias VARCHAR(256) NOT NULL,
    data VARBINARY NOT NULL,
    rowProtection VARCHAR,
    rowVersion INTEGER NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE HardTokenProfileData (
    id INTEGER NOT NULL,
    data VARCHAR,
    name VARCHAR(256) NOT NULL,
    rowProtection VARCHAR,
    rowVersion INTEGER NOT NULL,
    updateCounter INTEGER NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE HardTokenPropertyData (
    id VARCHAR(256) NOT NULL,
    property VARCHAR(256) NOT NULL,
    rowProtection VARCHAR,
    rowVersion INTEGER NOT NULL,
    value VARCHAR(256),
    PRIMARY KEY (id,
    property)
);

CREATE TABLE InternalKeyBindingData (
    id INTEGER NOT NULL,
    certificateId VARCHAR(256),
    cryptoTokenId INTEGER NOT NULL,
    keyBindingType VARCHAR(256) NOT NULL,
    keyPairAlias VARCHAR(256) NOT NULL,
    lastUpdate BIGINT NOT NULL,
    name VARCHAR(256) NOT NULL,
    rawData VARCHAR,
    rowProtection VARCHAR,
    rowVersion INTEGER NOT NULL,
    status VARCHAR(256) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE KeyRecoveryData (
    certSN VARCHAR(256) NOT NULL,
    issuerDN VARCHAR(256) NOT NULL,
    cryptoTokenId INTEGER DEFUALT 0 NOT NULL,
    keyAlias VARCHAR(256),
    keyData VARCHAR NOT NULL,
    markedAsRecoverable BOOLEAN NOT NULL,
    publicKeyId VARCHAR(256),
    rowProtection VARCHAR,
    rowVersion INTEGER NOT NULL,
    username VARCHAR(256),
    PRIMARY KEY (certSN,
    issuerDN)
);

CREATE TABLE PeerData (
    id INTEGER NOT NULL,
    connectorState INTEGER NOT NULL,
    data VARCHAR,
    name VARCHAR(256) NOT NULL,
    rowProtection VARCHAR,
    rowVersion INTEGER NOT NULL,
    url VARCHAR(256) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE ProfileData (
    id INTEGER NOT NULL,
    profileName VARCHAR(256) NOT NULL,
    profileType VARCHAR(256) NOT NULL,
    rawData VARCHAR,
    rowProtection VARCHAR,
    rowVersion INTEGER NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE PublisherData (
    id INTEGER NOT NULL,
    data VARCHAR,
    name VARCHAR(256),
    rowProtection VARCHAR,
    rowVersion INTEGER NOT NULL,
    updateCounter INTEGER NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE PublisherQueueData (
    pk VARCHAR(256) NOT NULL,
    fingerprint VARCHAR(256),
    lastUpdate BIGINT NOT NULL,
    publishStatus INTEGER NOT NULL,
    publishType INTEGER NOT NULL,
    publisherId INTEGER NOT NULL,
    rowProtection VARCHAR,
    rowVersion INTEGER NOT NULL,
    timeCreated BIGINT NOT NULL,
    tryCounter INTEGER NOT NULL,
    volatileData VARCHAR,
    PRIMARY KEY (pk)
);

CREATE TABLE BlacklistData (
    id INTEGER NOT NULL,
    type VARCHAR(256) NOT NULL,
    value VARCHAR(256) NOT NULL,
    data VARCHAR(256),
    rowProtection VARCHAR,
    rowVersion INTEGER NOT NULL,
    updateCounter INTEGER NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE RoleData (
    id INTEGER NOT NULL,
    roleName VARCHAR(256) NOT NULL,
    nameSpace VARCHAR(256),
    rawData VARCHAR,
    rowProtection VARCHAR,
    rowVersion INTEGER NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE RoleMemberData (
    primaryKey INTEGER NOT NULL,
    tokenType VARCHAR(256) NOT NULL,
    tokenIssuerId INTEGER NOT NULL,
    tokenMatchKey INTEGER NOT NULL,
    tokenMatchOperator INTEGER NOT NULL,
    tokenMatchValue VARCHAR(2000),
    roleId INTEGER NOT NULL,
    description VARCHAR(256),
    rowProtection VARCHAR,
    rowVersion INTEGER NOT NULL,
    PRIMARY KEY (primaryKey)
);

CREATE TABLE ServiceData (
    id INTEGER NOT NULL,
    data VARCHAR,
    name VARCHAR(256) NOT NULL,
    nextRunTimeStamp BIGINT NOT NULL,
    rowProtection VARCHAR,
    rowVersion INTEGER NOT NULL,
    runTimeStamp BIGINT NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE UserData (
    username VARCHAR(256) NOT NULL,
    cAId INTEGER NOT NULL,
    cardNumber VARCHAR(256),
    certificateProfileId INTEGER NOT NULL,
    clearPassword VARCHAR(256),
    endEntityProfileId INTEGER NOT NULL,
    extendedInformationData VARCHAR,
    hardTokenIssuerId INTEGER NOT NULL,
    keyStorePassword VARCHAR(256),
    passwordHash VARCHAR(256),
    rowProtection VARCHAR,
    rowVersion INTEGER NOT NULL,
    status INTEGER NOT NULL,
    subjectAltName VARCHAR(2000),
    subjectDN VARCHAR(400),
    subjectEmail VARCHAR(256),
    timeCreated BIGINT NOT NULL,
    timeModified BIGINT NOT NULL,
    tokenType INTEGER NOT NULL,
    type INTEGER NOT NULL,
    PRIMARY KEY (username)
);

CREATE TABLE UserDataSourceData (
    id INTEGER NOT NULL,
    data VARCHAR,
    name VARCHAR(256) NOT NULL,
    rowProtection VARCHAR,
    rowVersion INTEGER NOT NULL,
    updateCounter INTEGER NOT NULL,
    PRIMARY KEY (id)
);

alter table AccessRulesData add constraint FKABB4C1DFDBBC970 foreign key (AdminGroupData_accessRules) references AdminGroupData;

alter table AdminEntityData add constraint FKD9A99EBCB3A110AD foreign key (AdminGroupData_adminEntities) references AdminGroupData;

CREATE TABLE NoConflictCertificateData (
    id VARCHAR(256) NOT NULL,
    fingerprint VARCHAR(256) NOT NULL,
    base64Cert VARCHAR,
    cAFingerprint VARCHAR(256),
    certificateProfileId INTEGER NOT NULL,
    endEntityProfileId INTEGER,
    crlPartitionIndex INTEGER,
    expireDate BIGINT NOT NULL,
    issuerDN VARCHAR(256) NOT NULL,
    notBefore BIGINT,
    revocationDate BIGINT NOT NULL,
    revocationReason INTEGER NOT NULL,
    rowProtection VARCHAR,
    rowVersion INTEGER NOT NULL,
    serialNumber VARCHAR(256) NOT NULL,
    status INTEGER NOT NULL,
    subjectAltName VARCHAR(2000),
    subjectDN VARCHAR(400) NOT NULL,
    subjectKeyId VARCHAR(256),
    tag VARCHAR(256),
    type INTEGER NOT NULL,
    updateTime BIGINT NOT NULL,
    username VARCHAR(256),
    certificateRequest VARCHAR,
    PRIMARY KEY (id)
);

CREATE TABLE AcmeNonceData (
    nonce VARCHAR(256) NOT NULL,
    timeExpires BIGINT NOT NULL,
    rowProtection VARCHAR,
    rowVersion INTEGER NOT NULL,
    PRIMARY KEY (nonce)
);

CREATE TABLE AcmeAccountData (
    accountId VARCHAR(256) NOT NULL,
    currentKeyId VARCHAR(256) NOT NULL,
    rawData VARCHAR,
    rowProtection VARCHAR,
    rowVersion INTEGER NOT NULL,
    PRIMARY KEY (accountId)
);

CREATE TABLE AcmeOrderData (
    orderId VARCHAR(256) NOT NULL,
    accountId VARCHAR(256) NOT NULL,
    fingerprint VARCHAR(256),
    status VARCHAR(256) NOT NULL,
    rawData VARCHAR,
    rowProtection VARCHAR,
    rowVersion INTEGER NOT NULL,
    PRIMARY KEY (orderId)
);

CREATE TABLE AcmeChallengeData (
    challengeId VARCHAR(256) NOT NULL,
    authorizationId VARCHAR(256) NOT NULL,
    type VARCHAR(20) NOT NULL,
    rawData VARCHAR,
    rowProtection VARCHAR,
    rowVersion INTEGER NOT NULL,
    PRIMARY KEY (challengeId)
);

CREATE TABLE AcmeAuthorizationData (
    authorizationId VARCHAR(256) NOT NULL,
    orderId VARCHAR(256),
    accountId VARCHAR(256) NOT NULL,
    rawData VARCHAR,
    rowProtection VARCHAR,
    rowVersion INTEGER NOT NULL,
    PRIMARY KEY (authorizationId)
);
