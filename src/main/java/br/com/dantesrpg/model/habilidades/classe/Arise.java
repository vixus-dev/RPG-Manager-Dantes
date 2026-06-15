package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class Arise extends Habilidade {
	public Arise() {
		super("ARISE!", "Fortalece todas as sombras e a si mesmo.", TipoHabilidade.ATIVA, 5, 100, 8, TipoAlvo.SI_MESMO,
				0, 0, Collections.emptyList());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(">>> " + conjurador.getNome() + " comanda: ARISE!");

		List<Personagem> clones = conjurador.getClonesAtivos();
		int percepção = conjurador.getAtributosFinais().getOrDefault(Atributo.PERCEPCAO, 0);
		int bonusVidaClone = 4 * percepção;

		// Buff nos Clones (+Vida, +50% Dano)
		Map<String, Double> modsClone = new HashMap<>();
		modsClone.put("DANO_BONUS_PERCENTUAL", 0.50);

		for (Personagem clone : clones) {
			if (clone.isAtivoNoCombate()) {
				// Aumenta a vida máxima e cura
				clone.setVidaMaxima(clone.getVidaMaxima() + bonusVidaClone);
				clone.setVidaAtual(clone.getVidaAtual() + bonusVidaClone, estado, manager.getController());

				// Aplica buff de dano
				Efeito buffSombra = new Efeito("Poder das Sombras", TipoEfeito.BUFF, 300, modsClone, 0, 0);
				clone.adicionarEfeito(buffSombra);
				clone.recalcularAtributosEstatisticas();
				System.out.println(">>> Sombra fortalecida (+" + bonusVidaClone + " HP, +50% Dano).");
			}
		}

		// Buff no Jogador (+25% dano por sombra)
		int qtdSombras = clones.size();
		double bonusDanoJogador = qtdSombras * 0.25;

		Map<String, Double> modsJogador = new HashMap<>();
		modsJogador.put("DANO_BONUS_PERCENTUAL", bonusDanoJogador);

		Efeito buffJogador = new Efeito("Monarca das Sombras", TipoEfeito.BUFF, 300, modsJogador, 0, 0);
		conjurador.adicionarEfeito(buffJogador);

		System.out.println(">>> " + conjurador.getNome() + " ganha +" + (int) (bonusDanoJogador * 100)
				+ "% de dano por " + qtdSombras + " sombras.");

		conjurador.recalcularAtributosEstatisticas();
	}
}