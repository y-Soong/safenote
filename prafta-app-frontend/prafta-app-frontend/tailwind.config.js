// tailwind.config.js
const colors = require('tailwindcss/colors')

module.exports = {
  purge: {
    content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
    options: {
      safelist: ['bg-orange-300'],
    },
  },
  theme: {
    extend: {
      colors: {
        orange: colors.orange, // ✅ orange 팔레트 강제 복구
      },
      fontFamily: {
        sans: ['Pretendard', 'sans-serif'],
      },
    },
  },
  plugins: [],
}
