/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 */

/*!
 * Based on 'gatsby-remark-autolink-headers'
 * Original Author: Kyle Mathews <mathews.kyle@gmail.com>
 * Updated by Jared Palmer;
 * Copyright (c) 2015 Gatsbyjs
 */

const toString = require('mdast-util-to-string');
const visit = require('unist-util-visit');
const slugs = require('github-slugger')();

function patch(context, key, value) {
  if (!context[key]) {
    context[key] = value;
  }
  return context[key];
}


module.exports = ({
  className = `anchor`,
  maintainCase = false,
} = {}) => {
  slugs.reset();
  return function transformer(tree) {
    visit(tree, 'heading', (node) => {
      const children = node.children;
      let tail = children[children.length - 1];

      // A bit weird: this is to support MDX 2 comments in expressions,
      // while we’re still on MDX 1, which doesn’t support them.
      if (!tail || tail.type !== 'text' || tail.value !== '/}') {
        return;
      }

      tail = children[children.length - 2];

      if (!tail && tail.type !== 'emphasis') {
        return;
      }

      const id = toString(tail);

      tail = children[children.length - 3];

      if (!tail || tail.type !== 'text' || !tail.value.endsWith('{/')) {
        return;
      }

      // Remove the emphasis and trailing `/}`
      children.splice(children.length - 2, 2);
      // Remove the `{/`
      tail.value = tail.value.replace(/[ \t]*\{\/$/, '');

      const data = patch(node, 'data', {});

      patch(data, 'id', id);
      patch(data, 'htmlAttributes', {});
      patch(data, 'hProperties', {});
      patch(data.htmlAttributes, 'id', id);
      patch(data.hProperties, 'id', id);
    });
  };
};
