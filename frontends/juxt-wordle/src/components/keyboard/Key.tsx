import { ReactNode } from 'react'
import classnames from 'classnames'
import { KeyValue } from '../../lib/keyboard'
import { CharStatus } from '../../lib/statuses'
import { loadColourBlindMode } from '../../lib/localStorage'

type Props = {
  children?: ReactNode
  value: KeyValue
  width?: number
  status?: CharStatus
  onClick: (value: KeyValue) => void
}

export const Key = ({
  children,
  status,
  width = 40,
  value,
  onClick,
}: Props) => {
  const colourBlindMode = loadColourBlindMode()
  const classes = classnames(
    'flex items-center justify-center rounded mx-0.5 text-xs font-bold cursor-pointer',
    {
      'bg-slate-200 hover:bg-slate-300 active:bg-slate-400': !status,
      'bg-slate-400 text-white': status === 'absent',
      'bg-green-500 hover:bg-green-600 active:bg-green-700 text-white':
        status === 'correct',
      'bg-yellow-500 hover:bg-yellow-600 active:bg-yellow-700 text-white':
        status === 'present',
      'bg-orange-500 hover:bg-orange-600 active:bg-orange-700 text-white':
        colourBlindMode && status === 'correct',
      'bg-blue-300 hover:bg-blue-400 active:bg-blue-500 text-white':
        colourBlindMode && status === 'present',
    }
  )

  return (
    <div
      style={{ width: `${width}px`, height: '58px' }}
      className={classes}
      onClick={() => onClick(value)}
    >
      {children || value}
    </div>
  )
}
