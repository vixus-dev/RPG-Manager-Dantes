package br.com.dantesrpg.model.combat;

import br.com.dantesrpg.controller.MapController;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.PesoEntidade;

import java.util.ArrayList;
import java.util.List;

/**
 * Subsistema responsável por calcular e executar a mecânica de Empuxo (Knockback).
 *
 * <p>O empuxo é determinado pela relação entre a força de empuxo do ataque
 * ({@code forcaEmpuxo} na {@code Habilidade}) e o peso da entidade alvo
 * ({@code pesoEntidade} no {@code Personagem}).</p>
 *
 * <p>Fórmula:
 * <pre>
 *   distanciaMaxima = round(forcaEmpuxo / peso.getFatorResistencia())
 * </pre>
 * </p>
 *
 * <p>A trajetória é uma projeção tile a tile na direção normalizada do vetor
 * (atacante → alvo). O empuxo para no tile anterior à primeira colisão
 * (parede, borda do mapa ou outra entidade).</p>
 *
 * <p>Se o alvo estiver exatamente na mesma posição do atacante (corpo-a-corpo
 * puro sem deslocamento), a direção padrão é para a direita (+1, 0).</p>
 */
public class KnockbackProcessor {

	/**
	 * Dano de impacto por tile "não percorrido" quando há colisão com parede ou borda.
	 * Ex: empuxo de 4 tiles, parou em 2 → danoImpacto = 2 × FATOR_DANO_IMPACTO.
	 */
	private static final double FATOR_DANO_IMPACTO = 2.0;

	private final CombatManager combatManager;

	public KnockbackProcessor(CombatManager combatManager) {
		this.combatManager = combatManager;
	}

	// ========== API PRINCIPAL ==========

	/**
	 * Calcula o resultado do empuxo SEM aplicá-lo (usado para preview e para
	 * o pipeline interno de {@link DamageApplicator}).
	 *
	 * @param atacante     Quem gerou o ataque (define a direção do empuxo).
	 * @param alvo         Quem será empurrado.
	 * @param forcaEmpuxo  Valor bruto de força de empuxo (campo da Habilidade/Arma).
	 * @param mapController Referência ao mapa para checagem de paredes/entidades.
	 * @return {@link KnockbackResult} com toda a informação do empuxo.
	 */
	public KnockbackResult calcularEmpuxo(Personagem atacante, Personagem alvo,
			int forcaEmpuxo, MapController mapController) {

		// --- 1. Distância máxima ---
		int distanciaMaxima = calcularDistanciaEmpuxo(forcaEmpuxo, alvo.getPesoEntidade());

		if (distanciaMaxima <= 0) {
			return resultadoSemMomento(alvo);
		}

		// --- 2. Direção do empuxo ---
		int[] direcao = calcularDirecao(atacante, alvo);
		int dx = direcao[0];
		int dy = direcao[1];

		// --- 3. Trajetória tile a tile ---
		return tracarTrajetoria(alvo, dx, dy, distanciaMaxima, mapController);
	}

	/**
	 * Executa o empuxo: move o alvo para a posição final do {@link KnockbackResult}
	 * e solicita atualização visual (redesenho dos peões).
	 *
	 * @param alvo      Entidade a ser movida.
	 * @param resultado Resultado previamente calculado por {@link #calcularEmpuxo}.
	 */
	public void executarEmpuxo(Personagem alvo, KnockbackResult resultado) {
		if (!resultado.houveMomento()) return;

		int origemX = alvo.getPosX();
		int origemY = alvo.getPosY();

		alvo.setPosX(resultado.getPosicaoFinalX());
		alvo.setPosY(resultado.getPosicaoFinalY());

		System.out.printf(
			">>> EMPUXO: %s foi arremessado de (%d,%d) para (%d,%d) [%d tile(s)]%s%n",
			alvo.getNome(), origemX, origemY,
			resultado.getPosicaoFinalX(), resultado.getPosicaoFinalY(),
			resultado.getDistanciaReal(),
			resultado.isColidiu() ? " — COLIDIU!" : ""
		);

		// Redesenha tokens no mapa (se controlador disponível)
		MapController mapController = combatManager.getMainController() != null
				? combatManager.getMainController().getMapController()
				: null;
		if (mapController != null) {
			mapController.desenharPeoes(combatManager.getMainController().getCombatentes());
		}
	}

