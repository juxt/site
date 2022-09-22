import { DocumentNode } from 'graphql/language/ast';
import * as Dom from './types.dom';
export declare type Variables = {
    [key: string]: any;
};
export interface GraphQLError {
    message: string;
    locations?: {
        line: number;
        column: number;
    }[];
    path?: string[];
    extensions?: any;
}
export interface GraphQLResponse<T = any> {
    data?: T;
    errors?: GraphQLError[];
    extensions?: any;
    status: number;
    [key: string]: any;
}
export interface GraphQLRequestContext<V = Variables> {
    query: string | string[];
    variables?: V;
}
export declare class ClientError extends Error {
    response: GraphQLResponse;
    request: GraphQLRequestContext;
    constructor(response: GraphQLResponse, request: GraphQLRequestContext);
    private static extractMessage;
}
export declare type MaybeFunction<T> = T | (() => T);
export declare type RequestDocument = string | DocumentNode;
export declare type PatchedRequestInit = Omit<Dom.RequestInit, "headers"> & {
    headers?: MaybeFunction<Dom.RequestInit['headers']>;
};
export declare type BatchRequestDocument<V = Variables> = {
    document: RequestDocument;
    variables?: V;
};
export declare type RawRequestOptions<V = Variables> = {
    query: string;
    variables?: V;
    requestHeaders?: Dom.RequestInit['headers'];
    signal?: Dom.RequestInit['signal'];
};
export declare type RequestOptions<V = Variables> = {
    document: RequestDocument;
    variables?: V;
    requestHeaders?: Dom.RequestInit['headers'];
    signal?: Dom.RequestInit['signal'];
};
export declare type BatchRequestsOptions<V = Variables> = {
    documents: BatchRequestDocument<V>[];
    requestHeaders?: Dom.RequestInit['headers'];
    signal?: Dom.RequestInit['signal'];
};
export declare type RequestExtendedOptions<V = Variables> = {
    url: string;
} & RequestOptions<V>;
export declare type RawRequestExtendedOptions<V = Variables> = {
    url: string;
} & RawRequestOptions<V>;
export declare type BatchRequestsExtendedOptions<V = Variables> = {
    url: string;
} & BatchRequestsOptions<V>;
