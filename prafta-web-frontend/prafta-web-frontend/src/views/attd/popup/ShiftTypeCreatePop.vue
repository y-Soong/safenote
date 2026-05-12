<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-wide modal-content-shift-type"
        :style="positionStyle"
        ref="modalRef"
      >
        <div class="modal-header" @mousedown="startDrag">
          <span>{{
            isReadOnly ? "교대근무 타입 상세" : "교대근무 타입 생성"
          }}</span>
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

        <div class="modal-body-shift">
          <!-- 교대타입코드 -->
          <div class="form-section">
            <div class="form-row">
              <label>교대타입코드 <span class="required">*</span></label>
              <input
                v-model.trim="shiftTypeCd"
                type="text"
                placeholder="예: SH007"
                :readonly="isReadOnly"
              />
            </div>
          </div>

          <hr class="section-divider" />

          <!-- 교대 패턴 정의 -->
          <div class="form-section">
            <p class="section-desc">교대 근무에 사용할 패턴을 정의합니다.</p>
            <div class="form-row">
              <label>패턴 수</label>
              <select
                v-model.number="patternCount"
                class="select-pattern-count"
                :disabled="isReadOnly"
              >
                <option v-for="n in 4" :key="n" :value="n + 1">
                  {{ n + 1 }}
                </option>
              </select>
            </div>
            <div class="pattern-rows">
              <div v-for="i in patternCount" :key="i" class="pattern-row">
                <span class="pattern-label">패턴 {{ i }}</span>
                <select
                  v-model="patternSelections[i - 1]"
                  class="select-pattern"
                  :disabled="isReadOnly"
                >
                  <option value="OFF">휴무</option>
                  <option
                    v-for="p in patternOptions"
                    :key="p.value"
                    :value="p.value"
                  >
                    {{ p.label }}
                  </option>
                </select>
              </div>
            </div>
          </div>

          <hr class="section-divider" />

          <!-- 조/일 배치 -->
          <div class="form-section">
            <div class="group-day-header">
              <div class="group-buttons">
                <button
                  v-for="(g, idx) in groups"
                  :key="g"
                  type="button"
                  :class="[
                    'btn-group',
                    { removable: canRemoveGroup(idx) && !isReadOnly },
                  ]"
                  :disabled="isReadOnly"
                  @click="removeGroup(idx)"
                >
                  {{ g }}조
                  <span v-if="canRemoveGroup(idx)" class="btn-remove">×</span>
                </button>
                <button
                  v-if="groups.length < 5"
                  type="button"
                  class="btn-add-group"
                  :disabled="isReadOnly"
                  @click="addGroup"
                >
                  + 조 추가
                </button>
              </div>
              <div class="day-controls">
                <select
                  v-model.number="dayCount"
                  class="select-days"
                  :disabled="isReadOnly"
                >
                  <option v-for="d in dayOptions" :key="d" :value="d">
                    {{ d }}일
                  </option>
                </select>
                <button
                  type="button"
                  class="btn-action"
                  :disabled="isReadOnly"
                  @click="fnAutoFill"
                >
                  자동 채우기
                </button>
                <button
                  type="button"
                  class="btn-action"
                  :disabled="isReadOnly"
                  @click="fnReset"
                >
                  초기화
                </button>
              </div>
            </div>
            <p class="info-msg">
              ① A조 Day 1-Day {{ dayCount }}을 모두 선택하면 자동 채우기를
              사용할 수 있어요.
            </p>

            <div class="assignment-table-wrap">
              <table class="assignment-table">
                <thead>
                  <tr>
                    <th class="col-group">조</th>
                    <th v-for="d in dayCount" :key="d" class="col-day">
                      Day {{ d }}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(g, gi) in groups" :key="g">
                    <td class="col-group">{{ g }}조</td>
                    <td v-for="d in dayCount" :key="d" class="col-cell">
                      <select
                        v-model="assignments[gi][d - 1]"
                        class="cell-select"
                        :disabled="isReadOnly"
                      >
                        <option
                          v-for="p in tableCellOptions"
                          :key="p.value"
                          :value="p.value"
                        >
                          {{ p.label }}
                        </option>
                      </select>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <p v-if="!isAllAssigned" class="error-msg">
              모든 조/일 조합에 패턴 또는 휴무를 배치해주세요.
            </p>
          </div>

          <hr class="section-divider" />

          <!-- 사용여부 -->
          <div class="form-section">
            <div class="form-row">
              <label>사용여부</label>
              <BaseSelect v-model="useYn" class="select-use-yn">
                <option
                  v-for="opt in (systCodeArr['SYS003'] || []).filter(
                    (o) => o.systValDCd != null
                  )"
                  :key="opt.systValDCd"
                  :value="opt.systValDCd"
                >
                  {{ opt.systValDNm }}
                </option>
              </BaseSelect>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <p class="footer-hint">① 입력 오류가 있는 경우 생성할 수 없습니다.</p>
          <div class="footer-actions">
            <button class="btn btn-secondary" @click="$emit('close')">
              {{ isReadOnly ? "닫기" : "취소" }}
            </button>
            <button
              class="btn btn-primary"
              :disabled="!canCreate"
              @click="fnCreate"
            >
              {{ isReadOnly ? "수정" : "생성" }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
// ================ Imports ================
import { ref, computed, watch, getCurrentInstance, onMounted } from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import BaseSelect from "@/components/common/BaseSelect.vue";
import axios from "@/api/axios";
import { getMessage, MSG } from "@/messages";

// ================ Options ================
// (defineOptions 사용 시 추가)

// ================ Props & Emits ================
const props = defineProps({
  siteCd_p: { type: String, default: "" },
  shift_p: { type: Object, default: null },
  onSearch: { type: Function, default: null },
});
const emit = defineEmits(["close"]); // eslint-disable-line no-unused-vars -- used in template

// ================ Instance & Composables ================
const { proxy } = getCurrentInstance();
const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 2.5,
});

