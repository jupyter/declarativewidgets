var init = require('./dist/declarativewidgets/static/js/init/init');
var declWidgetModel = require('./dist/declarativewidgets/static/js/widgets/DeclWidgetModel');
var declWidgets = {
  DeclWidgetModel : declWidgetModel,
  init : init
};

if(window && window.define) {
  console.log('defining jupyter-decl-widgets/DeclWidgetModel.');
  window.define('jupyter-decl-widgets/DeclWidgetModel', declWidgetModel);
}

exports.init = init;