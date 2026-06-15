package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;

import java.util.Collections;
import java.util.List;

public class ParadaCardiaca extends Habilidade {

	public ParadaCardiaca() {
		super("Parada Cardíaca", "Desfere um golpe concentrado de pressão cardíaca que causa 175% de dano em um único alvo.",
				TipoHabilidade.ATIVA, 1, 115, 1, TipoAlvo.INDIVIDUAL, 1.75, 1, Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 3;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		if (alvos == null || alvos.isEmpty()) {
			return;
		}

		Personagem alvo = alvos.get(0);
		System.out.println(conjurador.getNome() + " ativa Parada Cardíaca em " + alvo.getNome() + "!");
	}
}
