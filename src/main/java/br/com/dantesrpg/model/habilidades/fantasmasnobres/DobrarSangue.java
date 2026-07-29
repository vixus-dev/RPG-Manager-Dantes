package br.com.dantesrpg.model.habilidades.fantasmasnobres;

import java.util.List;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;

/** Técnica ofensiva em área concedida pelo BloodMoon. */
public class DobrarSangue extends Habilidade {

	public DobrarSangue() {
		super("Dobrar Sangue",
				"Causa dano em uma área circular 3x3 a até 5 espaços. Cada inimigo que receber dano concede 5% da vida máxima como escudo de sangue.",
				TipoHabilidade.ATIVA, 2, 120, 1, TipoAlvo.AREA_CIRCULAR, 3, 1.0, 1, List.of());
	}

	@Override
	public int getAlcanceMaximo() {
		return 5;
	}

	@Override
	public boolean afetaAliados() {
		return false;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
			CombatManager manager) {
		System.out.println(">>> " + conjurador.getNome() + " dobra o sangue em uma área.");
	}
}
