import { rehype } from 'rehype'
import rehypeFormat from 'rehype-format'

function formatHtml(input: string) {
  return String(rehype().use(rehypeFormat).processSync(input))
    .replace(/<\/?(html|head|body)>/g, '')
    .replace(/\n {4}/g, '\n')
    .replace(/^\s+|\s+$/g, '')
}

export { formatHtml }
