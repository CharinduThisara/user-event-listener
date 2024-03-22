create_user(){

    local user_name=$1
    
    echo "Creating a new user with username: $user_name..."
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
    "userName": "'$user_name'",
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

create_user $1

