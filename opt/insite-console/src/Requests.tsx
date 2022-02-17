import {useSearch, useNavigate} from 'react-location';
import {
  GetRequestSummaryQuery,
  useAllRequestsQuery,
  useGetRequestSummaryQuery,
} from './generated/graphql';
import RequestsTable from './components/table/Table';
import {useMemo, useState} from 'react';
import {LocationGenerics} from './types';
import {
  Autocomplete,
  Container,
  Fade,
  Modal,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
} from '@mui/material';
import ReactJson from 'searchable-react-json-view';
import {notEmpty} from './common';

function useRequest(requestId: string) {
  return useGetRequestSummaryQuery(
    {
      uri: requestId,
    },
    {enabled: !!requestId, staleTime: Infinity}
  );
}

function searchAutocompleteOptions(status?: number | null) {
  if (!status) return [];
  const defaultOptions = ['duration-millis', 'access'];
  if (status && status > 400 && status <= 600) {
    return defaultOptions.concat(['message', 'lineNumber']);
  }
  if (status && status < 300) {
    return defaultOptions.concat(['ring.response/body']);
  }
  return defaultOptions;
}

function RequestInfoTable({
  data,
}: {
  data: NonNullable<GetRequestSummaryQuery['request']>;
}) {
  const colType = (key: string) => {
    switch (key) {
      case 'date':
        return 'date';
      default:
        return 'string';
    }
  };

  const rows = useMemo(
    () => [
      ...Object.entries(data)
        .filter(
          ([key, value]) =>
            notEmpty(value) &&
            key !== '_detail' &&
            key !== 'errors' &&
            key !== 'requestHeaders'
        )
        .map(([key, value]) => ({
          name: key,
          value,
          type: colType(key),
        })),
      {
        name: 'Request Time',
        value: `${data._detail['juxt.site.alpha/duration-millis']}ms`,
        type: 'string',
      },
    ],
    [data]
  );

  return (
    <TableContainer component={Paper}>
      <Table aria-label="Request info table">
        <TableHead>
          <TableRow>
            <TableCell>Key</TableCell>
            <TableCell>Value</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((row) => (
            <TableRow
              key={row.name}
              sx={{'&:last-child td, &:last-child th': {border: 0}}}>
              <TableCell component="th" scope="row">
                {row.name}
              </TableCell>
              <TableCell>{JSON.stringify(row.value)}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}

export function RequestInfo() {
  const {requestId} = useSearch<LocationGenerics>();
  const navigate = useNavigate<LocationGenerics>();

  const {status, data, error} = useRequest(requestId || '');
  const isOpen = !!data?.request?._detail;

  const [search, setSearch] = useState('');

  const isGraphql = !!data?.request?.graphqlOperationName;

  return (
    <Modal
      open={isOpen}
      onClose={() => {
        setSearch('');
        return navigate({
          search: {
            requestId: undefined,
          },
        });
      }}
      closeAfterTransition
      BackdropProps={{timeout: 500}}>
      <Fade in={isOpen}>
        <Paper
          sx={(theme) => ({
            m: 2,
            p: 4,
            [theme.breakpoints.up('md')]: {
              m: 10,
            },
            height: '80vh',
            overflow: 'auto',
          })}>
          <h1>Request Info</h1>
          {status === 'loading' && 'Loading...'}
          {status === 'error' && <span>Error: {error?.message}</span>}
          {data?.request && (
            <>
              <RequestInfoTable data={data.request} />
              {isGraphql && (
                <>
                  <h2>GraphQL</h2>
                  <p>
                    {data.request.graphqlOperationName}
                    {data.request.graphqlVariables && (
                      <>
                        <br />
                        <ReactJson
                          src={data.request.graphqlVariables}
                          collapsed={2}
                        />
                      </>
                    )}
                    {data.request.graphqlStoredQueryResourcePath && (
                      <>
                        <br />
                        <a href={data.request.graphqlStoredQueryResourcePath}>
                          Stored Query
                        </a>
                      </>
                    )}
                  </p>
                </>
              )}

              <Autocomplete
                id="request-info-autocomplete"
                freeSolo
                sx={{py: 2}}
                disableClearable
                inputValue={search}
                onInputChange={(_e, newValue) => setSearch(newValue)}
                options={searchAutocompleteOptions(data.request?.status)}
                renderInput={(params) => (
                  <TextField
                    {...params}
                    label="Search Request Details"
                    variant="outlined"
                    fullWidth
                    InputProps={{
                      ...params.InputProps,
                      type: 'search',
                    }}
                  />
                )}
              />

              <ReactJson
                src={data.request?._detail}
                collapsed={1}
                highlightSearch={search}
              />
            </>
          )}
        </Paper>
      </Fade>
    </Modal>
  );
}

export function RequestList() {
  const {status, data, error} = useAllRequestsQuery();
  const rows = useMemo(() => data?.requests?.summaries || [], [data]);

  return (
    <Container>
      <h2>Requests</h2>
      <div>
        {status === 'loading' && 'Loading...'}
        {status === 'error' && <span>Error: {error?.message}</span>}
        {rows.length && <RequestInfo />}
        <RequestsTable data={rows} />
      </div>
    </Container>
  );
}
