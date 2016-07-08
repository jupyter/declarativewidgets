var path = require('path');
var fs = require('fs');
var exec = require('child_process').execSync;

var logOutput = function(error, stdout, stderr){
  if (error) {
    console.error('exec error: ', error);
  }
  console.error(stderr);
  console.log(stdout);
};
var installPath = [process.env.PWD, '..', '..'].join(path.sep);
console.log('Installing declarativewidgets into: ' + installPath);

fs.readdir([process.env.PWD, 'elements'].join(path.sep), function(err, items) {
    for (var i=0; i<items.length; i++) {
        var component = [process.env.PWD, 'elements', items[i]].join(path.sep);
        exec('bower install --allow-root ' + component , {cwd :  installPath}, logOutput);
    }
});
