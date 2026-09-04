<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-wide"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <!-- 헤더 -->
        <div class="modal-header" @mousedown="startDrag">
          <span>개선완료보고서</span>
          <div class="header-actions">
            <button type="button" class="btn btn-report" @click="printReport">
              인쇄
            </button>
            <button class="icon-button" @click="$emit('close')">
              <svg
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
                stroke-width="1.5"
                stroke="currentColor"
                class="w-6 h-6"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M6 18L18 6M6 6l12 12"
                />
              </svg>
            </button>
          </div>
        </div>

        <!-- 인쇄 영역 -->
        <div class="modal-body report-body" ref="printRef">
          <h2 class="report-title">개선완료보고서</h2>

          <div class="report-grid">
            <!-- 위험상황 -->
            <section class="report-section">
              <div class="report-section__title">위험상황</div>
              <div class="report-row">
                <span class="report-label">작업명</span
                ><span class="report-value">{{ report.processNm || "-" }}</span>
              </div>
              <div class="report-row">
                <span class="report-label">위험성구분</span
                ><span class="report-value">{{ report.processNm || "-" }}</span>
              </div>
              <div class="report-row">
                <span class="report-label">위험성분류</span
                ><span class="report-value">{{
                  report.riskTypeNm || "-"
                }}</span>
              </div>
              <div class="report-row">
                <span class="report-label">평가요청일자</span
                ><span class="report-value">{{
                  report.initAssessDate || "-"
                }}</span>
              </div>
              <div class="report-row">
                <span class="report-label">유해요인명</span
                ><span class="report-value">{{ report.hazardNm || "-" }}</span>
              </div>
              <div class="report-row report-row--col">
                <span class="report-label">유해요인설명</span>
                <span class="report-value report-value--multiline">{{
                  report.initDesc || "-"
                }}</span>
              </div>
              <div class="report-row report-row--col">
                <span class="report-label">사진</span>
                <div class="report-photo">
                  <img
                    v-if="beforePhotoUrl"
                    :src="beforePhotoUrl"
                    alt="위험상황 사진"
                  />
                  <span v-else class="report-photo__empty">사진 없음</span>
                </div>
              </div>
              <div class="report-row">
                <span class="report-label">개선 전 위험도</span>
                <span class="report-value">
                  빈도 {{ report.initLikelihoodScore || "-" }} · 강도
                  {{ report.initSeverityScore || "-" }}
                  <span
                    class="risk-chip"
                    :class="riskLevelClass(report.initRiskLv)"
                    >{{ report.initRiskLv || "-" }}</span
                  >
                </span>
              </div>
            </section>

            <!-- 개선 결과 -->
            <section class="report-section">
              <div class="report-section__title">개선 결과</div>
              <div class="report-row">
                <span class="report-label">진행상태</span
                ><span class="report-value">개선완료</span>
              </div>
              <div class="report-row">
                <span class="report-label">개선완료일</span
                ><span class="report-value">{{
                  report.revalAssessDate || "-"
                }}</span>
              </div>
              <div class="report-row">
                <span class="report-label">개선관리자</span
                ><span class="report-value">{{
                  report.revalAssessorNm || "-"
                }}</span>
              </div>
              <div class="report-row report-row--col">
                <span class="report-label">위험도 변화</span>
                <div class="risk-change">
                  <span
                    class="risk-chip"
                    :class="riskLevelClass(report.initRiskLv)"
                    >{{ report.initRiskLv || "-" }}</span
                  >
                  <span class="risk-change__arrow">→</span>
                  <span
                    class="risk-chip"
                    :class="riskLevelClass(report.revalRiskLv)"
                    >{{ report.revalRiskLv || "-" }}</span
                  >
                </div>
              </div>
            </section>
          </div>

          <!-- 개선항목 이력 N건 -->
          <section class="report-section report-section--full">
            <div class="report-section__title">개선항목 이력</div>
            <table class="report-table">
              <thead>
                <tr>
                  <th class="report-table__no">#</th>
                  <th>개선일자</th>
                  <th>개선내용</th>
                  <th class="report-table__photo">사진</th>
                  <th class="report-table__num">빈도</th>
                  <th class="report-table__num">강도</th>
                  <th class="report-table__lv">위험도</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="improvementItems.length === 0">
                  <td colspan="7" class="report-table__empty">
                    개선항목 이력이 없습니다.
                  </td>
                </tr>
                <tr
                  v-for="(item, idx) in improvementItems"
                  :key="item.improvementSeq"
                >
                  <td class="report-table__no">{{ idx + 1 }}</td>
                  <td>{{ fnFormatYmd(item.improveDate) }}</td>
                  <td class="report-table__desc">
                    {{ item.improveDesc || "-" }}
                  </td>
                  <td class="report-table__photo">
                    <img
                      v-if="item.photoUrl"
                      :src="item.photoUrl"
                      alt="개선 사진"
                    />
                    <span v-else class="report-photo__empty">-</span>
                  </td>
                  <td class="report-table__num">
                    {{ item.likelihoodScore || "-" }}
                  </td>
                  <td class="report-table__num">
                    {{ item.severityScore || "-" }}
                  </td>
                  <td class="report-table__lv">
                    <span
                      class="risk-chip"
                      :class="riskLevelClass(item.riskLv)"
                      >{{ item.riskLv || "-" }}</span
                    >
                  </td>
                </tr>
              </tbody>
            </table>
          </section>

          <div class="report-date">출력일시: {{ printedAt }}</div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
