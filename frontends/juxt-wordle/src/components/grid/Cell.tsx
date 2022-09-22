import { CharStatus } from '../../lib/statuses'
import classnames from 'classnames'
import { loadColourBlindMode } from '../../lib/localStorage'

type Props = {
  value?: string
  status?: CharStatus
}

export const cellColours = (colourBlindMode: boolean, status?: CharStatus) => ({
  'bg-white border-slate-200': !status,
  'bg-green-500 text-white border-green-500': status === 'correct',
  'bg-yellow-500 text-white border-yellow-500': status === 'present',
  'bg-orange-500 border-orange-500': colourBlindMode && status === 'correct',
  'bg-blue-300 border-blue-300': colourBlindMode && status === 'present',
})

export const Cell = ({ value, status }: Props) => {
  const colourBlindMode = loadColourBlindMode()
  const colours = cellColours(colourBlindMode, status)
  const classes = classnames(
    'w-14 h-14 border-solid border-2 flex items-center justify-center mx-0.5 text-lg font-bold rounded',
    { 'bg-slate-400 text-white border-slate-400': status === 'absent' },
    colours
  )

  return (
    <>
      <div className={classes}>{value}</div>
    </>
  )
}
