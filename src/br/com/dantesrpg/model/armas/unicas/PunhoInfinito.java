package br.com.dantesrpg.model.armas.unicas;

import br.com.dantesrpg.model.AcaoMestreInput;
import br.com.dantesrpg.model.ArmaMelee;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.Raridade;

public class PunhoInfinito extends ArmaMelee {

	public PunhoInfinito() {
		super("Punho Infinito", "Manoplas", "manoplas espectrais que cobrem os punhos de Alexei", Raridade.UNICO, 0, 13,
				1, Atributo.FORCA, 85, 1);
	}

	// Passiva 1: Ignorar Defesa (Já existia, mantida)
	@Override
	public double getIgnorarDefesaPercentual(Personagem ator, Personagem alvo, EstadoCombate estado) {
		double ignoreBase = ator.getAtributosFinais().getOrDefault(Atributo.FORCA, 0) * 0.05;
		if (ator.getEfeitosAtivos().containsKey("Ringue da Vontade")) {
			ignoreBase += ator.getAtributosFinais().getOrDefault(Atributo.FORCA, 0) * 0.05;
		}
		return ignoreBase;
	}

	@Override
	public double getBonusDanoArma(Personagem ator, Personagem alvo, EstadoCombate estado, AcaoMestreInput input) {
		// Só aplica se o Ringue estiver ativo
		if (ator.getEfeitosAtivos().containsKey("Ringue da Vontade")) {

			double vidaAtual = ator.getVidaAtual();
			double vidaMax = ator.getVidaMaxima();
			double porcentagemVidaPerdida = (vidaMax - vidaAtual) / vidaMax; // Ex: 0.40 (40% perdida)

			// Se perdeu 40%, ganha 40% de dano
			double bonusDano = (porcentagemVidaPerdida * 100) * 0.01;

			if (bonusDano > 0) {
				System.out.println(
						">>> Punho Infinito: +" + String.format("%.1f", bonusDano * 100) + "% Dano (por HP perdido).");
				return 1.0 + bonusDano;
			}
		}
		return 1.0;
	}

	// Passiva 3: Aumentar TU do Alvo
	@Override
	public void onAttackHit(Personagem ator, Personagem alvo, double danoCausado, EstadoCombate estado) {
		// Só aplica se o Ringue estiver ativo
		if (ator.getEfeitosAtivos().containsKey("Ringue da Vontade")) {

			double vidaAtual = ator.getVidaAtual();
			double vidaMax = ator.getVidaMaxima();

			// Regra: "ao chegar em 50% de HP perdido..."
			if (vidaAtual <= (vidaMax * 0.50)) {
				int forca = ator.getAtributosFinais().getOrDefault(Atributo.FORCA, 0);

				// "...aumentam o TU do alvo em 1 para cada ponto em ST"
				int aumentoTU = forca * 1;

				alvo.setContadorTU(alvo.getContadorTU() + aumentoTU);
				System.out.println(
						">>> Punho Infinito (Berserk): Aumentou o TU de " + alvo.getNome() + " em +" + aumentoTU + "!");
			}
		}
	}
}