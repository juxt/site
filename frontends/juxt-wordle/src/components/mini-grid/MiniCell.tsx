import { CharStatus } from '../../lib/statuses'
import classnames from 'classnames'
import { loadColourBlindMode } from '../../lib/localStorage'
import { cellColours } from '../grid/Cell'

type Props = {
  status: CharStatus
  letter?: string
}

export const MiniCell = ({ status, letter }: Props) => {
  const colourBlindMode = loadColourBlindMode()
  const colours = cellColours(colourBlindMode, status)
  const classes = classnames(
    'w-10 h-10 border-solid text-white border-2 border-slate-200 flex items-center justify-center mx-0.5 text-lg font-bold rounded',
    colours,
    {
      'bg-white border-slate-200': !status,
      'border-none': letter,
      'bg-slate-400': letter && status === 'absent',
    }
  )

  return (
    <>
      <div className={classes}>{letter}</div>
    </>
  )
}
