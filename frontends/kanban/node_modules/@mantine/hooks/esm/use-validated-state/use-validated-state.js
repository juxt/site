import { useState } from 'react';

function useValidatedState(initialValue, validation) {
  const [value, setValue] = useState(initialValue);
  const [lastValidValue, setLastValidValue] = useState(validation(initialValue) ? initialValue : void 0);
  const [valid, setValid] = useState(validation(initialValue));
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

export { useValidatedState };
//# sourceMappingURL=use-validated-state.js.map
