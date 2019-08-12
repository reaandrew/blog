Cracking JWTs

https://github.com/danielmiessler/SecLists/tree/master/Passwords/Cracked-Hashes

https://willhaley.com/blog/generate-jwt-with-bash/

1. Download the cracked hash dictionary

2. Get a sample token which will have been signed by the provider

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBQ01FIiwidXNlcl9pZCI6ImZ1YmFyIiwidXNlcl9uYW1lIjoiYWJjQGFjbWUudWsiLCJzY29wZSI6WyJzb21lLWFwcCJdLCJpc3MiOiJBQ01FIEF1dGhvcmlzYXRpb24gU2VydmljZSIsImV4cCI6MTU2OTIxMDE1MCwiaWF0IjoxNTY5MTIzNzUwLCJjYXBhYmlsaXRpZXMiOlsiRE9fU09NRVRISU5HIl0sImp0aSI6ImE3YjZhMDVkLWM3N2MtNDAzZS1hZDQyLTljZDdiMjMwODViZCJ9.omH1bZZH87gBe3vttdYhGHKIdOzeCGiPvvd1fUZMvps
```

The goal is to recreate the token
