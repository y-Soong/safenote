import { defineStore } from "pinia";

export const useUserStore = defineStore("user", {
  state: () => ({
    gv_cmpnyCd: null,
    gv_userId: null,
    gv_userNm: null,
    gv_authCd: null,
    gv_mblNo: null,
    gv_email: null,
  }),
  actions: {
    setUser(data) {
      this.gv_cmpnyCd = data.cmpnyCd;
      this.gv_userId = data.userId;
      this.gv_userNm = data.userNm;
      this.gv_authCd = data.authCd;
      this.gv_mblNo = data.mblNo;
      this.gv_email = data.email;
    },
    logout() {
      this.gv_cmpnyCd = null;
      this.gv_userId = null;
      this.gv_userNm = null;
      this.gv_authCd = null;
      this.gv_mblNo = null;
      this.gv_email = null;
    },
  },
});
