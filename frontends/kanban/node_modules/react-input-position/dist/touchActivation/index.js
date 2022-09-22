"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = void 0;

var _doubleTap = _interopRequireDefault(require("./doubleTap"));

var _longTouch = _interopRequireDefault(require("./longTouch"));

var _tap = _interopRequireDefault(require("./tap"));

var _touch = _interopRequireDefault(require("./touch"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

var _default = {
  doubleTap: _doubleTap["default"],
  longTouch: _longTouch["default"],
  tap: _tap["default"],
  touch: _touch["default"]
};
exports["default"] = _default;