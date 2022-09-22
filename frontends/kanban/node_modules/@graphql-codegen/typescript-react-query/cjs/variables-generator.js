"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.generateMutationKeyMaker = exports.generateMutationKey = exports.generateQueryKeyMaker = exports.generateQueryKey = exports.generateInfiniteQueryKeyMaker = exports.generateInfiniteQueryKey = exports.generateQueryVariablesSignature = void 0;
function generateQueryVariablesSignature(hasRequiredVariables, operationVariablesTypes) {
    return `variables${hasRequiredVariables ? '' : '?'}: ${operationVariablesTypes}`;
}
exports.generateQueryVariablesSignature = generateQueryVariablesSignature;
function generateInfiniteQueryKey(node, hasRequiredVariables) {
    if (hasRequiredVariables)
        return `['${node.name.value}.infinite', variables]`;
    return `variables === undefined ? ['${node.name.value}.infinite'] : ['${node.name.value}.infinite', variables]`;
}
exports.generateInfiniteQueryKey = generateInfiniteQueryKey;
function generateInfiniteQueryKeyMaker(node, operationName, operationVariablesTypes, hasRequiredVariables) {
    const signature = generateQueryVariablesSignature(hasRequiredVariables, operationVariablesTypes);
    return `\nuseInfinite${operationName}.getKey = (${signature}) => ${generateInfiniteQueryKey(node, hasRequiredVariables)};\n`;
}
exports.generateInfiniteQueryKeyMaker = generateInfiniteQueryKeyMaker;
function generateQueryKey(node, hasRequiredVariables) {
    if (hasRequiredVariables)
        return `['${node.name.value}', variables]`;
    return `variables === undefined ? ['${node.name.value}'] : ['${node.name.value}', variables]`;
}
exports.generateQueryKey = generateQueryKey;
function generateQueryKeyMaker(node, operationName, operationVariablesTypes, hasRequiredVariables) {
    const signature = generateQueryVariablesSignature(hasRequiredVariables, operationVariablesTypes);
    return `\nuse${operationName}.getKey = (${signature}) => ${generateQueryKey(node, hasRequiredVariables)};\n`;
}
exports.generateQueryKeyMaker = generateQueryKeyMaker;
function generateMutationKey(node) {
    return `['${node.name.value}']`;
}
exports.generateMutationKey = generateMutationKey;
function generateMutationKeyMaker(node, operationName) {
    return `\nuse${operationName}.getKey = () => ${generateMutationKey(node)};\n`;
}
exports.generateMutationKeyMaker = generateMutationKeyMaker;
