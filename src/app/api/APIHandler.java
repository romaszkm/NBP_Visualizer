package app.api;

import app.api.xml.ArrayOfExchangeRatesTable;
import app.api.xml.ExchangeRatesTable;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

public class APIHandler {

    private static final String ADDRESS = "http://api.nbp.pl/api/exchangerates/tables/";
    private static final String FORMAT = "?format=xml";
    private static final int FETCH_TRIES = 5;

    private static APIHandler instance = new APIHandler();

    public static APIHandler getInstance() {
        return instance;
    }

    private APIHandler() {
    }

    public List<ExchangeRatesTable> getTableForDate(Calendar from, Calendar to) {
        ArrayOfExchangeRatesTable ret = null;
        for (int i = 1; i <= FETCH_TRIES; i++) {
            try {
                System.out.println("API: Try no. " + i + ": Fetching data from " + from.getTime() + " to " + to.getTime());
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                JAXBContext jaxbContext = JAXBContext.newInstance(ArrayOfExchangeRatesTable.class);
                Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
                URL url = new URL(ADDRESS + "A/" + sdf.format(from.getTime()) + "/" + sdf.format(to.getTime()) + FORMAT);
                InputStream stream = url.openStream();
                ret = ((ArrayOfExchangeRatesTable) jaxbUnmarshaller.unmarshal(stream));
                break;
            } catch (Exception e) {
                System.out.println("API: FAILED");
            }
        }
        return ret != null ? ret.getExchangeRatesTable() : null;
    }


}
