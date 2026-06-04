<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-normal modal-content-leave-type"
        :style="positionStyle"
        ref="modalRef"
      >
        <div class="modal-header" @mousedown="startDrag">
          <span>연차 타입 생성</span>
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

        <div class="modal-body-leave">
          <!-- A. 타입 구분 -->
          <div class="section-block">
            <div
              class="section-header"
              :class="{ expanded: expanded.A }"
              @click="expanded.A = !expanded.A"
            >
              <span class="section-title"
                >A. 타입 구분 <span class="required-tag">필수</span></span
              >
              <span class="section-chevron">
                {{ expanded.A ? "▲" : "▼" }}
              </span>
            </div>
            <div v-show="expanded.A" class="section-body">
              <div class="type-cards" :class="{ 'edit-mode': isEditMode }">
                <div
                  class="type-card"
                  :class="{ selected: leaveType === '01' }"
                  @click="!isEditMode && (leaveType = '01')"
                >
                  <div class="type-card-inner">
                    <span
                      class="type-radio"
                      :class="{ checked: leaveType === '01' }"
                    ></span>
                    <div class="type-content">
                      <strong>사용자 신청 타입</strong>
                      <p>
                        관리자가 타입을 만들어두면 사용자가 직접 신청할 수 있는
                        타입
                      </p>
                    </div>
                    <span v-if="leaveType === '01'" class="type-check">✓</span>
                  </div>
                </div>
                <div
                  class="type-card"
                  :class="{ selected: leaveType === '02' }"
                  @click="!isEditMode && (leaveType = '02')"
                >
                  <div class="type-card-inner">
                    <span
                      class="type-radio"
                      :class="{ checked: leaveType === '02' }"
                    ></span>
                    <div class="type-content">
                      <strong>관리자 부여 타입</strong>
                      <p>관리자가 사용자에게 부여해야만 사용할 수 있는 타입</p>
                    </div>
                    <span v-if="leaveType === '02'" class="type-check">✓</span>
                  </div>
                </div>
              </div>
              <!-- 관리자 부여 타입일 때만 부여 방식 표시 -->
              <div v-if="leaveType === '02'" class="grant-method-row">
                <span class="grant-method-label">부여 방식</span>
                <div class="grant-method-options">
                  <label
                    v-for="opt in (systCodeArr['SYS022'] || []).filter(
                      (o) => o.systValDCd != null
                    )"
                    :key="opt.systValDCd"
                    class="radio-option"
                  >
                    <input
                      type="radio"
                      v-model="grantType"
                      :value="opt.systValDCd"
                      :disabled="isEditMode"
                    />
                    <span>{{ opt.systValDNm }}</span>
                  </label>
                </div>
              </div>
            </div>
          </div>

          <!-- B. 기본 정보 -->
          <div class="section-block">
            <div
              class="section-header"
              :class="{ expanded: expanded.B }"
              @click="expanded.B = !expanded.B"
            >
              <span class="section-title">B. 기본 정보</span>
              <span class="section-chevron">{{ expanded.B ? "▲" : "▼" }}</span>
            </div>
            <div v-show="expanded.B" class="section-body">
              <div class="form-grid">
                <div class="form-item">
                  <label>연차코드 <span class="required">*</span></label>
                  <input
                    v-model.trim="leaveNo"
                    type="text"
                    placeholder="예: LEAVE_01"
                    :readonly="isEditMode"
                  />
                </div>
                <div class="form-item">
                  <label>연차명 <span class="required">*</span></label>
                  <input
                    v-model.trim="leaveNm"
                    type="text"
                    placeholder="예: 하계휴가"
                  />
                </div>
                <div class="form-item">
                  <label>유급구분 <span class="required">*</span></label>
                  <select v-model="paidType" :disabled="isEditMode">
                    <option
                      v-for="opt in (systCodeArr['SYS023'] || []).filter(
                        (o) => o.systValDCd != null
                      )"
                      :key="opt.systValDCd"
                      :value="opt.systValDCd"
                    >
                      {{ opt.systValDNm }}
                    </option>
                  </select>
                </div>
                <div class="form-item">
                  <label>휴가성격 <span class="required">*</span></label>
                  <select v-model="leaveNatureType" :disabled="isEditMode">
                    <option
                      v-for="opt in (systCodeArr['SYS024'] || []).filter(
                        (o) => o.systValDCd != null
                      )"
                      :key="opt.systValDCd"
                      :value="opt.systValDCd"
                    >
                      {{ opt.systValDNm }}
                    </option>
                  </select>
                </div>
                <div class="form-item">
                  <label>사용여부</label>
                  <div class="toggle-wrap">
                    <label class="toggle-switch">
                      <input
                        type="checkbox"
                        v-model="useYn"
                        true-value="Y"
                        false-value="N"
                      />
                      <span class="toggle-slider"></span>
                    </label>
                  </div>
                </div>
                <div class="form-item">
                  <label>비고(설명)</label>
                  <input
                    v-model.trim="leaveDesc"
                    type="text"
                    placeholder="설명을 입력하세요"
                  />
                </div>
              </div>
            </div>
          </div>

          <!-- C. 사용 규칙 (사용자 신청) / C. 부여 규칙 (관리자 부여) -->
          <div class="section-block">
            <div
              class="section-header"
              :class="{ expanded: expanded.C }"
              @click="expanded.C = !expanded.C"
            >
              <span class="section-title">
                {{ leaveType === "01" ? "C. 사용 규칙" : "C. 부여 규칙" }}
                <span v-if="leaveType === '02'" class="required-tag">필수</span>
              </span>
              <span class="section-chevron">{{ expanded.C ? "▲" : "▼" }}</span>
            </div>
            <div v-show="expanded.C" class="section-body">
              <!-- 사용자 신청 타입 -->
              <div v-if="leaveType === '01'" class="form-grid">
                <div class="form-item">
                  <label>최대 신청일수 <span class="required">*</span></label>
                  <input
                    v-model.number="maxAplyDays"
                    type="number"
                    min="1"
                    :readonly="isEditMode"
                  />
                </div>
                <div class="form-item">
                  <label>사용단위 <span class="required">*</span></label>
                  <select v-model="useUnitType" :disabled="isEditMode">
                    <option
                      v-for="opt in (systCodeArr['SYS025'] || []).filter(
                        (o) => o.systValDCd != null
                      )"
                      :key="opt.systValDCd"
                      :value="opt.systValDCd"
                    >
                      {{ opt.systValDNm }}
                    </option>
                  </select>
                </div>
                <div
                  class="form-item"
                  :class="{ 'full-width': availTermType !== '03' }"
                >
                  <label>사용 가능기간 <span class="required">*</span></label>
                  <select v-model="availTermType" :disabled="isEditMode">
                    <option
                      v-for="opt in (systCodeArr['SYS026'] || []).filter(
                        (o) => o.systValDCd != null
                      )"
                      :key="opt.systValDCd"
                      :value="opt.systValDCd"
                    >
                      {{ opt.systValDNm }}
                    </option>
                  </select>
                </div>
                <div v-if="availTermType === '03'" class="form-item full-width">
                  <label>기간 설정</label>
                  <div class="period-date-range">
                    <MonthDayPickerInput
                      v-model="availFromDt"
                      :readonly="isEditMode"
                    />
                    <span class="period-date-sep">~</span>
                    <MonthDayPickerInput
                      v-model="availToDt"
                      :readonly="isEditMode"
                    />
                  </div>
                </div>
              </div>
              <!-- 관리자 부여 타입 - 자동 부여 -->
              <div v-else-if="grantType === '01'" class="admin-grant-auto">
                <div class="auto-grant-panel">
                  <p class="auto-grant-title">① 자동 부여 기준 설정</p>
                  <div class="form-grid">
                    <div class="form-item">
                      <label>기준일</label>
                      <select v-model="grantBaseType" :disabled="isEditMode">
                        <option
                          v-for="opt in (systCodeArr['SYS027'] || []).filter(
                            (o) => o.systValDCd != null
                          )"
                          :key="opt.systValDCd"
                          :value="opt.systValDCd"
                        >
                          {{ opt.systValDNm }}
                        </option>
                      </select>
                    </div>
                    <div class="form-item">
                      <label>
                        {{ grantBaseType === "03" ? "부여일" : "실행 시점" }}
                      </label>
                      <div v-if="grantBaseType === '03'" class="exec-time-row">
                        <MonthDayPickerInput
                          v-model="grantAssignMmdd"
                          :readonly="isEditMode"
                        />
                      </div>
                      <div v-else class="exec-time-row">
                        <select
                          v-model.number="grantOffsetMonth"
                          :disabled="isEditMode"
                        >
                          <option v-for="m in 11" :key="m" :value="m">
                            {{ m }}
                          </option>
                        </select>
                        <span class="period-unit">개월 전 1일</span>
                      </div>
                    </div>
                  </div>
                  <p class="auto-grant-info">
                    <span class="info-icon">ⓘ</span>
                    {{ autoGrantInfoMsg }}
                  </p>
                </div>
                <div class="form-grid auto-grant-rule">
                  <div class="form-item">
                    <label>사용단위 <span class="required">*</span></label>
                    <select v-model="useUnitType" :disabled="isEditMode">
                      <option
                        v-for="opt in (systCodeArr['SYS025'] || []).filter(
                          (o) => o.systValDCd != null
                        )"
                        :key="opt.systValDCd"
                        :value="opt.systValDCd"
                      >
                        {{ opt.systValDNm }}
                      </option>
                    </select>
                  </div>
                  <div class="form-item">
                    <label>사용 가능기간 (선택)</label>
                    <select v-model="adminAvailTermType" :disabled="isEditMode">
                      <option
                        v-for="opt in (systCodeArr['SYS026'] || []).filter(
                          (o) => o.systValDCd != null
                        )"
                        :key="opt.systValDCd"
                        :value="opt.systValDCd"
                      >
                        {{ opt.systValDNm }}
                      </option>
                    </select>
                  </div>
                  <div
                    v-if="adminAvailTermType === '03'"
                    class="form-item full-width"
                  >
                    <label>기간 설정</label>
                    <div class="period-date-range">
                      <div class="period-date-calendar">
                        <CalendarSrch
                          v-model="adminAvailFromDt"
                          :readonly="isEditMode"
                        />
                      </div>
                      <span class="period-date-sep">~</span>
                      <div class="period-date-calendar">
                        <CalendarSrch
                          v-model="adminAvailToDt"
                          :readonly="isEditMode"
                        />
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              <!-- 관리자 부여 타입 - 수동 부여 -->
              <div v-else class="form-grid">
                <div class="form-item">
                  <label>사용단위 <span class="required">*</span></label>
                  <select v-model="useUnitType" :disabled="isEditMode">
                    <option
                      v-for="opt in (systCodeArr['SYS025'] || []).filter(
                        (o) => o.systValDCd != null
                      )"
                      :key="opt.systValDCd"
                      :value="opt.systValDCd"
                    >
                      {{ opt.systValDNm }}
                    </option>
                  </select>
                </div>
                <div class="form-item">
                  <label>사용 가능기간 (선택)</label>
                  <select v-model="adminAvailTermType" :disabled="isEditMode">
                    <option
                      v-for="opt in (systCodeArr['SYS026'] || []).filter(
                        (o) => o.systValDCd != null
                      )"
                      :key="opt.systValDCd"
                      :value="opt.systValDCd"
                    >
                      {{ opt.systValDNm }}
                    </option>
                  </select>
                </div>
                <div
                  v-if="adminAvailTermType === '03'"
                  class="form-item full-width"
                >
                  <label>기간 설정</label>
                  <div class="period-date-range">
                    <div class="period-date-calendar">
                      <CalendarSrch
                        v-model="adminAvailFromDt"
                        :readonly="isEditMode"
                      />
                    </div>
                    <span class="period-date-sep">~</span>
                    <div class="period-date-calendar">
                      <CalendarSrch
                        v-model="adminAvailToDt"
                        :readonly="isEditMode"
                      />
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- D. 결재 및 증빙 -->
          <div class="section-block">
            <div
              class="section-header"
              :class="{ expanded: expanded.D }"
              @click="expanded.D = !expanded.D"
            >
              <span class="section-title">D. 결재 및 증빙</span>
              <span class="section-chevron">{{ expanded.D ? "▲" : "▼" }}</span>
            </div>
            <div v-show="expanded.D" class="section-body">
              <div class="form-grid">
                <div class="form-item full-width">
                  <label>결재 여부</label>
                  <div class="toggle-wrap">
                    <label class="toggle-switch">
                      <input
                        type="checkbox"
                        v-model="aprvUseYn"
                        true-value="Y"
                        false-value="N"
                        :disabled="isEditMode"
                      />
                      <span class="toggle-slider"></span>
                    </label>
                  </div>
                  <p class="field-desc">
                    활성화 시 사용자의 연차 신청에 결재가 필요합니다.
                  </p>
                </div>
                <div class="form-item full-width">
                  <label>증빙 여부</label>
                  <div class="toggle-wrap">
                    <label class="toggle-switch">
                      <input
                        type="checkbox"
                        v-model="evidenceYn"
                        true-value="Y"
                        false-value="N"
                        :disabled="isEditMode"
                      />
                      <span class="toggle-slider"></span>
                    </label>
                  </div>
                  <p class="field-desc">
                    활성화 시 사용자가 연차 신청 시 증빙 자료를 첨부해야 합니다.
                  </p>
                </div>
                <div v-if="evidenceYn === 'Y'" class="form-item full-width">
                  <label>증빙 안내 문구</label>
                  <textarea
                    v-model.trim="evidenceGuideMsg"
                    class="evidence-guide-textarea"
                    rows="3"
                    placeholder="예: 가족관계증명서, 청첩장 또는 예식장 계약서 중 1부를 첨부해 주세요."
                  />
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <div class="footer-actions">
            <button class="btn btn-secondary" @click="$emit('close')">
              취소
            </button>
            <button
              class="btn btn-primary"
              :disabled="!canSave"
              @click="fnSave"
            >
              저장
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
// ================ Imports ================
import { ref, computed, getCurrentInstance, onMounted } from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import axios from "@/api/axios";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";
import MonthDayPickerInput from "@/components/common/MonthDayPickerInput.vue";
import CalendarSrch from "@/components/common/CalendarSrch.vue";

