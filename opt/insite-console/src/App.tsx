import {Outlet, ReactLocation, Router} from 'react-location';
import {QueryClient, QueryClientProvider} from 'react-query';
import {ReactQueryDevtools} from 'react-query/devtools';
import {ReactLocationDevtools} from 'react-location-devtools';
import {parseSearch, stringifySearch} from 'react-location-jsurl';
import {useAllApisQuery, useAllRequestsQuery} from './generated/graphql';
import {RequestList} from './Requests';
import Graphiql from './components/Graphiql';
import Swagger from './components/Swagger';
import {ApiList} from './Apis';
import {PageLayout} from './components/nav';
import {LocationGenerics} from './types';
import {baseUrl} from './common';

const location = new ReactLocation<LocationGenerics>({
  parseSearch,
  stringifySearch,
});
const rootQueryClient = new QueryClient();

export default function App() {
  return (
    <QueryClientProvider client={rootQueryClient}>
      <Router
        location={location}
        routes={[
          {
            path: baseUrl,
            children: [
              {
                path: '/',
                element: 'InSite',
              },
              {
                path: 'requests',
                element: <RequestList />,
                loader: () =>
                  rootQueryClient.getQueryData('requests') ??
                  rootQueryClient
                    .fetchQuery('requests', useAllRequestsQuery.fetcher())
                    .then(() => ({})),
              },
              {
                path: 'apis',
                children: [
                  {
                    path: '/',
                    element: <ApiList />,
                    loader: () =>
                      rootQueryClient.getQueryData('allApis') ??
                      rootQueryClient
                        .fetchQuery('allApis', useAllApisQuery.fetcher())
                        .then(() => ({})),
                  },
                  {
                    path: 'graphql',
                    element: <Graphiql />,
                  },
                  {
                    path: 'openapi',
                    element: <Swagger />,
                  },
                ],
              },
            ],
          },
        ]}>
        <PageLayout>
          <Outlet />
        </PageLayout>
        <ReactLocationDevtools initialIsOpen={false} position="bottom-right" />
      </Router>
      <ReactQueryDevtools initialIsOpen />
    </QueryClientProvider>
  );
}
