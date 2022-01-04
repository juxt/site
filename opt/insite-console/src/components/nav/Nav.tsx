import * as React from 'react';
import {alpha} from '@mui/material/styles';
import AppBar from '@mui/material/AppBar';
import Box from '@mui/material/Box';
import Toolbar from '@mui/material/Toolbar';
import IconButton from '@mui/material/IconButton';
import Typography from '@mui/material/Typography';
import Badge from '@mui/material/Badge';
import MenuItem from '@mui/material/MenuItem';
import Menu from '@mui/material/Menu';
import HomeIcon from '@mui/icons-material/Home';
import InfoIcon from '@mui/icons-material/Info';
import MenuIcon from '@mui/icons-material/Menu';
import SearchIcon from '@mui/icons-material/Search';
import AccountCircle from '@mui/icons-material/AccountCircle';
import MailIcon from '@mui/icons-material/Mail';
import NotificationsIcon from '@mui/icons-material/Notifications';
import MoreIcon from '@mui/icons-material/MoreVert';
import Container from '@mui/material/Container';
import Drawer from '@mui/material/Drawer';
import ListItem from '@mui/material/ListItem';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import {useScrollTrigger} from '@mui/material';
import {SidenavDesktop} from './Sidenav';
import {Link as RouterLink} from 'react-location';
import MainSearch from './MainSearch';

const drawerWidth = 100;

type ScrollProps = {
  children: React.ReactElement;
};

function ElevationScroll(props: ScrollProps) {
  const {children} = props;
  // Note that you normally won't need to set the window ref as useScrollTrigger
  // will default to window.
  // This is only being set here because the demo is in an iframe.
  const trigger = useScrollTrigger({
    disableHysteresis: true,
    threshold: 0,
  });

  return React.cloneElement(children, {
    elevation: trigger ? 4 : 0,
  });
}

function NavDrawer() {
  const iconSize = 'large';
  const navItems = [
    {
      name: 'Home',
      path: '/',
      icon: <HomeIcon fontSize={iconSize} />,
    },
    {
      name: 'Requests',
      path: '/requests',
      icon: <InfoIcon fontSize={iconSize} />,
    },
    {
      name: 'APIs',
      path: '/apis',
      icon: <SearchIcon fontSize={iconSize} />,
    },
  ];
  const color = (isActive: boolean) => (isActive ? 'primary.main' : 'black');
  return (
    <>
      {navItems.map((item) => {
        return (
          <RouterLink key={item.path} to={item.path}>
            {({isActive}) => {
              return (
                <ListItem
                  sx={{
                    flexDirection: 'column',
                    py: 4,
                    color: color(isActive),
                  }}
                  button>
                  <ListItemIcon
                    sx={{
                      minWidth: 0,
                      fontSize: 16,
                      color: color(isActive),
                    }}>
                    {item.icon}
                  </ListItemIcon>
                  <ListItemText primary={item.name} />
                </ListItem>
              );
            }}
          </RouterLink>
        );
      })}
    </>
  );
}

type AccountMenuProps = {
  mobileOpen: boolean;
  handleDrawerClose: () => void;
};

function RenderNavMenu({mobileOpen, handleDrawerClose}: AccountMenuProps) {
  return (
    <Drawer
      container={window.document.body}
      variant="temporary"
      open={mobileOpen}
      onClose={handleDrawerClose}
      ModalProps={{
        keepMounted: true, // Better open performance on mobile.
      }}
      sx={{
        zIndex: 99,
        display: {xs: 'block', sm: 'none'},
        '& .MuiDrawer-paper': {boxSizing: 'border-box', width: drawerWidth},
      }}>
      <NavDrawer />
    </Drawer>
  );
}

