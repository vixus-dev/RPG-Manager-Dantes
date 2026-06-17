package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.controller.CombatController;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import br.com.dantesrpg.model.items.Consumivel;
import br.com.dantesrpg.model.Item;

import java.util.Collections;
import java.util.List;

public class ArremessarPocao extends Habilidade {

	public ArremessarPocao() {
		super("Arremessar Poção", 
			  "Selecione uma poção do seu inventário e arremesse-a em uma área de 3x3, aplicando seu efeito a todos os aliados nessa área.", 
			  TipoHabilidade.ATIVA, 
			  2, // Custo Mana
			  100, // Custo TU
			  5, // Nível Necessário
			  TipoAlvo.AREA_QUADRADA, 
			  3, // Tamanho Área (3x3)
			  0.0, // Multiplicador de Dano
			  0, // Ticks de Dano
			  Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 4;
	}

	@Override
	public boolean afetaAliados() {
		return true;
	}

	@Override
	public boolean afetaSiMesmo() {
		return true;
	}

	@Override
	public boolean afetaInimigos() {
		return false;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		// Padrão não faz nada, pois a escolha e coordenadas são tratadas em executar com alvoX e alvoY
	}

	@Override
	public void executar(Personagem conjurador, int alvoX, int alvoY, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		if (conjurador == null || manager == null || manager.getLastInput() == null) {
			return;
		}

		String selectedId = manager.getLastInput().getOpcaoEscolhida();
		if (selectedId == null || selectedId.isEmpty()) {
			System.out.println(">>> Arremessar Poção falhou: nenhuma poção selecionada.");
			return;
		}

		if (!conjurador.getInventario().possuiItem(selectedId)) {
			System.out.println(">>> Arremessar Poção falhou: poção selecionada não está no inventário.");
			return;
		}

		// Remove 1 unidade do item do inventário do conjurador
		conjurador.getInventario().removerItemPorTipo(selectedId);

		CombatController controller = manager.getMainController();
		if (controller == null) {
			return;
		}

		Item item = controller.getItem(selectedId);
		if (!(item instanceof Consumivel)) {
			System.out.println(">>> Arremessar Poção falhou: item não é do tipo Consumivel.");
			return;
		}

		Consumivel potion = (Consumivel) item;
		System.out.println(">>> " + conjurador.getNome() + " arremessou " + potion.getNome() + " em (" + alvoX + "," + alvoY + ")!");

		// Aplica a poção a todos os alvos válidos na área
		for (Personagem target : alvos) {
			if (target != null && target.isAtivoNoCombate()) {
				potion.usar(target, estado, controller);
				target.recalcularAtributosEstatisticas();
			}
		}

		controller.atualizarInterfaceTotal();
	}
}
