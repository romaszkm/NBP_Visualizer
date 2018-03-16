package app.gui;

import app.api.xml.Rate;
import app.db.DBManager;
import app.utils.Currency;
import com.workday.insights.timeseries.arima.Arima;
import com.workday.insights.timeseries.arima.struct.ArimaParams;
import com.workday.insights.timeseries.arima.struct.ForecastResult;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Popup;
import javafx.util.Callback;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

public class Controller {

    private final DBManager dbManager = new DBManager();
    private Set<String> selection = new HashSet<>();
    private Popup selectionPopup;
    private Popup infoPopup;
    private LocalDate lastDate;

    public Controller() {
    }

    @FXML
    private LineChart<String, Double> chart;

    @FXML
    private DatePicker fromDate;

    @FXML
    private DatePicker toDate;

    @FXML
    private Button buttonChoice;

    @FXML
    private CheckBox checkBoxPrediction;

    @FXML
    private Label labelInfo;

    @FXML
    private void initialize() {
        initInfoPopup();
        //sleek lines on chart
        chart.setCreateSymbols(false);
        //listeners
        buttonChoice.setOnAction(e -> showCurrenciesPopup());
        checkBoxPrediction.setOnAction(e -> draw());
    }

    private void draw() {
        if (selection.isEmpty())
            return;
        showInfoPopup(true, "Fetching data");
        Task task = new Task() {
            @Override
            protected Object call() {
                try {
                    //create data series for each selected code
                    List<XYChart.Series<String, Double>> data = getData();
                    Platform.runLater(() -> {
                        chart.getData().clear();
                        chart.getData().addAll(data);
                        Node line = chart.getData().get(0).getNode().lookup(".chart-series-line");
                        line.setStyle("-fx-stroke: rgba(0,0,0,0);");
                        showInfoPopup(false, "");
                    });
                } catch (Exception e) {
                    showExceptionPopup(e);
                }
                return null;
            }
        };
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private List<XYChart.Series<String, Double>> getData() throws Exception {
        List<XYChart.Series<String, Double>> data = new LinkedList<>();
        // 'hack' to properly display date range
        data.add(getXAxis());
        for (String code : selection) {
            List<Rate> list = dbManager.getRate(code, fromDate.getValue(), toDate.getValue());
            XYChart.Series<String, Double> series = getSeries(list);
            series.setName(code);
            data.add(series);

            if (checkBoxPrediction.isSelected() &&
                    (toDate.getValue().compareTo(lastDate) == 0 || toDate.getValue().compareTo(LocalDate.now()) == 0)) {
                XYChart.Series<String, Double> forecast = getPrediction(list);
                data.add(forecast);
            }
        }
        return data;
    }

    private XYChart.Series<String, Double> getXAxis() {
        XYChart.Series<String, Double> series = new XYChart.Series<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar from = Calendar.getInstance();
        from.setTime(java.util.Date.from(fromDate.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        Calendar to = Calendar.getInstance();
        to.setTime(java.util.Date.from(toDate.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        while (from.compareTo(to) <= 0) {
            XYChart.Data<String, Double> dataPoint = new XYChart.Data<>(sdf.format(from.getTime()), 0.0);
            series.getData().add(dataPoint);
            from.add(Calendar.DATE, 1);
        }
        series.setName("Legend");
        return series;
    }

    private XYChart.Series<String, Double> getSeries(List<Rate> list) {
        XYChart.Series<String, Double> series = new XYChart.Series<>();
        for (int i = 0; i < list.size(); i++) {
            XYChart.Data<String, Double> dataPoint = new XYChart.Data<>(list.get(i).getTs(), Double.parseDouble(list.get(i).getMid()));
            series.getData().add(dataPoint);
        }
        return series;
    }

    private XYChart.Series<String, Double> getPrediction(List<Rate> list) throws Exception {
        List<Rate> listPredict = list;
        String code = list.get(0).getCode();
        if (fromDate.getValue().isAfter(toDate.getValue().minusMonths(12)))
            listPredict = dbManager.getRate(code, toDate.getValue().minusMonths(12), toDate.getValue());
        XYChart.Series<String, Double> forecast = new XYChart.Series<>();
        double[] dataArray = new double[listPredict.size()];
        for (int i = 0; i < listPredict.size(); i++) {
            dataArray[i] = Double.parseDouble(listPredict.get(i).getMid());
        }
        ForecastResult forecastResult = Arima.forecast_arima(dataArray, 21, new ArimaParams(3, 0, 3, 1, 1, 0, 0));
        double[] forecastData = forecastResult.getForecast();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar to = Calendar.getInstance();
        to.setTime(java.util.Date.from(toDate.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant()));

        for (int i = 0; i < forecastData.length; i++) {
            XYChart.Data<String, Double> dataPoint = new XYChart.Data<>(sdf.format(to.getTime()), forecastData[i]);
            to.add(Calendar.DATE, 1);
            forecast.getData().add(dataPoint);
        }
        forecast.setName(code + " - prediction");
        return forecast;
    }

    public void setData() {
        setDatePickers();
        initSelectionPopup();
    }

    private void setDatePickers() {
        try {
            lastDate = dbManager.getLastDate();
            fromDate.setValue(lastDate.minusMonths(6));
            toDate.setValue(lastDate);
        } catch (Exception e) {
            showExceptionPopup(e);
        }

        final Callback<DatePicker, DateCell> dayCellFactory = (final DatePicker datePicker) -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (item.isAfter(LocalDate.now()) || item.isBefore(LocalDate.parse(DBManager.FIRST_TABLE_DATE))) {
                    setDisable(true);
                    setStyle("-fx-background-color: #ffc0cb;");
                }
            }
        };

        fromDate.setDayCellFactory(dayCellFactory);
        toDate.setDayCellFactory(dayCellFactory);
        fromDate.setOnAction(e -> {
            if (fromDate.getValue().isBefore(LocalDate.parse(DBManager.FIRST_TABLE_DATE))) {
                fromDate.setValue(LocalDate.parse(DBManager.FIRST_TABLE_DATE));
            }
            draw();
        });
        toDate.setOnAction(e -> {
            if (toDate.getValue().isAfter(LocalDate.now())) {
                toDate.setValue(LocalDate.now());
            } else if (toDate.getValue().isBefore(lastDate)) {
                checkBoxPrediction.setDisable(true);
                labelInfo.setVisible(true);
            } else {
                checkBoxPrediction.setDisable(false);
                labelInfo.setVisible(false);
            }
            draw();
        });
    }

    private void initSelectionPopup() {
        try {
            int codesInRow = 5;
            List<Currency> currencyList = null;
            currencyList = dbManager.getCurrencies();

            selectionPopup = new Popup();

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(0, 10, 0, 10));

            Button button = new Button("Close");
            button.setOnAction(event -> {
                selectionPopup.hide();
                draw();
            });

            EventHandler ev = event -> {
                CheckBox source = (CheckBox) event.getSource();
                if (source.isSelected())
                    selection.add(source.getText());
                else
                    selection.remove(source.getText());
            };
            int i = 1;
            int j = 1;
            for (Currency c : currencyList) {
                CheckBox opt = new CheckBox(c.getCode());
                opt.setOnAction(ev);
                grid.add(opt, i, j);
                i++;
                if (i > codesInRow) {
                    i = 1;
                    j++;
                }
            }
            grid.add(button, codesInRow, ++j);
            grid.setStyle("-fx-background-color: cornsilk; -fx-padding: 10;");
            selectionPopup.getContent().add(grid);
        } catch (Exception e) {
            showExceptionPopup(e);
        }
    }

    private void showExceptionPopup(Exception e) {
        e.printStackTrace();
        Platform.runLater(() -> {
            showInfoPopup(true, "Unable to finish operation");
        });
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        Platform.runLater(() -> {
            showInfoPopup(false, "");
        });
    }

    private void showCurrenciesPopup() {
        if (!selectionPopup.isShowing()) {
            selectionPopup.show(chart.getScene().getWindow(), chart.getScene().getWindow().getX() + 100, chart.getScene().getWindow().getY() + 100);
        } else {
            selectionPopup.hide();
        }
    }

    public void disableUI(boolean enabled) {
        buttonChoice.setDisable(enabled);
        fromDate.setDisable(enabled);
        toDate.setDisable(enabled);
        checkBoxPrediction.setDisable(enabled);
    }

    private void initInfoPopup() {
        infoPopup = new Popup();
        VBox box = new VBox(5);
        Text text = new Text();
        box.getChildren().addAll(text, new ProgressIndicator(-1));
        box.setStyle("-fx-background-color: cornsilk; -fx-padding: 10;");
        infoPopup.getContent().add(box);
    }

    public void showInfoPopup(boolean visible, String text) {
        disableUI(visible);
        if (visible) {
            ((Text) ((VBox) infoPopup.getContent().get(0)).getChildren().get(0)).setText(text);
            infoPopup.show(chart.getScene().getWindow());
        } else {
            infoPopup.hide();
        }
    }

}
