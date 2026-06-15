package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class SeekAndDestroy extends Habilidade {
	public SeekAndDestroy() {
		super("Seek and Destroy", "Aumenta o dano de todos os aliados em 30%.", TipoHabilidade.ATIVA, 5, 100, 1,
				TipoAlvo.EQUIPE, 4, 0, 0, Collections.emptyList());
	}

	@Override
	public boolean afetaInimigos() {
		return false;
	}

	@Override
	public boolean afetaAliados() {
		return true;
	}

	@Override
	public boolean afetaSiMesmo() {
		return true;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(">>> SEEK AND DESTROY!");

		Map<String, Double> mods = new HashMap<>();
		mods.put("DANO_BONUS_PERCENTUAL", 0.30);
		int duracao = 175;

		for (Personagem p : alvos) {
			Efeito buff = new Efeito("Seek and Destroy", TipoEfeito.BUFF, duracao, mods, 0, 0);
			p.adicionarEfeito(buff);
			p.recalcularAtributosEstatisticas();
			System.out.println(">>> " + p.getNome() + " recebeu bônus de dano.");
		}
	}
}