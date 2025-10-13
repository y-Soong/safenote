<template>
  <router-view />
</template>

<script setup>
import { useUserStore } from '@/stores/userStore'
import { jwtDecode } from 'jwt-decode'

const token = sessionStorage.getItem('token')
const userStore = useUserStore()

if (token) {
  try {
    // JWT 디코딩
    const decoded = jwtDecode(token)

    // 디코딩에 성공했고 필요한 필드가 있다면 사용
    if (decoded.userId && decoded.userNm && decoded.cmpnyCd) {
      userStore.setUser({
        userId: decoded.userId,
        userNm: decoded.userNm,
        cmpnyCd: decoded.cmpnyCd,
      })
    } else {
      // 디코딩 실패 또는 필드 없음 → 수동 복원
      const userId = sessionStorage.getItem('userId')
      const userNm = sessionStorage.getItem('userNm')
      const cmpnyCd = sessionStorage.getItem('cmpnyCd')

      if (userId && userNm) {
        userStore.setUser({ userId, userNm, cmpnyCd })
      }
    }
  } catch (e) {
    console.error('Invalid JWT', e)
    sessionStorage.clear()
  }
}
// import { jwtDecode } from 'jwt-decode'
// import { useUserStore } from '@/stores/userStore'

// const token = sessionStorage.getItem('token')
// if (token) {
//   const decoded = jwtDecode(token)
//   const userStore = useUserStore()

//   userStore.setUser({
//     userId: decoded.userId
//     , userNm: decoded.userNm
//     , cmpnyCd : decoded.cmpnyCd
//     // 필요하면 추가 정보
//   })
// }
</script>

<style>
/* 전역 스타일 필요 시 여기에 작성 */
</style>
