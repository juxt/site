import React, { useState, useRef } from 'react';
import { useScrollIntoView } from '@mantine/hooks';
import { SelectScrollArea } from '../../Select/SelectScrollArea/SelectScrollArea.js';
import { Divider } from '../../Divider/Divider.js';
import useStyles from './RenderList.styles.js';
import { groupOptions } from '../../../utils/group-options/group-options.js';
import { UnstyledButton } from '../../Button/UnstyledButton/UnstyledButton.js';
import { Text } from '../../Text/Text.js';
import { TextInput } from '../../TextInput/TextInput.js';
import { ActionIcon } from '../../ActionIcon/ActionIcon.js';
import { PrevIcon } from '../../Pagination/icons/PrevIcon.js';
import { NextIcon } from '../../Pagination/icons/NextIcon.js';
import { FirstIcon } from '../../Pagination/icons/FirstIcon.js';
import { LastIcon } from '../../Pagination/icons/LastIcon.js';

const icons = {
  Prev: PrevIcon,
  Next: NextIcon,
  First: FirstIcon,
  Last: LastIcon
};
const rtlIons = {
  Next: PrevIcon,
  Prev: NextIcon,
  Last: FirstIcon,
  First: LastIcon
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
  const { classes, cx, theme } = useStyles({ reversed, native: listComponent !== SelectScrollArea }, { name: "TransferList", classNames, styles });
  const unGroupedItems = [];
  const groupedItems = [];
  const [query, setQuery] = useState("");
  const [hovered, setHovered] = useState(-1);
  const filteredData = data.filter((item) => filter(query, item)).slice(0, limit);
  const ListComponent = listComponent || "div";
  const Icons = theme.dir === "rtl" ? rtlIons : icons;
  const itemsRefs = useRef({});
  const sortedData = groupOptions({ data: filteredData });
  const { scrollIntoView, targetRef, scrollableRef } = useScrollIntoView({
    duration: 0,
    offset: 5,
    cancelable: false,
    isList: true
  });
  let groupName = null;
  sortedData.forEach((item, index) => {
    const itemComponent = /* @__PURE__ */ React.createElement(UnstyledButton, {
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
    }, /* @__PURE__ */ React.createElement(ItemComponent, {
      data: item,
      selected: selection.includes(item.value)
    }));
    if (!item.group) {
      unGroupedItems.push(itemComponent);
    } else {
      if (groupName !== item.group) {
        groupName = item.group;
        groupedItems.push(/* @__PURE__ */ React.createElement("div", {
          className: classes.separator,
          key: groupName
        }, /* @__PURE__ */ React.createElement(Divider, {
          classNames: { label: classes.separatorLabel },
          label: groupName
        })));
      }
      groupedItems.push(itemComponent);
    }
  });
  if (groupedItems.length > 0 && unGroupedItems.length > 0) {
    unGroupedItems.unshift(/* @__PURE__ */ React.createElement("div", {
      className: classes.separator
    }, /* @__PURE__ */ React.createElement(Divider, {
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
  return /* @__PURE__ */ React.createElement("div", {
    className: cx(classes.transferList, className)
  }, title && /* @__PURE__ */ React.createElement(Text, {
    weight: 500,
    className: classes.transferListTitle
  }, title), /* @__PURE__ */ React.createElement("div", {
    className: classes.transferListBody
  }, /* @__PURE__ */ React.createElement("div", {
    className: classes.transferListHeader
  }, /* @__PURE__ */ React.createElement(TextInput, {
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
  }), /* @__PURE__ */ React.createElement(ActionIcon, {
    variant: "default",
    size: 36,
    radius: 0,
    className: classes.transferListControl,
    disabled: selection.length === 0,
    onClick: onMove
  }, reversed ? /* @__PURE__ */ React.createElement(Icons.Prev, null) : /* @__PURE__ */ React.createElement(Icons.Next, null)), showTransferAll && /* @__PURE__ */ React.createElement(ActionIcon, {
    variant: "default",
    size: 36,
    radius: 0,
    className: classes.transferListControl,
    disabled: data.length === 0,
    onClick: onMoveAll
  }, reversed ? /* @__PURE__ */ React.createElement(Icons.First, null) : /* @__PURE__ */ React.createElement(Icons.Last, null))), /* @__PURE__ */ React.createElement(ListComponent, {
    ref: scrollableRef,
    onMouseLeave: () => setHovered(-1),
    className: classes.transferListItems,
    style: { height, position: "relative", overflowX: "hidden" }
  }, groupedItems.length > 0 || unGroupedItems.length > 0 ? /* @__PURE__ */ React.createElement(React.Fragment, null, groupedItems, unGroupedItems) : /* @__PURE__ */ React.createElement(Text, {
    color: "dimmed",
    size: "sm",
    align: "center",
    mt: "sm"
  }, nothingFound))));
}
RenderList.displayName = "@mantine/core/RenderList";

export { RenderList };
//# sourceMappingURL=RenderList.js.map
