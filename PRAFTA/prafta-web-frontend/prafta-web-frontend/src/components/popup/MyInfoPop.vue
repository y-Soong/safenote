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
             2026-08-05 사용자 확정(3경로: 관리자/웹 내정보/앱 마이페이지)에 따라 F-8-2 API 를 연결한다. -->
        <div class="section-title">근무 정보</div>
        <div class="form-container">
          <div class="form-row-max" v-if="!isEditingDefaultSch">
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
                  v-for="opt in defaultSchOptions"
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
              ⓘ 기본 근무타입 변경 시 내일(명일)부터 당해 연말까지 평일
              근무계획이 자동 생성·갱신됩니다(빈 날·자동생성분만,
              휴일·연차·교대팀 구간 제외).
            </p>
            <span class="form-msg" v-if="defaultSchErrorMsg">{{
              defaultSchErrorMsg
            }}</span>

            <!-- F-10 규약: 왼쪽=진행/확정(저장, primary), 오른쪽=이탈(취소) — 파일 기존 관례상
                 이탈 버튼은 정의되지 않은 btn-ghost 대신 공용 btn-second 사용(닫기 버튼과 동일). -->
            <div class="default-sch-actions">
              <button
                type="button"
                class="btn btn-primary"
                :disabled="!pendingDefaultSchCd || isSavingDefaultSch"
                @click="onSaveDefaultSch"
              >
                저장
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

        <!-- 약관 동의 설정 (선택약관 on/off) — 선택약관이 1건 이상일 때만 노출.
             앱 마이페이지(MyPageView "약관 동의 설정")와 같은 목록/동작을 웹에도 제공한다.
             연동 회사 제3자 제공 동의(006)는 이 토글이 유일한 웹 상시 변경 수단이다. -->
        <template v-if="optionalTerms.length > 0">
          <div class="section-title">약관 동의 설정</div>
          <div class="form-container">
            <div
              v-for="terms in optionalTerms"
              :key="terms.termsId"
              class="terms-row"
            >
              <div class="terms-row-text">
                <span class="terms-row-label">{{
                  "(선택) " + terms.termsNm
                }}</span>
                <button
                  type="button"
                  class="terms-row-view"
                  @click="fnViewTerms(terms)"
                >
                  보기
                </button>
              </div>
              <button
                type="button"
                role="switch"
                class="terms-switch"
                :class="{ 'terms-switch-on': terms.agrYn === 'Y' }"
                :aria-checked="terms.agrYn === 'Y' ? 'true' : 'false'"
                :aria-label="terms.termsNm + ' 동의'"
                :disabled="isTermsSaving"
                @click="fnToggleTerms(terms)"
              >
                <span class="terms-switch-knob"></span>
              </button>
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
            <button class="btn btn-second" @click="$emit('close')">
              닫기
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from "vue";
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
const isSchOptionsLoading = ref(false);
const pendingDefaultSchCd = ref("");
const isSavingDefaultSch = ref(false);
const defaultSchErrorMsg = ref("");

// 선택약관 동의 설정 — GET /comApi/consent/my-optional-terms 응답(현재버전 + agrYn).
//   비치명적: 조회 실패 시 빈 목록(섹션 미노출). 토글은 POST /comApi/consent/my-optional-terms-agree.
const optionalTerms = ref([]);
// 토글 저장 직렬화 가드(동시 저장 경합 방지).
const isTermsSaving = ref(false);

onMounted(async () => {
  await fnLoadMyInfo();
  await fnLoadOptionalTerms();
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

// 변경 클릭 → 인라인 전환 + 옵션 로드(현재값으로 선택 초기화).
const onStartEditDefaultSch = async () => {
  isEditingDefaultSch.value = true;
  pendingDefaultSchCd.value = defaultSchCd.value;
  defaultSchErrorMsg.value = "";
  await fnLoadDefaultSchOptions();
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

const onCancelEditDefaultSch = () => {
  isEditingDefaultSch.value = false;
  defaultSchErrorMsg.value = "";
};

// 저장 — 부작용 고지(명일부터 연말까지 근무계획 자동 생성·갱신) 확인 후 저장.
const onSaveDefaultSch = async () => {
  if (!pendingDefaultSchCd.value) return;

  const confirmed = await proxy.$confirm(
    getMessage(MSG.MY_INFO_DEFAULT_SCH_CHANGE_CONFIRM)
  );
  if (!confirmed) return;

  isSavingDefaultSch.value = true;
  defaultSchErrorMsg.value = "";
  try {
    await axios.post("/webApi/user01/update-my-default-sch", {
      defaultSchCd: pendingDefaultSchCd.value,
    });
    // 성공 — 선택된 옵션으로 현재값 표시 갱신 후 인라인 편집 종료.
    const selected = defaultSchOptions.value.find(
      (o) => o.schCd === pendingDefaultSchCd.value
    );
    defaultSchCd.value = pendingDefaultSchCd.value;
    if (selected) {
      defaultSchLabel.value = `${selected.schNo} (${fnFmtSchTime(
        selected.fstSchStrTime
      )}~${fnFmtSchTime(selected.fstSchEndTime)})`;
    }
    isEditingDefaultSch.value = false;
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

/* ===== 근무 정보(기본 근무타입 자기변경, F-8-3) ===== */
.default-sch-hint {
  margin: 0;
  font-size: 0.8rem;
  line-height: 1.5;
  color: var(--color-text-muted, #6b7280);
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
.terms-switch {
  flex-shrink: 0;
  position: relative;
  width: 40px;
  height: 22px;
  padding: 0;
  border: 0;
  border-radius: 999px;
  background: var(--color-border-strong, #d1d5db);
  cursor: pointer;
  transition: background 0.15s ease;
}

.terms-switch:disabled {
  opacity: 0.6;
  cursor: default;
}

.terms-switch-on {
  background: var(--color-primary, #16a34a);
}

.terms-switch-knob {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 16px;
  height: 16px;
  border-radius: 999px;
  background: #ffffff;
  transition: transform 0.15s ease;
}

.terms-switch-on .terms-switch-knob {
  transform: translateX(18px);
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
