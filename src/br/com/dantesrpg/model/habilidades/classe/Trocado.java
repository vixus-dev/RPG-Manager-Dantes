package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class Trocado extends Habilidade {
	public Trocado() {
		super("Trocado", "Arremessa de 1-4 moedas para o alto, ao atingi-las causa dano crescente",
				TipoHabilidade.ATIVA, 2, 80, 3, TipoAlvo.INDIVIDUAL, 0.75, 1, Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 6;
	}

	@Override
	public int getTicksModificados(Personagem ator, AcaoMestreInput input) {
		if (input == null)
			return 1; // Segurança

		// Lê o dado "DADO_CHANCE_TROCADO" que a HUD forneceu
		int rolagemHits = input.getResultadoDado("DADO_CHANCE_TROCADO");

		if (rolagemHits <= 0) {
			System.err.println("Trocado: Rolagem 1d4 inválida ou não fornecida. Usando 1 hit.");
			return 1;
		}

		// Limita entre 1 e 4 hits
		int hits = Math.max(1, Math.min(rolagemHits, 4));
		System.out.println(">>> Trocado: " + hits + " moedas acertadas!");
		return hits;
	}

	@Override
	public void executar(Personagem c, List<Personagem> a, EstadoCombate estado, CombatManager manager) {
		System.out.println(c.getNome() + " usa Trocado!");
		// Não aplica efeitos, apenas causa dano (tratado no hook de ticks)
	}
}