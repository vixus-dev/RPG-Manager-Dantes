package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoEfeito;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BencaoDeMaat extends Habilidade {

	public BencaoDeMaat() {
		super("Bênção de Maat", "Aplica um escudo igual a 2x a Inspiração do usuário e concede +25% de bônus de dano a um aliado.", TipoHabilidade.ATIVA,
				2, // Custo de Mana
				100, // Custo de TU
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
		
		// Calcula e aplica o escudo baseado em 2 * Inspiração
		double insp = conjurador.getAtributosFinais().getOrDefault(Atributo.INSPIRACAO, 1);
		double valorEscudo = 2 * insp;
		alvo.adicionarEscudoNormal(valorEscudo);
		
		// Cria e aplica o buff de +25% de dano
		Map<String, Double> mods = new HashMap<>();
		mods.put("DANO_BONUS_PERCENTUAL", 0.25);
		Efeito buffDano = new Efeito("Bênção de Maat", TipoEfeito.BUFF, 200, mods, 0, 0); // 200 TU de duração
		alvo.adicionarEfeito(buffDano);
		
		alvo.recalcularAtributosEstatisticas();
		
		br.com.dantesrpg.model.util.SessionLogger.log("⚖️ " + conjurador.getNome() + " abençoou " + alvo.getNome() + " com um escudo de " + (int)valorEscudo + " e +25% de Dano!");
	}
}
