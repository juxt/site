export default function makeCancellablePromise(promise) {
  var isCancelled = false;
  var wrappedPromise = new Promise(function (resolve, reject) {
    promise.then(function () {
      return !isCancelled && resolve.apply(void 0, arguments);
    })["catch"](function (error) {
      return !isCancelled && reject(error);
    });
  });
  return {
    promise: wrappedPromise,
    cancel: function cancel() {
      isCancelled = true;
    }
  };
}