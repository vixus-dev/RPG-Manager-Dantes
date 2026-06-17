package br.com.dantesrpg.model.util;

import javafx.scene.image.Image;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Cache central de imagens para otimização de memória RAM e CPU.
 * Evita releituras físicas do disco e aplica downsampling para economizar RAM nativa.
 */
public class ImageCache {

	private static final Map<String, Image> cache = new HashMap<>();

	/**
	 * Recupera uma imagem em cache ou carrega a partir do disco/classpath se necessário.
	 *
	 * @param caminhoRelativo O caminho relativo da imagem (ex: "/tokens/darrell.png")
	 * @param larguraDesejada Largura para redimensionamento (0 para tamanho original)
	 * @param alturaDesejada  Altura para redimensionamento (0 para tamanho original)
	 * @return A imagem JavaFX correspondente, ou null se houver erro
	 */
	public static Image get(String caminhoRelativo, double larguraDesejada, double alturaDesejada) {
		if (caminhoRelativo == null || caminhoRelativo.isEmpty()) {
			return null;
		}

		String chaveCache = caminhoRelativo + "_" + larguraDesejada + "x" + alturaDesejada;

		if (cache.containsKey(chaveCache)) {
			return cache.get(chaveCache);
		}

		try (InputStream is = FileLoader.carregarArquivo(caminhoRelativo)) {
			if (is == null) {
				return null;
			}

			Image imagem;
			if (larguraDesejada > 0 && alturaDesejada > 0) {
				// Downsampling nativo durante a leitura para economizar megabytes de RAM nativa
				imagem = new Image(is, larguraDesejada, alturaDesejada, true, false);
			} else {
				imagem = new Image(is);
			}

			if (!imagem.isError()) {
				cache.put(chaveCache, imagem);
				return imagem;
			}
		} catch (Exception e) {
			System.err.println("[ImageCache] Erro ao carregar imagem: " + caminhoRelativo + " -> " + e.getMessage());
		}

		return null;
	}

	/**
	 * Atalho para carregar imagens sem redimensionamento.
	 */
	public static Image get(String caminhoRelativo) {
		return get(caminhoRelativo, 0, 0);
	}

	/**
	 * Limpa o cache estático para liberar memória acumulada.
	 */
	public static void limpar() {
		cache.clear();
	}
}
