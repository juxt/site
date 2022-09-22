'use strict';

class UnsupportedError extends Error {
    constructor(message = 'Unsupported') {
        super(message);
    }
}
/**
 * Creates a new UnsupportedError
 * @param message
 */
function createUnsupportedError(message) {
    return new UnsupportedError(message);
}

exports.createUnsupportedError = createUnsupportedError;
