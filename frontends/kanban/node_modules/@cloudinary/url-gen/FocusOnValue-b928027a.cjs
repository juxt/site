'use strict';

var QualifierValue = require('./QualifierValue-e770d619.cjs');

/**
 * @memberOf Qualifiers.FocusOn
 * @extends {SDK.QualifierValue}
 */
class FocusOnValue extends QualifierValue.QualifierValue {
    constructor(name) {
        super();
        this.name = name;
    }
    toString() {
        return this.name;
    }
}

exports.FocusOnValue = FocusOnValue;
