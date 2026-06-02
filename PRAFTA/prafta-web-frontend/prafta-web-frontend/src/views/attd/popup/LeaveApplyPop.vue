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
            <input type="date" v-model="workYmd" />
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
  onMounted,
  getCurrentInstance,
  defineProps,
  defineEmits,
  defineOptions,
} from "vue";
import axios from "@/api/axios";
import BaseSelect from "@/components/common/BaseSelect.vue";
import TimeInput from "@/components/common/TimeInput.vue";
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

// 사용자 신청 타입(leaveType='01')만 신청 대상
const applicableTypes = computed(() =>
  leaveTypeList.value.filter((t) => t.leaveType === "01")
);
const selectedType = computed(() =>
  leaveTypeList.value.find((t) => t.leaveCd === leaveCd.value)
);
const needApproval = computed(() => selectedType.value?.aprvUseYn === "Y");
const isHourUnit = computed(() =>
  ["02", "03", "04"].includes(useUnitType.value)
);
const unitGuide = computed(
  () =>
    ({ "02": "2시간", "03": "1시간", "04": "30분" }[useUnitType.value] || "시간")
);

const inLine = (userCd) => line.value.some((s) => s.userCd === userCd);

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
.la-field input[type="date"],
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
