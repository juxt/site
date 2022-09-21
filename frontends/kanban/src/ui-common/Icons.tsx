import { ChevronDownIcon } from '@heroicons/react/solid'
import classNames from 'classnames'

export type IconProps = React.HTMLAttributes<HTMLOrSVGElement> & {
  fill?: string
  stroke?: string
}

export function SortIcon({ className }: IconProps) {
  return (
    <svg
      className={className}
      stroke="currentColor"
      fill="currentColor"
      strokeWidth="0"
      viewBox="0 0 320 512"
      height="1em"
      width="1em"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path d="M41 288h238c21.4 0 32.1 25.9 17 41L177 448c-9.4 9.4-24.6 9.4-33.9 0L24 329c-15.1-15.1-4.4-41 17-41zm255-105L177 64c-9.4-9.4-24.6-9.4-33.9 0L24 183c-15.1 15.1-4.4 41 17 41h238c21.4 0 32.1-25.9 17-41z" />
    </svg>
  )
}

export function SortUpIcon({ className }: IconProps) {
  return (
    <svg
      className={className}
      stroke="currentColor"
      fill="currentColor"
      strokeWidth="0"
      viewBox="0 0 320 512"
      height="1em"
      width="1em"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path d="M279 224H41c-21.4 0-32.1-25.9-17-41L143 64c9.4-9.4 24.6-9.4 33.9 0l119 119c15.2 15.1 4.5 41-16.9 41z" />
    </svg>
  )
}

export function SortDownIcon({ className }: IconProps) {
  return (
    <svg
      className={className}
      stroke="currentColor"
      fill="currentColor"
      strokeWidth="0"
      viewBox="0 0 320 512"
      height="1em"
      width="1em"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path d="M41 288h238c21.4 0 32.1 25.9 17 41L177 448c-9.4 9.4-24.6 9.4-33.9 0L24 329c-15.1-15.1-4.4-41 17-41z" />
    </svg>
  )
}

export function MoveInactiveIcon(props: IconProps) {
  return (
    <svg
      {...props}
      viewBox="0 0 20 20"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path d="M10 4H16V10" stroke="#A78BFA" strokeWidth="2" />
      <path d="M16 4L8 12" stroke="#A78BFA" strokeWidth="2" />
      <path d="M8 6H4V16H14V12" stroke="#A78BFA" strokeWidth="2" />
    </svg>
  )
}

export function MoveActiveIcon(props: IconProps) {
  return (
    <svg
      {...props}
      viewBox="0 0 20 20"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path d="M10 4H16V10" stroke="#C4B5FD" strokeWidth="2" />
      <path d="M16 4L8 12" stroke="#C4B5FD" strokeWidth="2" />
      <path d="M8 6H4V16H14V12" stroke="#C4B5FD" strokeWidth="2" />
    </svg>
  )
}

export function DeleteInactiveIcon(props: IconProps) {
  return (
    <svg
      {...props}
      viewBox="0 0 20 20"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <rect
        x="5"
        y="6"
        width="10"
        height="10"
        fill="#EDE9FE"
        stroke="#A78BFA"
        strokeWidth="2"
      />
      <path d="M3 6H17" stroke="#A78BFA" strokeWidth="2" />
      <path d="M8 6V4H12V6" stroke="#A78BFA" strokeWidth="2" />
    </svg>
  )
}

export function DeleteActiveIcon({ fill, stroke, ...props }: IconProps) {
  return (
    <svg
      {...props}
      viewBox="0 0 20 20"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <rect
        x="5"
        y="6"
        width="10"
        height="10"
        fill={fill ? fill : '#8B5CF6'}
        stroke={stroke ? stroke : '#C4B5FD'}
        strokeWidth="2"
      />
      <path d="M3 6H17" stroke={stroke ? stroke : '#C4B5FD'} strokeWidth="2" />
      <path
        d="M8 6V4H12V6"
        stroke={stroke ? stroke : '#C4B5FD'}
        strokeWidth="2"
      />
    </svg>
  )
}

