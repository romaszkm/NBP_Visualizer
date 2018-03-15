package app.db;

import app.ApplicationBean;
import app.api.APIHandler;
import app.api.xml.ExchangeRatesTable;
import app.api.xml.Rate;
import app.utils.Currency;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.Date;

public class DBInitializer {

    public boolean checkDatabaseExistence() {
        Connection connection = ApplicationBean.getInstance().getConnection();
        String sql = "" +
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.tables " +
                "WHERE TABLE_NAME = 'currencies' OR TABLE_NAME = 'rates'";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = connection.prepareStatement(sql);
            rs = ps.executeQuery();
            if (rs.next()) {
                if (rs.getInt(1) == 2)
                    return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                }
            }
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        return false;
    }

    public boolean isDatabaseUpToDate() throws Exception {
        LocalDate lastDate = getLastDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate today = LocalDate.now();
        if (lastDate.compareTo(today) < 0) {
            return false;
        }
        return true;
    }

    public void initDatabase() throws Exception {
        createSchema();
        populateData();
    }

    public void updateDatabase() throws Exception {
        Calendar lastDate = Calendar.getInstance();
        lastDate.setTime(getLastDate());
        lastDate.add(Calendar.DATE, 1);
        Calendar today = Calendar.getInstance();
        List<ExchangeRatesTable> ratesTables = APIHandler.getInstance().getTableForDate(lastDate, today);
        insertData(ratesTables);
    }

    private Date getLastDate() throws Exception {
        String sql = "SELECT MAX(ts) FROM rates";
        PreparedStatement ps = ApplicationBean.getInstance().getConnection().prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        //data can be empty
        if (rs.next())
            return new Date(rs.getDate(1).getTime());
        else {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            return sdf.parse(DBManager.FIRST_TABLE_DATE);
        }
    }

    private void createSchema() throws Exception {
        String sql = "" +
                "DROP TABLE IF EXISTS CURRENCIES, RATES;" +
                "CREATE TABLE CURRENCIES (" +
                "   CODE CHAR(3) PRIMARY KEY NOT NULL," +
                "   NAME TEXT NOT NULL" +
                ");" +
                "CREATE TABLE RATES (" +
                "   ID SERIAL PRIMARY KEY," +
                "   PRICE NUMERIC(10,8) NOT NULL," +
                "   CODE CHAR(3) NOT NULL REFERENCES CURRENCIES(CODE)," +
                "   TS DATE NOT NULL" +
                "); ";
        Connection connection = ApplicationBean.getInstance().getConnection();
        PreparedStatement ps = null;
        try {
            ps = connection.prepareStatement(sql);
            ps.executeUpdate();
        } catch (Exception e) {
            throw e;
        } finally {
            if (ps != null) {
                ps.close();
            }
        }
    }

    private void populateData() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar from = Calendar.getInstance();
        from.setTime(sdf.parse(DBManager.FIRST_TABLE_DATE));

        Calendar today = Calendar.getInstance();
        today.setTime(new java.util.Date());
        List<ExchangeRatesTable> ratesTableList = new LinkedList<>();
        while (from.compareTo(today) < 0) {
            Calendar to = Calendar.getInstance();
            to.setTime(from.getTime());
            to.add(Calendar.DATE, 50);
            if (to.compareTo(today) > 0) {
                to = today;
            }
            List<ExchangeRatesTable> ratesTables = APIHandler.getInstance().getTableForDate(from, to);
            if (ratesTables != null) {
                ratesTableList.addAll(ratesTables);
            }
            from.setTime(to.getTime());
            from.add(Calendar.DATE, 1);

        }
        System.out.println("Writing to DB");
        insertCurrencies(ratesTableList);
        insertData(ratesTableList);
    }

    private void insertData(List<ExchangeRatesTable> list) throws Exception {
        PreparedStatement ps = ApplicationBean.getInstance().getConnection().prepareStatement(DBManager.INSERT_RATES);
        for (ExchangeRatesTable ratesTable : list) {
            List<Rate> rates = ratesTable.getRatesList();
            DBManager.setValues(ratesTable, ps, rates);
        }
        ps.executeBatch();
        ps.close();
    }

    private void insertCurrencies(List<ExchangeRatesTable> list) throws Exception {
        PreparedStatement ps = ApplicationBean.getInstance().getConnection().prepareStatement(DBManager.INSERT_CURRENCY);
        Map<String, Currency> currencyMap = new HashMap<>();
        for (ExchangeRatesTable ratesTable : list) {
            List<Rate> rates = ratesTable.getRatesList();
            for (Rate rate : rates) {
                if (!currencyMap.containsKey(rate.getCode())) {
                    currencyMap.put(rate.getCode(), new Currency(rate.getCountry(), rate.getCurrency(), rate.getCode()));
                } else if (rate.getCurrency() != null && currencyMap.get(rate.getCode()).getCurrency() == null) {
                    currencyMap.remove(rate.getCode());
                    currencyMap.put(rate.getCode(), new Currency(rate.getCountry(), rate.getCurrency(), rate.getCode()));
                }
            }
        }
        for (String code : currencyMap.keySet()) {
            ps.setString(1, code);
            ps.setString(2, currencyMap.get(code).getName());
            ps.addBatch();
        }
        ps.executeBatch();
        ps.close();
    }

}
