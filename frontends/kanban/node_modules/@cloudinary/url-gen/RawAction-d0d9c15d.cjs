'use strict';

var unsupportedError = require('./unsupportedError-74070138.cjs');

/**
 * @summary SDK
 * @memberOf SDK
 * @description Defines an action that's a string literal, no validations or manipulations are performed
 */
class RawAction {
    constructor(raw) {
        this.raw = raw;
    }
    toString() {
        return this.raw;
    }
    toJson() {
        return { error: unsupportedError.createUnsupportedError(`unsupported action ${this.constructor.name}`) };
    }
}

exports.RawAction = RawAction;
