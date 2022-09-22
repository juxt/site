import { getGuessStatuses } from './statuses'
import { solution, solutionIndex } from './words'

export const shareStatus = (guesses: string[]) => {
  navigator.clipboard.writeText(
    'Wordle ' +
      solutionIndex +
      ' ' +
      guesses.length +
      '/6\n\n' +
      generateEmojiGrid(guesses)
  )
}

export const generateEmojiGrid = (guesses: string[]) => {
  return guesses
    .map((guess) => {
      const status = getGuessStatuses(guess, solution)
      return guess
        .split('')
        .map((letter, i) => {
          switch (status[i]) {
            case 'correct':
              return '🟩'
            case 'present':
              return '🟨'
            default:
              return '⬜'
          }
        })
        .join('')
    })
    .join('\n')
}
