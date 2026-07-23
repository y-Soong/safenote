<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div class="modal-content-narrow">
        <div class="modal-header">
          <span>데이터 공유 요청</span>
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

        <div class="form-container">
          <p class="reg-guide">
            연동 중인 상대 회사에 사업장 데이터 제공을 요청합니다.<br />
            상대 회사가 요청을 승인하면 승인 시점의 자료가 복제되며 읽기
            전용으로 조회할 수 있습니다.
          </p>

          <div class="form-row-max">
            <label>제공 회사</label>
            <select v-model="prvCmpnyCd" @change="fnLoadSites">
              <option value="">선택하세요</option>
              <option
                v-for="c in cmpnyList"
                :key="c.cmpnyCd"
                :value="c.cmpnyCd"
              >
                {{ c.cmpnyNm }} ({{ c.cmpnyCd }})
              </option>
            </select>
          </div>

          <div class="form-row-max">
            <label>대상 사업장</label>
            <select v-model="siteCd" :disabled="!prvCmpnyCd">
              <option value="">선택하세요</option>
              <option v-for="s in siteList" :key="s.siteCd" :value="s.siteCd">
                {{ s.siteNm }}
              </option>
            </select>
          </div>
          <p v-if="prvCmpnyCd && !siteList.length" class="create-note">
            선택한 회사와 연동된 사업장이 없습니다. 사업장 연동 관리에서 먼저
            연동하세요.
          </p>

          <div class="form-row-max">
            <label>데이터 유형</label>
            <select v-model="dataType">
              <option value="ATTD">근태</option>
              <option value="RISK">위험성평가</option>
              <option value="NEARMISS">아차사고</option>
            </select>
          </div>

          <div class="form-row-max">
            <label>대상 기간</label>
            <CalendarSrch
              v-model="periodStr"
              class="period-date"
              :max-date="periodEnd || todayStr"
            />
            <span class="range-sep">~</span>
            <CalendarSrch
              v-model="periodEnd"
              class="period-date"
              :min-date="periodStr"
              :max-date="todayStr"
            />
          </div>

          <div v-if="dataType === 'ATTD'" class="form-row-max">
            <label>마감 근태만</label>
            <label class="chk-inline">
              <input v-model="closedOnly" type="checkbox" />
              <span>근태 마감이 완료된 자료만 받습니다(권장)</span>
            </label>
          </div>
          <p v-else class="create-note create-note--info">
            해당 유형은 마감과 무관하게 확정된 자료만 제공됩니다.
          </p>

          <div class="form-row-max form-row-top">
            <label>제공 목적</label>
            <textarea
              v-model.trim="purpose"
              class="purpose-input"
              placeholder="제공 목적 (필수, 최대 500자)"
              maxlength="500"
              rows="3"
            ></textarea>
          </div>
        </div>

        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-primary" @click="$emit('close')">
              취소
            </button>
            <button
              class="btn btn-primary"
              :disabled="!prvCmpnyCd || !siteCd || !purpose"
              @click="fnCreate"
            >
              요청
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
/* eslint-disable */
import { ref, onMounted, defineProps, defineEmits, getCurrentInstance } from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import CalendarSrch from "@/components/common/CalendarSrch.vue";

const props = defineProps({ onSaved: Function });
const emit = defineEmits(["close"]);
const { proxy } = getCurrentInstance();

// CalendarSrch 모델값("YYYY-MM-DD") → 서버 포맷("YYYYMMDD").
const toYmd = (v) => (v || "").replace(/-/g, "");

// 오늘("YYYY-MM-DD") — 캘린더 상한(미래 기간 요청 금지)에 사용.
const now = new Date();
const todayStr = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(
  now.getDate()
).padStart(2, "0")}`;

// 이번 달 1일~말일 기본값 — 말일이 아직 오지 않았으면(미래 금지 규칙) 오늘까지로 자른다.
const fmt = (d) =>
  `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
const monthFirstStr = fmt(new Date(now.getFullYear(), now.getMonth(), 1));
const monthLastStr = fmt(new Date(now.getFullYear(), now.getMonth() + 1, 0));

