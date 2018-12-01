package com.oreilley.playthis1.databaseModel;

import javax.xml.transform.Result;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;


public abstract class DbModel {

    public static final String JDBC_DRIVER_CLASSPATH = "org.sqlite.JDBC";
    private static final String DEFAULT_DATABASE = "ptdev";
    private static Connection conn = null;

    /**
     * Classification of database to Java out of sync errors
     */
    public static final String SYNC_ERROR_NOT_FOUND_DB = "MySQL database is missing column(s): ";
    public static final String SYNC_ERROR_NOT_FOUND_JAVA = "Java source code is missing attribute(s): ";
    public static final String SYNC_ERROR_MISMATCH_DATA_TYPE = "Column data types are out of sync: ";

    /**
     * Names of general attributes common to all db objects referred to as "Audit" attributes. They are useful for investigations and event tracking.
     */
    public static final String ATTR_ID = "id";
    public static final String ATTR_CREATED_AT = "created_at";
    public static final String ATTR_UPDATED_AT = "updated_at";
    public static final String ATTR_DELETED_AT = "deleted_at";
    public static final String ATTR_ACTIVATED_AT = "activated_at";

    public Date createdAt, updatedAt, deletedAt, activatedAt;


    public static final ArrayList<String> ATTR_NAMES_AUDIT = new ArrayList<String>(5) {{
        add(ATTR_ID);
        add(ATTR_CREATED_AT);
        add(ATTR_UPDATED_AT);
        add(ATTR_ACTIVATED_AT);
        add(ATTR_DELETED_AT);
    }};

    public static Connection createConnection(String database){
        try {
            System.out.println(System.getProperty("user.dir"));
            String url = "jdbc:sqlite://" + System.getProperty("user.dir") + "/" + database;
            System.out.println("db: " + url);
            Class.forName(JDBC_DRIVER_CLASSPATH);

            return DriverManager.getConnection(url);

        } catch (ClassNotFoundException e) {
            System.out.println("createConnection error" + e);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Connection getConnection (){
        try{
            if (conn == null || conn.isClosed()){
                System.out.println("Creating new connection");
                conn = createConnection("playthis1.db");
            }
        }catch (Exception err){
            System.err.println("[getConnection] error creating connection: " + err);
        }
        return conn;
    }

    public HashMap<String, String> getValidation() {
        return new HashMap<>();
    }


    public abstract String getModelNamePlural();


    public String getTableName() {
        return this.getModelNamePlural().toLowerCase();
    }

    public abstract HashMap<String, AttributeType> getAttributes();

    public static void main (String [] args) throws SQLException {
        Connection c = createConnection("playthis1.db");
        System.out.println("Connection created " + c);

        ResultSet rs = c.createStatement().executeQuery("SELECT * FROM users");

        while(rs.next()) {
            // read the result set
            System.out.println("TEXT = " + rs.getString("name"));
        }

    }
}