export function ArchiveInactiveIcon(props: IconProps) {
  return (
    <svg
      {...props}
      viewBox="0 0 20 20"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <rect
        x="5"
        y="8"
        width="10"
        height="8"
        fill="#EDE9FE"
        stroke="#A78BFA"
        strokeWidth="2"
      />
      <rect
        x="4"
        y="4"
        width="12"
        height="4"
        fill="#EDE9FE"
        stroke="#A78BFA"
        strokeWidth="2"
      />
      <path d="M8 12H12" stroke="#A78BFA" strokeWidth="2" />
    </svg>
  )
}

export function ArchiveActiveIcon(props: IconProps) {
  return (
    <svg
      {...props}
      viewBox="0 0 20 20"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <rect
        x="5"
        y="8"
        width="10"
        height="8"
        fill="#8B5CF6"
        stroke="#C4B5FD"
        strokeWidth="2"
      />
      <rect
        x="4"
        y="4"
        width="12"
        height="4"
        fill="#8B5CF6"
        stroke="#C4B5FD"
        strokeWidth="2"
      />
      <path d="M8 12H12" stroke="#A78BFA" strokeWidth="2" />
    </svg>
  )
}

export function CloseIcon(open: boolean) {
  return (
    <ChevronDownIcon
      className={classNames(
        'w-4 h-4',
        open ? 'transform rotate-180 text-primary-500' : ''
      )}
    />
  )
}

export function ExpandIcon(props: IconProps) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      className="h-6 w-6"
      fill="none"
      viewBox="0 0 24 24"
      stroke="currentColor"
      {...props}
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={2}
        d="M4 8V4m0 0h4M4 4l5 5m11-1V4m0 0h-4m4 0l-5 5M4 16v4m0 0h4m-4 0l5-5m11 5l-5-5m5 5v-4m0 4h-4"
      />
    </svg>
  )
}

export function DoubleThumbDown(props: IconProps) {
  return (
    <svg {...props} viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
      <path d="M10.601 18.015l.67-3.315H5.75c-.963 0-1.75-.81-1.75-1.8l.009-.072L4 12.819V11.1c0-.234.044-.45.122-.657L5.556 7H3.75a1.49 1.49 0 00-1.38.915L.105 13.202A1.482 1.482 0 000 13.75v1.433l.007.007-.007.06c0 .825.675 1.5 1.5 1.5h4.732l-.712 3.428-.023.24c0 .307.128.592.33.795l.795.787 3.98-3.985zM24 3h-3v9h3V3zM7 11.25c0 .825.675 1.5 1.5 1.5h4.732l-.712 3.428-.022.24c0 .307.127.592.33.795l.794.787 4.935-4.942c.278-.27.443-.645.443-1.058V4.5c0-.825-.675-1.5-1.5-1.5h-6.75a1.49 1.49 0 00-1.38.915L7.105 9.203A1.482 1.482 0 007 9.75v1.433l.007.007-.007.06z"></path>
    </svg>
  )
}

export function ThumbDown(props: IconProps) {
  return (
    <svg
      {...props}
      viewBox="0 0 16 16"
      id="rating-thumb-down"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path d="M6.667 14l-.307-.287c-.24-.22-.433-.47-.587-.746a.567.567 0 01-.053-.403L6.5 10H3c-1 0-1-1-1-1.5C2 7 3 5 3.5 4c.441-.882.908-.986 1.333-.998L9.5 3c.86 0 1.5.718 1.5 1.5v4c0 1-.5 1.375-.5 1.375L6.667 14zM13 10a1 1 0 01-1-1V4a1 1 0 011-1h1v7h-1z"></path>
    </svg>
  )
}

