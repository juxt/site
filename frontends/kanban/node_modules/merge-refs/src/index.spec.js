import mergeRefs from './index';

describe('mergeRefs()', () => {
  it('returns falsy result given no arguments', () => {
    const result = mergeRefs();

    expect(result).toBe(undefined);
  });

  it('returns falsy result given falsy arguments', () => {
    const result = mergeRefs(null, null);

    expect(result).toBe(undefined);
  });

  it('returns original ref given only one ref', () => {
    const ref = () => {};

    const result = mergeRefs(ref);

    expect(result).toBe(ref);
  });

  it('returns original ref given one ref and one falsy argument', () => {
    const ref = () => {};

    const result = mergeRefs(ref, null);

    expect(result).toBe(ref);
  });

  it('returns merged refs properly', () => {
    const ref1 = () => {};
    const ref2 = {};

    const result = mergeRefs(ref1, ref2);

    expect(result).not.toBe(ref1);
    expect(result).toEqual(expect.any(Function));
  });

  it('handles merged functional refs properly', () => {
    const ref1 = jest.fn();
    const ref2 = {};

    const mergedRef = mergeRefs(ref1, ref2);

    const refElement = {};
    mergedRef(refElement);

    expect(ref1).toHaveBeenCalledTimes(1);
    expect(ref1).toHaveBeenCalledWith(refElement);
  });

  it('handles merged object refs properly', () => {
    const ref1 = {};
    const ref2 = jest.fn();

    const mergedRef = mergeRefs(ref1, ref2);

    const refElement = {};
    mergedRef(refElement);

    expect(ref1.current).toBe(refElement);
  });
});
