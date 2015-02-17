#### A) With LightTable:

```bash
lein cljx auto
lein cljsbuild auto dev
```

In LightTable: start http server by evaluating:
  server.clj

  logger.clj
  db.clj
  transform.clj
  client.cljs

#### B) Without LightTable:

Overview:
```

                                                                                    Web Browser
                                                                          +--------------------------------+
                                                                          |                                |
                                                                          |            Web App             <-----------+
                               lein ring server                      +---< >     http://localhost:3000/    |           |
                           +----------------------+                  |    |                                |           |
                           |                      |                  |    +--------------< >---------------+           |
                      +---< >     HTTP Server    < >-----------------+    |                                |           |
                      |    |        3000          |                  |    |   ClojureScript Browser REPL   |           |
                      |    |                      |                  +---< >      http://localhost:9000/   |           |
     dbase            |    +---------< >----------+                       |                                |           |
 +-----------+        |    |                      |                       +---------------+----------------+           |
 |           |        |    |       compiler       |                                       |                            |
 |          < >-------+    |  :auto-reload? true  <---------+                             |                            |
 |           |        |    |                      |         |                             |                            |
 +-----------+        |    +---------< >----------+         |                             |                            |
                      |    |                      |         |                             |                            |
                      |    |      nREPL Server   < >----+   |                             |                            |
                      +---< >        4500         |     |   |           emacs             |                            |
                           |                      |     |   |  +----------+---------+     |                            |
                           +----------------------+     |   |  |          |         |     |                            |
                                                        |   +--<   *.clj  |  cider < >----+                            |
                                                        |      |          |         |                                  |
                                                        |      +----------+---------+         +---------------------+  |
                                                        |      |          |         |         | lein cljsbuild auto |  |
                                                        +-----< >  cider  | *.cljs  >--------->                     >--+
                                                               |          |         |         |                     |
                                                               +----------+---------+         +---------------------+

```

##### 1. Launch leiningen tasks:

```bash
# lein cljx auto          # TODO fix the onelog-logger
lein cljsbuild auto dev
lein ring server
```

The http://localhost:3000/ gets opened in the browser.

##### 2. Open a REPL using:
```bash
lein repl
```

(or in emacs using cider-jack-in) and make it a ClojureScript Browser REPL:

```clojure
user> (require 'cljs.repl.browser)
nil
user> (cemerick.piggieback/cljs-repl
  :repl-env (cljs.repl.browser/repl-env :port 9000))
Compiling client js ...
Waiting for browser to connect ...
Type `:cljs/quit` to stop the ClojureScript REPL
nil
cljs.user> (js/alert "Hello from ClojureScript Browser REPL!")
```

The ClojureScript Browser REPL waits for the http://localhost:9000/repl request from web browser.
After that a popup "Hello from ClojureScript Browser REPL!" appears in the browser.
