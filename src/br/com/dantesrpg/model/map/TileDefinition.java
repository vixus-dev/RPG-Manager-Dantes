package br.com.dantesrpg.model.map;

/**
 * POJO que representa a definição de um tipo de tile/bloco do mapa.
 * Deserializado diretamente do tile_registry.json via Gson.
 */
public class TileDefinition {

	private String id;
	private String name;
	private String cssClass;
	private RGBColor rgb;
	private String terrainType;
	private boolean walkable;
	private boolean wall;
	private String category;
	private String texturePath;
	private EffectConfig effect; // Nullable

	// --- Getters ---

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getCssClass() {
		return cssClass;
	}

	public RGBColor getRgb() {
		return rgb;
	}

	public String getTerrainType() {
		return terrainType;
	}

	public boolean isWalkable() {
		return walkable;
	}

	public boolean isWall() {
		return wall;
	}

	public String getCategory() {
		return category;
	}

	public String getTexturePath() {
		return texturePath;
	}

	public EffectConfig getEffect() {
		return effect;
	}

	/**
	 * Retorna a chave RGB codificada como int único para lookups O(1).
	 * Formato: (r << 16) | (g << 8) | b
	 */
	public int getRgbKey() {
		if (rgb == null) return 0;
		return (rgb.r << 16) | (rgb.g << 8) | rgb.b;
	}

	@Override
	public String toString() {
		return "TileDefinition{id='" + id + "', name='" + name + "', css='" + cssClass + "'}";
	}

	// --- Classes Internas ---

	public static class RGBColor {
		public int r, g, b;

		public RGBColor() {}

		public RGBColor(int r, int g, int b) {
			this.r = r;
			this.g = g;
			this.b = b;
		}

		@Override
		public String toString() {
			return "RGB(" + r + "," + g + "," + b + ")";
		}
	}

	public static class EffectConfig {
		private String tipo;
		private int duracao;
		private int dano;
		private boolean permanente;

		public String getTipo() {
			return tipo;
		}

		public int getDuracao() {
			return duracao;
		}

		public int getDano() {
			return dano;
		}

		public boolean isPermanente() {
			return permanente;
		}
	}
}
