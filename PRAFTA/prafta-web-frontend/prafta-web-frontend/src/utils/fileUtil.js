/**
 * Browser File/Blob → raw Base64 string (no `data:*;base64,` prefix).
 * JSON request bodies cannot carry raw binary; encode as Base64 (or use multipart).
 *
 * @param {File|Blob} file
 * @returns {Promise<string>}
 */
export function readFileAsBase64(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      const r = String(reader.result);
      const comma = r.indexOf(",");
      resolve(comma >= 0 ? r.slice(comma + 1) : r);
    };
    reader.onerror = () => reject(reader.error);
    reader.readAsDataURL(file);
  });
}
