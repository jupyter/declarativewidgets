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
              browserName: "firefox",
              platform: "Linux"
          },
          {
              browserName: "safari",
              platform: "OS X 10.10"
          },
          {
              browserName: "chrome",
              platform: "Windows 10"
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
