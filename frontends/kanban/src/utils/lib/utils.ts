import {
  ReactLocation,
  parseSearchWith,
  stringifySearchWith
} from '@tanstack/react-location'
import { stringify, parse } from './jsurl'
import { useState, useEffect } from 'react'
import Resizer from 'react-image-file-resizer'
import { toast } from 'react-toastify'

export function utils(): string {
  return 'utils'
}

export function take<T>(
  input: T[],
  start: number,
  deleteCount = input.length - start
) {
  return input.slice(0, start).concat(input.slice(start + deleteCount))
}

export function mapKeys<T extends object, K extends keyof T>(
  obj: T,
  mapper: (key: K) => K
): { [P in K]: T[P] } {
  return Object.keys(obj).reduce((acc, key) => {
    return { ...acc, [mapper(key as K)]: obj[key as K] }
  }, {} as { [P in K]: T[P] })
}

export function isDefined<T>(argument: T | undefined): argument is T {
  return argument !== undefined
}

export function notEmpty<TValue>(
  value: TValue | null | undefined
): value is TValue {
  if (value === null || value === undefined) return false
  return true
}

export function distinctBy<T>(array: Array<T>, propertyName: keyof T) {
  return array.filter(
    (e, i) => array.findIndex((a) => a[propertyName] === e[propertyName]) === i
  )
}

export function indexById<T extends { id: string }>(
  array: Array<T>
): { [id: string]: T } {
  const initialValue = {}
  return array.reduce((obj, item) => {
    return {
      ...obj,
      [item.id]: item
    }
  }, initialValue)
}

export function compressImage(file: File): Promise<string> {
  return new Promise((resolve) => {
    Resizer.imageFileResizer(
      file,
      200,
      200,
      'JPEG',
      60,
      0,
      (uri) => {
        resolve(uri.toString())
      },
      'base64'
    )
  })
}

export function base64FileToImage(file: File) {
  return new Promise((resolve, reject) => {
    if (!file.type.startsWith('image/')) {
      reject(new Error('File is not an image'))
    }
    const reader = new FileReader()
    reader.readAsDataURL(file)
    reader.onload = () => {
      resolve(reader.result)
    }

    reader.onerror = () => {
      toast.error('Error reading file')
      reject(reader.error)
    }
  })
}

export function fileToString(file: File): Promise<string> {
  if (file.type.startsWith('image/')) {
    return compressImage(file)
  }
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.readAsDataURL(file)
    reader.onload = () => {
      resolve(reader.result?.toString() ?? '')
    }

    reader.onerror = () => {
      toast.error('Error reading file')
      reject(reader.error)
    }
  })
}

export const base64toBlob = (data: string) => {
  // Cut the prefix `data:application/pdf;base64` from the raw base 64 (some pdfs don't have this though)
  const base64Raw = data.startsWith('data:application')
    ? data.split(',')[1]
    : data
  const bytes = atob(base64Raw)
  let { length } = bytes
  const out = new Uint8Array(length)

  // eslint-disable-next-line no-plusplus
  while (length--) {
    out[length] = bytes.charCodeAt(length)
  }

  return new Blob([out], { type: 'application/pdf' })
}

export function downloadFile(blob: Blob, filename: string) {
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = filename
  link.click()
  link.remove()
  // in case the Blob uses a lot of memory
  setTimeout(() => URL.revokeObjectURL(link.href), 7000)
}

export function getMobileDetect(userAgent: string) {
  const isAndroid = (): boolean => Boolean(userAgent.match(/Android/i))
  const isIos = (): boolean => Boolean(userAgent.match(/iPhone|iPad|iPod/i))
  const isOpera = (): boolean => Boolean(userAgent.match(/Opera Mini/i))
  const isWindows = (): boolean => Boolean(userAgent.match(/IEMobile/i))
  const isSSR = (): boolean => Boolean(userAgent.match(/SSR/i))

  const isMobile = (): boolean =>
    Boolean(isAndroid() || isIos() || isOpera() || isWindows())
  const isDesktop = (): boolean => Boolean(!isMobile() && !isSSR())
  return {
    isMobile,
    isDesktop,
    isAndroid,
    isIos,
    isSSR
  }
}

type Size = {
  width: number | undefined
  height: number | undefined
}

export function useWindowSize(): Size {
  // Initialize state with undefined width/height so server and client renders match
  // Learn more here: https://joshwcomeau.com/react/the-perils-of-rehydration/
  const [windowSize, setWindowSize] = useState<Size>({
    width: undefined,
    height: undefined
  })
  useEffect(() => {
    // Handler to call on window resize
    function handleResize() {
      // Set window width/height to state
      setWindowSize({
        width: window.innerWidth,
        height: window.innerHeight
      })
    }
    // Add event listener
    window.addEventListener('resize', handleResize)
    // Call handler right away so state gets updated with initial window size
    handleResize()
    // Remove event listener on cleanup
    return () => window.removeEventListener('resize', handleResize)
  }, []) // Empty array ensures that effect is only run on mount
  return windowSize
}

export function formatDate(date: Date) {
  return new Intl.DateTimeFormat('en-GB', {
    dateStyle: 'full',
    timeStyle: 'long'
  }).format(date)
}

export function NewLocation() {
  return new ReactLocation({
    parseSearch: parseSearchWith(parse),
    stringifySearch: stringifySearchWith(stringify)
  })
}

export function encodeToBinary(str: string): string {
  return btoa(
    encodeURIComponent(str).replace(/%([0-9A-F]{2})/g, function (match, p1) {
      return String.fromCharCode(parseInt(p1, 16))
    })
  )
}
export function decodeFromBinary(str: string): string {
  return decodeURIComponent(
    Array.prototype.map
      .call(atob(str), function (c) {
        return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)
      })
      .join('')
  )
}
