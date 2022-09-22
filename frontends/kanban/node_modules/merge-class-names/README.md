[![npm](https://img.shields.io/npm/v/merge-class-names.svg)](https://www.npmjs.com/package/merge-class-names) ![downloads](https://img.shields.io/npm/dt/merge-class-names.svg) [![CI](https://github.com/wojtekmaj/merge-class-names/workflows/CI/badge.svg)](https://github.com/wojtekmaj/merge-class-names/actions) ![dependencies](https://img.shields.io/david/wojtekmaj/merge-class-names.svg) ![dev dependencies](https://img.shields.io/david/dev/wojtekmaj/merge-class-names.svg) [![tested with jest](https://img.shields.io/badge/tested_with-jest-99424f.svg)](https://github.com/facebook/jest)

# Merge-Class-Names
A function that merges given class names, no matter their format. Filters out invalid class names as well.

## tl;dr
* Install by executing `npm install merge-class-names` or `yarn add merge-class-names`.
* Import by adding `import mergeClassNames from 'merge-class-names'`.
* Use it in `className` like so: `<div className={mergeClassNames('foo', condition && 'bar', arrayOfClasses)} />`

## Accepted formats
* Strings with one or multiple class names: `a`, `a b`
* Array of strings with one or multiple class names: `['a', 'b']`, `['a b', 'c d']`.

## Examples

```js
> mergeClassNames('a', 'b', 'c');
< 'a b c'

> mergeClassNames('a b', 'c d', 'e f');
< 'a b c d e f'

> mergeClassNames(['a', 'b'], ['c', 'd']);
< 'a b c d'

> mergeClassNames(['a b', 'c d'], ['e f', 'g h']);
< 'a b c d e f g h'

> mergeClassNames('a', 'b', falsyCondition && 'c');
< 'a b'

> mergeClassNames('a', 'b', 'c', null, ['d', null], () => {}, 'e', undefined);
< 'a b c d e'
```

## License

The MIT License.

## Author

<table>
  <tr>
    <td>
      <img src="https://github.com/wojtekmaj.png?s=100" width="100">
    </td>
    <td>
      Wojciech Maj<br />
      <a href="mailto:kontakt@wojtekmaj.pl">kontakt@wojtekmaj.pl</a><br />
      <a href="https://wojtekmaj.pl">https://wojtekmaj.pl</a>
    </td>
  </tr>
</table>
