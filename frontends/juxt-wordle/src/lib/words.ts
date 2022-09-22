import { WORDS } from '../constants/wordlist'
import { VALIDGUESSES } from '../constants/validGuesses'

export const isWordInWordList = (word: string) => {
  return (
    WORDS.includes(word.toLowerCase()) ||
    VALIDGUESSES.includes(word.toLowerCase())
  )
}

export const isWinningWord = (word: string) => {
  return solution === word
}

export const getWordOfDay = () => {
  const now = new Date()
  now.setDate(now.getDate() + 1)
  now.setHours(0, 0, 0, 0)
  const then = new Date(now.getFullYear(), 0, 1).getTime()
  const index = Math.ceil((now.getTime() - then) / 86400000)
  return {
    solution: WORDS[index].toUpperCase(),
    solutionIndex: index,
  }
}

export const { solution, solutionIndex } = getWordOfDay()
