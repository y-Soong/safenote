import { defineStore } from "pinia";

export const useTabStore = defineStore("tab", {
  state: () => ({
    tabs: [], // 처음엔 비어 있음
    activeTab: null,
  }),
  actions: {
    addTab(tab) {
      const exists = this.tabs.find((t) => t.route === tab.route);
      if (!exists) {
        this.tabs.push(tab);
      }
      this.activeTab = tab.route;
    },
    closeTab(route) {
      this.tabs = this.tabs.filter((t) => t.route !== route);
      if (this.activeTab === route) {
        this.activeTab = this.tabs.length ? this.tabs[0].route : null;
      }
    },
  },
});
