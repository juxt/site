import Toolbar from '@mui/material/Toolbar';
import Drawer from '@mui/material/Drawer';
import List from '@mui/material/List';

export type SidenavProps = {
  drawerWidth: number;
  DrawerItems: React.ReactElement;
};

export function SidenavDesktop({drawerWidth, DrawerItems}: SidenavProps) {
  return (
    <Drawer
      variant="permanent"
      sx={{
        width: drawerWidth,
        display: {xs: 'none', sm: 'block'},
        flexShrink: 0,
        [`& .MuiDrawer-paper`]: {
          width: drawerWidth,
          boxSizing: 'border-box',
        },
      }}>
      <Toolbar />
      <List sx={{overflow: 'hidden'}}>{DrawerItems}</List>
    </Drawer>
  );
}
