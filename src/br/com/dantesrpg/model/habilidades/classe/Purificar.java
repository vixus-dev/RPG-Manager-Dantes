package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class Purificar extends Habilidade {
	public Purificar() {
		super("Purificar", "Remove todos os efeitos negativos do alvo.", TipoHabilidade.ATIVA, 0, 80, 3,
				TipoAlvo.INDIVIDUAL, 0, 0, Collections.emptyList());
	}

	@Override
	public int getCustoMana() {
		return -1;
	}

	@Override
	public int getAlcanceMaximo() {
		return 4;
	}

	@Override
	public void executar(Personagem c, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		Personagem alvo = alvos.get(0);
		List<String> debuffs = new ArrayList<>();

		for (Efeito e : alvo.getEfeitosAtivos().values()) {
			if (e.getTipo() == TipoEfeito.DEBUFF || e.getTipo() == TipoEfeito.DOT) {
				debuffs.add(e.getNome());
			}
		}
		for (String nome : debuffs) {
			alvo.removerEfeito(nome);
			System.out.println(">>> Purificado: " + nome + " removido de " + alvo.getNome());
		}
		alvo.recalcularAtributosEstatisticas();
	}
}