package br.com.dantesrpg.model.habilidades.classe;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.Raridade;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;

public class InvocarGuardiao extends Habilidade {
	public InvocarGuardiao() {
		super("Invocar Guardião", "Invoca um protetor com 100% dos seus atributos.", TipoHabilidade.ATIVA, 3, 200, 5,
				TipoAlvo.AREA, 1, 0, 0, null);
	}

	@Override
	public int getAlcanceMaximo() {
		return 8;
	}

	@Override
	public void executar(Personagem conjurador, int alvoX, int alvoY, List<Personagem> alvos, EstadoCombate estado,
			CombatManager manager) {
		Map<Atributo, Integer> stats = new HashMap<>(conjurador.getAtributosFinais());

		Personagem guardiao = new Personagem("Guardião de " + conjurador.getNome(),
				new br.com.dantesrpg.model.racas.RaçaPlaceholder(),
				new br.com.dantesrpg.model.classes.ClassePlaceholder(), 1, stats, conjurador.getVidaMaxima(),
				conjurador.getPlacarIniciativa());

		guardiao.setFaccao(conjurador.getFaccao());
		guardiao.setPosX(alvoX);
		guardiao.setPosY(alvoY);

		br.com.dantesrpg.model.ArmaMelee espadinha = new br.com.dantesrpg.model.ArmaMelee("Espada Do Guardião",
				"Espada", "Espada Fodator 1.0", Raridade.EPICO, 0, 10, 1, Atributo.FORCA, 115, 2);
		espadinha.setTipoAlvo(TipoAlvo.CONE);
		espadinha.setTamanhoArea(90);
		espadinha.addHabilidadeConcedida("Auto-Guarda");
		guardiao.setArmaEquipada(espadinha);

		if (manager.getMainController().getMapController() != null) {
			estado.getCombatentes().add(guardiao);
			guardiao.setContadorTU(conjurador.getContadorTU() + 20);
			manager.getMainController().atualizarInterfaceTotal();
			System.out.println(">>> O Guardião se manifestou!");
		}
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
	}
}
