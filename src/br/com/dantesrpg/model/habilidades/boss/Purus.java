package br.com.dantesrpg.model.habilidades.boss;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import br.com.dantesrpg.model.map.TerrainData;
import java.util.*;

/**
 * Purus — AOE Quadrado 3x3, cria tiles de ácido PERMANENTES na localização.
 * Custo: 3 mana, 100 TU.
 */
public class Purus extends Habilidade {

	public Purus() {
		super("Purus", "Invoca uma poça de ácido permanente em área 3x3.",
				TipoHabilidade.ATIVA, 3, 100, 1,
				TipoAlvo.AREA_QUADRADA, 3, 0, 1, Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 6;
	}

	@Override
	public boolean afetaAliados() {
		return false;
	}

	@Override
	public void executar(Personagem conjurador, int alvoX, int alvoY, List<Personagem> alvos,
			EstadoCombate estado, CombatManager manager) {

		System.out.println(">>> " + conjurador.getNome() + " usa PURUS em (" + alvoX + "," + alvoY + ")!");

		// Cria tiles de ácido permanentes na área 3x3
		br.com.dantesrpg.controller.CombatController main = manager.getMainController();

		if (main != null && main.getMapController() != null) {
			Arma arma = conjurador.getArmaEquipada();
			int danoAcido = (arma != null) ? (int) Math.max(1, arma.getDanoBase() * 0.3) : 5;

			int raio = (getTamanhoArea() - 1) / 2; // 1 para 3x3

			for (int y = alvoY - raio; y <= alvoY + raio; y++) {
				for (int x = alvoX - raio; x <= alvoX + raio; x++) {
					TerrainData.EfeitoInstance efeitoAcido = new TerrainData.EfeitoInstance(
							TerrainData.TipoEfeitoSolo.ACIDO, 9999, danoAcido, conjurador);
					efeitoAcido.setPermanente(true);
					main.getMapController().aplicarEfeitoNoSolo(x, y, efeitoAcido);
				}
			}
		}
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
			CombatManager manager) {
		System.err.println("ERRO: Purus foi chamado sem coordenadas!");
	}
}
