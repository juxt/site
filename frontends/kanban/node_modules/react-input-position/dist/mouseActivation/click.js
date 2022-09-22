"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = void 0;

var _utils = _interopRequireDefault(require("../utils"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function mouseDown(e) {
  this.mouseDown = true;
  this.clickMoveStartRef = e.clientX + e.clientY;
}

function mouseUp(e) {
  if (!this.mouseDown) return;
  this.mouseDown = false;
  var position = {
    x: e.clientX,
    y: e.clientY
  };
  var clickMoveEnd = position.x + position.y;
  var diff = Math.abs(this.clickMoveStartRef - clickMoveEnd);

  if (diff < this.props.clickMoveLimit) {
    this.toggleActive(position);
  }
}

function mouseMove(e) {
  var position = {
    x: e.clientX,
    y: e.clientY
  };

  if (!this.getState().active) {
    return this.setPassivePosition(position);
  }

  this.setPosition(position, this.mouseDown);
}

function mouseLeave() {
  this.mouseDown = false;
}

var _default = {
  mouseDown: mouseDown,
  mouseUp: mouseUp,
  mouseMove: mouseMove,
  mouseLeave: mouseLeave,
  dragStart: _utils["default"].preventDefault
};
exports["default"] = _default;