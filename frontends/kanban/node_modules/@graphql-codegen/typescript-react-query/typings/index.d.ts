import { PluginFunction, PluginValidateFn, Types } from '@graphql-codegen/plugin-helpers';
import { ReactQueryRawPluginConfig } from './config.js';
import { ReactQueryVisitor } from './visitor.js';
export declare const plugin: PluginFunction<ReactQueryRawPluginConfig, Types.ComplexPluginOutput>;
export declare const validate: PluginValidateFn<any>;
export { ReactQueryVisitor };
