"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.isNone = exports.isSome = void 0;
const isSome = (input) => input != null;
exports.isSome = isSome;
const isNone = (input) => input == null;
exports.isNone = isNone;
