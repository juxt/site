import { Fragment, ReactElement, useRef } from 'react'
import { Menu, Transition } from '@headlessui/react'
import { DotsVerticalIcon } from '@heroicons/react/solid'
import classNames from 'classnames'

type OptionsProps = {
  options: {
    label: string | ReactElement
    props: React.HTMLProps<HTMLButtonElement>
    ActiveIcon?: (props: React.HTMLAttributes<HTMLOrSVGElement>) => ReactElement
    Icon: (props: React.HTMLAttributes<HTMLOrSVGElement>) => ReactElement
    hidden?: boolean
    openOnSelect?: boolean
    id: string
  }[]
  openOnSelect?: boolean
}

export function OptionsMenu({ options, openOnSelect }: OptionsProps) {
  const ref = useRef<HTMLButtonElement | null>(null)
  return (
    <Menu as="div" className="relative shrink-0 pr-2">
      <Menu.Button
        ref={ref}
        className="inline-flex h-8 w-8 items-center justify-center rounded-full bg-white text-gray-400 hover:text-gray-500 focus:outline-none focus:ring-2 focus:ring-violet-500 focus:ring-offset-2"
      >
        <span className="sr-only">Open options</span>
        <DotsVerticalIcon className="h-5 w-5" aria-hidden="true" />
      </Menu.Button>
      <Transition
        as={Fragment}
        enter="transition ease-out duration-100"
        enterFrom="transform opacity-0 scale-95"
        enterTo="transform opacity-100 scale-100"
        leave="transition ease-in duration-75"
        leaveFrom="transform opacity-100 scale-100"
        leaveTo="transform opacity-0 scale-95"
      >
        <Menu.Items className="absolute right-10 top-3 z-10 mx-3 mt-1 w-max origin-top-right divide-y divide-gray-200 rounded-md bg-white shadow-lg ring-1 ring-black ring-opacity-5 focus:outline-none">
          {options
            .filter((option) => !option.hidden)
            .map(({ label, props, id, Icon, ...rest }) => {
              const ActiveIcon = rest.ActiveIcon || Icon
              const activeClass = rest.ActiveIcon ? '' : 'text-violet-50'
              return (
                <div key={id} className="cursor-pointer py-1">
                  <Menu.Item>
                    {({ active }) => (
                      <button
                        className={`${
                          active ? 'bg-violet-500 text-white' : 'text-gray-900'
                        } group flex w-full items-center rounded-md p-2 text-sm`}
                        {...props}
                        onClick={(e) => {
                          // horrible but the only way to do this for now
                          // https://github.com/tailwindlabs/headlessui/discussions/1122
                          setTimeout(() => {
                            if (openOnSelect || rest.openOnSelect) {
                              ref.current?.click()
                            }
                          }, 0)
                          props.onClick?.(e)
                        }}
                        type="button"
                      >
                        {active && (
                          <ActiveIcon
                            className={classNames(activeClass, 'w-5 h-5 mr-2')}
                            aria-hidden
                          />
                        )}
                        {!active && (
                          <Icon
                            className="mr-2 h-5 w-5 text-violet-500"
                            aria-hidden
                          />
                        )}
                        {label}
                      </button>
                    )}
                  </Menu.Item>
                </div>
              )
            })}
        </Menu.Items>
      </Transition>
    </Menu>
  )
}
