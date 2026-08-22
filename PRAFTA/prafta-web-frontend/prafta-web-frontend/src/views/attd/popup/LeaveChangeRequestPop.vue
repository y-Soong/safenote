<!--
  LeaveChangeRequestPop.vue — 관리자 연차 변경(이동)/삭제 발의 팝업 (prafta-com-008-C-3)
  유형: frontend-component (웹 관리자 팝업)
  연결 작업: PRAFTA-{C-4-web}
  참조 패턴: views/notice/popup/ArchiveCreatePop.vue (modal-popup-guide 본문 스크롤),
            views/attd/popup/AttdDayDetailPop.vue
  역할 분담: 골격 = 입력 폼 구조 + 유형 토글 + 사유 필수 UI. developer = 검증/제출 API.
  ※ 발의는 "이미 등록된 연차일(LEAVE_ID)"에 대해서만. 최초 등록(셀 신규)은 동의 불요 → 본 팝업 대상 아님.
-->
<template>
  <div class="modal-overlay" @click.self="onClose">
    <div class="modal-content lcr-pop">
      <header class="modal-header">
        <h2 class="modal-title">연차 변경/삭제 요청</h2>
        <button
          type="button"
          class="modal-close"
          aria-label="닫기"
          @click="onClose"
        >
          ×
        </button>
      </header>

      <div class="modal-body lcr-body">
        <!-- [부분휴가진입점-02] 다건 대상 선택 — §5-2 확정: 팝업 상단 목록에서 1건 선택 후 진행.
             pending 건은 비활성 톤+배지(사유 가시화)·클릭 시 안내(disabled 숨은차단 금지). -->
        <section v-if="candidates && candidates.length > 1" class="lcr-section">
          <h3 class="lcr-section__title">
            대상 선택 <span class="lcr-req">*</span>
          </h3>
          <ul class="lcr-candidate-list">
            <li
              v-for="(c, i) in candidates"
              :key="c.leaveId"
              class="lcr-candidate"
              :class="{
                'is-selected': selectedCandidateIdx === i,
                'is-pending': c.pending,
              }"
            >
              <!-- 클릭 가드 — pending 이면 안내 후 미선택, 아니면 선택 적용 -->
              <label
                class="lcr-candidate__label"
                @click.prevent="onSelectCandidate(i)"
              >
                <input
                  type="radio"
                  name="lcr-candidate"
                  :value="i"
                  :checked="selectedCandidateIdx === i"
                  :disabled="c.pending"
                />
                <span class="lcr-candidate__main">
                  {{ candidateMainLabel(c) }}
                </span>
                <span v-if="c.pending" class="lcr-badge-pending"
                  >결재 진행 중</span
                >
              </label>
              <p v-if="candidateTimeLabel(c)" class="lcr-candidate__sub">
                {{ candidateTimeLabel(c) }}
              </p>
            </li>
          </ul>
        </section>

        <!-- 대상 연차일 정보 (읽기 전용) — activeTarget: 종일 경로면 target 그대로(종전 렌더 동일),
             부분휴가 경로면 선택된 후보([부분휴가진입점-02]). -->
        <section class="lcr-section">
          <h3 class="lcr-section__title">대상 연차</h3>
          <dl class="lcr-target">
            <div>
              <dt>사용자</dt>
              <dd>{{ activeTarget?.userNm }}</dd>
            </div>
            <div>
              <dt>연차일</dt>
              <dd>{{ activeTarget?.startDate }}</dd>
            </div>
            <div>
              <dt>연차종류</dt>
              <dd>{{ activeTarget?.leaveNm }}</dd>
            </div>
            <!-- [부분휴가진입점-02] 사용단위·시각범위 — AttdDayDetailPop 카드 문구와 일치 -->
            <div v-if="candidates && candidates.length">
              <dt>사용단위</dt>
              <dd>{{ unitLabel(activeTarget) }}</dd>
            </div>
            <div
              v-if="
                candidates &&
                candidates.length &&
                candidateTimeLabel(activeTarget)
              "
            >
              <dt>시간</dt>
              <dd>{{ candidateTimeLabel(activeTarget) }}</dd>
            </div>
            <div>
              <dt>촉진단계</dt>
              <dd>{{ activeTarget?.promotionStageNm || "비촉진" }}</dd>
            </div>
          </dl>
        </section>

        <!-- 요청 유형 -->
        <section class="lcr-section">
          <h3 class="lcr-section__title">요청 유형</h3>
          <div class="lcr-radio-group">
            <label class="lcr-radio">
              <input type="radio" value="MOVE" v-model="reqType" />
              이동(변경)
            </label>
            <label class="lcr-radio">
              <input type="radio" value="DELETE" v-model="reqType" />
              삭제(근무 복귀)
            </label>
          </div>
        </section>

        <!-- 이동 대상일 (MOVE 시에만) -->
        <section v-if="reqType === 'MOVE'" class="lcr-section">
          <h3 class="lcr-section__title">이동 대상일</h3>
          <!-- TODO(developer): 캘린더 컴포넌트 바인딩. 만료일(AVAIL_TO_DATE) 이내 + 마감월 제외는 서버 강제. -->
          <CalendarSrch v-model="moveTargetDate" />
          <p class="lcr-hint">
            연차 만료일 이내로만 이동할 수 있습니다. 대상일에 같은 법정연차가
            있으면 거부됩니다.
          </p>
        </section>

        <!-- [부분휴가진입점-02] MOVE 위치선택 — 부분휴가 대상 + MOVE 일 때만(종일 무회귀).
             기본 미지정=종전 자동 배치. 서버 ATTD_400_208 이 최종(클라는 형식만). -->
        <section
          v-if="reqType === 'MOVE' && (isHalfTarget || isTimedTarget)"
          class="lcr-section"
        >
          <h3 class="lcr-section__title">이동 위치</h3>

          <!-- 반차(01): 파트 3택. 라벨 = AttdDayDetailPop LC_MOVE_HALF_PART_NM 동일 문구 -->
          <div v-if="isHalfTarget" class="lcr-radio-group">
            <label class="lcr-radio">
              <input type="radio" value="" v-model="moveHalfPart" />
              미지정(자동)
            </label>
            <label class="lcr-radio">
              <input type="radio" value="START" v-model="moveHalfPart" />
              시작 기준(늦게 출근)
            </label>
            <label class="lcr-radio">
              <input type="radio" value="END" v-model="moveHalfPart" />
              종료 기준(일찍 퇴근)
            </label>
          </div>

          <!-- 시간차(02/03/04): 시작 시각 HHMM. 빈값=미지정 -->
          <div v-if="isTimedTarget" class="lcr-time-row">
            <span class="lcr-time-lab">시작 시각</span>
            <input
              v-model="moveStartTime"
              type="text"
              class="lcr-time-input"
              inputmode="numeric"
              maxlength="4"
              placeholder="HHMM"
            />
          </div>

          <p class="lcr-hint">
            미지정 시 자동으로 배치됩니다. 근무시간 범위·기존 휴가와의 겹침은
            서버에서 최종 확인됩니다.
          </p>
        </section>

        <!-- 요청 사유 (필수) -->
        <section class="lcr-section">
          <h3 class="lcr-section__title">
            요청 사유 <span class="lcr-req">*</span>
          </h3>
          <textarea
            v-model="reason"
            class="lcr-textarea"
            rows="3"
            maxlength="500"
            placeholder="변경/삭제 사유를 입력하세요 (필수)"
          ></textarea>
        </section>
      </div>

      <!-- F-10 규약: 왼쪽=진행/확정(요청, primary), 오른쪽=이탈(취소) -->
      <footer class="modal-footer lcr-footer">
        <button
          type="button"
          class="btn btn-primary"
          :disabled="!canSubmit || submitting"
          @click="onSubmit"
        >
          요청
        </button>
        <button type="button" class="btn btn-ghost" @click="onClose">
          취소
        </button>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, getCurrentInstance } from "vue";
