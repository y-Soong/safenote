// /src/utils/imagePicker.js

function isHeicLike(file) {
  const name = (file && file.name ? file.name : '').toLowerCase()
  const type = (file && file.type ? file.type : '').toLowerCase()
  return (
    name.endsWith('.heic') ||
    name.endsWith('.heif') ||
    type.includes('heic') ||
    type.includes('heif')
  )
}

async function decodeToBitmapOrImage(file) {
  if ('createImageBitmap' in window) {
    try {
      return await createImageBitmap(file)
    } catch (e) {
      console.log(`[decodeToBitmapOrImage] createImageBitmap 실패: ${e && e.message}`)
    }
  }
  return await new Promise((resolve, reject) => {
    const img = new Image()
    img.onload = () => {
      try {
        URL.revokeObjectURL(img.src)
      } catch (err) {
        console.log(`[decode] revoke 실패: ${err && err.message}`)
      }
      resolve(img)
    }
    img.onerror = (err) => {
      try {
        URL.revokeObjectURL(img.src)
      } catch (err2) {
        console.log(`[decode] revoke 실패: ${err2 && err2.message}`)
      }
      console.log(`[decodeToBitmapOrImage] 이미지 로드 실패: ${err && err.message}`)
      reject(err)
    }
    img.crossOrigin = 'anonymous'
    try {
      img.src = URL.createObjectURL(file)
    } catch (e) {
      console.log(`[decodeToBitmapOrImage] objectURL 생성 실패: ${e && e.message}`)
      reject(e)
    }
  })
}

export async function ensureJpeg(file, quality = 0.92) {
  try {
    if (!isHeicLike(file)) return file
    console.log(`[ensureJpeg] HEIC->JPEG start: name=${file && file.name}`)
    const bmpOrImg = await decodeToBitmapOrImage(file)
    const w = bmpOrImg.width,
      h = bmpOrImg.height
    const canvas = document.createElement('canvas')
    canvas.width = w
    canvas.height = h
    const ctx = canvas.getContext('2d')
    ctx.drawImage(bmpOrImg, 0, 0, w, h)
    const blob = await new Promise((res) => canvas.toBlob(res, 'image/jpeg', quality))
    const base = (file.name || 'photo').replace(/\.[^.]+$/, '')
    const newFile = new File([blob], base + '.jpg', {
      type: 'image/jpeg',
      lastModified: Date.now(),
    })
    console.log(
      `[ensureJpeg] HEIC->JPEG done: name=${newFile.name}, type=${newFile.type}, size=${newFile.size}`,
    )
    return newFile
  } catch (e) {
    console.log(`[ensureJpeg] 변환 실패: ${e && e.message}`)
    return file
  }
}

export async function toPreviewUrl(file) {
  try {
    const reader = new FileReader()
    const p = new Promise((resolve, reject) => {
      reader.onload = () => resolve(reader.result)
      reader.onerror = reject
    })
    reader.readAsDataURL(file)
    const url = await p
    console.log('[toPreviewUrl] dataURL ok')
    return { url, isObjectURL: false }
  } catch (e) {
    console.log(`[toPreviewUrl] dataURL fail -> blob fallback: ${e && e.message}`)
    try {
      const url = URL.createObjectURL(file)
      return { url, isObjectURL: true }
    } catch (err) {
      console.log(`[toPreviewUrl] blob URL 생성 실패: ${err && err.message}`)
      throw err
    }
  }
}

