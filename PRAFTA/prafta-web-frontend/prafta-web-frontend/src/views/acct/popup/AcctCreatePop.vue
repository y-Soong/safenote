<template>
  <Transition name="fade">
    <div
      v-show="true"
      class="modal-overlay prafta-modal-popup"
      @click.self="$emit('close')"
    >
      <div
        class="modal-content-normal"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <!-- 1. Title -->
        <div class="modal-header" @mousedown="startDrag">
          <span>사고 등록</span>
          <button class="icon-button" @click="$emit('close')" aria-label="닫기">
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

        <!-- 2. Body -->
        <div class="acc-modal-body">
          <div class="acc-callout">
            ⚠ 사고관리는 <b>실제 피해(부상·사망 등)가 발생한 사고</b>를 대상으로
            합니다. 인적·물적 피해가 없는 사건은 <b>아차사고 보고</b>로
            등록하세요.
          </div>

          <!-- 발생일 / 발생시각 -->
          <div class="acc-row two">
            <div class="acc-field">
              <label>사고 발생일<span class="req">*</span></label>
              <CalendarSrch v-model="occurDate" />
            </div>
            <div class="acc-field">
              <label>발생 시각<span class="req">*</span></label>
              <TimeInput v-model="occurTimeInput" />
            </div>
          </div>

          <!-- 발생 사업장 (SiteSearchPop) -->
          <div class="acc-row">
            <div class="acc-field">
              <label>발생 사업장<span class="req">*</span></label>
              <div class="acc-inline">
                <input
                  type="text"
                  :value="siteNm"
                  placeholder="사업장을 선택하세요"
                  readonly
                  @click="fnSiteSearchPopOpen"
                />
                <button class="btn btn-second" @click="fnSiteSearchPopOpen">
                  사업장 검색
                </button>
              </div>
              <div class="acc-hint">
                사업장을 먼저 선택하면 재해자·연결 항목이 해당 사업장 기준으로
                조회됩니다.
              </div>
            </div>
          </div>

          <!-- 재해자 (victim-search) -->
          <div class="acc-row">
            <div class="acc-field">
              <label>재해자<span class="req">*</span></label>
              <div class="acc-inline">
                <input
                  type="text"
                  :value="victimLabel"
                  placeholder="사업장 선택 후 재해자를 검색하세요"
                  readonly
                  @click="fnOpenVictimSearch"
                />
                <button
                  class="btn btn-second"
                  :disabled="!siteCd"
                  @click="fnOpenVictimSearch"
                >
                  재해자 검색
                </button>
              </div>
            </div>
          </div>

          <!-- 연관 데이터 조회 조건 -->
          <div class="acc-divider">
            연관 데이터 조회 조건
            <span
              >· 관련된 순회점검·위험성평가·아차사고만 좁혀서 가져오기 위한
              값입니다 (미입력 시 전체)</span
            >
          </div>

          <!-- 순회점검: 점검구분(COM001) + 점검대상(ChkptSearchPop 다건) -->
          <div class="acc-row">
            <div class="acc-field">
              <label>순회점검 — 점검구분</label>
              <BaseSelect v-model="chklstType" :disabled="!siteCd">
                <option
                  v-for="opt in baseCodeArr['COM001'] || []"
                  :key="opt.baimValDCd"
                  :value="opt.baimValDCd"
                >
                  {{ opt.baimValDNm }}
                </option>
              </BaseSelect>
            </div>
          </div>
          <div class="acc-row">
            <div class="acc-field">
              <label>순회점검 — 점검대상 (다건)</label>
              <div class="acc-inline">
                <button
                  class="btn btn-second"
                  :disabled="!siteCd"
                  @click="fnOpenChkptSearch"
                >
                  점검대상 검색
                </button>
                <span class="acc-chip-count" v-if="selectedChkpts.length">
                  {{ selectedChkpts.length }}건 선택됨
                </span>
              </div>
              <div class="acc-chip-wrap" v-if="selectedChkpts.length">
                <span
                  v-for="c in selectedChkpts"
                  :key="c.chkptCd"
                  class="acc-chip"
                >
                  {{ c.chkptNm }}
                  <button class="acc-chip-x" @click="fnRemoveChkpt(c.chkptCd)">
                    ✕
                  </button>
                </span>
              </div>
            </div>
          </div>

          <!-- 위험성평가 3계층 -->
          <div class="acc-row three">
            <div class="acc-field">
              <label>위험성평가 — 공정</label>
              <BaseSelect v-model="processCd" :disabled="!siteCd">
                <option
                  v-for="opt in processOptions"
                  :key="opt.code"
                  :value="opt.code"
                >
                  {{ opt.name }}
                </option>
              </BaseSelect>
            </div>
            <div class="acc-field">
              <label>위험성평가 — 위험요인구분</label>
              <BaseSelect v-model="riskTypeCd" :disabled="!siteCd">
                <option
                  v-for="opt in riskTypeOptions"
                  :key="opt.code"
                  :value="opt.code"
                >
                  {{ opt.name }}
                </option>
              </BaseSelect>
            </div>
            <div class="acc-field">
              <label>위험성평가 — 유해요인</label>
              <BaseSelect v-model="hazardCd" :disabled="!siteCd">
                <option
                  v-for="opt in hazardOptions"
                  :key="opt.code"
                  :value="opt.code"
                >
                  {{ opt.name }}
                </option>
              </BaseSelect>
            </div>
          </div>

          <!-- 아차사고: 사건유형(SYS061) + 잠재중대성(SYS062) -->
          <div class="acc-row two">
            <div class="acc-field">
              <label>아차사고 — 사건유형</label>
              <BaseSelect v-model="incidentTypeCd">
                <option
                  v-for="opt in systCodeArr['SYS061'] || []"
                  :key="opt.systValDCd"
                  :value="opt.systValDCd"
                >
                  {{ opt.systValDNm }}
                </option>
              </BaseSelect>
            </div>
            <div class="acc-field">
              <label>아차사고 — 잠재중대성</label>
              <BaseSelect v-model="potentialSeverityCd">
                <option
                  v-for="opt in systCodeArr['SYS062'] || []"
                  :key="opt.systValDCd"
                  :value="opt.systValDCd"
                >
                  {{ opt.systValDNm }}
                </option>
              </BaseSelect>
            </div>
          </div>

          <!-- 사고 내용 -->
          <div class="acc-divider">사고 내용</div>

          <div class="acc-row">
            <div class="acc-field">
              <label>재해 등급<span class="req">*</span></label>
              <div class="acc-grade-pick">
                <div
                  v-for="opt in gradeOptions"
                  :key="opt.code"
                  class="acc-grade-opt"
                  :class="{ sel: acctGradeCd === opt.code }"
                  @click="acctGradeCd = opt.code"
                >
                  <div class="acc-grade-t" :class="opt.cls">{{ opt.name }}</div>
                  <div class="acc-grade-d">{{ opt.desc }}</div>
                </div>
              </div>
              <div class="acc-hint">
                휴업일수는 사고 직후 미확정일 수 있습니다. 등급은 추후 변경
                가능하며, 변경 시 법정 기한이 재계산됩니다.
              </div>
            </div>
          </div>

          <div class="acc-row">
            <div class="acc-field">
              <label>사고 경위<span class="req">*</span></label>
              <textarea
                v-model.trim="acctDesc"
                rows="3"
                placeholder="사고 경위를 입력하세요."
              ></textarea>
            </div>
          </div>

          <div class="acc-info-box">
            ℹ 등록하면 위 조건을 기준으로 <b>사고일 시점의 근태·순회점검·위험성평가·당일 TBM·아차사고</b>를
            확정(연결)하는 화면으로 이동합니다.
          </div>
        </div>

        <!-- 3. Footer -->
        <div class="modal-foot">
          <button class="btn btn-second" @click="$emit('close')">취소</button>
          <button class="btn btn-primary" @click="fnCreate">등록</button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import {
  ref,
  computed,
  defineProps,
  defineEmits,
  onMounted,
  getCurrentInstance,
} from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import { useModal } from "@/utils/useModal";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import BaseSelect from "@/components/common/BaseSelect.vue";
import CalendarSrch from "@/components/common/CalendarSrch.vue";
import TimeInput from "@/components/common/TimeInput.vue";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import ChkptSearchPop from "@/components/popup/ChkptSearchPop.vue";
import VictimSearchPop from "./VictimSearchPop.vue";

