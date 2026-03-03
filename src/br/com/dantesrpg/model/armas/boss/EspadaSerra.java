package br.com.dantesrpg.model.armas.boss;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import br.com.dantesrpg.model.habilidades.DoubleDown;
import java.util.*;

public class EspadaSerra extends br.com.dantesrpg.model.ArmaMelee {

	private boolean modoEngagedAtivo = false;

	public EspadaSerra() {
		super("Espada-Serra", "Arma Única", "Descrição", Raridade.LENDARIO, 0, 10, // Dano Base
				1, Atributo.FORCA, 100, // Custo TU
				2 // Alcance
		);

		this.addHabilidadeConcedida("Investida Serra-Espada");
		this.addHabilidadeConcedida("Arremesso Serra-Espada");
		this.addHabilidadeConcedida("Rodar Serra-Espada");
		this.addHabilidadeConcedida("DoubleDown");
	}

	@Override
	public void onDamageTaken(Personagem portador, double danoSofrido, EstadoCombate estado,
			br.com.dantesrpg.controller.CombatController controller) {
		if (modoEngagedAtivo)
			return;

		// Gatilho: 50% da Vida
		if (portador.getVidaAtual() <= (portador.getVidaMaxima() * 0.5)) {
			ativarModoEngaged(portador, controller);
		}
	}

	private void ativarModoEngaged(Personagem p, br.com.dantesrpg.controller.CombatController controller) {
		this.modoEngagedAtivo = true;

		System.out.println(">>> SWORDSMACHINE: MODO ENGAGED ATIVADO! <<<");
		br.com.dantesrpg.model.util.SessionLogger.log("⚠️ " + p.getNome() + " entra em fúria mecânica!");

		Map<String, Double> mods = new HashMap<>();
		mods.put("ARMADURA_TOTAL", 20.0);

		Efeito engagedBuff = new Efeito("Modo Engaged", TipoEfeito.BUFF, 9999, mods, 0, 0);
		p.adicionarEfeito(engagedBuff);

		// Agora este método existe porque adicionamos no Passo 2
		this.setCustoTU((int) (this.getCustoTU() * 0.7));

		p.setEscudoAtual(p.getVidaMaxima() * 0.3);
		p.recalcularAtributosEstatisticas();

		if (controller != null)
			controller.atualizarInterfaceTotal();
	}

	@Override
	public Habilidade getHabilidadeInstancia(String nomeHab) {
		if (nomeHab.equalsIgnoreCase("DoubleDown")) {
			return new DoubleDown();
		}
		return super.getHabilidadeInstancia(nomeHab);
	}
}