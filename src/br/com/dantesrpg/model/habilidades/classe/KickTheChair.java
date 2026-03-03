package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class KickTheChair extends Habilidade {
	public KickTheChair() {
		super("Kick The Chair",
				"Explode clones. Causa dano 3x3 e cura aliados. Cura Darrell por clone (Overheal vira Escudo x2).",
				TipoHabilidade.ATIVA, 4, 150, 1, TipoAlvo.SI_MESMO, 0, 0, Collections.emptyList());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		List<Personagem> clones = new ArrayList<>(conjurador.getClonesAtivos()); // Cópia para iterar com segurança

		if (clones.isEmpty()) {
			System.out.println(">>> Kick The Chair falhou: Nenhum clone para explodir.");
			return;
		}

		System.out.println(">>> KICK THE CHAIR: Explodindo " + clones.size() + " clones!");

		int danoBaseExplosao = 10;
		int inspiracao = conjurador.getAtributosFinais().getOrDefault(Atributo.INSPIRACAO, 0);
		double danoFinal = danoBaseExplosao + (inspiracao * 2);

		for (Personagem clone : clones) {
			if (!clone.isAtivoNoCombate())
				continue;

			int cx = clone.getPosX();
			int cy = clone.getPosY();
			System.out.println(">>> Explosão em (" + cx + "," + cy + ")!");

			int raio = 1;
			for (Personagem p : estado.getCombatentes()) {
				if (!p.isAtivoNoCombate() || p == clone)
					continue; // Ignora o próprio clone que está morrendo

				// Verifica distância (quadrada/xadrez)
				int dist = Math.max(Math.abs(p.getPosX() - cx), Math.abs(p.getPosY() - cy));

				if (dist <= raio) {
					// Se for INIMIGO -> Dano
					if (!p.getFaccao().equals(conjurador.getFaccao())) {
						// Aplica dano (100% do valor calculado)
						manager.aplicarDanoAoAlvo(conjurador, p, danoFinal, false, TipoAcao.HABILIDADE, estado);

						// Se for ALIADO -> Cura (20% do dano)
					} else {
						int curaAliado = (int) (danoFinal * 0.20);
						if (curaAliado > 0) {
							p.setVidaAtual(p.getVidaAtual() + curaAliado, estado, manager.getController());
							System.out.println(
									">>> Aliado " + p.getNome() + " curado pela explosão (" + curaAliado + ").");
						}
					}
				}
			}

			// --- Cura/Escudo para Darrell (5% MaxHP por clone) ---
			double curaDarrell = (int) (conjurador.getVidaMaxima() * 0.07);
			double vidaAtual = conjurador.getVidaAtual();
			double vidaMax = conjurador.getVidaMaxima();

			if (vidaAtual + curaDarrell <= vidaMax) {
				// Cura normal
				conjurador.setVidaAtual(vidaAtual + curaDarrell, estado, manager.getController());
			} else {
				// Overheal -> Escudo de Sangue (2x o excedente)
				double curaReal = vidaMax - vidaAtual;
				double excedente = curaDarrell - curaReal;

				conjurador.setVidaAtual(vidaMax, estado, manager.getController()); // Enche a vida

				double escudoSangue = excedente * 3;
				conjurador.setEscudoAtual(conjurador.getEscudoAtual() + escudoSangue);
				conjurador.setTemEscudoDeSangue(true);
				System.out.println(">>> Overheal! Darrell ganhou " + escudoSangue + " de Escudo de Sangue.");
			}

			// --- Mata o Clone ---
			clone.setVidaAtual(0, estado, manager.getController());
			conjurador.removerCloneMorto(clone);
		}

		conjurador.recalcularAtributosEstatisticas();
		br.com.dantesrpg.model.fantasmasnobres.ModoPolaris.reverterParaPolaris(conjurador);
	}
}