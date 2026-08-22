<template>
  <!--
    UI-001 UserTransferPop — 사용자 소속이동 입력 팝업 (PRAFTA-WEB_001-4)
    참조 패턴: UserInfoPop.vue / HireDateEditPop.vue (중첩 openPop + useCenteredDraggable)
    ※ 본 파일은 planner 가 작성한 "골격"이다. template + scoped style 만 완성돼 있고,
      script 의 API 호출 / store / router 연동은 developer 가 채운다. (// TODO(developer) 참고)
  -->
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-narrow user-transfer-pop"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <!-- 🔹 Title -->
        <div class="modal-header" @mousedown="startDrag">
          <span>소속이동</span>
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

        <!-- 🔹 Body -->
        <div class="form-container">
          <!-- 대상자 -->
          <div class="transfer-target">
            <strong>{{ userNm }}</strong>
            <span class="transfer-target__sub">
              {{ userId }} · {{ isRegular ? "정규직" : "일용직" }}
            </span>
          </div>

          <!-- 진입 시 최초 1회만 로딩 힌트 노출.
               PRAFTA-001-2(2.1.1-2): 값 변경마다 재조회(디바운스)될 때는 힌트를 삽입/제거하지 않아
               레이아웃 리플로우(깜빡임)를 방지한다. -->
          <p
            class="transfer-hint"
            v-show="eligibilityLoading && !initialEligibilityLoaded"
          >
            소속이동 가능 여부를 확인하는 중입니다...
          </p>

          <!-- 불가 사유 배너 (eligible=false 시).
               PRAFTA-001-2(2.1.1-2): 재조회 중에도 배너를 숨기지 않고 직전 결과를 유지 →
               새 응답이 도착하면 그때 갱신(깜빡임 제거). -->
          <div class="transfer-block" v-show="!eligible && blockReasons.length">
            <p class="transfer-block__title">
              아래 사유로 소속이동할 수 없습니다.
            </p>
            <ul>
              <li v-for="(r, idx) in blockReasons" :key="idx">
                {{ r.message }}
              </li>
            </ul>
          </div>

          <!-- 이동 사업장 -->
          <div class="form-row-max">
            <label>이동 사업장</label>
            <input v-model="toSiteNm" placeholder="이동 사업장" readonly />
            <!-- PRAFTA-WEB_002-T1-04(1.3-1): 소속이동 불가 사용자면 찾기 버튼도 비활성(가능여부 조회 중 포함). -->
            <button
              class="btn btn-primary"
              :disabled="!eligible"
              @click="fnSiteSearchPopOpen"
            >
              찾기
            </button>
          </div>

          <!-- 이동 소속부서 -->
          <div class="form-row-max">
            <label>이동 부서</label>
            <input v-model="toNodeNm" placeholder="이동 소속부서" readonly />
            <button
              class="btn btn-primary"
              :disabled="!eligible"
              @click="fnNodeSearchPopOpen"
            >
              찾기
            </button>
          </div>
          <!-- 일용직 안내: 관리자가 존재하는 부서만 지정 가능 -->
          <p class="transfer-hint" v-if="!isRegular">
            ⓘ 일용직은 담당자(관리자)가 지정된 부서로만 이동할 수 있습니다.
          </p>

          <!-- 이동일 (내일 이후만) -->
          <div class="form-row-max">
            <label>이동일</label>
            <CalendarSrch
              class="dialog-date-input"
              v-model="moveDate"
              :minDate="tomorrowDate"
            />
          </div>

          <!-- 기본 근무타입 (정규직만 노출·필수) -->
          <div class="form-row-max" v-if="isRegular">
            <label>기본 근무타입</label>
            <BaseSelect
              id="toDefaultSchCd"
              v-model="toDefaultSchCd"
              :disabled="schTypeLoading || !toSiteCd"
            >
              <option :value="''">-</option>
              <option
                v-for="opt in filteredSchTypeOptions"
                :key="opt.schCd"
                :value="opt.schCd"
              >
                {{ opt.schNo }} ({{ fnFmtSchTime(opt.fstSchStrTime) }}~{{
                  fnFmtSchTime(opt.fstSchEndTime)
                }})
              </option>
            </BaseSelect>
          </div>
          <p class="transfer-hint" v-if="isRegular">
            ⓘ 적용일자가 이동일 이전인 근무타입만 선택할 수 있습니다.
          </p>
          <p class="transfer-hint" v-if="isRegular">
            ⓘ 소속이동일(발효)부터 당해 연말까지 평일 근무계획이 자동
            생성·갱신됩니다(빈 날·자동생성분만, 휴일·연차·교대팀 구간 제외).
          </p>

          <!-- 사유 (필수, 최대 500) -->
          <div class="form-row-max form-row-max--top">
            <label>사유</label>
            <textarea
              v-model="moveReason"
              class="transfer-reason"
              maxlength="500"
              placeholder="소속이동 사유를 입력하세요. 대상자에게 안내되며 노무 감사 시 증빙 자료로 사용됩니다."
            ></textarea>
          </div>
        </div>

        <!-- 🔹 Footer -->
        <!-- F-10 규약: 왼쪽=진행/확정(소속이동 예약, primary), 오른쪽=이탈(취소, ghost), 폭 균등 -->
        <div class="modal-footer">
          <div class="btn-group">
            <button
              class="btn btn-primary"
              :disabled="!eligible || saving"
              @click="fnSubmit"
            >
              소속이동 예약
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
/*
 * ─────────────────────────────────────────────────────────────────────────
 *  developer 가 script 에서 채워야 할 항목 (planner 골격에는 미포함)
 *
 *  [props]  cmpnyCd_p / userCd_p / userId_p / userNm_p / employmentType_p(SYS041) / onSaved(콜백)
 *  [emit]   close
 *
 *  [필요 API]
 *   - GET  /webApi/user01/{userCd}/transfer-eligibility
 *          ?toSiteCd=&toDefaultSchCd=&moveDate=   (불가케이스⑤ 시간차 판정에 선택 파라미터)
 *          → { employmentType, eligible, blockReasons:[{code,message}] }
 *   - GET  /webApi/user01/sch-type-options?siteCd=   (기존 재사용, 정규직만 호출)
 *          → [{ schCd, schNo, fstSchStrTime, fstSchEndTime }]
 *   - POST /webApi/user01/transfer-reservation
 *          body { userCd, toSiteCd, toNodeCd, moveDate(YYYYMMDD), toDefaultSchCd|null, moveReason }
 *          → { reservationId }
 *
 *  [연결 체크리스트]
 *   1) onMounted: 대상자 표시값 세팅 + employmentType_p 로 isRegular 판정
 *                 + transfer-eligibility 1차 조회(eligible/blockReasons)
 *   2) toSiteCd watch: 정규직이면 sch-type-options 재조회 + toDefaultSchCd reset
 *   3) [toSiteCd, toDefaultSchCd, moveDate] watch: eligibility 재조회(케이스⑤ 재판정)
 *   4) fnSubmit: confirm → moveDate 하이픈 제거 → POST → 성공 시 onSaved() + close
 *   5) 모든 비즈니스 검증(불가케이스 5종 등)은 서버가 최종 판정. 프론트는 안내/필수값만.
 * ─────────────────────────────────────────────────────────────────────────
 */
import {
  ref,
  computed,
  watch,
  defineProps,
  defineEmits,
  onMounted,
  getCurrentInstance,
} from "vue";
import { useModal } from "@/utils/useModal";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import BaseSelect from "@/components/common/BaseSelect.vue";
import CalendarSrch from "@/components/common/CalendarSrch.vue";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import SiteNodeSearchPop from "@/components/popup/SiteNodeSearchPop.vue";

// =========================== Define ===========================
const emit = defineEmits(["close"]);
const props = defineProps({
  cmpnyCd_p: String, // 회사 코드
  userCd_p: String, // 대상 사용자 코드 (eligibility / 예약 등록 대상)
  userId_p: String, // 대상 사용자 ID (표시용)
  userNm_p: String, // 대상 사용자명 (표시용)
  siteCd_p: String, // 대상자 현재 사업장 코드 (이동 사업장 검색에서 현재 사업장 제외용)
  employmentType_p: String, // 고용형태 [SYS041] REGULAR/CONTRACT/DAILY/EXECUTIVE
  onSaved: Function, // 예약 성공 후 부모(UserInfoPop/User_01) 갱신 콜백
});

// =========================== Ref ===========================
const modalRef = ref(null);

// 표시용
const userNm = ref("");
const userId = ref("");
// 고용형태(SYS041) — 초기값은 prop, eligibility 응답으로 갱신(서버 판정 기준에 정합).
const employmentType = ref("");

// 입력 상태 (developer: 초기값/리셋 보완)
const toSiteCd = ref("");
const toSiteNm = ref("");
const toNodeCd = ref("");
const toNodeNm = ref("");
const moveDate = ref(""); // YYYY-MM-DD (서버 전송 시 하이픈 제거)
const toDefaultSchCd = ref("");
const moveReason = ref("");

// 옵션/검증 상태
const schTypeOptions = ref([]);
const schTypeLoading = ref(false);
const eligibilityLoading = ref(false);
// PRAFTA-001-2(2.1.1-2): 최초 1회 조회 완료 여부. 이후 값 변경 재조회 시 로딩 힌트를 숨겨
//   레이아웃 리플로우(깜빡임)를 방지한다.
const initialEligibilityLoaded = ref(false);
const eligible = ref(false); // 초기 false: 가능여부 확인 전 저장 차단
const blockReasons = ref([]); // [{ code, message }]
const saving = ref(false);

// =========================== Data ===========================
const { open: openPop } = useModal();
const { proxy } = getCurrentInstance();
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

// 이동일 최소값 = 내일 (현재/과거 불가)
const tomorrowDate = (() => {
  const d = new Date();
  d.setDate(d.getDate() + 1);
  return d.toISOString().slice(0, 10);
})();

// =========================== Computed ===========================
// 정규직 규칙(기본근무타입 필수·불가케이스 적용) 적용 대상 = 일용직(DAILY)이 아닌 전체.
//   (정규직/계약직/임원 = 정규직 규칙 / DAILY = 예외). 서버 판정 기준과 동일 정의.
const isRegular = computed(() => employmentType.value !== "DAILY");

// 'HHmm' → 'HH:mm' (4자리 미만이면 원본)
const fnFmtSchTime = (t) => {
  if (!t || t.length < 4) return t || "";
  return `${t.substring(0, 2)}:${t.substring(2, 4)}`;
};

// 이동일 기준 적용 가능한(최초 적용일 ≤ 이동일) 근무타입만 노출. 이동일 미선택이면 전체 노출.
//   earliestApplyDate 는 현재본·이력본 통틀어 가장 이른 APPLY_DATE(YYYYMMDD).
//   최종 판정은 서버(USER_400_083) — 프론트 필터는 안내용.
const filteredSchTypeOptions = computed(() => {
  const ymd = (moveDate.value || "").replace(/-/g, "");
  if (!ymd) return schTypeOptions.value;
  return schTypeOptions.value.filter(
    (o) => !o.earliestApplyDate || o.earliestApplyDate <= ymd
  );
});

// =========================== Watch ===========================
// 사업장 변경 시: 선택값(부서/근무타입) reset + 정규직이면 근무타입 옵션 재조회.
watch(toSiteCd, (newSiteCd) => {
  toNodeCd.value = "";
  toNodeNm.value = "";
  toDefaultSchCd.value = "";
  fnLoadSchTypeOptions(newSiteCd);
});

// 불가케이스(특히 ⑤ 시간차 연차 미감쌈)는 이동 사업장/부서/근무타입/이동일에 따라 달라지므로
//   값 변경 시 eligibility 를 디바운스 재조회한다.
watch([toSiteCd, toNodeCd, toDefaultSchCd, moveDate], () => {
  scheduleEligibility();
});

// 이동일 변경(또는 옵션 재조회)으로 선택한 근무타입이 필터에서 빠지면 선택 해제(무효 선택 잔존 방지).
watch([moveDate, schTypeOptions], () => {
  if (
    toDefaultSchCd.value &&
    !filteredSchTypeOptions.value.some((o) => o.schCd === toDefaultSchCd.value)
  ) {
    toDefaultSchCd.value = "";
  }
});

// =========================== Life Cycle ===========================
onMounted(() => {
  userNm.value = props.userNm_p || "";
  userId.value = props.userId_p || "";
  employmentType.value = props.employmentType_p || "";
  // 진입 시 1차 가능여부 조회(이동 사업장/부서 미선택 상태 — 사업장 비종속 불가케이스 ①~④ 우선 노출).
  //   정규직 근무타입 옵션은 사업장 선택 후 watch(toSiteCd) 에서 로드한다.
  fnLoadEligibility();
});

// =========================== Eligibility / Options ===========================
// 사업장 활성 근무타입 옵션 조회(정규직만). 회사 스코프는 서버가 토큰에서 강제.
const fnLoadSchTypeOptions = async (targetSiteCd) => {
  if (!targetSiteCd || !isRegular.value) {
    schTypeOptions.value = [];
    return;
  }
  schTypeLoading.value = true;
  try {
    const response = await axios.get("/webApi/user01/sch-type-options", {
      params: { siteCd: targetSiteCd },
    });
    schTypeOptions.value = response.data ?? [];
    // 선택값이 새 목록에 없으면(사업장 변경 등) 선택 해제.
    if (
      toDefaultSchCd.value &&
      !schTypeOptions.value.some((o) => o.schCd === toDefaultSchCd.value)
    ) {
      toDefaultSchCd.value = "";
    }
  } catch (err) {
    schTypeOptions.value = [];
    proxy.$alert(
      resolveApiErrorMessage(err, "근무타입 목록 조회 중 오류가 발생했습니다.")
    );
  } finally {
    schTypeLoading.value = false;
  }
};

// 가능여부 조회(불가케이스 사전판정). 비즈니스 검증의 최종 판정은 서버.
const fnLoadEligibility = async () => {
  if (proxy.$util.isEmpty(props.userCd_p)) return;
  eligibilityLoading.value = true;
  try {
    const params = {};
    if (proxy.$util.isNotEmpty(toSiteCd.value)) params.toSiteCd = toSiteCd.value;
    if (proxy.$util.isNotEmpty(toNodeCd.value)) params.toNodeCd = toNodeCd.value;
    if (isRegular.value && proxy.$util.isNotEmpty(toDefaultSchCd.value)) {
      params.toDefaultSchCd = toDefaultSchCd.value;
    }
    if (proxy.$util.isNotEmpty(moveDate.value)) {
      params.moveDate = (moveDate.value || "").replace(/-/g, "");
    }
    const response = await axios.get(
      `/webApi/user01/${props.userCd_p}/transfer-eligibility`,
      { params }
    );
    const data = response.data || {};
    if (proxy.$util.isNotEmpty(data.employmentType)) {
      employmentType.value = data.employmentType;
    }
    eligible.value = data.eligible === true;
    blockReasons.value = Array.isArray(data.blockReasons)
      ? data.blockReasons
      : [];
  } catch (err) {
    eligible.value = false;
    blockReasons.value = [];
    proxy.$alert(
      resolveApiErrorMessage(
        err,
        "소속이동 가능 여부 조회 중 오류가 발생했습니다."
      )
    );
  } finally {
    eligibilityLoading.value = false;
    initialEligibilityLoaded.value = true;
  }
};

// eligibility 디바운스 트리거(연속 입력 시 마지막 1회만 호출).
let eligibilityTimer = null;
const scheduleEligibility = () => {
  if (eligibilityTimer) clearTimeout(eligibilityTimer);
  eligibilityTimer = setTimeout(fnLoadEligibility, 300);
};

// =========================== Methods ===========================
// 이동 사업장 선택 팝업 (중첩 openPop). onSelect 시그니처: (siteCd, siteNo, siteNm)
const fnSiteSearchPopOpen = () => {
  openPop(SiteSearchPop, {
    cmpnyCd_p: props.cmpnyCd_p,
    // PRAFTA-WEB_002-T1-04(1.3-2): 현재 소속 사업장은 이동 대상에서 제외.
    excludeSiteCd_p: props.siteCd_p,
    onSelect: onSiteSelected,
  });
};

// 이동 부서 선택 팝업 (siteCd 선행 필수). onSelect 시그니처: (nodeCd, nodeNm)
const fnNodeSearchPopOpen = () => {
  if (proxy.$util.isEmpty(toSiteCd.value)) {
    proxy.$alert("이동할 사업장을 먼저 선택해 주세요.");
    return;
  }
  openPop(SiteNodeSearchPop, {
    cmpnyCd_p: props.cmpnyCd_p,
    siteCd_p: toSiteCd.value,
    // PRAFTA-WEB_002-T1-04(1.3-3): 정규직은 담당 미지정 부서도 노출(이동 시 자동 담당 정 지정 대상). 일용직은 담당 부서만.
    includeNoAdmin_p: isRegular.value,
    onSelect: onNodeSelected,
  });
};

const onSiteSelected = (siteCdVal, _siteNoVal, siteNmVal) => {
  toSiteCd.value = siteCdVal;
  toSiteNm.value = siteNmVal;
  // 이동할 사업장 변경 시 이동할 부서 초기화(부서는 사업장 종속 — 타 사업장 부서 잔존 방지)
  toNodeCd.value = "";
  toNodeNm.value = "";
};

const onNodeSelected = (nodeCdVal, nodeNmVal) => {
  toNodeCd.value = nodeCdVal;
  toNodeNm.value = nodeNmVal;
};

// 등록 — UI 레벨 필수값 검증 후 서버 등록(비즈니스 검증은 서버가 최종 판정).
const fnSubmit = async () => {
  if (proxy.$util.isEmpty(toSiteCd.value)) {
    proxy.$alert("이동할 사업장을 선택해 주세요.");
    return;
  }
  if (proxy.$util.isEmpty(toNodeCd.value)) {
    proxy.$alert("이동할 소속부서를 선택해 주세요.");
    return;
  }
  if (proxy.$util.isEmpty(moveDate.value)) {
    proxy.$alert("소속이동일을 지정해 주세요.");
    return;
  }
  if (isRegular.value && proxy.$util.isEmpty(toDefaultSchCd.value)) {
    proxy.$alert("기본 근무타입을 지정해 주세요.");
    return;
  }
  if (!moveReason.value || !moveReason.value.trim()) {
    proxy.$alert("소속이동 사유를 입력해 주세요.");
    return;
  }

  const confirmed = await proxy.$confirm(
    "입력한 내용으로 소속이동을 예약하시겠습니까?"
  );
  if (!confirmed) return;

  saving.value = true;
  try {
    const payload = {
      userCd: props.userCd_p,
      toSiteCd: toSiteCd.value,
      toNodeCd: toNodeCd.value,
      // 서버 계약: YYYYMMDD(하이픈 제거).
      moveDate: (moveDate.value || "").replace(/-/g, ""),
      // 정규직만 기본근무타입 전송. 일용직은 null(서버가 미지정 처리).
      toDefaultSchCd: isRegular.value ? toDefaultSchCd.value || null : null,
      moveReason: moveReason.value.trim(),
    };
    const response = await axios.post(
      "/webApi/user01/transfer-reservation",
      payload
    );
    if (response.status === 200) {
      await proxy.$alert("소속이동이 예약되었습니다.");
      // 부모(UserInfoPop) 갱신 콜백 → 본 팝업 닫기.
      if (props.onSaved) props.onSaved();
      emit("close");
    }
  } catch (err) {
    proxy.$alert(
      resolveApiErrorMessage(err, "소속이동 예약 중 오류가 발생했습니다.")
    );
  } finally {
    saving.value = false;
  }
};
</script>

<style scoped>
.user-transfer-pop {
  width: 90%;
  max-width: 520px;
}

/* F-10 규약: 좌우 버튼 폭 균등 */
.modal-footer .btn-group .btn {
  flex: 1;
}

.form-container {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: 1.2rem;
  max-width: 500px;
  margin: 0 auto;
}

/* 대상자 헤더 */
.transfer-target {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid var(--color-border);
}
.transfer-target strong {
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--color-text-strong);
}
.transfer-target__sub {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

/* 불가 사유 배너 (HireDateEditPop danger 박스 패턴) */
.transfer-block {
  background: rgba(239, 68, 68, 0.12);
  border-radius: var(--input-radius);
  padding: 0.625rem 0.875rem;
}
.transfer-block__title {
  margin: 0 0 0.25rem;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-danger);
}
.transfer-block ul {
  margin: 0;
  padding-left: 1rem;
  font-size: 0.75rem;
  color: var(--color-danger);
  line-height: 1.6;
}

