package br.com.dantesrpg.model.armas.boss;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import br.com.dantesrpg.model.habilidades.boss.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Justiça & Esplendor — Espada mítica do "Arcanjo - O Executor".
 *
 * Dano 100, alcance 3, +20 FORCA. Andar -4.
 * Concede: Estocada Divina, Corte Divino, Quebra-Elmo, Arremesso Divino.
 */
public class JusticaESplendor extends ArmaMelee {

	public JusticaESplendor() {
		super("Justiça & Esplendor", "Espada Mítica",
				"A lâmina do julgamento celeste, forjada para executar os ímpios.",
				Raridade.MITICO, 450, 250, 1, Atributo.FORCA, 100, 3);

		Map<Atributo, Integer> mods = new HashMap<>();
		mods.put(Atributo.FORCA, 20);
		mods.put(Atributo.SAGACIDADE, 20);
		mods.put(Atributo.TOPOR, 20);
		this.setModificadoresDeAtributo(mods);

		this.addHabilidadeConcedida("Estocada Divina");
		this.addHabilidadeConcedida("Corte Divino");
		this.addHabilidadeConcedida("Quebra-Elmo");
		this.addHabilidadeConcedida("Arremesso Divino");
		this.setWielding(2); 
	}

	@Override
	public Habilidade getHabilidadeInstancia(String nomeHab) {
		if (nomeHab.equalsIgnoreCase("Estocada Divina"))
			return new EstocadaDivina();
		if (nomeHab.equalsIgnoreCase("Corte Divino"))
			return new CorteDivino();
		if (nomeHab.equalsIgnoreCase("Quebra-Elmo"))
			return new QuebraElmo();
		if (nomeHab.equalsIgnoreCase("Arremesso Divino"))
			return new ArremessoDivino();
		return super.getHabilidadeInstancia(nomeHab);
	}
}
