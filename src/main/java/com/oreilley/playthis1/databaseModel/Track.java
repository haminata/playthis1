package com.oreilley.playthis1.databaseModel;

import com.oreilley.playthis1.usefulResources.Utils;

import java.util.HashMap;

public class Track extends DbModel {

    public static final String ATTR_TITLE = "title";
    public static final String ATTR_ARTIST_NAME = "artist_name";

    @Override
    public String getModelNamePlural() {
        return "tracks";
    }

    @Override
    public HashMap<String, AttributeType> getAttributes() {
        return new HashMap<String, AttributeType>(){{
            put(ATTR_TITLE, AttributeType.STRING);
            put(ATTR_ARTIST_NAME, AttributeType.STRING);
            put("trackId", AttributeType.STRING);
            put("thumbnail_url", new AttributeType(AttributeType.DATA_TYPE_STRING, 400));
        }};
    }


}

