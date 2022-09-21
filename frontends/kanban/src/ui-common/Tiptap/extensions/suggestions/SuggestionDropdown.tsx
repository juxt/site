import classNames from 'classnames'
import { SuggestionKeyDownProps } from '@tiptap/suggestion'
import { OverlayScrollbarsComponent } from 'overlayscrollbars-react'

import 'overlayscrollbars/css/OverlayScrollbars.css'

import './SuggestionDropdown.scss'
import {
  ForwardedRef,
  useEffect,
  useImperativeHandle,
  useRef,
  useState
} from 'react'

type SuggestionDropdownRef = {
  onKeyDown: (props: SuggestionKeyDownProps) => boolean
}

type SuggestionDropdownProps<TItem> = {
  forwardedRef: ForwardedRef<SuggestionDropdownRef>
  items: TItem[]
  onSelect: (item: TItem) => void
  renderItem: (item: TItem) => JSX.Element
}

function SuggestionDropdown<TItem>({
  forwardedRef,
  items,
  onSelect,
  renderItem
}: SuggestionDropdownProps<TItem>) {
  const [selectedIndex, setSelectedIndex] = useState(0)

  const overlayScrollbarsRef = useRef<OverlayScrollbarsComponent>(null)
  const selectedItemRef = useRef<HTMLLIElement>(null)

  function selectItem(index: number) {
    const item = items[index]

    if (item) {
      onSelect(item)
    }
  }

  useEffect(() => {
    overlayScrollbarsRef.current?.osInstance().scroll({
      el: selectedItemRef.current,
      scroll: {
        y: 'ifneeded',
        x: 'never'
      }
    })
  }, [selectedIndex])

  useImperativeHandle(forwardedRef, () => ({
    onKeyDown: ({ event }) => {
      if (event.key === 'ArrowUp') {
        setSelectedIndex((selectedIndex + items.length - 1) % items.length)
        return true
      }

      if (event.key === 'ArrowDown') {
        setSelectedIndex((selectedIndex + 1) % items.length)
        return true
      }

      if (event.key === 'Enter') {
        selectItem(selectedIndex)
        return true
      }

      return false
    }
  }))

  if (items.length === 0) {
    return null
  }

  return (
    <OverlayScrollbarsComponent
      className="SuggestionDropdown"
      ref={overlayScrollbarsRef}
    >
      <ul>
        {items.map((item, index) => (
          <button
            type="button"
            className={classNames({ selected: index === selectedIndex })}
            // eslint-disable-next-line react/no-array-index-key
            key={index}
            onClick={() => selectItem(index)}
          >
            <li ref={index === selectedIndex ? selectedItemRef : null}>
              {renderItem(item)}
            </li>
          </button>
        ))}
      </ul>
    </OverlayScrollbarsComponent>
  )
}

export { SuggestionDropdown }

export type { SuggestionDropdownRef }