// ================ Refs (Variables) ================
const patternOptions = ref([]);
const shiftTypeCd = ref("");
const patternCount = ref(2);
const patternSelections = ref([]);
const groups = ref(["A", "B", "C"]);
const dayCount = ref(2);
const useYn = ref("Y");
const systCodeArr = ref({});
const assignments = ref({}); // { groupIdx: [day0, day1, ...] }

// ================ Computed ================
const isReadOnly = computed(() => !!props.shift_p);

const positionStyle = computed(() => {
  const padding = 16;
  const maxX = window.innerWidth - (800 + padding);
  const maxY = window.innerHeight - (600 + padding);
  const x = Math.max(padding, Math.min(maxX, position.value.x));
  const y = Math.max(padding, Math.min(maxY, position.value.y));
  return { top: y + "px", left: x + "px" };
});

/** 테이블 셀 옵션: 패턴 1, 패턴 2... 에서 정한 값들(중복 제거) + 휴무 */
const tableCellOptions = computed(() => {
  const uniqueValues = [...new Set(patternSelections.value.filter((v) => v))];
  const opts = uniqueValues.map((schCd) => {
    const p = patternOptions.value.find((o) => o.value === schCd);
    const label = schCd === "OFF" ? "휴무" : (p?.label ?? schCd);
    return { value: schCd, label };
  });
  if (!opts.some((o) => o.value === "OFF")) {
    opts.push({ value: "OFF", label: "휴무" });
  }
  return opts;
});

/** 교대일수 옵션: patternCount 이상 ~ 14 */
const dayOptions = computed(() => {
  const min = patternCount.value;
  const max = 14;
  const arr = [];
  for (let i = min; i <= max; i++) arr.push(i);
  return arr;
});

const isAllAssigned = computed(() => {
  for (let gi = 0; gi < groups.value.length; gi++) {
    for (let d = 0; d < dayCount.value; d++) {
      const v = assignments.value[gi]?.[d];
      if (v == null || v === "") return false;
    }
  }
  return true;
});

const canCreate = computed(() => {
  return (
    shiftTypeCd.value.trim() &&
    patternSelections.value.every((v) => v) &&
    isAllAssigned.value
  );
});

// ================ Life Cycle Functions ================
onMounted(async () => {
  await fnGetSystinfoList();
  await fnSchInfoList();
  if (props.shift_p) {
    await fnShiftDetail();
  }
});

