<template>
  <div class="login-page">
    <!-- 로고 / 브랜드 -->
    <div class="brand">
      <img :src="safenote_logo" class="brand-logo" alt="logo" />
      <h1 class="brand-title">SAFETY NOTE</h1>
    </div>

    <!-- 로그인 박스 -->
    <div class="login-box">
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

const userId = ref('')
const password = ref('')
const rememberId = ref(false)
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

function focusKill(e) {
  if (e.target.id == 'userId') {
    if (proxy.$util.isNotEmpty(userId.value)) {
      userIdFocusKill()
    }
  }
}

/* API Call */
const fnSubmitLogin = async () => {
  if (!userId.value || !password.value) {
    await proxy.$alert('아이디와 비밀번호를 모두 입력해주세요')
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
      localStorage.setItem('refreshToken', refreshToken)

      // ✅ 아이디 저장 처리
      if (rememberId.value) {
        localStorage.setItem('savedUserId', userId.value)
      } else {
        localStorage.removeItem('savedUserId')
      }

      const redirect = route.query.redirect
      router.replace(redirect || '/MainView') // ✅ 권장

      /* 약관동의 체크 */
      //fnUserTermsAgrChk()

      // router.push('/MainView');
      // router.replace('/MainView')
    }
  } catch (err) {
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
function userIdFocusKill() {
  userId.value = proxy.$util.toUpperCase(userId.value)
}

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