	// ========== CÁLCULOS ==========

	/**
	 * Converte força de empuxo + peso em distância máxima (em tiles).
	 * Distância mínima: 0. Distância máxima real: sem cap (balancear via JSON).
	 */
	public int calcularDistanciaEmpuxo(int forcaEmpuxo, PesoEntidade peso) {
		if (peso == PesoEntidade.IMOVEL || forcaEmpuxo <= 0) return 0;
		return Math.max(0, (int) Math.round(forcaEmpuxo / peso.getFatorResistencia()));
	}

	/**
	 * Calcula a direção normalizada do empuxo com base na posição relativa
	 * atacante → alvo. Cada componente é -1, 0 ou +1.
	 *
	 * @return int[2] = {dx, dy}
	 */
	public int[] calcularDirecao(Personagem atacante, Personagem alvo) {
		int rawDx = alvo.getPosX() - atacante.getPosX();
		int rawDy = alvo.getPosY() - atacante.getPosY();

		int dx = Integer.signum(rawDx);
		int dy = Integer.signum(rawDy);

		// Fallback: mesmo tile (corpo-a-corpo sem deslocamento) → empurra para direita
		if (dx == 0 && dy == 0) {
			dx = 1;
		}

		return new int[]{dx, dy};
	}

	// ========== TRAJETÓRIA ==========

	/**
	 * Traça a trajetória do empuxo tile a tile na direção (dx, dy),
	 * checando paredes, bordas e entidades bloqueantes.
	 */
	private KnockbackResult tracarTrajetoria(Personagem alvo, int dx, int dy,
			int distanciaMaxima, MapController mapController) {

		List<int[]> trajetoria = new ArrayList<>();
		int curX = alvo.getPosX();
		int curY = alvo.getPosY();

		boolean colidiu = false;
		Personagem colidiuCom = null;

		int tilesPercorridos = 0;

		for (int i = 0; i < distanciaMaxima; i++) {
			int nextX = curX + dx;
			int nextY = curY + dy;

			// Colisão com borda do mapa
			if (mapController != null && (nextX < 0 || nextY < 0
					|| nextX >= mapController.getGridLargura()
					|| nextY >= mapController.getGridAltura())) {
				colidiu = true;
				break;
			}

			// Colisão com parede
			if (mapController != null && mapController.isParedeem(nextX, nextY)) {
				colidiu = true;
				break;
			}

			// Colisão com outra entidade (exceto o próprio alvo)
			if (mapController != null) {
				Personagem bloqueio = mapController.getPersonagemNaCelula(nextX, nextY);
				if (bloqueio != null && bloqueio != alvo) {
					colidiu = true;
					colidiuCom = bloqueio;
					break; // para ANTES do tile da entidade
				}
			}

			// Tile livre: avançar
			curX = nextX;
			curY = nextY;
			trajetoria.add(new int[]{curX, curY});
			tilesPercorridos++;
		}

		// Dano de impacto apenas em colisão com parede/borda (não com entidade)
		double danoImpacto = 0.0;
		if (colidiu && colidiuCom == null) {
			int tilesNaoPercorridos = distanciaMaxima - tilesPercorridos;
			danoImpacto = tilesNaoPercorridos * FATOR_DANO_IMPACTO;
		}

		return new KnockbackResult(
			curX, curY,
			tilesPercorridos,
			colidiu,
			colidiuCom,
			danoImpacto,
			trajetoria,
			dx, dy
		);
	}

	// ========== UTILITÁRIOS ==========

	/** Cria um resultado nulo (sem movimento). */
	private KnockbackResult resultadoSemMomento(Personagem alvo) {
		return new KnockbackResult(
			alvo.getPosX(), alvo.getPosY(),
			0, false, null, 0.0,
			new ArrayList<>(), 0, 0
		);
	}
}
