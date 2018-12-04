package com.oreilley.playthis1.databaseModel;

import java.util.HashMap;

public class User extends DbModel {

    public HashMap<String, AttributeType> getAttributes() {
        return new HashMap<String, AttributeType>(){{
            put("email", AttributeType.STRING);
            put("name", AttributeType.STRING);
            put("gender", AttributeType.CHARACTER);
//            put(ATTR_PASSWORD_HASH, AttributeType.TEXT);
//            put(ATTR_PASSWORD, AttributeType.TEXT_VIRTUAL);
//            put(ATTR_SPOTIFY_ACCESSTOKEN, new AttributeType(AttributeType.DATA_TYPE_STRING, 2500));
        }};
    }

    public String getModelNamePlural(){
        return "users";
    }


}
