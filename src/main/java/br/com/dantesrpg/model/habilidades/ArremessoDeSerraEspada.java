package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class ArremessoDeSerraEspada extends Habilidade {

	public ArremessoDeSerraEspada() {
		super("Arremesso Serra-Espada", "Eu te pego pela cabeça e te jooj", TipoHabilidade.ATIVA, 2, 80, 1,
				TipoAlvo.LINHA, 1, 0.8, 1, Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 5;
	}

	// --- LÓGICA DE EXECUÇÃO ---
	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " usa " + getNome() + "!");
	}
}