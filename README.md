# z3950-ill-proxy
Z39.50 protocol proxy for DBC ILL

# Code

DBC private docker setup is in the submodule docker use

```bash
git submodule update --init --recursive
```

to check it out

# Manual Tests

The `test-requests` catalog contains test requests for quick tests and a small script for posting the data to a test server.

quick the build and run with no changes to the `Dockerfile`
```bash
 mvn package && docker run -ti -p 8080:8080 -v${PWD}/target/z3950-ill-proxy-1.0-SNAPSHO.war:/payara-micro/wars/z3950-ill-proxy-1.0-SNAPSHOT.war docker-i.dbc.dk/z3950-ill-proxy
```


Test ill
```bash
cd test-requests
./run-holdings-request.sh http://localhost:8080/ill ./sb-ill-request1.json
```

# yaz4j build dependency

See Dockerfile for details
```bash
apt install jdk8-dbc libyaz4-dev swig g++
```