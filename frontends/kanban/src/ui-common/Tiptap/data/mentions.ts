import { juxters } from '../../../site'

type Mention = {
  name: string
  github?: string
  avatar: string
  staffRecord: {
    juxtcode: string
  }
}

const MENTION_SUGGESTIONS: Mention[] = juxters

export type { Mention }

export { MENTION_SUGGESTIONS }
