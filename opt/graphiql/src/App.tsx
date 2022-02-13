import GraphiQL from 'graphiql';
import GraphiQLExplorer from 'graphiql-explorer';
import { getIntrospectionQuery, buildClientSchema } from "graphql"
import 'graphiql/graphiql.min.css';
import { useCallback, useEffect, useRef, useState } from 'react';

async function fetchGraphQlSchema(fetcher: import("graphiql").Fetcher) {
  const result = await fetcher({
    query: getIntrospectionQuery(),
  } as never);

  //
  const schema = buildClientSchema(
    (result as unknown as { data: import("graphql").IntrospectionQuery }).data,
  );
  return schema;
}

const queryParams = new URLSearchParams(window.location.search);

const graphQLFetcher: import("graphiql").Fetcher = async (graphQLParams) => {
  const endpoint = queryParams.get('url') ?? '/_site/graphql';
  const data = await fetch(
    endpoint,
    {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(graphQLParams),
      credentials: 'include',
    }
  );
  return data.json().catch(() => console.error('error fetching from ' + endpoint, data));
}

const App = () => {
  const queryString = queryParams.get('q') ?? '';
  const [query, setQuery] = useState<string | undefined>(queryString);
  const [schema, setSchema] = useState<import("graphql").GraphQLSchema>();
  const [explorerIsOpen, setExplorerIsOpen] = useState(true);

  const refGq = useRef<GraphiQL | null>(null);

  const handleEditQuery = useCallback((query?: string) => {
    setQuery(query);
  }, []);

  const handleToggleExplorer = useCallback(() => {
    setExplorerIsOpen((old) => !old);
  }, []);

  useEffect(() => {
    fetchGraphQlSchema(graphQLFetcher).then((s) => setSchema(s)).catch(error => console.log('ERROR'));
  }, []);
  return (
    <div className={`graphiql-container`}>
      <GraphiQLExplorer
        schema={schema}
        query={query}
        onEdit={handleEditQuery}
        onRunOperation={(operationName?: string) =>
          refGq.current?.handleRunQuery(operationName)
        }
        explorerIsOpen={explorerIsOpen}
        onToggleExplorer={handleToggleExplorer}
      />
      {/*
 // @ts-ignore */}
      <GraphiQL
        ref={refGq}
        schema={schema}
        fetcher={graphQLFetcher}
        query={query}
        onEditQuery={handleEditQuery}
        defaultVariableEditorOpen
        toolbar={{
          additionalContent: (
            // @ts-ignore
            <GraphiQL.Button
              onClick={handleToggleExplorer}
              label="Explorer"
              title="Toggle Explorer"
            />
          ),
        }}
      />
    </div>
  );
};

export default App;
