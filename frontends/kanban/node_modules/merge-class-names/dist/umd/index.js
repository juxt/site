"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = mergeClassNames;

function mergeClassNames() {
  return Array.prototype.slice.call(arguments).reduce(function (classList, arg) {
    return classList.concat(arg);
  }, []).filter(function (arg) {
    return typeof arg === 'string';
  }).join(' ');
}