/**
 * @typedef {import('hast').Element & {tagName: 'audio'|'canvas'|'embed'|'iframe'|'img'|'math'|'object'|'picture'|'svg'|'video'}} Embedded
 * @typedef {import('hast-util-is-element').AssertPredicate<Embedded>} AssertEmbedded
 */

import {convertElement} from 'hast-util-is-element'

/**
 * Check if a node is an embedded element.
 * @type {AssertEmbedded}
 */
// @ts-ignore Sure, the assertion matches.
export const embedded = convertElement([
  'audio',
  'canvas',
  'embed',
  'iframe',
  'img',
  'math',
  'object',
  'picture',
  'svg',
  'video'
])
