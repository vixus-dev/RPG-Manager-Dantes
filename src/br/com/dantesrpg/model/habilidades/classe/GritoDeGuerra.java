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

public class GritoDeGuerra extends Habilidade {

	public GritoDeGuerra() {
		super("Grito de Guerra", "Fortalece a equipe inteira, reduzindo o dano sofrido em 20% por 220 TU.",
				TipoHabilidade.ATIVA, 3, 100, 5, TipoAlvo.EQUIPE, 99, 0, 0, Collections.emptyList());
	}

	@Override
	public boolean afetaInimigos() {
		return false;
	}

	@Override
	public boolean afetaAliados() {
		return true;
	}

	@Override
	public boolean afetaSiMesmo() {
		return true;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		Map<String, Double> mods = new HashMap<>();
		mods.put("REDUCAO_DANO_MODIFICADOR", 0.20);

		for (Personagem alvo : alvos) {
			Efeito buff = new Efeito("Grito de Guerra", TipoEfeito.BUFF, 220, new HashMap<>(mods), 0, 0);
			alvo.adicionarEfeito(buff);
			alvo.recalcularAtributosEstatisticas();
		}

		System.out.println(">>> " + conjurador.getNome() + " ergueu um Grito de Guerra sobre a equipe.");
	}
}
