---
title: Site CLI - put-static-site
---

<Intro>
The 'put-static-site' command is used to upload a directory of files to the Site server with the correct resource handlers for serving a website (either static HTML pages or an SPA such as a react application).
</Intro>

## Arguments

<SiteCliArgs argString='[["-d" "--directory DIR" "Directory containing static site files"]
                   ["-p" "--path PATH" "Path the site will be available at. e.g -p mysite will make the site available at BASE_URI/mysite/index.html"]
                   ["-spa" "--spa SPA" "If set the html file will be served at PATH/app/*"]]'/>
