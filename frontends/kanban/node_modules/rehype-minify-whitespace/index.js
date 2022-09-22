/**
 * rehype plugin to minify whitespace between elements.
 *
 * ## What is this?
 *
 * This package is a plugin that can minify the whitespace between elements.
 *
 * ## When should I use this?
 *
 * You can use this plugin when you want to improve the size of HTML documents.
 *
 * ## API
 *
 * ### `unified().use(rehypeMinifyWhitespace[, options])`
 *
 * Minify whitespace.
 *
 * ##### `options`
 *
 * Configuration (optional).
 *
 * ##### `options.newlines`
 *
 * Whether to collapse runs of whitespace that include line endings to one
 * line ending (`boolean`, default: `false`).
 * The default is to collapse everything to one space.
 *
 * @example
 *   <h1>Heading</h1>
 *   <p><strong>This</strong> and <em>that</em></p>
 */

/**
 * @typedef {import('hast').Root} Root
 * @typedef {import('hast').Element} Element
 * @typedef {import('hast').Text} Text
 * @typedef {Root|Root['children'][number]} Node
 *
 * @typedef Options
 * @property {boolean} [newlines=false]
 *   If `newlines: true`, collapses whitespace containing newlines to `'\n'`
 *   instead of `' '`.
 *   The default is to collapse to a single space.
 *
 * @typedef {'pre'|'nowrap'|'pre-wrap'|'normal'} Whitespace
 *
 * @typedef Context
 * @property {ReturnType<collapseFactory>} collapse
 * @property {Whitespace} whitespace
 * @property {boolean} [before]
 * @property {boolean} [after]
 *
 * @typedef Result
 * @property {boolean} remove
 * @property {boolean} ignore
 * @property {boolean} stripAtStart
 */

import {isElement} from 'hast-util-is-element'
import {embedded} from 'hast-util-embedded'
import {convert} from 'unist-util-is'
import {whitespace} from 'hast-util-whitespace'
import {blocks} from './block.js'
import {content as contents} from './content.js'
import {skippable as skippables} from './skippable.js'

const ignorableNode = convert(['doctype', 'comment'])

/**
 * Minify whitespace.
 *
 * @type {import('unified').Plugin<[Options?]|Array<void>, Root>}
 */
export default function rehypeMinifyWhitespace(options = {}) {
  const collapse = collapseFactory(
    options.newlines ? replaceNewlines : replaceWhitespace
  )

  return (tree) => {
    minify(tree, {collapse, whitespace: 'normal'})
  }
}

/**
 * @param {Node} node
 * @param {Context} context
 * @returns {Result}
 */
function minify(node, context) {
  if ('children' in node) {
    const settings = Object.assign({}, context)

    if (node.type === 'root' || blocklike(node)) {
      settings.before = true
      settings.after = true
    }

    settings.whitespace = inferWhiteSpace(node, context)

    return all(node, settings)
  }

  if (node.type === 'text') {
    if (context.whitespace === 'normal') {
      return minifyText(node, context)
    }

    // Naïve collapse, but no trimming:
    if (context.whitespace === 'nowrap') {
      node.value = context.collapse(node.value)
    }

    // The `pre-wrap` or `pre` whitespace settings are neither collapsed nor
    // trimmed.
  }

  return {remove: false, ignore: ignorableNode(node), stripAtStart: false}
}

/**
 * @param {Text} node
 * @param {Context} context
 * @returns {Result}
 */
function minifyText(node, context) {
  const value = context.collapse(node.value)
  const result = {remove: false, ignore: false, stripAtStart: false}
  let start = 0
  let end = value.length

  if (context.before && removable(value.charAt(0))) {
    start++
  }

  if (start !== end && removable(value.charAt(end - 1))) {
    if (context.after) {
      end--
    } else {
      result.stripAtStart = true
    }
  }

  if (start === end) {
    result.remove = true
  } else {
    node.value = value.slice(start, end)
  }

  return result
}

/**
 * @param {Root|Element} parent
 * @param {Context} context
 * @returns {Result}
 */
