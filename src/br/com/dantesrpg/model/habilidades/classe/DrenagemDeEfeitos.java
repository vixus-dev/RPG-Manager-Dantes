package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class DrenagemDeEfeitos extends Habilidade {
	public DrenagemDeEfeitos() {
		super("Drenagem de Efeitos",
				"Absorve todos os debuffs e converte a duração restante em bônus de dano temporário.",
				TipoHabilidade.ATIVA, 5, 10, 8, TipoAlvo.SI_MESMO, 0, 0, Collections.emptyList());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		// Coleta todos os debuffs ativos
		List<String> debuffsParaRemover = new ArrayList<>();
		int duracaoTotalAbsorvida = 0;

		for (Map.Entry<String, Efeito> entry : conjurador.getEfeitosAtivos().entrySet()) {
			Efeito efeito = entry.getValue();
			if (efeito.getTipo() == TipoEfeito.DEBUFF || efeito.getTipo() == TipoEfeito.DOT) {
				duracaoTotalAbsorvida += efeito.getDuracaoTURestante();
				debuffsParaRemover.add(entry.getKey());
			}
		}

		if (debuffsParaRemover.isEmpty()) {
			System.out.println(">>> Drenagem de Efeitos: Nenhum debuff para absorver!");
			return;
		}

		// Remove todos os debuffs
		for (String nomeEfeito : debuffsParaRemover) {
			conjurador.removerEfeito(nomeEfeito);
			System.out.println(">>> Debuff [" + nomeEfeito + "] absorvido!");
		}

		// Converte duração em bônus de dano: 1 TU = 1% = 0.01
		double bonusDano = duracaoTotalAbsorvida * 0.01;
		int duracaoBuff = duracaoTotalAbsorvida; // Proporção 1:1

		Map<String, Double> mods = new HashMap<>();
		mods.put("DANO_BONUS_PERCENTUAL", bonusDano);

		Efeito buff = new Efeito("Drenagem de Efeitos", TipoEfeito.BUFF, duracaoBuff, mods, 0, 0);
		conjurador.adicionarEfeito(buff);

		System.out.println(">>> Drenagem de Efeitos: " + conjurador.getNome() + " absorveu " + debuffsParaRemover.size()
				+ " debuffs! +" + duracaoTotalAbsorvida + "% de dano por " + duracaoBuff + " TU!");

		conjurador.recalcularAtributosEstatisticas();
	}
}
