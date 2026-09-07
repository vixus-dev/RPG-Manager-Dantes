package br.com.dantesrpg.main;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import br.com.dantesrpg.controller.CombatController;
import br.com.dantesrpg.model.map.TileRegistry;
import atlantafx.base.theme.PrimerDark;

public class Main extends Application {
    private CombatController controller;

    @Override
    public void start(Stage primaryStage) {
        try {
            Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/dantesrpg/view/CombatView.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/br/com/dantesrpg/view/grimorio.css").toExternalForm());

            primaryStage.setTitle("A Decadencia Combat Manager");
            primaryStage.setScene(scene);
            primaryStage.setMaximized(true);
            ApplicationWindowIconService.install(primaryStage);
            primaryStage.show(); // Mostra a janela IMEDIATAMENTE

            // Carregamentos pesados APÓS a janela aparecer
            Platform.runLater(() -> {
                try {
                    TileRegistry.getInstance().load();

                    controller = loader.getController();
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

    @Override
    public void stop() {
        ApplicationWindowIconService.uninstall();
        if(controller!=null)controller.forEachMap(br.com.dantesrpg.controller.MapController::liberarRecursos);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