// ================ Watchers ================
watch(
  dayOptions,
  (opts) => {
    if (opts.length && !opts.includes(dayCount.value)) {
      dayCount.value = opts[0];
    }
  },
  { immediate: true }
);

watch(
  patternCount,
  (n) => {
    const arr = [...patternSelections.value];
    while (arr.length < n) {
      arr.push("OFF");
    }
    patternSelections.value = arr.slice(0, n);

    const minGroups = n;
    if (groups.value.length < minGroups) {
      const add = minGroups - groups.value.length;
      for (let i = 0; i < add && groups.value.length < 5; i++) {
        const next = String.fromCharCode(65 + groups.value.length);
        groups.value.push(next);
      }
    } else if (groups.value.length > minGroups) {
      while (groups.value.length > minGroups && groups.value.length > 1) {
        groups.value.pop();
      }
    }
    rebuildAssignments();
  },
  { immediate: true }
);

watch(dayCount, () => rebuildAssignments());
watch(groups, () => rebuildAssignments(), { deep: true });

// ================ API Functions ================
const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-lists", {
      params: { systCodeList: ["SYS003"] },
    });
    if (response.status === 200) {
      const resData = response.data?.systInfoList || [];
      const grouped = {};
      resData.forEach((item) => {
        const key = item.systValCd;
        if (!grouped[key]) grouped[key] = [];
        grouped[key].push(item);
      });
      systCodeArr.value = grouped;
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "조회 중 오류가 발생했습니다.";
    await proxy.$alert(msg);
  }
};

