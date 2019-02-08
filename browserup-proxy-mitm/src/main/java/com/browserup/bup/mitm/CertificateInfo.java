package com.browserup.bup.mitm;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Container for X.509 Certificate information.
 */
public class CertificateInfo {
    private String commonName;
    private String organization;
    private String organizationalUnit;

    private String email;
    private String locality;
    private String state;
    private String countryCode;

    private Instant notBefore;
    private Instant notAfter;

    private List<String> subjectAlternativeNames = Collections.emptyList();

    public String getCommonName() {
        return commonName;
    }

    public String getOrganization() {
        return organization;
    }

    public String getOrganizationalUnit() {
        return organizationalUnit;
    }

    public Instant getNotBefore() {
        return notBefore;
    }

    public Instant getNotAfter() {
        return notAfter;
    }

    public String getEmail() {
        return email;
    }

    public String getLocality() {
        return locality;
    }

    public String getState() {
        return state;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public List<String> getSubjectAlternativeNames() {
        return subjectAlternativeNames;
    }

    public CertificateInfo commonName(String commonName) {
        this.commonName = commonName;
        return this;
    }

    public CertificateInfo organization(String organization) {
        this.organization = organization;
        return this;
    }

    public CertificateInfo organizationalUnit(String organizationalUnit) {
        this.organizationalUnit = organizationalUnit;
        return this;
    }

    public CertificateInfo notBefore(Instant notBefore) {
        this.notBefore = notBefore;
        return this;
    }

    public CertificateInfo notAfter(Instant notAfter) {
        this.notAfter = notAfter;
        return this;
    }

    public CertificateInfo email(String email) {
        this.email = email;
        return this;
    }

    public CertificateInfo locality(String locality) {
        this.locality = locality;
        return this;
    }

    public CertificateInfo state(String state) {
        this.state = state;
        return this;
    }

    public CertificateInfo countryCode(String countryCode) {
        this.countryCode = countryCode;
        return this;
    }

    public CertificateInfo subjectAlternativeNames(List<String> subjectAlternativeNames) {
        this.subjectAlternativeNames = subjectAlternativeNames;
        return this;
    }
}