import CalendarSrch from "@/components/common/CalendarSrch.vue";
import axios from "@/api/axios";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { formatLeaveMinutes } from "@/utils/leaveFormat";

// [부분휴가진입점-02] props 확장 — target(종일, 종전 계약 불변) + candidates(부분휴가 경로).
//   candidates 항목: { leaveId, userCd, userNm, startDate, leaveNm, useUnitType, useUnitNm,
//                      startTime, endTime, leaveMinutes, pending, promotionStageNm }
const props = defineProps({
  // 종일 경로: { leaveId, userCd, userNm, startDate, leaveNm, promotionStageNm }
  target: { type: Object, default: null },
  candidates: { type: Array, default: null },
});
const emit = defineEmits(["close", "submitted"]);

const { proxy } = getCurrentInstance();

// ── 입력 상태 ────────────────────────────────────────────────────────────
const reqType = ref("MOVE");
const moveTargetDate = ref("");
const reason = ref("");
const submitting = ref(false);

// ── 부분휴가 확장 상태 ([부분휴가진입점-02]) ─────────────────────────────
// 다건 선택 인덱스. 후보 1건이면 자동 선택(선택 목록 미표시), 다건이면 사용자가 1건 선택.
const selectedCandidateIdx = ref(
  props.candidates && props.candidates.length === 1 ? 0 : null
);
const moveHalfPart = ref(""); // 반차 MOVE 파트: ''=미지정 / 'START' / 'END'
const moveStartTime = ref(""); // 시간차 MOVE 시작 시각 HHMM: ''=미지정