function openChooser(source) {
  return new Promise((resolve, reject) => {
    const input = document.createElement('input')
    input.type = 'file'
    if (source === 'camera') {
      input.accept = 'image/jpeg' // ✅ 카메라 JPEG 유도
      input.setAttribute('capture', 'environment')
    } else {
      input.accept = 'image/*'
    }
    Object.assign(input.style, {
      position: 'fixed',
      left: '-9999px',
      width: '1px',
      height: '1px',
      opacity: '0',
      pointerEvents: 'none',
    })
    document.body.appendChild(input)
    input.value = ''

    let handled = false
    // 취소 폴백용 window focus 리스너 핸들
    let focusHandler = null

    // 모든 종결 경로를 단일 가드로 통일 (이중 resolve/reject 방지 + 리소스 정리)
    const cleanup = () => {
      if (focusHandler) {
        window.removeEventListener('focus', focusHandler)
        focusHandler = null
      }
      if (input.parentNode) {
        try {
          document.body.removeChild(input)
        } catch (err) {
          console.log(`[openChooser] input 제거 실패: ${err && err.message}`)
        }
      }
    }
    const finishWith = (file) => {
      if (handled) return
      handled = true
      cleanup()
      resolve(file)
    }
    const finishCancel = (reasonMsg) => {
      if (handled) return
      handled = true
      cleanup()
      console.log(`[openChooser] 선택 취소: ${reasonMsg}`)
      reject(new Error('no-file'))
    }

    // 빈 타입(image/jpeg 미설정) 보정
    const normalizeJpegType = (f) => {
      if (f && (!f.type || f.type === '')) {
        const lower = (f.name || '').toLowerCase()
        if (lower.endsWith('.jpg') || lower.endsWith('.jpeg')) {
          try {
            return new File([f], f.name, { type: 'image/jpeg', lastModified: Date.now() })
          } catch (err) {
            console.log(`[openChooser] type 보정 실패: ${err && err.message}`)
          }
        }
      }
      return f
    }

    // 정상 선택 경로 (기존 동작 보존)
    input.onchange = () => {
      const f = normalizeJpegType(input.files && input.files[0])
      if (f) finishWith(f)
      else finishCancel('onchange 빈 파일')
    }

    // 최신 WebView: 파일 선택 취소 시 cancel 이벤트 발화
    input.oncancel = () => {
      // change 가 먼저 들어온 경우엔 handled 가드로 무시됨
      finishCancel('cancel 이벤트')
    }

    try {
      input.click()
    } catch (e) {
      if (!handled) {
        handled = true
        cleanup()
        console.log(`[openChooser] input.click() 실패: ${e && e.message}`)
        reject(e)
      }
      return
    }

    // 파일이 늦게 도착하는 기기 보정 (기존 폴백 유지, 가드만 통일)
    setTimeout(() => {
      if (!handled && input.files && input.files[0]) {
        const f2 = normalizeJpegType(input.files[0])
        console.log(
          `[openChooser] 폴백 감지: name=${f2 && f2.name}, type=${f2 && f2.type}, size=${
            f2 && f2.size
          }`,
        )
        finishWith(f2)
      }
    }, 180)

    // 구형 호환: file chooser/카메라 닫히면 webview 가 다시 focus 를 받음.
    // focus 복귀 후 grace(파일 늦은 도착 보정 180ms 보다 충분히 큰 400ms) 내에
    // change/cancel 모두 미발화하고 input.files 가 비어 있으면 취소로 간주.
    focusHandler = () => {
      if (handled) return
      setTimeout(() => {
        if (handled) return
        if (!(input.files && input.files[0])) {
          finishCancel('focus 복귀 + 파일 없음')
        }
      }, 400)
    }
    // input.click() 직후 즉시 발생하는 focus(웹뷰 자체)로 오발동하지 않도록
    // 약간의 지연 후 1회성 리스너 등록.
    setTimeout(() => {
      if (handled) return
      window.addEventListener('focus', focusHandler, { once: true })
    }, 150)
  })
}

export async function selectImage(source) {
  try {
    const raw = await openChooser(source)
    console.log(
      `[imagePicker] raw: name=${raw && raw.name}, type=${raw && raw.type}, size=${raw && raw.size}`,
    )

    let file = raw
    if (!file.type && (file.name || '').toLowerCase().match(/\.(jpg|jpeg)$/)) {
      try {
        file = new File([file], file.name, { type: 'image/jpeg', lastModified: Date.now() })
      } catch (err) {
        console.log(`[selectImage] type 보정 실패: ${err && err.message}`)
      }
    }

    file = await ensureJpeg(file)
    const { url, isObjectURL } = await toPreviewUrl(file)

    console.log(
      `[selectImage] return: name=${file && file.name}, type=${
        file && file.type
      }, isObjectURL=${isObjectURL}`,
    )
    return { file, previewUrl: url, isObjectURL }
  } catch (e) {
    console.log(`[selectImage] fail: ${e && e.message}`)
    throw e
  }
}

export function revokePreview(url) {
  try {
    if (typeof url === 'string' && url.startsWith('blob:')) {
      URL.revokeObjectURL(url)
      console.log(`[revokePreview] blob URL 정리: ${url}`)
    }
  } catch (e) {
    console.log(`[revokePreview] revoke 실패: ${e && e.message}`)
  }
}