// ================ Props & Emits ================
const props = defineProps({
  onSearch: { type: Function, default: null },
  editRow: { type: Object, default: null }, // 수정 시 row 전체 데이터
});
const emit = defineEmits(["close"]);

// ================ Instance & Composables ================
const { proxy } = getCurrentInstance();
const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 2,
});

// ================ Refs (Variables) ================
const systCodeArr = ref([]);
const expanded = ref({ A: true, B: true, C: true, D: true });
const cmpnyCd = ref(""); // 회사코드 (CMPNY_CD)
const leaveNo = ref(""); // 연차번호 (LEAVE_NO)
const leaveNm = ref(""); // 연차명 (LEAVE_NM)
const leaveType = ref("01"); // SYS021: '01'=사용자 신청, '02'=관리자 부여
const grantType = ref("02"); // SYS022: '01'=자동부여, '02'=수동부여 (admin only)
const paidType = ref("01"); // SYS023: '01'=유급, '02'=무급
const leaveNatureType = ref("02"); // SYS024: '01'=법정, '02'=특별
const useYn = ref("Y");
const leaveDesc = ref("");

// 사용자 신청 타입용
const maxAplyDays = ref(1);
const useUnitType = ref("00"); // SYS025: 00=1일 / 01=반차 / 02=시간차(2시간) / 03=시간차(1시간) / 04=시간차(30분)
const availTermType = ref("02"); // SYS026: '01'=설정안함, '02'=해당년도 내, '03'=기간설정
const currentYear = new Date().getFullYear();
// 사용자 신청 기간설정: MMDD 형식 (예: 0101, 1231)
const availFromDt = ref("0101");
const availToDt = ref("1231");

