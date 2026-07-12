<!--
  LeavePromotionDesignatePop.vue — 2차 촉진 개별 사용자 직권 지정 팝업 (관리자 웹, 신규)
  - 작업 ID: prafta-com-008-A-8 (UI 명세: UI-web-008-A-1)
  - 정책 출처: 작업지시서 §3-1·§3-2(직권 지정 = 2차/회사직권 등록 + PUSH), §3-2(근로자 이동은 C 동의흐름)
  - 참조 패턴: views/attd/popup/LeaveApplyPop.vue (modal-overlay > modal-content > header/body, emit close)
  - planner 라운드: template + scoped style 완성, script 는 props/emits/ref 선언 + TODO.
  - developer 라운드: 날짜 다건(1일 단위) 입력 → POST /webApi/leavepromo01/designate { userCd, ymds }
      성공 시 PUSH(서버 적재) 안내 + emit('done') 로 부모 재조회.
-->
<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div class="modal-content-narrow lpd-modal">
        <div class="modal-header">
          <span>2차 촉진 지정 — {{ props.userNm }}</span>
          <button class="icon-button" @click="$emit('close')">✕</button>
        </div>

        <div class="modal-body lpd-body">
          <!-- 대상 요약 -->
          <p class="lpd-summary">
            직권 지정 대상 잔여
            <strong>{{ props.targetDays }}일</strong>
            · 선택
            <strong :class="{ 'lpd-over': isOver }"
              >{{ selectedYmds.length }}일</strong
            >
          </p>
          <p v-if="isOver" class="lpd-warn">
            대상 잔여({{ props.targetDays }}일)를 초과했습니다.
          </p>

          <!-- 날짜 행 목록 -->
          <ul v-if="selectedYmds.length > 0" class="lpd-list">
            <li v-for="ymd in sortedYmds" :key="ymd" class="lpd-row">
              <span>{{ formatYmd(ymd) }}</span>
              <button class="lpd-del" @click="onRemove(ymd)">삭제</button>
            </li>
          </ul>
          <p v-else class="lpd-empty">
            아래에서 지정할 날짜를 추가하세요. (1일 단위)
          </p>

          <!-- 날짜 키인 추가 -->
          <div class="lpd-add">
            <CalendarSrch v-model="keyinYmd" class="lpd-add-input" />
            <button class="lpd-add-btn" :disabled="!keyinYmd" @click="onAdd">
              + 추가
            </button>
          </div>

          <p class="lpd-note">
            지정된 날짜는 회사 직권(2차) 연차로 등록되며 근로자에게 통보됩니다.
            근로자는 만료 이내 다른 날로 이동만 가능합니다(취소 불가).
          </p>
        </div>

        <div class="modal-footer lpd-footer">
          <button class="btn-ghost" @click="$emit('close')">취소</button>
          <button
            class="btn-primary"
            :disabled="isSaving || selectedYmds.length === 0 || isOver"
            @click="onConfirm"
          >
            {{ isSaving ? "지정 중..." : "직권 지정" }}
          </button>
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
import CalendarSrch from "@/components/common/CalendarSrch.vue";

const { proxy } = getCurrentInstance();

const props = defineProps({
  userCd: { type: String, default: "" },
  userNm: { type: String, default: "" },
  // 직권 지정 cap = 실시간 grant 잔여(unusedDays). stage2TargetDays 스냅샷 아님(H1).
  targetDays: { type: Number, default: 0 },
});

const emit = defineEmits(["close", "done"]);

// 선택 날짜 (YYYYMMDD)
const selectedYmds = ref([]);
// 키인 임시값 (YYYY-MM-DD)
const keyinYmd = ref("");
const isSaving = ref(false);

const sortedYmds = computed(() => [...selectedYmds.value].sort());
const isOver = computed(() => selectedYmds.value.length > props.targetDays);

const formatYmd = (ymd) => {
  if (!ymd || ymd.length !== 8) return ymd || "";
  return `${ymd.slice(0, 4)}-${ymd.slice(4, 6)}-${ymd.slice(6, 8)}`;
};

const onRemove = (ymd) => {
  selectedYmds.value = selectedYmds.value.filter((d) => d !== ymd);
};

// 날짜 키인 추가 — 중복만 UI 검증(휴일/만료/근무일 가용성은 서버 권위 검증).
const onAdd = () => {
  if (!keyinYmd.value) return;
  const ymd = keyinYmd.value.replace(/-/g, "");
  if (selectedYmds.value.includes(ymd)) {
    proxy.$alert("이미 선택한 날짜입니다.");
    return;
  }
  selectedYmds.value = [...selectedYmds.value, ymd];
  keyinYmd.value = "";
};

