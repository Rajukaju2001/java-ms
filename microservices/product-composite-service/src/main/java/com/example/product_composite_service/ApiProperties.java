package com.example.product_composite_service;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "api")
public class ApiProperties {

    private Common common;
    private Map<String, String> responseCodes;
    private Map<String, Operation> productComposite;

    public Common getCommon() { return common; }
    public void setCommon(Common common) { this.common = common; }

    public Map<String, String> getResponseCodes() { return responseCodes; }
    public void setResponseCodes(Map<String, String> responseCodes) { this.responseCodes = responseCodes; }

    public Map<String, Operation> getProductComposite() { return productComposite; }
    public void setProductComposite(Map<String, Operation> productComposite) { this.productComposite = productComposite; }

    // Nested classes

    public static class Common {
        private String version;
        private String title;
        private String description;
        private String termsOfService;
        private String license;
        private String licenseUrl;
        private String externalDocDesc;
        private String externalDocUrl;
        private Contact contact;

        // getters/setters
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getTermsOfService() { return termsOfService; }
        public void setTermsOfService(String termsOfService) { this.termsOfService = termsOfService; }
        public String getLicense() { return license; }
        public void setLicense(String license) { this.license = license; }
        public String getLicenseUrl() { return licenseUrl; }
        public void setLicenseUrl(String licenseUrl) { this.licenseUrl = licenseUrl; }
        public String getExternalDocDesc() { return externalDocDesc; }
        public void setExternalDocDesc(String externalDocDesc) { this.externalDocDesc = externalDocDesc; }
        public String getExternalDocUrl() { return externalDocUrl; }
        public void setExternalDocUrl(String externalDocUrl) { this.externalDocUrl = externalDocUrl; }
        public Contact getContact() { return contact; }
        public void setContact(Contact contact) { this.contact = contact; }
    }

    public static class Contact {
        private String name;
        private String url;
        private String email;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class Operation {
        private String description;
        private String notes;

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }
}