const props = defineProps({
  onCreated: Function, // (acctId, siteCd, acctInfoForLink) => void
});
const emit = defineEmits(["close"]);

const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();
const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 4,
});

// ── 폼 상태 ──
const occurDate = ref(""); // YYYY-MM-DD (date input)
const occurTimeInput = ref(""); // HH:MM (time input)
const siteCd = ref("");
const siteNo = ref("");
const siteNm = ref("");
const victimUserTypeCd = ref("");
const victimUserCd = ref("");
const victimUserNm = ref("");
const victimDeptNm = ref("");

// 연관 조회 조건
const chklstType = ref("");
const selectedChkpts = ref([]); // [{ chkptCd, chkptNm, chklstType }]
const processCd = ref("");
const riskTypeCd = ref("");
const hazardCd = ref("");
const incidentTypeCd = ref("");
const potentialSeverityCd = ref("");

// 사고 내용
const acctGradeCd = ref("");
const acctDesc = ref("");

// 코드
const baseCodeArr = ref({}); // COM001
const systCodeArr = ref({}); // SYS061/062/065
const riskCategoryList = ref([]); // RiskCategoryOptionResult[]

const gradeFallback = [
  { code: "100", name: "중대재해", cls: "g-crit", desc: "사망1↑ / 3개월요양 2↑ / 10명↑ — 지체없이 보고" },
  { code: "200", name: "일반산재", cls: "g-norm", desc: "사망 또는 3일↑ 휴업 — 1개월 내 조사표" },
  { code: "300", name: "신고제외", cls: "g-exem", desc: "3일 미만 휴업 — 신고 의무 없음, 기록·보존만" },
];

