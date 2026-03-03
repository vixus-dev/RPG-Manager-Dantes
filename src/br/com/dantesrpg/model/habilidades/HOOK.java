package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class HOOK extends Habilidade {

	public HOOK() {
		super("HOOK", "Dá Uma bela Lapada seca", TipoHabilidade.ATIVA, 0, 80, 1, TipoAlvo.LINHA, 2, 5, 1,
				Collections.emptyList());
	}

	// --- CONFIGURAÇÃO DO CONE ---

	@Override
	public int getAlcanceMaximo() {
		return 3;
	}

	// --- LÓGICA DE EXECUÇÃO ---
	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " usa " + getNome() + "!");
	}
}