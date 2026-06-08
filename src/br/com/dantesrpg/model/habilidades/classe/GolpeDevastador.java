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
		super("Golpe Devastador", "Desfere 250% de dano e abre uma Hemorragia (5 ticks de 2% HP max, -30% cura por 1000 TU).", TipoHabilidade.ATIVA, 4,
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

		// Hemorragia: 5 ticks × 2% HP max, reduz cura em 30% pelo dobro da duração (1000 TU)
		Efeito hemorragiaExistente = alvo.getEfeitosAtivos().get("Hemorragia");
		if (hemorragiaExistente != null && hemorragiaExistente.getModificadores() != null) {
			// Renova duração se já existe
			hemorragiaExistente.setDuracaoTURestante(Math.max(hemorragiaExistente.getDuracaoTURestante(), 500));
		} else {
			// Cria via Factory (já inclui PERCENTUAL_HP_MAX e REDUCAO_CURA)
			Efeito hemorragia = br.com.dantesrpg.model.util.EffectFactory.criarEfeito("Hemorragia", 500, 0);
			alvo.adicionarEfeito(hemorragia);
		}

		alvo.recalcularAtributosEstatisticas();
		System.out.println(">>> " + alvo.getNome() + " sofreu Hemorragia de Golpe Devastador.");
	}
}
