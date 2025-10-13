const fs = require('fs')
const path = require('path')

module.exports = {
  publicPath: './',
  transpileDependencies: ['vuetify', 'pinia'],

  chainWebpack: (config) => {
    // ✅ .mjs 파일도 Babel 로 통과시켜서 최신 문법 지원
    config.module
      .rule('mjs')
      .test(/\.mjs$/)
      .include.add(/node_modules/)
      .end()
      .type('javascript/auto')
      .use('babel-loader')
      .loader('babel-loader')
      .options({
        presets: [['@babel/preset-env', { targets: { esmodules: true } }]],
      })
  },

  devServer: {
    port: 8082,
    https: {
      key: fs.readFileSync(path.resolve(__dirname, 'cert/localhost-key.pem')),
      cert: fs.readFileSync(path.resolve(__dirname, 'cert/localhost-cert.pem')),
    },
    allowedHosts: ['all'],

    proxy: {
      '/prafta': {
        target: 'http://172.30.1.4:8080',
        changeOrigin: true,
        secure: false,
        logLevel: 'debug',
      },
    },
  },
}

// module.exports = {
//   publicPath: './',
//   transpileDependencies: ['vuetify', 'pinia'],
//   chainWebpack: (config) => {
//     config.module
//       .rule('mjs')
//       .test(/\.mjs$/)
//       .include.add(/node_modules/)
//       .end()
//       .type('javascript/auto')
//       .use('babel-loader')
//       .loader('babel-loader')
//       .tap((options) => {
//         return options
//       })
//   },
//   devServer: {
//     port: 8082,
//     proxy: {
//       '/prafta': {
//         target: 'http://172.30.1.33:8080',
//         changeOrigin: true,
//       },
//     },
//   },
// }
