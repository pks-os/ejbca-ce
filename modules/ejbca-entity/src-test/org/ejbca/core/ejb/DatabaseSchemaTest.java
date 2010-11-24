/*************************************************************************
 *                                                                       *
 *  EJBCA: The OpenSource Certificate Authority                          *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/

package org.ejbca.core.ejb;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.apache.log4j.Logger;
import org.bouncycastle.util.Arrays;
import org.ejbca.core.ejb.approval.ApprovalData;
import org.ejbca.core.ejb.authorization.AccessRulesData;
import org.ejbca.core.ejb.authorization.AdminEntityData;
import org.ejbca.core.ejb.authorization.AdminGroupData;
import org.ejbca.core.ejb.ca.caadmin.CAData;
import org.ejbca.core.ejb.ca.caadmin.CertificateProfileData;
import org.ejbca.core.ejb.ca.publisher.PublisherData;
import org.ejbca.core.ejb.ca.publisher.PublisherQueueData;
import org.ejbca.core.ejb.ca.store.CRLData;
import org.ejbca.core.ejb.ca.store.CertReqHistoryData;
import org.ejbca.core.ejb.ca.store.CertificateData;
import org.ejbca.core.ejb.hardtoken.HardTokenCertificateMap;
import org.ejbca.core.ejb.hardtoken.HardTokenData;
import org.ejbca.core.ejb.hardtoken.HardTokenIssuerData;
import org.ejbca.core.ejb.hardtoken.HardTokenProfileData;
import org.ejbca.core.ejb.hardtoken.HardTokenPropertyData;
import org.ejbca.core.ejb.keyrecovery.KeyRecoveryData;
import org.ejbca.core.ejb.log.LogConfigurationData;
import org.ejbca.core.ejb.log.LogEntryData;
import org.ejbca.core.ejb.ra.UserData;
import org.ejbca.core.ejb.ra.raadmin.AdminPreferencesData;
import org.ejbca.core.ejb.ra.raadmin.EndEntityProfileData;
import org.ejbca.core.ejb.ra.raadmin.GlobalConfigurationData;
import org.ejbca.core.ejb.ra.userdatasource.UserDataSourceData;
import org.ejbca.core.ejb.services.ServiceData;
import org.ejbca.core.model.log.LogConfiguration;

/**
 * Simple class to trigger Hibernate's JPA schema validation.
 * 
 * We also validate that all fields can hold the values that we assume they can.
 * 
 * TODO: Rewrite as a JUnit test
 * 
 * @version $Id$
 */
public class DatabaseSchemaTest {
	
	private static final Logger LOG = Logger.getLogger(DatabaseSchemaTest.class);

	private static String VARCHAR_250B;
	private static String CLOB_10KiB;
	private static String CLOB_100KiB;
	private static String CLOB_1MiB;
	private static String CLOB_100MiB;
	private static final HashMap HASHMAP_200K = new HashMap();
	private static final HashMap HASHMAP_1M = new HashMap();
	private static final int BOGUS_INT = -32;	// Very random..
	private static final Integer BOGUS_INTEGER = Integer.valueOf(BOGUS_INT);
	private static EntityManagerFactory entityManagerFactory;
	private static EntityManager entityManager;
	private static int exitStatus = 0;
	
	public static void main(String[] args) throws Exception {
		DatabaseSchemaTest instance = new DatabaseSchemaTest();
		instance.test000Setup();
		try {
			instance.testApprovalData();
			instance.testAccessRulesData();
			instance.testAdminEntityData();
			instance.testAdminGroupData();
			instance.testCAData();
			instance.testCertificateProfileData();
			instance.testPublisherData();
			instance.testPublisherQueueData();
			instance.testCertificateData();
			instance.testCertReqHistoryData();
			instance.testCRLData();
			instance.testHardTokenCertificateMap();
			instance.testHardTokenData();
			instance.testHardTokenIssuerData();
			instance.testHardTokenProfileData();
			instance.testHardTokenPropertyData();
			instance.testKeyRecoveryData();
			instance.testLogConfigurationData();
			instance.testLogEntryData();
			instance.testUserData();
			instance.testAdminPreferencesData();
			instance.testEndEntityProfileData();
			instance.testGlobalConfigurationData();
			instance.testUserDataSourceData();
			instance.testServiceData();
		} catch (Exception e) {
			exitStatus = 1;
		} finally {
			instance.testZZZCleanUp();
		}
		System.exit(exitStatus);
	}
	
