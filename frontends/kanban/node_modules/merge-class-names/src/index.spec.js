import mergeClassNames from './index';

describe('mergeClassNames', () => {
  it('returns nothing given nothing', () => {
    const result = mergeClassNames();

    expect(result).toBe('');
  });

  it('returns properly merged class names given an array of strings with single class names', () => {
    const result = mergeClassNames('a', 'b', 'c');

    expect(result).toBe('a b c');
  });

  it('returns properly merged class names given an array of strings with multiple class names', () => {
    const result = mergeClassNames('a b', 'c d', 'e f');

    expect(result).toBe('a b c d e f');
  });

  it('returns properly merged class names given an array of arrays of strings with single class names', () => {
    const result = mergeClassNames(['a', 'b'], ['c', 'd']);

    expect(result).toBe('a b c d');
  });

  it('returns properly merged class names given an array of arrays of strings with multiple class names', () => {
    const result = mergeClassNames(['a b', 'c d'], ['e f', 'g h']);

    expect(result).toBe('a b c d e f g h');
  });

  it('does not include null, undefined or other non-string arguments in the result', () => {
    const result = mergeClassNames('a', 'b', {}, 'c', null, 'd', () => {}, 'e', undefined);

    expect(result).toBe('a b c d e');
  });

  it('does not include null, undefined or other non-string arguments passed in an array in the result', () => {
    const result = mergeClassNames('a', ['b', {}], ['c', null], ['d', () => {}], ['e', undefined]);

    expect(result).toBe('a b c d e');
  });
});