export function ThumbUp(props: IconProps) {
  return (
    <svg
      {...props}
      viewBox="0 0 16 16"
      id="rating-thumb-up"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path d="M6.667 2l-.307.287c-.24.22-.433.47-.587.746a.567.567 0 00-.053.403L6.5 6H3C2 6 2 7 2 7.5 2 9 3 11 3.5 12s1.033 1 1.5 1h4.5c.86 0 1.5-.718 1.5-1.5v-4c0-1-.5-1.375-.5-1.375L6.667 2zM13 6a1 1 0 00-1 1v5a1 1 0 001 1h1V6h-1z"></path>
    </svg>
  )
}

export function DoubleThumbUp(props: IconProps) {
  return (
    <svg
      {...props}
      viewBox="0 0 16 16"
      id="rating-double-thumb-up"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path d="M8.667 3l-.307.287c-.24.22-.433.47-.587.746a.567.567 0 00-.053.403L8.5 7H5C4 7 4 8 4 8.5 4 10 5 12 5.5 13s1.033 1 1.5 1h4.5c.86 0 1.5-.718 1.5-1.5v-4c0-1-.5-1.375-.5-1.375L8.667 3zM15 7a1 1 0 00-1 1v5a1 1 0 001 1h1V7h-1z"></path>
      <path d="M6.584 4.39L7 6.38H3.571c-.598 0-1.087.486-1.087 1.08l.006.043-.006.006V8.54c0 .14.027.27.076.394L3.45 11H2.33a.926.926 0 01-.857-.549L.065 7.278A.863.863 0 010 6.95v-.86l.005-.004L0 6.05c0-.495.42-.9.932-.9H3.87l-.443-2.057-.014-.143c0-.185.08-.356.205-.478L4.113 2l2.47 2.39z"></path>
    </svg>
  )
}

export function ThumbUpDown(props: IconProps) {
  return (
    <svg
      {...props}
      viewBox="0 0 24 24"
      id="rating-thumbs-up-down"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path d="M12 6c0-.55-.45-1-1-1H5.82l.66-3.18.02-.23c0-.31-.13-.59-.33-.8L5.38 0 .44 4.94C.17 5.21 0 5.59 0 6v6.5c0 .83.67 1.5 1.5 1.5h6.75c.62 0 1.15-.38 1.38-.91l2.26-5.29c.07-.17.11-.36.11-.55V6zm10.5 4h-6.75c-.62 0-1.15.38-1.38.91l-2.26 5.29c-.07.17-.11.36-.11.55V18c0 .55.45 1 1 1h5.18l-.66 3.18-.02.24c0 .31.13.59.33.8l.79.78 4.94-4.94c.27-.27.44-.65.44-1.06v-6.5c0-.83-.67-1.5-1.5-1.5z"></path>
    </svg>
  )
}

export function IconForScore({
  score,
  fill,
  withLabel,
  size = 'md'
}: {
  score?: number
  fill?: string
  withLabel?: boolean
  size?: 'sm' | 'md' | 'lg'
}) {
  const defaultProps = {
    className: classNames(
      'mr-2 inline-block',
      size === 'sm' && 'w-4 h-4',
      size === 'md' && 'w-6 h-6',
      size === 'lg' && 'w-8 h-8'
    )
  }
  const data = [
    {
      score: 1,
      icon: ThumbDown,
      defaultFill: 'red',
      label: 'No'
    },
    {
      score: 2,
      icon: ThumbUpDown,
      defaultFill: 'gray',
      label: 'Not sure'
    },
    {
      score: 3,
      icon: ThumbUp,
      defaultFill: 'green',
      label: 'Yes'
    },
    {
      score: 4,
      icon: DoubleThumbUp,
      defaultFill: 'green',
      label: 'Strong Yes'
    }
  ]
  const item = data.find((i) => i.score === score)
  return item ? (
    <>
      <item.icon
        {...defaultProps}
        title={item.label}
        fill={item?.defaultFill || fill}
      />

      {withLabel && <span className="text-xs">{item.label}</span>}
    </>
  ) : null
}
