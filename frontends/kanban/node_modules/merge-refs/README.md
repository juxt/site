[![npm](https://img.shields.io/npm/v/merge-refs.svg)](https://www.npmjs.com/package/merge-refs) ![downloads](https://img.shields.io/npm/dt/merge-refs.svg) ![build](https://img.shields.io/travis/wojtekmaj/merge-refs/master.svg) ![dependencies](https://img.shields.io/david/wojtekmaj/merge-refs.svg) ![dev dependencies](https://img.shields.io/david/dev/wojtekmaj/merge-refs.svg) [![tested with jest](https://img.shields.io/badge/tested_with-jest-99424f.svg)](https://github.com/facebook/jest)

# Merge-Refs
A function that merges React refs into one. Filters out invalid (eg. falsy) refs as well and returns original ref if only one valid ref was given.

## tl;dr
* Install by executing `npm install merge-refs` or `yarn add merge-refs`.
* Import by adding `import mergeRefs from 'merge-refs'`.
* Pass arguments to it. Forget.

## Accepted refs
* Refs created using `React.createRef()`
* Refs created using `React.useRef()`
* Functional refs

## Example

```js
function Hello() {
  const ref1 = useRef(); // I'm going to be updated!
  const ref2 = (element) => {
    // I'm going to be called!
  };

  return (
    <div ref={mergeRefs(ref1, ref2)} />
  );
}
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
      <a href="http://wojtekmaj.pl">http://wojtekmaj.pl</a>
    </td>
  </tr>
</table>
