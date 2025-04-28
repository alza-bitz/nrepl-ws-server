# nREPL WebSocket Server

A WebSocket server implementation for [nREPL](https://nrepl.org/), allowing browser-based REPL clients to connect to an nREPL server over a WebSocket connection.

## Prerequisites

- [Clojure CLI tools](https://clojure.org/guides/install_clojure)
- Java 11 or later

Alternatively, use an editor or environment that supports dev containers. The supplied [devcontainer.json](.devcontainer/devcontainer.json) will install all the needed prerequisites.

## Usage

### As a Library

Add the dependency to `deps.edn` :

```clojure
{nrepl-ws/nrepl-ws {:git/sha "xxxxxxx"}}
```

Start the server from a REPL :

```clojure
(require '[nrepl-ws.server.main :refer [system-config]])
(require '[integrant.core :as ig])

(def system (ig/init system-config))
```

Stop the server :

```clojure
(ig/halt! system)
```

### As a Project

Clone the repository :

```bash
git clone https://github.com/alza-bitz/nrepl-ws-server.git
cd nrepl-ws-server
```

Start the server from the CLI :

```bash
clojure -M:nrepl-ws
```

## Development

### Running the Tests

```bash
clojure -M:test
```

## License

Copyright © 2025 Alex Coyle

Distributed under the Eclipse Public License version 2.0.
