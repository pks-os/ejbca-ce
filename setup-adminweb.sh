#!/bin/sh
IFS=

# This script sets up the administrative web interface with client cert authentication.
# Usage: setup-adminweb <DN Tomcat Server Cert> <Tomcat keystore passwd> <java cacert keystore passwd>

if [ -f $1 ]
then
    echo "Usage: setup-adminweb <DN Tomcat Server Cert> <Tomcat keystore passwd> <SuperAdmin password> <java cacert keystore passwd>"
    exit
fi
if [ -f $2 ]
then
    echo "Usage: setup-adminweb <DN Tomcat Server Cert> <Tomcat keystore passwd> <SuperAdmin password> <java cacert keystore passwd>"
    exit
fi
if [ -f $3 ]
then
    echo "Usage: setup-adminweb <DN Tomcat Server Cert> <Tomcat keystore passwd> <SuperAdmin password> <java cacert keystore passwd>"
    exit
fi
if [ -f $4 ]
then
    echo "Usage: setup-adminweb <DN Tomcat Server Cert> <Tomcat keystore passwd> <SuperAdmin password> <java cacert keystore passwd>"
    exit
fi

./ra.sh adduser tomcat $2 \"$1\" null null 1 3

./ra.sh adduser superadmin $3 "CN=SuperAdmin" null null 65 2

./ra.sh setclearpwd tomcat $2

./ra.sh setclearpwd superadmin $3

./batch.sh

cp p12/tomcat.jks $JBOSS_HOME/.keystore

./ca.sh getrootcert tmp/rootca.der

#This command must be run as root
echo
echo Importing certs in the JAVA trust store requires root privileges
echo
su -c "$JAVA_HOME/bin/keytool -alias EJBCA-CA -import -trustcacerts -file tmp/rootca.der -keystore $JAVA_HOME/jre/lib/security/cacerts -storepass $4"

rm tmp/rootca.der

export CP=.:./admin.jar:./lib/regexp1_0_0.jar

if [ -f $JBOSS_HOME/server/default/deploy/tomcat4-service.xml ]
then
	TOMCAT_XML=tomcat4-service.xml
else
	TOMCAT_XML=tomcat41-service.xml
fi
java -cp $CP se.anatom.ejbca.util.TomcatServiceXMLPasswordReplace src/adminweb/WEB-INF/$TOMCAT_XML tmp/$TOMCAT_XML $2

cp tmp/$TOMCAT_XML $JBOSS_HOME/server/default/deploy/$TOMCAT_XML

rm tmp/$TOMCAT_XML
