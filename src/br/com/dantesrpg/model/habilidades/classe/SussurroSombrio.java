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
import java.util.List;

public class SussurroSombrio extends Habilidade {

	public SussurroSombrio() {
		super("Sussurro Sombrio", // Nome
				"Durante 300 TU, tem 25% de chance de agir novamente após o turno.", // Descrição
				TipoHabilidade.ATIVA, 3, // Custo de Mana
				50, // Custo de TU
				8, // Nível Necessário
				TipoAlvo.SI_MESMO, 0, // Multiplicador de Dano
				0, // Ticks de Dano
				Collections.emptyList());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " ativa o " + getNome() + "!");

		// Cria o Efeito (BUFF)
		Efeito buffSussurro = new Efeito("Sussurro Sombrio", // Nome do Efeito
				TipoEfeito.BUFF, // Tipo (para o ícone azul)
				300, // Duração (conforme sua regra)
				null, // Sem modificadores de stats
				0, 0 // (Não é DoT)
		);

		// Aplica no conjurador
		conjurador.adicionarEfeito(buffSussurro);
		System.out.println(">>> " + conjurador.getNome() + " está sob o efeito [Sussurro Sombrio].");

		// Recalcula os stats (para a UI mostrar o novo ícone de buff)
		conjurador.recalcularAtributosEstatisticas();
	}
}