// =========================== Ref ===========================
const cmpnyList = ref([]); // 관계 ACCEPTED 상대 회사
const siteList = ref([]); // 선택 회사와 체인이 있는 내 사업장
const prvCmpnyCd = ref("");
const siteCd = ref("");
const dataType = ref("ATTD");
const periodStr = ref(monthFirstStr);
const periodEnd = ref(monthLastStr > todayStr ? todayStr : monthLastStr);
const closedOnly = ref(true); // 기본 ON (마스터 §1-6)
const purpose = ref("");

// 요청 중복 클릭 방지 플래그.
const saving = ref(false);

// 데이터 유형 라벨(확인 메시지용).
const dataTypeLabel = (t) => ({ ATTD: "근태", RISK: "위험성평가", NEARMISS: "아차사고" }[t] || t);

// =========================== Life Cycle ===========================
// 후보 조회 — GET /webApi/subcon03/share-req-candidates (회사 목록만).
//   관계 수립 회사가 0건이면 요청 자체가 불가하므로 안내 후 닫는다.
onMounted(async () => {
  try {
    const response = await axios.get("/webApi/subcon03/share-req-candidates");

    if (response.status === 200) {
      cmpnyList.value = response.data?.cmpnyList || [];

      if (!cmpnyList.value.length) {
        await proxy.$alert("연동 중인 회사가 없습니다.\n연동회사 관리에서 먼저 관계를 수립하세요.");
        emit("close");
      }
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
    emit("close");
  }
});

// =========================== Methods ===========================
// 선택 회사와 사업장 연동 체인이 있는 "내" 사업장 조회.
//   (제공사 사업장 목록은 서버가 내려주지 않는다 — 대상 사업장은 서버가 체인으로 해석)
const fnLoadSites = async () => {
  siteCd.value = "";
  siteList.value = [];

  if (!prvCmpnyCd.value) return;

  try {
    const response = await axios.get("/webApi/subcon03/share-req-candidates", {
      params: { prvCmpnyCd: prvCmpnyCd.value },
    });

    if (response.status === 200) {
      siteList.value = response.data?.siteList || [];
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "사업장 조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// 요청 생성 — POST /webApi/subcon03/share-req-create.
//   기간/목적은 프론트에서 1차 검증하되, 서버가 동일 규칙(형식·미래 금지·12개월·목적 필수)을 재검증한다.
const fnCreate = async () => {
  if (!prvCmpnyCd.value || !siteCd.value) {
    await proxy.$alert("제공 회사와 대상 사업장을 선택해주세요.");
    return;
  }
  if (!periodStr.value || !periodEnd.value) {
    await proxy.$alert("대상 기간을 선택해주세요.");
    return;
  }
  if (periodStr.value > periodEnd.value) {
    await proxy.$alert("기간 시작일이 종료일보다 늦을 수 없습니다.");
    return;
  }

  const today = new Date();
  const todayYmd = `${today.getFullYear()}${String(today.getMonth() + 1).padStart(2, "0")}${String(
    today.getDate()
  ).padStart(2, "0")}`;
  if (toYmd(periodEnd.value) > todayYmd) {
    await proxy.$alert("미래 기간은 요청할 수 없습니다.");
    return;
  }

  const start = new Date(periodStr.value);
  const limit = new Date(start.getFullYear(), start.getMonth() + 12, start.getDate());
  if (new Date(periodEnd.value) > limit) {
    await proxy.$alert("요청 기간은 최대 12개월까지 가능합니다.");
    return;
  }

  if (!purpose.value) {
    await proxy.$alert("제공 목적을 입력해주세요.");
    return;
  }

  const cmpnyNm = cmpnyList.value.find((c) => c.cmpnyCd === prvCmpnyCd.value)?.cmpnyNm || prvCmpnyCd.value;
  const siteNm = siteList.value.find((s) => s.siteCd === siteCd.value)?.siteNm || siteCd.value;

  const ok = await proxy.$confirm(
    `'${cmpnyNm}'에 '${siteNm}' ${dataTypeLabel(dataType.value)} 자료 제공을 요청하시겠습니까?`
  );
  if (!ok) return;

  if (saving.value) return;
  saving.value = true;

  try {
    // 마감 옵션은 근태(ATTD) 전용 — 다른 유형은 서버가 무시하지만 프론트도 'N' 으로 고정 전송한다.
    const response = await axios.post("/webApi/subcon03/share-req-create", {
      prvCmpnyCd: prvCmpnyCd.value,
      siteCd: siteCd.value,
      dataType: dataType.value,
      periodStr: toYmd(periodStr.value),
      periodEnd: toYmd(periodEnd.value),
      closedOnlyYn: dataType.value === "ATTD" ? (closedOnly.value ? "Y" : "N") : "N",
      purpose: purpose.value,
    });

    if (response.status === 200) {
      await proxy.$alert("데이터 공유를 요청했습니다.");
      props.onSaved?.();
      emit("close");
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "공유 요청 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  } finally {
    saving.value = false;
  }
};
</script>

<style scoped>
/* 헤더와 안내문 사이 간격 축소 — 전역 modal-popup-guide.css의 padding: 20px 중 상단만 줄임 */
.prafta-modal-popup .form-container {
  padding-top: 12px;
}

/* 상단 안내문 — DailyBlacklistRegPop .reg-guide 전례 + 본문 폰트 크기로 확대 */
.reg-guide {
  margin: 0;
  font-size: var(--btn-font-lg, 14px);
  color: var(--color-text-muted, #4b5563);
  background: var(--color-warning-bg, #fef3c7);
  border-radius: var(--btn-radius, 8px);
  padding: 0.5rem 0.75rem;
  line-height: 1.5;
}

.form-row-max {
  display: flex;
  align-items: center;
  gap: var(--space-sm, 0.5rem);
}
.form-row-top {
  align-items: flex-start;
}
.form-row-max label {
  width: 100px;
  flex-shrink: 0;
  color: var(--color-text-muted, #6b7280);
}
.form-row-max select {
  flex: 1;
  min-width: 0;
}
/* 전역 modal.css의 .form-row-max span(flex:0 0 50px) 고정폭이 '~' 양옆 공백을 만들므로 재정의 */
.form-row-max .range-sep {
  flex: 0 0 auto;
  width: auto;
  color: var(--color-text-muted, #6b7280);
}
/* 기간 캘린더 — FROM/TO 가 남은 행 폭을 균등 분할해 우측 끝단을 다른 입력란과 정렬.
   내부 입력은 모달 입력 스타일에 맞춤(LeavePromotionAutoBatchPop .lpb-date 전례) */
.form-row-max .period-date {
  flex: 1 1 0;
  min-width: 0;
}
.period-date :deep(.calendar-input) {
  box-sizing: border-box;
  width: 100%;
  padding: 0.4rem 0.5rem;
  background: var(--color-bg, #f9fafb);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 10px);
  font-size: 0.875rem;
  color: var(--color-text-strong, #111827);
}
.period-date :deep(.calendar-input:focus) {
  border-color: var(--color-border-strong, #d1d5db);
  outline: none;
  box-shadow: 0 0 0 var(--focus-ring-width, 3px) var(--color-focus-ring);
}
/* 전역 modal.css의 .form-row-max label(120px)·input(flex:1 1 150px)·span(flex:0 0 50px)
   규칙이 체크박스 행에 그대로 적용되어 레이아웃이 깨지므로 여기서 재정의 */
.form-row-max label.chk-inline {
  display: flex;
  align-items: center;
  gap: var(--space-sm, 0.5rem);
  width: auto;
  flex: 1;
  min-width: 0;
  font-weight: 400;
  color: var(--color-text, #374151);
}
.form-row-max .chk-inline input[type="checkbox"] {
  flex: 0 0 auto;
  width: auto;
  padding: 0;
  accent-color: var(--color-primary, #16a34a);
}
.form-row-max .chk-inline span {
  flex: 0 1 auto;
  min-width: 0;
  font-weight: 400;
  line-height: 1.4;
}
.purpose-input {
  flex: 1;
  min-width: 0;
  resize: vertical;
}
.create-note {
  margin-top: var(--space-sm, 0.5rem);
  color: var(--color-danger, #dc2626);
  font-size: var(--btn-font-sm, 12px);
}
.create-note--info {
  color: var(--color-text-muted, #6b7280);
}
</style>
