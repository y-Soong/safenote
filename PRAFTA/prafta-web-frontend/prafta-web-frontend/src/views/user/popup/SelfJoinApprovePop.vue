<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div class="modal-content-narrow">
        <!-- Title -->
        <div class="modal-header">
          <span>셀프가입 승인</span>
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

        <!-- Form -->
        <div class="form-container">
          <p class="reg-guide">
            승인하면 계정이 활성화되고, 아래 입력값으로 인사정보와 소정근로시간 이력이
            함께 등록됩니다.
          </p>

          <div class="form-row-max">
            <label>신청자</label>
            <input
              class="row-readonly applicant-field"
              :value="props.userNm_p"
              readonly
            />
            <input
              class="row-readonly applicant-field"
              :value="props.userId_p"
              readonly
            />
          </div>

          <div class="form-row-max">
            <label>사업장</label>
            <input class="row-readonly" :value="props.siteNm_p" readonly />
          </div>

          <div class="form-row-max">
            <label>소속부서</label>
            <input class="row-readonly" :value="props.nodeNm_p" readonly />
          </div>

          <div class="form-row-max">
            <label>입사일 *</label>
            <CalendarSrch v-model="hireDateInput" class="hire-date-field" />
          </div>

          <!-- 고용형태 입력은 두지 않는다(2026-08-13 사용자 확정).
               서버 로직은 일용직(DAILY) 여부만 분기하고 정규직·계약직·임원은 완전히 동일하게
               동작한다(Java·SQL 전수 확인 — 조건절은 전부 = 'DAILY' / <> 'DAILY').
               셀프가입자는 일반 직원이므로 REGULAR 고정 전송하고, 계약직·임원 관리가 필요하면
               승인 후 User_01 에서 변경한다. 값을 비우면 Attd_09 배지·엑셀이 '-' 로 떨어지므로
               비우지 않는다. -->

          <div class="form-row-max">
            <label>직급</label>
            <BaseSelect id="rankCd" v-model="rankCd">
              <option :value="''">-</option>
              <option
                v-for="opt in rankOptions"
                :key="opt.baimValDCd"
                :value="opt.baimValDCd"
              >
                {{ opt.baimValDNm }}
              </option>
            </BaseSelect>
          </div>

          <!-- 소정근로시간: 계정 생성 폼(UserInfoPop)과 동일한 선택식.
               풀타임의 주 소정근로 분은 서버가 회사 통상 기준값으로 채운다(화면 하드코딩 금지). -->
          <div class="form-row-max">
            <label>소정근로시간 *</label>
            <div class="std-work-radio-group">
              <label class="std-work-radio">
                <input type="radio" value="FULL" v-model="stdWorkType" />
                <span>풀타임 ({{ stdWorkFullTimeLabel }})</span>
              </label>
              <label class="std-work-radio">
                <input type="radio" value="DIRECT" v-model="stdWorkType" />
                <span>단시간(직접 입력)</span>
              </label>
            </div>
          </div>

          <div class="form-row-max" v-if="isStdWorkDirect">
            <label>주 소정근로 *</label>
            <input
              class="row-short"
              type="number"
              min="0"
              max="168"
              v-model.number="stdWorkHours"
              placeholder="시간"
            />
            <span class="std-work-suffix">시간</span>
            <input
              class="row-short"
              type="number"
              min="0"
              max="59"
              v-model.number="stdWorkMinutes"
              placeholder="분"
            />
            <span class="std-work-suffix">분</span>
          </div>

          <div class="form-row-max" v-if="isStdWorkDirect">
            <label>소정근로 사유 *</label>
            <BaseSelect id="stdWorkReasonCd" v-model="stdWorkReasonCd">
              <option
                v-for="opt in stdWorkReasonOptions"
                :key="opt.reasonCd"
                :value="opt.reasonCd"
              >
                {{ opt.reasonNm }}
              </option>
            </BaseSelect>
          </div>

          <p class="std-work-warning" v-for="(warn, idx) in stdWorkWarnings" :key="idx">
            ⚠ {{ warn }}
          </p>

          <p class="reg-hint" v-if="isStdWorkDirect">
            ⓘ 육아기·임신기·가족돌봄 단축은 적용 기간이 필요해 승인 단계에서는 등록할 수
            없습니다. 승인 후 소정근로시간 관리에서 기간과 함께 등록해 주세요.
          </p>
        </div>

        <!-- F-10 규약: 왼쪽=진행/확정(primary), 오른쪽=이탈(ghost), 폭 균등 -->
        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-primary" :disabled="saving" @click="fnApprove">
              승인 확정
            </button>
            <button class="btn btn-second" @click="$emit('close')">취소</button>
          </div>
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
  defineProps,
  defineEmits,
  onMounted,
  getCurrentInstance,
} from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import BaseSelect from "@/components/common/BaseSelect.vue";
import CalendarSrch from "@/components/common/CalendarSrch.vue";