// 관리자 부여 타입용
const adminAvailTermType = ref("02"); // SYS026
const adminAvailFromDt = ref(`${currentYear}-01-01`);
const adminAvailToDt = ref(`${currentYear}-12-31`);
// 자동 부여용
const grantBaseType = ref("01"); // SYS027: '01'=입사일, '02'=생일, '03'=부여일지정
const grantOffsetMonth = ref(1);
// 자동 부여 - '부여일지정' 선택 시 MMDD 4자리 (예: 0901)
const grantAssignMmdd = ref("0101");

// 결재 및 증빙
const aprvUseYn = ref("N");
const evidenceYn = ref("N");
const evidenceGuideMsg = ref("");

// ================ Computed ================
const positionStyle = computed(() => {
  const padding = 16;
  const modalWidth = 1120;
  const modalHeight = 700;
  const maxX = window.innerWidth - (modalWidth + padding);
  const maxY = window.innerHeight - (modalHeight + padding);
  const x = Math.max(padding, Math.min(maxX, position.value.x));
  const y = Math.max(padding, Math.min(maxY, position.value.y));
  return { top: y + "px", left: x + "px" };
});

const autoGrantInfoMsg = computed(() => {
  const opts = (systCodeArr.value["SYS027"] || []).filter(
    (o) => o.systValDCd != null
  );
  const std =
    opts.find((o) => o.systValDCd === grantBaseType.value)?.systValDNm ||
    "입사일";
  if (grantBaseType.value === "03") {
    const v = String(grantAssignMmdd.value || "");
    const mm = v.length >= 2 ? v.slice(0, 2) : "MM";
    const dd = v.length >= 4 ? v.slice(2, 4) : "DD";
    return `(${std}, ${mm}월 ${dd}일) 매년 ${mm}월 ${dd}일 00시에 해당 연차가 사용자에게 부여됩니다.`;
  }
  const months = grantOffsetMonth.value;
  return `(${std}, ${months}개월 전 1일) ${std}이 9월 16일이면 8월 1일 00시에 해당 연차가 사용자에게 부여됩니다.`;
});

