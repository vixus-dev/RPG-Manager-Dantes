package br.com.dantesrpg.model.habilidades;

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

public class CampoDeForça2 extends Habilidade {
	public CampoDeForça2() {
		super("Campo de Força II", "Cria um escudo de energia com valor de 50% da vida maxima.", TipoHabilidade.ATIVA,
				2, 100, 3, // Custo de mana de 2
				TipoAlvo.SI_MESMO, 0.0, // Multiplicador de dano (não causa dano)
				0, // Ticks de dano
				Collections.emptyList());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " usa Técnica de Barreira!");

		// Calcula o valor do escudo (50% da VIDA ATUAL)
		int valorEscudo = (int) (conjurador.getVidaMaxima() * 0.5);

		if (valorEscudo <= 0) {
			System.out.println(">>> " + conjurador.getNome() + " não tem vida suficiente para criar uma barreira.");
			return;
		}

		// Adiciona o escudo ao personagem
		conjurador.setEscudoAtual(conjurador.getEscudoAtual() + valorEscudo);
		System.out.println(">>> " + conjurador.getNome() + " criou um Campo de força com valor de " + valorEscudo
				+ " Total: " + conjurador.getEscudoAtual());

		Efeito escudoEfeito = new Efeito("Campo De Força", TipoEfeito.BUFF, 500, // Duração
				null, 0, 0);
		conjurador.adicionarEfeito(escudoEfeito);

		// Recalcula stats (para a UI atualizar o valor do escudo e o ícone do efeito)
		conjurador.recalcularAtributosEstatisticas();
	}
}