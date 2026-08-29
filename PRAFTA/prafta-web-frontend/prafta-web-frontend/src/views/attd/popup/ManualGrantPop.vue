<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div class="modal-content-normal manual-grant-modal">
        <!-- ============ 헤더 ============ -->
        <div class="modal-header">
          <span>연차 수동 부여</span>
          <button class="icon-button" type="button" @click="fnClose">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              stroke-width="1.5"
              stroke="currentColor"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>

        <!-- ============ 바디 ============ -->
        <div class="modal-body manual-grant">
          <!-- ===== 대상 카드 ===== -->
          <!-- 단일 대상 -->
          <div v-if="isSingleTarget" class="mg-target-card">
            <div class="mg-avatar">{{ singleAvatarText }}</div>
            <div>
              <p class="mg-target-name">{{ targetUsers[0].userNm }}</p>
              <p class="mg-target-info">{{ targetUsers[0].deptNm || "-" }}</p>
            </div>
          </div>

          <!-- 일괄 대상 (N명) -->
          <div v-else class="mg-target-card mg-target-card--bulk">
            <div class="mg-bulk-summary">
              <p class="mg-target-name">
                선택된 직원 <strong>{{ targetUsers.length }}명</strong>에게 일괄
                부여합니다.
              </p>
              <button
                class="mg-target-toggle"
                type="button"
                @click="fnToggleTargetList"
              >
                대상 보기
                <svg
                  viewBox="0 0 24 24"
                  width="13"
                  height="13"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  :class="{ 'is-open': targetListOpen }"
                  aria-hidden="true"
                >
                  <polyline points="6 9 12 15 18 9" />
                </svg>
              </button>
            </div>
            <ul v-show="targetListOpen" class="mg-target-list">
              <li v-for="u in targetUsers" :key="u.userCd">
                {{ u.userNm }}
                <span class="mg-target-list-dept">{{ u.deptNm || "-" }}</span>
              </li>
            </ul>
          </div>

          <!-- ===== 부여 유형 ===== -->
          <div class="mg-field">
            <label class="mg-label">
              부여 유형<span class="mg-required">*</span>
            </label>
            <BaseSelect v-model="form.leaveCd">
              <option value="">선택해 주세요</option>
              <!-- 휴가 종류: LEAVE_TYPE='02' AND GRANT_TYPE='02' AND USE_YN='Y' -->
              <option
                v-for="t in leaveTypeOptions"
                :key="t.leaveCd"
                :value="t.leaveCd"
              >
                {{ t.leaveNm }}
              </option>
            </BaseSelect>
          </div>

          <!-- ===== 부여 일수 / 사용 가능일 ===== -->
          <div class="mg-grid">
            <div class="mg-field">
              <label class="mg-label">
                부여 일수<span class="mg-required">*</span>
              </label>
              <div class="mg-input-suffix">
                <input
                  v-model="form.grantDays"
                  class="mg-input"
                  type="number"
                  min="1"
                  step="1"
                />
                <span class="mg-suffix">일</span>
              </div>
            </div>
            <div class="mg-field">
              <label class="mg-label">
                사용 가능일<span class="mg-required">*</span>
              </label>
              <div class="mg-date-wrap">
                <CalendarSrch v-model="form.availFromDate" class="mg-input" />
                <svg
                  class="mg-date-icon"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  aria-hidden="true"
                >
                  <rect x="3" y="4" width="18" height="18" rx="2" />
                  <line x1="16" y1="2" x2="16" y2="6" />
                  <line x1="8" y1="2" x2="8" y2="6" />
                  <line x1="3" y1="10" x2="21" y2="10" />
                </svg>
              </div>
            </div>
          </div>

          <!-- ===== 부여 사유 ===== -->
          <div class="mg-field">
            <label class="mg-label">부여 사유</label>
            <textarea
              v-model="form.reason"
              class="mg-textarea"
              rows="3"
              placeholder="예: 2026년 1분기 안전관리 우수상 포상"
            ></textarea>
          </div>

          <!-- ===== 안내 박스 ===== -->
          <div class="mg-info-box">
            <svg
              viewBox="0 0 24 24"
              width="14"
              height="14"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              aria-hidden="true"
            >
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="16" x2="12" y2="12" />
              <line x1="12" y1="8" x2="12.01" y2="8" />
            </svg>
            <p class="mg-info-text">
              수동 부여한 연차는 부여 이력에 영구 기록되며 감사 추적이
              가능합니다. 법정연차와 별도(법정 외)로 관리됩니다.
            </p>
          </div>
        </div>

        <!-- ============ 푸터 ============ -->
        <!-- F-10 규약: 왼쪽=진행/확정(부여, primary), 오른쪽=이탈(취소) -->
        <div class="modal-footer">
          <button
            class="btn btn-primary"
            type="button"
            :disabled="isLoading"
            @click="fnSubmit"
          >
            부여하기
          </button>
          <button class="btn btn-second" type="button" @click="fnClose">
            취소
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
// ================ Imports ================
import { ref, computed, onMounted, getCurrentInstance } from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import BaseSelect from "@/components/common/BaseSelect.vue";
import CalendarSrch from "@/components/common/CalendarSrch.vue";

