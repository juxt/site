import {useSearch, useNavigate} from 'react-location';
import {
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
  TextField,
} from '@mui/material';
import ReactJson from 'searchable-react-json-view';

function useRequest(requestId: string) {
  return useGetRequestSummaryQuery(
    {
      uri: requestId,
    },
    {enabled: !!requestId}
  );
}

function searchAutocompleteOptions(status?: number) {
  const defaultOptions = ['duration-millis', 'access'];
  if (status && status > 400 && status <= 600) {
    return defaultOptions.concat(['message', 'lineNumber']);
  }
  if (status && status < 300) {
    return defaultOptions.concat(['ring.response/body']);
  }
  return defaultOptions;
}

export function RequestInfo() {
  const {requestId} = useSearch<LocationGenerics>();
  const navigate = useNavigate<LocationGenerics>();

  const {status, data, error, isFetching} = useRequest(requestId || '');
  const isOpen = !!data?.request?.detail;

  const [search, setSearch] = useState('');

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
        <Paper sx={{m: 12, p: 4, height: '80vh', overflow: 'auto'}}>
          <h1>Request {requestId}</h1>
          {status === 'loading' && 'Loading...'}
          {status === 'error' && <span>Error: {error?.message}</span>}
          {data && (
            <>
              <h1>
                {data.request?.method} {isFetching && 'fetching'}
              </h1>
              <Autocomplete
                id="request-info-autocomplete"
                freeSolo
                sx={{pb: 2}}
                disableClearable
                inputValue={search}
                onInputChange={(_e, newValue) => setSearch(newValue)}
                options={searchAutocompleteOptions(data.request?.status)}
                renderInput={(params) => (
                  <TextField
                    {...params}
                    label="Request"
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
                src={data.request?.detail}
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
