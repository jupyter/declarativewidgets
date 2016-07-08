define(['jquery', 'jupyter-js-widgets'], function($, Widgets) {  
  var WidgetManager = function(kernel, events) {
      //  Call the base class.
      Widgets.ManagerBase.call(this);
      this.kernel = kernel;
      // TODO Can we remove this?
      this.events = events;
      
      this._shimWidgetsLibs();
      this.validate();  
  };

  WidgetManager.prototype = Object.create(Widgets.ManagerBase.prototype);

  WidgetManager.register_widget_model = function(model_name, model_type) {
    return Widgets.ManagerBase.register_widget_model.apply(this, arguments);
  };

  WidgetManager.register_widget_view = function(view_name, view_type) {
    return Widgets.ManagerBase.register_widget_view.apply(this, arguments);
  };
  
  /*
  * Called to create a new comm channel between frontend and backend
  * jupyter widget models. Returns a promise of a new comm channel.
  *
  * targetName: comm channel target name on the backend
  * id: jupyter widget model ID
  * metadata: ???
  */
  WidgetManager.prototype._create_comm = function(targetName, id, data) {
      return Promise.resolve(
          this.commManager.new_comm(targetName, data, this.callbacks(), {}, id)
      );
  };

  WidgetManager.prototype._shimWidgetsLibs = function() {
    // Create a comm manager shim
    this.commManager = new Widgets.shims.services.CommManager(this.kernel);

    // Register the comm target
    this.commManager.register_target(this.comm_target_name, this.handle_comm_open.bind(this));
    
    this.kernel.widget_manager = this;
    this.kernel.comm_manager = this.commManager;
  };
  
  WidgetManager.prototype.validate = function() {
    this.validateVersion().then(function(valid) {
      if (!valid) {
        console.warn('Widget frontend version does not match the backend.');
      }
    }).catch(function(err) {
      console.warn('Could not cross validate the widget frontend and backend versions.', err);
    });
  };
  
  return WidgetManager;
});