// 재해등급(SYS065) 옵션: 서버 코드명 우선, 없으면 폴백
// 집계용 '전체' 등 더미 코드(코드값 000/공백 또는 코드명 '전체')는 등록 화면에서 제외한다.
const gradeOptions = computed(() => {
  const codes = (systCodeArr.value["SYS065"] || []).filter(
    (c) =>
      c.systValDCd &&
      c.systValDCd !== "000" &&
      c.systValDNm !== "전체"
  );
  if (codes.length === 0) return gradeFallback;
  return codes.map((c) => {
    const fb = gradeFallback.find((f) => f.code === c.systValDCd);
    return {
      code: c.systValDCd,
      name: c.systValDNm,
      cls: fb ? fb.cls : "",
      desc: fb ? fb.desc : "",
    };
  });
});

// 재해자 표시 라벨
const victimLabel = computed(() => {
  if (!victimUserNm.value) return "";
  const dept = victimDeptNm.value ? ` · ${victimDeptNm.value}` : "";
  const type = victimUserTypeCd.value === "DAILY" ? " (일용)" : "";
  return `${victimUserNm.value}${dept}${type}`;
});

// 위험성평가 3계층 옵션 (categoryType + parentCode 로 필터)
const processOptions = computed(() =>
  riskCategoryList.value.filter((o) => o.categoryType === "PROCESS")
);
const riskTypeOptions = computed(() =>
  riskCategoryList.value.filter(
    (o) =>
      o.categoryType === "RISK_TYPE" &&
      (!processCd.value || o.parentCode === processCd.value)
  )
);
const hazardOptions = computed(() =>
  riskCategoryList.value.filter(
    (o) =>
      o.categoryType === "HAZARD" &&
      (!riskTypeCd.value || o.parentCode === riskTypeCd.value)
  )
);

