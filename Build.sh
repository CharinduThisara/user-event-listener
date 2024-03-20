#!/bin/bash

# Function to perform the liveness probe
perform_liveness_probe() {
    url="https://localhost:9443/carbon/admin/login.jsp"
    curl -s -o /dev/null -w "%{http_code}" -k "$url"
}

delete_user(){
    echo "Deleting the user..."
    
    # Read the user ID from user_id.txt
    user_id=$(grep -oP '(?<=User ID: )[0-9a-f-]+' user_id.txt)

    # Check if user_id is empty
    if [ -z "$user_id" ]; then
        echo "Error: User ID not found in user_id.txt"
        return 1
    fi

    # Store the curl response in a variable
    response=$(curl -k -X 'DELETE' \
    "https://localhost:9443/scim2/Users/$user_id" \
    -H 'accept: application/scim+json' \
    -u admin:admin)

    # Remove the line containing the user ID from user_id.txt
    sed -i "/User ID: $user_id/d" user_id.txt
}


create_user(){
    echo "Creating a new user..."

    # Store the curl response in a variable
    response=$(curl -k -X 'POST' \
    'https://localhost:9443/scim2/Users' \
    -H 'accept: application/scim+json' \
    -H 'Content-Type: application/scim+json' \
    -u admin:admin \
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
    echo "User ID: $user_id" >> user_id.txt
}

perform_user_creation() {
    # Define the URL for liveness probe

    echo "Waiting for the server to start..."
    while true; do
        echo "Waiting for the server to start..."
        response_code=$(perform_liveness_probe)
        if [ "$response_code" -eq 200 ]; then
            # Server is live, break the loop
            break
        else
            # Server is not live, wait for a while and try again
            sleep 5  # Adjust the sleep interval as needed
        fi
    done

    echo "Server is live!"

    delete_user
    create_user
}

echo 3 | sudo update-alternatives --config java
JAVA_HOME=`/usr/lib/jvm/java-1.8.0-openjdk-amd64` mvn clean install

rm -rf ../wso2is-7.0.0
unzip ../wso2is-7.0.0 -d ../
cp target/org.wso2.custom.user.operation.event.listener-1.0-SNAPSHOT.jar ../wso2is-7.0.0/repository/components/dropins
cp dropins/* ../wso2is-7.0.0/repository/components/dropins

cp -r lib/* ../wso2is-7.0.0/repository/components/lib
rm -rf ../wso2is-7.0.0/repository/conf/deployment.toml
cp deployment.toml ../wso2is-7.0.0/repository/conf

echo 2 | sudo update-alternatives --config java

perform_user_creation &

../wso2is-7.0.0/bin/wso2server.sh

