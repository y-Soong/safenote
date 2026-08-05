<!--
  AiTokenUsagePop.vue — 회사별 월간 AI 토큰 사용량 이력 팝업 (플랫폼 운영자 전용, read-only)
  - 호출: Platform_03 행 "당월 AI 사용량" 셀 클릭. props 로 대상 회사 수신.
  - 조회: GET /platformApi/customer/token-usage-lists?cmpnyCd= (최근 24개월, USE_YM 내림차순)
-->
<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div class="modal-content-narrow">
        <!-- Title -->
        <div class="modal-header">
          <span>월별 AI 토큰 사용량</span>
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

        <div class="usage-container">
          <!-- 대상 회사 요약 -->
          <div class="usage-summary">
            <span class="usage-summary-label">회사</span>
            <span class="usage-summary-value"
              >{{ cmpnyNm }} ({{ cmpnyCd }})</span
            >
          </div>

          <!-- 월별 이력 표 -->
          <div class="usage-table-box">
            <table class="usage-table">
              <thead>
                <tr>
                  <th>연월</th>
                  <th class="usage-num">호출 수</th>
                  <th class="usage-num">입력 토큰</th>
                  <th class="usage-num">출력 토큰</th>
                  <th class="usage-num">합계</th>
                </tr>
              </thead>
              <tbody>
                <template v-if="loading">
                  <tr>
                    <td colspan="5" class="usage-empty">조회 중...</td>
                  </tr>
                </template>
                <template v-else-if="usageList.length === 0">
                  <tr>
                    <td colspan="5" class="usage-empty">
                      AI 사용 이력이 없습니다.
                    </td>
                  </tr>
                </template>
                <template v-else>
                  <tr v-for="row in usageList" :key="row.useYm">
                    <td class="usage-ym">{{ fnFormatYm(row.useYm) }}</td>
                    <td class="usage-num">
                      {{ (row.callCnt ?? 0).toLocaleString() }}
                    </td>
                    <td class="usage-num" :title="fnRawLabel(row.inputTokens)">
                      {{ fnFormatMan(row.inputTokens) }}
                    </td>
                    <td class="usage-num" :title="fnRawLabel(row.outputTokens)">
                      {{ fnFormatMan(row.outputTokens) }}
                    </td>
                    <td
                      class="usage-num usage-total"
                      :title="fnRawLabel(row.totalTokens)"
                    >
                      {{ fnFormatMan(row.totalTokens) }}
                    </td>
                  </tr>
                </template>
              </tbody>
            </table>
          </div>

          <p class="usage-guide">
            토큰 수는 만 단위 표기이며(셀에 마우스를 올리면 원시 값),
            합계(입력+출력)가 월 한도 판정 기준입니다. 이력은 최근 24개월까지,
            사용량 집계 기능 도입월(2026-07) 이후부터 표시됩니다.
          </p>
        </div>

        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-primary" @click="$emit('close')">
              닫기
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
/* eslint-disable */
import { ref, defineProps, defineEmits, getCurrentInstance, onMounted } from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";

// 대상 회사 정보(Platform_03 행 전달)
const props = defineProps({
  cmpnyCd: { type: String, required: true },
  cmpnyNm: { type: String, default: "" },
});
const emit = defineEmits(["close"]);

const { proxy } = getCurrentInstance();

// =========================== Ref ===========================
const usageList = ref([]); // [{ useYm, callCnt, inputTokens, outputTokens, totalTokens }]
const loading = ref(false);

// =========================== Methods ===========================
/* YYYYMM → "YYYY.MM" 표기 (형식 밖 값은 원문 유지) */
function fnFormatYm(useYm) {
  const ym = String(useYm ?? "");
  if (ym.length !== 6) return ym;
  return `${ym.slice(0, 4)}.${ym.slice(4)}`;
}

/* 원시 토큰 수 → 만 단위 표기(Platform_03 fnFormatMan 과 동일 규칙) */
function fnFormatMan(tokens) {
  const man = (tokens ?? 0) / 10000;
  return (Math.round(man * 10) / 10).toFixed(1).replace(/\.0$/, "") + "만";
}

/* 셀 title 용 원시 값 라벨 */
function fnRawLabel(tokens) {
  return `${(tokens ?? 0).toLocaleString()} 토큰`;
}

/* 월별 이력 조회 — GET /platformApi/customer/token-usage-lists */
onMounted(async () => {
  loading.value = true;
  try {
    const response = await axios.get("/platformApi/customer/token-usage-lists", {
      params: { cmpnyCd: props.cmpnyCd },
    });
    if (response.status === 200) {
      usageList.value = response.data?.usageList || [];
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
    emit("close");
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.usage-container {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: 1.2rem;
  max-width: 520px;
  margin: 0 auto;
}

/* 대상 회사 요약 박스 (AiTokenQuotaPop .quota-summary 전례) */
.usage-summary {
  display: flex;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  font-size: 0.85rem;
  background: var(--color-surface-muted, #f9fafb);
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius, 10px);
}
.usage-summary-label {
  flex: 0 0 3rem;
  color: var(--color-text-muted);
}
.usage-summary-value {
  color: var(--color-text);
  font-weight: 600;
}

/* 이력 표 — 24행 상한이라 팝업 내부 세로 스크롤 바운딩(modal-popup-guide) */
.usage-table-box {
  max-height: 46vh;
  overflow-y: auto;
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius, 10px);
}
.usage-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.83rem;
}
.usage-table th {
  position: sticky;
  top: 0;
  padding: 0.45rem 0.6rem;
  text-align: left;
  font-weight: 600;
  color: var(--color-text-muted);
  background: var(--color-surface-muted, #f9fafb);
  border-bottom: 1px solid var(--color-border);
}
.usage-table td {
  padding: 0.4rem 0.6rem;
  color: var(--color-text);
  border-bottom: 1px solid var(--color-border);
}
.usage-table tbody tr:last-child td {
  border-bottom: none;
}
.usage-num {
  text-align: right;
}
.usage-total {
  font-weight: 700;
}
.usage-ym {
  font-family: "D2Coding", Consolas, monospace;
}
.usage-empty {
  text-align: center;
  color: var(--color-text-muted);
  padding: 1rem 0;
}

/* 안내문 (AiTokenQuotaPop .quota-guide 전례) */
.usage-guide {
  margin: 0;
  font-size: var(--btn-font-sm, 11px);
  color: var(--color-text-muted, #4b5563);
  background: var(--color-surface-muted, #f9fafb);
  border-radius: var(--btn-radius, 8px);
  padding: 0.5rem 0.75rem;
  line-height: 1.5;
}
</style>
