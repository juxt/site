/* eslint-disable react-hooks/exhaustive-deps */
import Countdown from 'react-countdown'
import {
  CogIcon,
  InformationCircleIcon,
  UserGroupIcon,
} from '@heroicons/react/outline'
import { ChartBarIcon } from '@heroicons/react/outline'
import {
  useGameForIdQuery,
  useGameHistoryQuery,
  useSaveGameMutation,
} from './generated/graphql'
import { useState, useEffect } from 'react'
import { Alert } from './components/alerts/Alert'
import { Grid } from './components/grid/Grid'
import { Keyboard } from './components/keyboard/Keyboard'
import { AboutModal } from './components/modals/AboutModal'
import { InfoModal } from './components/modals/InfoModal'
import { LeaderboardModal } from './components/modals/LeaderboardModal'
import { SettingsModal } from './components/modals/SettingsModal'
import { WinModal } from './components/modals/WinModal'
import { StatsModal } from './components/modals/StatsModal'
import { isWordInWordList, isWinningWord, solution } from './lib/words'
import { dateStr, genGameId, tomorrow } from './helpers'
import { useQueryClient } from 'react-query'

function App({ username }: { username: string }) {
  const [currentGuess, setCurrentGuess] = useState('')
  const [isGameWon, setIsGameWon] = useState(false)
  const [isWinModalOpen, setIsWinModalOpen] = useState(false)
  const [isLeaderboardModalOpen, setIsLeaderboardModalOpen] = useState(false)
  const [isSettingsModalOpen, setIsSettingsModalOpen] = useState(false)
  const [isInfoModalOpen, setIsInfoModalOpen] = useState(false)
  const [isAboutModalOpen, setIsAboutModalOpen] = useState(false)
  const [isNotEnoughLetters, setIsNotEnoughLetters] = useState(false)
  const [isStatsModalOpen, setIsStatsModalOpen] = useState(false)
  const [isWordNotFoundAlertOpen, setIsWordNotFoundAlertOpen] = useState(false)
  const [isFubsyAlertOpen, setIsFubsyAlertOpen] = useState(false)
  const [isGameLost, setIsGameLost] = useState(false)
  const [shareComplete, setShareComplete] = useState(false)
  const date = dateStr()
  const id = genGameId(username)
  const tomorrowDate = tomorrow()
  const [guesses, setGuesses] = useState<string[]>([])
  const { data } = useGameForIdQuery(
    { id },
    {
      onSuccess: (data) => {
        const game = data?.gameForId
        const guesses = game?.guesses?.map((guess) => guess)
        setGuesses(guesses || [])
        if (game?.finished && guesses?.[guesses.length - 1] === game.solution) {
          setIsGameWon(true)
        }
      },
    }
  )
  const game = data?.gameForId

  const queryClient = useQueryClient()
  const updateMutation = useSaveGameMutation({
    onSettled: () => {
      console.log('saved game')
      queryClient.refetchQueries(useGameHistoryQuery.getKey({ id }))
      queryClient.refetchQueries(useGameForIdQuery.getKey({ id }))
    },
  })

  useEffect(() => {
    if (game?.date && game.date !== date) {
      console.log('new game')
      setGuesses([])
    }
  }, [data])

  useEffect(() => {
    if (isGameWon) {
      setIsWinModalOpen(true)
    }
  }, [isGameWon])

  const onChar = (value: string) => {
    if (currentGuess.length < 5 && guesses.length < 6) {
      setCurrentGuess(`${currentGuess}${value}`)
    }
  }

  const onDelete = () => {
    setCurrentGuess(currentGuess.slice(0, -1))
  }

  const onEnter = () => {
    if (!(currentGuess.length === 5)) {
      setIsNotEnoughLetters(true)
      return setTimeout(() => {
        setIsNotEnoughLetters(false)
      }, 2000)
    }
    if (currentGuess.toLowerCase() === 'fubsy') {
      setIsFubsyAlertOpen(true)
      return setTimeout(() => {
        setIsFubsyAlertOpen(false)
      }, 2000)
    }

    if (!isWordInWordList(currentGuess)) {
      setIsWordNotFoundAlertOpen(true)
      return setTimeout(() => {
        setIsWordNotFoundAlertOpen(false)
      }, 2000)
    }

    updateMutation.mutate({
      id,
      game: {
        date,
        username,
        guesses: [...guesses, currentGuess],
        solution,
      },
    })

    const winningWord = isWinningWord(currentGuess)
    if (currentGuess.length === 5 && guesses.length < 6 && !isGameWon) {
      setGuesses([...guesses, currentGuess])
      setCurrentGuess('')

      if (winningWord) {
        setIsGameWon(true)
      }

      if (guesses.length === 5) {
        setIsGameLost(true)
        return setTimeout(() => {
          setIsGameLost(false)
        }, 2000)
      }
    }
  }

  return (
    <div className="py-8 max-w-7xl mx-auto sm:px-6 lg:px-8">
      <Alert message="Not enough letters" isOpen={isNotEnoughLetters} />
      <Alert message="Word not found" isOpen={isWordNotFoundAlertOpen} />
      <Alert
        message="Sorry but you are too fat to use this word"
        isOpen={isFubsyAlertOpen}
      />
      <Alert
        message={`You lost, the word was ${solution}`}
        isOpen={isGameLost}
      />
      <Alert
        message="Game copied to clipboard"
        isOpen={shareComplete}
        variant="success"
      />
      <div className="flex w-64 sm:w-80 mx-auto items-center mb-8 space-x-2">
        <h1 className="text-xl grow font-bold">Playing as {username}</h1>
        <CogIcon
          className="w-6 h-6 cursor-pointer"
          onClick={() => setIsSettingsModalOpen(true)}
        />
        <InformationCircleIcon
          className="h-6 w-6 cursor-pointer"
          onClick={() => setIsInfoModalOpen(true)}
        />
        <ChartBarIcon
          className="h-6 w-6 cursor-pointer"
          onClick={() => setIsStatsModalOpen(true)}
        />
        <UserGroupIcon
          className="h-6 w-6 cursor-pointer"
          onClick={() => setIsLeaderboardModalOpen(true)}
        />
      </div>
      <Grid guesses={guesses} solution={solution} currentGuess={currentGuess} />
      {(isGameWon || isGameLost) && (
        <div className="flex font-bold text-sm flex-col justify-center items-center mb-5">
          NEXT JUXTLE
          <div className="text-3xl">
            <Countdown daysInHours date={tomorrowDate} />
          </div>
        </div>
      )}
      <Keyboard
        disabled={isGameWon || isGameLost}
        onChar={onChar}
        onDelete={onDelete}
        onEnter={onEnter}
        guesses={guesses}
      />
      <WinModal
        isOpen={isWinModalOpen}
        handleClose={() => setIsWinModalOpen(false)}
        guesses={guesses}
        handleStats={() => {
          setIsWinModalOpen(false)
          setIsStatsModalOpen(true)
        }}
        handleShare={() => {
          setIsWinModalOpen(false)
          setShareComplete(true)
          return setTimeout(() => {
            setShareComplete(false)
          }, 2000)
        }}
      />
      <InfoModal
        isOpen={isInfoModalOpen}
        handleClose={() => setIsInfoModalOpen(false)}
      />
      <SettingsModal
        isOpen={isSettingsModalOpen}
        handleClose={() => setIsSettingsModalOpen(false)}
      />
      <LeaderboardModal
        isOpen={isLeaderboardModalOpen}
        handleClose={() => setIsLeaderboardModalOpen(false)}
      />
      <StatsModal
        isOpen={isStatsModalOpen}
        handleClose={() => setIsStatsModalOpen(false)}
        username={username}
      />
      <AboutModal
        isOpen={isAboutModalOpen}
        handleClose={() => setIsAboutModalOpen(false)}
      />

      <button
        type="button"
        className="mx-auto mt-8 flex items-center px-2.5 py-1.5 border border-transparent text-xs font-medium rounded text-indigo-700 bg-indigo-100 hover:bg-indigo-200 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
        onClick={() => setIsAboutModalOpen(true)}
      >
        About this game
      </button>
    </div>
  )
}

export default App
