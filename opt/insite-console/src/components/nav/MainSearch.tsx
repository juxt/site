import InputBase from '@mui/material/InputBase';
import Paper from '@mui/material/Paper';
import Box from '@mui/material/Box';
import {grey} from '@mui/material/colors';
import ClickAwayListener from '@mui/material/ClickAwayListener';
import {useState} from 'react';

type Result = {
  name: string;
  id: number;
};
type Props = {
  title: string;
  results: Array<Result>;
};

function ResultBox({title, results}: Props) {
  return (
    <Box
      sx={{
        margin: (theme) => ({
          xs: `0 ${theme.spacing(2)}`,
          md: theme.spacing(2),
        }),
        padding: (theme) => ({
          xs: 0,
          md: `0 ${theme.spacing(2)}`,
        }),
        borderRight: {
          xs: 0,
          md: `1px solid ${grey.A200}`,
        },
        '&:last-child': {
          borderRight: 0,
        },
      }}>
      <h4 style={{fontWeight: 200}}>{title}</h4>
      {results?.map((element: Result) => (
        <p style={{fontWeight: 'bold'}} key={element.id}>
          {element.name}
        </p>
      ))}
    </Box>
  );
}

export type MainSearchProps = {
  suggestions: Array<Result>;
  publicCompanies: Array<Result>;
  privateCompanies: Array<Result>;
};

function MainSearch({
  suggestions,
  publicCompanies,
  privateCompanies,
}: MainSearchProps) {
  const [showSuggestion, openSuggestions] = useState<null | boolean>(false);

  return (
    <>
      <InputBase
        onClick={() => {
          openSuggestions(true);
        }}
        placeholder="Search…"
        inputProps={{'aria-label': 'search'}}
        sx={{
          color: 'inherit',
          width: '100%',
          '& .MuiInputBase-input': {
            padding: (theme) => theme.spacing(1, 1, 1, 0),
            transition: (theme) => theme.transitions.create('width'),
            width: {
              md: '100%',
              sm: '20ch',
            },
          },
        }}
      />
      {showSuggestion && (
        <ClickAwayListener
          onClickAway={() => {
            openSuggestions(false);
          }}>
          <Paper
            elevation={5}
            sx={{
              position: 'absolute',
              display: 'flex',
              top: '55px',
              backgroundColor: 'white',
              color: 'black',
              width: '100%',
              flexDirection: {
                xs: 'column',
                md: 'row',
              },
            }}>
            <ResultBox title="Suggestion" results={suggestions} />
            <ResultBox title="Public Companies" results={publicCompanies} />
            <ResultBox title="Private Companies" results={privateCompanies} />
          </Paper>
        </ClickAwayListener>
      )}
    </>
  );
}

export default MainSearch;
