package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import br.com.dantesrpg.model.items.PocaoAlquimica;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AprimorarPocao extends Habilidade {

	public AprimorarPocao() {
		super("Aprimorar Poção", "Selecione uma poção no seu inventário e aprimore para V2 baseado no dado de Sorte.", TipoHabilidade.ATIVA, 2, 120, 3, TipoAlvo.SI_MESMO, 0, 0, 0,
				Collections.emptyList());
	}

	public List<String> getOpcoesDinamicas(Personagem conjurador) {
		List<String> options = new ArrayList<>();
		if (conjurador == null || conjurador.getInventario() == null) {
			return options;
		}
		for (String itemType : conjurador.getInventario().getItensAgrupados().keySet()) {
			if (itemType.startsWith("PocaoAlquimica_") && !itemType.contains("_V2_")) {
				options.add(itemType);
			}
		}
		return options;
	}

	public void executarAprimoramento(Personagem conjurador, String itemType, int roll) {
		if (conjurador == null || itemType == null || !conjurador.getInventario().possuiItem(itemType)) {
			System.out.println(">>> Aprimorar Poção falhou: poção selecionada não está no inventário.");
			return;
		}

		// Remove a poção antiga
		conjurador.getInventario().removerItemPorTipo(itemType);

		// Estrutura do ID antigo: PocaoAlquimica_[Efeito]_[IS]
		// Nova estrutura: PocaoAlquimica_[Efeito]_[IS]_V2_[Roll]
		String newId = itemType + "_V2_" + roll;

		// Analisa os parâmetros antigos
		String[] parts = itemType.split("_");
		String tipoPocao = parts[1];
		int is = Integer.parseInt(parts[2]);

		// Cria a poção aprimorada
		PocaoAlquimica improved = new PocaoAlquimica(newId, tipoPocao, is, true, roll);

		// Adiciona no inventário
		conjurador.getInventario().adicionarItem(improved);
		System.out.println(">>> " + conjurador.getNome() + " aprimorou a poção para V2 baseada na Sorte (" + roll + "). Nova poção: " + improved.getNome());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		// Padrão não faz nada, pois a escolha é tratada no CombatManager
	}
}
