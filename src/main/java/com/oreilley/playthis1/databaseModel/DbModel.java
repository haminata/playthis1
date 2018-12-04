package com.oreilley.playthis1.databaseModel;

import com.mysql.cj.xdevapi.*;
import com.oreilley.playthis1.usefulResources.Utils;

import javax.xml.transform.Result;
import java.sql.*;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


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
    protected Integer id;
    protected HashMap<String, Object> values = new HashMap<>();


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

    public abstract String getModelNamePlural();

    public String getTableName() {
        return this.getModelNamePlural().toLowerCase();
    }

    public abstract HashMap<String, AttributeType> getAttributes();

    public HashMap<String, AttributeType> getResolvedAttributes(boolean includeVirtual) {
        HashMap<String, AttributeType> attrs = getAttributes();
        attrs = attrs == null ? new HashMap<>() : attrs;

        if (!includeVirtual) {
            ArrayList<String> virtualCols = new ArrayList<>();
            for (Map.Entry<String, AttributeType> e :
                    attrs.entrySet()) {
                if (e.getValue().isVirtual) virtualCols.add(e.getKey());
            }

            for (String vCol : virtualCols) {
                attrs.remove(vCol);
            }
        }

        attrs.put(ATTR_ID, AttributeType.INTEGER);
        attrs.put(ATTR_CREATED_AT, AttributeType.DATE);
        attrs.put(ATTR_UPDATED_AT, AttributeType.DATE);
        attrs.put(ATTR_ACTIVATED_AT, AttributeType.DATE);
        attrs.put(ATTR_DELETED_AT, AttributeType.DATE);

        return attrs;
    }

    public boolean createColumn(String colName, AttributeType attrType) {
        String query = "ALTER TABLE " + getTableName() + " ADD COLUMN " + colName + " " + attrType.toSql() + ";";
        System.out.println("[createColumn] query: " + query);

        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(query);
            return true;
        } catch (Exception e) {
            System.err.println("[createColumn] error: " + e);
        }
        return false;
    }

    public boolean createTable() {
        String query = "CREATE TABLE " + getTableName() + " (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL);";
        System.out.println("[createTable] query: " + query);

        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(query);

            HashMap<String, AttributeType> attrs = getResolvedAttributes(false);
            for (String attrName : attrs.keySet()) {
                if (attrName.equals(ATTR_ID)) continue;

                createColumn(attrName, attrs.get(attrName));
            }
        } catch (Exception e) {
            System.err.println("[createTable] error: " + e);
        }
        return false;
    }

    public static Boolean isAuditAttr(String attrName) {
        return ATTR_NAMES_AUDIT.contains(attrName);
    }

    public boolean isNew() {
        return this.id == null || this.id < 1;
    }

    public Integer getId() {
        return this.id;
    }

    public Object getAuditValue(String attributeName) {
        switch (attributeName) {
            case ATTR_CREATED_AT:
                return this.createdAt;
            case ATTR_UPDATED_AT:
                return this.updatedAt;
            case ATTR_DELETED_AT:
                return this.deletedAt;
            case ATTR_ACTIVATED_AT:
                return this.activatedAt;
            case ATTR_ID:
                return this.getId();
            default:
                return null;
        }
    }

    public Object getValue(String attributeName) {
        if (ATTR_NAMES_AUDIT.contains(attributeName)) return getAuditValue(attributeName);
        return values.get(attributeName);
    }

    public boolean save() {
        StringBuilder qms = new StringBuilder();
        StringBuilder cols = new StringBuilder();
        StringBuilder updates = new StringBuilder();

        HashMap<String, AttributeType> attrs = getResolvedAttributes(false);
        HashMap<Integer, String> colIdxMap = new HashMap<>();

        int colIdx = 1;
        for (Map.Entry<String, AttributeType> e : attrs.entrySet()) {
            String key = e.getKey();
            AttributeType attrType = e.getValue();

            if (key.equals(ATTR_ID) || attrType.isVirtual) continue;

            qms.append('?');
            cols.append(key);

            updates.append(key);
            updates.append('=');
            updates.append('?');

            colIdxMap.put(colIdx, key);
            if (colIdx != (attrs.size() - 1)) {
                cols.append(',');
                qms.append(',');
                updates.append(',');
            }

            colIdx++;
        }

        StringBuilder sql = new StringBuilder();

        if (isNew()) {
            sql.append("INSERT INTO ")
                    .append(getTableName())
                    .append(" (")
                    .append(cols)
                    .append(") VALUES (")
                    .append(qms)
                    .append(");");
        } else {
            sql.append("UPDATE ");
            sql.append(getTableName());
            sql.append(" SET ");

            sql.append(updates.toString());

            sql.append(" WHERE id=");
            sql.append(getId());
            sql.append(";");
        }

        Date updatedAt = new Date();

        if (this.createdAt == null) this.createdAt = new Date();
        Date createdAt = this.createdAt;

        PreparedStatement statement = null;
        Integer updateCount = null;
        boolean success = false;

        try {
            statement = getConnection().prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS);

            System.out.println("query: " + sql);

            for (Map.Entry<Integer, String> colEntry : colIdxMap.entrySet()) {
                String attrName = colEntry.getValue();

                AttributeType type = attrs.get(attrName);
                Object value = DbModel.isAuditAttr(attrName) ? getAuditValue(attrName) : getValue(attrName);

                if (colEntry.getValue().equals(ATTR_UPDATED_AT)) value = updatedAt;

                if (value == null) {
                    statement.setNull(colEntry.getKey(), Types.NULL);
                    continue;
                }

                switch (type.dataType) {
                    case AttributeType.DATA_TYPE_STRING:
                        statement.setString(colEntry.getKey(), (String) value);
                        break;
                    case AttributeType.DATA_TYPE_DATE:
                        Timestamp ts = new java.sql.Timestamp(((Date) value).getTime());
                        statement.setTimestamp(colEntry.getKey(), ts);
                        break;
                    case AttributeType.DATA_TYPE_INTEGER:
                        statement.setInt(colEntry.getKey(), (Integer) value);
                        break;
                }
            }

            updateCount = statement.executeUpdate();
            System.out.println("statement: " + statement.toString() + " updates: " + updateCount);

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;

            if (updateCount == null || updateCount == 0) {
                System.out.println("[" + this.getClass().getSimpleName() + "] save failed, no rows affected.");
            } else {

                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        this.id = generatedKeys.getInt(1);
                        success = true;
                    } else if (this.isNew()) {
                        throw new SQLException("[" + this.getClass().getSimpleName() + "] save failed, no ID obtained.");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }

            }
        }

        return success;
    }

    public HashMap<String, AttributeType> getResolvedAttributes() {
        return getResolvedAttributes(true);
    }

    public DbDoc toJson() {
        HashMap<String, AttributeType> attrs = getResolvedAttributes();
        attrs = attrs == null ? new HashMap<>() : attrs;
        DbDoc doc = new DbDocImpl();

        doc.add("type", new JsonString() {{
            setValue(DbModel.this.getClass().getSimpleName());
        }});

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (Map.Entry<String, AttributeType> entry : attrs.entrySet()) {
            String attrName = entry.getKey();

            Object value = DbModel.isAuditAttr(attrName) ? this.getAuditValue(attrName) : this.getValue(attrName);

            if (value == null) {
                doc.add(attrName, JsonLiteral.NULL);
                continue;
            }

            AttributeType attrType = entry.getValue();

            if (attrType.isNumber()) {
                doc.add(attrName, new JsonNumber() {{
                    this.setValue(value.toString());
                }});
            } else if (attrType.isString()) {
                doc.add(attrName, new JsonString() {{
                    this.setValue(value.toString());
                }});
            } else if (attrType.isDatetime()) {
                doc.add(attrName, new JsonString() {{
                    this.setValue(format.format(value));
                }});
            }
        }
        return doc;
    }

    public static <T extends DbModel> JsonArray manyToJson(ArrayList<T> models) {
        JsonArray jsonArray = new JsonArray();

        for (T model : models) {
            jsonArray.add(model.toJson());
        }

        return jsonArray;
    }

    public void updateAuditFromJson(DbDoc json) {
        if (json.containsKey(ATTR_CREATED_AT)) this.createdAt = Utils.extractDate(json, ATTR_CREATED_AT, null);
        if (json.containsKey(ATTR_UPDATED_AT)) this.updatedAt = Utils.extractDate(json, ATTR_UPDATED_AT, null);
        if (json.containsKey(ATTR_DELETED_AT)) this.deletedAt = Utils.extractDate(json, ATTR_DELETED_AT, null);
        if (json.containsKey(ATTR_ACTIVATED_AT)) this.activatedAt = Utils.extractDate(json, ATTR_ACTIVATED_AT, null);

        if (json.containsKey(ATTR_ID)) {
            try{

                JsonNumber num = (JsonNumber) json.get(ATTR_ID);
                this.id = num != null ? num.getInteger() : null;
            }catch (Exception e){
                System.out.println("[" + getClass().getSimpleName() + "] error parsing 'id':" + e);
            }
        }
    }

    public void updateFromJson(DbDoc json) {
        HashMap<String, AttributeType> attrs = getResolvedAttributes();

        for (String attrName : json.keySet()) {
            if (!attrs.containsKey(attrName)) continue;

            AttributeType attrType = attrs.get(attrName);
            JsonValue v = json.get(attrName);

            try{
                if (attrType.isNumber()) {
                    values.put(attrName, v != null ? ((JsonNumber) v).getInteger() : null);
                } else if (attrType.isString()) {
                    values.put(attrName, v != null ? ((JsonString) v).getString() : null);
                } else if (attrType.isDatetime()) {
                    Date d = v != null ? Timestamp.valueOf(((JsonString) v).getString()) : null;
                    values.put(attrName, d);
                }
            }catch (Exception e){
                System.err.println("[" + getClass().getSimpleName() + "] adding: " + attrName + ", val: " + v);
            }

        }
    }
    public void updateAuditFromResultSet(ResultSet resultSet) throws SQLException {
        this.createdAt = resultSet.getTimestamp(ATTR_CREATED_AT);
        this.updatedAt = resultSet.getTimestamp(ATTR_UPDATED_AT);
        this.deletedAt = resultSet.getTimestamp(ATTR_DELETED_AT);
        this.activatedAt = resultSet.getTimestamp(ATTR_ACTIVATED_AT);
        this.id = resultSet.getInt(ATTR_ID);
    }

    public void updateFromResultSet(ResultSet resultSet) throws SQLException {
        for (Map.Entry<String, AttributeType> e : getResolvedAttributes(false).entrySet()) {

            if (e.getValue().isNumber()) {
                Integer val = resultSet.getInt(e.getKey());
                val = resultSet.wasNull() ? null : val;
                values.put(e.getKey(), val);
            } else if (e.getValue().isString()) {
                values.put(e.getKey(), resultSet.getString(e.getKey()));
            } else if (e.getValue().isDatetime()) {
                values.put(e.getKey(), resultSet.getTimestamp(e.getKey()));
            }

        }
    }

    public Boolean update(DbDoc json) {
        updateAuditFromJson(json);
        updateFromJson(json);
        return true;
    }

    public Boolean update(ResultSet resultSet) {
        try {
            updateAuditFromResultSet(resultSet);
            updateFromResultSet(resultSet);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static class Json extends DbDocImpl {

        public Json(String key, String value){
            add(key, value);
        }

        public Json(){}

        public Json add(String key, String value){
            put(key, new JsonString(){{
                setValue(value);
            }});
            return this;
        }

        public Json add(String key, Integer value){
            put(key, new JsonNumber(){{
                setValue(value.toString());
            }});
            return this;
        }

        public static class Where extends HashMap<String, String> {
            public static final Where EMPTY = new Where();

            public Where() {
                super();
            }

            public Where(String key, String value) {
                put(key, value);
            }

            public Where and(String key, String value) {
                put(key, value);
                return this;
            }

        }

        public static <T extends DbModel> ArrayList<T> find(Class<T> entityClass, HashMap<String, String> where, Integer limit) {
            where = where == null ? Where.EMPTY : where;
            ArrayList<T> models = new ArrayList<>();

            if (entityClass == null) return models;

            String clsName = entityClass.getSimpleName();
            System.out.println("[" + clsName + "] find: " + where);
            //where.put(ATTR_DELETED_AT, "null");

            try {
                T inst = entityClass.newInstance();
                Statement stmt = inst.getConnection().createStatement();
                ResultSet rs;

                String tableName = inst.getTableName();

                if (tableName == null)
                    throw new Exception("Table name can not be null: " + inst.getClass().getCanonicalName());

                String query = "SELECT * FROM " + tableName + " WHERE " + ATTR_DELETED_AT + " IS NULL";

                HashMap<String, AttributeType> attrs = inst.getResolvedAttributes();

                for (Map.Entry<String, AttributeType> e : attrs.entrySet()) {

                    if (!where.containsKey(e.getKey())) continue;

                    query += " AND";

                    String value = where.get(e.getKey());
                    switch (e.getValue().dataType) {
                        case AttributeType.DATA_TYPE_STRING:
                            query += " " + e.getKey() + "=\"" + value + "\"";
                            break;
                        case AttributeType.DATA_TYPE_INTEGER:
                            query += " " + e.getKey() + "=" + value + "";
                            break;
                        default:
                            throw new Exception("Unsupported attribute: type=\"" + e.getValue() + "\", value=\"" + value);
                    }
                }

                query += " ORDER BY updated_at DESC";

                if (limit != null && limit > 0) query += " LIMIT " + limit;

                query += ";";

                System.out.println("[" + clsName + "#find] submitting query: " + query);
                rs = stmt.executeQuery(query);

                while (rs.next()) {
                    T obj = entityClass.newInstance();
                    obj.update(rs);
                    models.add(obj);
                }

            } catch (Exception err) {
                System.err.println("[" + clsName + "#find] error: " + err);
                err.printStackTrace();
            }
            return models;
        }

        public static <T extends DbModel> ArrayList<T> findAll(Class<T> entityClass, HashMap<String, String> where) {
            return find(entityClass, where, null);
        }

    }

    public static void main (String [] args) throws SQLException {
//        HashMap<String, AttributeType> attrs = new Musicroom().getDbAttributes();
//
//        System.out.println(attrs);
//
//        Connection c = createConnection("playthis1.db.old");
//        System.out.println("Connection created " + c);
//
//        ResultSet rs = c.createStatement().executeQuery(".schema");
//
//        //ResultSet rs = c.createStatement().executeQuery("pragma table_info(table_name);");
//
//        while(rs.next()) {
//            // read the result set
//            System.out.println("TEXT = " + rs.getString("name"));
//        }
//

       Musicroom room1 = new Musicroom();
        room1.createTable();
        room1.update(new Json()
                .add("Hawa", "Hawa's graduation")
                .add("Closed", "Come celebrate with me ")
        );
        room1.save();

        User user2 = new User();
        user2.createTable();

        user2.update(new Utils.Json()
                .add("email", "kando@gmail.net")
                .add("gender", "M")
                .add("name", "Kando")
        );
        user2.save();
        System.out.println(user2.values);

        Track song1 = new Track();
        song1.createTable();
        song1.update(new Utils.Json()
                .add("title", "you")
                .add("artist_name", "Jaques")
                .add("thumbnail_url", "www.spotify........jaques")
        );
        song1.save();


    }
}

