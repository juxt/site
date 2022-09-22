export function generateQueryVariablesSignature(hasRequiredVariables, operationVariablesTypes) {
    return `variables${hasRequiredVariables ? '' : '?'}: ${operationVariablesTypes}`;
}
export function generateInfiniteQueryKey(node, hasRequiredVariables) {
    if (hasRequiredVariables)
        return `['${node.name.value}.infinite', variables]`;
    return `variables === undefined ? ['${node.name.value}.infinite'] : ['${node.name.value}.infinite', variables]`;
}
export function generateInfiniteQueryKeyMaker(node, operationName, operationVariablesTypes, hasRequiredVariables) {
    const signature = generateQueryVariablesSignature(hasRequiredVariables, operationVariablesTypes);
    return `\nuseInfinite${operationName}.getKey = (${signature}) => ${generateInfiniteQueryKey(node, hasRequiredVariables)};\n`;
}
export function generateQueryKey(node, hasRequiredVariables) {
    if (hasRequiredVariables)
        return `['${node.name.value}', variables]`;
    return `variables === undefined ? ['${node.name.value}'] : ['${node.name.value}', variables]`;
}
export function generateQueryKeyMaker(node, operationName, operationVariablesTypes, hasRequiredVariables) {
    const signature = generateQueryVariablesSignature(hasRequiredVariables, operationVariablesTypes);
    return `\nuse${operationName}.getKey = (${signature}) => ${generateQueryKey(node, hasRequiredVariables)};\n`;
}
export function generateMutationKey(node) {
    return `['${node.name.value}']`;
}
export function generateMutationKeyMaker(node, operationName) {
    return `\nuse${operationName}.getKey = () => ${generateMutationKey(node)};\n`;
}
