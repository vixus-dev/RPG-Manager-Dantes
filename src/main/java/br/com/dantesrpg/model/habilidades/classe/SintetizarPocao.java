package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import br.com.dantesrpg.model.items.PocaoAlquimica;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SintetizarPocao extends Habilidade {

	public SintetizarPocao() {
		super("Sintetizar Poção", "Cria uma poção no seu inventário com o efeito selecionado.", TipoHabilidade.ATIVA, 2, 120, 1, TipoAlvo.SI_MESMO, 0, 0, 0,
				Collections.emptyList());
	}

	@Override
	public List<String> getOpcoesSelection() {
		return Arrays.asList("Cura", "Força", "Velocidade", "Resistencia", "Proteção");
	}

	public void executarComEscolha(Personagem conjurador, String escolha) {
		if (conjurador == null)
			return;

		// Pega a inspiração final de quem criou
		int is = conjurador.getAtributosFinais().getOrDefault(br.com.dantesrpg.model.enums.Atributo.INSPIRACAO, 0);

		// Cria a poção correspondente
		String tipoId = "PocaoAlquimica_" + escolha + "_" + is;
		PocaoAlquimica pocao = new PocaoAlquimica(tipoId, escolha, is);

		conjurador.getInventario().adicionarItem(pocao);
		System.out.println(">>> " + conjurador.getNome() + " sintetizou " + pocao.getNome() + " e a adicionou ao seu inventário.");
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		// Padrão não faz nada, pois a escolha é tratada no CombatManager
	}
}
