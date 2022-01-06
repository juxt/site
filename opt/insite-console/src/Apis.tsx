/* eslint-disable import/prefer-default-export */
import {Container} from '@mui/material';
import {Link} from 'react-location';
import {useAllApisQuery} from './generated/graphql';

export function ApiList() {
  const {status, data, error} = useAllApisQuery();
  return (
    <Container>
      <h2>Apis</h2>
      <div>
        {status === 'loading' && 'Loading...'}
        {status === 'error' && <span>Error: {error?.message}</span>}
        {data &&
          data.apis.map((api) => (
            <div key={api.id}>
              {api.type === 'GRAPHQL' ? (
                <Link
                  to="/apis/graphiql"
                  search={(old) => ({...old, graphqlUrl: api.id})}>
                  {api.id}
                </Link>
              ) : (
                <a
                  href={`http://localhost:5509/swagger-ui/index.html?url=${api.id}`}>
                  {api.id}
                </a>
              )}
              <p>{api.type}</p>
            </div>
          ))}
      </div>
    </Container>
  );
}
