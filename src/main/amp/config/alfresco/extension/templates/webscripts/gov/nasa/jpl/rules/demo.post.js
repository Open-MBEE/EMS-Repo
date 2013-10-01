<import resource="classpath:alfresco/extension/js/json2.js">
<import resource="classpath:alfresco/extension/js/utils.js">

var expression;

main();

function main() {
    var expression = requestbody.getContent().toString();
    
    // ensure mandatory file attributes have been located
    if (expression == undefined)
    {
        status.code = 400;
        status.message = "No expression specified";
        status.redirect = true;
    }
    else
    {
        // setup model for response template
        model.demo = demo;
        model.expression = expression;
        status.code = 200;
    }
}