onMounted(async () => {
  fnInitDefault();
  await Promise.all([fnGetBaseinfoList(), fnGetSystinfoList()]);
});

const fnInitDefault = () => {
  // 세션 사업장을 기본 사업장으로 채움
  siteCd.value = sessionStorage.getItem("gv_siteCd") ?? "";
  siteNo.value = sessionStorage.getItem("gv_siteNo") ?? "";
  siteNm.value = sessionStorage.getItem("gv_siteNm") ?? "";
};

// COM001 점검구분 코드
const fnGetBaseinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/base-info-lists", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
        baseCodeList: ["COM001"],
      },
    });
    if (response.status === 200) {
      const resData = response.data?.baseInfoList || [];
      const grouped = {};
      resData.forEach((item) => {
        const key = item.baimValCd;
        if (!grouped[key]) grouped[key] = [];
        grouped[key].push(item);
      });
      baseCodeArr.value = grouped;
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "점검구분 조회 중 오류가 발생했습니다.")
    );
  }
};

// SYS061(사건유형) / SYS062(잠재중대성) / SYS065(재해등급)
const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-lists", {
      params: { systCodeList: ["SYS061", "SYS062", "SYS065"] },
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
    await proxy.$alert(
      resolveApiErrorMessage(err, "코드 조회 중 오류가 발생했습니다.")
    );
  }
};

// 위험성평가 3계층 옵션 조회 (사업장 확정 후)
const fnGetRiskCategoryOptions = async () => {
  if (!siteCd.value) {
    riskCategoryList.value = [];
    return;
  }
  try {
    const response = await axios.get(
      "/webApi/acct01/risk/category-options",
      {
        params: {
          siteCd: siteCd.value,
          processCd: "",
          riskTypeCd: "",
        },
      }
    );
    if (response.status === 200) {
      riskCategoryList.value = response.data?.categoryOptionList || [];
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "위험성평가 분류 조회 중 오류가 발생했습니다.")
    );
  }
};

// 사업장 검색 팝업
const fnSiteSearchPopOpen = () => {
  openPop(SiteSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteNo_p: "",
    siteNm_p: "",
    onSelect: onSiteSelected,
  });
};

const onSiteSelected = async (siteCdVal, siteNoVal, siteNmVal) => {
  siteCd.value = siteCdVal;
  siteNo.value = siteNoVal;
  siteNm.value = siteNmVal;
  // 사업장 변경 시 사업장 종속값 초기화
  victimUserTypeCd.value = "";
  victimUserCd.value = "";
  victimUserNm.value = "";
  victimDeptNm.value = "";
  selectedChkpts.value = [];
  processCd.value = "";
  riskTypeCd.value = "";
  hazardCd.value = "";
  await fnGetRiskCategoryOptions();
};

// 재해자 검색 팝업 (인라인 검색 모달)
const fnOpenVictimSearch = () => {
  if (!siteCd.value) {
    proxy.$alert("발생 사업장을 먼저 선택하세요.");
    return;
  }
  openPop(VictimSearchPop, {
    siteCd: siteCd.value,
    onSelect: onVictimSelected,
  });
};

const onVictimSelected = (v) => {
  victimUserTypeCd.value = v.userTypeCd;
  victimUserCd.value = v.userCd;
  victimUserNm.value = v.userNm;
  victimDeptNm.value = v.nodeNm || "";
};

// 점검대상 검색 팝업 (다건)
const fnOpenChkptSearch = () => {
  if (!siteCd.value) {
    proxy.$alert("발생 사업장을 먼저 선택하세요.");
    return;
  }
  openPop(ChkptSearchPop, {
    siteCd: siteCd.value,
    onSelect: onChkptSelected,
  });
};

