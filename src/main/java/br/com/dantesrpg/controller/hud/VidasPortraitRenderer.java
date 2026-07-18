package br.com.dantesrpg.controller.hud;

import br.com.dantesrpg.model.util.ImageCache;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

/** Renderiza o retrato correspondente à quantidade atual de vidas. */
public final class VidasPortraitRenderer {

	private static final double TAMANHO_RETRATO = 120;

	private VidasPortraitRenderer() {
	}

	public static StackPane criar(int vidas) {
		StackPane retrato = new StackPane();
		retrato.setAlignment(Pos.CENTER);
		retrato.setMinSize(TAMANHO_RETRATO, TAMANHO_RETRATO);
		retrato.setPrefSize(TAMANHO_RETRATO, TAMANHO_RETRATO);
		retrato.setMaxSize(TAMANHO_RETRATO, TAMANHO_RETRATO);
		retrato.setStyle("-fx-background-color: #17131f; -fx-border-color: #5b2a86; "
				+ "-fx-border-width: 1; -fx-background-radius: 4; -fx-border-radius: 4;");
		atualizar(retrato, vidas);
		return retrato;
	}

	public static void atualizar(StackPane retrato, int vidas) {
		if (retrato == null) {
			return;
		}

		int vidasValidas = Math.max(0, Math.min(vidas, 3));
		retrato.getChildren().clear();

		Image imagem = ImageCache.get("/vidas/vidas_" + vidasValidas + ".png", TAMANHO_RETRATO, TAMANHO_RETRATO);
		if (imagem != null) {
			ImageView imageView = new ImageView(imagem);
			imageView.fitWidthProperty().bind(retrato.widthProperty());
			imageView.fitHeightProperty().bind(retrato.heightProperty());
			imageView.setPreserveRatio(false);
			retrato.getChildren().add(imageView);
		}
	}
}
