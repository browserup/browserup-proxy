# Changelog

# [2.1.0]

# [2.0.0]
- Performance, Page and Network assertions. The proxy now lets you "assert" over the REST API about the recent HTTP traffic. If you are familiar with HAR files, this lets you skip handling them directly for most use-cases. Some highlights (See the rest in: https://github.com/browserup/browserup-proxy/commit/889aeda6d27b05b50714b754f6e43b3a600e6d9b):
    - assertMostRecentResponseTimeLessThanOrEqual
    - assertResponseTimeLessThanOrEqual
    - assertMostRecentResponseContentContains
    - assertMostRecentResponseContentMatches
    - assertAnyUrlContentLengthLessThanOrEquals
    - assertAnyUrlContentMatches
    - assertAnyUrlContentDoesNotContain
    - assertAnyUrlResponseHeaderContains
    - assertResponseStatusCode
    - assertMostRecentResponseContentLengthLessThanOrEqual
- Fix compatibility with the HAR viewer by setting correct defaults per the HAR spec
- Update Netty to the latest version
- Merge in contribution from @jrgp to allow upstream proxy connections to utilize HTTPS.
- Default to the step name "Default" when requests come through and no page is set yet.

# [1.2.1]
- No changes, binaries compiled for Java 8+.

# [1.2.0]
- Add much-needed handling of Brotli Compression. Brotli has become a popular alternative to GZIP compression scheme, and is utilized all over the web by websites including Google and Facebook. The proxy can now decompress and recognize brotli.
- Add recognition for variant (versioned) JSON content type strings. Previously, response bodies for JSON content types with content types like  "application/something-v1+json"  would not be captured. Now they will be.
- Fix a credentials leak where the basic auth header was being added to non-connect request types.
- Dependency updates

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
