package br.com.dantesrpg.model.map;

import br.com.dantesrpg.model.util.FileLoader;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStreamReader;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Registro central de todos os tipos de tiles/blocos do mapa.
 * Carrega as definições de tile_registry.json e fornece lookups O(1) por RGB e por ID.
 *
 * Singleton — use TileRegistry.getInstance().
 */
public class TileRegistry {

	private static TileRegistry instance;

	private Map<String, TileDefinition> tilesById = new LinkedHashMap<>();
	private Map<Integer, TileDefinition> tilesByRgb = new HashMap<>();
	private Map<String, List<TileDefinition>> tilesByCategory = new LinkedHashMap<>();
	private Set<String> allCssClasses = new HashSet<>();
	private TileDefinition defaultTile;

	private boolean loaded = false;

	private TileRegistry() {
	}

	public static TileRegistry getInstance() {
		if (instance == null) {
			instance = new TileRegistry();
		}
		return instance;
	}

	/**
	 * Carrega o registro a partir do JSON. Deve ser chamado UMA VEZ na inicialização do app.
	 * Chamadas subsequentes são ignoradas (idempotente).
	 */
	public void load() {
		if (loaded) return;

		System.out.println("[TileRegistry] Carregando tile_registry.json...");

		try {
			InputStream is = FileLoader.carregarArquivo("/data/tile_registry.json");
			if (is == null) {
				System.err.println("[TileRegistry] ERRO CRÍTICO: tile_registry.json não encontrado!");
				return;
			}

			InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
			Gson gson = new Gson();

			// Deserializa o JSON raiz
			Type rootType = new TypeToken<RegistryRoot>() {}.getType();
			RegistryRoot root = gson.fromJson(reader, rootType);
			reader.close();

			if (root == null || root.tiles == null) {
				System.err.println("[TileRegistry] ERRO: JSON vazio ou sem campo 'tiles'.");
				return;
			}

			// Processa cada tile
			for (TileDefinition tile : root.tiles) {
				// Registra por ID
				if (tilesById.containsKey(tile.getId())) {
					System.err.println("[TileRegistry] AVISO: ID duplicado '" + tile.getId() + "'. Sobrescrevendo.");
				}
				tilesById.put(tile.getId(), tile);

				// Registra por RGB
				int rgbKey = tile.getRgbKey();
				if (tilesByRgb.containsKey(rgbKey)) {
					TileDefinition existente = tilesByRgb.get(rgbKey);
					System.err.println("[TileRegistry] AVISO: RGB duplicado " + tile.getRgb()
							+ " entre '" + existente.getId() + "' e '" + tile.getId() + "'.");
				}
				tilesByRgb.put(rgbKey, tile);

				// Registra por Categoria
				tilesByCategory
						.computeIfAbsent(tile.getCategory(), k -> new ArrayList<>())
						.add(tile);

				// Coleta CSS classes
				allCssClasses.add(tile.getCssClass());
			}

			// Define o tile padrão
			String defaultId = (root.defaultTileId != null) ? root.defaultTileId : "floor";
			defaultTile = tilesById.get(defaultId);
			if (defaultTile == null && !tilesById.isEmpty()) {
				defaultTile = tilesById.values().iterator().next();
				System.err.println("[TileRegistry] AVISO: defaultTileId '" + defaultId
						+ "' não encontrado. Usando '" + defaultTile.getId() + "' como fallback.");
			}

			loaded = true;
			System.out.println("[TileRegistry] Carregado com sucesso: " + tilesById.size() + " tiles em "
					+ tilesByCategory.size() + " categorias.");

		} catch (Exception e) {
			System.err.println("[TileRegistry] ERRO ao carregar tile_registry.json:");
			e.printStackTrace();
		}
	}

	// --- Lookups ---

	/**
	 * Busca um tile pela cor RGB do pixel do PNG. Retorna null se não encontrar.
	 */
	public TileDefinition getByRgb(int r, int g, int b) {
		int key = (r << 16) | (g << 8) | b;
		return tilesByRgb.get(key);
	}

	/**
	 * Busca um tile pelo ID único (ex: "lava", "wall3").
	 */
	public TileDefinition getById(String id) {
		return tilesById.get(id);
	}

	/**
	 * Retorna o tile padrão (usado quando nenhum RGB bate).
	 */
	public TileDefinition getDefault() {
		return defaultTile;
	}

	/**
	 * Retorna a lista ordenada de categorias (mantém ordem de inserção).
	 */
	public List<String> getCategories() {
		return new ArrayList<>(tilesByCategory.keySet());
	}

	/**
	 * Retorna todos os tiles de uma categoria específica.
	 */
	public List<TileDefinition> getTilesInCategory(String category) {
		return tilesByCategory.getOrDefault(category, Collections.emptyList());
	}

	/**
	 * Retorna TODAS as CSS classes de mapa registradas.
	 * Útil para limpar uma célula antes de aplicar um novo tile.
	 */
	public Set<String> getAllCssClasses() {
		return Collections.unmodifiableSet(allCssClasses);
	}

	/**
	 * Retorna todos os tiles registrados (valores).
	 */
	public Collection<TileDefinition> getAllTiles() {
		return Collections.unmodifiableCollection(tilesById.values());
	}

	/**
	 * Verifica se o registro foi carregado com sucesso.
	 */
	public boolean isLoaded() {
		return loaded;
	}

	// --- Classe interna para deserialização da raiz do JSON ---

	private static class RegistryRoot {
		int version;
		String defaultTileId;
		List<TileDefinition> tiles;
	}
}