// 실제 발의 대상 — 종일 경로면 target 그대로(종전 동작 불변), 부분휴가 경로면 선택된 후보.
const activeTarget = computed(() => {
  if (!props.candidates || !props.candidates.length) return props.target;
  return selectedCandidateIdx.value != null
    ? props.candidates[selectedCandidateIdx.value]
    : null;
});

// 시간차(02/03/04) 여부 — 종일 target 은 useUnitType 미보유 → 항상 false(무회귀).
const isTimedUnit = (t) => ["02", "03", "04"].includes(t?.useUnitType);
const isHalfTarget = computed(() => activeTarget.value?.useUnitType === "01");
const isTimedTarget = computed(() => isTimedUnit(activeTarget.value));

// pending 후보 클릭 가드 — Attd_05 종일 진입 차단과 동일 문구(단일 출처, 신조어 금지).
const onSelectCandidate = (i) => {
  const c = props.candidates?.[i];
  if (!c) return;
  if (c.pending) {
    proxy.$alert(
      "결재가 진행 중인 연차입니다.\n승인 전에는 변경·삭제할 수 없으며, 신청 취소 또는 결재 반려로 처리해 주세요."
    );
    return;
  }
  if (selectedCandidateIdx.value !== i) {
    selectedCandidateIdx.value = i;
    // 대상이 바뀌면 위치선택 입력 초기화(단위가 달라질 수 있음 — 미지정=종전 자동 배치).
    moveHalfPart.value = "";
    moveStartTime.value = "";
  }
};

// ── 표기 헬퍼 — AttdDayDetailPop 카드 문구 규칙과 일치(표류 금지) ────────
// "0930" → "09:30"
const fmtTime = (hhmm) => {
  if (!hhmm) return "";
  const v = String(hhmm);
  if (v.length < 4) return v;
  return `${v.slice(0, 2)}:${v.slice(2, 4)}`;
};

// "HHmm" → 분 (00:00 기준). 잘못된 값이면 null.
const hhmmToMin = (hhmm) => {
  if (!hhmm || String(hhmm).length !== 4) return null;
  const h = parseInt(String(hhmm).slice(0, 2), 10);
  const m = parseInt(String(hhmm).slice(2, 4), 10);
  if (Number.isNaN(h) || Number.isNaN(m)) return null;
  if (h < 0 || h > 23 || m < 0 || m > 59) return null;
  return h * 60 + m;
};

