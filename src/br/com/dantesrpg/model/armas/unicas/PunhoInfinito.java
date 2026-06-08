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

	private boolean isRingueAtivoEDentro(Personagem ator, EstadoCombate estado) {
		if (!ator.getEfeitosAtivos().containsKey("Ringue da Vontade"))
			return false;
		return estado.isPersonagemNoDominio(ator, "ringue_alexei");
	}

	// Passiva 1: Ignorar Defesa — 5% base, +5% dentro do Ringue
	@Override
	public double getIgnorarDefesaPercentual(Personagem ator, Personagem alvo, EstadoCombate estado) {
		double ignoreBase = ator.getAtributosFinais().getOrDefault(Atributo.FORCA, 0) * 0.05;
		if (isRingueAtivoEDentro(ator, estado)) {
			ignoreBase += ator.getAtributosFinais().getOrDefault(Atributo.FORCA, 0) * 0.05;
		}
		return ignoreBase;
	}

	// Passiva 2: Dano escala com HP perdido (só dentro do Ringue)
	@Override
	public double getBonusDanoArma(Personagem ator, Personagem alvo, EstadoCombate estado, AcaoMestreInput input) {
		if (isRingueAtivoEDentro(ator, estado)) {
			double vidaAtual = ator.getVidaAtual();
			double vidaMax = ator.getVidaMaxima();
			double porcentagemVidaPerdida = (vidaMax - vidaAtual) / vidaMax;

			if (porcentagemVidaPerdida > 0) {
				System.out.println(
						">>> Punho Infinito: +" + String.format("%.1f", porcentagemVidaPerdida * 100) + "% Dano (por HP perdido).");
				return 1.0 + porcentagemVidaPerdida;
			}
		}
		return 1.0;
	}

	// Passiva 3: Aumentar TU do Alvo (só dentro do Ringue, abaixo de 50% HP)
	@Override
	public void onAttackHit(Personagem ator, Personagem alvo, double danoCausado, EstadoCombate estado) {
		if (isRingueAtivoEDentro(ator, estado)) {
			double vidaAtual = ator.getVidaAtual();
			double vidaMax = ator.getVidaMaxima();

			if (vidaAtual <= (vidaMax * 0.50)) {
				int forca = ator.getAtributosFinais().getOrDefault(Atributo.FORCA, 0);
				alvo.setContadorTU(alvo.getContadorTU() + forca);
				System.out.println(
						">>> Punho Infinito (Berserk): Aumentou o TU de " + alvo.getNome() + " em +" + forca + "!");
			}
		}
	}
}
