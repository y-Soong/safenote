<!--
  LeavePromotionExcelPop.vue — 2차 촉진 엑셀 업로드 결과/실패행 안내 팝업 (관리자 웹, 신규)
  - 작업 ID: prafta-com-008-A-8 (UI 명세: UI-web-008-A-1)
  - 정책 출처: 작업지시서 §3-4(엑셀 업/다운로드, 행=사용자-날짜, 실패행 2시트 = prafta-052 패턴)
  - 참조 패턴: views/user/User_01.vue (엑셀 업로드 진행/실패행), prafta-052(시트1 양식+원본 / 시트2 사유)
  - planner 라운드: template + scoped style 완성, script 는 props/emits/ref 선언 + TODO.
  - developer 라운드:
      · 업로드 결과(성공/실패행)를 props 또는 내부 호출로 받아 요약 표시.
      · [실패행 다운로드] → 2시트 xlsx(시트1 양식+원본 16컬럼류 / 시트2 사유). PII 는 서버에서 AES-GCM 처리.
-->
<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div class="modal-content-narrow lpe-modal">
        <div class="modal-header">
          <span>엑셀 업로드 결과</span>
          <button class="icon-button" @click="$emit('close')">✕</button>
        </div>

        <div class="modal-body lpe-body">
          <!-- 처리 중 -->
          <div v-if="isProcessing" class="lpe-progress">
            <p class="lpe-progress__title">엑셀 업로드 처리 중...</p>
            <div class="lpe-progress__bar">
              <span
                class="lpe-progress__fill"
                :style="{ width: progressPct + '%' }"
              />
            </div>
            <p class="lpe-progress__pct">{{ progressPct }}%</p>
          </div>

          <!-- 결과 요약 -->
          <template v-else-if="result">
            <div class="lpe-summary">
              <span class="lpe-summary__ok"
                >성공 <strong>{{ result.successCount }}건</strong></span
              >
              <span class="lpe-summary__fail"
                >실패 <strong>{{ failCount }}건</strong></span
              >
            </div>

            <p v-if="failCount === 0" class="lpe-done">
              모든 행이 정상 처리되었습니다.
            </p>

            <!-- 실패행 안내 + 다운로드 (prafta-052 2시트) -->
            <div v-else class="lpe-fails">
              <p class="lpe-fails__note">
                실패한 행은 양식(시트1)과 사유(시트2)를 담은 엑셀로 내려받아
                수정 후 다시 업로드하세요.
              </p>
              <button
                class="lpe-fails__dl"
                :disabled="isDownloading"
                @click="onDownloadFails"
              >
                {{ isDownloading ? "준비 중..." : "실패행 엑셀 다운로드" }}
              </button>
            </div>
          </template>

          <p v-else class="lpe-empty">업로드 결과가 없습니다.</p>
        </div>

        <div class="modal-footer lpe-footer">
          <button class="btn-primary" @click="$emit('close')">확인</button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import {
  ref,
  computed,
  getCurrentInstance,
  defineProps,
  defineEmits,
} from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";

const { proxy } = getCurrentInstance();

const props = defineProps({
  // 업로드 결과 ({ successCount, failItems:[{ sourceRow, reason }], failsToken })
  //   - failItems[].sourceRow: 원본 행(양식 재구성용, nullable=그리드 경로 회귀방지 — prafta-052)
  //   - failsToken: 서버가 발급한 2시트 다운로드 토큰(AES-GCM 보관분 복호화 권한)
  result: { type: Object, default: null },
  // 비동기 처리 진행 여부/율 (동기 처리면 항상 false)
  processing: { type: Boolean, default: false },
});

const emit = defineEmits(["close"]);
void emit;

const isProcessing = computed(() => props.processing);
const progressPct = ref(0);
const isDownloading = ref(false);

const failCount = computed(() => props.result?.failItems?.length || 0);

// 실패행 2시트 다운로드.
const onDownloadFails = async () => {
  if (isDownloading.value) return;
  const token = props.result?.failsToken;
  if (!token) {
    await proxy.$alert("다운로드 토큰이 없어 실패행을 받을 수 없습니다.");
    return;
  }
  isDownloading.value = true;
  try {
    // GET /webApi/leavepromo01/excel/fails?token= → 2시트 xlsx(시트1 양식+원본, 시트2 사유).
    //   서버가 토큰 소유자(cmpny+user) 재검증 후 AES-GCM 보관분을 복호화해 스트림(prafta-052 패턴).
    const response = await axios.get("/webApi/leavepromo01/excel/fails", {
      params: { token },
      responseType: "blob",
    });
    if (response.status === 200) {
      const blob = new Blob([response.data], {
        type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = "연차일괄지정_실패행.xlsx";
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "실패행 다운로드 중 오류가 발생했습니다.")
    );
  } finally {
    isDownloading.value = false;
  }
};
</script>

<style scoped>
.lpe-modal {
  width: 420px;
  max-width: 92vw;
}
.lpe-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.lpe-progress {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 0;
}
.lpe-progress__title {
  margin: 0;
  font-size: 13px;
  color: var(--color-text);
}
.lpe-progress__bar {
  width: 100%;
  height: 8px;
  background: var(--color-border);
  border-radius: 999px;
  overflow: hidden;
}
.lpe-progress__fill {
  display: block;
  height: 100%;
  background: var(--color-primary);
}
.lpe-progress__pct {
  margin: 0;
  font-size: 12px;
  color: var(--color-text-muted);
}

.lpe-summary {
  display: flex;
  gap: 16px;
  font-size: 14px;
}
.lpe-summary__ok strong {
  color: var(--color-primary);
}
.lpe-summary__fail strong {
  color: var(--color-danger);
}

.lpe-done {
  margin: 0;
  font-size: 13px;
  color: var(--color-text);
}

.lpe-fails {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.lpe-fails__note {
  margin: 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--color-text-muted);
}
.lpe-fails__dl {
  height: var(--btn-height-lg, 32px);
  padding: 0 12px;
  border: 1px solid var(--color-primary);
  border-radius: var(--btn-radius, 8px);
  background: var(--color-surface);
  color: var(--color-primary);
  font-size: 12px;
  cursor: pointer;
}
.lpe-fails__dl:disabled {
  border-color: var(--color-border);
  color: var(--color-text-muted);
  cursor: default;
}

.lpe-empty {
  margin: 0;
  padding: 24px 0;
  text-align: center;
  font-size: 13px;
  color: var(--color-text-muted);
}

.lpe-footer {
  display: flex;
  justify-content: flex-end;
}
.btn-primary {
  height: var(--btn-height-lg, 32px);
  padding: 0 var(--btn-padding-lg, 13px);
  border: 0;
  border-radius: var(--btn-radius, 8px);
  background: var(--color-primary);
  color: var(--color-surface);
  font-size: var(--btn-font, 11px);
  cursor: pointer;
}
</style>
