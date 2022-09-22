1.4.2 / 2022-08-09
==================

Angular
------------------  
* Fix angular package version (#150)

VUE3
------------------
  * Test vue3 responsive (#160)
  * Test vue3 placeholder (#159)
  * Test vue3 lazyload (#158)
  * Test vue3 analytics (#157)
  * Test vue3 accessibility tests (#156)
  * Add vue3 advanced image (#153)
  * Add vue3 sdk base with a build script (#151)


1.4.1 / 2022-05-31
==================

  * Fix wrong README links (#148)


1.4.0 / 2022-05-11
==================

React
-------
  * Fix incorrect responsive behavior with SSR responsive (#147)

Angular
-------
  * Fix incorrect responsive behavior with SSR responsive (#147)
  * Add Angular 12 as peer dep (#146)



1.3.0 / 2022-04-11
==================

React
-------
  * Add peer dependency for react 18 (#145)

Angular
-------
  * Update README.md (#144)
  * Remove production mode (#143)


1.2.2 / 2022-03-28
==================

  * fix svelte build requirements (#141)

1.2.1 / 2022-03-24
==================

  * bump minimist version (#142)

1.2.0 / 2022-03-13
==================

React
-------
  * Responsive Plugin will now respect steps to define max width (#133)

Angular
-------
   * Responsive Plugin will now respect steps to define max width (#133)

Html
-------
  * Update htmlVideo to use the right format for ogg/ogv (#138)
  * Adjust package.json fields (#136)

1.1.0 / 2022-02-13
==================

React
-------
 * Replace micro-bundle with rollup (#131)

Angular
-------
  * Fix muted attribute on angular video (#126)
  * Add video reference (#127)


1.0.1 / 2022-01-05
==================

Other changes
-----------------
  * Added missing comments

1.0.0 / 2022-01-04
==================

 New functionality
-----------------
* Feature/add analytics (#114)

Breaking changes
-----------------
 * Change plugin input to object instead of string (#119)

Other changes
-----------------
  * Update url-gen version 
  * Update issue templates
  * Update pull_request_template.md
  * Add test for muted video (#122)
  * Fix video src url with analytics (#121)
  * Add img attributes to angular (#118

1.0.0-beta.14 / 2021-12-5
==================

* update readme for packages (#108)
* resolve broken ts imports (#109)
* rename angular package (#107)



1.0.0-beta.13 / 2021-11-30
==================

* Add Lerna (#104)



1.0.0-beta.11 / 2021-09-14
==================

 * updated to html to latest version (#96)


1.0.0-beta.10 / 2021-09-14
==================

New functionality
-----------------
* added autoOptimalBreakpoints to picture tag (#86)

Other changes
-----------------
* changed to url-gen package name (#94)

1.0.0-beta.9 / 2021-07-04
==================

* Fix/common js build (#87)
* Add advanced picture (#84)



1.0.0-beta.8 / 2021-05-12
==================

Other changes
-----------------
  * Release pipeline testing


1.0.0-beta.7 / 2021-05-12
==================

Other changes
-----------------
  * Fix svelte npm package file contents


1.0.0-beta.6 / 2021-05-11
==================

Other changes
-----------------
  * Release pipeline testing


1.0.0-beta.4 / 2021-05-09
==================

New functionality
-----------------
  * align video attributes in React and Angular(#77)
  * Added ondestroy lifecycle hook to video component (#78)

Other changes
-----------------
  * Disable package-lock generation by adding .npmrc config
  * Feature/add vue sdk infrastructure (#74)
  * Add dynamic copy right date (#76)


1.0.0-beta.3 / 2021-04-12
==================

New functionality
-----------------
  * React - Add innerRef prop to AdvanceVideo component (#65)
  * React - Add video component (#59)
  * Shared(Breaking) - Update video sources to accept Transcode action (#64)

Other changes
-----------------
  * Svelte - Fix ts errors by adding importsNotUsedAsValues rule (#68)
  * Angular - Add enableProdMode() (#63)
  * Svelte tests - Add svelte ssr tests (#62)
  * Svelte tests - Add responsive unit tests for svelte sdk (#60)
  * Angular tests - add missing angular tests (#61)
  * React tests - add react e2e tests (#58)
  * Svelte docs - Add svelte sdk docs (#57)
  * React - Update dependency of react version to be ^16.3.0 || ^17.0.0 (#49)
  * Shared - fix status canceled error on placeholder (#54)
  * Shared - Feature/add video layer (#52)

1.0.0-beta.1 / 2021-02-24
==================

New functionality
-----------------
  * Add svelte sdk (#48)

Other changes
-----------------
  * Upgrade to base beta (#51)
  * Handle placeholder onerror (#46)
  * Add travis.yml file (#50)
  * Docs - Add version number to the docs reference (#47)
  * Docs - Finalize readme before release (#45)





1.0.0-beta.0 / 2021-02-02
==================

Other changes
---------------
* Beta release

1.0.0-alpha.5 / 2021-02-02
==================

Other changes
---------------
* Add changelog
* Test full release-cycle using jenkins

1.0.0-alpha.4 / 2021-01-31
==========================

Initial Release
-------------
  * Implement plugins - Responsive, Placeholder, Accessibility, Lazyload
  * Implement React and Angular image components
