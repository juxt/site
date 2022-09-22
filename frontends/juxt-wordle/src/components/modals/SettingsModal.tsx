import { Fragment, useEffect, useState } from 'react'
import { Dialog, Switch, Transition } from '@headlessui/react'
import { XCircleIcon } from '@heroicons/react/outline'
import classNames from 'classnames'
import {
  loadColourBlindMode,
  saveColourBlindMode,
} from '../../lib/localStorage'

type Props = {
  isOpen: boolean
  handleClose: () => void
}

export const SettingsModal = ({ isOpen, handleClose }: Props) => {
  const [colourBlindMode, setColourBlindMode] = useState(loadColourBlindMode())
  useEffect(() => {
    if (isOpen) {
      saveColourBlindMode(colourBlindMode)
    }
  }, [colourBlindMode, isOpen])

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
                <div className="text-center">
                  <Dialog.Title
                    as="h3"
                    className="text-lg leading-6 font-medium text-gray-900"
                  >
                    Settings
                  </Dialog.Title>
                  <div className="mt-2">
                    <div className="pt-6 divide-y divide-gray-200">
                      <div className="px-4 sm:px-6">
                        <ul className="mt-2 divide-y divide-gray-200 text-left">
                          <Switch.Group
                            as="li"
                            className="py-4 flex items-center justify-between"
                          >
                            <div className="flex flex-col">
                              <Switch.Label
                                as="p"
                                className="text-sm font-medium text-gray-900"
                                passive
                              >
                                Colour Blind Mode
                              </Switch.Label>
                              <Switch.Description className="text-sm text-gray-500">
                                High contrast colors
                              </Switch.Description>
                            </div>
                            <Switch
                              checked={colourBlindMode}
                              onChange={setColourBlindMode}
                              className={classNames(
                                colourBlindMode ? 'bg-teal-500' : 'bg-gray-200',
                                'ml-4 relative inline-flex flex-shrink-0 h-6 w-11 border-2 border-transparent rounded-full cursor-pointer transition-colors ease-in-out duration-200 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-sky-500'
                              )}
                            >
                              <span
                                aria-hidden="true"
                                className={classNames(
                                  colourBlindMode
                                    ? 'translate-x-5'
                                    : 'translate-x-0',
                                  'inline-block h-5 w-5 rounded-full bg-white shadow transform ring-0 transition ease-in-out duration-200'
                                )}
                              />
                            </Switch>
                          </Switch.Group>
                        </ul>
                      </div>
                      <div className="mt-4 pt-6 px-4 flex justify-end sm:px-6">
                        <button
                          type="submit"
                          onClick={() => handleClose()}
                          className="ml-5 bg-sky-700 border border-transparent rounded-md shadow-sm py-2 px-4 inline-flex justify-center text-sm font-medium text-white hover:bg-sky-800 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-sky-500"
                        >
                          Done
                        </button>
                      </div>
                    </div>
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
