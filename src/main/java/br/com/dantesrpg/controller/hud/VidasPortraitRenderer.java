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
		int vidasValidas = Math.max(0, Math.min(vidas, 3));
		StackPane retrato = new StackPane();
		retrato.setAlignment(Pos.CENTER);
		retrato.setMinSize(TAMANHO_RETRATO, TAMANHO_RETRATO);
		retrato.setPrefSize(TAMANHO_RETRATO, TAMANHO_RETRATO);
		retrato.setMaxSize(TAMANHO_RETRATO, TAMANHO_RETRATO);
		retrato.setStyle("-fx-background-color: #17131f; -fx-border-color: #5b2a86; "
				+ "-fx-border-width: 1; -fx-background-radius: 4; -fx-border-radius: 4;");

		Image imagem = ImageCache.get("/vidas/vidas_" + vidasValidas + ".png", TAMANHO_RETRATO, TAMANHO_RETRATO);
		if (imagem != null) {
			ImageView imageView = new ImageView(imagem);
			imageView.setFitWidth(TAMANHO_RETRATO);
			imageView.setFitHeight(TAMANHO_RETRATO);
			imageView.setPreserveRatio(true);
			retrato.getChildren().add(imageView);
		}

		return retrato;
	}
}
