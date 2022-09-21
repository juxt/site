import { SuggestionProps } from '@tiptap/suggestion'
import { Mention } from '../../data'
import { SuggestionDropdown } from './SuggestionDropdown'

import type { SuggestionDropdownRef } from './SuggestionDropdown'

import './MentionDropdown.scss'
import { forwardRef } from 'react'

type MentionDropdownProps = Pick<SuggestionProps, 'command'> & {
  items: Mention[]
}

const MentionDropdown = forwardRef<SuggestionDropdownRef, MentionDropdownProps>(
  ({ items, command }, ref) => (
    <SuggestionDropdown
      forwardedRef={ref}
      items={items}
      onSelect={command}
      renderItem={({ name, avatar }) => (
        <div className="MentionDropdownItem">
          <img className="avatar" alt="avatar" src={avatar} />
          <span className="name">{name}</span>
        </div>
      )}
    />
  )
)

export { MentionDropdown }
