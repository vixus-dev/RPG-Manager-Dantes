package br.com.dantesrpg.model.habilidades.boss;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.Collections;
import java.util.List;

public class LaminasEspectrais extends Habilidade {

	public LaminasEspectrais() {
		super("Lâminas Espectrais", "Invoca espadas fantasmas que atacam o alvo repetidamente. (6 hits de 50%)",
				TipoHabilidade.ATIVA, 3, 100, 1, TipoAlvo.INDIVIDUAL, 0, 0.5, 6, Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 6;
	} // Alcance médio/longo

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(">>> " + conjurador.getNome() + ": Summoned Swords!");
	}
}