import classNames from 'classnames'

export function UserTabs({
  usernames,
  setUser,
  currentUser,
}: {
  usernames: string[]
  setUser: (username: string) => void
  currentUser: string
}) {
  return (
    <div>
      <div className="sm:hidden">
        <label htmlFor="tabs" className="sr-only">
          Select a tab
        </label>
        <select
          id="tabs"
          name="tabs"
          onChange={(e) => setUser(e.target.value)}
          className="block w-full focus:ring-indigo-500 focus:border-indigo-500 border-gray-300 rounded-md"
          defaultValue={currentUser}
        >
          {usernames.map((name) => (
            <option key={name}>{name}</option>
          ))}
        </select>
      </div>
      <div className="hidden sm:block">
        <nav
          className="relative z-0 overflow-x-auto rounded-lg shadow flex divide-x divide-gray-200"
          aria-label="Tabs"
        >
          {usernames.map((name, tabIdx) => {
            const isCurrent = name === currentUser
            return (
              <button
                key={name}
                onClick={() => setUser(name)}
                className={classNames(
                  isCurrent
                    ? 'text-gray-900'
                    : 'text-gray-500 hover:text-gray-700',
                  tabIdx === 0 ? 'rounded-l-lg' : '',
                  tabIdx === usernames.length - 1 ? 'rounded-r-lg' : '',
                  'group relative min-w-fit flex-1 overflow-hidden bg-white py-4 px-4 text-sm font-medium text-center hover:bg-gray-50 focus:z-10'
                )}
                aria-current={isCurrent ? 'page' : undefined}
              >
                <span>{name}</span>
                <span
                  aria-hidden="true"
                  className={classNames(
                    isCurrent ? 'bg-indigo-500' : 'bg-transparent',
                    'absolute inset-x-0 bottom-0 h-0.5'
                  )}
                />
              </button>
            )
          })}
        </nav>
      </div>
    </div>
  )
}