// [props] 대상 신청자 표시값 + onSaved(승인 성공 시 부모 목록 재조회 콜백).
//   실제 승인 대상은 userCd_p 하나이며, 사업장/부서/권한 검증은 전부 서버가 수행한다.
const props = defineProps({
  userCd_p: String,
  userId_p: String,
  userNm_p: String,
  siteNm_p: String,
  nodeNm_p: String,
  onSaved: Function,
});
const emit = defineEmits(["close"]);

const { proxy } = getCurrentInstance();

// =========================== Ref ===========================
const hireDateInput = ref(""); // CalendarSrch — YYYY-MM-DD
// 고용형태는 화면에서 받지 않고 REGULAR 고정 전송한다(위 template 주석 참조).
// 서버 화이트리스트(User09ServiceImpl.ALLOWED_EMPLOYMENT_TYPES)가 REGULAR 를 허용한다.
const EMPLOYMENT_TYPE_DEFAULT = "REGULAR";
const rankCd = ref("");
const rankOptions = ref([]);

// 소정근로시간 입력값(UserInfoPop 생성 모드와 동일 규약)
const stdWorkType = ref("FULL"); // FULL:풀타임 / DIRECT:단시간(직접 입력)
const stdWorkHours = ref(null);
const stdWorkMinutes = ref(0);
const stdWorkReasonCd = ref("");
const stdWorkReasonOptions = ref([]);
const cmpnyWeekStdMinutes = ref(null); // 회사 통상 기준값(분) — 풀타임 라벨 표기용

const saving = ref(false);

// =========================== Computed ===========================
const isStdWorkDirect = computed(() => stdWorkType.value === "DIRECT");

// 풀타임 라벨은 서버가 내려준 회사 기준값으로 만든다(주 40시간 하드코딩 금지).
const stdWorkFullTimeLabel = computed(() => {
  const minutes = Number(cmpnyWeekStdMinutes.value);
  if (!minutes || minutes <= 0) return "회사 기준";
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return m === 0 ? `주 ${h}시간` : `주 ${h}시간 ${m}분`;
});

const stdWorkInputMinutes = computed(() => {
  const hours = Number(stdWorkHours.value) || 0;
  const minutes = Number(stdWorkMinutes.value) || 0;
  return hours * 60 + minutes;
});

// 주 15시간 미만은 경고(저장 허용 — 서버 판정과 동일 규약).
//   육아기 범위 경고는 단축 사유를 다루는 소정근로시간 관리 화면이 담당한다.
const stdWorkWarnings = computed(() => {
  const list = [];
  if (!isStdWorkDirect.value) return list;
  if (stdWorkInputMinutes.value > 0 && stdWorkInputMinutes.value < 900) {
    list.push(
      "주 소정근로시간이 15시간 미만입니다. 초단시간근로자는 연차·주휴 적용 대상에서 제외될 수 있으니 계약 내용을 확인해 주세요."
    );
  }
  return list;
});

