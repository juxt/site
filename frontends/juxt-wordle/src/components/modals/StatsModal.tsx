import { Fragment, useState } from 'react'
import { UserTabs } from '../Tabs'
import _ from 'lodash-es'
import { Dialog, Transition } from '@headlessui/react'
import {
  ArrowLeftIcon,
  ArrowRightIcon,
  XCircleIcon,
} from '@heroicons/react/outline'
import { StatBar } from '../stats/StatBar'
import { Histogram } from '../stats/Histogram'
import { dateStr, notEmpty } from '../../helpers'
import {
  useAllGamesQuery,
  useAllUsernamesQuery,
  useTodaysGamesQuery,
} from '../../generated/graphql'
import { statsForGames } from '../../lib/stats'
import { MiniGrid } from '../mini-grid/MiniGrid'
import prettyMilliseconds from 'pretty-ms'
import { PageButton } from '../Buttons'

type Props = {
  isOpen: boolean
  handleClose: () => void
  username: string
}

export const StatsModal = ({ isOpen, handleClose, username }: Props) => {
  const [date, setDate] = useState(dateStr())
  const isToday = date === dateStr()
  const [dateOffset, setDateOffset] = useState(0)
  const { data: gamesData } = useTodaysGamesQuery(
    { date },
    {
      enabled: isOpen,
      select: (game) => {
        return {
          ...game,
          todaysGames: game?.todaysGames?.filter(notEmpty) ?? [],
        }
      },
    }
  )

  const [user, setUser] = useState(username)
  const { data: usersData } = useAllUsernamesQuery()
  const { data: allGamesData } = useAllGamesQuery(undefined, {
    enabled: isOpen,
    select: (games) => {
      return {
        ...games,
        allGames: games?.allGames?.filter(notEmpty) ?? [],
      }
    },
  })
  const allUsers = _.uniq(usersData?.allUsers)
    .filter(notEmpty)
    .filter((user) => {
      return gamesData?.todaysGames?.some((game) => game.username === user)
    })
  const myGames = allGamesData?.allGames.filter(
    (game) => game.username === user
  )

  const myStats = myGames && statsForGames(myGames)

  const currentGame = gamesData?.todaysGames?.find(
    (game) => game?.username === user
  )

  const myGame = gamesData?.todaysGames?.find(
    (game) => game?.username === username
  )
  const myGuesses = myGame?.guesses ?? []
  const canSpoil = myGuesses?.[myGuesses.length - 1] === myGame?.solution
  const bugFixers = ['wrk']
  const hasMedal = bugFixers.find((fixer) => fixer === user)

  return (
    <Transition.Root show={isOpen} as={Fragment}>
      <Dialog
        as="div"
        className="fixed z-10 inset-0 overflow-y-auto"
        onClose={handleClose}
      >
        <div className="flex items-center justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:block sm:p-0">
          <Transition.Child
            as={Fragment}
            enter="ease-out duration-300"
            enterFrom="opacity-0"
            enterTo="opacity-100"
            leave="ease-in duration-200"
            leaveFrom="opacity-100"
            leaveTo="opacity-0"
          >
            <Dialog.Overlay className="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity" />
          </Transition.Child>

          {/* This element is to trick the browser into centering the modal contents. */}
          <span
            className="hidden sm:inline-block sm:align-middle sm:h-screen"
            aria-hidden="true"
          >
            &#8203;
          </span>
          <Transition.Child
            as={Fragment}
            enter="ease-out duration-300"
            enterFrom="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95"
            enterTo="opacity-100 translate-y-0 sm:scale-100"
            leave="ease-in duration-200"
            leaveFrom="opacity-100 translate-y-0 sm:scale-100"
            leaveTo="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95"
          >
            <div
              className="inline-block align-bottom bg-white rounded-lg px-4 
                            pt-5 pb-4 text-left overflow-hidden shadow-xl transform 
                            transition-all sm:my-8 sm:align-middle sm:max-w-xl sm:w-full sm:p-6"
            >
              <button className="absolute right-4 top-4">
                <XCircleIcon
                  className="h-6 w-6 cursor-pointer"
                  onClick={handleClose}
                />
              </button>
              <div>
                <div className="text-center">
                  <Dialog.Title
                    as="h3"
                    title={hasMedal ? 'Fixed a bug' : 'Stats'}
                    className="text-lg leading-6 font-medium text-gray-900"
                  >
                    Statistics for {user}
                    {hasMedal && '🎖'}
                  </Dialog.Title>
                  {myStats ? (
                    <>
                      <StatBar gameStats={myStats} />
                      <h4 className="text-lg leading-6 font-medium text-gray-900">
                        Guess Distribution
                      </h4>
                      <Histogram gameStats={myStats} />

                      <div className="sm:h-80 relative">
                        <div className="absolute left-0 top-0">
                          <PageButton
                            onClick={() => {
                              setDateOffset(dateOffset - 1)
                              setDate(dateStr(dateOffset - 1))
                            }}
                          >
                            <>
                              <ArrowLeftIcon className="w-3 h-3 mr-1" />
                              {dateStr(dateOffset - 1).substring(0, 5)}
                            </>
                          </PageButton>
                        </div>
                        {currentGame && currentGame?.guesses ? (
                          <>
                            <p className="text-lg text-gray-600 my-2">
                              {isToday ? "Today's Game" : `Game on ${date}`}
                            </p>
                            <MiniGrid
                              solution={currentGame.solution}
                              showLetters={canSpoil}
                              guesses={currentGame.guesses}
                            />
                            {currentGame?.finished &&
                              !!currentGame?.timeTakenMillis && (
                                <p className="text-sm text-gray-600">
                                  Time Taken:{' '}
                                  {prettyMilliseconds(
                                    currentGame.timeTakenMillis
                                  )}
                                </p>
                              )}
                          </>
                        ) : (
                          <p className="text-lg text-gray-600 my-8 sm:my-0">
                            {isToday ? 'No game today' : `No game on ${date}`}
                          </p>
                        )}
                        <div className="absolute right-0 top-0">
                          <PageButton
                            onClick={() => {
                              console.log(dateOffset)

                              setDateOffset(dateOffset + 1)
                              setDate(dateStr(dateOffset + 1))
                            }}
                            disabled={dateOffset === 0}
                          >
                            <>
                              {dateStr(dateOffset + 1).substring(0, 5)}
                              <ArrowRightIcon className="w-3 h-3 mr-1" />
                            </>
                          </PageButton>
                        </div>
                      </div>
                    </>
                  ) : (
                    <div className="text-center">
                      <p className="text-lg leading-6 font-medium text-gray-900">
                        No stats for this user
                      </p>
                    </div>
                  )}
                  {allUsers.length > 1 && (
                    <UserTabs
                      usernames={allUsers}
                      currentUser={user}
                      setUser={setUser}
                    />
                  )}
                </div>
              </div>
            </div>
          </Transition.Child>
        </div>
      </Dialog>
    </Transition.Root>
  )
}
