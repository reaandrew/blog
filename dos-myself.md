I had some concerns that the rate limiting we had setup was not correctly configured so I wanted to create a test to assert what the actual behaviour was.  This was a interesting test to write as it was essentially a Denial of Service (DOS) attack, the expectation being that when I execute the test the different systems continue to work ass expected i.e. response times and error rates do not increase.

Before I started to creeate the test there were a several requirements I gave myself, the first was that I needed a route to the target service which did not go through AWS Cloud Front (due to their terms of service), second I wanted to use a low bandwidth solution which did not require mass of data being sent e.g. a load test; and the third was I wanted a CLI tool since my perferred workspace does not include a window manager.

There are quite a few tools which fit this criteria and the two which I tried out were slowloris and goldeneye.  THe way they work is by opennig up sockets to the target and trickling a header or two at set intervals to keep the connection alive.  The hope being that the target exhausts its available connections denying those which are actually using the service.  

The first problem that I hit was due to TLS SNI since I was using a route which was not through cloud front the DNS did not match the permitted names on the TLS Certificate.  To fix this problem I needed to configure the tool to not validate the certificate.  The second problem was that I needed to set the host header as we were using host header redirection in the AWS ALB.

QUESTION: Was this the reason I moved to golden eye as slowloris did not permit the use of custom headers?  I think I did alter the source code to set these and I think that I moved onto goldensys as slowloris did not appear to be working - this is was led to the investigation of the ALB buffering the requests.


