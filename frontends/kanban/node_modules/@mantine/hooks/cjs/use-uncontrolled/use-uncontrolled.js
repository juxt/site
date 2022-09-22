'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var react = require('react');

function useUncontrolled({
  value,
  defaultValue,
  finalValue,
  rule,
  onChange,
  onValueUpdate
}) {
  const shouldBeControlled = rule(value);
  const modeRef = react.useRef("initial");
  const initialValue = rule(defaultValue) ? defaultValue : finalValue;
  const [uncontrolledValue, setUncontrolledValue] = react.useState(initialValue);
  let effectiveValue = shouldBeControlled ? value : uncontrolledValue;
  if (!shouldBeControlled && modeRef.current === "controlled") {
    effectiveValue = finalValue;
  }
  modeRef.current = shouldBeControlled ? "controlled" : "uncontrolled";
  const mode = modeRef.current;
  const handleChange = (nextValue) => {
    typeof onChange === "function" && onChange(nextValue);
    if (mode === "uncontrolled") {
      setUncontrolledValue(nextValue);
    }
  };
  react.useEffect(() => {
    if (mode === "uncontrolled") {
      setUncontrolledValue(effectiveValue);
    }
    typeof onValueUpdate === "function" && onValueUpdate(effectiveValue);
  }, [mode, effectiveValue]);
  return [effectiveValue, handleChange, modeRef.current];
}

exports.useUncontrolled = useUncontrolled;
//# sourceMappingURL=use-uncontrolled.js.map
