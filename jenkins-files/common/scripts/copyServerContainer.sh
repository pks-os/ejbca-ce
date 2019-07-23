#!/bin/sh

# copyDatabaseContainer.sh SRC_PATH TARGET_PATH DB_CONTAINER JDK DB_FAMILY DB_VERSION SERVER_FAMILY SERVER_VERSION
#                          [1]      [2]         [3]          [4] [5]       [6]        [7]           [8]

DOCKERFILE_PATH=""
DOCKERFILE_RUN_PATH=""
DOCKERFILE_STANDALONE_FILTERED=false
DOCKERFILE_STANDALONE1_PATH=""
DOCKERFILE_STANDALONE2_PATH=""
DOCKERFILE_STANDALONE_DATASOURCE_JNDI_NAME="java:/EjbcaDS"
DOCKERFILE_STANDALONE_DATASOURCE_CONNECTION_URL=""
DOCKERFILE_STANDALONE_DATASOURCE_DRIVER=""
DOCKERFILE_STANDALONE_DATASOURCE_DRIVER_CLASS=""
DOCKERFILE_STANDALONE_DATASOURCE_USERNAME=""
DOCKERFILE_STANDALONE_DATASOURCE_PASSWORD=""
DOCKERFILE_STANDALONE_DATASOURCE_VALID_CONNECTION=""
DOCKERFILE_STANDALONE_DRIVER=""

########################################################################################################################
# Setup variables
########################################################################################################################
echo "Looking for application server container..."
if [ $7 = "wildfly" ]
then
    if [ -f "$1/$7/$8/Dockerfile" ]
    then
        echo "Found WildFly container with version $8"
        DOCKERFILE_PATH="$1/$7/$8/Dockerfile"
        DOCKERFILE_RUN_PATH="$1/$7/$8/run.sh"
        if [ -f "$1/$7/$8/standalone1.xml" ]
        then
            DOCKERFILE_STANDALONE_FILTERED=true
            DOCKERFILE_STANDALONE1_PATH="$1/$7/$8/standalone1.xml"
            DOCKERFILE_STANDALONE2_PATH="$1/$7/$8/standalone2.xml"
        else
            DOCKERFILE_STANDALONE_FILTERED=false
            DOCKERFILE_STANDALONE1_PATH=""
            DOCKERFILE_STANDALONE2_PATH=""
        fi
    else
        echo "Error: Cannot find the WildFly container with version $6"
        exit 1
    fi
else
  echo "Error: Cannot map the application server family"
  exit 1
fi

if [ $DOCKERFILE_STANDALONE_FILTERED = true ]
then
    echo "Configuring database in standalone.xml files..."
    if [ $5 = "db2" ]
    then
        echo "Using DB2 pattern..."
        DOCKERFILE_STANDALONE_DATASOURCE_CONNECTION_URL="jdbc:db2://$3:50000/ejbca"
        DOCKERFILE_STANDALONE_DATASOURCE_DRIVER="db2"
        DOCKERFILE_STANDALONE_DATASOURCE_DRIVER_CLASS=""
        DOCKERFILE_STANDALONE_DATASOURCE_USERNAME="db2inst1"
        DOCKERFILE_STANDALONE_DATASOURCE_PASSWORD="db2inst1"
        DOCKERFILE_STANDALONE_DATASOURCE_VALID_CONNECTION="<check-valid-connection-sql>select 1 from sysibm.sysdummy1</check-valid-connection-sql>"
        DOCKERFILE_STANDALONE_DRIVER="<driver name=\"db2\" module=\"com.ibm.db2\"><xa-datasource-class>com.ibm.db2.jcc.DB2XADataSource</xa-datasource-class></driver>"
    elif [ $5 = "mariadb" ]
    then
        echo "Using MariaDB pattern..."
        DOCKERFILE_STANDALONE_DATASOURCE_CONNECTION_URL="jdbc:mysql://$3:3306/ejbca"
        DOCKERFILE_STANDALONE_DATASOURCE_DRIVER="dbdriver.jar"
        DOCKERFILE_STANDALONE_DATASOURCE_DRIVER_CLASS="<driver-class>org.mariadb.jdbc.Driver</driver-class>"
        DOCKERFILE_STANDALONE_DATASOURCE_USERNAME="ejbca"
        DOCKERFILE_STANDALONE_DATASOURCE_PASSWORD="ejbca"
        DOCKERFILE_STANDALONE_DATASOURCE_VALID_CONNECTION="<check-valid-connection-sql>select 1;</check-valid-connection-sql>"
        DOCKERFILE_STANDALONE_DRIVER=""
    elif [ $5 = "mssql" ]
    then
        echo "Using MS SQL pattern..."
        DOCKERFILE_STANDALONE_DATASOURCE_CONNECTION_URL="jdbc:sqlserver://$3:1433;databaseName=ejbca"
        DOCKERFILE_STANDALONE_DATASOURCE_DRIVER="mssql"
        DOCKERFILE_STANDALONE_DATASOURCE_DRIVER_CLASS=""
        DOCKERFILE_STANDALONE_DATASOURCE_USERNAME="sa"
        DOCKERFILE_STANDALONE_DATASOURCE_PASSWORD="MyEjbcaPass1100"
        DOCKERFILE_STANDALONE_DATASOURCE_VALID_CONNECTION="<valid-connection-checker class-name=\"org.jboss.jca.adapters.jdbc.extensions.mssql.MSSQLValidConnectionChecker\"/>"
        DOCKERFILE_STANDALONE_DRIVER="<driver name=\"mssql\" module=\"com.microsoft.mssql\"><driver-class>com.microsoft.sqlserver.jdbc.SQLServerDriver</driver-class></driver>"
    elif [ $5 = "oracle" ]
    then
        echo "Using Oracle DB pattern..."
        echo "Error: Not implemented"
        exit 1
    else
      echo "Error: Cannot map the database family"
      exit 1
    fi
