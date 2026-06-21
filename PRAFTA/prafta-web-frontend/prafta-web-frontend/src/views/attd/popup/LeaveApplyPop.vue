<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div class="modal-content-narrow la-modal">
        <div class="modal-header">
          <span>연차 신청</span>
          <button class="icon-button" @click="$emit('close')">✕</button>
        </div>

        <div class="modal-body la-body">
          <div class="la-field">
            <label>연차 타입 <span class="req">*</span></label>
            <BaseSelect v-model="leaveCd">
              <option value="">선택</option>
              <option
                v-for="t in applicableTypes"
                :key="t.leaveCd"
                :value="t.leaveCd"
              >
                {{ t.leaveNm }}
              </option>
            </BaseSelect>
          </div>

          <div class="la-field">
            <label>근무일 <span class="req">*</span></label>
            <CalendarSrch v-model="workYmd" />
          </div>

          <div class="la-field">
            <label>사용 단위 <span class="req">*</span></label>
            <BaseSelect v-model="useUnitType">
              <option
                v-for="u in unitOptions"
                :key="u.systValDCd"
                :value="u.systValDCd"
              >
                {{ u.systValDNm }}
              </option>
            </BaseSelect>
          </div>

          <div v-if="isHourUnit" class="la-field">
            <label>시간대 <span class="req">*</span></label>
            <div class="la-time-row">
              <TimeInput v-model="startTime" :minute-step="10" />
              <span class="la-sep">~</span>
              <TimeInput v-model="endTime" :minute-step="10" allow24 />
            </div>
            <p class="la-hint">
              {{ unitGuide }} 단위로 신청하며, 휴게시간을 가로지를 수 없습니다.
            </p>
          </div>

          <!-- prafta-com-011-6 가불(미래 연차 당겨쓰기) 동의 — 시스템 법정 연차 + 가불 가능 + 잔여 부족 시에만 노출 -->
          <div v-if="showBorrowToggle" class="la-field">
            <label class="la-borrow-toggle">
              <input type="checkbox" v-model="borrowAgreed" class="la-borrow-cb" />
              <span class="la-borrow-txt">미래 연차를 당겨 사용(가불)</span>
            </label>

            <!-- 토글 ON 시: 가불 한도/만료/부족분 안내 -->
            <div v-if="borrowAgreed" class="la-borrow-info">
              <div class="la-borrow-row">
                <span class="la-borrow-lbl">가불 가능 한도</span>
                <span class="la-borrow-val">{{ formatDays(borrowQuota) }}일</span>
              </div>
              <div v-if="borrowExpiryDisplay" class="la-borrow-row">
                <span class="la-borrow-lbl">만료(소멸)</span>
                <span class="la-borrow-val">{{ borrowExpiryDisplay }}</span>
              </div>
              <p v-if="borrowDeficitText" class="la-borrow-deficit">
                {{ borrowDeficitText }}
              </p>
              <p class="la-borrow-guide">
                · 결재 승인 후 확정돼요. 미래에 발생할 연차에서 자동 차감됩니다.
              </p>
            </div>
          </div>

          <div class="la-field">
            <label>사유</label>
            <textarea v-model="reason" rows="2" maxlength="500" />
          </div>

          <!-- 결재 필요 타입: 결재라인 구성 -->
          <div v-if="needApproval" class="la-approval">
            <div class="la-approval__head">
              <span>결재라인 구성 <span class="req">*</span></span>
              <select
                v-if="presets.length > 0"
                class="la-preset-sel"
                v-model="selectedPresetId"
                @change="fnApplyPresetSel"
              >
                <option value="">프리셋 선택</option>
                <option
                  v-for="p in presets"
                  :key="p.presetId"
                  :value="p.presetId"
                >
                  {{ p.presetNm }}{{ p.defaultYn === "Y" ? " (기본)" : "" }}
                </option>
              </select>
              <span v-else class="la-preset-empty">
                등록된 프리셋 없음 (사용자관리 &gt; 연차 결재라인 구성)
              </span>
            </div>
            <div class="la-approval__cols">
              <div class="la-pane">
                <div class="la-pane__title">후보</div>
                <div class="la-list">
                  <div
                    v-for="c in candidates"
                    :key="c.userCd"
                    class="la-cand"
                    :class="{ added: inLine(c.userCd) }"
                  >
                    <span>{{ c.userNm }} · {{ c.rankNm || "직급없음" }}</span>
                    <button :disabled="inLine(c.userCd)" @click="fnAdd(c)">
                      추가
                    </button>
                  </div>
                  <div v-if="candidates.length === 0" class="la-empty">
                    후보 없음
                  </div>
                </div>
              </div>
              <div class="la-pane">
                <div class="la-pane__title">결재 순서 ({{ line.length }})</div>
                <div class="la-list">
                  <div v-for="(s, i) in line" :key="s.userCd" class="la-step">
                    <span class="la-step__no">{{ i + 1 }}</span>
                    <span class="la-step__nm">{{ s.userNm }}</span>
                    <button :disabled="i === 0" @click="fnUp(i)">▲</button>
                    <button
                      :disabled="i === line.length - 1"
                      @click="fnDown(i)"
                    >
                      ▼
                    </button>
                    <button class="la-del" @click="fnRemove(i)">✕</button>
                  </div>
                  <div v-if="line.length === 0" class="la-empty">
                    결재자를 추가하세요
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="modal-footer la-footer">
          <button class="btn-cancel" @click="$emit('close')">취소</button>
          <button class="btn-confirm" :disabled="submitting" @click="fnSubmit">
            신청
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
/* eslint-disable */
import {
  ref,
  computed,
  watch,
  onMounted,
  getCurrentInstance,
  defineProps,
  defineEmits,
  defineOptions,
} from "vue";
import axios from "@/api/axios";
import BaseSelect from "@/components/common/BaseSelect.vue";
import TimeInput from "@/components/common/TimeInput.vue";
import CalendarSrch from "@/components/common/CalendarSrch.vue";
import { formatYmdDot } from "@/utils/dateFormat";
import { resolveApiErrorMessage } from "@/utils/apiError";

