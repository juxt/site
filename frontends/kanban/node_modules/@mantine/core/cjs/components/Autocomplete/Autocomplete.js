'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var React = require('react');
var hooks = require('@mantine/hooks');
var styles = require('@mantine/styles');
var SelectDropdown = require('../Select/SelectDropdown/SelectDropdown.js');
var SelectItems = require('../Select/SelectItems/SelectItems.js');
var DefaultItem = require('../Select/DefaultItem/DefaultItem.js');
var filterData = require('./filter-data/filter-data.js');
var Autocomplete_styles = require('./Autocomplete.styles.js');
var InputWrapper = require('../InputWrapper/InputWrapper.js');
var Input = require('../Input/Input.js');

function _interopDefaultLegacy (e) { return e && typeof e === 'object' && 'default' in e ? e['default'] : e; }

var React__default = /*#__PURE__*/_interopDefaultLegacy(React);

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
const Autocomplete = React.forwardRef((_a, ref) => {
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
    itemComponent = DefaultItem.DefaultItem,
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
    styles: styles$1,
    filter = defaultFilter,
    nothingFound,
    onDropdownClose,
    onDropdownOpen,
    withinPortal,
    switchDirectionOnFlip = false,
    zIndex = styles.getDefaultZIndex("popover"),
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
  const { classes } = Autocomplete_styles['default']({ size }, { classNames, styles: styles$1, name: "Autocomplete" });
  const { margins, rest } = styles.extractMargins(others);
  const [dropdownOpened, _setDropdownOpened] = React.useState(initiallyOpened);
  const [hovered, setHovered] = React.useState(-1);
  const [direction, setDirection] = React.useState("column");
  const inputRef = React.useRef(null);
  const uuid = hooks.useUuid(id);
  const [_value, handleChange] = hooks.useUncontrolled({
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
  hooks.useDidUpdate(() => {
    setHovered(0);
  }, [_value]);
  const handleItemClick = (item) => {
    handleChange(item.value);
    typeof onItemSubmit === "function" && onItemSubmit(item);
    setDropdownOpened(false);
  };
  const formattedData = data.map((item) => typeof item === "string" ? { value: item } : item);
  const filteredData = filterData.filterData({ data: formattedData, value: _value, limit, filter });
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
  return /* @__PURE__ */ React__default.createElement(InputWrapper.InputWrapper, __spreadValues(__spreadValues({
    required,
    id: uuid,
    label,
    error,
    description,
    size,
    className,
    classNames,
    styles: styles$1,
    __staticSelector: "Autocomplete",
    sx,
    style
  }, margins), wrapperProps), /* @__PURE__ */ React__default.createElement("div", {
    className: classes.wrapper,
    role: "combobox",
    "aria-haspopup": "listbox",
    "aria-owns": `${uuid}-items`,
    "aria-controls": uuid,
    "aria-expanded": shouldRenderDropdown,
    onMouseLeave: () => setHovered(-1),
    tabIndex: -1
  }, /* @__PURE__ */ React__default.createElement(Input.Input, __spreadProps(__spreadValues({}, rest), {
    "data-mantine-stop-propagation": dropdownOpened,
    required,
    ref: hooks.useMergedRef(ref, inputRef),
    id: uuid,
    type: "string",
    invalid: !!error,
    size,
    onKeyDown: handleInputKeydown,
    classNames,
    styles: styles$1,
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
  })), /* @__PURE__ */ React__default.createElement(SelectDropdown.SelectDropdown, {
    mounted: shouldRenderDropdown,
    transition,
    transitionDuration,
    transitionTimingFunction,
    uuid,
    shadow,
    maxDropdownHeight: "auto",
    classNames,
    styles: styles$1,
    __staticSelector: "Autocomplete",
    direction,
    onDirectionChange: setDirection,
    switchDirectionOnFlip,
    referenceElement: inputRef.current,
    withinPortal,
    zIndex,
    dropdownPosition
  }, /* @__PURE__ */ React__default.createElement(SelectItems.SelectItems, {
    data: filteredData,
    hovered,
    classNames,
    styles: styles$1,
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

exports.Autocomplete = Autocomplete;
exports.defaultFilter = defaultFilter;
//# sourceMappingURL=Autocomplete.js.map
