package br.com.dantesrpg.model.map;

import br.com.dantesrpg.model.Personagem;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Representa um domínio (zona de combate) no mapa. Domínios são áreas estáticas
 * que bloqueiam entrada e saída, e cujos efeitos só se aplicam enquanto o
 * personagem estiver dentro da área.
 */
public class Dominio {

	private final String id; // Ex: "ringue_alexei", "dominio_lyria", "fusao_abc"
	private final String nomeEfeito; // Nome do Efeito no personagem (ex: "Ringue da Vontade")
	private final Personagem dono; // null para domínios fundidos
	private final int centroX;
	private final int centroY;
	private final int raio; // Para 7x7 = raio 3
	private final String cssClass; // Classe CSS para estilização visual
	private String texturePath;   // Caminho do sprite overlay (ex: "/effects/sangue_negro.png"), null = sem overlay
	private double overlayOpacity = 0.7; // Opacidade do sprite overlay (0.0 = transparente, 1.0 = opaco)

	// Coordenadas das células do domínio (para checagem rápida O(1))
	private final Set<Long> coordenadas = new HashSet<>();

	// Domínios originais que foram fundidos neste (só para domínios fusão)
	private final List<Dominio> dominiosOriginais = new ArrayList<>();
	private boolean isFusao = false;

	public Dominio(String id, String nomeEfeito, Personagem dono, int centroX, int centroY, int tamanho,
			String cssClass) {
		this.id = id;
		this.nomeEfeito = nomeEfeito;
		this.dono = dono;
		this.centroX = centroX;
		this.centroY = centroY;
		this.raio = (tamanho - 1) / 2;
		this.cssClass = cssClass;

		// Pré-calcula todas as coordenadas da área
		for (int y = centroY - raio; y <= centroY + raio; y++) {
			for (int x = centroX - raio; x <= centroX + raio; x++) {
				coordenadas.add(coordKey(x, y));
			}
		}
	}

	/**
	 * Construtor para domínio fundido (fusão de dois domínios empatados).
	 * As coordenadas são a UNIÃO das coordenadas dos domínios originais.
	 */
	private Dominio(String id, String nomeEfeito, String cssClass, Set<Long> coordenadasUnidas,
			List<Dominio> originais) {
		this.id = id;
		this.nomeEfeito = nomeEfeito;
		this.dono = null;
		this.cssClass = cssClass;
		this.isFusao = true;
		this.dominiosOriginais.addAll(originais);
		this.coordenadas.addAll(coordenadasUnidas);

		// Calcula centro aproximado da união
		long somaX = 0, somaY = 0;
		for (long key : coordenadasUnidas) {
			somaX += (int) (key >> 32);
			somaY += (int) key;
		}
		this.centroX = (int) (somaX / coordenadasUnidas.size());
		this.centroY = (int) (somaY / coordenadasUnidas.size());
		this.raio = 0; // Não se aplica em fusão (forma irregular)
	}

	/**
	 * Cria um domínio fundido a partir de dois domínios empatados.
	 * A área é a UNIÃO das duas áreas originais.
	 */
	public static Dominio criarFusao(Dominio a, Dominio b, String fusaoId, String nomeEfeitoFusao) {
		Set<Long> uniao = new HashSet<>(a.coordenadas);
		uniao.addAll(b.coordenadas);

		List<Dominio> originais = new ArrayList<>();
		originais.add(a);
		originais.add(b);

		return new Dominio(fusaoId, nomeEfeitoFusao, "zona-dominio-fusao", uniao, originais);
	}

	/**
	 * Cria um domínio expandido (vitória na disputa).
	 * A área cobre TODA a união das coordenadas originais.
	 */
	public static Dominio criarExpandido(Dominio vencedor, Dominio perdedor) {
		Set<Long> uniao = new HashSet<>(vencedor.coordenadas);
		uniao.addAll(perdedor.coordenadas);

		// Novo domínio com a identidade do vencedor mas a área expandida
		Dominio expandido = new Dominio(
				vencedor.id, vencedor.nomeEfeito, vencedor.dono,
				vencedor.centroX, vencedor.centroY, 1, // tamanho dummy
				vencedor.cssClass);
		expandido.coordenadas.clear();
		expandido.coordenadas.addAll(uniao);
		return expandido;
	}

