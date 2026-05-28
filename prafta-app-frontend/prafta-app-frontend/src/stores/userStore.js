import { defineStore } from 'pinia'

// 정책 §11.1(최소 수집·목적 제한)에 따라 클라이언트 store에는 PII(휴대폰/이메일)를 보관하지 않는다.
// 휴대폰/이메일이 필요한 화면은 인증 API로 직접 조회한다.
// web 프론트(@/stores/userStore)와 동일한 gv_* 스키마를 사용한다.
export const useUserStore = defineStore('user', {
  state: () => ({
    gv_cmpnyCd: null,
    gv_userCd: null,
    gv_userId: null,
    gv_userNm: null,
    gv_siteCd: null,
    gv_siteNo: null,
    gv_siteNm: null,
    gv_nodeCd: null,
    gv_nodeNm: null,
    gv_authCd: null,
    gv_authLevel: null,
  }),
  actions: {
    setUser(data) {
      this.gv_cmpnyCd = data.cmpnyCd
      this.gv_userCd = data.userCd
      this.gv_userId = data.userId
      this.gv_userNm = data.userNm
      this.gv_siteCd = data.siteCd
      this.gv_siteNo = data.siteNo
      this.gv_siteNm = data.siteNm
      this.gv_nodeCd = data.nodeCd
      this.gv_nodeNm = data.nodeNm
      this.gv_authCd = data.authCd
      this.gv_authLevel = data.authLevel
    },
    logout() {
      this.gv_cmpnyCd = null
      this.gv_userCd = null
      this.gv_userId = null
      this.gv_userNm = null
      this.gv_siteCd = null
      this.gv_siteNo = null
      this.gv_siteNm = null
      this.gv_nodeCd = null
      this.gv_nodeNm = null
      this.gv_authCd = null
      this.gv_authLevel = null
    },
  },
})
