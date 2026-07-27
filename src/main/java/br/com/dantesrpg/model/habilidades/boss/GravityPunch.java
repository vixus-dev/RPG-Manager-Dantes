package br.com.dantesrpg.model.habilidades.boss;

import br.com.dantesrpg.controller.MapController;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.combat.KnockbackProcessor;
import br.com.dantesrpg.model.combat.KnockbackResult;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import br.com.dantesrpg.model.util.EffectFactory;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Golpe de gravidade com direção de empuxo definida na rosa dos ventos. */
public class GravityPunch extends Habilidade {
	private static final int FORCA_EMPUXAO = 9;
	private static final Map<String, int[]> DIRECOES = Map.of(
			"NO", new int[] { -1, -1 }, "N", new int[] { 0, -1 }, "NE", new int[] { 1, -1 },
			"O", new int[] { -1, 0 }, "L", new int[] { 1, 0 },
			"SO", new int[] { -1, 1 }, "S", new int[] { 0, 1 }, "SE", new int[] { 1, 1 });

	public GravityPunch() {
		super("Gravity Punch",
				"Golpe de gravidade que causa 80% de dano, quebra 30% da armadura e empurra na direção escolhida.",
				TipoHabilidade.ATIVA, 1, 80, 1, TipoAlvo.INDIVIDUAL, 0, 0.8, 1, Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 7;
	}

	@Override
	public boolean afetaAliados() {
		return false;
	}

	@Override
	public List<String> getOpcoesSelection() {
		return List.of("NO", "N", "NE", "O", "L", "SO", "S", "SE");
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		if (manager == null || alvos == null || alvos.isEmpty()) return;
		String opcao = manager.getLastInput() != null ? manager.getLastInput().getOpcaoEscolhida() : null;
		int[] direcao = DIRECOES.getOrDefault(opcao, DIRECOES.get("N"));
		MapController mapa = manager.getMainController() != null ? manager.getMainController().getPrimaryMap() : null;
		KnockbackProcessor empuxo = manager.getKnockbackProcessor();

		for (Personagem alvo : alvos) {
			if (alvo == null || !alvo.isAtivoNoCombate()) continue;
			Efeito armaduraQuebrada = EffectFactory.criarEfeito("Armadura Quebrada (-30%)", 200, 0);
			manager.getEffectProcessor().aplicarEfeito(alvo, armaduraQuebrada);

			int distancia = empuxo.calcularDistanciaEmpuxo(FORCA_EMPUXAO, alvo.getPesoEntidade());
			KnockbackResult resultado = empuxo.calcularEmpuxoDirecional(alvo, direcao[0], direcao[1], distancia, mapa);
			empuxo.executarEmpuxo(alvo, resultado);
		}
	}
}
