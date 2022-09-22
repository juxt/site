"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = void 0;

var _utils = _interopRequireDefault(require("../utils"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function mouseDown() {
  this.mouseDown = true;
}

function mouseUp() {
  this.mouseDown = false;
}

function mouseMove(e) {
  var position = {
    x: e.clientX,
    y: e.clientY
  };

  if (!this.getState().active) {
    return this.activate(position);
  }

  this.setPosition(position, this.mouseDown);
}

function mouseEnter(e) {
  var position = {
    x: e.clientX,
    y: e.clientY
  };
  this.activate(position);
}

function mouseLeave() {
  this.deactivate();
  this.mouseDown = false;
}

var _default = {
  mouseDown: mouseDown,
  mouseUp: mouseUp,
  mouseMove: mouseMove,
  mouseEnter: mouseEnter,
  mouseLeave: mouseLeave,
  dragStart: _utils["default"].preventDefault
};
exports["default"] = _default;