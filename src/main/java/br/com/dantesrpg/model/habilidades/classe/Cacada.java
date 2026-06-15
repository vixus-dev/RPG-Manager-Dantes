package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import java.util.Collections;
import java.util.List;

public class Cacada extends Habilidade {

	@Override
	public int getAlcanceMaximo() {
		return 6;
	}

	public Cacada() {
		super("Caçada", // Nome
				"Roda 1d6 e dispara de 1 a 6 tiros em inimigos aleatórios.", // Descrição
				TipoHabilidade.ATIVA, 3, // Custo de Mana (Corrigido)
				150, // Custo de TU
				5, // Nível Necessário
				TipoAlvo.MULTIPLOS, // Sinaliza que é uma habilidade multi-alvo
				0.80, // Multiplicador de Dano (80% por tiro)
				1, // Ticks de Dano (1 tick por tiro)
				Collections.emptyList());
	}

	@Override
	public void executar(Personagem c, List<Personagem> a, EstadoCombate estado, CombatManager manager) {
		System.out.println(c.getNome() + " usa " + getNome() + "!");
	}
}