/**
 * @typedef {import('hast').Root} Root
 * @typedef {Root['children'][number]} Child
 * @typedef {import('hast').Element} Element
 * @typedef {Root|Child} Node
 *
 * @typedef Options
 *   Configuration.
 * @property {number|string} [indent=2]
 *   Indentation per level (`number`, `string`, default: `2`).
 *   When number, uses that amount of spaces.
 *   When `string`, uses that per indentation level.
 * @property {boolean} [indentInitial=true]
 *   Whether to indent the first level (`boolean`, default: `true`).
 *   This is usually the `<html>`, thus not indenting `head` and `body`.
 * @property {Array<string>} [blanks=[]]
 *   List of tag names to join with a blank line (`Array<string>`, default:
 *   `[]`).
 *   These tags, when next to each other, are joined by a blank line (`\n\n`).
 *   For example, when `['head', 'body']` is given, a blank line is added
 *   between these two.
 */

import rehypeMinifyWhitespace from 'rehype-minify-whitespace'
import {visitParents, SKIP} from 'unist-util-visit-parents'
import {embedded} from 'hast-util-embedded'
import {phrasing} from 'hast-util-phrasing'
import {whitespace} from 'hast-util-whitespace'
import {isElement} from 'hast-util-is-element'
import {whitespaceSensitiveTagNames} from 'html-whitespace-sensitive-tag-names'

const minify = rehypeMinifyWhitespace({newlines: true})

/**
 * Format whitespace in HTML.
 *
 * @type {import('unified').Plugin<[Options?] | Array<void>, Root>}
 */
export default function rehypeFormat(options = {}) {
  let indent = options.indent || 2
  let indentInitial = options.indentInitial

  if (typeof indent === 'number') {
    indent = ' '.repeat(indent)
  }

  // Default to indenting the initial level.
  if (indentInitial === null || indentInitial === undefined) {
    indentInitial = true
  }

  return (tree) => {
    /** @type {boolean|undefined} */
    let head

    // @ts-expect-error: fine, it’s a sync transformer.
    minify(tree)

    // eslint-disable-next-line complexity
    visitParents(tree, (node, parents) => {
      let index = -1

      if (!('children' in node)) {
        return
      }

      if (isElement(node, 'head')) {
        head = true
      }

      if (head && isElement(node, 'body')) {
        head = undefined
      }

      if (isElement(node, whitespaceSensitiveTagNames)) {
        return SKIP
      }

      const children = node.children
      let level = parents.length

      // Don’t indent content of whitespace-sensitive nodes / inlines.
      if (children.length === 0 || !padding(node, head)) {
        return
      }

      if (!indentInitial) {
        level--
      }

      /** @type {boolean|undefined} */
      let eol

      // Indent newlines in `text`.
      while (++index < children.length) {
        const child = children[index]

        if (child.type === 'text' || child.type === 'comment') {
          if (child.value.includes('\n')) {
            eol = true
          }

          child.value = child.value.replace(
            / *\n/g,
            '$&' + String(indent).repeat(level)
          )
        }
      }

      /** @type {Array<Child>} */
      const result = []
      /** @type {Child|undefined} */
      let previous

      index = -1

      while (++index < children.length) {
        const child = children[index]

        if (padding(child, head) || (eol && !index)) {
          addBreak(result, level, child)
          eol = true
        }

        previous = child
        result.push(child)
      }

      if (previous && (eol || padding(previous, head))) {
        // Ignore trailing whitespace (if that already existed), as we’ll add
        // properly indented whitespace.
        if (whitespace(previous)) {
          result.pop()
          previous = result[result.length - 1]
        }

        addBreak(result, level - 1)
      }

      node.children = result
    })
  }

  /**
   * @param {Array<Child>} list
   * @param {number} level
   * @param {Child} [next]
   * @returns {void}
   */
  function addBreak(list, level, next) {
    const tail = list[list.length - 1]
    const previous = whitespace(tail) ? list[list.length - 2] : tail
    const replace =
      (blank(previous) && blank(next) ? '\n\n' : '\n') +
      String(indent).repeat(Math.max(level, 0))

    if (tail && tail.type === 'text') {
      tail.value = whitespace(tail) ? replace : tail.value + replace
    } else {
      list.push({type: 'text', value: replace})
    }
  }

  /**
   * @param {Node|undefined} node
   * @returns {boolean}
   */
  function blank(node) {
    return Boolean(
      node &&
        node.type === 'element' &&
        options.blanks &&
        options.blanks.length > 0 &&
        options.blanks.includes(node.tagName)
    )
  }
}

/**
 * @param {Node} node
 * @param {boolean|undefined} head
 * @returns {boolean}
 */
function padding(node, head) {
  return (
    node.type === 'root' ||
    (node.type === 'element'
      ? head || isElement(node, 'script') || embedded(node) || !phrasing(node)
      : false)
  )
}
