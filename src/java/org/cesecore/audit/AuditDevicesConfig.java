/*************************************************************************
 *                                                                       *
 *  CESeCore: CE Security Core                                           *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.cesecore.audit;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.cesecore.audit.audit.AuditExporter;
import org.cesecore.audit.impl.AuditExporterDummy;
import org.cesecore.config.ConfigurationHolder;
import org.cesecore.util.ValidityDate;

/**
 * Parses configuration related to the log devices.
 * 
 * Custom properties for each device is reformatted. E.g. "securityeventsaudit.deviceproperty.1.key1.key2=value" is available to the log device
 * implementation 1 as "key1.key2=value"
 * 
 * Based on cesecore: AuditDevicesConfig.java 907 2011-06-22 14:42:15Z johane
 * 
 * @version $Id$
 */
public class AuditDevicesConfig {

    private static final Logger log = Logger.getLogger(AuditDevicesConfig.class);
    private static final ReentrantLock lock = new ReentrantLock(false);
    private static Map<String, AuditLogDevice> loggers = null;
    private static final Map<String, Class<? extends AuditExporter>> exporters = new HashMap<String, Class<? extends AuditExporter>>();
    private static final Map<String, Properties> deviceProperties = new HashMap<String, Properties>();

    private static Map<String, AuditLogDevice> getLoggers() {
        setup();
        return loggers;
    }

    @SuppressWarnings("unchecked")
    private static void setup() {
        try {
            lock.lock();
            if (loggers == null) {
                loggers = new HashMap<String, AuditLogDevice>();
                final Configuration conf = ConfigurationHolder.instance();
                final String DEVICE_CLASS = "securityeventsaudit.implementation.";
                final String EXPORTER_CLASS = "securityeventsaudit.exporter.";
                // Extract custom properties configured for any device, to avoid lookup for each device later on..
                final String DEVICE_PROPERTY = "securityeventsaudit\\.deviceproperty\\.(\\d+)\\.(.+)";
                final Map<Integer, Properties> allDeviceProperties = new HashMap<Integer, Properties>();
                final Iterator<String> iterator = conf.getKeys();
                while (iterator.hasNext()) {
                    final String currentKey = iterator.next();
                    Pattern pattern = Pattern.compile(DEVICE_PROPERTY);
                    Matcher matcher = pattern.matcher(currentKey);
                    if (matcher.matches()) {
                        final Integer deviceConfId = Integer.parseInt(matcher.group(1));
                        Properties deviceProperties = allDeviceProperties.get(deviceConfId);
                        if (deviceProperties == null) {
                            deviceProperties = new Properties();
                        }
                        final String devicePropertyName = matcher.group(2);
                        final String devicePropertyValue = conf.getString(currentKey);
                        if (log.isDebugEnabled()) {
                            log.debug("deviceConfId=" + deviceConfId.toString() + " " + devicePropertyName + "=" + devicePropertyValue);
                        }
                        deviceProperties.put(devicePropertyName, devicePropertyValue);
                        allDeviceProperties.put(deviceConfId, deviceProperties);
                    }
                }
                for (int i = 0; i < 255; i++) {
                    final String deviceClass = conf.getString(DEVICE_CLASS + i);
                    if (deviceClass != null) {
                        if (log.isDebugEnabled()) {
                            log.debug("Trying to register audit device using implementation: " + deviceClass);
                        }
                        try {
                            // Instantiate device
                            final Class<AuditLogDevice> implClass = (Class<AuditLogDevice>) Class.forName(deviceClass);
                            final AuditLogDevice auditLogDevice = implClass.newInstance();
                            final String name = implClass.getSimpleName();
                            loggers.put(name, auditLogDevice);
                            log.info("Registered audit device using implementation: " + deviceClass);
                            // Store custom properties for this device, so they are searchable by name
                            if (!allDeviceProperties.containsKey(Integer.valueOf(i))) {
                                allDeviceProperties.put(Integer.valueOf(i), new Properties());
                            }
                            deviceProperties.put(name, allDeviceProperties.get(Integer.valueOf(i)));
                            // Setup an exporter for this device
                            final String exporterClassName = conf.getString(EXPORTER_CLASS + i);
                            Class<? extends AuditExporter> exporterClass = AuditExporterDummy.class;
                            if (exporterClassName != null) {
                                try {
                                    exporterClass = (Class<? extends AuditExporter>) Class.forName(exporterClassName);
                                } catch (Exception e) {
                                    // ClassCastException, ClassNotFoundException
                                    log.error("Could not configure exporter for audit device " + name + " using implementation: " + exporterClass, e);
                                }
                            }
                            log.info("Configured exporter " + exporterClass.getSimpleName() + " for device " + name);
                            exporters.put(name, exporterClass);
                        } catch (Exception e) {
                            // ClassCastException, ClassNotFoundException, InstantiationException, IllegalAccessException
                            log.error("Could not creating audit device using implementation: " + deviceClass, e);
                        }
                    }
                }
                if (loggers.isEmpty()) {
                    log.warn("No security event audit devices has been configured.");
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /** @return the ids of all devices that support querying as a serilizable (Hash)Set. */
    public static Set<String> getQuerySupportingDeviceIds() {
        final Set<String> set = new HashSet<String>();
        for (final String id : getLoggers().keySet()) {
            if (loggers.get(id).isSupportingQueries()) {
                set.add(id);
            }
        }
        return set;
    }

    /** @return the ids of all devices as a serilizable (Hash)Set. */
    public static Set<String> getAllDeviceIds() {
        return new HashSet<String>(getLoggers().keySet());
    }

    public static AuditLogDevice getDevice(final Map<Class<?>, ?> ejbs, final String id) {
        final AuditLogDevice auditLogDevice = getLoggers().get(id);
        if (auditLogDevice != null) {
            auditLogDevice.setEjbs(ejbs);
        }
        return auditLogDevice;
    }

    public static Class<? extends AuditExporter> getExporter(final String id) {
        setup();
        return exporters.get(id);
    }

    public static Properties getProperties(final String id) {
        setup();
        return deviceProperties.get(id);
    }

    /** @return the file name of the current export. */
    public static File getExportFile(final Properties properties, final Date exportDate) throws IOException {
        final File dir = new File(properties.getProperty("export.dir", System.getProperty("java.io.tmpdir")));
        return new File(dir, "cesecore-" + ValidityDate.formatAsISO8601(exportDate, ValidityDate.TIMEZONE_UTC) + ".log");
    }

    /** Parameter to specify the number of logs to be fetched in each validation round trip. */
    public static int getAuditLogValidationFetchSize(final Properties properties) {
        return getInt(properties, "validate.fetchsize", 1000);
    }

    /** Parameter to specify the number of logs to be fetched in each export round trip. */
    public static int getAuditLogExportFetchSize(final Properties properties) {
        return getInt(properties, "export.fetchsize", 1000);
    }

    private static int getInt(final Properties properties, final String key, final int defaultValue) {
        int ret = defaultValue;
        try {
            ret = Integer.valueOf(properties.getProperty(key, Integer.valueOf(ret).toString()));
        } catch (NumberFormatException e) {
            log.error("Invalid value in " + key + ", must be decimal number. Using default " + defaultValue + ". Message: " + e.getMessage());
        }
        return ret;
    }
}
