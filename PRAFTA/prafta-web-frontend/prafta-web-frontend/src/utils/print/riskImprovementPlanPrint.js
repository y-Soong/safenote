// 위험성평가 개선실행계획서 인쇄 공용 유틸 (패턴 A: window.open + document.write).
//   RiskAssessInfo.vue 의 fnOpenImprovementPlan HTML 빌더를 추출한 단일 출처.
//   동작/출력은 기존과 1:1 동일해야 한다(회귀 금지). 호출처: RiskAssessInfo.vue, AcctSafetyPrintPop.vue.
//
// data 필드(원형 그대로 전달; 기본값/개행치환/조건분기는 본 빌더가 내부 처리):
//   processNm, riskTypeNm, initAssessDate, initAssessorNm, hazardNm, initDesc,
//   initLikelihoodScore, initSeverityScore, initRiskLv, assessmentStatus,
//   assessmentStatusName(진행상태명, SYS011), revalDate, revalBeforeDesc, beforePhotoUrl
import { getRiskLevelClass6 } from "@/utils/riskLevel";

// HTML 특수문자 5종 엔티티 치환(Stored XSS 방어). 인쇄 HTML 은 document.write 로 동일출처 새 창에
//   기록되므로, 사용자 자유입력(위험성평가 설명/평가자명 등)을 그대로 박으면 <script>/<img onerror>
//   가 실행되어 window.opener 의 JWT 탈취가 가능하다. 모든 동적 텍스트는 결합 전 반드시 escape 한다.
//   ★export: RiskAssessInfo.vue 의 개선완료보고서 인라인 빌더도 동일 유틸로 이스케이프(단일 출처).
export const escapeHtml = (value) => {
  const s = value == null ? "" : String(value);
  return s
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
};

// 텍스트 표시값: escape 먼저 → 그 다음 개행만 <br> 로(정상 텍스트는 1:1 동일, 악성 태그만 무력화).
const textWithBreaks = (value) => escapeHtml(value).replace(/\n/g, "<br>");

