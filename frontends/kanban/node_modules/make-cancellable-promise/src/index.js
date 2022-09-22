export default function makeCancellablePromise(promise) {
  let isCancelled = false;

  const wrappedPromise = new Promise((resolve, reject) => {
    promise
      .then((...args) => !isCancelled && resolve(...args))
      .catch((error) => !isCancelled && reject(error));
  });

  return {
    promise: wrappedPromise,
    cancel() {
      isCancelled = true;
    },
  };
}