	public void test000Setup() throws Exception {
		entityManagerFactory = Persistence.createEntityManagerFactory("ejbca-pu");
		entityManager = entityManagerFactory.createEntityManager();
		LOG.info("Allocating memory..");
		final byte[] lob250B = new byte[250];
		final byte[] lob10K = new byte[10*1024];
		final byte[] lob100K = new byte[100*1024];
		final byte[] lob196K = new byte[196*1024];
		final byte[] lob996K = new byte[996*1024];
		final byte[] lob1M = new byte[1*1024*1024];
		final byte[] lob100M = new byte[100*1024*1024];
		LOG.info("Filling memory..");
		Arrays.fill(lob250B, (byte) '0');
		Arrays.fill(lob10K, (byte) '0');
		Arrays.fill(lob100K, (byte) '0');
		Arrays.fill(lob196K, (byte) '0');
		Arrays.fill(lob996K, (byte) '0');
		Arrays.fill(lob1M, (byte) '0');
		Arrays.fill(lob100M, (byte) '0');
		LOG.info("Creating Strings..");
		VARCHAR_250B = new String(lob250B);
		CLOB_10KiB = new String(lob10K);
		CLOB_100KiB = new String(lob100K);
		CLOB_1MiB = new String(lob1M);
		CLOB_100MiB = new String(lob100M);
		LOG.info("Filling HashMaps..");
		HASHMAP_200K.put("object", lob196K);	// It need to be less than 200KiB in Serialized format..
		HASHMAP_1M.put("object", lob996K);		// It need to be less than 1MiB in Serialized format.. 
		LOG.info("Init done!");
	}
	
	/**
	 * Outputs which method it is run from.
	 * Validates that all getters on the entity that is annotated with @javax.persistence.Column is set. 
	 * Commits the entity in one transaction and then removes it in another transaction.
	 */
	private void storeAndRemoveEntity(Object entity) {
		LOG.info(Thread.currentThread().getStackTrace()[2].getMethodName() + " running:");
		Class<?> entityClass = entity.getClass();
		LOG.info("  - verifying that all getter has an assigned value for " + entityClass.getName());
		for (Method m : entityClass.getDeclaredMethods()) {
			for (Annotation a :m.getAnnotations()) {
				if (a.annotationType().equals(javax.persistence.Column.class) && m.getName().startsWith("get")) {
					try {
						m.setAccessible(true);
						if (m.invoke(entity) == null) {
							LOG.warn(m.getName() + " was annotated with @Column, but value was null. Test should be updated!");
							exitStatus = 1;
						}
					} catch (Exception e) {
						LOG.error(m.getName() + " was annotated with @Column and could not be read. " + e.getMessage());
						exitStatus = 1;
					}
				}
			}
		}
		LOG.info("  - adding entity.");
		EntityTransaction transaction = entityManager.getTransaction();
		transaction.begin();
		entityManager.persist(entity);
		transaction.commit();
		LOG.info("  - removing entity.");
		transaction = entityManager.getTransaction();
		transaction.begin();
		entityManager.remove(entity);
		transaction.commit();
		LOG.info(Thread.currentThread().getStackTrace()[2].getMethodName() + " done!");
	}
	
	/** Used in order to bypass validity check of different private fields that are access via transient setters. */
	private void setPrivateField(Object entity, String fieldName, Object value) {
		try {
			Field field = entity.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(entity, value);
		} catch (Exception e) {
			LOG.error("Could not set " + fieldName + " to " + value + ": " + e.getMessage());
		}
	}

	public void testApprovalData() {
		ApprovalData entity = new ApprovalData();
		entity.setApprovalid(0);
		entity.setApprovaldata(CLOB_1MiB);
		entity.setApprovaltype(0);
		entity.setCaid(0);
		entity.setEndentityprofileid(0);
		entity.setExpiredate(0);
		entity.setId(Integer.valueOf(0));
		entity.setRemainingapprovals(0);
		entity.setReqadmincertissuerdn(VARCHAR_250B);
		entity.setReqadmincertsn(VARCHAR_250B);
		entity.setRequestdata(CLOB_1MiB);
		entity.setRequestdate(0);
		entity.setRowProtection(CLOB_10KiB);
		entity.setRowVersion(0);
		entity.setStatus(0);
		storeAndRemoveEntity(entity);
	}

