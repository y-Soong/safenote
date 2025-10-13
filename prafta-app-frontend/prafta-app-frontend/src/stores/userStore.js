import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    userId: '',
    userNm: '',
    cmpnyCd: '',
  }),
  actions: {
    setUser(data) {
      this.userId = data.userId
      this.userNm = data.userNm
      this.cmpnyCd = data.cmpnyCd
    },
    logout() {
      this.userId = ''
      this.userNm = ''
      this.cmpnyCd = ''
    },
  },
})
