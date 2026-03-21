package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class TrocaIlusoria extends Habilidade {
	public TrocaIlusoria() {
		super("Troca Ilusória", "Sacrifica um clone para curar e entrar em Stealth.", TipoHabilidade.ATIVA, 1, 80, 3,
				TipoAlvo.SI_MESMO, 0, 0, Collections.emptyList());
	}

	@Override
	public int getCustoMana() {
		return 1;
	}

	@Override
	public int getAlcanceMaximo() {
		return 99;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		List<Personagem> clones = conjurador.getClonesAtivos();

		if (clones.isEmpty()) {
			System.out.println(">>> Falha: Nenhum clone para trocar!");
			return;
		}

		// Sacrifica o primeiro clone da lista (o mais antigo)
		Personagem sacrificio = clones.get(0);
		System.out.println(">>> Trocando de lugar com " + sacrificio.getNome() + "...");

		// Mata o clone (Dano massivo direto ou setVida 0)
		sacrificio.setVidaAtual(0, estado, manager.getController());
		manager.processarMorteClone(sacrificio, estado);

		// Cura (10% HP Max + 1% por ponto de PE)
		int percepcao = (int) ((conjurador.getVidaMaxima() * 0.01)
				* (conjurador.getAtributosFinais().get(Atributo.PERCEPCAO)));
		int curaBase = (int) (conjurador.getVidaMaxima() * 0.10);
		int curaTotal = curaBase + percepcao;

		conjurador.setVidaAtual(conjurador.getVidaAtual() + curaTotal, estado, manager.getController());
		System.out.println(">>> " + conjurador.getNome() + "curou " + curaTotal + " HP e entrou em Stealth.");

		conjurador.recalcularAtributosEstatisticas();
	}
}