const onChkptSelected = (list) => {
  // 기존 선택과 병합(중복 제거)
  const map = {};
  [...selectedChkpts.value, ...list].forEach((c) => {
    map[c.chkptCd] = c;
  });
  selectedChkpts.value = Object.values(map);
};

const fnRemoveChkpt = (chkptCd) => {
  selectedChkpts.value = selectedChkpts.value.filter(
    (c) => c.chkptCd !== chkptCd
  );
};

// YYYY-MM-DD → YYYYMMDD
const toYmd = (d) => (d ? d.replace(/-/g, "") : "");
// HH:MM → HHMM
const toHm = (t) => (t ? t.replace(/:/g, "") : "");

const fnValidate = () => {
  if (!occurDate.value) return "사고 발생일을 입력하세요.";
  if (!occurTimeInput.value) return "발생 시각을 입력하세요.";
  if (!siteCd.value) return "발생 사업장을 선택하세요.";
  if (!victimUserCd.value) return "재해자를 선택하세요.";
  if (!acctGradeCd.value) return "재해 등급을 선택하세요.";
  if (!acctDesc.value) return "사고 경위를 입력하세요.";
  return "";
};

const fnCreate = async () => {
  const msg = fnValidate();
  if (msg) {
    await proxy.$alert(msg);
    return;
  }

  const body = {
    siteCd: siteCd.value,
    victimUserTypeCd: victimUserTypeCd.value,
    victimUserCd: victimUserCd.value,
    occurYmd: toYmd(occurDate.value),
    occurTime: toHm(occurTimeInput.value),
    acctGradeCd: acctGradeCd.value,
    acctDesc: acctDesc.value,
  };

  try {
    const response = await axios.post("/webApi/acct01/create", body);
    if (response.status === 200) {
      const acctId = response.data?.acctId;
      await proxy.$alert("사고가 등록되었습니다.");
      // 등록 후 수평선 확정화면에서 쓸 조회조건을 함께 넘김
      if (typeof props.onCreated === "function") {
        props.onCreated({
          acctId,
          siteCd: siteCd.value,
          chklstType: chklstType.value,
          chkptCds: selectedChkpts.value.map((c) => c.chkptCd),
          processCd: processCd.value,
          riskTypeCd: riskTypeCd.value,
          hazardCd: hazardCd.value,
          incidentTypeCd: incidentTypeCd.value,
          potentialSeverityCd: potentialSeverityCd.value,
        });
      }
      emit("close");
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "사고 등록 중 오류가 발생했습니다.")
    );
  }
};
</script>

