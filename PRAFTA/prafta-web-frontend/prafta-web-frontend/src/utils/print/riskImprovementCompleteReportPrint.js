// 위험성평가 개선완료보고서 인쇄 공용 유틸 (패턴 A: window.open + document.write).
//   RiskAssessInfo.vue 의 fnOpenImprovementReport 인라인 HTML 빌더를 추출한 단일 출처.
//   동작/출력은 기존 Risk_03 개선완료보고서와 1:1 동일해야 한다(회귀 금지).
//   호출처: RiskAssessInfo.vue(위험성평가 정보 팝업), AcctSafetyPrintPop.vue(사고 일괄출력).
//
// data 필드(원형 그대로 전달; 기본값/개행치환/이스케이프는 본 빌더가 내부 처리):
//   processNm, riskTypeNm, initAssessDate, initAssessorNm, hazardNm, initDesc,
//   initLikelihoodScore, initSeverityScore, initRiskLv,
//   assessmentStatusName(진행상태명, SYS011), revalDate, revalBeforeDesc,
//   revalAssessorNm, revalAssessDate, revalDesc,
//   revalLikelihoodScore, revalSeverityScore, revalRiskLv,
//   beforePhotoUrl(개선 전 사진 URL), afterPhotoUrl(개선 후 사진 URL; blob: 프리뷰 허용)
import { getRiskLevelClass6 } from "@/utils/riskLevel";
import {
  escapeHtml,
  safeImageSrc,
} from "@/utils/print/riskImprovementPlanPrint";

// 텍스트 표시값: escape 먼저 → 그 다음 개행만 <br> 로(개선실행계획서 유틸과 동일 패턴).
const textWithBreaks = (value) => escapeHtml(value).replace(/\n/g, "<br>");

// 개선완료보고서 인쇄용 HTML 문자열 생성(기존 RiskAssessInfo 인라인 빌더와 동일 산출).
export const buildImprovementCompleteReportHtml = (data) => {
  const d = data || {};

  // 데이터 준비 — XSS 방어: document.write 로 흘러가는 모든 동적 텍스트는 escapeHtml 후 결합.
  const processNm = escapeHtml(d.processNm || "-");
  const riskTypeNm = escapeHtml(d.riskTypeNm || "-");
  const initAssessDate = escapeHtml(d.initAssessDate || "-");
  const initAssessorNm = escapeHtml(d.initAssessorNm || "-");
  const hazardNm = escapeHtml(d.hazardNm || "-");
  const initDesc = textWithBreaks(d.initDesc || "-");
  const initLikelihoodScore = escapeHtml(d.initLikelihoodScore || "-");
  const initSeverityScore = escapeHtml(d.initSeverityScore || "-");
  const initRiskLv = escapeHtml(d.initRiskLv || "-");
  const initRiskLevelClass = getRiskLevelClass6(d.initRiskLv);
  const assessmentStatusName = escapeHtml(d.assessmentStatusName || "-");
  const revalDate = escapeHtml(d.revalDate || "-");
  const revalBeforeDesc = textWithBreaks(d.revalBeforeDesc || "-");
  const revalAssessorNm = escapeHtml(d.revalAssessorNm || "-");
  const revalAssessDate = escapeHtml(d.revalAssessDate || "-");
  const revalDesc = textWithBreaks(d.revalDesc || "-");
  const revalLikelihoodScore = escapeHtml(d.revalLikelihoodScore || "-");
  const revalSeverityScore = escapeHtml(d.revalSeverityScore || "-");
  const revalRiskLv = escapeHtml(d.revalRiskLv || "-");
  const revalRiskLevelClass = getRiskLevelClass6(d.revalRiskLv);
  // 이미지 src 는 화이트리스트+escape 경유(safeImageSrc — http(s)/blob:/상대경로 외 스킴 차단).
  const safeBeforeSrc = safeImageSrc(d.beforePhotoUrl);
  const beforePhotoHtml = safeBeforeSrc
    ? '<img src="' +
      safeBeforeSrc +
      '" alt="개선 전 사진" class="print-photo" />'
    : "사진 없음";
  const safeAfterSrc = safeImageSrc(d.afterPhotoUrl);
  const afterPhotoHtml = safeAfterSrc
    ? '<img src="' +
      safeAfterSrc +
      '" alt="개선 후 사진" class="print-photo" />'
    : "사진 없음";
  const printDate = new Date().toLocaleString("ko-KR");

  // 프린트용 HTML 생성
  let printContent =
    "<!DOCTYPE html>" +
    "<html>" +
    "<head>" +
    '<meta charset="UTF-8">' +
    "<title>개선완료보고서</title>" +
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
    "  margin-bottom: 5px;" +
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
    "  margin-bottom: 8px;" +
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
    "  padding-top: 5px;" +
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
    "}" +
    ".risk-evaluation {" +
    "  display: flex;" +
    "  gap: 10px;" +
    "  align-items: center;" +
    "  margin-top: -3px;" +
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
    "  padding-top: 5px;" +
    "  font-size: 11px;" +
    "  color: #666;" +
    "  flex-shrink: 0;" +
    "}" +
    "</style>" +
    "</head>" +
    "<body>" +
    '<div class="print-header">' +
    "<h1>개선완료보고서</h1>" +
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
    beforePhotoHtml +
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
    initRiskLevelClass +
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
    "</div>" +
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
    "</div>" +
    '<div class="print-row">' +
    '<div class="print-label">개선관리자</div>' +
    '<div class="print-value">' +
    revalAssessorNm +
    "</div>" +
    "</div>" +
    '<div class="print-row">' +
    '<div class="print-label">개선완료일자</div>' +
    '<div class="print-value">' +
    revalAssessDate +
    "</div>" +
    "</div>" +
    '<div class="print-row">' +
    '<div class="print-label">개선내용</div>' +
    '<div class="print-value print-textarea">' +
    revalDesc +
    "</div>" +
    "</div>" +
    '<div class="print-row">' +
    '<div class="print-label">개선사진</div>' +
    '<div class="print-value">' +
    afterPhotoHtml +
    "</div>" +
    "</div>" +
    '<div class="print-row">' +
    '<div class="print-label">개선 후 위험성 평가</div>' +
    '<div class="print-value">' +
    '<div class="risk-evaluation">' +
    '<div class="risk-item">' +
    '<span class="risk-item-label">빈도:</span>' +
    "<span>" +
    revalLikelihoodScore +
    "</span>" +
    "</div>" +
    '<div class="risk-item">' +
    '<span class="risk-item-label">강도:</span>' +
    "<span>" +
    revalSeverityScore +
    "</span>" +
    "</div>" +
    '<div class="risk-item">' +
    '<span class="risk-item-label">위험도:</span>' +
    '<span class="risk-level ' +
    revalRiskLevelClass +
    '">' +
    revalRiskLv +
    "</span>" +
    "</div>" +
    "</div>" +
    "</div>" +
    "</div>" +
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

// 개선완료보고서 인쇄(새 창 열기). 팝업이 차단되면 false 반환(호출처가 안내 처리).
export const printImprovementCompleteReport = (data) => {
  const printContent = buildImprovementCompleteReportHtml(data);

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
