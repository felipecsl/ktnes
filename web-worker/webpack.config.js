'use strict';

var path = require('path');

var config = {
  "mode": "development",
  "context": path.resolve('js'),
  "entry": {
    "web-worker": "./web-worker"
  },
  "output": {
    "path": path.resolve('./bundle'),
    "filename": "[name].bundle.js",
    "chunkFilename": "[id].bundle.js",
    "publicPath": "/"
  },
  "module": {
    "rules": []
  },
  "resolve": {
    "modules": [
      path.resolve('js'),
      path.resolve('..', 'src'),
      path.resolve('.'),
      path.resolve('node_modules'),
    ]
  },
  "plugins": []
};

module.exports = config;
