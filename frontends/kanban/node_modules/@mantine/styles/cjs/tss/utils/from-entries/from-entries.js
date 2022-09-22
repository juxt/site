'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

function fromEntries(entries) {
  const o = {};
  Object.keys(entries).forEach((key) => {
    const [k, v] = entries[key];
    o[k] = v;
  });
  return o;
}

exports.fromEntries = fromEntries;
//# sourceMappingURL=from-entries.js.map
