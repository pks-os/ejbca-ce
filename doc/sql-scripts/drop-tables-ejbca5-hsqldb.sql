alter table AccessRulesData drop constraint FKABB4C1DFDBBC970;
alter table AdminEntityData drop constraint FKD9A99EBCB3A110AD;
drop table AccessRulesData if exists;
drop table AdminEntityData if exists;
drop table AdminGroupData if exists;
drop table AdminPreferencesData if exists;
drop table ApprovalData if exists;
drop table AuditRecordData if exists;
drop table AuthorizationTreeUpdateData if exists;
drop table CAData if exists;
drop table CRLData if exists;
drop table CertReqHistoryData if exists;
drop table CertificateData if exists;
drop table CertificateProfileData if exists;
drop table CryptoTokenData if exists;
drop table EndEntityProfileData if exists;
drop table InternalKeyBindingData if exists;
drop table GlobalConfigurationData if exists;
drop table HardTokenCertificateMap if exists;
drop table HardTokenData if exists;
drop table HardTokenIssuerData if exists;
drop table HardTokenProfileData if exists;
drop table HardTokenPropertyData if exists;
drop table KeyRecoveryData if exists;
drop table PublisherData if exists;
drop table PublisherQueueData if exists;
drop table ServiceData if exists;
drop table UserData if exists;
drop table UserDataSourceData if exists;
