// QE-1-11 시드 — 00010 지오펜스 좌표 설정/원복. node e1/seed-1-11-gps.mjs set|clear
// UI(SiteInfoPop)와 동일 payload/endpoint(save-site-infos). 좌표는 지시서 M7 값.
import { call, getToken } from "../lib/http.mjs";

const mode = process.argv[2] || "set";
const base = {
  siteCd: "00010",
  siteNo: "QT001",
  siteNm: "QT통합테스트사업장",
  cmpnyCd: "001",
  addr1: "서울특별시 테스트구 QT로 1",
  addr2: null,
  zipCode: "04524",
  strDate: "20260101",
  endDate: null,
  useYn: "Y",
  siteAdminCd: null,
  telNo: null,
  siteDesc: "[QT-0] 통합 시나리오 테스트 전용 사업장",
};
const body = mode === "set"
  ? [{ ...base, gpsRange: "150", lat: "37.5532178", lon: "126.9377458" }]
  : [{ ...base, gpsRange: null, lat: null, lon: null }];

const main = async () => {
  const t = await getToken("QTHR", "QtTest!2026", "WEB");
  const r = await call("POST", "/webApi/baim01/save-site-infos", { token: t, body });
  console.log(`${mode}:`, r.status, r.text.slice(0, 200));
};
main().catch((e) => { console.error("실패:", e.message); process.exitCode = 1; });
