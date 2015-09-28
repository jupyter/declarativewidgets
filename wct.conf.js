/*
 * Copyright (c) Jupyter Development Team.
 * Distributed under the terms of the Modified BSD License.
 */
module.exports = {
  plugins: {
    local: {
      browsers: ['chrome']
    }
  },
  webserver: {
    pathMappings: [
      {'/elements': 'bower_components'}
    ],
    urlPrefix: ''
  }
};
