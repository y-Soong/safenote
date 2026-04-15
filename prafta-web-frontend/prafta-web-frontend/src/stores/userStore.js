import { defineStore } from "pinia";

export const useUserStore = defineStore("user", {
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
    gv_mblNo: null,
    gv_email: null,
  }),
  actions: {
    setUser(data) {
      this.gv_cmpnyCd = data.cmpnyCd;
      this.gv_userCd = data.userCd;
      this.gv_userId = data.userId;
      this.gv_userNm = data.userNm;
      this.gv_siteCd = data.siteCd;
      this.gv_siteNo = data.siteNo;
      this.gv_siteNm = data.siteNm;
      this.gv_nodeCd = data.nodeCd;
      this.gv_nodeNm = data.nodeNm;
      this.gv_authCd = data.authCd;
      this.gv_authLevel = data.authLevel;
      this.gv_mblNo = data.mblNo;
      this.gv_email = data.email;
    },
    logout() {
      this.gv_cmpnyCd = null;
      this.gv_userCd = null;
      this.gv_userId = null;
      this.gv_userNm = null;
      this.gv_siteCd = null;
      this.gv_siteNo = null;
      this.gv_siteNm = null;
      this.gv_nodeCd = null;
      this.gv_nodeNm = null;
      this.gv_authCd = null;
      this.gv_authLevel = null;
      this.gv_mblNo = null;
      this.gv_email = null;
    },
  },
});
