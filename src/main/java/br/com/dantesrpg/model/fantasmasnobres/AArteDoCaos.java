package br.com.dantesrpg.model.fantasmasnobres;

import java.util.List;
import br.com.dantesrpg.model.AcaoMestreInput;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.FantasmaNobre;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;

/** Manipula até cinco tiles do andar, criando ou removendo paredes. */
public class AArteDoCaos extends FantasmaNobre {
	@Override public String getNome() { return "A Arte do Caos"; }

	@Override public String getDescricao() {
		return "Escolha Criar ou Remover paredes e selecione exatamente 5 tiles do andar.";
	}

	@Override public int getCustoMana() { return 2; }
	@Override public int getCustoTU() { return 80; }
	@Override public int getCooldownTU() { return 0; }
	@Override public TipoAlvo getTipoAlvo() { return TipoAlvo.MULTI_AOE; }
	@Override public TipoAlvo getSubtipoArea() { return TipoAlvo.AREA_QUADRADA; }
	@Override public int getTamanhoArea() { return 1; }
	@Override public int getNumeroDeAlvos() { return 0; }
	@Override public int getNumeroDeAreas() { return 5; }
	@Override public List<String> getOpcoesSelection() { return List.of("Criar paredes", "Remover paredes"); }

	/** A seleção precisa alcançar tanto chão quanto paredes existentes. */
	public boolean ignoraParedes() { return true; }

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
			AcaoMestreInput input, CombatManager manager) {
		if (manager == null || manager.getMainController() == null || input == null) return;
		boolean criar = !"Remover paredes".equals(input.getOpcaoEscolhida());
		int aplicados = 0;
		for (AcaoMestreInput.AreaSelecionada area : input.getAreasSelecionadas()) {
			if (area == null || aplicados >= getNumeroDeAreas()) continue;
			int x = area.epicentro().x();
			int y = area.epicentro().y();
			manager.getMainController().forEachMap(map -> map.aplicarParedeDaArteDoCaos(x, y, criar));
			aplicados++;
		}
		System.out.println(">>> A Arte do Caos: " + (criar ? "criadas" : "removidas")
				+ " " + aplicados + " paredes.");
	}
}
