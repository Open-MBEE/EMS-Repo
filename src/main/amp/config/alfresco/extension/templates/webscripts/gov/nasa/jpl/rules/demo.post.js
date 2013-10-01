var expression;

main();

function main() {
    //Query parameters on URL as exposed as args dictionary)
    if ("expression" in args) {
        expression =  args.expression;
    }
    
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
