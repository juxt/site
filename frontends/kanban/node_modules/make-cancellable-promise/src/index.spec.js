import makeCancellablePromise from './index';

jest.useFakeTimers();

describe('makeCancellablePromise()', () => {
  function resolveInFiveSeconds() {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve('Success');
      }, 5000);
    });
  }

  function rejectInFiveSeconds() {
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        reject(new Error('Error'));
      }, 5000);
    });
  }

  it('resolves promise if not cancelled', async () => {
    const resolve = jest.fn();
    const reject = jest.fn();

    const { promise } = makeCancellablePromise(resolveInFiveSeconds());

    jest.advanceTimersByTime(5000);
    await promise.then(resolve).catch(reject);

    expect(resolve).toHaveBeenCalledWith('Success');
    expect(reject).not.toHaveBeenCalled();
  });

  it('rejects promise if not cancelled', async () => {
    const resolve = jest.fn();
    const reject = jest.fn();

    const { promise } = makeCancellablePromise(rejectInFiveSeconds());

    jest.runAllTimers();
    await promise.then(resolve).catch(reject);

    expect(resolve).not.toHaveBeenCalled();
    expect(reject).toHaveBeenCalledWith(expect.any(Error));
  });

  it('does not resolve promise if cancelled', async () => {
    expect.assertions(0);

    const resolve = jest.fn(() => {
      // Will fail because of expect.assertions(0);
      expect(true).toBe(true);
    });
    const reject = jest.fn(() => {
      // Will fail because of expect.assertions(0);
      expect(true).toBe(true);
    });

    const { promise, cancel } = makeCancellablePromise(rejectInFiveSeconds());
    promise.then(resolve).catch(reject);

    jest.advanceTimersByTime(2500);
    cancel();
    jest.advanceTimersByTime(2500);
  });

  it('does not reject promise if cancelled', () => {
    expect.assertions(0);

    const resolve = jest.fn(() => {
      // Will fail because of expect.assertions(0);
      expect(true).toBe(true);
    });
    const reject = jest.fn(() => {
      // Will fail because of expect.assertions(0);
      expect(true).toBe(true);
    });

    const { promise, cancel } = makeCancellablePromise(rejectInFiveSeconds());
    promise.then(resolve).catch(reject);

    jest.advanceTimersByTime(2500);
    cancel();
    jest.advanceTimersByTime(2500);
  });
});
