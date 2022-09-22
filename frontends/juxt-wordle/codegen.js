module.exports = {
  schema: "schema.graphql",
  documents: "src/**/*.graphql",
  generates: {
    "src/generated/graphql.ts": {
      plugins: [
        "typescript",
        "typescript-operations",
        "typescript-react-query",
      ],
      config: {
        exposeQueryKeys: true,
        exposeDocument: true,
        exposeFetcher: true,
        errorType: "Error",
        fetcher: {
          endpoint: process.env.GRAPHQL_API_URL,
          fetchParams: JSON.stringify({
            headers: {
              "Content-Type": "application/json",
              Accept: "application/json",
            },
            credentials: "include",
          }),
        },
      },
    },
  },
};
