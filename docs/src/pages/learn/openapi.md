---
title: Open Api Tutorial
---

Make sure you have [set up the Site cli](cli)

Also check out the [XTDB Primer](xtdb) to understand the data for this tutorial

First we will build and install swager-ui

```shell
cd opt/swager-ui
./install
```

[Login](login)
then [Open the tool](http://localhost:2021/swagger-ui/index.html?url=/_site/apis/site/openapi.json#/APIs/put_apis__id_)


## Minimal Schema Setup
```json
{
  "openapi": "3.0.2",
  "info": {
    "version": "1.0.0",
    "title": "Movies"
  },
  "paths": {}
}
```

TODO   415 Undocumented Error: Unsupported Media Type from swagger ui when PUT app




