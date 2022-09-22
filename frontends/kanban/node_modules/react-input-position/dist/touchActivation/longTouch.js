"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = void 0;

function touchStart(e) {
  this.touched = true;
  this.justTouched = true;
  clearTimeout(this.longTouchTimer);
  var touch = e.touches[0];
  var position = {
    x: touch.clientX,
    y: touch.clientY
  };
  this.longTouchStartRef = position.x + position.y;
  this.startLongTouchTimer(position);
}

function touchEnd(e) {
  if (e.cancelable) e.preventDefault();
  this.touched = false;
  this.justTouched = false;
}

function touchMove(e) {
  var touch = e.touches[0];
  var position = {
    x: touch.clientX,
    y: touch.clientY
  };
  var end = position.x + position.y;
  var diff = Math.abs(this.longTouchStartRef - end);

  if (diff > this.props.longTouchMoveLimit) {
    clearTimeout(this.longTouchTimer);
  }

  if (!this.getState().active) return;
  if (e.cancelable) e.preventDefault();
  this.setPosition(position, this.touched && !this.justTouched);
  this.justTouched = false;
}

function touchCancel() {
  this.deactivate();
}

var _default = {
  touchStart: touchStart,
  touchEnd: touchEnd,
  touchMove: touchMove,
  touchCancel: touchCancel
};
exports["default"] = _default;