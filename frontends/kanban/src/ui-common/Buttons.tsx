import classNames from 'classnames'

type ButtonProps = React.ButtonHTMLAttributes<HTMLButtonElement> & {
  children: React.ReactNode
  primary?: boolean
  red?: boolean
  noMargin?: boolean
  className?: string
}

export function Button({
  children,
  className,
  primary,
  red,
  noMargin,
  ...rest
}: ButtonProps) {
  return (
    <button
      type="button"
      className={classNames(
        'relative disabled:opacity-50 inline-flex items-center border border-gray-300 text-sm font-medium rounded-md py-2 px-3',
        !noMargin && 'sm:mr-3 sm:last:mr-0',
        primary &&
          'w-full inline-flex justify-center rounded-lg border border-transparent shadow-sm bg-blue-600 text-base font-medium hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 sm:w-auto sm:text-sm',
        red &&
          'w-full inline-flex justify-center rounded-lg border border-transparent shadow-sm bg-red-600 text-base font-medium hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500 sm:w-auto sm:text-sm',

        primary || red
          ? 'text-white'
          : 'text-gray-700 bg-white hover:bg-gray-50',
        className
      )}
      {...rest}
    >
      {children}
    </button>
  )
}

export function PageButton({ children, className, ...rest }: ButtonProps) {
  return (
    <button
      type="button"
      className={classNames(
        'relative inline-flex items-center px-2 py-2 border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50',
        className
      )}
      {...rest}
    >
      {children}
    </button>
  )
}