	/**
	 * Chave única para uma coordenada (x, y). Suporta coordenadas de -32768 a
	 * 32767.
	 */
	public static long coordKey(int x, int y) {
		return ((long) x << 32) | (y & 0xFFFFFFFFL);
	}

	/** Verifica se a coordenada (x, y) está dentro deste domínio. O(1). */
	public boolean contemCoordenada(int x, int y) {
		return coordenadas.contains(coordKey(x, y));
	}

	/**
	 * Verifica se um personagem está fisicamente dentro deste domínio, baseado na
	 * sua posição atual no grid.
	 */
	public boolean contemPersonagem(Personagem p) {
		return contemCoordenada(p.getPosX(), p.getPosY());
	}

	/**
	 * Verifica se mover de (origemX, origemY) para (destinoX, destinoY) cruza a
	 * fronteira do domínio (um está dentro e o outro fora). Retorna true se o
	 * movimento deve ser BLOQUEADO.
	 */
	public boolean bloqueiaMovimento(int origemX, int origemY, int destinoX, int destinoY) {
		if (id != null && id.startsWith("heatWave_")) {
			return false; // A onda de calor é uma zona climática e não uma barreira física/mágica
		}
		boolean origemDentro = contemCoordenada(origemX, origemY);
		boolean destinoDentro = contemCoordenada(destinoX, destinoY);
		return origemDentro != destinoDentro;
	}

	/** Verifica se este domínio tem tiles em comum com outro. */
	public boolean sobrepoe(Dominio outro) {
		for (long coord : outro.coordenadas) {
			if (coordenadas.contains(coord))
				return true;
		}
		return false;
	}

	/** Retorna as coordenadas que se sobrepõem com outro domínio. */
	public Set<Long> getCoordenadasSobrepostas(Dominio outro) {
		Set<Long> sobrepostas = new HashSet<>(coordenadas);
		sobrepostas.retainAll(outro.coordenadas);
		return sobrepostas;
	}

	// --- Getters ---

	public String getId() {
		return id;
	}

	public String getNomeEfeito() {
		return nomeEfeito;
	}

	public Personagem getDono() {
		return dono;
	}

	public int getCentroX() {
		return centroX;
	}

	public int getCentroY() {
		return centroY;
	}

	public int getRaio() {
		return raio;
	}

	public String getCssClass() {
		return cssClass;
	}

	public Set<Long> getCoordenadas() {
		return coordenadas;
	}

	public String getTexturePath() {
		return texturePath;
	}

	public void setTexturePath(String texturePath) {
		this.texturePath = texturePath;
	}

	public double getOverlayOpacity() {
		return overlayOpacity;
	}

	public void setOverlayOpacity(double overlayOpacity) {
		this.overlayOpacity = Math.max(0.0, Math.min(1.0, overlayOpacity));
	}

	public boolean isFusao() {
		return isFusao;
	}

	public List<Dominio> getDominiosOriginais() {
		return dominiosOriginais;
	}

	/**
	 * Retorna os limites da área para iteração visual. [minX, maxX, minY, maxY]
	 */
	public int[] getLimites() {
		if (coordenadas.isEmpty())
			return new int[] { 0, 0, 0, 0 };

		int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
		int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
		for (long key : coordenadas) {
			int x = (int) (key >> 32);
			int y = (int) key;
			minX = Math.min(minX, x);
			maxX = Math.max(maxX, x);
			minY = Math.min(minY, y);
			maxY = Math.max(maxY, y);
		}
		return new int[] { minX, maxX, minY, maxY };
	}
}
