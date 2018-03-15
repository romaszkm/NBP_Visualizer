package app.utils;

public class Currency {
    private String country;
    private String currency;
    private String code;

    public Currency(String country, String name, String code) {
        this.country = country;
        this.currency = name;
        this.code = code;
    }

    public Currency(String name, String code) {
        this.currency = name;
        this.code = code;
    }

    public String getName() {
        return currency != null ? currency : country;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
