package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoEfeito;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import java.util.Collections;
import java.util.List;

public class Mordida extends Habilidade {
	public Mordida() {
		super("Mordida", "Vo te moide", TipoHabilidade.ATIVA, 1, // Custo de Mana 3
				90, // Custo de TU 150
				1, // Nível 5
				TipoAlvo.INDIVIDUAL, 1.15, // 115% de dano
				1, Collections.emptyList());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " Moidida!");

		if (alvos == null || alvos.isEmpty())
			return;
		Personagem alvoPrincipal = alvos.get(0);
		int danoTick = 3;

		Efeito Veneno = new Efeito("Veneno", TipoEfeito.DOT, 455, null, danoTick, 65);

		// O Personagem.java já tem um método para adicionar efeitos
		alvoPrincipal.adicionarEfeito(Veneno);
		System.out.println(">>> " + getNome() + " aplica [Veneno] (Dano: " + danoTick + "/tick) em "
				+ alvoPrincipal.getNome() + ".");

		// É crucial recalcular os stats para a UI (e lógicas) verem o novo efeito
		alvoPrincipal.recalcularAtributosEstatisticas();
	}
}