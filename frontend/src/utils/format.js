/** 商品图片在 uploads 目录下，库里只存相对路径 */
export function productImageUrl(path) {
  return path ? '/api/uploads/' + path : ''
}

/** 头像接口返回的路径已经带了 uploads/ 前缀 */
export function avatarImageUrl(path) {
  return path ? '/api/' + path : undefined
}

export function money(value) {
  const n = Number(value)
  return Number.isFinite(n) ? n.toFixed(2) : '--'
}

/** 价签上只写整数部分，小数不是 .00 时才带上 */
export function stickerPrice(value) {
  const n = Number(value)
  if (!Number.isFinite(n)) return '--'
  return Number.isInteger(n) ? String(n) : n.toFixed(2)
}

/** 后端时间是 ISO 格式（2026-09-24T18:43:46），展示时换成空格、去掉秒 */
export function dateTime(value) {
  return value ? String(value).replace('T', ' ').substring(0, 16) : ''
}

export function dateOnly(value) {
  return value ? String(value).substring(0, 10) : ''
}

/** 没有图片时的招牌配色：同一件商品在任何页面都是同一个颜色 */
const TINTS = ['blue', 'orange', 'yellow', 'green', 'pink', 'lilac']

export function tintFor(seed) {
  const s = String(seed ?? '')
  let h = 0
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) >>> 0
  return TINTS[h % TINTS.length]
}
