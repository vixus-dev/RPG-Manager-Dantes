package br.com.dantesrpg.model.fantasmasnobres;

import br.com.dantesrpg.model.AcaoMestreInput;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.FantasmaNobre;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.armas.unicas.AstrapiVasileasSword;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoEfeito;
import br.com.dantesrpg.model.map.CoordenadaMapa;
import br.com.dantesrpg.model.map.Dominio;

import java.util.List;
import java.util.Map;

/** Fantasma Nobre de Cassandra que cria uma cerca elétrica de contenção. */
public class AstrapiVasileas extends FantasmaNobre {

	public static final String ID_DOMINIO = "cerca_eletrica_astrapi";
	public static final String EFEITO = "Cerca Elétrica";

	@Override public String getNome() { return "Astrapí Vasiléas"; }

	@Override
	public String getDescricao() {
		return "Cerca Elétrica: cria um campo de contenção 5x5 por 500 TU. Aliados curam 10% da vida máxima e recuperam 1 MP a cada 100 TU; inimigos recebem Choque de +5 TU e causam 25% menos dano.";
	}

	@Override public int getCustoMana() { return 4; }
	@Override public int getCustoTU() { return 0; }
	@Override public int getCooldownTU() { return 500; }
	@Override public TipoAlvo getTipoAlvo() { return TipoAlvo.AREA_QUADRADA; }
	@Override public int getTamanhoArea() { return 5; }
	@Override public int getNumeroDeAlvos() { return 0; }
	@Override public int getAlcanceMaximo() { return 8; }

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
			AcaoMestreInput input, CombatManager manager) {
		if (input == null || input.getEpicentros().isEmpty()) {
			System.out.println(">>> Cerca Elétrica exige a seleção de uma área no mapa.");
			return;
		}

		CoordenadaMapa epicentro = input.getEpicentros().get(0);
		manager.getEffectProcessor().aplicarEfeito(conjurador,
				new Efeito(EFEITO, TipoEfeito.BUFF, 500, Map.of(), 0, 0));

		if (manager.getMainController() != null) {
			Dominio cerca = new Dominio(ID_DOMINIO, EFEITO, conjurador, epicentro.x(), epicentro.y(),
					getTamanhoArea(), "zona-dominio-cerca-eletrica");
			cerca.configurarTesteDestrezaParaSair(obterDanoFalhaSaida(conjurador));
			manager.getDomainManager().ativarDominioNoMapa(cerca, conjurador, estado);
			manager.atualizarAuras(estado);
		}

		System.out.println(">>> " + conjurador.getNome() + " ergueu a Cerca Elétrica em ("
				+ epicentro.x() + "," + epicentro.y() + ").");
	}

	private double obterDanoFalhaSaida(Personagem conjurador) {
		if (conjurador.getArmaEquipada() instanceof AstrapiVasileasSword espada) {
			return espada.getDanoBase() * 0.5;
		}
		return 12.5;
	}
}
