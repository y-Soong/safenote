<template>
  <Transition name="fade">
    <div
      v-show="true"
      class="modal-overlay prafta-modal-popup"
      @click.self="$emit('close')"
    >
      <div
        class="modal-content-narrow"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <!-- 헤더 -->
        <div class="modal-header" @mousedown="startDrag">
          <span>내 정보</span>
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

        <!-- ★스크롤 담당 래퍼 — 전역 modal-popup-guide.css 는 .form-container 를 "모달 바디"로 보고
             overflow-y:auto + flex:1 을 건다. 이 팝업은 섹션이 4개라 래퍼가 없으면 섹션마다
             스크롤바가 따로 생긴다(2026-08-15 통합테스트 지적). 스크롤은 이 래퍼 하나만 갖는다. -->
        <div class="modal-body my-info-body">
          <!-- 사용자 기본정보 -->
          <div class="section-title">기본 정보</div>
          <div class="form-container">
            <div class="form-row-max">
              <label>아이디</label>
              <input v-model="userId" disabled />
            </div>
            <div class="form-row-max">
              <label>이름</label>
              <input v-model="userNm" disabled />
            </div>
            <div class="form-row-max">
              <label>사업장</label>
              <input v-model="siteNm" disabled />
            </div>
            <div class="form-row-max">
              <label>소속부서</label>
              <input v-model="nodeNm" disabled />
            </div>
            <div class="form-row-max">
              <label>휴대폰</label>
              <input v-model="mblNo" disabled />
            </div>
            <div class="form-row-max">
              <label>이메일</label>
              <input v-model="email" disabled />
            </div>
            <div class="form-row-max">
              <label>마지막 로그인</label>
              <input v-model="lastLoginDtime" disabled />
            </div>
          </div>

          <!-- 근무 정보 (F-8-3) — 기본 근무타입 자기변경(웹 내정보). 현재값 표시 + 인라인 변경.
             본인 변경은 정책서 §6.1 이 "관리자 주체"로만 서술돼 있어 명시적 근거는 없으나,
             2026-08-05 사용자 확정(3경로: 관리자/웹 내정보/앱 마이페이지)에 따라 F-8-2 API 를 연결한다.
             PRAFTA-004(결재자선택UI 추가, 2026-08-27): 편집 모드에 결재선 구성 UI 추가
             (LeaveApplyPop.vue 결재라인 구성 패턴 재사용). 웹은 selfApprvYn 개념이 없어 항상 노출하며
             미선택 시 서버가 부서 기본 결재자로 자동 폴백한다(필수 아님 — 2026-08-27 사용자 승인). -->
          <div class="section-title">근무 정보</div>
          <div class="form-container">
            <!-- 대기중 신청 있음 — 배너만 노출, 변경 버튼 숨김(PRAFTA-004 신규 3번째 분기). -->
            <template v-if="pendingDefaultSch.reqId">
              <div class="form-row-max">
                <label>기본 근무타입</label>
                <input :value="defaultSchLabel" disabled placeholder="미설정" />
              </div>
              <div class="default-sch-pending">
                승인 대기 중 —
                {{ pendingDefaultSch.schNo || pendingDefaultSch.schCd }} 신청함
                <br />
                {{ pendingDefaultSch.reqDate }} · 관리자 승인 후 반영됩니다.
              </div>
            </template>

            <div class="form-row-max" v-else-if="!isEditingDefaultSch">
              <label>기본 근무타입</label>
              <input :value="defaultSchLabel" disabled placeholder="미설정" />
              <button
                type="button"
                class="btn btn-second"
                @click="onStartEditDefaultSch"
              >
                변경
              </button>
            </div>

            <template v-else>
              <div class="form-row-max">
                <label>기본 근무타입</label>
                <BaseSelect
                  id="myDefaultSchCd"
                  v-model="pendingDefaultSchCd"
                  :disabled="isSchOptionsLoading || isSavingDefaultSch"
                >
                  <option :value="''">-</option>
                  <option
                    v-for="opt in filteredDefaultSchOptions"
                    :key="opt.schCd"
                    :value="opt.schCd"
                  >
                    {{ opt.schNo }} ({{ fnFmtSchTime(opt.fstSchStrTime) }}~{{
                      fnFmtSchTime(opt.fstSchEndTime)
                    }})
                  </option>
                </BaseSelect>
              </div>
              <p class="default-sch-hint">
                ⓘ 근무타입 변경 신청 시 승인 후 명일(내일)부터 당해 연말까지
                평일 근무계획이 자동 생성·갱신됩니다(빈 날·자동생성분만,
                휴일·연차·교대팀 구간 제외).
              </p>
              <!-- PRAFTA-004 신규 — 변경 사유 입력(필수, ATTD_400_096 과 매칭). -->
              <div class="form-row-max">
                <label>변경 사유</label>
                <textarea
                  v-model="defaultSchReqReason"
                  class="default-sch-reason"
                  rows="2"
                  maxlength="500"
                  placeholder="변경 사유를 입력해 주세요."
                ></textarea>
              </div>

              <!-- 결재선 구성 (PRAFTA-004, LeaveApplyPop.vue 패턴 재사용) — 웹은 선택 사항(미선택 시
                 서버가 부서 기본 결재자로 자동 폴백, 2026-08-27 사용자 승인). -->
              <div class="default-sch-approval">
                <div class="default-sch-approval__head">
                  <span>결재선 구성 (선택)</span>
                  <select
                    v-if="approvalPresets.length > 0"
                    class="default-sch-preset-sel"
                    v-model="selectedApprovalPresetId"
                    @change="fnApplyApprovalPresetSel"
                  >
                    <option value="">프리셋 선택</option>
                    <option
                      v-for="p in approvalPresets"
                      :key="p.presetId"
                      :value="p.presetId"
                    >
                      {{ p.presetNm }}{{ p.defaultYn === "Y" ? " (기본)" : "" }}
                    </option>
                  </select>
                  <span v-else class="default-sch-preset-empty">
                    등록된 프리셋 없음 (사용자관리 &gt; 연차 결재라인 구성)
                  </span>
                </div>
                <div class="default-sch-approval__cols">
                  <div class="default-sch-pane">
                    <div class="default-sch-pane__title">후보</div>
                    <div class="default-sch-list">
                      <div
                        v-for="c in approvalCandidates"
                        :key="c.userCd"
                        class="default-sch-cand"
                        :class="{ added: fnInApprovalLine(c.userCd) }"
                      >
                        <span
                          >{{ c.userNm }} · {{ c.rankNm || "직급없음" }}</span
                        >
                        <button
                          type="button"
                          :disabled="fnInApprovalLine(c.userCd)"
                          @click="fnAddApprover(c)"
                        >
                          추가
                        </button>
                      </div>
                      <div
                        v-if="approvalCandidates.length === 0"
                        class="default-sch-empty"
                      >
                        후보 없음
                      </div>
                    </div>
                  </div>
                  <div class="default-sch-pane">
                    <div class="default-sch-pane__title">
                      결재 순서 ({{ approvalLine.length }})
                    </div>
                    <div class="default-sch-list">
                      <div
                        v-for="(s, i) in approvalLine"
                        :key="s.userCd"
                        class="default-sch-step"
                      >
                        <span class="default-sch-step__no">{{ i + 1 }}</span>
                        <span class="default-sch-step__nm">{{ s.userNm }}</span>
                        <button
                          type="button"
                          :disabled="i === 0"
                          @click="fnMoveApproverUp(i)"
                        >
                          ▲
                        </button>
                        <button
                          type="button"
                          :disabled="i === approvalLine.length - 1"
                          @click="fnMoveApproverDown(i)"
                        >
                          ▼
                        </button>
                        <button
                          type="button"
                          class="default-sch-del"
                          @click="fnRemoveApprover(i)"
                        >
                          ✕
                        </button>
                      </div>
                      <div
                        v-if="approvalLine.length === 0"
                        class="default-sch-empty"
                      >
                        지정하지 않으면 부서 기본 결재자에게 자동 배정됩니다.
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <span class="form-msg" v-if="defaultSchErrorMsg">{{
                defaultSchErrorMsg
              }}</span>

              <!-- F-10 규약: 왼쪽=진행/확정(신청, primary), 오른쪽=이탈(취소) — 파일 기존 관례상
                 이탈 버튼은 정의되지 않은 btn-ghost 대신 공용 btn-second 사용(닫기 버튼과 동일). -->
              <div class="default-sch-actions">
                <button
                  type="button"
                  class="btn btn-primary"
                  :disabled="!pendingDefaultSchCd || isSavingDefaultSch"
                  @click="onRequestDefaultSch"
                >
                  신청
                </button>
                <button
                  type="button"
                  class="btn btn-second"
                  @click="onCancelEditDefaultSch"
                >
                  취소
                </button>
              </div>
            </template>
          </div>

          <!-- 약관 동의 설정 — 선택약관 + 위치정보 동의(005)를 같은 목록에 둔다.
             앱 마이페이지(MyPageView "약관 동의 설정")와 구성·조작을 동일하게 맞춘다.
             ★두 행 모두 [체크박스][(선택) 약관명][보기] 순서로 왼쪽 정렬한다.
               종전에는 스위치였으나 앱과 형태를 통일했다(2026-09-02).
             ★위치정보의 '일시 중지 ↔ 재동의'는 되돌릴 수 있으므로 체크박스가 맡는다.
               반면 '동의 철회'는 좌표 파기를 동반해 되돌릴 수 없으므로 체크박스에 싣지 않고
               해제 팝업에서 고르게 한다(오조작 1회로 파기되면 복구 수단이 없다). -->
          <template
            v-if="optionalTerms.length > 0 || locationConsent.consentState"
          >
            <div class="section-title">약관 동의 설정</div>
            <div class="form-container">
              <div
                v-for="terms in optionalTerms"
                :key="terms.termsId"
                class="terms-row"
              >
                <button
                  type="button"
                  role="checkbox"
                  class="terms-check"
                  :class="{ 'terms-check-on': terms.agrYn === 'Y' }"
                  :aria-checked="terms.agrYn === 'Y' ? 'true' : 'false'"
                  :aria-label="terms.termsNm + ' 동의'"
                  :disabled="isTermsSaving"
                  @click="fnToggleTerms(terms)"
                >
                  <span class="terms-check-box" aria-hidden="true">
                    <svg class="terms-check-mark" viewBox="0 0 24 24">
                      <path d="M5 13l4 4L19 7" />
                    </svg>
                  </span>
                  <span class="terms-check-label">{{
                    "(선택) " + terms.termsNm
                  }}</span>
                </button>
                <button
                  type="button"
                  class="terms-row-view"
                  @click="fnViewTerms(terms)"
                >
                  보기
                </button>
              </div>

              <!-- 위치정보 동의(005) — 위 선택약관 행과 동일한 구조.
                 상태가 4가지라, 체크 해제 상태의 사유만 아랫줄에 배지로 덧붙인다. -->
              <div v-if="locationConsent.consentState" class="terms-row-stack">
                <div class="terms-row">
                  <button
                    type="button"
                    role="checkbox"
                    class="terms-check"
                    :class="{ 'terms-check-on': isLocationAgreed }"
                    :aria-checked="isLocationAgreed ? 'true' : 'false'"
                    :aria-label="
                      (locationConsent.termsNm || '위치기반서비스 이용약관') +
                      ' 동의'
                    "
                    :disabled="isLocationSaving"
                    @click="fnToggleLocationConsent"
                  >
                    <span class="terms-check-box" aria-hidden="true">
                      <svg class="terms-check-mark" viewBox="0 0 24 24">
                        <path d="M5 13l4 4L19 7" />
                      </svg>
                    </span>
                    <span class="terms-check-label">{{
                      "(선택) " +
                      (locationConsent.termsNm || "위치기반서비스 이용약관")
                    }}</span>
                  </button>
                  <button
                    type="button"
                    class="terms-row-view"
                    @click="fnViewLocationTerms"
                  >
                    보기
                  </button>
                </div>
                <div v-if="!isLocationAgreed" class="terms-state">
                  <span class="terms-badge" :class="locationBadgeClass">{{
                    locationStateLabel
                  }}</span>
                </div>
              </div>
            </div>
          </template>

          <!-- 비밀번호 변경 -->
          <div class="section-title">비밀번호 변경</div>
          <div class="form-container">
            <div class="form-row-max">
              <label>현재 비밀번호</label>
              <input
                type="password"
                v-model="currentPw"
                placeholder="현재 비밀번호"
                autocomplete="current-password"
              />
            </div>
            <div class="form-row-max">
              <label>새 비밀번호</label>
              <input
                type="password"
                v-model="newPw"
                placeholder="8자 이상, 영문+숫자+특수문자"
                autocomplete="new-password"
              />
            </div>
            <div class="form-row-max">
              <label>비밀번호 확인</label>
              <input
                type="password"
                v-model="newPwConfirm"
                placeholder="새 비밀번호 재입력"
                autocomplete="new-password"
              />
            </div>
          </div>
        </div>
        <!-- /.my-info-body -->

        <!-- 푸터 버튼 -->
        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-primary" @click="fnChangePassword">
              비밀번호 변경
            </button>
            <button class="btn btn-withdrawal" @click="fnSelfWithdrawal">
              회원탈퇴
            </button>
            <!-- F-10 규약: 이탈(닫기)은 ghost 계열 — 정의되지 않은 btn-default 대신 공용 btn-second 사용 -->
            <button class="btn btn-second" @click="$emit('close')">닫기</button>
          </div>
        </div>
      </div>

      <!-- 위치정보 동의 해제 방식 선택 팝업 — 체크박스를 해제할 때 열린다.
           ★openPop 으로 열지 않는다(openPop 은 기존 모달을 먼저 닫아 본 팝업이 사라진다).
             닫으면 아무 전이도 일어나지 않으므로 체크는 그대로 유지된다. -->
      <LocationConsentOffPop
        v-if="locationOffPopOpen"
        :saving="isLocationSaving"
        @close="locationOffPopOpen = false"
        @suspend="fnSuspendLocation"
        @withdraw="fnWithdrawLocation"
      />
    </div>
  </Transition>
