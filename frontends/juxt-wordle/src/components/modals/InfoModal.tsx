import { Fragment } from 'react'
import { Dialog, Transition } from '@headlessui/react'
import { Cell } from '../grid/Cell'
import { XCircleIcon } from '@heroicons/react/outline'

type Props = {
  isOpen: boolean
  handleClose: () => void
}

export const InfoModal = ({ isOpen, handleClose }: Props) => {
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
            <div className="inline-block align-middle bg-white rounded-lg px-4 pt-5 pb-4 text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-sm sm:w-full sm:p-6">
              <button className="absolute right-4 top-4">
                <XCircleIcon
                  className="h-6 w-6 cursor-pointer"
                  onClick={() => handleClose()}
                />
              </button>
              <div>
                <div className="text-center">
                  <Dialog.Title
                    as="h3"
                    className="text-lg leading-6 font-medium text-gray-900"
                  >
                    How to play
                  </Dialog.Title>
                  <div className="mt-2">
                    <p className="text-sm text-gray-500">
                      Guess the JUXTLE in 6 tries. After each guess, the color
                      of the tiles will change to show how close your guess was
                      to the word.
                    </p>

                    <div className="flex justify-center mb-1 mt-4">
                      <Cell value="W" status="correct" />
                      <Cell value="E" />
                      <Cell value="A" />
                      <Cell value="R" />
                      <Cell value="Y" />
                    </div>
                    <p className="text-sm text-gray-500">
                      The letter W is in the word and in the correct spot.
                    </p>

                    <div className="flex justify-center mb-1 mt-4">
                      <Cell value="P" />
                      <Cell value="I" />
                      <Cell value="L" status="present" />
                      <Cell value="O" />
                      <Cell value="T" />
                    </div>
                    <p className="text-sm text-gray-500">
                      The letter L is in the word but in the wrong spot.
                    </p>

                    <div className="flex justify-center mb-1 mt-4">
                      <Cell value="V" />
                      <Cell value="A" />
                      <Cell value="G" />
                      <Cell value="U" status="absent" />
                      <Cell value="E" />
                    </div>
                    <p className="text-sm text-gray-500">
                      The letter U is not in the word in any spot.
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </Transition.Child>
        </div>
      </Dialog>
    </Transition.Root>
  )
}
