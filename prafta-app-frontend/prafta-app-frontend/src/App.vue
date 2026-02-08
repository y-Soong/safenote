<template>
  <router-view />
</template>

<script setup>
import { onMounted } from 'vue'
import { useUserStore } from '@/stores/userStore'
import { jwtDecode } from 'jwt-decode'
import api from '@/api/axios'

const userStore = useUserStore()

function setUserFromToken(token) {
  try {
    const decoded = jwtDecode(token)

    // ✅ JWT에 gv_* 로 들어있다고 가정(너 백엔드 generateToken 기준)
    if (decoded.gv_userId && decoded.gv_userNm && decoded.gv_cmpnyCd) {
      userStore.setUser({
        gv_cmpnyCd: decoded.gv_cmpnyCd,
        gv_userId: decoded.gv_userId,
        gv_userNm: decoded.gv_userNm,
        gv_siteCd: decoded.gv_siteCd,
        gv_siteNo: decoded.gv_siteNo,
        gv_siteNm: decoded.gv_siteNm,
        gv_authCd: decoded.gv_authCd,
        gv_mblNo: decoded.gv_mblNo,
        gv_email: decoded.gv_email,
      })
      return true
    }
  } catch (e) {
    console.error('Invalid JWT', e)
  }
  return false
}

// ✅ 1) 기존 로직: token 있으면 복원
const token = sessionStorage.getItem('token')
if (token) {
  const ok = setUserFromToken(token)
  if (!ok) {
    // fallback
    const gv_cmpnyCd = sessionStorage.getItem('gv_cmpnyCd')
    const gv_userId = sessionStorage.getItem('gv_userId')
    const gv_userNm = sessionStorage.getItem('gv_userNm')

    if (gv_userId && gv_userNm) {
      userStore.setUser({ gv_cmpnyCd, gv_userId, gv_userNm })
    } else {
      sessionStorage.clear()
    }
  }
}

// ✅ 2) 새로고침/웹뷰 재로딩/새 탭 대비: token 없으면 refresh 시도
onMounted(async () => {
  const tokenNow = sessionStorage.getItem('token')
  const refreshToken = localStorage.getItem('refreshToken')

  if (!tokenNow && refreshToken) {
    try {
      const res = await api.post('/comApi/auth/refresh', { refreshToken })
      const newToken = res.data?.token

      if (newToken) {
        sessionStorage.setItem('token', newToken)
        setUserFromToken(newToken)

        // 로그인 화면이면 메인으로 이동시키고 싶으면
        // window.location.hash = "#/MainView";
      }
    } catch (e) {
      sessionStorage.clear()
      localStorage.removeItem('refreshToken')
      userStore.logout()
      // 로그인으로
      window.location.hash = '#/'
    }
  }
})
</script>
