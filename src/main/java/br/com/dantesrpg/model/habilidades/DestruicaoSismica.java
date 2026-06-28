package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class DestruicaoSismica extends Habilidade {

	public DestruicaoSismica() {
		super("Destruição Sísmica", "Arremessa o machado em um cone sísmico devastador", TipoHabilidade.ATIVA,
				1, // Custo de Mana
				90, // Custo de TU
				1, // Nível Necessário
				TipoAlvo.CONE, // Cone de 30°
				10, // Tamanho/Alcance da área (10 células)
				1.5, // Multiplicador de dano base (igual ao Arremesso de Machado)
				1, // Ticks de dano
				Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 10;
	}

	@Override
	public int getAnguloCone() {
		return 30;
	}

	@Override
	public boolean afetaAliados() {
		return false;
	}

	// --- LÓGICA DE EXECUÇÃO ---
	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " usa " + getNome() + "!");
	}
}
