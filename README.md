# z3950-ill-proxy
Z39.50 protocol proxy for DBC ILL

# Code

DBC private docker setup is in the submodule docker use

```bash
git submodule update --init --recursive
```

to check it out

# Manual Tests

Test ill
```bash
curl -d @test-requests/sb-ill-request1.json -H "Accept:application/json" -H "Content-Type:application/json" http://localhost:8080/ill ; echo
```


# yaz4j build dependency

See docker file. 
```bash
apt install jdk8-dbc libyaz4-dev swig g++
```