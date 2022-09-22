'use strict';

var unsupportedError = require('./unsupportedError-74070138.cjs');

/**
 * Returns the action's model
 */
function qualifierToJson() {
    return this._qualifierModel || { error: unsupportedError.createUnsupportedError(`unsupported qualifier ${this.constructor.name}`) };
}

class QualifierModel {
    constructor() {
        this._qualifierModel = {};
    }
    toJson() {
        return qualifierToJson.apply(this);
    }
}

exports.QualifierModel = QualifierModel;