// 이미지 URL 화이트리스트: http(s):// · blob: 또는 '/' 시작 상대경로만 허용, 그 외는 빈값(주입 차단).
//   속성값 자리에 들어가므로 escapeHtml 도 함께 적용한다.
//   ★blob: 허용 사유: RiskAssessInfo 개선완료보고서의 개선 후 사진은 방금 업로드한 미저장 프리뷰
//     (URL.createObjectURL 산출 blob: URL)가 유입된다. blob: 은 img src 컨텍스트에서 스크립트 실행
//     벡터가 아니며(문서 탐색 아님·이미지 디코드만), 저장 데이터에 "blob:" 문자열이 섞여 들어와도
//     이미지 로드 실패로 끝난다 — javascript: 등 위험 스킴 차단 목적은 그대로 유지된다.
//   ★export: RiskAssessInfo.vue 의 개선완료보고서 인라인 빌더 <img src> 에도 동일 유틸 적용(단일 출처).
export const safeImageSrc = (url) => {
  const s = url == null ? "" : String(url).trim();
  if (!/^https?:\/\//i.test(s) && !/^blob:/i.test(s) && !s.startsWith("/"))
    return "";
  return escapeHtml(s);
};

// 개선실행계획서 인쇄용 HTML 문자열 생성(기존 fnOpenImprovementPlan 과 동일 산출 + XSS 무력화).
export const buildImprovementPlanHtml = (data) => {
  const d = data || {};

  // 데이터 준비 (동적 텍스트는 전부 escape, 자유입력 설명은 escape 후 개행치환)
  const processNm = escapeHtml(d.processNm || "-");
  const riskTypeNm = escapeHtml(d.riskTypeNm || "-");
  const initAssessDate = escapeHtml(d.initAssessDate || "-");
  const initAssessorNm = escapeHtml(d.initAssessorNm || "-");
  const hazardNm = escapeHtml(d.hazardNm || "-");
  const initDesc = textWithBreaks(d.initDesc || "-");
  const initLikelihoodScore = escapeHtml(d.initLikelihoodScore || "-");
  const initSeverityScore = escapeHtml(d.initSeverityScore || "-");
  const initRiskLv = escapeHtml(d.initRiskLv || "-");
  // riskLevelClass 는 getRiskLevelClass6 가 고정 클래스 문자열만 반환(사용자 입력 아님).
  const riskLevelClass = getRiskLevelClass6(d.initRiskLv);
  const assessmentStatusName = escapeHtml(d.assessmentStatusName || "");
  const revalDate =
    d.assessmentStatus == "002" || d.assessmentStatus == "003"
      ? escapeHtml(d.revalDate || "-")
      : "";
  const revalBeforeDesc =
    d.assessmentStatus == "002" || d.assessmentStatus == "003"
      ? textWithBreaks(d.revalBeforeDesc || "-")
      : "";
  const safeSrc = safeImageSrc(d.beforePhotoUrl);
  const photoHtml = safeSrc
    ? '<img src="' + safeSrc + '" alt="개선 전 사진" class="print-photo" />'
    : "사진 없음";
  const printDate = new Date().toLocaleString("ko-KR");

  // 프린트용 HTML 생성
  let printContent =
    "<!DOCTYPE html>" +
    "<html>" +
    "<head>" +
    '<meta charset="UTF-8">' +
    "<title>개선실행계획서</title>" +
    "<style>" +
    "@media print {" +
    "  @page { size: A4 landscape; margin: 5mm; }" +
    "  body { margin: 0; padding: 0; height: 100%; overflow: hidden; }" +
    "}" +
    "body {" +
    '  font-family: "Pretendard", sans-serif;' +
    "  font-size: 13px;" +
    "  line-height: 1.4;" +
    "  color: #333;" +
    "  padding: 10px 8px 5px 8px;" +
    "  height: 100%;" +
    "  display: flex;" +
    "  flex-direction: column;" +
    "  box-sizing: border-box;" +
    "}" +
    ".print-header {" +
    "  text-align: center;" +
    "  margin-bottom: 10px;" +
    "  border-bottom: 2px solid #333;" +
    "  padding-bottom: 6px;" +
    "  flex-shrink: 0;" +
    "}" +
    ".print-header h1 {" +
    "  font-size: 20px;" +
    "  font-weight: bold;" +
    "  margin: 0;" +
    "}" +
    ".print-content {" +
    "  display: flex;" +
    "  gap: 10px;" +
    "  margin-bottom: 2px;" +
    "  flex: 1;" +
    "  min-height: 0;" +
    "}" +
    ".print-section {" +
    "  flex: 1;" +
    "  border: 1px solid #ddd;" +
    "  border-radius: 4px;" +
    "  padding: 8px;" +
    "  display: flex;" +
    "  flex-direction: column;" +
    "  min-height: 0;" +
    "  overflow: hidden;" +
    "}" +
    ".section-title {" +
    "  background: #f5f5f5;" +
    "  padding: 6px;" +
    "  font-weight: bold;" +
    "  border-bottom: 1px solid #ddd;" +
    "  margin: -8px -8px 6px -8px;" +
    "  font-size: 14px;" +
    "  flex-shrink: 0;" +
    "}" +
    ".print-row {" +
    "  display: flex;" +
    "  margin-bottom: 3px;" +
    "  align-items: flex-start;" +
    "  flex-shrink: 0;" +
    "}" +
    ".print-label {" +
    "  flex: 0 0 120px;" +
    "  font-weight: 500;" +
    "  color: #333;" +
    "  font-size: 13px;" +
    "  padding-top: 5px;" +
    "}" +
    ".print-value {" +
    "  flex: 1;" +
    "  color: #666;" +
    "  font-size: 13px;" +
    "}" +
    ".print-textarea {" +
    "  min-height: 35px;" +
    "  white-space: pre-wrap;" +
    "  word-break: break-word;" +
    "  line-height: 1.2;" +
    "}" +
    ".print-photo {" +
    "  max-width: 100%;" +
    "  max-height: 144px;" +
    "  border: 1px solid #ddd;" +
    "  padding: 3px;" +
    "}" +
    ".risk-evaluation {" +
    "  display: flex;" +
    "  gap: 10px;" +
    "  align-items: center;" +
    "}" +
    ".risk-item {" +
    "  display: flex;" +
    "  align-items: center;" +
    "  gap: 5px;" +
    "}" +
    ".risk-item-label {" +
    "  font-weight: 500;" +
    "  font-size: 13px;" +
    "}" +
    ".risk-level {" +
    "  padding: 3px 8px;" +
    "  border-radius: 4px;" +
    "  font-weight: bold;" +
    "  min-width: 35px;" +
    "  text-align: center;" +
    "  font-size: 13px;" +
    "}" +
    ".risk-very-high { background: #ff4444; color: #fff; }" +
    ".risk-high { background: #ff8800; color: #fff; }" +
    ".risk-slightly-high { background: #ffaa00; color: #1f1e1e; }" +
    ".risk-normal { background: #ffd700; color: #1f1e1e; }" +
    ".risk-low { background: #90ee90; color: #1f1e1e; }" +
    ".risk-very-low { background: #228b22; color: #fff; }" +
    ".print-date {" +
    "  text-align: right;" +
    "  margin-top: auto;" +
    "  padding-top: 3px;" +
    "  font-size: 11px;" +
    "  color: #666;" +
    "  flex-shrink: 0;" +
    "}" +
    "</style>" +
    "</head>" +
    "<body>" +
    '<div class="print-header">' +
    "<h1>개선실행계획서</h1>" +
    "</div>" +
    '<div class="print-content">' +
    '<div class="print-section">' +
    '<div class="section-title">개선 전</div>' +
    '<div class="print-row">' +
    '<div class="print-label">작업명</div>' +
    '<div class="print-value">' +
    processNm +
    "</div>" +
    "</div>" +
    '<div class="print-row">' +
    '<div class="print-label">위험성구분</div>' +
    '<div class="print-value">' +
    processNm +
    "</div>" +
    "</div>" +
    '<div class="print-row">' +
    '<div class="print-label">위험성분류</div>' +
    '<div class="print-value">' +
    riskTypeNm +
    "</div>" +
    "</div>" +
    '<div class="print-row">' +
    '<div class="print-label">평가요청일자</div>' +
    '<div class="print-value">' +
    initAssessDate +
    "</div>" +
    "</div>" +
    '<div class="print-row">' +
    '<div class="print-label">평가요청자</div>' +
    '<div class="print-value">' +
    initAssessorNm +
    "</div>" +
    "</div>" +
    '<div class="print-row">' +
    '<div class="print-label">유해요인명</div>' +
    '<div class="print-value">' +
    hazardNm +
    "</div>" +
    "</div>" +
    '<div class="print-row">' +
    '<div class="print-label">유해요인설명</div>' +
    '<div class="print-value print-textarea">' +
    initDesc +
    "</div>" +
    "</div>" +
    '<div class="print-row">' +
    '<div class="print-label">사진</div>' +
    '<div class="print-value">' +
    photoHtml +
    "</div>" +
    "</div>" +
    '<div class="print-row">' +
    '<div class="print-label">개선 전 위험성 평가</div>' +
    '<div class="print-value">' +
    '<div class="risk-evaluation">' +
    '<div class="risk-item">' +
    '<span class="risk-item-label">빈도:</span>' +
    "<span>" +
    initLikelihoodScore +
    "</span>" +
    "</div>" +
    '<div class="risk-item">' +
    '<span class="risk-item-label">강도:</span>' +
    "<span>" +
    initSeverityScore +
    "</span>" +
    "</div>" +
    '<div class="risk-item">' +
    '<span class="risk-item-label">위험도:</span>' +
    '<span class="risk-level ' +
    riskLevelClass +
    '">' +
    initRiskLv +
    "</span>" +
    "</div>" +
    "</div>" +
    "</div>" +
    "</div>" +
    "</div>" +
    '<div class="print-section">' +
    '<div class="section-title">개선 후</div>' +
    '<div class="print-row">' +
    '<div class="print-label">진행상태</div>' +
    '<div class="print-value">' +
    assessmentStatusName +
    "</div>" +
    "</div>";

  // 개선예정일자와 임시조치 내용 추가
  if (d.assessmentStatus == "002" || d.assessmentStatus == "003") {
    printContent =
      printContent +
      '<div class="print-row">' +
      '<div class="print-label">개선예정일자</div>' +
      '<div class="print-value">' +
      revalDate +
      "</div>" +
      "</div>" +
      '<div class="print-row">' +
      '<div class="print-label">임시조치 내용</div>' +
      '<div class="print-value print-textarea">' +
      revalBeforeDesc +
      "</div>" +
      "</div>";
  }

  printContent =
    printContent +
    "</div>" +
    "</div>" +
    '<div class="print-date">' +
    "출력일시: " +
    printDate +
    "</div>" +
    "</body>" +
    "</html>";

  return printContent;
};

// 개선실행계획서 인쇄(새 창 열기). 팝업이 차단되면 false 반환(호출처가 안내 처리).
export const printImprovementPlan = (data) => {
  const printContent = buildImprovementPlanHtml(data);

  // 새 창 열기
  const printWindow = window.open("", "_blank");
  if (printWindow) {
    printWindow.document.write(printContent);
    printWindow.document.close();
    // 프린트 대화상자 열기
    setTimeout(() => {
      printWindow.print();
      // 프린트 후 창 닫기
      printWindow.addEventListener("afterprint", () => {
        printWindow.close();
      });
    }, 250);
    return true;
  }
  return false;
};
