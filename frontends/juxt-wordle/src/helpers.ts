//@ts-ignore
const formatter = new Intl.ListFormat('en', {
  style: 'long',
  type: 'conjunction',
})

export function notEmpty<TValue>(
  value: TValue | null | undefined
): value is TValue {
  if (value === null || value === undefined) return false
  return true
}

export function dateStr(offset?: number): string {
  const date = new Date()
  const offsetDate = date.setDate(date.getDate() + (offset || 0))
  return new Intl.DateTimeFormat('en-GB').format(offsetDate)
}

export function tomorrow() {
  const date = new Date()
  date.setDate(date.getDate() + 1)
  date.setHours(0, 0, 0, 0)
  return date
}

export function genGameId(username: string) {
  const date = dateStr()
  return username + 'wordleGame' + date
}

export const joinList = (list: string[]) => formatter.format(list)
