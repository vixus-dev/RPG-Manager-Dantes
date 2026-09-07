package br.com.dantesrpg.main;

import java.io.InputStream;

import br.com.dantesrpg.model.util.FileLoader;
import javafx.collections.ListChangeListener;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Window;

/** Aplica o ícone da aplicação a toda janela JavaFX criada durante a sessão. */
public final class ApplicationWindowIconService {
	private static final String ICON_PATH = "/logoTrasnp.png";
	private static final ListChangeListener<Window> WINDOW_LISTENER = change -> {
		while (change.next()) {
			for (Window window : change.getAddedSubList()) {
				applyTo(window);
			}
		}
	};

	private static Image applicationIcon;
	private static boolean installed;

	private ApplicationWindowIconService() {
	}

	public static void install(Stage primaryStage) {
		if (installed) {
			applyTo(primaryStage);
			return;
		}
		applicationIcon = loadIcon();
		installed = true;
		applyTo(primaryStage);
		Window.getWindows().forEach(ApplicationWindowIconService::applyTo);
		Window.getWindows().addListener(WINDOW_LISTENER);
	}

	public static void uninstall() {
		if (!installed) {
			return;
		}
		Window.getWindows().removeListener(WINDOW_LISTENER);
		installed = false;
	}

	private static Image loadIcon() {
		try (InputStream input = FileLoader.carregarArquivoOpcional(ICON_PATH)) {
			if (input == null) {
				System.err.println("JANELA: Logo da aplicação não encontrada em " + ICON_PATH);
				return null;
			}
			Image icon = new Image(input);
			if (icon.isError()) {
				System.err.println("JANELA: Não foi possível decodificar a logo da aplicação.");
				return null;
			}
			return icon;
		} catch (Exception e) {
			System.err.println("JANELA: Erro ao carregar a logo da aplicação: " + e.getMessage());
			return null;
		}
	}

	private static void applyTo(Window window) {
		if (applicationIcon == null || !(window instanceof Stage stage)
				|| stage.getIcons().contains(applicationIcon)) {
			return;
		}
		stage.getIcons().add(applicationIcon);
	}
}
