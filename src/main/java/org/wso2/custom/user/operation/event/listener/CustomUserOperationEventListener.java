package org.wso2.custom.user.operation.event.listener;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.common.*;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;


import java.net.InetSocketAddress;

import java.security.KeyStore;

import javax.net.ssl.SSLContext;

import io.github.cdimascio.dotenv.Dotenv;

import java.io.File;
import java.util.Arrays;

/**
 *
 */
public class CustomUserOperationEventListener extends AbstractUserOperationEventListener {

    private static final String DEFAULT_KEYSPACE = "my_keyspace";
    private static final String DEFAULT_DATA_CENTER = "datacenter1";
    private static final String DEFAULT_TABLE = "my_table";
    private static final String DEFAULT_NODE = "127.0.0.1";
    private static final int DEFAULT_PORT = 9042;
    
    private static String COSMOS_CONFIG_PATH;
    private String systemUserPrefix = "system_";
    private CqlSession session;
    private String cassandraKeyspace;
    private String cassandraTable;   
    
        
    public CustomUserOperationEventListener() {
        super();

        initializeCassandra();
    }

    public void initializeCassandra(){
        
        connectCosmos();
        System.out.println("Connected to Cosmos");

        createKeySpace();
        System.out.println("Keyspace Created");

        createTable();
        System.out.println("Table Created");
    }

    public void connectToLocalCassandra() {
        
        File file = new File(COSMOS_CONFIG_PATH);

        DriverConfigLoader loader = DriverConfigLoader.fromFile(file);
        CqlSessionBuilder builder = CqlSession.builder();
        builder.addContactPoint(new InetSocketAddress(DEFAULT_NODE, DEFAULT_PORT));
        builder.withLocalDatacenter(DEFAULT_DATA_CENTER);
        builder.withConfigLoader(loader);
        
        this.session = builder.build();

        System.out.println("Session Created : " + session.getName());
    }
    
    public void connectCosmos() {

        SSLContext sc = null;
        try{

            final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(null, null);

            final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null);

            sc = SSLContext.getInstance("TLSv1.2");
            sc.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            Dotenv dotenv = Dotenv.configure().load();

            COSMOS_CONFIG_PATH = dotenv.get("COSMOS_CONFIG_PATH");
            String cassandraHost = dotenv.get("COSMOS_CONTACT_POINT");
            int cassandraPort = Integer.parseInt(dotenv.get("COSMOS_PORT"));
            String region = dotenv.get("COSMOS_REGION");
            String cassandraUsername = dotenv.get("COSMOS_USER_NAME");
            String cassandraPassword = dotenv.get("COSMOS_PASSWORD");
            cassandraKeyspace = dotenv.get("COSMOS_KEYSPACE");
            cassandraTable = dotenv.get("COSMOS_TABLE");
        
            DriverConfigLoader loader = DriverConfigLoader.fromFile(new File(COSMOS_CONFIG_PATH));

            System.out.println("Connecting to Cosmos "+cassandraHost+":"+cassandraPort+" with keyspace: "+cassandraKeyspace+" and table: "+cassandraTable);

            this.session = CqlSession.builder().withSslContext(sc)
            .addContactPoint(new InetSocketAddress(cassandraHost, cassandraPort)).withLocalDatacenter(region)
            .withConfigLoader(loader)   
            .withAuthCredentials(cassandraUsername, cassandraPassword).build();
            
        }
        catch (Exception e) {
            System.out.println("Error creating session");
            e.printStackTrace();
        }

    }

    private void close() {
        this.session.close();
    }

    @Override
    public int getExecutionOrderId() {
        return 9000;
    }

    private void createTable(){
        String createTableQuery = "CREATE TABLE IF NOT EXISTS " + cassandraKeyspace + "." + cassandraTable + " ("
                                + "user_id TEXT PRIMARY KEY, "
                                + "username TEXT, "
                                + "credential TEXT, "
                                + "role_list SET<TEXT>, "
                                + "claims MAP<TEXT, TEXT>,"
                                + "profile TEXT, "
                                + "central_us BOOLEAN, "
                                + "east_us BOOLEAN)";


        session.execute(createTableQuery);
  
    }

    private void createKeySpace(){
        String createKeyspaceQuery = "CREATE KEYSPACE IF NOT EXISTS "+cassandraKeyspace+ " "  
                                         + "WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};" ;
    
        session.execute(createKeyspaceQuery);
           
    }

    @Override
    public boolean doPreDeleteUser
            (String s, UserStoreManager userStoreManager) throws UserStoreException {

        System.out.println("User deleted successfully");
        System.out.println("User Name: " + s);
        System.out.println("User Store Manager: " + userStoreManager);

        return true;
    }

    @Override
    public boolean doPreDeleteUserWithID
            (String s, UserStoreManager userStoreManager) throws UserStoreException {
            System.out.println("doPreDeleteUserWithID");
        if (s.contains(systemUserPrefix)) {
            return false;
        } else {
            return true;
        }
    }

    
    @Override
    public boolean doPostAddUser(String userName, Object credential, String[] roleList, Map<String, String> claims, String profile, UserStoreManager userStoreManager) throws UserStoreException {

        System.out.println("User added successfully");
        System.out.printf("User Name: %s\n", userName);
        System.out.printf("User Credential: %s\n", credential);
        System.out.printf("User Store Manager: %s\n", userStoreManager);
        System.out.println("User Claims:");
        printMap(claims);
        System.out.println("User Roles:");
        printArray(roleList);
        System.out.printf("User Profile: %s\n", profile);

        final String INSERT_USER_QUERY = "INSERT INTO "+ cassandraKeyspace +"."+cassandraTable+" (user_id, username, credential, role_list, claims, profile, central_us, east_us) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        
        try{

            // Prepare the insert statement
            PreparedStatement preparedStatement = session.prepare(INSERT_USER_QUERY);

            String userId = claims.get("http://wso2.org/claims/userid");
            Set<String> roleSet = new HashSet<>(Arrays.asList(roleList));
            
            // Execute the insert statement
            session.execute(preparedStatement.bind(
                    userId,                // user_id
                    userName,             // username
                    credential.toString(),// credential
                    roleSet,              // role_list
                    claims,               // claims
                    profile,
                    false,
                    true
                    ));            // profile

        }
        catch(Exception e){
            System.out.println("Error: " + e);
            e.printStackTrace();
        }
   
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

