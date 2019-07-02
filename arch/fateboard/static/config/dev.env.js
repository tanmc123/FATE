'use strict'
const merge = require('webpack-merge')
const prodEnv = require('./prod.env')

module.exports = merge(prodEnv, {
  NODE_ENV: '"development"',
  BASE_API: '"http://172.16.153.113:16688"',
  WEBSOCKET_BASE_API: '"ws://172.16.153.113:16688"'
})