package br.com.dantesrpg.model.fantasmasnobres;

import br.com.dantesrpg.model.AcaoMestreInput;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.FantasmaNobre;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoEfeito;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class VigiliaEterna extends FantasmaNobre {

	@Override
	public String getNome() {
		return "Vigília Eterna";
	}

	@Override
	public String getDescricao() {
		return "Concede +50 Armadura e 25% de dano para aliados. para si mesmo +25% de taxa critica e 25% de bonus de dano e Imunidade a DoT para todos os aliados. Eidan ganha Crítico.";
	}

	@Override
	public int getCustoMana() {
		return 2;
	}

	@Override
	public int getCustoTU() {
		return 50;
	}

	@Override
	public int getCooldownTU() {
		return 600;
	}

	@Override
	public TipoAlvo getTipoAlvo() {
		return TipoAlvo.SI_MESMO;
	} // Global

	@Override
	public int getTamanhoArea() {
		return 0;
	}

	@Override
	public int getNumeroDeAlvos() {
		return 0;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, AcaoMestreInput input,
			CombatManager manager) {
		System.out.println(">>> " + conjurador.getNome() + " ativa VIGÍLIA ETERNA!");

		int duracao = 450;

		// Buff Global (Armadura + Imunidade)
		Map<String, Double> modsAliado = new HashMap<>();
		modsAliado.put("ARMADURA_TOTAL", 50.0);
		modsAliado.put("DANO_BONUS_PERCENTUAL", 0.25);
		modsAliado.put("TAXA_CRITICA", 0.10);

		@SuppressWarnings("unused")
		Efeito buffGlobal = new Efeito("Bênção da Vigília", TipoEfeito.BUFF, duracao, modsAliado, 0, 0);

		// Aplica em todos os aliados
		for (Personagem p : estado.getCombatentes()) {
			if (p.isAtivoNoCombate() && p.getFaccao().equals(conjurador.getFaccao())) {
				p.adicionarEfeito(new Efeito("Bênção da Vigília", TipoEfeito.BUFF, duracao, modsAliado, 0, 0));
				p.setVidaAtual(p.getVidaAtual() + (p.getAtributosFinais().getOrDefault(Atributo.INSPIRACAO, 0) * 5));
				p.recalcularAtributosEstatisticas();
				System.out.println(">>> " + p.getNome() + " recebeu a Bênção.");
			}
		}

		// Buff Pessoal do Eidan (+20% Crit)
		Map<String, Double> modsEidan = new HashMap<>();
		modsEidan.put("TAXA_CRITICA", 0.25);
		modsEidan.put("DANO_BONUS_PERCENTUAL", 0.25);
		Efeito buffEidan = new Efeito("Vigília", TipoEfeito.BUFF, duracao, modsEidan, 0, 0);

		conjurador.adicionarEfeito(buffEidan);
		conjurador.recalcularAtributosEstatisticas();
	}
}