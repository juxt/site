import prettyMilliseconds from 'pretty-ms'
import { GameStats } from '../../lib/stats'

type Props = {
  gameStats: GameStats
}

const StatItem = ({
  label,
  value,
}: {
  label: string
  value: string | number
}) => {
  return (
    <div className="items-center justify-center m-1 w-1/4">
      <div className="text-3xl font-bold">{value}</div>
      <div className="text-xs">{label}</div>
    </div>
  )
}

export const StatBar = ({ gameStats }: Props) => {
  return (
    <div className="flex justify-center my-2">
      <StatItem label="Total tries" value={gameStats.totalGames} />
      <StatItem label="Success rate" value={`${gameStats.successRate}%`} />
      <StatItem label="Current streak" value={gameStats.currentStreak} />
      <StatItem
        label="Average Time Taken"
        value={prettyMilliseconds(gameStats.averageTimeTakenMillis || 0, {
          compact: true,
        })}
      />
    </div>
  )
}
