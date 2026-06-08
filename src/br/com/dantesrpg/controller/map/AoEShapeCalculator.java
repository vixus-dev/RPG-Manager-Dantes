package br.com.dantesrpg.controller.map;

import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.map.Dominio;
import javafx.scene.layout.Pane;
import javafx.util.Pair;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Algoritmos puros de cálculo de alcance, AoE e linha de visão para o mapa de combate.
 * Extraído de MapController para isolar lógica de geometria e pathfinding da UI.
 * Não aplica CSS nem modifica estado visual — retorna conjuntos de células/personagens.
 */
public class AoEShapeCalculator {

	private final boolean[][] paredesGrid;
	private final Pane[][] celulasDoGrid;
	private final int largura;
	private final int altura;
	private final BiFunction<Integer, Integer, Personagem> personagemLocator;
	private final Supplier<List<Personagem>> combatentesSupplier;

	public AoEShapeCalculator(boolean[][] paredesGrid, Pane[][] celulasDoGrid,
			int largura, int altura,
			BiFunction<Integer, Integer, Personagem> personagemLocator,
			Supplier<List<Personagem>> combatentesSupplier) {
		this.paredesGrid = paredesGrid;
		this.celulasDoGrid = celulasDoGrid;
		this.largura = largura;
		this.altura = altura;
		this.personagemLocator = personagemLocator;
		this.combatentesSupplier = combatentesSupplier;
	}

	// ========== ALCANCE DE MOVIMENTO (BFS — bloqueia em paredes e objetos) ==========

	/**
	 * Calcula as células alcançáveis via movimento (BFS ortogonal, respeita domínios).
	 * Não aplica CSS — o chamador é responsável por estilizar as células retornadas.
	 */
	public Set<Pane> calcularCelulasMovimento(int startX, int startY, int maxDist,
			Map<String, Dominio> dominiosAtivos) {
		Set<Pane> resultado = new HashSet<>();
		if (maxDist <= 0) return resultado;

		Queue<Pair<Integer, Integer>> fila = new LinkedList<>();
		int[][] distancias = new int[largura][altura];
		for (int i = 0; i < largura; i++) java.util.Arrays.fill(distancias[i], -1);

		fila.add(new Pair<>(startX, startY));
		distancias[startX][startY] = 0;
		int[] dx = {0, 0, 1, -1};
		int[] dy = {1, -1, 0, 0};

		while (!fila.isEmpty()) {
			Pair<Integer, Integer> atual = fila.poll();
			int x = atual.getKey();
			int y = atual.getValue();
			if (distancias[x][y] + 1 > maxDist) continue;

			for (int i = 0; i < 4; i++) {
				int novoX = x + dx[i];
				int novoY = y + dy[i];
				if (novoX < 0 || novoX >= largura || novoY < 0 || novoY >= altura) continue;

				boolean bloqueadoPorDominio = false;
				for (Dominio dom : dominiosAtivos.values()) {
					if (dom.bloqueiaMovimento(x, y, novoX, novoY)) {
						bloqueadoPorDominio = true;
						break;
					}
				}
				if (bloqueadoPorDominio) continue;

				if (!paredesGrid[novoX][novoY] && distancias[novoX][novoY] == -1) {
					distancias[novoX][novoY] = distancias[x][y] + 1;
					fila.add(new Pair<>(novoX, novoY));
					resultado.add(celulasDoGrid[novoX][novoY]);
				}
			}
		}
		return resultado;
	}

	// ========== ALCANCE DE ATAQUE (LoS — permite mirar em objetos) ==========

	/**
	 * Calcula as células atacáveis com linha de visão (Chebyshev + Bresenham).
	 * Não aplica CSS — o chamador é responsável por estilizar as células retornadas.
	 */
	public Set<Pane> calcularCelulasAtaque(int startX, int startY, int maxDist) {
		Set<Pane> resultado = new HashSet<>();
		if (maxDist <= 0) return resultado;

		for (int y = startY - maxDist; y <= startY + maxDist; y++) {
			for (int x = startX - maxDist; x <= startX + maxDist; x++) {
				if (x < 0 || x >= largura || y < 0 || y >= altura) continue;
				if (x == startX && y == startY) continue;

				boolean isParede = paredesGrid[x][y];
				boolean isObjeto = false;
				if (isParede) {
					Personagem p = personagemLocator.apply(x, y);
					if (p instanceof br.com.dantesrpg.model.elementos.ObjetoDestrutivel) {
						isObjeto = true;
					}
				}
				if (isParede && !isObjeto) continue;

				int dist = Math.max(Math.abs(x - startX), Math.abs(y - startY));
				if (dist > maxDist) continue;

				if (temLinhaDeVisao(startX, startY, x, y)) {
					resultado.add(celulasDoGrid[x][y]);
				}
			}
		}
		return resultado;
	}

	// ========== AoE: ENCONTRAR ALVOS NA FORMA ==========

