import { GameStats } from '../../lib/stats'
import { Progress } from './Progress'

type Props = {
  gameStats: GameStats
}

export const Histogram = ({ gameStats }: Props) => {
  const { totalGames, winDistribution } = gameStats

  return (
    <div className="columns-1 justify-left m-2 text-sm">
      {winDistribution.map((value, i) => (
        <Progress
          key={i}
          index={i}
          size={95 * (value / totalGames)}
          label={String(value)}
        />
      ))}
    </div>
  )
}
