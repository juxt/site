export function isDefined<T>(argument: T | undefined): argument is T {
  return argument !== undefined;
}
export function notEmpty<TValue>(
  value: TValue | null | undefined
): value is TValue {
  if (value === null || value === undefined) return false;
  return true;
}

export const baseUrl = '/_site/insite';