// ================ Props & Emits ================
const props = defineProps({
  // 대상 직원 목록 [{ userCd, userNm, deptNm }]. 단일=1건, 일괄=N건
  targetUsers: { type: Array, default: () => [] },
  // 부여 성공 시 부모(대시보드/상세) 재조회 신호 콜백
  //   (useModal이 onClose를 덮어쓰므로 재조회는 별도 콜백 prop으로 받는다)
  onGranted: { type: Function, default: null },
});
const emit = defineEmits(["close"]);

// ================ Instance & Composables ================
const { proxy } = getCurrentInstance();

// ================ Refs (Variables) ================
// props 복사 (대상 직원)
const targetUsers = ref(
  Array.isArray(props.targetUsers) ? [...props.targetUsers] : []
);

// 일괄 대상 목록 펼침 상태
const targetListOpen = ref(false);

// 휴가 종류 옵션 [{ leaveCd, leaveNm }]
const leaveTypeOptions = ref([]);

// 입력 폼
const form = ref({
  leaveCd: "",
  grantDays: "",
  availFromDate: "",
  reason: "",
});

const isLoading = ref(false);

// ================ Computed ================
const isSingleTarget = computed(() => targetUsers.value.length === 1);

// 단일 대상 아바타 (이름 앞 2글자)
const singleAvatarText = computed(() => {
  const nm = targetUsers.value[0]?.userNm || "";
  return nm.slice(0, 2) || "-";
});

// ================ Life Cycle Functions ================
onMounted(() => {
  fnLoadLeaveTypes();
});

// ================ API Functions ================
// 수동 부여 가능 휴가 종류 조회
const fnLoadLeaveTypes = async () => {
  try {
    const response = await axios.get("/webApi/attd09/leave-grant/manual-types");
    leaveTypeOptions.value = Array.isArray(response.data?.types)
      ? response.data.types
      : [];
  } catch (err) {
    const msg = resolveApiErrorMessage(
      err,
      "휴가 종류를 불러오는 중 오류가 발생했습니다."
    );
    await proxy.$alert(msg);
  }
};

