#!/bin/bash

# Function to perform the liveness probe
perform_liveness_probe() {
    curl -s -o /dev/null -w "%{http_code}" "$url"
}

echo 3 | sudo update-alternatives --config java
JAVA_HOME=`/usr/lib/jvm/java-1.8.0-openjdk-amd64` mvn clean install

rm -rf ../wso2is-7.0.0
unzip ../wso2is-7.0.0 -d ../
cp target/org.wso2.custom.user.operation.event.listener-1.0-SNAPSHOT.jar ../wso2is-7.0.0/repository/components/dropins
cp lib ../wso2is-7.0.0/repository/components/dropins
rm -rf ../wso2is-7.0.0/repository/conf/deployment.toml
cp deployment.toml ../wso2is-7.0.0/repository/conf

echo 2 | sudo update-alternatives --config java

../wso2is-7.0.0/bin/wso2server.sh 

# Define the URL for liveness probe
url="https://localhost:9443/carbon/admin/login.jsp"

while true; do
    response_code=$(perform_liveness_probe)
    if [ "$response_code" -eq 200 ]; then
        # Server is live, break the loop
        break
    else
        # Server is not live, wait for a while and try again
        sleep 5  # Adjust the sleep interval as needed
    fi
done

# Store the curl response in a variable
response=$(curl -X 'POST' \
'https://localhost:9443/scim2/Users' \
-H 'accept: application/scim+json' \
-H 'Content-Type: application/scim+json' \
-d '{
"schemas": [],
"name": {
  "givenName": "Kim",
  "familyName": "Berry"
},
"userName": "kim",
"password": "MyPa33w@rd",
"emails": [
  {
    "type": "home",
    "value": "kim@gmail.com",
    "primary": true
  },
  {
    "type": "work",
    "value": "kim@wso2.com"
  }
],
"urn:ietf:params:scim:schemas:extension:enterprise:2.0:User": {
  "employeeNumber": "1234A",
  "manager": {
    "value": "Taylor"
  }
}
}')

# Extract the "id" using jq
user_id=$(echo "$response" | jq -r '.id')

# Print the user ID
echo "User ID: $user_id"
