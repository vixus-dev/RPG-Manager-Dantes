package br.com.dantesrpg.model.habilidades.boss;

import java.util.Collections;
import java.util.List;

import br.com.dantesrpg.controller.MapController;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import br.com.dantesrpg.model.map.TerrainData.EfeitoInstance;
import br.com.dantesrpg.model.map.TerrainData.TipoEfeitoSolo;

/** Cria uma nuvem de gás venenoso 5x5 ao redor do GutterTank. */
public class GasLeak extends Habilidade {

	private static final int TAMANHO_AREA = 6;
	private static final int DURACAO_GAS_TU = 250;
	private static final int DANO_VENENO = 3;

	public GasLeak() {
		super("GasLeak", "Libera gás venenoso em uma área 6x6 ao redor do usuário.",
				TipoHabilidade.ATIVA, 2, 125, 1, TipoAlvo.SI_MESMO, TAMANHO_AREA, 0, 0,
				Collections.emptyList());
	}

	@Override
	public boolean afetaInimigos() {
		return false;
	}

	@Override
	public boolean afetaAliados() {
		return false;
	}

	@Override
	public boolean afetaSiMesmo() {
		return true;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
			CombatManager manager) {
		if (conjurador == null || manager == null || manager.getMainController() == null) {
			return;
		}

		MapController mapa = manager.getMainController().getMapController();
		if (mapa == null) {
			return;
		}

		int deslocamentoMinimo = (TAMANHO_AREA / 2) - 1;
		int deslocamentoMaximo = TAMANHO_AREA / 2;
		for (int y = conjurador.getPosY() - deslocamentoMinimo;
				y <= conjurador.getPosY() + deslocamentoMaximo; y++) {
			for (int x = conjurador.getPosX() - deslocamentoMinimo;
					x <= conjurador.getPosX() + deslocamentoMaximo; x++) {
				if (mapa.getTerreno(x, y) == br.com.dantesrpg.model.map.TerrainData.TipoTerreno.PAREDE) {
					continue;
				}
				mapa.aplicarEfeitoNoSolo(x, y,
						new EfeitoInstance(TipoEfeitoSolo.GAS, DURACAO_GAS_TU, DANO_VENENO, conjurador));
			}
		}
		System.out.println(">>> " + conjurador.getNome() + " liberou gás venenoso em uma área 6x6.");
	}
}
