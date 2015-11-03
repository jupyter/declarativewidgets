var hyd = require('hydrolysis'),
    fs = require('fs');

if (!process.argv[2] || !process.argv[3]) {
    console.log('Usage: ' + process.argv[1] + ' inputFile outputFile');
    process.exit(1);
}

hyd.Analyzer.analyze(process.argv[2], {
        clean: true,
        filter: function(href){
            if ((href.indexOf('bower_components') > -1 &&
                    href.indexOf('urth') === -1) ||
                    href.indexOf("http://") > -1 ||
                    href.indexOf("https://") > -1) {
                return true;
            } else {
                return false;
            }
        }
    }).then(function(analyzer) {
        function clean(item) {
            item.scriptElement = undefined;
            item.javascriptNode = undefined;
            Array.isArray(item.properties) && item.properties.forEach(function(property) {
                property.javascriptNode = undefined;
                property.observerNode = undefined;
            });
        }

        analyzer.elements.forEach(clean);
        analyzer.behaviors.forEach(clean);
        var myobj = {
            behaviors: analyzer.behaviors,
            behaviorsByName: {},
            elements: analyzer.elements,
            elementsByTagName: {},
            features: []
        };
        myobj.elements.forEach(function(element) {
            if (!myobj.elementsByTagName[element.is]) {
                myobj.elementsByTagName[element.is] = element;
            }
        });
        myobj.behaviors.forEach(function(behavior) {
            if (!myobj.behaviorsByName[behavior.is]) {
                myobj.behaviorsByName[behavior.is] = behavior;
            }
        });
        fs.writeFileSync(process.argv[3], JSON.stringify(myobj));
});
