package app.api.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name="ArrayOfExchangeRatesTable")
public class ArrayOfExchangeRatesTable {

    private List<ExchangeRatesTable> exchangeRatesTable;

    @XmlElement(name="ExchangeRatesTable")
    public List<ExchangeRatesTable> getExchangeRatesTable(){
        return exchangeRatesTable;
    }

    public void setExchangeRatesTable(List<ExchangeRatesTable> exchangeRatesTable) {
        this.exchangeRatesTable = exchangeRatesTable;
    }
}
