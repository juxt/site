'use strict';

if (process.env.NODE_ENV === 'production') {
  module.exports = require('./detect-it.cjs.production.js');
} else {
  module.exports = require('./detect-it.cjs.development.js');
}
