package br.com.dantesrpg.main;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import br.com.dantesrpg.controller.CombatController;
import br.com.dantesrpg.model.map.TileRegistry;
import br.com.dantesrpg.model.util.FileLoader;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/dantesrpg/view/CombatView.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);

            primaryStage.setTitle("A Decadencia Combat Manager");
            primaryStage.setScene(scene);
            primaryStage.setMaximized(true);
            primaryStage.show(); // Mostra a janela IMEDIATAMENTE

            // Carregamentos pesados APÓS a janela aparecer
            Platform.runLater(() -> {
                try {
                    TileRegistry.getInstance().load();
                    primaryStage.getIcons().add(new Image(FileLoader.carregarArquivo("/logoTrasnp.png")));

                    CombatController controller = loader.getController();
                    controller.inicializacaoTardia();
                } catch (Exception e) {
                    System.err.println("Erro na inicialização tardia:");
                    e.printStackTrace();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