	public void testAccessRulesData() {
		AccessRulesData entity = new AccessRulesData();
		entity.setAccessRule(VARCHAR_250B);
		entity.setIsRecursive(false);
		entity.setPrimKey(BOGUS_INTEGER.intValue());
		entity.setRowProtection(CLOB_10KiB);
		entity.setRowVersion(0);
		entity.setRule(0);
		storeAndRemoveEntity(entity);
	}

	public void testAdminEntityData() {
		AdminEntityData entity = new AdminEntityData();
		entity.setCaId(BOGUS_INTEGER);
		entity.setMatchType(0);
		entity.setMatchValue(VARCHAR_250B);
		entity.setMatchWith(0);
		entity.setPrimeKey(BOGUS_INT);
		entity.setRowProtection(CLOB_10KiB);
		entity.setRowVersion(0);
		storeAndRemoveEntity(entity);
	}

	public void testAdminGroupData() {
		AdminGroupData entity = new AdminGroupData();
		entity.setAdminGroupName(VARCHAR_250B);
		entity.setCaId(BOGUS_INT);
		entity.setPrimeKey(BOGUS_INTEGER);
		entity.setRowProtection(CLOB_10KiB);
		entity.setRowVersion(0);
		storeAndRemoveEntity(entity);
	}

	public void testCAData() {
		CAData entity = new CAData();
		entity.setCaId(BOGUS_INTEGER);
		entity.setData(CLOB_100KiB);
		entity.setExpireTime(0);
		entity.setName(VARCHAR_250B);
		entity.setRowProtection(CLOB_10KiB);
		entity.setRowVersion(0);
		entity.setStatus(0);
		entity.setSubjectDN(VARCHAR_250B);
		entity.setUpdateTime(0);
		storeAndRemoveEntity(entity);
	}

	public void testCertificateProfileData() {
		CertificateProfileData entity = new CertificateProfileData();
		entity.setCertificateProfileName(VARCHAR_250B);
		entity.setDataUnsafe(HASHMAP_1M);
		entity.setId(BOGUS_INTEGER);
		entity.setRowProtection(CLOB_10KiB);
		entity.setRowVersion(0);
		storeAndRemoveEntity(entity);
	}

	public void testPublisherData() {
		PublisherData entity = new PublisherData();
		entity.setData(CLOB_100KiB);
		entity.setId(BOGUS_INTEGER);
		entity.setName(VARCHAR_250B);
		entity.setRowProtection(CLOB_10KiB);
		entity.setRowVersion(0);
		entity.setUpdateCounter(0);
		storeAndRemoveEntity(entity);
	}

	public void testPublisherQueueData() {
		PublisherQueueData entity = new PublisherQueueData();
		entity.setFingerprint(VARCHAR_250B);
		entity.setLastUpdate(0);
		entity.setPk(VARCHAR_250B);
		entity.setPublisherId(0);
		entity.setPublishStatus(0);
		entity.setPublishType(0);
		entity.setRowProtection(CLOB_10KiB);
		entity.setRowVersion(0);
		entity.setTimeCreated(0);
		entity.setTryCounter(0);
		entity.setVolatileData(CLOB_100KiB);
		storeAndRemoveEntity(entity);
	}

	public void testCertificateData() {
		CertificateData entity = new CertificateData();
		entity.setBase64Cert(CLOB_1MiB);
		entity.setCaFingerprint(VARCHAR_250B);
		entity.setCertificateProfileId(BOGUS_INTEGER);
		entity.setExpireDate(0L);
		entity.setFingerprint(VARCHAR_250B);
		setPrivateField(entity, "issuerDN", VARCHAR_250B);
		entity.setRevocationDate(0L);
		entity.setRevocationReason(0);
		entity.setRowProtection(CLOB_10KiB);
		entity.setRowVersion(0);
		entity.setSerialNumber(VARCHAR_250B);
		entity.setStatus(0);
		setPrivateField(entity, "subjectDN", VARCHAR_250B);
		entity.setSubjectKeyId(VARCHAR_250B);
		entity.setTag(VARCHAR_250B);
		entity.setType(0);
		entity.setUpdateTime(Long.valueOf(0L));
		entity.setUsername(VARCHAR_250B);
		storeAndRemoveEntity(entity);
	}

