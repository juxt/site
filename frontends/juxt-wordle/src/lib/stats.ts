import { GameFieldsFragment } from '../generated/graphql'

export type GameStats = {
  username: string
  currentStreak: number
  gamesFailed: number
  totalTimeTakenMillis: number
  averageTimeTakenMillis: number
  lastPlayed: string
  successRate: number
  totalGames: number
  winDistribution: number[]
  averageGuesses: number
}

const getSuccessRate = ({
  totalGames,
  gamesFailed,
}: {
  totalGames: number
  gamesFailed: number
}) => {
  return Math.round(
    (100 * (totalGames - gamesFailed)) / Math.max(totalGames, 1)
  )
}

type Game = GameFieldsFragment & {
  won?: boolean
  lost?: boolean
}

type Games = Array<Game>

export function statsForGames(gamesInput: Games): GameStats {
  const finishedGames = gamesInput.filter(({ finished }) => finished === true)
  if (!finishedGames?.[0]?.username) {
    return {
      username: '',
      currentStreak: 0,
      averageGuesses: 0,
      gamesFailed: 0,
      lastPlayed: '',
      totalTimeTakenMillis: 0,
      averageTimeTakenMillis: 0,
      successRate: 0,
      totalGames: 0,
      winDistribution: [],
    }
  }
  const games = finishedGames.map((game) => {
    const won = game.guesses?.includes(game.solution)
    const lost = game.guesses?.length === 6
    if (won) {
      return {
        ...game,
        won,
      }
    } else {
      return {
        ...game,
        lost,
      }
    }
  })

  const totalGames = games.length
  const totalTimeTakenMillis = games.reduce(
    (acc, game) => acc + (game?.timeTakenMillis ?? 0),
    0
  )
  const averageTimeTakenMillis = totalTimeTakenMillis / totalGames
  const gamesFailed = games.filter((game) => game?.lost).length
  const successRate = getSuccessRate({
    totalGames,
    gamesFailed,
  })
  const winDistribution = games.reduce(
    (acc, game) => {
      if (game?.guesses && game.guesses.length > 0) {
        const guesses = game.guesses.length - 1
        acc[guesses] = (acc[guesses] ?? 0) + 1
      }
      return acc
    },
    [0, 0, 0, 0, 0, 0]
  )
  const gamesDesc = games
    .sort((a, b) => (a.date < b.date ? 1 : -1))
    .filter((game) => game?.won || game?.lost)

  const currentStreak = gamesDesc.reduce((acc, game) => {
    if (game.guesses?.includes(game.solution)) {
      acc += 1
    } else {
      return acc
    }
    return acc
  }, 0)

  const averageGuesses =
    gamesDesc.reduce((acc, game) => {
      if (game?.guesses && game.guesses?.length > 0) {
        if (game?.won) {
          acc += game.guesses.length - 1
        } else {
          acc += game.guesses.length
        }
      }
      return acc
    }, 0) / totalGames

  return {
    username: games[0].username,
    lastPlayed: gamesDesc?.[0]?.date ?? '',
    averageGuesses,
    totalGames,
    totalTimeTakenMillis,
    averageTimeTakenMillis,
    gamesFailed,
    successRate,
    winDistribution,
    currentStreak,
  }
}
