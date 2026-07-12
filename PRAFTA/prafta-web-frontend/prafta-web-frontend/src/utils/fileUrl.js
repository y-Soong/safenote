// src/utils/fileUrl.js
// 첨부 파일 정적 서빙 URL 빌더 (이미지/영상/PDF <img src> 등 표시용 단일 출처).
// - FILE_PATH 가 Windows 저장으로 백슬래시(\)를 포함할 수 있어 슬래시로 정규화한다.
// - fileName 은 확장자 포함명(FILE_MGMT_CD + FILE_EXT)을 그대로 받는다.
//   (chkLst03 inspect-result-details / chkLst04 defect-lists 모두 fileMgmtCd 에 확장자 포함)
// - base 결정: 런타임 주입(window.__APP_CONFIG__.FILE_BASE → API_BASE) → 없으면 "동일 출처 상대경로".
//   ★빌드타임 VITE_API_BASE_URL 절대주소(http://localhost:8080)를 쓰지 않는다 —
//     Cloudflare 터널/도메인(https) 경유 시 localhost 절대주소는 혼합콘텐츠 차단 + 원격 도달 불가로
//     이미지가 깨진다. 상대경로(/uploads/...)는 API(/prafta)와 동일하게 현재 origin 을 타므로
//     localhost 직접 접속과 터널 도메인 접속 모두에서 동작한다(vite dev 프록시에 /uploads 등록).
export function buildFileServingUrl(filePath, fileName) {
  if (!filePath || !fileName) return "";
  const normalizedPath = String(filePath).replace(/\\/g, "/");
  const isAbsolute =
    normalizedPath.startsWith("http://") ||
    normalizedPath.startsWith("https://");
  if (isAbsolute) {
    return `${normalizedPath}/${fileName}`;
  }
  // 상대 서빙 경로는 항상 선두 슬래시 보장(/uploads/...)
  const relPath = normalizedPath.startsWith("/")
    ? normalizedPath
    : `/${normalizedPath}`;

  // 런타임 설정(배포 시 index.html 에서 주입)이 있으면 해당 host 를 prefix.
  const cfg = (typeof window !== "undefined" && window.__APP_CONFIG__) || {};
  const base = cfg.FILE_BASE || cfg.API_BASE || "";
  if (!base) {
    return `${relPath}/${fileName}`; // 동일 출처 상대경로 (기본)
  }
  const cleanBase = base.endsWith("/") ? base.slice(0, -1) : base;
  return `${cleanBase}${relPath}/${fileName}`;
}
