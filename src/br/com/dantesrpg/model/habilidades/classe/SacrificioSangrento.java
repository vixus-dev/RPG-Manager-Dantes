package br.com.dantesrpg.model.habilidades.classe;

import java.util.List;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAcao;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;

public class SacrificioSangrento extends Habilidade {
	public SacrificioSangrento() {
		super("Sacrifício Sangrento", "Destrói uma invocação para causar 500% de dano.", TipoHabilidade.ATIVA, 1, 100,
				8, TipoAlvo.MULTIPLOS, 0, 5.0, 1, null);
	}

	@Override
	public int getNumeroDeAlvos() {
		return 2;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		if (alvos.size() < 2)
			return;

		Personagem sacrificio = alvos.get(0);

		for (int e = 0; e < alvos.size(); e++) {

			if (e == 0) {
				manager.aplicarDanoAoAlvo(conjurador, sacrificio, 9999, true, TipoAcao.OUTRO, estado);
				System.out.println("dano aplicado no sacrificio");
			} else {
				System.out.println("dano aplicado na vitima");
			}
		}
	}
}