import { getGuessStatuses } from '../../lib/statuses'
import { MiniCell } from './MiniCell'

type Props = {
  guess: string
  solution: string
  showLetters?: boolean
}

export const MiniCompletedRow = ({ guess, solution, showLetters }: Props) => {
  const statuses = getGuessStatuses(guess, solution)

  return (
    <div className="flex justify-center mb-1">
      {guess.split('').map((letter, i) => (
        <MiniCell
          key={i}
          status={statuses[i]}
          letter={showLetters ? letter : undefined}
        />
      ))}
    </div>
  )
}
