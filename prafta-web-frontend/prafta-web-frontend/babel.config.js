module.exports = {
  presets: ["@babel/preset-env"],
  plugins: [
    "@babel/plugin-proposal-optional-chaining",
    "@babel/plugin-proposal-nullish-coalescing-operator",
    "@babel/plugin-proposal-logical-assignment-operators", // 이 플러그인이 핵심!
  ],
};
