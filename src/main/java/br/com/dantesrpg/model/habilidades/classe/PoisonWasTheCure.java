package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class PoisonWasTheCure extends Habilidade {
	public static final String INPUT_DADO_CURA = "DADO_BLACK_MAGIC";

	@Override
	public String getDescricao() {
		return "Cura um aliado em 10% da vida m\u00e1xima + (2 * resultado do dado).";
	}

	public PoisonWasTheCure() {
		super("Black Magic", "Cura um aliado em 10% + (2 * IS).", TipoHabilidade.ATIVA, 4, 100, 1,
				TipoAlvo.INDIVIDUAL, 0, 0, Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 6;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		if (alvos == null || alvos.isEmpty()) {
			return;
		}
		Personagem alvo = alvos.get(0);

		// Cálculo da Cura: 10% MaxHP Alvo + (2 * IS Darrell)
		int resultadoDado = obterResultadoDado(manager, INPUT_DADO_CURA);
		int curaBase = (int) (alvo.getVidaMaxima() * 0.10);
		int curaExtra = resultadoDado * 2;
		int curaTotal = curaBase + curaExtra;

		alvo.setVidaAtual(alvo.getVidaAtual() + curaTotal, estado, manager.getController());

		System.out.println(">>> Poison was the Cure: " + alvo.getNome() + " recuperou " + curaTotal + " HP.");
	}

	private int obterResultadoDado(CombatManager manager, String chaveDado) {
		if (manager == null || manager.getLastInput() == null) {
			return 0;
		}
		return Math.max(0, manager.getLastInput().getResultadoDado(chaveDado));
	}
}
