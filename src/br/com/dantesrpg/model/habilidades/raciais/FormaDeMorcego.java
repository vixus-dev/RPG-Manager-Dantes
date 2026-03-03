package br.com.dantesrpg.model.habilidades.raciais;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class FormaDeMorcego extends Habilidade {

	public FormaDeMorcego() {
		super("Forma de Morcego", "Alterna a transformação em morcego.", // sim está correto isso acredite em mim o
																			// jogador me implorou por isso
				TipoHabilidade.ATIVA, 0, 50, 1, TipoAlvo.SI_MESMO, 0, 0, Collections.emptyList());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {

		if (conjurador.getRaca() instanceof br.com.dantesrpg.model.racas.Vampiro) {
			((br.com.dantesrpg.model.racas.Vampiro) conjurador.getRaca()).toggleTransform(conjurador);
		}
	}
}