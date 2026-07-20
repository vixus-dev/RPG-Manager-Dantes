package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.controller.CombatController;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoEfeito;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/** Prende dois inimigos e amaldiçoa aleatoriamente uma habilidade de um deles. */
public class EnraizarCoral extends Habilidade {
	private static final int DURACAO_TU = 300;
	private static final String EFEITO_MALDICAO_CORAL = "Maldição de Coral";

	public EnraizarCoral() {
		super("Enraizar Coral", "Prende até dois alvos. Um deles recebe a Intoxicação por Coral, bloqueando uma habilidade ativa aleatória.",
				TipoHabilidade.ATIVA, 0, 100, 1, TipoAlvo.MULTIPLOS, 0, 0, 0, Collections.emptyList());
	}

	@Override public int getNumeroDeAlvos() { return 2; }
	@Override public int getAlcanceMaximo() { return 5; }

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		if (alvos == null || alvos.isEmpty()) return;

		List<Personagem> alvosEnraizados = new ArrayList<>();
		for (Personagem alvo : alvos) {
			if (alvo == null || !alvo.isAtivoNoCombate() || alvosEnraizados.size() >= getNumeroDeAlvos()) continue;
			alvo.adicionarEfeito(new Efeito("Enraizado por Coral", TipoEfeito.DEBUFF, DURACAO_TU,
					Map.of("MOVIMENTO", -1000.0), 0, 0));
			alvo.setMovimentoRestanteTurno(0);
			alvo.recalcularAtributosEstatisticas();
			alvosEnraizados.add(alvo);
		}
		if (alvosEnraizados.isEmpty()) return;

		Personagem alvoAmaldicoado = alvosEnraizados.get(ThreadLocalRandom.current().nextInt(alvosEnraizados.size()));
		Habilidade bloqueada = sortearHabilidadeBloqueavel(alvoAmaldicoado);
		aplicarMaldicaoDeCoral(alvoAmaldicoado, bloqueada);

		CombatController controller = manager.getMainController();
		if (controller != null) {
			controller.mostrarAlertaHabilidadeBloqueadaPorCoral(alvoAmaldicoado, bloqueada);
			controller.atualizarInterfaceTotal();
		}
	}

	private Habilidade sortearHabilidadeBloqueavel(Personagem alvo) {
		List<Habilidade> candidatas = alvo.getHabilidadesDeClasse().stream()
				.filter(habilidade -> habilidade != null && habilidade.getTipo() == TipoHabilidade.ATIVA)
				.filter(habilidade -> habilidade.getNome() != null && !habilidade.getNome().equalsIgnoreCase("Ataque Básico"))
				.collect(LinkedHashMap<String, Habilidade>::new,
						(mapa, habilidade) -> mapa.putIfAbsent(habilidade.getNome().toLowerCase(Locale.ROOT), habilidade),
						Map::putAll)
				.values().stream().toList();
		return candidatas.isEmpty() ? null : candidatas.get(ThreadLocalRandom.current().nextInt(candidatas.size()));
	}

	private void aplicarMaldicaoDeCoral(Personagem alvo, Habilidade bloqueada) {
		alvo.adicionarEfeito(new Efeito(EFEITO_MALDICAO_CORAL, TipoEfeito.DEBUFF, DURACAO_TU, null, 0, 0));
		Efeito efeitoAplicado = alvo.getEfeitosAtivos().get(EFEITO_MALDICAO_CORAL);
		if (efeitoAplicado != null) {
			efeitoAplicado.setDuracaoTUInicial(DURACAO_TU);
			efeitoAplicado.setDuracaoTURestante(DURACAO_TU);
		}
		if (bloqueada == null) {
			alvo.limparHabilidadeBloqueadaPorCoral();
			return;
		}
		alvo.bloquearHabilidadePorCoral(bloqueada.getNome());
		System.out.println(">>> ENRAIZAR CORAL: " + alvo.getNome() + " teve [" + bloqueada.getNome()
				+ "] bloqueada por 300 TU.");
	}
}