// =========================== Life Cycle ===========================
onMounted(async () => {
  // 입사일 기본값 = 오늘(관리자가 실제 입사일로 조정).
  const today = new Date();
  const yyyy = today.getFullYear();
  const mm = String(today.getMonth() + 1).padStart(2, "0");
  const dd = String(today.getDate()).padStart(2, "0");
  hireDateInput.value = `${yyyy}-${mm}-${dd}`;

  await Promise.all([fnLoadCodeOptions(), fnLoadStdWorkOptions()]);
});

// =========================== Methods ===========================
// 직급(COM007) 셀렉트 옵션. 고용형태(SYS041)는 화면 입력을 없애 조회하지 않는다.
const fnLoadCodeOptions = async () => {
  try {
    const [baseRes] = await Promise.all([
      axios.get("/comApi/baseinfo/base-info-lists", {
        params: {
          cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
          baseCodeList: ["COM007"],
        },
      }),
    ]);

    if (baseRes.status === 200) {
      const rows = baseRes.data?.baseInfoList || [];
      rankOptions.value = rows.filter(
        (o) => o.baimValCd === "COM007" && o.baimValDCd != null
      );
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "코드 조회 중 오류가 발생했습니다.")
    );
  }
};

// 소정근로 입력 옵션(회사 통상 기준값 + 사유 셀렉트). 계정 생성 폼과 같은 EP 를 쓴다 —
//   승인 시트에서 고를 수 있는 사유 집합(통상/단축 제외)이 계정 생성과 동일하기 때문이다.
const fnLoadStdWorkOptions = async () => {
  try {
    const response = await axios.get("/webApi/user01/std-work-options");
    if (response.status === 200) {
      const data = response.data || {};
      cmpnyWeekStdMinutes.value = data.cmpnyWeekStdMinutes ?? null;
      stdWorkReasonOptions.value = data.reasonOptions ?? [];
      if (!stdWorkReasonCd.value && stdWorkReasonOptions.value.length > 0) {
        stdWorkReasonCd.value = stdWorkReasonOptions.value[0].reasonCd;
      }
    }
  } catch (err) {
    stdWorkReasonOptions.value = [];
    await proxy.$alert(
      resolveApiErrorMessage(err, "소정근로시간 옵션 조회 중 오류가 발생했습니다.")
    );
  }
};

