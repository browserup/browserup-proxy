package com.browserup.bup.mitm;

import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * A {@link CertificateInfoGenerator} that uses only a hostname to populate a new {@link CertificateInfo}. The
 * values in the upstream server's original X.509 certificate will be ignored.
 */
public class HostnameCertificateInfoGenerator implements CertificateInfoGenerator {
    /**
     * The 'O' to use for the impersonated server certificate when doing "simple" certificate impersonation (i.e.
     * not copying values from actual server certificate).
     */
    private static final String DEFAULT_IMPERSONATED_CERT_ORG = "Impersonated Certificate";

    /**
     * The 'O' to use for the impersonated server certificate when doing "simple" certificate impersonation.
     */
    private static final String DEFAULT_IMPERSONATED_CERT_ORG_UNIT = "LittleProxy MITM";

    @Override
    public CertificateInfo generate(List<String> hostnames, X509Certificate originalCertificate) {
        if (hostnames == null || hostnames.size() < 1) {
            throw new IllegalArgumentException("Cannot create X.509 certificate without server hostname");
        }

        // take the first entry as the CN
        String commonName = hostnames.get(0);

        return new CertificateInfo()
                .commonName(commonName)
                .organization(DEFAULT_IMPERSONATED_CERT_ORG)
                .organizationalUnit(DEFAULT_IMPERSONATED_CERT_ORG_UNIT)
                .notBefore(getNotBefore())
                .notAfter(getNotAfter())
                .subjectAlternativeNames(hostnames);
    }

    /**
     * Returns the default Not Before date for impersonated certificates. Defaults to the current date minus 1 year.
     */
    protected Instant getNotBefore() {
        return ZonedDateTime.now().minusYears(1).toInstant();
    }

    /**
     * Returns the default Not After date for impersonated certificates. Defaults to the current date plus 1 year.
     */
    protected Instant getNotAfter() {
        return ZonedDateTime.now().plusYears(1).toInstant();
    }
}
