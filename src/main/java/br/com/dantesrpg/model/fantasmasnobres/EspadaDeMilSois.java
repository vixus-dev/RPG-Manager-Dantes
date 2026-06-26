package br.com.dantesrpg.model.fantasmasnobres;

import br.com.dantesrpg.model.AcaoMestreInput;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.FantasmaNobre;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.combat.DamageApplicator;
import br.com.dantesrpg.model.combat.DamageCalculator;
import br.com.dantesrpg.model.enums.ModoAtaque;
import br.com.dantesrpg.model.enums.TipoAlvo;

import java.util.List;

public class EspadaDeMilSois extends FantasmaNobre {

	@Override
	public String getNome() {
		return "Espada de Mil Sóis";
	}

	@Override
	public String getDescricao() {
		return "Ganha 100 de escudo de sangue e atira um golpe com multiplicador de 10x dano em área esférica.";
	}

	@Override
	public int getCustoMana() {
		return 0;
	}

	@Override
	public int getCustoTU() {
		return 50;
	}

	@Override
	public int getCooldownTU() {
		return 0;
	}

	@Override
	public TipoAlvo getTipoAlvo() {
		return TipoAlvo.AREA_CIRCULAR;
	}

	@Override
	public int getTamanhoArea() {
		return 5;
	}

	@Override
	public int getNumeroDeAlvos() {
		return 0;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, AcaoMestreInput input,
			CombatManager manager) {

		// Ganha 100 de escudo de sangue
		conjurador.adicionarEscudoSangue(1000.0);

		// Atira um golpe com multiplicador de 10x o dano em um inimigo (AOE)
		if (alvos != null && !alvos.isEmpty()) {
			int rolagem = 0;
			manager.getDamageCalculator().resolverDanoPadrao(conjurador, conjurador.getArmaEquipada(), rolagem, alvos, 30.0, br.com.dantesrpg.model.enums.TipoAcao.FANTASMA_NOBRE, null, estado, input);
		}
	}
}
