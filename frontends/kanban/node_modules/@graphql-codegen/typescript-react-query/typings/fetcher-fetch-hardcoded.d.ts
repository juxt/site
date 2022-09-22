import { FetcherRenderer } from './fetcher.js';
import { HardcodedFetch } from './config.js';
import { OperationDefinitionNode } from 'graphql';
import { ReactQueryVisitor } from './visitor.js';
export declare class HardcodedFetchFetcher implements FetcherRenderer {
    private visitor;
    private config;
    constructor(visitor: ReactQueryVisitor, config: HardcodedFetch);
    private getEndpoint;
    private getFetchParams;
    generateFetcherImplementaion(): string;
    generateInfiniteQueryHook(node: OperationDefinitionNode, documentVariableName: string, operationName: string, operationResultType: string, operationVariablesTypes: string, hasRequiredVariables: boolean): string;
    generateQueryHook(node: OperationDefinitionNode, documentVariableName: string, operationName: string, operationResultType: string, operationVariablesTypes: string, hasRequiredVariables: boolean): string;
    generateMutationHook(node: OperationDefinitionNode, documentVariableName: string, operationName: string, operationResultType: string, operationVariablesTypes: string, hasRequiredVariables: boolean): string;
    generateFetcherFetch(node: OperationDefinitionNode, documentVariableName: string, operationName: string, operationResultType: string, operationVariablesTypes: string, hasRequiredVariables: boolean): string;
}
