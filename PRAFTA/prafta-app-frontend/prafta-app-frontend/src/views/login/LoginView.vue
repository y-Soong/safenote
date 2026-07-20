<template>
  <div class="login-page">
    <!-- 로고 / 브랜드 -->
    <div class="brand">
      <img :src="safenote_logo" class="brand-logo" alt="logo" />
      <h1 class="brand-title">SAFENOTE</h1>
    </div>

    <!-- 로그인 박스 -->
    <div class="login-box">
      <!-- 사용자 유형 토글 (PRAFTA-app-027-5) -->
      <div class="user-type-toggle" role="tablist" aria-label="사용자 유형">
        <button
          type="button"
          class="user-type-btn"
          :class="{ 'user-type-btn--active': userType === 'REGULAR' }"
          role="tab"
          :aria-selected="userType === 'REGULAR'"
          @click="userType = 'REGULAR'"
        >
          정규 사용자
        </button>
        <button
          type="button"
          class="user-type-btn"
          :class="{ 'user-type-btn--active': userType === 'DAILY' }"
          role="tab"
          :aria-selected="userType === 'DAILY'"
          @click="userType = 'DAILY'"
        >
          일용직
        </button>
      </div>

      <!-- 아이디 입력 -->
      <div class="field">
        <input
          id="userId"
          type="text"
          v-model="userId"
          placeholder="아이디를 입력하세요"
          @blur="focusKill"
          class="form-input"
        />
      </div>

      <!-- 비밀번호 입력 -->
      <div class="field password-field">
        <input
          :type="showPassword ? 'text' : 'password'"
          v-model="password"
          placeholder="비밀번호를 입력하세요"
          class="form-input pr-10"
        />
        <button
          type="button"
          class="eye-btn"
          @click="showPassword = !showPassword"
          :aria-label="showPassword ? '비밀번호 숨기기' : '비밀번호 표시'"
        >
          <svg
            v-if="showPassword"
            xmlns="http://www.w3.org/2000/svg"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
            <circle cx="12" cy="12" r="3" />
          </svg>
          <svg
            v-else
            xmlns="http://www.w3.org/2000/svg"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path
              d="M17.94 17.94A10.94 10.94 0 0 1 12 20c-7 0-11-8-11-8a21.85 21.85 0 0 1 5.06-5.94"
            />
            <path d="M9.9 4.24A10.94 10.94 0 0 1 12 4c7 0 11 8 11 8a21.78 21.78 0 0 1-3.16 4.19" />
            <path d="M1 1l22 22" />
            <path d="M14.12 14.12a3 3 0 1 1-4.24-4.24" />
          </svg>
        </button>
      </div>

      <!-- 에러 메시지 -->
      <p v-if="errorMessage" class="error-msg">
        {{ errorMessage }}
      </p>

      <!-- 아이디 저장 -->
      <div class="remember-row">
        <input id="rememberId" type="checkbox" v-model="rememberId" class="remember-check" />
        <label for="rememberId" class="remember-label">아이디 저장</label>
      </div>

      <!-- 로그인 버튼 -->
      <button @click="fnSubmitLogin" class="btn-login">로그인</button>
    </div>

    <!-- 아이디/비밀번호 찾기 -->
    <div class="help-links">
      <a class="aTagCls" href="#" @click.prevent="acountInfoSrch">아이디/비밀번호 찾기</a>
    </div>

    <!-- 가입하기 영역 -->
    <div class="signup-box">
      <p class="signup-desc">계정이 없나요?</p>
      <button class="btn-signup" @click="fnOpenTerms">가입하기</button>
    </div>

    <!-- Footer -->
    <footer class="footer">
      <p class="footer-brand">PRAFTA</p>
      <p>고객센터 1234-5678</p>
      <p>© PRAFTA INC. ALL RIGHTS RESERVED.</p>
    </footer>
  </div>
</template>