	public void testCertReqHistoryData() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		CertReqHistoryData entity = new CertReqHistoryData();
		setPrivateField(entity, "issuerDN", VARCHAR_250B);
		setPrivateField(entity, "fingerprint", VARCHAR_250B);
		entity.setRowProtection(CLOB_10KiB);
		entity.setRowVersion(0);
		setPrivateField(entity, "serialNumber", VARCHAR_250B);
		entity.setTimestamp(0L);
		entity.setUserDataVO(CLOB_1MiB);
		setPrivateField(entity, "username", VARCHAR_250B);
		storeAndRemoveEntity(entity);
	}

	public void testCRLData() {
		CRLData entity = new CRLData();
		entity.setBase64Crl(CLOB_100MiB);
		entity.setCaFingerprint(VARCHAR_250B);
		entity.setCrlNumber(0);
		entity.setDeltaCRLIndicator(0);
		entity.setFingerprint(VARCHAR_250B);
		setPrivateField(entity, "issuerDN", VARCHAR_250B);
		entity.setNextUpdate(0L);
		entity.setRowProtection(CLOB_10KiB);
		entity.setRowVersion(0);
		entity.setThisUpdate(0L);
		storeAndRemoveEntity(entity);
	}

	public void testHardTokenCertificateMap() {
		HardTokenCertificateMap entity = new HardTokenCertificateMap();
		entity.setCertificateFingerprint(VARCHAR_250B);
		entity.setRowProtection(CLOB_10KiB);
		entity.setRowVersion(0);
		entity.setTokenSN(VARCHAR_250B);
		storeAndRemoveEntity(entity);
	}

	public void testHardTokenData() {
		HardTokenData entity = new HardTokenData();
		entity.setCtime(0L);
		entity.setData(HASHMAP_200K);
		entity.setMtime(0L);
		entity.setRowProtection(CLOB_10KiB);
		entity.setRowVersion(0);
		entity.setSignificantIssuerDN(VARCHAR_250B);
		entity.setTokenSN(VARCHAR_250B);
		entity.setTokenType(0);
		entity.setUsername(VARCHAR_250B);
		storeAndRemoveEntity(entity);
	}

	public void testHardTokenIssuerData() {
		HardTokenIssuerData entity = new HardTokenIssuerData();
		entity.setAdminGroupId(0);
		entity.setAlias(VARCHAR_250B);
		entity.setDataUnsafe(HASHMAP_200K);
		entity.setId(BOGUS_INTEGER);
		entity.setRowProtection(CLOB_10KiB);
		entity.setRowVersion(0);
		storeAndRemoveEntity(entity);
	}

	public void testHardTokenProfileData() {
		HardTokenProfileData entity = new HardTokenProfileData();
		entity.setData(CLOB_1MiB);
		entity.setId(BOGUS_INTEGER);
		entity.setName(VARCHAR_250B);
		entity.setRowProtection(CLOB_10KiB);
		entity.setRowVersion(0);
		entity.setUpdateCounter(0);
		storeAndRemoveEntity(entity);
	}

	public void testHardTokenPropertyData() {
		HardTokenPropertyData entity = new HardTokenPropertyData();
		entity.setId(VARCHAR_250B);
		entity.setProperty(VARCHAR_250B);
		entity.setRowProtection(CLOB_10KiB);
		entity.setRowVersion(0);
		entity.setValue(VARCHAR_250B);
		storeAndRemoveEntity(entity);
	}

	public void testKeyRecoveryData() {
		KeyRecoveryData entity = new KeyRecoveryData();
		entity.setCertSN(VARCHAR_250B);
		entity.setIssuerDN(VARCHAR_250B);
		entity.setKeyData(CLOB_1MiB);
		entity.setMarkedAsRecoverable(false);
		entity.setRowProtection(CLOB_10KiB);
		entity.setRowVersion(0);
		entity.setUsername(VARCHAR_250B);
		storeAndRemoveEntity(entity);
	}

	public void testLogConfigurationData() {
		LogConfigurationData entity = new LogConfigurationData();
		entity.setId(BOGUS_INTEGER);
		entity.setLogConfigurationUnsafe(new LogConfiguration(false, false, HASHMAP_200K));
		entity.setLogEntryRowNumber(0);
		entity.setRowProtection(CLOB_10KiB);
		entity.setRowVersion(0);
		storeAndRemoveEntity(entity);
	}

	public void testLogEntryData() {
		LogEntryData entity = new LogEntryData();
		entity.setAdminData(VARCHAR_250B);
		entity.setAdminType(0);
		entity.setCaId(0);
		entity.setCertificateSNR(VARCHAR_250B);
		entity.setEvent(0);
		entity.setId(BOGUS_INTEGER);
		entity.setLogComment(VARCHAR_250B);
		entity.setModule(0);
		entity.setRowProtection(CLOB_10KiB);
		entity.setRowVersion(0);
		entity.setTime(0L);
		entity.setUsername(VARCHAR_250B);
		storeAndRemoveEntity(entity);
	}

	public void testUserData() {
		UserData entity = new UserData();
		entity.setCaId(0);
		entity.setCardNumber(VARCHAR_250B);
		entity.setCertificateProfileId(0);
		entity.setClearPassword(VARCHAR_250B);
		entity.setEndEntityProfileId(0);
		entity.setExtendedInformationData(CLOB_1MiB);
		entity.setHardTokenIssuerId(0);
		entity.setKeyStorePassword(VARCHAR_250B);
		entity.setPasswordHash(VARCHAR_250B);
		entity.setRowProtection(CLOB_10KiB);
		entity.setRowVersion(0);
		entity.setStatus(0);
		entity.setSubjectAltName(VARCHAR_250B);
		entity.setSubjectDN(VARCHAR_250B);
		entity.setSubjectEmail(VARCHAR_250B);
		entity.setTimeCreated(0L);
		entity.setTimeModified(0L);
		entity.setTokenType(0);
		entity.setType(0);
		entity.setUsername(VARCHAR_250B);
		storeAndRemoveEntity(entity);
	}

	public void testAdminPreferencesData() {
		AdminPreferencesData entity = new AdminPreferencesData();
		entity.setDataUnsafe(HASHMAP_200K);
		entity.setId(VARCHAR_250B);
		entity.setRowProtection(CLOB_10KiB);
		entity.setRowVersion(0);
		storeAndRemoveEntity(entity);
	}

	public void testEndEntityProfileData() {
		EndEntityProfileData entity = new EndEntityProfileData();
		entity.setDataUnsafe(HASHMAP_200K);
		entity.setId(BOGUS_INTEGER);
		entity.setProfileName(VARCHAR_250B);
		entity.setRowProtection(CLOB_10KiB);
		entity.setRowVersion(0);
		storeAndRemoveEntity(entity);
	}

	public void testGlobalConfigurationData() {
		GlobalConfigurationData entity = new GlobalConfigurationData();
		entity.setConfigurationId(VARCHAR_250B);
		entity.setDataUnsafe(HASHMAP_200K);
		entity.setRowProtection(CLOB_10KiB);
		entity.setRowVersion(0);
		storeAndRemoveEntity(entity);
	}

	public void testUserDataSourceData() {
		UserDataSourceData entity = new UserDataSourceData();
		entity.setData(CLOB_100KiB);
		entity.setId(BOGUS_INTEGER);
		entity.setName(VARCHAR_250B);
		entity.setRowProtection(CLOB_10KiB);
		entity.setRowVersion(0);
		entity.setUpdateCounter(0);
		storeAndRemoveEntity(entity);
	}

	public void testServiceData() {
		ServiceData entity = new ServiceData();
		entity.setData(CLOB_100KiB);
		entity.setId(BOGUS_INTEGER);
		entity.setName(VARCHAR_250B);
		entity.setNextRunTimeStamp(0L);
		entity.setRowProtection(CLOB_10KiB);
		entity.setRowVersion(0);
		entity.setRunTimeStamp(0L);
		storeAndRemoveEntity(entity);
	}
	
	public void testZZZCleanUp() throws Exception {
		entityManager.close();
		entityManagerFactory.close();
	}
}
