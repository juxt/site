import DoneIcon from '@mui/icons-material/Done';
import GlobalFilter from './GlobalFilter';
import {lighten} from '@mui/material/styles';
import Toolbar from '@mui/material/Toolbar';
import {Chip, Typography} from '@mui/material';

type Filter = {
  name: string;
  value: any[];
  setter: (value: any[] | undefined) => void;
  isActive: boolean;
};

type Props = {
  setGlobalFilter: (filter: string | undefined) => void;
  rows: Array<unknown>;
  globalFilter: string;
  filters: Array<Filter>;
};

function TableToolbar(props: Props) {
  const {rows, setGlobalFilter, globalFilter, filters} = props;
  return (
    <Toolbar
      sx={(theme) =>
        theme.palette.mode === 'light'
          ? {
              color: theme.palette.secondary.main,
              backgroundColor: lighten(theme.palette.secondary.light, 0.85),
            }
          : {
              color: theme.palette.text.primary,
              backgroundColor: theme.palette.secondary.dark,
            }
      }>
      <Typography
        sx={{
          flex: '1 1 auto',
          color: 'black',
        }}
        variant="h6"
        id="tableTitle"
        component="div">
        Requests
      </Typography>
      {filters.map(({name, value, isActive, setter}) => {
        const handleDelete = () => setter([]);
        return isActive ? (
          <Chip
            key={name}
            label={`Filtering ${name}`}
            onDelete={handleDelete}
            onClick={handleDelete}
          />
        ) : (
          <Chip
            key={name}
            label={`Filter ${name}`}
            variant="outlined"
            onClick={() => setter(value)}
          />
        );
      })}
      <GlobalFilter
        rows={rows}
        globalFilter={globalFilter}
        setGlobalFilter={setGlobalFilter}
      />
    </Toolbar>
  );
}

export default TableToolbar;
