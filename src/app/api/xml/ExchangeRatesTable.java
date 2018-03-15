package app.api.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name="ExchangeRatesTable")
public class ExchangeRatesTable {

    private Rates rates;
    private String table;
    private String effectiveDate;

    @XmlElement(name="Rates")
    public Rates getRates() {
        return rates;
    }

    public void setRates(Rates rates) {
        this.rates = rates;
    }

    public List<Rate> getRatesList() {
        return rates.getRates();
    }

    @XmlElement(name="Table")
    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    @XmlElement(name="EffectiveDate")
    public String getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(String effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    @Override
    public String toString() {
        return "table: " + table + "\n" +
                "date: " + effectiveDate + "\n";
    }

}
