package br.com.dantesrpg.model.armas.unicas;

import br.com.dantesrpg.model.AcaoMestreInput;
import br.com.dantesrpg.model.ArmaMelee;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.Raridade;
import br.com.dantesrpg.model.enums.TipoEfeito;
import java.util.Map;

public class PalidaVigilia extends ArmaMelee {

	public PalidaVigilia() {
		super("Pálida Vigília", "Foice", "Uma foice invocavel composta de um material angelical", Raridade.UNICO, 0, 14,
				1, Atributo.FORCA, 110, 2);
		this.setTipoAlvo(br.com.dantesrpg.model.enums.TipoAlvo.CONE);
		this.setTamanhoArea(2);
	}

	@Override
	public double getBonusDanoArma(Personagem ator, Personagem alvo, EstadoCombate estado, AcaoMestreInput input) {
		int inspiracao = ator.getAtributosFinais().getOrDefault(Atributo.INSPIRACAO, 0);
		double bonusPercentual = inspiracao * 0.05;
		return 1.0 + bonusPercentual;
	}

	@Override
	public boolean isDanoHibrido(Personagem ator) {
		return false; // Ignora redução de armadura física
	}

	@Override
	public int getAnguloCone() {
		return 135;
	}

	@Override
	public void onDamageTaken(Personagem ator, double danoRecebido, EstadoCombate estado,
			br.com.dantesrpg.controller.CombatController controller) {
		// Verifica Cooldown
		if (ator.getEfeitosAtivos().containsKey("CD: Pálida Vigília")) {
			return;
		}

		double limiar = ator.getVidaMaxima() * 0.10;
		if (danoRecebido >= limiar) {
			System.out.println(">>> PÁLIDA VIGÍLIA: Dano massivo detectado (" + danoRecebido + ")!");

			int cura = (int) (danoRecebido * 0.75);
			if (cura < 1)
				cura = 1;
			int raio = 5;

			System.out.println(">>> Ativando Cura em Área de " + cura + " HP!");

			for (Personagem p : estado.getCombatentes()) {
				// Verifica se é aliado (mesma facção) e está vivo
				if (p.isAtivoNoCombate() && p.getFaccao().equals(ator.getFaccao())) {
					// Verifica Distância
					int dist = Math.max(Math.abs(p.getPosX() - ator.getPosX()), Math.abs(p.getPosY() - ator.getPosY()));
					if (dist <= raio) {
						// Cura
						if (p.getVidaAtual() < p.getVidaMaxima()) {
							p.setVidaAtual(p.getVidaAtual() + cura, estado, controller);
							System.out.println(">>> " + p.getNome() + " foi curado pela Vigília.");
						}
					}
				}
			}

			// Aplica Cooldown (220 TU)
			// Usamos um efeito invisível/debuff para controlar o CD
			Efeito cooldown = new Efeito("CD: Pálida Vigília", TipoEfeito.DEBUFF, 200, Map.of(), 0, 0);
			ator.adicionarEfeito(cooldown);
			ator.recalcularAtributosEstatisticas(); // Para atualizar ícone de CD
		}
	}
}