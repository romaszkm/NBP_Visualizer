package app.db;

import app.ApplicationBean;
import app.api.xml.ExchangeRatesTable;
import app.api.xml.Rate;
import app.utils.Currency;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

public class DBManager {

    protected static final String INSERT_RATES = "INSERT INTO RATES (PRICE, CODE, TS) VALUES (?, ?, ?)";
    protected static final String INSERT_CURRENCY = "INSERT INTO CURRENCIES (CODE, NAME) VALUES (?, ?)";
    public static final String FIRST_TABLE_DATE = "2002-01-02";

    public void insertRates(ExchangeRatesTable exchangeRatesTable) throws Exception {
        PreparedStatement ps = ApplicationBean.getInstance().getConnection().prepareStatement(DBManager.INSERT_RATES);
        List<Rate> rates = exchangeRatesTable.getRatesList();
        setValues(exchangeRatesTable, ps, rates);
        ps.executeBatch();
        ps.close();
    }

    protected static void setValues(ExchangeRatesTable exchangeRatesTable, PreparedStatement ps, List<Rate> rates) throws Exception {
        for (Rate rate : rates) {
            ps.setDouble(1, Double.parseDouble(rate.getMid()));
            ps.setString(2, rate.getCode());
            ps.setDate(3, Date.valueOf(exchangeRatesTable.getEffectiveDate()));
            ps.addBatch();
        }
    }

    public List<Rate> getRate(String code, LocalDate from, LocalDate to) throws Exception {
        Calendar f = Calendar.getInstance();
        f.setTime(java.util.Date.from(from.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        Calendar t = Calendar.getInstance();
        t.setTime(java.util.Date.from(to.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        return getRate(code, f, t);
    }

    public List<Rate> getRate(String code, Calendar from, Calendar to) throws Exception {
        String sql = "" +
                "   SELECT price, ts FROM RATES " +
                "\n WHERE ts >= ? AND ts <= ?" +
                "\n AND code = ?";
        PreparedStatement ps = ApplicationBean.getInstance().getConnection().prepareStatement(sql);
        ps.setDate(1, new Date(from.getTimeInMillis()));
        ps.setDate(2, new Date(to.getTimeInMillis()));
        ps.setString(3, code);
        ResultSet rs = ps.executeQuery();
        List<Rate> list = new LinkedList<>();
        while (rs.next()) {
            Rate rate = new Rate();
            rate.setMid(rs.getString(1));
            rate.setTs(rs.getString(2));
            rate.setCode(code);
            list.add(rate);
        }
        return list;
    }

    public List<Currency> getCurrencies() throws Exception {
        String sql = "SELECT * FROM CURRENCIES";
        PreparedStatement ps = ApplicationBean.getInstance().getConnection().prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        List<Currency> list = new LinkedList<>();
        while (rs.next()) {
            String code = rs.getString("code");
            String name = rs.getString("name");
            list.add(new Currency(name, code));
        }
        return list;
    }

}
