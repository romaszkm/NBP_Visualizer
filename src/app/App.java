package app;

import app.db.DBInitializer;
import app.gui.Controller;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Popup;
import javafx.stage.Stage;

public class App extends Application {

    private Controller mainScreenController;

    @Override
    public void start(Stage primaryStage) {
        try {
            primaryStage.setTitle("ExtremeRobotics_zad_rekr");
            showMainScreen(primaryStage);
            primaryStage.show();
            showLoginPopup(primaryStage);
        } catch (Exception e) {
            showFailedScreen(primaryStage, e);
        }
    }

    private void initialize() throws Exception {
        ApplicationBean.getInstance().initConnection();
        DBInitializer dbInitializer = new DBInitializer();
        if (!dbInitializer.checkDatabaseExistence()) {
            initDatabase(dbInitializer);
        } else if (!dbInitializer.isDatabaseUpToDate()){
            updateDatabase(dbInitializer);
        } else {
            mainScreenController.initSelectionPopup();
        }
        mainScreenController.disableUI(false);
    }

    private void initDatabase(DBInitializer dbInitializer) {
        mainScreenController.showInfoPopup(true, "Initializing database");
        Task t = new Task() {
            @Override
            protected Object call() throws Exception {
                dbInitializer.initDatabase();
                Platform.runLater(() -> {
                    mainScreenController.showInfoPopup(false, "");
                    mainScreenController.initSelectionPopup();
                });
                return null;
            }
        };
        Thread thread = new Thread(t);
        thread.setDaemon(true);
        thread.start();
    }

    private void updateDatabase(DBInitializer dbInitializer) {
        mainScreenController.showInfoPopup(true, "Updating database");
        Task t = new Task() {
            @Override
            protected Object call() throws Exception {
                dbInitializer.updateDatabase();
                Platform.runLater(() -> {
                    mainScreenController.showInfoPopup(false, "");
                    mainScreenController.initSelectionPopup();
                });
                return null;
            }
        };
        Thread thread = new Thread(t);
        thread.setDaemon(true);
        thread.start();
    }

    private void showFailedScreen(Stage primaryStage, Exception e) {
        Text t1 = new Text("Failed to initialize. Error:");
        t1.setFont(Font.font("Verdana", 20));
        Text t2 = new Text(e.getLocalizedMessage());
        BorderPane pane = new BorderPane(t2, t1, null, null, null);
        Scene scene = new Scene(pane, 500, 800);
        primaryStage.setScene(scene);
    }

    private void showMainScreen(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("gui/sample.fxml"));
        Parent root = loader.load();
        mainScreenController = loader.getController();
        Scene scene = new Scene(root, 1000, 500);
        primaryStage.setScene(scene);
    }

    private void showLoginPopup(Stage stage) {
        Popup loginPopup = new Popup();
        VBox box = new VBox(5);
        Text textLogin = new Text("Login");
        TextField fieldLogin = new TextField();
        Text textPassword = new Text("Password");
        TextField fieldPassword = new PasswordField();
        Text textAddr = new Text("Address");
        TextField fieldAddress = new TextField("localhost:5432/testdb");
        Button buttonLogin = new Button("Login");
        buttonLogin.setOnAction(e -> {
            ApplicationBean bean = ApplicationBean.getInstance();
            bean.setLogin(((TextField)((VBox)loginPopup.getContent().get(0)).getChildren().get(1)).getText());
            bean.setPass(((TextField)((VBox)loginPopup.getContent().get(0)).getChildren().get(3)).getText());
            bean.setAddress(((TextField)((VBox)loginPopup.getContent().get(0)).getChildren().get(5)).getText());
            loginPopup.hide();
            try {
                initialize();
            } catch (Exception e1) {
                showFailedScreen(stage, e1);
            }
        });
        box.getChildren().addAll(textLogin, fieldLogin, textPassword, fieldPassword, textAddr, fieldAddress, buttonLogin);
        box.setStyle("-fx-background-color: cornsilk; -fx-padding: 10;");
        loginPopup.getContent().addAll(box);
        loginPopup.show(stage);
    }

    public static void main(String[] args) {
        launch(args);
    }

}
