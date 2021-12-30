/* This example requires Tailwind CSS v2.0+ */
import {
  ApolloClient,
  InMemoryCache,
  useQuery,
  gql,
  createHttpLink
} from "@apollo/client";

const link = createHttpLink({
  uri: 'https://home.test/_site/graphql',
  credentials: 'include'
});

const client = new ApolloClient({
  cache: new InMemoryCache(),
  link,
});

export default function Card() {
  const { loading, data, error } = useQuery(
    gql`
  query GetRequestSummary($uri: ID) {
    request(id: $uri) {
      id
      status
      date
      requestUri
      method
      operationName
      detail
    }
  }
  `,
    {variables: {uri: "https://home.test/_site/requests/d2d15afa4cc24049f55db79f"}});

  if (loading) return <p>Loading...</p>;
  if (error) return <p>Error</p>;

  const rows = [
    { title: "Method", value: data.request?.method },
    { title: "Date", value: data.request?.date },
    { title: "Operation Name", value: data.request?.operationName },
  ];

  return (
    <div className="bg-white shadow overflow-hidden sm:rounded-lg">
      <div className="px-4 py-5 sm:px-6">
        <h3 className="text-lg leading-6 font-medium text-gray-900">{data.request?.status || "Unknown"}</h3>
        <p className="mt-1 max-w-2xl text-sm text-gray-500">{data.request?.requestUri}</p>
      </div>
      <div className="border-t border-gray-200 px-4 py-5 sm:p-0">
        <dl className="sm:divide-y sm:divide-gray-200">
          {rows.map(({ title, value }) => (
            <div key={title} className="py-4 sm:py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
              <dt className="text-sm font-medium text-gray-500">{title}</dt>
              <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">{value}</dd>
            </div>))}
        </dl>
      </div>
      <div className="px-4 py-5 sm:px-6">
        <h3 className="text-lg leading-6 font-medium text-gray-900">Detail</h3>
        <div className="py-4 text-xs">
          <pre>{JSON.stringify(data.request?.detail, null, 2)}</pre>
        </div>
      </div>
    </div>
  );
}