/* 안내 문구 */
.transfer-hint {
  margin: 0;
  font-size: 0.75rem;
  color: var(--color-text-muted);
  line-height: 1.5;
}

/* 사유 입력 행: 라벨 상단 정렬 */
.form-row-max--top {
  align-items: flex-start;
}
.transfer-reason {
  flex: 1;
  min-height: 4.5rem;
  resize: vertical;
  padding: 0.4rem 0.6rem;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
  color: var(--color-text-strong);
  font-size: 0.875rem;
  font-family: "Pretendard", sans-serif;
  line-height: 1.5;
}
.transfer-reason::placeholder {
  color: var(--color-text-muted);
}
.transfer-reason:focus {
  border-color: var(--color-border-strong);
  outline: none;
  box-shadow: 0 0 0 var(--focus-ring-width) var(--color-focus-ring);
  outline-offset: var(--outline-offset);
}

/* 이동일(CalendarSrch) — PRAFTA-001-2(2.1.1-1): 사용자 생성 팝업 입사일(.hire-date-field)과 UI 정합.
   외곽 래퍼(.calendar-search) 장식을 제거하고 내부 input(.calendar-input)에만 테두리를 적용해
   이중 테두리(박스 속 캘린더)를 없애고 다른 입력과 동일한 단일 캘린더 입력으로 표시한다. */
.dialog-date-input {
  flex: 1;
  padding: 0;
  background: transparent;
  border: none;
}
.dialog-date-input :deep(.calendar-input) {
  width: 100%;
  padding: 0.4rem 0.6rem;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
  color: var(--color-text-strong);
  font-size: 0.875rem;
  font-family: "Pretendard", sans-serif;
}
.dialog-date-input :deep(.calendar-input):focus {
  border-color: var(--color-border-strong);
  outline: none;
  box-shadow: 0 0 0 var(--focus-ring-width) var(--color-focus-ring);
  outline-offset: var(--outline-offset);
}
</style>
