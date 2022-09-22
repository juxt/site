"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = void 0;

function touchStart(e) {
  this.touched = true;
  this.justTouched = true;
  var touch = e.touches[0];
  var position = {
    x: touch.clientX,
    y: touch.clientY
  };
  this.activate(position);
}

function touchEnd(e) {
  if (e.cancelable) e.preventDefault();
  this.touched = false;
  this.justTouched = false;
  this.deactivate();
}

function touchMove(e) {
  if (!this.getState().active) return;
  if (e.cancelable) e.preventDefault();
  var touch = e.touches[0];
  var position = {
    x: touch.clientX,
    y: touch.clientY
  };
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