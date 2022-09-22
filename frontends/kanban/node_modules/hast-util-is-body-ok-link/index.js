/**
 * @fileoverview
 *   Check if a `link` element is “Body OK”.
 * @longdescription
 *   ## Use
 *
 *   ```js
 *   import {h} from 'hastscript'
 *   import {isBodyOkLink} from 'hast-util-is-body-ok-link'
 *
 *   isBodyOkLink(h('link', {itemProp: 'foo'})) //=> true
 *   isBodyOkLink(h('link', {rel: ['stylesheet'], href: 'index.css'})) //=> true
 *   isBodyOkLink(h('link', {rel: ['author'], href: 'index.css'})) //=> false
 *   ```
 *
 *   ## API
 *
 *   ### `isBodyOkLink(node)`
 *
 *   * Return `true` for `link` elements with an `itemProp`
 *   * Return `true` for `link` elements with a `rel` list where one or more
 *     entries are `pingback`, `prefetch`, or `stylesheet`.
 */

import {isElement} from 'hast-util-is-element'
import {hasProperty} from 'hast-util-has-property'

const list = new Set(['pingback', 'prefetch', 'stylesheet'])

/**
 * @typedef {import('hast').Root} Root
 * @typedef {Root|Root['children'][number]} Node
 */

/**
 * Check if a `link` element is “Body OK”.
 *
 * @param {Node} node
 * @returns {boolean}
 */
export function isBodyOkLink(node) {
  if (!isElement(node, 'link')) {
    return false
  }

  if (hasProperty(node, 'itemProp')) {
    return true
  }

  const props = node.properties || {}
  const rel = props.rel || []
  let index = -1

  if (!Array.isArray(rel) || rel.length === 0) {
    return false
  }

  while (++index < rel.length) {
    if (!list.has(String(rel[index]))) {
      return false
    }
  }

  return true
}