/* eslint-disable */
import { ref, onMounted, getCurrentInstance, defineProps, defineEmits } from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { getRiskLevelClass6 } from "@/utils/riskLevel";
import { buildFileServingUrl } from "@/utils/fileUrl";

const { proxy } = getCurrentInstance();

// 컴포넌트 계약(§A): 평가키 + 표시 데이터 props
const props = defineProps({
  riskAssessmentData: {
    type: Object,
    default: () => ({}),
  },
});

const emit = defineEmits(["close"]);

const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

const printRef = ref(null);

// 개선일자 표시 형식: DB 는 varchar(8) YYYYMMDD 로 저장하므로 화면에서는 YYYY-MM-DD 로 끊어 보여준다.
//   공용 유틸(formatDateString)은 8/6자리만 변환하고 그 외(빈값·날짜시각 문자열)는 원본을 돌려주므로 안전하다.
const fnFormatYmd = (ymd) => proxy.$util.formatDateString(ymd) || "-";

// 위험상황 + 개선 결과 표시 데이터 (props 로 채움)
const report = ref({
  processNm: "",
  riskTypeNm: "",
  initAssessDate: "",
  hazardNm: "",
  initDesc: "",
  initLikelihoodScore: "",
  initSeverityScore: "",
  initRiskLv: "",
  revalAssessDate: "",
  revalAssessorNm: "",
  revalRiskLv: "",
});

// 개선항목 이력 N건
const improvementItems = ref([]);

// 개선 전 사진 URL
const beforePhotoUrl = ref(null);

// 출력일시
const printedAt = ref("");

// 위험도 6단계 클래스 (공용 유틸 위임 — D1 단일 출처)
const riskLevelClass = (riskLv) => getRiskLevelClass6(riskLv);

// 평가키 (API 공통 파라미터)
const assessKeys = () => ({
  siteCd: props.riskAssessmentData.siteCd,
  processCd: props.riskAssessmentData.processCd,
  assessmentCd: props.riskAssessmentData.assessmentCd,
});

// 파일 경로 + 파일관리코드 → 표시 URL 조합.
//   공용 유틸(buildFileServingUrl)로 일원화 — 동일 출처 상대경로 조립이라 터널/도메인 경유에도 동작.
const buildFileUrl = (filePath, fileMgmtCd) =>
  buildFileServingUrl(filePath, fileMgmtCd) || null;

// 인쇄 (브라우저 인쇄 — @media print 로 헤더/오버레이 숨김)
const printReport = () => {
  window.print();
};

