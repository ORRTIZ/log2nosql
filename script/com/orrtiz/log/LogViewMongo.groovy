import org.ofbiz.webtools.*;
result = runService("getAllLogsFromMongoDb", ["userLogin": userLogin]);
logList = result.get("logList");
context.logList = logList;