// const { defineConfig } = require("@vue/cli-service");

module.exports = {
  transpileDependencies: ["vuetify", "pinia"],
  chainWebpack: (config) => {
    config.module
      .rule("mjs")
      .test(/\.mjs$/)
      .include.add(/node_modules/)
      .end()
      .type("javascript/auto")
      .use("babel-loader")
      .loader("babel-loader")
      .tap((options) => {
        return options;
      });
  },
  devServer: {
    port: 8081,
    proxy: {
      "/prafta": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
};
