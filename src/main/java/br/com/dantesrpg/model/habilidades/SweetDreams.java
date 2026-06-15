package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import br.com.dantesrpg.model.util.EffectFactory;
import java.util.*;

public class SweetDreams extends Habilidade {
	public SweetDreams() {
		super("sweet dreams", "aplica pesadelo em todos os alvos dormindo no ambiente",
				TipoHabilidade.ATIVA, 1, 100, 1, TipoAlvo.SI_MESMO, 0, 0.0, 1, Collections.emptyList());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " usa sweet dreams!");
		int count = 0;
		for (Personagem p : estado.getCombatentes()) {
			if (p.isAtivoNoCombate() && p.getEfeitosAtivos().containsKey("Dormindo")) {
				Efeito pesadelo = EffectFactory.criarEfeito("Pesadelo", 200, 0);
				manager.getEffectProcessor().aplicarEfeito(p, pesadelo);
				count++;
			}
		}
		System.out.println("Sweet Dreams: " + count + " alvos receberam Pesadelo.");
	}
}
