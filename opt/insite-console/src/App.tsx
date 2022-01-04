import {Outlet, ReactLocation, Router} from 'react-location';
import {QueryClient, QueryClientProvider} from 'react-query';
import {ReactQueryDevtools} from 'react-query/devtools';
import {ReactLocationDevtools} from 'react-location-devtools';
import {parseSearch, stringifySearch} from 'react-location-jsurl';
import {useAllRequestsQuery} from './generated/graphql';
import {RequestList, RequestInfo} from './Requests';
import {PageLayout} from './components/nav';
import {LocationGenerics} from './types';

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