// 부여하기 — 단일/일괄 분기
const fnSubmit = async () => {
  // com-013-08-5(프론트): 더블클릭/연타 1차 방어 — 제출 진행 중이면 즉시 무시.
  //   isLoading 을 동기적으로 선점해, 1차 검증/확인 다이얼로그 대기 중의 추가 클릭까지 차단한다.
  //   (서버에서도 결정적 멱등키로 단시간 동일 제출을 차단하므로 2중 방어.)
  if (isLoading.value) return;
  isLoading.value = true;
  try {
    // 1) 1차 검증 (최종 권위는 백엔드)
    if (!fnValidate()) {
      isLoading.value = false;
      return;
    }

    // 2) 사용 가능일이 오늘 이전이면 즉시 사용 가능 안내 (차단 아님)
    const availFromDate = fnToYyyymmdd(form.value.availFromDate);
    if (availFromDate < fnTodayYyyymmdd()) {
      const ok = await proxy.$confirm(
        "사용 가능일이 오늘 이전입니다. 진행하시겠습니까?"
      );
      if (!ok) {
        isLoading.value = false;
        return;
      }
    }

    // 3) 요청 body 구성 (단일/일괄 공통 필드)
    const grantDays = parseFloat(form.value.grantDays);
    const reason = form.value.reason || "";

    if (isSingleTarget.value) {
      await axios.post("/webApi/attd09/leave-grant/manual-grant", {
        userCd: targetUsers.value[0].userCd,
        leaveCd: form.value.leaveCd,
        grantDays,
        availFromDate,
        reason,
      });
    } else {
      await axios.post("/webApi/attd09/leave-grant/bulk-manual-grant", {
        userCds: targetUsers.value.map((u) => u.userCd),
        leaveCd: form.value.leaveCd,
        grantDays,
        availFromDate,
        reason,
      });
    }
    await proxy.$alert("부여되었습니다.");
    // 부모(대시보드/상세) 재조회 신호 → 모달 닫기
    //   (useModal이 onClose를 덮어쓰므로 재조회는 onGranted prop 콜백으로 전달)
    if (typeof props.onGranted === "function") props.onGranted();
    emit("close");
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "부여 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  } finally {
    isLoading.value = false;
  }
};

// ================ Methods/Functions ================
// 1차 검증 (필수/일수/1일 단위 정수/날짜 형식)
const fnValidate = () => {
  if (!form.value.leaveCd) {
    proxy.$alert("부여 유형을 선택해 주세요.");
    return false;
  }

  const days = parseFloat(form.value.grantDays);
  if (Number.isNaN(days) || days <= 0) {
    proxy.$alert("부여 일수는 0보다 커야 합니다.");
    return false;
  }
  // 1일 단위 (정수만 허용)
  if (!Number.isInteger(days)) {
    proxy.$alert("부여 일수는 1일 단위로 입력해 주세요.");
    return false;
  }

  if (!form.value.availFromDate) {
    proxy.$alert("사용 가능일을 입력해 주세요.");
    return false;
  }

  return true;
};

// 일괄 대상 목록 토글
const fnToggleTargetList = () => {
  targetListOpen.value = !targetListOpen.value;
};

// 모달 닫기
const fnClose = () => {
  emit("close");
};

// ================ 내부 유틸 ================
// YYYY-MM-DD → YYYYMMDD (input[type=date] 값 변환)
const fnToYyyymmdd = (ymd) => String(ymd || "").replace(/-/g, "");