<style scoped>
.acc-modal-body {
  padding: var(--card-padding, 20px);
  overflow-y: auto;
  max-height: 70vh;
}
.acc-callout {
  background: var(--color-warning-bg, #fef3c7);
  border: 1px solid var(--color-warning-bg, #fde68a);
  border-radius: var(--input-radius, 10px);
  padding: 0.625rem 0.75rem;
  font-size: 0.75rem;
  color: var(--color-warning-text, #92400e);
  margin-bottom: 1rem;
  line-height: 1.55;
}
.acc-row {
  margin-bottom: 0.9rem;
}
.acc-row.two {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}
.acc-row.three {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 0.75rem;
}
.acc-field {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}
.acc-field > label {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--color-text-muted, #4b5563);
}
.acc-field .req {
  color: var(--color-danger, #ef4444);
  margin-left: 2px;
}
.acc-field input,
.acc-field textarea {
  width: 100%;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 10px);
  padding: 0.5rem 0.65rem;
  font-size: 0.85rem;
  font-family: "Pretendard", sans-serif;
  color: var(--color-text-strong, #111827);
  background: var(--color-surface, #fff);
}
/* BaseSelect 는 다중 루트 컴포넌트라 부모 scoped 속성이 내부 select 에 붙지 않는다.
   :deep() 로 직접 타게팅해 input·textarea 와 동일한 테두리/포커스 규격을 적용 */
.acc-field :deep(select) {
  width: 100%;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 10px);
  padding: 0.5rem 0.65rem;
  font-size: 0.85rem;
  font-family: "Pretendard", sans-serif;
  color: var(--color-text-strong, #111827);
  background: var(--color-surface, #fff);
  cursor: pointer;
}
.acc-field input:focus,
.acc-field textarea:focus {
  outline: none;
  border-color: var(--color-primary, #16a34a);
  box-shadow: 0 0 0 var(--focus-ring-width, 3px) var(--color-focus-ring);
}
.acc-field :deep(select:focus) {
  outline: none;
  border-color: var(--color-primary, #16a34a);
  box-shadow: 0 0 0 var(--focus-ring-width, 3px) var(--color-focus-ring);
}
.acc-field textarea {
  resize: vertical;
  min-height: 62px;
}
.acc-inline {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.acc-inline input {
  flex: 1;
  cursor: pointer;
}
.acc-hint {
  font-size: 0.7rem;
  color: var(--color-text-muted, #8b94a3);
  line-height: 1.5;
}
.acc-divider {
  font-size: 0.75rem;
  font-weight: 800;
  color: var(--color-primary-hover, #15803d);
  margin: 1.1rem 0 0.75rem;
  padding-top: 0.875rem;
  border-top: 1px solid var(--color-border, #e5e7eb);
  display: flex;
  align-items: baseline;
  gap: 0.5rem;
  flex-wrap: wrap;
}
.acc-divider span {
  font-weight: 400;
  color: var(--color-text-muted, #8b94a3);
  font-size: 0.66rem;
}
.acc-chip-count {
  font-size: 0.72rem;
  color: var(--color-primary-hover, #15803d);
  font-weight: 600;
}
.acc-chip-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
  margin-top: 0.25rem;
}
.acc-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.72rem;
  padding: 0.2rem 0.5rem;
  border-radius: var(--radius-pill, 999px);
  background: var(--color-primary-soft, rgba(22, 163, 74, 0.12));
  color: var(--color-primary-hover, #15803d);
}
.acc-chip-x {
  border: none;
  background: transparent;
  color: inherit;
  cursor: pointer;
  font-size: 0.7rem;
  line-height: 1;
  padding: 0;
}
.acc-grade-pick {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.5rem;
}
.acc-grade-opt {
  border: 1.5px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 10px);
  padding: 0.7rem 0.6rem;
  cursor: pointer;
  text-align: center;
}
.acc-grade-opt:hover {
  border-color: var(--color-primary, #16a34a);
}
.acc-grade-opt.sel {
  border-color: var(--color-primary, #16a34a);
  background: var(--color-primary-soft, rgba(22, 163, 74, 0.08));
}
.acc-grade-t {
  font-weight: 800;
  font-size: 0.85rem;
  margin-bottom: 0.2rem;
  color: var(--color-text-strong, #111827);
}
.acc-grade-t.g-crit {
  color: var(--color-danger, #ef4444);
}
.acc-grade-t.g-norm {
  color: var(--color-warning-text, #b45309);
}
.acc-grade-t.g-exem {
  color: var(--color-text-muted, #475569);
}
.acc-grade-d {
  font-size: 0.66rem;
  color: var(--color-text-muted, #8b94a3);
  line-height: 1.45;
}
.acc-info-box {
  background: var(--color-primary-soft, #f0fdf4);
  border: 1px solid var(--color-primary-soft, #dcfce7);
  border-radius: var(--input-radius, 10px);
  padding: 0.625rem 0.75rem;
  font-size: 0.72rem;
  color: var(--color-text, #374151);
  line-height: 1.55;
  margin-top: 0.5rem;
}
.modal-foot {
  display: flex;
  gap: 0.5rem;
  justify-content: flex-end;
  padding: 0.875rem 1rem;
  border-top: 1px solid var(--color-border, #e5e7eb);
  background: var(--color-bg, #f9fafb);
}
</style>
