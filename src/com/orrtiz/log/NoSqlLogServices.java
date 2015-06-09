package com.orrtiz.log;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.entity.util.EntityUtilProperties;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

public class NoSqlLogServices {
    public static final String module = NoSqlLogServices.class.getName();
    
    public static DBCollection getConnectionMongoDb(String username, String password, String dbName, String collectionName) {
        DBCollection coll = null;
        try{  
           // To connect to mongodb server
           MongoClient mongoClient = new MongoClient( "localhost" , 27017 );
           // Now connect to your databases
           DB db = mongoClient.getDB(dbName);
           Debug.log("Connect to database successfully");
           boolean auth = db.authenticate(username, password.toCharArray());
           Debug.log("Authentication: "+auth);
           
           coll = db.getCollection(collectionName);
        }catch(Exception e){
           System.err.println( e.getClass().getName() + ": " + e.getMessage() );
        }
        return coll;
     }
    
    public static Map<String, Object> getLogFromNoSqlDb(DispatchContext dctx, Map<String, ? extends Object> context) {
        List<DBObject> logList = new ArrayList<DBObject>();
        DBCollection collection = getConnectionMongoDb("ofbiz", "ofbiz", "ofbiz", "applicationLog");
        DBCursor cursor = collection.find().limit(400);
        while (cursor.hasNext()) {
            DBObject dbObject = cursor.next();
            logList.add(dbObject);
        }
        Map<String, Object> resp = ServiceUtil.returnSuccess();
        resp.put("logList", logList);
        return resp;
    }
    
    public static Map<String, Object> searchNoSqlLog(DispatchContext dctx, Map<String, ? extends Object> context) {
        String searchText = (String) context.get("searchText");
        String fatal = (String) context.get("fatal");
        String error = (String) context.get("error");
        String warning = (String) context.get("warning");
        String info = (String) context.get("info");
        List<DBObject> logList = new ArrayList<DBObject>();
        Map<String, Object> resp = ServiceUtil.returnSuccess();
             
        List<String> selectedOptions = new ArrayList<String>();
        // Find out the checkboxes selected
        if ("Y".equals(fatal)) {
            selectedOptions.add("FATAL");
        }
        if ("Y".equals(error)) {
            selectedOptions.add("ERROR");
        }
        if ("Y".equals(warning)) {
            selectedOptions.add("WARNING");
        }
        if ("Y".equals(info)) {
            selectedOptions.add("INFO");
        }
             
        // By default, select all levels
        if (selectedOptions.isEmpty()) {
            selectedOptions.add("ERROR");
            selectedOptions.add("INFO");
            selectedOptions.add("FATAL");
            selectedOptions.add("WARNING");
        }
        // Get DBCollection object by connecting to MongoDB
        DBCollection collection = getConnectionMongoDb("ofbiz", "ofbiz", "ofbiz", "applicationLog");
        BasicDBObject basicDBObject = new BasicDBObject();
        basicDBObject.put("level", new BasicDBObject("$in", selectedOptions));
        
        if (UtilValidate.isEmpty(searchText)) {
            // If no search text is entered:
            // call the find method constrained by the log level selected
            // set limit to 400 logs
            DBCursor cursor = collection.find(basicDBObject).limit(400);
            // Sort results based on date
            cursor.sort(new BasicDBObject("date", -1));
            // Iterate records and add them to list
            while (cursor.hasNext()) {
                DBObject dbObject = cursor.next();
                logList.add(dbObject);
            }
            cursor.close();
            resp.put("logList", logList);
        } else {
            // If search text is entered:
            // prepare a regular expression for the search text
            BasicDBObject regexQuery = new BasicDBObject();
            regexQuery.put("message", new BasicDBObject("$regex", searchText).append("$options", "i"));
                 
            BasicDBObject andQuery = new BasicDBObject();
                 
            List<BasicDBObject> obj = new ArrayList<BasicDBObject>();
            obj.add(regexQuery);
            obj.add(basicDBObject);
            // Constrain using both the conditions: search text and levels
            andQuery.put("$and", obj);
            // Perform the find operation
            DBCursor cursor = collection.find(andQuery).limit(400);
            // Sort results based on date
            cursor.sort(new BasicDBObject("date", -1));
            // Iterate records and add them to list
            while (cursor.hasNext()) {
                DBObject dbObject = cursor.next();
                logList.add(dbObject);
            }
            cursor.close();
            resp.put("logList", logList);
        }
        return resp;
    }
}