import { useRef, useState, useEffect } from 'react';

function useUncontrolled({
  value,
  defaultValue,
  finalValue,
  rule,
  onChange,
  onValueUpdate
}) {
  const shouldBeControlled = rule(value);
  const modeRef = useRef("initial");
  const initialValue = rule(defaultValue) ? defaultValue : finalValue;
  const [uncontrolledValue, setUncontrolledValue] = useState(initialValue);
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
  useEffect(() => {
    if (mode === "uncontrolled") {
      setUncontrolledValue(effectiveValue);
    }
    typeof onValueUpdate === "function" && onValueUpdate(effectiveValue);
  }, [mode, effectiveValue]);
  return [effectiveValue, handleChange, modeRef.current];
}

export { useUncontrolled };
//# sourceMappingURL=use-uncontrolled.js.map