// leaveCd 기준 수정 모드: 사용여부, 비고(설명)만 편집 가능
const isEditMode = computed(() => !!props.editRow && !!props.editRow.leaveCd);

// MMDD <-> MM-DD 변환 (사용자 신청 기간설정용)
const toMMDD = (val) => {
  if (!val) return "";
  return String(val).replace(/\D/g, "").slice(0, 4);
};

// prafta-044-FU2: 관리자 부여 사용기간(절대 날짜) 변환
// CalendarSrch 네이티브(YYYY-MM-DD) -> 저장 payload(YYYYMMDD 8자)
const toYmd8 = (val) => {
  if (!val) return "";
  return String(val).replace(/\D/g, "").slice(0, 8);
};
// DB 저장값(YYYYMMDD 8자) -> CalendarSrch 채우기(YYYY-MM-DD)
const fromYmd8 = (val) => {
  if (!val) return null;
  const s = String(val).replace(/\D/g, "");
  if (s.length !== 8) return null;
  return `${s.slice(0, 4)}-${s.slice(4, 6)}-${s.slice(6, 8)}`;
};

const canSave = computed(() => {
  if (!leaveNo.value.trim() || !leaveNm.value.trim()) return false;
  if (leaveType.value === "01") {
    if (maxAplyDays.value <= 0 || !useUnitType.value) return false;
    if (availTermType.value === "03") {
      const from = toMMDD(availFromDt.value);
      const to = toMMDD(availToDt.value);
      if (from.length !== 4 || to.length !== 4) return false;
      if (from > to) return false;
    }
  }
  if (leaveType.value === "02" && grantType.value === "02") {
    // 관리자 수동부여: 사용단위 필수 (소비 단위 결정 출처)
    if (!useUnitType.value) return false;
    if (adminAvailTermType.value === "03") {
      if (!adminAvailFromDt.value || !adminAvailToDt.value) return false;
      if (adminAvailFromDt.value > adminAvailToDt.value) return false;
    }
  }
  if (leaveType.value === "02" && grantType.value === "01") {
    // 관리자 부여 + 자동 부여: 사용단위 필수 (수동부여와 동일, 소비 단위 출처)
    if (!useUnitType.value) return false;
    if (adminAvailTermType.value === "03") {
      if (!adminAvailFromDt.value || !adminAvailToDt.value) return false;
      if (adminAvailFromDt.value > adminAvailToDt.value) return false;
    }
    // 정책서 §8.1.2에 따라 기준일별 cross-field 검증
    if (grantBaseType.value === "01" || grantBaseType.value === "02") {
      // 입사일/생일 기준: 실행시점 개월수 1-11 필수
      const m = Number(grantOffsetMonth.value);
      if (!Number.isInteger(m) || m < 1 || m > 11) return false;
    } else if (grantBaseType.value === "03") {
      // 부여일지정: MMDD 4자리 + 월별 일수 검증 (02/29 허용, 평년 02/28 fallback은 서버 책임)
      const v = toMMDD(grantAssignMmdd.value);
      if (v.length !== 4) return false;
      const mm = Number(v.slice(0, 2));
      const dd = Number(v.slice(2, 4));
      if (mm < 1 || mm > 12 || dd < 1) return false;
      const maxDay = mm === 2 ? 29 : [4, 6, 9, 11].includes(mm) ? 30 : 31;
      if (dd > maxDay) return false;
    } else {
      // SYS027 코드값 외 입력 거부
      return false;
    }
  }
  return true;
});

