package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class ThyEndThisNow extends Habilidade {

	public ThyEndThisNow() {
		super("thy end this now", "Ataque impiedoso com 3 golpes.", TipoHabilidade.ATIVA, 
				0, // custo mana
				100, // custo TU
				1, // nivel
				TipoAlvo.INDIVIDUAL, 
				0, // tamanhoArea
				0.5, // multiplicadorDeDano (por tick)
				3, // ticksDeDano
				Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 20;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(">>> " + conjurador.getNome() + " usa thy end this now!");
		for (Personagem alvo : alvos) {
			if (Math.random() <= 0.10) {
				Efeito stun = new Efeito("Atordoado", TipoEfeito.DEBUFF, 50, null, 0, 0);
				alvo.adicionarEfeito(stun);
				System.out.println(">>> " + alvo.getNome() + " foi atordoado por thy end this now!");
			}
		}
	}
}
