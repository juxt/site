module.exports = {
  schema: '../../src/juxt/site/alpha/site-schema.graphql',
  documents: 'src/**/*.graphql',
  generates: {
    'src/generated/graphql.ts': {
      plugins: [
        'typescript',
        'typescript-operations',
        'typescript-react-query',
      ],
      config: {
        exposeQueryKeys: true,
        exposeDocument: true,
        exposeFetcher: true,
        errorType: 'Error',
        fetcher: {
          endpoint: '"/_site/graphql"',
          fetchParams: JSON.stringify({
            headers: {
              'Content-Type': 'application/json',
            },
            credentials: 'include',
          }),
        },
      },
    },
  },
};
