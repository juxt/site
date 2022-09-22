"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.GithubLoader = void 0;
const tslib_1 = require("tslib");
const utils_1 = require("@graphql-tools/utils");
const graphql_tag_pluck_1 = require("@graphql-tools/graphql-tag-pluck");
const graphql_1 = require("graphql");
const sync_fetch_1 = tslib_1.__importDefault(require("@ardatan/sync-fetch"));
const fetch_1 = require("@whatwg-node/fetch");
// github:owner/name#ref:path/to/file
function extractData(pointer) {
    const [repo, file] = pointer.split('#');
    const [owner, name] = repo.split(':')[1].split('/');
    const [ref, path] = file.split(':');
    return {
        owner,
        name,
        ref,
        path,
    };
}
/**
 * This loader loads a file from GitHub.
 *
 * ```js
 * const typeDefs = await loadTypedefs('github:githubUser/githubRepo#branchName:path/to/file.ts', {
 *   loaders: [new GithubLoader()],
 *   token: YOUR_GITHUB_TOKEN,
 * })
 * ```
 */
class GithubLoader {
    async canLoad(pointer) {
        return this.canLoadSync(pointer);
    }
    canLoadSync(pointer) {
        return typeof pointer === 'string' && pointer.toLowerCase().startsWith('github:');
    }
    async load(pointer, options) {
        if (!(await this.canLoad(pointer))) {
            return [];
        }
        const { owner, name, ref, path } = extractData(pointer);
        const fetch = options.customFetch || fetch_1.fetch;
        const request = await fetch('https://api.github.com/graphql', this.prepareRequest({ owner, ref, path, name, options }));
        const response = await request.json();
        const status = request.status;
        return this.handleResponse({ pointer, path, options, response, status });
    }
    loadSync(pointer, options) {
        if (!this.canLoadSync(pointer)) {
            return [];
        }
        const { owner, name, ref, path } = extractData(pointer);
        const fetch = options.customFetch || sync_fetch_1.default;
        const request = fetch('https://api.github.com/graphql', this.prepareRequest({ owner, ref, path, name, options }));
        const response = request.json();
        const status = request.status;
        return this.handleResponse({ pointer, path, options, response, status });
    }
    handleResponse({ pointer, path, options, response, status, }) {
        let errorMessage = null;
        if (response.errors && response.errors.length > 0) {
            errorMessage = response.errors.map((item) => item.message).join(', ');
        }
        else if (status === 401) {
            errorMessage = response.message;
        }
        else if (!response.data) {
            errorMessage = response;
        }
        if (errorMessage) {
            throw new Error('Unable to download schema from github: ' + errorMessage);
        }
        const content = response.data.repository.object.text;
        if (/\.(gql|graphql)s?$/i.test(path)) {
            return [(0, utils_1.parseGraphQLSDL)(pointer, content, options)];
        }
        if (/\.json$/i.test(path)) {
            return [(0, utils_1.parseGraphQLJSON)(pointer, content, options)];
        }
        if (path.endsWith('.tsx') || path.endsWith('.ts') || path.endsWith('.js') || path.endsWith('.jsx')) {
            const sources = (0, graphql_tag_pluck_1.gqlPluckFromCodeStringSync)(pointer, content, options.pluckConfig);
            return sources.map(source => ({
                location: pointer,
                document: (0, graphql_1.parse)(source, options),
            }));
        }
        throw new Error(`Invalid file extension: ${path}`);
    }
    prepareRequest({ owner, ref, path, name, options, }) {
        return {
            method: 'POST',
            headers: {
                'content-type': 'application/json; charset=utf-8',
                authorization: `bearer ${options.token}`,
            },
            body: JSON.stringify({
                query: `
          query GetGraphQLSchemaForGraphQLtools($owner: String!, $name: String!, $expression: String!) {
            repository(owner: $owner, name: $name) {
              object(expression: $expression) {
                ... on Blob {
                  text
                }
              }
            }
          }
        `,
                variables: {
                    owner,
                    name,
                    expression: ref + ':' + path,
                },
                operationName: 'GetGraphQLSchemaForGraphQLtools',
            }),
        };
    }
}
exports.GithubLoader = GithubLoader;
