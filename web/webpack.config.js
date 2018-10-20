var path = require('path');

module.exports = {
  entry: './classes/kotlin/js/main/web.js',
  output: {
    path: path.resolve('./bundle'),
    publicPath: '/build/',
    filename: 'bundle.js'
  },
  resolve: {
    modules: [
      path.resolve('js'),
      path.resolve('..', 'src'),
      path.resolve('.'),
      path.resolve('node_modules')
    ],
    extensions: ['.js', '.css']
  },
  module: {
    rules: []
  },
  devtool: '#source-map',
  plugins: []
};

console.log(module.exports.resolve.modules);


