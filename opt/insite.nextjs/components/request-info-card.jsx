import JSONTree from 'react-json-tree';

const theme = {
  scheme: 'monokai',
  author: 'wimer hazenberg (http://www.monokai.nl)',
  base00: '#272822',
  base01: '#383830',
  base02: '#49483e',
  base03: '#75715e',
  base04: '#a59f85',
  base05: '#f8f8f2',
  base06: '#f5f4f1',
  base07: '#f9f8f5',
  base08: '#f92672',
  base09: '#fd971f',
  base0A: '#f4bf75',
  base0B: '#a6e22e',
  base0C: '#a1efe4',
  base0D: '#66d9ef',
  base0E: '#ae81ff',
  base0F: '#cc6633',
};

// "https://home.test/_site/requests/d2d15afa4cc24049f55db79f"

export default function RequestInfoCard({ request }) {

  const rows = [
    { title: "Method", value: request.method },
    { title: "Date", value: request.date },
  ];

  const graphQLRows = [
    { title: "Operation Name", value: request.operationName },
  ];

  return (
    <div className="bg-white shadow overflow-hidden sm:rounded-lg">
      <div className="px-4 py-5 sm:px-6">
        <h3 className="text-6xl font-bold text-gray-900">{request.status || "000"}</h3>
        <p className="mt-1 max-w-2xl text-sm text-gray-500">{request.requestUri || "<Request URI>"}</p>
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
      <div className="border-t border-gray-200 px-4 py-5 sm:px-0">
        <h3 className="sm:px-6 text-lg leading-6 font-medium text-gray-900">GraphQL</h3>
        <dl className="sm:divide-y sm:divide-gray-200">
          {graphQLRows.map(({ title, value }) => (
            <div key={title} className="py-4 sm:py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
              <dt className="text-sm font-medium text-gray-500">{title}</dt>
              <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">{value}</dd>
            </div>))}
        </dl>
      </div>
      <div className="border-t border-gray-200 px-4 py-5 sm:px-0">
        <h3 className="sm:px-6 text-lg leading-6 font-medium text-gray-900">Detail</h3>
        <div className="sm:px-6 text-xs">
          <JSONTree data={request.detail} theme={theme} />
        </div>
      </div>
    </div>
  );
}
