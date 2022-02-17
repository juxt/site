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
          data.apis.map((api) => {
            const url = new URL(api.id);
            const path = url.pathname;
            return (
              <div key={api.id}>
                <Link
                  to={`./${api.type.toLowerCase()}`}
                  search={(old) => ({...old, url: path})}>
                  {api.id}
                </Link>
                <p>{api.type}</p>
              </div>
            );
          })}
      </div>
    </Container>
  );
}