// 오늘 YYYYMMDD
const fnTodayYyyymmdd = () => {
  const d = new Date();
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}${m}${day}`;
};
</script>

<style scoped>
@import "@/assets/css/modal-popup-guide.css";

.manual-grant-modal {
  width: 100%;
  max-width: 520px;
}

.manual-grant {
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
}

/* ===== 대상 카드 ===== */
.mg-target-card {
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
  padding: 0.625rem 0.875rem;
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.mg-target-card--bulk {
  flex-direction: column;
  align-items: stretch;
  gap: 0.5rem;
}

.mg-avatar {
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  background: rgba(22, 163, 74, 0.08);
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 0.6875rem;
  flex-shrink: 0;
}

.mg-target-name {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-strong);
  margin: 0;
}

.mg-target-info {
  font-size: 0.6875rem;
  color: var(--color-text-muted);
  margin: 0.0625rem 0 0;
}

.mg-bulk-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.mg-target-name strong {
  color: var(--color-primary-pressed);
}

.mg-target-toggle {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.6875rem;
  color: var(--color-text-muted);
  background: none;
  border: none;
  cursor: pointer;
  font-family: "Pretendard", sans-serif;
  flex-shrink: 0;
}

.mg-target-toggle svg {
  transition: transform 0.2s ease;
}

.mg-target-toggle svg.is-open {
  transform: rotate(180deg);
}

.mg-target-list {
  list-style: none;
  margin: 0;
  padding: 0.5rem 0 0;
  border-top: 1px dashed var(--color-border);
  max-height: 8rem;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.mg-target-list li {
  font-size: 0.75rem;
  color: var(--color-text);
  display: flex;
  justify-content: space-between;
  gap: 0.5rem;
}

.mg-target-list-dept {
  color: var(--color-text-muted);
  font-size: 0.6875rem;
}

/* ===== 폼 필드 ===== */
.mg-field {
  display: flex;
  flex-direction: column;
}

.mg-label {
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--color-text-strong);
  margin-bottom: 0.375rem;
}

.mg-required {
  color: var(--color-danger);
  margin-left: 0.125rem;
}

.mg-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.625rem;
}

.mg-input {
  width: 100%;
  height: 2.125rem;
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
  padding: 0 0.625rem;
  font-size: 0.8125rem;
  background: var(--color-surface);
  color: var(--color-text-strong);
  font-family: "Pretendard", sans-serif;
}

.mg-input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 var(--focus-ring-width, 3px) var(--color-focus-ring);
}

/* 네이티브 date input → CalendarSrch 교체. 내부 input 셀렉터로 사이즈 유지 */
.mg-input :deep(.calendar-input) {
  width: 100%;
  height: 2.125rem;
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
  padding: 0 0.625rem;
  font-size: 0.8125rem;
  background: var(--color-surface);
  color: var(--color-text-strong);
  font-family: "Pretendard", sans-serif;
  padding-right: 1.875rem;
  cursor: pointer;
}
.mg-input :deep(.calendar-input):focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 var(--focus-ring-width, 3px) var(--color-focus-ring);
}

/* CalendarSrch 내부 placeholder(이모지)는 OS별 렌더 크기가 들쭉날쭉해
   넓은 필드에서 너무 작게 보임 → 숨기고 우측에 고정 크기 아이콘을 별도 배치 */
.mg-input :deep(.calendar-input)::placeholder {
  color: transparent;
}

.mg-date-wrap {
  position: relative;
}

.mg-date-icon {
  position: absolute;
  right: 0.625rem;
  top: 50%;
  transform: translateY(-50%);
  width: 1rem;
  height: 1rem;
  color: var(--color-text-muted);
  pointer-events: none;
}

.mg-input-suffix {
  position: relative;
}

.mg-input-suffix .mg-input {
  padding-right: 1.875rem;
}

.mg-suffix {
  position: absolute;
  right: 0.625rem;
  top: 50%;
  transform: translateY(-50%);
  font-size: 0.75rem;
  color: var(--color-text-muted);
  pointer-events: none;
}

.mg-textarea {
  width: 100%;
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
  padding: 0.5rem 0.625rem;
  font-size: 0.8125rem;
  line-height: 1.5;
  resize: vertical;
  min-height: 4.375rem;
  background: var(--color-surface);
  color: var(--color-text-strong);
  font-family: "Pretendard", sans-serif;
}

.mg-textarea:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 var(--focus-ring-width, 3px) var(--color-focus-ring);
}

/* ===== 안내 박스 ===== */
.mg-info-box {
  display: flex;
  gap: 0.5rem;
  background: rgba(22, 163, 74, 0.06);
  border-radius: var(--input-radius);
  padding: 0.625rem 0.75rem;
}

.mg-info-box svg {
  color: var(--color-primary-pressed);
  flex-shrink: 0;
  margin-top: 0.125rem;
}

.mg-info-text {
  font-size: 0.6875rem;
  color: var(--color-primary-pressed);
  line-height: 1.5;
  margin: 0;
}

/* ===== 반응형 ===== */
@media (max-width: 480px) {
  .mg-grid {
    grid-template-columns: 1fr;
  }
}
</style>
