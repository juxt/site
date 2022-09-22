'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var React = require('react');
var MediaQuery_styles = require('./MediaQuery.styles.js');

function _interopDefaultLegacy (e) { return e && typeof e === 'object' && 'default' in e ? e['default'] : e; }

var React__default = /*#__PURE__*/_interopDefaultLegacy(React);

function MediaQuery({
  children,
  smallerThan,
  largerThan,
  query,
  styles,
  className
}) {
  var _a;
  const { classes, cx } = MediaQuery_styles['default']({ smallerThan, largerThan, query, styles }, { name: "MediaQuery" });
  const child = React.Children.only(children);
  return React__default.cloneElement(child, {
    className: cx(classes.media, (_a = child.props) == null ? void 0 : _a.className, className)
  });
}
MediaQuery.displayName = "@mantine/core/MediaQuery";

exports.MediaQuery = MediaQuery;
//# sourceMappingURL=MediaQuery.js.map
