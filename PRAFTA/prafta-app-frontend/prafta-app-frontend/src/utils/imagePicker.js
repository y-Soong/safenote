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
      `[ensureJpeg] HEIC->JPEG done: name=${newFile.name}, type=${newFile.type}, size=${newFile.size}`
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
    input.onchange = () => {
      handled = true
      let f = input.files && input.files[0]
      document.body.removeChild(input)

      if (f && (!f.type || f.type === '')) {
        const lower = (f.name || '').toLowerCase()
        if (lower.endsWith('.jpg') || lower.endsWith('.jpeg')) {
          try {
            f = new File([f], f.name, { type: 'image/jpeg', lastModified: Date.now() })
          } catch (err) {
            console.log(`[openChooser] type 보정 실패: ${err && err.message}`)
          }
        }
      }

      if (f) resolve(f)
      else {
        console.log('[openChooser] 파일 선택 안됨')
        reject(new Error('no-file'))
      }
    }

    try {
      input.click()
    } catch (e) {
      console.log(`[openChooser] input.click() 실패: ${e && e.message}`)
      reject(e)
    }

    setTimeout(() => {
      if (!handled && input.files && input.files[0]) {
        let f2 = input.files[0]
        if (f2 && (!f2.type || f2.type === '')) {
          const lower = (f2.name || '').toLowerCase()
          if (lower.endsWith('.jpg') || lower.endsWith('.jpeg')) {
            try {
              f2 = new File([f2], f2.name, { type: 'image/jpeg', lastModified: Date.now() })
            } catch (err) {
              console.log(`[openChooser:delay] type 보정 실패: ${err && err.message}`)
            }
          }
        }
        document.body.removeChild(input)
        console.log(
          `[openChooser] 폴백 감지: name=${f2 && f2.name}, type=${f2 && f2.type}, size=${
            f2 && f2.size
          }`
        )
        resolve(f2)
      }
    }, 180)
  })
}

export async function selectImage(source) {
  try {
    const raw = await openChooser(source)
    console.log(
      `[imagePicker] raw: name=${raw && raw.name}, type=${raw && raw.type}, size=${raw && raw.size}`
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
      }, isObjectURL=${isObjectURL}`
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