</template>

<script setup>
import { ref, computed, onMounted, getCurrentInstance } from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import { useModal } from "@/utils/useModal";
import { useUserStore } from "@/stores/userStore";
import axios from "@/api/axios";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { formatDateTimeDotWithSec } from "@/utils/dateFormat";
// 연동 회사 제3자 제공 동의(006) 식별 — 철회(Y→N) 확인 팝업 판별용(앱 termsGate 와 동일 상수).
import { THIRD_PARTY_CONSENT_TERMS_ID } from "@/utils/consentTerms";
import TermsDetailPop from "@/components/popup/TermsDetailPop.vue";
import LocationConsentOffPop from "@/components/popup/LocationConsentOffPop.vue";
// 문구 단일 출처 — 앱(prafta-app-frontend/src/utils/locationConsent.js)과 같은 사실을 말해야 한다.
import {
  LOCATION_STATE_LABEL,
  LOCATION_WITHDRAW_CONFIRM,
  LOCATION_SUSPEND_CONFIRM,
} from "@/utils/locationConsent";
import BaseSelect from "@/components/common/BaseSelect.vue";

const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();
const userStore = useUserStore();
const modalRef = ref(null);

const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3,
});

// 기본정보
const userId = ref("");
const userNm = ref("");
const siteNm = ref("");
const nodeNm = ref("");
const mblNo = ref("");
const email = ref("");
const lastLoginDtime = ref("");

