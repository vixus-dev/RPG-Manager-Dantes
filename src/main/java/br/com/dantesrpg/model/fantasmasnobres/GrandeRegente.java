package br.com.dantesrpg.model.fantasmasnobres;

import br.com.dantesrpg.controller.MapController;
import br.com.dantesrpg.model.AcaoMestreInput;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.FantasmaNobre;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.combat.KnockbackResult;
import br.com.dantesrpg.model.enums.TipoAcao;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoEfeito;
import java.util.List;
import java.util.Map;

/** Fantasma Nobre de Invictus: converte movimento em impacto e alcance de área. */
public class GrandeRegente extends FantasmaNobre {

	public static final String EFEITO_GRANDE_REGENTE = "Grande Regente";
	private static final int DANO_BASE = 20;
	private static final int CUSTO_TU_BASE = 80;

	@Override public String getNome() { return "Grande Regente"; }
	@Override public String getDescricao() {
		return "Consome movimento para aumentar o dano em 10% e o custo em 5% por quadrado. "
				+ "A cada 4 quadrados, a bola cresce 2x2.";
	}
	@Override public int getCustoMana() { return 0; }
	@Override public int getCustoTU() { return CUSTO_TU_BASE; }
	@Override public int getCooldownTU() { return 0; }
	@Override public TipoAlvo getTipoAlvo() { return TipoAlvo.AREA_CIRCULAR; }
	@Override public int getTamanhoArea() { return 1; }
	@Override public int getNumeroDeAlvos() { return 0; }

	@Override
	public int getCustoTU(Personagem conjurador, AcaoMestreInput input) {
		return (int) Math.round(CUSTO_TU_BASE * (1.0 + 0.05 * obterMovimentoReservado(input)));
	}

	public static int calcularDiametro(int movimentoReservado) {
		return 1 + 2 * Math.max(0, movimentoReservado) / 4;
	}

	public static int obterMovimentoReservado(AcaoMestreInput input) {
		return input == null ? 0 : Math.max(0, input.getMovimentoReservado());
	}

	public static boolean temReservaAtiva(Personagem personagem, AcaoMestreInput input) {
		return personagem != null && personagem.getFantasmaNobre() instanceof GrandeRegente
				&& personagem.getEfeitosAtivos().containsKey(EFEITO_GRANDE_REGENTE)
				&& obterMovimentoReservado(input) > 0;
	}

	public static int consumirMovimentoReservado(Personagem personagem, AcaoMestreInput input) {
		if (!temReservaAtiva(personagem, input)) return 0;
		int reservado = Math.min(obterMovimentoReservado(input), personagem.getMovimentoRestanteTurno());
		input.setMovimentoReservado(reservado);
		personagem.setMovimentoRestanteTurno(personagem.getMovimentoRestanteTurno() - reservado);
		return reservado;
	}

	@Override
	public void onCombatStart(Personagem conjurador, EstadoCombate estado, CombatManager manager) {
		if (!conjurador.getEfeitosAtivos().containsKey(EFEITO_GRANDE_REGENTE)) {
			adicionarAcumulos(conjurador, 1);
		}
	}

	@Override
	public void onCombatEnd(Personagem conjurador, EstadoCombate estado, CombatManager manager) {
		conjurador.removerEfeito(EFEITO_GRANDE_REGENTE);
	}

	public static void adicionarAcumulos(Personagem personagem, int quantidade) {
		if (personagem == null || quantidade <= 0) return;
		Efeito efeito = personagem.getEfeitosAtivos().get(EFEITO_GRANDE_REGENTE);
		int stacks = (efeito == null ? 0 : efeito.getStacks()) + quantidade;
		int bonusMovimento = (stacks / 2) * 4;
		Efeito atualizado = new Efeito(EFEITO_GRANDE_REGENTE, TipoEfeito.BUFF, 999_999,
				Map.of("ARMADURA_TOTAL", 50.0, "MOVIMENTO", (double) bonusMovimento), 0, 0);
		atualizado.setStacks(stacks);
		if (efeito == null) {
			personagem.adicionarEfeito(atualizado);
		} else {
			efeito.setStacks(stacks);
			efeito.setModificadores(atualizado.getModificadores());
			efeito.setDuracaoTURestante(atualizado.getDuracaoTURestante());
			personagem.recalcularAtributosEstatisticas();
		}
		System.out.println(">>> GRANDE REGENTE: " + personagem.getNome() + " possui " + stacks
				+ " acúmulo(s) e +" + bonusMovimento + " de movimento.");
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
			AcaoMestreInput input, CombatManager manager) {
		int movimentoReservado = Math.min(obterMovimentoReservado(input), conjurador.getMovimentoRestanteTurno());
		conjurador.setMovimentoRestanteTurno(conjurador.getMovimentoRestanteTurno() - movimentoReservado);
		int diametro = calcularDiametro(movimentoReservado);
		double dano = DANO_BASE * (1.0 + 0.10 * movimentoReservado);

		for (Personagem alvo : alvos == null ? List.<Personagem>of() : alvos) {
			if (alvo == null || !alvo.isAtivoNoCombate()) continue;
			if (!mesmaFaccao(conjurador, alvo)) {
				manager.aplicarDanoAoAlvo(conjurador, alvo, dano, false, TipoAcao.FANTASMA_NOBRE, estado);
			}
			empurrarAteABorda(conjurador, alvo, input, diametro, manager);
		}
		System.out.println(">>> " + conjurador.getNome() + " descarregou Grande Regente: " + diametro
				+ "x" + diametro + ", " + movimentoReservado + " movimento, " + String.format("%.1f", dano) + " dano.");
	}

	private void empurrarAteABorda(Personagem conjurador, Personagem alvo, AcaoMestreInput input, int diametro,
			CombatManager manager) {
		if (input == null || input.getEpicentroX() < 0 || input.getEpicentroY() < 0 || !alvo.isVivo()) return;
		int dx = Integer.signum(alvo.getPosX() - input.getEpicentroX());
		int dy = Integer.signum(alvo.getPosY() - input.getEpicentroY());
		if (dx == 0 && dy == 0) return;
		int raio = (diametro - 1) / 2;
		int distanciaAtual = Math.max(Math.abs(alvo.getPosX() - input.getEpicentroX()),
				Math.abs(alvo.getPosY() - input.getEpicentroY()));
		int distanciaAteABorda = Math.max(0, raio - distanciaAtual);
		if (distanciaAteABorda == 0) return;
		MapController mapa = manager.getMainController() == null ? null : manager.getMainController().getMapController();
		KnockbackResult resultado = manager.getKnockbackProcessor().calcularEmpuxoDirecional(alvo, dx, dy,
				distanciaAteABorda, mapa);
		manager.getKnockbackProcessor().executarEmpuxo(alvo, resultado);
	}

	private boolean mesmaFaccao(Personagem primeiro, Personagem segundo) {
		return primeiro.getFaccao() != null && primeiro.getFaccao().equals(segundo.getFaccao());
	}
}