defineOptions({ name: "LeaveApplyPop" });
const props = defineProps({
  onSaved: { type: Function, default: null },
});
const emit = defineEmits(["close"]);
const { proxy } = getCurrentInstance();

// ===== 데이터 =====
const leaveTypeList = ref([]);
const unitOptions = ref([]); // SYS025
const candidates = ref([]);
const presets = ref([]); // 본인 결재라인 프리셋 (prafta-020)
const selectedPresetId = ref("");
const submitting = ref(false);

// ===== 폼 =====
const leaveCd = ref("");
const workYmd = ref("");
const useUnitType = ref("00");
const startTime = ref("09:00");
const endTime = ref("11:00");
const reason = ref("");
const line = ref([]); // [{ userCd, userNm }]

// prafta-com-011-6 가불(미래 연차 당겨쓰기) 동의 상태. 종류/단위/날짜 변경 시 리셋.
const borrowAgreed = ref(false);

// 신청 대상: 사용자 신청 타입(leaveType='01') + 시스템 법정 시드(systemYn='Y', 가불 대상 월차/본연차 포함).
//   기존 비가불 UX 회귀 0 — '01' 노출은 유지하고, 가불용 시스템 법정 종류를 함께 노출한다(앱 메타 미러).
const applicableTypes = computed(() =>
  leaveTypeList.value.filter(
    (t) => t.leaveType === "01" || t.systemYn === "Y"
  )
);
const selectedType = computed(() =>
  leaveTypeList.value.find((t) => t.leaveCd === leaveCd.value)
);
// 결재 필요 여부(타입 플래그). 가불(borrowAgreed) ON 이면 체크박스 무관 결재 강제(결정 §4).
const needApproval = computed(
  () => selectedType.value?.aprvUseYn === "Y" || borrowAgreed.value
);
const isHourUnit = computed(() =>
  ["02", "03", "04"].includes(useUnitType.value)
);
const unitGuide = computed(
  () =>
    ({ "02": "2시간", "03": "1시간", "04": "30분" }[useUnitType.value] || "시간")
);

const inLine = (userCd) => line.value.some((s) => s.userCd === userCd);

// ===== 가불(미래 연차 당겨쓰기) 파생값 (prafta-com-011-6, 앱 LeaveApplyForm 미러) =====
// 가불 한도(서버 권위, 메타 borrowQuota). 비대상이면 0.
const borrowQuota = computed(() => Number(selectedType.value?.borrowQuota) || 0);

