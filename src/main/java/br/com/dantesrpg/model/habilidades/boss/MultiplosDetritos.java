package br.com.dantesrpg.model.habilidades.boss;

import br.com.dantesrpg.model.AcaoMestreInput;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import java.util.Collections;
import java.util.List;

/** Arremessa três blocos de escombros em áreas quadradas independentes. */
public class MultiplosDetritos extends Habilidade {
	public static final String OPCAO_SEM_ESCOMBROS = "Sem escombros";
	public static final String OPCAO_CRIAR_ESCOMBROS = "Criar escombros";

	public MultiplosDetritos() {
		super("Multiplos Detritos",
				"Arremessa prédios em três áreas quadradas 3x3. Escolha se os impactos deixam escombros bloqueando o caminho.",
				TipoHabilidade.ATIVA, 1, 120, 1, TipoAlvo.MULTI_AOE, 3, 1.5, 1, Collections.emptyList());
	}

	@Override
	public TipoAlvo getSubtipoArea() {
		return TipoAlvo.AREA_QUADRADA;
	}

	@Override
	public int getNumeroDeAreas() {
		return 3;
	}

	@Override
	public int getAlcanceMaximo() {
		return 7;
	}

	@Override
	public boolean afetaAliados() {
		return false;
	}

	@Override
	public List<String> getOpcoesSelection() {
		return List.of(OPCAO_SEM_ESCOMBROS, OPCAO_CRIAR_ESCOMBROS);
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		// A sobrecarga com as áreas estruturadas é usada pelo pipeline.
	}

	@Override
	public void executar(Personagem conjurador, List<AcaoMestreInput.AreaSelecionada> areas,
			List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		if (manager == null || manager.getLastInput() == null
				|| !OPCAO_CRIAR_ESCOMBROS.equals(manager.getLastInput().getOpcaoEscolhida())
				|| areas == null || areas.isEmpty() || manager.getMainController() == null) {
			return;
		}

		for (AcaoMestreInput.AreaSelecionada area : areas) {
			manager.getMainController().forEachMap(mapa -> mapa.criarEscombros(
					area.epicentro().x(), area.epicentro().y(), getTamanhoArea()));
		}
		System.out.println(">>> " + conjurador.getNome() + " derrubou escombros nos " + areas.size() + " impactos.");
	}
}
