alter table AccessRulesData drop foreign key FKABB4C1DFDBBC970;
alter table AdminEntityData drop foreign key FKD9A99EBCB3A110AD;
drop table if exists AccessRulesData;
drop table if exists AcmeAuthorizationData;
drop table if exists AcmeChallengeData;
drop table if exists AcmeNonceData;
drop table if exists AdminEntityData;
drop table if exists AdminGroupData;
drop table if exists AdminPreferencesData;
drop table if exists ApprovalData;
drop table if exists AuditRecordData;
drop table if exists AuthorizationTreeUpdateData;
drop table if exists Base64CertData;
drop table if exists CAData;
drop table if exists CRLData;
drop table if exists CertReqHistoryData;
drop table if exists CertificateData;
drop table if exists CertificateProfileData;
drop table if exists CryptoTokenData;
drop table if exists EndEntityProfileData;
drop table if exists GlobalConfigurationData;
drop table if exists HardTokenCertificateMap;
drop table if exists HardTokenData;
drop table if exists HardTokenIssuerData;
drop table if exists HardTokenProfileData;
drop table if exists HardTokenPropertyData;
drop table if exists InternalKeyBindingData;
drop table if exists KeyRecoveryData;
drop table if exists PeerData;
drop table if exists ProfileData;
drop table if exists PublisherData;
drop table if exists PublisherQueueData;
drop table if exists BlacklistData;
drop table if exists RoleData;
drop table if exists RoleMemberData;
drop table if exists ServiceData;
drop table if exists UserData;
drop table if exists UserDataSourceData;
drop table if exists NoConflictCertificateData;
drop table if exists AcmeAccountData;