export function PrimaryNavBar() {
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);
  const [mobileMoreAnchorEl, setMobileMoreAnchorEl] =
    React.useState<null | HTMLElement>(null);

  const isMenuOpen = Boolean(anchorEl);
  const isMobileMenuOpen = Boolean(mobileMoreAnchorEl);

  const handleProfileMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleMobileMenuClose = () => {
    setMobileMoreAnchorEl(null);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
    handleMobileMenuClose();
  };

  const handleMobileMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setMobileMoreAnchorEl(event.currentTarget);
  };

  const [navMenuOpen, setNavMenuOpen] = React.useState(false);
  const handleNavMenuToggle = () => {
    setNavMenuOpen(!navMenuOpen);
  };

  const handleLogout = () => {
    localStorage.removeItem('user');
    window.location.reload();
  };

  const menuId = 'primary-search-account-menu';
  const renderAccountMenu = (
    <Menu
      anchorEl={anchorEl}
      anchorOrigin={{
        vertical: 'top',
        horizontal: 'right',
      }}
      id={menuId}
      keepMounted
      transformOrigin={{
        vertical: 'top',
        horizontal: 'right',
      }}
      open={isMenuOpen}
      onClose={handleMenuClose}>
      <MenuItem onClick={handleLogout}>Logout</MenuItem>
    </Menu>
  );

  const mobileMenuId = 'primary-search-account-menu-mobile';
  const renderMobileMenu = (
    <Menu
      anchorEl={mobileMoreAnchorEl}
      anchorOrigin={{
        vertical: 'top',
        horizontal: 'right',
      }}
      id={mobileMenuId}
      keepMounted
      transformOrigin={{
        vertical: 'top',
        horizontal: 'right',
      }}
      open={isMobileMenuOpen}
      onClose={handleMobileMenuClose}>
      <MenuItem>
        <IconButton size="large" aria-label="show 4 new mails" color="inherit">
          <Badge badgeContent={4} color="error">
            <MailIcon />
          </Badge>
        </IconButton>
        <p>Messages</p>
      </MenuItem>
      <MenuItem>
        <IconButton
          size="large"
          aria-label="show 17 new notifications"
          color="inherit">
          <Badge badgeContent={17} color="error">
            <NotificationsIcon />
          </Badge>
        </IconButton>
        <p>Notifications</p>
      </MenuItem>
      <MenuItem onClick={handleProfileMenuOpen}>
        <IconButton
          size="large"
          aria-label="account of current user"
          aria-controls="primary-search-account-menu"
          aria-haspopup="true"
          color="inherit">
          <AccountCircle />
        </IconButton>
        <p>Profile</p>
      </MenuItem>
    </Menu>
  );

  return (
    <Box sx={{mb: '64px'}}>
      <RenderNavMenu
        mobileOpen={navMenuOpen}
        handleDrawerClose={() => setNavMenuOpen(false)}
      />
      <ElevationScroll>
        <AppBar
          color="primary"
          position="fixed"
          sx={{zIndex: (theme) => theme.zIndex.drawer + 1}}>
          <Toolbar>
            <IconButton
              size="large"
              edge="start"
              color="inherit"
              onClick={handleNavMenuToggle}
              aria-label="open drawer"
              sx={{mr: 2, display: {xs: 'block', sm: 'none'}}}>
              <MenuIcon />
            </IconButton>
            <Typography
              variant="h6"
              noWrap
              component="div"
              sx={{overflow: 'visible', display: {xs: 'none', sm: 'block'}}}>
              InSite
            </Typography>
            <Box
              sx={{
                position: 'relative',
                display: 'flex',
                justifyContent: 'left',
                alignItems: 'center',
                borderRadius: 1,
                backgroundColor: (theme) =>
                  alpha(theme.palette.common.white, 0.15),
                '&:hover': {
                  backgroundColor: (theme) =>
                    alpha(theme.palette.common.white, 0.25),
                },
                marginRight: (theme) => theme.spacing(2),
                marginLeft: (theme) => ({
                  sm: theme.spacing(3),
                  xs: 0,
                }),
                width: '100%',
              }}>
              <Box
                sx={{
                  padding: (theme) => theme.spacing(0, 2),
                  height: '100%',
                  pointerEvents: 'none',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}>
                <SearchIcon />
              </Box>
              <MainSearch
                suggestions={[]}
                publicCompanies={[]}
                privateCompanies={[]}
              />
            </Box>
            <Box sx={{flexGrow: 1}} />
            <Box sx={{display: {xs: 'none', md: 'flex'}}}>
              <IconButton
                size="large"
                aria-label="show 4 new mails"
                color="inherit">
                <Badge badgeContent={4} color="error">
                  <MailIcon />
                </Badge>
              </IconButton>
              <IconButton
                size="large"
                aria-label="show 17 new notifications"
                color="inherit">
                <Badge badgeContent={17} color="error">
                  <NotificationsIcon />
                </Badge>
              </IconButton>
              <IconButton
                size="large"
                edge="end"
                aria-label="account of current user"
                aria-controls={menuId}
                aria-haspopup="true"
                onClick={handleProfileMenuOpen}
                color="inherit">
                <AccountCircle />
              </IconButton>
            </Box>
            <Box sx={{display: {xs: 'flex', md: 'none'}}}>
              <IconButton
                size="large"
                aria-label="show more"
                aria-controls={mobileMenuId}
                aria-haspopup="true"
                onClick={handleMobileMenuOpen}
                color="inherit">
                <MoreIcon />
              </IconButton>
            </Box>
          </Toolbar>
        </AppBar>
      </ElevationScroll>
      {renderMobileMenu}
      {renderAccountMenu}
    </Box>
  );
}

export function PageLayout({children}: {children: React.ReactNode}) {
  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        minHeight: '100vh',
        backgroundColor: 'background',
      }}>
      <PrimaryNavBar />
      <SidenavDesktop drawerWidth={drawerWidth} DrawerItems={<NavDrawer />} />
      <Container
        sx={{
          maxWidth: {xs: 'md', md: 'xl'},
          pl: {sm: `${drawerWidth + 28}px`},
        }}>
        {children}
      </Container>
    </Box>
  );
}
