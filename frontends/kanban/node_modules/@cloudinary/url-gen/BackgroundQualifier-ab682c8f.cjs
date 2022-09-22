'use strict';

var Qualifier = require('./Qualifier-6633a22f.cjs');

/**
 * @description Defines the visual appearance of the background.
 * @memberOf Qualifiers.Background
 * @extends {SDK.Qualifier}
 */
class BackgroundQualifier extends Qualifier.Qualifier {
    constructor(backgroundValue) {
        // The qualifier key for this qualifier
        super('b');
        // Such as color (b_red)
        if (backgroundValue) {
            this.addValue(backgroundValue);
        }
    }
}

exports.BackgroundQualifier = BackgroundQualifier;
