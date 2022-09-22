'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var React = require('react');
var hooks = require('@mantine/hooks');
var SelectScrollArea = require('../../Select/SelectScrollArea/SelectScrollArea.js');
var Divider = require('../../Divider/Divider.js');
var RenderList_styles = require('./RenderList.styles.js');
var groupOptions = require('../../../utils/group-options/group-options.js');
var UnstyledButton = require('../../Button/UnstyledButton/UnstyledButton.js');
var Text = require('../../Text/Text.js');
var TextInput = require('../../TextInput/TextInput.js');
var ActionIcon = require('../../ActionIcon/ActionIcon.js');
var PrevIcon = require('../../Pagination/icons/PrevIcon.js');
var NextIcon = require('../../Pagination/icons/NextIcon.js');
var FirstIcon = require('../../Pagination/icons/FirstIcon.js');
var LastIcon = require('../../Pagination/icons/LastIcon.js');

function _interopDefaultLegacy (e) { return e && typeof e === 'object' && 'default' in e ? e['default'] : e; }

var React__default = /*#__PURE__*/_interopDefaultLegacy(React);

const icons = {
  Prev: PrevIcon.PrevIcon,
  Next: NextIcon.NextIcon,
  First: FirstIcon.FirstIcon,
  Last: LastIcon.LastIcon
};
const rtlIons = {
  Next: PrevIcon.PrevIcon,
  Prev: NextIcon.NextIcon,
  Last: FirstIcon.FirstIcon,
  First: LastIcon.LastIcon
};
function RenderList({
  className,
  data,
  onSelect,
  selection,
  itemComponent: ItemComponent,
  listComponent,
  searchPlaceholder,
  filter,
  nothingFound,
  title,
  showTransferAll,
  reversed,
  onMoveAll,
  onMove,
  height,
  classNames,
  styles,
  limit
}) {
  const { classes, cx, theme } = RenderList_styles['default']({ reversed, native: listComponent !== SelectScrollArea.SelectScrollArea }, { name: "TransferList", classNames, styles });
  const unGroupedItems = [];
  const groupedItems = [];
  const [query, setQuery] = React.useState("");
  const [hovered, setHovered] = React.useState(-1);
  const filteredData = data.filter((item) => filter(query, item)).slice(0, limit);
  const ListComponent = listComponent || "div";
  const Icons = theme.dir === "rtl" ? rtlIons : icons;
  const itemsRefs = React.useRef({});
  const sortedData = groupOptions.groupOptions({ data: filteredData });
  const { scrollIntoView, targetRef, scrollableRef } = hooks.useScrollIntoView({
    duration: 0,
    offset: 5,
    cancelable: false,
    isList: true
  });
  let groupName = null;
  sortedData.forEach((item, index) => {
    const itemComponent = /* @__PURE__ */ React__default.createElement(UnstyledButton.UnstyledButton, {
      tabIndex: -1,
      onClick: () => onSelect(item.value),
      key: item.value,
      onMouseEnter: () => setHovered(index),
      className: cx(classes.transferListItem, {
        [classes.transferListItemHovered]: index === hovered
      }),
      ref: (node) => {
        if (itemsRefs && itemsRefs.current) {
          itemsRefs.current[item.value] = node;
        }
      }
    }, /* @__PURE__ */ React__default.createElement(ItemComponent, {
      data: item,
      selected: selection.includes(item.value)
    }));
    if (!item.group) {
      unGroupedItems.push(itemComponent);
    } else {
      if (groupName !== item.group) {
        groupName = item.group;
        groupedItems.push(/* @__PURE__ */ React__default.createElement("div", {
          className: classes.separator,
          key: groupName
        }, /* @__PURE__ */ React__default.createElement(Divider.Divider, {
          classNames: { label: classes.separatorLabel },
          label: groupName
        })));
      }
      groupedItems.push(itemComponent);
    }
  });
  if (groupedItems.length > 0 && unGroupedItems.length > 0) {
    unGroupedItems.unshift(/* @__PURE__ */ React__default.createElement("div", {
      className: classes.separator
    }, /* @__PURE__ */ React__default.createElement(Divider.Divider, {
      classNames: { label: classes.separatorLabel }
    })));
  }
  const handleSearchKeydown = (event) => {
    switch (event.code) {
      case "Enter": {
        event.preventDefault();
        if (filteredData[hovered]) {
          onSelect(filteredData[hovered].value);
        }
        break;
      }
      case "ArrowDown": {
        event.preventDefault();
        setHovered((current) => {
          var _a;
          const nextIndex = current < filteredData.length - 1 ? current + 1 : current;
          targetRef.current = itemsRefs.current[(_a = filteredData[nextIndex]) == null ? void 0 : _a.value];
          scrollIntoView({
            alignment: "end"
          });
          return nextIndex;
        });
        break;
      }
      case "ArrowUp": {
        event.preventDefault();
        setHovered((current) => {
          var _a;
          const nextIndex = current > 0 ? current - 1 : current;
          targetRef.current = itemsRefs.current[(_a = filteredData[nextIndex]) == null ? void 0 : _a.value];
          scrollIntoView({
            alignment: "start"
          });
          return nextIndex;
        });
      }
    }
  };
  return /* @__PURE__ */ React__default.createElement("div", {
    className: cx(classes.transferList, className)
  }, title && /* @__PURE__ */ React__default.createElement(Text.Text, {
    weight: 500,
    className: classes.transferListTitle
  }, title), /* @__PURE__ */ React__default.createElement("div", {
    className: classes.transferListBody
  }, /* @__PURE__ */ React__default.createElement("div", {
    className: classes.transferListHeader
  }, /* @__PURE__ */ React__default.createElement(TextInput.TextInput, {
    value: query,
    onChange: (event) => {
      setQuery(event.currentTarget.value);
      setHovered(0);
    },
    onFocus: () => setHovered(0),
    onBlur: () => setHovered(-1),
    placeholder: searchPlaceholder,
    radius: 0,
    onKeyDown: handleSearchKeydown,
    sx: { flex: 1 },
    classNames: { input: classes.transferListSearch }
  }), /* @__PURE__ */ React__default.createElement(ActionIcon.ActionIcon, {
    variant: "default",
    size: 36,
    radius: 0,
    className: classes.transferListControl,
    disabled: selection.length === 0,
    onClick: onMove
  }, reversed ? /* @__PURE__ */ React__default.createElement(Icons.Prev, null) : /* @__PURE__ */ React__default.createElement(Icons.Next, null)), showTransferAll && /* @__PURE__ */ React__default.createElement(ActionIcon.ActionIcon, {
    variant: "default",
    size: 36,
    radius: 0,
    className: classes.transferListControl,
    disabled: data.length === 0,
    onClick: onMoveAll
  }, reversed ? /* @__PURE__ */ React__default.createElement(Icons.First, null) : /* @__PURE__ */ React__default.createElement(Icons.Last, null))), /* @__PURE__ */ React__default.createElement(ListComponent, {
    ref: scrollableRef,
    onMouseLeave: () => setHovered(-1),
    className: classes.transferListItems,
    style: { height, position: "relative", overflowX: "hidden" }
  }, groupedItems.length > 0 || unGroupedItems.length > 0 ? /* @__PURE__ */ React__default.createElement(React__default.Fragment, null, groupedItems, unGroupedItems) : /* @__PURE__ */ React__default.createElement(Text.Text, {
    color: "dimmed",
    size: "sm",
    align: "center",
    mt: "sm"
  }, nothingFound))));
}
RenderList.displayName = "@mantine/core/RenderList";

exports.RenderList = RenderList;
//# sourceMappingURL=RenderList.js.map
