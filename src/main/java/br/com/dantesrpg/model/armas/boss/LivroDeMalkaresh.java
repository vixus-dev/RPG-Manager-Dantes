package br.com.dantesrpg.model.armas.boss;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import br.com.dantesrpg.model.habilidades.Sanctum;
import br.com.dantesrpg.model.habilidades.boss.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Livro de Malkaresh — Arma do boss "O Justiceiro Cego - O Décimo Primeiro".
 *
 * Versão boss: dano 25, alcance 6, +14 INSPIRACAO.
 * Concede: Sanctum, Purus, Locus, MEAT...
 *
 * Quando o efeito "Falsa Justiça" está ativo, todo dano sofrido é parcialmente
 * devolvido a quem atacou (% da vida máxima do atacante proporcional ao % da
 * vida máxima do boss que foi perdida).
 */
public class LivroDeMalkaresh extends ArmaMelee {

	public LivroDeMalkaresh() {
		super("LivroDeMalkaresh", "Grimorio", "Um grimório profano de falsa justiça divina",
				Raridade.LENDARIO, 250, 25, 1, Atributo.INSPIRACAO, 100, 6);

		Map<Atributo, Integer> mods = new HashMap<>();
		mods.put(Atributo.INSPIRACAO, 14);
		this.setModificadoresDeAtributo(mods);

		this.addHabilidadeConcedida("Sanctum");
		this.addHabilidadeConcedida("Purus");
		this.addHabilidadeConcedida("Locus");
		this.addHabilidadeConcedida("MEAT...");
		this.setWielding(1); 
	}

	@Override
	public Habilidade getHabilidadeInstancia(String nomeHab) {
		if (nomeHab.equalsIgnoreCase("Sanctum"))
			return new Sanctum();
		if (nomeHab.equalsIgnoreCase("Purus"))
			return new Purus();
		if (nomeHab.equalsIgnoreCase("Locus"))
			return new Locus();
		if (nomeHab.equalsIgnoreCase("MEAT..."))
			return new Meat();
		return super.getHabilidadeInstancia(nomeHab);
	}

}