// ================ API Functions ================
const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-lists", {
      params: {
        systCodeList: [
          "SYS003",
          "SYS021",
          "SYS022",
          "SYS023",
          "SYS024",
          "SYS025",
          "SYS026",
          "SYS027",
        ],
      },
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
      const firstValid = (key) => {
        const arr = grouped[key] || [];
        const opt = arr.find((o) => o.systValDCd != null);
        return opt?.systValDCd ?? null;
      };
      if (props.editRow) {
        // 수정 모드: editRow(row) 값으로 폼 채우기 - row 구조 기준
        const r = props.editRow;
        cmpnyCd.value = r.cmpnyCd ?? cmpnyCd.value;
        leaveNo.value = r.leaveNo ?? leaveNo.value;
        leaveNm.value = r.leaveNm ?? leaveNm.value;
        leaveType.value =
          r.leaveType ?? firstValid("SYS021") ?? leaveType.value;
        grantType.value =
          r.grantType ?? firstValid("SYS022") ?? grantType.value;
        paidType.value = r.paidType ?? firstValid("SYS023") ?? paidType.value;
        leaveNatureType.value =
          r.leaveNatureType ?? firstValid("SYS024") ?? leaveNatureType.value;
        useYn.value = r.useYn ?? useYn.value;
        leaveDesc.value = r.leaveDesc ?? leaveDesc.value;
        // leaveDays: 사용자신청=maxAplyDays (관리자 부여 타입은 부여일수 없음)
        const days = r.leaveDays != null ? Number(r.leaveDays) : null;
        if (days != null) {
          maxAplyDays.value = days;
        }
        useUnitType.value =
          r.useUnitType ?? firstValid("SYS025") ?? useUnitType.value;
        availTermType.value =
          r.availTermType ?? firstValid("SYS026") ?? availTermType.value;
        // availFromDt, availToDt: MMDD 형식 (API가 YYYY-MM-DD면 추출)
        const normMMDD = (v) => {
          if (!v) return null;
          const s = String(v);
          if (s.length >= 4 && /^\d{4}/.test(s)) return s.slice(0, 4);
          if (/^\d{4}-\d{2}-\d{2}/.test(s))
            return s.slice(5, 7) + s.slice(8, 10);
          return s.replace(/\D/g, "").slice(0, 4).padStart(4, "0") || null;
        };
        availFromDt.value = normMMDD(r.availFromDt) ?? availFromDt.value;
        availToDt.value = normMMDD(r.availToDt) ?? availToDt.value;
        adminAvailTermType.value =
          r.adminAvailTermType ??
          firstValid("SYS026") ??
          adminAvailTermType.value;
        // prafta-044-FU2: DB는 YYYYMMDD 8자 → CalendarSrch가 받는 YYYY-MM-DD로 변환
        adminAvailFromDt.value = fromYmd8(r.adminAvailFromDt) ?? adminAvailFromDt.value;
        adminAvailToDt.value = fromYmd8(r.adminAvailToDt) ?? adminAvailToDt.value;
        // 자동 부여 기준일, 실행시점
        grantBaseType.value =
          r.grantBaseType ?? firstValid("SYS027") ?? grantBaseType.value;
        grantOffsetMonth.value =
          r.grantOffsetMonth != null
            ? Number(r.grantOffsetMonth)
            : grantOffsetMonth.value;
        // 자동부여 지정일 (MMDD)
        if (r.grantAssignMmdd != null && String(r.grantAssignMmdd) !== "") {
          grantAssignMmdd.value = String(r.grantAssignMmdd)
            .replace(/\D/g, "")
            .slice(0, 4)
            .padStart(4, "0");
        }
        // 결재 및 증빙
        aprvUseYn.value = r.aprvUseYn ?? aprvUseYn.value;
        evidenceYn.value = r.evidenceYn ?? evidenceYn.value;
        evidenceGuideMsg.value = r.evidenceGuideMsg ?? evidenceGuideMsg.value;
      } else {
        // 신규 모드: systCode 첫 번째 유효 옵션으로 초기값 설정
        leaveType.value = firstValid("SYS021") ?? leaveType.value;
        grantType.value = firstValid("SYS022") ?? grantType.value;
        paidType.value = firstValid("SYS023") ?? paidType.value;
        leaveNatureType.value = firstValid("SYS024") ?? leaveNatureType.value;
        useUnitType.value = firstValid("SYS025") ?? useUnitType.value;
        availTermType.value = firstValid("SYS026") ?? availTermType.value;
        adminAvailTermType.value =
          firstValid("SYS026") ?? adminAvailTermType.value;
        grantBaseType.value = firstValid("SYS027") ?? grantBaseType.value;
      }
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

