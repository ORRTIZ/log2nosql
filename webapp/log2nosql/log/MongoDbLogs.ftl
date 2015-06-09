<h3>Log Results: </h3>
<#if logList?has_content>
    <#list logList as logLine>
      <div>${logLine.date?datetime} : [${logLine.level}] : ${logLine.loggerName} : ${logLine.message}</div>
    </#list>
<#else>
    No Logs found!
</#if>