<script setup>
import { ref, getCurrentInstance, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import safenote_logo from '@/assets/img/safenote_sign.png'
import axios from '@/api/axios'
import { requestDeviceInfo, getCachedDeviceMeta } from '@/utils/deviceBridge'
import { routeAfterLogin } from '@/utils/termsGate'

const userId = ref('')
const password = ref('')
const rememberId = ref(false)
/* 사용자 유형 (PRAFTA-app-027-5): 'REGULAR' | 'DAILY'. 기본 정규. */
const userType = ref('REGULAR')
const showPassword = ref(false)
const router = useRouter()
const route = useRoute()
const errorMessage = ref('')

const { proxy } = getCurrentInstance()

onMounted(() => {
  const token = sessionStorage.getItem('token')
  if (token) {
    router.replace('/MainView')
    return
  }

  if (proxy.$util.isNotEmpty(route) && proxy.$util.isNotEmpty(route.query.userId)) {
    userId.value = route.query.userId
  }

  const savedId = localStorage.getItem('savedUserId')
  if (savedId) {
    userId.value = savedId
    rememberId.value = true
  }
})

function focusKill() {
  // 사용자 ID 자동 대문자 변환 제거: 입력값을 변형하지 않음
}

/* API Call */
const fnSubmitLogin = async () => {
  if (!userId.value || !password.value) {
    await proxy.$alert('아이디와 비밀번호를 모두 입력해주세요')
    return
  }

  // PRAFTA-app-027-5: 일용직 토글 선택 시 별도 EP 로 분기(정규 흐름과 완전 분리).
  if (userType.value === 'DAILY') {
    await fnSubmitDailyLogin()
    return
  }

  try {
    // prafta-com-003 C4: 네이티브 디바이스 정보 취득(브리지 부재/실패 시 폴백, 로그인 차단 안 함).
    //   deviceId 는 axios 인터셉터가 gv_deviceId 로 동봉하므로 여기서는 메타(type/model/os/app)만 body 에 추가한다.
    //   브리지가 deviceId 를 반환하면 localStorage('gv_deviceId')가 네이티브값으로 갱신된 뒤 인터셉터가 읽는다.
    try {
      await requestDeviceInfo()
    } catch (e) {
      console.warn('[Login] 디바이스 정보 취득 실패(로그인 영향 없음):', e?.message)
    }
    const deviceMeta = getCachedDeviceMeta()

    const response = await axios.post('/comApi/login/login', {
      userId: userId.value,
      userPw: password.value,
      ...deviceMeta,
    })

    if (response.status === 200) {
      // PRAFTA-037-F3: 인증대기(SYS013='04') 계정은 nextStep='PHONE_AUTH' + 임시 scope JWT 응답.
      // 임시 토큰은 URL 쿼리스트링 노출 금지 → vue-router history state 로만 전달.
      if (response.data?.nextStep === 'PHONE_AUTH') {
        router.push({
          path: '/PhoneAuth',
          state: {
            phoneAuthToken: response.data.token,
            cmpnyCd: response.data.cmpnyCd,
          },
        })
        return
      }

      // PRAFTA-COM-008-E-8c: 기본 근무타입 미설정(교대 비소속) 게이트.
      // 임시 scope=DEFAULT_SCH 토큰을 history state 로만 전달(URL 노출 금지).
      if (response.data?.nextStep === 'DEFAULT_SCH') {
        router.push({
          path: '/DefaultSchGate',
          state: {
            defaultSchToken: response.data.token,
            cmpnyCd: response.data.cmpnyCd,
          },
        })
        return
      }

      // 정책 §11.1에 따라 휴대폰(mblNo)/이메일(email)은 응답에 없으며 sessionStorage/store에 보관하지 않는다.
      const {
        token,
        userCd,
        userId: id,
        userNm,
        cmpnyCd,
        siteCd,
        siteNo,
        siteNm,
        nodeCd,
        nodeNm,
        authCd,
        authLevel,
        refreshToken,
        employmentType,
      } = response.data

      // ✅ 로그인 토큰 세팅
      sessionStorage.setItem('token', token)
      axios.defaults.headers.common.Authorization = `Bearer ${token}`

      sessionStorage.setItem('gv_cmpnyCd', cmpnyCd)
      sessionStorage.setItem('gv_userCd', userCd)
      sessionStorage.setItem('gv_userId', id)
      sessionStorage.setItem('gv_userNm', userNm)
      sessionStorage.setItem('gv_siteCd', siteCd)
      sessionStorage.setItem('gv_siteNo', siteNo)
      sessionStorage.setItem('gv_siteNm', siteNm)
      sessionStorage.setItem('gv_nodeCd', nodeCd)
      sessionStorage.setItem('gv_nodeNm', nodeNm)
      sessionStorage.setItem('gv_authCd', authCd)
      sessionStorage.setItem('gv_authLevel', authLevel)
      // prafta-app-025 J1-4: 고용형태(일용직=DAILY) 저장. 각 화면이 라운드트립 없이 근태조회 숨김 판정에 사용.
      sessionStorage.setItem('gv_employmentType', employmentType || '')
      localStorage.setItem('refreshToken', refreshToken)

      // prafta-com-008-F (F03) 푸시 토큰 등록은 게이트 체인 통과 후로 이연(termsGate.routeAfterRequiredTerms).
      //   여기서 호출하면 약관 미동의 시 서버 게이트(AUTH_403_001)에 걸려 강제 로그아웃 레이스가 발생한다.

      // ✅ 아이디 저장 처리
      if (rememberId.value) {
        localStorage.setItem('savedUserId', userId.value)
      } else {
        localStorage.removeItem('savedUserId')
      }

      const redirect = route.query.redirect

      // prafta-app-033: 강제 비밀번호 변경 게이트(PWD_CHG_DTIME IS NULL → nextStep='PASSWORD_CHANGE').
      //   PHONE_AUTH/DEFAULT_SCH 와 달리 정식 토큰이 발급되므로(위에서 세션 저장 완료) 비번변경 EP 호출이 가능하다.
      //   약관 게이트보다 먼저 처리한다(routeAfterLogin 호출 전). 성공 시 화면이 routeAfterLogin 으로 후속 진행.
      if (response.data?.nextStep === 'PASSWORD_CHANGE' || response.data?.mustChangePassword) {
        router.replace({
          path: '/ForcedPasswordChange',
          state: { redirect: redirect || '/MainView' },
        })
        return
      }

      // 필수약관 미동의 게이트: 미동의 약관이 있으면 /TermsAgree, 없으면 redirect||/MainView 로 라우팅.
      //   게이트 조회 실패는 가용성 우선으로 통과(termsGate 내부 처리).
      await routeAfterLogin(router, redirect)
    }
  } catch (err) {
    await proxy.$alert(err.response?.data?.message || '로그인에 실패했습니다.')
  }
}

/**
 * PRAFTA-app-027-5: 일용직 직접 로그인.
 *   - POST /comApi/dailyLogin/login (userId/userPw + 디바이스 메타)
 *   - 응답에 nextStep(PHONE_AUTH/DEFAULT_SCH) 게이트 없음. 토큰/세션 저장 후 곧바로 MainView 라우팅.
 *   - 일용직은 NODE/AUTH 미발급 → 해당 세션 키는 빈값으로 저장(다운스트림 호환).
 *   - gv_userTrack='DAILY' + gv_employmentType='DAILY' 저장(MainView/J1-4 화면 숨김 신호 재사용).
 *   - 차단(비활성/만료/오ID/오비번/탈퇴)은 백엔드 통합 메시지를 그대로 노출(계정 존재 비노출).
 */
const fnSubmitDailyLogin = async () => {
  try {
    try {
      await requestDeviceInfo()
    } catch (e) {
      console.warn('[DailyLogin] 디바이스 정보 취득 실패(로그인 영향 없음):', e?.message)
    }
    const deviceMeta = getCachedDeviceMeta()

    const response = await axios.post('/comApi/dailyLogin/login', {
      userId: userId.value,
      userPw: password.value,
      ...deviceMeta,
    })

    if (response.status === 200) {
      const {
        token,
        userCd,
        userId: id,
        userNm,
        cmpnyCd,
        siteCd,
        siteNo,
        siteNm,
        authCd,
        authLevel,
        employmentType,
        userTrack,
        refreshToken,
      } = response.data

      // ✅ 로그인 토큰 세팅
      sessionStorage.setItem('token', token)
      axios.defaults.headers.common.Authorization = `Bearer ${token}`

      sessionStorage.setItem('gv_cmpnyCd', cmpnyCd)
      sessionStorage.setItem('gv_userCd', userCd)
      sessionStorage.setItem('gv_userId', id)
      sessionStorage.setItem('gv_userNm', userNm)
      sessionStorage.setItem('gv_siteCd', siteCd)
      // PRAFTA-app-027-5'(통합형): 일용직도 TB_USER 정식 사용자 → 정규 동일 세션 값 저장.
      sessionStorage.setItem('gv_siteNo', siteNo || '')
      sessionStorage.setItem('gv_authCd', authCd || '')
      sessionStorage.setItem('gv_authLevel', authLevel || '')
      sessionStorage.setItem('gv_siteNm', siteNm)
      // NODE_CD=NULL(일용직 무소속) — 노드 세션 키는 빈값 유지.
      sessionStorage.setItem('gv_nodeCd', '')
      sessionStorage.setItem('gv_nodeNm', '')
      // 일용직 식별: 트랙 + 고용형태(J1-4 근태조회 숨김 신호 재사용, 응답값 사용·폴백 'DAILY').
      sessionStorage.setItem('gv_userTrack', userTrack || 'DAILY')
      sessionStorage.setItem('gv_employmentType', employmentType || 'DAILY')
      localStorage.setItem('refreshToken', refreshToken)

      // 푸시 토큰 등록은 게이트 체인 통과 후로 이연(termsGate.routeAfterRequiredTerms — 정규 로그인과 동일 사유).

      // ✅ 아이디 저장 처리
      if (rememberId.value) {
        localStorage.setItem('savedUserId', userId.value)
      } else {
        localStorage.removeItem('savedUserId')
      }

      // 필수약관 미동의 게이트(일용직도 동일 적용).
      const redirect = route.query.redirect
      await routeAfterLogin(router, redirect)
    }
  } catch (err) {
    // 일용직 계약서+승인제 T4: 입장 승인 대기(006)/거부(007)는 alert 대신 전용 안내 화면으로(R4).
    //   상태는 history state 로만 전달(URL 미노출). 그 외 에러는 기존 통합 메시지 유지.
    const errorCode = err.response?.data?.errorCode
    if (errorCode === 'DAILYLOGIN_400_006') {
      router.replace({ path: '/DailyEntryPending', state: { status: 'PENDING' } })
      return
    }
    if (errorCode === 'DAILYLOGIN_400_007') {
      router.replace({ path: '/DailyEntryPending', state: { status: 'REJECTED' } })
      return
    }
    await proxy.$alert(err.response?.data?.message || '로그인에 실패했습니다.')
  }
}

// const fnUserTermsAgrChk = async () => {
//   try {
//     const response = await axios.post("/comApi/login/getUserTermsAgrChk", {
//       userId: userId.value,
//     });

//     if (response.status === 200) {
//       userTermsNonAgrList.value = response.data;

//       if (response.data.length > 0) {
//         openPop(TermsPop, {
//           loginFlg_p: "Y",
//           userTermsNonAgrList_p: userTermsNonAgrList.value,
//           onMoveMain: fnMoveMainPath,
//           onfnUserLogout: fnUserLogout,
//         });
//         // fnUserLogout();
//       } else {
//         fnMoveMainPath();
//       }
//     }
//   } catch (err) {
//     await proxy.$alert(err.response?.data?.message || "로그인에 실패했습니다.");
//   }
// };

/* User Function */
function fnOpenTerms() {
  router.push('/TermsInfo')
}

function acountInfoSrch() {
  router.push('/ActInfoSrch')
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: #fff;
  padding: 1.5rem 1.25rem 2rem;
  box-sizing: border-box;
}

/* 브랜드 영역 */
.brand {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 1.5rem;
  margin-bottom: 1.75rem;
}

.brand-logo {
  width: 140px;
  height: auto;
  margin-bottom: 0.75rem;
}

.brand-title {
  font-size: 1.5rem;
  font-weight: 800;
  color: #1f1f1f;
  letter-spacing: 0.5px;
  margin: 0;
}

/* 로그인 박스 */
.login-box {
  width: 100%;
  max-width: 360px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 1.5rem;
  box-sizing: border-box;
}

/* 사용자 유형 토글 (PRAFTA-app-027-5) */
.user-type-toggle {
  display: flex;
  width: 100%;
  margin-bottom: 1rem;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;
}

.user-type-btn {
  flex: 1;
  padding: 0.6rem 0;
  background: #fff;
  color: #6b7280;
  font-size: 0.9rem;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.user-type-btn--active {
  background: #16a34a;
  color: #fff;
}

.field {
  margin-bottom: 0.75rem;
}

.form-input {
  width: 100%;
  padding: 0.75rem 1rem;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  font-size: 0.95rem;
  color: #1f1f1f;
  background: #fff;
  box-sizing: border-box;
  outline: none;
  transition: border-color 0.15s;
}

.form-input::placeholder {
  color: #9ca3af;
}

.form-input:focus {
  border-color: #16a34a;
}

.pr-10 {
  padding-right: 2.75rem;
}

/* 비밀번호 + 눈 아이콘 */
.password-field {
  position: relative;
}

.eye-btn {
  position: absolute;
  top: 50%;
  right: 0.75rem;
  transform: translateY(-50%);
  background: transparent;
  border: none;
  padding: 0.25rem;
  cursor: pointer;
  color: #6b7280;
  display: flex;
  align-items: center;
  justify-content: center;
}

.eye-btn svg {
  width: 20px;
  height: 20px;
}

.error-msg {
  color: #ef4444;
  font-size: 0.8rem;
  margin: 0 0 0.5rem;
}

/* 아이디 저장 (우측 정렬) */
.remember-row {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin: 0.5rem 0 1rem;
}

.remember-check {
  margin-right: 0.4rem;
  width: 16px;
  height: 16px;
  accent-color: #1f1f1f;
}

.remember-label {
  font-size: 0.85rem;
  color: #4b5563;
}

/* 로그인 버튼 */
.btn-login {
  width: 100%;
  padding: 0.85rem;
  background: #16a34a;
  color: #fff;
  font-size: 1rem;
  font-weight: 600;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s;
}

.btn-login:hover {
  background: #15803d;
}

/* 아이디/비밀번호 찾기 */
.help-links {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 1rem;
  font-size: 0.85rem;
}

.help-links a,
.aTagCls {
  color: #4b5563;
  text-decoration: none;
}

.aTagCls:hover {
  color: #1f1f1f;
}

/* 가입하기 */
.signup-box {
  width: 100%;
  max-width: 360px;
  margin-top: 0.75rem;
  text-align: center;
}

.signup-desc {
  font-size: 0.85rem;
  color: #6b7280;
  margin: 0 0 0.6rem;
}

.btn-signup {
  width: 100%;
  padding: 0.75rem;
  background: #fff;
  color: #16a34a;
  border: 1px solid #16a34a;
  border-radius: 10px;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
}

.btn-signup:hover {
  background: #f0fdf4;
}

/* Footer */
.footer {
  margin-top: auto;
  padding-top: 2rem;
  text-align: center;
  font-size: 0.75rem;
  color: #9ca3af;
  line-height: 1.6;
}

.footer-brand {
  font-weight: 700;
  color: #1f1f1f;
  font-size: 0.85rem;
  margin: 0 0 0.25rem;
}

.footer p {
  margin: 0;
}
</style>
