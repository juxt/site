import { useEffect } from 'react'
import { atom, useAtom } from 'jotai'
export const dirtyAtom = atom(false)

export function useDirty({
  isDirty,
  message = 'are you sure?'
}: {
  isDirty: boolean
  message?: string
}) {
  const [dirty, setDirty] = useAtom(dirtyAtom)

  useEffect(() => {
    setDirty(isDirty)
    if (isDirty) {
      window.onbeforeunload = () => message
    }
    return () => {
      window.onbeforeunload = null
    }
  }, [isDirty, message, setDirty])
  return [dirty, setDirty]
}

export const searchAtom = atom('')

export function useGlobalSearch(): [string, (value: string) => void] {
  const [searchValue, setSearchValue] = useAtom(searchAtom)

  useEffect(() => {
    setSearchValue(searchValue)
  }, [searchValue, setSearchValue])

  return [searchValue, setSearchValue]
}