// 가불분 만료(소멸)일 YYYYMMDD(서버 산출). 없으면 ''.
const borrowExpiryYmd = computed(() =>
  String(selectedType.value?.borrowExpiryYmd || "")
);

// 만료일 표시("YYYY.MM.DD"). 미산정이면 ''. dateFormat 단일 출처에 위임.
const borrowExpiryDisplay = computed(() => {
  const ymd = borrowExpiryYmd.value;
  if (!ymd || ymd.length !== 8 || !/^\d{8}$/.test(ymd)) return "";
  return formatYmdDot(ymd);
});

// 신청 일수 추정(표시 전용 근사). 종일(00)=1.0 / 반차(01)=0.5.
//   시간차(02·03·04)는 웹 신청 폼에 소정근로 컨텍스트가 없어 추정 보류(null) → 가불 토글 미노출(앱 동일 정책).
const estimatedDays = computed(() => {
  if (!selectedType.value) return null;
  if (useUnitType.value === "00") return 1.0;
  if (useUnitType.value === "01") return 0.5;
  return null;
});

// 가불 토글 노출: 시스템 법정 연차(systemYn='Y') + 가불 가능(borrowable) + 추정 신청일수 > 잔여(부족).
//   잔여 충분/추정 불가/비대상이면 미노출(결정 §6-1: 부족할 때만).
const showBorrowToggle = computed(() => {
  const type = selectedType.value;
  if (!type) return false;
  if (String(type.systemYn) !== "Y") return false;
  if (!type.borrowable) return false;
  const bal = Number(type.balanceDays);
  const est = estimatedDays.value;
  if (Number.isNaN(bal) || est === null) return false;
  return est > bal;
});

// 가불 충당(부족) 안내 텍스트 — 예: "남은 0일 + 가불 1일". 추정 불가/충분이면 ''.
const borrowDeficitText = computed(() => {
  const type = selectedType.value;
  if (!type) return "";
  const bal = Number(type.balanceDays);
  const est = estimatedDays.value;
  if (Number.isNaN(bal) || est === null) return "";
  const deficit = est - Math.max(0, bal);
  if (deficit <= 0) return "";
  return `남은 ${formatDays(Math.max(0, bal))}일 + 가불 ${formatDays(deficit)}일`;
});

// 선택 일자가 가불 만료(소멸)일을 지났는지(가불 토글 ON 한정 가드). 만료 미산정이면 false.
const borrowDateExpired = computed(() => {
  if (!borrowAgreed.value) return false;
  const exp = borrowExpiryYmd.value;
  const ymd = (workYmd.value || "").replace(/-/g, "");
  if (!exp || !ymd || ymd.length !== 8) return false;
  return ymd > exp;
});

// 표시 헬퍼 — 정수면 정수, 소수면 1자리(앱 formatDays 정합).
const formatDays = (d) => {
  const n = Number(d);
  if (Number.isNaN(n)) return "0";
  return Number.isInteger(n) ? String(n) : n.toFixed(1);
};

// 연차 종류 변경 시 가불 동의 해제(가불 동의는 종류별 — 앱 onSelectType 정합).
watch(leaveCd, () => {
  borrowAgreed.value = false;
});

// 가불 토글 노출 조건이 깨지면(잔여 충분/비대상/단위 변경 등) 동의 자동 해제 — 잔존 동의 누수 방지.
watch(showBorrowToggle, (visible) => {
  if (!visible && borrowAgreed.value) borrowAgreed.value = false;
});

// 가불 토글 ON + 만료 경과 일자 선택 → alert 안내 후 차단(결정 §3, 서버도 fail-closed). 날짜 초기화.
watch(borrowDateExpired, (expired) => {
  if (expired) {
    proxy.$alert("가불 만료일이 지난 날짜에는 사용할 수 없어요.");
    workYmd.value = "";
  }
});

// ===== 로딩 =====
const fnLoadTypes = async () => {
  try {
    const r = await axios.get("/webApi/attd05/leave-type-lists", {});
    leaveTypeList.value = r.data?.leaveTypeResultList ?? [];
  } catch (e) {
    /* 타입 로딩 실패는 알림만 */
    await proxy.$alert(resolveApiErrorMessage(e, "연차 타입 조회 오류."));
  }
};
const fnLoadUnits = async () => {
  try {
    const r = await axios.get("/comApi/baseinfo/syst-info-lists", {
      params: { systCodeList: ["SYS025"] },
    });
    unitOptions.value = (r.data?.systInfoList ?? []).filter(
      (o) => o.systValDCd != null && o.systValCd === "SYS025"
    );
  } catch (e) {
    /* noop */
  }
};
const fnLoadCandidates = async () => {
  try {
    const r = await axios.get("/webApi/user04/approval-candidates", {});
    candidates.value = r.data?.candidates ?? [];
  } catch (e) {
    /* noop */
  }
};

