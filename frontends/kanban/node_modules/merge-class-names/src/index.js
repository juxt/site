export default function mergeClassNames() {
  return Array.prototype.slice.call(arguments).reduce(
    (classList, arg) => classList.concat(arg),
    [],
  ).filter((arg) => typeof arg === 'string').join(' ');
}
