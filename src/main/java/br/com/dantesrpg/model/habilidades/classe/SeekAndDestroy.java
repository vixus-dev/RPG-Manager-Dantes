package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class SeekAndDestroy extends Habilidade {
	public static final String INPUT_TESTE_BUFF = "DADO_FIGHT_TILL_DEATH";

	@Override
	public String getDescricao() {
		return "Aumenta o dano de todos os aliados em 2% por resultado do teste.";
	}

	public SeekAndDestroy() {
		super("Fight Till Death", "Aumenta o dano de todos os aliados em 30%.", TipoHabilidade.ATIVA, 5, 100, 1,
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

		int resultadoTeste = obterResultadoDado(manager, INPUT_TESTE_BUFF);
		Map<String, Double> mods = new HashMap<>();
		mods.put("DANO_BONUS_PERCENTUAL", resultadoTeste * 0.02);
		int duracao = 175;

		for (Personagem p : alvos) {
			Efeito buff = new Efeito("Seek and Destroy", TipoEfeito.BUFF, duracao, mods, 0, 0);
			p.adicionarEfeito(buff);
			p.recalcularAtributosEstatisticas();
			System.out.println(">>> " + p.getNome() + " recebeu bônus de dano.");
		}
	}

	private int obterResultadoDado(CombatManager manager, String chaveDado) {
		if (manager == null || manager.getLastInput() == null) {
			return 0;
		}
		return Math.max(0, manager.getLastInput().getResultadoDado(chaveDado));
	}
}
