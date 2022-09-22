import { MiniCompletedRow } from './MiniCompletedRow'

type Props = {
  guesses: string[]
  solution: string
  showLetters?: boolean
}

export const MiniGrid = ({ guesses, solution, showLetters }: Props) => {
  return (
    <div className="pb-6">
      {guesses.map((guess, i) => (
        <MiniCompletedRow
          key={i}
          guess={guess}
          solution={solution}
          showLetters={showLetters}
        />
      ))}
    </div>
  )
}
