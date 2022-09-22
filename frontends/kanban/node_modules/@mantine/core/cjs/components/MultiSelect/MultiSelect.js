'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var React = require('react');
var hooks = require('@mantine/hooks');
var styles = require('@mantine/styles');
var DefaultValue = require('./DefaultValue/DefaultValue.js');
var DefaultItem = require('../Select/DefaultItem/DefaultItem.js');
var filterData = require('./filter-data/filter-data.js');
var getSelectRightSectionProps = require('../Select/SelectRightSection/get-select-right-section-props.js');
var SelectScrollArea = require('../Select/SelectScrollArea/SelectScrollArea.js');
var SelectItems = require('../Select/SelectItems/SelectItems.js');
var SelectDropdown = require('../Select/SelectDropdown/SelectDropdown.js');
var MultiSelect_styles = require('./MultiSelect.styles.js');
var groupOptions = require('../../utils/group-options/group-options.js');
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
function defaultFilter(value, selected, item) {
  if (selected) {
    return false;
  }
  return item.label.toLowerCase().trim().includes(value.toLowerCase().trim());
}
function defaultShouldCreate(query, data) {
  return !!query && !data.some((item) => item.value.toLowerCase() === query.toLowerCase());
}
const MultiSelect = React.forwardRef((_a, ref) => {
  var _b = _a, {
    className,
    style,
    required,
    label,
    description,
    size = "sm",
    error,
    classNames,
    styles: styles$1,
    wrapperProps,
    value,
    defaultValue,
    data,
    onChange,
    valueComponent: Value = DefaultValue.DefaultValue,
    itemComponent = DefaultItem.DefaultItem,
    id,
    transition = "pop-top-left",
    transitionDuration = 0,
    transitionTimingFunction,
    maxDropdownHeight = 220,
    shadow = "sm",
    nothingFound,
    onFocus,
    onBlur,
    searchable = false,
    placeholder,
    filter = defaultFilter,
    limit = Infinity,
    clearSearchOnChange = true,
    clearable = false,
    clearSearchOnBlur = false,
    clearButtonLabel,
    variant,
    onSearchChange,
    disabled = false,
    initiallyOpened = false,
    radius = "sm",
    icon,
    rightSection,
    rightSectionWidth,
    creatable = false,
    getCreateLabel,
    shouldCreate = defaultShouldCreate,
    onCreate,
    sx,
    dropdownComponent,
    onDropdownClose,
    onDropdownOpen,
    maxSelectedValues,
    withinPortal,
    switchDirectionOnFlip = false,
    zIndex = styles.getDefaultZIndex("popover"),
    selectOnBlur = false,
    name,
    dropdownPosition
  } = _b, others = __objRest(_b, [
    "className",
    "style",
    "required",
    "label",
    "description",
    "size",
    "error",
    "classNames",
    "styles",
    "wrapperProps",
    "value",
    "defaultValue",
    "data",
    "onChange",
    "valueComponent",
    "itemComponent",
    "id",
    "transition",
    "transitionDuration",
    "transitionTimingFunction",
    "maxDropdownHeight",
    "shadow",
    "nothingFound",
    "onFocus",
    "onBlur",
    "searchable",
    "placeholder",
    "filter",
    "limit",
    "clearSearchOnChange",
    "clearable",
    "clearSearchOnBlur",
    "clearButtonLabel",
    "variant",
    "onSearchChange",
    "disabled",
    "initiallyOpened",
    "radius",
    "icon",
    "rightSection",
    "rightSectionWidth",
    "creatable",
    "getCreateLabel",
    "shouldCreate",
    "onCreate",
    "sx",
    "dropdownComponent",
    "onDropdownClose",
    "onDropdownOpen",
    "maxSelectedValues",
    "withinPortal",
    "switchDirectionOnFlip",
    "zIndex",
    "selectOnBlur",
    "name",
    "dropdownPosition"
  ]);
  const { classes, cx, theme } = MultiSelect_styles['default']({ size, invalid: !!error }, { classNames, styles: styles$1, name: "MultiSelect" });
  const { margins, rest } = styles.extractMargins(others);
  const dropdownRef = React.useRef();
  const inputRef = React.useRef();
  const wrapperRef = React.useRef();
  const itemsRefs = React.useRef({});
  const uuid = hooks.useUuid(id);
  const [dropdownOpened, _setDropdownOpened] = React.useState(initiallyOpened);
  const [hovered, setHovered] = React.useState(-1);
  const [direction, setDirection] = React.useState("column");
  const [searchValue, setSearchValue] = React.useState("");
  const { scrollIntoView, targetRef, scrollableRef } = hooks.useScrollIntoView({
    duration: 0,
    offset: 5,
    cancelable: false,
    isList: true
  });
  const isCreatable = creatable && typeof getCreateLabel === "function";
  let createLabel = null;
  const setDropdownOpened = (opened) => {
    _setDropdownOpened(opened);
    const handler = opened ? onDropdownOpen : onDropdownClose;
    typeof handler === "function" && handler();
  };
  const handleSearchChange = (val) => {
    typeof onSearchChange === "function" && onSearchChange(val);
    setSearchValue(val);
  };
  const formattedData = data.map((item) => typeof item === "string" ? { label: item, value: item } : item);
  const sortedData = groupOptions.groupOptions({ data: formattedData });
  const [_value, setValue] = hooks.useUncontrolled({
    value,
    defaultValue,
    finalValue: [],
    rule: (val) => Array.isArray(val),
    onChange
  });
  const valuesOverflow = React.useRef(!!maxSelectedValues && maxSelectedValues < _value.length);
  const handleValueRemove = (_val) => {
    const newValue = _value.filter((val) => val !== _val);
    setValue(newValue);
    if (!!maxSelectedValues && newValue.length < maxSelectedValues) {
      valuesOverflow.current = false;
    }
  };
  const handleInputChange = (event) => {
    handleSearchChange(event.currentTarget.value);
    setDropdownOpened(true);
  };
  const handleInputFocus = (event) => {
    typeof onFocus === "function" && onFocus(event);
  };
  const filteredData = filterData.filterData({
    data: sortedData,
    searchable,
    searchValue,
    limit,
    filter,
    value: _value
  });
  const getNextIndex = (index, nextItem, compareFn) => {
    let i = index;
    while (compareFn(i)) {
      i = nextItem(i);
      if (!filteredData[i].disabled)
        return i;
    }
    return index;
  };
  hooks.useDidUpdate(() => {
    setHovered(getNextIndex(-1, (index) => index + 1, (index) => index < filteredData.length - 1));
  }, [searchValue]);
  hooks.useDidUpdate(() => {
    if (!disabled && _value.length >= data.length)
      setDropdownOpened(false);
  }, [_value]);
  const handleItemSelect = (item) => {
    clearSearchOnChange && handleSearchChange("");
    if (_value.includes(item.value)) {
      handleValueRemove(item.value);
    } else {
      setValue([..._value, item.value]);
      if (_value.length === maxSelectedValues - 1) {
        valuesOverflow.current = true;
        setDropdownOpened(false);
      }
      if (hovered === filteredData.length - 1) {
        setHovered(filteredData.length - 2);
      }
    }
    if (item.creatable) {
      typeof onCreate === "function" && onCreate(item.value);
    }
  };
  const handleInputBlur = (event) => {
    typeof onBlur === "function" && onBlur(event);
    if (selectOnBlur && filteredData[hovered] && dropdownOpened) {
      handleItemSelect(filteredData[hovered]);
    }
    clearSearchOnBlur && handleSearchChange("");
    setDropdownOpened(false);
  };
  const handleInputKeydown = (event) => {
    const isColumn = direction === "column";
    const handleNext = () => {
      setHovered((current) => {
        var _a2;
        const nextIndex = getNextIndex(current, (index) => index + 1, (index) => index < filteredData.length - 1);
        if (dropdownOpened) {
          targetRef.current = itemsRefs.current[(_a2 = filteredData[nextIndex]) == null ? void 0 : _a2.value];
          scrollIntoView({
            alignment: isColumn ? "end" : "start"
          });
        }
        return nextIndex;
      });
    };
    const handlePrevious = () => {
      setHovered((current) => {
        var _a2;
        const nextIndex = getNextIndex(current, (index) => index - 1, (index) => index > 0);
        if (dropdownOpened) {
          targetRef.current = itemsRefs.current[(_a2 = filteredData[nextIndex]) == null ? void 0 : _a2.value];
          scrollIntoView({
            alignment: isColumn ? "start" : "end"
          });
        }
        return nextIndex;
      });
    };
    switch (event.nativeEvent.code) {
      case "ArrowUp": {
        event.preventDefault();
        setDropdownOpened(true);
        isColumn ? handlePrevious() : handleNext();
        break;
      }
      case "ArrowDown": {
        event.preventDefault();
        setDropdownOpened(true);
        isColumn ? handleNext() : handlePrevious();
        break;
      }
      case "Enter": {
        event.preventDefault();
        if (filteredData[hovered] && dropdownOpened) {
          handleItemSelect(filteredData[hovered]);
        } else {
          setDropdownOpened(true);
        }
        break;
      }
      case "Space": {
        if (!searchable) {
          event.preventDefault();
          if (filteredData[hovered] && dropdownOpened) {
            handleItemSelect(filteredData[hovered]);
          } else {
            setDropdownOpened(true);
          }
        }
        break;
      }
      case "Backspace": {
        if (_value.length > 0 && searchValue.length === 0) {
          setValue(_value.slice(0, -1));
          setDropdownOpened(true);
        }
        break;
      }
      case "Escape": {
        setDropdownOpened(false);
      }
    }
  };
  const selectedItems = _value.map((val) => {
    let selectedItem = sortedData.find((item) => item.value === val && !item.disabled);
    if (!selectedItem && isCreatable) {
      selectedItem = {
        value: val,
        label: val
      };
    }
    return selectedItem;
  }).filter((val) => !!val).map((item) => /* @__PURE__ */ React__default.createElement(Value, __spreadProps(__spreadValues({}, item), {
    disabled,
    className: classes.value,
    onRemove: (event) => {
      if (dropdownOpened) {
        event.preventDefault();
        event.stopPropagation();
      }
      handleValueRemove(item.value);
      setDropdownOpened(true);
    },
    key: item.value,
    size,
    styles: styles$1,
    classNames,
    radius
  })));
  const handleClear = () => {
    var _a2;
    handleSearchChange("");
    setValue([]);
    (_a2 = inputRef.current) == null ? void 0 : _a2.focus();
  };
  if (isCreatable && shouldCreate(searchValue, sortedData)) {
    createLabel = getCreateLabel(searchValue);
    filteredData.push({ label: searchValue, value: searchValue, creatable: true });
  }
  const shouldRenderDropdown = filteredData.length > 0 || isCreatable || searchValue.length > 0 && !!nothingFound && filteredData.length === 0;
  return /* @__PURE__ */ React__default.createElement(InputWrapper.InputWrapper, __spreadValues(__spreadValues({
    required,
    id: uuid,
    label,
    error,
    description,
    size,
    className,
    style,
    classNames,
    styles: styles$1,
    __staticSelector: "MultiSelect",
    sx
  }, margins), wrapperProps), /* @__PURE__ */ React__default.createElement("div", {
    className: classes.wrapper,
    role: "combobox",
    "aria-haspopup": "listbox",
    "aria-owns": `${uuid}-items`,
    "aria-controls": uuid,
    "aria-expanded": dropdownOpened,
    onMouseLeave: () => setHovered(-1),
    tabIndex: -1,
    ref: wrapperRef
  }, /* @__PURE__ */ React__default.createElement(Input.Input, __spreadValues({
    __staticSelector: "MultiSelect",
    style: { overflow: "hidden" },
    component: "div",
    multiline: true,
    size,
    variant,
    disabled,
    invalid: !!error,
    required,
    radius,
    icon,
    onMouseDown: (event) => {
      var _a2;
      event.preventDefault();
      !disabled && !valuesOverflow.current && setDropdownOpened(!dropdownOpened);
      (_a2 = inputRef.current) == null ? void 0 : _a2.focus();
    },
    classNames: __spreadProps(__spreadValues({}, classNames), {
      input: cx({ [classes.input]: !searchable }, classNames == null ? void 0 : classNames.input)
    })
  }, getSelectRightSectionProps.getSelectRightSectionProps({
    theme,
    rightSection,
    rightSectionWidth,
    styles: styles$1,
    size,
    shouldClear: clearable && _value.length > 0,
    clearButtonLabel,
    onClear: handleClear,
    error,
    disabled
  })), /* @__PURE__ */ React__default.createElement("div", {
    className: classes.values
  }, selectedItems, /* @__PURE__ */ React__default.createElement("input", __spreadValues({
    ref: hooks.useMergedRef(ref, inputRef),
    type: "text",
    id: uuid,
    className: cx(classes.searchInput, {
      [classes.searchInputPointer]: !searchable,
      [classes.searchInputInputHidden]: !dropdownOpened && _value.length > 0 || !searchable && _value.length > 0,
      [classes.searchInputEmpty]: _value.length === 0
    }),
    onKeyDown: handleInputKeydown,
    value: searchValue,
    onChange: handleInputChange,
    onFocus: handleInputFocus,
    onBlur: handleInputBlur,
    readOnly: !searchable || valuesOverflow.current,
    placeholder: _value.length === 0 ? placeholder : void 0,
    disabled,
    "data-mantine-stop-propagation": dropdownOpened,
    name,
    autoComplete: "nope"
  }, rest)))), /* @__PURE__ */ React__default.createElement(SelectDropdown.SelectDropdown, {
    mounted: dropdownOpened && shouldRenderDropdown,
    transition,
    transitionDuration,
    transitionTimingFunction,
    uuid,
    shadow,
    maxDropdownHeight,
    classNames,
    styles: styles$1,
    ref: hooks.useMergedRef(dropdownRef, scrollableRef),
    __staticSelector: "MultiSelect",
    dropdownComponent: dropdownComponent || SelectScrollArea.SelectScrollArea,
    referenceElement: wrapperRef.current,
    direction,
    onDirectionChange: setDirection,
    switchDirectionOnFlip,
    withinPortal,
    zIndex,
    dropdownPosition
  }, /* @__PURE__ */ React__default.createElement(SelectItems.SelectItems, {
    data: filteredData,
    hovered,
    classNames,
    styles: styles$1,
    uuid,
    __staticSelector: "MultiSelect",
    onItemHover: setHovered,
    onItemSelect: handleItemSelect,
    itemsRefs,
    itemComponent,
    size,
    nothingFound,
    creatable: creatable && !!createLabel,
    createLabel
  }))));
});
MultiSelect.displayName = "@mantine/core/MultiSelect";

exports.MultiSelect = MultiSelect;
exports.defaultFilter = defaultFilter;
exports.defaultShouldCreate = defaultShouldCreate;
//# sourceMappingURL=MultiSelect.js.map
