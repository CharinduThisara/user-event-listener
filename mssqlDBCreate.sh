sqlcmd -S localhost -U sa -P 'nimdager@123' -C -Q "DROP DATABASE is_db_um"         
sqlcmd -S localhost -U sa -P 'nimdager@123' -C -Q "CREATE DATABASE is_db_um"         

sqlcmd -S localhost -U sa -P 'nimdager@123' -C -i ~/IS/Code_Bases/custom-user-operation-event-listener/DBSCRIPTS/mssql.sql -d is_db_um 
sqlcmd -S localhost -U sa -P 'nimdager@123' -C -i ~/IS/Code_Bases/custom-user-operation-event-listener/DBSCRIPTS/mssql-consent.sql -d is_db_um 
sqlcmd -S localhost -U sa -P 'nimdager@123' -C -i ~/IS/Code_Bases/custom-user-operation-event-listener/DBSCRIPTS/mssql-identity.sql -d is_db_um 


../wso2is-7.0.0/bin/wso2server.sh -DenableCorrelationLogs=true

# select name from sys.databases;

# SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE';

# INSERT INTO UM_USER (UM_USER_ID, UM_USER_NAME, UM_USER_PASSWORD, UM_SALT_VALUE, UM_REQUIRE_CHANGE, UM_CHANGED_TIME, UM_TENANT_ID) VALUES ('admin', 'Admin User', 'adminpassword', 'adminsalt', 0, '2024-03-08 12:00:00', 1);