	public List<Personagem> encontrarAlvosNaForma(int centroX, int centroY, Habilidade habilidade, Personagem ator) {
		List<Personagem> alvosEncontrados = new ArrayList<>();
		boolean atravessaParedes = habilidade.ignoraParedes();
		TipoAlvo tipo = habilidade.getTipoAlvo();

		Set<Pane> celulasDaForma = new HashSet<>();

		if (tipo == TipoAlvo.AREA) {
			int raio = (habilidade.getTamanhoArea()) / 2;
			int px = ator.getPosX();
			int py = ator.getPosY();
			for (int y = py - raio; y <= py + raio; y++) {
				for (int x = px - raio; x <= px + raio; x++) {
					if (Math.abs(x - px) + Math.abs(y - py) <= raio) {
						coletarCelula(x, y, atravessaParedes, celulasDaForma);
					}
				}
			}
		} else if (tipo == TipoAlvo.AREA_CIRCULAR) {
			int raio = (habilidade.getTamanhoArea()) / 2;
			for (int y = centroY - raio; y <= centroY + raio; y++) {
				for (int x = centroX - raio; x <= centroX + raio; x++) {
					if (Math.abs(x - centroX) + Math.abs(y - centroY) <= raio) {
						coletarCelula(x, y, atravessaParedes, celulasDaForma);
					}
				}
			}
		} else if (tipo == TipoAlvo.AREA_QUADRADA) {
			int tamanho = habilidade.getTamanhoArea();
			int raio = (tamanho - 1) / 2;
			for (int y = centroY - raio; y <= centroY + raio; y++) {
				for (int x = centroX - raio; x <= centroX + raio; x++) {
					coletarCelula(x, y, atravessaParedes, celulasDaForma);
				}
			}
		} else if (tipo == TipoAlvo.LINHA) {
			int comprimento = habilidade.getAlcanceMaximo();
			int larguraLinha = habilidade.getTamanhoArea();
			int raioLargura = (larguraLinha - 1) / 2;
			int px = ator.getPosX();
			int py = ator.getPosY();

			double angulo = Math.atan2(centroY - py, centroX - px);
			if (angulo < 0) angulo += 2 * Math.PI;
			int setor = (int) Math.round(angulo / (Math.PI / 4)) % 8;

			int[][] direcoes = {
				{ 1,  0}, { 1,  1}, { 0,  1}, {-1,  1},
				{-1,  0}, {-1, -1}, { 0, -1}, { 1, -1}
			};
			int dirX = direcoes[setor][0];
			int dirY = direcoes[setor][1];
			boolean isDiagonal = (dirX != 0 && dirY != 0);

			for (int i = 1; i <= comprimento; i++) {
				int baseX = px + (i * dirX);
				int baseY = py + (i * dirY);
				boolean bloqueado = false;

				for (int j = -raioLargura; j <= raioLargura; j++) {
					int currentX, currentY;
					if (isDiagonal) {
						currentX = baseX + (j * (-dirY));
						currentY = baseY + (j * dirX);
					} else if (dirX != 0) {
						currentX = baseX;
						currentY = baseY + j;
					} else {
						currentX = baseX + j;
						currentY = baseY;
					}
					if (!coletarCelula(currentX, currentY, atravessaParedes, celulasDaForma) && !atravessaParedes) {
						bloqueado = true;
						break;
					}
				}
				if (bloqueado) break;
			}
		} else if (tipo == TipoAlvo.CONE) {
			int alcance = habilidade.getAlcanceMaximo();
			double anguloHabilidadeMetade = Math.toRadians(habilidade.getAnguloCone() / 2.0);
			int px = ator.getPosX();
			int py = ator.getPosY();
			double anguloCentralMouse = Math.atan2(centroY - py, centroX - px + 0.0001);
			List<Integer> desvios = habilidade.getAngulosDesvio();

			for (int y = py - alcance; y <= py + alcance; y++) {
				for (int x = px - alcance; x <= px + alcance; x++) {
					if (x == px && y == py) continue;
					if (!dentroDoGrid(x, y)) continue;
					int dist = Math.max(Math.abs(x - px), Math.abs(y - py));
					if (dist > alcance) continue;

					double anguloCelula = Math.atan2(y - py, x - px + 0.0001);
					boolean acertou = false;

					for (int desvioGraus : desvios) {
						double desvioRad = Math.toRadians(desvioGraus);
						double anguloCentralAjustado = anguloCentralMouse + desvioRad;
						double diff = anguloCelula - anguloCentralAjustado;
						if (diff > Math.PI) diff -= (2 * Math.PI);
						if (diff < -Math.PI) diff += (2 * Math.PI);
						if (Math.abs(diff) <= anguloHabilidadeMetade) {
							acertou = true;
							break;
						}
					}
					if (acertou) {
						if (atravessaParedes || temLinhaDeVisao(px, py, x, y)) {
							coletarCelula(x, y, atravessaParedes, celulasDaForma);
						}
					}
				}
			}
		}

		for (Personagem p : combatentesSupplier.get()) {
			boolean alvoValido = p.isAtivoNoCombate();
			if (!alvoValido && p instanceof br.com.dantesrpg.model.elementos.ObjetoDestrutivel) {
				alvoValido = ((br.com.dantesrpg.model.elementos.ObjetoDestrutivel) p).isIntacto();
			}
			if (!alvoValido) continue;

			if (personagemIntersecaForma(p, celulasDaForma)) {
				boolean isAliado = p.getFaccao().equals(ator.getFaccao());
				if (p == ator && !habilidade.afetaSiMesmo()) continue;
				if (isAliado && !habilidade.afetaAliados()) continue;
				if (!isAliado && !habilidade.afetaInimigos() && !p.getFaccao().equals("OBJETO")) continue;
				if (ator.isClone() && p.isClone()) {
					if (ator.getCriador() == p.getCriador()) continue;
				}
				alvosEncontrados.add(p);
			}
		}
		return alvosEncontrados;
	}

