# Changelog

# Upcoming

Performance, Page, and Network Assertions against the HAR.

# [1.1.0]
- ZIP distribution with launch scripts, SSL certificates and keys
- Dependency updates

# [1.0.0]
- Initial fork based on BrowserMob Proxy
- HTTP/2 support via Netty 4.1.34 upgrade
- Java 11 support
- Upgrades to dependencies (mockito, etc)
- Upgrade to an actively maintained, [LittleProxy](https://github.com/mrog/LittleProxy) fork
- Switch to Gradle
- Import a new, better HAR reader from https://github.com/sdstoehr/har-reader
- Extend the har reader with filtering/finding capabilities
- Modify every existing file by adding a header to ensure compliance with Apache License
- Rename our fork to our own name, BrowserUp, as we will be investing in it heavily. 
    We have no relation to BrowserMob, which was a company acquired by Neustar in 2010.
- Updates to the Readme to remove legacy proxyserver information
