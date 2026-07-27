package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.TipoAcao;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import br.com.dantesrpg.model.fantasmasnobres.GrandeRegente;
import java.util.List;

public class MorteViltrumita extends Habilidade {
	public MorteViltrumita() {
		super("Morte Viltrumita", "Executa um inimigo cuja vida atual esteja em percentual igual ou inferior à sua FORÇA.",
				TipoHabilidade.ATIVA, 0, 80, 1, TipoAlvo.INDIVIDUAL, 0, 0, 0, List.of());
	}

	@Override public int getAlcanceMaximo() { return 2; }
	@Override public boolean afetaAliados() { return false; }

	@Override
	public String getMotivoBloqueio(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado) {
		if (alvos == null || alvos.size() != 1 || alvos.get(0) == null) return "Morte Viltrumita exige um alvo.";
		Personagem alvo = alvos.get(0);
		if (!alvo.isAtivoNoCombate() || mesmaFaccao(conjurador, alvo)) return "Morte Viltrumita só pode executar um inimigo ativo.";
		double limite = conjurador.getAtributosFinais().getOrDefault(Atributo.FORCA, 0) / 100.0;
		if (alvo.getVidaAtual() > alvo.getVidaMaxima() * limite) {
			return "O alvo precisa estar com no máximo " + (int) Math.round(limite * 100) + "% de vida.";
		}
		return null;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		Personagem alvo = alvos.get(0);
		manager.aplicarDanoAoAlvo(conjurador, alvo, alvo.getVidaAtual(), true, TipoAcao.HABILIDADE, estado);
		if (!alvo.isVivo()) {
			GrandeRegente.adicionarAcumulos(conjurador, 1);
			System.out.println(">>> " + conjurador.getNome() + " executou " + alvo.getNome() + " com Morte Viltrumita.");
		}
	}

	private boolean mesmaFaccao(Personagem primeiro, Personagem segundo) {
		return primeiro.getFaccao() != null && primeiro.getFaccao().equals(segundo.getFaccao());
	}
}
