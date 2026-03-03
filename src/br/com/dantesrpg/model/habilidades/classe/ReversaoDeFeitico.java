package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate; // Import necessário
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import java.util.Collections;
import java.util.List;

public class ReversaoDeFeitico extends Habilidade {
	public ReversaoDeFeitico() {
		super("Reversão de Feitiço", "Restaura a vida perdida nos últimos 300 TU.", TipoHabilidade.ATIVA, 2, 100, 5, // Nível
																														// 5
				TipoAlvo.SI_MESMO, 0, 0, Collections.emptyList());
	}

	@Override
	public int getCooldownTU() {
		return 600; // Define o cooldown de 600 TU
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " usa Reversão de Feitiço!");

		if (estado == null) {
			System.err.println("Erro: Reversão de Feitiço falhou (EstadoCombate nulo).");
			return;
		}

		// Pega o dano sofrido nos últimos 300 TU
		double vidaARecuperar = conjurador.getDanoSofridoRecentemente(300, estado.getTempoGlobalCombate());

		if (vidaARecuperar > 0) {
			// Usa o setVidaAtual que passa o 'estado' (para regras de Humano/Marionette)
			conjurador.setVidaAtual(conjurador.getVidaAtual() + vidaARecuperar, estado, manager.getController());
			System.out.println(">>> " + conjurador.getNome() + " reverteu e recuperou " + vidaARecuperar + " de HP!");
		} else {
			System.out.println(">>> " + conjurador.getNome() + " não sofreu dano recentemente.");
		}

		// Recalcula stats (para a UI atualizar o HP)
		conjurador.recalcularAtributosEstatisticas();
	}
}