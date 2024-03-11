echo 3 | sudo update-alternatives --config java
JAVA_HOME=`/usr/lib/jvm/java-1.8.0-openjdk-amd64` mvn clean install

rm -rf ../wso2is-7.0.0/repository/components/dropins/org.wso2.custom.user.operation.event.listener-1.0-SNAPSHOT.jar
cp target/org.wso2.custom.user.operation.event.listener-1.0-SNAPSHOT.jar ../wso2is-7.0.0/repository/components/dropins

echo 2 | sudo update-alternatives --config java

../wso2is-7.0.0/bin/wso2server.sh 
```