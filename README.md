### 1. Compile, Launch etc.:

#### 1.1. With LightTable:

``` bash
lein cljx auto
lein cljsbuild auto dev
```

In LightTable: start http server by evaluating:
  server.clj

  logger.clj
  db.clj
  transform.clj
  client.cljs

#### 1.2. Without LightTable:

``` bash
lein cljx auto
lein cljsbuild auto dev
lein ring server
```


### 2. In browser / lighttable browser tab: http://localhost:8080/
