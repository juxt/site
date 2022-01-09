// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-nocheck
import React from 'react';
import Paper from '@mui/material/Paper';
import MaUTable from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableFooter from '@mui/material/TableFooter';
import TableHead from '@mui/material/TableHead';
import TablePagination from '@mui/material/TablePagination';
import TablePaginationActions from './TablePaginationActions';
import TableRow from '@mui/material/TableRow';
import TableSortLabel from '@mui/material/TableSortLabel';
import TableToolbar from './TableToolbar';
import {LocationGenerics} from '../../types';
import {useNavigate} from 'react-location';
import {
  useGlobalFilter,
  usePagination,
  useFilters,
  useSortBy,
  useTable,
} from 'react-table';

// Set our editable cell renderer as the default Cell renderer
const defaultColumn = {};

type TableProps = {
  data: Array<object>;
};

function RequestsTable({data}: TableProps) {
  const columns = React.useMemo(
    () => [
      {accessor: 'id', Header: 'ID', width: 90},
      {
        accessor: 'date',
        type: 'date',
        Header: 'Request',
        width: 150,
      },
      {
        accessor: 'status',
        Header: 'Status',
        filter: 'includesValue',
      },
    ],
    []
  );
  const {
    getTableProps,
    headerGroups,
    prepareRow,
    rows,
    page,
    gotoPage,
    setPageSize,
    setFilter,
    preGlobalFilteredRows,
    setGlobalFilter,
    state: {pageIndex, pageSize, globalFilter, filters},
  } = useTable(
    {
      columns,
      data,
      defaultColumn,
    },
    useGlobalFilter,
    useFilters,
    useSortBy,
    usePagination
  );
  const navigate = useNavigate<LocationGenerics>();

  const handleChangePage = (_event, newPage) => {
    gotoPage(newPage);
  };

  const handleChangeRowsPerPage = (event) => {
    setPageSize(Number(event.target.value));
  };

  return (
    <Paper sx={{width: '100%', mb: 2}}>
      <TableContainer>
        <TableToolbar
          filters={[
            {
              name: 'errors',
              value: [400, 401, 403, 404, 500],
              isActive: filters.find((f) => f.id === 'status'),
              setter: (value: number[]) => setFilter('status', value),
            },
          ]}
          rows={rows}
          setGlobalFilter={setGlobalFilter}
          globalFilter={globalFilter}
        />
        <MaUTable {...getTableProps()}>
          <TableHead>
            {headerGroups.map((headerGroup) => (
              <TableRow {...headerGroup.getHeaderGroupProps()}>
                {headerGroup.headers.map((column) => (
                  <TableCell
                    {...(column.id === 'selection'
                      ? column.getHeaderProps()
                      : column.getHeaderProps(column.getSortByToggleProps()))}>
                    {column.render('Header')}
                    {column.id !== 'selection' ? (
                      <TableSortLabel
                        active={column.isSorted}
                        // react-table has a unsorted state which is not treated here
                        direction={column.isSortedDesc ? 'desc' : 'asc'}
                      />
                    ) : null}
                  </TableCell>
                ))}
              </TableRow>
            ))}
          </TableHead>
          <TableBody>
            {page.map((row) => {
              prepareRow(row);
              return (
                <TableRow
                  hover
                  sx={{cursor: 'pointer'}}
                  onClick={() =>
                    navigate({
                      search: (old) => ({
                        ...old,
                        requestId: row.original.id,
                      }),
                    })
                  }
                  {...row.getRowProps()}>
                  {row.cells.map((cell) => {
                    return (
                      <TableCell {...cell.getCellProps()}>
                        {cell.render('Cell')}
                      </TableCell>
                    );
                  })}
                </TableRow>
              );
            })}
          </TableBody>

          <TableFooter>
            <TableRow>
              <TablePagination
                rowsPerPageOptions={[
                  5,
                  10,
                  25,
                  {label: 'All', value: data.length},
                ]}
                colSpan={3}
                count={rows.length}
                rowsPerPage={pageSize}
                page={pageIndex}
                SelectProps={{
                  inputProps: {'aria-label': 'rows per page'},
                  native: true,
                }}
                onPageChange={handleChangePage}
                onRowsPerPageChange={handleChangeRowsPerPage}
                ActionsComponent={TablePaginationActions}
              />
            </TableRow>
          </TableFooter>
        </MaUTable>
      </TableContainer>
    </Paper>
  );
}

export default RequestsTable;
