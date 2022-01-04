import InputBase from '@mui/material/InputBase';
import SearchIcon from '@mui/icons-material/Search';
import {Box} from '@mui/material';
import {alpha} from '@mui/material/styles';

type Props = {
  preGlobalFilteredRows: Array<unknown>;
  globalFilter: string;
  setGlobalFilter: (value: string | undefined) => void;
};

function GlobalFilter({
  preGlobalFilteredRows,
  globalFilter,
  setGlobalFilter,
}: Props) {
  const count = preGlobalFilteredRows.length;

  // Global filter only works with pagination from the first page.
  // This may not be a problem for server side pagination when
  // only the current page is downloaded.

  return (
    <Box
      sx={(theme) => ({
        position: 'relative',
        borderRadius: theme.shape.borderRadius,
        backgroundColor: alpha(theme.palette.common.white, 0.15),
        '&:hover': {
          backgroundColor: alpha(theme.palette.common.white, 0.25),
        },
        marginRight: theme.spacing(2),
        marginLeft: 0,
        width: '100%',
        [theme.breakpoints.up('sm')]: {
          marginLeft: theme.spacing(3),
          width: 'auto',
        },
      })}>
      <Box
        sx={(theme) => ({
          width: theme.spacing(7),
          height: '100%',
          position: 'absolute',
          pointerEvents: 'none',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        })}>
        <SearchIcon />
      </Box>
      <InputBase
        sx={(theme) => ({
          padding: theme.spacing(1, 1, 1, 7),
          transition: theme.transitions.create('width'),
          width: '100%',
          [theme.breakpoints.up('md')]: {
            width: 200,
          },
        })}
        value={globalFilter || ''}
        onChange={(e) => {
          setGlobalFilter(e.target.value || undefined); // Set undefined to remove the filter entirely
        }}
        placeholder={`${count} records...`}
        inputProps={{'aria-label': 'search'}}
      />
    </Box>
  );
}

export default GlobalFilter;
