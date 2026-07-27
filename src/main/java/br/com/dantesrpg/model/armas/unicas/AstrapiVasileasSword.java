package br.com.dantesrpg.model.armas.unicas;

import br.com.dantesrpg.model.ArmaMelee;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.Raridade;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.fantasmasnobres.AstrapiVasileas;
import br.com.dantesrpg.model.util.EffectFactory;

/** Espada única vinculada ao Fantasma Nobre Astrapí Vasiléas. */
public class AstrapiVasileasSword extends ArmaMelee {

	public static final String NOME = "Astrapí Vasiléas Sword";

	public AstrapiVasileasSword() {
		super(NOME, "Espada Longa",
				"Uma espada longa forjada a partir de relâmpagos divinos. Sua lâmina nunca permanece sólida por completo, sendo composta por plasma e eletricidade celestial. Cada golpe deixa descargas elétricas no ambiente, permitindo que Cassandra transforme o campo de batalha em seu domínio.",
				Raridade.UNICO, 0, 25, 1, Atributo.DESTREZA, 100, 2);
		setWielding(2);
		setTipoAlvo(TipoAlvo.INDIVIDUAL);
		setHasSpecialAttack(true);
		setSpecialAttackName("Corte do Relâmpago Celestial");
		setSpecialAttackDmg(1.0);
		setSpecialAttackCd(1.0);
		setSpecialAttackType(TipoAlvo.LINHA.name());
		setSpecialAttackSize(1);
		setSpecialAttackRange(3);
	}

	@Override
	public void onAttackHit(Personagem ator, Personagem alvo, double danoCausado, EstadoCombate estado,
			CombatManager manager) {
		if (ator == null || alvo == null || manager == null || danoCausado <= 0) {
			return;
		}

		double chanceChoque = ator.getFantasmaNobre() instanceof AstrapiVasileas ? 1.0 : 0.25;
		if (Math.random() > chanceChoque) {
			return;
		}

		manager.getEffectProcessor().aplicarEfeito(alvo, EffectFactory.criarEfeito("Choque", 1, 10));
		System.out.println(">>> " + NOME + ": Choque aplicado em " + alvo.getNome()
				+ " (+10 TU; chance " + (int) (chanceChoque * 100) + "%).");
	}
}
