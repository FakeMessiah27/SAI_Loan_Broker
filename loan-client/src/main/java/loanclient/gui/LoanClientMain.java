package loanclient.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class LoanClientMain extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // load FXML file is in loan-client/src/main/resources/loanclient.fxml
        Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("loanclient.fxml"));
        primaryStage.setTitle("Loan Client");
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                Platform.exit();
                System.exit(0);
            }
        });

        primaryStage.setScene(new Scene(root, 500,300));
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
