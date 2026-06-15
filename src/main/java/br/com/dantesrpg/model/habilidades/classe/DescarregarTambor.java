package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;

import java.util.Collections;
import java.util.List;

public class DescarregarTambor extends Habilidade {

	public DescarregarTambor() {
		super("Descarregar Tambor",
				"Dispara 6 tiros sequenciais. Cada bala causa 50% do dano base e resolve crítico, armadura e escudo separadamente.",
				TipoHabilidade.ATIVA, 3, 200, 5, TipoAlvo.INDIVIDUAL, 0.50, 6,
				Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 8;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " descarrega o tambor!");
	}
}
