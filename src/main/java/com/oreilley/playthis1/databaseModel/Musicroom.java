package com.oreilley.playthis1.databaseModel;

import java.util.HashMap;

public class Musicroom extends DbModel{
    public static final String ATTR_CREATED_BY = "created_by";


    @Override
    public HashMap<String, AttributeType> getAttributes() {
        return new HashMap<String, AttributeType>() {{
            put(ATTR_CREATED_BY, AttributeType.INTEGER);
            put("name", AttributeType.STRING);
            put("status", AttributeType.STRING);
            put("description", AttributeType.TEXT);
        }};
    }
    public String getModelNamePlural(){
        return "musicrooms";
    }
}
