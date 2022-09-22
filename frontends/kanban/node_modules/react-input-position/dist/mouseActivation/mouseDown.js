"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = void 0;

var _utils = _interopRequireDefault(require("../utils"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function mouseDown(e) {
  var position = {
    x: e.clientX,
    y: e.clientY
  };
  this.activate(position);
}

function mouseUp() {
  this.deactivate();

  if (this.mouseOutside) {
    addRemoveOutsideHandlers.call(this);
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

  this.setPosition(position, true);
}

function mouseEnter() {
  if (this.mouseOutside) {
    this.mouseOutside = false;
    addRemoveOutsideHandlers.call(this);
  }
}

function mouseLeave() {
  if (!this.getState().active) {
    return;
  }

  if (!this.props.mouseDownAllowOutside) {
    return this.deactivate();
  }

  this.mouseOutside = true;
  addRemoveOutsideHandlers.call(this, true);
}

function addRemoveOutsideHandlers(add) {
  this.mouseHandlers.filter(function (h) {
    return h.event === "mouseup" || h.event === "mousemove";
  }).forEach(function (_ref) {
    var event = _ref.event,
        handler = _ref.handler;

    if (add) {
      window.addEventListener(event, handler);
    } else {
      window.removeEventListener(event, handler);
    }
  });
}

var _default = {
  mouseDown: mouseDown,
  mouseUp: mouseUp,
  mouseMove: mouseMove,
  mouseLeave: mouseLeave,
  mouseEnter: mouseEnter,
  dragStart: _utils["default"].preventDefault
};
exports["default"] = _default;