package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class InvocarGolem extends Habilidade {

	public InvocarGolem() {
		super("Invocar Golem", "Cria um constructo. Pode usar Mana ou Materiais.", TipoHabilidade.ATIVA, 2, 150, 1,
				TipoAlvo.AREA, 1, 0, 0, null);
	}

	@Override
	public int getAlcanceMaximo() {
		return 4;
	}

	@Override
	public List<String> getOpcoesSelection() {
		return Arrays.asList("Mana", "Material");
	}

	@Override
	public void executar(Personagem conjurador, int alvoX, int alvoY, List<Personagem> alvos, EstadoCombate estado,
			CombatManager manager) {
		String escolha = manager.getLastInput().getOpcaoEscolhida();
		boolean isMana = "Mana".equals(escolha);

		// Criar os Stats
		double escala = isMana ? 0.5 : 0.6;
		Map<Atributo, Integer> stats = new HashMap<>();
		for (Atributo a : Atributo.values()) {
			int valor = (int) (conjurador.getAtributosFinais().getOrDefault(a, 1) * escala);
			stats.put(a, Math.max(1, valor));
		}

		// Instanciar o NPC
		Personagem golem = new Personagem("Golem (" + conjurador.getNome() + ")",
				new br.com.dantesrpg.model.racas.RaçaPlaceholder(),
				new br.com.dantesrpg.model.classes.ClassePlaceholder(), 1, stats, (conjurador.getVidaMaxima() * escala),
				5);

		golem.setFaccao(conjurador.getFaccao());
		golem.setPosX(alvoX);
		golem.setPosY(alvoY);

		br.com.dantesrpg.model.ArmaMelee espadinha = new br.com.dantesrpg.model.ArmaMelee("Espada", "Espada",
				"blouw baby", Raridade.COMUM, 0, 10, 1, Atributo.FORCA, 80, 1);
		espadinha.setTipoAlvo(TipoAlvo.INDIVIDUAL);
		golem.setArmaEquipada(espadinha);

		// Inserção no Combate (A chave do erro anterior)
		if (manager.getMainController().getMapController() != null) {
			estado.getCombatentes().add(golem);
			golem.setContadorTU(conjurador.getContadorTU() + 100); // Age depois do mestre
			manager.getMainController().atualizarInterfaceTotal();
			System.out.println(">>> Golem invocado com sucesso via " + escolha);
		}
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
	}
}