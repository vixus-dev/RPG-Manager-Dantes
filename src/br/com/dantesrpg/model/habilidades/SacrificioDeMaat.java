package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.TipoAcao;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoEfeito;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SacrificioDeMaat extends Habilidade {

	public SacrificioDeMaat() {
		super("Sacrifício de Maat", "Causa dano a um aliado igual à Inspiração do usuário e concede a ele um bônus de dano de 5x o dano sofrido.", TipoHabilidade.ATIVA,
				2, // Custo de Mana
				110, // Custo de TU
				1, // Nível Necessário
				TipoAlvo.INDIVIDUAL,
				0.0, // Multiplicador de Dano
				0, // Ticks de Dano
				Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 6;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		if (alvos.isEmpty()) return;
		Personagem alvo = alvos.get(0);
		
		System.out.println(conjurador.getNome() + " usa " + getNome() + " em " + alvo.getNome() + "!");
		
		// Calcula dano baseado na Inspiração
		double insp = conjurador.getAtributosFinais().getOrDefault(Atributo.INSPIRACAO, 1);
		double danoAliado = Math.max(1.0, insp);
		
		// Aplica dano ao aliado (ignora armadura porque é sacrifício místico direto)
		manager.aplicarDanoAoAlvo(conjurador, alvo, danoAliado, true, TipoAcao.OUTRO, estado);
		
		// Calcula bônus de dano: 7.5x o dano em percentual (ex: dano de 10 = 75% de bônus de dano)
		double bonusDano = (7.5 * danoAliado) / 100.0;
		
		// Cria e aplica o buff
		Map<String, Double> mods = new HashMap<>();
		mods.put("DANO_BONUS_PERCENTUAL", bonusDano);
		Efeito buffDano = new Efeito("Sacrifício de Maat", TipoEfeito.BUFF, 300, mods, 0, 0); // 300 TU de duração
		alvo.adicionarEfeito(buffDano);
		
		alvo.recalcularAtributosEstatisticas();
		

}
}