function all(parent, context) {
  let before = context.before
  const after = context.after
  const children = parent.children
  let length = children.length
  let index = -1

  while (++index < length) {
    const result = minify(
      children[index],
      Object.assign({}, context, {
        before,
        after: collapsableAfter(children, index, after)
      })
    )

    if (result.remove) {
      children.splice(index, 1)
      index--
      length--
    } else if (!result.ignore) {
      before = result.stripAtStart
    }

    // If this element, such as a `<select>` or `<img>`, contributes content
    // somehow, allow whitespace again.
    if (content(children[index])) {
      before = false
    }
  }

  return {remove: false, ignore: false, stripAtStart: Boolean(before || after)}
}

/**
 * @param {Array<Node>} nodes
 * @param {number} index
 * @param {boolean|undefined} [after]
 * @returns {boolean|undefined}
 */
function collapsableAfter(nodes, index, after) {
  while (++index < nodes.length) {
    const node = nodes[index]
    let result = inferBoundary(node)

    if (result === undefined && 'children' in node && !skippable(node)) {
      result = collapsableAfter(node.children, -1)
    }

    if (typeof result === 'boolean') {
      return result
    }
  }

  return after
}

/**
 * Infer two types of boundaries:
 *
 * 1. `true` — boundary for which whitespace around it does not contribute
 *    anything
 * 2. `false` — boundary for which whitespace around it *does* contribute
 *
 * No result (`undefined`) is returned if it is unknown.
 *
 * @param {Node} node
 * @returns {boolean|undefined}
 */
function inferBoundary(node) {
  if (node.type === 'element') {
    if (content(node)) {
      return false
    }

    if (blocklike(node)) {
      return true
    }

    // Unknown: either depends on siblings if embedded or metadata, or on
    // children.
  } else if (node.type === 'text') {
    if (!whitespace(node)) {
      return false
    }
  } else if (!ignorableNode(node)) {
    return false
  }
}

/**
 * Infer whether a node is skippable.
 *
 * @param {Node} node
 * @returns {boolean}
 */
function content(node) {
  return embedded(node) || isElement(node, contents)
}

/**
 * See: <https://html.spec.whatwg.org/#the-css-user-agent-style-sheet-and-presentational-hints>
 *
 * @param {Element} node
 * @returns {boolean}
 */
function blocklike(node) {
  return isElement(node, blocks)
}

/**
 * @param {Element|Root} node
 * @returns {boolean}
 */
function skippable(node) {
  return (
    Boolean(
      'properties' in node && node.properties && node.properties.hidden
    ) ||
    ignorableNode(node) ||
    isElement(node, skippables)
  )
}

/**
 * @param {string} character
 * @returns {boolean}
 */
function removable(character) {
  return character === ' ' || character === '\n'
}

/**
 * @param {string} value
 * @returns {string}
 */
function replaceNewlines(value) {
  const match = /\r?\n|\r/.exec(value)
  return match ? match[0] : ' '
}

/**
 * @returns {string}
 */
function replaceWhitespace() {
  return ' '
}

/**
 * @param {(value: string) => string} replace
 */
function collapseFactory(replace) {
  return collapse

  /**
   * @param {string} value
   * @returns {string}
   */
  function collapse(value) {
    return String(value).replace(/[\t\n\v\f\r ]+/g, replace)
  }
}

/**
 * We don’t need to support void elements here (so `nobr wbr` -> `normal` is
 * ignored).
 *
 * @param {Root|Element} node
 * @param {Context} context
 * @returns {Whitespace}
 */
function inferWhiteSpace(node, context) {
  if ('tagName' in node && node.properties) {
    switch (node.tagName) {
      // Whitespace in script/style, while not displayed by CSS as significant,
      // could have some meaning in JS/CSS, so we can’t touch them.
      case 'listing':
      case 'plaintext':
      case 'script':
      case 'style':
      case 'xmp':
        return 'pre'
      case 'nobr':
        return 'nowrap'
      case 'pre':
        return node.properties.wrap ? 'pre-wrap' : 'pre'
      case 'td':
      case 'th':
        return node.properties.noWrap ? 'nowrap' : context.whitespace
      case 'textarea':
        return 'pre-wrap'
      default:
    }
  }

  return context.whitespace
}
