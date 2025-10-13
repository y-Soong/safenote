// src/plugins/popupPlugin.js
import { createVNode, render } from 'vue'
import AlertModal from '@/components/modal/AlertModal.vue'
import ConfirmModal from '@/components/modal/ConfirmModal.vue'

export default {
  install(app) {
    // $alert 메서드 등록
    app.config.globalProperties.$alert = (message) => {
      return new Promise((resolve) => {
        const container = document.createElement('div')
        document.body.appendChild(container)
        document.body.classList.add('alert-open')

        const blockKeyEvents = (e) => {
          e.preventDefault()
          e.stopPropagation()
        }
        window.addEventListener('keydown', blockKeyEvents, true)
        window.addEventListener('keyup', blockKeyEvents, true)

        const cleanup = () => {
          render(null, container)
          container.remove()
          document.body.classList.remove('alert-open')
          window.removeEventListener('keydown', blockKeyEvents, true)
          window.removeEventListener('keyup', blockKeyEvents, true)
        }

        const vnode = createVNode(AlertModal, {
          visible: true,
          message: message.replace(/\\n/g, '\n'),
          onConfirm: () => {
            cleanup()
            resolve(true)
          },
          onClose: () => {
            cleanup()
          },
        })

        render(vnode, container)
      })
    }

    // $confirm 메서드 등록
    app.config.globalProperties.$confirm = (message) => {
      return new Promise((resolve) => {
        const container = document.createElement('div')
        document.body.appendChild(container)
        document.body.classList.add('alert-open')

        const blockKeyEvents = (e) => {
          e.preventDefault()
          e.stopPropagation()
        }
        window.addEventListener('keydown', blockKeyEvents, true)
        window.addEventListener('keyup', blockKeyEvents, true)

        const cleanup = () => {
          render(null, container)
          container.remove()
          document.body.classList.remove('alert-open')
          window.removeEventListener('keydown', blockKeyEvents, true)
          window.removeEventListener('keyup', blockKeyEvents, true)
        }

        const vnode = createVNode(ConfirmModal, {
          visible: true,
          message: message.replace(/\\n/g, '\n'),
          onConfirm: () => {
            cleanup()
            resolve(true)
          },
          onCancel: () => {
            cleanup()
            resolve(false)
          },
          onClose: () => {
            cleanup()
            resolve(false)
          },
        })

        render(vnode, container)
      })
    }
  },
}
