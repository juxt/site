import React, { forwardRef, useState, useRef, useEffect } from 'react';
import { useUuid, useScrollIntoView, useUncontrolled, useDidUpdate, useMergedRef } from '@mantine/hooks';
import { getDefaultZIndex, extractMargins } from '@mantine/styles';
import { SelectScrollArea } from './SelectScrollArea/SelectScrollArea.js';
import { DefaultItem } from './DefaultItem/DefaultItem.js';
import { getSelectRightSectionProps } from './SelectRightSection/get-select-right-section-props.js';
import { SelectItems } from './SelectItems/SelectItems.js';
import { SelectDropdown } from './SelectDropdown/SelectDropdown.js';
import { filterData } from './filter-data/filter-data.js';
import useStyles from './Select.styles.js';
import { groupOptions } from '../../utils/group-options/group-options.js';
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
  return item.label.toLowerCase().trim().includes(value.toLowerCase().trim());
}
function defaultShouldCreate(query, data) {
  return !!query && !data.some((item) => item.label.toLowerCase() === query.toLowerCase());
}
const Select = forwardRef((_a, ref) => {
  var _b = _a, {
    className,
    style,
    required = false,
    label,
    id,
    error,
    description,
    size = "sm",
    shadow = "sm",
    data,
    value,
    defaultValue,
    onChange,
    itemComponent = DefaultItem,
    onKeyDown,
    onBlur,
    onFocus,
    transition = "fade",
    transitionDuration = 0,
    initiallyOpened = false,
    transitionTimingFunction,
    wrapperProps,
    classNames,
    styles,
    filter = defaultFilter,
    maxDropdownHeight = 220,
    searchable = false,
    clearable = false,
    nothingFound,
    clearButtonLabel,
    limit = Infinity,
    disabled = false,
    onSearchChange,
    rightSection,
    rightSectionWidth,
    creatable = false,
    getCreateLabel,
    shouldCreate = defaultShouldCreate,
    selectOnBlur = false,
    onCreate,
    sx,
    dropdownComponent,
    onDropdownClose,
    onDropdownOpen,
    withinPortal,
    switchDirectionOnFlip = false,
    zIndex = getDefaultZIndex("popover"),
    name,
    dropdownPosition,
    allowDeselect
  } = _b, others = __objRest(_b, [
    "className",
    "style",
    "required",
    "label",
    "id",
    "error",
    "description",
    "size",
    "shadow",
    "data",
    "value",
    "defaultValue",
    "onChange",
    "itemComponent",
    "onKeyDown",
    "onBlur",
    "onFocus",
    "transition",
    "transitionDuration",
    "initiallyOpened",
    "transitionTimingFunction",
    "wrapperProps",
    "classNames",
    "styles",
    "filter",
    "maxDropdownHeight",
    "searchable",
    "clearable",
    "nothingFound",
    "clearButtonLabel",
    "limit",
    "disabled",
    "onSearchChange",
    "rightSection",
    "rightSectionWidth",
    "creatable",
    "getCreateLabel",
    "shouldCreate",
    "selectOnBlur",
    "onCreate",
    "sx",
    "dropdownComponent",
    "onDropdownClose",
    "onDropdownOpen",
    "withinPortal",
    "switchDirectionOnFlip",
    "zIndex",
    "name",
    "dropdownPosition",
    "allowDeselect"
  ]);
  const { classes, cx, theme } = useStyles();
  const { margins, rest } = extractMargins(others);
  const [dropdownOpened, _setDropdownOpened] = useState(initiallyOpened);
  const [hovered, setHovered] = useState(-1);
  const inputRef = useRef();
  const dropdownRef = useRef();
  const itemsRefs = useRef({});
  const [direction, setDirection] = useState("column");
  const isColumn = direction === "column";
  const uuid = useUuid(id);
  const { scrollIntoView, targetRef, scrollableRef } = useScrollIntoView({
    duration: 0,
    offset: 5,
    cancelable: false,
    isList: true
  });
  const isDeselectable = allowDeselect === void 0 ? clearable : allowDeselect;
  const setDropdownOpened = (opened) => {
    if (dropdownOpened !== opened) {
      _setDropdownOpened(opened);
      const handler = opened ? onDropdownOpen : onDropdownClose;
      typeof handler === "function" && handler();
    }
  };
  const isCreatable = creatable && typeof getCreateLabel === "function";
  let createLabel = null;
  const formattedData = data.map((item) => typeof item === "string" ? { label: item, value: item } : item);
  const sortedData = groupOptions({ data: formattedData });
  const [_value, handleChange, inputMode] = useUncontrolled({
    value,
    defaultValue,
    finalValue: null,
    onChange,
    rule: (val) => typeof val === "string" || val === null
  });
  const selectedValue = sortedData.find((item) => item.value === _value);
  const [inputValue, setInputValue] = useState((selectedValue == null ? void 0 : selectedValue.label) || "");
  const handleSearchChange = (val) => {
    setInputValue(val);
    if (searchable && typeof onSearchChange === "function") {
      onSearchChange(val);
    }
  };
  const handleClear = () => {
    var _a2;
    handleChange(null);
    if (inputMode === "uncontrolled") {
      handleSearchChange("");
    }
    (_a2 = inputRef.current) == null ? void 0 : _a2.focus();
  };
  useEffect(() => {
    const newSelectedValue = sortedData.find((item) => item.value === _value);
    if (newSelectedValue) {
      handleSearchChange(newSelectedValue.label);
    } else if (!isCreatable || !_value) {
      handleSearchChange("");
    }
  }, [_value]);
  useEffect(() => {
    if (selectedValue && (!searchable || !dropdownOpened)) {
      handleSearchChange(selectedValue.label);
    }
  }, [selectedValue == null ? void 0 : selectedValue.label]);
  const handleItemSelect = (item) => {
    if (isDeselectable && (selectedValue == null ? void 0 : selectedValue.value) === item.value) {
      handleChange(null);
      setDropdownOpened(false);
    } else {
      handleChange(item.value);
      if (item.creatable) {
        typeof onCreate === "function" && onCreate(item.value);
      }
      if (inputMode === "uncontrolled") {
        handleSearchChange(item.label);
      }
      setHovered(-1);
      setDropdownOpened(false);
      inputRef.current.focus();
    }
  };
  const filteredData = filterData({
    data: sortedData,
    searchable,
    limit,
    searchValue: inputValue,
    filter
  });
  if (isCreatable && shouldCreate(inputValue, filteredData)) {
    createLabel = getCreateLabel(inputValue);
    filteredData.push({ label: inputValue, value: inputValue, creatable: true });
  }
  const getNextIndex = (index, nextItem, compareFn) => {
    let i = index;
    while (compareFn(i)) {
      i = nextItem(i);
      if (!filteredData[i].disabled)
        return i;
    }
    return index;
  };
  useDidUpdate(() => {
    setHovered(getNextIndex(-1, (index) => index + 1, (index) => index < filteredData.length - 1));
  }, [inputValue]);
  const selectedItemIndex = _value ? filteredData.findIndex((el) => el.value === _value) : 0;
  const handlePrevious = () => {
    setHovered((current) => {
      var _a2;
      const nextIndex = getNextIndex(current, (index) => index - 1, (index) => index > 0);
      targetRef.current = itemsRefs.current[(_a2 = filteredData[nextIndex]) == null ? void 0 : _a2.value];
      scrollIntoView({ alignment: isColumn ? "start" : "end" });
      return nextIndex;
    });
  };
  const handleNext = () => {
    setHovered((current) => {
      var _a2;
      const nextIndex = getNextIndex(current, (index) => index + 1, (index) => index < filteredData.length - 1);
      targetRef.current = itemsRefs.current[(_a2 = filteredData[nextIndex]) == null ? void 0 : _a2.value];
      scrollIntoView({ alignment: isColumn ? "end" : "start" });
      return nextIndex;
    });
  };
  const scrollSelectedItemIntoView = () => window.setTimeout(() => {
    var _a2;
    targetRef.current = itemsRefs.current[(_a2 = filteredData[selectedItemIndex]) == null ? void 0 : _a2.value];
    scrollIntoView({ alignment: isColumn ? "end" : "start" });
  }, 0);
  const handleInputKeydown = (event) => {
    typeof onKeyDown === "function" && onKeyDown(event);
    switch (event.nativeEvent.code) {
      case "ArrowUp": {
        event.preventDefault();
        if (!dropdownOpened) {
          setHovered(selectedItemIndex);
          setDropdownOpened(true);
          scrollSelectedItemIntoView();
        } else {
          isColumn ? handlePrevious() : handleNext();
        }
        break;
      }
      case "ArrowDown": {
        event.preventDefault();
        if (!dropdownOpened) {
          setHovered(selectedItemIndex);
          setDropdownOpened(true);
          scrollSelectedItemIntoView();
        } else {
          isColumn ? handleNext() : handlePrevious();
        }
        break;
      }
      case "Escape": {
        event.preventDefault();
        setDropdownOpened(false);
        setHovered(-1);
        break;
      }
      case "Space": {
        if (!searchable) {
          if (filteredData[hovered] && dropdownOpened) {
            event.preventDefault();
            handleItemSelect(filteredData[hovered]);
          } else {
            setDropdownOpened(!dropdownOpened);
            setHovered(selectedItemIndex);
            scrollSelectedItemIntoView();
          }
        }
        break;
      }
      case "Enter": {
        event.preventDefault();
        if (filteredData[hovered] && dropdownOpened) {
          event.preventDefault();
          handleItemSelect(filteredData[hovered]);
        } else {
          setDropdownOpened(true);
          setHovered(selectedItemIndex);
          scrollSelectedItemIntoView();
        }
      }
    }
  };
  const handleInputBlur = (event) => {
    typeof onBlur === "function" && onBlur(event);
    const selected = sortedData.find((item) => item.value === _value);
    if (selectOnBlur && filteredData[hovered] && dropdownOpened) {
      handleItemSelect(filteredData[hovered]);
    }
    handleSearchChange((selected == null ? void 0 : selected.label) || "");
    setDropdownOpened(false);
  };
  const handleInputFocus = (event) => {
    typeof onFocus === "function" && onFocus(event);
    if (searchable) {
      setDropdownOpened(true);
      scrollSelectedItemIntoView();
    }
  };
  const handleInputChange = (event) => {
    handleSearchChange(event.currentTarget.value);
    if (clearable && event.currentTarget.value === "") {
      handleChange(null);
    }
    setHovered(0);
    setDropdownOpened(true);
  };
  const handleInputClick = () => {
    let dropdownOpen = true;
    if (!searchable) {
      dropdownOpen = !dropdownOpened;
    }
    setDropdownOpened(dropdownOpen);
    if (_value && dropdownOpen) {
      setHovered(selectedItemIndex);
      scrollSelectedItemIntoView();
    }
  };
  const shouldShowDropdown = dropdownOpened && (searchable && !creatable ? filteredData.length > 0 || !!nothingFound : dropdownOpened);
  return /* @__PURE__ */ React.createElement(InputWrapper, __spreadValues(__spreadValues({
    required,
    id: uuid,
    label,
    error,
    description,
    size,
    className,
    style,
    classNames,
    styles,
    __staticSelector: "Select",
    sx
  }, margins), wrapperProps), /* @__PURE__ */ React.createElement("div", {
    role: "combobox",
    "aria-haspopup": "listbox",
    "aria-owns": `${uuid}-items`,
    "aria-controls": uuid,
    "aria-expanded": shouldShowDropdown,
    onMouseLeave: () => setHovered(-1),
    tabIndex: -1
  }, /* @__PURE__ */ React.createElement(Input, __spreadValues(__spreadProps(__spreadValues({
    autoComplete: "nope"
  }, rest), {
    type: "text",
    required,
    ref: useMergedRef(ref, inputRef),
    id: uuid,
    invalid: !!error,
    size,
    onKeyDown: handleInputKeydown,
    __staticSelector: "Select",
    value: inputValue,
    onChange: handleInputChange,
    "aria-autocomplete": "list",
    "aria-controls": shouldShowDropdown ? `${uuid}-items` : null,
    "aria-activedescendant": hovered !== -1 ? `${uuid}-${hovered}` : null,
    onClick: handleInputClick,
    onBlur: handleInputBlur,
    onFocus: handleInputFocus,
    readOnly: !searchable,
    disabled,
    "data-mantine-stop-propagation": shouldShowDropdown,
    name,
    classNames: __spreadProps(__spreadValues({}, classNames), {
      input: cx({ [classes.input]: !searchable }, classNames == null ? void 0 : classNames.input)
    })
  }), getSelectRightSectionProps({
    theme,
    rightSection,
    rightSectionWidth,
    styles,
    size,
    shouldClear: clearable && !!selectedValue,
    clearButtonLabel,
    onClear: handleClear,
    error,
    disabled
  }))), /* @__PURE__ */ React.createElement(SelectDropdown, {
    referenceElement: inputRef.current,
    mounted: shouldShowDropdown,
    transition,
    transitionDuration,
    transitionTimingFunction,
    uuid,
    shadow,
    maxDropdownHeight,
    classNames,
    styles,
    ref: useMergedRef(dropdownRef, scrollableRef),
    __staticSelector: "Select",
    dropdownComponent: dropdownComponent || SelectScrollArea,
    direction,
    onDirectionChange: setDirection,
    switchDirectionOnFlip,
    withinPortal,
    zIndex,
    dropdownPosition
  }, /* @__PURE__ */ React.createElement(SelectItems, {
    data: filteredData,
    hovered,
    classNames,
    styles,
    isItemSelected: (val) => val === _value,
    uuid,
    __staticSelector: "Select",
    onItemHover: setHovered,
    onItemSelect: handleItemSelect,
    itemsRefs,
    itemComponent,
    size,
    nothingFound,
    creatable: isCreatable && !!createLabel,
    createLabel
  }))));
});
Select.displayName = "@mantine/core/Select";

export { Select, defaultFilter, defaultShouldCreate };
//# sourceMappingURL=Select.js.map
