package com.oreilley.playthis1.usefulResources;

import com.mysql.cj.xdevapi.*;

import java.io.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;

import com.mysql.cj.xdevapi.*;
import com.oreilley.playthis1.databaseModel.DbModel;

import java.io.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

public class Utils {

    public static String readFile(String filePath){
        String fileData = null;
        try (BufferedReader in = new BufferedReader(new FileReader(filePath))){
            System.out.println("File path is: " + filePath);
            String str;

            StringBuilder contentBuilder = new StringBuilder();
            while ((str = in.readLine()) != null) {
                contentBuilder.append(str);
                contentBuilder.append("\n");
            }

            fileData = contentBuilder.toString();
        } catch (IOException e) {
            System.err.println("[Error] unable to read \""+ filePath + "\": " + e);
        }
        return fileData;
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

    }

    public static <T extends DbModel> DbDoc responseJson(ArrayList<T> models){
        DbDoc doc = new DbDocImpl();
        doc.put("data", DbModel.manyToJson(models));
        return doc;
    }

    public static String stringFromInput(InputStream in) {
        StringBuilder buf = new StringBuilder(512);
        InputStreamReader isr;

        try {
            isr = new InputStreamReader(in, "utf-8");
            BufferedReader br = new BufferedReader(isr);

            int b;

            while ((b = br.read()) != -1) {
                buf.append((char) b);
            }

            br.close();
            isr.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return buf.toString();
    }

    public static DbDoc jsonFromInput(InputStream in){
        String body = stringFromInput(in);
        return JsonParser.parseDoc(body);
    }

    public static Date extractDate(DbDoc json, String attrCreatedAt, Date defaultValue) {
        JsonString v = (JsonString) json.get(attrCreatedAt);
        if(v == null || v.getString().trim().isEmpty()) return defaultValue;

        return Timestamp.valueOf(v.getString());
    }

    public static String toStartCase(String s) {
        return s.substring(0, 1).toUpperCase() +
                s.substring(1).toLowerCase();
    }

    public static String formFromSchema(DbDoc jsonValue) {
        StringBuilder htmlForm = new StringBuilder();

        htmlForm.append("<form>");

        DbDoc props = (DbDoc) jsonValue.get("properties");

        for (Map.Entry<String, JsonValue> e: props.entrySet()){
            DbDoc js = (DbDoc) e.getValue();
            String format = js.containsKey("format") ? ((JsonString) js.get("format")).getString() : null;
            String type = ((JsonString) js.get("type")).getString();

            //htmlForm.append("<div class=\"form-row\">");
            htmlForm.append("<div class=\"form-group\">");

            htmlForm.append("<label for=\"").append(e.getKey()).append("\">");
            htmlForm.append(toStartCase(e.getKey()));
            htmlForm.append("</label>");

            String input = "text";
            if(type.equals("string") && format != null && format.equals("date-time")){
                input = "date";
            }else if(type.equals("integer")){
                input = "number";
            }

            String attrName = e.getKey();
            htmlForm.append("<input type=\"").append(input)
                    .append("\" class=\"form-control\" id=\"")
                    .append(e.getKey()).append("\" name=\"")
                    .append(e.getKey()).append("\" value=\"<%= ")
                    .append(attrName).append(" %>\"")
                    .append(">");

            htmlForm.append("</div>");
        }
        htmlForm.append("</form>");
        return htmlForm.toString();
    }
}

