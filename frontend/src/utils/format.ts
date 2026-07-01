import dayjs from 'dayjs'

export function formatMoney(value: number | string | undefined): string {
  if (value === undefined || value === null || value === '') return '-'
  const num = typeof value === 'string' ? parseFloat(value) : value
  if (Number.isNaN(num)) return '-'
  return `${num.toFixed(2)} 万`
}

export function formatDate(value: string | Date | undefined): string {
  if (!value) return '-'
  return dayjs(value).format('YYYY-MM-DD')
}

export function formatDateTime(value: string | Date | undefined): string {
  if (!value) return '-'
  return dayjs(value).format('YYYY-MM-DD HH:mm:ss')
}

export function formatWeek(value: string | Date | undefined): string {
  if (!value) return '-'
  return dayjs(value).format('YYYY-[W]WW')
}

export function maskMoney(value: number | string | undefined): string {
  if (value === undefined || value === null || value === '') return '-'
  return '** 万'
}