	// ========== UTILITÁRIOS ==========

	/** Bresenham line-of-sight check. */
	public boolean temLinhaDeVisao(int x0, int y0, int x1, int y1) {
		int dx = Math.abs(x1 - x0);
		int dy = -Math.abs(y1 - y0);
		int sx = (x0 < x1) ? 1 : -1;
		int sy = (y0 < y1) ? 1 : -1;
		int err = dx + dy;
		int currentX = x0;
		int currentY = y0;

		while (true) {
			if ((currentX != x0 || currentY != y0) && paredesGrid[currentX][currentY]) {
				if (currentX == x1 && currentY == y1) return true;
				return false;
			}
			if (currentX == x1 && currentY == y1) break;
			int e2 = 2 * err;
			if (e2 >= dy) {
				if (currentX == x1) break;
				err += dy;
				currentX += sx;
			}
			if (e2 <= dx) {
				if (currentY == y1) break;
				err += dx;
				currentY += sy;
			}
		}
		return true;
	}

	/** BFS distance through walkable terrain (-1 se inalcançável ou parede). */
	public int calcularDistancia(int startX, int startY, int endX, int endY) {
		if (paredesGrid[endX][endY]) return -1;

		Queue<Pair<Integer, Integer>> fila = new LinkedList<>();
		int[][] distancias = new int[largura][altura];
		for (int i = 0; i < largura; i++) java.util.Arrays.fill(distancias[i], -1);
		fila.add(new Pair<>(startX, startY));
		distancias[startX][startY] = 0;
		int[] dx = {0, 0, 1, -1};
		int[] dy = {1, -1, 0, 0};

		while (!fila.isEmpty()) {
			Pair<Integer, Integer> atual = fila.poll();
			int x = atual.getKey();
			int y = atual.getValue();
			if (x == endX && y == endY) return distancias[x][y];

			for (int i = 0; i < 4; i++) {
				int novoX = x + dx[i];
				int novoY = y + dy[i];
				if (novoX >= 0 && novoX < largura && novoY >= 0 && novoY < altura) {
					if (!paredesGrid[novoX][novoY] && distancias[novoX][novoY] == -1) {
						distancias[novoX][novoY] = distancias[x][y] + 1;
						fila.add(new Pair<>(novoX, novoY));
					}
				}
			}
		}
		return -1;
	}

	public boolean dentroDoGrid(int x, int y) {
		return x >= 0 && x < largura && y >= 0 && y < altura;
	}

	// ========== PRIVADOS ==========

	private boolean coletarCelula(int x, int y, boolean atravessaParedes, Set<Pane> celulasColetadas) {
		if (x >= 0 && x < largura && y >= 0 && y < altura) {
			Pane cell = celulasDoGrid[x][y];
			if (cell != null) {
				if (paredesGrid[x][y] && !atravessaParedes) {
					Personagem p = personagemLocator.apply(x, y);
					boolean isObjeto = (p instanceof br.com.dantesrpg.model.elementos.ObjetoDestrutivel);
					if (!isObjeto) return false;
				}
				celulasColetadas.add(cell);
				return true;
			}
		}
		return false;
	}

	private boolean personagemIntersecaForma(Personagem personagem, Set<Pane> celulasDaForma) {
		if (personagem == null || celulasDaForma == null || celulasDaForma.isEmpty()) return false;

		for (int y = personagem.getPosY(); y < personagem.getPosY() + personagem.getTamanhoY(); y++) {
			for (int x = personagem.getPosX(); x < personagem.getPosX() + personagem.getTamanhoX(); x++) {
				if (!dentroDoGrid(x, y)) continue;
				if (celulasDaForma.contains(celulasDoGrid[x][y])) return true;
			}
		}
		return false;
	}
}
