'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var react = require('react');

function useValidatedState(initialValue, validation) {
  const [value, setValue] = react.useState(initialValue);
  const [lastValidValue, setLastValidValue] = react.useState(validation(initialValue) ? initialValue : void 0);
  const [valid, setValid] = react.useState(validation(initialValue));
  const onChange = (val) => {
    if (validation(val)) {
      setLastValidValue(val);
      setValid(true);
    } else {
      setValid(false);
    }
    setValue(val);
  };
  return [{ value, lastValidValue, valid }, onChange];
}

exports.useValidatedState = useValidatedState;
//# sourceMappingURL=use-validated-state.js.map
