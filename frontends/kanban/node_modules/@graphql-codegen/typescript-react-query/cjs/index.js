"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ReactQueryVisitor = exports.validate = exports.plugin = void 0;
const graphql_1 = require("graphql");
const plugin_helpers_1 = require("@graphql-codegen/plugin-helpers");
const visitor_js_1 = require("./visitor.js");
Object.defineProperty(exports, "ReactQueryVisitor", { enumerable: true, get: function () { return visitor_js_1.ReactQueryVisitor; } });
const path_1 = require("path");
const plugin = (schema, documents, config) => {
    const allAst = (0, graphql_1.concatAST)(documents.map(v => v.document));
    const allFragments = [
        ...allAst.definitions.filter(d => d.kind === graphql_1.Kind.FRAGMENT_DEFINITION).map(fragmentDef => ({
            node: fragmentDef,
            name: fragmentDef.name.value,
            onType: fragmentDef.typeCondition.name.value,
            isExternal: false,
        })),
        ...(config.externalFragments || []),
    ];
    const visitor = new visitor_js_1.ReactQueryVisitor(schema, allFragments, config, documents);
    const visitorResult = (0, plugin_helpers_1.oldVisit)(allAst, { leave: visitor });
    if (visitor.hasOperations) {
        return {
            prepend: [...visitor.getImports(), visitor.getFetcherImplementation()],
            content: [visitor.fragments, ...visitorResult.definitions.filter(t => typeof t === 'string')].join('\n'),
        };
    }
    return {
        prepend: [...visitor.getImports()],
        content: [visitor.fragments, ...visitorResult.definitions.filter(t => typeof t === 'string')].join('\n'),
    };
};
exports.plugin = plugin;
const validate = async (schema, documents, config, outputFile) => {
    if ((0, path_1.extname)(outputFile) !== '.ts' && (0, path_1.extname)(outputFile) !== '.tsx') {
        throw new Error(`Plugin "typescript-react-query" requires extension to be ".ts" or ".tsx"!`);
    }
};
exports.validate = validate;
