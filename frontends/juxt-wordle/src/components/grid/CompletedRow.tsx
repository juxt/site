import { getGuessStatuses } from '../../lib/statuses'
import { Cell } from './Cell'

type Props = {
  guess: string
  solution: string
}

export const CompletedRow = ({ guess, solution }: Props) => {
  const statuses = getGuessStatuses(guess, solution)

  return (
    <div className="flex justify-center mb-1">
      {guess.split('').map((letter, i) => (
        <Cell key={i} value={letter} status={statuses[i]} />
      ))}
    </div>
  )
}
