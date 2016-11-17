/*************************************************************************
 *                                                                       *
 *  EJBCA Community: The OpenSource Certificate Authority                *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.ejbca.util;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.log4j.Logger;

/**
 * Helper class for reading the index meta data from the database using direct JDBC.
 * 
 * @version $Id$
 */
public abstract class DatabaseIndexUtil {

    private static final Logger log = Logger.getLogger(DatabaseIndexUtil.class);

    /** Private helper class to help sorting the columns in the right order even if the database would return them in a different order than the ordinal */
    private static class OrdinalColumn implements Comparable<OrdinalColumn> {
        final short ordinalPosition;
        final String columnName;
        
        private OrdinalColumn(final short ordinalPosition, final String columnName) {
            this.ordinalPosition = ordinalPosition;
            this.columnName = columnName;
        }

        @Override
        public int compareTo(final OrdinalColumn other) {
            return this.ordinalPosition-other.ordinalPosition;
        }
    }
    
    /** Database index representation. */
    public static class DatabaseIndex {
        private final String indexName;
        private final List<OrdinalColumn> ordinalColumns = new ArrayList<>();
        private List<String> columnNames = new ArrayList<>();
        private final boolean nonUnique;

        public DatabaseIndex(final String indexName, final boolean nonUnique) {
            this.indexName = indexName;
            this.nonUnique = nonUnique;
        }

        private void appendOrdinalColumn(final OrdinalColumn ordinalColumn) {
            ordinalColumns.add(ordinalColumn);
            Collections.sort(ordinalColumns);
            columnNames.clear();
            for (final OrdinalColumn current : ordinalColumns) {
                columnNames.add(current.columnName);
            }
        }

        /** @return the name of the index as reported by the database */
        public String getIndexName() { return indexName; }

        /** @return the column names in the correct order as reported by the database */
        public List<String> getColumnNames() { return columnNames; }

        /** @return true if the index is reported as unique */
        public boolean isUnique() { return !nonUnique; }

        /** Case insensitive check if all columns present in the argument is also exactly present in this index. */
        public boolean isExactlyOverColumns(final List<String> columnNames) {
            final List<String> indexColumnNames = new ArrayList<>();
            for (final String indexColumnName : getColumnNames()) {
                indexColumnNames.add(indexColumnName.toLowerCase());
            }
            for (final String columnName : columnNames) {
                if (!indexColumnNames.remove(columnName.toLowerCase())) {
                    return false;
                }
            }
            return indexColumnNames.isEmpty();
        }
    }

    /** @return true if there exists an index on the specified table exactly matches the requested columns and optionally is unique */
    public static boolean isIndexPresentOverColumns(final DataSource dataSource, final String tableName, final List<String> columnNames, final boolean requireUnique) throws SQLException {
        final List<DatabaseIndex> databaseIndexes = getDatabaseIndexFromTable(dataSource, tableName, requireUnique);
        for (final DatabaseIndex databaseIndex : databaseIndexes) {
            if (databaseIndex.isExactlyOverColumns(columnNames)) {
                return true;
            }
        }
        return false;
    }

    /** @return a list of representations of each database index present for a table */
    public static List<DatabaseIndex> getDatabaseIndexFromTable(final DataSource dataSource, final String tableName, final boolean requireUnique) throws SQLException {
        final List<DatabaseIndex> ret = new ArrayList<>();
        try (final Connection connection = dataSource.getConnection();) {
            final DatabaseMetaData databaseMetaData = connection.getMetaData();
            final Map<String,DatabaseIndex> tableIndexMap = new HashMap<>();
            // http://docs.oracle.com/javase/7/docs/api/java/sql/DatabaseMetaData.html#getIndexInfo(java.lang.String,%20java.lang.String,%20java.lang.String,%20boolean,%20boolean)
            try (final ResultSet resultSet = databaseMetaData.getIndexInfo(/*catalog=*/null, /*schema=*/null, tableName, /*unique=*/requireUnique, /*approximate=*/false);) {
                while (resultSet.next()) {
                    final String indexName = resultSet.getString("INDEX_NAME");
                    final boolean nonUnique = resultSet.getBoolean("NON_UNIQUE");
                    if (!tableIndexMap.containsKey(indexName)) {
                        tableIndexMap.put(indexName, new DatabaseIndex(indexName, nonUnique));
                    }
                    final DatabaseIndex databaseIndex = tableIndexMap.get(indexName);
                    final String columnName = resultSet.getString("COLUMN_NAME");
                    final short ordinalPosition = resultSet.getShort("ORDINAL_POSITION");
                    databaseIndex.appendOrdinalColumn(new OrdinalColumn(ordinalPosition, columnName));
                    if (log.isDebugEnabled()) {
                        // Extract additional info if we are debug logging
                        final short type = resultSet.getShort("TYPE");
                        final String typeString;
                        switch (type) {
                        case DatabaseMetaData.tableIndexStatistic: typeString = "tableIndexStatistic"; break;
                        case DatabaseMetaData.tableIndexClustered: typeString = "tableIndexClustered"; break;
                        case DatabaseMetaData.tableIndexHashed: typeString = "tableIndexHashed"; break;
                        case DatabaseMetaData.tableIndexOther: typeString = "tableIndexOther"; break;
                        default: typeString = "unknown";
                        }
                        log.debug("Detected part of index on table '" + tableName + "' indexName: " + indexName + " column["+ordinalPosition+"]: " + columnName +
                                " unique: " + !nonUnique + " type: " + typeString + " current columns: " + Arrays.toString(databaseIndex.getColumnNames().toArray()));
                    }
                }
            }
            ret.addAll(tableIndexMap.values());
        }
        return ret;
    }
}