// 직권 지정 확정.
const onConfirm = async () => {
  if (isSaving.value || selectedYmds.value.length === 0 || isOver.value) return;
  isSaving.value = true;
  try {
    // PromotionDesignateRequest 계약: { targetUserCd, dates }. 대상 사업장/부서·권한은 서버 재조회로 강제(IDOR).
    // 서버가 2차/회사직권 등록 + PUSH 적재. 부분 성공(지정/스킵/실패) 집계를 안내한다.
    const { data } = await axios.post("/webApi/leavepromo01/designate", {
      targetUserCd: props.userCd,
      dates: selectedYmds.value,
    });
    // PromotionDesignateResultResponse: { designatedDates, skippedDates, failedDates }
    const designated = Array.isArray(data?.designatedDates)
      ? data.designatedDates
      : [];
    const skipped = Array.isArray(data?.skippedDates) ? data.skippedDates : [];
    const failed = Array.isArray(data?.failedDates) ? data.failedDates : [];
    let msg = `2차 촉진 직권 지정이 완료되었습니다. (지정 ${designated.length}건`;
    if (skipped.length > 0) msg += `, 중복 ${skipped.length}건`;
    if (failed.length > 0) msg += `, 실패 ${failed.length}건`;
    msg += ") 근로자에게 통보됩니다.";
    if (failed.length > 0) {
      msg +=
        "\n실패한 날짜는 휴일/마감/근무일 아님 등으로 등록되지 않았습니다.";
    }
    await proxy.$alert(msg);
    emit("done");
    emit("close");
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "직권 지정 중 오류가 발생했습니다.")
    );
  } finally {
    isSaving.value = false;
  }
};
</script>

<style scoped>
.lpd-modal {
  width: 420px;
  max-width: 92vw;
}
.lpd-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.lpd-summary {
  margin: 0;
  font-size: 13px;
  color: var(--color-text);
}
.lpd-summary strong {
  color: var(--color-text-strong);
}
.lpd-over {
  color: var(--color-danger);
}
.lpd-warn {
  margin: 0;
  font-size: 12px;
  color: var(--color-danger);
}

.lpd-list {
  list-style: none;
  margin: 0;
  padding: 0;
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius, 10px);
  overflow: hidden;
}
.lpd-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  font-size: 13px;
  color: var(--color-text);
  border-bottom: 1px solid var(--color-border);
}
.lpd-row:last-child {
  border-bottom: 0;
}
.lpd-del {
  background: transparent;
  border: 0;
  color: var(--color-danger);
  font-size: 12px;
  cursor: pointer;
}
.lpd-empty {
  margin: 0;
  font-size: 12px;
  color: var(--color-text-muted);
}

.lpd-add {
  display: flex;
  gap: 8px;
}
/* 네이티브 date input → CalendarSrch 교체. 레이아웃/사이즈 유지 */
.lpd-add-input {
  flex: 1;
}
.lpd-add-input :deep(.calendar-input) {
  width: 100%;
  height: var(--btn-height-lg, 32px);
  border: 1px solid var(--color-border-strong);
  border-radius: var(--input-radius, 10px);
  padding: 0 10px;
  font-size: 13px;
}
.lpd-add-btn {
  flex: 0 0 auto;
  height: var(--btn-height-lg, 32px);
  padding: 0 12px;
  border: 1px solid var(--color-primary);
  border-radius: var(--btn-radius, 8px);
  background: var(--color-surface);
  color: var(--color-primary);
  font-size: 12px;
  cursor: pointer;
}
.lpd-add-btn:disabled {
  border-color: var(--color-border);
  color: var(--color-text-muted);
  cursor: default;
}

.lpd-note {
  margin: 0;
  font-size: 11px;
  line-height: 1.5;
  color: var(--color-text-muted);
}

.lpd-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
.btn-ghost {
  height: var(--btn-height-lg, 32px);
  padding: 0 var(--btn-padding-lg, 13px);
  border: 1px solid var(--color-border-strong);
  border-radius: var(--btn-radius, 8px);
  background: var(--color-surface);
  color: var(--color-text);
  font-size: var(--btn-font, 11px);
  cursor: pointer;
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
.btn-primary:disabled {
  background: var(--color-border);
  color: var(--color-text-muted);
  cursor: default;
}
</style>
