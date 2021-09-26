import GraphiQL, { GraphiQLProps } from 'graphiql';
import 'graphiql/graphiql.min.css';

const App = () => (
  // @ts-ignore
  <GraphiQL
    fetcher={async (graphQLParams: GraphiQLProps) => {
      const data = await fetch(
        '/graphql',
        {
          method: 'POST',
          headers: {
            Accept: 'application/json',
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(graphQLParams),
          credentials: 'include',
        },
      );
      return data.json().catch(() => data.text());
    }}
  />
);

export default App;