// 본인 프리셋 로드 + 기본 프리셋 자동 적용 (prafta-020)
const fnLoadPresets = async () => {
  try {
    const r = await axios.get("/webApi/user04/presets", {});
    presets.value = r.data?.presets ?? [];
    const def = presets.value.find((p) => p.defaultYn === "Y");
    if (def) {
      selectedPresetId.value = def.presetId;
      fnApplyPresetSel();
    }
  } catch (e) {
    /* noop */
  }
};

// ===== 결재라인 구성 =====
const fnAdd = (c) => {
  if (!inLine(c.userCd)) line.value.push({ userCd: c.userCd, userNm: c.userNm });
};
const fnRemove = (i) => line.value.splice(i, 1);
const fnUp = (i) => {
  if (i <= 0) return;
  const a = line.value;
  [a[i - 1], a[i]] = [a[i], a[i - 1]];
};
const fnDown = (i) => {
  const a = line.value;
  if (i >= a.length - 1) return;
  [a[i + 1], a[i]] = [a[i], a[i + 1]];
};
// 선택한 프리셋의 결재라인을 적용 (prafta-020)
const fnApplyPresetSel = () => {
  const p = presets.value.find((x) => x.presetId === selectedPresetId.value);
  if (!p) {
    return;
  }
  line.value = (p.steps ?? []).map((s) => ({
    userCd: s.approverUserCd,
    userNm: s.userNm,
  }));
};

// ===== 신청 =====
const leaveTypeCode = computed(() => {
  if (useUnitType.value === "00") return "ANNUAL";
  if (useUnitType.value === "01") return "HALF";
  return "HOUR";
});

const fnSubmit = async () => {
  if (!leaveCd.value) return proxy.$alert("연차 타입을 선택해주세요.");
  if (!workYmd.value) return proxy.$alert("근무일을 선택해주세요.");
  if (isHourUnit.value && (!startTime.value || !endTime.value)) {
    return proxy.$alert("시간대를 입력해주세요.");
  }
  if (needApproval.value && line.value.length === 0) {
    return proxy.$alert("결재라인을 구성해주세요.");
  }
  // prafta-com-011-6 가불 토글 ON + 만료 경과 일자면 제출 차단(결정 §3, 서버 fail-closed 사전 방어).
  if (borrowDateExpired.value) {
    return proxy.$alert("가불 만료일이 지난 날짜에는 사용할 수 없어요.");
  }

  const payload = {
    leaveCd: leaveCd.value,
    leaveType: leaveTypeCode.value,
    workYmd: workYmd.value.replace(/-/g, ""),
    useUnitType: useUnitType.value,
    startTime: isHourUnit.value ? startTime.value.replace(":", "") : null,
    endTime: isHourUnit.value ? endTime.value.replace(":", "") : null,
    reason: reason.value,
    approverUserCds: needApproval.value
      ? line.value.map((s) => s.userCd)
      : [],
    // 가불 동의(prafta-com-011-6): 토글 ON 시 true(서버가 결재 강제·잔여 부족분 가불). 미선택이면 false.
    isBorrow: borrowAgreed.value,
  };

  submitting.value = true;
  try {
    await axios.post("/webApi/leaveflow/apply", payload);
    await proxy.$alert("연차 신청이 완료되었습니다.");
    if (typeof props.onSaved === "function") props.onSaved();
    emit("close");
  } catch (e) {
    await proxy.$alert(resolveApiErrorMessage(e, "연차 신청 중 오류가 발생했습니다."));
  } finally {
    submitting.value = false;
  }
};

onMounted(() => {
  fnLoadTypes();
  fnLoadUnits();
  fnLoadCandidates();
  fnLoadPresets();
});
</script>