const fnSave = async () => {
  if (!canSave.value) return;

  const ok = await proxy.$confirm(getMessage(MSG.LEAVE_SAVE_CONFIRM));
  if (!ok) return;

  try {
    // TODO: API 연동
    // eslint-disable-next-line no-unused-vars
    const payload = {
      // 연차코드 (수정 시 editRow.leaveCd, 신규 시 leaveNo)
      leaveCd: props.editRow?.leaveCd,
      // cmpnyCd: props.editRow?.cmpnyCd ?? cmpnyCd.value,
      // A. 타입구분
      // 타입구분 (01:사용자 신청, 02:관리자 부여)
      leaveType: leaveType.value,
      // 연차 부여 방식
      grantType: leaveType.value === "02" ? grantType.value : null,

      // B. 기본구분
      // 연차코드
      leaveNo: leaveNo.value.trim(),
      // 연차명
      leaveNm: leaveNm.value.trim(),
      // 유급구분
      paidType: paidType.value,
      // SYS024 휴가성격
      leaveNatureType: leaveNatureType.value,
      // 사용여부
      useYn: useYn.value,
      // 비고(설명)
      leaveDesc: leaveDesc.value,

      // C. 사용규칙
      // 사용자 신청 타입
      // 최대 신청일수 (사용자 신청 타입)
      maxAplyDays: leaveType.value === "01" ? maxAplyDays.value : null,
      // 연차 사용 단위 (사용자 신청 + 관리자 부여 전체(자동/수동) 입력값 전송)
      useUnitType:
        leaveType.value === "01" || leaveType.value === "02"
          ? useUnitType.value
          : null,
      // 사용가능기간
      availTermType: leaveType.value === "01" ? availTermType.value : null,
      // 기간설정 시작일 (사용자 신청) MMDD 4자리
      availFromDt:
        leaveType.value === "01" && availTermType.value === "03"
          ? toMMDD(availFromDt.value)
          : null,
      // 기간설정 종료일 (사용자 신청) MMDD 4자리
      availToDt:
        leaveType.value === "01" && availTermType.value === "03"
          ? toMMDD(availToDt.value)
          : null,

      // 관리자 부여 타입 (자동/수동 공통)
      // 사용가능기간
      adminAvailTermType:
        leaveType.value === "02" ? adminAvailTermType.value : null,
      // 기간설정 시작일 (절대 날짜 YYYYMMDD 8자, prafta-044-FU2)
      adminAvailFromDt:
        leaveType.value === "02" && adminAvailTermType.value === "03"
          ? toYmd8(adminAvailFromDt.value)
          : null,
      // 기간설정 종료일 (절대 날짜 YYYYMMDD 8자, prafta-044-FU2)
      adminAvailToDt:
        leaveType.value === "02" && adminAvailTermType.value === "03"
          ? toYmd8(adminAvailToDt.value)
          : null,

      // 관리자 부여 타입 (자동부여)
      // 자동 부여 기준일
      grantBaseType:
        leaveType.value === "02" && grantType.value === "01"
          ? grantBaseType.value
          : null,
      // 실행 시점 (개월 전 1일) - grantBaseType '01','02'일 때만 사용
      grantOffsetMonth:
        leaveType.value === "02" &&
        grantType.value === "01" &&
        grantBaseType.value !== "03"
          ? grantOffsetMonth.value
          : null,
      // 자동부여 지정일 (MMDD) - grantBaseType '03'(부여일지정)일 때만 사용
      grantAssignMmdd:
        leaveType.value === "02" &&
        grantType.value === "01" &&
        grantBaseType.value === "03"
          ? toMMDD(grantAssignMmdd.value)
          : null,

      // D. 결재 및 증빙
      // 결재 여부
      aprvUseYn: aprvUseYn.value,
      // 증빙 여부
      evidenceYn: evidenceYn.value,
      // 증빙 안내 문구
      evidenceGuideMsg:
        evidenceYn.value === "Y" ? evidenceGuideMsg.value : null,
    };

    await axios.post("/webApi/attd03/update-leave-types", payload);

    proxy.$alert(getMessage(MSG.SAVE_COMPLETED));
    props.onSearch?.();
    emit("close");
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "저장 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// ================ Life Cycle ================
onMounted(async () => {
  await fnGetSystinfoList();
});
</script>

<style scoped>
@import "@/assets/css/modal-popup-guide.css";

.modal-content-leave-type {
  width: 1120px;
  max-height: 85vh;
}

.modal-body-leave {
  padding: 1rem 1.5rem;
  overflow-y: auto;
  flex: 1;
  min-height: 0;
}

.section-block {
  margin-bottom: 1rem;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 10px;
  overflow: hidden;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1rem;
  background: #f9fafb;
  cursor: pointer;
  user-select: none;
}

.section-header:hover {
  background: #f3f4f6;
}

.section-title {
  font-weight: 600;
  font-size: 0.9375rem;
  color: #111827;
}

.required-tag {
  color: #ef4444;
  font-size: 0.8125rem;
  font-weight: 500;
  margin-left: 0.25rem;
}

.section-chevron {
  font-size: 0.75rem;
  color: #6b7280;
}

.section-body {
  padding: 1rem;
  background: #fff;
  border-top: 1px solid var(--color-border, #e5e7eb);
}

.type-cards {
  display: flex;
  flex-direction: row;
  gap: 1rem;
}

.type-cards.edit-mode .type-card {
  cursor: not-allowed;
  pointer-events: none;
}

.type-card {
  flex: 1;
  border: 2px solid #e5e7eb;
  border-radius: 10px;
  cursor: pointer;
  transition:
    border-color 0.2s,
    background 0.2s;
}

.type-card:hover {
  border-color: #d1d5db;
  background: #fafafa;
}

.type-card.selected {
  border-color: #16a34a;
  background: rgba(22, 163, 74, 0.04);
}

.type-card-inner {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 1rem;
}

.type-radio {
  width: 18px;
  height: 18px;
  border: 2px solid #d1d5db;
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 2px;
}

.type-radio.checked {
  border-color: #16a34a;
  background: #16a34a;
  box-shadow: inset 0 0 0 3px #fff;
}

.type-content {
  flex: 1;
}

.type-content strong {
  display: block;
  font-size: 0.9375rem;
  color: #111827;
  margin-bottom: 0.25rem;
}

.type-content p {
  margin: 0;
  font-size: 0.8125rem;
  color: #6b7280;
  line-height: 1.4;
}

.type-check {
  color: #16a34a;
  font-size: 1.25rem;
  font-weight: 700;
}

.grant-method-row {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid #e5e7eb;
}

.grant-method-label {
  display: block;
  font-weight: 500;
  font-size: 0.875rem;
  margin-bottom: 0.5rem;
}

.grant-method-options {
  display: flex;
  gap: 1.5rem;
}

.radio-option {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  font-size: 0.875rem;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem 1.5rem;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.form-item.full-width {
  grid-column: 1 / -1;
}

.form-item label {
  font-size: 0.8125rem;
  font-weight: 500;
  color: #374151;
}

.form-item .required {
  color: #ef4444;
}

.form-item input,
.form-item select {
  padding: 0.5rem 0.75rem;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 0.875rem;
}

.form-item input:focus,
.form-item select:focus {
  outline: none;
  border-color: #16a34a;
  box-shadow: 0 0 0 2px rgba(22, 163, 74, 0.2);
}

.evidence-guide-textarea {
  width: 100%;
  padding: 0.5rem 0.75rem;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 0.875rem;
  font-family: "Pretendard", sans-serif;
  resize: vertical;
  min-height: 4rem;
}

.evidence-guide-textarea:focus {
  outline: none;
  border-color: #16a34a;
  box-shadow: 0 0 0 2px rgba(22, 163, 74, 0.2);
}

.toggle-wrap {
  display: flex;
  align-items: center;
}

.toggle-switch {
  position: relative;
  display: inline-block;
  width: 44px;
  height: 24px;
}

.toggle-switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.toggle-slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #d1d5db;
  border-radius: 24px;
  transition: 0.3s;
}

.toggle-slider::before {
  position: absolute;
  content: "";
  height: 18px;
  width: 18px;
  left: 3px;
  bottom: 3px;
  background-color: white;
  border-radius: 50%;
  transition: 0.3s;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
}

.toggle-switch input:checked + .toggle-slider {
  background-color: #16a34a;
}

.toggle-switch input:checked + .toggle-slider::before {
  transform: translateX(20px);
}

.field-desc {
  margin: 0.25rem 0 0;
  font-size: 0.75rem;
  color: #6b7280;
}

.form-item .full-width-input {
  width: 100%;
  box-sizing: border-box;
}

.period-days-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.period-date-range {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.period-date-range .month-day-picker-input-wrap,
.period-date-range .period-date-calendar {
  flex: 1;
  min-width: 0;
}

.period-date-calendar .calendar-search {
  width: 100%;
}

/* 기존 input type="date"와 동일한 모양, 클릭 시 flatpickr 캘린더 */
.period-date-calendar :deep(.calendar-input) {
  width: 100%;
  box-sizing: border-box;
  padding: 0.5rem 0.75rem;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 0.875rem;
}

.period-date-calendar :deep(.calendar-input:focus) {
  outline: none;
  border-color: #16a34a;
  box-shadow: 0 0 0 2px rgba(22, 163, 74, 0.2);
}

.period-date-input {
  flex: 1;
  min-width: 0;
  padding: 0.5rem 0.75rem;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 0.875rem;
}

.period-date-input:focus {
  outline: none;
  border-color: #16a34a;
  box-shadow: 0 0 0 2px rgba(22, 163, 74, 0.2);
}

.period-date-sep {
  flex-shrink: 0;
  font-weight: 500;
  color: #6b7280;
}

.exec-time-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.exec-time-row input,
.exec-time-row select {
  width: 4rem;
  padding: 0.5rem 0.75rem;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 0.875rem;
}

.admin-grant-auto {
  width: 100%;
}

.auto-grant-rule {
  margin-top: 1rem;
}

.auto-grant-panel {
  padding: 1rem 1.25rem;
  background: #fff8e1;
  border: 1px solid #fde68a;
  border-radius: 10px;
}

.auto-grant-title {
  margin: 0 0 1rem;
  font-weight: 600;
  font-size: 0.9375rem;
  color: #b45309;
}

.auto-grant-info {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  margin: 1rem 0 0;
  font-size: 0.8125rem;
  color: #92400e;
  line-height: 1.5;
}

.auto-grant-info .info-icon {
  flex-shrink: 0;
  font-size: 0.875rem;
  color: #b45309;
}

.period-days-row input {
  flex: 1;
  min-width: 0;
}

.period-days-row .period-unit {
  flex-shrink: 0;
  font-size: 0.875rem;
  color: #6b7280;
}

.period-combo {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.period-combo select {
  width: 6rem;
  padding: 0.5rem 0.75rem;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 0.875rem;
}

.period-combo input {
  width: 5rem;
  padding: 0.5rem 0.75rem;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 0.875rem;
}

.period-unit {
  font-size: 0.875rem;
  color: #6b7280;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  padding: 1rem 1.5rem;
  background: var(--modal-footer-bg, #f9fafb);
  border-top: 1px solid var(--modal-border, #e5e7eb);
}

.footer-actions {
  display: flex;
  gap: 0.5rem;
}

.btn {
  padding: 0.5rem 1rem;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
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
