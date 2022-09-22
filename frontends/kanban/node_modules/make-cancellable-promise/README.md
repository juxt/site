[![npm](https://img.shields.io/npm/v/make-cancellable-promise.svg)](https://www.npmjs.com/package/make-cancellable-promise) ![downloads](https://img.shields.io/npm/dt/make-cancellable-promise.svg) [![CI](https://github.com/wojtekmaj/make-cancellable-promise/workflows/CI/badge.svg)](https://github.com/wojtekmaj/make-cancellable-promise/actions) ![dependencies](https://img.shields.io/david/wojtekmaj/make-cancellable-promise.svg) ![dev dependencies](https://img.shields.io/david/dev/wojtekmaj/make-cancellable-promise.svg) [![tested with jest](https://img.shields.io/badge/tested_with-jest-99424f.svg)](https://github.com/facebook/jest)

# Make-Cancellable-Promise
Make any Promise cancellable.

## tl;dr
* Install by executing `npm install make-cancellable-promise` or `yarn add make-cancellable-promise`.
* Import by adding `import makeCancellablePromise from 'make-cancellable-promise`.
* Do stuff with it!
    ```js
    const { promise, cancel } = makeCancellablePromise(myPromise);
    ```

## User guide

### makeCancellablePromise(myPromise)

A function that returns an object with two properties:

`promise` and `cancel`. `promise` is a wrapped around your promise. `cancel` is a function which stops `.then()` and `.catch()` from working on `promise`, even if promise passed to `makeCancellablePromise` resolves or rejects.

#### Usage

```js
const { promise, cancel } = makeCancellablePromise(myPromise);
```

Typically, you'd want to use `makeCancellablePromise` in React components. If you call `setState` on an unmounted component, React will throw an error.

Here's how you can use `makeCancellablePromise` with React:

```jsx
function MyComponent() {
  const [status, setStatus] = useState('initial');

  useEffect(() => {
    const { promise, cancel } = makeCancellable(fetchData());

    promise
      .then(() => setStatus('success'))
      .catch(() => setStatus('error'));

    return () => {
      cancel();
    };
  }, []);

  const text = (() => {
    switch (status) {
      case 'pending': return 'Fetching…';
      case 'success': return 'Success';
      case 'error': return 'Error!';
      default: return 'Click to fetch';
    }
  })();

  return (
    <p>{text}</p>
  );
}
```
