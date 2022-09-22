import React, { forwardRef, useState, useRef } from 'react';
import { useUuid, useUncontrolled, useDidUpdate, useMergedRef } from '@mantine/hooks';
import { getDefaultZIndex, extractMargins } from '@mantine/styles';
import { SelectDropdown } from '../Select/SelectDropdown/SelectDropdown.js';
import { SelectItems } from '../Select/SelectItems/SelectItems.js';
import { DefaultItem } from '../Select/DefaultItem/DefaultItem.js';
import { filterData } from './filter-data/filter-data.js';
import useStyles from './Autocomplete.styles.js';
import { InputWrapper } from '../InputWrapper/InputWrapper.js';
import { Input } from '../Input/Input.js';

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
var __objRest = (source, exclude) => {
  var target = {};
  for (var prop in source)
    if (__hasOwnProp.call(source, prop) && exclude.indexOf(prop) < 0)
      target[prop] = source[prop];
  if (source != null && __getOwnPropSymbols)
    for (var prop of __getOwnPropSymbols(source)) {
      if (exclude.indexOf(prop) < 0 && __propIsEnum.call(source, prop))
        target[prop] = source[prop];
    }
  return target;
};
function defaultFilter(value, item) {
  return item.value.toLowerCase().trim().includes(value.toLowerCase().trim());
}
const Autocomplete = forwardRef((_a, ref) => {
  var _b = _a, {
    className,
    style,
    sx,
    required = false,
    label,
    id,
    error,
    description,
    size = "sm",
    shadow = "sm",
    data,
    limit = 5,
    value,
    defaultValue,
    onChange,
    itemComponent = DefaultItem,
    onItemSubmit,
    onKeyDown,
    onFocus,
    onBlur,
    onClick,
    transition = "skew-up",
    transitionDuration = 0,
    initiallyOpened = false,
    transitionTimingFunction,
    wrapperProps,
    classNames,
    styles,
    filter = defaultFilter,
    nothingFound,
    onDropdownClose,
    onDropdownOpen,
    withinPortal,
    switchDirectionOnFlip = false,
    zIndex = getDefaultZIndex("popover"),
    dropdownPosition = "bottom"
  } = _b, others = __objRest(_b, [
    "className",
    "style",
    "sx",
    "required",
    "label",
    "id",
    "error",
    "description",
    "size",
    "shadow",
    "data",
    "limit",
    "value",
    "defaultValue",
    "onChange",
    "itemComponent",
    "onItemSubmit",
    "onKeyDown",
    "onFocus",
    "onBlur",
    "onClick",
    "transition",
    "transitionDuration",
    "initiallyOpened",
    "transitionTimingFunction",
    "wrapperProps",
    "classNames",
    "styles",
    "filter",
    "nothingFound",
    "onDropdownClose",
    "onDropdownOpen",
    "withinPortal",
    "switchDirectionOnFlip",
    "zIndex",
    "dropdownPosition"
  ]);
  const { classes } = useStyles({ size }, { classNames, styles, name: "Autocomplete" });
  const { margins, rest } = extractMargins(others);
  const [dropdownOpened, _setDropdownOpened] = useState(initiallyOpened);
  const [hovered, setHovered] = useState(-1);
  const [direction, setDirection] = useState("column");
  const inputRef = useRef(null);
  const uuid = useUuid(id);
  const [_value, handleChange] = useUncontrolled({
    value,
    defaultValue,
    finalValue: "",
    onChange,
    rule: (val) => typeof val === "string"
  });
  const setDropdownOpened = (opened) => {
    _setDropdownOpened(opened);
    const handler = opened ? onDropdownOpen : onDropdownClose;
    typeof handler === "function" && handler();
  };
  useDidUpdate(() => {
    setHovered(0);
  }, [_value]);
  const handleItemClick = (item) => {
    handleChange(item.value);
    typeof onItemSubmit === "function" && onItemSubmit(item);
    setDropdownOpened(false);
  };
  const formattedData = data.map((item) => typeof item === "string" ? { value: item } : item);
  const filteredData = filterData({ data: formattedData, value: _value, limit, filter });
  const handleInputKeydown = (event) => {
    typeof onKeyDown === "function" && onKeyDown(event);
    const isColumn = direction === "column";
    const handleNext = () => {
      setHovered((current) => current < filteredData.length - 1 ? current + 1 : current);
    };
    const handlePrevious = () => {
      setHovered((current) => current > 0 ? current - 1 : current);
    };
    switch (event.nativeEvent.code) {
      case "ArrowUp": {
        event.preventDefault();
        isColumn ? handlePrevious() : handleNext();
        break;
      }
      case "ArrowDown": {
        event.preventDefault();
        isColumn ? handleNext() : handlePrevious();
        break;
      }
      case "Enter": {
        if (dropdownOpened) {
          event.preventDefault();
        }
        if (filteredData[hovered] && dropdownOpened) {
          handleChange(filteredData[hovered].value);
          typeof onItemSubmit === "function" && onItemSubmit(filteredData[hovered]);
          setDropdownOpened(false);
        }
        break;
      }
      case "Escape": {
        if (dropdownOpened) {
          event.preventDefault();
          setDropdownOpened(false);
        }
      }
    }
  };
  const handleInputFocus = (event) => {
    typeof onFocus === "function" && onFocus(event);
    setDropdownOpened(true);
  };
  const handleInputBlur = (event) => {
    typeof onBlur === "function" && onBlur(event);
    setDropdownOpened(false);
  };
  const handleInputClick = (event) => {
    typeof onClick === "function" && onClick(event);
    setDropdownOpened(true);
  };
  const shouldRenderDropdown = dropdownOpened && (filteredData.length > 0 || filteredData.length === 0 && !!nothingFound);
  return /* @__PURE__ */ React.createElement(InputWrapper, __spreadValues(__spreadValues({
    required,
    id: uuid,
    label,
    error,
    description,
    size,
    className,
    classNames,
    styles,
    __staticSelector: "Autocomplete",
    sx,
    style
  }, margins), wrapperProps), /* @__PURE__ */ React.createElement("div", {
    className: classes.wrapper,
    role: "combobox",
    "aria-haspopup": "listbox",
    "aria-owns": `${uuid}-items`,
    "aria-controls": uuid,
    "aria-expanded": shouldRenderDropdown,
    onMouseLeave: () => setHovered(-1),
    tabIndex: -1
  }, /* @__PURE__ */ React.createElement(Input, __spreadProps(__spreadValues({}, rest), {
    "data-mantine-stop-propagation": dropdownOpened,
    required,
    ref: useMergedRef(ref, inputRef),
    id: uuid,
    type: "string",
    invalid: !!error,
    size,
    onKeyDown: handleInputKeydown,
    classNames,
    styles,
    __staticSelector: "Autocomplete",
    value: _value,
    onChange: (event) => {
      handleChange(event.currentTarget.value);
      setDropdownOpened(true);
    },
    onFocus: handleInputFocus,
    onBlur: handleInputBlur,
    onClick: handleInputClick,
    autoComplete: "nope",
    "aria-autocomplete": "list",
    "aria-controls": shouldRenderDropdown ? `${uuid}-items` : null,
    "aria-activedescendant": hovered !== -1 ? `${uuid}-${hovered}` : null
  })), /* @__PURE__ */ React.createElement(SelectDropdown, {
    mounted: shouldRenderDropdown,
    transition,
    transitionDuration,
    transitionTimingFunction,
    uuid,
    shadow,
    maxDropdownHeight: "auto",
    classNames,
    styles,
    __staticSelector: "Autocomplete",
    direction,
    onDirectionChange: setDirection,
    switchDirectionOnFlip,
    referenceElement: inputRef.current,
    withinPortal,
    zIndex,
    dropdownPosition
  }, /* @__PURE__ */ React.createElement(SelectItems, {
    data: filteredData,
    hovered,
    classNames,
    styles,
    uuid,
    __staticSelector: "Autocomplete",
    onItemHover: setHovered,
    onItemSelect: handleItemClick,
    itemComponent,
    size,
    nothingFound
  }))));
});
Autocomplete.displayName = "@mantine/core/Autocomplete";

export { Autocomplete, defaultFilter };
//# sourceMappingURL=Autocomplete.js.map