// 시간차 시각범위 병기 — "10:00~11:30 (1시간 30분)" (AttdDayDetailPop hourlyRangeLabel 규칙).
const hourlyRangeLabel = (startTime, endTime) => {
  const range = `${fmtTime(startTime)}~${fmtTime(endTime)}`;
  const s = hhmmToMin(startTime);
  const e = hhmmToMin(endTime);
  if (s == null || e == null || e <= s) return range;
  return `${range} (${formatLeaveMinutes(e - s)})`;
};

// 사용단위 라벨 — 시간차면 '시간차 ' 접두(이미 접두 보유 시 미중복. AttdDayDetailPop 접두 규칙).
const unitLabel = (t) => {
  if (!t) return "";
  return t.useUnitNm
    ? isTimedUnit(t) && !t.useUnitNm.startsWith("시간차")
      ? `시간차 ${t.useUnitNm}`
      : t.useUnitNm
    : "연차";
};

// 시간 표기 — 시간차=시각범위(+사용 분), 반차=사용 분. 해당 없으면 ''(행/부제 미표시).
const candidateTimeLabel = (t) => {
  if (!t) return "";
  if (isTimedUnit(t)) return hourlyRangeLabel(t.startTime, t.endTime);
  if (t.useUnitType === "01" && t.leaveMinutes != null)
    return formatLeaveMinutes(t.leaveMinutes);
  return "";
};

// 선택 목록 항목 라벨 — "연차종류 · 단위"
const candidateMainLabel = (c) => `${c.leaveNm} · ${unitLabel(c)}`;

// 단순 입력 검증(필수값·형식)만 화면에서 처리. 만료일/마감/충돌은 서버 강제.
const canSubmit = computed(() => {
  if (!reason.value.trim()) return false;
  if (reqType.value === "MOVE" && !moveTargetDate.value) return false;
  // [부분휴가진입점-02] 다건 미선택 시 비활성(사유는 선택 목록 * 필수 표기로 가시화).
  if (props.candidates && props.candidates.length && !activeTarget.value)
    return false;
  // 시간차 MOVE 시작 시각은 형식(숫자 4자리)만 클라 검사 — 겹침/범위는 서버 ATTD_400_208 최종.
  if (
    reqType.value === "MOVE" &&
    isTimedTarget.value &&
    moveStartTime.value &&
    !/^\d{4}$/.test(moveStartTime.value)
  )
    return false;
  return true;
});

// CalendarSrch v-model 값(YYYY-MM-DD 등) → YYYYMMDD 정규화
const toYmd8 = (v) =>
  v
    ? String(v)
        .replace(/[^0-9]/g, "")
        .slice(0, 8)
    : "";

const onClose = () => emit("close");

// POST /webApi/attd13/change-requests
//   body(대문자 키) = { TARGET_LEAVE_ID, REQ_TYPE, MOVE_TARGET_DATE(MOVE만),
//                      MOVE_TARGET_HALF_PART(반차 MOVE·미지정 null),
//                      MOVE_TARGET_START_TIME(시간차 MOVE·미지정 null), REQ_REASON }
//   식별/스코프/만료/충돌/마감/중복요청은 서버 JWT + 재검증(body 비신뢰).
const onSubmit = async () => {
  if (!canSubmit.value || submitting.value) return;
  const t = activeTarget.value;
  if (!t?.leaveId) {
    await proxy.$alert("대상 연차 정보가 없습니다.");
    return;
  }
  submitting.value = true;
  try {
    await axios.post("/webApi/attd13/change-requests", {
      TARGET_LEAVE_ID: t.leaveId,
      REQ_TYPE: reqType.value,
      MOVE_TARGET_DATE:
        reqType.value === "MOVE" ? toYmd8(moveTargetDate.value) : null,
      // [부분휴가진입점-02] MOVE 위치선택 — 미지정(null)=종전 자동 배치 경로.
      //   종일·DELETE 는 항상 null(서버 blank→null 정규화 보유 — 구계약과 무해 정합).
      MOVE_TARGET_HALF_PART:
        reqType.value === "MOVE" && isHalfTarget.value && moveHalfPart.value
          ? moveHalfPart.value
          : null,
      MOVE_TARGET_START_TIME:
        reqType.value === "MOVE" && isTimedTarget.value && moveStartTime.value
          ? moveStartTime.value
          : null,
      REQ_REASON: reason.value.trim(),
    });
    await proxy.$alert("변경 요청이 등록되었습니다.");
    emit("submitted");
  } catch (err) {
    await proxy.$alert(resolveApiErrorMessage(err, getMessage(MSG.SAVE_ERROR)));
  } finally {
    submitting.value = false;
  }
};
</script>

