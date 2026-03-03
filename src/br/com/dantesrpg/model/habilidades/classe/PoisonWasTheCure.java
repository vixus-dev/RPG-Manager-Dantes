package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class PoisonWasTheCure extends Habilidade {
	public PoisonWasTheCure() {
		super("Poison was the Cure", "Cura um aliado em 10% + (2 * IS).", TipoHabilidade.ATIVA, 4, 100, 1,
				TipoAlvo.INDIVIDUAL, 0, 0, Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 6;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		Personagem alvo = alvos.get(0);

		// Cálculo da Cura: 10% MaxHP Alvo + (2 * IS Darrell)
		int inspiracao = conjurador.getAtributosFinais().getOrDefault(Atributo.INSPIRACAO, 0);
		int curaBase = (int) (alvo.getVidaMaxima() * 0.20);
		int curaExtra = inspiracao * 3;
		int curaTotal = curaBase + curaExtra;

		alvo.setVidaAtual(alvo.getVidaAtual() + curaTotal, estado, manager.getController());

		System.out.println(">>> Poison was the Cure: " + alvo.getNome() + " recuperou " + curaTotal + " HP.");
	}
}