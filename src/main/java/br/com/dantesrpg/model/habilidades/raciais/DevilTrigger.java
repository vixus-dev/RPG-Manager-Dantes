package br.com.dantesrpg.model.habilidades.raciais;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import java.util.Collections;
import java.util.List;

public class DevilTrigger extends Habilidade {

	public DevilTrigger() {
		this(25);
	}

	public DevilTrigger(int custoTU) {
		super("Devil Trigger", "Usa 5 acúmulos para liberar a forma demoníaca.", TipoHabilidade.ATIVA, 0, custoTU, 1,
				TipoAlvo.SI_MESMO, 0, 0, Collections.emptyList());
	}

	@Override
	public String getMotivoBloqueio(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado) {
		if (conjurador == null) {
			return "Devil Trigger exige um conjurador.";
		}
		if (!(conjurador.getRaca() instanceof br.com.dantesrpg.model.racas.HalfDemon demon)) {
			return "Devil Trigger só pode ser usado por um Half-Demon.";
		}
		if (demon.isTransformed()) {
			return "Devil Trigger já está transformado.";
		}
		if (demon.getCurrentStacks() < 5) {
			return "Devil Trigger exige 5 acúmulos.";
		}
		return null;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		if (conjurador.getRaca() instanceof br.com.dantesrpg.model.racas.HalfDemon demon
				&& demon.getCurrentStacks() >= 5 && !demon.isTransformed()) {
			demon.ativarDevilTrigger(conjurador);
		}
	}
}
