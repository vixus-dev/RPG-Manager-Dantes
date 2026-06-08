package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class ImpactoVingativo extends Habilidade {
	public ImpactoVingativo() {
		super("Impacto Vingativo",
				"Causa dano equivalente a 5x os pontos de vida perdidos.", TipoHabilidade.ATIVA, 3, 125, 5,
				TipoAlvo.INDIVIDUAL, 0, 1, Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 2;
	}

	@Override
	public double getMultiplicadorModificado(Personagem ator, Personagem alvo, EstadoCombate estado) {
		return 0; // Sem dano base - o dano real é calculado no executar
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		if (alvos == null || alvos.isEmpty()) return;

		double vidaPerdida = conjurador.getVidaMaxima() - conjurador.getVidaAtual();
		double dano = vidaPerdida * 5.0;

		if (dano <= 0) {
			System.out.println(">>> Impacto Vingativo falhou: " + conjurador.getNome() + " não perdeu vida!");
			return;
		}

		// Aplica modificador de crítico
		boolean critico = Math.random() < conjurador.getTaxaCritica();
		if (critico) {
			dano *= (1 + conjurador.getDanoCritico());
			System.out.println(">>> ACERTO CRÍTICO!");
		}

		// Aplica bônus de dano percentual
		if (conjurador.getBonusDanoPercentual() > 0) {
			dano *= (1.0 + conjurador.getBonusDanoPercentual());
		}

		Personagem alvo = alvos.get(0);
		System.out.println(">>> Impacto Vingativo! " + conjurador.getNome() + " perdeu "
				+ String.format("%.0f", vidaPerdida) + " HP → causa " + String.format("%.0f", dano) + " de dano!");

		manager.aplicarDanoAoAlvo(conjurador, alvo, dano, false, TipoAcao.HABILIDADE, estado);
	}
}
