package org.wso2.custom.user.operation.event.listener;

import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.common.*;
    
import com.datastax.oss.driver.api.core.CqlSession;

import com.datastax.oss.driver.api.core.CqlSessionBuilder;

import java.net.InetSocketAddress;

import java.util.Map;


/**
 *
 */
public class CustomUserOperationEventListener extends AbstractUserOperationEventListener {

    private CqlSession session;

    private String systemUserPrefix = "system_";

    private static final String NODE_IP = "127.0.0.1";
    private static final int PORT = 9042;
    private static final String KEYSPACE = "my_keyspace";
    private static final String LOCAL_DATACENTER = "datacenter1";


    private static final String INSERT_USER_QUERY = "INSERT INTO users (user_id, username, credential, role_list, claims, profile) VALUES (?, ?, ?, ?, ?, ?)";

    String createKeyspaceQuery = "CREATE KEYSPACE IF NOT EXISTS my_keyspace "
            + "WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};";

    String createTableQuery = "CREATE TABLE IF NOT EXISTS my_keyspace.users ("
            + "user_id UUID PRIMARY KEY, "
            + "username TEXT, "
            + "credential BLOB, "
            + "role_list SET<TEXT>, "
            + "claims MAP<TEXT, TEXT>, "
            + "profile TEXT)";

    public CustomUserOperationEventListener() {
        super();

        // initializeCassandra();
    }

    public void initializeCassandra(){
        
        connect(NODE_IP, PORT, LOCAL_DATACENTER);
        System.out.println("Connected to Cassandra");

        session.execute(createKeyspaceQuery);
        System.out.println("Keyspace created");
      
        session.execute(createTableQuery);
        System.out.println("Table created");
        
    }

    public void connect(String node, Integer port, String dataCenter) {
        CqlSessionBuilder builder = CqlSession.builder();
        builder.addContactPoint(new InetSocketAddress(node, port));
        builder.withLocalDatacenter(dataCenter);

        this.session = builder.build();

        System.out.println("Connected to Cassandra");
    }



    @Override
    public boolean doPostAddUser(String userName, Object credential, String[] roleList, Map<String, String> claims, String profile, UserStoreManager userStoreManager) throws UserStoreException {

        System.out.println("User added successfully");
        System.out.printf("User Name: %s\n", userName);
        System.out.printf("User Store Manager: %s\n", userStoreManager);
        System.out.println("User Claims:");
        printMap(claims);
        System.out.println("User Roles:");
        printArray(roleList);
        System.out.printf("User Profile: %s\n", profile);
        
        connect(NODE_IP, PORT,LOCAL_DATACENTER);
        // try (CqlSession session = CqlSession.builder()
        // .addContactPoint(new InetSocketAddress(NODE_IP, PORT))
        // .withKeyspace(KEYSPACE)
        // .build()) {
        //     // Generate a unique UUID for the user
        //     java.util.UUID userId = Uuids.timeBased();

        //     // Convert roleList array to a set
        //     HashSet<String> roleSet = arrayToSet(roleList);

        //     // Prepare the insert statement
        //     PreparedStatement preparedStatement = session.prepare(INSERT_USER_QUERY);

        //     // Execute the insert statement
        //     session.execute(preparedStatement.bind(
        //             userId,                // user_id
        //             userName,             // username
        //             credential.toString(),// credential
        //             roleSet,              // role_list
        //             claims,               // claims
        //             profile));            // profile

        //     // Print success message
        //     System.out.println("User added successfully to Cassandra database");
        // } catch (Exception e) {
        //     e.printStackTrace();
        //     return false;
        // }

        return true;
    }
    

    @Override
    public boolean doPreDeleteUserWithID
            (String s, UserStoreManager userStoreManager) throws UserStoreException {

        if (s.contains(systemUserPrefix)) {
            return false;
        } else {
            return true;
        }
    }


    @Override
    public int getExecutionOrderId() {
        return 9000;
    }

    @Override
    public boolean doPreDeleteUser
            (String s, UserStoreManager userStoreManager) throws UserStoreException {

                System.out.println("User deleted successfully");
                System.out.println("User Name: " + s);
                System.out.println("User Store Manager: " + userStoreManager);
                // connect(NODE_IP, PORT,LOCAL_DATACENTER);

                System.out.println("Connecting to Cassandra...");
                // Establishing connection to Cassandra
                // try (CqlSession session = new CqlSessionBuilder()
                //         .addContactPoint(new InetSocketAddress(NODE_IP, PORT))
                //         .withLocalDatacenter("datacenter1") // Adjust to your local datacenter name
                //         .build()) {
                //     System.out.println("Connected to Cassandra.");
                //     // Writing data to the user_data table
                   
                // } catch (Exception e) {
                //     System.err.println("Error: " + e);
                // }

                this.session = new CqlSessionBuilder()
                        .addContactPoint(new InetSocketAddress(NODE_IP, PORT))
                        .withLocalDatacenter("datacenter1") // Adjust to your local datacenter name
                        .build();
                System.out.println("Connected to Cassandra.");
                    
    
                
        return true;
    }

    private void printMap(Map<String, String> map) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            System.out.printf("%s: %s\n", entry.getKey(), entry.getValue());
        }
    }
    
    private void printArray(String[] array) {
        for (String item : array) {
            System.out.println(item);
        }
    }

}
