import { Fragment } from 'react'
import { dateStr, joinList, notEmpty } from '../../helpers'
import { Dialog, Transition } from '@headlessui/react'
import { CheckIcon } from '@heroicons/react/outline'
import { MiniGrid } from '../mini-grid/MiniGrid'
import { shareStatus } from '../../lib/share'
import { XCircleIcon } from '@heroicons/react/outline'
import { useAllGamesQuery } from '../../generated/graphql'
import { solution } from '../../lib/words'

type Props = {
  isOpen: boolean
  handleClose: () => void
  guesses: string[]
  handleShare: () => void
  handleStats: () => void
}

export const WinModal = ({
  isOpen,
  handleClose,
  guesses,
  handleShare,
  handleStats,
}: Props) => {
  const me = window.localStorage.getItem('username')
  const { data } = useAllGamesQuery(undefined, { enabled: isOpen })
  const othersGames =
    data?.allGames
      ?.filter((game) => game?.date === dateStr())
      ?.filter((game) => game?.finished)
      ?.map((game) => game?.username)
      .filter((username) => username !== me)
      .filter(notEmpty) || []

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
            <div className="inline-block align-bottom bg-white rounded-lg px-4 pt-5 pb-4 text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-sm sm:w-full sm:p-6">
              <button className="absolute right-4 top-4">
                <XCircleIcon
                  className="h-6 w-6 cursor-pointer"
                  onClick={() => handleClose()}
                />
              </button>
              <div>
                <div className="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-green-100">
                  <CheckIcon
                    className="h-6 w-6 text-green-600"
                    aria-hidden="true"
                  />
                </div>
                <div className="mt-3 text-center sm:mt-5">
                  <Dialog.Title
                    as="h3"
                    className="text-lg leading-6 font-medium text-gray-900"
                  >
                    You won!
                  </Dialog.Title>
                  <div className="mt-2">
                    <MiniGrid guesses={guesses} solution={solution} />
                    <p className="text-sm text-gray-500">Great job.</p>
                    {othersGames.length > 0 ? (
                      <p>
                        {`${joinList(othersGames)} ${
                          othersGames.length === 1 ? 'has ' : 'have '
                        } also finished today's game, see how they did!`}
                      </p>
                    ) : (
                      <p>
                        {`You're the first to finish today's game, why not pester some other people to play!`}
                      </p>
                    )}
                  </div>
                </div>
              </div>
              <div className="mt-5 sm:mt-6">
                <button
                  type="button"
                  className="inline-flex justify-center w-full rounded-md border border-transparent shadow-sm px-4 py-2 bg-orange-400 text-base font-medium text-white hover:bg-orange-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-orange-300 sm:text-sm"
                  onClick={() => {
                    handleStats()
                  }}
                >
                  See stats for today's game
                </button>

                <button
                  type="button"
                  className="mt-2 inline-flex justify-center w-full rounded-md border border-transparent shadow-sm px-4 py-2 bg-gray-600 text-base font-medium text-white hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-gray-300 sm:text-sm"
                  onClick={() => {
                    shareStatus(guesses)
                    handleShare()
                  }}
                >
                  Copy Board To Share
                </button>
              </div>
            </div>
          </Transition.Child>
        </div>
      </Dialog>
    </Transition.Root>
  )
}
