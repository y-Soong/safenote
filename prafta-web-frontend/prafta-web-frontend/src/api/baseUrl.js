// src/api/baseUrl.js
export const resolveBaseURL = () => {
  const cfg = (typeof window !== "undefined" && window.__APP_CONFIG__) || {};
  if (cfg.API_BASE) {
    const context = cfg.CONTEXT ?? "/prafta";
    return `${cfg.API_BASE}${context}`;
  }

  if (typeof window !== "undefined" && window.location?.protocol === "file:") {
    const apiBase =
      import.meta.env.VITE_FILE_API_BASE || "http://172.30.1.4:8080";
    const context = import.meta.env.VITE_API_CONTEXT || "/prafta";
    return `${apiBase}${context}`;
  }

  return "/prafta";
};
