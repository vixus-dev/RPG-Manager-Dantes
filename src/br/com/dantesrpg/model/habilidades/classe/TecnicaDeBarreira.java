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

public class TecnicaDeBarreira extends Habilidade {
	public TecnicaDeBarreira() {
		super("Técnica de Barreira", "Cria um escudo de mana com valor de 50% da vida atual.", TipoHabilidade.ATIVA, 2,
				150, 3, // Custo de mana de 2
				TipoAlvo.SI_MESMO, 0.0, // Multiplicador de dano (não causa dano)
				0, // Ticks de dano
				Collections.emptyList());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " usa Técnica de Barreira!");

		// Calcula o valor do escudo (50% da VIDA ATUAL)
		int valorEscudo = (int) (conjurador.getVidaAtual() * 0.50);

		if (valorEscudo <= 0) {
			System.out.println(">>> " + conjurador.getNome() + " não tem vida suficiente para criar uma barreira.");
			return;
		}

		// Adiciona o escudo ao personagem
		conjurador.setEscudoAtual(conjurador.getEscudoAtual() + valorEscudo);
		System.out.println(">>> " + conjurador.getNome() + " criou Barreira de Mana de " + valorEscudo
				+ " escudo! Total: " + conjurador.getEscudoAtual());

		// Cria e aplica o efeito "marcador" (para a UI saber que a barreira está ativa)
		Efeito escudoEfeito = new Efeito("Barreira de Mana", TipoEfeito.BUFF, 300, // Duração
				null, 0, 0);
		conjurador.adicionarEfeito(escudoEfeito); // Aplica o marcador

		// Recalcula stats (para a UI atualizar o valor do escudo e o ícone do efeito)
		conjurador.recalcularAtributosEstatisticas();
	}
}