const fnSchInfoList = async () => {
  if (proxy.$util.isEmpty(props.siteCd_p)) {
    proxy.$alert(getMessage(MSG.SITE_REQUIRED));
    return;
  }
  patternOptions.value = [];
  try {
    const response = await axios.get("/webApi/attd01/sch-info-lists", {
      params: {
        siteCd: props.siteCd_p,
        schType: "01",
        useYn: "Y",
      },
    });
    if (response.status === 200) {
      const list = response.data?.schInfoResultList ?? [];
      patternOptions.value = [
        ...list.map((item) => ({
          value: item.schCd,
          label: item.schNo + " - " + item.fstSchTime,
        })),
        { value: "OFF", label: "휴무" },
      ];
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "조회 중 오류가 발생했습니다.";
    await proxy.$alert(msg);
  }
};

const fnShiftDetail = async () => {
  const shiftCd = props.shift_p?.shiftCd ?? props.shift_p?.shiftNo;
  if (!shiftCd) return;
  try {
    const response = await axios.get("/webApi/attd01/shift-sch-details", {
      params: {
        siteCd: props.siteCd_p,
        shiftCd,
      },
    });
    if (response.status === 200) {
      console.log(response.data);
      bindDetailToForm(response.data);
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "조회 중 오류가 발생했습니다.";
    await proxy.$alert(msg);
  }
};

const fnCreate = async () => {
  if (!canCreate.value) return;

  const ok = await proxy.$confirm(getMessage(MSG.CREATE_CONFIRM));
  if (!ok) return;

  const payload = buildCreatePayload();

  try {
    const response = await axios.post(
      "/webApi/attd01/update-shift-sch-infos",
      payload
    );
    if (response.status === 200) {
      proxy.$alert(getMessage(MSG.SAVE_SUCCESS));
      props.onSearch?.();
      emit("close");
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "처리 중 오류가 발생했습니다.";
    await proxy.$alert(msg);
  }
};

// ================ Methods/Functions ================
function rebuildAssignments() {
  const next = {};
  const defaultVal = "OFF";
  groups.value.forEach((_, gi) => {
    next[gi] = assignments.value[gi]?.slice(0, dayCount.value) || [];
    while (next[gi].length < dayCount.value) next[gi].push(defaultVal);
  });
  assignments.value = next;
}

function addGroup() {
  if (groups.value.length >= 5) return;
  const next = String.fromCharCode(65 + groups.value.length);
  groups.value.push(next);
}

function canRemoveGroup(idx) {
  return groups.value.length > patternCount.value && idx >= patternCount.value;
}

function removeGroup(idx) {
  if (!canRemoveGroup(idx)) return;
  groups.value.splice(idx, 1);
  rebuildAssignments();
}

function fnAutoFill() {
  const base = assignments.value[0];
  if (!base || base.some((v) => !v)) {
    proxy.$alert(getMessage(MSG.SHIFT_DAY_REQUIRED));
    return;
  }
  const next = { ...assignments.value };
  const dc = dayCount.value;
  for (let gi = 1; gi < groups.value.length; gi++) {
    next[gi] = [];
    for (let d = 0; d < dc; d++) {
      next[gi].push(base[(d - gi + dc) % dc]);
    }
  }
  assignments.value = next;
}

function fnReset() {
  const next = {};
  groups.value.forEach((_, gi) => {
    next[gi] = Array(dayCount.value).fill("OFF");
  });
  assignments.value = next;
}

/** 생성 시 서버로 전송할 4개 데이터 구조 생성 */
function buildCreatePayload() {
  const shiftType = {
    siteCd: props.siteCd_p,
    shiftNo: shiftTypeCd.value.trim(),
    shiftPtrnCnt: patternCount.value,
    shiftTeamCnt: groups.value.length,
    shiftCycleDays: dayCount.value,
    useYn: useYn.value,
  };

  const shiftPatternList = patternSelections.value.map((schCd, idx) => ({
    siteCd: props.siteCd_p,
    ptrnIdx: idx + 1,
    schCd: schCd || null,
  }));

  const shiftTeamList = groups.value.map((groupName, idx) => ({
    siteCd: props.siteCd_p,
    teamIdx: idx + 1,
    teamNm: groupName,
  }));

  const shiftAssignList = [];
  for (let gi = 0; gi < groups.value.length; gi++) {
    for (let d = 0; d < dayCount.value; d++) {
      const cellVal = assignments.value[gi]?.[d] ?? "";
      const isOff = cellVal === "OFF" || cellVal === "";
      shiftAssignList.push({
        siteCd: props.siteCd_p,
        dayNo: d + 1,
        teamIdx: gi + 1,
        assignYn: isOff ? "N" : "Y",
        schCd: isOff ? null : cellVal,
      });
    }
  }

  return {
    shiftType,
    shiftPatternList,
    shiftTeamList,
    shiftAssignList,
  };
}

/**
 * 상세 조회 후 화면에 바인딩
 * shiftTypeInfoList, shiftTeamInfoList, shiftPatternInfoList, shiftAssignInfoList
 */
function bindDetailToForm(data) {
  const typeList = data?.shiftTypeInfoResultList ?? [];
  const st = typeList[0] ?? {};
  const teamList = data?.shiftTeamInfoResultList ?? [];
  const ptrnList = data?.shiftPatternInfoResultList ?? [];
  const assignList = data?.shiftAssignInfoResultList ?? [];

  const toNum = (v) => {
    if (v == null || v === "") return undefined;
    const n = parseInt(v, 10);
    return Number.isNaN(n) ? undefined : n;
  };

  shiftTypeCd.value = st.shiftNo ?? st.shiftTypeCd ?? "";
  useYn.value = st.useYn ?? "Y";

  patternCount.value = toNum(st.shiftPtrnCnt) ?? 2;
  dayCount.value = toNum(st.shiftCycleDays) ?? 2;

  patternSelections.value = ptrnList
    .sort((a, b) => (toNum(a.ptrnIdx) ?? 0) - (toNum(b.ptrnIdx) ?? 0))
    .map((p) => p.schCd ?? "OFF");

  groups.value = teamList
    .sort((a, b) => (toNum(a.teamIdx) ?? 0) - (toNum(b.teamIdx) ?? 0))
    .map((t) => t.teamNm ?? "");

  const nextAssign = {};
  assignList.forEach((a) => {
    const gi = (toNum(a.teamIdx) ?? 1) - 1;
    const d = (toNum(a.dayNo) ?? 1) - 1;
    if (!nextAssign[gi]) nextAssign[gi] = [];
    const val = a.assignYn === "Y" && a.schCd ? a.schCd : "OFF";
    nextAssign[gi][d] = val;
  });
  groups.value.forEach((_, gi) => {
    if (!nextAssign[gi]) nextAssign[gi] = [];
    while (nextAssign[gi].length < dayCount.value) {
      nextAssign[gi].push("OFF");
    }
  });
  assignments.value = nextAssign;
}
</script>

<style scoped>
@import "@/assets/css/modal-popup-guide.css";

.modal-body-shift {
  padding: 1rem 1.5rem;
  overflow-y: auto;
  flex: 1;
  min-height: 0;
}

.form-section {
  margin-bottom: 1.5rem;
}

.section-divider {
  border: none;
  border-top: 1px solid #e5e7eb;
  margin: 1rem 0.5rem;
}

.form-section:last-of-type {
  margin-bottom: 0;
}

.section-desc {
  color: #6b7280;
  font-size: 0.875rem;
  margin-bottom: 0.75rem;
}

.form-row {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 0.75rem;
}

.form-row label {
  min-width: 120px;
  font-weight: 500;
  font-size: 0.875rem;
}

.form-row input,
.form-row select {
  padding: 0.5rem 0.75rem;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 0.875rem;
}

.form-row input {
  width: 12rem;
}

.required {
  color: #ef4444;
}

.select-pattern-count,
.select-pattern {
  padding: 0.5rem 0.75rem;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 0.875rem;
}

.select-pattern-count {
  width: 5rem;
}

.pattern-rows {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-top: 0.5rem;
  margin-left: 3rem;
}

.pattern-row {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.pattern-label {
  min-width: 80px;
  font-size: 0.875rem;
}

.select-pattern {
  flex: 1;
  max-width: 320px;
}

.group-day-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 1rem;
  margin-bottom: 0.5rem;
}

.group-buttons {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
}

.btn-group {
  padding: 0.4rem 0.75rem;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f9fafb;
  font-size: 0.875rem;
  cursor: pointer;
}

.btn-group.removable:hover {
  background: #fee2e2;
  border-color: #fecaca;
}

.btn-remove {
  margin-left: 0.25rem;
  color: #ef4444;
}

.btn-add-group {
  padding: 0.4rem 0.75rem;
  border: 1px dashed #16a34a;
  border-radius: 8px;
  background: transparent;
  color: #16a34a;
  font-size: 0.875rem;
  cursor: pointer;
}

.btn-add-group:hover {
  background: rgba(22, 163, 74, 0.08);
}

.day-controls {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-left: auto;
}

.select-days {
  width: 5rem;
  padding: 0.4rem 0.5rem;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 0.875rem;
}

.btn-action {
  padding: 0.4rem 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background: #fff;
  font-size: 0.8125rem;
  cursor: pointer;
}

.btn-action:hover {
  background: #f3f4f6;
}

.info-msg {
  font-size: 0.8125rem;
  color: #6b7280;
  margin-bottom: 0.75rem;
}

.assignment-table-wrap {
  overflow-x: auto;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  margin-bottom: 0.5rem;
}

.assignment-table {
  width: 100%;
  table-layout: fixed;
  border-collapse: collapse;
  font-size: 0.8125rem;
}

.assignment-table th,
.assignment-table td {
  border: 1px solid #e5e7eb;
  padding: 0.35rem 0.5rem;
}

.assignment-table th {
  background: #f9fafb;
  font-weight: 600;
  text-align: center;
}

.col-group {
  width: 4rem;
  min-width: 4rem;
}

.col-day,
.col-cell {
  width: 11rem;
  min-width: 11rem;
}

.cell-select {
  width: 100%;
  padding: 0.25rem 0.35rem;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  font-size: 0.75rem;
}

.error-msg {
  font-size: 0.8125rem;
  color: #ef4444;
  margin-top: 0.25rem;
}

.select-use-yn {
  width: 8rem;
}

.modal-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem 1.5rem;
  background: #f9fafb;
  border-top: 1px solid #e5e7eb;
}

.footer-hint {
  font-size: 0.8125rem;
  color: #6b7280;
  margin: 0;
}

.footer-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-shrink: 0;
}

.footer-actions .btn {
  padding: 0.5rem 1rem;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
}

.btn-secondary {
  background: #fff;
  border: 1px solid #d1d5db;
  color: #374151;
}

.btn-secondary:hover {
  background: #f9fafb;
}

.btn-primary {
  background: #16a34a;
  border: 1px solid #16a34a;
  color: #fff;
}

.btn-primary:hover:not(:disabled) {
  background: #15803d;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
