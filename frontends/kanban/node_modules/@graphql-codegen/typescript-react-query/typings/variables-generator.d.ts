import { OperationDefinitionNode } from 'graphql';
export declare function generateQueryVariablesSignature(hasRequiredVariables: boolean, operationVariablesTypes: string): string;
export declare function generateInfiniteQueryKey(node: OperationDefinitionNode, hasRequiredVariables: boolean): string;
export declare function generateInfiniteQueryKeyMaker(node: OperationDefinitionNode, operationName: string, operationVariablesTypes: string, hasRequiredVariables: boolean): string;
export declare function generateQueryKey(node: OperationDefinitionNode, hasRequiredVariables: boolean): string;
export declare function generateQueryKeyMaker(node: OperationDefinitionNode, operationName: string, operationVariablesTypes: string, hasRequiredVariables: boolean): string;
export declare function generateMutationKey(node: OperationDefinitionNode): string;
export declare function generateMutationKeyMaker(node: OperationDefinitionNode, operationName: string): string;