// 비밀번호 변경
const currentPw = ref("");
const newPw = ref("");
const newPwConfirm = ref("");

// 근무 정보(F-8-2) — 기본 근무타입 자기변경. 현재값은 my-profile 보강 응답으로 표시.
const defaultSchCd = ref(""); // 현재 설정된 기본 근무타입 코드
const defaultSchLabel = ref(""); // 현재값 표시용 라벨("주간조 (09:00~18:00)")
const isEditingDefaultSch = ref(false);
const defaultSchOptions = ref([]);
// 반영 시점은 항상 명일(오늘+1, applyDefaultSchChange 규칙) — 적용일이 명일보다 미래인
//   근무타입은 노출하지 않는다(2026-08-22, 최종 판정은 서버 isValidDefaultSch).
const tomorrowYmd = (() => {
  const d = new Date();
  d.setDate(d.getDate() + 1);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}${m}${day}`;
})();
const filteredDefaultSchOptions = computed(() =>
  defaultSchOptions.value.filter(
    (o) => !o.earliestApplyDate || o.earliestApplyDate <= tomorrowYmd
  )
);
const isSchOptionsLoading = ref(false);
const pendingDefaultSchCd = ref("");
const isSavingDefaultSch = ref(false);
const defaultSchErrorMsg = ref("");

// PRAFTA-001/004(기본근무타입-승인제, 2026-08-27): 대기중 신청 요약(my-profile 응답 보강 필드,
//   신규 조회 API 없이 이 값만으로 배너를 그린다) + 신청 사유 입력.
const pendingDefaultSch = ref({
  reqId: "",
  schCd: "",
  schNo: "",
  reqDate: "",
});
const defaultSchReqReason = ref("");

// PRAFTA-004(결재자선택UI 추가, 2026-08-27): 결재선 구성 상태(LeaveApplyPop.vue 미러).
//   웹은 selfApprvYn 개념이 없어 항상 노출 — 미선택 제출 시 서버가 부서 기본 결재자로 자동 폴백한다
//   (2026-08-27 사용자 승인 — "선택 사항" 설계 확정).
const approvalCandidates = ref([]); // [{ userCd, userNm, rankNm, nodeNm }]
const approvalPresets = ref([]); // 본인 결재라인 프리셋(webApi/user04/presets)
const selectedApprovalPresetId = ref("");
const approvalLine = ref([]); // [{ userCd, userNm }] — 순서 = 결재 단계

// 선택약관 동의 설정 — GET /comApi/consent/my-optional-terms 응답(현재버전 + agrYn).
//   비치명적: 조회 실패 시 빈 목록(섹션 미노출). 토글은 POST /comApi/consent/my-optional-terms-agree.
const optionalTerms = ref([]);
// 토글 저장 직렬화 가드(동시 저장 경합 방지).
const isTermsSaving = ref(false);

// 위치정보 동의(005) — GET /comApi/consent/location-consent.
//   consentState 가 비면(조회 실패) 행 자체를 그리지 않는다(비치명적).
const locationConsent = ref({
  consentState: "",
  termsVersion: "",
  collectAllowed: false,
  termsId: "",
  termsNm: "",
});
const isLocationSaving = ref(false);
const locationOffPopOpen = ref(false);

const isLocationAgreed = computed(
  () => locationConsent.value.consentState === "AGREED"
);
const locationStateLabel = computed(
  () => LOCATION_STATE_LABEL[locationConsent.value.consentState] || ""
);
const locationBadgeClass = computed(() => ({
  "is-agreed": locationConsent.value.consentState === "AGREED",
  "is-suspended": locationConsent.value.consentState === "SUSPENDED",
  "is-pending": locationConsent.value.consentState === "PENDING_REAGREE",
  "is-withdrawn": locationConsent.value.consentState === "WITHDRAWN",
}));

onMounted(async () => {
  await fnLoadMyInfo();
  await fnLoadOptionalTerms();
  await fnLoadLocationConsent();
});

const fnLoadMyInfo = async () => {
  // userStore에서 기본값 먼저 세팅 (비-PII만)
  // 정책 §11.1에 따라 휴대폰/이메일은 store/sessionStorage에 두지 않으므로,
  // 아래 API 응답으로만 채워진다.
  userId.value =
    userStore.gv_userId || sessionStorage.getItem("gv_userId") || "";
  userNm.value =
    userStore.gv_userNm || sessionStorage.getItem("gv_userNm") || "";
  siteNm.value =
    userStore.gv_siteNm || sessionStorage.getItem("gv_siteNm") || "";

  // 본인 전용 API로 최신 정보 조회.
  // 조회 대상은 서버가 토큰으로만 결정하므로 클라이언트 식별자(params) 미전달 (IDOR 방지).
  try {
    const response = await axios.get("/webApi/user01/my-profile");
    if (response.status === 200 && response.data) {
      const info = response.data;
      userId.value = info.userId || userId.value;
      userNm.value = info.userNm || userNm.value;
      siteNm.value = info.siteNm || siteNm.value;
      nodeNm.value = info.nodeNm || "";
      mblNo.value = proxy.$util?.formatPhoneNumber
        ? proxy.$util.formatPhoneNumber(info.mblNo)
        : info.mblNo || "";
      email.value = info.email || "";
      if (info.lastLoginDtime) {
        // 마지막 로그인 일시(초 실재) → 점/콜론 표시. dateFormat 단일 출처 위임.
        lastLoginDtime.value = formatDateTimeDotWithSec(info.lastLoginDtime);
      }
      // F-8-2 보강 필드 — 현재 기본 근무타입 표시(미설정이면 전부 null).
      defaultSchCd.value = info.defaultSchCd || "";
      defaultSchLabel.value = info.defaultSchNo
        ? `${info.defaultSchNo} (${fnFmtSchTime(
            info.defaultSchStrTime
          )}~${fnFmtSchTime(info.defaultSchEndTime)})`
        : "";
      // PRAFTA-001(기본근무타입-승인제) 보강 필드 — 대기중 신청 요약(없으면 전부 null).
      if (info.pendingDefaultSchReqId) {
        pendingDefaultSch.value = {
          reqId: info.pendingDefaultSchReqId,
          schCd: info.pendingDefaultSchCd || "",
          schNo: info.pendingDefaultSchNo || "",
          reqDate: info.pendingDefaultSchReqDate || "",
        };
      } else {
        pendingDefaultSch.value = {
          reqId: "",
          schCd: "",
          schNo: "",
          reqDate: "",
        };
      }
    }
  } catch {
    // 조회 실패 시 userStore 값으로 대체 (이미 세팅됨)
  }
};

// ── F-8-3: 근무 정보(기본 근무타입 자기변경) ────────────────────────
// 'HHmm' → 'HH:mm' 라벨 포맷(DefaultSchGatePop.vue 와 동일).
const fnFmtSchTime = (t) => {
  if (!t || t.length < 4) return t || "";
  return `${t.substring(0, 2)}:${t.substring(2, 4)}`;
};

// 변경 클릭 → 인라인 전환 + 옵션/결재선 로드(현재값으로 선택 초기화).
//   PRAFTA-004: 결재 후보/프리셋도 이 시점에 함께 로드(LeaveApplyPop.vue onMounted 패턴을
//   "편집 진입 시점"으로 옮긴 것 — 팝업 mount 시점에 항상 로드할 필요 없음).
const onStartEditDefaultSch = async () => {
  isEditingDefaultSch.value = true;
  pendingDefaultSchCd.value = defaultSchCd.value;
  defaultSchErrorMsg.value = "";
  defaultSchReqReason.value = "";
  approvalLine.value = [];
  selectedApprovalPresetId.value = "";
  await Promise.all([
    fnLoadDefaultSchOptions(),
    fnLoadApprovalCandidates(),
    fnLoadApprovalPresets(),
  ]);
};

// 세션 사업장 고정 옵션 조회(파라미터 없음 — 서버가 토큰으로만 사업장 도출, IDOR 방지).
const fnLoadDefaultSchOptions = async () => {
  isSchOptionsLoading.value = true;
  try {
    const response = await axios.get("/webApi/user01/my-default-sch-options");
    defaultSchOptions.value = response.data ?? [];
    if (defaultSchOptions.value.length === 0) {
      defaultSchErrorMsg.value =
        "선택 가능한 근무타입이 없습니다. 관리자에게 문의해 주세요.";
    }
  } catch (err) {
    defaultSchOptions.value = [];
    defaultSchErrorMsg.value = resolveApiErrorMessage(
      err,
      "근무타입 목록 조회 중 오류가 발생했습니다."
    );
  } finally {
    isSchOptionsLoading.value = false;
  }
};

// PRAFTA-004: 결재자 후보 조회(LeaveApplyPop.vue fnLoadCandidates 미러). 비치명적 — 실패는 빈 배열.
const fnLoadApprovalCandidates = async () => {
  try {
    const r = await axios.get("/webApi/user04/approval-candidates", {});
    approvalCandidates.value = r.data?.candidates ?? [];
  } catch (e) {
    approvalCandidates.value = [];
  }
};

// PRAFTA-004: 본인 결재라인 프리셋 조회 + 기본 프리셋 자동 적용(LeaveApplyPop.vue fnLoadPresets 미러).
const fnLoadApprovalPresets = async () => {
  try {
    const r = await axios.get("/webApi/user04/presets", {});
    approvalPresets.value = r.data?.presets ?? [];
    const def = approvalPresets.value.find((p) => p.defaultYn === "Y");
    if (def) {
      selectedApprovalPresetId.value = def.presetId;
      fnApplyApprovalPresetSel();
    }
  } catch (e) {
    approvalPresets.value = [];
  }
};

// ── 결재라인 구성 (PRAFTA-004, LeaveApplyPop.vue 헬퍼 미러) ─────────────
const fnInApprovalLine = (userCd) =>
  approvalLine.value.some((s) => s.userCd === userCd);
const fnAddApprover = (c) => {
  if (!fnInApprovalLine(c.userCd)) {
    approvalLine.value.push({ userCd: c.userCd, userNm: c.userNm });
  }
};
const fnRemoveApprover = (i) => approvalLine.value.splice(i, 1);
const fnMoveApproverUp = (i) => {
  if (i <= 0) return;
  const a = approvalLine.value;
  [a[i - 1], a[i]] = [a[i], a[i - 1]];
};
const fnMoveApproverDown = (i) => {
  const a = approvalLine.value;
  if (i >= a.length - 1) return;
  [a[i + 1], a[i]] = [a[i], a[i + 1]];
};
// 선택한 프리셋의 결재라인을 적용.
const fnApplyApprovalPresetSel = () => {
  const p = approvalPresets.value.find(
    (x) => x.presetId === selectedApprovalPresetId.value
  );
  if (!p) return;
  approvalLine.value = (p.steps ?? []).map((s) => ({
    userCd: s.approverUserCd,
    userNm: s.userNm,
  }));
};

const onCancelEditDefaultSch = () => {
  isEditingDefaultSch.value = false;
  defaultSchErrorMsg.value = "";
  defaultSchReqReason.value = "";
  approvalLine.value = [];
  selectedApprovalPresetId.value = "";
};

// PRAFTA-001/004(기본근무타입-승인제): 저장 → 신청 전환. 승인 전까지 현재값(defaultSchCd/defaultSchLabel)은
//   갱신하지 않는다(원 요청서 §3) — 성공 시 pendingDefaultSch 를 채워 배너로 즉시 전환한다.
//   PRAFTA-004: approverUserCds 를 SSOT 로 함께 전송(빈 배열이면 서버가 부서 기본 결재자로 자동 폴백).
const onRequestDefaultSch = async () => {
  if (!pendingDefaultSchCd.value) return;
  if (!(defaultSchReqReason.value || "").trim()) {
    defaultSchErrorMsg.value = getMessage(
      MSG.MY_INFO_DEFAULT_SCH_REASON_REQUIRED
    );
    return;
  }

  const confirmed = await proxy.$confirm(
    getMessage(MSG.MY_INFO_DEFAULT_SCH_CHANGE_CONFIRM)
  );
  if (!confirmed) return;

  isSavingDefaultSch.value = true;
  defaultSchErrorMsg.value = "";
  try {
    // TODO(developer): approvalLine 이 비어 있을 때 approverUserCds:[] 전송이 서버 기본 결재자 폴백과
    //   정확히 정합하는지 최종 확인(PRAFTA-002 배선 이후).
    const { data } = await axios.post("/webApi/user01/update-my-default-sch", {
      defaultSchCd: pendingDefaultSchCd.value,
      reqReason: defaultSchReqReason.value,
      approverUserCds: approvalLine.value.map((s) => s.userCd),
    });
    // 성공 — 선택된 옵션에서 schNo 라벨을 파생해 대기 배너를 채운다(defaultSchCd/defaultSchLabel 은 갱신하지 않음).
    const selected = defaultSchOptions.value.find(
      (o) => o.schCd === pendingDefaultSchCd.value
    );
    pendingDefaultSch.value = {
      reqId: data?.reqId || "",
      schCd: pendingDefaultSchCd.value,
      schNo: selected?.schNo || "",
      reqDate: "",
    };
    isEditingDefaultSch.value = false;
    defaultSchReqReason.value = "";
    approvalLine.value = [];
    selectedApprovalPresetId.value = "";
  } catch (err) {
    defaultSchErrorMsg.value = resolveApiErrorMessage(
      err,
      getMessage(MSG.MY_INFO_DEFAULT_SCH_SAVE_FAILED)
    );
  } finally {
    isSavingDefaultSch.value = false;
  }
};

// 선택약관 목록 조회. 대상은 서버가 토큰으로만 결정한다(식별자 미전달, IDOR 방지).
//   비치명적: 실패해도 alert 하지 않고 섹션만 감춘다(내 정보 팝업 본연의 기능은 계속 쓸 수 있어야 한다).
const fnLoadOptionalTerms = async () => {
  try {
    const response = await axios.get("/comApi/consent/my-optional-terms");
    const list = response?.data?.terms || [];
    optionalTerms.value = list.map((t) => ({
      termsId: t.termsId,
      termsNm: t.termsNm,
      termsVersion: t.termsVersion,
      agrYn: t.agrYn === "Y" ? "Y" : "N",
    }));
  } catch {
    optionalTerms.value = [];
  }
};

// 선택약관 토글(낙관적 토글 + POST 저장, 실패 시 원복).
//   연동 회사 제3자 제공 동의(006)의 '철회'(Y→N)만 확인 팝업을 거친다 — 철회는 소급되지 않으므로
//   이미 제공된 자료가 회수되지 않는다는 점을 반드시 고지한다. 동의(N→Y)는 즉시 저장.
const fnToggleTerms = async (terms) => {
  if (isTermsSaving.value) return;

  const prev = terms.agrYn;
  const next = prev === "Y" ? "N" : "Y";

  if (
    terms.termsId === THIRD_PARTY_CONSENT_TERMS_ID &&
    prev === "Y" &&
    next === "N"
  ) {
    const ok = await proxy.$confirm(
      getMessage(MSG.MY_INFO_TERMS_THIRD_PARTY_WITHDRAW_CONFIRM)
    );
    // 취소: 낙관적 토글 이전이라 스위치 상태 변경도, 서버 호출도 없다.
    if (!ok) return;
  }

  terms.agrYn = next;
  isTermsSaving.value = true;
  try {
    // termsVersion 은 보내지 않는다 — 서버가 현재버전을 resolve 한다(클라 버전 위조 차단).
    await axios.post("/comApi/consent/my-optional-terms-agree", {
      termsId: terms.termsId,
      agrYn: next,
    });
  } catch (err) {
    terms.agrYn = prev;
    await proxy.$alert(
      resolveApiErrorMessage(err, getMessage(MSG.MY_INFO_TERMS_SAVE_FAILED))
    );
  } finally {
    isTermsSaving.value = false;
  }
};

// 약관 전문 보기 — 기존 약관 상세 팝업 재사용(로그인 약관 팝업과 동일 경로).
// ───────────────────────────────────────────────────────────
// 위치정보 동의(005) — 앱 마이페이지와 동일한 계약/동작.
//   EP 는 web·app 공용(/comApi/consent/location-consent*)이라 서버는 채널을 가리지 않는다.
// ───────────────────────────────────────────────────────────

// 상태 로드. 비치명적 — 실패하면 행을 감춘다(내 정보 팝업 본연의 기능은 계속 쓸 수 있어야 한다).
const fnLoadLocationConsent = async () => {
  try {
    const { data } = await axios.get("/comApi/consent/location-consent");
    locationConsent.value = {
      consentState: data?.consentState || "",
      termsVersion: data?.termsVersion || "",
      collectAllowed: !!data?.collectAllowed,
      termsId: data?.termsId || "",
      termsNm: data?.termsNm || "",
    };
  } catch {
    locationConsent.value = {
      consentState: "",
      termsVersion: "",
      collectAllowed: false,
      termsId: "",
      termsNm: "",
    };
  }
};

// 상태 전이 공통 호출기. 반환값 = 성공 여부(해제 팝업은 true 일 때만 닫는다).
//   ★낙관적 갱신을 쓰지 않는다 — 철회는 파기를 동반해 되돌릴 수 없으므로 서버 확정값만 반영한다.
const fnCallLocationConsent = async (path, successMsg) => {
  if (isLocationSaving.value) return false;
  isLocationSaving.value = true;
  try {
    const { data } = await axios.post(
      `/comApi/consent/location-consent/${path}`
    );
    locationConsent.value = {
      consentState: data?.consentState || "",
      termsVersion: data?.termsVersion || "",
      collectAllowed: !!data?.collectAllowed,
      // ★전이 응답에는 약관명이 없을 수 있다 — 기존 값을 보존해 [보기] 버튼이 죽지 않게 한다.
      termsId: data?.termsId || locationConsent.value.termsId,
      termsNm: data?.termsNm || locationConsent.value.termsNm,
    };
    await proxy.$alert(
      typeof successMsg === "function" ? successMsg(data) : successMsg
    );
    return true;
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(
        err,
        "처리하지 못했어요. 잠시 후 다시 시도해 주세요."
      )
    );
    return false;
  } finally {
    isLocationSaving.value = false;
  }
};

// 일시 중지 — 과거 기록은 유지된다(법 제24조②). 취소하면 해제 팝업은 열린 채로 남는다.
const fnSuspendLocation = async () => {
  const confirmed = await proxy.$confirm(LOCATION_SUSPEND_CONFIRM);
  if (!confirmed) return;
  const ok = await fnCallLocationConsent(
    "suspend",
    "위치정보 수집을 중지했습니다.\n지금까지의 기록은 그대로 있습니다."
  );
  if (ok) locationOffPopOpen.value = false;
};

// 동의 철회 — ★수집된 위치정보를 전부 파기한다. 팝업 설명과 별개로 확인을 한 번 더 받는다
//   (파기 직전 마지막 관문). 취소하면 해제 팝업은 열린 채로 남는다.
const fnWithdrawLocation = async () => {
  const confirmed = await proxy.$confirm(LOCATION_WITHDRAW_CONFIRM);
  if (!confirmed) return;
  const ok = await fnCallLocationConsent("withdraw", (data) => {
    const purged = Number(data?.purgedRows || 0);
    return purged > 0
      ? `동의를 철회하고 위치정보 ${purged}건을 삭제했습니다.`
      : "동의를 철회했습니다. 삭제할 위치정보는 없었습니다.";
  });
  if (ok) locationOffPopOpen.value = false;
};

// 체크박스 토글 — 선택약관 행과 조작 형태를 맞춘 진입점.
//   ★체크 해제는 그 자리에서 처리하지 않는다. 중지(되돌릴 수 있음)와 철회(파기, 복구 불가)는
//     결과가 전혀 달라 사용자가 골라야 하기 때문이다 → 해제 팝업을 띄운다.
//   체크(재동의)는 잃는 것이 없으므로 즉시 처리한다.
const fnToggleLocationConsent = async () => {
  if (isLocationSaving.value) return;
  if (isLocationAgreed.value) {
    locationOffPopOpen.value = true;
  } else {
    // SUSPENDED / PENDING_REAGREE / WITHDRAWN 어느 상태에서도 resume 가능(서버 계약).
    await fnCallLocationConsent("resume", "위치정보 제공에 다시 동의했습니다.");
  }
};

// 위치정보 약관 전문 보기 — 선택약관 [보기] 와 동일 계약.
const fnViewLocationTerms = () => {
  openPop(TermsDetailPop, {
    termsId_p: locationConsent.value.termsId || "005",
    termsNm_p: locationConsent.value.termsNm || "위치기반서비스 이용약관",
  });
};

const fnViewTerms = (terms) => {
  openPop(TermsDetailPop, {
    termsId_p: terms.termsId,
    termsNm_p: terms.termsNm,
  });
};

const fnSelfWithdrawal = async () => {
  const confirmed = await proxy.$confirm(
    getMessage(MSG.MY_INFO_WITHDRAWAL_CONFIRM)
  );
  if (!confirmed) return;

  try {
    // 탈퇴 대상은 서버가 토큰으로만 결정한다. 식별자 파라미터는 더 이상 전송하지 않는다.
    await axios.post("/webApi/user01/withdraw-my-account", {});
    await proxy.$alert(getMessage(MSG.MY_INFO_WITHDRAWAL_SUCCESS));
    sessionStorage.clear();
    window.location.replace("/safenote");
  } catch (err) {
    const msg = resolveApiErrorMessage(
      err,
      getMessage(MSG.MY_INFO_WITHDRAWAL_FAILED)
    );
    await proxy.$alert(msg);
  }
};

const fnChangePassword = async () => {
  if (!currentPw.value) {
    await proxy.$alert(getMessage(MSG.MY_INFO_CURRENT_PW_REQUIRED));
    return;
  }
  if (!newPw.value) {
    await proxy.$alert(getMessage(MSG.MY_INFO_NEW_PW_REQUIRED));
    return;
  }
  if (newPw.value !== newPwConfirm.value) {
    await proxy.$alert(getMessage(MSG.MY_INFO_PW_MISMATCH));
    return;
  }
  if (newPw.value.length < 8) {
    await proxy.$alert(getMessage(MSG.MY_INFO_PW_TOO_SHORT));
    return;
  }

  const confirmed = await proxy.$confirm(
    getMessage(MSG.MY_INFO_PW_CHANGE_CONFIRM)
  );
  if (!confirmed) return;

  try {
    const response = await axios.post("/webApi/user01/update-my-passwd", {
      cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
      userCd: sessionStorage.getItem("gv_userCd"),
      currentPw: currentPw.value,
      newPw: newPw.value,
    });
    if (response.status === 200) {
      await proxy.$alert(getMessage(MSG.MY_INFO_PW_CHANGED));
      currentPw.value = "";
      newPw.value = "";
      newPwConfirm.value = "";
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(
      err,
      getMessage(MSG.MY_INFO_PW_CHANGE_FAILED)
    );
    await proxy.$alert(msg);
  }
};
</script>

<style scoped>
/* ★섹션별 스크롤바 제거 (2026-08-15 통합테스트 지적)
   전역 modal-popup-guide.css 가 `.form-container` 에 overflow-y:auto + flex:1 1 auto 를 건다.
   그 규칙은 "팝업 바디가 .form-container 하나"인 화면을 전제로 한 것이라, 섹션이 4개인 이 팝업에서는
   섹션마다 스크롤 영역이 만들어져 스크롤바가 4개 보였다(한 줄짜리 근무정보·약관 섹션에도 붙었다).
   스크롤은 .my-info-body 래퍼가 전담하고, 내부 섹션은 내용 높이만큼만 차지하게 되돌린다.
   전역 규칙(.prafta-modal-popup .form-container)보다 우선하도록 선택자를 한 단계 더 붙인다. */
.prafta-modal-popup .my-info-body .form-container {
  overflow: visible;
  flex: 0 0 auto;
  min-height: auto;
  padding: 0 1.2rem 1rem;
}

.section-title {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--color-text-muted, #6b7280);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding: 0.75rem 1.2rem 0;
  border-top: 1px solid var(--color-border, #e5e7eb);
}

.section-title:first-of-type {
  border-top: none;
}

.form-container {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
  padding: 0.75rem 1.2rem 1rem;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  padding: 0.75rem 1.2rem;
  border-top: 1px solid var(--color-border, #e5e7eb);
  background: var(--color-bg, #f9fafb);
}

/* ===== 근무 정보(기본 근무타입 자기변경, F-8-3 / PRAFTA-001·004 신청 흐름 전환) ===== */
.default-sch-hint {
  margin: 0;
  font-size: 0.8rem;
  line-height: 1.5;
  color: var(--color-text-muted, #6b7280);
}

.default-sch-pending {
  font-size: 0.8rem;
  line-height: 1.5;
  color: var(--color-text, #374151);
  background: var(--color-warning-bg, #fffbeb);
  border: 1px solid var(--color-warning-border, #fde68a);
  border-radius: 0.4rem;
  padding: 0.5rem 0.65rem;
}

.default-sch-reason {
  width: 100%;
  resize: vertical;
  font-family: inherit;
  font-size: 0.85rem;
  padding: 0.4rem 0.5rem;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 0.35rem;
}

/* 결재선 구성 (PRAFTA-004, LeaveApplyPop.vue .la-* 톤 재사용) */
.default-sch-approval {
  border-top: 1px solid var(--color-border, #e5e7eb);
  padding-top: 0.6rem;
}
.default-sch-approval__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.85rem;
  margin-bottom: 0.4rem;
}
.default-sch-preset-sel {
  border: 1px solid var(--color-border, #d1d5db);
  border-radius: 0.3rem;
  padding: 0.2rem 0.4rem;
  font-size: 0.78rem;
  background: var(--color-surface, #fff);
  cursor: pointer;
  max-width: 60%;
}
.default-sch-preset-empty {
  font-size: 0.74rem;
  color: var(--color-text-muted, #9ca3af);
}
.default-sch-approval__cols {
  display: flex;
  gap: 0.6rem;
}
.default-sch-pane {
  flex: 1;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 0.4rem;
  padding: 0.4rem;
  min-height: 120px;
}
.default-sch-pane__title {
  font-size: 0.8rem;
  font-weight: 600;
  margin-bottom: 0.3rem;
}
.default-sch-list {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}
.default-sch-cand,
.default-sch-step {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.82rem;
}
.default-sch-cand {
  justify-content: space-between;
}
.default-sch-cand.added {
  opacity: 0.5;
}
.default-sch-cand button,
.default-sch-step button {
  border: 1px solid var(--color-border, #d1d5db);
  background: var(--color-surface, #fff);
  border-radius: 0.3rem;
  padding: 0.1rem 0.4rem;
  font-size: 0.75rem;
  cursor: pointer;
}
.default-sch-step__no {
  width: 1.3rem;
  height: 1.3rem;
  border-radius: 50%;
  background: var(--color-primary, #16a34a);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.72rem;
}
.default-sch-step__nm {
  flex: 1;
}
.default-sch-del {
  color: var(--color-danger, #dc2626);
}
.default-sch-empty {
  font-size: 0.78rem;
  color: var(--color-text-muted, #9ca3af);
  text-align: center;
  padding: 0.6rem 0;
}

.default-sch-actions {
  display: flex;
  gap: 0.5rem;
}

.default-sch-actions .btn {
  flex: 1;
}

/* ===== 약관 동의 설정(선택약관 토글) ===== */
.terms-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.terms-row-text {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  min-width: 0;
}

.terms-row-label {
  font-size: 0.85rem;
  color: var(--color-text, #374151);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.terms-row-view {
  flex-shrink: 0;
  background: transparent;
  border: 0;
  padding: 0;
  font-size: 0.8rem;
  color: var(--color-primary, #16a34a);
  text-decoration: underline;
  cursor: pointer;
}

/* 스위치: 앱 마이페이지 토글과 동일한 조작감(켜짐=동의) */
/* 체크박스 — 앱 마이페이지(.mp-terms-check)와 동일 형태. 종전 스위치를 대체한다. */
.terms-check {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex: 1 1 auto;
  min-width: 0;
  background: transparent;
  border: 0;
  padding: 0;
  cursor: pointer;
  font-family: inherit;
  text-align: left;
}
.terms-check:disabled {
  opacity: 0.5;
  cursor: default;
}
.terms-check-box {
  flex-shrink: 0;
  width: 18px;
  height: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1.5px solid var(--color-border);
  border-radius: 4px;
  background: var(--color-surface, #fff);
}
.terms-check-on .terms-check-box {
  border-color: var(--color-primary);
  background: var(--color-primary);
}
.terms-check-mark {
  width: 12px;
  height: 12px;
  fill: none;
  stroke: #fff;
  stroke-width: 3;
  stroke-linecap: round;
  stroke-linejoin: round;
  opacity: 0;
}
.terms-check-on .terms-check-mark {
  opacity: 1;
}
.terms-check-label {
  font-size: 0.875rem;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 위치정보 행 — 체크박스 줄 + 상태 배지 줄.
   ★배지를 라벨과 같은 줄에 두면 배지 폭만큼 자리를 뺏겨 약관명이 잘린다(앱에서 확인). */
.terms-row-stack {
  display: flex;
  flex-direction: column;
}
.terms-state {
  padding-left: calc(18px + 0.5rem);
  padding-bottom: 0.35rem;
}
.terms-badge {
  display: inline-block;
  padding: 1px 8px;
  border-radius: 10px;
  font-size: 0.75rem;
  background: var(--color-border);
  color: var(--color-text-muted);
}
.terms-badge.is-agreed {
  background: #ecfdf5;
  color: #047857;
}
.terms-badge.is-suspended,
.terms-badge.is-pending {
  background: var(--color-border);
  color: var(--color-text-muted);
}
.terms-badge.is-withdrawn {
  background: #fef2f2;
  color: #b91c1c;
}

.btn-withdrawal {
  background-color: #ef4444;
  color: #fff;
  border: none;
  padding: 0.35rem 0.85rem;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.8rem;
}
.btn-withdrawal:hover {
  background-color: #dc2626;
}
</style>
