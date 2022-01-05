import JSONTree from 'react-json-tree';

const jsonTreeTheme = {
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

export default function Card({ request }) {

  const rows = [
    { title: "Status message", value: request.statusMessage },
    { title: "Method", value: request.method },
    { title: "URI", value: request.requestUri },
    { title: "Date", value: request.date }
  ];

  return (
    <div className="card bg-white shadow overflow-hidden sm:rounded-lg">
      <div className="px-4 py-5 sm:px-6">
        <h3 className="text-6xl font-bold text-gray-900">{request.status || "000"}</h3>
      </div>

      <div className="px-4 py-2 sm:px-6">
        <p className="mt-1 max-w-sm text-sm">{request.method} <a className="underline text-blue-800 hover:text-blue-600" href={ request.requestUri }>{request.requestUri}</a></p>
      </div>

      <div className="border-t border-gray-200 px-4 py-2 sm:p-0">
        <dl className="sm:divide-y sm:divide-gray-200">
          {rows.map(({ title, value }) => (
            <div key={title} className="py-4 sm:py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
              <dt className="text-sm font-medium text-gray-500">{title}</dt>
              <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">{value}</dd>
            </div>))}
        </dl>
      </div>

      <div className="border-t border-gray-200 px-4 py-5 sm:px-0">
        <h3 className="sm:px-6 text-lg leading-6 font-bold text-gray-900">Errors</h3>

        {request.errors.map(({ message, stackTrace, exData, __typename, ...error }) => (
          <div key={message} className="py-4 sm:py-5 sm:px-6">
            <p className="text-sm font-medium py-4">{message}</p>
            <div>
              <ul className="text-xs">
                {stackTrace.map(({methodName, fileName, lineNumber, className}) => (
                  <li>{"at " + className + "." + methodName + "(" + fileName + ":" + lineNumber + ")"}</li>
                  ))}
              </ul>
            </div>

            {exData ?
             <div className="py-4">
               <div>
                 <h4>
                   Exception Data
                 </h4>
               </div>
               <div className="text-xs font-medium text-gray-500">
               <JSONTree data={exData} theme={jsonTreeTheme} sortObjectKeys={true} hideRoot={true}  />
               </div></div> :
             <div/>}

            {__typename == "SiteGraphqlExecutionError" ?
             <div className="py-4">
               <h4>
                 GraphQL field errors
               </h4>
                 <div className="py-2">
                   <table className="table-auto text-xs w-full">
                     <thead>
                       <tr>
                         <th className="text-left">Path</th>
                         <th className="text-left">Message</th>
                         <th className="text-left">Stack trace</th>
                       </tr>
                     </thead>
                     <tbody>
                       {error.fieldErrors.map(({ path, message, extensions, stackTrace }) => (
                         <tr>
                           <td>{path}</td>
                           <td>{message}</td>
                           <td><ul className="text-xs">
                {stackTrace?.map(({methodName, fileName, lineNumber, className}) => (
                  <li>{"at " + className + "." + methodName + "(" + fileName + ":" + lineNumber + ")"}</li>
                  ))}
                               </ul>
                           </td>
                         </tr>
                       ))}
                     </tbody>
                   </table>
                 </div>
             </div>
             : <div/>}


          </div>))}
      </div>

      <div className="border-t border-gray-200 px-4 py-5 sm:px-0">
        <h3 className="sm:px-6 text-lg leading-6 font-bold text-gray-900">
          GraphQL
        </h3>
        <dl className="sm:divide-y sm:divide-gray-200">
          <div key="query" className="py-4 sm:py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
            <dt className="text-sm font-medium text-gray-500">Query</dt>
            <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">{request.graphqlStoredQueryResourcePath}</dd>
          </div>
          <div key="opname" className="py-4 sm:py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
            <dt className="text-sm font-medium text-gray-500">Operation Name</dt>
            <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">{request.graphqlOperationName}</dd>
          </div>
          <div key="opname" className="py-4 sm:py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
            <dt className="text-sm font-medium text-gray-500">Variables</dt>
            <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">
              <JSONTree data={request.graphqlVariables} theme={jsonTreeTheme} sortObjectKeys={true} hideRoot={true}  />
            </dd>
            <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">
              <pre>
                {JSON.stringify(request.graphqlVariables,null,2)}
              </pre>
            </dd>
          </div>
        </dl>
      </div>

      <div className="border-t border-gray-200 px-4 py-5 sm:px-0">
        <h3 className="sm:px-6 text-lg leading-6 font-bold text-gray-900">All Data</h3>
        <div className="sm:px-6 text-xs">
          <JSONTree data={request} theme={jsonTreeTheme} sortObjectKeys={true} hideRoot={true}  />
        </div>
      </div>
    </div>
  );
}
