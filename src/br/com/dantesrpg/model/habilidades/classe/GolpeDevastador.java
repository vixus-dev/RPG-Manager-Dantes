package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoEfeito;
import br.com.dantesrpg.model.enums.TipoHabilidade;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GolpeDevastador extends Habilidade {

	public GolpeDevastador() {
		super("Golpe Devastador", "Desfere 250% de dano e abre uma Hemorragia por 400 TU.", TipoHabilidade.ATIVA, 4,
				200, 8, TipoAlvo.INDIVIDUAL, 2.5, 1, Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 2;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		if (alvos == null || alvos.isEmpty()) {
			return;
		}

		Personagem alvo = alvos.get(0);
		Map<String, Double> mods = new HashMap<>();
		mods.put("PERCENTUAL_HP_MAX", 0.025);

		Efeito hemorragiaExistente = alvo.getEfeitosAtivos().get("Hemorragia");
		if (hemorragiaExistente != null && hemorragiaExistente.getModificadores() != null) {
			hemorragiaExistente.getModificadores().put("PERCENTUAL_HP_MAX", 0.025);
			hemorragiaExistente.setDuracaoTURestante(Math.max(hemorragiaExistente.getDuracaoTURestante(), 400));
		} else {
			Efeito hemorragia = new Efeito("Hemorragia", TipoEfeito.DOT, 400, mods, 0, 100);
			alvo.adicionarEfeito(hemorragia);
		}

		System.out.println(">>> " + alvo.getNome() + " sofreu Hemorragia de Golpe Devastador.");
	}
}
