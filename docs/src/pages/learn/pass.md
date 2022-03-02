---
title: Site’s authorization module
---

APIs are also able to benefit from Site’s authorization module, Pass, providing Policy-Based Access Control, loosely based on XACML.

Access is configured via rules. Here we allow all for a login page:

```clojure
;login-form-rule.edn
{:type "Rule"
 :target
 #juxt.site.alpha/as-str
 [[request :ring.request/path "/_site/login.html"]
  [request :ring.request/method #{:get :head :options}]]
 :effect :juxt.pass.alpha/allow}
```
```shell
site put-rule -n site-login-form -r login-form-rule.edn
```

TODO walk through `opt/login-form` example