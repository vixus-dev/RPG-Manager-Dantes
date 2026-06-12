package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import br.com.dantesrpg.model.util.EffectFactory;
import java.util.Collections;
import java.util.List;

public class VeuDeSombras extends Habilidade {

	public VeuDeSombras() {
		super("Véu de Sombras", "Sacrifica 10% do HP máximo para entrar em modo Furtivo (Stealth).", TipoHabilidade.ATIVA,
				1, // Custo de Mana
				60, // Custo de TU
				1, // Nível Necessário
				TipoAlvo.SI_MESMO,
				0.0, // Multiplicador de Dano
				0, // Ticks de Dano
				Collections.emptyList());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " usa " + getNome() + "!");
		
		// Custo de HP: 10% da vida máxima
		double custoVida = conjurador.getVidaMaxima() * 0.10;
		double novaVida = Math.max(1.0, conjurador.getVidaAtual() - custoVida);
		conjurador.setVidaAtual(novaVida, estado, manager.getController());
		conjurador.recalcularAtributosEstatisticas();
		
		// Aplica Stealth
		Efeito stealth = EffectFactory.criarEfeito("Stealth", 250, 0); // 150 TU de duração
		conjurador.adicionarEfeito(stealth);
		
		br.com.dantesrpg.model.util.SessionLogger.log("👤 " + conjurador.getNome() + " sacrificou " + (int)custoVida + " HP e desapareceu nas sombras!");
	}
}
