// src/plugins/popupPlugin.js
import { createVNode, render, nextTick } from "vue";
import AlertModal from "@/components/modal/AlertModal.vue";
import ConfirmModal from "@/components/modal/ConfirmModal.vue";

export default {
  install(app) {
    // $alert 메서드 등록
    app.config.globalProperties.$alert = (message) => {
      return new Promise((resolve) => {
        const container = document.createElement("div");
        document.body.appendChild(container);
        document.body.classList.add("alert-open");

        const blockKeyEvents = (e) => {
          if (e.key === "Enter") {
            e.preventDefault();
            e.stopPropagation();
            // 팝업에서만 처리하도록 → 모달 안에 focus 고정
            const modal = container.querySelector(
              ".alert-modal, .confirm-modal"
            );
            const confirmBtn = modal?.querySelector(".confirm-btn");
            if (confirmBtn) confirmBtn.click();
          }
          if (e.key === "Escape") {
            e.preventDefault();
            e.stopPropagation();
          }
        };
        window.addEventListener("keydown", blockKeyEvents, true);
        window.addEventListener("keyup", blockKeyEvents, true);

        const cleanup = () => {
          render(null, container);
          container.remove();
          document.body.classList.remove("alert-open");
          window.removeEventListener("keydown", blockKeyEvents, true);
          window.removeEventListener("keyup", blockKeyEvents, true);
        };

        const vnode = createVNode(AlertModal, {
          visible: true,
          message: message.replace(/\\n/g, "\n"),
          onConfirm: () => {
            cleanup();
            resolve(true);
          },
          onClose: () => {
            cleanup();
          },
        });

        render(vnode, container);
      });
    };

    // $confirm 메서드 등록
    app.config.globalProperties.$confirm = (message) => {
      return new Promise((resolve) => {
        const container = document.createElement("div");
        document.body.appendChild(container);
        document.body.classList.add("alert-open");

        const blockKeyEvents = (e) => {
          if (e.key === "Enter") {
            e.preventDefault();
            e.stopPropagation();
            // 팝업에서만 처리하도록 → 모달 안에 focus 고정
            const modal = container.querySelector(
              ".alert-modal, .confirm-modal"
            );
            const confirmBtn = modal?.querySelector(".confirm-btn");
            if (confirmBtn) confirmBtn.click();
          }
          if (e.key === "Escape") {
            e.preventDefault();
            e.stopPropagation();
          }
        };
        window.addEventListener("keydown", blockKeyEvents, true);
        window.addEventListener("keyup", blockKeyEvents, true);

        const cleanup = () => {
          render(null, container);
          container.remove();
          document.body.classList.remove("alert-open");
          window.removeEventListener("keydown", blockKeyEvents, true);
          window.removeEventListener("keyup", blockKeyEvents, true);
        };

        const vnode = createVNode(ConfirmModal, {
          visible: true,
          message: message.replace(/\\n/g, "\n"),
          onConfirm: () => {
            cleanup();
            resolve(true);
          },
          onCancel: () => {
            cleanup();
            resolve(false);
          },
          onClose: () => {
            cleanup();
            resolve(false);
          },
        });
        render(vnode, container);
        nextTick().then(() => {
          const el = document.querySelector(".prafta-modal-alert-confirm");
          if (el && typeof el.focus === "function") el.focus();
        });
      });
    };
  },
};
