package br.com.dantesrpg.model.habilidades.boss;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import br.com.dantesrpg.model.racas.RaçaPlaceholder;
import br.com.dantesrpg.model.classes.ClassePlaceholder;
import java.util.*;

/**
 * Invoca um "Tentáculo de Sangue" imóvel na tile alvo.
 * - HP: 100
 * - Mana: 4
 * - Arma: tentaculoSangue (concede Arremesso e Enraizar)
 */
public class Coagular extends Habilidade {

	public Coagular() {
		super("Coagular",
				"Materializa um tentáculo de sangue coagulado na tile alvo. Imóvel, ergue-se para atacar em linha.",
				TipoHabilidade.ATIVA, 2, 110, 1,
				TipoAlvo.AREA, 1, 0, 1, Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 6;
	}

	@Override
	public void executar(Personagem conjurador, int alvoX, int alvoY, List<Personagem> alvos, EstadoCombate estado,
			CombatManager manager) {

		System.out.println(">>> " + conjurador.getNome() + " coagula sangue no solo em (" + alvoX + "," + alvoY + ")...");

		// Atributos base do tentáculo (TOPOR 4 concedido pela arma)
		Map<Atributo, Integer> stats = new HashMap<>();
		for (Atributo a : Atributo.values())
			stats.put(a, 1);

		Personagem tentaculo = new Personagem("Tentáculo de Sangue", new RaçaPlaceholder(), new ClassePlaceholder(), 1,
				stats, 100, 1);

		tentaculo.setFaccao(conjurador.getFaccao());
		tentaculo.setMestreInvocador(conjurador);
		tentaculo.setPosX(alvoX);
		tentaculo.setPosY(alvoY);
		tentaculo.setManaMaxima(4);
		tentaculo.setManaAtual(4);
		tentaculo.setPesoEntidade(PesoEntidade.IMOVEL);
		tentaculo.setGrau(1);

		// Arma tentaculoSangue: AOE Linha, 10 dano, TOPOR, 100TU, alcance 4
		br.com.dantesrpg.model.ArmaMelee tentaculoSangue = new br.com.dantesrpg.model.ArmaMelee("tentaculoSangue",
				"Natural", "Tentáculo de sangue coagulado que se ergue do chão.", Raridade.LENDARIO, 0, 10, 1,
				Atributo.TOPOR, 100, 4);
		tentaculoSangue.setTipoAlvo(TipoAlvo.LINHA);
		tentaculoSangue.setTamanhoArea(4);
		tentaculoSangue.addHabilidadeConcedida("Arremesso");
		tentaculoSangue.addHabilidadeConcedida("Enraizar");

		Map<Atributo, Integer> modsArma = new HashMap<>();
		modsArma.put(Atributo.TOPOR, 4);
		tentaculoSangue.setModificadoresDeAtributo(modsArma);

		tentaculo.setArmaEquipada(tentaculoSangue);
		tentaculo.recalcularAtributosEstatisticas();

		// Registra no combate
		if (manager.getMainController() != null) {
			estado.getCombatentes().add(tentaculo);
			tentaculo.setContadorTU(conjurador.getContadorTU() + 50);
			manager.getMainController().atualizarInterfaceTotal();
			System.out.println(">>> Tentáculo de Sangue surgiu em (" + alvoX + "," + alvoY + ")!");
		}
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		// Fallback: se não for chamado com alvoX/alvoY, spawna ao lado do conjurador
		executar(conjurador, conjurador.getPosX() + 1, conjurador.getPosY(), alvos, estado, manager);
	}
}
