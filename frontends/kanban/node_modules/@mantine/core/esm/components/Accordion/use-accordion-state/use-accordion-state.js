import { useUncontrolled, useDidUpdate } from '@mantine/hooks';

var __defProp = Object.defineProperty;
var __defProps = Object.defineProperties;
var __getOwnPropDescs = Object.getOwnPropertyDescriptors;
var __getOwnPropSymbols = Object.getOwnPropertySymbols;
var __hasOwnProp = Object.prototype.hasOwnProperty;
var __propIsEnum = Object.prototype.propertyIsEnumerable;
var __defNormalProp = (obj, key, value) => key in obj ? __defProp(obj, key, { enumerable: true, configurable: true, writable: true, value }) : obj[key] = value;
var __spreadValues = (a, b) => {
  for (var prop in b || (b = {}))
    if (__hasOwnProp.call(b, prop))
      __defNormalProp(a, prop, b[prop]);
  if (__getOwnPropSymbols)
    for (var prop of __getOwnPropSymbols(b)) {
      if (__propIsEnum.call(b, prop))
        __defNormalProp(a, prop, b[prop]);
    }
  return a;
};
var __spreadProps = (a, b) => __defProps(a, __getOwnPropDescs(b));
function createAccordionState(length, initialItem = -1) {
  return Array.from({ length }).reduce((acc, _item, index) => {
    acc[index] = index === initialItem;
    return acc;
  }, {});
}
function useAccordionState({
  initialState,
  total,
  initialItem = -1,
  state,
  onChange,
  multiple = false
}) {
  const [value, setState] = useUncontrolled({
    value: state,
    defaultValue: initialState || createAccordionState(total, initialItem),
    finalValue: {},
    onChange,
    rule: (val) => val !== null && typeof val === "object"
  });
  const toggle = (index) => {
    if (multiple) {
      setState(__spreadProps(__spreadValues({}, value), { [index]: !value[index] }));
    } else {
      const newValues = Array(total).fill(0).reduce((acc, _, itemIndex) => {
        acc[itemIndex] = false;
        return acc;
      }, {});
      newValues[index] = !value[index];
      setState(newValues);
    }
  };
  useDidUpdate(() => {
    if (!multiple) {
      setState(createAccordionState(total));
    }
  }, [multiple]);
  return [value, { toggle, setState }];
}

export { createAccordionState, useAccordionState };
//# sourceMappingURL=use-accordion-state.js.map
