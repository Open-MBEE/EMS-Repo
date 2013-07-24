<html>
  <head>
    <title>Folder ${folder.displayPath}/${folder.name}</title>
  </head>
  <body>
    <p>Alfresco ${server.edition} Edition v${server.version} : dir</p>
    <p>Contents of folder ${folder.displayPath}/${folder.name}</p>
    <table>
    <#list folder.children as child>
       <tr>
           <td><#if child.isContainer>d</#if></td>
           <#if verbose>
              <td>${child.properties.modifier}</td>
              <td><#if child.isDocument>
                 ${child.properties.content.size}</#if></td>
              <td>${child.properties.modified?date}</td>
           </#if>
           <td>${child.name}</td>
       </tr>
    </#list>
    </table>
  </body>
</html>