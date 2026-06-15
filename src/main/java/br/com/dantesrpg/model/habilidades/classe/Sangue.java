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

public class Sangue extends Habilidade {
	private int custoTUAtual = 100;

	public Sangue() {
		super("SANGUE...", "Caçar... Consumir... Adaptar...",
				TipoHabilidade.ATIVA, 1, 100, 1, TipoAlvo.SI_MESMO, 0, 0.0, 0, Collections.emptyList());
	}

	@Override
	public int getCustoTU() {
		return custoTUAtual;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " ativa " + getNome() + "!");

		// --- 1. Aplica/Acumula o buff "Adaptando" PRIMEIRO (para aumentar HP máximo antes de curar) ---
		Efeito existente = conjurador.getEfeitosAtivos().get("Adaptando");
		int novosStacks;

		if (existente != null) {
			novosStacks = existente.getStacks() + 1;
		} else {
			novosStacks = 1;
		}

		// HP: cada stack = +25% composto sobre a vida máxima base
		// HP_MAXIMO (flat bonus) = vidaMaximaBase * (1.25^stacks - 1)
		double hpBonus = conjurador.getVidaMaximaBase() * (Math.pow(1.25, novosStacks) - 1);

		// Dano: cada stack = +10% de bônus de dano
		double danoBonus = novosStacks * 0.10;

		Map<String, Double> modificadores = new HashMap<>();
		modificadores.put("HP_MAXIMO", hpBonus);
		modificadores.put("DANO_BONUS_PERCENTUAL", danoBonus);

		// Remove o efeito antigo e aplica o novo com valores atualizados
		if (existente != null) {
			conjurador.removerEfeito("Adaptando");
		}

		Efeito adaptando = new Efeito("Adaptando", TipoEfeito.BUFF, 99999, modificadores, 0, 0);
		adaptando.setStacks(novosStacks);
		conjurador.adicionarEfeito(adaptando);

		System.out.println(">>> Buff [Adaptando] aplicado! Acúmulos: " + novosStacks);
		System.out.println("    +HP Máximo: +" + String.format("%.0f", hpBonus)
				+ " | +Dano: +" + String.format("%.0f%%", danoBonus * 100));

		// --- 2. Cura 50% da vida máxima (agora já inclui o bônus do Adaptando) ---
		double cura = conjurador.getVidaMaxima() * 0.50;
		double vidaNova = conjurador.getVidaAtual() + cura;
		conjurador.setVidaAtual(vidaNova, estado, null);
		System.out.println(">>> " + conjurador.getNome() + " recuperou " + String.format("%.0f", cura)
				+ " HP! (50% da vida máxima)");

		// --- 3. Reduz o custoTU em 10 por stack (mínimo 50) ---
		custoTUAtual = Math.max(50, 100 - (novosStacks * 10));
		System.out.println("    Cooldown da skill: " + custoTUAtual + " TU");
	}
}
