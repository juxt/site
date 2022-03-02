---
title: Site cli setup
---

<Intro>
Site comes with a command line interface to streamline tasks.
</Intro>


add the site tool to your PATH (e.g. add to ~/.bashrc)
```bash
export PATH=$PATH:/path/to/site/bin
```

setup [pass](https://www.passwordstore.org/) so we don't need to authenticate every time.
If you don't have pass setup you can follow this [guide](https://www.thepolyglotdeveloper.com/2018/12/manage-passwords-gpg-command-line-pass/)

create a password for site with your system username:
```shell
pass generate -n site/local/<USER>
```
Save the password returned for the next step

Lets start and configure Site from a [Clojure REPL](https://github.com/juxt/site#the-repl)


```clojure
(start)
(put-site-api!)
(put-auth-resources!)
(put-superuser-role!)
(put-superuser! "<USER>" "Administrator" "<PASS>")
; set tokens to last a day
(put! (assoc (e "http://localhost:2021/_site/token")  ::pass/expires-in (* 24 3600)))
```

Now we can check the site tool by fetching an auth token
```shell
pass site/local/<USER>
site get-token
```

Great! Now you are ready to set up an [OpenAPI](openapi) or [Graphql](graphql) API