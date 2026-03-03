package br.com.dantesrpg.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

	@Override
	public void start(Stage primaryStage) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/dantesrpg/view/CombatView.fxml"));
			Parent root = loader.load();

			Scene scene = new Scene(root);

			primaryStage.setTitle("RPG: A Decadencia - Combat Manager");
			primaryStage.setScene(scene);
			primaryStage.setMaximized(true);
			primaryStage.show();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		launch(args);
	}
}