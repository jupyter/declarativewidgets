/*
 * Copyright (c) Jupyter Development Team.
 * Distributed under the terms of the Modified BSD License.
 */
module.exports = {
  plugins: {
    local: {
      browsers: ['chrome']
    },
    sauce: {
      disabled: true,
      browsers: [
          {
              browserName: "chrome",
              platform: "Linux",
              version: "45.0"
          },
          {
              browserName: "chrome",
              platform: "OS X 10.10",
              version: "45.0"
          },
          {
              browserName: "chrome",
              platform: "Windows 10",
              version: "45.0"
          }
      ]
    }
  },
  webserver: {
    pathMappings: [
      {'/elements': 'bower_components'}
    ],
    urlPrefix: ''
  }
};
