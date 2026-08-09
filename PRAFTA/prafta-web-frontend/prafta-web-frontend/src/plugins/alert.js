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
            // AlertModal/ConfirmModal 은 <Teleport to="body"> 라 실제 DOM 이 container 밖(body 직속)에 렌더된다
            // — container.querySelector 는 항상 빈 컨테이너만 보므로 document 에서 찾는다(아래 nextTick 포커스 로직과 동일 패턴).
            // 확정 버튼은 variant 에 따라 btn-primary(기본) 또는 btn-danger(F-10 파괴적 확인) — btn-ghost(취소)는 제외.
            const modal = document.querySelector(".prafta-modal-alert-confirm");
            const confirmBtn = modal?.querySelector(".btn-primary, .btn-danger");
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
    // options.variant: 'primary'(기본) | 'danger' — F-10 규약, 삭제 등 파괴적 확인에 한해 'danger' 전달
    app.config.globalProperties.$confirm = (message, options = {}) => {
      return new Promise((resolve) => {
        const container = document.createElement("div");
        document.body.appendChild(container);
        document.body.classList.add("alert-open");

        const blockKeyEvents = (e) => {
          if (e.key === "Enter") {
            e.preventDefault();
            e.stopPropagation();
            // 팝업에서만 처리하도록 → 모달 안에 focus 고정
            // AlertModal/ConfirmModal 은 <Teleport to="body"> 라 실제 DOM 이 container 밖(body 직속)에 렌더된다
            // — container.querySelector 는 항상 빈 컨테이너만 보므로 document 에서 찾는다(아래 nextTick 포커스 로직과 동일 패턴).
            // 확정 버튼은 variant 에 따라 btn-primary(기본) 또는 btn-danger(F-10 파괴적 확인) — btn-ghost(취소)는 제외.
            const modal = document.querySelector(".prafta-modal-alert-confirm");
            const confirmBtn = modal?.querySelector(".btn-primary, .btn-danger");
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
          message: message.replace(/\\n/g, "\n"),
          variant: options.variant === "danger" ? "danger" : "primary",
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