// 개선항목 이력 로드 (시간순 = SEQ 오름차순 표시)
const loadImprovementItems = async () => {
  try {
    const response = await axios.get("/webApi/riskimpr01/improvement-items", {
      params: assessKeys(),
    });
    if (response.status === 200) {
      const list = response.data?.improvementItemList || [];
      // 목록은 최신 SEQ 상단(DESC) → 이력 테이블은 등록순(오름차순)으로 표시
      const ordered = [...list].sort(
        (a, b) => (a.improvementSeq || 0) - (b.improvementSeq || 0)
      );
      improvementItems.value = ordered.map((row) => ({
        improvementSeq: row.improvementSeq,
        improveDate: row.improveDate || "",
        improveDesc: row.improveDesc || "",
        photoUrl: buildFileUrl(row.filePath, row.fileMgmtCd),
        likelihoodScore: row.likelihoodScore != null ? String(row.likelihoodScore) : "",
        severityScore: row.severityScore != null ? String(row.severityScore) : "",
        riskLv: row.riskLv || "",
      }));
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// 위험상황 + 개선 결과 표시값 초기화 (props 데이터)
const initReport = () => {
  const d = props.riskAssessmentData || {};
  report.value = {
    processNm: d.processNm || "",
    riskTypeNm: d.riskTypeNm || "",
    initAssessDate: d.initAssessDate || "",
    hazardNm: d.hazardNm || "",
    initDesc: d.initDesc || "",
    initLikelihoodScore: d.initLikelihoodScore || "",
    initSeverityScore: d.initSeverityScore || "",
    initRiskLv: d.initRiskLv || "",
    revalAssessDate: d.revalAssessDate || "",
    revalAssessorNm: d.revalAssessorNm || "",
    revalRiskLv: d.revalRiskLv || "",
  };
  beforePhotoUrl.value = buildFileUrl(d.initFilePath, d.initFileMgmtCd);
};

onMounted(async () => {
  initReport();
  await loadImprovementItems();
  printedAt.value = new Date().toLocaleString("ko-KR");
});
</script>

<style scoped>
.modal-content-wide {
  width: 100%;
  max-width: 1400px;
  max-height: 90vh;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.report-body {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.report-title {
  text-align: center;
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--color-text-strong);
  border-bottom: 2px solid var(--color-border-strong);
  padding-bottom: 0.5rem;
  margin: 0;
}

.report-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.report-section {
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 0.75rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.report-section--full {
  grid-column: 1 / -1;
}

.report-section__title {
  font-weight: 700;
  color: var(--color-text-strong);
  border-bottom: 1px solid var(--color-border);
  padding-bottom: 0.4rem;
  margin-bottom: 0.4rem;
}

.report-row {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  font-size: 0.875rem;
}

.report-row--col {
  flex-direction: column;
}

.report-label {
  flex: 0 0 110px;
  font-weight: 500;
  color: var(--color-text);
}

.report-value {
  flex: 1 1 auto;
  color: var(--color-text-muted);
}

.report-value--multiline {
  white-space: pre-wrap;
  word-break: break-word;
}

.report-photo {
  border: 1px solid var(--color-border);
  border-radius: 4px;
  background: var(--color-bg);
  min-height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0.5rem;
  width: 100%;
  box-sizing: border-box;
}

.report-photo img {
  max-width: 100%;
  max-height: 200px;
  object-fit: contain;
}

.report-photo__empty {
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

/* 위험도 변화 */
.risk-change {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.risk-change__arrow {
  font-weight: 700;
  color: var(--color-text-muted);
}

.risk-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 48px;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  font-weight: 700;
  font-size: 0.85rem;
  border: 1px solid var(--color-border);
}
/* 위험도 6단계 색상은 P1 공용 유틸 클래스(전역) 사용 — 색 하드코딩 금지(D1) */

/* 이력 테이블 */
.report-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.8125rem;
  table-layout: fixed;
}

.report-table th,
.report-table td {
  border: 1px solid var(--color-border);
  padding: 0.4rem 0.5rem;
  text-align: left;
  vertical-align: top;
  word-break: break-word;
}

.report-table thead th {
  background: var(--color-bg);
  font-weight: 600;
  color: var(--color-text);
}

.report-table__no {
  width: 40px;
  text-align: center;
}

.report-table__num {
  width: 56px;
  text-align: center;
}

.report-table__lv {
  width: 90px;
  text-align: center;
}

.report-table__photo {
  width: 100px;
  text-align: center;
}

.report-table__photo img {
  max-width: 88px;
  max-height: 64px;
  object-fit: contain;
}

.report-table__desc {
  white-space: pre-wrap;
}

.report-table__empty {
  text-align: center;
  color: var(--color-text-muted);
  padding: 0.75rem 0;
}

.report-date {
  text-align: right;
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

/* 버튼 */
.btn {
  padding: 0 1rem;
  border-radius: var(--btn-radius);
  font-size: 0.875rem;
  font-weight: 500;
  font-family: "Pretendard", sans-serif;
  cursor: pointer;
  white-space: nowrap;
}

.btn-report {
  background: var(--color-surface);
  color: var(--color-primary);
  border: 1px solid var(--color-primary);
}

.btn-report:hover {
  background: rgba(22, 163, 74, 0.06);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 인쇄: 오버레이/헤더 버튼 숨기고 보고서 본문만 (developer 가 인쇄 트리거 시 활용) */
@media print {
  .modal-header,
  .header-actions {
    display: none;
  }
  .modal-overlay {
    position: static;
    background: none;
  }
  .modal-content-wide {
    max-width: none;
    max-height: none;
    box-shadow: none;
    border: none;
  }
}
</style>
