// tailwind.config.js – PRAFTA UI 가이드 토큰
module.exports = {
  content: ["./index.html", "./src/**/*.{vue,js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        primary: "#16a34a",
        danger: "#ef4444",
        "text-strong": "#111827",
        "text-muted": "#4b5563",
        border: "#e5e7eb",
        borderStrong: "#d1d5db",
      },
      backgroundColor: {
        page: "#f9fafb",
      },
      ringColor: {
        focus: "rgba(22, 163, 74, 0.35)",
      },
    },
  },
  plugins: [],
};
