
cd src\java

rem Check for proper settings of environment variables
if "%JBOSS_HOME%" == ""  goto error

rem JBoss
set TEST_CP=.;%JBOSS_HOME%\client\jnp-client.jar;%JBOSS_HOME%\client\jboss-client.jar;%JBOSS_HOME%\client\jboss-j2ee.jar;%JBOSS_HOME%\client\jbosssx-client.jar;%JBOSS_HOME%\client\jboss-common-client.jar;..\..\lib\junit.jar;..\..\lib\log4j-1.2.jar;..\..\lib\jce-jdk13-116.jar;..\..\lib\bcmail-jdk13-116.jar

rem Weblogic
rem set TEST_CP=.;..\..\lib\weblogic.jar;..\..\lib\junit.jar;..\..\lib\log4j-1.2.jar;..\..\lib\jce-jdk13-116.jar;..\..\lib\bcmail-jdk13-116.jar


echo Testing utils
java -cp %TEST_CP% se.anatom.ejbca.util.junit.TestRunner
echo Testing ra
java -cp %TEST_CP% se.anatom.ejbca.ra.junit.TestRunner
echo Testing ca.auth
java -cp %TEST_CP% se.anatom.ejbca.ca.auth.junit.TestRunner
echo Testing ca.store
java -cp %TEST_CP% se.anatom.ejbca.ca.store.junit.TestRunner
echo Testing ca.sign
java -cp %TEST_CP% se.anatom.ejbca.ca.sign.junit.TestRunner
echo Testing ca.crl
java -cp %TEST_CP% se.anatom.ejbca.ca.crl.junit.TestRunner
echo Testing batch
java -cp %TEST_CP% se.anatom.ejbca.batch.junit.TestRunner

cd ..\..

goto end
:error 
echo JBOSS_HOME must be set
:end
