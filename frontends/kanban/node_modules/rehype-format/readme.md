# rehype-format

[![Build][build-badge]][build]
[![Coverage][coverage-badge]][coverage]
[![Downloads][downloads-badge]][downloads]
[![Size][size-badge]][size]
[![Sponsors][sponsors-badge]][collective]
[![Backers][backers-badge]][collective]
[![Chat][chat-badge]][chat]

**[rehype][]** plugin to format HTML.

## Contents

*   [What is this?](#what-is-this)
*   [When should I use this?](#when-should-i-use-this)
*   [Install](#install)
*   [Use](#use)
*   [API](#api)
    *   [`unified().use(rehypeFormat[, options])`](#unifieduserehypeformat-options)
*   [Examples](#examples)
    *   [Example: markdown input (remark)](#example-markdown-input-remark)
    *   [Example: tabs and blank lines (`indent`, `blanks`)](#example-tabs-and-blank-lines-indent-blanks)
*   [Types](#types)
*   [Compatibility](#compatibility)
*   [Security](#security)
*   [Related](#related)
*   [Contribute](#contribute)
*   [License](#license)

## What is this?

This package is a [unified][] ([rehype][]) plugin to format whitespace in HTML.
In short, it works as follows:

*   collapse all existing white space to either a line ending or a single space
*   remove those spaces and line endings if they do not contribute to the
    document
*   inject needed line endings
*   indent previously collapsed line endings properly

**unified** is a project that transforms content with abstract syntax trees
(ASTs).
**rehype** adds support for HTML to unified.
**hast** is the HTML AST that rehype uses.
This is a rehype plugin that changes whitespace in hast.

## When should I use this?

This package is useful when you want to improve the readability of HTML source
code as it adds insignificant but pretty whitespace between elements.
A different package, [`rehype-stringify`][rehype-stringify], controls how HTML
is actually printed: which quotes to use, whether to put a `/` on `<img />`,
etc.
Yet another project, [`rehype-minify`][rehype-minify], does the inverse: improve
the size of HTML source code by making it hard to read.

## Install

This package is [ESM only](https://gist.github.com/sindresorhus/a39789f98801d908bbc7ff3ecc99d99c).
In Node.js (version 12.20+, 14.14+, or 16.0+), install with [npm][]:

```sh
npm install rehype-format
```

In Deno with [Skypack][]:

```js
import rehypeFormat from 'https://cdn.skypack.dev/rehype-format@4?dts'
```

In browsers with [Skypack][]:

```html
<script type="module">
  import rehypeFormat from 'https://cdn.skypack.dev/rehype-format@4?min'
</script>
```

## Use

Say we have the following file `index.html`:

```html
<!doCTYPE HTML><html>
 <head>
    <title>Hello!</title>
<meta charset=utf8>
      </head>
  <body><section>    <p>hi there</p>
     </section>
 </body>
</html>
```

And our module `example.js` looks as follows:

```js
import {read} from 'to-vfile'
import {unified} from 'unified'
import rehypeParse from 'rehype-parse'
import rehypeFormat from 'rehype-format'
import rehypeStringify from 'rehype-stringify'

main()

async function main() {
  const file = await unified()
    .use(rehypeParse)
    .use(rehypeFormat)
    .use(rehypeStringify)
    .process(await read('index.html'))

  console.log(String(file))
}
```

Now running `node example.js` yields:

```html
<!doctype html>
<html>
  <head>
    <title>Hello!</title>
    <meta charset="utf8">
  </head>
  <body>
    <section>
      <p>hi there</p>
    </section>
  </body>
</html>
```

## API

This package exports no identifiers.
The default export is `rehypeFormat`.

### `unified().use(rehypeFormat[, options])`

Format whitespace in HTML.

##### `options`

Configuration (optional).

###### `options.indent`

Indentation per level (`number`, `string`, default: `2`).
When `number`, uses that amount of spaces.
When `string`, uses that per indentation level.

###### `options.indentInitial`

Whether to indent the first level (`boolean`, default: `true`).
The initial element is usually the `<html>` element, so when this is set to
`false`, its children `<head>` and `<body>` would not be indented.

###### `options.blanks`

List of tag names to join with a blank line (`Array<string>`, default: `[]`).
These tags, when next to each other, are joined by a blank line (`\n\n`).
For example, when `['head', 'body']` is given, a blank line is added between
these two.

## Examples

### Example: markdown input (remark)

The following example shows how remark and rehype can be combined to turn
markdown into HTML, using this plugin to pretty print the HTML:

```js
import {unified} from 'unified'
import remarkParse from 'remark-parse'
import remarkRehype from 'remark-rehype'
import rehypeDocument from 'rehype-document'
import rehypeFormat from 'rehype-format'
import rehypeStringify from 'rehype-stringify'

main()

async function main() {
  const file = await unified()
    .use(remarkParse)
    .use(remarkRehype)
    .use(rehypeDocument, {title: 'Neptune'})
    .use(rehypeFormat)
    .use(rehypeStringify)
    .process('# Hello, Neptune!')

  console.log(String(file))
}
```

Yields:

```html
<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <title>Neptune</title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
  </head>
  <body>
    <h1>Hello, Neptune!</h1>
  </body>
</html>
```

### Example: tabs and blank lines (`indent`, `blanks`)

The following example shows how this plugin can format with tabs instead of
spaces by passing the `indent` option and how blank lines can be added between
certain elements:

```js
import {unified} from 'unified'
import rehypeParse from 'rehype-parse'
import rehypeFormat from 'rehype-format'
import rehypeStringify from 'rehype-stringify'

main()

async function main() {
  const file = await unified()
    .use(rehypeParse)
    .use(rehypeFormat, {indent: '\t', blanks: ['head', 'body']})
    .use(rehypeStringify)
    .process('<h1>Hi!</h1><p>Hello, Venus!</p>')

  console.log(String(file))
}
```

Yields:

<!--lint ignore no-tabs-->

```html
<html>
	<head></head>

	<body>
		<h1>Hi!</h1>
		<p>Hello, Venus!</p>
	</body>
</html>
```

> 👉 **Note**: the added tags (`html`, `head`, and `body`) do not come from this
> plugin.
> They’re instead added by `rehype-parse`, which in document mode (default),
> adds them according to the HTML spec.

## Types

This package is fully typed with [TypeScript][].
It exports an `Options` type, which specifies the interface of the accepted
options.

## Compatibility

Projects maintained by the unified collective are compatible with all maintained
versions of Node.js.
As of now, that is Node.js 12.20+, 14.14+, and 16.0+.
Our projects sometimes work with older versions, but this is not guaranteed.

This plugin works with `rehype-parse` version 3+, `rehype-stringify` version 3+,
`rehype` version 5+, and `unified` version 6+.

## Security

Use of `rehype-format` changes white space in the tree.
White space in `<script>`, `<style>`, `<pre>`, or `<textarea>` is not modified.
If the tree is already safe, use of this plugin does not open you up for a
[cross-site scripting (XSS)][xss] attack.
When in doubt, use [`rehype-sanitize`][rehype-sanitize].

## Related

*   [`rehype-minify`](https://github.com/rehypejs/rehype-minify)
    — minify HTML
*   [`rehype-document`](https://github.com/rehypejs/rehype-document)
    — wrap a fragment in a document
*   [`rehype-sanitize`](https://github.com/rehypejs/rehype-sanitize)
    — sanitize HTML
*   [`rehype-toc`](https://github.com/JS-DevTools/rehype-toc)
    — add a table of contents (TOC)
*   [`rehype-section`](https://github.com/agentofuser/rehype-section)
    — wrap headings and their contents in sections

## Contribute

See [`contributing.md`][contributing] in [`rehypejs/.github`][health] for ways
to get started.
See [`support.md`][support] for ways to get help.

This project has a [code of conduct][coc].
By interacting with this repository, organization, or community you agree to
abide by its terms.

## License

[MIT][license] © [Titus Wormer][author]

<!-- Definitions -->

[build-badge]: https://github.com/rehypejs/rehype-format/workflows/main/badge.svg

[build]: https://github.com/rehypejs/rehype-format/actions

[coverage-badge]: https://img.shields.io/codecov/c/github/rehypejs/rehype-format.svg

[coverage]: https://codecov.io/github/rehypejs/rehype-format

[downloads-badge]: https://img.shields.io/npm/dm/rehype-format.svg

[downloads]: https://www.npmjs.com/package/rehype-format

[size-badge]: https://img.shields.io/bundlephobia/minzip/rehype-format.svg

[size]: https://bundlephobia.com/result?p=rehype-format

[sponsors-badge]: https://opencollective.com/unified/sponsors/badge.svg

[backers-badge]: https://opencollective.com/unified/backers/badge.svg

[collective]: https://opencollective.com/unified

[chat-badge]: https://img.shields.io/badge/chat-discussions-success.svg

[chat]: https://github.com/rehypejs/rehype/discussions

[skypack]: https://www.skypack.dev

[npm]: https://docs.npmjs.com/cli/install

[health]: https://github.com/rehypejs/.github

[contributing]: https://github.com/rehypejs/.github/blob/HEAD/contributing.md

[support]: https://github.com/rehypejs/.github/blob/HEAD/support.md

[coc]: https://github.com/rehypejs/.github/blob/HEAD/code-of-conduct.md

[license]: license

[author]: https://wooorm.com

[xss]: https://en.wikipedia.org/wiki/Cross-site_scripting

[typescript]: https://www.typescriptlang.org

[unified]: https://github.com/unifiedjs/unified

[rehype]: https://github.com/rehypejs/rehype

[rehype-stringify]: https://github.com/rehypejs/rehype/tree/main/packages/rehype-stringify

[rehype-sanitize]: https://github.com/rehypejs/rehype-sanitize

[rehype-minify]: https://github.com/rehypejs/rehype-minify