// 승인 확정 — POST /webApi/user09/self-join-approve.
//   대상/권한 검증과 이력 등록은 서버가 한 트랜잭션으로 처리한다.
const fnApprove = async () => {
  if (proxy.$util.isEmpty(hireDateInput.value)) {
    await proxy.$alert("입사일을 선택해 주세요.");
    return;
  }
  if (stdWorkType.value !== "FULL" && stdWorkType.value !== "DIRECT") {
    await proxy.$alert("소정근로시간을 선택해 주세요.");
    return;
  }
  if (isStdWorkDirect.value) {
    if (stdWorkInputMinutes.value <= 0) {
      await proxy.$alert("주 소정근로시간을 입력해 주세요.");
      return;
    }
    if (proxy.$util.isEmpty(stdWorkReasonCd.value)) {
      await proxy.$alert("소정근로 사유를 선택해 주세요.");
      return;
    }
  }

  const ok = await proxy.$confirm(
    `${props.userNm_p}(${props.userId_p}) 님의 가입을 승인하시겠습니까?`
  );
  if (!ok) return;

  if (saving.value) return;
  saving.value = true;

  try {
    const response = await axios.post("/webApi/user09/self-join-approve", {
      userCd: props.userCd_p,
      hireDate: (hireDateInput.value || "").replace(/-/g, ""),
      employmentType: EMPLOYMENT_TYPE_DEFAULT,
      rankCd: proxy.$util.isEmpty(rankCd.value) ? null : rankCd.value,
      stdWorkType: stdWorkType.value,
      stdWorkWeekMinutes: isStdWorkDirect.value ? stdWorkInputMinutes.value : null,
      stdWorkReasonCd: isStdWorkDirect.value ? stdWorkReasonCd.value || null : null,
    });

    if (response.status === 200) {
      await proxy.$alert("가입을 승인했습니다.");
      props.onSaved?.();
      emit("close");
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "승인 처리 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  } finally {
    saving.value = false;
  }
};
</script>

<style scoped>
.form-container {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: 1.2rem;
  /* ★가로 넘침 방지: 팝업(.modal-content-narrow)이 이미 max-width 500px 이라
     여기서 520px 를 잡으면 팝업보다 넓어져 가로 스크롤이 생긴다. 폭은 팝업이 결정한다. */
  width: 100%;
  box-sizing: border-box;
  /* 팝업 높이 바운딩 — 내용이 길어지면 본문만 스크롤(modal-popup-guide) */
  max-height: 60vh;
  overflow-y: auto;
  overflow-x: hidden;
}

/* ★공용 .form-row-max 는 "라벨 120px + 입력 flex 1 1 150px" 로 라벨 1 + 입력 1 을 전제한다.
   입력이 2개 이상인 행(신청자, 주 소정근로)에서는 flex-basis 150px + input 고유 최소폭이
   합산돼 팝업 폭(약 460px 가용)을 넘긴다. min-width:0 을 줘야 flex 가 실제로 축소된다. */
.form-container :deep(.form-row-max) {
  min-width: 0;
}
.form-container :deep(.form-row-max input),
.form-container :deep(.form-row-max select) {
  min-width: 0;
}
/* 신청자 행: 이름·아이디 두 입력이 남은 폭을 균등 분배하도록 basis 를 0 으로 */
.applicant-field {
  flex: 1 1 0 !important;
  min-width: 0;
}

.reg-guide {
  margin: 0;
  font-size: var(--btn-font-sm, 11px);
  color: var(--color-text-muted, #4b5563);
  background: var(--color-surface-muted, #f3f4f6);
  border-radius: var(--btn-radius, 8px);
  padding: 0.5rem 0.75rem;
  line-height: 1.5;
}

.reg-hint {
  margin: 0;
  font-size: var(--btn-font-sm, 11px);
  color: var(--color-text-muted, #4b5563);
  line-height: 1.5;
}

/* 소정근로시간 선택식 입력 — UserInfoPop 'C' 모드와 동일 규약(표기 일관성) */
.std-work-radio-group {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}
.std-work-radio {
  display: inline-flex;
  align-items: center;
  /* ★라벨이 좁은 폭에 밀려 글자 단위로 쪼개지던 문제("주 40시 / 간") — 항목 단위 줄바꿈만 허용 */
  white-space: nowrap;
  gap: 0.375rem;
  font-size: 0.8125rem;
  color: var(--color-text-strong, #111827);
  cursor: pointer;
}
.std-work-suffix {
  font-size: 0.75rem;
  color: var(--color-text-muted, #4b5563);
}
.std-work-warning {
  margin: 0;
  padding: 0.5rem 0.75rem;
  border-radius: var(--input-radius, 10px);
  background: var(--color-warning-bg, #fffbeb);
  color: var(--color-warning-text, #b45309);
  font-size: 0.6875rem;
  line-height: 1.5;
}

/* 입사일 캘린더 — 이중 테두리 제거(UserInfoPop 동일 처리) */
.hire-date-field {
  flex: 1;
  padding: 0;
  background: transparent;
  border: none;
}
.hire-date-field :deep(.calendar-input) {
  width: 100%;
  padding: 0.4rem 0.6rem;
  background: var(--color-bg, #f9fafb);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 10px);
  color: var(--color-text-strong, #111827);
  font-size: 0.875rem;
  font-family: "Pretendard", sans-serif;
}
.hire-date-field :deep(.calendar-input):focus {
  border-color: var(--color-border-strong, #d1d5db);
  outline: none;
  box-shadow: 0 0 0 var(--focus-ring-width, 3px) var(--color-focus-ring);
}

/* F-10 규약: 좌우 버튼 폭 균등 */
.modal-footer .btn-group .btn {
  flex: 1;
}
</style>
