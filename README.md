# z3950-ill-proxy
Z39.50 protocol proxy for DBC ILL

# Code

DBC private docker setup is in the submodule docker use

```bash
git submodule update --init --recursive
```

to check it out


# yaz4j build dependency

See docker file. 
```bash
apt install jdk8-dbc libyaz4-dev swig g++
```