fi

########################################################################################################################
# Copy resources
########################################################################################################################
cp $DOCKERFILE_PATH $2/
cp $DOCKERFILE_RUN_PATH $2/
if [ $DOCKERFILE_STANDALONE_FILTERED = true ]
then
    # standalone1.xml
    sed -e "s#DOCKERFILE_STANDALONE_DATASOURCE_JNDI_NAME#$DOCKERFILE_STANDALONE_DATASOURCE_JNDI_NAME#" \
        -e "s#DOCKERFILE_STANDALONE_DATASOURCE_CONNECTION_URL#$DOCKERFILE_STANDALONE_DATASOURCE_CONNECTION_URL#" \
        -e "s#DOCKERFILE_STANDALONE_DATASOURCE_DRIVER#$DOCKERFILE_STANDALONE_DATASOURCE_DRIVER#" \
        -e "s#DOCKERFILE_STANDALONE_DATASOURCE_DRV_CLASS#$DOCKERFILE_STANDALONE_DATASOURCE_DRIVER_CLASS#" \
        -e "s#DOCKERFILE_STANDALONE_DATASOURCE_USERNAME#$DOCKERFILE_STANDALONE_DATASOURCE_USERNAME#" \
        -e "s#DOCKERFILE_STANDALONE_DATASOURCE_PASSWORD#$DOCKERFILE_STANDALONE_DATASOURCE_PASSWORD#" \
        -e "s#DOCKERFILE_STANDALONE_DATASOURCE_VALID_CONNECTION#$DOCKERFILE_STANDALONE_DATASOURCE_VALID_CONNECTION#" \
        -e "s#DOCKERFILE_STANDALONE_DRIVER#$DOCKERFILE_STANDALONE_DRIVER#" \
        $DOCKERFILE_STANDALONE1_PATH > $2/standalone1.xml
    # standalone2.xml
    sed -e "s#DOCKERFILE_STANDALONE_DATASOURCE_JNDI_NAME#$DOCKERFILE_STANDALONE_DATASOURCE_JNDI_NAME#" \
        -e "s#DOCKERFILE_STANDALONE_DATASOURCE_CONNECTION_URL#$DOCKERFILE_STANDALONE_DATASOURCE_CONNECTION_URL#" \
        -e "s#DOCKERFILE_STANDALONE_DATASOURCE_DRIVER#$DOCKERFILE_STANDALONE_DATASOURCE_DRIVER#" \
        -e "s#DOCKERFILE_STANDALONE_DATASOURCE_DRV_CLASS#$DOCKERFILE_STANDALONE_DATASOURCE_DRIVER_CLASS#" \
        -e "s#DOCKERFILE_STANDALONE_DATASOURCE_USERNAME#$DOCKERFILE_STANDALONE_DATASOURCE_USERNAME#" \
        -e "s#DOCKERFILE_STANDALONE_DATASOURCE_PASSWORD#$DOCKERFILE_STANDALONE_DATASOURCE_PASSWORD#" \
        -e "s#DOCKERFILE_STANDALONE_DATASOURCE_VALID_CONNECTION#$DOCKERFILE_STANDALONE_DATASOURCE_VALID_CONNECTION#" \
        -e "s#DOCKERFILE_STANDALONE_DRIVER#$DOCKERFILE_STANDALONE_DRIVER#" \
        $DOCKERFILE_STANDALONE2_PATH > $2/standalone2.xml
fi