<style scoped>
.la-modal {
  width: 520px;
  max-width: 94vw;
}
.la-body {
  display: flex;
  flex-direction: column;
  gap: 0.7rem;
  padding: 0.5rem 0.25rem;
}
.la-field {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}
.la-field > label {
  font-size: 0.85rem;
  color: var(--color-text, #111827);
}
.req {
  color: var(--color-danger, #dc2626);
}
/* 네이티브 date input → CalendarSrch 교체. 내부 input 셀렉터로 스타일 유지 */
.la-field :deep(.calendar-input),
.la-field textarea {
  border: 1px solid var(--color-border, #d1d5db);
  border-radius: 0.35rem;
  padding: 0.35rem 0.5rem;
  font-size: 0.9rem;
}
.la-time-row {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}
.la-sep {
  color: var(--color-text-muted, #6b7280);
}
.la-hint {
  font-size: 0.78rem;
  color: var(--color-text-muted, #6b7280);
  margin: 0;
}
/* prafta-com-011-6 가불 동의 토글 + 안내 — 기존 토큰만 사용(하드코딩 금지) */
.la-borrow-toggle {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  cursor: pointer;
  font-size: 0.85rem;
  color: var(--color-text, #374151);
}
.la-borrow-cb {
  width: 1rem;
  height: 1rem;
  accent-color: var(--color-primary, #16a34a);
  cursor: pointer;
}
.la-borrow-txt {
  font-weight: 500;
}
.la-borrow-info {
  margin-top: 0.4rem;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  padding: 0.5rem 0.6rem;
  background: var(--color-warning-bg, #fef3c7);
  border: 1px solid var(--color-warning-text, #b45309);
  border-radius: 0.35rem;
}
.la-borrow-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.la-borrow-lbl {
  font-size: 0.8rem;
  color: var(--color-warning-text, #b45309);
}
.la-borrow-val {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--color-warning-text, #b45309);
}
.la-borrow-deficit {
  margin: 0;
  font-size: 0.78rem;
  font-weight: 500;
  color: var(--color-warning-text, #b45309);
}
.la-borrow-guide {
  margin: 0;
  font-size: 0.76rem;
  color: var(--color-warning-text, #b45309);
}

.la-approval {
  border-top: 1px solid var(--color-border, #e5e7eb);
  padding-top: 0.6rem;
}
.la-approval__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.85rem;
  margin-bottom: 0.4rem;
}
.la-preset-sel {
  border: 1px solid var(--color-border, #d1d5db);
  border-radius: 0.3rem;
  padding: 0.2rem 0.4rem;
  font-size: 0.78rem;
  background: var(--color-surface, #fff);
  cursor: pointer;
  max-width: 60%;
}
.la-preset-empty {
  font-size: 0.74rem;
  color: var(--color-text-muted, #9ca3af);
}
.la-approval__cols {
  display: flex;
  gap: 0.6rem;
}
.la-pane {
  flex: 1;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 0.4rem;
  padding: 0.4rem;
  min-height: 140px;
}
.la-pane__title {
  font-size: 0.8rem;
  font-weight: 600;
  margin-bottom: 0.3rem;
}
.la-list {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}
.la-cand,
.la-step {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.82rem;
}
.la-cand {
  justify-content: space-between;
}
.la-cand.added {
  opacity: 0.5;
}
.la-cand button,
.la-step button {
  border: 1px solid var(--color-border, #d1d5db);
  background: var(--color-surface, #fff);
  border-radius: 0.3rem;
  padding: 0.1rem 0.4rem;
  font-size: 0.75rem;
  cursor: pointer;
}
.la-step__no {
  width: 1.3rem;
  height: 1.3rem;
  border-radius: 50%;
  background: var(--color-primary, #30796a);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.72rem;
}
.la-step__nm {
  flex: 1;
}
.la-del {
  color: var(--color-danger, #dc2626);
}
.la-empty {
  font-size: 0.8rem;
  color: var(--color-text-muted, #9ca3af);
  text-align: center;
  padding: 0.8rem 0;
}
.la-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  padding-top: 0.6rem;
}
.la-footer button {
  border-radius: 0.35rem;
  padding: 0.4rem 0.9rem;
  font-size: 0.85rem;
  cursor: pointer;
  border: 1px solid var(--color-border, #d1d5db);
}
.btn-confirm {
  background: var(--color-primary, #30796a);
  color: #fff;
  border-color: var(--color-primary, #30796a);
}
.btn-confirm:disabled {
  opacity: 0.6;
  cursor: default;
}
</style>
