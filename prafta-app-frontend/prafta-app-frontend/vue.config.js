const fs = require('fs')
const path = require('path')

module.exports = {
  publicPath: './',
  transpileDependencies: ['vuetify', 'pinia'],

  chainWebpack: (config) => {
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
    host: '0.0.0.0',
    port: 8082,

    // 🔴 핵심: 카메라 복귀 시 전체 새로고침/HMR 차단
    hot: false,
    liveReload: false,
    overlay: false,

    // v3에서는 disableHostCheck 로 충분 (allowedHosts 제거!)
    disableHostCheck: true,

    // 소켓 고정 (선택) — v3에서도 동작함
    sockHost: '172.30.1.4',
    sockPort: '8082',
    sockPath: '/sockjs-node',

    https: {
      key: fs.readFileSync(path.resolve(__dirname, 'cert/172.30.1.4-key.pem')),
      cert: fs.readFileSync(path.resolve(__dirname, 'cert/172.30.1.4.pem')),
    },

    historyApiFallback: true,

    // 변경 감지 안정화 (선택)
    watchOptions: {
      ignored: /node_modules/,
      poll: 1000,
      aggregateTimeout: 300,
    },

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