<style scoped>
.lcr-pop {
  width: 480px;
  max-width: 92vw;
  max-height: 84vh;
  /* 기본 modal-content 의 20px 패딩 제거 → 헤더/본문/푸터가 박스 끝에 밀착.
     overflow:hidden 으로 헤더/푸터 모서리를 16px 라운드에 맞춰 클립. */
  padding: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.lcr-body {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--card-padding, 20px);
  padding: var(--card-padding, 20px);
}

.lcr-section__title {
  margin: 0 0 var(--space-sm, 8px);
  font-size: var(--btn-font, 11px);
  font-weight: 600;
  color: var(--color-text-strong);
}

.lcr-req {
  color: var(--color-danger);
}

.lcr-target {
  display: grid;
  gap: var(--space-xs, 4px);
  padding: var(--space-sm, 8px);
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
}
.lcr-target div {
  display: flex;
  gap: var(--space-sm, 8px);
  font-size: var(--btn-font, 11px);
}
.lcr-target dt {
  width: 64px;
  color: var(--color-text-muted);
}
.lcr-target dd {
  margin: 0;
  color: var(--color-text-strong);
}

.lcr-radio-group {
  display: flex;
  gap: var(--card-padding, 20px);
}
.lcr-radio {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs, 4px);
  font-size: var(--btn-font, 11px);
  color: var(--color-text);
}

.lcr-textarea {
  width: 100%;
  border: 1px solid var(--color-border-strong);
  border-radius: var(--input-radius);
  padding: var(--space-sm, 8px);
  font-family: inherit;
  font-size: var(--btn-font, 11px);
  resize: vertical;
}

.lcr-hint {
  margin: var(--space-xs, 4px) 0 0;
  font-size: var(--btn-font-sm, 11px);
  color: var(--color-warning-text);
}

.lcr-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-sm, 8px);
  padding: var(--space-sm, 8px) var(--card-padding, 20px);
  border-top: 1px solid var(--color-border);
}

/* [부분휴가진입점-02] 다건 대상 선택 목록 */
.lcr-candidate-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: var(--space-xs, 4px);
}
.lcr-candidate {
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
  padding: var(--space-sm, 8px);
  background: var(--color-bg);
}
.lcr-candidate.is-selected {
  border-color: var(--color-primary);
}
.lcr-candidate.is-pending {
  opacity: 0.6; /* 비활성 톤 — 배지로 사유 병기(숨은 차단 금지) */
}
.lcr-candidate__label {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs, 4px);
  font-size: var(--btn-font, 11px);
  color: var(--color-text-strong);
}
.lcr-candidate__sub {
  margin: var(--space-xs, 4px) 0 0;
  font-size: var(--btn-font-sm, 11px);
  color: var(--color-text-muted);
}
.lcr-badge-pending {
  font-size: var(--btn-font-sm, 11px);
  color: var(--color-warning-text);
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
  padding: 0 var(--space-xs, 4px);
}

/* [부분휴가진입점-02] 시간차 MOVE 시작 시각 입력 */
.lcr-time-row {
  display: inline-flex;
  align-items: center;
  gap: var(--space-sm, 8px);
  margin-top: var(--space-sm, 8px);
}
.lcr-time-lab {
  font-size: var(--btn-font, 11px);
  color: var(--color-text-muted);
}
.lcr-time-input {
  width: 72px;
  border: 1px solid var(--color-border-strong);
  border-radius: var(--input-radius);
  padding: var(--space-xs, 4px) var(--space-sm, 8px);
  font-size: var(--btn-font, 11px);
  text-align: center;
}
</style>
